/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.c1x.target.x86;

import java.util.*;

import com.sun.c1x.ci.*;
import com.sun.c1x.xir.*;

/**
 * X86 specific preprocessing of XIR.
 *
 * @author Thomas Wuerthinger
 *
 */
public class X86XirAssembler extends CiXirAssembler {


    @Override
    protected XirTemplate buildTemplate(String name, boolean isStub) {
        List<XirInstruction> fastPath = new ArrayList<XirInstruction>(instructions.size());
        List<XirInstruction> slowPath = new ArrayList<XirInstruction>();

        int flags = 0;

        if (isStub) {
            flags |= XirTemplate.GlobalFlags.GLOBAL_STUB.mask;
        }

        List<XirInstruction> currentList = fastPath;

        XirOperand divModTemp = null;
        XirOperand divModLeftInput = null;

        for (XirInstruction i : instructions) {
            boolean appended = false;
            switch (i.op) {

                case Mov:
                    break;

                case Add:
                case Sub:
                case Div:
                case Mul:
                case Mod:
                case Shl:
                case Shr:
                case And:
                case Or:
                case Xor:
                    // Convert to two operand form
                    XirOperand xOp = i.x();
                    if (i.op == XirOp.Div || i.op == XirOp.Mod) {
                        // Special treatment to make sure that the left input of % and / is in RAX
                        if (divModLeftInput == null) {
                            divModLeftInput = this.createRegister("divModLeftInput", CiKind.Int, X86.rax);
                        }
                        currentList.add(new XirInstruction(i.x().kind, XirOp.Mov, divModLeftInput, i.x()));
                        xOp = divModLeftInput;
                    } else {
                        if (i.result != i.x()) {
                            currentList.add(new XirInstruction(i.result.kind, XirOp.Mov, i.result, i.x()));
                            xOp = i.result;
                        }
                    }

                    XirOperand yOp = i.y();
                    if (i.op == XirOp.Shl || i.op == XirOp.Shr) {
                        // Special treatment to make sure that the shift count is always in RCX
                        XirOperand fixedLocation = createRegister("fixedShiftCount", i.y().kind, X86.rcx);
                        currentList.add(new XirInstruction(i.result.kind, XirOp.Mov, fixedLocation, i.y()));
                        yOp = fixedLocation;
                    } else if (i.op == XirOp.Mul && (i.y() instanceof XirConstantOperand)) {
                        // Cannot multiply directly with a constant, so introduce a new temporary variable
                        XirOperand tempLocation = createTemp("mulTempLocation", i.y().kind);
                        currentList.add(new XirInstruction(i.result.kind, XirOp.Mov, tempLocation, i.y()));
                        yOp = tempLocation;

                    }

                    if (i.op == XirOp.Div || i.op == XirOp.Mod) {
                        if (divModTemp == null) {
                            divModTemp = this.createRegister("divModTemp", CiKind.Int, X86.rdx);
                        }
                    }

                    if (xOp != i.x() || yOp != i.y()) {
                        currentList.add(new XirInstruction(i.result.kind, i.op, i.result, xOp, yOp));
                        appended = true;
                    }

                    break;

                case PointerLoad:
                case PointerStore:
                case PointerLoadDisp:
                case PointerStoreDisp:
                case PointerCAS:
                    break;
                case CallStub:
                    flags |= XirTemplate.GlobalFlags.HAS_STUB_CALL.mask;
                    break;
                case CallRuntime:
                    flags |= XirTemplate.GlobalFlags.HAS_RUNTIME_CALL.mask;
                    break;
                case CallJava:
                    assert false : "Java calls must be tail calls and not expressed in XIR";
                    // TODO (tw): Assert the properties and conditions around calls
                    break;
                case Jmp:
                    flags |= XirTemplate.GlobalFlags.HAS_CONTROL_FLOW.mask;
                    break;
                case Jeq:
                    flags |= XirTemplate.GlobalFlags.HAS_CONTROL_FLOW.mask;
                    break;
                case Jneq:
                    flags |= XirTemplate.GlobalFlags.HAS_CONTROL_FLOW.mask;
                    break;
                case Jgt:
                    flags |= XirTemplate.GlobalFlags.HAS_CONTROL_FLOW.mask;
                    break;
                case Jgteq:
                    flags |= XirTemplate.GlobalFlags.HAS_CONTROL_FLOW.mask;
                    break;
                case Jugteq:
                    flags |= XirTemplate.GlobalFlags.HAS_CONTROL_FLOW.mask;
                   break;
                case Jlt:
                    flags |= XirTemplate.GlobalFlags.HAS_CONTROL_FLOW.mask;
                    break;
                case Jlteq:
                    flags |= XirTemplate.GlobalFlags.HAS_CONTROL_FLOW.mask;
                    break;
                case Bind:
                    XirLabel label = (XirLabel) i.extra;
                    currentList = label.inline ? fastPath : slowPath;
                    break;
            }
            if (!appended) {
                currentList.add(i);
            }
        }
        XirInstruction[] fp = fastPath.toArray(new XirInstruction[fastPath.size()]);
        XirInstruction[] sp = slowPath.size() > 0 ? slowPath.toArray(new XirInstruction[slowPath.size()]) : null;
        XirLabel[] xirLabels = labels.toArray(new XirLabel[labels.size()]);
        XirParameter[] xirParameters = parameters.toArray(new XirParameter[parameters.size()]);
        XirTemp[] temporaryOperands = temps.toArray(new XirTemp[temps.size()]);
        XirConstant[] constantOperands = constants.toArray(new XirConstant[constants.size()]);
        return new XirTemplate(name, this.variableCount, this.allocateResultOperand, resultOperand, fp, sp, xirLabels, xirParameters, temporaryOperands, constantOperands, flags);
    }

    @Override
    public CiXirAssembler copy() {
        return new X86XirAssembler();
    }
}

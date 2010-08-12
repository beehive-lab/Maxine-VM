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
package com.sun.c1x.target.amd64;

import static com.sun.cri.xir.XirTemplate.GlobalFlags.*;

import java.util.*;

import com.sun.c1x.util.*;
import com.sun.cri.ci.*;
import com.sun.cri.xir.*;

/**
 * AMD64 version of {@link CiXirAssembler}.
 *
 * @author Thomas Wuerthinger
 *
 */
public class AMD64XirAssembler extends CiXirAssembler {

    @Override
    protected XirTemplate buildTemplate(String name, boolean isStub) {
        List<XirInstruction> fastPath = new ArrayList<XirInstruction>(instructions.size());
        List<XirInstruction> slowPath = new ArrayList<XirInstruction>();
        List<XirTemplate> calleeTemplates = new ArrayList<XirTemplate>();

        int flags = 0;

        if (isStub) {
            flags |= GLOBAL_STUB.mask;
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
                        if (divModTemp == null) {
                            divModTemp = createRegister("divModTemp", CiKind.Int, AMD64.rdx);
                        }
                        // Special treatment to make sure that the left input of % and / is in RAX
                        if (divModLeftInput == null) {
                            divModLeftInput = createRegister("divModLeftInput", CiKind.Int, AMD64.rax);
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
                        XirOperand fixedLocation = createRegister("fixedShiftCount", i.y().kind, AMD64.rcx);
                        currentList.add(new XirInstruction(i.result.kind, XirOp.Mov, fixedLocation, i.y()));
                        yOp = fixedLocation;
                    } else if (i.op == XirOp.Mul && (i.y() instanceof XirConstantOperand)) {
                        // Cannot multiply directly with a constant, so introduce a new temporary variable
                        XirOperand tempLocation = createTemp("mulTempLocation", i.y().kind);
                        currentList.add(new XirInstruction(i.result.kind, XirOp.Mov, tempLocation, i.y()));
                        yOp = tempLocation;

                    }

                    if (xOp != i.x() || yOp != i.y()) {
                        currentList.add(new XirInstruction(i.result.kind, i.op, i.result, xOp, yOp));
                        appended = true;
                    }
                    break;

                case NullCheck:
                case PointerLoad:
                case PointerStore:
                case PointerLoadDisp:
                case PointerStoreDisp:
                case PointerCAS:
                    break;
                case CallStub:
                    flags |= HAS_STUB_CALL.mask;
                    calleeTemplates.add((XirTemplate) i.extra);
                    break;
                case CallRuntime:
                    flags |= HAS_RUNTIME_CALL.mask;
                    break;
                case Jmp:
                case Jeq:
                case Jneq:
                case Jgt:
                case Jgteq:
                case Jugteq:
                case Jlt:
                case Jlteq:
                    flags |= HAS_CONTROL_FLOW.mask;
                    break;
                case Bind:
                    XirLabel label = (XirLabel) i.extra;
                    currentList = label.inline ? fastPath : slowPath;
                    break;
                case Safepoint:
                case Align:
                case Entrypoint:
                case PushFrame:
                case PopFrame:
                case Push:
                case Pop:
                case RawBytes:
                case ShouldNotReachHere:
                    break;
                default:
                    throw Util.unimplemented("XIR operation " + i.op);
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
        XirTemplate[] calleeTemplateArray = calleeTemplates.toArray(new XirTemplate[calleeTemplates.size()]);
        return new XirTemplate(name, this.variableCount, this.allocateResultOperand, resultOperand, fp, sp, xirLabels, xirParameters, temporaryOperands, constantOperands, flags, calleeTemplateArray);
    }

    @Override
    public CiXirAssembler copy() {
        return new AMD64XirAssembler();
    }
}

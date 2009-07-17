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
package com.sun.c1x.lir;

import java.util.*;

import com.sun.c1x.*;
import com.sun.c1x.ci.*;
import com.sun.c1x.lir.Location.*;
import com.sun.c1x.target.*;
import com.sun.c1x.target.x86.*;
import com.sun.c1x.value.*;

/**
 *
 * @author Thomas Wuerthinger
 *
 */
public class FrameMap {

    int framesize;
    int argcount;
    int numMonitors;
    int numSpills;
    int reservedArgumentAreaSize;
    int oopMapArgCount;

    CallingConvention incomingArguments;
    int[] argumentLocations;
    final C1XCompilation compilation;

    public FrameMap(C1XCompilation compilation, CiMethod method, int monitors, int maxStack) {

        this.compilation = compilation;
        framesize = -1;
        numSpills = -1;

        assert monitors >= 0 : "not set";
        numMonitors = monitors;
        assert reservedArgumentAreaSize >= 0 : "not set";
        reservedArgumentAreaSize = Math.max(4, reservedArgumentAreaSize) * compilation.target.arch.wordSize;

        argcount = method.signatureType().argumentCount(!method.isStatic());
        argumentLocations = new int[argcount];
        for (int i = 0; i < argcount; i++) {
            argumentLocations[i] = -1;
        }
        incomingArguments = javaCallingConvention(signatureTypeArrayFor(method), false);
        oopMapArgCount = incomingArguments.reservedStackSlots();

        int javaIndex = 0;
        for (int i = 0; i < incomingArguments.length(); i++) {
            LIROperand opr = incomingArguments.at(i);
            if (opr.isAddress()) {
                LIRAddress pointer = opr.asAddressPtr();
                argumentLocations[javaIndex] = pointer.displacement - compilation.target.arch.stackBias;
                incomingArguments.setArg(i, LIROperandFactory.stack(javaIndex, pointer.type()));
            }
            javaIndex += opr.type().size;
        }
    }

    private static BasicType[] signatureTypeArrayFor(CiMethod method) {
        CiSignature sig = method.signatureType();
        BasicType[] sta = new BasicType[sig.argumentCount(!method.isStatic())];

        int z = 0;

        // add receiver, if any
        if (!method.isStatic()) {
            sta[z++] = BasicType.Object;
        }

        // add remaining arguments
        for (int i = 0; i < sig.argumentSlots(false); i++) {
            CiType type = sig.argumentTypeAt(i);
            BasicType t = type.basicType();
            sta[z++] = t;
        }

        assert z == sta.length;

        // done
        return sta;
    }

    public CallingConvention runtimeCallingConvention(BasicType[] signature) {
        // TODO Auto-generated method stub
        return null;
    }

    public CallingConvention javaCallingConvention(BasicType[] signature, boolean outgoing) {


        // compute the size of the arguments first.  The signature array
        // that javaCallingConvention takes includes a TVOID after double
        // work items but our signatures do not.
        int i;
        int sizeargs = 0;
        for (i = 0; i < signature.length; i++) {
          sizeargs += signature[i].size;
        }

        BasicType[] sigBt = new BasicType[sizeargs];
        CiLocation[] regs = new CiLocation[sizeargs];
        int sigIndex = 0;
        for (i = 0; i < sizeargs; i++, sigIndex++) {
          sigBt[i] = signature[sigIndex];
          if (sigBt[i] == BasicType.Long || sigBt[i] == BasicType.Double) {
            sigBt[i + 1] = BasicType.Void;
            i++;
          }
        }

        int outPreserve = compilation.runtime.javaCallingConvention(compilation.method, regs, outgoing);


        List<LIROperand> args = new ArrayList<LIROperand>(signature.length);
        for (i = 0; i < sizeargs;) {
          BasicType t = sigBt[i];
          assert t != BasicType.Void :  "should be skipping these";

          LIROperand opr = mapToOpr(t, regs[i], outgoing);
          args.add(opr);
          if (opr.isAddress()) {
            LIRAddress addr = opr.asAddressPtr();
            outPreserve = Math.max(outPreserve, addr.displacement / 4);
          }
          i += t.size;
        }
        assert args.size() == signature.length :  "size mismatch";
        outPreserve += compilation.runtime.outPreserveStackSlots();

        if (outgoing) {
          // update the space reserved for arguments.
          updateReservedArgumentAreaSize(outPreserve);
        }
        return new CallingConvention(args, outPreserve);
    }

    private void updateReservedArgumentAreaSize(int outPreserve) {
        // TODO Auto-generated method stub

    }

    private LIROperand mapToOpr(BasicType t, CiLocation pair, boolean outgoing) {
        // TODO Auto-generated method stub
        return LIROperandFactory.registerPairToOperand(pair);
    }

    public CallingConvention incomingArguments() {
        return this.incomingArguments;
    }

    public Address addressForSlot(int singleStackIx) {
        // TODO Auto-generated method stub
        return null;
    }

    public Address addressForSlot(int doubleStackIx, int i) {
        // TODO Auto-generated method stub
        return null;
    }

    public int reservedArgumentAreaSize() {
        // TODO Auto-generated method stub
        return 0;
    }

    public Address addressForMonitorLock(int monitorNo) {
        // TODO Auto-generated method stub
        return null;
    }

    public Address addressForMonitorObject(int i) {
        // TODO Auto-generated method stub
        return null;
    }

    public int framesize() {
        // TODO Auto-generated method stub
        return 0;
    }

    public int argcount() {
        // TODO Auto-generated method stub
        return 0;
    }

    public Register[] callerSavedRegisters() {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean finalizeFrame(int maxSpills) {
        // TODO Complete this, for now just return true
        return true;
    }

    public VMReg regname(LIROperand opr) {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean isCallerSaveRegister(LIROperand res) {
        // TODO Auto-generated method stub
        return false;
    }

    public int oopMapArgCount() {
        // TODO Auto-generated method stub
        return 0;
    }

    public VMReg slotRegname(int i) {
        // TODO Auto-generated method stub
        return null;
    }

    public VMReg monitorObjectRegname(int i) {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean locationForMonitorObject(int monitorIndex, Location[] loc) {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean locationForMonitorLock(int monitorIndex, Location[] loc) {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean locationsForSlot(int name, LocationType locType, Location[] loc) {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean locationsForSlot(int doubleStackIx, LocationType locType, Location[] loc1, Object object) {
        // TODO Auto-generated method stub
        return false;
    }

    public VMReg fpuRegname(int fpuRegnrHi) {
        // TODO Auto-generated method stub
        return null;
    }
}

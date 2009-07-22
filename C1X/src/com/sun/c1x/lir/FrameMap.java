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
import com.sun.c1x.asm.*;
import com.sun.c1x.ci.*;
import com.sun.c1x.lir.Location.*;
import com.sun.c1x.target.*;
import com.sun.c1x.util.*;
import com.sun.c1x.value.*;

/**
 *
 * @author Thomas Wuerthinger
 *
 */
public abstract class FrameMap {

    public static final int spillSlotSizeInBytes = 4;

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
        argcount = method.signatureType().argumentSlots(!method.isStatic());

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
        for (int i = 0; i < sig.argumentCount(false); i++) {
            CiType type = sig.argumentTypeAt(i);
            BasicType t = type.basicType();
            sta[z++] = t;
        }

        assert z == sta.length;

        // done
        return sta;
    }

    public CallingConvention runtimeCallingConvention(BasicType[] signature) {
        return javaCallingConvention(signature, true);
    }

    public CallingConvention javaCallingConvention(BasicType[] signature, boolean outgoing) {

        CiLocation[] regs = new CiLocation[signature.length];
        int preservedStackSlots = compilation.runtime.javaCallingConvention(signature, regs, outgoing);
        List<LIROperand> args = new ArrayList<LIROperand>(signature.length);
        for (int i = 0; i < signature.length; i++) {
            args.add(mapToOpr(signature[i], regs[i], outgoing));
        }

        return new CallingConvention(args, preservedStackSlots);
    }

    private LIROperand mapToOpr(BasicType t, CiLocation location, boolean outgoing) {

        if (location.isStackOffset()) {
            return LIROperandFactory.stack(location.stackOffset, t);
        } else if (location.second == null) {
            assert location.first != null;
            return new LIRLocation(t, location.first);
        } else {
            assert location.first != null;
            return new LIRLocation(t, location.first, location.second);
        }
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
        assert framesize != -1 :  "hasn't been calculated";
        return framesize;
    }

    public int argcount() {
        return argcount;
    }

    public boolean finalizeFrame(int nofSlots) {
        assert nofSlots >= 0 :  "must be positive";
        assert numSpills == -1 :  "can only be set once";
        numSpills = nofSlots;
        assert framesize == -1 :  "should only be calculated once";


        // TODO:  Add offset of deopt orig pc
        framesize =  Util.roundTo(spOffsetForMonitorBase(0) +
                               numMonitors * compilation.runtime.sizeofBasicObjectLock() +

                               compilation.target.arch.framePadding,
                               compilation.target.stackAlignment) / 4;

        for (int i = 0; i < incomingArguments.length(); i++) {
          LIROperand opr = incomingArguments.at(i);
          if (opr.isStack()) {
            argumentLocations[i] += framesizeInBytes();
          }
        }
        // make sure it's expressible on the platform
        return validateFrame();
    }

    private int spOffsetForMonitorBase(int index) {
        int endOfSpills = Util.roundTo(compilation.target.firstAvailableSpInFrame + reservedArgumentAreaSize, Util.sizeofDouble()) +
        numSpills * spillSlotSizeInBytes;
      int offset = Util.roundTo(endOfSpills, compilation.target.arch.wordSize) + index * compilation.runtime.sizeofBasicObjectLock();
      return offset;
    }

    private int framesizeInBytes() {
        return framesize * 4;
    }

    private boolean validateFrame() {
        return true;
    }

    public CiLocation regname(LIROperand opr) {
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

    public CiLocation slotRegname(int i) {
        // TODO Auto-generated method stub
        return null;
    }

    public CiLocation monitorObjectRegname(int i) {
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

    public CiLocation fpuRegname(int fpuRegnrHi) {
        // TODO Auto-generated method stub
        return null;
    }

    public LIROperand receiverOpr() {
        return mapToOpr(BasicType.Object, compilation.runtime.receiverLocation(), false);
    }

    public abstract boolean allocatableRegister(Register r);
}

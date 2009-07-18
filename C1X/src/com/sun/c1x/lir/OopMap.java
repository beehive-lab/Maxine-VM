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

import com.sun.c1x.asm.*;
import com.sun.c1x.ci.*;
import com.sun.c1x.lir.OopMapValue.*;
import com.sun.c1x.util.*;

/**
 * The <code>OopMap</code> class definition.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 *
 */
public class OopMap {

    private int pcOffset;
    private int omvCount;
    private int omvDataSize;
    private byte [] omvData;
    private CompressedWriteStream writeStream;
    private List<OopTypes> locsUsed;
    private int locsLength;

    private OopMap() {

    }

    private OopMap(OopMap source) {
        // used only by deepCopy
        // This constructor does a deep copy
        // of the source OopMap.
        setWriteStream(new CompressedWriteStream(source.omvCount() * 2));
        setOmvData(null);
        setOmvCount(0);
        setOffset(source.offset());

        locsLength = source.locsLength;
        locsUsed = new ArrayList<OopTypes>(locsLength);
        for (int i = 0; i < locsLength; i++) {
            locsUsed.set(i, OopTypes.UnusedValue);
        }

        // We need to copy the entries too.
        for (OopMapStream oms = new OopMapStream(source); !oms.isDone(); oms.next()) {
          OopMapValue omv = oms.current();
          omv.writeOn(writeStream());
          incrementCount();
        }
    }

    public OopMap(int frameSize, int argCount) {
        // OopMaps are usually quite so small, so pick a small initial size
        setWriteStream(new CompressedWriteStream(32));
        setOmvData(null);
        setOmvCount(0);

        locsLength = CiLocation.stack2reg(0).value() + frameSize + argCount;
        locsUsed = new ArrayList<OopTypes>(locsLength);
        for (int i = 0; i < locsLength; i++) {
            locsUsed.set(i, OopTypes.UnusedValue);
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#clone()
     */
    @Override
    protected Object clone() throws CloneNotSupportedException {
        OopMap newOop = new OopMap();
        newOop.locsLength = this.locsLength;
        newOop.locsUsed = new ArrayList<OopTypes>(locsUsed);
        newOop.omvData = omvData.clone();
        newOop.omvDataSize = omvDataSize;
        newOop.pcOffset = pcOffset;
        newOop.omvCount = omvCount;
        newOop.writeStream = writeStream;
        return newOop;
    }

    // Accessors
    byte []  omvData() {
        return omvData;
    }

    private void setOmvData(byte []  value) {
        omvData = value;
    }

    private int omvDataSize() {
        return omvDataSize;
    }

    private void setOmvDataSize(int value) {
        omvDataSize = value;
    }

    int omvCount() {
        return omvCount;
    }

    private void setOmvCount(int value) {
        omvCount = value;
    }

    private void incrementCount() {
        omvCount++;
    }

    public CompressedWriteStream writeStream() {
        return writeStream;
    }

    private void setWriteStream(CompressedWriteStream value) {
        writeStream = value;
    }

    // pc-offset handling
    public int offset() {
        return pcOffset;
    }

    public void setOffset(int o) {
        pcOffset = o;
    }

    // Check to avoid double insertion
    public OopTypes locsUsed(int indx) {
        return locsUsed.get(indx);
    }

    // Construction
    // frameSize units are stack-slots (4 bytes) NOT intptrT; we can name odd
    // slots to hold 4-byte values like ints and floats in the LP64 build.
    public void setOop(CiLocation reg) {
        setXxx(reg, OopTypes.OopValue, CiLocation.bad());
    }

    public void setValue(CiLocation reg) {
        // At this time, we only need value entries in our OopMap when ZapDeadCompiledLocals is active.
        // if (ZapDeadCompiledLocals)
        setXxx(reg, OopTypes.ValueValue, CiLocation.bad());
    }

    public void setNarrowoop(CiLocation reg) {
        setXxx(reg, OopTypes.NarrowOopValue, CiLocation.bad());
    }

    public void setDead(CiLocation reg) {
    }

    public void setCalleeSaved(CiLocation reg, CiLocation callerMachineRegister) {
        setXxx(reg, OopTypes.CalleeSavedValue, callerMachineRegister);
    }

    public void setDerivedOop(CiLocation reg, CiLocation derivedFromLocalRegister) {
        if (reg == derivedFromLocalRegister) {
            // Actually an oop, derived shares storage with base,
            setOop(reg);
        } else {
            setXxx(reg, OopTypes.DerivedOopValue, derivedFromLocalRegister);
        }
    }

    public void setXxx(CiLocation reg, OopTypes x, CiLocation optional) {
        assert reg.value() < locsLength : "too big reg value for stack size";
        assert locsUsed.get(reg.value()) == OopTypes.UnusedValue : "cannot insert twice";
        locsUsed.add(reg.value(), x);
        OopMapValue o = new OopMapValue(reg, x);

        if (x == OopTypes.CalleeSavedValue) {
            // This can never be a stack location, so we don't need to transform it.
            assert optional.isReg() : "Trying to callee save a stack location";
            o.setContentReg(optional);
        } else if (x == OopTypes.DerivedOopValue) {
            o.setContentReg(optional);
        }

        o.writeOn(writeStream());
        incrementCount();
    }

    public int heapSize() {
        Util.unimplemented();
        // TODO : need to redesing this
        int size = OopMap.sizeInBytes();
        int align = 0; // sizeof(void *) - 1;
        if (writeStream() != null) {
            size += writeStream().position();
        } else {
            size += omvDataSize();
        }
        // Align to a reasonable ending point
        size = ((size + align) & ~align);
        return size;
    }

    /**
     * @return
     */
    private static int sizeInBytes() {
        // TODO Auto-generated method stub
        return 0;
    }

    public void copyTo(Pointer addr) {
        Util.unimplemented();
        Pointer.memcopy(addr, this, sizeInBytes());
        // TODO need to solve this
         Pointer.memcopy(new Pointer(addr.address() + sizeInBytes()), writeStream().buffer(), writeStream().position());
         OopMap newOop = this;
         newOop.setOmvDataSize(writeStream().position());
        // newOop.setOmvData((unsigned char *)(addr + sizeof(OopMap)));
         newOop.setWriteStream(null);
    }

    public OopMap deepCopy() {
        return new OopMap(this);
    }

    public boolean legalVmRegName(CiLocation local) {
        return OopMapValue.legalVmRegName(local);
    }

    // Printing
    void printOn(LogStream st) {
        OopMapValue omv;
        st.print("OopMap{");
        for (OopMapStream oms = new OopMapStream(this); !oms.isDone(); oms.next()) {
            omv = oms.current();
            omv.printOn(st);
        }
        st.printf("off=%d}", offset());
    }

    void print() {
        printOn(TTY.out);
    }
}

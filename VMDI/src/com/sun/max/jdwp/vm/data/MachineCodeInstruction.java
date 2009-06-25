/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.max.jdwp.vm.data;

/**
 * This class represents a machine code instruction of a target method.
 *
 * @author Thomas Wuerthinger
 *
 */
public class MachineCodeInstruction extends AbstractSerializableObject {

    private final String mnemonic;
    private final int position;
    private final long address;
    private final String label;
    private final byte[] bytes;
    private final String operands;
    private final long targetAddress;

    public MachineCodeInstruction(String mnemonic, int position, long address, String label, byte[] bytes, String operands, long targetAddress) {
        this.mnemonic = mnemonic;
        this.position = position;
        this.address = address;
        this.label = label;
        this.bytes = bytes;
        this.operands = operands;
        this.targetAddress = targetAddress;
    }

    public String getMnemonic() {
        return mnemonic;
    }

    public int getPosition() {
        return position;
    }

    public long getAddress() {
        return address;
    }

    public String getLabel() {
        return label;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public String getOperands() {
        return operands;
    }

    public long getTargetAddress() {
        return targetAddress;
    }
}

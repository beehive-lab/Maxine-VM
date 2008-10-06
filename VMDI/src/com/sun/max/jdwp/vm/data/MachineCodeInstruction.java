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
/*VCSID=ae55bc3b-9e60-480b-9074-0ddc3a8167cc*/
package com.sun.max.jdwp.vm.data;

/**
 * This class represents a machine code instruction of a target method.
 *
 * @author Thomas Wuerthinger
 *
 */
public class MachineCodeInstruction extends AbstractSerializableObject {

    private String _mnemonic;
    private int _position;
    private long _address;
    private String _label;
    private byte[] _bytes;
    private String _operands;
    private long _targetAddress;

    public MachineCodeInstruction(String mnemonic, int position, long address, String label, byte[] bytes, String operands, long targetAddress) {
        _mnemonic = mnemonic;
        _position = position;
        _address = address;
        _label = label;
        _bytes = bytes;
        _operands = operands;
        _targetAddress = targetAddress;
    }

    public String getMnemonic() {
        return _mnemonic;
    }

    public int getPosition() {
        return _position;
    }

    public long getAddress() {
        return _address;
    }

    public String getLabel() {
        return _label;
    }

    public byte[] getBytes() {
        return _bytes;
    }

    public String getOperands() {
        return _operands;
    }

    public long getTargetAddress() {
        return _targetAddress;
    }
}

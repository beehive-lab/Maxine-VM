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
/*VCSID=dd012036-7e89-4b39-bc82-2d6b86137a3a*/
package com.sun.max.jdwp.vm.data;

/**
 * An entry in the variable table as specified by the JDWP protocol.
 *
 * @author Thomas Wuerthinger
 *
 */
public class VariableTableEntry extends AbstractSerializableObject {
    private long _codeIndex;
    private int _length;
    private String _name;
    private int _slot;
    private String _signature;
    private String _genericSignature;

    public VariableTableEntry(long codeIndex, int length, String name, int slot, String signature, String genericSignature) {
        _codeIndex = codeIndex;
        _length = length;
        _name = name;
        _slot = slot;
        _signature = signature;
        _genericSignature = genericSignature;
    }

    public long getCodeIndex() {
        return _codeIndex;
    }

    public int getLength() {
        return _length;
    }

    public String getName() {
        return _name;
    }

    public int getSlot() {
        return _slot;
    }

    public String getSignature() {
        return _signature;
    }

    public String getGenericSignature() {
        return _genericSignature;
    }
}

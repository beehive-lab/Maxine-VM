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
 * This class represents an array of register sets. There is e.g. one entry in the array for floating point and one entry for integer registers. The reason for wrapping
 * the array in a class is that this way the whole object can be serialized and transmitted at once.
 *
 * @author Thomas Wuerthinger
 *
 */
public class RegistersGroup extends AbstractSerializableObject {

    private Registers[] registers;

    public RegistersGroup(Registers[] arr) {
        registers = arr;
    }

    public Registers[] getRegisters() {
        return registers;
    }
}

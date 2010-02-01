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
package com.sun.max.jdwp.vm.proxy;

import com.sun.max.jdwp.vm.core.*;

/**
 * Class representing a JDWP stack frame.
 *
 * @author Thomas Wuerthinger
 *
 */
public interface FrameProvider extends Provider {

    /**
     * @return the current location of the instruction pointer in this frame
     */
    CodeLocation getLocation();

    /**
     * Accesses the values on the stack.
     *
     * @param i the index of the accessed value
     * @return the stack value at the specified index
     */
    VMValue getValue(int i);

    /**
     * Modifies a value on the stack.
     *
     * @param i the index of the value that should be set
     * @param value the new value of the stack slot
     */
    void setValue(int i, VMValue value);

    /**
     * @return the thread of this stack frame
     */
    ThreadProvider getThread();

    /**
     * @return the object representing the instance of the java "this" object in the stack frame
     */
    ObjectProvider thisObject();

    /**
     * @return the target method whose call produced the stack frame
     */
    @ConstantReturnValue
    @JDWPPlus
    TargetMethodAccess getTargetMethodProvider();

    /**
     * @return the address of the instruction pointer in this frame
     */
    @JDWPPlus
    long getInstructionPointer();

    /**
     * @return the address of the frame pointer
     */
    @JDWPPlus
    long getFramePointer();

    /**
     * @return the address of the stack pointer
     */
    @JDWPPlus
    long getStackPointer();

    /**
     * @return the raw values on the stack
     */
    @JDWPPlus
    long[] getRawValues();
}

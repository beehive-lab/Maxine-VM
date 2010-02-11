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
package com.sun.max.tele;

import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;

/**
 * A location in code in the VM, possibly specified by an absolute
 * address (for example for compiled methods that are not relocatable)
 * or by method description (which can be specified whether the
 * named method has been compiled or even loaded into the VM).
 *
 * @author Michael Van De Vanter
 */
public interface MaxCodeLocation {

    /**
     * Is there a target code representation for the code location in the VM.
     * <br>
     * Thread-safe
     *
     * @return whether an absolute code location for target code is specified
     */
    boolean hasAddress();

    /**
     * Target code instruction pointer;
     * zero if no target code information about the location available.
     * <br>
     * Thread-safe
     *
     * @return memory address in the VM of the code location
     */
    Address address();

    /**
     * Is there a bytecode representation for the code location, expressed in terms
     * of the method description loaded in the VM and a byte offset of the instruction.
     * <br>
     * Initially false when location is created by key.  Becomes true when a
     * corresponding loaded method in the VM is located.
     * <br>
     * Thread-safe
     *
     * @return whether information about the method loaded in the VM is available.
     */
    boolean hasBytecodeLocation();

    /**
     * A representation for the code location, expressed in terms
     * of the method description loaded in the VM and a byte offset of the instruction.
     * <br>
     * Initially null when location is created by key.  Becomes non-null when a
     * corresponding loaded method in the VM is located.
     * <br>
     * Thread-safe
     *
     * @return  bytecode position and information about the method loaded in the VM
     */
    MaxBytecodeLocation bytecodeLocation();

    /**
     * Is there an abstract intentional representation of the location, in terms of method name and signature, available?
     * <br>
     * Thread-safe
     *
     * @return whether a method key is available
     */
    boolean hasKey();

    /**
     * Gets an intentional description of the location, which may be the only specification present.
     *
     * @return a key describing by name the method and bytecode location, independent of the loaded class.
     */
    MethodKey methodKey();

    /**
     * Determines whether two descriptions are equivalent.  For locations specified in terms of
     * an address in a method compilation, returns whether the address are equal.  For locations
     * specified only in terms of a method description, return whether the descriptions refer to
     * the same method in terms of the language.  Comparing one kind with the other will always
     * return false.
     */
    boolean isSameAs(MaxCodeLocation codeLocation);

    /**
     * A string describing the intent of the location, useful, for example, as a menu
     * entry, or for debugging.
     *
     * @return a human-readable description of the intent of the location
     */
    String description();

}

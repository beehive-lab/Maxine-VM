/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.tele;

import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;

/**
 * Location of a code instruction in the VM.  This might be specified by the absolute memory
 * address of a machine code instruction, for example in a method compilation (which
 * are assumed to be not relocatable).  A location also might be specified
 * by method description, which can be specified whether the
 * named method has been compiled or even loaded into the VM.
 *
 * @author Michael Van De Vanter
 */
public interface MaxCodeLocation {

    /**
     * Is there a machine code representation for the code location in the VM,
     * which refers to a specific compilation.
     * <br>
     * Thread-safe
     *
     * @return whether an absolute machine code location is specified
     * @see #address()
     */
    boolean hasAddress();

    /**
     * Location in VM memory of a machine code instruction;
     * returns {@link Address#zero()}
     * if no machine code information about the location is specified.
     * <br>
     * Machine code is assumed <strong>not</strong> to relocate in VM memory.
     * <br>
     * Thread-safe
     *
     * @return memory address in the VM of the machine code instruction,
     * {@link Address#zero()} if not available.
     * @see #hasAddress()
     */
    Address address();

    /**
     * Is there a bytecode representation for the code location, expressed in terms
     * of the method description loaded in the VM, and by extension a specific
     * bytecode representation.
     * <br>
     * Initially false when location is created by method key specification.  Becomes true when a
     * corresponding loaded method in the VM is located.
     * <br>
     * Thread-safe
     *
     * @return whether bytecode information about the method loaded in the VM is available.
     * @see #teleClassMethodActor()
     */
    boolean hasTeleClassMethodActor();

    /**
     * Access to a description in the VM of the method containing the location, and by extension
     * to the bytecode representation of the method.
     * <br>
     * Initially null when location is created by key.  Becomes non-null when a
     * corresponding loaded method in the VM is located.
     * <br>
     * Thread-safe
     *
     * @return  bytecode position and information about the method loaded in the VM
     * @see #hasTeleClassMethodActor()
     */
    TeleClassMethodActor teleClassMethodActor();

    /**
     * Is there an abstract intentional representation of the location, in terms of method name and signature, available?
     * <br>
     * Thread-safe
     *
     * @return whether a method key is available
     * @see #methodKey()
     */
    boolean hasMethodKey();

    /**
     * Gets an intentional description of the location, which may be the only specification present.
     *
     * @return a key describing by name the method and bytecode location, independent of the loaded class.
     * @see #hasMethodKey()
     */
    MethodKey methodKey();

    /**
     * Bytecode Index: location of a bytecode instruction, specified as the byte offset from the beginning of the bytecodes, default value is -1.
     * <p>
     * Specific values:
     * <ol>
     * <li>-1; specifies method entry (first bytecode instruction, and the prologue entry of any machine code compilation); this value
     * can be meaningful, even if bytecode information is not yet available. </li>
     * <li>0:  specifies method entry (first bytecode instruction of the method, and the machine code corresponding to the first bytecode instruction
     * in any machine code compilation); this value can be meaningful, even if bytecode information is not yet available.</li>
     * <li>> 0:  offset in bytes of the instruction, relative to the method bytecodes available via {@link MaxCodeLocation#teleClassMethodActor()}.</li>
     * </ol>
     *
     * @return offset of location in bytes from beginning of bytecode representation
     * @see #teleClassMethodActor()
     */
    int bci();

    /**
     * Determines whether two descriptions are equivalent.  For locations specified in terms of
     * an address in a machine code compilation, returns whether the address are equal.  For locations
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

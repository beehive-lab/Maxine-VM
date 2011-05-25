/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.jdwp.vm.proxy;

import com.sun.max.jdwp.vm.core.*;
import com.sun.max.jdwp.vm.data.*;

/**
 * Class representing a method in the VM.
 */
public interface MethodProvider extends Provider {

    /**
     * @return flags of the method according to the Java VM specification
     */
    @ConstantReturnValue
    int getFlags();

    /**
     * @return the name of the method
     */
    @ConstantReturnValue
    String getName();

    /**
     * @return the signature of the method according to the Java VM specification
     */
    @ConstantReturnValue
    String getSignature();

    /**
     * @return the signature with generics according to the Java VM specification
     */
    @ConstantReturnValue
    String getSignatureWithGeneric();

    /**
     * Only valid when it is a static method. This invokes a static method.
     *
     * @param arguments the arguments for invoking the method
     * @param threadProvider the thread that should be used for the method execution
     * @param singleThreaded if this boolean is set to true then it is guaranteed that only the specified thread will run
     * @return
     */
    VMValue invokeStatic(VMValue[] arguments, ThreadProvider threadProvider, boolean singleThreaded);

    /**
     * Only valid when it is an instance method. This invokes the instance method.
     *
     * @param object the instance object on which the method should be invoked
     * @param arguments the arguments for invoking the method
     * @param threadProvider the thread that should be used for the method execution
     * @param singleThreaded  if this boolean is set to true then it is guaranteed that only the specified thread will run
     * @param nonVirtual if this boolean is set then a non-virtual call should be preformed
     * @return
     */
    VMValue invoke(ObjectProvider object, VMValue[] arguments, ThreadProvider threadProvider, boolean singleThreaded, boolean nonVirtual);

    /**
     * @return the reference type in which this method is declared
     */
    @ConstantReturnValue
    ReferenceTypeProvider getReferenceTypeHolder();

    /**
     * @return line table information as specified in the Java VM specification
     */
    @ConstantReturnValue
    LineTableEntry[] getLineTable();

    /**
     * @return variable table information as specified in the Java VM specification
     */
    @ConstantReturnValue
    VariableTableEntry[] getVariableTable();

    /**
     * @return the number of words in the frame used by arguments. Eight-byte arguments use two words; all others use one
     */
    @ConstantReturnValue
    int getNumberOfArguments();

    /**
     * @return an array of target methods that represent machine code representations of this Java method
     */
    @JDWPPlus
    TargetMethodAccess[] getTargetMethods();
}

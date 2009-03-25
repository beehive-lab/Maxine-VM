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
import com.sun.max.jdwp.vm.data.*;

/**
 * Class representing a method in the VM.
 *
 * @author Thomas Wuerthinger
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
     * @return the number of parameters of the method
     */
    @ConstantReturnValue
    int getNumberOfParameters();

    /**
     * @return an array of target methods that represent machine code representations of this Java method
     */
    @JDWPPlus
    TargetMethodAccess[] getTargetMethods();
}

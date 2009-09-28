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
package com.sun.max.vm.compiler.eir.allocate;

import com.sun.max.*;
import com.sun.max.asm.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.vm.compiler.eir.*;

/**
 * A factory controlling which subclass of {@link EirAllocator} is to used when doing register allocation.
 *
 * @see #EIR_ALLOCATOR_FACTORY_CLASS_PROPERTY_NAME
 * @see #EIR_ALLOCATOR_ALGORITHM_PROPERTY_NAME
 *
 * @author Doug Simon
 */
public abstract class EirAllocatorFactory {

    /**
     * The name of the system property specifying a subclass of {@link EirAllocatorFactory} that is
     * to be instantiated and used at runtime to create {@link EirAllocator} instances.
     */
    public static final String EIR_ALLOCATOR_FACTORY_CLASS_PROPERTY_NAME = "max.eirAllocator.factory.class";

    /**
     * The name of the system property specifying the register allocator algorithm for which an
     * {@link EirAllocatorFactory} is to be instantiated and used at runtime to create {@link EirAllocator} instances.
     * The factory class instantiated is the class named {@code "Factory"} in the sub-package of {@code
     * com.sun.max.vm.compiler.eir.allocate} obtained by appending the algorithm name followed by the lowercase name of
     * the {@linkplain InstructionSet ISA} specified by the {@linkplain Platform#target() target platform}. For example,
     * if the value of this system property is "linearscan" and the target ISA is "amd64" then the factory class to be
     * instantiated is {@code com.sun.max.vm.compiler.eir.allocate.linearscan.amd64}.
     *
     * This property is ignored if {@link #EIR_ALLOCATOR_FACTORY_CLASS_PROPERTY_NAME} is set.
     */
    public static final String EIR_ALLOCATOR_ALGORITHM_PROPERTY_NAME = "max.eirAllocator.algorithm";

    /**
     * The name of the default register allocator algorithm to be used.
     *
     * @see #EIR_ALLOCATOR_ALGORITHM_PROPERTY_NAME
     */
    private static final String DEFAULT_ALGORITHM = "some";

    private static final EirAllocatorFactory instance;

    static {
        String factoryClassName = System.getProperty(EIR_ALLOCATOR_FACTORY_CLASS_PROPERTY_NAME);
        if (factoryClassName == null) {
            final String isa = Platform.hostOrTarget().processorKind.instructionSet.name().toLowerCase();
            final String algorithm = System.getProperty(EIR_ALLOCATOR_ALGORITHM_PROPERTY_NAME, DEFAULT_ALGORITHM);
            final String factoryPackageName = new Package().subPackage(algorithm, isa).name();
            factoryClassName = factoryPackageName + ".Factory";
        }
        try {
            instance = (EirAllocatorFactory) Class.forName(factoryClassName).newInstance();
        } catch (Exception exception) {
            throw ProgramError.unexpected("Error instantiating " + factoryClassName, exception);
        }
    }

    /**
     * Creates a register allocator.
     */
    public abstract EirAllocator newAllocator(EirMethodGeneration methodGeneration);

    /**
     * Creates a register allocator using the selected factory.
     */
    public static EirAllocator createAllocator(EirMethodGeneration methodGeneration) {
        return instance.newAllocator(methodGeneration);
    }

    /**
     * Determines if the selected factory is an instance of a given class.
     *
     * @param factoryClass the class to test
     * @return {@code true} if the selected factory is an instance of {@code factoryClass}; {@code false} otherwise
     */
    public static boolean isSelected(Class<? extends EirAllocatorFactory> factoryClass) {
        return factoryClass.isInstance(instance);
    }

    /**
     * Determines if the selected factory is in a sub-package of a given package.
     *
     * @param allocatorPackage the package to test
     * @return {@code true} if the selected factory is in a sub-package of {@code allocatorPackage}; {@code false} otherwise
     */
    public static boolean isSelected(MaxPackage allocatorPackage) {
        return instance.getClass().getPackage().getName().startsWith(allocatorPackage.name());
    }
}

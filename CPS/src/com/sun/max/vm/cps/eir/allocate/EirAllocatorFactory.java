/*
 * Copyright (c) 2009, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.eir.allocate;

import com.sun.max.config.*;
import com.sun.max.lang.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.vm.cps.eir.*;

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
     * the {@linkplain ISA ISA} specified by the {@linkplain Platform#platform() target platform}. For example,
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
            final String isa = Platform.platform().isa.name().toLowerCase();
            final String algorithm = System.getProperty(EIR_ALLOCATOR_ALGORITHM_PROPERTY_NAME, DEFAULT_ALGORITHM);
            final String factoryPackageName = Classes.getPackageName(EirAllocator.class) + "." + algorithm + "." + isa;
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
    public static boolean isSelected(BootImagePackage allocatorPackage) {
        return instance.getClass().getPackage().getName().startsWith(allocatorPackage.name());
    }
}

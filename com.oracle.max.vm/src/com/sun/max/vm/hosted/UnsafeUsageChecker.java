/*
 * Copyright (c) 2011, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.hosted;

import java.util.*;

import com.sun.cri.bytecode.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.jdk.*;
import com.sun.max.vm.type.*;

/**
 * The class {@linkplain sun.misc.Unsafe} provides several methods to query the offset of fields within an object
 * and to layout of arrays. These offsets are different when running in a hosted environment during boot image generation
 * compared to the offsets used by Maxine (which depend on the object and array layout and are thus configurable by schemes).
 * Therefore, it is necessary to patch offsets when writing them into the boot image, which is done by the class {@linkplain JDKInterceptor}.
 * However, finding the classes and fields that need interception is tricky: there is no list of JDK classes that use Unsafe.
 * Missing interceptions lead to hard to find errors at run time.
 *
 * This class provides a heuristic to find usages of Unsafe that need interception. It is based on the observations that queries
 * for offsets are usually in the same class that also store the offsets. Therefore, every class that queries for an offset must
 * have at least one interceptor registered. There is a whitelist to filter false positives.
 *
 * The heuristic cannot detect wrong definitions of interceptors, and missing definition of interceptors when there is already at least
 * one interceptor defined for a class. However, it will raise an alarm when a previously uknown class is added to the boot image that
 * queries field offsets.
 */
@HOSTED_ONLY
public class UnsafeUsageChecker {
    /**
     * Classes that have a call to one of the checked methods, but were inspected to not store any offsets in fields.
     */
    private static final Set<String> whitelist = new HashSet<String>(Arrays.asList(new String[] {
        JDK.java_util_concurrent_SynchronousQueue.className(),
    }));

    /**
     * The set of classes discovered during class loading that contain a call to one of the checked methods.
     */
    private static Set<String> classesUsingUnsafe = new HashSet<String>();

    /**
     * Type descriptor of the Unsafe class.
     */
    private static final TypeDescriptor unsafeType = JavaTypeDescriptor.getDescriptorForTupleType(JDK.sun_misc_Unsafe.javaClass());

    /**
     * List of checked methods in the Unsafe class that retrieve field or array offsets.
     */
    private static final Set<Utf8Constant> checkedMethodNames = new HashSet<Utf8Constant>(Arrays.asList(new Utf8Constant[] {
        SymbolTable.makeSymbol("objectFieldOffset"),
        SymbolTable.makeSymbol("staticFieldOffset"),
        SymbolTable.makeSymbol("staticFieldBase"),
        SymbolTable.makeSymbol("fieldOffset"),
        SymbolTable.makeSymbol("arrayBaseOffset"),
        SymbolTable.makeSymbol("arrayIndexScale"),
    }));


    /**
     * Returns true if the given class calls any of the checked methods.
     */
    public static boolean isClassUsingUnsafe(String className) {
        return classesUsingUnsafe.contains(className);
    }

    /**
     * Called by the class loader for every loaded method. Note that we also need to analyze static class initializers that
     * are not present in the boot image because they were already executed during boot image generation. This method must
     * be called before this filtering.  Because of that, there is no ClassActor or MethodActor available yet.
     *
     * @param classDescriptor The class that contains the method.
     * @param method The name of the method that is analyzed. It is actually not used, but useful for debug printing.
     * @param code The code of the method.
     */
    public static void methodLoadedHook(TypeDescriptor classDescriptor, Utf8Constant method, CodeAttribute code) {
        if (code == null) {
            return;
        }

        BytecodeStream stream = new BytecodeStream(code.code());
        int bci = 0;
        while (bci < stream.endBCI()) {
            switch (stream.currentBC()) {
                case Bytecodes.INVOKESTATIC:
                case Bytecodes.INVOKESPECIAL:
                case Bytecodes.INVOKEVIRTUAL:
                case Bytecodes.INVOKEINTERFACE:
                    MethodRefConstant methodRef = code.cp.methodAt(stream.readCPI());
                    if (methodRef.holder(code.cp) == unsafeType && checkedMethodNames.contains(methodRef.name(code.cp)) &&
                                    !whitelist.contains(classDescriptor.toJavaString())) {
                        classesUsingUnsafe.add(classDescriptor.toJavaString());
                    }
                    break;
            }
            stream.next();
            bci = stream.currentBCI();
        }
    }
}

/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.cri.ri;

import java.lang.reflect.*;
import java.util.*;

import com.sun.cri.ci.*;

/**
 * Encapsulates the main functionality of the runtime for the compiler, including access
 * to constant pools, OSR frames, inlining requirements, and runtime calls such as checkcast.
s */
public interface RiRuntime {

    /**
     * Gets the constant pool for a method.
     * @param method the method
     * @return the constant pool for the method
     */
    RiConstantPool getConstantPool(RiMethod method);

    /**
     * Gets an {@link RiOsrFrame OSR frame} instance for the specified method
     * at the specified OSR bytecode index.
     * @param method the method
     * @param bci the bytecode index
     * @return an OSR frame that describes the layout of the frame
     */
    RiOsrFrame getOsrFrame(RiMethod method, int bci);

    /**
     * Checks whether the specified method is required to be inlined (for semantic reasons).
     * If this method returns true, then the null-check of the receiver emitted during
     * inlining is omitted.
     *
     * @param method the method being called
     * @return {@code true} if the method must be inlined; {@code false} to let the compiler
     * use its own heuristics
     */
    boolean mustInline(RiMethod method);

    /**
     * Checks whether the specified method must not be inlined (for semantic reasons).
     * @param method the method being called
     * @return {@code true} if the method must not be inlined; {@code false} to let the compiler
     * use its own heuristics
     */
    boolean mustNotInline(RiMethod method);

    /**
     * Checks whether the specified method cannot be compiled.
     * @param method the method being called
     * @return {@code true} if the method cannot be compiled
     */
    boolean mustNotCompile(RiMethod method);

    /**
     * Offset of the lock within the lock object on the stack.
     *
     * @return the offset in bytes
     */
    int basicObjectLockOffsetInBytes();

    /**
     * Get the size in bytes of a lock object on the stack.
     */
    int sizeOfBasicObjectLock();

    /**
     * The offset of the normal entry to the code. The compiler inserts NOP instructions to satisfy this constraint.
     *
     * @return the code offset in bytes
     */
    int codeOffset();

    /**
     * Returns the disassembly of the given code bytes. Used for debugging purposes only.
     *
     * @param code the code bytes that should be disassembled
     * @param address an address at which the bytes are located. This can be used for an address prefix per line of disassembly.
     * @return the disassembly. This will be of length 0 if the runtime does not support disassembling.
     */
    String disassemble(byte[] code, long address);

    /**
     * Returns the disassembly of the given code bytes. Used for debugging purposes only.
     *
     * @param targetMethod the {@link CiTargetMethod} containing the code bytes that should be disassembled
     * @return the disassembly. This will be of length 0 if the runtime does not support disassembling.
     */
    String disassemble(CiTargetMethod targetMethod);

    /**
     * Returns the disassembly of the given method in a {@code javap}-like format.
     * Used for debugging purposes only.
     *
     * @param method the method that should be disassembled
     * @return the disassembly. This will be of length 0 if the runtime does not support disassembling.
     */
    String disassemble(RiMethod method);

    /**
     * Registers the given global stub and returns an object that can be used to identify it in the relocation
     * information.
     *
     * @param targetMethod the target method representing the code of the global stub
     * @param name the name of the stub, used for debugging purposes only
     * @return the identification object
     */
    Object registerGlobalStub(CiTargetMethod targetMethod, String name);

    /**
     * Returns the RiType object representing the base type for the given kind.
     */
    RiType asRiType(CiKind kind);

    /**
     * Returns the type of the given constant object.
     *
     * @return {@code null} if {@code constant.isNull() || !constant.kind.isObject()}
     */
    RiType getTypeOf(CiConstant constant);

    /**
     * Returns true if the given type is a subtype of java/lang/Throwable.
     */
    boolean isExceptionType(RiType type);

    /**
     * Returns the runtime interface representation of the given Java method object.
     *
     * @param javaMethod the Java method object
     * @return the runtime interface representation of {@code javaMethod}
     */
    RiMethod getRiMethod(Method javaMethod);

    /**
     * Returns the runtime interface representation of the given Java constructor object.
     *
     * @param javaConstructor the Java constructor object
     * @return the runtime interface representation {@code javaConstructor}
     */
    RiMethod getRiMethod(Constructor<?> javaConstructor);

    /**
     * Returns the runtime interface representation of the given Java field object.
     *
     * @param javaField the Java field object
     * @return the runtime interface representation of {@code javaField}
     */
    RiField getRiField(Field javaField);

    /**
     * Gets the {@linkplain RiSnippets snippets} provided by the runtime.
     */
    RiSnippets getSnippets();

    /**
     * Attempts to compile-time evaluate or "fold" a call to a given method. A foldable method is a pure function
     * that has no side effects. Such methods can be executed via reflection when all their inputs are constants,
     * and the resulting value is substituted for the method call.
     *
     * @param method the compiler interface method for which folding is being requested
     * @param args the arguments to the call
     * @return the result of the folding or {@code null} if no folding occurred
     */
    CiConstant invoke(RiMethod method, CiMethodInvokeArguments args);

    /**
     * Attempts to compile-time evaluate or "fold" a bytecode operation that involves {@linkplain CiKind#Word word} types.
     *
     * @param opcode the bytecode operation to perform
     * @param args the inputs to the operation
     * @return the result of folding the operation if it is foldable, {@code null} otherwise
     */
    CiConstant foldWordOperation(int opcode, CiMethodInvokeArguments args);

    /**
     * Used by the canonicalizer to compare objects, since a given runtime might not want to expose the real objects to the compiler.
     *
     * @return true if the two parameters represent the same runtime object, false otherwise
     */
    boolean areConstantObjectsEqual(CiConstant x, CiConstant y);

    /**
     * Gets the register configuration to use when compiling a given method.
     *
     * @param method the top level method of a compilation
     */
    RiRegisterConfig getRegisterConfig(RiMethod method);

    /**
     * Custom area on the stack of each compiled method that the VM can use for its own purposes.
     * @return the size of the custom area in bytes
     */
    int getCustomStackAreaSize();

    /**
     * Determines if this runtime wants {@link System#arraycopy} and {@link Arrays#copyOf} intrinsified.
     */
    boolean supportsArrayIntrinsics();

    /**
     * Gets the length of the array that is wrapped in a CiConstant object.
     */
    int getArrayLength(CiConstant array);

    /**
     * Converts the given CiConstant object to a object.
     *
     * @return {@code null} if the conversion is not possible <b>OR</b> {@code c.isNull() == true}
     */
    Object asJavaObject(CiConstant c);

    /**
     * Converts the given CiConstant object to a {@link Class} object.
     *
     * @return {@code null} if the conversion is not possible.
     */
    Class<?> asJavaClass(CiConstant c);
}

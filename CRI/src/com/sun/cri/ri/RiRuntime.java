/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.cri.ri;

import java.io.*;
import java.lang.reflect.*;

import com.sun.cri.ci.*;

/**
 * Encapsulates the main functionality of the runtime for the compiler, including access
 * to constant pools, OSR frames, inlining requirements, and runtime calls such as checkcast.
 *
 * @author Ben L. Titzer
 * @author Thomas Wuerthinger
 */
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
     * Offset of the lock within the lock object.
     *
     * @return the offset in bytes
     */
    int basicObjectLockOffsetInBytes();

    /**
     * Size of a lock object.
     *
     * @return the size in bytes
     */
    int sizeofBasicObjectLock();

    /**
     * The offset of the normal entry to the code. The compiler inserts NOP instructions to satisfy this constraint.
     *
     * @return the code offset in bytes
     */
    int codeOffset();

    /**
     * If the VM is using multiple compilers with different calling conventions, then each calling convention will have
     * a designated offset in the compiled code of a callee. If necessary, there will be code at these special
     * offsets that moves the outgoing arguments of the caller to the locations expected by the callee.
     * This method emits the adapter code if it is required.
     *
     * TODO: Parameterize this method with the calling convention in use by the code currently being compiled.
     *
     * @param method the callee method being compiled that may need an adapter code prologue
     * @param out where the prologue code (if any) will be emitted
     */
    void codePrologue(RiMethod method, OutputStream out);

    /**
     * Returns the disassembly of the given code bytes. Used for debugging purposes only.
     *
     * @param code the code bytes that should be disassembled
     * @return the disassembly as a String object
     */
    String disassemble(byte[] code);

    /**
     * Returns the disassembly of the given code bytes. Used for debugging purposes only.
     *
     * @param targetMethod the {@link CiTargetMethod} containing the code bytes that should be disassembled
     * @return the disassembly as a String object
     */
    String disassemble(CiTargetMethod targetMethod);

    /**
     * Returns the disassembly of the given method in a {@code javap}-like format.
     * Used for debugging purposes only.
     *
     * @param method the method that should be disassembled
     * @return the disassembly as a String object
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
    Object registerTargetMethod(CiTargetMethod targetMethod, String name);

    /**
     * Returns the runtime interface representation of the given Java class object.
     *
     * @param javaClass the Java class object
     * @return the runtime interface representation
     */
    RiType getRiType(Class<?> javaClass);

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
     * The {@link CiUtil#invoke(Method, CiMethodInvokeArguments)} utility can be used to implement this method.
     * 
     * @param method the compiler interface method for which folding is being requested
     * @param the arguments to the call
     * @return the result of the folding or {@code null} if no folding occurred
     */
    CiConstant invoke(RiMethod method, CiMethodInvokeArguments args);
    
    /**
     * Attempts to compile-time evaluate or "fold" a bytecode operation that involves {@linkplain CiKind#Word word} types.
     *
     * @param opcode the bytecode operation to perform
     * @param the inputs to the operation
     * @return the result of folding the operation if it is foldable, {@code null} otherwise
     */
    CiConstant foldWordOperation(int opcode, CiMethodInvokeArguments args);
}

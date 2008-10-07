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
/*VCSID=6a3b2aa8-703c-4045-bd20-d19265be8372*/
package com.sun.max.vm.template.source;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.template.*;
import com.sun.max.vm.type.*;


/**
 * Template sources for the invoke bytecode instructions where the methods to be invoked are resolved.
 * <p>
 * The calling convention is such that the part of the callee's frame that contains the incoming arguments of the method
 * is the top of the stack of the caller. The rest of the frame is built on method entry. Thus, the template for a
 * method invocation doesn't need to marshal the arguments. We can't just have templates compiled from standard method
 * invocations as the compiler mixes instructions for arguments passing and method invocation. So we have to write
 * template such that the call is explicitly made (using the Call SpecialBuiltin). Further, we need a template for each
 * of the four kinds of returned result (void, one word, two words, a reference).
 * <p>
 * For methods with a static binding (e.g., methods invoked via invokestatic or invokespecial), we just need to issue a
 * call. Thus a template for these bytecode is a single call instruction. A template resulting in this can be achieved
 * by invoke a parameterless static method of an initialized class. We generate these templates for completion, although
 * in practice a JIT might be better off generating the call instruction directly.
 * <p>
 * For dynamic method, the receiver is needed and method dispatch need to be generated. For the template, we pick an
 * object at an arbitrary position on the expression stack to be the receiver, and rely on the optimizing compiler to
 * generate dependency information about the constant value used as offset to read off the expression stack. JIT
 * compilers just have to modify this offset using the appropriate instruction modifier from the generated template.
 * Similarly, JIT compilers have to customized the virtual table index / itable serial identifier. Note that we use a
 * parameter-less void method in the expression performing dynamic method selection, regardless of the number of
 * parameters or of the kind of return value the template is to be used for.
 *
 * @author Laurent Daynes
 */
@TEMPLATE(resolved = TemplateChooser.Resolved.YES)
public final class ResolvedInvokeTemplateSource {

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKEVIRTUAL, kind = KindEnum.VOID)
    public static void invokevirtual(int vTableIndex, int receiverStackIndex) {
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = ObjectAccess.readHub(receiver).getWord(vTableIndex).asAddress();
        JitStackFrameOperation.indirectCallVoid(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKEVIRTUAL, kind = KindEnum.FLOAT)
    public static void invokevirtualReturnFloat(int vTableIndex, int receiverStackIndex) {
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = ObjectAccess.readHub(receiver).getWord(vTableIndex).asAddress();
        JitStackFrameOperation.indirectCallFloat(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKEVIRTUAL, kind = KindEnum.LONG)
    public static void invokevirtualLong(int vTableIndex, int receiverStackIndex) {
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = ObjectAccess.readHub(receiver).getWord(vTableIndex).asAddress();
        JitStackFrameOperation.indirectCallLong(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKEVIRTUAL, kind = KindEnum.DOUBLE)
    public static void invokevirtualDouble(int vTableIndex, int receiverStackIndex) {
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = ObjectAccess.readHub(receiver).getWord(vTableIndex).asAddress();
        JitStackFrameOperation.indirectCallDouble(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKEVIRTUAL, kind = KindEnum.WORD)
    public static void invokevirtualWord(int vTableIndex, int receiverStackIndex) {
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = ObjectAccess.readHub(receiver).getWord(vTableIndex).asAddress();
        JitStackFrameOperation.indirectCallWord(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKEINTERFACE, kind = KindEnum.VOID)
    public static void invokeinterface(InterfaceMethodActor interfaceMethodActor, int receiverStackIndex) {
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = MethodSelectionSnippet.SelectInterfaceMethod.selectInterfaceMethod(receiver, interfaceMethodActor).asAddress();
        JitStackFrameOperation.indirectCallVoid(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKEINTERFACE, kind = KindEnum.FLOAT)
    public static void invokeinterfaceFloat(InterfaceMethodActor interfaceMethodActor, int receiverStackIndex) {
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = MethodSelectionSnippet.SelectInterfaceMethod.selectInterfaceMethod(receiver, interfaceMethodActor).asAddress();
        JitStackFrameOperation.indirectCallFloat(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKEINTERFACE, kind = KindEnum.LONG)
    public static void invokeinterfaceLong(InterfaceMethodActor interfaceMethodActor, int receiverStackIndex) {
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = MethodSelectionSnippet.SelectInterfaceMethod.selectInterfaceMethod(receiver, interfaceMethodActor).asAddress();
        JitStackFrameOperation.indirectCallLong(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKEINTERFACE, kind = KindEnum.DOUBLE)
    public static void invokeinterfaceDouble(InterfaceMethodActor interfaceMethodActor, int receiverStackIndex) {
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = MethodSelectionSnippet.SelectInterfaceMethod.selectInterfaceMethod(receiver, interfaceMethodActor).asAddress();
        JitStackFrameOperation.indirectCallDouble(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKEINTERFACE, kind = KindEnum.WORD)
    public static void invokeinterfaceWord(InterfaceMethodActor interfaceMethodActor, int receiverStackIndex) {
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = MethodSelectionSnippet.SelectInterfaceMethod.selectInterfaceMethod(receiver, interfaceMethodActor).asAddress();
        JitStackFrameOperation.indirectCallWord(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKESPECIAL, kind = KindEnum.VOID)
    public static void invokespecial() {
        JitStackFrameOperation.directCallVoid();
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKESPECIAL, kind = KindEnum.FLOAT)
    public static void invokespecialReturnSingleSlot() {
        JitStackFrameOperation.directCallFloat();
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKESPECIAL, kind = KindEnum.LONG)
    public static void invokespecialLong() {
        JitStackFrameOperation.directCallLong();
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKESPECIAL, kind = KindEnum.DOUBLE)
    public static void invokespecialDouble() {
        JitStackFrameOperation.directCallDouble();
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKESPECIAL, kind = KindEnum.WORD)
    public static void invokespecialWord() {
        JitStackFrameOperation.directCallWord();
    }
}

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


package com.sun.max.vm.template.source;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.instrument.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.template.*;
import com.sun.max.vm.type.*;


/**
 * Templates for invokevirutal and invokeinterface that contains instrumentation.
 *
 * @See ResolvedInvokeTemplateSources.
 * @author Yi Guo
 * @author Aziz Ghuloum
 */
@TEMPLATE(resolved = TemplateChooser.Resolved.YES, instrumented = TemplateChooser.Instrumented.YES)
public final class InstrumentedInvokeTemplateSource {

    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKEVIRTUAL, kind = KindEnum.VOID)
    public static void invokevirtual(int vTableIndex, int receiverStackIndex, MethodInstrumentation.CallCounterTable table) {
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = selectVirtualMethod(receiver, vTableIndex, table);
        JitStackFrameOperation.indirectCallVoid(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKEVIRTUAL, kind = KindEnum.FLOAT)
    public static void invokevirtualReturnFloat(int vTableIndex, int receiverStackIndex, MethodInstrumentation.CallCounterTable table) {
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = selectVirtualMethod(receiver, vTableIndex, table);
        JitStackFrameOperation.indirectCallFloat(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKEVIRTUAL, kind = KindEnum.LONG)
    public static void invokevirtualLong(int vTableIndex, int receiverStackIndex, MethodInstrumentation.CallCounterTable table) {
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = selectVirtualMethod(receiver, vTableIndex, table);
        JitStackFrameOperation.indirectCallLong(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKEVIRTUAL, kind = KindEnum.DOUBLE)
    public static void invokevirtualDouble(int vTableIndex, int receiverStackIndex, MethodInstrumentation.CallCounterTable table) {
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = selectVirtualMethod(receiver, vTableIndex, table);
        JitStackFrameOperation.indirectCallDouble(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKEVIRTUAL, kind = KindEnum.WORD)
    public static void invokevirtualWord(int vTableIndex, int receiverStackIndex, MethodInstrumentation.CallCounterTable table) {
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = selectVirtualMethod(receiver, vTableIndex, table);
        JitStackFrameOperation.indirectCallWord(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }


    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKEINTERFACE, kind = KindEnum.VOID)
    public static void invokeinterface(InterfaceMethodActor interfaceMethodActor, int receiverStackIndex, MethodInstrumentation.CallCounterTable table) {
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = selectInterfaceMethod(receiver, interfaceMethodActor, table);
        JitStackFrameOperation.indirectCallVoid(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKEINTERFACE, kind = KindEnum.FLOAT)
    public static void invokeinterfaceFloat(InterfaceMethodActor interfaceMethodActor, int receiverStackIndex, MethodInstrumentation.CallCounterTable table) {
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = selectInterfaceMethod(receiver, interfaceMethodActor, table);
        JitStackFrameOperation.indirectCallFloat(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKEINTERFACE, kind = KindEnum.LONG)
    public static void invokeinterfaceLong(InterfaceMethodActor interfaceMethodActor, int receiverStackIndex, MethodInstrumentation.CallCounterTable table) {
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = selectInterfaceMethod(receiver, interfaceMethodActor, table);
        JitStackFrameOperation.indirectCallLong(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKEINTERFACE, kind = KindEnum.DOUBLE)
    public static void invokeinterfaceDouble(InterfaceMethodActor interfaceMethodActor, int receiverStackIndex, MethodInstrumentation.CallCounterTable table) {
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = selectInterfaceMethod(receiver, interfaceMethodActor, table);
        JitStackFrameOperation.indirectCallDouble(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKEINTERFACE, kind = KindEnum.WORD)
    public static void invokeinterfaceWord(InterfaceMethodActor interfaceMethodActor, int receiverStackIndex, MethodInstrumentation.CallCounterTable table) {
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = selectInterfaceMethod(receiver, interfaceMethodActor, table);
        JitStackFrameOperation.indirectCallWord(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @INLINE
    private static Address selectVirtualMethod(final Object receiver, int vTableIndex, MethodInstrumentation.CallCounterTable table) {
        final Hub hub = ObjectAccess.readHub(receiver);
        final Address entryPoint = hub.getWord(vTableIndex).asAddress();
        table.record(hub);
        return entryPoint;
    }

    @INLINE
    private static Address selectInterfaceMethod(final Object receiver, InterfaceMethodActor interfaceMethodActor, MethodInstrumentation.CallCounterTable table) {
        final Hub hub = ObjectAccess.readHub(receiver);
        final Address entryPoint = MethodSelectionSnippet.SelectInterfaceMethod.selectInterfaceMethod(receiver, interfaceMethodActor).asAddress();
        table.record(hub);
        return entryPoint;
    }
}

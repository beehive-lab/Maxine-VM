/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.t1x.vma;

import static com.sun.max.vm.t1x.T1XFrameOps.*;
import static com.sun.max.vm.t1x.T1XRuntime.*;
import static com.sun.max.vm.t1x.T1XTemplateTag.*;
import static com.sun.max.vm.t1x.T1XTemplateSource.*;
import static com.oracle.max.vm.ext.vma.run.java.VMAJavaRunScheme.*;

import com.oracle.max.vm.ext.vma.runtime.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.profile.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.t1x.T1XRuntime;
import com.sun.max.vm.t1x.T1X_TEMPLATE;
import com.sun.max.vm.type.*;

/**
 * Template source for after advice (where available).
 */
public class VMAdviceAfterTemplateSource {

    // BEGIN GENERATED CODE

    // GENERATED -- EDIT AND RUN VMAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKEVIRTUAL$float)
    public static void invokevirtualFloat(ResolutionGuard.InPool guard, int receiverStackIndex) {
        Object receiver = peekObject(receiverStackIndex);
        Address entryPoint = resolveAndSelectVirtualMethod(receiver, guard, receiverStackIndex);
        final float result = indirectCallFloat(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterInvokeVirtual(receiver, -1);
        }
        pushFloat(result);
    }

    // GENERATED -- EDIT AND RUN VMAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKEVIRTUAL$float$resolved)
    public static void invokevirtualFloat(int vTableIndex, int receiverStackIndex) {
        Object receiver = peekObject(receiverStackIndex);
        Address entryPoint = ObjectAccess.readHub(receiver).getWord(vTableIndex).asAddress();
        final float result = indirectCallFloat(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterInvokeVirtual(receiver, vTableIndex);
        }
        pushFloat(result);
    }

    // GENERATED -- EDIT AND RUN VMAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKEVIRTUAL$float$instrumented)
    public static void invokevirtualFloat(int vTableIndex, int receiverStackIndex, MethodProfile mpo, int mpoIndex) {
        Object receiver = peekObject(receiverStackIndex);
        Address entryPoint = selectVirtualMethod(receiver, vTableIndex, mpo, mpoIndex);
        final float result = indirectCallFloat(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterInvokeVirtual(receiver, vTableIndex);
        }
        pushFloat(result);
    }

    // GENERATED -- EDIT AND RUN VMAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKEINTERFACE$float)
    public static void invokeinterfaceFloat(ResolutionGuard.InPool guard, int receiverStackIndex) {
        Object receiver = peekObject(receiverStackIndex);
        Address entryPoint = resolveAndSelectInterfaceMethod(guard, receiver);
        final float result = indirectCallFloat(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterInvokeInterface(receiver, -1);
        }
        pushFloat(result);
    }

    // GENERATED -- EDIT AND RUN VMAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKEINTERFACE$float$resolved)
    public static void invokeinterfaceFloat(InterfaceMethodActor interfaceMethodActor, int receiverStackIndex) {
        Object receiver = peekObject(receiverStackIndex);
        Address entryPoint = Snippets.selectInterfaceMethod(receiver, interfaceMethodActor).asAddress();
        final float result = indirectCallFloat(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterInvokeInterface(receiver, -1);
        }
        pushFloat(result);
    }

    // GENERATED -- EDIT AND RUN VMAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKEINTERFACE$float$instrumented)
    public static void invokeinterfaceFloat(InterfaceMethodActor interfaceMethodActor, int receiverStackIndex, MethodProfile mpo, int mpoIndex) {
        Object receiver = peekObject(receiverStackIndex);
        Address entryPoint = Snippets.selectInterfaceMethod(receiver, interfaceMethodActor, mpo, mpoIndex);
        final float result = indirectCallFloat(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterInvokeInterface(receiver, -1);
        }
        pushFloat(result);
    }

    // GENERATED -- EDIT AND RUN VMAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKESPECIAL$float)
    public static void invokespecialFloat(ResolutionGuard.InPool guard, int receiverStackIndex) {
        Pointer receiver = peekWord(receiverStackIndex).asPointer();
        nullCheck(receiver);
        final float result = indirectCallFloat(resolveSpecialMethod(guard), CallEntryPoint.OPTIMIZED_ENTRY_POINT);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterInvokeSpecial(Reference.fromOrigin(receiver).toJava(), -1);
        }
        pushFloat(result);
    }

    // GENERATED -- EDIT AND RUN VMAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKESPECIAL$float$resolved)
    public static void invokespecialFloat(int receiverStackIndex) {
        Pointer receiver = peekWord(receiverStackIndex).asPointer();
        nullCheck(receiver);
        final float result = directCallFloat();
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterInvokeSpecial(Reference.fromOrigin(receiver).toJava(), -1);
        }
        pushFloat(result);
    }

    // GENERATED -- EDIT AND RUN VMAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKESTATIC$float)
    public static void invokestaticFloat(ResolutionGuard.InPool guard) {
        final float result = indirectCallFloat(resolveStaticMethod(guard), CallEntryPoint.OPTIMIZED_ENTRY_POINT);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterInvokeStatic(null, -1);
        }
        pushFloat(result);
    }

    // GENERATED -- EDIT AND RUN VMAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKESTATIC$float$init)
    public static void invokestaticFloat() {
        final float result = directCallFloat();
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterInvokeStatic(null, -1);
        }
        pushFloat(result);
    }

    // GENERATED -- EDIT AND RUN VMAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKEVIRTUAL$long)
    public static void invokevirtualLong(ResolutionGuard.InPool guard, int receiverStackIndex) {
        Object receiver = peekObject(receiverStackIndex);
        Address entryPoint = resolveAndSelectVirtualMethod(receiver, guard, receiverStackIndex);
        final long result = indirectCallLong(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterInvokeVirtual(receiver, -1);
        }
        pushLong(result);
    }

    // GENERATED -- EDIT AND RUN VMAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKEVIRTUAL$long$resolved)
    public static void invokevirtualLong(int vTableIndex, int receiverStackIndex) {
        Object receiver = peekObject(receiverStackIndex);
        Address entryPoint = ObjectAccess.readHub(receiver).getWord(vTableIndex).asAddress();
        final long result = indirectCallLong(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterInvokeVirtual(receiver, vTableIndex);
        }
        pushLong(result);
    }

    // GENERATED -- EDIT AND RUN VMAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKEVIRTUAL$long$instrumented)
    public static void invokevirtualLong(int vTableIndex, int receiverStackIndex, MethodProfile mpo, int mpoIndex) {
        Object receiver = peekObject(receiverStackIndex);
        Address entryPoint = selectVirtualMethod(receiver, vTableIndex, mpo, mpoIndex);
        final long result = indirectCallLong(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterInvokeVirtual(receiver, vTableIndex);
        }
        pushLong(result);
    }

    // GENERATED -- EDIT AND RUN VMAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKEINTERFACE$long)
    public static void invokeinterfaceLong(ResolutionGuard.InPool guard, int receiverStackIndex) {
        Object receiver = peekObject(receiverStackIndex);
        Address entryPoint = resolveAndSelectInterfaceMethod(guard, receiver);
        final long result = indirectCallLong(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterInvokeInterface(receiver, -1);
        }
        pushLong(result);
    }

    // GENERATED -- EDIT AND RUN VMAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKEINTERFACE$long$resolved)
    public static void invokeinterfaceLong(InterfaceMethodActor interfaceMethodActor, int receiverStackIndex) {
        Object receiver = peekObject(receiverStackIndex);
        Address entryPoint = Snippets.selectInterfaceMethod(receiver, interfaceMethodActor).asAddress();
        final long result = indirectCallLong(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterInvokeInterface(receiver, -1);
        }
        pushLong(result);
    }

    // GENERATED -- EDIT AND RUN VMAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKEINTERFACE$long$instrumented)
    public static void invokeinterfaceLong(InterfaceMethodActor interfaceMethodActor, int receiverStackIndex, MethodProfile mpo, int mpoIndex) {
        Object receiver = peekObject(receiverStackIndex);
        Address entryPoint = Snippets.selectInterfaceMethod(receiver, interfaceMethodActor, mpo, mpoIndex);
        final long result = indirectCallLong(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterInvokeInterface(receiver, -1);
        }
        pushLong(result);
    }

    // GENERATED -- EDIT AND RUN VMAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKESPECIAL$long)
    public static void invokespecialLong(ResolutionGuard.InPool guard, int receiverStackIndex) {
        Pointer receiver = peekWord(receiverStackIndex).asPointer();
        nullCheck(receiver);
        final long result = indirectCallLong(resolveSpecialMethod(guard), CallEntryPoint.OPTIMIZED_ENTRY_POINT);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterInvokeSpecial(Reference.fromOrigin(receiver).toJava(), -1);
        }
        pushLong(result);
    }

    // GENERATED -- EDIT AND RUN VMAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKESPECIAL$long$resolved)
    public static void invokespecialLong(int receiverStackIndex) {
        Pointer receiver = peekWord(receiverStackIndex).asPointer();
        nullCheck(receiver);
        final long result = directCallLong();
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterInvokeSpecial(Reference.fromOrigin(receiver).toJava(), -1);
        }
        pushLong(result);
    }

    // GENERATED -- EDIT AND RUN VMAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKESTATIC$long)
    public static void invokestaticLong(ResolutionGuard.InPool guard) {
        final long result = indirectCallLong(resolveStaticMethod(guard), CallEntryPoint.OPTIMIZED_ENTRY_POINT);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterInvokeStatic(null, -1);
        }
        pushLong(result);
    }

    // GENERATED -- EDIT AND RUN VMAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKESTATIC$long$init)
    public static void invokestaticLong() {
        final long result = directCallLong();
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterInvokeStatic(null, -1);
        }
        pushLong(result);
    }

    // GENERATED -- EDIT AND RUN VMAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKEVIRTUAL$double)
    public static void invokevirtualDouble(ResolutionGuard.InPool guard, int receiverStackIndex) {
        Object receiver = peekObject(receiverStackIndex);
        Address entryPoint = resolveAndSelectVirtualMethod(receiver, guard, receiverStackIndex);
        final double result = indirectCallDouble(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterInvokeVirtual(receiver, -1);
        }
        pushDouble(result);
    }

    // GENERATED -- EDIT AND RUN VMAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKEVIRTUAL$double$resolved)
    public static void invokevirtualDouble(int vTableIndex, int receiverStackIndex) {
        Object receiver = peekObject(receiverStackIndex);
        Address entryPoint = ObjectAccess.readHub(receiver).getWord(vTableIndex).asAddress();
        final double result = indirectCallDouble(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterInvokeVirtual(receiver, vTableIndex);
        }
        pushDouble(result);
    }

    // GENERATED -- EDIT AND RUN VMAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKEVIRTUAL$double$instrumented)
    public static void invokevirtualDouble(int vTableIndex, int receiverStackIndex, MethodProfile mpo, int mpoIndex) {
        Object receiver = peekObject(receiverStackIndex);
        Address entryPoint = selectVirtualMethod(receiver, vTableIndex, mpo, mpoIndex);
        final double result = indirectCallDouble(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterInvokeVirtual(receiver, vTableIndex);
        }
        pushDouble(result);
    }

    // GENERATED -- EDIT AND RUN VMAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKEINTERFACE$double)
    public static void invokeinterfaceDouble(ResolutionGuard.InPool guard, int receiverStackIndex) {
        Object receiver = peekObject(receiverStackIndex);
        Address entryPoint = resolveAndSelectInterfaceMethod(guard, receiver);
        final double result = indirectCallDouble(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterInvokeInterface(receiver, -1);
        }
        pushDouble(result);
    }

    // GENERATED -- EDIT AND RUN VMAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKEINTERFACE$double$resolved)
    public static void invokeinterfaceDouble(InterfaceMethodActor interfaceMethodActor, int receiverStackIndex) {
        Object receiver = peekObject(receiverStackIndex);
        Address entryPoint = Snippets.selectInterfaceMethod(receiver, interfaceMethodActor).asAddress();
        final double result = indirectCallDouble(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterInvokeInterface(receiver, -1);
        }
        pushDouble(result);
    }

    // GENERATED -- EDIT AND RUN VMAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKEINTERFACE$double$instrumented)
    public static void invokeinterfaceDouble(InterfaceMethodActor interfaceMethodActor, int receiverStackIndex, MethodProfile mpo, int mpoIndex) {
        Object receiver = peekObject(receiverStackIndex);
        Address entryPoint = Snippets.selectInterfaceMethod(receiver, interfaceMethodActor, mpo, mpoIndex);
        final double result = indirectCallDouble(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterInvokeInterface(receiver, -1);
        }
        pushDouble(result);
    }

    // GENERATED -- EDIT AND RUN VMAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKESPECIAL$double)
    public static void invokespecialDouble(ResolutionGuard.InPool guard, int receiverStackIndex) {
        Pointer receiver = peekWord(receiverStackIndex).asPointer();
        nullCheck(receiver);
        final double result = indirectCallDouble(resolveSpecialMethod(guard), CallEntryPoint.OPTIMIZED_ENTRY_POINT);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterInvokeSpecial(Reference.fromOrigin(receiver).toJava(), -1);
        }
        pushDouble(result);
    }

    // GENERATED -- EDIT AND RUN VMAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKESPECIAL$double$resolved)
    public static void invokespecialDouble(int receiverStackIndex) {
        Pointer receiver = peekWord(receiverStackIndex).asPointer();
        nullCheck(receiver);
        final double result = directCallDouble();
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterInvokeSpecial(Reference.fromOrigin(receiver).toJava(), -1);
        }
        pushDouble(result);
    }

    // GENERATED -- EDIT AND RUN VMAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKESTATIC$double)
    public static void invokestaticDouble(ResolutionGuard.InPool guard) {
        final double result = indirectCallDouble(resolveStaticMethod(guard), CallEntryPoint.OPTIMIZED_ENTRY_POINT);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterInvokeStatic(null, -1);
        }
        pushDouble(result);
    }

    // GENERATED -- EDIT AND RUN VMAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKESTATIC$double$init)
    public static void invokestaticDouble() {
        final double result = directCallDouble();
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterInvokeStatic(null, -1);
        }
        pushDouble(result);
    }

    // GENERATED -- EDIT AND RUN VMAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKEVIRTUAL$word)
    public static void invokevirtualWord(ResolutionGuard.InPool guard, int receiverStackIndex) {
        Object receiver = peekObject(receiverStackIndex);
        Address entryPoint = resolveAndSelectVirtualMethod(receiver, guard, receiverStackIndex);
        final Word result = indirectCallWord(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterInvokeVirtual(receiver, -1);
        }
        pushWord(result);
    }

    // GENERATED -- EDIT AND RUN VMAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKEVIRTUAL$word$resolved)
    public static void invokevirtualWord(int vTableIndex, int receiverStackIndex) {
        Object receiver = peekObject(receiverStackIndex);
        Address entryPoint = ObjectAccess.readHub(receiver).getWord(vTableIndex).asAddress();
        final Word result = indirectCallWord(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterInvokeVirtual(receiver, vTableIndex);
        }
        pushWord(result);
    }

    // GENERATED -- EDIT AND RUN VMAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKEVIRTUAL$word$instrumented)
    public static void invokevirtualWord(int vTableIndex, int receiverStackIndex, MethodProfile mpo, int mpoIndex) {
        Object receiver = peekObject(receiverStackIndex);
        Address entryPoint = selectVirtualMethod(receiver, vTableIndex, mpo, mpoIndex);
        final Word result = indirectCallWord(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterInvokeVirtual(receiver, vTableIndex);
        }
        pushWord(result);
    }

    // GENERATED -- EDIT AND RUN VMAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKEINTERFACE$word)
    public static void invokeinterfaceWord(ResolutionGuard.InPool guard, int receiverStackIndex) {
        Object receiver = peekObject(receiverStackIndex);
        Address entryPoint = resolveAndSelectInterfaceMethod(guard, receiver);
        final Word result = indirectCallWord(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterInvokeInterface(receiver, -1);
        }
        pushWord(result);
    }

    // GENERATED -- EDIT AND RUN VMAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKEINTERFACE$word$resolved)
    public static void invokeinterfaceWord(InterfaceMethodActor interfaceMethodActor, int receiverStackIndex) {
        Object receiver = peekObject(receiverStackIndex);
        Address entryPoint = Snippets.selectInterfaceMethod(receiver, interfaceMethodActor).asAddress();
        final Word result = indirectCallWord(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterInvokeInterface(receiver, -1);
        }
        pushWord(result);
    }

    // GENERATED -- EDIT AND RUN VMAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKEINTERFACE$word$instrumented)
    public static void invokeinterfaceWord(InterfaceMethodActor interfaceMethodActor, int receiverStackIndex, MethodProfile mpo, int mpoIndex) {
        Object receiver = peekObject(receiverStackIndex);
        Address entryPoint = Snippets.selectInterfaceMethod(receiver, interfaceMethodActor, mpo, mpoIndex);
        final Word result = indirectCallWord(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterInvokeInterface(receiver, -1);
        }
        pushWord(result);
    }

    // GENERATED -- EDIT AND RUN VMAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKESPECIAL$word)
    public static void invokespecialWord(ResolutionGuard.InPool guard, int receiverStackIndex) {
        Pointer receiver = peekWord(receiverStackIndex).asPointer();
        nullCheck(receiver);
        final Word result = indirectCallWord(resolveSpecialMethod(guard), CallEntryPoint.OPTIMIZED_ENTRY_POINT);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterInvokeSpecial(Reference.fromOrigin(receiver).toJava(), -1);
        }
        pushWord(result);
    }

    // GENERATED -- EDIT AND RUN VMAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKESPECIAL$word$resolved)
    public static void invokespecialWord(int receiverStackIndex) {
        Pointer receiver = peekWord(receiverStackIndex).asPointer();
        nullCheck(receiver);
        final Word result = directCallWord();
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterInvokeSpecial(Reference.fromOrigin(receiver).toJava(), -1);
        }
        pushWord(result);
    }

    // GENERATED -- EDIT AND RUN VMAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKESTATIC$word)
    public static void invokestaticWord(ResolutionGuard.InPool guard) {
        final Word result = indirectCallWord(resolveStaticMethod(guard), CallEntryPoint.OPTIMIZED_ENTRY_POINT);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterInvokeStatic(null, -1);
        }
        pushWord(result);
    }

    // GENERATED -- EDIT AND RUN VMAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKESTATIC$word$init)
    public static void invokestaticWord() {
        final Word result = directCallWord();
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterInvokeStatic(null, -1);
        }
        pushWord(result);
    }

    // GENERATED -- EDIT AND RUN VMAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKEVIRTUAL$void)
    public static void invokevirtualVoid(ResolutionGuard.InPool guard, int receiverStackIndex) {
        Object receiver = peekObject(receiverStackIndex);
        Address entryPoint = resolveAndSelectVirtualMethod(receiver, guard, receiverStackIndex);
        indirectCallVoid(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterInvokeVirtual(receiver, -1);
        }
    }

    // GENERATED -- EDIT AND RUN VMAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKEVIRTUAL$void$resolved)
    public static void invokevirtualVoid(int vTableIndex, int receiverStackIndex) {
        Object receiver = peekObject(receiverStackIndex);
        Address entryPoint = ObjectAccess.readHub(receiver).getWord(vTableIndex).asAddress();
        indirectCallVoid(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterInvokeVirtual(receiver, vTableIndex);
        }
    }

    // GENERATED -- EDIT AND RUN VMAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKEVIRTUAL$void$instrumented)
    public static void invokevirtualVoid(int vTableIndex, int receiverStackIndex, MethodProfile mpo, int mpoIndex) {
        Object receiver = peekObject(receiverStackIndex);
        Address entryPoint = selectVirtualMethod(receiver, vTableIndex, mpo, mpoIndex);
        indirectCallVoid(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterInvokeVirtual(receiver, vTableIndex);
        }
    }

    // GENERATED -- EDIT AND RUN VMAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKEINTERFACE$void)
    public static void invokeinterfaceVoid(ResolutionGuard.InPool guard, int receiverStackIndex) {
        Object receiver = peekObject(receiverStackIndex);
        Address entryPoint = resolveAndSelectInterfaceMethod(guard, receiver);
        indirectCallVoid(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterInvokeInterface(receiver, -1);
        }
    }

    // GENERATED -- EDIT AND RUN VMAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKEINTERFACE$void$resolved)
    public static void invokeinterfaceVoid(InterfaceMethodActor interfaceMethodActor, int receiverStackIndex) {
        Object receiver = peekObject(receiverStackIndex);
        Address entryPoint = Snippets.selectInterfaceMethod(receiver, interfaceMethodActor).asAddress();
        indirectCallVoid(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterInvokeInterface(receiver, -1);
        }
    }

    // GENERATED -- EDIT AND RUN VMAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKEINTERFACE$void$instrumented)
    public static void invokeinterfaceVoid(InterfaceMethodActor interfaceMethodActor, int receiverStackIndex, MethodProfile mpo, int mpoIndex) {
        Object receiver = peekObject(receiverStackIndex);
        Address entryPoint = Snippets.selectInterfaceMethod(receiver, interfaceMethodActor, mpo, mpoIndex);
        indirectCallVoid(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterInvokeInterface(receiver, -1);
        }
    }

    // GENERATED -- EDIT AND RUN VMAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKESPECIAL$void)
    public static void invokespecialVoid(ResolutionGuard.InPool guard, int receiverStackIndex) {
        Pointer receiver = peekWord(receiverStackIndex).asPointer();
        nullCheck(receiver);
        indirectCallVoid(resolveSpecialMethod(guard), CallEntryPoint.OPTIMIZED_ENTRY_POINT);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterInvokeSpecial(Reference.fromOrigin(receiver).toJava(), -1);
        }
    }

    // GENERATED -- EDIT AND RUN VMAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKESPECIAL$void$resolved)
    public static void invokespecialVoid(int receiverStackIndex) {
        Pointer receiver = peekWord(receiverStackIndex).asPointer();
        nullCheck(receiver);
        directCallVoid();
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterInvokeSpecial(Reference.fromOrigin(receiver).toJava(), -1);
        }
    }

    // GENERATED -- EDIT AND RUN VMAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKESTATIC$void)
    public static void invokestaticVoid(ResolutionGuard.InPool guard) {
        indirectCallVoid(resolveStaticMethod(guard), CallEntryPoint.OPTIMIZED_ENTRY_POINT);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterInvokeStatic(null, -1);
        }
    }

    // GENERATED -- EDIT AND RUN VMAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKESTATIC$void$init)
    public static void invokestaticVoid() {
        directCallVoid();
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterInvokeStatic(null, -1);
        }
    }

    // GENERATED -- EDIT AND RUN VMAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(NEW)
    public static void new_(ResolutionGuard arg) {
        Object object = resolveClassForNewAndCreate(arg);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterNew(object);
        }
        pushObject(object);
    }

    // GENERATED -- EDIT AND RUN VMAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(NEW$init)
    public static void new_(ClassActor arg) {
        Object object = createTupleOrHybrid(arg);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterNew(object);
        }
        pushObject(object);
    }

    // GENERATED -- EDIT AND RUN VMAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(NEWARRAY)
    public static void newarray(Kind<?> kind) {
        int length = peekInt(0);
        Object array = createPrimitiveArray(kind, length);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterNewArray(array, length);
        }
        pokeObject(0, array);
    }

    // GENERATED -- EDIT AND RUN VMAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(ANEWARRAY)
    public static void anewarray(ResolutionGuard guard) {
        ArrayClassActor<?> arrayClassActor = UnsafeCast.asArrayClassActor(Snippets.resolveArrayClass(guard));
        int length = peekInt(0);
        Object array = T1XRuntime.createReferenceArray(arrayClassActor, length);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterNewArray(array, length);
        }
        pokeObject(0, array);
    }

    // GENERATED -- EDIT AND RUN VMAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(ANEWARRAY$resolved)
    public static void anewarray(ArrayClassActor<?> arrayClassActor) {
        int length = peekInt(0);
        Object array = T1XRuntime.createReferenceArray(arrayClassActor, length);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterNewArray(array, length);
        }
        pokeObject(0, array);
    }

    // GENERATED -- EDIT AND RUN VMAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(MULTIANEWARRAY)
    public static void multianewarray(ResolutionGuard guard, int[] lengthsShared) {
        ClassActor arrayClassActor = Snippets.resolveClass(guard);
        // Need to use an unsafe cast to remove the checkcast inserted by javac as that
        // causes this template to have a reference literal in its compiled form.
        int[] lengths = UnsafeCast.asIntArray(cloneArray(lengthsShared));
        int numberOfDimensions = lengths.length;

        for (int i = 1; i <= numberOfDimensions; i++) {
            int length = popInt();
            checkArrayDimension(length);
            ArrayAccess.setInt(lengths, numberOfDimensions - i, length);
        }

        Object array = Snippets.createMultiReferenceArray(arrayClassActor, lengths);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterMultiNewArray(array, lengths);
        }
        pushObject(array);
    }

    // GENERATED -- EDIT AND RUN VMAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(MULTIANEWARRAY$resolved)
    public static void multianewarray(ArrayClassActor<?> arrayClassActor, int[] lengthsShared) {
        // Need to use an unsafe cast to remove the checkcast inserted by javac as that
        // causes this template to have a reference literal in its compiled form.
        int[] lengths = UnsafeCast.asIntArray(cloneArray(lengthsShared));
        int numberOfDimensions = lengths.length;

        for (int i = 1; i <= numberOfDimensions; i++) {
            int length = popInt();
            checkArrayDimension(length);
            ArrayAccess.setInt(lengths, numberOfDimensions - i, length);
        }

        Object array = Snippets.createMultiReferenceArray(arrayClassActor, lengths);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterMultiNewArray(array, lengths);
        }
        pushObject(array);
    }


}

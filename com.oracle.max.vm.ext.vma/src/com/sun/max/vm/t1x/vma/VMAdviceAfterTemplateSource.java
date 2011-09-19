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

import static com.oracle.max.vm.ext.t1x.T1XRuntime.*;
import static com.oracle.max.vm.ext.t1x.T1XTemplateTag.*;

import com.oracle.max.vm.ext.vma.run.java.*;
import com.oracle.max.vm.ext.vma.runtime.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.oracle.max.vm.ext.t1x.*;
import com.sun.max.vm.type.*;

/**
 * Template source for after advice (where available).
 */
public class VMAdviceAfterTemplateSource {

// START GENERATED CODE
    @T1X_TEMPLATE(NEW)
    public static Object new_(ResolutionGuard guard) {
        Object object = resolveClassForNewAndCreate(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseAfterNew(object);
        }
        return object;
    }

    @T1X_TEMPLATE(NEW$init)
    public static Object new_(ClassActor classActor) {
        Object object = createTupleOrHybrid(classActor);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseAfterNew(object);
        }
        return object;
    }

    @T1X_TEMPLATE(NEWARRAY)
    public static Object newarray(Kind<?> kind, @Slot(0) int length) {
        Object array = createPrimitiveArray(kind, length);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseAfterNewArray(array, length);
        }
        return array;
    }

    @T1X_TEMPLATE(ANEWARRAY)
    public static Object anewarray(ResolutionGuard arrayType, @Slot(0) int length) {
        ArrayClassActor<?> arrayClassActor = UnsafeCast.asArrayClassActor(Snippets.resolveArrayClass(arrayType));
        Object array = T1XRuntime.createReferenceArray(arrayClassActor, length);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseAfterNewArray(array, length);
        }
        return array;
    }

    @T1X_TEMPLATE(ANEWARRAY$resolved)
    public static Object anewarray(ArrayClassActor<?> arrayType, @Slot(0) int length) {
        ArrayClassActor<?> arrayClassActor = arrayType;
        Object array = T1XRuntime.createReferenceArray(arrayClassActor, length);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseAfterNewArray(array, length);
        }
        return array;
    }

    @T1X_TEMPLATE(MULTIANEWARRAY)
    public static Reference multianewarray(ResolutionGuard guard, int[] lengths) {
        ClassActor arrayClassActor = Snippets.resolveClass(guard);
        Object array = Snippets.createMultiReferenceArray(arrayClassActor, lengths);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseAfterMultiNewArray(array, lengths);
        }
        return Reference.fromJava(array);
    }

    @T1X_TEMPLATE(MULTIANEWARRAY$resolved)
    public static Reference multianewarray(ArrayClassActor<?> arrayClassActor, int[] lengths) {
        Object array = Snippets.createMultiReferenceArray(arrayClassActor, lengths);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseAfterMultiNewArray(array, lengths);
        }
        return Reference.fromJava(array);
    }

    @T1X_TEMPLATE(TRACE_METHOD_ENTRY)
    public static void traceMethodEntry(MethodActor methodActor, Object receiver) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseAfterMethodEntry(receiver, methodActor);
        }
    }

    @T1X_TEMPLATE(INVOKEVIRTUAL$adviseafter)
    public static void adviseAfterInvokeVirtual() {
        VMAStaticBytecodeAdvice.adviseAfterInvokeVirtual(VMAJavaRunScheme.loadReceiver(), VMAJavaRunScheme.loadMethodActor());
    }

    @T1X_TEMPLATE(INVOKEINTERFACE$adviseafter)
    public static void adviseAfterInvokeInterface() {
        VMAStaticBytecodeAdvice.adviseAfterInvokeInterface(VMAJavaRunScheme.loadReceiver(), VMAJavaRunScheme.loadMethodActor());
    }

    @T1X_TEMPLATE(INVOKESPECIAL$adviseafter)
    public static void adviseAfterInvokeSpecial() {
        VMAStaticBytecodeAdvice.adviseAfterInvokeSpecial(VMAJavaRunScheme.loadReceiver(), VMAJavaRunScheme.loadMethodActor());
    }

    @T1X_TEMPLATE(INVOKESTATIC$adviseafter)
    public static void adviseAfterInvokeStatic() {
        VMAStaticBytecodeAdvice.adviseAfterInvokeStatic(VMAJavaRunScheme.loadReceiver(), VMAJavaRunScheme.loadMethodActor());
    }

// END GENERATED CODE
}

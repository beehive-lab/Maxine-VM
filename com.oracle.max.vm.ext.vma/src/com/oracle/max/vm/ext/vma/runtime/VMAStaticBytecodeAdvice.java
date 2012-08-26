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
package com.oracle.max.vm.ext.vma.runtime;

import static com.oracle.max.vm.ext.vma.run.java.VMAJavaRunScheme.*;

import com.oracle.max.vm.ext.vma.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.actor.member.*;

/**
 * An facade that defines static methods that parallel those in {@BytecodeAdvice} for use
 * in T1X templates, where we do not want the overhead of virtual method dispatch.
 *
 * The implementation forwards the call to the {@link VMAdviceHandler} registered
 * with {@link VMAJavaRunScheme}, and also disables/enables advice generation around the call.
 *
 * The methods are automatically generated.
 */

public class VMAStaticBytecodeAdvice {

// START GENERATED CODE
// EDIT AND RUN VMAStaticBytecodeAdviceGenerator.main() TO MODIFY

    @NEVER_INLINE
    public static void adviseBeforeConstLoad(int arg1, long arg2) {
        disableAdvising();
        adviceHandler().adviseBeforeConstLoad(arg1, arg2);
        enableAdvising();
    }

    @NEVER_INLINE
    public static void adviseBeforeConstLoad(int arg1, Object arg2) {
        disableAdvising();
        adviceHandler().adviseBeforeConstLoad(arg1, arg2);
        enableAdvising();
    }

    @NEVER_INLINE
    public static void adviseBeforeConstLoad(int arg1, float arg2) {
        disableAdvising();
        adviceHandler().adviseBeforeConstLoad(arg1, arg2);
        enableAdvising();
    }

    @NEVER_INLINE
    public static void adviseBeforeConstLoad(int arg1, double arg2) {
        disableAdvising();
        adviceHandler().adviseBeforeConstLoad(arg1, arg2);
        enableAdvising();
    }

    @NEVER_INLINE
    public static void adviseBeforeLoad(int arg1, int arg2) {
        disableAdvising();
        adviceHandler().adviseBeforeLoad(arg1, arg2);
        enableAdvising();
    }

    @NEVER_INLINE
    public static void adviseBeforeArrayLoad(int arg1, Object arg2, int arg3) {
        disableAdvising();
        adviceHandler().adviseBeforeArrayLoad(arg1, arg2, arg3);
        enableAdvising();
    }

    @NEVER_INLINE
    public static void adviseBeforeStore(int arg1, int arg2, long arg3) {
        disableAdvising();
        adviceHandler().adviseBeforeStore(arg1, arg2, arg3);
        enableAdvising();
    }

    @NEVER_INLINE
    public static void adviseBeforeStore(int arg1, int arg2, float arg3) {
        disableAdvising();
        adviceHandler().adviseBeforeStore(arg1, arg2, arg3);
        enableAdvising();
    }

    @NEVER_INLINE
    public static void adviseBeforeStore(int arg1, int arg2, double arg3) {
        disableAdvising();
        adviceHandler().adviseBeforeStore(arg1, arg2, arg3);
        enableAdvising();
    }

    @NEVER_INLINE
    public static void adviseBeforeStore(int arg1, int arg2, Object arg3) {
        disableAdvising();
        adviceHandler().adviseBeforeStore(arg1, arg2, arg3);
        enableAdvising();
    }

    @NEVER_INLINE
    public static void adviseBeforeArrayStore(int arg1, Object arg2, int arg3, float arg4) {
        disableAdvising();
        adviceHandler().adviseBeforeArrayStore(arg1, arg2, arg3, arg4);
        enableAdvising();
    }

    @NEVER_INLINE
    public static void adviseBeforeArrayStore(int arg1, Object arg2, int arg3, long arg4) {
        disableAdvising();
        adviceHandler().adviseBeforeArrayStore(arg1, arg2, arg3, arg4);
        enableAdvising();
    }

    @NEVER_INLINE
    public static void adviseBeforeArrayStore(int arg1, Object arg2, int arg3, double arg4) {
        disableAdvising();
        adviceHandler().adviseBeforeArrayStore(arg1, arg2, arg3, arg4);
        enableAdvising();
    }

    @NEVER_INLINE
    public static void adviseBeforeArrayStore(int arg1, Object arg2, int arg3, Object arg4) {
        disableAdvising();
        adviceHandler().adviseBeforeArrayStore(arg1, arg2, arg3, arg4);
        enableAdvising();
    }

    @NEVER_INLINE
    public static void adviseBeforeStackAdjust(int arg1, int arg2) {
        disableAdvising();
        adviceHandler().adviseBeforeStackAdjust(arg1, arg2);
        enableAdvising();
    }

    @NEVER_INLINE
    public static void adviseBeforeOperation(int arg1, int arg2, long arg3, long arg4) {
        disableAdvising();
        adviceHandler().adviseBeforeOperation(arg1, arg2, arg3, arg4);
        enableAdvising();
    }

    @NEVER_INLINE
    public static void adviseBeforeOperation(int arg1, int arg2, float arg3, float arg4) {
        disableAdvising();
        adviceHandler().adviseBeforeOperation(arg1, arg2, arg3, arg4);
        enableAdvising();
    }

    @NEVER_INLINE
    public static void adviseBeforeOperation(int arg1, int arg2, double arg3, double arg4) {
        disableAdvising();
        adviceHandler().adviseBeforeOperation(arg1, arg2, arg3, arg4);
        enableAdvising();
    }

    @NEVER_INLINE
    public static void adviseBeforeConversion(int arg1, int arg2, float arg3) {
        disableAdvising();
        adviceHandler().adviseBeforeConversion(arg1, arg2, arg3);
        enableAdvising();
    }

    @NEVER_INLINE
    public static void adviseBeforeConversion(int arg1, int arg2, long arg3) {
        disableAdvising();
        adviceHandler().adviseBeforeConversion(arg1, arg2, arg3);
        enableAdvising();
    }

    @NEVER_INLINE
    public static void adviseBeforeConversion(int arg1, int arg2, double arg3) {
        disableAdvising();
        adviceHandler().adviseBeforeConversion(arg1, arg2, arg3);
        enableAdvising();
    }

    @NEVER_INLINE
    public static void adviseBeforeIf(int arg1, int arg2, int arg3, int arg4) {
        disableAdvising();
        adviceHandler().adviseBeforeIf(arg1, arg2, arg3, arg4);
        enableAdvising();
    }

    @NEVER_INLINE
    public static void adviseBeforeIf(int arg1, int arg2, Object arg3, Object arg4) {
        disableAdvising();
        adviceHandler().adviseBeforeIf(arg1, arg2, arg3, arg4);
        enableAdvising();
    }

    @NEVER_INLINE
    public static void adviseBeforeBytecode(int arg1, int arg2) {
        disableAdvising();
        adviceHandler().adviseBeforeBytecode(arg1, arg2);
        enableAdvising();
    }

    @NEVER_INLINE
    public static void adviseBeforeReturn(int arg1, double arg2) {
        disableAdvising();
        adviceHandler().adviseBeforeReturn(arg1, arg2);
        enableAdvising();
    }

    @NEVER_INLINE
    public static void adviseBeforeReturn(int arg1, long arg2) {
        disableAdvising();
        adviceHandler().adviseBeforeReturn(arg1, arg2);
        enableAdvising();
    }

    @NEVER_INLINE
    public static void adviseBeforeReturn(int arg1, float arg2) {
        disableAdvising();
        adviceHandler().adviseBeforeReturn(arg1, arg2);
        enableAdvising();
    }

    @NEVER_INLINE
    public static void adviseBeforeReturn(int arg1, Object arg2) {
        disableAdvising();
        adviceHandler().adviseBeforeReturn(arg1, arg2);
        enableAdvising();
    }

    @NEVER_INLINE
    public static void adviseBeforeReturn(int arg1) {
        disableAdvising();
        adviceHandler().adviseBeforeReturn(arg1);
        enableAdvising();
    }

    @NEVER_INLINE
    public static void adviseBeforeGetStatic(int arg1, Object arg2, int arg3) {
        disableAdvising();
        adviceHandler().adviseBeforeGetStatic(arg1, arg2, arg3);
        enableAdvising();
    }

    @NEVER_INLINE
    public static void adviseBeforePutStatic(int arg1, Object arg2, int arg3, Object arg4) {
        disableAdvising();
        adviceHandler().adviseBeforePutStatic(arg1, arg2, arg3, arg4);
        enableAdvising();
    }

    @NEVER_INLINE
    public static void adviseBeforePutStatic(int arg1, Object arg2, int arg3, double arg4) {
        disableAdvising();
        adviceHandler().adviseBeforePutStatic(arg1, arg2, arg3, arg4);
        enableAdvising();
    }

    @NEVER_INLINE
    public static void adviseBeforePutStatic(int arg1, Object arg2, int arg3, long arg4) {
        disableAdvising();
        adviceHandler().adviseBeforePutStatic(arg1, arg2, arg3, arg4);
        enableAdvising();
    }

    @NEVER_INLINE
    public static void adviseBeforePutStatic(int arg1, Object arg2, int arg3, float arg4) {
        disableAdvising();
        adviceHandler().adviseBeforePutStatic(arg1, arg2, arg3, arg4);
        enableAdvising();
    }

    @NEVER_INLINE
    public static void adviseBeforeGetField(int arg1, Object arg2, int arg3) {
        disableAdvising();
        adviceHandler().adviseBeforeGetField(arg1, arg2, arg3);
        enableAdvising();
    }

    @NEVER_INLINE
    public static void adviseBeforePutField(int arg1, Object arg2, int arg3, Object arg4) {
        disableAdvising();
        adviceHandler().adviseBeforePutField(arg1, arg2, arg3, arg4);
        enableAdvising();
    }

    @NEVER_INLINE
    public static void adviseBeforePutField(int arg1, Object arg2, int arg3, double arg4) {
        disableAdvising();
        adviceHandler().adviseBeforePutField(arg1, arg2, arg3, arg4);
        enableAdvising();
    }

    @NEVER_INLINE
    public static void adviseBeforePutField(int arg1, Object arg2, int arg3, long arg4) {
        disableAdvising();
        adviceHandler().adviseBeforePutField(arg1, arg2, arg3, arg4);
        enableAdvising();
    }

    @NEVER_INLINE
    public static void adviseBeforePutField(int arg1, Object arg2, int arg3, float arg4) {
        disableAdvising();
        adviceHandler().adviseBeforePutField(arg1, arg2, arg3, arg4);
        enableAdvising();
    }

    @NEVER_INLINE
    public static void adviseBeforeInvokeVirtual(int arg1, Object arg2, MethodActor arg3) {
        disableAdvising();
        adviceHandler().adviseBeforeInvokeVirtual(arg1, arg2, arg3);
        enableAdvising();
    }

    @NEVER_INLINE
    public static void adviseBeforeInvokeSpecial(int arg1, Object arg2, MethodActor arg3) {
        disableAdvising();
        adviceHandler().adviseBeforeInvokeSpecial(arg1, arg2, arg3);
        enableAdvising();
    }

    @NEVER_INLINE
    public static void adviseBeforeInvokeStatic(int arg1, Object arg2, MethodActor arg3) {
        disableAdvising();
        adviceHandler().adviseBeforeInvokeStatic(arg1, arg2, arg3);
        enableAdvising();
    }

    @NEVER_INLINE
    public static void adviseBeforeInvokeInterface(int arg1, Object arg2, MethodActor arg3) {
        disableAdvising();
        adviceHandler().adviseBeforeInvokeInterface(arg1, arg2, arg3);
        enableAdvising();
    }

    @NEVER_INLINE
    public static void adviseBeforeArrayLength(int arg1, Object arg2, int arg3) {
        disableAdvising();
        adviceHandler().adviseBeforeArrayLength(arg1, arg2, arg3);
        enableAdvising();
    }

    @NEVER_INLINE
    public static void adviseBeforeThrow(int arg1, Object arg2) {
        disableAdvising();
        adviceHandler().adviseBeforeThrow(arg1, arg2);
        enableAdvising();
    }

    @NEVER_INLINE
    public static void adviseBeforeCheckCast(int arg1, Object arg2, Object arg3) {
        disableAdvising();
        adviceHandler().adviseBeforeCheckCast(arg1, arg2, arg3);
        enableAdvising();
    }

    @NEVER_INLINE
    public static void adviseBeforeInstanceOf(int arg1, Object arg2, Object arg3) {
        disableAdvising();
        adviceHandler().adviseBeforeInstanceOf(arg1, arg2, arg3);
        enableAdvising();
    }

    @NEVER_INLINE
    public static void adviseBeforeMonitorEnter(int arg1, Object arg2) {
        disableAdvising();
        adviceHandler().adviseBeforeMonitorEnter(arg1, arg2);
        enableAdvising();
    }

    @NEVER_INLINE
    public static void adviseBeforeMonitorExit(int arg1, Object arg2) {
        disableAdvising();
        adviceHandler().adviseBeforeMonitorExit(arg1, arg2);
        enableAdvising();
    }

    @NEVER_INLINE
    public static void adviseAfterNew(int arg1, Object arg2) {
        disableAdvising();
        adviceHandler().adviseAfterNew(arg1, arg2);
        enableAdvising();
    }

    @NEVER_INLINE
    public static void adviseAfterNewArray(int arg1, Object arg2, int arg3) {
        disableAdvising();
        adviceHandler().adviseAfterNewArray(arg1, arg2, arg3);
        enableAdvising();
    }

    @NEVER_INLINE
    public static void adviseAfterMultiNewArray(int arg1, Object arg2, int[] arg3) {
        disableAdvising();
        adviceHandler().adviseAfterMultiNewArray(arg1, arg2, arg3);
        enableAdvising();
    }

    @NEVER_INLINE
    public static void adviseAfterMethodEntry(int arg1, Object arg2, MethodActor arg3) {
        disableAdvising();
        adviceHandler().adviseAfterMethodEntry(arg1, arg2, arg3);
        enableAdvising();
    }

// END GENERATED CODE
}

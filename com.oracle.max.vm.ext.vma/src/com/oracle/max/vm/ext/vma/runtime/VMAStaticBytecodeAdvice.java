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

    // BEGIN GENERATED CODE

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseBeforeConstLoad(long arg1) {
        disableAdvising();
        adviceHandler().adviseBeforeConstLoad(arg1);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseBeforeConstLoad(Object arg1) {
        disableAdvising();
        adviceHandler().adviseBeforeConstLoad(arg1);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseBeforeConstLoad(float arg1) {
        disableAdvising();
        adviceHandler().adviseBeforeConstLoad(arg1);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseBeforeConstLoad(double arg1) {
        disableAdvising();
        adviceHandler().adviseBeforeConstLoad(arg1);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseBeforeIPush(int arg1) {
        disableAdvising();
        adviceHandler().adviseBeforeIPush(arg1);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseBeforeLoad(int arg1) {
        disableAdvising();
        adviceHandler().adviseBeforeLoad(arg1);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseBeforeArrayLoad(Object arg1, int arg2) {
        disableAdvising();
        adviceHandler().adviseBeforeArrayLoad(arg1, arg2);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseBeforeStore(int arg1, long arg2) {
        disableAdvising();
        adviceHandler().adviseBeforeStore(arg1, arg2);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseBeforeStore(int arg1, float arg2) {
        disableAdvising();
        adviceHandler().adviseBeforeStore(arg1, arg2);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseBeforeStore(int arg1, double arg2) {
        disableAdvising();
        adviceHandler().adviseBeforeStore(arg1, arg2);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseBeforeStore(int arg1, Object arg2) {
        disableAdvising();
        adviceHandler().adviseBeforeStore(arg1, arg2);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseBeforeArrayStore(Object arg1, int arg2, float arg3) {
        disableAdvising();
        adviceHandler().adviseBeforeArrayStore(arg1, arg2, arg3);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseBeforeArrayStore(Object arg1, int arg2, long arg3) {
        disableAdvising();
        adviceHandler().adviseBeforeArrayStore(arg1, arg2, arg3);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseBeforeArrayStore(Object arg1, int arg2, double arg3) {
        disableAdvising();
        adviceHandler().adviseBeforeArrayStore(arg1, arg2, arg3);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseBeforeArrayStore(Object arg1, int arg2, Object arg3) {
        disableAdvising();
        adviceHandler().adviseBeforeArrayStore(arg1, arg2, arg3);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseBeforeStackAdjust(int arg1) {
        disableAdvising();
        adviceHandler().adviseBeforeStackAdjust(arg1);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseBeforeOperation(int arg1, long arg2, long arg3) {
        disableAdvising();
        adviceHandler().adviseBeforeOperation(arg1, arg2, arg3);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseBeforeOperation(int arg1, float arg2, float arg3) {
        disableAdvising();
        adviceHandler().adviseBeforeOperation(arg1, arg2, arg3);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseBeforeOperation(int arg1, double arg2, double arg3) {
        disableAdvising();
        adviceHandler().adviseBeforeOperation(arg1, arg2, arg3);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseBeforeIInc(int arg1, int arg2, int arg3) {
        disableAdvising();
        adviceHandler().adviseBeforeIInc(arg1, arg2, arg3);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseBeforeConversion(int arg1, long arg2) {
        disableAdvising();
        adviceHandler().adviseBeforeConversion(arg1, arg2);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseBeforeConversion(int arg1, float arg2) {
        disableAdvising();
        adviceHandler().adviseBeforeConversion(arg1, arg2);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseBeforeConversion(int arg1, double arg2) {
        disableAdvising();
        adviceHandler().adviseBeforeConversion(arg1, arg2);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseBeforeIf(int arg1, int arg2, int arg3) {
        disableAdvising();
        adviceHandler().adviseBeforeIf(arg1, arg2, arg3);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseBeforeIf(int arg1, Object arg2, Object arg3) {
        disableAdvising();
        adviceHandler().adviseBeforeIf(arg1, arg2, arg3);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseBeforeReturn(Object arg1) {
        disableAdvising();
        adviceHandler().adviseBeforeReturn(arg1);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseBeforeReturn(long arg1) {
        disableAdvising();
        adviceHandler().adviseBeforeReturn(arg1);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseBeforeReturn(float arg1) {
        disableAdvising();
        adviceHandler().adviseBeforeReturn(arg1);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseBeforeReturn(double arg1) {
        disableAdvising();
        adviceHandler().adviseBeforeReturn(arg1);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseBeforeReturn() {
        disableAdvising();
        adviceHandler().adviseBeforeReturn();
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseBeforeGetStatic(Object arg1, int arg2) {
        disableAdvising();
        adviceHandler().adviseBeforeGetStatic(arg1, arg2);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseBeforePutStatic(Object arg1, int arg2, double arg3) {
        disableAdvising();
        adviceHandler().adviseBeforePutStatic(arg1, arg2, arg3);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseBeforePutStatic(Object arg1, int arg2, long arg3) {
        disableAdvising();
        adviceHandler().adviseBeforePutStatic(arg1, arg2, arg3);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseBeforePutStatic(Object arg1, int arg2, float arg3) {
        disableAdvising();
        adviceHandler().adviseBeforePutStatic(arg1, arg2, arg3);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseBeforePutStatic(Object arg1, int arg2, Object arg3) {
        disableAdvising();
        adviceHandler().adviseBeforePutStatic(arg1, arg2, arg3);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseBeforeGetField(Object arg1, int arg2) {
        disableAdvising();
        adviceHandler().adviseBeforeGetField(arg1, arg2);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseBeforePutField(Object arg1, int arg2, double arg3) {
        disableAdvising();
        adviceHandler().adviseBeforePutField(arg1, arg2, arg3);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseBeforePutField(Object arg1, int arg2, long arg3) {
        disableAdvising();
        adviceHandler().adviseBeforePutField(arg1, arg2, arg3);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseBeforePutField(Object arg1, int arg2, float arg3) {
        disableAdvising();
        adviceHandler().adviseBeforePutField(arg1, arg2, arg3);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseBeforePutField(Object arg1, int arg2, Object arg3) {
        disableAdvising();
        adviceHandler().adviseBeforePutField(arg1, arg2, arg3);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseBeforeInvokeVirtual(Object arg1, int arg2) {
        disableAdvising();
        adviceHandler().adviseBeforeInvokeVirtual(arg1, arg2);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseBeforeInvokeSpecial(Object arg1, int arg2) {
        disableAdvising();
        adviceHandler().adviseBeforeInvokeSpecial(arg1, arg2);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseBeforeInvokeStatic(Object arg1, int arg2) {
        disableAdvising();
        adviceHandler().adviseBeforeInvokeStatic(arg1, arg2);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseBeforeInvokeInterface(Object arg1, int arg2) {
        disableAdvising();
        adviceHandler().adviseBeforeInvokeInterface(arg1, arg2);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseBeforeArrayLength(Object arg1, int arg2) {
        disableAdvising();
        adviceHandler().adviseBeforeArrayLength(arg1, arg2);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseBeforeThrow(Object arg1) {
        disableAdvising();
        adviceHandler().adviseBeforeThrow(arg1);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseBeforeCheckCast(Object arg1, Object arg2) {
        disableAdvising();
        adviceHandler().adviseBeforeCheckCast(arg1, arg2);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseBeforeInstanceOf(Object arg1, Object arg2) {
        disableAdvising();
        adviceHandler().adviseBeforeInstanceOf(arg1, arg2);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseBeforeMonitorEnter(Object arg1) {
        disableAdvising();
        adviceHandler().adviseBeforeMonitorEnter(arg1);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseBeforeMonitorExit(Object arg1) {
        disableAdvising();
        adviceHandler().adviseBeforeMonitorExit(arg1);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseBeforeBytecode(int arg1) {
        disableAdvising();
        adviceHandler().adviseBeforeBytecode(arg1);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseAfterInvokeVirtual(Object arg1, int arg2) {
        disableAdvising();
        adviceHandler().adviseAfterInvokeVirtual(arg1, arg2);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseAfterInvokeSpecial(Object arg1, int arg2) {
        disableAdvising();
        adviceHandler().adviseAfterInvokeSpecial(arg1, arg2);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseAfterInvokeStatic(Object arg1, int arg2) {
        disableAdvising();
        adviceHandler().adviseAfterInvokeStatic(arg1, arg2);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseAfterInvokeInterface(Object arg1, int arg2) {
        disableAdvising();
        adviceHandler().adviseAfterInvokeInterface(arg1, arg2);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseAfterNew(Object arg1) {
        disableAdvising();
        adviceHandler().adviseAfterNew(arg1);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseAfterNewArray(Object arg1, int arg2) {
        disableAdvising();
        adviceHandler().adviseAfterNewArray(arg1, arg2);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseAfterMultiNewArray(Object arg1, int[] arg2) {
        disableAdvising();
        adviceHandler().adviseAfterMultiNewArray(arg1, arg2);
        enableAdvising();
    }



}

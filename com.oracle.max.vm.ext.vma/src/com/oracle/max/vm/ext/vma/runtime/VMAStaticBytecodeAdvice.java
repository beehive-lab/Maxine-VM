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
    public static void adviseBeforeArrayLoad(Object arg1, int arg2, long arg3) {
        disableAdvising();
        adviceHandler().adviseBeforeArrayLoad(arg1, arg2, arg3);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseBeforeArrayLoad(Object arg1, int arg2, float arg3) {
        disableAdvising();
        adviceHandler().adviseBeforeArrayLoad(arg1, arg2, arg3);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseBeforeArrayLoad(Object arg1, int arg2, double arg3) {
        disableAdvising();
        adviceHandler().adviseBeforeArrayLoad(arg1, arg2, arg3);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseBeforeArrayLoad(Object arg1, int arg2, Object arg3) {
        disableAdvising();
        adviceHandler().adviseBeforeArrayLoad(arg1, arg2, arg3);
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
    public static void adviseBeforeGetStatic(Object arg1, int arg2, double arg3) {
        disableAdvising();
        adviceHandler().adviseBeforeGetStatic(arg1, arg2, arg3);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseBeforeGetStatic(Object arg1, int arg2, long arg3) {
        disableAdvising();
        adviceHandler().adviseBeforeGetStatic(arg1, arg2, arg3);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseBeforeGetStatic(Object arg1, int arg2, float arg3) {
        disableAdvising();
        adviceHandler().adviseBeforeGetStatic(arg1, arg2, arg3);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseBeforeGetStatic(Object arg1, int arg2, Object arg3) {
        disableAdvising();
        adviceHandler().adviseBeforeGetStatic(arg1, arg2, arg3);
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
    public static void adviseBeforePutStatic(Object arg1, int arg2, double arg3) {
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
    public static void adviseBeforeGetField(Object arg1, int arg2, long arg3) {
        disableAdvising();
        adviceHandler().adviseBeforeGetField(arg1, arg2, arg3);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseBeforeGetField(Object arg1, int arg2, float arg3) {
        disableAdvising();
        adviceHandler().adviseBeforeGetField(arg1, arg2, arg3);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseBeforeGetField(Object arg1, int arg2, double arg3) {
        disableAdvising();
        adviceHandler().adviseBeforeGetField(arg1, arg2, arg3);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseBeforeGetField(Object arg1, int arg2, Object arg3) {
        disableAdvising();
        adviceHandler().adviseBeforeGetField(arg1, arg2, arg3);
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
    public static void adviseBeforePutField(Object arg1, int arg2, Object arg3) {
        disableAdvising();
        adviceHandler().adviseBeforePutField(arg1, arg2, arg3);
        enableAdvising();
    }

    // GENERATED -- EDIT AND RUN VMAStaticBytecodeAdvice.main() TO MODIFY
    @NEVER_INLINE
    public static void adviseAfterInvokeSpecial(Object arg1) {
        disableAdvising();
        adviceHandler().adviseAfterInvokeSpecial(arg1);
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

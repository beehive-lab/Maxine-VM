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
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;

/**
 * A facade that defines static methods that parallel those in {@BytecodeAdvice} for use
 * in T1X templates, where we do not want the overhead of virtual method dispatch.
 *
 * The implementation forwards the call to the {@link VMAdviceHandler} registered
 * with {@link VMAJavaRunScheme}, and also disables/enables advice generation around the call.
 *
 * The methods are automatically generated.
 */

public class VMAStaticBytecodeAdvice {

    @NEVER_INLINE
    private static void debug(Throwable t) {
        Log.print("VMA: handler threw exception: ");
        Log.println(t.getClass().getName());
        MaxineVM.native_exit(1);
    }

// START GENERATED CODE
// EDIT AND RUN VMAStaticBytecodeAdviceGenerator.main() TO MODIFY

    @NEVER_INLINE
    public static void adviseBeforeConstLoad(int arg1, double arg2) {
        disableAdvising();
        try {
            adviceHandler().adviseBeforeConstLoad(arg1, arg2);
        } catch (Throwable t) {
            debug(t);
        } finally {
            enableAdvising();
        }
    }

    @NEVER_INLINE
    public static void adviseBeforeConstLoad(int arg1, float arg2) {
        disableAdvising();
        try {
            adviceHandler().adviseBeforeConstLoad(arg1, arg2);
        } catch (Throwable t) {
            debug(t);
        } finally {
            enableAdvising();
        }
    }

    @NEVER_INLINE
    public static void adviseBeforeConstLoad(int arg1, long arg2) {
        disableAdvising();
        try {
            adviceHandler().adviseBeforeConstLoad(arg1, arg2);
        } catch (Throwable t) {
            debug(t);
        } finally {
            enableAdvising();
        }
    }

    @NEVER_INLINE
    public static void adviseBeforeConstLoad(int arg1, Object arg2) {
        disableAdvising();
        try {
            adviceHandler().adviseBeforeConstLoad(arg1, arg2);
        } catch (Throwable t) {
            debug(t);
        } finally {
            enableAdvising();
        }
    }

    @NEVER_INLINE
    public static void adviseBeforeLoad(int arg1, int arg2) {
        disableAdvising();
        try {
            adviceHandler().adviseBeforeLoad(arg1, arg2);
        } catch (Throwable t) {
            debug(t);
        } finally {
            enableAdvising();
        }
    }

    @NEVER_INLINE
    public static void adviseBeforeArrayLoad(int arg1, Object arg2, int arg3) {
        disableAdvising();
        try {
            adviceHandler().adviseBeforeArrayLoad(arg1, arg2, arg3);
        } catch (Throwable t) {
            debug(t);
        } finally {
            enableAdvising();
        }
    }

    @NEVER_INLINE
    public static void adviseBeforeStore(int arg1, int arg2, Object arg3) {
        disableAdvising();
        try {
            adviceHandler().adviseBeforeStore(arg1, arg2, arg3);
        } catch (Throwable t) {
            debug(t);
        } finally {
            enableAdvising();
        }
    }

    @NEVER_INLINE
    public static void adviseBeforeStore(int arg1, int arg2, double arg3) {
        disableAdvising();
        try {
            adviceHandler().adviseBeforeStore(arg1, arg2, arg3);
        } catch (Throwable t) {
            debug(t);
        } finally {
            enableAdvising();
        }
    }

    @NEVER_INLINE
    public static void adviseBeforeStore(int arg1, int arg2, float arg3) {
        disableAdvising();
        try {
            adviceHandler().adviseBeforeStore(arg1, arg2, arg3);
        } catch (Throwable t) {
            debug(t);
        } finally {
            enableAdvising();
        }
    }

    @NEVER_INLINE
    public static void adviseBeforeStore(int arg1, int arg2, long arg3) {
        disableAdvising();
        try {
            adviceHandler().adviseBeforeStore(arg1, arg2, arg3);
        } catch (Throwable t) {
            debug(t);
        } finally {
            enableAdvising();
        }
    }

    @NEVER_INLINE
    public static void adviseBeforeArrayStore(int arg1, Object arg2, int arg3, double arg4) {
        disableAdvising();
        try {
            adviceHandler().adviseBeforeArrayStore(arg1, arg2, arg3, arg4);
        } catch (Throwable t) {
            debug(t);
        } finally {
            enableAdvising();
        }
    }

    @NEVER_INLINE
    public static void adviseBeforeArrayStore(int arg1, Object arg2, int arg3, Object arg4) {
        disableAdvising();
        try {
            adviceHandler().adviseBeforeArrayStore(arg1, arg2, arg3, arg4);
        } catch (Throwable t) {
            debug(t);
        } finally {
            enableAdvising();
        }
    }

    @NEVER_INLINE
    public static void adviseBeforeArrayStore(int arg1, Object arg2, int arg3, float arg4) {
        disableAdvising();
        try {
            adviceHandler().adviseBeforeArrayStore(arg1, arg2, arg3, arg4);
        } catch (Throwable t) {
            debug(t);
        } finally {
            enableAdvising();
        }
    }

    @NEVER_INLINE
    public static void adviseBeforeArrayStore(int arg1, Object arg2, int arg3, long arg4) {
        disableAdvising();
        try {
            adviceHandler().adviseBeforeArrayStore(arg1, arg2, arg3, arg4);
        } catch (Throwable t) {
            debug(t);
        } finally {
            enableAdvising();
        }
    }

    @NEVER_INLINE
    public static void adviseBeforeStackAdjust(int arg1, int arg2) {
        disableAdvising();
        try {
            adviceHandler().adviseBeforeStackAdjust(arg1, arg2);
        } catch (Throwable t) {
            debug(t);
        } finally {
            enableAdvising();
        }
    }

    @NEVER_INLINE
    public static void adviseBeforeOperation(int arg1, int arg2, double arg3, double arg4) {
        disableAdvising();
        try {
            adviceHandler().adviseBeforeOperation(arg1, arg2, arg3, arg4);
        } catch (Throwable t) {
            debug(t);
        } finally {
            enableAdvising();
        }
    }

    @NEVER_INLINE
    public static void adviseBeforeOperation(int arg1, int arg2, float arg3, float arg4) {
        disableAdvising();
        try {
            adviceHandler().adviseBeforeOperation(arg1, arg2, arg3, arg4);
        } catch (Throwable t) {
            debug(t);
        } finally {
            enableAdvising();
        }
    }

    @NEVER_INLINE
    public static void adviseBeforeOperation(int arg1, int arg2, long arg3, long arg4) {
        disableAdvising();
        try {
            adviceHandler().adviseBeforeOperation(arg1, arg2, arg3, arg4);
        } catch (Throwable t) {
            debug(t);
        } finally {
            enableAdvising();
        }
    }

    @NEVER_INLINE
    public static void adviseBeforeConversion(int arg1, int arg2, double arg3) {
        disableAdvising();
        try {
            adviceHandler().adviseBeforeConversion(arg1, arg2, arg3);
        } catch (Throwable t) {
            debug(t);
        } finally {
            enableAdvising();
        }
    }

    @NEVER_INLINE
    public static void adviseBeforeConversion(int arg1, int arg2, float arg3) {
        disableAdvising();
        try {
            adviceHandler().adviseBeforeConversion(arg1, arg2, arg3);
        } catch (Throwable t) {
            debug(t);
        } finally {
            enableAdvising();
        }
    }

    @NEVER_INLINE
    public static void adviseBeforeConversion(int arg1, int arg2, long arg3) {
        disableAdvising();
        try {
            adviceHandler().adviseBeforeConversion(arg1, arg2, arg3);
        } catch (Throwable t) {
            debug(t);
        } finally {
            enableAdvising();
        }
    }

    @NEVER_INLINE
    public static void adviseBeforeIf(int arg1, int arg2, Object arg3, Object arg4, int arg5) {
        disableAdvising();
        try {
            adviceHandler().adviseBeforeIf(arg1, arg2, arg3, arg4, arg5);
        } catch (Throwable t) {
            debug(t);
        } finally {
            enableAdvising();
        }
    }

    @NEVER_INLINE
    public static void adviseBeforeIf(int arg1, int arg2, int arg3, int arg4, int arg5) {
        disableAdvising();
        try {
            adviceHandler().adviseBeforeIf(arg1, arg2, arg3, arg4, arg5);
        } catch (Throwable t) {
            debug(t);
        } finally {
            enableAdvising();
        }
    }

    @NEVER_INLINE
    public static void adviseBeforeGoto(int arg1, int arg2) {
        disableAdvising();
        try {
            adviceHandler().adviseBeforeGoto(arg1, arg2);
        } catch (Throwable t) {
            debug(t);
        } finally {
            enableAdvising();
        }
    }

    @NEVER_INLINE
    public static void adviseBeforeReturn(int arg1) {
        disableAdvising();
        try {
            adviceHandler().adviseBeforeReturn(arg1);
        } catch (Throwable t) {
            debug(t);
        } finally {
            enableAdvising();
        }
    }

    @NEVER_INLINE
    public static void adviseBeforeReturn(int arg1, Object arg2) {
        disableAdvising();
        try {
            adviceHandler().adviseBeforeReturn(arg1, arg2);
        } catch (Throwable t) {
            debug(t);
        } finally {
            enableAdvising();
        }
    }

    @NEVER_INLINE
    public static void adviseBeforeReturn(int arg1, double arg2) {
        disableAdvising();
        try {
            adviceHandler().adviseBeforeReturn(arg1, arg2);
        } catch (Throwable t) {
            debug(t);
        } finally {
            enableAdvising();
        }
    }

    @NEVER_INLINE
    public static void adviseBeforeReturn(int arg1, float arg2) {
        disableAdvising();
        try {
            adviceHandler().adviseBeforeReturn(arg1, arg2);
        } catch (Throwable t) {
            debug(t);
        } finally {
            enableAdvising();
        }
    }

    @NEVER_INLINE
    public static void adviseBeforeReturn(int arg1, long arg2) {
        disableAdvising();
        try {
            adviceHandler().adviseBeforeReturn(arg1, arg2);
        } catch (Throwable t) {
            debug(t);
        } finally {
            enableAdvising();
        }
    }

    @NEVER_INLINE
    public static void adviseBeforeGetStatic(int arg1, Object arg2, FieldActor arg3) {
        disableAdvising();
        try {
            adviceHandler().adviseBeforeGetStatic(arg1, arg2, arg3);
        } catch (Throwable t) {
            debug(t);
        } finally {
            enableAdvising();
        }
    }

    @NEVER_INLINE
    public static void adviseBeforePutStatic(int arg1, Object arg2, FieldActor arg3, float arg4) {
        disableAdvising();
        try {
            adviceHandler().adviseBeforePutStatic(arg1, arg2, arg3, arg4);
        } catch (Throwable t) {
            debug(t);
        } finally {
            enableAdvising();
        }
    }

    @NEVER_INLINE
    public static void adviseBeforePutStatic(int arg1, Object arg2, FieldActor arg3, long arg4) {
        disableAdvising();
        try {
            adviceHandler().adviseBeforePutStatic(arg1, arg2, arg3, arg4);
        } catch (Throwable t) {
            debug(t);
        } finally {
            enableAdvising();
        }
    }

    @NEVER_INLINE
    public static void adviseBeforePutStatic(int arg1, Object arg2, FieldActor arg3, double arg4) {
        disableAdvising();
        try {
            adviceHandler().adviseBeforePutStatic(arg1, arg2, arg3, arg4);
        } catch (Throwable t) {
            debug(t);
        } finally {
            enableAdvising();
        }
    }

    @NEVER_INLINE
    public static void adviseBeforePutStatic(int arg1, Object arg2, FieldActor arg3, Object arg4) {
        disableAdvising();
        try {
            adviceHandler().adviseBeforePutStatic(arg1, arg2, arg3, arg4);
        } catch (Throwable t) {
            debug(t);
        } finally {
            enableAdvising();
        }
    }

    @NEVER_INLINE
    public static void adviseBeforeGetField(int arg1, Object arg2, FieldActor arg3) {
        disableAdvising();
        try {
            adviceHandler().adviseBeforeGetField(arg1, arg2, arg3);
        } catch (Throwable t) {
            debug(t);
        } finally {
            enableAdvising();
        }
    }

    @NEVER_INLINE
    public static void adviseBeforePutField(int arg1, Object arg2, FieldActor arg3, float arg4) {
        disableAdvising();
        try {
            adviceHandler().adviseBeforePutField(arg1, arg2, arg3, arg4);
        } catch (Throwable t) {
            debug(t);
        } finally {
            enableAdvising();
        }
    }

    @NEVER_INLINE
    public static void adviseBeforePutField(int arg1, Object arg2, FieldActor arg3, long arg4) {
        disableAdvising();
        try {
            adviceHandler().adviseBeforePutField(arg1, arg2, arg3, arg4);
        } catch (Throwable t) {
            debug(t);
        } finally {
            enableAdvising();
        }
    }

    @NEVER_INLINE
    public static void adviseBeforePutField(int arg1, Object arg2, FieldActor arg3, double arg4) {
        disableAdvising();
        try {
            adviceHandler().adviseBeforePutField(arg1, arg2, arg3, arg4);
        } catch (Throwable t) {
            debug(t);
        } finally {
            enableAdvising();
        }
    }

    @NEVER_INLINE
    public static void adviseBeforePutField(int arg1, Object arg2, FieldActor arg3, Object arg4) {
        disableAdvising();
        try {
            adviceHandler().adviseBeforePutField(arg1, arg2, arg3, arg4);
        } catch (Throwable t) {
            debug(t);
        } finally {
            enableAdvising();
        }
    }

    @NEVER_INLINE
    public static void adviseBeforeInvokeVirtual(int arg1, Object arg2, MethodActor arg3) {
        disableAdvising();
        try {
            adviceHandler().adviseBeforeInvokeVirtual(arg1, arg2, arg3);
        } catch (Throwable t) {
            debug(t);
        } finally {
            enableAdvising();
        }
    }

    @NEVER_INLINE
    public static void adviseBeforeInvokeSpecial(int arg1, Object arg2, MethodActor arg3) {
        disableAdvising();
        try {
            adviceHandler().adviseBeforeInvokeSpecial(arg1, arg2, arg3);
        } catch (Throwable t) {
            debug(t);
        } finally {
            enableAdvising();
        }
    }

    @NEVER_INLINE
    public static void adviseBeforeInvokeStatic(int arg1, Object arg2, MethodActor arg3) {
        disableAdvising();
        try {
            adviceHandler().adviseBeforeInvokeStatic(arg1, arg2, arg3);
        } catch (Throwable t) {
            debug(t);
        } finally {
            enableAdvising();
        }
    }

    @NEVER_INLINE
    public static void adviseBeforeInvokeInterface(int arg1, Object arg2, MethodActor arg3) {
        disableAdvising();
        try {
            adviceHandler().adviseBeforeInvokeInterface(arg1, arg2, arg3);
        } catch (Throwable t) {
            debug(t);
        } finally {
            enableAdvising();
        }
    }

    @NEVER_INLINE
    public static void adviseBeforeArrayLength(int arg1, Object arg2, int arg3) {
        disableAdvising();
        try {
            adviceHandler().adviseBeforeArrayLength(arg1, arg2, arg3);
        } catch (Throwable t) {
            debug(t);
        } finally {
            enableAdvising();
        }
    }

    @NEVER_INLINE
    public static void adviseBeforeThrow(int arg1, Object arg2) {
        disableAdvising();
        try {
            adviceHandler().adviseBeforeThrow(arg1, arg2);
        } catch (Throwable t) {
            debug(t);
        } finally {
            enableAdvising();
        }
    }

    @NEVER_INLINE
    public static void adviseBeforeCheckCast(int arg1, Object arg2, Object arg3) {
        disableAdvising();
        try {
            adviceHandler().adviseBeforeCheckCast(arg1, arg2, arg3);
        } catch (Throwable t) {
            debug(t);
        } finally {
            enableAdvising();
        }
    }

    @NEVER_INLINE
    public static void adviseBeforeInstanceOf(int arg1, Object arg2, Object arg3) {
        disableAdvising();
        try {
            adviceHandler().adviseBeforeInstanceOf(arg1, arg2, arg3);
        } catch (Throwable t) {
            debug(t);
        } finally {
            enableAdvising();
        }
    }

    @NEVER_INLINE
    public static void adviseBeforeMonitorEnter(int arg1, Object arg2) {
        disableAdvising();
        try {
            adviceHandler().adviseBeforeMonitorEnter(arg1, arg2);
        } catch (Throwable t) {
            debug(t);
        } finally {
            enableAdvising();
        }
    }

    @NEVER_INLINE
    public static void adviseBeforeMonitorExit(int arg1, Object arg2) {
        disableAdvising();
        try {
            adviceHandler().adviseBeforeMonitorExit(arg1, arg2);
        } catch (Throwable t) {
            debug(t);
        } finally {
            enableAdvising();
        }
    }

    @NEVER_INLINE
    public static void adviseAfterLoad(int arg1, int arg2, Object arg3) {
        disableAdvising();
        try {
            adviceHandler().adviseAfterLoad(arg1, arg2, arg3);
        } catch (Throwable t) {
            debug(t);
        } finally {
            enableAdvising();
        }
    }

    @NEVER_INLINE
    public static void adviseAfterArrayLoad(int arg1, Object arg2, int arg3, Object arg4) {
        disableAdvising();
        try {
            adviceHandler().adviseAfterArrayLoad(arg1, arg2, arg3, arg4);
        } catch (Throwable t) {
            debug(t);
        } finally {
            enableAdvising();
        }
    }

    @NEVER_INLINE
    public static void adviseAfterNew(int arg1, Object arg2) {
        disableAdvising();
        try {
            adviceHandler().adviseAfterNew(arg1, arg2);
        } catch (Throwable t) {
            debug(t);
        } finally {
            enableAdvising();
        }
    }

    @NEVER_INLINE
    public static void adviseAfterNewArray(int arg1, Object arg2, int arg3) {
        disableAdvising();
        try {
            adviceHandler().adviseAfterNewArray(arg1, arg2, arg3);
        } catch (Throwable t) {
            debug(t);
        } finally {
            enableAdvising();
        }
    }

    @NEVER_INLINE
    public static void adviseAfterMultiNewArray(int arg1, Object arg2, int[] arg3) {
        disableAdvising();
        try {
            adviceHandler().adviseAfterMultiNewArray(arg1, arg2, arg3);
        } catch (Throwable t) {
            debug(t);
        } finally {
            enableAdvising();
        }
    }

    @NEVER_INLINE
    public static void adviseAfterMethodEntry(int arg1, Object arg2, MethodActor arg3) {
        disableAdvising();
        try {
            adviceHandler().adviseAfterMethodEntry(arg1, arg2, arg3);
        } catch (Throwable t) {
            debug(t);
        } finally {
            enableAdvising();
        }
    }

// END GENERATED CODE
}

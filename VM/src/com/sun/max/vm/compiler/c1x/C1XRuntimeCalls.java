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
package com.sun.max.vm.compiler.c1x;

import static com.sun.max.vm.VMConfiguration.*;

import java.lang.annotation.*;
import java.lang.reflect.*;

import com.sun.cri.ci.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.snippet.Snippet.DoubleRemainder;
import com.sun.max.vm.compiler.snippet.Snippet.FloatRemainder;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;

/**
 * This class contains the implementation of runtime calls that are called by
 * code emitted by the C1X compiler.
 *
 * @author Thomas Wuerthinger
 * @author Ben L. Titzer
 */
public class C1XRuntimeCalls {

    // a local flag to enable calls to verify the reference map for each runtime call
    private static final boolean ENABLE_REFMAP_VERIFICATION = true;

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface C1X_RUNTIME_ENTRYPOINT {
        CiRuntimeCall runtimeCall();
    }

    public static ClassMethodActor getClassMethodActor(CiRuntimeCall call) {
        final ClassMethodActor result = runtimeCallMethods[call.ordinal()];
        assert result != null;
        return result;
    }

    private static ClassMethodActor[] runtimeCallMethods = new ClassMethodActor[CiRuntimeCall.values().length];

    static {
        for (Method method : C1XRuntimeCalls.class.getMethods()) {
            C1X_RUNTIME_ENTRYPOINT entry = method.getAnnotation(C1X_RUNTIME_ENTRYPOINT.class);
            if (entry != null) {
                registerMethod(method, entry.runtimeCall());
            }
        }

        for (CiRuntimeCall call : CiRuntimeCall.values()) {
            assert getClassMethodActor(call) != null : "no runtime method defined for " + call.toString();
            assert checkCompatible(call, getClassMethodActor(call));
        }
    }

    @C1X_RUNTIME_ENTRYPOINT(runtimeCall = CiRuntimeCall.UnwindException)
    public static void runtimeUnwindException(Throwable throwable) throws Throwable {
        Throw.raise(throwable);
    }

    private static boolean checkCompatible(CiRuntimeCall call, ClassMethodActor classMethodActor) {
        assert classMethodActor.resultKind().ciKind == call.resultKind;
        for (int i = 0; i < call.arguments.length; i++) {
            assert classMethodActor.getParameterKinds()[i].ciKind == call.arguments[i];
        }

        return true;
    }

    private static void verifyRefMaps() {
        if (ENABLE_REFMAP_VERIFICATION) {
            StackReferenceMapPreparer.verifyReferenceMapsForThisThread();
        }
    }

    @C1X_RUNTIME_ENTRYPOINT(runtimeCall = CiRuntimeCall.RegisterFinalizer)
    public static void runtimeRegisterFinalizer(Object object) {
        verifyRefMaps();
        SpecialReferenceManager.registerFinalizee(object);
    }

    @C1X_RUNTIME_ENTRYPOINT(runtimeCall = CiRuntimeCall.HandleException)
    public static void runtimeHandleException(Throwable throwable) throws Throwable {
        verifyRefMaps();
        Throw.raise(throwable);
    }

    @C1X_RUNTIME_ENTRYPOINT(runtimeCall = CiRuntimeCall.OSRMigrationEnd)
    public static void runtimeOSRMigrationEnd() {
        verifyRefMaps();
    }

    @C1X_RUNTIME_ENTRYPOINT(runtimeCall = CiRuntimeCall.JavaTimeMillis)
    public static long runtimeJavaTimeMillis() {
        verifyRefMaps();
        return MaxineVM.native_currentTimeMillis();
    }

    @C1X_RUNTIME_ENTRYPOINT(runtimeCall = CiRuntimeCall.JavaTimeNanos)
    public static long runtimeJavaTimeNanos() {
        verifyRefMaps();
        return MaxineVM.native_nanoTime();
    }

    @C1X_RUNTIME_ENTRYPOINT(runtimeCall = CiRuntimeCall.Debug)
    public static void runtimeDebug() {
        verifyRefMaps();
        throw FatalError.unexpected("Debug");
    }

    @C1X_RUNTIME_ENTRYPOINT(runtimeCall = CiRuntimeCall.ArithmethicLrem)
    public static long runtimeArithmethicLrem(long a, long b) {
        verifyRefMaps();
        throw FatalError.unexpected("Compiler should directly translate LREM");
    }

    @C1X_RUNTIME_ENTRYPOINT(runtimeCall = CiRuntimeCall.ArithmeticLdiv)
    public static long runtimeArithmeticLdiv(long a, long b) {
        verifyRefMaps();
        throw FatalError.unexpected("Compiler should directly translate LDIV");
    }

    @C1X_RUNTIME_ENTRYPOINT(runtimeCall = CiRuntimeCall.ArithmeticFrem)
    public static float runtimeArithmeticFrem(float v1, float v2) {
        verifyRefMaps();
        return FloatRemainder.floatRemainder(v1, v2);
    }

    @C1X_RUNTIME_ENTRYPOINT(runtimeCall = CiRuntimeCall.ArithmeticDrem)
    public static double runtimeArithmeticDrem(double v1, double v2) {
        verifyRefMaps();
        return DoubleRemainder.doubleRemainder(v1, v2);
    }

    @C1X_RUNTIME_ENTRYPOINT(runtimeCall = CiRuntimeCall.ArithmeticCos)
    public static double runtimeArithmeticCos(double v) {
        verifyRefMaps();
        return Math.cos(v);
    }

    @C1X_RUNTIME_ENTRYPOINT(runtimeCall = CiRuntimeCall.ArithmeticTan)
    public static double runtimeArithmeticTan(double v) {
        verifyRefMaps();
        return Math.tan(v);
    }

    @C1X_RUNTIME_ENTRYPOINT(runtimeCall = CiRuntimeCall.ArithmeticLog)
    public static double runtimeArithmeticLog(double v) {
        verifyRefMaps();
        return Math.log(v);
    }

    @C1X_RUNTIME_ENTRYPOINT(runtimeCall = CiRuntimeCall.ArithmeticLog10)
    public static double runtimeArithmeticLog10(double v) {
        verifyRefMaps();
        return Math.log10(v);
    }

    @C1X_RUNTIME_ENTRYPOINT(runtimeCall = CiRuntimeCall.ArithmeticSin)
    public static double runtimeArithmeticSin(double v) {
        verifyRefMaps();
        return Math.sin(v);
    }

    private static void registerMethod(Method selectedMethod, CiRuntimeCall call) {
        ClassMethodActor classMethodActor = null;
        assert runtimeCallMethods[call.ordinal()] == null : "method already defined";
        classMethodActor = ClassMethodActor.fromJava(selectedMethod);
        runtimeCallMethods[call.ordinal()] = classMethodActor;
        if (MaxineVM.isHosted()) {
            new CriticalMethod(classMethodActor, CallEntryPoint.OPTIMIZED_ENTRY_POINT);
        } else {
            vmConfig().compilationScheme().synchronousCompile(classMethodActor);
        }
    }
}

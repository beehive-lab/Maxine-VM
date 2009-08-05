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

import java.lang.annotation.*;
import java.lang.reflect.*;

import com.sun.c1x.ci.*;
import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;

/**
 *
 * @author Thomas Wuerthinger
 */
public class C1XRuntimeCalls {

    public static ClassMethodActor getClassMethodActor(CiRuntimeCall call) {
        final ClassMethodActor result = criticalMethods[call.ordinal()].classMethodActor;
        assert result != null;
        return result;
    }

    private static CriticalMethod[] criticalMethods = new CriticalMethod[CiRuntimeCall.values().length];

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface RUNTIME_ENTRY {
        CiRuntimeCall type();
    }

    static {
        for (Method method : C1XRuntimeCalls.class.getMethods()) {
            RUNTIME_ENTRY entry = method.getAnnotation(RUNTIME_ENTRY.class);
            if (entry != null && entry.type() != null) {
                registerMethod(method, entry.type());
            }
        }

        for (CiRuntimeCall call : CiRuntimeCall.values()) {
            assert getClassMethodActor(call) != null : "no runtime method defined for " + call.toString();
        }
    }


    @RUNTIME_ENTRY(type = CiRuntimeCall.UnwindException)
    public static void runtimeUnwindException(Throwable throwable) throws Throwable {
        throw throwable;
    }


    @RUNTIME_ENTRY(type = CiRuntimeCall.ThrowRangeCheckFailed)
    public static void runtimeThrowRangeCheckFailed(int index) throws ArrayIndexOutOfBoundsException {
        throw new ArrayIndexOutOfBoundsException(index);
    }


    @RUNTIME_ENTRY(type = CiRuntimeCall.ThrowIndexException)
    public static void runtimeThrowIndexException(int index) throws ArrayIndexOutOfBoundsException {
        throw new ArrayIndexOutOfBoundsException(index);
    }


    @RUNTIME_ENTRY(type = CiRuntimeCall.ThrowDiv0Exception)
    public static void runtimeThrowDiv0Exception() throws ArithmeticException {
        throw new ArithmeticException("division by zero");
    }


    @RUNTIME_ENTRY(type = CiRuntimeCall.ThrowNullPointerException)
    public static void runtimeThrowNullPointerException() throws NullPointerException {
        throw new NullPointerException();
    }


    @RUNTIME_ENTRY(type = CiRuntimeCall.RegisterFinalizer)
    public static void runtimeRegisterFinalizer() {
    }


    @RUNTIME_ENTRY(type = CiRuntimeCall.NewInstance)
    public static Object runtimeNewInstance(Hub hub) {
        final ClassActor classActor = hub.classActor;
        if (MaxineVM.isPrototyping()) {
            try {
                return Objects.allocateInstance(classActor.toJava());
            } catch (InstantiationException instantiationException) {
                throw ProgramError.unexpected(instantiationException);
            }
        }
        if (classActor.isHybridClassActor()) {
            return Heap.createHybrid(classActor.dynamicHub());
        }
        final Object object = Heap.createTuple(classActor.dynamicHub());
        if (classActor.hasFinalizer()) {
            SpecialReferenceManager.registerFinalizee(object);
        }
        return object;
    }

    @INLINE
    private static Object createArray(Hub hub, int length) {
        if (length < 0) {
            Throw.negativeArraySizeException(length);
        }
        if (MaxineVM.isPrototyping()) {
            return Array.newInstance(hub.classActor.componentClassActor().toJava(), length);
        }
        return Heap.createArray(hub.classActor.dynamicHub(), length);
    }

    @RUNTIME_ENTRY(type = CiRuntimeCall.NewArray)
    public static Object runtimeNewArray(Hub arrayClassActor, int length) {
        return createArray(arrayClassActor, length);
    }

    @INLINE
    private static Object createNonNegativeSizeArray(ClassActor arrayClassActor, int length) {
        if (MaxineVM.isPrototyping()) {
            return Array.newInstance(arrayClassActor.componentClassActor().toJava(), length);
        }
        return Heap.createArray(arrayClassActor.dynamicHub(), length);
    }


    @RUNTIME_ENTRY(type = CiRuntimeCall.NewMultiArray)
    public static Object runtimeNewMultiArray(int index, ClassActor arrayClassActor, int[] lengths) {
        return runtimeNewMultiArrayHelper(index, arrayClassActor, lengths);
    }

    private static Object runtimeNewMultiArrayHelper(int index, ClassActor arrayClassActor, int[] lengths) {
        final int length = lengths[index];
        final Object result = createNonNegativeSizeArray(arrayClassActor, length);
        if (length > 0) {
            final int nextIndex = index + 1;
            if (nextIndex < lengths.length) {
                final ClassActor subArrayClassActor = arrayClassActor.componentClassActor();
                for (int i = 0; i < length; i++) {
                    final Object subArray = runtimeNewMultiArrayHelper(nextIndex, subArrayClassActor, lengths);
                    if (MaxineVM.isPrototyping()) {
                        final Object[] array = (Object[]) result;
                        array[i] = subArray;
                    } else {
                        ArrayAccess.setObject(result, i, subArray);
                    }
                }
            }
        }
        return result;
    }


    @RUNTIME_ENTRY(type = CiRuntimeCall.HandleException)
    public static void runtimeHandleException() {
    }


    @RUNTIME_ENTRY(type = CiRuntimeCall.ThrowArrayStoreException)
    public static void runtimeThrowArrayStoreException() {
        throw new ArrayStoreException();
    }


    @RUNTIME_ENTRY(type = CiRuntimeCall.ThrowClassCastException)
    public static void runtimeThrowClassCastException() {
        throw new ClassCastException();
    }


    @RUNTIME_ENTRY(type = CiRuntimeCall.ThrowIncompatibleClassChangeError)
    public static void runtimeThrowIncompatibleClassChangeError() {
        throw new IncompatibleClassChangeError();
    }


    @RUNTIME_ENTRY(type = CiRuntimeCall.SlowSubtypeCheck)
    public static boolean runtimeSlowSubtypeCheck(Hub a, Hub b) {
        return b.isSubClassHub(a.classActor);
    }


    @RUNTIME_ENTRY(type = CiRuntimeCall.Monitorenter)
    public static void runtimeMonitorenter() {
    }


    @RUNTIME_ENTRY(type = CiRuntimeCall.Monitorexit)
    public static void runtimeMonitorexit() {
    }


    @RUNTIME_ENTRY(type = CiRuntimeCall.TraceBlockEntry)
    public static void runtimeTraceBlockEntry() {
    }


    @RUNTIME_ENTRY(type = CiRuntimeCall.OSRMigrationEnd)
    public static void runtimeOSRMigrationEnd() {
    }


    @RUNTIME_ENTRY(type = CiRuntimeCall.JavaTimeMillis)
    public static long runtimeJavaTimeMillis() {
        return System.currentTimeMillis();
    }


    @RUNTIME_ENTRY(type = CiRuntimeCall.JavaTimeNanos)
    public static long runtimeJavaTimeNanos() {
        // TODO: Implement correctly!
        return 0;
    }


    @RUNTIME_ENTRY(type = CiRuntimeCall.OopArrayCopy)
    public static void runtimeOopArrayCopy() {
        // TODO: Implement correctly!
    }


    @RUNTIME_ENTRY(type = CiRuntimeCall.PrimitiveArrayCopy)
    public static void runtimePrimitiveArrayCopy() {
        // TODO: Implement correctly!
    }


    @RUNTIME_ENTRY(type = CiRuntimeCall.ArrayCopy)
    public static void runtimeArrayCopy() {
        // TODO: Implement correctly!
    }


    @RUNTIME_ENTRY(type = CiRuntimeCall.ResolveOptVirtualCall)
    public static void runtimeResolveOptVirtualCall() {
        // TODO: Implement correctly!
    }


    @RUNTIME_ENTRY(type = CiRuntimeCall.ResolveStaticCall)
    public static void runtimeResolveStaticCall() {
        // TODO: Implement correctly!
    }


    @RUNTIME_ENTRY(type = CiRuntimeCall.ResolveVirtualCall)
    public static void runtimeResolveVirtualCall() {
        // TODO: Implement correctly!
    }


    @RUNTIME_ENTRY(type = CiRuntimeCall.Debug)
    public static void runtimeDebug() {
    }


    @RUNTIME_ENTRY(type = CiRuntimeCall.ArithmethicLrem)
    public static long runtimeArithmethicLrem(long a, long b) {
        return a % b;
    }


    @RUNTIME_ENTRY(type = CiRuntimeCall.ArithmeticLdiv)
    public static long runtimeArithmeticLdiv(long a, long b) {
        return a / b;
    }


    @RUNTIME_ENTRY(type = CiRuntimeCall.ArithmeticLmul)
    public static long runtimeArithmeticLmul(long a, long b) {
        return a * b;
    }


    @RUNTIME_ENTRY(type = CiRuntimeCall.ArithmeticFrem)
    public static float runtimeArithmeticFrem(float v1, float v2) {
        return v1 % v2;
    }


    @RUNTIME_ENTRY(type = CiRuntimeCall.ArithmeticDrem)
    public static double runtimeArithmeticDrem(double v1, double v2) {
        return v1 % v2;
    }


    @RUNTIME_ENTRY(type = CiRuntimeCall.ArithmeticCos)
    public static double runtimeArithmeticCos(double v) {
        return Math.cos(v);
    }


    @RUNTIME_ENTRY(type = CiRuntimeCall.ArithmeticTan)
    public static double runtimeArithmeticTan(double v) {
        return Math.tan(v);
    }


    @RUNTIME_ENTRY(type = CiRuntimeCall.ArithmeticLog)
    public static double runtimeArithmeticLog(double v) {
        return Math.log(v);
    }


    @RUNTIME_ENTRY(type = CiRuntimeCall.ArithmeticLog10)
    public static double runtimeArithmeticLog10(double v) {
        return Math.log10(v);
    }


    @RUNTIME_ENTRY(type = CiRuntimeCall.ArithmeticSin)
    public static double runtimeArithmeticSin(double v) {
        return Math.sin(v);
    }

    private static void registerMethod(Method selectedMethod, CiRuntimeCall call) {
        assert criticalMethods[call.ordinal()] == null : "method already defined";
        criticalMethods[call.ordinal()] = new CriticalMethod(C1XRuntimeCalls.class, selectedMethod.getName(), SignatureDescriptor.create(selectedMethod.getReturnType(), selectedMethod.getParameterTypes()));
    }
}

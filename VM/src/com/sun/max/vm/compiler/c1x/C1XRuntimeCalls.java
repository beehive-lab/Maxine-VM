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
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;
import com.sun.max.unsafe.Word;

/**
 *
 * @author Thomas Wuerthinger
 */
public class C1XRuntimeCalls {

    public static ClassMethodActor getClassMethodActor(CiRuntimeCall call) {
        final ClassMethodActor result = runtimeCallMethods[call.ordinal()];
        assert result != null;
        return result;
    }

    private static ClassMethodActor[] runtimeCallMethods = new ClassMethodActor[CiRuntimeCall.values().length];

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
            assert checkCompatible(call, getClassMethodActor(call));
        }
    }


    @RUNTIME_ENTRY(type = CiRuntimeCall.UnwindException)
    public static void runtimeUnwindException(Throwable throwable) throws Throwable {
        throw throwable;
    }


    private static boolean checkCompatible(CiRuntimeCall call, ClassMethodActor classMethodActor) {

        assert checkCompatible(call.resultType, classMethodActor.resultKind());
        for (int i = 0; i < call.arguments.length; i++) {
            assert checkCompatible(call.arguments[i], classMethodActor.getParameterKinds()[i]);
        }

        return true;
    }


    private static boolean checkCompatible(CiKind resultType, Kind resultKind) {
        switch(resultType) {
            case Boolean:
                return resultKind == Kind.BOOLEAN;
            case Byte:
                return resultKind == Kind.BYTE;
            case Char:
                return resultKind == Kind.CHAR;
            case Double:
                return resultKind == Kind.DOUBLE;
            case Float:
                return resultKind == Kind.FLOAT;
            case Illegal:
                return false;
            case Int:
                return resultKind == Kind.INT;
            case Jsr:
                return resultKind == Kind.REFERENCE;
            case Long:
                return resultKind == Kind.LONG;
            case Object:
                return resultKind == Kind.REFERENCE;
            case Short:
                return resultKind == Kind.SHORT;
            case Void:
                return resultKind == Kind.VOID;
            case Word:
                return resultKind == Kind.LONG;
        }

        return false;
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
    private static Object createArray(DynamicHub hub, int length) {
        if (length < 0) {
            Throw.negativeArraySizeException(length);
        }
        if (MaxineVM.isPrototyping()) {
            return Array.newInstance(hub.classActor.componentClassActor().toJava(), length);
        }
        return Heap.createArray(hub, length);
    }

    @RUNTIME_ENTRY(type = CiRuntimeCall.NewArray)
    public static Object runtimeNewArray(DynamicHub arrayClassActor, int length) {
        return createArray(arrayClassActor, length);
    }

    @UNSAFE
    @RUNTIME_ENTRY(type = CiRuntimeCall.RetrieveInterfaceIndex)
    public static int retrieveInterfaceIndex(Object receiver, int interfaceId) {
        if (receiver == null) {
            return 0;
        }

        final Class receiverClass = receiver.getClass();
        final ClassActor classActor = ClassActor.fromJava(receiverClass);
        final int interfaceIIndex = classActor.dynamicHub().getITableIndex(interfaceId);
        return interfaceIIndex * Word.size() + VMConfiguration.target().layoutScheme().hybridLayout.headerSize(); // TODO (tw): return word size here!
    }


    @UNSAFE
    @RUNTIME_ENTRY(type = CiRuntimeCall.ResolveInterfaceIndex)
    public static int resolveInterfaceIndex(Object receiver, int index, ConstantPool constantPool) {
        if (receiver == null) {
            return 0;
        }

        final InterfaceMethodActor methodActor = (InterfaceMethodActor) constantPool.interfaceMethodAt(index).resolve(constantPool, index);
        final InterfaceActor interfaceActor = (InterfaceActor) methodActor.holder();
        int interfaceId = interfaceActor.id;
        final Class receiverClass = receiver.getClass();
        final ClassActor classActor = ClassActor.fromJava(receiverClass);
        final int interfaceIIndex = classActor.dynamicHub().getITableIndex(interfaceId);
        return interfaceIIndex * Word.size() + VMConfiguration.target().layoutScheme().hybridLayout.headerSize() + Word.size() * methodActor.iIndexInInterface(); // TODO (tw): return word size here!
    }

    @UNSAFE
    @INLINE
    private static Object createNonNegativeSizeArray(ClassActor arrayClassActor, int length) {
        if (MaxineVM.isPrototyping()) {
            return Array.newInstance(arrayClassActor.componentClassActor().toJava(), length);
        }
        return Heap.createArray(arrayClassActor.dynamicHub(), length);
    }


    @RUNTIME_ENTRY(type = CiRuntimeCall.NewMultiArray)
    public static Object runtimeNewMultiArray(Hub arrayClassHub, int[] lengths) {
        for (int length : lengths) {
            if (length < 0) {
                Throw.negativeArraySizeException(length);
            }
        }
        return runtimeNewMultiArrayHelper(0, arrayClassHub.classActor, lengths);
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
    public static void runtimeHandleException(Throwable throwable) throws Throwable {
        throw throwable;
    }


    @RUNTIME_ENTRY(type = CiRuntimeCall.ThrowArrayStoreException)
    public static void runtimeThrowArrayStoreException() {
        throw new ArrayStoreException();
    }


    @RUNTIME_ENTRY(type = CiRuntimeCall.ThrowClassCastException)
    public static void runtimeThrowClassCastException(Object o) {
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
    public static void runtimeMonitorenter(Object obj, int monitorID) {
        VMConfiguration.target().monitorScheme().monitorEnter(obj);
    }

    @RUNTIME_ENTRY(type = CiRuntimeCall.Monitorexit)
    public static void runtimeMonitorexit(Object obj, int monitorID) {
        VMConfiguration.target().monitorScheme().monitorExit(obj);
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

    @UNSAFE
    @RUNTIME_ENTRY(type = CiRuntimeCall.ResolveOptVirtualCall)
    public static long runtimeResolveOptVirtualCall(int index, ConstantPool constantPool) {
        final VirtualMethodActor methodActor = constantPool.classMethodAt(index).resolveVirtual(constantPool, index);
        return CompilationScheme.Static.compile(methodActor, CallEntryPoint.OPTIMIZED_ENTRY_POINT).toLong();
    }

    @UNSAFE
    @RUNTIME_ENTRY(type = CiRuntimeCall.ResolveStaticCall)
    public static long runtimeResolveStaticCall(int index, ConstantPool constantPool) {
        final StaticMethodActor staticMethodActor = constantPool.classMethodAt(index).resolveStatic(constantPool, index);
        return CompilationScheme.Static.compile(staticMethodActor, CallEntryPoint.OPTIMIZED_ENTRY_POINT).toLong();
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

    @RUNTIME_ENTRY(type = CiRuntimeCall.ResolveClass)
    public static Object resolveClass(int index, ConstantPool constantPool) {
        final ClassActor classActor = constantPool.classAt(index).resolve(constantPool, index);
        return classActor.dynamicHub();
    }

    @RUNTIME_ENTRY(type = CiRuntimeCall.ResolveArrayClass)
    public static Object resolveArrayClass(int index, ConstantPool constantPool) {
        final ClassActor classActor = constantPool.classAt(index).resolve(constantPool, index);
        return ArrayClassActor.forComponentClassActor(classActor).dynamicHub();
    }

    @RUNTIME_ENTRY(type = CiRuntimeCall.ResolveStaticFields)
    public static Object resolveStaticFields(int index, ConstantPool constantPool) {
        // Here the reference to the field cp entry is given
        final ClassActor classActor = constantPool.fieldAt(index).resolve(constantPool, index).holder();
        return classActor.staticTuple();
    }

    @RUNTIME_ENTRY(type = CiRuntimeCall.ResolveJavaClass)
    public static Object resolveJavaClass(int index, ConstantPool constantPool) {
        final ClassActor classActor = constantPool.classAt(index).resolve(constantPool, index);
        return classActor.toJava();
    }

    @RUNTIME_ENTRY(type = CiRuntimeCall.ResolveFieldOffset)
    public static int resolveFieldOffset(int index, ConstantPool constantPool) {
        final FieldActor fieldActor = constantPool.fieldAt(index).resolve(constantPool, index);
        return fieldActor.offset();
    }


    @RUNTIME_ENTRY(type = CiRuntimeCall.ResolveVTableIndex)
    public static int resolveVTableIndex(int index, ConstantPool constantPool) {
        final VirtualMethodActor virtualMethodActor = constantPool.classMethodAt(index).resolveVirtual(constantPool, index);
        return virtualMethodActor.vTableIndex();
    }



    @RUNTIME_ENTRY(type = CiRuntimeCall.ArithmeticSin)
    public static double runtimeArithmeticSin(double v) {
        return Math.sin(v);
    }

    private static void registerMethod(Method selectedMethod, CiRuntimeCall call) {
        assert runtimeCallMethods[call.ordinal()] == null : "method already defined";
        if (MaxineVM.isPrototyping()) {
            new CriticalMethod(C1XRuntimeCalls.class, selectedMethod.getName(), SignatureDescriptor.create(selectedMethod.getReturnType(), selectedMethod.getParameterTypes()));
        }

        runtimeCallMethods[call.ordinal()] = ClassMethodActor.fromJava(selectedMethod);
    }
}

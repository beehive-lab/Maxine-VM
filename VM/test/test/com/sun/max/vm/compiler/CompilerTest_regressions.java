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
package test.com.sun.max.vm.compiler;

import java.io.*;
import java.net.*;
import java.util.zip.*;

import jtt.micro.*;
import sun.misc.*;

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.b.c.*;
import com.sun.max.vm.compiler.bir.*;
import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.jit.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;
import com.sun.max.vm.verifier.*;

/**
 * Tests for individual methods that exposed compiler bugs.
 *
 * @author Hiroshi Yamauchi
 * @author Bernd Mathiske
 */
public abstract class CompilerTest_regressions<Method_Type extends IrMethod> extends CompilerTestCase<Method_Type> {

    public CompilerTest_regressions(String name) {
        super(name);
    }

    @NEVER_INLINE
    private static int next(int a) {
        return a - 1;
    }

    public static int loopConfusion(int argument) {
        int a = argument;
        int previous = -1;
        while (a > 0) {
            previous = a; // This assignment comes second in DIR and when the bug is present it uses the result from the other assignment
            a = next(a); // This assignment comes first in DIR and when the bug is present, 'a' from before is not represented separately
        }
        return previous;
    }

    public static void callWithLotsOfArguments(int i1, int i2, int i3) {
        calleeWithLotsOfArguments(i1, i2, i3, 4, 5, 6, 7, 77);
    }

    @NEVER_INLINE
    private static int calleeWithLotsOfArguments(int i1,  int i2, int i3, int i4, int  i5, int i6, int i7, int i8) {
        return ((i1 + i2 + i3  + i4)  * i7) ^ i8;
    }

    public void test_sparcRegression() {
        compileMethod("callWithLotsOfArguments");
        compileMethod(jtt.jni.JNI_OverflowArguments.class, "read1");
    }

    // Copy of java.lang.StringCode.scale
    public static int scale(int len, float expansionFactor) {
        // We need to perform double, not float, arithmetic; otherwise
        // we lose low order bits when len is larger than 2**24.
        return (int) (len * (double) expansionFactor);
    }

    public void test_StringCoding() {
        // compileMethod("callerSaveMethod", SignatureDescriptor.create(float.class, float.class));
        compileMethod("scale", SignatureDescriptor.create(int.class, int.class, float.class));
    }

    public void test_compileDynamicLinker() {
        compileMethod(DynamicLinker.class, "doLoad");
    }

    public void test_FloatingPointRegisterAliasing() {
        compileMethod("scale", SignatureDescriptor.create(int.class, int.class, float.class));
    }

    /**
     * Exposed CIR->DIR translation bug.
     * Two assignments get effectively transposed,
     * because the intermediate variable that maintains a value from before the first assignment is not reified.
     */
    public void test_loopConfusion() {
        final Method_Type method = compileMethod("loopConfusion", SignatureDescriptor.create(int.class, int.class));
        if (!(method instanceof TargetMethod)) {
            final Value result = execute(method, IntValue.from(2));
            assertTrue(result.asInt() == 1);
        }
    }

    public void test_BytecodeToTargetTranslator() {
        compileClass(BytecodeToTargetTranslator.class);
    }

    public void test_InvocationStub() {
        final ClassMethodActor method = ClassActor.fromJava(Pointer.class).findLocalVirtualMethodActor("readIntAtIntOffset");
        final ClassActor stubClassActor = ClassActor.fromJava(method.makeInvocationStub().getClass());
        for (ClassMethodActor invoke : stubClassActor.localVirtualMethodActors()) {
            if (!invoke.isInitializer()) {
                assert invoke.name.equals("invoke");
                compileMethod(invoke);
            }

        }
    }

    public void test_BigFloatParams() {
        final double answer = BigFloatParams01.test(0);
        final Method_Type method = compileMethod(BigFloatParams01.class, "test");
        if (!(method instanceof TargetMethod)) {
            final Value result = execute(method, IntValue.from(0));
            assertTrue(result.asDouble() == answer);
        }
    }

    @INLINE
    private static Object pop() {
        final Object value = VMRegister.getAbiStackPointer().getReference().toJava();
        VMRegister.addWordsToAbiStackPointer(1);
        return value;
    }

    public void athrow() {
        final RuntimeException e = UnsafeLoophole.cast(pop());
        throw e;
    }

    /**
     * Exercises CirDirTranslation to use cc as ce.
     */
    public void test_athrow() {
        compileMethod(CompilerTest_regressions.class, "athrow");
    }

    public void test_InetAddress_getAddressFromNameService() {
        compileMethod(InetAddress.class, "getAddressFromNameService");
    }

    private static void foo() {
    }

    private static synchronized void sync() {
        foo();
    }

    public void test_sync() {
        compileMethod(CompilerTest_regressions.class, "sync");
    }

    private static Throwable terminationCause;

    public void test_SubroutineInliner_rewriteOneSubroutine() {
        compileMethod(SubroutineInliner.class, "rewriteOneSubroutine");
    }

    public void test_BirGenerator_getIrMethod() {
        compileMethod(BirGenerator.class, "createIrMethod", SignatureDescriptor.create(IrMethod.class, ClassMethodActor.class));
    }

    public static void finallyCatch() {
        try {
            try {
                //
            } finally {
                if (terminationCause != null) {
                    throw terminationCause;
                }
            }
        } catch (Throwable throwable) {
        }
    }

    public void test_VMThreadRun() {
        compileMethod(CompilerTest_regressions.class, "finallyCatch");
    }

    private void multipleCE() {
        try {
            new ZipFile("");
        } catch (IOException ioException) {
        }
    }

    public void test_ObjectOutputStream() {
        compileMethod(java.io.ObjectOutputStream.class, "writeArray", SignatureDescriptor.create(void.class, Object.class, ObjectStreamClass.class, boolean.class));
    }

    /**
     * Exposed CIR->DIR translation bug. Block contains multiple exception continuation parameters.
     */
    public void test_multipleCE() {
        compileMethod("multipleCE", SignatureDescriptor.create(void.class));
    }

    /**
     * Exposed AMD64 EIR register allocation bug. Several immediates with the same value are assigned to the same
     * variable. The r-values of all these assignments must be made identical, not just equal, when propagating
     * locations in the allocator, so that it can remove all the redundant assignments and does not incur allocation
     * constraint violations, having a immediates as l-value.
     */
    public void test_gregorianCalendar() {
        compileMethod(java.util.GregorianCalendar.class, "getActualMaximum");
    }

    @INLINE
    private static Pointer inner(final Pointer stackPointer) {
        return stackPointer.roundedUpBy(1024).minus(12);
    }

    public static Pointer outer(Pointer p) {
        return inner(p);
    }

    /**
     * Exposed CIR->DIR translation bug. Missing DirReturn.
     */
    public void test_inlinedRoundedUpBy() {
        final Method_Type method = compileMethod("outer", SignatureDescriptor.create(Pointer.class, Pointer.class));
        if (!(method instanceof TargetMethod)) {
            final Value result = execute(method, new WordValue(Pointer.fromInt(957)));
            assertTrue(result.asWord().asOffset().toLong() == 1012L);
        }
    }

    /**
     * Exposed bug in AMD64 register allocation. Failed to assign a required register (RCX for a shift operation) to an
     * EirValue, because it was deemed live throughout the entire method.
     */
    public void test_FloatingDecimal_dtoa() {
        final ClassMethodActor methodActor = getClassMethodActor(FloatingDecimal.class, "dtoa", SignatureDescriptor.create(void.class, int.class, long.class, int.class));
        compileMethod(methodActor);
    }

    /**
     * Exposed bug in removal of "unreachable" exception handling code.
     */
    public void test_JniFunctions_reserved() {
        compileMethod(getClassMethodActor(JniFunctions.class, "reserved0", SignatureDescriptor.create(Void.TYPE, Pointer.class)));
        compileMethod(getClassMethodActor(JniFunctions.class, "reserved1", SignatureDescriptor.create(Void.TYPE, Pointer.class)));
        compileMethod(getClassMethodActor(JniFunctions.class, "reserved2", SignatureDescriptor.create(Void.TYPE, Pointer.class)));
        compileMethod(getClassMethodActor(JniFunctions.class, "reserved3", SignatureDescriptor.create(Void.TYPE, Pointer.class)));
    }

    /**
     * Exposed bug in AMD64 backend caused by inlining of a block that takes the address of a local variable (i.e.
     * creates a stack handle). The aliasing between handles and the original local variables assumed that there would
     * never be more than one alias for a variable which is not true when blocks are inlined.
     */
    public void test_Proxy_defineClass0() {
        final ClassMethodActor methodActor = getClassMethodActor(java.lang.reflect.Proxy.class, "defineClass0", SignatureDescriptor.create(Class.class, ClassLoader.class, String.class, byte[].class,
                        int.class, int.class));
        compileMethod(methodActor);
    }

    /**
     * Compiling this method took more time than one would want to wait for, due to some continuation replication
     * explosion. This has been fixed by encapsulating CirSwitch continuation arguments by blocks before inlining a
     * block. Now compilation time is linear.
     *
     * @see CirBlock.inline()
     */
    private static void repeatIfInstanceOf(Object object) {
        if (object instanceof Byte) {
            return;
        }
        if (object instanceof Boolean) {
            return;
        }
        if (object instanceof Short) {
            return;
        }
        if (object instanceof Character) {
            return;
        }
        if (object instanceof Integer) {
            return;
        }
        if (object instanceof Float) {
            return;
        }
        if (object instanceof Long) {
            return;
        }
        if (object instanceof Double) {
            return;
        }
        if (object instanceof WordValue) {
            return;
        }
    }

    /**
     * Revealed bug in bytecode preprocessing which caused the result to not verify.
     */
    public void test_HttpURLConnection_getInputStream() {
        compileMethod(sun.net.www.protocol.http.HttpURLConnection.class, "getInputStream");
    }

    public void test_fromBoxedJavaValue() {
        final ClassMethodActor methodActor = getClassMethodActor("repeatIfInstanceOf", SignatureDescriptor.create(void.class, Object.class));
        compileMethod(methodActor);
    }

    public void test_BytecodePrinter_epilog1() {
        final ClassMethodActor methodActor = getClassMethodActor(BytecodePrinter.class, "epilog", SignatureDescriptor.create(void.class));
        compileMethod(methodActor);
    }

    public void test_Memory_clear() {
        final ClassMethodActor methodActor = getClassMethodActor(Memory.class, "clearBytes", SignatureDescriptor.create(void.class, Pointer.class, Size.class));
        compileMethod(methodActor);
    }

    public void test_BytecodePrinter_epilog() {
        final ClassMethodActor methodActor = getClassMethodActor(BytecodePrinter.class, "epilog", SignatureDescriptor.create(void.class));
        compileMethod(methodActor);
    }

    public void test_TupleClassActor_findClassMethodMethodActor() {
        final ClassMethodActor methodActor = getClassMethodActor(ClassActor.class, "findClassMethodActor", SignatureDescriptor.create(ClassMethodActor.class, Utf8Constant.class, SignatureDescriptor.class));
        compileMethod(methodActor);
    }

    private void m(int i) {
    }

    private int perform_join() {
        try {
            m(0);
        } catch (Exception e) {
        }
        try {
            m(1);
        } catch (Exception e) {
        }
        try {
            m(2);
        } catch (Exception e) {
        }
        try {
            m(3);
        } catch (Exception e) {
        }
        return 100;
    }

    public void test_continuation_sharing() {
        final ClassMethodActor methodActor = getClassMethodActor("perform_join", SignatureDescriptor.create(int.class));
        compileMethod(methodActor);
    }

    public void test_CirGenerator_SWITCH_TABLE_com_sun_max_vm_type_KindEnum() {
        final ClassMethodActor methodActor = getEnumSwitchTranslationTableInitializer(BytecodeTranslation.class, KindEnum.class);
        if (methodActor != null) {
            compileMethod(methodActor);
        }
    }

    public void test_Classpath_readClassFile() {
        final ClassMethodActor methodActor = getClassMethodActor(Classpath.class, "readClassFile", SignatureDescriptor.create(ClasspathFile.class, String.class));
        compileMethod(methodActor);
    }

}

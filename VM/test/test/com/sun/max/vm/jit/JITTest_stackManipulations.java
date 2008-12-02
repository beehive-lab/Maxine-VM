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
package test.com.sun.max.vm.jit;

import test.com.sun.max.vm.compiler.*;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.prototype.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.template.source.*;

/**
 * Playing with Stack manipulation routines for a template generator that can be used either by a simple JIT or an interpreter.
 *
 * @author Laurent Daynes
 */
public class JITTest_stackManipulations extends CompilerTestCase<TargetMethod> {

    public static final class JavaStack {
        private JavaStack() {
        }

        @INLINE
        static void modifyStack() {
            Pointer sp = VMRegister.getAbiStackPointer();
            sp = sp.plusWords(10);
            VMRegister.setAbiStackPointer(sp);
        }

        @INLINE
        static Pointer doNotModifyStack() {
            Pointer sp = VMRegister.getAbiStackPointer();
            sp = sp.plusWords(10);
            return sp;
        }

        @INLINE
        static int popInt1() {
            final Pointer sp = VMRegister.getAbiStackPointer();
            final int value = sp.readInt(Ints.SIZE);
            sp.plusWords(1);
            return value;
        }

        @INLINE
        static int popInt2() {
            final Pointer sp = VMRegister.getAbiStackPointer();
            final int value = sp.readInt(0);
            SpecialBuiltin.addWordsToIntegerRegister(VMRegister.Role.CPU_STACK_POINTER, 1);
            return value;
        }

        @INLINE
        static int popInt4() {
            final int value = VMRegister.getAbiStackPointer().getInt();
            // SpecialBuiltin.addWordsToRegister(VMRegister.Role.JAVA_STACK, 4);
            return value;
        }

        @INLINE
        static void pushInt1(final int value) {
            final Pointer sp = VMRegister.getAbiStackPointer();
            // SpecialBuiltin.addWordsToRegister(VMRegister.Role.JAVA_STACK, -1);
            sp.writeInt(0, value);
        }

        @INLINE
        static void pushInt2(final int value) {
            final Pointer sp = VMRegister.getAbiStackPointer();
            // SpecialBuiltin.addWordsToRegister(VMRegister.Role.JAVA_STACK, -1);
            sp.writeInt(-Ints.SIZE, value);
        }

        @INLINE
        static void pushInt3(final int value) {
            // SpecialBuiltin.addWordsToRegister(VMRegister.Role.JAVA_STACK, -1);
            JitStackFrameOperation.pokeInt(0, value);
        }
    }

    public int popInt1() {
        return JavaStack.popInt1();
    }

    public int popInt2() {
        return JavaStack.popInt2();
    }

    public int popInt4() {
        return JavaStack.popInt4();
    }

    //  Checkstyle: stop final variable check
    public void pushInt1() {
        int value = 0; // not final on purpose.
        JavaStack.pushInt1(value);
    }
    //  Checkstyle: resume final variable check

    public void pushInt2(final int value) {
        JavaStack.pushInt1(value);
    }

    public void pushInt4(final int value) {
        JavaStack.pushInt3(value);
    }

    private Class initializeClassInTarget(final Class javaClass) {
        return MaxineVM.usingTarget(new Function<Class>() {
            public Class call() {
                final Class targetClass = Classes.load(PrototypeClassLoader.PROTOTYPE_CLASS_LOADER, javaClass.getName());
                final TupleClassActor tupleClassActor = (TupleClassActor) ClassActor.fromJava(targetClass);
                tupleClassActor.makeInitialized();
                return targetClass;
            }
        });
    }

    private static final int JIT_SLOT_SIZE = 8;
    private static final int SLOT_WORDS = JIT_SLOT_SIZE / 8;
    private static final int BIAS = 0;

    @INLINE
    static void removeSlots(int numberOfSlots) {
        VMRegister.addWordsToAbiStackPointer(numberOfSlots * SLOT_WORDS);
    }

    @INLINE
    static int peekInt1(int index) {
        return VMRegister.getAbiStackPointer().readInt(BIAS + (index * JIT_SLOT_SIZE));
    }

    @INLINE
    public static void pokeInt1(int index, int value) {
        VMRegister.getAbiStackPointer().writeInt(BIAS + (index * JIT_SLOT_SIZE), value);
    }

    @INLINE
    static int peekInt2(int index) {
        return VMRegister.getAbiStackPointer().readInt(index * JIT_SLOT_SIZE);
    }

    @INLINE
    public static void pokeInt2(int index, int value) {
        VMRegister.getAbiStackPointer().writeInt(index * JIT_SLOT_SIZE, value);
    }

    public void imul1() {
        final int value2 = peekInt1(0);
        final int value1 = peekInt1(1);
        removeSlots(1);
        pokeInt1(0, value1 * value2);
    }


    public void imul2() {
        final int value2 = peekInt2(0);
        final int value1 = peekInt2(1);
        removeSlots(1);
        pokeInt2(0, value1 * value2);
    }
/*
    public void test_imul() {
        compileMethod("imul1", SignatureDescriptor.create(void.class));

        compileMethod("imul2", SignatureDescriptor.create(void.class));
    }*/

    public long getHashcCode1(int h, int s) {
        return  Address.fromUnsignedInt(h).shiftedLeft(s).toLong();
    }

    public void test_h1() {
        compileMethod(JITTest_stackManipulations.class, "getHashcCode1");
    }
/*
    public void test_stack() {
        initializeClassInTarget(JavaStack.class);

        final TargetMethod method1 = compileMethod(JavaStack.class, "modifyStack", SignatureDescriptor.create(void.class));
        disassemble(method1);

        final TargetMethod method2 = compileMethod(JavaStack.class, "doNotModifyStack", SignatureDescriptor.create(Pointer.class));
        disassemble(method2);
    }

    public void test_pushInt() {
          // Make sure the JavaStack is initialized in the target so that its static final variable can fold when compiling the template generators.
        initializeClassInTarget(JavaStack.class);

        // Now compile the method we're interrested in
        final TargetMethod method1 = compileMethod("pushInt1", SignatureDescriptor.create(void.class));
        disassemble(method1);

        final TargetMethod method2 = compileMethod("pushInt2", SignatureDescriptor.create(void.class, int.class));
        disassemble(method2);

//        final TargetMethod method3 = compileMethod("pushInt3", SignatureDescriptor.create(void.class, int.class));
//        disassemble(method3);
//
//        final TargetMethod method4 = compileMethod("pushInt4", SignatureDescriptor.create(void.class, int.class));
//        disassemble(method4);
    }

    public void test_popInt() {

        // Make sure the JavaStack is initialized in the target so that its static final variable can fold when compiling the template generators.
        initializeClassInTarget(JavaStack.class);

        // Now compile the method we're interrested in
        //final TargetMethod method1 = compileMethod("popInt1", SignatureDescriptor.create(int.class));
        //method1.disassemble();

        //final TargetMethod method2 = compileMethod("popInt2", SignatureDescriptor.create(int.class));
        //method2.disassemble();

//        final TargetMethod method3 = compileMethod("popInt3", SignatureDescriptor.create(int.class));
//        disassemble(method3);
        final TargetMethod method4 = compileMethod("popInt4", SignatureDescriptor.create(int.class));
        disassemble(method4);
    }*/
}



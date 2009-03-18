/*
 * Copyright (c) 2008 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.max.vm.interpreter;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;

/**
 * This class implements the semantics of bytecode operations, independent
 * of stack layout. It therefore provides a cleaner, more consistent view
 * of Java bytecode semantics without the entanglement with the stack.
 *
 * One idea is to reuse the machine code that results from compiling these methods
 * as templates for a new template-based JIT compiler. This would allow runtime
 * portability (since these methods are compiled with the optimzing compiler), while
 * the new template-based JIT would be responsible for removing the Java operand
 * stack and performing a very simple register allocation and code selection
 * algorithm. This could yield perhaps 2x-3x code improvement, while still keeping
 * compilation time very low
 *
 * @author Ben L. Titzer
 */
@Hypothetical
public class BytecodeSemantics {

    // =============================================================================
    // ========================= Array operations ==================================
    // =============================================================================
    public boolean booleanArrayLoad(boolean[] array, int index) {
        return array[index];
    }

    public void booleanArrayStore(boolean[] array, int index, boolean val) {
        array[index] = val;
    }

    public byte byteArrayLoad(byte[] array, int index) {
        return array[index];
    }

    public void byteArrayStore(byte[] array, int index, byte val) {
        array[index] = val;
    }

    public short shortArrayLoad(short[] array, int index) {
        return array[index];
    }

    public void shortArrayStore(short[] array, int index, short val) {
        array[index] = val;
    }

    public char charArrayLoad(char[] array, int index) {
        return array[index];
    }

    public void charArrayStore(char[] array, int index, char val) {
        array[index] = val;
    }

    public int intArrayLoad(int[] array, int index) {
        return array[index];
    }

    public void intArrayStore(int[] array, int index, int val) {
        array[index] = val;
    }

    public long longArrayLoad(long[] array, int index) {
        return array[index];
    }

    public void longArrayStore(long[] array, int index, long val) {
        array[index] = val;
    }

    public float floatArrayLoad(float[] array, int index) {
        return array[index];
    }

    public void floatArrayStore(float[] array, int index, float val) {
        array[index] = val;
    }

    public double doubleArrayLoad(double[] array, int index) {
        return array[index];
    }

    public void doubleArrayStore(double[] array, int index, double val) {
        array[index] = val;
    }

    public Object objectArrayLoad(Object[] array, int index) {
        return array[index];
    }

    public void objectArrayStore(Object[] array, int index, Object val) {
        array[index] = val;
    }

    // =============================================================================
    // ========================= Field operations ==================================
    // =============================================================================
    public boolean booleanGetField(Object object, ResolutionGuard guard) {
        final FieldActor fieldActor = ResolutionSnippet.ResolveInstanceFieldForReading.resolveInstanceFieldForReading(guard);
        return FieldReadSnippet.ReadBoolean.readBoolean(object, UnsafeLoophole.<BooleanFieldActor>cast(fieldActor));
    }

    public boolean booleanGetField(Object object, int offset) {
        return Reference.fromJava(object).readBoolean(offset);
    }


    // =============================================================================
    // ========================= Float operations ==================================
    // =============================================================================
    public float floatPlus(float a, float b) {
        return a + b;
    }

    public float floatMinus(float a, float b) {
        return a - b;
    }

    public float floatTimes(float a, float b) {
        return a * b;
    }

    public float floatDivide(float a, float b) {
        return a / b;
    }

    public float floatModulus(float a, float b) {
        return a % b;
    }

    public int floatCompareG(float a, float b) {
        return FloatCompareG.floatCompareG(a, b); // TODO: really need to insert the FCMPG bytecode, which is not accessible at the Java source level
    }

    public int floatCompareL(float a, float b) {
        return FloatCompareL.floatCompareL(a, b); // TODO: really need to insert the FCMPL bytecode, which is not accessible at the Java source level
    }

    // =============================================================================
    // ========================= Double operations ==================================
    // =============================================================================
    public double doublePlus(double a, double b) {
        return a + b;
    }

    public double doubleMinus(double a, double b) {
        return a - b;
    }

    public double doubleTimes(double a, double b) {
        return a * b;
    }

    public double doubleDivide(double a, double b) {
        return a / b;
    }

    public double doubleModulus(double a, double b) {
        return a % b;
    }

    public int doubleCompareG(double a, double b) {
        return DoubleCompareG.doubleCompareG(a, b); // TODO: really need to insert the DCMPG bytecode, which is not accessible at the Java source level
    }

    public int doubleCompareL(double a, double b) {
        return DoubleCompareL.doubleCompareL(a, b); // TODO: really need to insert the DCMPL bytecode, which is not accessible at the Java source level
    }

    // =============================================================================
    // ========================= Object allocation =================================
    // =============================================================================
    public Object newObject(ClassActor classActor) {
        return NonFoldableSnippet.CreateTupleOrHybrid.createTupleOrHybrid(classActor);
    }
    public Object newObject(ResolutionGuard guard) {
        final ClassActor classActor = ResolutionSnippet.ResolveClass.resolveClass(guard);
        return NonFoldableSnippet.CreateTupleOrHybrid.createTupleOrHybrid(classActor);
    }
}

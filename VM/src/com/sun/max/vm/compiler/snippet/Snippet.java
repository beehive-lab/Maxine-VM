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
package com.sun.max.vm.compiler.snippet;

import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.object.host.*;
import com.sun.max.vm.runtime.*;

/**
 * Snippets are subroutines for byte code implementations that are written in Java. They are
 * compiled at VM build time and are statically "pre-optimized" (along with the rest of the
 * VM core).
 *
 * Expressing most bytecode semantics at this level greatly simplifies the compiler interface.
 *
 * @author Bernd Mathiske
 */
public abstract class Snippet extends IrRoutine {

    private static final AppendableIndexedSequence<Snippet> _snippets = new ArrayListSequence<Snippet>();

    public static IndexedSequence<Snippet> snippets() {
        return _snippets;
    }

    private final int _serial;

    public final int serial() {
        return _serial;
    }

    public Snippet() {
        super(null);
        _serial = _snippets.length();
        _snippets.append(this);
        foldingMethodActor().beUnsafe();
    }

    @Override
    public String toString() {
        return "<snippet: " + name() + ">";
    }

    @PROTOTYPE_ONLY
    public static void register() {
        for (Snippet snippet : _snippets) {
            final MethodActor  foldingMethodActor = snippet.foldingMethodActor();
            if (foldingMethodActor != null) {
                MaxineVM.registerImageInvocationStub(foldingMethodActor);
            }
        }
    }

    // Miscellaneous Snippets:

    /**
     * Ensures that the class in which a given static method or field is declared is initialized, performing class
     * initialization if necessary.
     */
    public static final class MakeHolderInitialized extends Snippet {
        @SNIPPET
        @INLINE
        public static void makeHolderInitialized(MemberActor memberActor) {
            MakeClassInitialized.makeClassInitialized(memberActor.holder());
        }
        public static final MakeHolderInitialized SNIPPET = new MakeHolderInitialized();
    }

    /**
     * Ensures that a given class is initialized, performing class initialization if necessary.
     */
    public static final class MakeClassInitialized extends Snippet {
        @SNIPPET
        @INLINE
        public static void makeClassInitialized(ClassActor classActor) {
            if (MaxineVM.isPrototyping()) {
                classActor.makeInitialized();
            } else if (!classActor.isInitialized()) {
                if (!classActor.isInitialized()) {
                    classActor.makeInitialized();
                }
            }
        }
        public static final MakeClassInitialized SNIPPET = new MakeClassInitialized();
    }

    /**
     * Produces an address corresponding to the entry point for the code of a given method
     * as compiled by the {@linkplain CallEntryPoint#OPTIMIZED_ENTRY_POINT optimizing compiler}.
     *
     * If the compiled code does not yet exist for the method, it is compiled with the
     * {@linkplain CompilationDirective#DEFAULT default} compiler.
     */
    public static final class MakeEntrypoint extends Snippet {
        @SNIPPET
        public static Address makeEntrypoint(ClassMethodActor classMethodActor) {
            return CompilationScheme.Static.compile(classMethodActor, CallEntryPoint.OPTIMIZED_ENTRY_POINT, CompilationDirective.DEFAULT);
        }
        public static final MakeEntrypoint SNIPPET = new MakeEntrypoint();
    }

    /**
     * Produces an address corresponding to the entry point for the code of a given method
     * as compiled by the {@linkplain CallEntryPoint#OPTIMIZED_ENTRY_POINT optimizing compiler}.
     *
     * If the compiled code does not yet exist for the method, it is compiled with the
     * with a compiler that inserts {@linkplain CompilationDirective#TRACE_JIT tracing} instrumentation.
     */
    public static final class MakeTracedEntrypoint extends Snippet {
        @SNIPPET
        public static Address makeTracedEntrypoint(ClassMethodActor classMethodActor) {
            return CompilationScheme.Static.compile(classMethodActor, CallEntryPoint.OPTIMIZED_ENTRY_POINT, CompilationDirective.TRACE_JIT);
        }
        public static final MakeTracedEntrypoint SNIPPET = new MakeTracedEntrypoint();
    }

    public static final class CheckCast extends Snippet {
        @SNIPPET
        @INLINE(afterSnippetsAreCompiled = true)
        public static void checkCast(ClassActor classActor, Object object) {
            if (MaxineVM.isPrototyping()) {
                if (object != null && !classActor.toJava().isAssignableFrom(object.getClass())) {
                    Throw.classCastException();
                }
            } else if (!classActor.isNullOrInstance(object)) {
                Throw.classCastException();
            }
        }
        public static final CheckCast SNIPPET = new CheckCast();
    }

    public static final class InstanceOf extends Snippet {
        @SNIPPET
        @INLINE(afterSnippetsAreCompiled = true)
        public static boolean instanceOf(ClassActor classActor, Object object) {
            if (MaxineVM.isPrototyping()) {
                return object != null && classActor.toJava().isAssignableFrom(object.getClass());
            }
            return classActor.isNonNullInstance(object);
        }
        public static final InstanceOf SNIPPET = new InstanceOf();
    }

    public static final class CheckArrayIndex extends Snippet {
        @SNIPPET
        @INLINE
        public static void checkArrayIndex(Object array, int index) throws ArrayIndexOutOfBoundsException {
            if (MaxineVM.isPrototyping()) {
                HostArrayAccess.checkIndex(array, index);
            } else {
                ArrayAccess.inlineCheckIndex(array, index);
            }
        }
        public static final CheckArrayIndex SNIPPET = new CheckArrayIndex();
    }

    public static final class CheckReferenceArrayStore extends Snippet {
        @SNIPPET
        @INLINE
        public static void checkReferenceArrayStore(Object array, Object value) throws ArrayIndexOutOfBoundsException {
            if (MaxineVM.isPrototyping()) {
                HostArrayAccess.checkSetObject(array, value);
            } else {
                ArrayAccess.checkSetObject(array, value);
            }
        }
        public static final CheckReferenceArrayStore SNIPPET = new CheckReferenceArrayStore();
    }

    public static final class CheckNullPointer extends Snippet {
        @SNIPPET
        @INLINE
        public static void checkNullPointer(Object object) throws NullPointerException {
            if (MaxineVM.isPrototyping()) {
                noninlineCheckNullPointer(object);
            }
            // null checks are implicitly checked
        }

        public static final CheckNullPointer SNIPPET = new CheckNullPointer();
    }

    @NEVER_INLINE
    static void noninlineCheckNullPointer(Object object) {
        if (object == null) {
            throw new NullPointerException();
        }
    }

    public static final class ConvertFloatToInt extends Snippet {
        private static final float MAX_FLOAT_VALUE =  -1 >>> 1;
        @SNIPPET
        public static int convertFloatToInt(float value) {
            if (value < MAX_FLOAT_VALUE) {
                return IEEE754Builtin.ConvertFloatToInt.convertFloatToInt(value);
            }
            if (value >= MAX_FLOAT_VALUE) {
                return -1 >>> 1;
            }
            return 0;
        }
        public static final ConvertFloatToInt SNIPPET = new ConvertFloatToInt();
    }

    public static final class ConvertFloatToLong extends Snippet {
        private static final float MAX_FLOAT_VALUE = ((long) -1) >>> 1;
        @SNIPPET
        public static long convertFloatToLong(float value) {
            if (value < MAX_FLOAT_VALUE) {
                return IEEE754Builtin.ConvertFloatToLong.convertFloatToLong(value);
            }
            if (value >= MAX_FLOAT_VALUE) {
                return ((long) -1) >>> 1;
            }
            return 0;
        }
        public static final ConvertFloatToLong SNIPPET = new ConvertFloatToLong();
    }

    public static final class ConvertDoubleToInt extends Snippet {
        private static final double MAX_DOUBLE_VALUE = -1 >>> 1;
        @SNIPPET
        public static int convertDoubleToInt(double value) {
            if (value < MAX_DOUBLE_VALUE) {
                return IEEE754Builtin.ConvertDoubleToInt.convertDoubleToInt(value);
            }
            if (value >= MAX_DOUBLE_VALUE) {
                return -1 >>> 1;
            }
            return 0;
        }
        public static final ConvertDoubleToInt SNIPPET = new ConvertDoubleToInt();
    }

    public static final class ConvertDoubleToLong extends Snippet {
        private static final double MAX_DOUBLE_VALUE = ((long) -1) >>> 1;
        @SNIPPET
        public static long convertDoubleToLong(double value) {
            if (value < MAX_DOUBLE_VALUE) {
                return IEEE754Builtin.ConvertDoubleToLong.convertDoubleToLong(value);
            }
            if (value >= MAX_DOUBLE_VALUE) {
                return ((long) -1) >>> 1;
            }
            return 0;
        }
        public static final ConvertDoubleToLong SNIPPET = new ConvertDoubleToLong();
    }


    /*
     * Snippets that are used in lieu of a builtin when it is not yet implemented by the compiler scheme:
     */

    public static final class LongCompare extends Snippet {
        @C_FUNCTION
        private static native int nativeLongCompare(long greater, long less);

        @SNIPPET
        public static int longCompare(long greater, int less) {
            if (MaxineVM.isPrototyping()) {
                if (greater > less) {
                    return 1;
                }
                if (greater == less) {
                    return 0;
                }
                return -1;
            }
            return nativeLongCompare(greater, less);
        }

        public static final LongCompare SNIPPET = new LongCompare();
    }

    public static final class LongSignedShiftedRight extends Snippet {
        @C_FUNCTION
        private static native long nativeLongSignedShiftedRight(long number, int shift);

        @SNIPPET
        public static long longSignedShiftedRight(long number, int shift) {
            if (MaxineVM.isPrototyping()) {
                return number >> shift;
            }
            return nativeLongSignedShiftedRight(number, shift);
        }

        public static final LongSignedShiftedRight SNIPPET = new LongSignedShiftedRight();
    }

    public static final class LongTimes extends Snippet {
        @C_FUNCTION
        private static native long nativeLongTimes(long factor1, long factor2);

        @SNIPPET
        public static long longTimes(long factor1, long factor2) {
            if (MaxineVM.isPrototyping()) {
                return factor1 * factor2;
            }
            return nativeLongTimes(factor1, factor2);
        }

        public static final LongTimes SNIPPET = new LongTimes();
    }

    public static final class LongDivided extends Snippet {
        @C_FUNCTION
        private static native long nativeLongDivided(long dividend, long divisor);

        @SNIPPET
        public static long longDivided(long dividend, long divisor) {
            if (MaxineVM.isPrototyping()) {
                return dividend / divisor;
            }
            return nativeLongDivided(dividend, divisor);
        }

        public static final LongDivided SNIPPET = new LongDivided();
    }

    public static final class LongRemainder extends Snippet {
        @C_FUNCTION
        private static native long nativeLongRemainder(long dividend, long divisor);

        @SNIPPET
        public static long longRemainder(long dividend, long divisor) {
            if (MaxineVM.isPrototyping()) {
                return dividend % divisor;
            }
            return nativeLongRemainder(dividend, divisor);
        }

        public static final LongRemainder SNIPPET = new LongRemainder();
    }

    public static final class FloatRemainder extends Snippet {
        @C_FUNCTION
        private static native float nativeFloatRemainder(float dividend, float divisor);

        @SNIPPET
        public static float floatRemainder(float dividend, float divisor) {
            if (MaxineVM.isPrototyping()) {
                return dividend % divisor;
            }
            return nativeFloatRemainder(dividend, divisor);
        }

        public static final FloatRemainder SNIPPET = new FloatRemainder();
    }

    public static final class DoubleRemainder extends Snippet {
        @C_FUNCTION
        private static native double nativeDoubleRemainder(double dividend, double divisor);

        @SNIPPET
        public static double doubleRemainder(double dividend, double divisor) {
            if (MaxineVM.isPrototyping()) {
                return dividend % divisor;
            }
            return nativeDoubleRemainder(dividend, divisor);
        }

        public static final DoubleRemainder SNIPPET = new DoubleRemainder();
    }

    public static final class CheckArrayDimension extends Snippet {
        @SNIPPET
        public static void checkArrayDimension(int length) {
            if (length < 0) {
                throw new NegativeArraySizeException();
            }
        }
        public static final CheckArrayDimension SNIPPET = new CheckArrayDimension();
    }


}

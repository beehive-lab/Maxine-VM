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
package com.sun.max.vm.compiler.cir.operator;

import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.b.c.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.builtin.SafepointBuiltin.*;
import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.compiler.cir.builtin.*;
import com.sun.max.vm.compiler.cir.snippet.*;
import com.sun.max.vm.compiler.cir.transform.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.compiler.snippet.Snippet.*;
import com.sun.max.vm.type.*;


/**
 * High Level CIR Operator.  This class, its subclasses, and {@link JavaBuiltin}s comprise
 * the operators that can appear in the procedure position of a {@link CirCall}.
 *
 * @author Yi Guo
 * @author Aziz Ghuloum
 */
public abstract class JavaOperator extends CirOperator {

    protected int _reasonsMayStop;

    /**
     * Creates an object for the application of an operator.
     *
     * @param reasonsMayStop specifies the reasons the translation of this operator may produce one or more
     *            {@linkplain Stoppable stops}
     */
    protected JavaOperator(int reasonsMayStop) {
        _reasonsMayStop = reasonsMayStop;
    }

    public final int reasonsMayStop() {
        return _reasonsMayStop;
    }

    /**
     * Removes one of the reasons this operator may stop.
     *
     * @param reasonMayStop exactly one of the flags defined in {@link Stoppable}
     */
    public final void removeReasonMayStop(int reasonMayStop) {
        assert reasonMayStop != 0 && Ints.isPowerOfTwoOrZero(reasonMayStop & Stoppable.Static.ALL_REASONS) : "Exactly one reason for stopping must be removed at a time";
        _reasonsMayStop &= ~reasonMayStop;
    }

    @Override
    public MethodActor foldingMethodActor() {
        throw Problem.unimplemented();
    }

    @Override
    public String name() {
        return "CirOperator";
    }

    @Override
    public void acceptVisitor(CirVisitor visitor) {
        visitor.visitHCirOperator(this);
    }

    public void acceptVisitor(HCirOperatorVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public abstract String toString();

    /**
     * Base class for all the Java operators that have a resolvable constant pool entry.
     *
     * @author Doug Simon
     */
    public abstract static class JavaResolvableOperator<Actor_Type extends Actor> extends JavaOperator {
        /**
         * The constant pool entry index.
         */
        protected final int _index;

        /**
         * The constant pool.
         */
        protected final ConstantPool _constantPool;

        protected final Kind _resultKind;

        protected Actor_Type _actor;

        public JavaResolvableOperator(int reasonsMayStop, ConstantPool constantPool, int index, Kind resultKind) {
            super(reasonsMayStop);
            assert (reasonsMayStop & CALL) != 0 : "operator translated by one more snippets must indicate CALL as a stop reason";
            _constantPool = constantPool;
            _index = index;
            _resultKind = resultKind;
            if (constantPool != null) {
                final ResolvableConstant constant = constantPool.resolvableAt(index);
                if (constant.isResolved() || constant.isResolvableWithoutClassLoading(constantPool)) {
                    try {
                        resolve();
                    } catch (PrototypeOnlyFieldError prototypeOnlyFieldError) {
                        // Suppress: will have to be dealt with when 'resolve()' is called
                    } catch (PrototypeOnlyMethodError prototypeOnlyMethodError) {
                        // Suppress: will have to be dealt with when 'resolve()' is called
                    }
                }
            }
        }

        /**
         * Gets the constant pool containing the entry referenced by this operator.
         */
        public final ConstantPool constantPool() {
            return _constantPool;
        }

        /**
         * Gets the constant pool entry referenced by this operator.
         */
        public final ResolvableConstant constant() {
            return _constantPool.resolvableAt(_index);
        }

        /**
         * Gets the index of the constant pool entry referenced by this operator.
         */
        public final int index() {
            return _index;
        }

        /**
         * Gets the resolved actor object corresponding to the constant pool entry referenced by this operator.
         * This method will return {@code null} if the entry has not been {@linkplain #isResolved() resolved}.
         */
        public final Actor_Type actor() {
            return _actor;
        }

        /**
         * Determines if the constant pool entry referenced by this operator has been resolved.
         */
        public final boolean isResolved() {
            return _actor != null;
        }

        /**
         * Resolves the constant pool entry referenced by this operator. The resolution only happens once.
         */
        public void resolve() {
            final Class<Actor_Type> type = null;
            _actor = StaticLoophole.cast(type, _constantPool.resolvableAt(_index).resolve(_constantPool, _index));
        }

        public final Kind resultKind() {
            return _resultKind;
        }

        /**
         * Gets the kind of the field referenced by this operator.
         *
         * @throws VerifyError if this operator does not operate on a field
         */
        public final Kind fieldKind() {
            return _constantPool.fieldAt(_index).type(_constantPool).toKind();
        }

        /**
         * Determines if this operator includes a class initialization check.
         */
        public boolean requiresClassInitialization() {
            return false;
        }

        /**
         * Gets the class that must be initialized as a side effect of executing this operator.
         */
        private ClassActor classToBeInitialized() {
            assert requiresClassInitialization() && _actor != null;
            if (_actor instanceof MemberActor) {
                return ((MemberActor) _actor).holder();
            }
            return (ClassActor) _actor;
        }

        /**
         * Determines if the class to be initialized by this operator is indeed initialized. This must only be called
         * for operators that included a {@linkplain #requiresClassInitialization() class initialization} check.
         */
        public final boolean isClassInitialized() {
            assert requiresClassInitialization();
            if (!isResolved()) {
                return false;
            }
            return classToBeInitialized().isInitialized();
        }

        /**
         * Ensures that the class to be initialized by this operator is indeed initialized.
         * This must only be called for operators that included a {@linkplain #requiresClassInitialization() class initialization} check.
         */
        public final void initializeClass() {
            assert requiresClassInitialization();
            if (!isClassInitialized()) {
                if (!isResolved()) {
                    resolve();
                }
                MakeClassInitialized.makeClassInitialized(classToBeInitialized());
            }
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + ":" + _constantPool.at(_index).valueString(_constantPool);
        }
    }

    /**
     * This class is a work around for some {@link JavaBuiltin} classes that can appear
     * in the CIR.   We list them here explicitly instead of allowing all builtins to be
     * valid HCIR operators.
     *
     * @author Yi Guo
     * @author Aziz Ghuloum
     */
    private static final class JavaBuiltinOperator extends JavaOperator implements Lowerable {

        private final CirBuiltin _cirBuiltin;

        private JavaBuiltinOperator(Builtin builtin) {
            super(builtin.reasonsMayStop());
            _cirBuiltin = CirBuiltin.get(builtin);
        }

        public CirBuiltin builtin() {
            return _cirBuiltin;
        }

        @Override
        public Kind resultKind() {
            return _cirBuiltin.resultKind();
        }

        @Override
        public Kind[] parameterKinds() {
            return _cirBuiltin.parameterKinds();
        }

        @Override
        public void acceptVisitor(CirVisitor visitor) {
            visitor.visitHCirOperator(this);
        }

        @Override
        public void acceptVisitor(HCirOperatorVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public void toLCir(Lowerable op, CirCall call, CompilerScheme compilerScheme) {
            call.setProcedure(_cirBuiltin);
        }

        @Override
        public String toString() {
            return _cirBuiltin.name();
        }
    }

    /**
     * This class is a work around for some {@link Snippet}s that can appear in HCIR. They are wrapped with an instance
     * of this class instead of allowing all {@link Snippet}s to be valid HCIR operators.
     *
     * @author Yi Guo
     * @author Aziz Ghuloum
     */
    static final class JavaSnippetOperator extends JavaOperator implements Lowerable {
        private final CirSnippet _snippet;

        JavaSnippetOperator(Snippet snippet) {
            super(CALL);
            _snippet = CirSnippet.get(snippet);
        }

        public CirSnippet snippet() {
            return _snippet;
        }

        @Override
        public Kind resultKind() {
            return _snippet.resultKind();
        }

        @Override
        public Kind[] parameterKinds() {
            return _snippet.parameterKinds();
        }

        @Override
        public void acceptVisitor(CirVisitor visitor) {
            visitor.visitHCirOperator(this);
        }

        @Override
        public void acceptVisitor(HCirOperatorVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public void toLCir(Lowerable op, CirCall call, CompilerScheme compilerScheme) {
            call.setProcedure(_snippet);
        }

        @Override
        public String toString() {
            return _snippet.name();
        }
    }

    /**
     * Some operators may be lowered to either a Snippet call or a Builtin call depending on
     * whether or not the compiler backend knows how to handle it.
     *
     * @author Aziz Ghuloum
     */
    private static final class JavaBuiltinOrSnippetOperator extends JavaOperator implements Lowerable {
        private final Builtin _builtin;
        private final Snippet _snippet;
        private JavaBuiltinOrSnippetOperator(Builtin builtin, Snippet snippet) {
            super(VMConfiguration.target().compilerScheme().isBuiltinImplemented(builtin) ? builtin.reasonsMayStop() : CALL);
            _builtin = builtin;
            _snippet = snippet;
        }
        @Override
        public void toLCir(Lowerable op, CirCall call, CompilerScheme compilerScheme) {
            if (compilerScheme.isBuiltinImplemented(_builtin)) {
                call.setProcedure(CirBuiltin.get(_builtin));
            } else {
                call.setProcedure(CirSnippet.get(_snippet));
            }
        }
        @Override
        public Kind resultKind() {
            return CirBuiltin.get(_builtin).resultKind();
        }

        @Override
        public Kind[] parameterKinds() {
            return CirBuiltin.get(_builtin).parameterKinds();
        }

        @Override
        public void acceptVisitor(CirVisitor visitor) {
            visitor.visitHCirOperator(this);
        }

        @Override
        public void acceptVisitor(HCirOperatorVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public String toString() {
            return _builtin.name();
        }
    }

    public static final JavaOperator SAFEPOINT_OP = new JavaBuiltinOperator(SoftSafepoint.BUILTIN);

    public static final JavaOperator FLOAT_TO_INT = new JavaSnippetOperator(Snippet.ConvertFloatToInt.SNIPPET);
    public static final JavaOperator FLOAT_TO_LONG = new JavaSnippetOperator(Snippet.ConvertFloatToLong.SNIPPET);
    public static final JavaOperator FLOAT_TO_DOUBLE = new JavaBuiltinOperator(JavaBuiltin.ConvertFloatToDouble.BUILTIN);

    public static final JavaOperator FLOAT_COMPARE_L = new JavaBuiltinOperator(JavaBuiltin.FloatCompareL.BUILTIN);
    public static final JavaOperator FLOAT_COMPARE_G = new JavaBuiltinOperator(JavaBuiltin.FloatCompareG.BUILTIN);
    public static final JavaOperator FLOAT_PLUS = new JavaBuiltinOperator(JavaBuiltin.FloatPlus.BUILTIN);
    public static final JavaOperator FLOAT_MINUS = new JavaBuiltinOperator(JavaBuiltin.FloatMinus.BUILTIN);
    public static final JavaOperator FLOAT_NEG = new JavaBuiltinOperator(JavaBuiltin.FloatNegated.BUILTIN);
    public static final JavaOperator FLOAT_TIMES = new JavaBuiltinOperator(JavaBuiltin.FloatTimes.BUILTIN);
    public static final JavaOperator FLOAT_DIVIDE = new JavaBuiltinOperator(JavaBuiltin.FloatDivided.BUILTIN);
    public static final JavaOperator FLOAT_REMAINDER = new JavaBuiltinOrSnippetOperator(JavaBuiltin.FloatRemainder.BUILTIN, Snippet.FloatRemainder.SNIPPET);

    public static final JavaOperator DOUBLE_TO_INT = new JavaSnippetOperator(Snippet.ConvertDoubleToInt.SNIPPET);
    public static final JavaOperator DOUBLE_TO_LONG = new JavaSnippetOperator(Snippet.ConvertDoubleToLong.SNIPPET);
    public static final JavaOperator DOUBLE_TO_FLOAT = new JavaBuiltinOperator(JavaBuiltin.ConvertDoubleToFloat.BUILTIN);

    public static final JavaOperator DOUBLE_COMPARE_L = new JavaBuiltinOperator(JavaBuiltin.DoubleCompareL.BUILTIN);
    public static final JavaOperator DOUBLE_COMPARE_G = new JavaBuiltinOperator(JavaBuiltin.DoubleCompareG.BUILTIN);

    public static final JavaOperator DOUBLE_PLUS = new JavaBuiltinOperator(JavaBuiltin.DoublePlus.BUILTIN);
    public static final JavaOperator DOUBLE_MINUS = new JavaBuiltinOperator(JavaBuiltin.DoubleMinus.BUILTIN);
    public static final JavaOperator DOUBLE_NEG = new JavaBuiltinOperator(JavaBuiltin.DoubleNegated.BUILTIN);
    public static final JavaOperator DOUBLE_TIMES = new JavaBuiltinOperator(JavaBuiltin.DoubleTimes.BUILTIN);
    public static final JavaOperator DOUBLE_DIVIDE = new JavaBuiltinOperator(JavaBuiltin.DoubleDivided.BUILTIN);
    public static final JavaOperator DOUBLE_REMAINDER = new JavaBuiltinOrSnippetOperator(JavaBuiltin.DoubleRemainder.BUILTIN, Snippet.DoubleRemainder.SNIPPET);

    public static final JavaOperator INT_TO_BYTE = new JavaBuiltinOperator(JavaBuiltin.ConvertIntToByte.BUILTIN);
    public static final JavaOperator INT_TO_CHAR = new JavaBuiltinOperator(JavaBuiltin.ConvertIntToChar.BUILTIN);
    public static final JavaOperator INT_TO_SHORT = new JavaBuiltinOperator(JavaBuiltin.ConvertIntToShort.BUILTIN);
    public static final JavaOperator INT_TO_LONG = new JavaBuiltinOperator(JavaBuiltin.ConvertIntToLong.BUILTIN);
    public static final JavaOperator INT_TO_FLOAT = new JavaBuiltinOperator(JavaBuiltin.ConvertIntToFloat.BUILTIN);
    public static final JavaOperator INT_TO_DOUBLE = new JavaBuiltinOperator(JavaBuiltin.ConvertIntToDouble.BUILTIN);

    public static final JavaOperator INT_NEG = new JavaBuiltinOperator(JavaBuiltin.IntNegated.BUILTIN);
    public static final JavaOperator INT_PLUS = new JavaBuiltinOperator(JavaBuiltin.IntPlus.BUILTIN);
    public static final JavaOperator INT_TIMES = new JavaBuiltinOperator(JavaBuiltin.IntTimes.BUILTIN);
    public static final JavaOperator INT_MINUS = new JavaBuiltinOperator(JavaBuiltin.IntMinus.BUILTIN);
    public static final JavaOperator INT_DIVIDE = new JavaBuiltinOperator(JavaBuiltin.IntDivided.BUILTIN);
    public static final JavaOperator INT_REMAINDER = new JavaBuiltinOperator(JavaBuiltin.IntRemainder.BUILTIN);
    public static final JavaOperator INT_AND = new JavaBuiltinOperator(JavaBuiltin.IntAnd.BUILTIN);
    public static final JavaOperator INT_OR = new JavaBuiltinOperator(JavaBuiltin.IntOr.BUILTIN);
    public static final JavaOperator INT_XOR = new JavaBuiltinOperator(JavaBuiltin.IntXor.BUILTIN);
    public static final JavaOperator INT_SHIFT_LEFT = new JavaBuiltinOperator(JavaBuiltin.IntShiftedLeft.BUILTIN);
    public static final JavaOperator INT_SIGNED_SHIFT_RIGHT = new JavaBuiltinOperator(JavaBuiltin.IntSignedShiftedRight.BUILTIN);
    public static final JavaOperator INT_UNSIGNED_SHIFT_RIGHT = new JavaBuiltinOperator(JavaBuiltin.IntUnsignedShiftedRight.BUILTIN);

    public static final JavaOperator LONG_TO_INT = new JavaBuiltinOperator(JavaBuiltin.ConvertLongToInt.BUILTIN);
    public static final JavaOperator LONG_TO_FLOAT = new JavaBuiltinOperator(JavaBuiltin.ConvertLongToFloat.BUILTIN);
    public static final JavaOperator LONG_TO_DOUBLE = new JavaBuiltinOperator(JavaBuiltin.ConvertLongToDouble.BUILTIN);

    public static final JavaOperator LONG_COMPARE = new JavaBuiltinOrSnippetOperator(JavaBuiltin.LongCompare.BUILTIN, Snippet.LongCompare.SNIPPET);
    public static final JavaOperator LONG_PLUS = new JavaBuiltinOperator(JavaBuiltin.LongPlus.BUILTIN);
    public static final JavaOperator LONG_MINUS = new JavaBuiltinOperator(JavaBuiltin.LongMinus.BUILTIN);
    public static final JavaOperator LONG_NEG = new JavaBuiltinOperator(JavaBuiltin.LongNegated.BUILTIN);
    public static final JavaOperator LONG_TIMES = new JavaBuiltinOrSnippetOperator(JavaBuiltin.LongTimes.BUILTIN, Snippet.LongTimes.SNIPPET);
    public static final JavaOperator LONG_DIVIDE = new JavaBuiltinOrSnippetOperator(JavaBuiltin.LongDivided.BUILTIN, Snippet.LongDivided.SNIPPET);
    public static final JavaOperator LONG_REMAINDER = new JavaBuiltinOrSnippetOperator(JavaBuiltin.LongRemainder.BUILTIN, Snippet.LongRemainder.SNIPPET);
    public static final JavaOperator LONG_AND = new JavaBuiltinOperator(JavaBuiltin.LongAnd.BUILTIN);
    public static final JavaOperator LONG_OR = new JavaBuiltinOperator(JavaBuiltin.LongOr.BUILTIN);
    public static final JavaOperator LONG_XOR = new JavaBuiltinOperator(JavaBuiltin.LongXor.BUILTIN);
    public static final JavaOperator LONG_SHIFT_LEFT = new JavaBuiltinOperator(JavaBuiltin.LongShiftedLeft.BUILTIN);
    public static final JavaOperator LONG_SIGNED_SHIFT_RIGHT = new JavaBuiltinOrSnippetOperator(JavaBuiltin.LongSignedShiftedRight.BUILTIN, Snippet.LongSignedShiftedRight.SNIPPET);
    public static final JavaOperator LONG_UNSIGNED_SHIFT_RIGHT = new JavaBuiltinOperator(JavaBuiltin.LongUnsignedShiftedRight.BUILTIN);
}

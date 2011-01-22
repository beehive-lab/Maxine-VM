/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.cir.operator;

import com.sun.max.*;
import com.sun.max.lang.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.snippet.NativeStubSnippet.LinkNativeMethod;
import com.sun.max.vm.compiler.snippet.NativeStubSnippet.NativeCallEpilogue;
import com.sun.max.vm.compiler.snippet.NativeStubSnippet.NativeCallEpilogueForC;
import com.sun.max.vm.compiler.snippet.NativeStubSnippet.NativeCallPrologue;
import com.sun.max.vm.compiler.snippet.NativeStubSnippet.NativeCallPrologueForC;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.compiler.snippet.Snippet.MakeClassInitialized;
import com.sun.max.vm.cps.b.c.*;
import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.builtin.*;
import com.sun.max.vm.cps.cir.snippet.*;
import com.sun.max.vm.cps.cir.transform.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;

/**
 * High Level CIR Operator.  This class, its subclasses, and {@link JavaBuiltin}s comprise
 * the operators that can appear in the procedure position of a {@link CirCall}.
 *
 * @author Yi Guo
 * @author Aziz Ghuloum
 */
public abstract class JavaOperator extends CirOperator {

    protected int reasonsMayStop;

    /**
     * Creates an object for the application of an operator.
     *
     * @param reasonsMayStop specifies the reasons the translation of this operator may produce one or more
     *            {@linkplain Stoppable stops}
     */
    protected JavaOperator(int reasonsMayStop) {
        this.reasonsMayStop = reasonsMayStop;
    }

    public final int reasonsMayStop() {
        return reasonsMayStop;
    }

    /**
     * Removes one of the reasons this operator may stop.
     *
     * @param reasonMayStop exactly one of the flags defined in {@link Stoppable}
     */
    public final void removeReasonMayStop(int reasonMayStop) {
        assert reasonMayStop != 0 && Ints.isPowerOfTwoOrZero(reasonMayStop & Stoppable.Static.ALL_REASONS) : "Exactly one reason for stopping must be removed at a time";
        reasonsMayStop &= ~reasonMayStop;
    }

    public MethodActor foldingMethodActor() {
        throw FatalError.unimplemented();
    }

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
        protected final int index;

        /**
         * The constant pool.
         */
        protected final ConstantPool constantPool;

        protected final Kind resultKind;

        protected Actor_Type actor;

        public JavaResolvableOperator(int reasonsMayStop, ConstantPool constantPool, int index, Kind resultKind) {
            super(reasonsMayStop);
            assert (reasonsMayStop & CALL_STOP) != 0 : "operator translated by one more snippets must indicate CALL as a stop reason";
            this.constantPool = constantPool;
            this.index = index;
            this.resultKind = resultKind;
            if (constantPool != null) {
                final ResolvableConstant constant = constantPool.resolvableAt(index);
                if (constant.isResolved() || constant.isResolvableWithoutClassLoading(constantPool)) {
                    try {
                        resolve();
                    } catch (HostOnlyFieldError error) {
                        // Suppress: will have to be dealt with when 'resolve()' is called
                    } catch (HostOnlyMethodError error) {
                        // Suppress: will have to be dealt with when 'resolve()' is called
                    } catch (OmittedClassError error) {
                        // Suppress: will have to be dealt with when 'resolve()' is called
                    }
                }
            }
        }

        /**
         * Gets the constant pool containing the entry referenced by this operator.
         */
        public final ConstantPool constantPool() {
            return constantPool;
        }

        public boolean isFieldOrMethod() {
            if (actor != null) {
                return actor instanceof MemberActor;
            }
            return constant() instanceof MemberRefConstant;
        }

        /**
         * Gets the constant pool entry referenced by this operator.
         */
        public final ResolvableConstant constant() {
            return constantPool.resolvableAt(index);
        }

        /**
         * Gets the index of the constant pool entry referenced by this operator.
         */
        public final int index() {
            return index;
        }

        /**
         * Gets the resolved actor object corresponding to the constant pool entry referenced by this operator.
         * This method will return {@code null} if the entry has not been {@linkplain #isResolved() resolved}.
         */
        public final Actor_Type actor() {
            return actor;
        }

        /**
         * Determines if the constant pool entry referenced by this operator has been resolved.
         */
        public final boolean isResolved() {
            return actor != null;
        }

        /**
         * Resolves the constant pool entry referenced by this operator. The resolution only happens once.
         */
        public void resolve() {
            final Class<Actor_Type> type = null;
            actor = Utils.cast(type, constantPool.resolvableAt(index).resolve(constantPool, index));
        }

        public final Kind resultKind() {
            return resultKind;
        }

        /**
         * Gets the kind of the field referenced by this operator.
         *
         * @throws VerifyError if this operator does not operate on a field
         */
        public final Kind fieldKind() {
            return constantPool.fieldAt(index).type(constantPool).toKind();
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
            assert requiresClassInitialization() && actor != null;
            if (actor instanceof MemberActor) {
                return ((MemberActor) actor).holder();
            }
            return (ClassActor) actor;
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
            return getClass().getSimpleName() + ":" + constantPool.at(index).valueString(constantPool);
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
    public static final class JavaBuiltinOperator extends JavaOperator implements Lowerable {

        private final CirBuiltin cirBuiltin;

        public JavaBuiltinOperator(Builtin builtin) {
            super(builtin.reasonsMayStop());
            cirBuiltin = CirBuiltin.get(builtin);
        }

        public CirBuiltin builtin() {
            return cirBuiltin;
        }

        public Kind resultKind() {
            return cirBuiltin.resultKind();
        }

        @Override
        public Kind[] parameterKinds() {
            return cirBuiltin.parameterKinds();
        }

        @Override
        public void acceptVisitor(CirVisitor visitor) {
            visitor.visitHCirOperator(this);
        }

        @Override
        public void acceptVisitor(HCirOperatorVisitor visitor) {
            visitor.visit(this);
        }

        public void toLCir(Lowerable op, CirCall call, CPSCompiler compilerScheme) {
            call.setProcedure(cirBuiltin);
        }

        @Override
        public String toString() {
            return cirBuiltin.name();
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
        private final CirSnippet snippet;

        JavaSnippetOperator(Snippet snippet) {
            super(CALL_STOP);
            this.snippet = CirSnippet.get(snippet);
        }

        public CirSnippet snippet() {
            return snippet;
        }

        public Kind resultKind() {
            return snippet.resultKind();
        }

        @Override
        public Kind[] parameterKinds() {
            return snippet.parameterKinds();
        }

        @Override
        public void acceptVisitor(CirVisitor visitor) {
            visitor.visitHCirOperator(this);
        }

        @Override
        public void acceptVisitor(HCirOperatorVisitor visitor) {
            visitor.visit(this);
        }

        public void toLCir(Lowerable op, CirCall call, CPSCompiler compilerScheme) {
            call.setProcedure(snippet);
        }

        @Override
        public String toString() {
            return snippet.name();
        }
    }

    /**
     * Some operators may be lowered to either a Snippet call or a Builtin call depending on
     * whether or not the compiler backend knows how to handle it.
     *
     * @author Aziz Ghuloum
     */
    private static final class JavaBuiltinOrSnippetOperator extends JavaOperator implements Lowerable {
        private final Builtin builtin;
        private final Snippet snippet;
        private JavaBuiltinOrSnippetOperator(Builtin builtin, Snippet snippet) {
            super(CPSCompiler.Static.compiler().isBuiltinImplemented(builtin) ? builtin.reasonsMayStop() : CALL_STOP);
            this.builtin = builtin;
            this.snippet = snippet;
        }
        public void toLCir(Lowerable op, CirCall call, CPSCompiler compilerScheme) {
            if (compilerScheme.isBuiltinImplemented(builtin)) {
                call.setProcedure(CirBuiltin.get(builtin));
            } else {
                call.setProcedure(CirSnippet.get(snippet));
            }
        }

        public Kind resultKind() {
            return CirBuiltin.get(builtin).resultKind();
        }

        @Override
        public Kind[] parameterKinds() {
            return CirBuiltin.get(builtin).parameterKinds();
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
            return builtin.name;
        }
    }

    public static final JavaOperator INFOPOINT_OP = new JavaBuiltinOperator(InfopointBuiltin.BUILTIN);
    public static final JavaOperator LINK_OP = new JavaSnippetOperator(LinkNativeMethod.SNIPPET);
    public static final JavaOperator J2N_OP = new JavaSnippetOperator(NativeCallPrologue.SNIPPET);
    public static final JavaOperator N2J_OP = new JavaSnippetOperator(NativeCallEpilogue.SNIPPET);
    public static final JavaOperator J2NC_OP = new JavaSnippetOperator(NativeCallPrologueForC.SNIPPET);
    public static final JavaOperator N2JC_OP = new JavaSnippetOperator(NativeCallEpilogueForC.SNIPPET);

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

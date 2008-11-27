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

import com.sun.max.program.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.b.c.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.builtin.SafepointBuiltin.*;
import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.compiler.cir.builtin.*;
import com.sun.max.vm.compiler.cir.snippet.*;
import com.sun.max.vm.compiler.cir.transform.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.type.*;


/**
 * High Level CIR Operator.  This class, its subclasses, and {@link JavaBuiltin}s comprise
 * the operators that can appear in the procedure position of a {@link CirCall}.
 *
 * @author Yi Guo
 * @author Aziz Ghuloum
 */
public abstract class JavaOperator extends CirOperator implements CirRoutine {

    @Override
    public boolean mayThrowException() {
        return true;
    }

    @Override
    public Kind[] parameterKinds() {
        throw Problem.unimplemented();
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
    public boolean needsJavaFrameDescriptor() {
        return true;
    }

    @Override
    public void acceptVisitor(CirVisitor visitor) {
        visitor.visitHCirOperator(this);
    }

    public void acceptVisitor(HCirOperatorVisitor visitor) {
        visitor.visit(this);
    }

    /**
     * This class is a work around for some non {@link JavaBuiltin} classes that can appear
     * in the CIR.   We list them here explicitly instead of allowing all builtins to be
     * valid HCir operators.
     *
     * @author Yi Guo
     * @author Aziz Ghuloum
     */
    private static final class JavaBuiltinOperator extends JavaOperator implements Lowerable {
        @Override
        public boolean needsJavaFrameDescriptor() {
            return false;
        }

        private final CirBuiltin _cirBuiltin;

        private JavaBuiltinOperator(Builtin builtin) {
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
        public boolean mayThrowException() {
            return _cirBuiltin.mayThrowException();
        }
    }

    /**
     * This class is a work around for some {@link Snippet}s that can
     * appear in the CIR. We list them here explicitly instead of
     * allowing all {@link Snippet}s to be valid HCir operators.
     *
     * @author Yi Guo
     * @author Aziz Ghuloum
     */
    private static final class JavaSnippetOperator extends JavaOperator implements Lowerable {
        private final CirSnippet _snippet;

        private JavaSnippetOperator(Snippet snippet) {
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
        public boolean mayThrowException() {
            return _builtin.mayThrowException();
        }
    }

    public static final JavaOperator PROLOGUE = new JavaBuiltinOperator(SoftSafepoint.BUILTIN);

    public static final JavaOperator LONG_COMPARE = new JavaBuiltinOrSnippetOperator(JavaBuiltin.LongCompare.BUILTIN, Snippet.LongCompare.SNIPPET);
    public static final JavaOperator FLOAT_COMPARE_L = new JavaBuiltinOperator(JavaBuiltin.FloatCompareL.BUILTIN);
    public static final JavaOperator FLOAT_COMPARE_G = new JavaBuiltinOperator(JavaBuiltin.FloatCompareG.BUILTIN);
    public static final JavaOperator DOUBLE_COMPARE_L = new JavaBuiltinOperator(JavaBuiltin.DoubleCompareL.BUILTIN);
    public static final JavaOperator DOUBLE_COMPARE_G = new JavaBuiltinOperator(JavaBuiltin.DoubleCompareG.BUILTIN);

    public static final JavaOperator INT_PLUS = new JavaBuiltinOperator(JavaBuiltin.IntPlus.BUILTIN);
    public static final JavaOperator LONG_PLUS = new JavaBuiltinOperator(JavaBuiltin.LongPlus.BUILTIN);
    public static final JavaOperator FLOAT_PLUS = new JavaBuiltinOperator(JavaBuiltin.FloatPlus.BUILTIN);
    public static final JavaOperator DOUBLE_PLUS = new JavaBuiltinOperator(JavaBuiltin.DoublePlus.BUILTIN);

    public static final JavaOperator INT_MINUS = new JavaBuiltinOperator(JavaBuiltin.IntMinus.BUILTIN);
    public static final JavaOperator LONG_MINUS = new JavaBuiltinOperator(JavaBuiltin.LongMinus.BUILTIN);
    public static final JavaOperator FLOAT_MINUS = new JavaBuiltinOperator(JavaBuiltin.FloatMinus.BUILTIN);
    public static final JavaOperator DOUBLE_MINUS = new JavaBuiltinOperator(JavaBuiltin.DoubleMinus.BUILTIN);

    public static final JavaOperator INT_NEG = new JavaBuiltinOperator(JavaBuiltin.IntNegated.BUILTIN);
    public static final JavaOperator LONG_NEG = new JavaBuiltinOperator(JavaBuiltin.LongNegated.BUILTIN);
    public static final JavaOperator FLOAT_NEG = new JavaBuiltinOperator(JavaBuiltin.FloatNegated.BUILTIN);
    public static final JavaOperator DOUBLE_NEG = new JavaBuiltinOperator(JavaBuiltin.DoubleNegated.BUILTIN);

    public static final JavaOperator INT_TIMES = new JavaBuiltinOperator(JavaBuiltin.IntTimes.BUILTIN);
    public static final JavaOperator LONG_TIMES = new JavaBuiltinOrSnippetOperator(JavaBuiltin.LongTimes.BUILTIN, Snippet.LongTimes.SNIPPET);
    public static final JavaOperator FLOAT_TIMES = new JavaBuiltinOperator(JavaBuiltin.FloatTimes.BUILTIN);
    public static final JavaOperator DOUBLE_TIMES = new JavaBuiltinOperator(JavaBuiltin.DoubleTimes.BUILTIN);

    public static final JavaOperator INT_AND = new JavaBuiltinOperator(JavaBuiltin.IntAnd.BUILTIN);
    public static final JavaOperator LONG_AND = new JavaBuiltinOperator(JavaBuiltin.LongAnd.BUILTIN);
    public static final JavaOperator INT_OR = new JavaBuiltinOperator(JavaBuiltin.IntOr.BUILTIN);
    public static final JavaOperator LONG_OR = new JavaBuiltinOperator(JavaBuiltin.LongOr.BUILTIN);
    public static final JavaOperator INT_XOR = new JavaBuiltinOperator(JavaBuiltin.IntXor.BUILTIN);
    public static final JavaOperator LONG_XOR = new JavaBuiltinOperator(JavaBuiltin.LongXor.BUILTIN);

    public static final JavaOperator INT_DIVIDE = new JavaBuiltinOperator(JavaBuiltin.IntDivided.BUILTIN);
    public static final JavaOperator LONG_DIVIDE = new JavaBuiltinOrSnippetOperator(JavaBuiltin.LongDivided.BUILTIN, Snippet.LongDivided.SNIPPET);
    public static final JavaOperator FLOAT_DIVIDE = new JavaBuiltinOperator(JavaBuiltin.FloatDivided.BUILTIN);
    public static final JavaOperator DOUBLE_DIVIDE = new JavaBuiltinOperator(JavaBuiltin.DoubleDivided.BUILTIN);

    public static final JavaOperator INT_REMAINDER = new JavaBuiltinOperator(JavaBuiltin.IntRemainder.BUILTIN);
    public static final JavaOperator LONG_REMAINDER = new JavaBuiltinOrSnippetOperator(JavaBuiltin.LongRemainder.BUILTIN, Snippet.LongRemainder.SNIPPET);
    public static final JavaOperator FLOAT_REMAINDER = new JavaBuiltinOrSnippetOperator(JavaBuiltin.FloatRemainder.BUILTIN, Snippet.FloatRemainder.SNIPPET);
    public static final JavaOperator DOUBLE_REMAINDER = new JavaBuiltinOrSnippetOperator(JavaBuiltin.DoubleRemainder.BUILTIN, Snippet.DoubleRemainder.SNIPPET);

    public static final JavaOperator F2I = new JavaSnippetOperator(Snippet.ConvertFloatToInt.SNIPPET);
    public static final JavaOperator F2L = new JavaSnippetOperator(Snippet.ConvertFloatToLong.SNIPPET);
    public static final JavaOperator D2I = new JavaSnippetOperator(Snippet.ConvertDoubleToInt.SNIPPET);
    public static final JavaOperator D2L = new JavaSnippetOperator(Snippet.ConvertDoubleToLong.SNIPPET);
    public static final JavaOperator I2B = new JavaBuiltinOperator(JavaBuiltin.ConvertIntToByte.BUILTIN);
    public static final JavaOperator I2C = new JavaBuiltinOperator(JavaBuiltin.ConvertIntToChar.BUILTIN);
    public static final JavaOperator I2S = new JavaBuiltinOperator(JavaBuiltin.ConvertIntToShort.BUILTIN);
    public static final JavaOperator I2L = new JavaBuiltinOperator(JavaBuiltin.ConvertIntToLong.BUILTIN);
    public static final JavaOperator I2F = new JavaBuiltinOperator(JavaBuiltin.ConvertIntToFloat.BUILTIN);
    public static final JavaOperator I2D = new JavaBuiltinOperator(JavaBuiltin.ConvertIntToDouble.BUILTIN);
    public static final JavaOperator L2I = new JavaBuiltinOperator(JavaBuiltin.ConvertLongToInt.BUILTIN);
    public static final JavaOperator L2F = new JavaBuiltinOperator(JavaBuiltin.ConvertLongToFloat.BUILTIN);
    public static final JavaOperator L2D = new JavaBuiltinOperator(JavaBuiltin.ConvertLongToDouble.BUILTIN);
    public static final JavaOperator F2D = new JavaBuiltinOperator(JavaBuiltin.ConvertFloatToDouble.BUILTIN);
    public static final JavaOperator D2F = new JavaBuiltinOperator(JavaBuiltin.ConvertDoubleToFloat.BUILTIN);

    public static final JavaOperator INT_SHIFT_LEFT = new JavaBuiltinOperator(JavaBuiltin.IntShiftedLeft.BUILTIN);
    public static final JavaOperator LONG_SHIFT_LEFT = new JavaBuiltinOperator(JavaBuiltin.LongShiftedLeft.BUILTIN);
    public static final JavaOperator INT_SIGNED_SHIFT_RIGHT = new JavaBuiltinOperator(JavaBuiltin.IntSignedShiftedRight.BUILTIN);
    public static final JavaOperator LONG_SIGNED_SHIFT_RIGHT = new JavaBuiltinOrSnippetOperator(JavaBuiltin.LongSignedShiftedRight.BUILTIN, Snippet.LongSignedShiftedRight.SNIPPET);
    public static final JavaOperator INT_UNSIGNED_SHIFT_RIGHT = new JavaBuiltinOperator(JavaBuiltin.IntUnsignedShiftedRight.BUILTIN);
    public static final JavaOperator LONG_UNSIGNED_SHIFT_RIGHT = new JavaBuiltinOperator(JavaBuiltin.LongUnsignedShiftedRight.BUILTIN);
}

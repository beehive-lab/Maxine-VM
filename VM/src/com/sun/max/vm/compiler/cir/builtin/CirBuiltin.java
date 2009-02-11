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
package com.sun.max.vm.compiler.cir.builtin;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.compiler.cir.optimize.*;
import com.sun.max.vm.compiler.cir.transform.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Wrapper around {@link Builtin} for CIR use.
 *
 * @author Bernd Mathiske
 */
public class CirBuiltin extends CirOperator implements CirFoldable, CirReducible{

    private static CirValue[] withoutContinuations(CirValue[] arguments) {
        return Arrays.subArray(arguments, 0, arguments.length - 2);
    }

    protected final Builtin _builtin;
    private final Kind[] _parameterKinds;

    public boolean isReducible(CirOptimizer cirOptimizer, CirValue[] arguments) {
        return false;
    }

    public CirCall reduce(CirOptimizer cirOptimizer, CirValue... arguments) {
        throw ProgramError.unexpected();
    }

    /**
     * Under certain circumstances, a non-foldable builtin may be replaced by a variant that is foldable.
     * Thus we can for example propagate the foldability of a constant field down
     * to the memory access builtin that implements a field access.
     *
     * @author Bernd Mathiske
     */
    private static class FoldableVariant extends CirBuiltin {
        private CirBuiltin _original;

        protected FoldableVariant(CirBuiltin original) {
            super(original._builtin);
            _original = original;
        }

        @Override
        public String name() {
            return "fv-" + super.name();
        }

        @Override
        public boolean isFoldable(CirOptimizer cirOptimizer, CirValue[] arguments) {
            final CirValue[] a = withoutContinuations(arguments);
            if (_builtin.isHostFoldable(a)) {
                return true;
            }
            return _builtin.isFoldable(a);
        }

        @Override
        public CirCall fold(CirOptimizer cirOptimizer, CirValue... arguments) throws CirFoldingException {
            if (cirOptimizer.cirGenerator().isCrossCompiling() && _builtin.isHostFoldable(withoutContinuations(arguments))) {
                return CirRoutine.Static.fold(_builtin.hostFoldingMethodActor(), arguments);
            }
            return CirRoutine.Static.fold(foldingMethodActor(), arguments);
        }

        @Override
        public boolean isReducible(CirOptimizer cirOptimizer, CirValue[] arguments) {
            return _original.isReducible(cirOptimizer, arguments);
        }

        @Override
        public CirCall reduce(CirOptimizer cirOptimizer, CirValue... arguments) {
            return _original.reduce(cirOptimizer, arguments);
        }
    }

//    public boolean needsJavaFrameDescriptor() {
//        return runtimeChecks() != NONE;
//    }

    @Override
    public void acceptVisitor(CirVisitor visitor) {
        visitor.visitBuiltin(this);
    }

    @Override
    public void acceptBlockScopedVisitor(CirBlockScopedVisitor visitor, CirBlock scope) {
        visitor.visitBuiltin(this, scope);
    }

    @Override
    public CirNode acceptTransformation(CirTransformation transformation) {
        return transformation.transformBuiltin(this);
    }

    @Override
    public boolean acceptUpdate(CirUpdate update) {
        return update.updateBuiltin(this);
    }

    @Override
    public boolean acceptPredicate(CirPredicate predicate) {
        return predicate.evaluateBuiltin(this);
    }

    @CONSTANT
    protected FoldableVariant _foldableVariant;

    /**
     * Same idea as {@link FoldableVariant} above, but restricted to uses of {@link CONSTANT_WHEN_NOT_ZERO}.
     *
     * @author Bernd Mathiske
     */
    private static class FoldableWhenNotZeroVariant extends FoldableVariant {
        protected FoldableWhenNotZeroVariant(CirBuiltin original) {
            super(original);
        }

        @Override
        public String name() {
            return "nz" + super.name();
        }

        @Override
        public boolean isFoldable(CirOptimizer cirOptimizer, CirValue[] arguments) {
            final CirValue[] a = withoutContinuations(arguments);
            if (_builtin.isHostFoldable(a)) {
                final MethodActor methodActor = cirOptimizer.cirGenerator().isCrossCompiling() ? _builtin.hostFoldingMethodActor() : _builtin.foldingMethodActor();
                try {
                    final Value result = CirRoutine.Static.evaluate(methodActor, arguments);
                    return !result.isZero();
                } catch (CirFoldingException cirFoldingException) {
                    return false;
                }
            }
            return _builtin.isFoldable(a);
        }
    }

    @CONSTANT
    protected FoldableWhenNotZeroVariant _foldableWhenNotZeroVariant;

    protected CirBuiltin(Builtin builtin) {
        _builtin = builtin;
        _parameterKinds = builtin.parameterKinds();
    }

    private void initialize() {
        _foldableVariant = new FoldableVariant(this);
        _foldableWhenNotZeroVariant = new FoldableWhenNotZeroVariant(this);

        _foldableVariant._foldableVariant = _foldableVariant;
        _foldableVariant._foldableWhenNotZeroVariant = _foldableWhenNotZeroVariant;

        _foldableWhenNotZeroVariant._foldableVariant = _foldableVariant;
        _foldableWhenNotZeroVariant._foldableWhenNotZeroVariant = _foldableWhenNotZeroVariant;
    }

    public final Builtin builtin() {
        return _builtin;
    }

    public final CirBuiltin foldableVariant() {
        return _foldableVariant;
    }

    public final CirBuiltin foldableWhenNotZeroVariant() {
        return _foldableWhenNotZeroVariant;
    }

    private static CirBuiltin[] createCirBuiltins() {
        final int numberOfBuiltins = Builtin.builtins().length();
        final CirBuiltin[] cirBuiltins = new CirBuiltin[numberOfBuiltins];
        for (int i = 0; i < numberOfBuiltins; i++) {
            final Builtin builtin = Builtin.builtins().get(i);
            assert builtin.serial() == i;
            cirBuiltins[i] = new CirBuiltin(builtin);
            cirBuiltins[i].initialize();
        }
        return cirBuiltins;
    }

    private static CirBuiltin[] _cirBuiltins = createCirBuiltins();

    /**
     * Used by subclass constructors to update the above array
     * to consistently contain entries of the respective subclass.
     */
    protected void register() {
        _cirBuiltins[_builtin.serial()] = this;
        initialize();
    }

    /**
     * @param builtin the IR-indpenendent builtin
     * @return the CIR wrapper for the builtin, extending its functionality at this layer
     */
    public static CirBuiltin get(Builtin builtin) {
        return _cirBuiltins[builtin.serial()];
    }

    @Override
    public String name() {
        return _builtin.name();
    }

    public final Kind resultKind() {
        return _builtin.resultKind();
    }

    @Override
    public final Kind[] parameterKinds() {
        return _parameterKinds;
    }

    public final MethodActor foldingMethodActor() {
        return _builtin.foldingMethodActor();
    }

    public boolean isFoldable(CirOptimizer cirOptimizer, CirValue[] arguments) {
        return _builtin.isFoldable(withoutContinuations(arguments));
    }

    public CirCall fold(CirOptimizer cirOptimizer, CirValue... arguments) throws CirFoldingException {
        return CirRoutine.Static.fold(foldingMethodActor(), arguments);
    }

    @Override
    public int reasonsMayStop() {
        return _builtin.reasonsMayStop();
    }

    @Override
    public String toString() {
        return _builtin.toString();
    }
}

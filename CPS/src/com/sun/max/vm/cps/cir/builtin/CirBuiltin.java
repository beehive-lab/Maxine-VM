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
package com.sun.max.vm.cps.cir.builtin;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.builtin.AddressBuiltin.*;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.*;
import com.sun.max.vm.compiler.builtin.SpecialBuiltin.*;
import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.builtin.CirReducibleComparison.*;
import com.sun.max.vm.cps.cir.optimize.*;
import com.sun.max.vm.cps.cir.transform.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Wrapper around {@link Builtin} for CIR use.
 *
 * @author Bernd Mathiske
 */
public class CirBuiltin extends CirOperator implements CirFoldable, CirReducible{

    public final Builtin builtin;
    private final Kind[] parameterKinds;
    public final boolean nonFoldable;

    public boolean isReducible(CirOptimizer cirOptimizer, CirValue[] arguments) {
        return false;
    }

    public CirCall reduce(CirOptimizer cirOptimizer, CirValue... arguments) {
        throw ProgramError.unexpected();
    }

    @HOSTED_ONLY
    public boolean isHostFoldable(CirValue[] arguments) {
        return false;
    }

    /**
     * Under certain circumstances, a non-foldable builtin may be replaced by a variant that is foldable.
     * Thus we can for example propagate the foldability of a constant field down
     * to the memory access builtin that implements a field access.
     *
     * @author Bernd Mathiske
     */
    private static class FoldableVariant extends CirBuiltin {
        final CirBuiltin original;

        @HOSTED_ONLY
        protected FoldableVariant(CirBuiltin original) {
            super(original.builtin);
            this.original = original;
        }

        @Override
        public String name() {
            return "fv-" + super.name();
        }

        @Override
        public boolean isFoldable(CirOptimizer cirOptimizer, CirValue[] arguments) {
            if (MaxineVM.isHosted() && original.isHostFoldable(arguments)) {
                return true;
            }
            return original.isFoldable(cirOptimizer, arguments);
        }

        @Override
        public CirCall fold(CirOptimizer cirOptimizer, CirValue... arguments) throws CirFoldingException {
            if (MaxineVM.isHosted()) {
                if (original.isHostFoldable(arguments)) {
                    return CirFoldable.Static.fold(builtin.hostedExecutable, arguments);
                }
            }
            return CirFoldable.Static.fold(foldingMethodActor(), arguments);
        }

        @Override
        public boolean isReducible(CirOptimizer cirOptimizer, CirValue[] arguments) {
            return original.isReducible(cirOptimizer, arguments);
        }

        @Override
        public CirCall reduce(CirOptimizer cirOptimizer, CirValue... arguments) {
            return original.reduce(cirOptimizer, arguments);
        }
    }

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
    protected FoldableVariant foldableVariant;

    /**
     * Same idea as {@link FoldableVariant} above, but restricted to uses of {@link CONSTANT_WHEN_NOT_ZERO}.
     *
     * @author Bernd Mathiske
     */
    private static class FoldableWhenNotZeroVariant extends FoldableVariant {
        @HOSTED_ONLY
        protected FoldableWhenNotZeroVariant(CirBuiltin original) {
            super(original);
        }

        @Override
        public String name() {
            return "nz" + super.name();
        }

        @Override
        public boolean isFoldable(CirOptimizer cirOptimizer, CirValue[] arguments) {
            if (MaxineVM.isHosted() && original.isHostFoldable(arguments)) {
                final MethodActor methodActor =  MaxineVM.isHosted() ? builtin.hostedExecutable : builtin.executable;
                try {
                    final Value result = CirFoldable.Static.evaluate(methodActor, arguments);
                    return !result.isZero();
                } catch (CirFoldingException cirFoldingException) {
                    return false;
                }
            }
            return original.isFoldable(cirOptimizer, arguments);
        }
    }

    @CONSTANT
    protected FoldableWhenNotZeroVariant foldableWhenNotZeroVariant;

    @HOSTED_ONLY
    protected CirBuiltin(Builtin builtin) {
        this.builtin = builtin;
        this.parameterKinds = builtin.parameterKinds();
        nonFoldable = builtin instanceof SpecialBuiltin || builtin instanceof PointerBuiltin;
    }

    @HOSTED_ONLY
    private void initialize() {
        foldableVariant = new FoldableVariant(this);
        foldableWhenNotZeroVariant = new FoldableWhenNotZeroVariant(this);

        foldableVariant.foldableVariant = foldableVariant;
        foldableVariant.foldableWhenNotZeroVariant = foldableWhenNotZeroVariant;

        foldableWhenNotZeroVariant.foldableVariant = foldableVariant;
        foldableWhenNotZeroVariant.foldableWhenNotZeroVariant = foldableWhenNotZeroVariant;
    }

    public final CirBuiltin foldableVariant() {
        return foldableVariant;
    }

    public final CirBuiltin foldableWhenNotZeroVariant() {
        return foldableWhenNotZeroVariant;
    }

    @HOSTED_ONLY
    private static void register(CirBuiltin[] cirBuiltins, CirBuiltin cirBuiltin) {
        int serial = cirBuiltin.builtin.serial();
        assert cirBuiltins[serial] == null;
        cirBuiltins[serial] = cirBuiltin;
        cirBuiltin.initialize();
    }

    @HOSTED_ONLY
    private static CirBuiltin[] createCirBuiltins() {
        final int numberOfBuiltins = Builtin.builtins().size();
        final CirBuiltin[] builtins = new CirBuiltin[numberOfBuiltins];

        register(builtins, new CirReducibleAddressComparison(GreaterEqual.BUILTIN, ValueComparator.UNSIGNED_GREATER_EQUAL, Address.max(), Address.zero(), true));
        register(builtins, new CirReducibleAddressComparison(GreaterThan.BUILTIN, ValueComparator.UNSIGNED_GREATER_THAN, Address.zero(), Address.max(), false));
        register(builtins, new CirReducibleAddressComparison(LessEqual.BUILTIN, ValueComparator.UNSIGNED_LESS_EQUAL, Address.zero(), Address.max(), true));
        register(builtins, new CirReducibleAddressComparison(LessThan.BUILTIN, ValueComparator.UNSIGNED_LESS_THAN, Address.max(), Address.zero(), false));
        register(builtins, new CirReducibleUnsignedIntComparison(AboveEqual.BUILTIN, ValueComparator.UNSIGNED_GREATER_EQUAL, IntValue.from(0xffffffff), IntValue.ZERO, true));
        register(builtins, new CirReducibleUnsignedIntComparison(AboveThan.BUILTIN, ValueComparator.UNSIGNED_GREATER_THAN, IntValue.ZERO, IntValue.from(0xffffffff), false));
        register(builtins, new CirReducibleUnsignedIntComparison(BelowEqual.BUILTIN, ValueComparator.UNSIGNED_LESS_EQUAL, IntValue.ZERO, IntValue.from(0xffffffff), true));
        register(builtins, new CirReducibleUnsignedIntComparison(BelowThan.BUILTIN, ValueComparator.UNSIGNED_LESS_THAN, IntValue.from(0xffffffff), IntValue.ZERO, false));
        register(builtins, new CirShiftBuiltin(IntShiftedLeft.BUILTIN, Ints.WIDTH, false));
        register(builtins, new CirShiftBuiltin(LongShiftedLeft.BUILTIN, Longs.WIDTH, false));
        register(builtins, new CirShiftBuiltin(IntUnsignedShiftedRight.BUILTIN, Ints.WIDTH, false));
        register(builtins, new CirShiftBuiltin(LongUnsignedShiftedRight.BUILTIN, Longs.WIDTH, false));
        register(builtins, new CirShiftBuiltin(IntSignedShiftedRight.BUILTIN, Ints.WIDTH, true));
        register(builtins, new CirShiftBuiltin(LongSignedShiftedRight.BUILTIN, Longs.WIDTH, true));
        register(builtins, new CirStrengthReducible.IntPlus());
        register(builtins, new CirStrengthReducible.IntMinus());
        register(builtins, new CirStrengthReducible.IntTimes());
        register(builtins, new CirStrengthReducible.IntDivided());
        register(builtins, new CirStrengthReducible.IntRemainder());
        register(builtins, new CirStrengthReducible.IntAnd());
        register(builtins, new CirStrengthReducible.IntOr());
        register(builtins, new CirStrengthReducible.IntXor());
        register(builtins, new CirReducibleComparison.IntNot());
        register(builtins, new CirReducibleComparison.LongCompare());
        register(builtins, new CirStrengthReducible.LongPlus());
        register(builtins, new CirStrengthReducible.LongMinus());
        register(builtins, new CirStrengthReducible.LongTimes());
        register(builtins, new CirStrengthReducible.LongDivided());
        register(builtins, new CirStrengthReducible.LongRemainder());
        register(builtins, new CirStrengthReducible.LongAnd());
        register(builtins, new CirStrengthReducible.LongOr());
        register(builtins, new CirStrengthReducible.LongXor());
        register(builtins, new CirStrengthReducible.AddressDividedByAddress());
        register(builtins, new CirStrengthReducible.AddressDividedByInt());
        register(builtins, new CirStrengthReducible.AddressRemainderByAddress());
        register(builtins, new CirStrengthReducible.AddressRemainderByInt());

        for (int i = 0; i < numberOfBuiltins; i++) {
            final Builtin builtin = Builtin.builtins().get(i);
            assert builtin.serial() == i;
            if (builtins[i] == null) {
                if (builtin instanceof PointerLoadBuiltin) {
                    register(builtins, new CirPointerLoadBuiltin((PointerLoadBuiltin) builtin));
                } else {
                    register(builtins, new CirBuiltin(builtin));
                }
            }
        }

        for (int i = 0; i < builtins.length; i++) {
            assert builtins[i] != null;
        }

        return builtins;
    }

    private static CirBuiltin[] cirBuiltins = createCirBuiltins();

    /**
     * @param builtin the IR-independent builtin
     * @return the CIR wrapper for the builtin, extending its functionality at this layer
     */
    public static CirBuiltin get(Builtin builtin) {
        return cirBuiltins[builtin.serial()];
    }

    public String name() {
        return builtin.name;
    }

    public final Kind resultKind() {
        return builtin.resultKind;
    }

    @Override
    public final Kind[] parameterKinds() {
        return parameterKinds;
    }

    public final MethodActor foldingMethodActor() {
        return builtin.executable;
    }

    /**
     * A call to a CIR builtin is foldable if all of its non-continuation arguments
     * are {@linkplain CirValue#isConstant() constant} and none of them are of type
     * {@link Kind#REFERENCE}.
     */
    public boolean isFoldable(CirOptimizer cirOptimizer, CirValue[] arguments) {
        if (nonFoldable) {
            return false;
        }
        for (int i = 0; i < arguments.length - 2; i++) {
            CirValue argument = arguments[i];
            if (!argument.isConstant() || argument.kind().isReference) {
                return false;
            }
        }
        return true;
    }

    public CirCall fold(CirOptimizer cirOptimizer, CirValue... arguments) throws CirFoldingException {
        return CirFoldable.Static.fold(foldingMethodActor(), arguments);
    }

    public int reasonsMayStop() {
        return builtin.reasonsMayStop();
    }

    @Override
    public String toString() {
        return builtin.toString();
    }
}

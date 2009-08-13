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

import com.sun.max.unsafe.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.compiler.cir.optimize.*;
import com.sun.max.vm.compiler.cir.transform.*;
import com.sun.max.vm.compiler.cir.variable.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Merging a comparison and a subsequent conditional branch into a mere conditional branch
 * with a condition of the comparison's kind.
 *
 * @author Bernd Mathiske
 */
public abstract class CirReducibleComparison extends CirReducibleCombination {

    private final Kind kind;

    protected CirReducibleComparison(Builtin builtin, Kind kind) {
        super(builtin);
        this.kind = kind;
    }

    /**
     * Test whether this comparison is directly followed by a related CirSwitch(Kind.INT).
     */
    @Override
    public boolean isReducible(CirOptimizer cirOptimizer, CirValue[] arguments) {
        final CirValue normalContinuation = arguments[arguments.length - 2];
        if (normalContinuation instanceof CirClosure) {
            final CirClosure closure = (CirClosure) normalContinuation;
            final CirCall body = closure.body();
            final CirValue procedure = body.procedure();
            if (procedure instanceof CirSwitch) {
                final CirSwitch cirSwitch = (CirSwitch) procedure;
                if (cirSwitch.comparisonKind() == Kind.INT) {
                    final CirVariable[] parameters = closure.parameters();
                    // Are we passing the continuation argument on as switch tag?
                    if (parameters.length == 1 && parameters[0] == body.arguments()[0]) {
                        return cirSwitch.numberOfMatches() == 1;
                    }
                }
            }
        }
        return false;
    }

    protected ValueComparator mapValueComparator(ValueComparator valueComparator, int match) {
        return valueComparator;
    }

    /**
     * Rewrite this longCompare and its subsequent CirSwitch(Kind.INT) as a CirSwitch(_kind).
     */
    @Override
    public CirCall reduce(CirOptimizer cirOptimizer, CirValue... arguments) {
        final CirValue normalContinuation = arguments[arguments.length - 2];
        final CirClosure closure = (CirClosure) normalContinuation;
        final CirCall body = closure.body();

        final CirValue[] oldSwitchArguments = body.arguments();
        assert oldSwitchArguments.length == 4;

        // The old switch value might still be lurking in Java frame descriptors - neutralize it:
        CirJavaFrameDescriptorClipping.applySingle(closure, oldSwitchArguments[0]);

        final int oldSwitchMatch = oldSwitchArguments[1].value().toInt();
        final CirValue oldSwitchMatchContinuation = oldSwitchArguments[2];
        final CirValue oldSwitchDefaultContinuation = oldSwitchArguments[3];

        final CirSwitch oldSwitch = (CirSwitch) body.procedure();
        final CirSwitch newSwitch = new CirSwitch(kind, mapValueComparator(oldSwitch.valueComparator(), oldSwitchMatch), 1);


        return new CirCall(newSwitch, arguments[0], arguments[1], oldSwitchMatchContinuation, oldSwitchDefaultContinuation);
    }

    public static final class LongCompare extends CirReducibleComparison {
        public LongCompare() {
            super(JavaBuiltin.LongCompare.BUILTIN, Kind.LONG);
        }
    }

    private abstract static class CirReducibleAddressComparison extends CirReducibleComparison {
        private final ValueComparator valueComparator;
        private final Address pathologicalValue0;
        private final Address pathologicalValue1;
        private final boolean pathologicalResult;

        protected CirReducibleAddressComparison(Builtin builtin, ValueComparator valueComparator,
                  Address pathologicalValue0, Address pathologicalValue1, boolean pathologicalResult) {
            super(builtin, Kind.WORD);
            this.valueComparator = valueComparator;
            this.pathologicalValue0 = pathologicalValue0;
            this.pathologicalValue1 = pathologicalValue1;
            this.pathologicalResult = pathologicalResult;
        }

        private boolean isPathological(CirValue argument, Address pathologicalValue) {
            return argument.isConstant() && argument.value().toWord().asAddress().equals(pathologicalValue);
        }

        private boolean arePathological(CirValue[] arguments) {
            return isPathological(arguments[0], pathologicalValue0) || isPathological(arguments[1], pathologicalValue1);
        }

        @Override
        public boolean isReducible(CirOptimizer cirOptimizer, CirValue[] arguments) {
            if (arePathological(arguments)) {
                return true;
            }
            if (!super.isReducible(cirOptimizer, arguments)) {
                return false;
            }
            final CirClosure closure = (CirClosure) arguments[arguments.length - 2];
            final CirCall body = closure.body();
            final CirSwitch oldSwitch = (CirSwitch) body.procedure();
            if (oldSwitch.valueComparator() != ValueComparator.EQUAL && oldSwitch.valueComparator() != ValueComparator.NOT_EQUAL) {
                return false;
            }
            final int match = body.arguments()[1].value().toInt();
            if (match != 1 && match != 0) {
                return false;
            }
            final CirValue value = body.arguments()[0];
            final CirValue normalContinuation = body.arguments()[2];
            final CirValue exceptionContinuation = body.arguments()[3];
            // Check if there is no other use of the integer comparison value beyond the reducible combination.
            // (This should only happen in bytecode not emitted by javac, but let's play it safe here.)
            return !CirSearch.OutsideBlocksAndJavaFrameDescriptors.contains(normalContinuation, value) && !CirSearch.OutsideBlocksAndJavaFrameDescriptors.contains(exceptionContinuation, value);
        }

        @Override
        public CirCall reduce(CirOptimizer cirOptimizer, CirValue... arguments) {
            if (arePathological(arguments)) {
                final CirValue normalContinuation = arguments[arguments.length - 2];
                return new CirCall(normalContinuation, new CirConstant(BooleanValue.from(pathologicalResult)));
            }
            return super.reduce(cirOptimizer, arguments);
        }

        @Override
        protected ValueComparator mapValueComparator(ValueComparator comparator, int match) {
            if ((match == 1) == (comparator == ValueComparator.EQUAL)) {
                return this.valueComparator;
            }
            return this.valueComparator.complement();
        }
    }

    public static final class AddressLessEqual extends CirReducibleAddressComparison {

        public AddressLessEqual() {
            super(AddressBuiltin.LessEqual.BUILTIN, ValueComparator.UNSIGNED_LESS_EQUAL, Address.zero(), Address.max(), true);
        }
    }

    public static final class AddressLessThan extends CirReducibleAddressComparison {

        public AddressLessThan() {
            super(AddressBuiltin.LessThan.BUILTIN, ValueComparator.UNSIGNED_LESS_THAN, Address.max(), Address.zero(), false);
        }
    }

    public static final class AddressGreaterThan extends CirReducibleAddressComparison {

        public AddressGreaterThan() {
            super(AddressBuiltin.GreaterThan.BUILTIN, ValueComparator.UNSIGNED_GREATER_THAN, Address.zero(), Address.max(), false);
        }
    }

    public static final class AddressGreaterEqual extends CirReducibleAddressComparison {

        public AddressGreaterEqual() {
            super(AddressBuiltin.GreaterEqual.BUILTIN, ValueComparator.UNSIGNED_GREATER_EQUAL, Address.max(), Address.zero(), true);
        }
    }

    public static class CirReducibleUnsignedIntComparison extends CirReducibleComparison {

        private final ValueComparator valueComparator;
        private final IntValue pathologicalValue0;
        private final IntValue pathologicalValue1;
        private final boolean pathologicalResult;

        protected CirReducibleUnsignedIntComparison(Builtin builtin, ValueComparator valueComparator, IntValue pathologicalValue0, IntValue pathologicalValue1, boolean pathologicalResult) {
            super(builtin, Kind.INT);
            this.valueComparator = valueComparator;
            this.pathologicalValue0 = pathologicalValue0;
            this.pathologicalValue1 = pathologicalValue1;
            this.pathologicalResult = pathologicalResult;
        }

        private boolean isPathological(CirValue argument, IntValue pathologicalValue) {
            return argument.isConstant() && argument.value().equals(pathologicalValue);
        }

        private boolean arePathological(CirValue[] arguments) {
            return isPathological(arguments[0], pathologicalValue0) || isPathological(arguments[1], pathologicalValue1);
        }

        @Override
        public boolean isReducible(CirOptimizer cirOptimizer, CirValue[] arguments) {
            if (arePathological(arguments)) {
                return true;
            }
            if (!super.isReducible(cirOptimizer, arguments)) {
                return false;
            }
            final CirClosure closure = (CirClosure) arguments[arguments.length - 2];
            final CirCall body = closure.body();
            final CirSwitch oldSwitch = (CirSwitch) body.procedure();
            if (oldSwitch.valueComparator() != ValueComparator.EQUAL && oldSwitch.valueComparator() != ValueComparator.NOT_EQUAL) {
                return false;
            }
            final int match = body.arguments()[1].value().toInt();
            if (match != 1 && match != 0) {
                return false;
            }
            final CirValue value = body.arguments()[0];
            final CirValue normalContinuation = body.arguments()[2];
            final CirValue exceptionContinuation = body.arguments()[3];
            // Check if there is no other use of the integer comparison value beyond the reducible combination.
            // (This should only happen in bytecode not emitted by javac, but let's play it safe here.)
            return !CirSearch.OutsideBlocksAndJavaFrameDescriptors.contains(normalContinuation, value) && !CirSearch.OutsideBlocksAndJavaFrameDescriptors.contains(exceptionContinuation, value);
        }
        @Override
        public CirCall reduce(CirOptimizer cirOptimizer, CirValue... arguments) {
            if (arePathological(arguments)) {
                final CirValue normalContinuation = arguments[arguments.length - 2];
                return new CirCall(normalContinuation, new CirConstant(BooleanValue.from(pathologicalResult)));
            }
            return super.reduce(cirOptimizer, arguments);
        }
        @Override
        protected ValueComparator mapValueComparator(ValueComparator comparator, int match) {
            if ((match == 1) == (comparator == ValueComparator.EQUAL)) {
                return this.valueComparator;
            }
            return this.valueComparator.complement();
        }


    }
    public static final class UnsignedIntGreaterEqual extends CirReducibleUnsignedIntComparison {
        public UnsignedIntGreaterEqual() {
            super(SpecialBuiltin.UnsignedIntGreaterEqual.BUILTIN, ValueComparator.UNSIGNED_GREATER_EQUAL, IntValue.from(0xffffffff), IntValue.ZERO, true);
        }
    }

}

/*
 * Copyright (c) 2011, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.graal.nodes.calc;

import com.oracle.max.graal.graph.*;
import com.oracle.max.graal.nodes.*;
import com.oracle.max.graal.nodes.spi.*;
import com.sun.cri.ci.*;

/* TODO(tw/gd) For high-level optimization purpose the compare node should be a boolean *value* (it is currently only a helper node)
 * But in the back-end the comparison should not always be materialized (for example in x86 the comparison result will not be in a register but in a flag)
 *
 * Compare should probably be made a value (so that it can be canonicalized for example) and in later stages some Compare usage should be transformed
 * into variants that do not materialize the value (CompareIf, CompareGuard...)
 */
public final class CompareNode extends BooleanNode implements Canonicalizable {

    @Input private ValueNode x;
    @Input private ValueNode y;

    @Data private Condition condition;
    @Data private boolean unorderedIsTrue;

    public ValueNode x() {
        return x;
    }

    public ValueNode y() {
        return y;
    }

    /**
     * Constructs a new Compare instruction.
     *
     * @param x the instruction producing the first input to the instruction
     * @param condition the condition (comparison operation)
     * @param y the instruction that produces the second input to this instruction
     * @param graph
     */
    public CompareNode(ValueNode x, Condition condition, ValueNode y) {
        super(CiKind.Illegal);
        assert (x == null && y == null) || x.kind == y.kind;
        this.condition = condition;
        this.x = x;
        this.y = y;
    }

    /**
     * Gets the condition (comparison operation) for this instruction.
     *
     * @return the condition
     */
    public Condition condition() {
        return condition;
    }

    /**
     * Checks whether unordered inputs mean true or false.
     *
     * @return {@code true} if unordered inputs produce true
     */
    public boolean unorderedIsTrue() {
        return unorderedIsTrue;
    }

    public void setUnorderedIsTrue(boolean unorderedIsTrue) {
        this.unorderedIsTrue = unorderedIsTrue;
    }

    /**
     * Swaps the operands to this if and mirrors the condition (e.g. > becomes <).
     *
     * @see Condition#mirror()
     */
    public void swapOperands() {
        condition = condition.mirror();
        throw new UnsupportedOperationException("swapOperands...");
    }

    public void negate() {
        condition = condition.negate();
        unorderedIsTrue = !unorderedIsTrue;
    }

    @Override
    public void accept(ValueVisitor v) {
    }

    @Override
    public String shortName() {
        return "Comp " + condition.operator;
    }

    private Node optimizeMaterialize(CiConstant constant, MaterializeNode materializeNode) {
        CiConstant trueConstant = materializeNode.trueValue().asConstant();
        CiConstant falseConstant = materializeNode.falseValue().asConstant();

        if (falseConstant != null && trueConstant != null) {
            Boolean trueResult = condition().foldCondition(trueConstant, constant, graph().runtime(), unorderedIsTrue());
            Boolean falseResult = condition().foldCondition(falseConstant, constant, graph().runtime(), unorderedIsTrue());

            if (trueResult != null && falseResult != null) {
                boolean trueUnboxedResult = trueResult;
                boolean falseUnboxedResult = falseResult;
                if (trueUnboxedResult == falseUnboxedResult) {
                    return ConstantNode.forBoolean(trueUnboxedResult, graph());
                } else {
                    if (trueUnboxedResult) {
                        assert falseUnboxedResult == false;
                        return materializeNode.condition();
                    } else {
                        assert falseUnboxedResult == true;
                        return graph().unique(new NegateBooleanNode(materializeNode.condition()));

                    }
                }
            }
        }
        return this;
    }

    private Node optimizeNormalizeCmp(CiConstant constant, NormalizeCompareNode normalizeNode) {
        if (normalizeNode.x().kind.isFloatOrDouble()) {
            // NaN values lead to unexpected comparison results...
            return this;
        }
        if (constant.kind == CiKind.Int && constant.asInt() == 0) {
            Condition condition = condition();
            if (normalizeNode == y()) {
                condition = condition.mirror();
            }
            CompareNode result = graph().unique(new CompareNode(normalizeNode.x(), condition, normalizeNode.y()));
            boolean isLess = condition == Condition.LE || condition == Condition.LT || condition == Condition.BE || condition == Condition.BT;
            result.unorderedIsTrue = condition != Condition.EQ && (condition == Condition.NE || !(isLess ^ normalizeNode.isUnorderedLess()));
            return result;
        }
        return this;
    }

    @Override
    public Node canonical(CanonicalizerTool tool) {
        if (x().isConstant() && !y().isConstant()) { // move constants to the left (y)
            return graph().unique(new CompareNode(y(), condition.mirror(), x()));
        } else if (x().isConstant() && y().isConstant()) {
            CiConstant constX = x().asConstant();
            CiConstant constY = y().asConstant();
            Boolean result = condition().foldCondition(constX, constY, graph().runtime(), unorderedIsTrue());
            if (result != null) {
                return ConstantNode.forBoolean(result, graph());
            }
        }

        if (y().isConstant()) {
            if (x() instanceof MaterializeNode) {
                return optimizeMaterialize(y().asConstant(), (MaterializeNode) x());
            } else if (x() instanceof NormalizeCompareNode) {
                return optimizeNormalizeCmp(y().asConstant(), (NormalizeCompareNode) x());
            }
        }

        if (x() == y() && x().kind != CiKind.Float && x().kind != CiKind.Double) {
            return ConstantNode.forBoolean(condition().check(1, 1), graph());
        }
        if ((condition == Condition.NE || condition == Condition.EQ) && x().kind == CiKind.Object) {
            ValueNode object = null;
            if (x().isNullConstant()) {
                object = y();
            } else if (y().isNullConstant()) {
                object = x();
            }
            if (object != null) {
                IsNonNullNode nonNull = graph().unique(new IsNonNullNode(object));
                if (condition == Condition.NE) {
                    return nonNull;
                } else {
                    assert condition == Condition.EQ;
                    return graph().unique(new NegateBooleanNode(nonNull));
                }
            }
        }
        return this;
    }
}

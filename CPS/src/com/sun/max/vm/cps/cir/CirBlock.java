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
package com.sun.max.vm.cps.cir;

import java.util.*;

import com.sun.max.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.cps.cir.optimize.*;
import com.sun.max.vm.cps.cir.transform.*;
import com.sun.max.vm.cps.cir.variable.*;
import com.sun.max.vm.cps.ir.*;
import com.sun.max.vm.type.*;

/**
 * A CIR block is a {@link CirProcedure procedure} that is derived from
 * a basic block in a control flow graph. The block will either be the first block in an
 * {@link Role#EXCEPTION_DISPATCHER exception dispatcher} or a
 * {@link Role#NORMAL normal} block in a control flow graph.  A block
 * contains no free variables, and thus can be called from multiple places
 * without concern for variable scope.
 * <p>
 * A CIR block wraps a {@link CirClosure CIR closure}.  Early in CIR
 * generation, any free variables within the closure's body are captured
 * and included as parameters of the closure.
 * <p>
 * In the {@link CirPrinter trace}, a CIR block is shown as
 * block#n [parameters...] . { body }
 * where n is the bytecode offset of the start of the block.  The parameters shown
 * are actually the parameters of the block's CirClosure.
 *
 * @author Bernd Mathiske
 */
public final class CirBlock extends CirProcedure implements IrBlock, CirInlineable {

    @Override
    public boolean isConstant() {
        return true;
    }

    private final IrBlock.Role role;
    private CirClosure closure;

    private LinkedList<CirCall> calls;

    public CirBlock(IrBlock.Role role) {
        this.role = role;
        this.closure = new CirClosure(new CirCall(), CirClosure.NO_PARAMETERS);
    }

    public CirBlock(CirCall call) {
        this.role = IrBlock.Role.NORMAL;
        this.closure = new CirClosure(call, CirClosure.NO_PARAMETERS);
    }

    public CirBlock(CirClosure closure) {
        this.role = IrBlock.Role.NORMAL;
        this.closure = closure;
    }

    @Override
    public Object clone() {
        final CirBlock result = (CirBlock) super.clone();

        // If '_calls' were not cleared, we would be referencing calls in the old graph:
        reset();
        // It is ok to clear '_calls', since they always gets updated (by 'CirBlockUpdating.apply')
        // before being accessed (by block inlining).

        return result;
    }

    public IrBlock.Role role() {
        return role;
    }

    public int serial() {
        return id();
    }

    public CirClosure closure() {
        return closure;
    }

    public void setClosure(CirClosure closure) {
        this.closure = closure;
    }

    @Override
    public Kind[] parameterKinds() {
        return closure.parameterKinds();
    }

    @RESET
    private transient boolean isRecursionDetermined;

    @RESET
    private transient boolean isRecursive;

    public boolean isRecursive() {
        if (isRecursionDetermined) {
            return isRecursive;
        }
        final CirNode node = CirSearch.byPredicate(closure(), new CirPredicate() {
            @Override
            public boolean evaluateBlock(CirBlock block) {
                return block == CirBlock.this;
            }
        });
        assert node == null || node == this;
        isRecursive = node == this;
        isRecursionDetermined = true;
        return isRecursive;
    }

    public void reset() {
        calls = null;
        isRecursionDetermined = false;
    }

    public void addCall(CirCall call) {
        if (calls == null) {
            calls = new LinkedList<CirCall>();
        }
        assert call.arguments().length == closure().parameters().length;
        assert !(Utils.indexOfIdentical(calls, call) != -1);
        calls.add(call);
    }

    /**
     * Returns the list of calls to this block.
     * ATTENTION: make sure it is updated before using it!!!
     *
     * @return list of calls to this block or 'null' if none
     */
    public LinkedList<CirCall> calls() {
        return calls;
    }

    public int numberOfCalls() {
        return calls == null ? 0 : calls.size();
    }

    private int findIndex(CirVariable[] parameters, CirValue argument) {
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i] == argument) {
                return i;
            }
        }
        return -1;
    }

    private boolean isFirstCallFoldableSwitch(CirOptimizer optimizer, CirValue[] arguments) {
        final CirValue procedure = closure.body().procedure();
        if (procedure instanceof CirSwitch) {
            final CirFoldable foldable = (CirFoldable) procedure;
            final CirValue[] bodyArguments = closure.body().arguments();
            final int n = bodyArguments.length;
            final CirValue[] foldArguments = CirCall.newArguments(n);
            for (int i = 0; i < n; i++) {
                if (bodyArguments[i].isConstant()) {
                    foldArguments[i] = bodyArguments[i];
                } else {
                    final int index = findIndex(closure.parameters(), bodyArguments[i]);
                    foldArguments[i] = arguments[index];
                }
            }
            return foldable.isFoldable(optimizer, foldArguments);
        }
        return false;
    }

    public boolean isInlineable(CirOptimizer optimizer, CirValue[] arguments) {
        assert arguments.length == closure.parameters().length;
        return numberOfCalls() <= 1 || closure.body().procedure() instanceof CirContinuationVariable ||
               (isFirstCallFoldableSwitch(optimizer, arguments) && !isRecursive());
    }

    public CirCall inline(CirOptimizer optimizer, CirValue[] arguments, CirJavaFrameDescriptor javaFrameDescriptor) {
        if (numberOfCalls() > 1) {
            CirSwitchEncapsulation.apply(this);
        }
        final CirClosure closureCopy = CirReplication.replicateLocalClosure(closure);
        return CirBetaReduction.applyMultiple(closureCopy, arguments);
    }

    @Override
    public String toString() {
        return role.toString();
    }

    @Override
    public void acceptVisitor(CirVisitor visitor) {
        visitor.visitBlock(this);
    }

    @Override
    public void acceptBlockScopedVisitor(CirBlockScopedVisitor visitor, CirBlock scope) {
        visitor.visitBlock(this, scope);
    }

    @Override
    public CirNode acceptTransformation(CirTransformation transformation) {
        return transformation.transformBlock(this);
    }

    @Override
    public boolean acceptUpdate(CirUpdate update) {
        return update.updateBlock(this);
    }

    @Override
    public boolean acceptPredicate(CirPredicate predicate) {
        return predicate.evaluateBlock(this);
    }
}

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

import com.sun.max.vm.cps.cir.transform.*;
import com.sun.max.vm.cps.cir.variable.*;
import com.sun.max.vm.type.*;

/**
 * A CIR closure is a procedure whose body is defined by a {@link CirCall CIR call}.
 * <p>
 * A closure has an array of parameters, which can be referred to within the body of the call.
 * <p>
 * In the {@link CirPrinter trace} output a CirClosure shows up as:
 *
 * <pre>
 * proc[parameters...] { body }
 * </pre>
 *
 * @author Bernd Mathiske
 */
public class CirClosure extends CirProcedure {

    public static CirVariable[] newParameters(int count) {
        return count == 0 ? NO_PARAMETERS : new CirVariable[count];
    }

    @Override
    public boolean isConstant() {
        return true;
    }

    private CirVariable[] parameters;
    private CirCall body;

    /**
     * The value that must be used when passing a zero-length array as the value of {@code parameters} to
     * {@link #CirClosure(CirCall, CirVariable...)} and {@link #setParameters(CirVariable...)}.
     */
    public static final CirVariable[] NO_PARAMETERS = {};

    public CirClosure() {
    }

    /**
     * Creates a closure for a given body and set of parameters.
     *
     * @param body the body of the closure
     * @param parameters the parameters of this closure. If {@code parameters.length == 0} then the value of {@code
     *            parameters} must be {@link #NO_PARAMETERS}.
     */
    public CirClosure(CirCall body, CirVariable... parameters) {
        setParameters(parameters);
        this.body = body;
        assert verifyParameters();
    }

    @Override
    public String toString() {
        return "<CirClosure>";
    }

    public boolean verifyParameters() {
        int nCC = 0;
        int nCE = 0;
        for (CirVariable parameter : parameters) {
            if (parameter instanceof CirContinuationVariable) {
                if (parameter instanceof CirNormalContinuationParameter) {
                    nCC++;
                } else {
                    nCE++;
                }
            }
        }
        assert nCC <= 1;
        assert nCE <= 1 : nCE + " " + this.traceToString(false);
        return true;
    }

    /**
     * Sets the parameters of this closure.
     *
     * @param parameters the parameters of this closure. If {@code parameters.length == 0} then the value of {@code
     *            parameters} must be {@link #NO_PARAMETERS}.
     */
    public void setParameters(CirVariable... parameters) {
        assert parameters.length > 0 || parameters == CirClosure.NO_PARAMETERS;
        this.parameters = parameters;
    }

    public CirVariable[] parameters() {
        return parameters;
    }

    public void removeParameter(int index) {
        int newLength = parameters.length - 1;
        CirVariable[] newParameters = new CirVariable[newLength];
        System.arraycopy(parameters, 0, newParameters, 0, index);
        System.arraycopy(parameters, index + 1, newParameters, index, newLength - index);
        parameters = newParameters;
    }

    public boolean hasTheseParameters(CirValue[] values) {
        if (values.length != parameters.length) {
            return false;
        }
        for (int i = 0; i < values.length; i++) {
            if (values[i] != parameters[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Kind[] parameterKinds() {
        final Kind[] kinds = new Kind[parameters.length];
        for (int i = 0; i != kinds.length; ++i) {
            kinds[i] = parameters[i].kind();
        }
        return kinds;
    }

    public void setBody(CirCall body) {
        this.body = body;
    }

    public CirCall body() {
        return body;
    }

    /**
     * Ensure that the body is a block call,
     * creating a new block to wrap the body if necessary.
     */
    public void makeBlockCall() {
        assert parameters().length == 0;
        if (body().procedure() instanceof CirBlock) {
            return;
        }
        final CirBlock block = new CirBlock(body());
        CirFreeVariableSearch.applyClosureConversion(block.closure());
        final CirVariable[] parameters = block.closure().parameters();
        final CirValue[] arguments;
        if (parameters.length > 0) {
            arguments = new CirValue[parameters.length];
            System.arraycopy(parameters, 0, arguments, 0, arguments.length);
        } else {
            arguments = CirCall.NO_ARGUMENTS;
        }
        setBody(new CirCall(block, arguments));
    }

    @Override
    public boolean equals(Object other, CirVariableRenaming renaming) {
        if (other instanceof CirClosure) {
            final CirClosure closure = (CirClosure) other;
            if (closure.parameters().length != parameters().length) {
                return false;
            }
            CirVariableRenaming r = renaming;
            for (int i = 0; i < parameters().length; i++) {
                r = new CirVariableRenaming(r, parameters()[0], closure.parameters()[i]);
            }
            return body().equals(closure.body(), r);
        }
        return false;
    }

    @Override
    public boolean equals(Object other) {
        return equals(other, null);
    }

    @Override
    public int hashCode() {
        return parameters().length; // TODO
    }

    @Override
    public void acceptVisitor(CirVisitor visitor) {
        visitor.visitClosure(this);
    }

    @Override
    public void acceptBlockScopedVisitor(CirBlockScopedVisitor visitor, CirBlock scope) {
        visitor.visitClosure(this, scope);
    }

    @Override
    public CirNode acceptTransformation(CirTransformation transformation) {
        return transformation.transformClosure(this);
    }

    @Override
    public boolean acceptUpdate(CirUpdate update) {
        return update.updateClosure(this);
    }

    @Override
    public boolean acceptPredicate(CirPredicate predicate) {
        return predicate.evaluateClosure(this);
    }
}

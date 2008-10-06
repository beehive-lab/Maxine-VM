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
/*VCSID=566559bd-7c88-410c-aee8-7581765dae57*/
package com.sun.max.vm.compiler.cir;

import com.sun.max.lang.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.compiler.cir.transform.*;
import com.sun.max.vm.compiler.cir.variable.*;
import com.sun.max.vm.type.*;

/**
 * A CIR closure is a procedure whose body is defined by a {@link CirCall CIR call}.
 * <p>
 * A closure has an array of parameters, which can be referred to
 * within the body of the call.
 * <p>
 * In the {@link CirPrinter trace} output a CirClosure shows up as
 * proc[parameters...] { body }
 *
 * @author Bernd Mathiske
 */
public class CirClosure extends CirProcedure {

    @Override
    public boolean isConstant() {
        return true;
    }

    private CirVariable[] _parameters;
    private CirCall _body;
    private final BytecodeLocation _location;

    public CirClosure(BytecodeLocation location) {
        _location = location;
    }

    public CirClosure(CirCall body, CirVariable... parameters) {
        setParameters(parameters);
        _body = body;
        _location = null;
        assert verifyParameters();
    }

    @Override
    public String toString() {
        return "<CirClosure>";
    }

    public boolean verifyParameters() {
        int nCC = 0;
        int nCE = 0;
        for (CirVariable parameter : _parameters) {
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

    public void setParameters(CirVariable... parameters) {
        _parameters = parameters;
    }

    public CirVariable[] parameters() {
        return _parameters;
    }

    public void removeParameter(int index) {
        _parameters = Arrays.remove(CirVariable.class, _parameters, index);
    }

    public boolean hasTheseParameters(CirValue[] values) {
        if (values.length != _parameters.length) {
            return false;
        }
        for (int i = 0; i < values.length; i++) {
            if (values[i] != _parameters[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Kind[] parameterKinds() {
        return CirValue.getKinds(_parameters);
    }

    public void setBody(CirCall body) {
        _body = body;
    }

    public CirCall body() {
        return _body;
    }

    public BytecodeLocation location() {
        return _location;
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
        final CirValue[] arguments = Arrays.from(CirValue.class, block.closure().parameters());
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

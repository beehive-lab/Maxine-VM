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
package com.sun.max.vm.compiler.cir;

import com.sun.max.vm.compiler.cir.transform.*;
import com.sun.max.vm.value.*;

/**
 * A manifest Java value.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public final class CirConstant extends CirValue {

    private final Value value;

    public CirConstant(Value value) {
        super(value.kind());
        this.value = value;
    }

    public static final CirConstant NULL = new CirConstant(ReferenceValue.NULL);

    /**
     * @return the constant converted to a kind that the Java expression stack uses
     */
    public Value toStackValue() {
        return value().kind().toStackKind().convert(value());
    }

    @Override
    public boolean isConstant() {
        return true;
    }

    @Override
    public Value value() {
        return value;
    }

    public static CirConstant fromInt(int n) {
        return new CirConstant(IntValue.from(n));
    }

    public static CirConstant fromFloat(float f) {
        return new CirConstant(FloatValue.from(f));
    }

    public static CirConstant fromDouble(double d) {
        return new CirConstant(DoubleValue.from(d));
    }

    public static CirConstant fromObject(Object object) {
        if (object == null) {
            return NULL;
        }
        return new CirConstant(ReferenceValue.from(object));
    }

    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof CirConstant) {
            final CirConstant constant = (CirConstant) other;
            return value.equals(constant.value);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    public static final CirConstant TRUE = new CirConstant(BooleanValue.TRUE);

    @Override
    public void acceptVisitor(CirVisitor visitor) {
        visitor.visitConstant(this);
    }

    @Override
    public void acceptBlockScopedVisitor(CirBlockScopedVisitor visitor, CirBlock scope) {
        visitor.visitConstant(this, scope);
    }

    @Override
    public CirNode acceptTransformation(CirTransformation transformation) {
        return transformation.transformConstant(this);
    }

    @Override
    public boolean acceptUpdate(CirUpdate update) {
        return update.updateConstant(this);
    }

    @Override
    public boolean acceptPredicate(CirPredicate predicate) {
        return predicate.evaluateConstant(this);
    }
}

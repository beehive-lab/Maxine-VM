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
/*VCSID=c80aa382-d68c-452a-917c-ac581f631313*/
package com.sun.max.vm.compiler.cir.variable;

import com.sun.max.program.*;
import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.compiler.cir.transform.*;
import com.sun.max.vm.type.*;

/**
 * @author Bernd Mathiske
 */
public abstract class CirVariable extends CirValue {

    private int _serial;

    public int serial() {
        return _serial;
    }

    protected CirVariable(int serial, Kind kind) {
        super(kind.toStackKind());
        _serial = serial;
    }

    CirVariable createFresh(int newSerial) {
        try {
            // HotSpot randomly cannot invoke clone() reflectively.
            // This bug in HotSpot prevents us from using Objects.clone(), so we try this:
            final CirVariable result = (CirVariable) clone();
            result._serial = newSerial;
            return result;
        } catch (Throwable throwable) {
            throw ProgramError.unexpected("clone() failed for: " + this);
        }
    }

    @Override
    public boolean equals(Object other, CirVariableRenaming renaming) {
        if (other == this) {
            return true;
        }
        if (renaming == null) {
            return false;
        }
        return other == renaming.find(this);
    }

    @Override
    public void acceptVisitor(CirVisitor visitor) {
        visitor.visitVariable(this);
    }

    @Override
    public void acceptBlockScopedVisitor(CirBlockScopedVisitor visitor, CirBlock scope) {
        visitor.visitVariable(this, scope);
    }

    @Override
    public CirNode acceptTransformation(CirTransformation transformation) {
        return transformation.transformVariable(this);
    }

    @Override
    public boolean acceptUpdate(CirUpdate update) {
        return update.updateVariable(this);
    }

    @Override
    public boolean acceptPredicate(CirPredicate predicate) {
        return predicate.evaluateVariable(this);
    }
}

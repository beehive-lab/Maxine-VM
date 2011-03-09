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
package com.sun.max.vm.cps.cir.variable;

import com.sun.max.program.*;
import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.transform.*;
import com.sun.max.vm.type.*;

/**
 * @author Bernd Mathiske
 */
public abstract class CirVariable extends CirValue {

    private int serial;

    public int serial() {
        return serial;
    }

    protected CirVariable(int serial, Kind kind) {
        super(kind.stackKind);
        this.serial = serial;
    }

    CirVariable createFresh(int newSerial) {
        try {
            // HotSpot randomly cannot invoke clone() reflectively.
            // This bug in HotSpot prevents us from using Objects.clone(), so we try this:
            final CirVariable result = (CirVariable) clone();
            result.serial = newSerial;
            return result;
        } catch (Throwable throwable) {
            throw ProgramError.unexpected("clone() failed for: " + this, throwable);
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

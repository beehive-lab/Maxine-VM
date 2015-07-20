/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.layout.hosted;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.type.*;

/**
 */
public class HostedHybridLayout extends HostedArrayLayout implements HybridLayout {

    @Override
    public Layout.Category category() {
        return Layout.Category.HYBRID;
    }

    @Override
    public boolean isHybridLayout() {
        return true;
    }

    @Override
    public boolean isArrayLayout() {
        return false;
    }

    public int firstAvailableWordArrayIndex(Size initialCellSize) {
        return 0;
    }

    private final HostedTupleLayout tupleLayout;

    public HostedHybridLayout() {
        super(Kind.WORD);
        tupleLayout = new HostedTupleLayout();
    }

    public Size layoutFields(ClassActor superClassActor, FieldActor[] fieldActors) {
        return tupleLayout.layoutFields(superClassActor, fieldActors);
    }

    @INLINE
    public int getFieldOffsetInCell(FieldActor fieldActor) {
        return tupleLayout.getFieldOffsetInCell(fieldActor);
    }
}

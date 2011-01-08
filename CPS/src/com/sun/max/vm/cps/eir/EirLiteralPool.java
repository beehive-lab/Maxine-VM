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
package com.sun.max.vm.cps.eir;

import java.util.*;

import com.sun.max.vm.value.*;

public class EirLiteralPool {

    public EirLiteralPool() {
    }

    private Map<Value, EirLiteral> referenceLiterals = new LinkedHashMap<Value, EirLiteral>();

    public Collection<EirLiteral> referenceLiterals() {
        return referenceLiterals.values();
    }

    private Map<Value, EirLiteral> scalarLiterals = new LinkedHashMap<Value, EirLiteral>();

    public Collection<EirLiteral> scalarLiterals() {
        return scalarLiterals.values();
    }

    public boolean hasLiterals() {
        return !(referenceLiterals.isEmpty() && scalarLiterals.isEmpty());
    }

    private int nextReferenceLiteralIndex;
    private int nextScalarLiteralByteIndex;

    public EirLiteral makeLiteral(Value value) {
        if (value.kind().isReference) {
            EirLiteral literal = referenceLiterals.get(value);
            if (literal == null) {
                literal = new EirLiteral(nextReferenceLiteralIndex, value);
                nextReferenceLiteralIndex++;
                referenceLiterals.put(value, literal);
            }
            return literal;
        }
        EirLiteral literal = scalarLiterals.get(value);
        if (literal == null) {
            literal = new EirLiteral(nextScalarLiteralByteIndex, value);
            nextScalarLiteralByteIndex += value.kind().width.numberOfBytes;
            scalarLiterals.put(value, literal);
        }
        return literal;
    }
}

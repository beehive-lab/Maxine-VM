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
package com.sun.max.vm.cps.eir;

import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.vm.value.*;

public class EirLiteralPool {

    public EirLiteralPool() {
    }

    private Map<Value, EirLiteral> referenceLiterals = new LinkedHashMap<Value, EirLiteral>();

    public Sequence<EirLiteral> referenceLiterals() {
        return new ArraySequence<EirLiteral>(referenceLiterals.values());
    }

    private Map<Value, EirLiteral> scalarLiterals = new LinkedHashMap<Value, EirLiteral>();

    public Sequence<EirLiteral> scalarLiterals() {
        return new ArraySequence<EirLiteral>(scalarLiterals.values());
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

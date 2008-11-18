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
package com.sun.max.vm.compiler.eir;

import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

public class EirLiteralPool {

    public EirLiteralPool() {
    }

    private Map<Value, EirLiteral> _referenceLiterals = new LinkedHashMap<Value, EirLiteral>();

    public Sequence<EirLiteral> referenceLiterals() {
        return new ArraySequence<EirLiteral>(_referenceLiterals.values());
    }

    private Map<Value, EirLiteral> _scalarLiterals = new LinkedHashMap<Value, EirLiteral>();

    public Sequence<EirLiteral> scalarLiterals() {
        return new ArraySequence<EirLiteral>(_scalarLiterals.values());
    }

    public boolean hasLiterals() {
        return !(_referenceLiterals.isEmpty() && _scalarLiterals.isEmpty());
    }

    private int _nextReferenceLiteralIndex;
    private int _nextScalarLiteralByteIndex;

    public EirLiteral makeLiteral(Value value) {
        if (value.kind() == Kind.REFERENCE) {
            EirLiteral literal = _referenceLiterals.get(value);
            if (literal == null) {
                literal = new EirLiteral(_nextReferenceLiteralIndex, value);
                _nextReferenceLiteralIndex++;
                _referenceLiterals.put(value, literal);
            }
            return literal;
        }
        EirLiteral literal = _scalarLiterals.get(value);
        if (literal == null) {
            literal = new EirLiteral(_nextScalarLiteralByteIndex, value);
            _nextScalarLiteralByteIndex += value.kind().size();
            _scalarLiterals.put(value, literal);
        }
        return literal;
    }
}

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
package com.sun.max.util;

import java.util.*;

import com.sun.max.program.*;

/**
 * @author Bernd Mathiske
 */
final class ListSymbolizer<S extends Symbol> implements Symbolizer<S> {

    private final Class<S> symbolType;
    private final List<S> symbols;

    ListSymbolizer(Class<S> symbolType, List<S> symbols) {
        if (symbolType.getName().startsWith("com.sun.max.asm") && Symbolizer.Static.hasPackageExternalAccessibleConstructors(symbolType)) {
            // This test ensures that values passed for symbolic parameters of methods in the
            // generated assemblers are guaranteed to be legal (assuming client code does not
            // inject its own classes into the package where the symbol classes are defined).
            ProgramError.unexpected("type of assembler symbol can have values constructed outside of defining package: " + symbolType);
        }
        this.symbolType = symbolType;
        this.symbols = symbols;
        ProgramError.check(!symbols.isEmpty());
    }

    public Class<S> type() {
        return symbolType;
    }

    public int numberOfValues() {
        return symbols.size();
    }

    public S fromValue(int value) {
        for (S symbol : symbols) {
            if (symbol.value() == value) {
                return symbol;
            }
        }
        return null;
    }

    public Iterator<S> iterator() {
        return symbols.iterator();
    }
}

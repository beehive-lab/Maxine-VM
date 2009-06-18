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

import com.sun.max.collect.*;
import com.sun.max.program.*;

/**
 * @author Bernd Mathiske
 */
final class SequenceSymbolizer<Symbol_Type extends Symbol> implements Symbolizer<Symbol_Type> {

    private final Class<Symbol_Type> symbolType;
    private final Sequence<Symbol_Type> symbols;

    SequenceSymbolizer(Class<Symbol_Type> symbolType, Sequence<Symbol_Type> symbols) {
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

    public Class<Symbol_Type> type() {
        return symbolType;
    }

    public int numberOfValues() {
        return symbols.length();
    }

    public Symbol_Type fromValue(int value) {
        for (Symbol_Type symbol : symbols) {
            if (symbol.value() == value) {
                return symbol;
            }
        }
        return null;
    }

    public Iterator<Symbol_Type> iterator() {
        return symbols.iterator();
    }
}

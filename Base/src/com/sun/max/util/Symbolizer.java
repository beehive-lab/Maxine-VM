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

import java.lang.reflect.*;
import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.lang.*;

/**
 * A symbolizer is used to group a set of {@link Symbol symbols}. The members of the
 * group can be iterated and the symbol corresponding to a given value can be
 * retrieved from the group.
 *
 * This class is similar to the semantics of {@code enum}s in Java but adds
 * the ability to have a set of predefined symbols whose primitive values are not
 * necessarily contiguous and starting at 0.
 *
 * @author Bernd Mathiske
 */
public interface Symbolizer<Symbol_Type extends Symbol> extends Iterable<Symbol_Type> {

    /**
     * @return the concrete type of the symbols in the group
     */
    Class<Symbol_Type> type();

    /**
     * Gets the symbol in the group whose primitive value equals {@code value}.
     *
     * @param value the search key
     * @return the found symbol or {@code null} if no symbol is found for {@code value}
     */
    Symbol_Type fromValue(int value);

    int numberOfValues();

    public static final class Static {

        private Static() {

        }

        public static boolean hasPackageExternalAccessibleConstructors(Class type) {
            final int publicOrProtected = Modifier.PUBLIC | Modifier.PROTECTED;
            for (Constructor constructor : type.getConstructors()) {
                if ((constructor.getModifiers() & publicOrProtected) != 0) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Gets a map from name to symbol for all the symbols represented by a given symbolizer.
         *
         * @param <Symbol_Type> the type of the symbol
         * @param symbolizer a set of symbols
         * @return a map from symbol name to symbol
         */
        public static <Symbol_Type extends Symbol> Map<String, Symbol_Type> toSymbolMap(Symbolizer<Symbol_Type> symbolizer) {
            final Map<String, Symbol_Type> map = new HashMap<String, Symbol_Type>(symbolizer.numberOfValues());
            for (Symbol_Type symbol : symbolizer) {
                map.put(symbol.name(), symbol);
            }
            return map;
        }

        public static <Symbol_Type extends Symbol> Symbolizer<Symbol_Type> from(Class<Symbol_Type> symbolType, Symbol_Type... symbols) {
            return new SequenceSymbolizer<Symbol_Type>(symbolType, new ArraySequence<Symbol_Type>(symbols));
        }

        public static <Symbol_Type extends Symbol> Symbolizer<Symbol_Type> fromSequence(Class<Symbol_Type> symbolType, Iterable< ? extends Symbol_Type> symbols,
                        final Symbol_Type... additionalSymbols) {
            final AppendableSequence<Symbol_Type> sequence = new ArrayListSequence<Symbol_Type>(additionalSymbols);
            for (Symbol_Type symbol : symbols) {
                sequence.append(symbol);
            }
            return new SequenceSymbolizer<Symbol_Type>(symbolType, sequence);
        }

        public static <Symbol_Type extends Symbol> Symbolizer<Symbol_Type> append(Symbolizer<Symbol_Type> symbolizer, Symbol_Type... symbols) {
            return fromSequence(symbolizer.type(), symbolizer, symbols);
        }

        public static <Symbol_Type extends Symbol> Symbolizer<Symbol_Type> append(Class<Symbol_Type> symbolType, Symbolizer< ? extends Symbol_Type> symbolizer,
                        final Symbol_Type... symbols) {
            return fromSequence(symbolType, symbolizer, symbols);
        }

        public static <Symbol_Type extends Symbol> Symbolizer<Symbol_Type> initialize(Class staticNameFieldClass, Class<Symbol_Type> symbolType) {
            final AppendableSequence<Symbol_Type> sequence = new ArrayListSequence<Symbol_Type>();
            final Sequence<StaticFieldName> staticFieldNames = StaticFieldName.Static.initialize(staticNameFieldClass);
            for (StaticFieldName staticFieldName : staticFieldNames) {
                if (symbolType.isInstance(staticFieldName)) {
                    sequence.append(symbolType.cast(staticFieldName));
                }
            }
            return new SequenceSymbolizer<Symbol_Type>(symbolType, sequence);
        }

        public static <Symbol_Type extends Symbol> Symbolizer<Symbol_Type> initialize(Class<Symbol_Type> symbolType) {
            return initialize(symbolType, symbolType);
        }

        public static <Symbol_Type extends Symbol> Symbolizer<Symbol_Type> fromSymbolizer(Symbolizer<Symbol_Type> symbolizer, Predicate<Symbol_Type> predicate) {
            if (predicate == null) {
                return symbolizer;
            }
            return fromSequence(symbolizer.type(), Sequence.Static.filter(symbolizer, predicate));
        }

    }
}

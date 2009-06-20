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
package com.sun.max.vm.classfile.constant;

import com.sun.max.collect.*;
import com.sun.max.collect.ChainedHashMapping.*;
import com.sun.max.vm.*;

/**
 * Implementation of symbol and String interning, the latter of which is a Java language requirement.
 *
 * @author Doug Simon
 */
public final class SymbolTable {

    private SymbolTable() {
    }

    /**
     * The only concrete subclass of {@link Utf8Constant}.
     * Using a subclass hides the details of storing Utf8Constants in a {@link ChainedHashMapping}.
     */
    static final class Utf8ConstantEntry extends Utf8Constant implements ChainedHashMapping.Entry<String, Utf8ConstantEntry> {

        Utf8ConstantEntry(String value) {
            super(value);
        }

        public String key() {
            return toString();
        }

        private Entry<String, Utf8ConstantEntry> next;

        public Entry<String, Utf8ConstantEntry> next() {
            return next;
        }

        public void setNext(Entry<String, Utf8ConstantEntry> next) {
            this.next = next;
        }

        public void setValue(Utf8ConstantEntry value) {
            assert value == this;
        }

        public Utf8ConstantEntry value() {
            return this;
        }
    }

    /**
     * Searching and adding entries to this map is only performed by {@linkplain #makeSymbol(String) one method} which
     * is synchronized.
     */
    private static final GrowableMapping<String, Utf8ConstantEntry> symbolTable = new ChainingValueChainedHashMapping<String, Utf8ConstantEntry>(40000);

    public static final Utf8Constant INIT = makeSymbol("<init>");
    public static final Utf8Constant CLINIT = makeSymbol("<clinit>");
    public static final Utf8Constant FINALIZE = makeSymbol("finalize");

    public static int length() {
        return symbolTable.length();
    }

    public static synchronized Utf8Constant lookupSymbol(String value) {
        return symbolTable.get(value);
    }

    public static synchronized Utf8Constant makeSymbol(String value) {
        Utf8ConstantEntry utf8 = symbolTable.get(value);
        if (utf8 == null) {
            if (MaxineVM.isPrototyping()) {
                // String interning is implemented with another data structure when running hosted
                utf8 = new Utf8ConstantEntry(value.intern());
            } else {
                utf8 = new Utf8ConstantEntry(value);
            }
            symbolTable.put(value, utf8);
        }
        return utf8;
    }

    public static String intern(String value) {
        return makeSymbol(value).toString();
    }
}

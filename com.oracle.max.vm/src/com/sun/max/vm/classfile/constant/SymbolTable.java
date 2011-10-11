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
package com.sun.max.vm.classfile.constant;

import java.io.*;

import com.sun.max.collect.*;
import com.sun.max.collect.ChainedHashMapping.*;
import com.sun.max.vm.*;

/**
 * Implementation of symbol and String interning, the latter of which is a Java language requirement.
 *
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

        @Override
        public void writeOn(DataOutputStream stream, ConstantPoolEditor editor, int index) throws IOException {
            super.writeOn(stream, editor, index);
            stream.writeUTF(editor.pool().utf8At(index, null).toString());
        }
    }

    /**
     * Searching and adding entries to this map is only performed by {@linkplain #makeSymbol(String) one method} which
     * is synchronized.
     */
    private static final ChainingValueChainedHashMapping<String, Utf8ConstantEntry> symbolTable = new ChainingValueChainedHashMapping<String, Utf8ConstantEntry>(40000);

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
            if (MaxineVM.isHosted()) {
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

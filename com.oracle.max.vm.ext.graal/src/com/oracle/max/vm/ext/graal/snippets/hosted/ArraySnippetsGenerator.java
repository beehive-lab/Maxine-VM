/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.graal.snippets.hosted;

import java.io.*;
import java.util.*;

import com.oracle.graal.api.meta.*;
import com.oracle.max.vm.ext.graal.snippets.*;

/**
 * Generates the boilerplate for {@link ArraySnippets}.
 */
public class ArraySnippetsGenerator extends SnippetsGenerator {

    private static final String LOAD_SNIPPET =
        "    @Snippet(inlining = MaxSnippetInliningPolicy.class)\n" +
        "    public static #KIND# #K#aloadSnippet(Object array, int index) {\n" +
        "        checkIndex(array, index);\n" +
        "        return #UCB#ArrayAccess.get#UKIND#(array, index)#UCA#;\n" +
        "    }\n\n";

    private static final String STORE_SNIPPET =
        "    @Snippet(inlining = MaxSnippetInliningPolicy.class)\n" +
        "    public static void #K#astoreSnippet(Object array, int index, #KIND# value) {\n" +
        "        checkIndex(array, index#CHECK_SET_OBJECT#);\n" +
        "        ArrayAccess.set#UKIND#(array, index, value);\n" +
        "    }\n\n";

    private static final String ADD_SNIPPETS_DECL =
        "    @HOSTED_ONLY\n" +
        "    private void addSnippets(LoadIndexedLowering loadIndexedLowering, StoreIndexedLowering storeIndexedLowering) {\n";

    private static final String ADD_LOAD_SNIPPET =
        "        loadIndexedLowering.setSnippet(Kind.#UKIND#, snippet(ArraySnippets.class, \"#K#aloadSnippet\"));\n";

    private static final String ADD_STORE_SNIPPET =
        "        storeIndexedLowering.setSnippet(Kind.#UKIND#, snippet(ArraySnippets.class, \"#K#astoreSnippet\"));\n";

    @Override
    protected void doGenerate() throws IOException {
        ArrayList<String> addSnippets = new ArrayList<>();
        for (Kind kind : Kind.values()) {
            if (notVoidOrIllegal(kind)) {
                out.print(replace(replaceUCast(replaceKinds(LOAD_SNIPPET, kind), kind), "#K#", String.valueOf(kind.getTypeChar())));
                String s = replace(replaceKinds(STORE_SNIPPET, kind), "#K#", String.valueOf(kind.getTypeChar()));
                s = replace(s, "#CHECK_SET_OBJECT#", kind == Kind.Object ? ", value" : "");
                out.print(s);
                addSnippets.add(replace(replaceKinds(ADD_LOAD_SNIPPET, kind), "#K#", String.valueOf(kind.getTypeChar())));
                addSnippets.add(replace(replaceKinds(ADD_STORE_SNIPPET, kind), "#K#", String.valueOf(kind.getTypeChar())));
            }
        }
        out.print(ADD_SNIPPETS_DECL);
        for (String addSnippet : addSnippets) {
            out.print(addSnippet);
        }
        out.print("    }\n");

    }

    public static void main(String[] args) throws IOException {
        if (new ArraySnippetsGenerator().generate(false, ArraySnippets.class)) {
            System.exit(1);
        }
    }

}

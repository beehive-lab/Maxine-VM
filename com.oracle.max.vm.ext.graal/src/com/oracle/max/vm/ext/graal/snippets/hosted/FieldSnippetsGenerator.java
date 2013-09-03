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
import com.sun.max.annotate.*;

/**
 * Generates the boilerplate for {@link FieldSnippets}.
 *
 * We try to be consistent on naming. Problematic because the bytecode names are GET/PUT/STATIC and GET/PUT/FIELD yet
 * we also refer to them to as "static fields" and "instance fields", which is more natural. There is also existing
 * code in Maxine that uses names "InstanceField" and "StaticField", so we follow this approach.
 */
@HOSTED_ONLY
public class FieldSnippetsGenerator extends SnippetsGenerator {

    private static final String PUT_FIELD_METHOD =
        "    @INLINE\n" +
        "    private static void putField#UKIND#(Object object, boolean nullCheck, int offset, boolean isVolatile, #KIND# value) {\n" +
        "        if (isVolatile) {\n" +
        "            memoryBarrier(JMM_PRE_VOLATILE_WRITE);\n" +
        "        }\n" +
        "        if (nullCheck) {\n" +
        "            MaxNullCheckNode.nullCheck(object);\n" +
        "        }\n" +
        "        TupleAccess.write#UKIND#(object, offset, value);\n" +
        "        if (isVolatile) {\n" +
        "            memoryBarrier(JMM_POST_VOLATILE_READ);\n" +
        "        }\n" +
        "    }\n\n";

    private static final String GET_FIELD_METHOD =
        "    @INLINE\n" +
        "    private static #KIND# getField#UKIND#(Object object, boolean nullCheck, int offset, boolean isVolatile) {\n" +
        "        if (isVolatile) {\n" +
        "            memoryBarrier(JMM_PRE_VOLATILE_READ);\n" +
        "        }\n" +
        "        if (nullCheck) {\n" +
        "            MaxNullCheckNode.nullCheck(object);\n" +
        "        }\n" +
        "        #KIND# result = TupleAccess.read#UKIND#(object, offset);\n" +
        "        if (isVolatile) {\n" +
        "            memoryBarrier(JMM_POST_VOLATILE_READ);\n" +
        "        }\n" +
        "        return result;\n" +
        "    }\n\n";

    private static final String PUT_FIELD_SNIPPET =
        "    @Snippet(inlining = MaxSnippetInliningPolicy.class)\n" +
        "    private static void putField#UKIND#Snippet(Object object, int offset, @ConstantParameter boolean isStatic, @ConstantParameter boolean isVolatile, #KIND# value) {\n" +
        "        putField#UKIND#(object, !isStatic, offset, isVolatile, value);\n" +
        "    }\n\n";

    private static final String GET_FIELD_SNIPPET =
        "    @Snippet(inlining = MaxSnippetInliningPolicy.class)\n" +
        "    private static #KIND# getField#UKIND#Snippet(Object object, int offset, @ConstantParameter boolean isStatic, @ConstantParameter boolean isVolatile) {\n" +
        "        return #UCB#getField#UKIND#(object, !isStatic, offset, isVolatile)#UCA#;\n" +
        "    }\n\n";

    private static final String PUT_UNRESOLVED_FIELD_SNIPPET =
                    "    @Snippet(inlining = MaxSnippetInliningPolicy.class)\n" +
                    "    private static void putUnresolvedField#UKIND#Snippet(Object object, @ConstantParameter boolean isStatic, FieldActor fieldActor, #KIND# value) {\n" +
                    "        putField#UKIND#(isStatic ? fieldActor.holder().staticTuple() : object, !isStatic, fieldActor.offset(), fieldActor.isVolatile(), value);\n" +
                    "    }\n\n";

    private static final String GET_UNRESOLVED_FIELD_SNIPPET =
                    "    @Snippet(inlining = MaxSnippetInliningPolicy.class)\n" +
                    "    private static #KIND# getUnresolvedField#UKIND#Snippet(Object object, @ConstantParameter boolean isStatic, FieldActor fieldActor) {\n" +
                    "        return #UCB#getField#UKIND#(isStatic ? fieldActor.holder().staticTuple() : object, !isStatic, fieldActor.offset(), fieldActor.isVolatile())#UCA#;\n" +
                    "    }\n\n";

    private static final String ADD_SNIPPETS_DECL =
        "    @HOSTED_ONLY\n" +
        "    private void addSnippets(\n" +
        "            LoadFieldLowering loadFieldLowering, StoreFieldLowering storeFieldLowering,\n" +
        "            UnresolvedLoadFieldLowering loadUnresolvedFieldLowering, UnresolvedStoreFieldLowering storeUnresolvedFieldLowering) {\n";

    private static final String ADD_LOAD_SNIPPET =
        "        loadFieldLowering.setSnippet(Kind.#UKIND#, snippet(FieldSnippets.class, \"getField#UKIND#Snippet\"));\n" +
        "        loadUnresolvedFieldLowering.setSnippet(Kind.#UKIND#, snippet(FieldSnippets.class, \"getUnresolvedField#UKIND#Snippet\"));\n";

    private static final String ADD_STORE_SNIPPET =
        "        storeFieldLowering.setSnippet(Kind.#UKIND#, snippet(FieldSnippets.class, \"putField#UKIND#Snippet\"));\n" +
        "        storeUnresolvedFieldLowering.setSnippet(Kind.#UKIND#, snippet(FieldSnippets.class, \"putUnresolvedField#UKIND#Snippet\"));\n";



    @Override
    protected void doGenerate() throws IOException {
        ArrayList<String> addSnippets = new ArrayList<>();
        for (Kind kind : Kind.values()) {
            if (notVoidOrIllegal(kind)) {
                out.print(replaceKinds(GET_FIELD_METHOD, kind));
                out.print(replaceKinds(PUT_FIELD_METHOD, kind));
                out.print(replaceUCast(replaceKinds(GET_FIELD_SNIPPET, kind), kind));
                out.print(replaceKinds(PUT_FIELD_SNIPPET, kind));
                // Unresolved variants
                out.print(replaceUCast(replaceKinds(GET_UNRESOLVED_FIELD_SNIPPET, kind), kind));
                out.print(replaceKinds(PUT_UNRESOLVED_FIELD_SNIPPET, kind));

                addSnippets.add(replaceKinds(ADD_LOAD_SNIPPET, kind));
                addSnippets.add(replaceKinds(ADD_STORE_SNIPPET, kind));
            }
        }
        out.print(ADD_SNIPPETS_DECL);
        for (String addSnippet : addSnippets) {
            out.print(addSnippet);
        }
        out.print("    }\n");
    }

    private String replaceModes(String template, String mode) {
        String uMode = toFirstUpper(mode);
        String result = replace(template, "#UMODE#", uMode);
        return replace(result, "#MODE#", mode);
    }

    public static void main(String[] args) throws IOException {
        if (new FieldSnippetsGenerator().generate(false, FieldSnippets.class)) {
            System.exit(1);
        }
    }

}

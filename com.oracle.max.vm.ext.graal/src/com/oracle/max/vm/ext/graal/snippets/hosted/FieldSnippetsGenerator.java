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
import com.sun.max.ide.*;
import com.sun.max.io.*;


/**
 * Generates the boilerplate for {@link FieldSnippets}.
 *
 * We try to be consistent on naming. Problematic because the bytecode names are GET/PUT/STATIC and GET/PUT/FIELD yet
 * we also refer to them to as "static fields" and "instance fields", which is more natural. There is also existing
 * code in Maxine that uses names "InstanceField" and "StaticField", so we follow this approach. TODO actually implement it!
 */
@HOSTED_ONLY
public class FieldSnippetsGenerator {

    private static final String UNSAFE_CAST_BEFORE = "UnsafeCastNode.unsafeCast(";
    private static final String UNSAFE_CAST_AFTER = ", StampFactory.forNodeIntrinsic())";

    private static final String STORE_FIELD_SNIPPET =
        "    @Snippet(inlining = MaxSnippetInliningPolicy.class)\n" +
        "    public static void store#UKIND#FieldSnippet(@Parameter(\"object\") Object object,\n" + "" +
        "            @Parameter(\"offset\") int offset, @ConstantParameter(\"isVolatile\") boolean isVolatile, @Parameter(\"value\") #KIND# value) {\n" +
        "        if (isVolatile) {\n" +
        "            memoryBarrier(JMM_PRE_VOLATILE_WRITE);\n" +
        "        }\n" +
        "        TupleAccess.write#UKIND#(object, offset, value);\n" +
        "        if (isVolatile) {\n" +
        "            memoryBarrier(JMM_POST_VOLATILE_READ);\n" +
        "        }\n" +
        "    }\n\n";

    private static final String LOAD_FIELD_SNIPPET =
        "    @Snippet(inlining = MaxSnippetInliningPolicy.class)\n" +
        "    public static #KIND# load#UKIND#FieldSnippet(@Parameter(\"object\") Object object,\n" +
        "            @Parameter(\"offset\") int offset, @ConstantParameter(\"isVolatile\") boolean isVolatile) {\n" +
        "        if (isVolatile) {\n" +
        "            memoryBarrier(JMM_PRE_VOLATILE_READ);\n" +
        "        }\n" +
        "        #KIND# result = TupleAccess.read#UKIND#(object, offset);\n" +
        "        if (isVolatile) {\n" +
        "            memoryBarrier(JMM_POST_VOLATILE_READ);\n" +
        "        }\n" +
        "        return #UCB#result#UCA#;\n" +
        "    }\n\n";

    private static final String HOLDER_INIT_GET_FIELD_METHOD =
        "    @RUNTIME_ENTRY\n" +
        "    public static #KIND# holderInitAndGetField#UKIND#(FieldActor f, Object object) {\n" +
        "        Snippets.makeHolderInitialized(f);\n" +
        "        return load#UKIND#FieldSnippet(object, f.offset(), f.isVolatile());\n" +
        "    }\n\n";

    private static final String HOLDER_INIT_GET_STATIC_METHOD =
        "    @RUNTIME_ENTRY\n" +
        "    public static #KIND# holderInitAndGetStatic#UKIND#(FieldActor f) {\n" +
        "        Snippets.makeHolderInitialized(f);\n" +
        "        return load#UKIND#FieldSnippet(f.holder().staticTuple(), f.offset(), f.isVolatile());\n" +
        "    }\n\n";

    private static final String HOLDER_INIT_PUT_FIELD_METHOD =
        "    @RUNTIME_ENTRY\n" +
        "    public static void holderInitAndPutField#UKIND#(FieldActor f, Object object, #KIND# value) {\n" +
        "        Snippets.makeHolderInitialized(f);\n" +
        "        store#UKIND#FieldSnippet(object, f.offset(), f.isVolatile(), value);\n" +
        "    }\n\n";

    private static final String HOLDER_INIT_PUT_STATIC_METHOD =
        "    @RUNTIME_ENTRY\n" +
        "    public static void holderInitAndPutStatic#UKIND#(FieldActor f, #KIND# value) {\n" +
        "        Snippets.makeHolderInitialized(f);\n" +
        "        store#UKIND#FieldSnippet(f.holder().staticTuple(), f.offset(), f.isVolatile(), value);\n" +
        "    }\n\n";

    private static final String RESOLVE_LOAD_FIELD_METHOD =
         "    @RUNTIME_ENTRY\n" +
         "    public static #KIND# resolveAndGetField#UKIND#(ResolutionGuard.InPool guard, Object object) {\n" +
         "        FieldActor f = Snippets.resolveInstanceFieldForReading(guard);\n" +
         "        return holderInitAndGetField#UKIND#(f, object);\n" +
         "    }\n\n";

    private static final String RESOLVE_LOAD_STATIC_METHOD =
         "    @RUNTIME_ENTRY\n" +
         "    public static #KIND# resolveAndGetStatic#UKIND#(ResolutionGuard.InPool guard) {\n" +
         "        FieldActor f = Snippets.resolveStaticFieldForReading(guard);\n" +
         "        return holderInitAndGetStatic#UKIND#(f);\n" +
         "    }\n\n";

    private static final String RESOLVE_STORE_FIELD_METHOD =
         "    @RUNTIME_ENTRY\n" +
         "    public static void resolveAndPutField#UKIND#(ResolutionGuard.InPool guard, Object object, #KIND# value) {\n" +
         "        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);\n" +
         "        holderInitAndPutField#UKIND#(f, object, value);\n" +
         "    }\n\n";

    private static final String RESOLVE_STORE_STATIC_METHOD =
         "    @RUNTIME_ENTRY\n" +
         "    public static void resolveAndPutStatic#UKIND#(ResolutionGuard.InPool guard, #KIND# value) {\n" +
         "        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);\n" +
         "        holderInitAndPutStatic#UKIND#(f, value);\n" +
         "    }\n\n";

    private static final String LOAD_UNRESOLVED_FIELD_SNIPPET =
        "    @Snippet(inlining = MaxSnippetInliningPolicy.class)\n" +
        "    public static #KIND# load#UKIND#Unresolved#MODE#Snippet(@Parameter(\"guard\") ResolutionGuard.InPool guard, @Parameter(\"object\") Object object) {\n" +
        "        return #UCB#resolveAndGet#MODE##UKIND#(guard, object)#UCA#;\n" +
        "    }\n\n";


    private static final String STORE_UNRESOLVED_FIELD_SNIPPET =
        "    @Snippet(inlining = MaxSnippetInliningPolicy.class)\n" +
        "    public static void store#UKIND#Unresolved#MODE#Snippet(@Parameter(\"guard\") ResolutionGuard.InPool guard, @Parameter(\"object\") Object object,\n" +
        "            @Parameter(\"value\") #KIND# value) {\n" +
        "        resolveAndPut#MODE##UKIND#(guard, object, value);\n" +
        "    }\n\n";


    private static final String LOAD_UNRESOLVED_STATIC_SNIPPET =
        "    @Snippet(inlining = MaxSnippetInliningPolicy.class)\n" +
        "    public static #KIND# load#UKIND#Unresolved#MODE#Snippet(@Parameter(\"guard\") ResolutionGuard.InPool guard) {\n" +
        "        return #UCB#resolveAndGet#MODE##UKIND#(guard)#UCA#;\n" +
        "    }\n\n";


    private static final String STORE_UNRESOLVED_STATIC_SNIPPET =
        "    @Snippet(inlining = MaxSnippetInliningPolicy.class)\n" +
        "    public static void store#UKIND#Unresolved#MODE#Snippet(@Parameter(\"guard\") ResolutionGuard.InPool guard,\n" +
        "            @Parameter(\"value\") #KIND# value) {\n" +
        "        resolveAndPut#MODE##UKIND#(guard, value);\n" +
        "    }\n\n";

    private static final String LOAD_HOLDER_INIT_FIELD_SNIPPET =
        "    @Snippet(inlining = MaxSnippetInliningPolicy.class)\n" +
        "    public static #KIND# load#UKIND#HolderInit#MODE#Snippet(@Parameter(\"f\") FieldActor f, @Parameter(\"object\") Object object) {\n" +
        "        return #UCB#holderInitAndGet#MODE##UKIND#(f, object)#UCA#;\n" +
        "    }\n\n";


    private static final String STORE_HOLDER_INIT_FIELD_SNIPPET =
        "    @Snippet(inlining = MaxSnippetInliningPolicy.class)\n" +
        "    public static void store#UKIND#HolderInit#MODE#Snippet(@Parameter(\"f\") FieldActor f, @Parameter(\"object\") Object object,\n" +
        "            @Parameter(\"value\") #KIND# value) {\n" +
        "        holderInitAndPut#MODE##UKIND#(f, object, value);\n" +
        "    }\n\n";


    private static final String LOAD_HOLDER_INIT_STATIC_SNIPPET =
        "    @Snippet(inlining = MaxSnippetInliningPolicy.class)\n" +
        "    public static #KIND# load#UKIND#HolderInit#MODE#Snippet(@Parameter(\"f\") FieldActor f) {\n" +
        "        return #UCB#holderInitAndGet#MODE##UKIND#(f)#UCA#;\n" +
        "    }\n\n";


    private static final String STORE_HOLDER_INIT_STATIC_SNIPPET =
        "    @Snippet(inlining = MaxSnippetInliningPolicy.class)\n" +
        "    public static void store#UKIND#HolderInit#MODE#Snippet(@Parameter(\"f\") FieldActor f,\n" +
        "            @Parameter(\"value\") #KIND# value) {\n" +
        "        holderInitAndPut#MODE##UKIND#(f, value);\n" +
        "    }\n\n";


    private static final String ADD_SNIPPETS_DECL =
        "    @HOSTED_ONLY\n" +
        "    private void addSnippets(\n" +
        "            LoadFieldLowering loadFieldLowering, StoreFieldLowering storeFieldLowering,\n" +
        "            LoadUnresolvedFieldLowering loadUnresolvedFieldLowering, StoreUnresolvedFieldLowering storeUnresolvedFieldLowering) {\n";

    private static final String ADD_LOAD_SNIPPET =
        "        loadFieldLowering.setSnippet(Kind.#UKIND#, findSnippet(FieldSnippets.class, \"load#UKIND#FieldSnippet\"));\n";

    private static final String ADD_STORE_SNIPPET =
        "        storeFieldLowering.setSnippet(Kind.#UKIND#, findSnippet(FieldSnippets.class, \"store#UKIND#FieldSnippet\"));\n";


    private static final String ADD_UNRESOLVED_LOAD_SNIPPET =
        "        loadUnresolvedFieldLowering.setSnippet(Kind.#UKIND#, findSnippet(FieldSnippets.class, \"load#UKIND#Unresolved#UMODE#Snippet\"));\n";

    private static final String ADD_UNRESOLVED_STORE_SNIPPET =
        "        storeUnresolvedFieldLowering.setSnippet(Kind.#UKIND#, findSnippet(FieldSnippets.class, \"store#UKIND#Unresolved#UMODE#Snippet\"));\n";

    private static final String ADD_UNRESOLVED_STATIC_LOAD_SNIPPET =
        "        loadUnresolvedFieldLowering.#MODE#FieldLowering.setSnippet(Kind.#UKIND#, findSnippet(FieldSnippets.class, \"load#UKIND#Unresolved#UMODE#Snippet\"));\n";

    private static final String ADD_UNRESOLVED_STATIC_STORE_SNIPPET =
        "        storeUnresolvedFieldLowering.#MODE#FieldLowering.setSnippet(Kind.#UKIND#, findSnippet(FieldSnippets.class, \"store#UKIND#Unresolved#UMODE#Snippet\"));\n";

    private static final String ADD_HOLDER_INIT_LOAD_SNIPPET =
        "        loadUnresolvedFieldLowering.initHolder#UMODE#Lowering.setSnippet(Kind.#UKIND#, findSnippet(FieldSnippets.class, \"load#UKIND#HolderInit#UMODE#Snippet\"));\n";

    private static final String ADD_HOLDER_INIT_STORE_SNIPPET =
        "        storeUnresolvedFieldLowering.initHolder#UMODE#Lowering.setSnippet(Kind.#UKIND#, findSnippet(FieldSnippets.class, \"store#UKIND#HolderInit#UMODE#Snippet\"));\n";

    private static final String ADD_HOLDER_INIT_STATIC_LOAD_SNIPPET =
        "        loadUnresolvedFieldLowering.initHolder#UMODE#Lowering.setSnippet(Kind.#UKIND#, findSnippet(FieldSnippets.class, \"load#UKIND#HolderInit#UMODE#Snippet\"));\n";

    private static final String ADD_HOLDER_INIT_STATIC_STORE_SNIPPET =
        "        storeUnresolvedFieldLowering.initHolder#UMODE#Lowering.setSnippet(Kind.#UKIND#, findSnippet(FieldSnippets.class, \"store#UKIND#HolderInit#UMODE#Snippet\"));\n";

    private PrintStream out;

    private FieldSnippetsGenerator(PrintStream out) {
        this.out = out;
    }

    private static boolean generate(boolean checkOnly, Class target) throws IOException {
        File base = new File(JavaProject.findWorkspace(), "com.oracle.max.vm.ext.graal/src");
        File outputFile = new File(base, target.getName().replace('.', File.separatorChar) + ".java").getAbsoluteFile();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new FieldSnippetsGenerator(new PrintStream(baos)).doGenerate();
        ReadableSource content = ReadableSource.Static.fromString(baos.toString());
        return Files.updateGeneratedContent(outputFile, content, "// START GENERATED CODE", "// END GENERATED CODE", checkOnly);
    }

    private void doGenerate() {
        ArrayList<String> addSnippets = new ArrayList<>();
        for (Kind kind : Kind.values()) {
            if (hasFieldOp(kind)) {
                out.print(replaceUCast(replaceKinds(LOAD_FIELD_SNIPPET, kind), kind));
                out.print(replaceKinds(STORE_FIELD_SNIPPET, kind));
                // Unresolved variants
                out.print(replaceKinds(RESOLVE_LOAD_FIELD_METHOD, kind));
                out.print(replaceKinds(RESOLVE_LOAD_STATIC_METHOD, kind));
                out.print(replaceKinds(RESOLVE_STORE_FIELD_METHOD, kind));
                out.print(replaceKinds(RESOLVE_STORE_STATIC_METHOD, kind));

                out.print(replaceKinds(HOLDER_INIT_GET_FIELD_METHOD, kind));
                out.print(replaceKinds(HOLDER_INIT_GET_STATIC_METHOD, kind));
                out.print(replaceKinds(HOLDER_INIT_PUT_FIELD_METHOD, kind));
                out.print(replaceKinds(HOLDER_INIT_PUT_STATIC_METHOD, kind));

                out.print(replace(replaceUCast(replaceKinds(LOAD_HOLDER_INIT_FIELD_SNIPPET, kind), kind), "#MODE#", "Field"));
                out.print(replace(replaceUCast(replaceKinds(LOAD_HOLDER_INIT_STATIC_SNIPPET, kind), kind), "#MODE#", "Static"));
                out.print(replace(replaceKinds(STORE_HOLDER_INIT_FIELD_SNIPPET, kind), "#MODE#", "Field"));
                out.print(replace(replaceKinds(STORE_HOLDER_INIT_STATIC_SNIPPET, kind), "#MODE#", "Static"));

                out.print(replace(replaceUCast(replaceKinds(LOAD_UNRESOLVED_FIELD_SNIPPET, kind), kind), "#MODE#", "Field"));
                out.print(replace(replaceUCast(replaceKinds(LOAD_UNRESOLVED_STATIC_SNIPPET, kind), kind), "#MODE#", "Static"));
                out.print(replace(replaceKinds(STORE_UNRESOLVED_FIELD_SNIPPET, kind), "#MODE#", "Field"));
                out.print(replace(replaceKinds(STORE_UNRESOLVED_STATIC_SNIPPET, kind), "#MODE#", "Static"));

                addSnippets.add(replaceKinds(ADD_LOAD_SNIPPET, kind));
                addSnippets.add(replaceKinds(ADD_STORE_SNIPPET, kind));
                addSnippets.add(replaceModes(replaceKinds(ADD_UNRESOLVED_LOAD_SNIPPET, kind), "field"));
                addSnippets.add(replaceModes(replaceKinds(ADD_UNRESOLVED_STATIC_LOAD_SNIPPET, kind), "static"));
                addSnippets.add(replaceModes(replaceKinds(ADD_UNRESOLVED_STORE_SNIPPET, kind), "field"));
                addSnippets.add(replaceModes(replaceKinds(ADD_UNRESOLVED_STATIC_STORE_SNIPPET, kind), "static"));

                addSnippets.add(replaceModes(replaceKinds(ADD_HOLDER_INIT_LOAD_SNIPPET, kind), "field"));
                addSnippets.add(replaceModes(replaceKinds(ADD_HOLDER_INIT_STATIC_LOAD_SNIPPET, kind), "static"));
                addSnippets.add(replaceModes(replaceKinds(ADD_HOLDER_INIT_STORE_SNIPPET, kind), "field"));
                addSnippets.add(replaceModes(replaceKinds(ADD_HOLDER_INIT_STATIC_STORE_SNIPPET, kind), "static"));
            }
        }
        out.print(ADD_SNIPPETS_DECL);
        for (String addSnippet : addSnippets) {
            out.print(addSnippet);
        }
        out.print("    }\n");
    }

    private String replaceUCast(String template, Kind kind) {
        String ucb = kind != Kind.Object ? "" : UNSAFE_CAST_BEFORE;
        String uca = kind != Kind.Object ? "" : UNSAFE_CAST_AFTER;
        return replace(replace(template, "#UCA#", uca), "#UCB#", ucb);
    }

    /**
     * Returns the argument with first character upper-cased.
     * @param s
     */
    private static String toFirstUpper(String s) {
        if (s.length() == 0) {
            return s;
        } else {
            return s.substring(0, 1).toUpperCase() + s.substring(1);
        }
    }

    private String replaceKinds(String template, Kind kind) {
        String uJavaName = toFirstUpper(kind.getJavaName());
        String result = replace(template, "#UKIND#", uJavaName);
        return replace(result, "#KIND#", kind.getJavaName());
    }

    private String replaceModes(String template, String mode) {
        String uMode = toFirstUpper(mode);
        String result = replace(template, "#UMODE#", uMode);
        return replace(result, "#MODE#", mode);
    }

    private String replace(String template, String param, String arg) {
        return template.replaceAll(param, arg);
    }

    private boolean hasFieldOp(Kind kind) {
        return !(kind == Kind.Void || kind == Kind.Illegal);
    }

    public static void main(String[] args) throws IOException {
        if (generate(false, FieldSnippets.class)) {
            System.out.println("Source for " + FieldSnippets.class + " was updated");
            System.exit(1);
        }
    }

}

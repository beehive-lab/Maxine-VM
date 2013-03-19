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
package com.oracle.max.vm.ext.graal.snippets;

import java.util.*;

import com.oracle.graal.api.code.*;
import com.oracle.graal.api.meta.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.nodes.java.*;
import com.oracle.graal.nodes.spi.*;
import com.oracle.graal.snippets.*;
import com.oracle.graal.snippets.Snippet.Parameter;
import com.oracle.graal.snippets.SnippetTemplate.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.object.*;


public class ArraySnippets extends SnippetLowerings implements SnippetsInterface {

    @HOSTED_ONLY
    public ArraySnippets(CodeCacheProvider runtime, TargetDescription targetDescription, Assumptions assumptions, Map<Class< ? extends Node>, LoweringProvider> lowerings) {
        super(runtime, assumptions, targetDescription);
    }

    @Override
    @HOSTED_ONLY
    public void registerLowerings(CodeCacheProvider runtime, TargetDescription targetDescription, Assumptions assumptions, Map<Class< ? extends Node>, LoweringProvider> lowerings) {
        lowerings.put(ArrayLengthNode.class, new ArrayLengthLowering(this));

        LoadIndexedLowering loadIndexedLowering = new LoadIndexedLowering();
        StoreIndexedLowering storeIndexedLowering = new StoreIndexedLowering();
        addSnippets(loadIndexedLowering, storeIndexedLowering);

        lowerings.put(LoadIndexedNode.class, loadIndexedLowering);
        lowerings.put(StoreIndexedNode.class, storeIndexedLowering);
    }

    protected class ArrayLengthLowering extends Lowering implements LoweringProvider<ArrayLengthNode> {

        ArrayLengthLowering(ArraySnippets newSnippets) {
            super(newSnippets, "arrayLengthSnippet");
        }

        @Override
        public void lower(ArrayLengthNode node, LoweringTool tool) {
            Key key = new Key(snippet);
            Arguments args = new Arguments();
            args.add("array", node.array());
            instantiate(node, key, args);
        }

    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static int arrayLengthSnippet(@Parameter("array") Object array) {
        return ArrayAccess.readArrayLength(array);
    }

    protected abstract class IndexedLowering extends Lowering {
        protected final ResolvedJavaMethod[] snippets = new ResolvedJavaMethod[Kind.values().length];

        void setSnippet(Kind kind, ResolvedJavaMethod snippet) {
            snippets[kind.ordinal()] = snippet;
        }

        void lower(AccessIndexedNode node) {
            Key key = new Key(snippets[node.elementKind().ordinal()]);
            Arguments args = new Arguments();
            args.add("array", node.array());
            args.add("index", node.index());
            storeIndexedArg(node, args);
            instantiate(node, key, args);
        }

        protected void storeIndexedArg(AccessIndexedNode node, Arguments args) {
        }

    }

    protected class LoadIndexedLowering extends IndexedLowering implements LoweringProvider<LoadIndexedNode> {
        @Override
        public void lower(LoadIndexedNode node, LoweringTool tool) {
            lower(node);
        }
    }

    protected class StoreIndexedLowering extends IndexedLowering implements LoweringProvider<StoreIndexedNode> {
        @Override
        public void lower(StoreIndexedNode node, LoweringTool tool) {
            lower(node);
        }

        @Override
        protected void storeIndexedArg(AccessIndexedNode node, Arguments args) {
            args.add("value", ((StoreIndexedNode) node).value());
        }
    }

// START GENERATED CODE
    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static boolean zaloadSnippet(@Parameter("array") Object array, @Parameter("index") int index) {
        ArrayAccess.checkIndex(array, index);
        return ArrayAccess.getBoolean(array, index);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static void zastoreSnippet(@Parameter("array") Object array, @Parameter("index") int index,
            @Parameter("value") boolean value) {
        ArrayAccess.checkIndex(array, index);
        ArrayAccess.setBoolean(array, index, value);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static byte baloadSnippet(@Parameter("array") Object array, @Parameter("index") int index) {
        ArrayAccess.checkIndex(array, index);
        return ArrayAccess.getByte(array, index);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static void bastoreSnippet(@Parameter("array") Object array, @Parameter("index") int index,
            @Parameter("value") byte value) {
        ArrayAccess.checkIndex(array, index);
        ArrayAccess.setByte(array, index, value);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static short saloadSnippet(@Parameter("array") Object array, @Parameter("index") int index) {
        ArrayAccess.checkIndex(array, index);
        return ArrayAccess.getShort(array, index);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static void sastoreSnippet(@Parameter("array") Object array, @Parameter("index") int index,
            @Parameter("value") short value) {
        ArrayAccess.checkIndex(array, index);
        ArrayAccess.setShort(array, index, value);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static char caloadSnippet(@Parameter("array") Object array, @Parameter("index") int index) {
        ArrayAccess.checkIndex(array, index);
        return ArrayAccess.getChar(array, index);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static void castoreSnippet(@Parameter("array") Object array, @Parameter("index") int index,
            @Parameter("value") char value) {
        ArrayAccess.checkIndex(array, index);
        ArrayAccess.setChar(array, index, value);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static int ialoadSnippet(@Parameter("array") Object array, @Parameter("index") int index) {
        ArrayAccess.checkIndex(array, index);
        return ArrayAccess.getInt(array, index);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static void iastoreSnippet(@Parameter("array") Object array, @Parameter("index") int index,
            @Parameter("value") int value) {
        ArrayAccess.checkIndex(array, index);
        ArrayAccess.setInt(array, index, value);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static float faloadSnippet(@Parameter("array") Object array, @Parameter("index") int index) {
        ArrayAccess.checkIndex(array, index);
        return ArrayAccess.getFloat(array, index);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static void fastoreSnippet(@Parameter("array") Object array, @Parameter("index") int index,
            @Parameter("value") float value) {
        ArrayAccess.checkIndex(array, index);
        ArrayAccess.setFloat(array, index, value);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static long jaloadSnippet(@Parameter("array") Object array, @Parameter("index") int index) {
        ArrayAccess.checkIndex(array, index);
        return ArrayAccess.getLong(array, index);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static void jastoreSnippet(@Parameter("array") Object array, @Parameter("index") int index,
            @Parameter("value") long value) {
        ArrayAccess.checkIndex(array, index);
        ArrayAccess.setLong(array, index, value);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static double daloadSnippet(@Parameter("array") Object array, @Parameter("index") int index) {
        ArrayAccess.checkIndex(array, index);
        return ArrayAccess.getDouble(array, index);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static void dastoreSnippet(@Parameter("array") Object array, @Parameter("index") int index,
            @Parameter("value") double value) {
        ArrayAccess.checkIndex(array, index);
        ArrayAccess.setDouble(array, index, value);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static Object aaloadSnippet(@Parameter("array") Object array, @Parameter("index") int index) {
        ArrayAccess.checkIndex(array, index);
        return ArrayAccess.getObject(array, index);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static void aastoreSnippet(@Parameter("array") Object array, @Parameter("index") int index,
            @Parameter("value") Object value) {
        ArrayAccess.checkIndex(array, index);
        ArrayAccess.setObject(array, index, value);
    }

    @HOSTED_ONLY
    private void addSnippets(LoadIndexedLowering loadIndexedLowering, StoreIndexedLowering storeIndexedLowering) {
        loadIndexedLowering.setSnippet(Kind.Boolean, findSnippet(ArraySnippets.class, "zaloadSnippet"));
        storeIndexedLowering.setSnippet(Kind.Boolean, findSnippet(ArraySnippets.class, "zastoreSnippet"));
        loadIndexedLowering.setSnippet(Kind.Byte, findSnippet(ArraySnippets.class, "baloadSnippet"));
        storeIndexedLowering.setSnippet(Kind.Byte, findSnippet(ArraySnippets.class, "bastoreSnippet"));
        loadIndexedLowering.setSnippet(Kind.Short, findSnippet(ArraySnippets.class, "saloadSnippet"));
        storeIndexedLowering.setSnippet(Kind.Short, findSnippet(ArraySnippets.class, "sastoreSnippet"));
        loadIndexedLowering.setSnippet(Kind.Char, findSnippet(ArraySnippets.class, "caloadSnippet"));
        storeIndexedLowering.setSnippet(Kind.Char, findSnippet(ArraySnippets.class, "castoreSnippet"));
        loadIndexedLowering.setSnippet(Kind.Int, findSnippet(ArraySnippets.class, "ialoadSnippet"));
        storeIndexedLowering.setSnippet(Kind.Int, findSnippet(ArraySnippets.class, "iastoreSnippet"));
        loadIndexedLowering.setSnippet(Kind.Float, findSnippet(ArraySnippets.class, "faloadSnippet"));
        storeIndexedLowering.setSnippet(Kind.Float, findSnippet(ArraySnippets.class, "fastoreSnippet"));
        loadIndexedLowering.setSnippet(Kind.Long, findSnippet(ArraySnippets.class, "jaloadSnippet"));
        storeIndexedLowering.setSnippet(Kind.Long, findSnippet(ArraySnippets.class, "jastoreSnippet"));
        loadIndexedLowering.setSnippet(Kind.Double, findSnippet(ArraySnippets.class, "daloadSnippet"));
        storeIndexedLowering.setSnippet(Kind.Double, findSnippet(ArraySnippets.class, "dastoreSnippet"));
        loadIndexedLowering.setSnippet(Kind.Object, findSnippet(ArraySnippets.class, "aaloadSnippet"));
        storeIndexedLowering.setSnippet(Kind.Object, findSnippet(ArraySnippets.class, "aastoreSnippet"));
    }
// END GENERATED CODE

}

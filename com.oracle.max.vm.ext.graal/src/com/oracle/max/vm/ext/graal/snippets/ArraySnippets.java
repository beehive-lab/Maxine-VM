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
import com.oracle.graal.replacements.*;
import com.oracle.graal.replacements.SnippetTemplate.*;
import com.oracle.max.cri.intrinsics.UnsignedMath;
import com.oracle.max.vm.ext.graal.nodes.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.runtime.*;


public class ArraySnippets extends SnippetLowerings {

    @HOSTED_ONLY
    public ArraySnippets(CodeCacheProvider runtime, Replacements replacements, TargetDescription targetDescription, Map<Class< ? extends Node>, LoweringProvider> lowerings) {
        super(runtime, replacements, targetDescription);
    }

    @Override
    @HOSTED_ONLY
    public void registerLowerings(CodeCacheProvider runtime, Replacements replacements, TargetDescription targetDescription, Map<Class< ? extends Node>, LoweringProvider> lowerings) {
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
            Arguments args = new Arguments(snippet);
            args.add("array", node.array());
            instantiate(node, args);
        }

    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static int arrayLengthSnippet(Object array) {
        return ArrayAccess.readArrayLength(array);
    }

    protected abstract class IndexedLowering extends Lowering {
        protected final SnippetInfo[] snippets = new SnippetInfo[Kind.values().length];

        void setSnippet(Kind kind, SnippetInfo snippet) {
            snippets[kind.ordinal()] = snippet;
        }

        void lower(AccessIndexedNode node) {
            Arguments args = new Arguments(snippets[node.elementKind().ordinal()]);
            args.add("array", node.array());
            args.add("index", node.index());
            storeIndexedArg(node, args);
            instantiate(node, args);
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

    /**
     * Functionally equivalent to {@link ArrayAccess.checkIndex}.
     * However, is Graal-specific by way of the {@code throw UnreachableNode.unreachable();},
     * which is important to avoid unnecessary checks.
     */
    private static void checkIndex(Object array, int index) {
        // note that we must read the array length first (implicit null check has precedence over bounds check)
        if (UnsignedMath.aboveOrEqual(index, ArrayAccess.readArrayLength(array))) {
            Throw.throwArrayIndexOutOfBoundsException(array, index);
            throw UnreachableNode.unreachable();
        }
    }

    /**
     * {@see checkIndex}.
     */
    private static void checkSetObject(Object array, Object value) {
        if (value != null) {
            final ClassActor arrayClassActor = ObjectAccess.readClassActor(array);
            if (!arrayClassActor.componentClassActor().isNonNullInstance(value)) {
                Throw.throwArrayStoreException(array, value);
                throw UnreachableNode.unreachable();
            }
        }
    }


// START GENERATED CODE
    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static boolean zaloadSnippet(Object array, int index) {
        checkIndex(array, index);
        return ArrayAccess.getBoolean(array, index);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static void zastoreSnippet(Object array, int index, boolean value) {
        checkIndex(array, index);
        ArrayAccess.setBoolean(array, index, value);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static byte baloadSnippet(Object array, int index) {
        checkIndex(array, index);
        return ArrayAccess.getByte(array, index);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static void bastoreSnippet(Object array, int index, byte value) {
        checkIndex(array, index);
        ArrayAccess.setByte(array, index, value);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static short saloadSnippet(Object array, int index) {
        checkIndex(array, index);
        return ArrayAccess.getShort(array, index);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static void sastoreSnippet(Object array, int index, short value) {
        checkIndex(array, index);
        ArrayAccess.setShort(array, index, value);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static char caloadSnippet(Object array, int index) {
        checkIndex(array, index);
        return ArrayAccess.getChar(array, index);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static void castoreSnippet(Object array, int index, char value) {
        checkIndex(array, index);
        ArrayAccess.setChar(array, index, value);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static int ialoadSnippet(Object array, int index) {
        checkIndex(array, index);
        return ArrayAccess.getInt(array, index);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static void iastoreSnippet(Object array, int index, int value) {
        checkIndex(array, index);
        ArrayAccess.setInt(array, index, value);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static float faloadSnippet(Object array, int index) {
        checkIndex(array, index);
        return ArrayAccess.getFloat(array, index);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static void fastoreSnippet(Object array, int index, float value) {
        checkIndex(array, index);
        ArrayAccess.setFloat(array, index, value);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static long jaloadSnippet(Object array, int index) {
        checkIndex(array, index);
        return ArrayAccess.getLong(array, index);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static void jastoreSnippet(Object array, int index, long value) {
        checkIndex(array, index);
        ArrayAccess.setLong(array, index, value);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static double daloadSnippet(Object array, int index) {
        checkIndex(array, index);
        return ArrayAccess.getDouble(array, index);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static void dastoreSnippet(Object array, int index, double value) {
        checkIndex(array, index);
        ArrayAccess.setDouble(array, index, value);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static Object aaloadSnippet(Object array, int index) {
        checkIndex(array, index);
        return ArrayAccess.getObject(array, index);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static void aastoreSnippet(Object array, int index, Object value) {
        checkIndex(array, index);
        checkSetObject(array, value);
        ArrayAccess.setObject(array, index, value);
    }

    @HOSTED_ONLY
    private void addSnippets(LoadIndexedLowering loadIndexedLowering, StoreIndexedLowering storeIndexedLowering) {
        loadIndexedLowering.setSnippet(Kind.Boolean, snippet(ArraySnippets.class, "zaloadSnippet"));
        storeIndexedLowering.setSnippet(Kind.Boolean, snippet(ArraySnippets.class, "zastoreSnippet"));
        loadIndexedLowering.setSnippet(Kind.Byte, snippet(ArraySnippets.class, "baloadSnippet"));
        storeIndexedLowering.setSnippet(Kind.Byte, snippet(ArraySnippets.class, "bastoreSnippet"));
        loadIndexedLowering.setSnippet(Kind.Short, snippet(ArraySnippets.class, "saloadSnippet"));
        storeIndexedLowering.setSnippet(Kind.Short, snippet(ArraySnippets.class, "sastoreSnippet"));
        loadIndexedLowering.setSnippet(Kind.Char, snippet(ArraySnippets.class, "caloadSnippet"));
        storeIndexedLowering.setSnippet(Kind.Char, snippet(ArraySnippets.class, "castoreSnippet"));
        loadIndexedLowering.setSnippet(Kind.Int, snippet(ArraySnippets.class, "ialoadSnippet"));
        storeIndexedLowering.setSnippet(Kind.Int, snippet(ArraySnippets.class, "iastoreSnippet"));
        loadIndexedLowering.setSnippet(Kind.Float, snippet(ArraySnippets.class, "faloadSnippet"));
        storeIndexedLowering.setSnippet(Kind.Float, snippet(ArraySnippets.class, "fastoreSnippet"));
        loadIndexedLowering.setSnippet(Kind.Long, snippet(ArraySnippets.class, "jaloadSnippet"));
        storeIndexedLowering.setSnippet(Kind.Long, snippet(ArraySnippets.class, "jastoreSnippet"));
        loadIndexedLowering.setSnippet(Kind.Double, snippet(ArraySnippets.class, "daloadSnippet"));
        storeIndexedLowering.setSnippet(Kind.Double, snippet(ArraySnippets.class, "dastoreSnippet"));
        loadIndexedLowering.setSnippet(Kind.Object, snippet(ArraySnippets.class, "aaloadSnippet"));
        storeIndexedLowering.setSnippet(Kind.Object, snippet(ArraySnippets.class, "aastoreSnippet"));
    }
// END GENERATED CODE

}

/*
 * Copyright (c) 2012, 2012, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.graal.api.code.RuntimeCallTarget.Descriptor;
import com.oracle.graal.api.meta.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.graph.Node.ConstantNodeParameter;
import com.oracle.graal.graph.Node.NodeIntrinsic;
import com.oracle.graal.nodes.extended.*;
import com.oracle.graal.nodes.java.*;
import com.oracle.graal.nodes.spi.*;
import com.oracle.graal.snippets.*;
import com.oracle.graal.snippets.Snippet.ConstantParameter;
import com.oracle.graal.snippets.Snippet.Parameter;
import com.oracle.graal.snippets.SnippetTemplate.Arguments;
import com.oracle.graal.snippets.SnippetTemplate.Key;
import com.oracle.max.vm.ext.graal.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.heap.*;

public class NewSnippets extends SnippetLowerings implements SnippetsInterface {

    private final VMConfiguration vmConfig;

    @NodeIntrinsic(value = RuntimeCallNode.class)
    public static native Object runtimeCall(@ConstantNodeParameter Descriptor descriptor, DynamicHub hub);

    @SuppressWarnings("unused")
    public static void registerLowerings(VMConfiguration config, TargetDescription targetDescription, MetaAccessProvider runtime, Assumptions assumptions, Map<Class< ? extends Node>, LoweringProvider> lowerings) {
        new NewSnippets(config, targetDescription, runtime, assumptions, lowerings);
    }

    private NewSnippets(VMConfiguration vmConfig, TargetDescription targetDescription, MetaAccessProvider runtime, Assumptions assumptions, Map<Class< ? extends Node>, LoweringProvider> lowerings) {
        super(runtime, assumptions, targetDescription);
        this.vmConfig = vmConfig;

        lowerings.put(NewInstanceNode.class, new NewInstanceLowering());
        /*
        lowerings.put(NewObjectArrayNode.class, new NewArrayLowering());
        lowerings.put(NewPrimitiveArrayNode.class, new NewArrayLowering());
        lowerings.put(NewMultiArrayNode.class, new NewMultiArrayLowering());
        lowerings.put(NewInnerMultiArrayNode.class, new LoweredNewMultiArrayLowering());
        lowerings.put(FastAllocateNode.class, new FastAllocateLowering());
        lowerings.put(FormatObjectNode.class, new FormatObjectLowering());
        lowerings.put(FormatArrayNode.class, new FormatArrayLowering());
        */
    }

    protected class NewInstanceLowering implements LoweringProvider<NewInstanceNode> {

        private final ResolvedJavaMethod newInstance = findSnippet(NewSnippets.class, "newInstanceSnippet");

        @Override
        public void lower(NewInstanceNode node, LoweringTool tool) {
            ClassActor type = (ClassActor) MaxResolvedJavaType.get(node.instanceClass());
            Key key = new Key(newInstance);
            key.add("fillContents", node.fillContents());
            Arguments args = new Arguments();
            args.add("hub", type.dynamicHub());
            cache.get(key, assumptions).instantiate(runtime, node, SnippetTemplate.DEFAULT_REPLACER, args);
        }
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static Object newInstanceSnippet(@Parameter("hub") DynamicHub hub, @ConstantParameter("fillContents") boolean fillContents) {
        return Heap.createTuple(hub);
    }

    /*
    protected class NewArrayLowering implements LoweringProvider<NewArrayNode> {

        private final ResolvedJavaMethod newArray = findSnippet(NewSnippets.class, "newArraySnippet");

        @Override
        public void lower(NewArrayNode node, LoweringTool tool) {
            ClassActor type = (ClassActor) ((ClassActor) node.elementType()).arrayOf();
            Key key = new Key(newArray);
            key.add("layoutEncoding", type.dynamicHub().getLayoutEncoding());
            key.add("fillContents", node.fillContents());
            Arguments args = new Arguments();
            args.add("hub", type.dynamicHub());
            args.add("length", node.length());
            cache.get(key, assumptions).instantiate(runtime, node, SnippetTemplate.DEFAULT_REPLACER, args);
        }
    }
    */

    /**
     * Maximum array length for which fast path allocation is used.
     */
    /*
    private static final int MAX_ARRAY_FAST_PATH_ALLOCATION_LENGTH = 0x00FFFFFF;

    @Snippet
    public static Object newArraySnippet(@Parameter("hub") DynamicHub hub, @Parameter("length") int length, @ConstantParameter("layoutEncoding") int layoutEncoding,
                    @ConstantParameter("fillContents") boolean fillContents) {
        Object result;
        try {
            if (aboveThan(length, MAX_ARRAY_FAST_PATH_ALLOCATION_LENGTH)) {
                // This handles both negative array sizes and very large array sizes.
                throw FastAllocationFailed.INSTANCE;
            }
            // Note: layoutEncoding is passed in as a @ConstantParameter so that much of the array size computation can
            // be folded away early when preparing the snippet.
            Word size = Word.fromLong(LayoutEncoding.arraySize(layoutEncoding, length, getVMConfig().getObjectLayout()));
            Word memory = fastAllocateImpl(size);
            result = formatArrayImpl(memory, hub, length, layoutEncoding, size, fillContents);
        } catch (FastAllocationFailed ex) {
            result = runtimeCall(SLOW_NEW_ARRAY, hub, length);
        }
        return unsafeArrayCast(result, length, StampFactory.forNodeIntrinsic());
    }

    @Snippet
    public static Word fastAllocateSnippet(@Parameter("size") Word size) {
        try {
            return fastAllocateImpl(size);
        } catch (FastAllocationFailed ex) {
            return Word.zero();
        }
    }

    @Snippet
    public static Object formatObjectSnippet(@Parameter("memory") Word memory, @Parameter("hub") DynamicHub hub) {
        Word size = Word.fromLong(LayoutEncoding.instanceSize(hub.getLayoutEncoding()));
        return formatObjectImpl(memory, hub, size, true);
    }

    @Snippet
    public static Object formatArraySnippet(@Parameter("memory") Word memory, @Parameter("hub") DynamicHub hub, @Parameter("length") int length) {
        int layoutEncoding = hub.getLayoutEncoding();
        Word size = Word.fromLong(LayoutEncoding.arraySize(layoutEncoding, length, getVMConfig().getObjectLayout()));
        return formatArrayImpl(memory, hub, length, layoutEncoding, size, true);
    }

    @Snippet
    public static Object newMultiArraySnippet(@Parameter("hub") DynamicHub hub, @Parameter("length") int length, @ConstantParameter("layoutEncoding") int layoutEncoding,
                    @ConstantParameter("innerDimensions") int innerDimensions, @Parameter("newMultiArray") Object newMultiArray) {
        Object result = newArraySnippet(hub, length, layoutEncoding, innerDimensions == 0);

        if (innerDimensions > 0) {
            Word offset = Word.fromLong(LayoutEncoding.firstArrayElementOffset(layoutEncoding));
            Word size = Word.fromLong(LayoutEncoding.arraySize(layoutEncoding, length, getVMConfig().getObjectLayout()));
            while (offset.below(size)) {
                storeObject(result, 0, offset.toLong(), newInnerMultiArray(newMultiArray, innerDimensions));
                offset = offset.plus(getVMConfig().getObjectLayout().getObjectSize());
            }
        }
        return result;
    }
*/
}


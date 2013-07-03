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

import static com.sun.max.vm.intrinsics.MaxineIntrinsicIDs.*;

import java.util.*;

import com.oracle.graal.api.code.*;
import com.oracle.graal.api.meta.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.extended.*;
import com.oracle.graal.nodes.spi.*;
import com.oracle.graal.nodes.type.*;
import com.oracle.graal.replacements.*;
import com.oracle.graal.replacements.SnippetTemplate.*;
import com.oracle.max.vm.ext.graal.*;
import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;

public class TestSnippets extends SnippetLowerings {

    public TestSnippets(CodeCacheProvider runtime, Replacements replacements, TargetDescription target, Map<Class< ? extends Node>, LoweringProvider> lowerings) {
        super(runtime, replacements, target);
    }

    @Override
    public void registerLowerings(CodeCacheProvider runtime, Replacements replacements, TargetDescription targetDescription, Map<Class< ? extends Node>, LoweringProvider> lowerings) {
//      lowerings.put(TestSnippetNode1.class, new TestSnippetLowering1(this));
//      lowerings.put(TestSnippetNode2.class, new TestSnippetLowering2(this));
    }

    private static Object testSnippet(ClassActor actor, Object object) {
        return testSnippetIntrinsic(actor, object);
    }

    @INTRINSIC(TEST_SNIPPET_2)
    private static Object testSnippetIntrinsic(ClassActor actor, Object object) {
        return null;
    }

    private static class TestSnippetNode1 extends FixedWithNextNode implements Lowerable {
        @Input ValueNode arg1;

        TestSnippetNode1(Stamp stamp, ValueNode arg1) {
            super(stamp);
            this.arg1 = arg1;
        }

        @Override
        public void lower(LoweringTool tool, LoweringType loweringType) {
            tool.getRuntime().lower(this, tool);
        }
    }

    private static class TestSnippetNode2 extends FixedWithNextNode implements Lowerable {
        @Input ValueNode arg1;
        @Input ValueNode arg2;

        TestSnippetNode2(Stamp stamp, ValueNode arg1, ValueNode arg2) {
            super(stamp);
            this.arg1 = arg1;
            this.arg2 = arg2;
        }

        @Override
        public void lower(LoweringTool tool, LoweringType loweringType) {
            tool.getRuntime().lower(this, tool);
        }

    }

    private class TestSnippetLowering1 extends Lowering implements LoweringProvider<TestSnippetNode1> {

        TestSnippetLowering1(TestSnippets testSnippets) {
            super(testSnippets, "xxx");
        }

        @Override
        public void lower(TestSnippetNode1 node, LoweringTool tool) {
            Arguments args = new Arguments(snippet);
            args.add("object", node.arg1);
            instantiate(node, args);
        }

    }

    private class TestSnippetLowering2 extends Lowering implements LoweringProvider<TestSnippetNode2> {

        TestSnippetLowering2(TestSnippets testSnippets) {
            super(testSnippets, "testThrow");
        }

        @Override
        public void lower(TestSnippetNode2 node, LoweringTool tool) {
            Arguments args = new Arguments(snippet);
            args.add("actor", node.arg2);
            args.add("object", node.arg1);
            instantiate(node, args);
        }

    }

    public static ValueNode createTestSnippet1(StructuredGraph graph, ResolvedJavaMethod method, ValueNode arg1) {
        return graph.add(new TestSnippetNode1(stampForReturnType(method), arg1));
    }

    public static ValueNode createTestSnippet2(StructuredGraph graph, ResolvedJavaMethod method, ValueNode arg1, ValueNode arg2) {
        return graph.add(new TestSnippetNode2(stampForReturnType(method), arg1, arg2));
    }

    private static Stamp stampForReturnType(ResolvedJavaMethod method) {
        ResolvedJavaType type = (ResolvedJavaType) method.getSignature().getReturnType(method.getDeclaringClass());
        return MaxIntrinsics.stampFor(type);
    }

    //@Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static boolean testIsHosted() {
        if (MaxineVM.isHosted()) {
            return true;
        } else {
            return false;
        }
    }

    //@Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static int testIsHostedArg(int a, int b) {
        if (MaxineVM.isHosted()) {
            return a;
        } else {
            return b;
        }
    }

    //@Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static long testWordWidth() {
        return com.sun.max.unsafe.Word.width();
    }

    //@Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static com.sun.max.unsafe.Address testWordAsAddress(com.sun.max.unsafe.Word word) {
        return word.asAddress();
    }

    //@Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static long testAddressToLong(com.sun.max.unsafe.Address address) {
        return address.toLong();
    }

    //@Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static boolean testUnsignedAboveThan(long a, long b) {
        return com.oracle.max.cri.intrinsics.UnsignedMath.aboveThan(a, b);
    }

    //@Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static com.sun.max.unsafe.Address testAddressZero() {
        return com.sun.max.unsafe.Address.zero();
    }

    //@Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static com.sun.max.unsafe.Word testWordZero() {
        return com.sun.max.unsafe.Word.zero();
    }

    //@Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static com.sun.max.unsafe.Word testWordAllOnes() {
        return com.sun.max.unsafe.Word.allOnes();
    }

    //@Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static com.sun.max.unsafe.Offset testOffsetFromLong(long value) {
        return Offset.fromLong(value);
    }

    //@Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static com.sun.max.unsafe.Address testAddressFromLong(long value) {
        return Address.fromLong(value);
    }

    //@Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static com.sun.max.unsafe.Offset testOffsetFromInt(int value) {
        return Offset.fromInt(value);
    }

    //@Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static com.sun.max.unsafe.Offset testOffsetPlusOffset(com.sun.max.unsafe.Offset offset, Offset addend) {
        return offset.plus(addend);
    }

    //@Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static com.sun.max.unsafe.Offset testOffsetPlus(com.sun.max.unsafe.Offset offset, int value) {
        return offset.plus(value);
    }

    //@Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static com.sun.max.unsafe.Address testAddressPlus(com.sun.max.unsafe.Address address, int value) {
        return address.plus(value);
    }

    //@Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static Reference testRefFromJava(Object object) {
        return Reference.fromJava(object);
    }

    //@Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static Object testRefToJava(Reference ref) {
        return ref.toJava();
    }

    //@Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static Object testRefFromOrigin(Pointer origin) {
        return Reference.fromOrigin(origin).toJava();
    }

    //@Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static Pointer testTupleCellToOrigin(Pointer cell) {
        return Layout.tupleCellToOrigin(cell);
    }

    //@Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static void testWriteHubReference(Pointer origin, Reference ref) {
        Layout.writeHubReference(origin, ref);
    }

    //@Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static Reference testReadHubReference(Pointer origin) {
        return Layout.readHubReference(origin);
    }

    //@Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static Size testHubAccess(Hub hub) {
        return hub.tupleSize;
    }

    //@Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static long testTupleAccess_readLong(Object staticTuple, int offset) {
        return TupleAccess.readLong(staticTuple, offset);
    }

    //@Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static boolean testIsSubClassHub(Hub hub, ClassActor testClassActor) {
        return hub.isSubClassHub(testClassActor);
    }

    //@Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static Object testUnsafeCast(Object object) {
        return UnsafeCastNode.unsafeCast(Reference.fromJava(object), StampFactory.forNodeIntrinsic());
    }

    //@Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static Object testThrow(ClassActor testClassActor, Object object) {
        throw Throw.throwClassCastException(testClassActor, object);
    }

    //@Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static void testWrite(Pointer p, int offset, short value) {
        p.writeShort(offset, value);
    }

    //@Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static boolean testAddressEquals(Address a, int b) {
        return a.equals(b);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static Pointer testGetJniHandle(Pointer handles, int offset, Object value) {
        return (value == null) ? Pointer.zero() : handles.plus(offset);
    }

}

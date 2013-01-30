package com.oracle.max.vm.ext.graal.snippets;

import com.oracle.graal.api.code.*;
import com.oracle.graal.api.meta.*;
import com.oracle.graal.snippets.*;
import com.oracle.graal.snippets.Snippet.Parameter;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.reference.*;


public class TestSnippets extends SnippetLowerings implements SnippetsInterface {

    public TestSnippets(MetaAccessProvider runtime, Assumptions assumptions, TargetDescription target) {
        super(runtime, assumptions, target);
    }

//    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static boolean testIsHosted() {
        if (MaxineVM.isHosted()) {
            return true;
        } else {
            return false;
        }
    }

//    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static int testIsHostedArg(@Parameter("a") int a, @Parameter("b") int b) {
        if (MaxineVM.isHosted()) {
            return a;
        } else {
            return b;
        }
    }

//    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static long testWordWidth() {
        return com.sun.max.unsafe.Word.width();
    }

//    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static com.sun.max.unsafe.Address testWordAsAddress(@Parameter("word") com.sun.max.unsafe.Word word) {
        return word.asAddress();
    }

//    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static long testAddressToLong(@Parameter("address") com.sun.max.unsafe.Address address) {
        return address.toLong();
    }

//    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static boolean testUnsignedAboveThan(@Parameter("a") long a, @Parameter("b") long b) {
        return com.oracle.max.cri.intrinsics.UnsignedMath.aboveThan(a, b);
    }

//    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static com.sun.max.unsafe.Address testAddressZero() {
        return com.sun.max.unsafe.Address.zero();
    }

//    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static com.sun.max.unsafe.Word testWordZero() {
        return com.sun.max.unsafe.Word.zero();
    }

//    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static com.sun.max.unsafe.Word testWordAllOnes() {
        return com.sun.max.unsafe.Word.allOnes();
    }

//    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static com.sun.max.unsafe.Offset testOffsetFromLong(@Parameter("value") long value) {
        return Offset.fromLong(value);
    }

//    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static com.sun.max.unsafe.Offset testOffsetFromInt(@Parameter("value") int value) {
        return Offset.fromInt(value);
    }

//    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static com.sun.max.unsafe.Offset testOffsetPlusOffset(@Parameter("word") com.sun.max.unsafe.Offset offset, @Parameter("addend") Offset addend) {
        return offset.plus(addend);
    }

//    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static com.sun.max.unsafe.Offset testOffsetPlus(@Parameter("word") com.sun.max.unsafe.Offset offset, @Parameter("value") int value) {
        return offset.plus(value);
    }

//    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static com.sun.max.unsafe.Address testAddressPlus(@Parameter("word") com.sun.max.unsafe.Address address, @Parameter("value") int value) {
        return address.plus(value);
    }

//    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static Reference testRefFromJava(@Parameter("object") Object object) {
        return Reference.fromJava(object);
    }

//    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static Object testRefFromOrigin(@Parameter("origin") Pointer origin) {
        return Reference.fromOrigin(origin).toJava();
    }

//    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static Pointer testTupleCellToOrigin(@Parameter("cell") Pointer cell) {
        return Layout.tupleCellToOrigin(cell);
    }

//    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static void testWriteHubReference(@Parameter("origin") Pointer origin, @Parameter("ref") Reference ref) {
        Layout.writeHubReference(origin, ref);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    public static Size testHubAccess(@Parameter("hub") Hub hub) {
        return hub.tupleSize;
    }
}

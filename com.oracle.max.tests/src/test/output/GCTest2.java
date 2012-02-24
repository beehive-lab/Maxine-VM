/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
package test.output;

// This is adapted from a benchmark written by John Ellis and Pete Kovac
// of Post Communications.
// It was modified by Hans Boehm of Silicon Graphics.
//
// This is no substitute for real applications. No actual application
// is likely to behave in exactly this way. However, this benchmark was
// designed to be more representative of real applications than other
// Java GC benchmarks of which we are aware.
// It attempts to model those properties of allocation requests that
// are important to current GC techniques.
// It is designed to be used either to obtain a single overall performance
// number, or to give a more detailed estimate of how collector
// performance varies with object lifetimes. It prints the time
// required to allocate and collect balanced binary trees of various
// sizes. Smaller trees result in shorter object lifetimes. Each cycle
// allocates roughly the same amount of memory.
// Two data structures are kept around during the entire process, so
// that the measured performance is representative of applications
// that maintain some live in-memory data. One of these is a tree
// containing many pointers. The other is a large array containing
// double precision floating point numbers. Both should be of comparable
// size.
//
// The results are only really meaningful together with a specification
// of how much memory was used. It is possible to trade memory for
// better time performance. This benchmark should be run in a 32 MB
// heap, though we don't currently know how to enforce that uniformly.
//
// Unlike the original Ellis and Kovac benchmark, we do not attempt
// measure pause times. This facility should eventually be added back
// in. There are several reasons for omitting it for now. The original
// implementation depended on assumptions about the thread scheduler
// that don't hold uniformly. The results really measure both the
// scheduler and GC. Pause time measurements tend to not fit well with
// current benchmark suites. As far as we know, none of the current
// commercial Java implementations seriously attempt to minimize GC pause
// times.
//
// Known deficiencies:
// - No way to check on memory use
// - No cyclic data structures
// - No attempt to measure variation with object size
// - Results are sensitive to locking cost, but we dont
// check for proper locking

class Node {
    Node left;
    Node right;
    int i;
    int j;

    Node(Node l, Node r) {
        this.left = l;
        this.right = r;
    }

    Node() {
    }
}

public class GCTest2 {

    public static final int kStretchTreeDepth = 18; // about 16Mb
    public static final int kLongLivedTreeDepth = 16; // about 4Mb
    public static final int kArraySize = 500000; // about 4Mb
    public static final int kMinTreeDepth = 4;
    public static final int kMaxTreeDepth = 16;

// Nodes used by a tree of a given size
    static int treeSize(int i) {
        return 1 << (i + 1) - 1;
    }

// Number of iterations to use for a given tree depth
    static int numIters(int i) {
        return 2 * treeSize(kStretchTreeDepth) / treeSize(i);
    }

// Build tree top down, assigning to older objects.
    static void populate(int iDepth, Node thisNode) {
        int depth = iDepth;
        if (depth <= 0) {
            return;
        }

        depth--;
        thisNode.left = new Node();
        thisNode.right = new Node();
        populate(depth, thisNode.left);
        populate(depth, thisNode.right);
    }

// Build tree bottom-up
    static Node makeTree(int iDepth) {
        if (iDepth <= 0) {
            return new Node();
        }
        return new Node(makeTree(iDepth - 1), makeTree(iDepth - 1));

    }

    static void printDiagnostics() {
        final long lFreeMemory = Runtime.getRuntime().freeMemory();
        final long lTotalMemory = Runtime.getRuntime().totalMemory();
        System.out.print(" Total memory available=" + lTotalMemory + " bytes");
        System.out.println("  Free memory=" + lFreeMemory + " bytes");
    }

    static void timeConstruction(int depth) {
        // Node root;
        long tStart;
        long tFinish;
        final int iNumIters = numIters(depth);
        Node tempTree;

        System.out.println("Creating " + iNumIters + " trees of depth " + depth);
        tStart = System.currentTimeMillis();
        for (int i = 0; i < iNumIters; ++i) {
            tempTree = new Node();
            populate(depth, tempTree);
            tempTree = null;
        }
        tFinish = System.currentTimeMillis();
        if (omitTimes) {
            System.out.println("\tTop down construction completed");
        } else {
            System.out.println("\tTop down construction took " + (tFinish - tStart) + "msecs");
        }
        tStart = System.currentTimeMillis();
        for (int i = 0; i < iNumIters; ++i) {
            tempTree = makeTree(depth);
            tempTree = null;
        }
        tFinish = System.currentTimeMillis();
        if (omitTimes) {
            System.out.println("\tBottom up construction completed");
        } else {
            System.out.println("\tBottom up construction took " + (tFinish - tStart) + "msecs");
        }
    }

    public static void run() {
        main(new String[] {});
    }

    private static boolean omitTimes = true;
    public static void main(String[] args) {
        if (args.length > 0) {
            omitTimes = false;
        }
        // Node root;
        Node longLivedTree;
        @SuppressWarnings("unused")
        Node tempTree;

        System.out.println("Garbage Collector Test");
        System.out.println(" Stretching memory with a binary tree of depth " + kStretchTreeDepth);

        // Stretch the memory space quickly
        tempTree = makeTree(kStretchTreeDepth);
        tempTree = null;

        // Create a long lived object
        System.out.println(" Creating a long-lived binary tree of depth " + kLongLivedTreeDepth);
        longLivedTree = new Node();
        populate(kLongLivedTreeDepth, longLivedTree);

        // Create long-lived array, filling half of it
        System.out.println(" Creating a long-lived array of " + kArraySize + " doubles");
        final double[]  array = new double[kArraySize];
        for (int i = 0; i < kArraySize / 2; ++i) {
            array[i] = 1.0 / i;
        }

        for (int d = kMinTreeDepth; d <= kMaxTreeDepth; d += 2) {
            timeConstruction(d);
        }

        if (longLivedTree == null || array[1000] != 1.0 / 1000) {
            System.out.println("Failed");
        }
        // fake reference to LongLivedTree
        // and array
        // to keep them from being optimized away

        System.out.println(GCTest2.class.getSimpleName() + " done.");
    }
} // class JavaGC

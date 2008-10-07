/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package util;

import java.util.*;

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

class Node2 {

    Node2 _left;
    Node2 _right;
    int _i;
    int _j;

    Node2(Node2 l, Node2 r) {
        _left = l;
        _right = r;
    }

    Node2() {
    }
}

public class GCBench1 {

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
    static void populate(int iDepth, Node2 thisNode) {
        int depth = iDepth;
        if (depth <= 0) {
            return;
        }

        depth--;
        thisNode._left = new Node2();
        thisNode._right = new Node2();
        populate(depth, thisNode._left);
        populate(depth, thisNode._right);
    }

// Build tree bottom-up
    static Node2 makeTree(int iDepth) {
        if (iDepth <= 0) {
            return new Node2();
        }
        return new Node2(makeTree(iDepth - 1), makeTree(iDepth - 1));

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
        Node2 tempTree;

        System.out.println("Creating " + iNumIters + " trees of depth " + depth);
        tStart = System.currentTimeMillis();
        for (int i = 0; i < iNumIters; ++i) {
            tempTree = new Node2();
            populate(depth, tempTree);
            tempTree = null;
        }
        tFinish = System.currentTimeMillis();
        if (_omitTimes) {
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
        if (_omitTimes) {
            System.out.println("\tBottom up construction completed");
        } else {
            System.out.println("\tBottom up construction took " + (tFinish - tStart) + "msecs");
        }
    }

    public static void run() {
        main(new String[] {});
    }

    private static boolean _omitTimes = true;
    public static void main(String[] args) {
        if (args.length > 0) {
            _omitTimes = false;
        }
        // Node root;
        final List<Node2> longLivedTrees = new ArrayList<Node2>();
        Node2 longLivedTree;
        @SuppressWarnings("unused")
        Node2 tempTree;
        int i = 0;
        System.out.println("Garbage Collector Test");
        while (i < 100) {
            System.out.println(" Stretching memory with a binary tree of depth " + kStretchTreeDepth);

            // Stretch the memory space quickly
            tempTree = makeTree(kStretchTreeDepth);
            tempTree = null;

            // Create a long lived object
            System.out.println(" Creating a long-lived binary tree of depth " + kLongLivedTreeDepth);
            longLivedTree = new Node2();
            populate(kLongLivedTreeDepth, longLivedTree);
            longLivedTrees.add(longLivedTree);
            i++;
        }
    }
} // class JavaGC

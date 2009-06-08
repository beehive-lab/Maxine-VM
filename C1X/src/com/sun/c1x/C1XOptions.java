/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.c1x;

/**
 * The <code>C1XOptions</code> class encapsulates options that control the behavior of the
 * C1X compiler.
 *
 * @author Ben L. Titzer
 */
public class C1XOptions {
    public static int MaximumInstructionCount = 37000;
    public static float MaximumInlineRatio = 0.90f;
    public static int MaximumInlineSize = 35;
    public static int MaximumTrivialSize = 6;
    public static int MaximumInlineLevel = 9;
    public static int MaximumRecursiveInlineLevel = 1;
    public static int MaximumDesiredSize = 8000;
    public static int SSEVersion = 2;

    public static boolean MergeEquivalentConstants = false;
    public static boolean PinAllInstructions = false;
    public static boolean InlineMethodsWithExceptionHandlers = false;
    public static boolean InlineSynchronizedMethods = false;
    public static boolean TestPatching = false;
    public static boolean PrintInitialBlockList = false;
    public static boolean RoundFPResults = false;
    public static boolean EliminateNarrowingInStores = false;
    public static boolean EliminateFieldAccess = false;
    public static boolean CanonicalizeInstructions = false;
    public static boolean SupportObjectConstants = false;
    public static boolean OptimizeUnsafes = false;
    public static boolean ReduceMultipliesToShifts = false;
    public static boolean UseLocalValueNumbering = false;
    public static boolean CSEArrayLength = false;
    public static boolean ProfileBranches = false;
    public static boolean ProfileCalls = false;
    public static boolean ProfileCheckcasts = false;
    public static boolean ProfileInlinedCalls = false;
    public static boolean UseCHA = false;
    public static boolean UseSlowPath = true;
    public static boolean UseDeopt = false;
    public static boolean UseCHALeafMethods = false;
    public static boolean UseInlineCaches = false;
    public static boolean CanonicalizeConstantFields = false;
    public static boolean RegisterFinalizersAtInit = true;
    public static boolean FoldIntrinsics = false;
    public static boolean FoldFloatingPoint = false;
    public static boolean ExtraPhiChecking = true;
    public static boolean ComputeStoresInLoops = false;
}

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

    private static final boolean ____ = false;
    private static final boolean TRUE = true;

    // inlining settings
    public static boolean InlineMethods                      = ____;
    public static boolean InlineIntrinsics                   = ____;
    public static boolean InlineMethodsWithExceptionHandlers = ____;
    public static boolean InlineSynchronizedMethods          = ____;
    public static int     MaximumInstructionCount            = 37000;
    public static float   MaximumInlineRatio                 = 0.90f;
    public static int     MaximumInlineSize                  = 35;
    public static int     MaximumTrivialSize                 = 6;
    public static int     MaximumInlineLevel                 = 9;
    public static int     MaximumRecursiveInlineLevel        = 1;
    public static int     MaximumDesiredSize                 = 8000;

    // floating point settings
    public static int SSEVersion                             = 2;
    public static boolean RoundFPResults                     = ____;

    // debugging settings
    public static boolean PinAllInstructions                 = ____;
    public static boolean TestPatching                       = ____;
    public static boolean TestSlowPath                       = TRUE;
    public static boolean PrintInitialBlockList              = ____;

    // canonicalizer settings
    public static boolean CanonicalizeInstructions           = TRUE;
    public static boolean CanonicalizeIntrinsics             = TRUE;
    public static boolean CanonicalizeFloatingPoint          = TRUE;
    public static boolean CanonicalizeNarrowingInStores      = TRUE;
    public static boolean CanonicalizeConstantFields         = TRUE;
    public static boolean CanonicalizeUnsafes                = TRUE;
    public static boolean CanonicalizeMultipliesToShifts     = TRUE;
    public static boolean CanonicalizeObjectCheckCast        = TRUE;
    public static boolean CanonicalizeObjectInstanceOf       = TRUE;

    // local value numbering / load elimination settings
    public static boolean UseLocalValueNumbering             = ____;
    public static boolean EliminateFieldAccess               = TRUE;
    public static boolean AlwaysCSEArrayLength               = ____;

    // profiling settings
    public static boolean ProfileBranches                    = ____;
    public static boolean ProfileCalls                       = ____;
    public static boolean ProfileCheckcasts                  = ____;
    public static boolean ProfileInlinedCalls                = ____;

    // optimistic optimization settings
    public static boolean UseCHA                             = ____;
    public static boolean UseDeopt                           = ____;
    public static boolean UseCHALeafMethods                  = ____;

    // state merging settings
    public static boolean MergeEquivalentConstants           = ____;
    public static boolean ComputeStoresInLoops               = TRUE;
    public static boolean AssumeVerifiedBytecode             = ____;
    public static boolean ExtraPhiChecking                   = TRUE;

    // miscellaneous settings
    public static boolean SupportObjectConstants             = TRUE;
    public static boolean UseInlineCaches                    = ____;
    public static boolean RegisterFinalizersAtInit           = TRUE;

    // future settings
    public static boolean DoGlobalValueNumbering             = ____;
    public static boolean DoArrayBoundsCheckElimination      = ____;
    public static boolean DistinguishExceptionHandlerCode    = ____;
    public static boolean DoNullCheckElimination             = ____;
    public static boolean DoProfileGuidedInlining            = ____;
    public static boolean DoTypeFlowAnalysis                 = ____;
    public static int     ReOptUnresolvedCount               = 4;
    public static boolean DetectCascadingInstanceOf          = ____;
    public static float   MonomorphicProfileRatio            = 0.85f;
    public static float   BimorphicProfileRatio              = 0.90f;
    public static int     MaximumTypeSwitchInlining          = 10;
}

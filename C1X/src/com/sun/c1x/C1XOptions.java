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

    // Checkstyle: stop
    private static final boolean ____ = false;
    // Checkstyle: resume

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
    public static int     SSEVersion                         = 2;
    public static boolean RoundFPResults                     = ____;

    public static boolean PinAllInstructions                 = ____;
    public static boolean TestPatching                       = ____;
    public static boolean TestSlowPath                       = ____;
    public static boolean PrintInitialBlockList              = ____;
    public static boolean PrintIR                            = ____;
    public static boolean PrintCFGToFile                     = ____;
    public static boolean PrintMetrics                       = ____;
    public static boolean PrintCFG                           = ____;
    public static boolean PrintCompilation                   = ____;
    public static boolean PrintExceptionHandlers             = ____;
    public static boolean PrintNotLoaded                     = ____;
    public static boolean IRChecking                         = ____;
    public static boolean FatalUnimplemented                 = ____;
    public static boolean InterpretInvokedMethods            = ____;

    // canonicalizer settings
    public static boolean CanonicalizeInstructions           = true;
    public static boolean CanonicalizeIntrinsics             = true;
    public static boolean CanonicalizeFloatingPoint          = true;
    public static boolean CanonicalizeNarrowingInStores      = true;
    public static boolean CanonicalizeConstantFields         = true;
    public static boolean CanonicalizeUnsafes                = true;
    public static boolean CanonicalizeMultipliesToShifts     = true;
    public static boolean CanonicalizeObjectCheckCast        = true;
    public static boolean CanonicalizeObjectInstanceOf       = true;
    public static boolean CanonicalizeFoldableMethods        = ____;

    // local value numbering / load elimination settings
    public static boolean UseLocalValueNumbering             = ____;
    public static boolean EliminateFieldAccess               = ____;
    public static boolean AlwaysCSEArrayLength               = ____;

    // profiling settings
    public static boolean Profile                            = ____;
    public static boolean ProfileBranches                    = ____;
    public static boolean ProfileCalls                       = ____;
    public static boolean ProfileCheckcasts                  = ____;
    public static boolean ProfileInlinedCalls                = ____;
    public static boolean OptimizeVirtualCallProfiling       = ____;
    public static boolean ProfileVirtualCalls                = ____;
    public static int     ProfileTypeWidth                   = 0;

    // optimistic optimization settings
    public static boolean UseCHA                             = ____;
    public static boolean UseDeopt                           = ____;
    public static boolean UseCHALeafMethods                  = ____;
    public static boolean AggressivelyResolveCPEs            = true;

    // state merging settings
    public static boolean MergeEquivalentConstants           = ____;
    public static boolean ComputeStoresInLoops               = true;
    public static boolean AssumeVerifiedBytecode             = ____;
    public static boolean ExtraPhiChecking                   = true;
    public static boolean SimplifyPhis                       = true;

    // miscellaneous settings
    public static boolean SupportObjectConstants             = true;
    public static boolean SupportWordTypes                   = ____;
    public static boolean UseInlineCaches                    = ____;
    public static boolean RegisterFinalizersAtInit           = true;

    // global optimization settings
    public static boolean DoGlobalValueNumbering             = ____;
    public static boolean DoCEElimination                    = ____;
    public static boolean DoBlockMerging                     = ____;
    public static boolean DoBlockSkipping                    = ____;
    public static boolean DoNullCheckElimination             = ____;
    public static boolean DoIterativeNCE                     = ____;
    public static boolean DoFlowSensitiveNCE                 = ____;
    public static boolean DoDeadCodeElimination1             = ____;
    public static boolean DoDeadCodeElimination2             = ____;

    // backend optimization settings
    public static boolean OptimizeControlFlow                = ____;
    public static boolean OptimizeMoves                      = ____;

    // future settings
    public static boolean DoArrayBoundsCheckElimination      = ____;
    public static boolean DistinguishExceptionHandlerCode    = ____;
    public static boolean DoProfileGuidedInlining            = ____;
    public static boolean DoTypeFlowAnalysis                 = ____;
    public static boolean DetectCascadingInstanceOf          = ____;
    public static float   MonomorphicProfileRatio            = 0.85f;
    public static float   BimorphicProfileRatio              = 0.90f;
    public static int     MaximumTypeSwitchInlining          = 10;

    // Linear scan settings
    public static boolean StressLinearScan                   = ____;
    public static int     TraceLinearScanLevel               = 0;

    // LIR settings
    public static boolean GenerateLIR                        = true;
    public static boolean PrintIRWithLIR                     = ____;
    public static boolean LIRFillDelaySlots                  = ____;
    public static boolean LIRTraceExecution                  = ____;
    public static boolean TwoOperandLIRForm                  = true; // This flag is false for SPARC => probably move it to target
    public static boolean GenerateSynchronizationCode        = true;
    public static boolean GenerateArrayStoreCheck            = true;
    public static boolean GenerateBoundsChecks               = true;
    public static boolean GenerateCompilerNullChecks         = true;

    public static boolean UseTableRanges                     = ____;
    public static boolean DetailedAsserts                    = ____;
    public static boolean FastPathTypeCheck                  = ____;

    public static boolean PrintLIR                           = ____;
    public static boolean Verbose                            = ____;
    public static boolean LIRTracePeephole                   = ____;

    public static boolean TestCompressedStreamEnabled        = ____;

    // Runtime settings
    public static boolean UseBiasedLocking                   = ____;
    public static boolean UseImplicitDiv0Checks              = ____;
    public static boolean UseTLAB                            = ____;
    public static int     ReadPrefetchInstr                  = 0;
    public static int     AllocatePrefetchInstr              = 0;
    public static boolean UseFastLocking                     = ____;
    public static boolean UseFastNewInstance                 = ____;
    public static boolean UseSlowPath                        = ____;
    public static boolean UseFastNewObjectArray              = ____;
    public static boolean UseFastNewTypeArray                = ____;
    public static boolean UseStackBanging                    = true;
    public static int     StackShadowPages                   = 3;

    // Assembler settings
    public static boolean GenerateAssembly                   = true;
    public static boolean CommentedAssembly                  = ____;
    public static boolean PrintLIRWithAssembly               = ____;
    public static boolean VerifyOopMaps                      = ____;
    public static boolean VerifyOops                         = ____;
    public static int     Atomics                            = 0;
    public static boolean UseNormalNop                       = true;
    public static boolean UseAddressNop                      = true;
    public static boolean UseIncDec                          = ____;
    public static boolean UseXmmLoadAndClearUpper            = ____;
    public static boolean UseXmmRegToRegMoveAll              = ____;
    public static boolean CMSIncrementalMode                 = ____;
    public static boolean GenerateAssertionCode              = ____;
    public static boolean EmitStaticCallStubs                = ____;
    public static boolean TraceRelocation                    = ____;
    public static boolean PrintAssembly                      = ____;
    public static int     BytesPerLine                       = 16;
    public static boolean TraceLIRVisit                      = ____;

    public static void setOptimizationLevel(int level) {
        if (level <= 0) {
            setOptimizationLevel0();
        } else if (level == 1) {
            setOptimizationLevel1();
        } else if (level == 2) {
            setOptimizationLevel2();
        } else {
            setOptimizationLevel3();
        }
    }

    private static void setOptimizationLevel0() {
        // turn off all optimizations
        InlineMethods                      = ____;
        CanonicalizeInstructions           = ____;
        UseLocalValueNumbering             = ____;
        EliminateFieldAccess               = ____;
        AlwaysCSEArrayLength               = ____;

        MergeEquivalentConstants           = ____; // won't work until constants can be outside CFG
        ComputeStoresInLoops               = ____;
        SimplifyPhis                       = ____;

        // turn off backend optimizations
        OptimizeControlFlow                = ____;
        OptimizeMoves                      = ____;

        DoGlobalValueNumbering             = ____;
        DoCEElimination                    = ____;
        DoBlockMerging                     = ____;
        DoArrayBoundsCheckElimination      = ____;
        DistinguishExceptionHandlerCode    = ____;
        DoNullCheckElimination             = ____;
        DoDeadCodeElimination1             = ____;
        DoDeadCodeElimination2             = ____;
    }

    private static void setOptimizationLevel1() {
        // turn on basic inlining and local optimizations
        InlineMethods                      = ____; // inlining heuristics may need to be adjusted
        CanonicalizeInstructions           = true;
        UseLocalValueNumbering             = true;
        EliminateFieldAccess               = true;
        AlwaysCSEArrayLength               = ____;

        // turn on state merging optimizations
        MergeEquivalentConstants           = ____; // won't work until constants can be outside CFG
        ComputeStoresInLoops               = true;
        SimplifyPhis                       = true;

        // turn on speculative optimizations
        UseCHA                             = ____;
        UseDeopt                           = ____;
        UseCHALeafMethods                  = ____;

        // turn on backend optimizations
        OptimizeControlFlow                = ____;
        OptimizeMoves                      = ____;

        // turn off global optimizations, except null check elimination
        DoGlobalValueNumbering             = ____;
        DoCEElimination                    = ____;
        DoBlockMerging                     = ____;
        DoArrayBoundsCheckElimination      = ____;
        DistinguishExceptionHandlerCode    = ____;
        DoNullCheckElimination             = true;
        DoIterativeNCE                     = ____; // don't iterate NCE
        DoFlowSensitiveNCE                 = ____;
        DoDeadCodeElimination1             = ____;
        DoDeadCodeElimination2             = ____;
    }

    private static void setOptimizationLevel2() {
        // turn on basic inlining and local optimizations
        InlineMethods                      = true; // inlining heuristics may need to be adjusted
        CanonicalizeInstructions           = true;
        UseLocalValueNumbering             = true;
        EliminateFieldAccess               = true;
        AlwaysCSEArrayLength               = ____;

        // turn on state merging optimizations
        MergeEquivalentConstants           = ____; // won't work until constants can be outside CFG
        ComputeStoresInLoops               = true;
        SimplifyPhis                       = true;

        // turn on speculative optimizations
        UseCHA                             = true;
        UseDeopt                           = true;
        UseCHALeafMethods                  = true;

        // turn on backend optimizations
        OptimizeControlFlow                = true;
        OptimizeMoves                      = true;

        // turn off global optimizations, except null check elimination
        DoGlobalValueNumbering             = ____;
        DoCEElimination                    = ____;
        DoBlockMerging                     = true;
        DoArrayBoundsCheckElimination      = ____;
        DistinguishExceptionHandlerCode    = ____;
        DoNullCheckElimination             = true;
        DoIterativeNCE                     = ____; // don't iterate NCE
        DoFlowSensitiveNCE                 = ____;
        DoDeadCodeElimination1             = ____;
        DoDeadCodeElimination2             = ____;
    }

    private static void setOptimizationLevel3() {
        // turn on basic inlining and local optimizations
        InlineMethods                      = true;
        CanonicalizeInstructions           = true;
        UseLocalValueNumbering             = true;
        EliminateFieldAccess               = true;
        AlwaysCSEArrayLength               = true;

        // turn on state merging optimizations
        MergeEquivalentConstants           = ____; // won't work until constants can be outside CFG
        ComputeStoresInLoops               = true;
        SimplifyPhis                       = true;

        // turn on speculative optimizations
        UseCHA                             = true;
        UseDeopt                           = true;
        UseCHALeafMethods                  = true;

        // turn on backend optimizations
        OptimizeControlFlow                = true;
        OptimizeMoves                      = true;

        // turn on global optimizations
        DoGlobalValueNumbering             = true;
        DoCEElimination                    = true;
        DoBlockMerging                     = true;
        DoBlockSkipping                    = true;
        DoArrayBoundsCheckElimination      = true;
        DistinguishExceptionHandlerCode    = true;
        DoNullCheckElimination             = true;
        DoIterativeNCE                     = true;
        DoFlowSensitiveNCE                 = true;
        DoDeadCodeElimination1             = true;
        DoDeadCodeElimination2             = true;
    }
}

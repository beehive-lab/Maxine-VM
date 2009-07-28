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
    private static final boolean TRUE = true;
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

    // debugging settings
    public static boolean GatherMetrics                      = TRUE;
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
    public static boolean TypeChecking                       = ____;
    public static boolean FatalUnimplemented                 = ____;

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
    public static boolean TestCompressedStreamEnabled        = ____;

    // optimistic optimization settings
    public static boolean UseCHA                             = ____;
    public static boolean UseDeopt                           = ____;
    public static boolean UseCHALeafMethods                  = ____;
    public static boolean AggressivelyResolveCPEs            = TRUE;

    // state merging settings
    public static boolean MergeEquivalentConstants           = ____;
    public static boolean ComputeStoresInLoops               = TRUE;
    public static boolean AssumeVerifiedBytecode             = ____;
    public static boolean ExtraPhiChecking                   = TRUE;
    public static boolean SimplifyPhis                       = TRUE;

    // miscellaneous settings
    public static boolean SupportObjectConstants             = TRUE;
    public static boolean SupportWordTypes                   = ____;
    public static boolean UseInlineCaches                    = ____;
    public static boolean RegisterFinalizersAtInit           = TRUE;

    // global optimization settings
    public static boolean DoGlobalValueNumbering             = ____;
    public static int     MaximumGVNLoopSize                 = 8;
    public static boolean DoIterativeNullCheckElimination    = ____;

    // future settings
    public static boolean DoArrayBoundsCheckElimination      = ____;
    public static boolean DoCEElimination                    = ____;
    public static boolean DoBlockMerging                     = ____;
    public static boolean DistinguishExceptionHandlerCode    = ____;
    public static boolean DoNullCheckElimination             = ____;
    public static boolean DoProfileGuidedInlining            = ____;
    public static boolean DoTypeFlowAnalysis                 = ____;
    public static int     ReOptUnresolvedCount               = 4;
    public static boolean DetectCascadingInstanceOf          = ____;
    public static float   MonomorphicProfileRatio            = 0.85f;
    public static float   BimorphicProfileRatio              = 0.90f;
    public static int     MaximumTypeSwitchInlining          = 10;

    // Linear scan settings
    public static boolean StressLinearScan                   = ____;
    public static int     TraceLinearScanLevel               = 0;

    // LIR settings
    public static boolean GenerateLIR                        = TRUE;
    public static boolean PrintIRWithLIR                     = ____;
    public static boolean LIRFillDelaySlots                  = ____;
    public static boolean LIRTraceExecution                  = ____;
    public static boolean TwoOperandLIRForm                  = TRUE; // This flag is false for SPARC => probably move it to target
    public static boolean PatchALot                          = ____;
    public static boolean PrintNotLoaded                     = ____;
    public static boolean GenerateSynchronizationCode        = ____;
    public static boolean GenerateArrayStoreCheck            = TRUE;
    public static boolean GenerateBoundsChecks               = ____;
    public static boolean GenerateCompilerNullChecks         = ____;
    public static boolean UseTableRanges                     = ____;
    public static boolean DetailedAsserts                    = ____;

    public static boolean ImplicitDiv0Checks                 = ____;

    public static boolean PrintLIR                           = ____;
    public static boolean Verbose                            = ____;
    public static boolean LIRTracePeephole                   = ____;

    public static boolean AvoidUnsupported                   = TRUE; // Bails out when reaching code that is currently not supported

    // backend optimization settings
    public static boolean OptimizeControlFlow                = ____;
    public static boolean OptimizeMoves                      = ____;

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
    public static boolean UseStackBanging                    = TRUE;
    public static int     StackShadowPages                   = 3;

    // Assembler settings
    public static boolean GenerateAssembly                   = TRUE;
    public static boolean CommentedAssembly                  = ____;
    public static boolean PrintLIRWithAssembly               = ____;
    public static boolean VerifyOopMaps                      = ____;
    public static boolean VerifyOops                         = ____;
    public static int     SafepointPollOffset                = 256; // x86 only?
    public static int     Atomics                            = 0;
    public static boolean UseNormalNop                       = TRUE;
    public static boolean UseAddressNop                      = TRUE;
    public static boolean ForceUnreachable                   = ____;
    public static boolean PrintBiasedLockingStatistics       = ____;
    public static boolean UseIncDec                          = ____;
    public static boolean UseXmmLoadAndClearUpper            = ____;
    public static boolean UseXmmRegToRegMoveAll              = ____;
    public static boolean CMSIncrementalMode                 = ____;
    public static boolean TLABStats                          = ____;
    public static boolean GenerateAssertionCode              = ____;
    public static boolean EmitStaticCallStubs                = ____;
    public static boolean TraceRelocation                    = ____;

    // Profiling settings

    public static final int InvocationEntryBci = -1;

    public static void setOptimizationLevel(int level) {
        if (level <= 0) {
            setOptimizationLevel0();
        } else if (level == 1) {
            setOptimizationLevel1();
        } else {
            setOptimizationLevel2();
        }
    }

    private static void setOptimizationLevel0() {
        // turn off all optimizations
        InlineMethods                      = ____;
        CanonicalizeInstructions           = ____;
        UseLocalValueNumbering             = ____;
        EliminateFieldAccess               = ____;
        AlwaysCSEArrayLength               = ____;

        MergeEquivalentConstants           = ____;
        ComputeStoresInLoops               = ____;
        SimplifyPhis                       = ____;

        // turn off backend optimizations
        OptimizeControlFlow                = ____;
        OptimizeMoves                      = ____;

        DoGlobalValueNumbering             = ____;
        DoArrayBoundsCheckElimination      = ____;
        DistinguishExceptionHandlerCode    = ____;
        DoNullCheckElimination             = ____;
        DoProfileGuidedInlining            = ____;
        DoTypeFlowAnalysis                 = ____;
    }

    private static void setOptimizationLevel1() {
        // turn on basic inlining and local optimizations
        InlineMethods                      = TRUE; // inlining heuristics may need to be adjusted
        CanonicalizeInstructions           = TRUE;
        UseLocalValueNumbering             = TRUE;
        EliminateFieldAccess               = TRUE;
        AlwaysCSEArrayLength               = ____;

        // turn on state merging optimizations
        MergeEquivalentConstants           = ____;
        ComputeStoresInLoops               = TRUE;
        SimplifyPhis                       = TRUE;

        // turn on speculative optimizations
        UseCHA                             = TRUE;
        UseDeopt                           = TRUE;
        UseCHALeafMethods                  = TRUE;

        // turn on backend optimizations
        OptimizeControlFlow                = TRUE;
        OptimizeMoves                      = TRUE;

        // turn off global optimizations, except null check elimination
        DoGlobalValueNumbering             = ____;
        DoCEElimination                    = ____;
        DoBlockMerging                     = ____;
        DoArrayBoundsCheckElimination      = ____;
        DistinguishExceptionHandlerCode    = ____;
        DoNullCheckElimination             = TRUE;
        DoIterativeNullCheckElimination    = ____; // don't iterate NCE
        DoProfileGuidedInlining            = ____;
        DoTypeFlowAnalysis                 = ____;
    }

    private static void setOptimizationLevel2() {
        // turn on basic inlining and local optimizations
        InlineMethods                      = TRUE;
        CanonicalizeInstructions           = TRUE;
        UseLocalValueNumbering             = TRUE;
        EliminateFieldAccess               = TRUE;
        AlwaysCSEArrayLength               = TRUE;

        // turn on state merging optimizations
        MergeEquivalentConstants           = TRUE;
        ComputeStoresInLoops               = TRUE;
        SimplifyPhis                       = TRUE;

        // turn on speculative optimizations
        UseCHA                             = TRUE;
        UseDeopt                           = TRUE;
        UseCHALeafMethods                  = TRUE;

        // turn on backend optimizations
        OptimizeControlFlow                = TRUE;
        OptimizeMoves                      = TRUE;

        // turn on global optimizations
        DoGlobalValueNumbering             = TRUE;
        DoCEElimination                    = TRUE;
        DoBlockMerging                     = TRUE;
        DoArrayBoundsCheckElimination      = TRUE;
        DistinguishExceptionHandlerCode    = TRUE;
        DoNullCheckElimination             = TRUE;
        DoIterativeNullCheckElimination    = TRUE;
        DoProfileGuidedInlining            = TRUE;
        DoTypeFlowAnalysis                 = TRUE;
        DetectCascadingInstanceOf          = TRUE;
    }
}

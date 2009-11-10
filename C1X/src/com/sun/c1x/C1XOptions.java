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
 * This class encapsulates options that control the behavior of the C1X compiler.
 * The default value for each option field must be set in {@link #setDefaults()} so that
 * the options can be reverted to their defaults.
 *
 * @author Ben L. Titzer
 */
public class C1XOptions {

    // Checkstyle: stop
    private static final boolean ____ = false;
    // Checkstyle: resume

    // inlining settings
    public static boolean InlineMethods;
    public static boolean InlineMethodsWithExceptionHandlers;
    public static boolean InlineSynchronizedMethods;
    public static int     MaximumInstructionCount;
    public static float   MaximumInlineRatio;
    public static int     MaximumInlineSize;
    public static int     MaximumTrivialSize;
    public static int     MaximumInlineLevel;
    public static int     MaximumRecursiveInlineLevel;
    public static int     MaximumDesiredSize;

    // intrinsification settings
    public static boolean Intrinsify;
    public static boolean IntrinsifyObjectOps;
    public static boolean IntrinsifyClassOps;
    public static boolean IntrinsifyIntOps;
    public static boolean IntrinsifyLongOps;
    public static boolean IntrinsifyStringOps;
    public static boolean IntrinsifyArrayOps;
    public static boolean IntrinsifyReflection;
    public static boolean IntrinsifyMath;
    public static boolean IntrinsifyAtomic;
    public static boolean IntrinsifyUnsafe;

    // floating point settings
    public static int     SSEVersion;
    public static boolean RoundFPResults;

    // debugging and printing settings
    public static boolean IRChecking;
    public static boolean PinAllInstructions;
    public static boolean TestPatching;
    public static boolean TestSlowPath;
    public static boolean VerifyOopMaps;
    public static boolean VerifyOops;
    public static boolean PrintIR;
    public static boolean PrintCFGToFile;
    public static boolean PrintMetrics;
    public static boolean PrintVEEMetrics;
    public static boolean GatherStaticHIRInstructionCount;
    public static boolean PrintTimers;
    public static boolean PrintCFG;
    public static boolean PrintCompilation;
    public static boolean PrintExceptionHandlers;
    public static boolean PrintNotLoaded;
    public static boolean FatalUnimplemented;
    public static boolean InterpretInvokedMethods;
    public static boolean PrintStateInInterpreter;
    public static boolean PrintAssembly;
    public static int     PrintAssemblyBytesPerLine;
    public static int     TraceLinearScanLevel;
    public static boolean TraceRelocation;
    public static boolean TraceLIRVisit;
    public static boolean PrintLoopList;

    // canonicalizer settings
    public static boolean CanonicalizeInstructions;
    public static boolean CanonicalizeClassIsInstance;
    public static boolean CanonicalizeIfInstanceOf;
    public static boolean CanonicalizeIntrinsics;
    public static boolean CanonicalizeFloatingPoint;
    public static boolean CanonicalizeNarrowingInStores;
    public static boolean CanonicalizeConstantFields;
    public static boolean CanonicalizeUnsafes;
    public static boolean CanonicalizeMultipliesToShifts;
    public static boolean CanonicalizeObjectCheckCast;
    public static boolean CanonicalizeObjectInstanceOf;
    public static boolean CanonicalizeFoldableMethods;

    // local value numbering / load elimination settings
    public static boolean UseLocalValueNumbering;
    public static boolean EliminateFieldAccess;
    public static boolean AlwaysCSEArrayLength;

    public static boolean ProfileBranches;
    public static boolean ProfileCheckcasts;
    public static boolean ProfileInlinedCalls;

    // optimistic optimization settings
    public static boolean UseCHA;
    public static boolean UseDeopt;
    public static boolean UseCHALeafMethods;
    public static boolean AggressivelyResolveCPEs;

    // state merging settings
    public static boolean MergeEquivalentConstants;
    public static boolean ComputeStoresInLoops;
    public static boolean AssumeVerifiedBytecode;
    public static boolean ExtraPhiChecking;
    public static boolean SimplifyPhis;

    // miscellaneous settings
    public static boolean SupportObjectConstants;
    public static boolean SupportWordTypes;
    public static boolean RegisterFinalizersAtInit;

    // global optimization settings
    public static boolean DoGlobalValueNumbering;
    public static boolean DoCEElimination;
    public static boolean DoBlockMerging;
    public static boolean DoBlockSkipping;
    public static boolean DoNullCheckElimination;
    public static boolean DoIterativeNCE;
    public static boolean DoFlowSensitiveNCE;
    public static boolean DoDeadCodeElimination1;
    public static boolean DoDeadCodeElimination2;
    public static boolean DoLoopPeeling;

    // backend optimization settings
    public static boolean OptimizeControlFlow;
    public static int     ShortLoopSize;
    public static boolean OptimizeMoves;

    // Linear scan settings
    public static boolean StressLinearScan;

    // LIR settings
    public static boolean GenerateLIR;
    public static boolean PrintXirTemplates;
    public static boolean PrintIRWithLIR;
    public static boolean LIRTraceExecution;
    public static boolean TwoOperandLIRForm; // This flag is false for SPARC => probably move it to target
    public static boolean GenerateSynchronizationCode;
    public static boolean GenerateArrayStoreCheck;
    public static boolean GenerateBoundsChecks;
    public static boolean GenerateCompilerNullChecks;

    public static boolean UseTableRanges;
    public static boolean DetailedAsserts;
    public static boolean FastPathTypeCheck;

    public static boolean PrintLIR;
    public static boolean Verbose;

    // Runtime settings
    public static boolean UseXIR;
    public static boolean UseBiasedLocking;
    public static boolean UseImplicitDiv0Checks;
    public static boolean UseTLAB;
    public static int     ReadPrefetchInstr;
    public static boolean UseFastLocking;
    public static boolean UseSlowPath;
    public static boolean UseFastNewObjectArray;
    public static boolean UseFastNewTypeArray;
    public static boolean UseStackBanging;
    public static int     StackShadowPages;

    // Assembler settings
    public static boolean GenerateAssembly;
    public static boolean CommentedAssembly;
    public static boolean PrintLIRWithAssembly;
    public static int     Atomics;
    public static boolean UseNormalNop;
    public static boolean UseAddressNop;
    public static boolean UseIncDec;
    public static boolean UseXmmLoadAndClearUpper;
    public static boolean UseXmmRegToRegMoveAll;
    public static boolean GenerateAssertionCode;

    /**
     * Sets the default value for each option field declared in this class.
     */
    public static void setDefaults() {
        InlineMethods                      = ____;
        InlineMethodsWithExceptionHandlers = ____;
        InlineSynchronizedMethods          = ____;
        MaximumInstructionCount            = 37000;
        MaximumInlineRatio                 = 0.90f;
        MaximumInlineSize                  = 35;
        MaximumTrivialSize                 = 6;
        MaximumInlineLevel                 = 9;
        MaximumRecursiveInlineLevel        = 1;
        MaximumDesiredSize                 = 8000;

        // intrinsification settings
        Intrinsify                         = ____;
        IntrinsifyObjectOps                = true;
        IntrinsifyClassOps                 = true;
        IntrinsifyIntOps                   = true;
        IntrinsifyLongOps                  = true;
        IntrinsifyStringOps                = true;
        IntrinsifyArrayOps                 = true;
        IntrinsifyReflection               = true;
        IntrinsifyMath                     = true;
        IntrinsifyAtomic                   = true;
        IntrinsifyUnsafe                   = true;

        // floating point settings
        SSEVersion                         = 2;
        RoundFPResults                     = ____;

        // debugging and printing settings
        IRChecking                         = ____;
        PinAllInstructions                 = ____;
        TestPatching                       = ____;
        TestSlowPath                       = ____;
        VerifyOopMaps                      = ____;
        VerifyOops                         = ____;
        PrintIR                            = ____;
        PrintCFGToFile                     = ____;
        PrintMetrics                       = ____;
        GatherStaticHIRInstructionCount    = ____;
        PrintTimers                        = ____;
        PrintCFG                           = ____;
        PrintCompilation                   = ____;
        PrintExceptionHandlers             = ____;
        PrintNotLoaded                     = ____;
        FatalUnimplemented                 = ____;
        InterpretInvokedMethods            = ____;
        PrintStateInInterpreter            = ____;
        PrintAssembly                      = ____;
        PrintAssemblyBytesPerLine          = 16;
        TraceLinearScanLevel               = 0;
        TraceRelocation                    = ____;
        TraceLIRVisit                      = ____;
        PrintLoopList                      = ____;

        // canonicalizer settings
        CanonicalizeInstructions           = true;
        CanonicalizeClassIsInstance        = true;
        CanonicalizeIfInstanceOf           = ____;
        CanonicalizeIntrinsics             = true;
        CanonicalizeFloatingPoint          = true;
        CanonicalizeNarrowingInStores      = true;
        CanonicalizeConstantFields         = true;
        CanonicalizeUnsafes                = true;
        CanonicalizeMultipliesToShifts     = true;
        CanonicalizeObjectCheckCast        = true;
        CanonicalizeObjectInstanceOf       = true;
        CanonicalizeFoldableMethods        = true;

        // local value numbering / load elimination settings
        UseLocalValueNumbering             = ____;
        EliminateFieldAccess               = ____;
        AlwaysCSEArrayLength               = ____;

        ProfileBranches                    = ____;
        ProfileCheckcasts                  = ____;
        ProfileInlinedCalls                = ____;

        // optimistic optimization settings
        UseCHA                             = ____;
        UseDeopt                           = ____;
        UseCHALeafMethods                  = ____;
        AggressivelyResolveCPEs            = true;

        // state merging settings
        MergeEquivalentConstants           = ____;
        ComputeStoresInLoops               = true;
        AssumeVerifiedBytecode             = ____;
        ExtraPhiChecking                   = true;
        SimplifyPhis                       = true;

        // miscellaneous settings
        SupportObjectConstants             = true;
        SupportWordTypes                   = ____;
        RegisterFinalizersAtInit           = true;

        // global optimization settings
        DoGlobalValueNumbering             = ____;
        DoCEElimination                    = ____;
        DoBlockMerging                     = ____;
        DoBlockSkipping                    = ____;
        DoNullCheckElimination             = ____;
        DoIterativeNCE                     = ____;
        DoFlowSensitiveNCE                 = ____;
        DoDeadCodeElimination1             = ____;
        DoDeadCodeElimination2             = ____;
        DoLoopPeeling                      = ____;

        // backend optimization settings
        OptimizeControlFlow                = ____;
        ShortLoopSize                      = 5;
        OptimizeMoves                      = ____;

        // Linear scan settings
        StressLinearScan                   = ____;

        // LIR settings
        GenerateLIR                        = true;
        UseXIR = ____;
        PrintXirTemplates                  = ____;
        PrintIRWithLIR                     = ____;
        LIRTraceExecution                  = ____;
        TwoOperandLIRForm                  = true; // This flag is false for SPARC => probably move it to target
        GenerateSynchronizationCode        = true;
        GenerateArrayStoreCheck            = true;
        GenerateBoundsChecks               = true;
        GenerateCompilerNullChecks         = true;

        UseTableRanges                     = ____;
        DetailedAsserts                    = ____;
        FastPathTypeCheck                  = ____;

        PrintLIR                           = ____;
        Verbose                            = ____;

        // Runtime settings
        UseBiasedLocking                   = ____;
        UseImplicitDiv0Checks              = ____;
        UseTLAB                            = ____;
        ReadPrefetchInstr                  = 0;
        UseFastLocking                     = ____;
        UseSlowPath                        = ____;
        UseFastNewObjectArray              = ____;
        UseFastNewTypeArray                = ____;
        UseStackBanging                    = true;
        StackShadowPages                   = 3;

        // Assembler settings
        GenerateAssembly                   = true;
        CommentedAssembly                  = ____;
        PrintLIRWithAssembly               = ____;
        Atomics                            = 0;
        UseNormalNop                       = true;
        UseAddressNop                      = true;
        UseIncDec                          = ____;
        UseXmmLoadAndClearUpper            = ____;
        UseXmmRegToRegMoveAll              = ____;
        GenerateAssertionCode              = ____;

    }

    static {
        setDefaults();
    }

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
        SimplifyPhis                       = true;

        // turn off backend optimizations
        OptimizeControlFlow                = ____;
        OptimizeMoves                      = ____;

        DoGlobalValueNumbering             = ____;
        DoCEElimination                    = ____;
        DoBlockMerging                     = ____;
        DoNullCheckElimination             = ____;
        DoDeadCodeElimination1             = ____;
        DoDeadCodeElimination2             = ____;
        DoLoopPeeling                      = ____;
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
        OptimizeControlFlow                = true;
        OptimizeMoves                      = true;

        // turn off global optimizations, except null check elimination
        DoGlobalValueNumbering             = ____;
        DoCEElimination                    = ____;
        DoBlockMerging                     = ____;
        DoNullCheckElimination             = true;
        DoIterativeNCE                     = ____; // don't iterate NCE
        DoFlowSensitiveNCE                 = ____;
        DoDeadCodeElimination1             = ____;
        DoDeadCodeElimination2             = ____;
        DoLoopPeeling                      = ____;
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
        DoNullCheckElimination             = true;
        DoIterativeNCE                     = ____; // don't iterate NCE
        DoFlowSensitiveNCE                 = ____;
        DoDeadCodeElimination1             = ____;
        DoDeadCodeElimination2             = ____;
        DoLoopPeeling                      = ____; // still need to insert Phi instructions at merge blocks
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
        DoNullCheckElimination             = true;
        DoIterativeNCE                     = true;
        DoFlowSensitiveNCE                 = true;
        DoDeadCodeElimination1             = true;
        DoDeadCodeElimination2             = true;
        DoLoopPeeling                      = ____; // still need to insert Phi instructions at merge blocks
    }
}

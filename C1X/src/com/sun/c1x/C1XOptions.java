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
 *
 * @author Ben L. Titzer
 */
public class C1XOptions {

    // Checkstyle: stop
    private static final boolean ____ = false;
    // Checkstyle: resume

    // inlining settings
    public static boolean OptInline                          = ____;
    public static boolean OptInlineExcept                    = ____;
    public static boolean OptInlineSynchronized              = ____;

    public static int     MaximumInstructionCount            = 37000;
    public static float   MaximumInlineRatio                 = 0.90f;
    public static int     MaximumInlineSize                  = 35;
    public static int     MaximumTrivialSize                 = 6;
    public static int     MaximumInlineLevel                 = 9;
    public static int     MaximumRecursiveInlineLevel        = 1;
    public static int     MaximumDesiredSize                 = 8000;
    public static int     MaximumShortLoopSize               = 5;

    // intrinsification settings
    public static boolean OptIntrinsify                      = ____;
    public static boolean IntrinsifyObjectOps                = true;
    public static boolean IntrinsifyClassOps                 = true;
    public static boolean IntrinsifyIntOps                   = true;
    public static boolean IntrinsifyLongOps                  = true;
    public static boolean IntrinsifyStringOps                = true;
    public static boolean IntrinsifyArrayOps                 = true;
    public static boolean IntrinsifyReflection               = true;
    public static boolean IntrinsifyMath                     = true;
    public static boolean IntrinsifyAtomic                   = true;
    public static boolean IntrinsifyUnsafe                   = true;

    // floating point settings
    public static int     SSEVersion                         = 2;
    public static boolean RoundFPResults                     = ____;

    // debugging and printing settings
    public static boolean IRChecking                         = ____;
    public static boolean PinAllInstructions                 = ____;
    public static boolean TestPatching                       = ____;
    public static boolean TestSlowPath                       = ____;
    public static boolean VerifyOopMaps                      = ____;
    public static boolean VerifyOops                         = ____;
    public static boolean PrintHIR                           = ____;
    public static boolean PrintLIR                           = ____;
    public static boolean PrintCFGToFile                     = ____;
    public static boolean PrintMetrics                       = ____;
    public static boolean PrintTimers                        = ____;
    public static boolean PrintCompilation                   = ____;
    public static boolean PrintExceptionHandlers             = ____;
    public static boolean PrintXirTemplates                  = ____;
    public static boolean PrintIRWithLIR                     = ____;
    public static boolean FatalUnimplemented                 = ____;
    public static boolean InterpretInvokedMethods            = ____;
    public static boolean PrintStateInInterpreter            = ____;
    public static boolean PrintAssembly                      = ____;
    public static int     PrintAssemblyBytesPerLine          = 16;
    public static int     TraceLinearScanLevel               = 0;
    public static boolean TraceRelocation                    = ____;
    public static boolean TraceLIRVisit                      = ____;
    public static int     TraceGraphBuilderLevel             = 0;
    public static boolean PrintLoopList                      = ____;

    // canonicalizer settings
    public static boolean CanonicalizeClassIsInstance        = true;
    public static boolean CanonicalizeIfInstanceOf           = ____;
    public static boolean CanonicalizeIntrinsics             = true;
    public static boolean CanonicalizeFloatingPoint          = true;
    public static boolean CanonicalizeNarrowingInStores      = true;
    public static boolean CanonicalizeConstantFields         = true;
    public static boolean CanonicalizeFinalFields            = true;
    public static boolean CanonicalizeUnsafes                = true;
    public static boolean CanonicalizeMultipliesToShifts     = true;
    public static boolean CanonicalizeObjectCheckCast        = true;
    public static boolean CanonicalizeObjectInstanceOf       = true;
    public static boolean CanonicalizeFoldableMethods        = true;
    public static boolean CanonicalizeArrayStoreChecks       = true;

    // all optimization settings
    public static boolean OptCanonicalize;
    public static boolean OptLocalValueNumbering;
    public static boolean OptLocalLoadElimination;
    public static boolean OptCSEArrayLength;
    public static boolean OptCHA;
    public static boolean OptLeafMethods;
    public static boolean OptGlobalValueNumbering;
    public static boolean OptCEElimination;
    public static boolean OptBlockMerging;
    public static boolean OptBlockSkipping;
    public static boolean OptNullCheckElimination;
    public static boolean OptIterativeNCE;
    public static boolean OptFlowSensitiveNCE;
    public static boolean OptDeadCodeElimination1;
    public static boolean OptDeadCodeElimination2;
    public static boolean OptLoopPeeling;
    public static boolean OptControlFlow;
    public static boolean OptMoveElimination;

    // optimistic optimization settings
    public static boolean UseDeopt                      = ____;
    public static boolean AggressivelyResolveCPEs       = true;

    // state merging settings
    public static boolean AssumeVerifiedBytecode        = ____;
    public static boolean PhiChecking                   = true;
    public static boolean PhiSimplify                   = true;
    public static boolean PhiLoopStores                 = true;

    // miscellaneous settings
    public static boolean SupportObjectConstants        = true;
    public static boolean SupportWordTypes              = true;

    // Linear scan settings
    public static boolean StressLinearScan              = ____;

    // Code generator settings
    public static boolean GenLIR                        = true;
    public static boolean GenCode                       = true;
    public static boolean GenDeopt                      = true;

    public static boolean GenSynchronization            = true;
    public static boolean GenArrayStoreCheck            = true;
    public static boolean GenBoundsChecks               = true;
    public static boolean GenExplicitNullChecks         = ____;
    public static boolean GenExplicitDiv0Checks         = ____;
    public static boolean GenSpecialDivChecks           = true;
    public static boolean GenStackBanging               = true;
    public static boolean GenAssertionCode              = ____;
    public static boolean GenFinalizerRegistration      = true;
    public static boolean GenTableRanges                = ____;

    public static int     InitialCodeBufferSize         = 232;
    public static boolean DetailedAsserts               = ____;

    // Runtime settings
    public static boolean UseXIR                        = ____;
    public static boolean UseBiasedLocking              = ____;
    public static boolean UseTLAB                       = ____;
    public static int     ReadPrefetchInstr             = 0;
    public static boolean UseFastLocking                = ____;
    public static boolean UseSlowPath                   = ____;
    public static boolean UseFastNewObjectArray         = ____;
    public static boolean UseFastNewTypeArray           = ____;
    public static int     StackShadowPages              = 3;

    // Assembler settings
    public static boolean CommentedAssembly             = ____;
    public static boolean PrintLIRWithAssembly          = ____;
    public static int     Atomics                       = 0;
    public static boolean UseNormalNop                  = true;
    public static boolean UseAddressNop                 = true;
    public static boolean UseIncDec                     = ____;
    public static boolean UseXmmLoadAndClearUpper       = ____;
    public static boolean UseXmmRegToRegMoveAll         = ____;

    static {
        setOptimizationLevel(1);
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
        OptInline                       = ____;
        OptCanonicalize                 = ____;
        OptLocalValueNumbering          = ____;
        OptLocalLoadElimination         = ____;
        OptCSEArrayLength               = ____;

        PhiLoopStores = ____;

        // turn off backend optimizations
        OptControlFlow                  = ____;
        OptMoveElimination              = ____;

        OptGlobalValueNumbering         = ____;
        OptCEElimination                = ____;
        OptBlockMerging                 = ____;
        OptNullCheckElimination         = ____;
        OptDeadCodeElimination1         = ____;
        OptDeadCodeElimination2         = ____;
        OptLoopPeeling                  = ____;
    }

    private static void setOptimizationLevel1() {
        // turn on basic inlining and local optimizations
        OptInline                       = ____;
        OptCanonicalize                 = true;
        OptLocalValueNumbering          = true;
        OptLocalLoadElimination         = true;
        OptCSEArrayLength               = ____;

        // turn on state merging optimizations
        PhiLoopStores = true;

        // turn on speculative optimizations
        OptCHA                          = ____;
        UseDeopt                        = ____;
        OptLeafMethods                  = ____;

        // turn on backend optimizations
        OptControlFlow                  = true;
        OptMoveElimination              = true;

        // turn off global optimizations, except null check elimination
        OptGlobalValueNumbering         = ____;
        OptCEElimination                = ____;
        OptBlockMerging                 = ____;
        OptNullCheckElimination         = true;
        OptIterativeNCE                 = ____; // don't iterate NCE
        OptFlowSensitiveNCE             = ____;
        OptDeadCodeElimination1         = ____;
        OptDeadCodeElimination2         = ____;
        OptLoopPeeling                  = ____;
    }

    private static void setOptimizationLevel2() {
        // turn on basic inlining and local optimizations
        OptInline                       = true;
        OptCanonicalize                 = true;
        OptLocalValueNumbering          = true;
        OptLocalLoadElimination         = true;
        OptCSEArrayLength               = ____;

        // turn on state merging optimizations
        PhiLoopStores = true;

        // turn on speculative optimizations
        OptCHA                          = true;
        UseDeopt                        = true;
        OptLeafMethods                  = true;

        // turn on backend optimizations
        OptControlFlow                  = true;
        OptMoveElimination              = true;

        // turn off global optimizations, except null check elimination
        OptGlobalValueNumbering         = ____;
        OptCEElimination                = ____;
        OptBlockMerging                 = true;
        OptNullCheckElimination         = true;
        OptIterativeNCE                 = ____; // don't iterate NCE
        OptFlowSensitiveNCE             = ____;
        OptDeadCodeElimination1         = ____;
        OptDeadCodeElimination2         = ____;
        OptLoopPeeling                  = ____; // still need to insert Phi instructions at merge blocks
    }

    private static void setOptimizationLevel3() {
        // turn on basic inlining and local optimizations
        OptInline                       = true;
        OptCanonicalize                 = true;
        OptLocalValueNumbering          = true;
        OptLocalLoadElimination         = true;
        OptCSEArrayLength               = true;

        // turn on state merging optimizations
        PhiLoopStores = true;

        // turn on speculative optimizations
        OptCHA                          = true;
        UseDeopt                        = true;
        OptLeafMethods                  = true;

        // turn on backend optimizations
        OptControlFlow                  = true;
        OptMoveElimination              = true;

        // turn on global optimizations
        OptGlobalValueNumbering         = true;
        OptCEElimination                = true;
        OptBlockMerging                 = true;
        OptBlockSkipping                = true;
        OptNullCheckElimination         = true;
        OptIterativeNCE                 = true;
        OptFlowSensitiveNCE             = true;
        OptDeadCodeElimination1         = true;
        OptDeadCodeElimination2         = true;
        OptLoopPeeling                  = ____; // still need to insert Phi instructions at merge blocks
    }
}

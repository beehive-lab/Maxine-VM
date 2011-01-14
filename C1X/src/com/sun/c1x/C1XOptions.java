/*
 * Copyright (c) 2009, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.c1x;

import java.util.*;

import com.sun.c1x.debug.TTY.*;

/**
 * This class encapsulates options that control the behavior of the C1X compiler.
 * The help message for each option is specified by a {@linkplain #helpMap help map}.
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

    // debugging and printing settings
    public static boolean VerifyPointerMaps                  = ____;
    public static boolean IRChecking                         = ____;
    public static boolean PinAllInstructions                 = ____;
    public static boolean TestPatching                       = ____;
    public static boolean TestSlowPath                       = ____;
    public static boolean PrintHIR                           = ____;
    public static boolean PrintInliningFailures              = ____;
    public static boolean PrintLIR                           = ____;
    public static boolean PrintCFGToFile                     = ____;
    public static boolean PrintMetrics                       = ____;
    public static boolean PrintTimers                        = ____;
    public static boolean PrintCompilation                   = ____;
    public static boolean PrintXirTemplates                  = ____;
    public static boolean PrintIRWithLIR                     = ____;
    public static boolean FatalUnimplemented                 = ____;
    public static boolean InterpretInvokedMethods            = ____;
    public static boolean PrintStateInInterpreter            = ____;
    public static boolean PrintAssembly                      = ____;
    public static boolean PrintCodeBytes                     = ____;
    public static int     PrintAssemblyBytesPerLine          = 16;
    public static int     TraceLinearScanLevel               = 0;
    public static boolean TraceRelocation                    = ____;
    public static boolean TraceLIRVisit                      = ____;
    public static boolean TraceAssembler                     = ____;
    public static int     TraceBytecodeParserLevel           = 0;
    public static boolean PrintLoopList                      = ____;
    public static int     MethodEndBreakpointGuards          = 0;
    public static boolean ZapStackOnMethodEntry              = ____;
    public static boolean PrintAssumptions                   = ____;
    public static boolean PrintInlinedIntrinsics             = ____;

    /**
     * See {@link Filter#Filter(String, Object)}.
     */
    public static String  PrintFilter                        = null;

    // canonicalizer settings
    public static boolean CanonicalizeClassIsInstance        = true;
    public static boolean CanonicalizeIfInstanceOf           = ____;
    public static boolean CanonicalizeIntrinsics             = true;
    public static boolean CanonicalizeFloatingPoint          = true;
    public static boolean CanonicalizeNarrowingInStores      = true;
    public static boolean CanonicalizeConstantFields         = true;
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
    public static boolean OptGlobalValueNumbering;
    public static boolean OptCEElimination;
    public static boolean OptBlockMerging;
    public static boolean OptBlockSkipping;
    public static boolean OptNullCheckElimination;
    public static boolean OptIterativeNCE;
    public static boolean OptFlowSensitiveNCE;
    public static boolean OptDeadCodeElimination1;
    public static boolean OptDeadCodeElimination2;
    //public static boolean OptLoopPeeling;
    public static boolean OptControlFlow;
    public static boolean OptMoveElimination;

    // optimistic optimization settings
    public static boolean UseAssumptions                = ____;
    public static boolean NormalCPEResolution           = true;

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
    public static boolean CopyPointerStackArguments     = true;

    // Code generator settings
    public static boolean GenLIR                        = true;
    public static boolean GenCode                       = true;
    public static boolean GenDeopt                      = true;

    public static boolean UseConstDirectCall            = false;

    public static boolean GenSynchronization            = true;
    public static boolean GenArrayStoreCheck            = true;
    public static boolean GenBoundsChecks               = true;
    public static boolean GenSpecialDivChecks           = ____;
    public static boolean GenStackBanging               = true;
    public static boolean GenAssertionCode              = ____;
    public static boolean GenFinalizerRegistration      = true;
    public static boolean GenTableRanges                = ____;
    public static boolean AlignCallsForPatching         = true;
    public static boolean NullCheckUniquePc             = ____;
    public static boolean invokeinterfaceTemplatePos    = ____;
    public static boolean GenerateClassResolveForStaticInvoke = ____;

    public static int     InitialCodeBufferSize         = 232;
    public static boolean DetailedAsserts               = true;

    // Runtime settings
    public static boolean UseBiasedLocking              = ____;
    public static int     ReadPrefetchInstr             = 0;
    public static boolean UseFastLocking                = ____;
    public static boolean UseSlowPath                   = ____;
    public static boolean UseFastNewObjectArray         = ____;
    public static boolean UseFastNewTypeArray           = ____;
    public static boolean UseStackMapTableLiveness      = ____;
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
        //OptLoopPeeling                  = ____;
    }

    private static void setOptimizationLevel1() {
        // turn on basic inlining and local optimizations
        OptInline                       = ____;
        OptCanonicalize                 = true;
        OptLocalValueNumbering          = true;
        OptLocalLoadElimination         = true;
        OptCSEArrayLength               = ____;
        OptIntrinsify                   = ____;

        // turn on state merging optimizations
        PhiLoopStores = true;

        // turn on speculative optimizations
        UseAssumptions                  = ____;

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
        //OptLoopPeeling                  = ____;
    }

    private static void setOptimizationLevel2() {
        // turn on basic inlining and local optimizations
        OptInline                       = true;
        OptCanonicalize                 = true;
        OptLocalValueNumbering          = true;
        OptLocalLoadElimination         = true;
        OptCSEArrayLength               = ____;
        OptIntrinsify                   = ____;

        // turn on state merging optimizations
        PhiLoopStores                   = true;

        // turn on speculative optimizations
        UseAssumptions                  = true;

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
        //OptLoopPeeling                  = ____; // still need to insert Phi instructions at merge blocks
    }

    private static void setOptimizationLevel3() {
        // turn on basic inlining and local optimizations
        OptInline                       = true;
        OptCanonicalize                 = true;
        OptLocalValueNumbering          = true;
        OptLocalLoadElimination         = true;
        OptCSEArrayLength               = true;
        OptIntrinsify                   = true;

        // turn on more aggressive inlining
        OptInlineExcept                 = true;
        OptInlineSynchronized           = true;

        // turn on state merging optimizations
        PhiLoopStores                   = true;

        UseStackMapTableLiveness        = true;

        // turn on speculative optimizations
        UseAssumptions                  = true;

        OptControlFlow                  = true;
        // turn on backend optimizations
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
        //OptLoopPeeling                  = ____; // still need to insert Phi instructions at merge blocks
    }

    /**
     * A map from option field names to some text describing the meaning and
     * usage of the corresponding option.
     */
    public static final Map<String, String> helpMap;

    static {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("PrintFilter",
                "Filter compiler tracing to methods whose fully qualified name " +
                "matches <arg>. If <arg> starts with \"~\", then <arg> (without " +
                "the \"~\") is interpreted as a regular expression. Otherwise, " +
                "<arg> is interpreted as a simple substring.");

        map.put("TraceBytecodeParserLevel",
                "Trace frontend bytecode parser at level <n> where 0 means no " +
                "tracing, 1 means instruction tracing and 2 means instruction " +
                "plus frame state tracing.");

        map.put("DetailedAsserts",
                "Turn on detailed error checking that has a noticeable performance impact.");

        map.put("NormalCPEResolution",
                "Eagerly resolve constant pool entries when the resolution can be done " +
                "without triggering class loading.");

        map.put("GenSpecialDivChecks",
                "Generate code to check for (Integer.MIN_VALUE / -1) or (Long.MIN_VALUE / -1) " +
                "instead of detecting these cases via instruction decoding in a trap handler.");

        map.put("UseStackMapTableLiveness",
                "Use liveness information derived from StackMapTable class file attribute.");

        for (String name : map.keySet()) {
            try {
                C1XOptions.class.getField(name);
            } catch (Exception e) {
                throw new InternalError("The name '" + name + "' does not denote a field in " + C1XOptions.class);
            }
        }
        helpMap = Collections.unmodifiableMap(map);
    }
}

/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.graal;

import java.lang.reflect.*;
import java.util.*;

import com.oracle.graal.options.*;
import com.oracle.max.vm.ext.graal.hosted.*;
import com.sun.max.annotate.*;
import com.sun.max.config.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.Phase;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.runtime.*;

/**
 * Adapter for Graal compiler options using standard Maxine {@link VMOptions} machinery. A set of {@link VMOption}s that
 * parallel the Graal options is automatically generated and registered with the VM. Any options that are present on the
 * command line are forwarded to the Graal options from the Maxine options.
 */
public class MaxGraalOptions {

    private static Map<VMOption, OptionValue> optionMap = new HashMap<>();

    static void initialize(Phase phase) {
        if (MaxineVM.isHosted() && phase == Phase.HOSTED_COMPILING) {
            checkandSetOptions();
        } else if (phase == Phase.RUNNING) {
            checkandSetOptions();
        }
    }

    /**
     * Checks whether the option {@code name} was set and returns the {@link VMOption} if so, else {@code null}.
     * N.B. This <b>does</b> not consult or set the associated Graal option.
     * @param optionName
     * @return
     */
    public static VMOption isPresent(String optionName) {
        VMOption vmOption = getVMOption(optionName);
        assert vmOption != null;
        return vmOption.isPresent() ? vmOption : null;
    }

    /**
     * Checks the Maxine proxy options and forwards those that were set to the associated Graal options.
     */
    static void checkandSetOptions() {
        for (Map.Entry<VMOption, OptionValue> entry : optionMap.entrySet()) {
            VMOption vmOption = entry.getKey();
            if (vmOption.isPresent()) {
                Class<? extends VMOption> optionClass = vmOption.getClass();
                OptionValue entryValue = entry.getValue();
                if (optionClass == VMStringOption.class) {
                    entryValue.setValue(((VMStringOption) vmOption).getValue());
                } else if (optionClass == VMBooleanOption.class) {
                    entryValue.setValue(((VMBooleanOption) vmOption).getValue());
                } else if (optionClass == VMIntOption.class) {
                    entryValue.setValue(((VMIntOption) vmOption).getValue());
                } else if (optionClass == VMFloatOption.class) {
                    entryValue.setValue(((VMFloatOption) vmOption).getValue());
                } else if (optionClass == VMDoubleOption.class) {
                    entryValue.setValue(((VMDoubleOption) vmOption).getValue());
                } else {
                    MaxGraal.unimplemented(" handling of option class: " + optionClass.getName());
                }
            }
        }
    }

    /**
     * Returns the {@link VMOption} correspond to {@code name}.
     */
    public static VMOption getVMOption(String name) {
        for (VMOption vmOption : optionMap.keySet()) {
            if (vmOption.name().equals(name)) {
                return vmOption;
            }
        }
        return null;
    }

    public static OptionValue getOptionValue(VMOption vmOption) {
        return optionMap.get(vmOption);
    }

    // check that options are up to date wrt to Graal

    static {
        JavaPrototype.registerGeneratedCodeCheckerCallback(new GeneratedCodeCheckerCallback());
    }

    @HOSTED_ONLY
    private static class GeneratedCodeCheckerCallback implements JavaPrototype.GeneratedCodeCheckerCallback,  SourceGenerator.PackageChecker {

        @Override
        public void checkGeneratedCode() {
            boolean wouldBeUpdated = MaxGraalOptionsGenerator.check(this);
            FatalError.check(!wouldBeUpdated, "MaxGraalOptions is out of sync with Graal, regenerate and refresh in IDE");
        }

        @Override
        public boolean include(String name) {
            for (BootImagePackage p : VMConfiguration.activeConfig().bootImagePackages) {
                if (p.name().equals(name)) {
                    return true;
                }
            }
            return false;
        }

    }

    @HOSTED_ONLY
    private static VMOption register(VMOption vmOption, Class< ? > optionClass, String fieldName) {
        VMOptions.register(vmOption, Phase.STARTING);
        try {
            Field field = optionClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            optionMap.put(vmOption, (OptionValue) field.get(null));
        } catch (Exception ex) {
            Trace.line(0, "WARNING: error reading Graal option value: " + fieldName);
        }
        return vmOption;
    }

// START GENERATED CODE
    static {
        // Options from com.oracle.graal.compiler.GraalDebugConfig
        register(new VMBooleanOption("-G:+", "Debug", "Enable scope-based debugging"), com.oracle.graal.compiler.GraalDebugConfig.class, "DebugEnabled");
        register(new VMBooleanOption("-G:+", "DetailedAsserts", "Enable expensive assertions"), com.oracle.graal.compiler.GraalDebugConfig.class, "DetailedAsserts");
        register(new VMStringOption("-G:Dump=", false, null, "Scopes to be dumped"), com.oracle.graal.compiler.GraalDebugConfig.class, "Dump");
        register(new VMBooleanOption("-G:-", "DumpOnError", "Send Graal IR to dump handlers on error"), com.oracle.graal.compiler.GraalDebugConfig.class, "DumpOnError");
        register(new VMStringOption("-G:Log=", false, null, "Scopes to be logged"), com.oracle.graal.compiler.GraalDebugConfig.class, "Log");
        register(new VMStringOption("-G:Meter=", false, null, "Scopes to be metered"), com.oracle.graal.compiler.GraalDebugConfig.class, "Meter");
        register(new VMStringOption("-G:MethodFilter=", false, null, "Filters debug scope output by method name/pattern"), com.oracle.graal.compiler.GraalDebugConfig.class, "MethodFilter");
        register(new VMBooleanOption("-G:-", "PerThreadDebugValues", ""), com.oracle.graal.compiler.GraalDebugConfig.class, "PerThreadDebugValues");
        register(new VMBooleanOption("-G:-", "SummarizeDebugValues", ""), com.oracle.graal.compiler.GraalDebugConfig.class, "SummarizeDebugValues");
        register(new VMBooleanOption("-G:-", "SummarizePerPhase", ""), com.oracle.graal.compiler.GraalDebugConfig.class, "SummarizePerPhase");
        register(new VMStringOption("-G:Time=", false, null, "Scopes to be timed"), com.oracle.graal.compiler.GraalDebugConfig.class, "Time");
        // Options from com.oracle.graal.compiler.phases.HighTier
        register(new VMBooleanOption("-G:+", "Inline", "Enable inlining"), com.oracle.graal.compiler.phases.HighTier.class, "Inline");
        register(new VMBooleanOption("-G:+", "VerifyUsageWithEquals", ""), com.oracle.graal.compiler.phases.HighTier.class, "VerifyUsageWithEquals");
        // Options from com.oracle.graal.phases.GraalOptions
        register(new VMBooleanOption("-G:-", "AOTCompilation", "configure compiler to emit code compatible with AOT requirements for HotSpot"), com.oracle.graal.phases.GraalOptions.class, "AOTCompilation");
        register(new VMBooleanOption("-G:+", "AlignCallsForPatching", ""), com.oracle.graal.phases.GraalOptions.class, "AlignCallsForPatching");
        register(new VMBooleanOption("-G:-", "AlwaysInlineVTableStubs", ""), com.oracle.graal.phases.GraalOptions.class, "AlwaysInlineVTableStubs");
        register(new VMFloatOption("-G:BoostInliningForEscapeAnalysis=", 2.0f, ""), com.oracle.graal.phases.GraalOptions.class, "BoostInliningForEscapeAnalysis");
        register(new VMBooleanOption("-G:-", "BootstrapReplacements", ""), com.oracle.graal.phases.GraalOptions.class, "BootstrapReplacements");
        register(new VMBooleanOption("-G:+", "CacheGraphs", ""), com.oracle.graal.phases.GraalOptions.class, "CacheGraphs");
        register(new VMBooleanOption("-G:+", "CanOmitFrame", ""), com.oracle.graal.phases.GraalOptions.class, "CanOmitFrame");
        register(new VMFloatOption("-G:CapInheritedRelevance=", 1.0f, ""), com.oracle.graal.phases.GraalOptions.class, "CapInheritedRelevance");
        register(new VMIntOption("-G:CheckcastMaxHints=", 2, ""), com.oracle.graal.phases.GraalOptions.class, "CheckcastMaxHints");
        register(new VMDoubleOption("-G:CheckcastMinHintHitProbability=", 0.5, ""), com.oracle.graal.phases.GraalOptions.class, "CheckcastMinHintHitProbability");
        register(new VMStringOption("-G:CompileTheWorld=", false, null, ""), com.oracle.graal.phases.GraalOptions.class, "CompileTheWorld");
        register(new VMIntOption("-G:CompileTheWorldStartAt=", 1, ""), com.oracle.graal.phases.GraalOptions.class, "CompileTheWorldStartAt");
        register(new VMIntOption("-G:CompileTheWorldStopAt=", 2147483647, ""), com.oracle.graal.phases.GraalOptions.class, "CompileTheWorldStopAt");
        register(new VMBooleanOption("-G:+", "ConditionalElimination", ""), com.oracle.graal.phases.GraalOptions.class, "ConditionalElimination");
        register(new VMStringOption("-G:DecompileAfterPhase=", false, null, "Sets a phase after which the decompiler dumps the graph, -G:Dump= required"), com.oracle.graal.phases.GraalOptions.class, "DecompileAfterPhase");
        register(new VMBooleanOption("-G:-", "DeoptALot", ""), com.oracle.graal.phases.GraalOptions.class, "DeoptALot");
        register(new VMIntOption("-G:DeoptsToDisableOptimisticOptimization=", 40, ""), com.oracle.graal.phases.GraalOptions.class, "DeoptsToDisableOptimisticOptimization");
        register(new VMBooleanOption("-G:-", "EscapeAnalysisHistogram", ""), com.oracle.graal.phases.GraalOptions.class, "EscapeAnalysisHistogram");
        register(new VMIntOption("-G:EscapeAnalysisIterations=", 2, ""), com.oracle.graal.phases.GraalOptions.class, "EscapeAnalysisIterations");
        register(new VMStringOption("-G:EscapeAnalyzeOnly=", false, null, ""), com.oracle.graal.phases.GraalOptions.class, "EscapeAnalyzeOnly");
        register(new VMIntOption("-G:ExactFullUnrollMaxNodes=", 1200, ""), com.oracle.graal.phases.GraalOptions.class, "ExactFullUnrollMaxNodes");
        register(new VMBooleanOption("-G:-", "ExitVMOnBailout", ""), com.oracle.graal.phases.GraalOptions.class, "ExitVMOnBailout");
        register(new VMBooleanOption("-G:+", "ExitVMOnException", ""), com.oracle.graal.phases.GraalOptions.class, "ExitVMOnException");
        register(new VMBooleanOption("-G:+", "FullUnroll", ""), com.oracle.graal.phases.GraalOptions.class, "FullUnroll");
        register(new VMIntOption("-G:FullUnrollMaxNodes=", 300, ""), com.oracle.graal.phases.GraalOptions.class, "FullUnrollMaxNodes");
        register(new VMIntOption("-G:GCDebugStartCycle=", -1, ""), com.oracle.graal.phases.GraalOptions.class, "GCDebugStartCycle");
        register(new VMBooleanOption("-G:-", "GenAssertionCode", ""), com.oracle.graal.phases.GraalOptions.class, "GenAssertionCode");
        register(new VMBooleanOption("-G:+", "GenLoopSafepoints", ""), com.oracle.graal.phases.GraalOptions.class, "GenLoopSafepoints");
        register(new VMBooleanOption("-G:+", "GenSafepoints", ""), com.oracle.graal.phases.GraalOptions.class, "GenSafepoints");
        register(new VMIntOption("-G:GraphCacheSize=", 1000, ""), com.oracle.graal.phases.GraalOptions.class, "GraphCacheSize");
        register(new VMBooleanOption("-G:-", "HotSpotPrintCompilation", ""), com.oracle.graal.phases.GraalOptions.class, "HotSpotPrintCompilation");
        register(new VMBooleanOption("-G:-", "HotSpotPrintInlining", ""), com.oracle.graal.phases.GraalOptions.class, "HotSpotPrintInlining");
        register(new VMBooleanOption("-G:-", "InlineEverything", ""), com.oracle.graal.phases.GraalOptions.class, "InlineEverything");
        register(new VMBooleanOption("-G:+", "InlineMegamorphicCalls", "Enable inlining of megamorphic calls"), com.oracle.graal.phases.GraalOptions.class, "InlineMegamorphicCalls");
        register(new VMBooleanOption("-G:+", "InlineMonomorphicCalls", "Enable inlining of monomorphic calls"), com.oracle.graal.phases.GraalOptions.class, "InlineMonomorphicCalls");
        register(new VMBooleanOption("-G:+", "InlinePolymorphicCalls", "Enable inlining of polymorphic calls"), com.oracle.graal.phases.GraalOptions.class, "InlinePolymorphicCalls");
        register(new VMBooleanOption("-G:+", "InlineVTableStubs", ""), com.oracle.graal.phases.GraalOptions.class, "InlineVTableStubs");
        register(new VMIntOption("-G:InstanceOfMaxHints=", 2, ""), com.oracle.graal.phases.GraalOptions.class, "InstanceOfMaxHints");
        register(new VMDoubleOption("-G:InstanceOfMinHintHitProbability=", 0.5, ""), com.oracle.graal.phases.GraalOptions.class, "InstanceOfMinHintHitProbability");
        register(new VMBooleanOption("-G:+", "Intrinsify", "Enable use of compiler intrinsics"), com.oracle.graal.phases.GraalOptions.class, "Intrinsify");
        register(new VMBooleanOption("-G:+", "IntrinsifyAESMethods", ""), com.oracle.graal.phases.GraalOptions.class, "IntrinsifyAESMethods");
        register(new VMBooleanOption("-G:+", "IntrinsifyArrayCopy", ""), com.oracle.graal.phases.GraalOptions.class, "IntrinsifyArrayCopy");
        register(new VMBooleanOption("-G:+", "IntrinsifyCallSiteTarget", ""), com.oracle.graal.phases.GraalOptions.class, "IntrinsifyCallSiteTarget");
        register(new VMBooleanOption("-G:+", "IntrinsifyClassMethods", ""), com.oracle.graal.phases.GraalOptions.class, "IntrinsifyClassMethods");
        register(new VMBooleanOption("-G:+", "IntrinsifyInstalledCodeMethods", ""), com.oracle.graal.phases.GraalOptions.class, "IntrinsifyInstalledCodeMethods");
        register(new VMBooleanOption("-G:+", "IntrinsifyMathMethods", ""), com.oracle.graal.phases.GraalOptions.class, "IntrinsifyMathMethods");
        register(new VMBooleanOption("-G:-", "IntrinsifyObjectClone", ""), com.oracle.graal.phases.GraalOptions.class, "IntrinsifyObjectClone");
        register(new VMBooleanOption("-G:+", "IntrinsifyObjectMethods", ""), com.oracle.graal.phases.GraalOptions.class, "IntrinsifyObjectMethods");
        register(new VMBooleanOption("-G:+", "IntrinsifyReflectionMethods", ""), com.oracle.graal.phases.GraalOptions.class, "IntrinsifyReflectionMethods");
        register(new VMBooleanOption("-G:+", "IntrinsifySystemMethods", ""), com.oracle.graal.phases.GraalOptions.class, "IntrinsifySystemMethods");
        register(new VMBooleanOption("-G:+", "IntrinsifyThreadMethods", ""), com.oracle.graal.phases.GraalOptions.class, "IntrinsifyThreadMethods");
        register(new VMBooleanOption("-G:+", "IntrinsifyUnsafeMethods", ""), com.oracle.graal.phases.GraalOptions.class, "IntrinsifyUnsafeMethods");
        register(new VMBooleanOption("-G:-", "IterativeInlining", ""), com.oracle.graal.phases.GraalOptions.class, "IterativeInlining");
        register(new VMDoubleOption("-G:LimitInlinedInvokes=", 5.0, ""), com.oracle.graal.phases.GraalOptions.class, "LimitInlinedInvokes");
        register(new VMIntOption("-G:LoopMaxUnswitch=", 3, ""), com.oracle.graal.phases.GraalOptions.class, "LoopMaxUnswitch");
        register(new VMBooleanOption("-G:+", "LoopPeeling", ""), com.oracle.graal.phases.GraalOptions.class, "LoopPeeling");
        register(new VMBooleanOption("-G:+", "LoopUnswitch", ""), com.oracle.graal.phases.GraalOptions.class, "LoopUnswitch");
        register(new VMIntOption("-G:LoopUnswitchMaxIncrease=", 50, ""), com.oracle.graal.phases.GraalOptions.class, "LoopUnswitchMaxIncrease");
        register(new VMIntOption("-G:LoopUnswitchUncertaintyBoost=", 5, ""), com.oracle.graal.phases.GraalOptions.class, "LoopUnswitchUncertaintyBoost");
        register(new VMIntOption("-G:MaximumDesiredSize=", 20000, ""), com.oracle.graal.phases.GraalOptions.class, "MaximumDesiredSize");
        register(new VMIntOption("-G:MaximumEscapeAnalysisArrayLength=", 32, ""), com.oracle.graal.phases.GraalOptions.class, "MaximumEscapeAnalysisArrayLength");
        register(new VMIntOption("-G:MaximumInliningSize=", 300, ""), com.oracle.graal.phases.GraalOptions.class, "MaximumInliningSize");
        register(new VMIntOption("-G:MaximumRecursiveInlining=", 5, ""), com.oracle.graal.phases.GraalOptions.class, "MaximumRecursiveInlining");
        register(new VMDoubleOption("-G:MegamorphicInliningMinMethodProbability=", 0.33, ""), com.oracle.graal.phases.GraalOptions.class, "MegamorphicInliningMinMethodProbability");
        register(new VMBooleanOption("-G:+", "MemoryAwareScheduling", ""), com.oracle.graal.phases.GraalOptions.class, "MemoryAwareScheduling");
        register(new VMDoubleOption("-G:MinTableSwitchDensity=", 0.5, ""), com.oracle.graal.phases.GraalOptions.class, "MinTableSwitchDensity");
        register(new VMIntOption("-G:MinimumJumpTableSize=", 5, ""), com.oracle.graal.phases.GraalOptions.class, "MinimumJumpTableSize");
        register(new VMFloatOption("-G:MinimumPeelProbability=", 0.35f, ""), com.oracle.graal.phases.GraalOptions.class, "MinimumPeelProbability");
        register(new VMBooleanOption("-G:-", "NewMemoryAwareScheduling", ""), com.oracle.graal.phases.GraalOptions.class, "NewMemoryAwareScheduling");
        register(new VMBooleanOption("-G:-", "OmitHotExceptionStacktrace", ""), com.oracle.graal.phases.GraalOptions.class, "OmitHotExceptionStacktrace");
        register(new VMBooleanOption("-G:+", "OptAssumptions", ""), com.oracle.graal.phases.GraalOptions.class, "OptAssumptions");
        register(new VMBooleanOption("-G:+", "OptCanonicalizer", ""), com.oracle.graal.phases.GraalOptions.class, "OptCanonicalizer");
        register(new VMBooleanOption("-G:+", "OptConvertDeoptsToGuards", ""), com.oracle.graal.phases.GraalOptions.class, "OptConvertDeoptsToGuards");
        register(new VMBooleanOption("-G:+", "OptDevirtualizeInvokesOptimistically", ""), com.oracle.graal.phases.GraalOptions.class, "OptDevirtualizeInvokesOptimistically");
        register(new VMBooleanOption("-G:+", "OptEliminateGuards", ""), com.oracle.graal.phases.GraalOptions.class, "OptEliminateGuards");
        register(new VMBooleanOption("-G:+", "OptEliminatePartiallyRedundantGuards", ""), com.oracle.graal.phases.GraalOptions.class, "OptEliminatePartiallyRedundantGuards");
        register(new VMBooleanOption("-G:+", "OptEliminateSafepoints", ""), com.oracle.graal.phases.GraalOptions.class, "OptEliminateSafepoints");
        register(new VMBooleanOption("-G:+", "OptFilterProfiledTypes", ""), com.oracle.graal.phases.GraalOptions.class, "OptFilterProfiledTypes");
        register(new VMBooleanOption("-G:+", "OptFloatingReads", ""), com.oracle.graal.phases.GraalOptions.class, "OptFloatingReads");
        register(new VMBooleanOption("-G:+", "OptImplicitNullChecks", ""), com.oracle.graal.phases.GraalOptions.class, "OptImplicitNullChecks");
        register(new VMBooleanOption("-G:+", "OptLivenessAnalysis", ""), com.oracle.graal.phases.GraalOptions.class, "OptLivenessAnalysis");
        register(new VMBooleanOption("-G:+", "OptLoopTransform", ""), com.oracle.graal.phases.GraalOptions.class, "OptLoopTransform");
        register(new VMBooleanOption("-G:+", "OptPushThroughPi", ""), com.oracle.graal.phases.GraalOptions.class, "OptPushThroughPi");
        register(new VMBooleanOption("-G:+", "OptReadElimination", ""), com.oracle.graal.phases.GraalOptions.class, "OptReadElimination");
        register(new VMBooleanOption("-G:+", "OptScheduleOutOfLoops", ""), com.oracle.graal.phases.GraalOptions.class, "OptScheduleOutOfLoops");
        register(new VMBooleanOption("-G:+", "OptTailDuplication", ""), com.oracle.graal.phases.GraalOptions.class, "OptTailDuplication");
        register(new VMBooleanOption("-G:-", "PEAInliningHints", ""), com.oracle.graal.phases.GraalOptions.class, "PEAInliningHints");
        register(new VMBooleanOption("-G:+", "PartialEscapeAnalysis", ""), com.oracle.graal.phases.GraalOptions.class, "PartialEscapeAnalysis");
        register(new VMBooleanOption("-G:-", "PrintBailout", ""), com.oracle.graal.phases.GraalOptions.class, "PrintBailout");
        register(new VMIntOption("-G:PrintBinaryGraphPort=", 4445, ""), com.oracle.graal.phases.GraalOptions.class, "PrintBinaryGraphPort");
        register(new VMBooleanOption("-G:+", "PrintBinaryGraphs", ""), com.oracle.graal.phases.GraalOptions.class, "PrintBinaryGraphs");
        register(new VMBooleanOption("-G:-", "PrintCFG", ""), com.oracle.graal.phases.GraalOptions.class, "PrintCFG");
        register(new VMBooleanOption("-G:-", "PrintCodeBytes", ""), com.oracle.graal.phases.GraalOptions.class, "PrintCodeBytes");
        register(new VMBooleanOption("-G:-", "PrintCompilation", ""), com.oracle.graal.phases.GraalOptions.class, "PrintCompilation");
        register(new VMStringOption("-G:PrintFilter=", false, null, ""), com.oracle.graal.phases.GraalOptions.class, "PrintFilter");
        register(new VMBooleanOption("-G:-", "PrintGraphCache", ""), com.oracle.graal.phases.GraalOptions.class, "PrintGraphCache");
        register(new VMBooleanOption("-G:-", "PrintGraphProbabilities", "outputs probabilities for fixed nodes during binary graph dumping"), com.oracle.graal.phases.GraalOptions.class, "PrintGraphProbabilities");
        register(new VMBooleanOption("-G:-", "PrintIRWithLIR", ""), com.oracle.graal.phases.GraalOptions.class, "PrintIRWithLIR");
        register(new VMStringOption("-G:PrintIdealGraphAddress=", false, "127.0.0.1", ""), com.oracle.graal.phases.GraalOptions.class, "PrintIdealGraphAddress");
        register(new VMBooleanOption("-G:-", "PrintIdealGraphFile", ""), com.oracle.graal.phases.GraalOptions.class, "PrintIdealGraphFile");
        register(new VMIntOption("-G:PrintIdealGraphPort=", 4444, ""), com.oracle.graal.phases.GraalOptions.class, "PrintIdealGraphPort");
        register(new VMBooleanOption("-G:-", "PrintProfilingInformation", ""), com.oracle.graal.phases.GraalOptions.class, "PrintProfilingInformation");
        register(new VMBooleanOption("-G:-", "PrintStackTraceOnException", ""), com.oracle.graal.phases.GraalOptions.class, "PrintStackTraceOnException");
        register(new VMIntOption("-G:RangeTestsSwitchDensity=", 5, ""), com.oracle.graal.phases.GraalOptions.class, "RangeTestsSwitchDensity");
        register(new VMBooleanOption("-G:+", "ReassociateInvariants", ""), com.oracle.graal.phases.GraalOptions.class, "ReassociateInvariants");
        register(new VMStringOption("-G:RegisterPressure=", false, null, ""), com.oracle.graal.phases.GraalOptions.class, "RegisterPressure");
        register(new VMFloatOption("-G:RelevanceCapForInlining=", 1.0f, ""), com.oracle.graal.phases.GraalOptions.class, "RelevanceCapForInlining");
        register(new VMBooleanOption("-G:+", "RemoveNeverExecutedCode", ""), com.oracle.graal.phases.GraalOptions.class, "RemoveNeverExecutedCode");
        register(new VMBooleanOption("-G:-", "ResolveClassBeforeStaticInvoke", ""), com.oracle.graal.phases.GraalOptions.class, "ResolveClassBeforeStaticInvoke");
        register(new VMIntOption("-G:SmallCompiledLowLevelGraphSize=", 300, ""), com.oracle.graal.phases.GraalOptions.class, "SmallCompiledLowLevelGraphSize");
        register(new VMBooleanOption("-G:-", "SnippetCounters", ""), com.oracle.graal.phases.GraalOptions.class, "SnippetCounters");
        register(new VMIntOption("-G:StackShadowPages=", 2, ""), com.oracle.graal.phases.GraalOptions.class, "StackShadowPages");
        register(new VMBooleanOption("-G:+", "SupportJsrBytecodes", ""), com.oracle.graal.phases.GraalOptions.class, "SupportJsrBytecodes");
        register(new VMDoubleOption("-G:TailDuplicationProbability=", 0.5, ""), com.oracle.graal.phases.GraalOptions.class, "TailDuplicationProbability");
        register(new VMIntOption("-G:TailDuplicationTrivialSize=", 1, ""), com.oracle.graal.phases.GraalOptions.class, "TailDuplicationTrivialSize");
        register(new VMIntOption("-G:TraceBytecodeParserLevel=", 0, ""), com.oracle.graal.phases.GraalOptions.class, "TraceBytecodeParserLevel");
        register(new VMBooleanOption("-G:-", "TraceEscapeAnalysis", ""), com.oracle.graal.phases.GraalOptions.class, "TraceEscapeAnalysis");
        register(new VMIntOption("-G:TraceLIRGeneratorLevel=", 0, ""), com.oracle.graal.phases.GraalOptions.class, "TraceLIRGeneratorLevel");
        register(new VMIntOption("-G:TraceLinearScanLevel=", 0, ""), com.oracle.graal.phases.GraalOptions.class, "TraceLinearScanLevel");
        register(new VMIntOption("-G:TrivialInliningSize=", 10, ""), com.oracle.graal.phases.GraalOptions.class, "TrivialInliningSize");
        register(new VMBooleanOption("-G:+", "UseExceptionProbability", ""), com.oracle.graal.phases.GraalOptions.class, "UseExceptionProbability");
        register(new VMBooleanOption("-G:+", "UseExceptionProbabilityForOperations", ""), com.oracle.graal.phases.GraalOptions.class, "UseExceptionProbabilityForOperations");
        register(new VMBooleanOption("-G:+", "UseLoopLimitChecks", ""), com.oracle.graal.phases.GraalOptions.class, "UseLoopLimitChecks");
        register(new VMBooleanOption("-G:+", "UseProfilingInformation", ""), com.oracle.graal.phases.GraalOptions.class, "UseProfilingInformation");
        register(new VMBooleanOption("-G:+", "UseTypeCheckHints", ""), com.oracle.graal.phases.GraalOptions.class, "UseTypeCheckHints");
        register(new VMBooleanOption("-G:-", "VerifyPhases", ""), com.oracle.graal.phases.GraalOptions.class, "VerifyPhases");
        register(new VMBooleanOption("-G:-", "ZapStackOnMethodEntry", ""), com.oracle.graal.phases.GraalOptions.class, "ZapStackOnMethodEntry");
        // Options from com.oracle.graal.phases.common.InliningPhase
        register(new VMBooleanOption("-G:-", "AlwaysInlineIntrinsics", "Unconditionally inline intrinsics"), com.oracle.graal.phases.common.InliningPhase.Options.class, "AlwaysInlineIntrinsics");
        // Options from com.oracle.graal.phases.tiers.Suites
        register(new VMStringOption("-G:CompilerConfiguration=", false, "", "The compiler configuration to use"), com.oracle.graal.phases.tiers.Suites.Options.class, "CompilerConfiguration");
        // Options from com.oracle.graal.virtual.phases.ea.PartialEscapePhase
        register(new VMBooleanOption("-G:+", "OptEarlyReadElimination", ""), com.oracle.graal.virtual.phases.ea.PartialEscapePhase.class, "OptEarlyReadElimination");
    }
// END GENERATED CODE

}

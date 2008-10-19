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
package com.sun.max.vm.profile;

import java.util.*;
import java.io.*;

import com.sun.max.annotate.*;
import com.sun.max.profile.*;
import com.sun.max.profile.ValueMetrics.*;
import com.sun.max.util.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.*;
import com.sun.max.vm.debug.*;

/**
 * This scheme is used to collect Profiling data. Collecting is enabled at the beginning of the VM
 * {@linkplain MaxineVM.Phase#STARTING starting} phase and disabled when the VM is terminating, after which the data is
 * dumped to maxvm.prof
 *
 * @author Yi Guo, Aziz Ghuloum
 **/
public class ProfilingScheme extends AbstractVMScheme implements VMScheme {
    private static VMOption _isProfilingOption = new VMOption("-XX:Profile", "Collect and dump profiling info", Phase.STARTING);
    private static volatile boolean _isProfiling = false;


    private static final String _profilingDumpFileName = "maxvm.prof";
    @CONSTANT_WHEN_NOT_ZERO
    private static PrintStream _out;

    /**
     * Log for IR Generation and individual transformations.
     */
    @CONSTANT_WHEN_NOT_ZERO
    private static LinkedList<IrGenerationInfo> _irGenerationLog;

    /**
     * Log for compilation of each Method.
     */
    @CONSTANT_WHEN_NOT_ZERO
    private static LinkedList <CompilationInfo> _compilationLog;

    /**
     * Timer to time the compilation and its sub-generation/passes.
     */
    @CONSTANT_WHEN_NOT_ZERO
    private static NanoTimer _compilationTimer = null;

    /**
     * Timestamp when entering each phases.
     */
    private static final long[] _phaseTimeStamps = new long[Phase.values().length];

    @CONSTANT_WHEN_NOT_ZERO
    private static IntegerDistribution _bytecodeSizeDistribution = null;

    @CONSTANT_WHEN_NOT_ZERO
    private static IntegerDistribution _targetcodeSizeDistribution = null;

    @CONSTANT_WHEN_NOT_ZERO
    private static IntegerDistribution _targetcodeSizePerByteCodeDistribution = null;

    public ProfilingScheme(VMConfiguration configuration) {
        super(configuration);
    }

    @Override
    public void finalize(Phase phase) {
        if (phase == Phase.RUNNING) {
            if (ProfilingScheme.isProfiling()) {
                ProfilingScheme.disable();
                ProfilingScheme.report();
            }
        }
    }

    @Override
    public void initialize(Phase phase) {
        if (phase == Phase.STARTING) {
            if (_isProfilingOption.isPresent()) {
                initializeMetrics();
                // if _isProfiling is seen, all metrics have already been initialized
                _isProfiling = true;
            }
        }
    }

    public static boolean isProfiling() {
        return _isProfiling;
    }

    private static void disable() {
        _isProfiling = false;
    }

    public static NanoTimer compilerTimer() {
        return _compilationTimer;
    }

    private void initializeMetrics() {
        _bytecodeSizeDistribution = ValueMetrics.newIntegerDistribution(null);
        _targetcodeSizeDistribution = ValueMetrics.newIntegerDistribution(null);
        _targetcodeSizePerByteCodeDistribution = ValueMetrics.newIntegerDistribution(null);
        _compilationLog = new LinkedList<CompilationInfo>();
        _irGenerationLog = new LinkedList<IrGenerationInfo>();
        _compilationTimer = new NanoTimer();
    }


    public static void setPhaseTimeStamp(Phase phase) {
        _phaseTimeStamps[phase.ordinal()] = System.nanoTime();
    }

    private static void printInterval(String from, String to, long t) {
        _out.println(from + " to " + to + ": " + t / 1e6 + " msec");
    }

    private static void reportMetrics(Metrics.Metric metric, String name) {
        metric.report("BEGIN " + name, _out);
        _out.println("END " + name);
    }
    private static void reportCompilationStats() {
        _out.println("BEGIN " + "Compilation_Stats");
        _out.println(CompilationInfo.getHeader());
        for (CompilationInfo c : _compilationLog)  {
            _out.println(c);
        }
        _out.println("END " + "Compilation_Stats");
    }

    private static void reportIrGenerationStats() {
        _out.println("BEGIN " + "IrGeneration_Stats");
        _out.println(IrGenerationInfo.getHeader());
        for (IrGenerationInfo irGen : _irGenerationLog)  {
            _out.println(irGen);
        }
        _out.println("END " + "IrGeneration_Stats");
    }

    public static void report() {
        try {
            _out = new PrintStream(_profilingDumpFileName);
        } catch (FileNotFoundException e) {
            Debug.println("Cannot create max.prof");
            e.printStackTrace();
            return;
        }
        try {
            reportMetrics(_bytecodeSizeDistribution, "Bytecode_Size");
            reportMetrics(_targetcodeSizeDistribution, "Targetcode_Size");
            reportMetrics(_targetcodeSizePerByteCodeDistribution, "TargetcodeSize_Per_Bytecode");
            reportCompilationStats();
            reportIrGenerationStats();
            Phase prev = null;
            _out.println("Running time for each phase:");
            for (Phase p : Phase.values()) {
                if (p.ordinal() > Phase.PRISTINE.ordinal()) {
                    printInterval(prev.name(), p.name(), _phaseTimeStamps[p.ordinal()] - _phaseTimeStamps[prev.ordinal()]);
                }
                prev = p;
            }
            printInterval(prev.name(), "end", System.nanoTime() - _phaseTimeStamps[prev.ordinal()]);
        } finally {
            _out.close();
        }
    }


    public static synchronized void recordBytecodeSize(int size) {
        if (isProfiling()) {
            _bytecodeSizeDistribution.record(size);
        }
    }

    public static synchronized void recordTargetcodeSize(int size) {
        if (isProfiling()) {
            _targetcodeSizeDistribution.record(size);
        }
    }

    public static synchronized void recordTargetcodeSizePerByteCode(int ratio10) {
        if (isProfiling()) {
            _targetcodeSizePerByteCodeDistribution.record(ratio10);
        }
    }

    public static synchronized void  recordCompilationInfo(String methodName, String compilerName, int bytecodeSize, int sourceCodeSize, int targetCodeSize,
                    int numberOfDirectCalls, int numberOfIndirectCalls, int numberOfSafepoints, int numberOfReferenceLiterals, int numberOfScalarLiteralBytes,
                    long compilationNanotime) {
        if (isProfiling()) {
            _compilationLog.add(new CompilationInfo(methodName, compilerName, bytecodeSize, sourceCodeSize, targetCodeSize,
                             numberOfDirectCalls, numberOfIndirectCalls, numberOfSafepoints, numberOfReferenceLiterals, numberOfScalarLiteralBytes,
                             compilationNanotime));
        }
    }

    public static synchronized void recordIrGenerationInfo(String qualifiedName, String irName, long time, int lev) {
        if (isProfiling()) {
            _irGenerationLog.add(new IrGenerationInfo(qualifiedName, irName, time, lev));
        }
    }

    static class CompilationInfo {
        private String _methodName;
        private String _compilerName;
        private int _bytecodeSize;
        private int _sourceCodeSize;
        private int _targetCodeSize;
        private int _numberOfDirectCalls;
        private int _numberOfIndirectCalls;
        private int _numberOfSafepoints;
        private int _numberOfReferenceLiterals;
        private int _numberOfScalarLiteralBytes;
        private long _compilationNanotime;
        private static String _formatString = "%30s : %12s : %12s : %12s : %12s : %12s : %12s : %12s : %12s : %12s : %s";
        CompilationInfo(String methodName, String compilerName, int bytecodeSize, int sourceCodeSize, int targetCodeSize,
                        int numberOfDirectCalls, int numberOfIndirectCalls, int numberOfSafepoints, int numberOfReferenceLiterals, int numberOfScalarLiteralBytes,
                        long compilationNanotime) {
            _methodName = methodName;
            _compilerName = compilerName;
            _bytecodeSize = bytecodeSize;
            _sourceCodeSize = sourceCodeSize;
            _targetCodeSize  = targetCodeSize;
            _numberOfDirectCalls = numberOfDirectCalls;
            _numberOfIndirectCalls = numberOfIndirectCalls;
            _numberOfSafepoints = numberOfSafepoints;
            _numberOfReferenceLiterals = numberOfReferenceLiterals;
            _numberOfScalarLiteralBytes = numberOfScalarLiteralBytes;
            _compilationNanotime = compilationNanotime;
        }

        public static String getHeader() {
            return String.format(_formatString, "#Compiler", "BC_Size", "Source_Size", "Target_Size", "Dir_Call", "Ind_Call", "Safepoints", "Refs", "ScalarBytes", "Comp_Time (ms)", "Method");
        }

        @Override
        public String toString() {
            return String.format(_formatString,
                                 _compilerName,
                                 Integer.toString(_bytecodeSize),
                                 Integer.toString(_sourceCodeSize),
                                 Integer.toString(_targetCodeSize),
                                 Integer.toString(_numberOfDirectCalls),
                                 Integer.toString(_numberOfIndirectCalls),
                                 Integer.toString(_numberOfSafepoints),
                                 Integer.toString(_numberOfReferenceLiterals),
                                 Integer.toString(_numberOfScalarLiteralBytes),
                                 String.format("%.3f", _compilationNanotime / 1e6),
                                 _methodName);
        }
    }

    static class IrGenerationInfo {
        private String _methodName;
        private String _irName;
        private Long _time;
        private int _level;
        private static String _formatString = "%20s : %6s : %10s : %s";
        IrGenerationInfo(String methodName, String irName, long time, int level) {
            _methodName = methodName;
            _irName = irName;
            _time = time;
            _level = level;
        }
        public static String getHeader() {
            return String.format(_formatString, "#Ir",  "Level", "Time", "Method");
        }

        @Override
        public String toString() {
            return String.format(_formatString, _irName, Integer.toString(_level), String.format("%.3f", _time / 1e6), _methodName);
        }
    }

}

/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.vma.run.java;

import static com.oracle.max.vm.ext.vma.VMABytecodes.*;
import static com.oracle.max.vm.ext.vma.run.java.VMAOptions.AdviceModeOption.*;

import java.util.regex.Pattern;

import com.oracle.max.vm.ext.vma.*;
import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.Log;
import com.sun.max.vm.MaxineVM;
import com.sun.max.vm.VMOptions;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.log.VMLog.Record;
import com.sun.max.vm.log.hosted.*;
import com.sun.max.vm.thread.*;

/**
 * Defines all the options that can be used to control the VMA system.
 *
 * VMA is enabled only at VM runtime {@link MaxineVM.Phase#RUNNING} through the {@code XX:VMA} style options.
 * However, JDK classes in the boot image can be deoptimized and instrumented at runtime.
 *
 * Note that that the {@link #VMA} option is dominant. If it is not set, the other options are ignored even if they are
 * set.
 *
 */
public class VMAOptions {

    public enum AdviceModeOption {
        B, A, BA;
    }

    public static class BM {
        VMABytecodes bytecode;
        AdviceModeOption adviceModeOption;
        public BM(VMABytecodes bytecode, AdviceModeOption adviceModeOption) {
            this.bytecode = bytecode;
            this.adviceModeOption = adviceModeOption;
        }

        boolean isApplied(AdviceMode am) {
            if (adviceModeOption == AdviceModeOption.BA) {
                return true;
            } else {
                return am == AdviceMode.BEFORE ? adviceModeOption == AdviceModeOption.B : adviceModeOption == AdviceModeOption.A;
            }
        }
    }

    /**
     * A handler can register an instance of this to perform a handler-specific check
     * for instrumenting a method. If an instance of this class is registered, it
     * completely overrides the normal regex based checks for instrumentation.
     */
    public interface SpecificInstrumentChecker {
        boolean instrument(MethodActor methodActor);
    }

    /**
     * Object creation.
     */
    public static final BM[] NEW_BM = new BM[] {
        new BM(NEW, A), new BM(NEWARRAY, A), new BM(ANEWARRAY, A),
        new BM(MULTIANEWARRAY, A)
    };

    /**
     * Getting a field of an object or a class (static).
     */
    public static final BM[] GETFIELD_BM = new BM[] {new BM(GETFIELD, B), new BM(GETSTATIC, B)};

    /**
     * An array load.
     */
    public static final BM[] ARRAYLOAD_BM = new BM[] {new BM(IALOAD, B), new BM(LALOAD, B), new BM(FALOAD, B),
        new BM(DALOAD, B), new BM(AALOAD, B), new BM(BALOAD, B), new BM(CALOAD, B), new BM(SALOAD, B)};

    /**
     * Method invocation (before).
     */
    public static final BM[] BEFORE_INVOKE_BM = new BM[] {
        new BM(INVOKEVIRTUAL, B), new BM(INVOKEINTERFACE, B),
        new BM(INVOKESTATIC, B), new BM(INVOKESPECIAL, B)
    };

    /**
     * IF operations on objects.
     */
    public static final BM[] IFOBJECT_BM = new BM[] {new BM(IF_ACMPNE, B), new BM(IF_ACMPEQ, B), new BM(IFNULL, B), new BM(IFNONNULL, B)};

    /**
     * Monitor entry/exit.
     */
    public static final BM[] MONITOR_BM = new BM[] {new BM(MONITORENTER, B), new BM(MONITOREXIT, B)};

    /**
     * Casts.
     */
    private static final BM[] CAST_BM = new BM[] {new BM(INSTANCEOF, B), new BM(CHECKCAST, B)};

    /**
     * Throw.
     */
    public static final BM[] THROW_BM = new BM[] {new BM(ATHROW, B)};

    /**
     * A read, where this is defined as any access to the object's state (fields, array elements or class metadata).
     * The latter encompasses many bytecodes, e.g. method invocation, checkcast, etc.
     */
    public static final BM[] READ_BM = compose(GETFIELD_BM, ARRAYLOAD_BM, new BM[] {new BM(ARRAYLENGTH, B)},
                                        BEFORE_INVOKE_BM, MONITOR_BM, CAST_BM, THROW_BM);

    /**
     * A use of an object that does not involve reading it's state.
     */
    private static final BM[] USE_BM = compose(IFOBJECT_BM, new BM[] {new BM(ARETURN, B), new BM(ASTORE, B), new BM(AASTORE, B),
                                                                      new BM(ALOAD, A), new BM(AALOAD, A)});
    /**
     * Writing a field of an object or a class (static).
     */
    public static final BM[] PUTFIELD_BM = new BM[] {new BM(PUTFIELD, B), new BM(PUTSTATIC, B)};

    /**
     * Array store.
     */
    public static final BM[] ARRAYSTORE_BM = new BM[] {new BM(IASTORE, B), new BM(LASTORE, B), new BM(FASTORE, B),
        new BM(DASTORE, B), new BM(AASTORE, B), new BM(BASTORE, B), new BM(CASTORE, B), new BM(SASTORE, B)};

    /**
     * A write, defined similarly to read.
     */
    public static final BM[] WRITE_BM = compose(ARRAYSTORE_BM, PUTFIELD_BM);

    /**
     * Method entry.
     */
    public static final BM[] METHOD_ENTRY_BM = new BM[] {
        new BM(MENTRY, A),
    };

    /**
     * Method exit, defined as the various forms of {@code RETURN}.
     */
    public static final BM[] METHOD_EXIT_BM = new BM[] {
        new BM(IRETURN, B), new BM(LRETURN, B), new BM(FRETURN, B),
        new BM(DRETURN, B), new BM(ARETURN, B), new BM(RETURN, B)
    };

    /**
     * Method entry and exit.
     */
    public static final BM[] METHOD_ENTRY_EXIT_BM = compose(METHOD_ENTRY_BM, METHOD_EXIT_BM);

    /**
     * Enables determination of the begin/end of object construction.
     * If we had INVOKE after advice this could be streamlined, but absent that we
     * need all method entries and returns.
     */
    public static final BM[] CONSTRUCTOR_BM = METHOD_ENTRY_EXIT_BM;

    /*
    private static final BM[] AFTERINVOKE_BM = new BM[] {
        new BM(INVOKEVIRTUAL, A), new BM(INVOKEINTERFACE, A),
        new BM(INVOKESTATIC, A), new BM(INVOKESPECIAL, A)
    };
    */

    public static final BM[] OBJECT_ACCESS = compose(NEW_BM, CONSTRUCTOR_BM, READ_BM, WRITE_BM);

    public static final BM[] OBJECT_USE = compose(OBJECT_ACCESS, USE_BM);

    public static class Config {
        private String name;
        private BM[] bytecodesToApply;

        public Config(String name, BM[] bytecodesToApply) {
            this.name = name;
            this.bytecodesToApply = bytecodesToApply;
        }
    }

    public static final Config[] STD_CONFIGS = new Config[] {
        new Config("null", new BM[0]),
        new Config("objectuse", OBJECT_USE),
        new Config("objectaccess", OBJECT_ACCESS),
        new Config("read", compose(NEW_BM, READ_BM)),
        new Config("write", compose(NEW_BM, WRITE_BM)),
        new Config("monitor", MONITOR_BM),
        new Config("beforeinvoke", BEFORE_INVOKE_BM),
//        new Config("afterinvoke", AFTERINVOKE_BM),
        new Config("invoke", BEFORE_INVOKE_BM),
        new Config("entry", METHOD_ENTRY_BM),
        new Config("exit", METHOD_EXIT_BM),
        new Config("entryexit", METHOD_ENTRY_EXIT_BM)
    };

    /**
     * A handler can call this to check that the VM is being run with a config that
     * is compatible with {@code config}, and optionally override any existing config.
     * @param config
     * @param override override the configuration with {@code config}
     */
    public static boolean checkConfig(Config config, boolean override) {
        boolean result = true;
        if (override) {
            initBytecodesToApply(false);
            setConfig(config);
            logBytecodeApply();
            return true;
        }
        for (BM bm : config.bytecodesToApply) {
            if (!bytecodeApply[bm.bytecode.ordinal()][bm.adviceModeOption.ordinal()]) {
                result = false;
                break;
            }
        }
        return result;
    }

    private static void setConfig(Config config) {
        for (BM ab : config.bytecodesToApply) {
            for (AdviceMode am : AdviceMode.values()) {
                boolean isApplied = ab.isApplied(am);
                if (isApplied) {
                    bytecodeApply[ab.bytecode.ordinal()][am.ordinal()] = isApplied;
                }
            }
        }
    }

    private static void initBytecodesToApply(boolean defValue) {
        for (VMABytecodes b : VMABytecodes.values()) {
            for (AdviceMode am : AdviceMode.values()) {
                bytecodeApply[b.ordinal()][am.ordinal()] = defValue;
            }
        }
    }

    public static BM[] compose(BM[] ... bms) {
        int length = 0;
        for (BM[] bm : bms) {
            length += bm.length;
        }
        BM[] result = new BM[length];

        length = 0;
        for (BM[] bm : bms) {
            System.arraycopy(bm, 0, result, length, bm.length);
            length += bm.length;
        }
        return result;
    }

    private static final String VMA_PKG_PATTERN = "com\\.oracle\\.max\\.vm\\.ext\\.vma\\..*";
    private static final String X_PATTERN = VMA_PKG_PATTERN;

    static {
        VMOptions.addFieldOption("-XX:", "VMA", VMAOptions.class, "enable advising");
        VMOptions.addFieldOption("-XX:", "VMAMI", VMAOptions.class, "regex for methods to instrument");
        VMOptions.addFieldOption("-XX:", "VMAMX", VMAOptions.class, "regex for methods not to instrument");
        VMOptions.addFieldOption("-XX:", "VMAXJDK", VMAOptions.class, "do not instrument any JDK classes");
        VMOptions.addFieldOption("-XX:", "VMATI", VMAOptions.class, "regex for threads to include");
        VMOptions.addFieldOption("-XX:", "VMATX", VMAOptions.class, "regex for threads to exclude");
        VMOptions.addFieldOption("-XX:", "VMABI", VMAOptions.class, "regex for bytecodes to match");
        VMOptions.addFieldOption("-XX:", "VMABX", VMAOptions.class, "regex for bytecodes to not match");
        VMOptions.addFieldOption("-XX:", "VMAConfig", VMAOptions.class, "use pre-defined configuration");
        VMOptions.addFieldOption("-XX:", "VMATime", VMAOptions.class, "specify how time is recorded");
        VMOptions.addFieldOption("-XX:", "VMASample", VMAOptions.class, "run in sample mode; interval,period");
    }

    /**
     * {@link Pattern regex pattern} defining specific methods to instrument.
     * If this option is set, only these methods are candidates for instrumentation, otherwise
     * all methods are candidates.
     * Syntax of a method pattern is {@code classpattern#methodpattern}.
     * The {@code #methodpattern} may be replaced with {@code .*} to indicate all methods.
     */
    private static String VMAMI;
    /**
     * {@link Pattern regex pattern} defining specific methods to exclude from instrumentation.
     * These modify the candidates for inclusion specified by {@link #VMAMI}.
     */
    private static String VMAMX;

    /**
     * Convenience option to specify the exclusion of all JDK classes.
     */
    private static boolean VMAXJDK;

    private static String VMATI;
    private static String VMATX;

    /**
     * {@link Pattern regex pattern} defining specific bytecodes to instrument.
     * If this option is set, only these bytecodes are instrumented, otherwise
     * all classes are instrumented. The format of the bytecode specification
     * is {@code CODE:MODE}, where {@code MODE} is {@code A}, {@code B} or {@code AB}.
     * If {@code MODE} is omitted it defaults to {@code AB}.
     */
    private static String VMABI;
    /**
     * {@link Pattern regex pattern} defining specific bytecodes to exclude from instrumentation.
     */
    private static String VMABX;

    private static String VMATime;

    private static VMATimeMode timeMode = VMATimeMode.WALLNS;

    private static Pattern methodInclusionPattern;
    private static Pattern methodExclusionPattern;

    private static Pattern threadInclusionPattern;
    private static Pattern threadExclusionPattern;

    /**
     * Records whether advice is being applied in the given mode for each bytecode.
     */
    private static boolean[][] bytecodeApply = new boolean[VMABytecodes.values().length][AdviceMode.values().length];


    /**
     * Specify a canned configuration that select specific bytecodes for particular analyses.
     * This must be a comma seperated list of values from {@link StConfig}.
     */
    private static String VMAConfig;

    /**
     * {@code true} if and only if we are doing any kind of advising.
     */
    static boolean VMA = true;

    /**
     * Sampling option.
     */
    static String VMASample;

    /**
     * If not {@code null} a handler-specific checker for which methods to instrument.
     */
    private static SpecificInstrumentChecker specificInstrumentChecker;

    public static void registerSpecificInstrumentChecker(SpecificInstrumentChecker checker) {
        specificInstrumentChecker = checker;
    }

    /**
     * Check options and set up the default {@link #bytecodeApply} based on the options,
     * and compile the {@link Pattern patterns} for method inclusion/exclusion.
     * This is called in the {@link MaxineVM.Phase#RUNNING running phase} only if {@link #VMA} is {@code true}.
     * N.B. A handler may override these settings in its initialize method.
     *
     */
    public static void initialize(MaxineVM.Phase phase) {
        assert VMA && phase == MaxineVM.Phase.RUNNING;

        // always exclude advising packages
        String xPattern = X_PATTERN;
        // optionally exclude JDK
        if (VMAXJDK) {
            xPattern += "|java.*|sun.*";
        }

        if (VMAMX != null) {
            xPattern += "|" + VMAMX;
        }
        if (VMAMI != null) {
            methodInclusionPattern = Pattern.compile(VMAMI);
        }
        methodExclusionPattern = Pattern.compile(xPattern);

        if (VMATI != null) {
            threadInclusionPattern = Pattern.compile(VMATI);
        }
        if (VMATX != null) {
            threadExclusionPattern = Pattern.compile(VMATX);
        }

        if (VMAConfig != null) {
            String[] vmaConfigs = VMAConfig.split(",");
            for (String vmaConfig : vmaConfigs) {
                Config stdConfig = null;
                for (Config c : STD_CONFIGS) {
                    if (c.name.equals(vmaConfig)) {
                        stdConfig = c;
                        break;
                    }
                }
                if (stdConfig == null) {
                    VMAJavaRunScheme.fail(vmaConfig + " is not a standard VMA configuration");
                }
                setConfig(stdConfig);
            }
        } else {
            // setup default values
            initBytecodesToApply(VMABI == null ? true : false);

            if (VMABI != null) {
                Pattern bytecodeInclusionPattern = Pattern.compile(VMABI);
                for (VMABytecodes b : VMABytecodes.values()) {
                    matchBytecode(bytecodeInclusionPattern, b, bytecodeApply[b.ordinal()], true);
                }
            }
            if (VMABX != null) {
                Pattern bytecodeExclusionPattern = Pattern.compile(VMABX);
                for (VMABytecodes b : VMABytecodes.values()) {
                    matchBytecode(bytecodeExclusionPattern, b, bytecodeApply[b.ordinal()], false);
                }
            }
        }

        if (VMATime != null) {
            if (VMATime.startsWith("wall")) {
                timeMode = VMATime.startsWith("wallns") ? VMATimeMode.WALLNS : VMATimeMode.WALLMS;
                if (VMATime.endsWith("abs")) {
                    timeMode.setAbsolute();
                }
            } else if (VMATime.startsWith("id")) {
                timeMode = VMATime.startsWith("ida") ? VMATimeMode.IDATOMIC : VMATimeMode.ID;
                if (VMATime.endsWith("abs")) {
                    timeMode.setAbsolute();
                }
            } else if (VMATime.equals("none")) {
                timeMode = VMATimeMode.NONE;
            } else {
                Log.println("VMA: unknown time mode: " + VMATime);
                MaxineVM.native_exit(1);
            }
        }

        logBytecodeApply();
    }

    private static void logBytecodeApply() {
        if (logger.enabled()) {
            for (VMABytecodes b : VMABytecodes.values()) {
                boolean[] state = bytecodeApply[b.ordinal()];
                logger.logBytecodeSetting(b, state[0], state[1]);
            }
        }
    }

    public static VMATimeMode getTimeMode() {
        return timeMode;
    }

    /**
     * Check if bytecode:advice combination is matched by the provided pattern.
     * @param pattern
     * @param b
     */
    private static void matchBytecode(Pattern pattern, VMABytecodes b, boolean[] state, boolean setting) {
        final String name = b.name();
        if (pattern.matcher(name).matches() || pattern.matcher(name + ":AB").matches()) {
            state[0] = setting;
            state[1] = setting;
        } else if (pattern.matcher(name + ":B").matches()) {
            state[0] = setting;
        } else if (pattern.matcher(name + ":A").matches()) {
            state[1] = setting;
        }
    }

    static boolean instrumentMethod(ClassMethodActor methodActor) {
        if (methodActor.holder().isReflectionStub()) {
            return false;
        }
        if (specificInstrumentChecker != null) {
            return specificInstrumentChecker.instrument(methodActor);
        }
        String matchName = methodActor.format("%H#%n(%p)");
        boolean include = methodInclusionPattern == null || methodInclusionPattern.matcher(matchName).matches();
        if (include) {
            include = !methodExclusionPattern.matcher(matchName).matches();
        }
        return include;
    }

    /**
     * Check if given method should be instrumented for advising.
     * @param cma
     */
    public static boolean instrumentForAdvising(ClassMethodActor cma) {
        boolean include = VMAJavaRunScheme.isInstrumenting();
        if (include) {
            include = instrumentMethod(cma);
        }
        if (logger.enabled()) {
            logger.logInstrument(cma, include);
        }
        return include;
    }

    public static boolean instrumentThread(VmThread vmThread) {
        String name = vmThread.getName();
        boolean include = threadInclusionPattern == null || threadInclusionPattern.matcher(name).matches();
        if (include) {
            include = threadExclusionPattern == null || !threadExclusionPattern.matcher(name).matches();
        }
        return include;
    }

    /**
     * Returns the template options for the given bytecode.
     * The result is a {@code boolean} array indexed by {@link AdviceMode#ordinal()}.
     * @param opcode
     */
    public static boolean[] getVMATemplateOptions(int opcode) {
        return bytecodeApply[opcode];
    }

    @VMLoggerInterface
    private interface VMALoggerInterface {
        void bytecodeSetting(@VMLogParam(name = "bytecode") VMABytecodes bytecode,
                             @VMLogParam(name = "before") boolean before, @VMLogParam(name = "after") boolean after);
        void instrument(@VMLogParam(name = "methodActor") ClassMethodActor methodActor, @VMLogParam(name = "include") boolean include);
        void jdkDeopt(@VMLogParam(name = "stage") String stage);
    }

    public static final VMALogger logger = new VMALogger();

    public static class VMALogger extends VMALoggerAuto {
        VMALogger() {
            super("VMA", "VMA operations");
        }

        @Override
        protected void traceInstrument(ClassMethodActor methodActor, boolean include) {
            Log.print("VMA: ");
            Log.print(methodActor);
            Log.print(" instrumented: ");
            Log.println(include);
        }

        @Override
        protected void traceBytecodeSetting(VMABytecodes bytecode, boolean before, boolean after) {
            if (before || after) {
                Log.print("VMA: ");
                Log.print(bytecode.name());
                Log.print(" setting: ");
                Log.print(before ? "BEFORE" : "");
                Log.print("/");
                Log.println(after ? "AFTER" : "");
            }
        }

        @Override
        protected void traceJdkDeopt(String stage) {
            Log.print("VMA: JDKDeopt: ");
            Log.println(stage);
        }

    }

// START GENERATED CODE
    private static abstract class VMALoggerAuto extends com.sun.max.vm.log.VMLogger {
        public enum Operation {
            BytecodeSetting, Instrument, JdkDeopt;

            @SuppressWarnings("hiding")
            public static final Operation[] VALUES = values();
        }

        private static final int[] REFMAPS = new int[] {0x0, 0x0, 0x1};

        protected VMALoggerAuto(String name, String optionDescription) {
            super(name, Operation.VALUES.length, optionDescription, REFMAPS);
        }

        @Override
        public String operationName(int opCode) {
            return Operation.VALUES[opCode].name();
        }

        @INLINE
        public final void logBytecodeSetting(VMABytecodes bytecode, boolean before, boolean after) {
            log(Operation.BytecodeSetting.ordinal(), vMABytecodesArg(bytecode), booleanArg(before), booleanArg(after));
        }
        protected abstract void traceBytecodeSetting(VMABytecodes bytecode, boolean before, boolean after);

        @INLINE
        public final void logInstrument(ClassMethodActor methodActor, boolean include) {
            log(Operation.Instrument.ordinal(), methodActorArg(methodActor), booleanArg(include));
        }
        protected abstract void traceInstrument(ClassMethodActor methodActor, boolean include);

        @INLINE
        public final void logJdkDeopt(String stage) {
            log(Operation.JdkDeopt.ordinal(), objectArg(stage));
        }
        protected abstract void traceJdkDeopt(String stage);

        @Override
        protected void trace(Record r) {
            switch (r.getOperation()) {
                case 0: { //BytecodeSetting
                    traceBytecodeSetting(toVMABytecodes(r, 1), toBoolean(r, 2), toBoolean(r, 3));
                    break;
                }
                case 1: { //Instrument
                    traceInstrument(toClassMethodActor(r, 1), toBoolean(r, 2));
                    break;
                }
                case 2: { //JdkDeopt
                    traceJdkDeopt(toString(r, 1));
                    break;
                }
            }
        }

        private static VMABytecodes toVMABytecodes(Record r, int argNum) {
            return VMABytecodes.VALUES[r.getIntArg(argNum)];
        }

        private static Word vMABytecodesArg(VMABytecodes enumType) {
            return Address.fromInt(enumType.ordinal());
        }
    }

// END GENERATED CODE

}

/*
 * Copyright (c) 2011, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.vma.options;

import static com.oracle.max.vm.ext.vma.VMABytecodes.*;
import static com.oracle.max.vm.ext.vma.options.VMAOptions.AdviceModeOption.*;

import java.util.regex.Pattern;

import com.oracle.max.vm.ext.vma.*;
import com.sun.max.program.*;
import com.sun.max.vm.Log;
import com.sun.max.vm.MaxineVM;
import com.sun.max.vm.VMOptions;
import com.sun.max.vm.actor.member.ClassMethodActor;

/**
 * Defines all the options that can be used to control the VMA system.
 *
 * Normally, VMA is enabled only at VM runtime {@link MaxineVM.Phase#RUNNING} through the {@code XX:VMA} style options.
 * However, it possible (in principle) to enable VMA in the boot image, using the corresponding {@code max.vma.*} properties
 * in which case the options apply to the classes compiled into the boot image. The question then is whether setting
 * the runtime options should be treated completely separately, thereby allowing different settings for dynamically
 * loaded classes, or whether the boot image options should be sticky, and the runtime options ignored. Currently
 * they are treated separately. Note that that the {@link #VMA} option is dominant. If it is not set, the other options
 * are ignored even if they are set.
 *
 */
public class VMAOptions {

    enum AdviceModeOption {
        B, A, BA;
    }

    static class BM {
        VMABytecodes bytecode;
        AdviceModeOption adviceModeOption;
        BM(VMABytecodes bytecode, AdviceModeOption adviceModeOption) {
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

    private static final BM[] LIFETIME_BM = new BM[] {
        new BM(NEW, A), new BM(NEWARRAY, A), new BM(ANEWARRAY, A),
        new BM(MULTIANEWARRAY, A), new BM(INVOKESPECIAL, A)
    };

    private static final BM[] READ_BM = compose(LIFETIME_BM, new BM[] {new BM(GETFIELD, B), new BM(GETSTATIC, B)});

    private static final BM[] WRITE_BM = compose(LIFETIME_BM, new BM[] {new BM(PUTFIELD, B), new BM(PUTSTATIC, B)});

    private static final BM[] MONITOR_BM = new BM[] {new BM(MONITORENTER, B), new BM(MONITOREXIT, B)};

    private static final BM[] METHOD_ENTRY_EXIT_BM = new BM[] {
        new BM(MENTRY, A),
        new BM(IRETURN, B), new BM(LRETURN, B), new BM(FRETURN, B),
        new BM(DRETURN, B), new BM(ARETURN, B), new BM(RETURN, B)
    };

    private static final BM[] BEFOREINVOKE_BM = new BM[] {
        new BM(INVOKEVIRTUAL, B), new BM(INVOKEINTERFACE, B),
        new BM(INVOKESTATIC, B), new BM(INVOKESPECIAL, B)
    };

    private static final BM[] AFTERINVOKE_BM = new BM[] {
        new BM(INVOKEVIRTUAL, A), new BM(INVOKEINTERFACE, A),
        new BM(INVOKESTATIC, A), new BM(INVOKESPECIAL, A)
    };

    private static final BM[] INVOKE_BM = compose(BEFOREINVOKE_BM, AFTERINVOKE_BM);

    enum StdConfig {
        LIFETIME("lifetime", LIFETIME_BM),
        READ("read", READ_BM),
        WRITE("write", WRITE_BM),
        MONITOR("monitor", MONITOR_BM),
        BEFOREINVOKE("beforeinvoke", BEFOREINVOKE_BM),
        AFTERINVOKE("afterinvoke", AFTERINVOKE_BM),
        INVOKE("invoke", INVOKE_BM),
        ENTRYEXIT("entryexit", METHOD_ENTRY_EXIT_BM);

        private String name;
        private BM[] bytecodesToApply;

        private StdConfig(String name, BM[] bytecodesToApply) {
            this.name = name;
            this.bytecodesToApply = bytecodesToApply;
        }
    }

    private static BM[] compose(BM[] a, BM[] b) {
        BM[] result = new BM[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    private static final String VMA_PKG_PATTERN = "com\\.oracle\\.max\\.vm\\.ext\\.vma\\..*";
    private static final String JDK_X_PATTERN = "sun.misc.FloatingDecimal";
    private static final String X_PATTERN = VMA_PKG_PATTERN + "|" + JDK_X_PATTERN;

    static {
        VMOptions.addFieldOption("-XX:", "VMA", "enable advising");
        VMOptions.addFieldOption("-XX:", "VMACI", "regex for classes to instrument");
        VMOptions.addFieldOption("-XX:", "VMACX", "regex for classes not to instrument");
        VMOptions.addFieldOption("-XX:", "VMABI", "regex for bytecodes to match");
        VMOptions.addFieldOption("-XX:", "VMABX", "regex for bytecodes to not match");
        VMOptions.addFieldOption("-XX:", "VMATrace", "trace instrumentation as methods are compiled");
        VMOptions.addFieldOption("-XX:", "VMAConfig", "use pre-defined configuration");
    }

    /**
     * {@link Pattern regex pattern} defining specific classes to instrument.
     * If this option is set, only these classes are instrumented, otherwise
     * all classes are instrumented.
     */
    private static String VMACI;
    /**
     * {@link Pattern regex pattern} defining specific classes to exclude from instrumentation.
     */
    private static String VMACX;

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

    private static Pattern classInclusionPattern;
    private static Pattern classExclusionPattern;

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
     * Trace the the instrumentation process.
     */
    private static boolean VMATrace;

    /**
     * {@code true} if and only if we are doing any kind of advising.
     */
    public static boolean VMA;

    /**
     * The {@link VMAdviceHandler} handler class name.
     */
    private static String handlerClassName;

     /**
     * Ultimately will have the same value as the @link {@link #VMA} option but is not set until the VM phase
     * is reached where advising can safely start.
     */
    private static boolean advising;

    /**
     * Properties that can be set to control advising in the boot image.
     * Values have the same syntax and semantics as the field options.
     */
    private static final String VMA_PROPERTY = "max.vma";
    private static final String VMA_CI_PROPERTY = "max.vma.ci";
    private static final String VMA_CX_PROPERTY = "max.vma.cx";
    private static final String VMA_BI_PROPERTY = "max.vma.bi";
    private static final String VMA_BX_PROPERTY = "max.vma.bx";
    private static final String VMA_TRACE_PROPERTY = "max.vma.trace";
    private static final String VMA_CONFIG_PROPERTY = "max.vma.config";

    /**
     * Not currently interpreted, but could allow a different template class to be built into the boot image.
     */
    private static final String VMA_TEMPLATES_PROPERTY = "max.vma.templates";


    /**
     * This property must be specified at boot image time.
     */
    private static final String VMA_HANDLER_CLASS_PROPERTY = "max.vma.handler";
    private static final String DEFAULT_HANDLER_CLASS = "com.oracle.max.vm.ext.vma.runtime.SyncLogVMAdviceHandler";

    private static boolean isImageBuilding(MaxineVM.Phase phase) {
        return phase == MaxineVM.Phase.BOOTSTRAPPING || phase == MaxineVM.Phase.COMPILING;
    }

    public static String getHandlerClassName() {
        assert handlerClassName != null;
        return handlerClassName;
    }

    /**
     * Check options.
     *
     * @param phase
     * @return true iff advising is enabled, i.e. {@link #VMA == true}
     */
    public static boolean initialize(MaxineVM.Phase phase) {
        if (isImageBuilding(phase)) {
            VMA = System.getProperty(VMA_PROPERTY) != null;
            VMACI = System.getProperty(VMA_CI_PROPERTY);
            VMACX = System.getProperty(VMA_CX_PROPERTY);
            VMABI = System.getProperty(VMA_BI_PROPERTY);
            VMABX = System.getProperty(VMA_BX_PROPERTY);
            VMATrace = System.getProperty(VMA_TRACE_PROPERTY) != null;
            VMAConfig = System.getProperty(VMA_CONFIG_PROPERTY);
            handlerClassName = System.getProperty(VMA_HANDLER_CLASS_PROPERTY);
            if (handlerClassName == null) {
                handlerClassName = DEFAULT_HANDLER_CLASS;
            }
        }
        // We execute the setup code below when isImageBuilding if and only if VMA was set in that phase.
        if ((isImageBuilding(phase) && VMA) || phase == MaxineVM.Phase.RUNNING) {
            // always exclude advising packages and key JDK classes
            String xPattern = X_PATTERN;
            if (VMACX != null) {
                xPattern += "|" + VMACX;
            }
            if (VMACI != null) {
                classInclusionPattern = Pattern.compile(VMACI);
            }
            classExclusionPattern = Pattern.compile(xPattern);

            if (VMAConfig != null) {
                String[] vmaConfigs = VMAConfig.split(",");
                for (String vmaConfig : vmaConfigs) {
                    StdConfig stdConfig = null;
                    for (StdConfig c : StdConfig.values()) {
                        if (c.name.equals(vmaConfig)) {
                            stdConfig = c;
                            break;
                        }
                    }
                    if (stdConfig == null) {
                        ProgramError.unexpected(vmaConfig + " is not a standard VMA configuration");
                    }
                    for (BM ab : stdConfig.bytecodesToApply) {
                        for (AdviceMode am : AdviceMode.values()) {
                            boolean isApplied = ab.isApplied(am);
                            if (isApplied) {
                                bytecodeApply[ab.bytecode.ordinal()][am.ordinal()] = isApplied;
                            }
                        }
                    }
                }
            } else {
                // setup default values
                for (VMABytecodes b : VMABytecodes.values()) {
                    for (AdviceMode am : AdviceMode.values()) {
                        bytecodeApply[b.ordinal()][am.ordinal()] = VMABI == null ? true : false;
                    }
                }

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
            if (VMATrace) {
                Log.println("VMA: bytecode advice settings");
                for (VMABytecodes b : VMABytecodes.values()) {
                    boolean[] state = bytecodeApply[b.ordinal()];
                    if (state[0] || state[1]) {
                        Log.println("  " + b.name() + ":" + (state[0] ? "BEFORE" : "") + "/" + (state[1] ? "AFTER" : ""));
                    }
                }
            }
            advising = VMA;
        }
        return VMA;
    }

    /**
     * Check if bytecode:advice combination is matched by the provided pattern.
     * @param pattern
     * @param b
     * @return
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


    /**
     * Check if given method should be instrumented for advising.
     * Currently limited to per class not per method.
     * @param cma
     * @return
     */
    public static boolean instrumentForAdvising(ClassMethodActor cma) {
        final String className = cma.holder().typeDescriptor.toJavaString();
        boolean include = false;
        if (advising) {
            include = classInclusionPattern == null || classInclusionPattern.matcher(className).matches();
            if (include) {
                include = !classExclusionPattern.matcher(className).matches();
            }
        }
        if (VMATrace) {
            Log.println("VMA: " + className + "." + cma.name() + " instrumented: " + include);
        }
        return include;
    }

    /**
     * Returns the template options for the given bytecode.
     * The result is a {@code boolean} array indexed by {@link AdviceMode#ordinal()}.
     * @param opcode
     * @return
     */
    public static boolean[] getVMATemplateOptions(int opcode) {
        return bytecodeApply[opcode];
    }

}

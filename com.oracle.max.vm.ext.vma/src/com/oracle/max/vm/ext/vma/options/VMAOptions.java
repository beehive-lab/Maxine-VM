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

import java.util.regex.Pattern;

import com.oracle.max.vm.ext.vma.*;
import com.sun.max.vm.Log;
import com.sun.max.vm.MaxineVM;
import com.sun.max.vm.VMOptions;
import com.sun.max.vm.actor.member.ClassMethodActor;

public class VMAOptions {
    private static final String VMA_PKG_PATTERN = "com\\.oracle\\.max\\.vm\\.ext\\.vma\\..*";
    private static final String JDK_X_PATTERN = "sun.misc.FloatingDecimal";
    private static final String X_PATTERN = VMA_PKG_PATTERN + "|" + JDK_X_PATTERN;

    static {
        VMOptions.addFieldOption("-XX:", "VMA", "enable advising");
        VMOptions.addFieldOption("-XX:", "VMACI", "regex for classes to instrument");
        VMOptions.addFieldOption("-XX:", "VMACX", "regex for classes not to instrument");
        VMOptions.addFieldOption("-XX:", "VMABI", "regex for bytecodes to match");
        VMOptions.addFieldOption("-XX:", "VMABX", "regex for bytecodes to not match");
        VMOptions.addFieldOption("-XX:", "VMATemplatesClass", "class defining VMA T1X templates");
        VMOptions.addFieldOption("-XX:", "VMATrace", "trace instrumentation");
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
     * all classes are instrumented.
     */
    private static String VMABI;
    /**
     * {@link Pattern regex pattern} defining specific bytecodes to exclude from instrumentation.
     */
    private static String VMABX;

    /**
     * Class that defines the VMA advice, by providing specific template source.
     */
    public static String VMATemplatesClass;

    private static Pattern classInclusionPattern;
    private static Pattern classExclusionPattern;

    private static boolean[] bytecodeApply = new boolean[VMABytecodes.values().length];

    private static boolean VMATrace;

    private static boolean advising;

    /**
     * {@code true} if and only if we are doing any kind of advising.
     */
    public static boolean VMA;


    /**
     * Property specifying the template source class for the image.
     */
    private static final String VMA_ADVICE_CLASS_PROPERTY = "max.vma.templates.class";

    /**
     * Properties that can be set to control advising in the boot image.
     * Values have the same syntax and semantics as the field options.
     */
    private static final String VMA_PROPERTY = "max.vma";
    private static final String VMA_CI_PROPERTY = "max.vma.ci";
    private static final String VMA_CX_PROPERTY = "max.vma.cx";
    private static final String VMA_BI_PROPERTY = "max.vma.bi";
    private static final String VMA_BX_PROPERTY = "max.vma.bx";

    /**
     * Check options.
     * @param phase
     * @return true iff advising is enabled, i.e. {@link #VMA == true}
     */
    public static boolean initialize(MaxineVM.Phase phase) {
        if (phase == MaxineVM.Phase.BOOTSTRAPPING) {
            // aka image building
            VMATemplatesClass = System.getProperty(VMA_ADVICE_CLASS_PROPERTY);

            VMA = System.getProperty(VMA_PROPERTY) != null;
            VMACI = System.getProperty(VMA_CI_PROPERTY);
            VMACX = System.getProperty(VMA_CX_PROPERTY);
            VMABI = System.getProperty(VMA_BI_PROPERTY);
            VMABX = System.getProperty(VMA_BX_PROPERTY);
        }
        if (phase == MaxineVM.Phase.BOOTSTRAPPING || phase == MaxineVM.Phase.COMPILING ||
                phase == MaxineVM.Phase.RUNNING) {
            // always exclude advising packages and key JDK classes
            String xPattern = X_PATTERN;
            if (VMACX != null) {
                xPattern += "|" + VMACX;
            }
            if (VMACI != null) {
                classInclusionPattern = Pattern.compile(VMACI);
            }
            classExclusionPattern = Pattern.compile(xPattern);

            for (VMABytecodes b : VMABytecodes.values()) {
                bytecodeApply[b.ordinal()] = VMABI == null ? true : false;
            }

            if (VMABI != null) {
                Pattern bytecodeInclusionPattern = Pattern.compile(VMABI);
                for (VMABytecodes b : VMABytecodes.values()) {
                    if (bytecodeInclusionPattern.matcher(b.name()).matches()) {
                        bytecodeApply[b.ordinal()] = true;
                    }
                }
            }
            if (VMABX != null) {
                Pattern bytecodeExclusionPattern = Pattern.compile(VMABX);
                for (VMABytecodes b : VMABytecodes.values()) {
                    if (bytecodeExclusionPattern.matcher(b.name()).matches()) {
                        bytecodeApply[b.ordinal()] = false;
                    }
                }
            }
            advising = VMA;
        }
        return VMA;
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

    public static boolean useVMATemplate(int opcode) {
        return bytecodeApply[opcode];
    }

}

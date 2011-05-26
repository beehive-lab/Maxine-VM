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

import com.sun.max.program.ProgramError;
import com.sun.max.vm.Log;
import com.sun.max.vm.MaxineVM;
import com.sun.max.vm.VMOptions;
import com.sun.max.vm.actor.member.ClassMethodActor;

public class VMAOptions {
    private static final String VMA_PKG_PATTERN = "com\\.oracle\\.max\\.vm\\.ext\\.vma\\..*";
    private static final String JDK_X_PATTERN = "sun.misc.FloatingDecimal";
    private static final String X_PATTERN = VMA_PKG_PATTERN + "|" + JDK_X_PATTERN;

    static {
        VMOptions.addFieldOption("-XX:", "VMACI", "regex for classes to instrument");
        VMOptions.addFieldOption("-XX:", "VMACX", "regex for classes not to instrument");
        VMOptions.addFieldOption("-XX:", "VMATrace", "trace instrumentation");
        VMOptions.addFieldOption("-XX:", "VMAMode", "instrumentation mode");
        VMOptions.addFieldOption("-XX:", "VMAAdviceClass", "class defining VMA advice");
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
     * Class that defines the VMA advice, by providing specific template source.
     */
    public static String VMAAdviceClass;

    private static Pattern inclusionPattern;
    private static Pattern exclusionPattern;

    private static boolean VMATrace;

    public static boolean trackReads;
    public static boolean trackWrites;
    public static boolean trackLifetime;
    public static boolean checkConst;

    /**
     * {@code true} if and only if we are doing any kind of advising.
     */
    public static boolean advising;


    /**
     * Value should be a comma separated set of strings indicating which analysis to enable, from:
     * <ul>
     * <li>life: track object lifetime, creation and gc.
     * <li>read: track reads of object fields and array elements (implies life).
     * <li>write: track writes of object fields and array elements (implies life).
     * <li>readwrite: read,write
     * <li>const: validate that {@link @CONSTANT} annotations are not violated.
     * <li>disable: no advising, equivalent to not setting the option
     * </ul>
     */
    private static String VMAMode;

    /**
     * A property that can be set to enable tracking to be compiled into the boot image.
     * Value has the same syntax and semantics as the {@link #VMAMode} field option.
     */
    private static final String VMA_MODE_PROPERTY = "max.vma.mode";
    private static final String VMA_CI_PROPERTY = "max.vma.ci";
    private static final String VMA_CX_PROPERTY = "max.vma.cx";
    private static final String VMA_ADVICE_CLASS_PROPERTY = "max.vma.advice.class";

    /**
     * Check options.
     * @param phase
     * @return true iff we are instrumenting anything, i.e. {@link #VMAMode} not {@code null}
     */
    public static boolean initialize(MaxineVM.Phase phase) {
        if (phase == MaxineVM.Phase.BOOTSTRAPPING) {
            // aka image building
            VMAMode = System.getProperty(VMA_MODE_PROPERTY);
            VMACI = System.getProperty(VMA_CI_PROPERTY);
            VMACX = System.getProperty(VMA_CX_PROPERTY);
            VMAAdviceClass = System.getProperty(VMA_ADVICE_CLASS_PROPERTY);
        }
        if (phase == MaxineVM.Phase.BOOTSTRAPPING || phase == MaxineVM.Phase.COMPILING ||
                phase == MaxineVM.Phase.RUNNING) {
            // always exclude tracking packages and key JDK classes
            String xPattern = X_PATTERN;
            if (VMACX != null) {
                xPattern += "|" + VMACX;
            }
            if (VMACI != null) {
                inclusionPattern = Pattern.compile(VMACI);
            }
            exclusionPattern = Pattern.compile(xPattern);

            advising = checkMode();
        }
        return advising;
    }


    /**
     * Check if given method should be instrumented for tracking.
     * Currently limited to per class not per method.
     * @param cma
     * @return
     */
    public static boolean instrumentForTracking(ClassMethodActor cma) {
        final String className = cma.holder().typeDescriptor.toJavaString();
        boolean include = false;
        if (advising) {
            include = inclusionPattern == null || inclusionPattern.matcher(className).matches();
            if (include) {
                include = !exclusionPattern.matcher(className).matches();
            }
        }
        if (VMATrace) {
            Log.println("VMA: " + className + "." + cma.name() + " instrumented: " + include);
        }
        return include;
    }

    private static boolean checkMode() {
        if (VMAMode == null) {
            return false;
        }
        final String[] parts = VMAMode.split(",");
        for (String part : parts) {
            if (part.equals("life")) {
                trackLifetime = true;
            } else if (part.equals("read")) {
                trackLifetime = true;
                trackReads = true;
            } else if (part.equals("write")) {
                trackLifetime = true;
                trackWrites = true;
            } else if (part.equals("readwrite")) {
                trackLifetime = true;
                trackReads = true;
                trackWrites = true;
            } else if (part.equals("const")) {
                checkConst = true;
            } else if (part.equals("disable")) {
                return false;
            } else {
                ProgramError.unexpected("unknown VMA option: " + part);
            }
        }
        return true;
    }


}

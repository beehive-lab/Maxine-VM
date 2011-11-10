/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.tele;

import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.target.*;

/**
 * Makes critical state information about code remotely inspectable.
 * <p>
 * Active only when VM is being inspected.
 */
public final class InspectableCompilationInfo {

    /**
     * Maximum number of chars that may be written by the inspector into the char array, identifying classes requiring
     * inspector notification upon method compilation.
     */
    public static final int BREAKPOINT_DESCRIPTORS_ARRAY_LENGTH = 500;

    /**
     * Array into which the Inspector writes space-terminated type descriptors of all classes that currently require inspector
     * notification upon method compilation.
     * <br>
     * Each time the Inspector writes into the array, it starts from the beginning and overwrites old data, and it writes into
     * the {@linkplain #breakpointClassDescriptorsCharCount char count field} the number of characters written.
     * <br>
     * Each time the Inspector writes into the array, it increments the {@linkplain #breakpointClassDescriptorsEpoch generation counter}
     * so that changes can be detected efficiently.
     *
     * @see #breakpointClassDescriptorsCharCount
     * @see #breakpointClassDescriptorsEpoch
     */
    @INSPECTED
    private static char[] breakpointClassDescriptorCharArray = new char[BREAKPOINT_DESCRIPTORS_ARRAY_LENGTH];

    /**
     * Field into which the Inspector writes the count of characters written into the array of class descriptors each
     * time that it does so.
     *
     * @see InspectableCompilationInfo#breakpointClassDescriptorCharArray
     */
    @INSPECTED
    private static int breakpointClassDescriptorsCharCount = 0;

    /**
     * Field into which the Inspector writes the number of times that it has written into the array of class descriptors each
     * time that it does so.
     *
     * @see #breakpointClassDescriptorCharArray
     */
    @INSPECTED
    private static int breakpointClassDescriptorsEpoch = 0;

    private static int lastRefreshedBreakpointClassDescriptorsEpoch = 0;

    /**
     * Type descriptors for all classes currently requiring inspector notification upon method compilation.
     * These are reconstructed from the char array
     * written by the Inspector each time the counter is noticed to have been incremented by the
     * Inspector.
     *
     * @see #breakpointClassDescriptorsEpoch
     * @see #breakpointClassDescriptorCharArray
     */
    private static String[] breakpointClassDescriptors = new String[0];

    /**
     * Reads contents of the char array into which the Inspector may have written a sequence of class type descriptors, each
     * terminated with a space character. Breaks these out into an array of strings, one type descriptor per element.
     * <br>
     * Only called when VM is being inspected.
     */
    private static void refreshBreakpointClassDescriptors() {
        if (breakpointClassDescriptorsCharCount < 0 || breakpointClassDescriptorsCharCount >= BREAKPOINT_DESCRIPTORS_ARRAY_LENGTH) {
            throw ProgramError.unexpected("InspectableCodeInfo: bad char count from inspector=" + breakpointClassDescriptorsCharCount);
        } else if (breakpointClassDescriptorsCharCount == 0) {
            breakpointClassDescriptors = new String[0];
        } else {
            final String descriptorString = new String(breakpointClassDescriptorCharArray, 0, breakpointClassDescriptorsCharCount);
//            System.out.println("BreakpointClassDescriptor string=\"" + descriptors + "\"");
            breakpointClassDescriptors = descriptorString.split(" ");
//            System.out.println("BreakpointClassDescriptors updated, count=" + breakpointClassDescriptors.length);
//            for (String descriptor : breakpointClassDescriptors) {
//                System.out.println("     \"" + descriptor + "\"");
//            }
        }
    }

    /**
     * Makes information inspectable concerning a method compilation event.
     * <br>
     * <strong>Note:</strong> further optimization here is possible if it is determined that the string
     * comparisons here (compare against every string in the list every time a method is compiled)
     * take too long.  It is hard to imagine improving unless a bottleneck is being created by
     * having a very large number of classes listed with bytecode breakpoints.
     *
     * @param method method involved in compilation event
     * @param targetMethod compilation just completed or {@code null} if this is a pre-compilation notification
     */
    public static void notifyCompilationEvent(ClassMethodActor method, TargetMethod targetMethod) {
        if (Inspectable.isVmInspected()) {
            if (breakpointClassDescriptorsEpoch > lastRefreshedBreakpointClassDescriptorsEpoch) {
                refreshBreakpointClassDescriptors();
                lastRefreshedBreakpointClassDescriptorsEpoch = breakpointClassDescriptorsEpoch;
            }
            if (breakpointClassDescriptors.length > 0) {
                final String typeDescriptorString = method.holder().typeDescriptor.string;
                for (String breakpointClassTypeDescriptor : breakpointClassDescriptors) {
                    if (breakpointClassTypeDescriptor.equals(typeDescriptorString)) {
                        // Match:  the method just compiled is in a class on the list of type descriptors written into the VM by the Inspector.
                        // Call the method that the Inspector is watching.
                        if (targetMethod == null) {
                            inspectableCompilationStarted(typeDescriptorString, method.name.string, method.descriptor.string);
                        } else {
                            inspectableCompilationCompleted(typeDescriptorString, method.name.string, method.descriptor.string, targetMethod);
                        }
                        break;
                    }
                }
            }
        }
    }

    /**
     * An empty method whose purpose is to be interrupted by the Inspector when it needs to monitor method compilation events
     * in the VM. The arguments are deliberately made simple so that they can be read with low-level mechanisms in the
     * Inspector. <br>
     * <strong>Important:</strong> The Inspector assumes that this method is loaded and compiled in the boot image and
     * that it will never be dynamically recompiled.
     *
     * @param holderType type description for class holding the method
     * @param methodName name of the the method
     * @param signature argument type descriptors for the method
     */
    @NEVER_INLINE
    @INSPECTED
    private static void inspectableCompilationStarted(String holderType, String methodName, String signature) {
    }

    /**
     * An empty method whose purpose is to be interrupted by the Inspector when it needs to monitor method compilation events
     * in the VM. The arguments are deliberately made simple so that they can be read with low-level mechanisms in the
     * Inspector. <br>
     * <strong>Important:</strong> The Inspector assumes that this method is loaded and compiled in the boot image and
     * that it will never be dynamically recompiled.
     *
     * @param holderType type description for class holding the method
     * @param methodName name of the the method
     * @param signature argument type descriptors for the method
     * @param targetMethod the result of the method compilation
     */
    @NEVER_INLINE
    @INSPECTED
    private static void inspectableCompilationCompleted(String holderType, String methodName, String signature, TargetMethod targetMethod) {
    }
}

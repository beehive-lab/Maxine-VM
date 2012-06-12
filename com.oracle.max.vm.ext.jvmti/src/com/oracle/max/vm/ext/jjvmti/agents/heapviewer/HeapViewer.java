/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.jjvmti.agents.heapviewer;

import static com.sun.max.vm.ext.jvmti.JVMTIConstants.*;

import java.util.Arrays;
import java.util.EnumSet;
import com.sun.max.vm.Log;
import com.sun.max.vm.MaxineVM;
import com.sun.max.vm.actor.holder.ClassActor;
import com.sun.max.vm.classfile.constant.SymbolTable;
import com.sun.max.vm.ext.jvmti.*;

/**
 * The standard JVMTI heap viewer demo using JJVMTI.
 */
public class HeapViewer extends NullJJVMTIStdAgentAdapter implements JJVMTICommon.HeapCallbacks {
    private boolean vmDeathCalled;
    private boolean dumpInProgress;
    private int totalCount;
    private static boolean showZero;

    private static class ClassDetails implements Comparable<ClassDetails> {
        String signature;
        int count;
        int space;
        ClassDetails(String signature) {
            this.signature = signature;
        }

        @Override
        public int compareTo(ClassDetails other) {
            if (space < other.space) {
                return -1;
            } else if (space > other.space) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    private HeapViewer() {
        JJVMTIStdAgentAdapter.register(this);
    }

    public static void onLoad(String agentArgs) {
        String[] args = agentArgs.split(",");
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("showzero")) {
                showZero = true;
            }
        }

        HeapViewer heapViewer = new HeapViewer();
        try {
            heapViewer.addCapabilities(EnumSet.of(JVMTICapabilities.E.CAN_GENERATE_GARBAGE_COLLECTION_EVENTS));
            heapViewer.setEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_DEATH, null);
            heapViewer.setEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_DATA_DUMP_REQUEST, null); // TODO
            // the heapIteration method must be compiled before iterateThroughHeap is called,
            // as allocation is disabled inside iterateThroughHeap
            ClassActor.fromJava(HeapViewer.class).findLocalClassMethodActor(
                    SymbolTable.makeSymbol("heapIteration"), null).makeTargetMethod();
        } catch (JJVMTIException ex) {
            fail("initialization error: " + JVMTIError.getName(ex.error));
        }
    }

    public synchronized void dataDumpRequest() {
        if (vmDeathCalled || dumpInProgress) {
            return;
        }
        dumpInProgress = true;
        /* Get all the loaded classes */
        Class<?>[] classes = getLoadedClasses();

        /* Setup an area to hold details about these classes */
        ClassDetails[] details = new ClassDetails[classes.length];
        for (int i = 0; i < classes.length; i++) {
            Class<?> klass = classes[i];
            /* Get and save the class signature */
            details[i] = new ClassDetails(getClassSignature(klass));
            /* Tag this class with ClassDetails instance */
            setTag(klass, details[i]);
        }
        totalCount = 0;
        /* Iterate through the heap and count up uses of classes */
        iterateThroughHeap(JVMTI_HEAP_FILTER_CLASS_UNTAGGED, null, this, null);

        /* Remove tags */
        for (Class<?> klass : classes) {
            setTag(klass, null);
        }
        /* Sort details by space used */
        Arrays.sort(details);

        /* Print out sorted table */
        System.out.printf("Heap View, Total of %d objects found.%n%n", totalCount);

        System.out.println("Space      Count      Class Signature");
        System.out.println("---------- ---------- ----------------------");

        for (int i = classes.length - 1; i >= 0; i--) {
            if (details[i].space == 0 && !showZero) {
                continue;
            }
            System.out.printf("%10d %10d %s\n",
                details[i].space, details[i].count, details[i].signature);
        }
        System.out.println("---------- ---------- ----------------------\n");

    }

    @Override
    public void vmDeath() {
        forceGarbageCollection();
        setEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_DATA_DUMP_REQUEST, null);
        dataDumpRequest();
        vmDeathCalled = true;
    }

    private static void fail(String message) {
        Log.println(message);
        MaxineVM.exit(-1);
    }

    @Override
    public int heapIteration(Object classTag, long size, Object objectTag,
            int length, Object userData) {
        if (classTag != null) {
            ClassDetails details = (ClassDetails) classTag;
            details.count++;
            details.space += size;
            totalCount++;
        }
        return JVMTI_VISIT_OBJECTS;
    }


}

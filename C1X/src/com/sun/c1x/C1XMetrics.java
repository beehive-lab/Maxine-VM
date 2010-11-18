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

import java.lang.reflect.*;
import java.util.*;

import com.sun.c1x.debug.*;


/**
 * This class contains a number of fields that collect metrics about compilation, particularly
 * the number of times certain optimizations are performed.
 *
 * @author Ben L. Titzer
 */
public class C1XMetrics {
    public static int CompiledMethods;
    public static int TargetMethods;
    public static int LocalValueNumberHits;
    public static int GlobalValueNumberHits;
    public static int ValueMapResizes;
    public static int InlinedFinalizerChecks;
    public static int MethodsFolded;
    public static int InlineForcedMethods;
    public static int InlineForbiddenMethods;
    public static int InlinedJsrs;
    public static int NullCheckIterations;
    public static int NullCheckEliminations;
    public static int NullChecksRedundant;
    public static int NullCheckIdsAssigned;
    public static int ZeroChecksRedundant;
    public static int DivideSpecialChecksRedundant;
    public static int StoreChecksRedundant;
    public static int ConditionalEliminations;
    public static int BlocksMerged;
    public static int BlocksSkipped;
    public static int BlocksDeleted;
    public static int DeadCodeEliminated;
    public static int ResolveCPEAttempts;
    public static int BytecodesCompiled;
    public static int CodeBytesEmitted;
    public static int SafepointsEmitted;
    public static int ExceptionHandlersEmitted;
    public static int DataPatches;
    public static int DirectCallSitesEmitted;
    public static int IndirectCallSitesEmitted;
    public static int HIRInstructions;
    public static int LiveHIRInstructions;
    public static int LIRInstructions;
    public static int LIRVariables;
    public static int LIRXIRInstructions;
    public static int LIRMoveInstructions;
    public static int LSRAIntervalsCreated;
    public static int LSRASpills;
    public static int LoadConstantIterations;
    public static int CodeBufferCopies;
    public static int UniqueValueIdsAssigned;
    public static int RedundantConditionals;
    public static int FrameStatesCreated;
    public static int FrameStateValuesCreated;

    public static void print() {
        printClassFields(C1XMetrics.class);

    }
    private static void printClassFields(Class<?> javaClass) {
        final String className = javaClass.getSimpleName();
        TTY.println(className + " {");
        for (final Field field : javaClass.getFields()) {
            printField(field, false);
        }
        TTY.println("}");
    }

    public static void printField(final Field field, boolean tabbed) {
        final String fieldName = String.format("%35s", field.getName());
        try {
            String prefix = tabbed ? "" : "    " + fieldName + " = ";
            String postfix = tabbed ? "\t" : "\n";
            if (field.getType() == int.class) {
                TTY.print(prefix + field.getInt(null) + postfix);
            } else if (field.getType() == boolean.class) {
                TTY.print(prefix + field.getBoolean(null) + postfix);
            } else if (field.getType() == float.class) {
                TTY.print(prefix + field.getFloat(null) + postfix);
            } else if (field.getType() == String.class) {
                TTY.print(prefix + field.get(null) + postfix);
            } else if (field.getType() == Map.class) {
                Map<?, ?> m = (Map<?, ?>) field.get(null);
                TTY.print(prefix + printMap(m) + postfix);
            } else {
                TTY.print(prefix + field.get(null) + postfix);
            }
        } catch (IllegalAccessException e) {
            // do nothing.
        }
    }

    private static String printMap(Map<?, ?> m) {
        StringBuilder sb = new StringBuilder();

        List<String> keys = new ArrayList<String>();
        for (Object key : m.keySet()) {
            keys.add((String) key);
        }
        Collections.sort(keys);

        for (String key : keys) {
            sb.append(key);
            sb.append("\t");
            sb.append(m.get(key));
            sb.append("\n");
        }

        return sb.toString();
    }

    private static void printField(String fieldName, long value) {
        TTY.print("    " + fieldName + " = " + value + "\n");
    }

    private static void printField(String fieldName, double value) {
        TTY.print("    " + fieldName + " = " + value + "\n");
    }
}


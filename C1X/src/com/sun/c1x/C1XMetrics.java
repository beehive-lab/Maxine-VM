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

/**
 * This class contains a number of fields that collect metrics about compilation, particularly
 * the number of times certain optimizations are performed.
 *
 * @author Ben L. Titzer
 */
public class C1XMetrics {
    public static int TargetMethods;
    public static int LocalValueNumberHits;
    public static int GlobalValueNumberHits;
    public static int ValueMapResizes;
    public static int InlinedFinalizerChecks;
    public static int FoldableMethodsRegistered;
    public static int MethodsFolded;
    public static int InlineForcedMethods;
    public static int InlineForbiddenMethods;
    public static int NullCheckIterations;
    public static int NullCheckEliminations;
    public static int NullChecksRedundant;
    public static int ZeroChecksRedundant;
    public static int EquivalentConstantsMerged;
    public static int EquivalentConstantsChecked;
    public static int ConditionalEliminations;
    public static int BlocksMerged;
    public static int BlocksSkipped;
    public static int BlocksDeleted;
    public static int DeadCodeEliminated;
    public static int ResolveCPEAttempts;
    public static int CodeBytesEmitted;
    public static int SafepointsEmitted;
    public static int ExceptionHandlersEmitted;
    public static int DataPatches;
    public static int DirectCallSitesEmitted;
    public static int IndirectCallSitesEmitted;

    /**
     * The number of LIR instructions that were allocated during compilation.
     */
    public static int NumberOfLIRInstructions;

    /**
     * The number of interval objects that were allocated during linear scan register allocation.
     */
    public static int LSRA_IntervalsCreated;

    /**
     * The number of spill slots required for linear scan register allocation.
     */
    public static int LSRA_NumberOfSpills;
}

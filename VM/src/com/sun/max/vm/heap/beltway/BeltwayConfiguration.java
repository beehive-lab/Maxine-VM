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

package com.sun.max.vm.heap.beltway;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;

/**
 * @author Christos Kotselidis
 */
public final class BeltwayConfiguration extends BeltwayHeapSchemeConfiguration {

    public static final int numberOfGCThreads = 1;

    private static int numberOfBelts;

    public static final boolean useTLABS = true;
    public static boolean useGCTlabs = false;
    public static boolean parallelScavenging = false;

    private static int[] percentagesOfUsableMemoryPerBelt;


    public void initializeBeltWayConfiguration(Address applicationHeapStartAddress, Size applicationMaxHeapSize, int numberOfBelts, int[] percentagesOfUsableMemoryPerBelt) {
        BeltwayConfiguration.applicationHeapStartAddress = applicationHeapStartAddress;
        BeltwayConfiguration.applicationHeapMaxSize = applicationMaxHeapSize;
        BeltwayConfiguration.numberOfBelts = numberOfBelts;
        BeltwayConfiguration.percentagesOfUsableMemoryPerBelt = percentagesOfUsableMemoryPerBelt;
    }

    public BeltwayConfiguration() {
        super(Address.zero(), Size.zero());
    }

    @INLINE
    public static void setNumberOfBelts(int numOfBelts) {
        numberOfBelts = numOfBelts;
    }

    @INLINE
    public static void setPercentageOfUsableMemoryPerFrame(int[] percent) {
        percentagesOfUsableMemoryPerBelt = percent;
    }

    @INLINE
    public static int getPercentOfUsableMemoryPerBelt(int indexOfBelt) {
        return percentagesOfUsableMemoryPerBelt[indexOfBelt];
    }

    @INLINE
    public static int getNumberOfBelts() {
        return numberOfBelts;
    }

}

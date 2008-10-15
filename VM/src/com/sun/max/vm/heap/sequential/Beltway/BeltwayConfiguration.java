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

package com.sun.max.vm.heap.sequential.Beltway;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.*;

/**
 * @author Christos Kotselidis
 */

public final class BeltwayConfiguration extends HeapSchemeConfiguration {

    public static final int _numberOfGCThreads = 1;

    private static int _numberOfBelts;

    public static final boolean _useTLABS = true;
    public static boolean _useGCTlabs = true;
    public static boolean _parallelScavenging = false;

    private static int[] _percentagesOfUsableMemoryPerBelt;


    public void initializeBeltWayConfiguration(Address applicationHeapStartAddress, Size applicationMaxHeapSize, int numberOfBelts, int[] percentagesOfUsableMemoryPerBelt) {
        _applicationHeapStartAddress = applicationHeapStartAddress;
        _applicationHeapMaxSize = applicationMaxHeapSize;
        _numberOfBelts = numberOfBelts;
        _percentagesOfUsableMemoryPerBelt = percentagesOfUsableMemoryPerBelt;
    }

    public BeltwayConfiguration() {
        super(Address.zero(), Size.zero());
    }

    @INLINE
    public static void setNumberOfBelts(int numOfBelts) {
        _numberOfBelts = numOfBelts;
    }

    @INLINE
    public static void setPercentageOfUsableMemoryPerFrame(int[] percent) {
        _percentagesOfUsableMemoryPerBelt = percent;
    }

    @INLINE
    public static int getPercentOfUsableMemoryPerBelt(int indexOfBelt) {
        return _percentagesOfUsableMemoryPerBelt[indexOfBelt];
    }

    @INLINE
    public static int getNumberOfBelts() {
        return _numberOfBelts;
    }

}

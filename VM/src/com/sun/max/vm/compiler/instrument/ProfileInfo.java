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
package com.sun.max.vm.compiler.instrument;

/**
 * This class collects profiling information of various kinds into a single place.
 * It implements a dense encoding for space-efficient recording of various types
 * of profiling information, including method entry count, location count, taken/not taken
 * branch counts, and receiver type and method profiles.
 *
 * Typically the instrumentation code that updates the underlying data is not synchronized,
 * so users of this information should be prepared for the occassional oddity (e.g.
 * branch taken + branch not taken != count of entry of block).
 *
 * This class only stores primitive profiling information in the form of ints--i.e. no Objects.
 * Therefore profiling receiver method types and method implementations can use approximations
 * (e.g. the low order bits of the code entrypoint address) or the ID of a type instead
 * of the type itself.
 *
 * @author Ben L. Titzer
 */
public class ProfileInfo {

    private static final byte METHOD_ENTRY  = 0;
    private static final byte BC_LOCATION   = 1;
    private static final byte BR_TAKEN      = 2;
    private static final byte BR_NOT_TAKEN  = 3;
    private static final byte RECVR_TYPE    = 4;
    private static final byte RECVR_METHOD  = 5;
    private static final byte RECVR_END     = 6;
    private static final byte RECVR_COUNT   = 7;

    private int[] indexData;
    private int[] data;

    public Integer getEntryCount() {
        return get(search(0, METHOD_ENTRY));
    }

    public Integer[] getTypeProfile(int bci) {
        return null;
    }

    public Integer[] getReceiverProfile(int bci) {
        return null;
    }

    public Integer[] getBranchCounts(int bci) {
        Integer taken = get(search(bci, BR_TAKEN));
        Integer notTaken = get(search(bci, BR_NOT_TAKEN));
        if (taken != null || notTaken != null) {
            return new Integer[] { taken, notTaken };
        }
        return null;
    }

    public int getLocationCount(int bci) {
        return get(search(bci, BC_LOCATION));
    }

    private Integer get(int index) {
        if (index >= 0 && index < data.length) {
            return data[index];
        }
        return null;
    }

    private int search(int bci, byte type) {
        // search for a specific type of data at a particular bci
        int index = search(bci);
        if (index >= 0) {
            int encoding = encodeType(bci, type);
            while (index < indexData.length && bciAt(index) == bci) {
                if (indexData[index] == encoding) {
                    return index;
                }
                index++;
            }
        }
        return -1;
    }

    private int search(int bci) {
        // perform binary search to find the lowest entry with data for the bci
        int low = 0;
        int high = indexData.length - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            int midVal = bciAt(mid);

            if (midVal < bci) {
                low = mid + 1;
            } else if (midVal > bci) {
                high = mid - 1;
            } else {
                return mid; // key found
            }
        }
        return -(low + 1);  // key not found.
    }

    private int encodeType(int bci, byte type) {
        return type | (bci << 16);
    }

    private byte typeAt(int index) {
        return (byte) indexData[index];
    }

    private int bciAt(int index) {
        return indexData[index] >>> 16;
    }

}

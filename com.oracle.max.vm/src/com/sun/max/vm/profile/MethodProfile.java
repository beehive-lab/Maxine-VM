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
package com.sun.max.vm.profile;

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.vm.actor.member.*;

/**
 * This class collects profiling information of various kinds into a single place.
 * It implements a dense encoding for space-efficient recording of various types
 * of profiling information, including method entry count, location count, taken/not taken
 * branch counts, and receiver type and method profiles.
 *
 * Typically the instrumentation code that updates the underlying data is not synchronized,
 * so users of this information should be prepared for the occasional oddity (e.g.
 * branch taken + branch not taken != count of entry of block).
 *
 * This class only stores primitive profiling information in the form of ints--i.e. no Objects.
 * Therefore profiling receiver method types and method implementations can use approximations
 * (e.g. the low order bits of the code entrypoint address) or the ID of a type instead
 * of the type itself.
 *
 * This class maintains the data sorted by bytecode index and therefore most operations to
 * receive information for a particular BCI take logarithmic time.
 */
public class MethodProfile {

    private static final byte METHOD_ENTRY  = 0;
    private static final byte BC_LOCATION   = 1;
    private static final byte BR_TAKEN      = 2;
    private static final byte BR_NOT_TAKEN  = 3;
    private static final byte RECVR_TYPE    = 4;
    private static final byte RECVR_METHOD  = 5;
    private static final byte RECVR_COUNT   = 6;
    private static final byte RECVR_NOT_FOUND = 7;

    public int entryCount;
    public boolean triggered;
    private int[] data; // records actual counts
    private int[] info; // records bci and type for each count entry

    public ClassMethodActor method;

    MethodProfile() {
    }

    /**
     * Gets the count at the method entrypoint, if it is available.
     * @return the count of the method entrypoint if available;
     * {@code null} if this profile info does not have such an entry
     */
    public Integer getEntryCount() {
        return get(search(0, METHOD_ENTRY));
    }

    /**
     * Gets the type profile of the specified bytecode index, if it is available.
     * The data is formatted as an array of integers, in pairs. The first integer in
     * a pair represents the ID of a type, and the second integer represents the number
     * of times that type was seen.
     * @param bci the bytecode index for which to get the information
     * @return an array of type id / count pairs;
     * {@code null} if this profile info does not have such an entry
     */
    public Integer[] getTypeProfile(int bci) {
        return extractPairs(bci, RECVR_TYPE);
    }

    /**
     * Gets the receiver method profile of the specified bytecode index, if it is available.
     * The data is formatted as an array of integers, in pairs. The first integer in
     * a pair represents the low 32 bits of the method entrypoint, and the second integer
     * represents the number of times that entrypoint was seen.
     * @param bci the bytecode index for which to get the information
     * @return an array of entrypoint / count pairs;
     * {@code null} if this profile info does not have such an entry
     */
    public Integer[] getReceiverProfile(int bci) {
        return extractPairs(bci, RECVR_METHOD);
    }

    /**
     * Gets the taken and not taken counts for a branch at the specified index, if they
     * are available.
     * @param bci the bytecode index for which to get the information
     * @return an array of length 2, with the first element representing the taken count,
     * if it is available ({@code null} otherwise), the second element representing the
     * not taken count, if it is available ({@code null} otherwise); {@code null} if there
     * is no information for this branch
     */
    public Integer[] getBranchCounts(int bci) {
        Integer taken = get(search(bci, BR_TAKEN));
        Integer notTaken = get(search(bci, BR_NOT_TAKEN));
        if (taken != null || notTaken != null) {
            return new Integer[] {taken, notTaken};
        }
        return null;
    }

    /**
     * Gets the count for a particular bytecode location, if it exists.
     * @param bci the bytecode index for which to get the information
     * @return the value of the counter at the specified index, if it is available;
     * {@code null} if there is no information for this bytecode location
     */
    public Integer getLocationCount(int bci) {
        return get(search(bci, BC_LOCATION));
    }

    /**
     * Provides access to the raw data of this method profile.
     * @return the int array that stores the data of this profile
     */
    @INLINE
    public final int[] rawData() {
        return data;
    }

    private Integer[] extractPairs(int bci, byte type) {
        int index = search(bci, type);
        int tinfo = encodeInfo(bci, type);
        int cinfo = encodeInfo(bci, RECVR_COUNT);
        int einfo = encodeInfo(bci, RECVR_NOT_FOUND);
        if (index >= 0) {
            List<Integer> list = new ArrayList<Integer>();
            for (; index < dataLength() - 1; index += 2) {
                if (infoAt(index + 1) == cinfo) {
                    if (infoAt(index) == tinfo) {
                        list.add(dataAt(index));
                        list.add(dataAt(index + 1));
                    } else if (infoAt(index) == einfo) {
                        list.add(0);
                        list.add(dataAt(index + 1));
                    }
                }
            }
            return list.toArray(new Integer[list.size()]);
        }
        return null;
    }

    private Integer get(int index) {
        if (index >= 0 && index < dataLength()) {
            return dataAt(index);
        }
        return null;
    }

    private int search(int bci, byte type) {
        // search for a specific type of data at a particular bci
        int index = search(bci);
        if (index >= 0) {
            int info = encodeInfo(bci, type);
            while (index < dataLength() && bciAt(index) == bci) {
                if (infoAt(index) == info) {
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
        int high = dataLength() - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            int midVal = bciAt(mid);

            if (midVal < bci) {
                low = mid + 1;
            } else if (midVal > bci) {
                high = mid - 1;
            } else {
                while (mid > 0) {
                    // rewind to the first entry for this bci
                    if (bciAt(mid - 1) != bci) {
                        return mid;
                    }
                    mid--;
                }
                return mid; // key found
            }
        }
        return -(low + 1);  // key not found.
    }

    private int dataLength() {
        return data.length;
    }

    private int infoAt(int index) {
        return info[index];
    }

    private int dataAt(int index) {
        return data[index];
    }

    private static int encodeInfo(int bci, byte type) {
        return type | (bci << 16);
    }

    private byte typeAt(int index) {
        return (byte) infoAt(index);
    }

    private int bciAt(int index) {
        return infoAt(index) >>> 16;
    }

    /**
     * This class implements a builder that collects the instrumentation created for a particular
     * method and then packs the information into a dense, sorted representation in the form of
     * a {@link MethodProfile}.
     */
    public static class Builder {
        private List<Integer> infoList = new ArrayList<Integer>();
        private List<Integer> dataList = new ArrayList<Integer>();
        private final MethodProfile mpo = new MethodProfile();
        private int lastBci = 0;

        public void addEntryCounter(int initialValue) {
            mpo.entryCount = initialValue;
        }

        public int addGotoCounter(int bci) {
            return add(bci, BR_TAKEN, 0);
        }

        public int addLocationCounter(int initialValue) {
            return add(0, BC_LOCATION, initialValue);
        }

        public int addBranchCounters(int bci) {
            add(bci, BR_TAKEN, 0);
            return add(bci, BR_NOT_TAKEN, 0) - 1;
        }

        public int addTypeProfile(int bci, int entries) {
            int index = infoList.size();
            for (int i = 0; i < entries; i++) {
                add(bci, RECVR_TYPE, 0);
                add(bci, RECVR_COUNT, 0);
            }
            add(bci, RECVR_NOT_FOUND, 0);
            add(bci, RECVR_COUNT, 0);
            return index;
        }

        public int addMethodProfile(int bci, int entries) {
            int index = infoList.size();
            for (int i = 0; i < entries; i++) {
                add(bci, RECVR_METHOD, 0);
                add(bci, RECVR_COUNT, 0);
            }
            add(bci, RECVR_NOT_FOUND, 0);
            add(bci, RECVR_COUNT, 0);
            return index;
        }

        @INLINE
        public final MethodProfile methodProfileObject() {
            return mpo;
        }

        public MethodProfile finish() {
            int size = infoList.size();
            if (size > 0) {
                int[] data = new int[size];
                int[] info = new int[size];
                for (int i = 0; i < size; i++) {
                    info[i] = infoList.get(i);
                }
                for (int i = 0; i < size; i++) {
                    data[i] = dataList.get(i);
                }
                mpo.info = info;
                mpo.data = data;
            }
            return mpo;
        }

        private int add(int bci, byte type, int value) {
            setLastBci(bci);
            infoList.add(encodeInfo(0, type));
            dataList.add(value);
            return infoList.size() - 1;
        }

        private void setLastBci(int bci) {
            if (bci < lastBci) {
                throw ProgramError.unexpected("Profiling information not added in increasing BCI order: " + bci);
            }
            lastBci = bci;
        }
    }
}

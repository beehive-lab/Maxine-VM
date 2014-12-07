/*
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.max.vm.actor.holder.ClassIDManager;
import com.sun.max.vm.compiler.target.*;

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

    private static final byte METHOD_ENTRY_COUNT                 = 0;
    private static final byte BC_LOCATION                        = 1;
    private static final byte BR_TAKEN_COUNT                     = 2;
    private static final byte BR_NOT_TAKEN_COUNT                 = 3;
    private static final byte TYPE_ID                            = 4;
    private static final byte METHOD_ID                          = 5;
    private static final byte TYPE_METHOD_COUNT                  = 6;
    private static final byte TYPE_COUNT                         = TYPE_METHOD_COUNT;
    private static final byte METHOD_COUNT                       = TYPE_METHOD_COUNT;
    private static final byte TYPE_NULL_SEEN_METHOD_UNUSED_COUNT = 7;
    private static final byte TYPE_NULL_SEEN_COUNT               = TYPE_NULL_SEEN_METHOD_UNUSED_COUNT;
    private static final byte METHOD_UNUSED_COUNT                = TYPE_NULL_SEEN_METHOD_UNUSED_COUNT;
    private static final byte SWITCH_CASE_COUNT                  = 8;
    private static final byte SWITCH_DEFAULT_COUNT               = 9;
    private static final byte EXCEPTION_SEEN_COUNT               = 10;

    private static final byte BR_TAKEN_INDEX = 0;
    private static final byte BR_NOT_TAKEN_INDEX = 1;

    public static final int UNDEFINED_TYPE_ID = ClassIDManager.NULL_CLASS_ID;
    public static final int UNDEFINED_METHOD_ID = -1;

    public static final int UNDEFINED_EXECUTION_COUNT = -1;
    public static final byte UNDEFINED_INDEX = -1;

    /**
     * The method that contains the instrumentation to increase counters in this profile.
     */
    public TargetMethod method;

    /**
     * The method invocation and backward branch counter. Decremented by profiling code.
     * This is a separate counter since even methods without heavy profiling need a simple
     * invocation counter to trigger recompilation.
     */
    public int entryBackedgeCount;

    /**
     * Records actual counts of a count entry.
     */
    private int[] data;

    /**
     * Records bci and type for each count entry.
     */
    private int[] info;

    /**
     * When {@code true} re-compilation is disabled.
     * This is used by JVMTI to prevent methods with JVMTI instrumentation from
     * being recompiled with the optimizing compiler (and so removing the instrumentation).
     */
    public boolean compilationDisabled;

    protected MethodProfile() {
    }

    /**
     * Gets the count at the method entrypoint, if it is available.
     * @return the count of the method entrypoint if available;
     * {@code null} if this profile info does not have such an entry
     */
    public Integer getEntryCount() {
        return get(search(0, METHOD_ENTRY_COUNT));
    }

    /**
     * Returns execution count for a given bci.
     */
    public int getExecutionCount(int bci) {

        // Calculate branch execution count
        Integer[] branchCounts = getBranchCounts(bci);
        if (branchCounts != null) {
            long totalCount = 0;
            Integer takenCount = branchCounts[MethodProfile.BR_TAKEN_INDEX];
            Integer notTakenCount = branchCounts[MethodProfile.BR_NOT_TAKEN_INDEX];
            totalCount += takenCount;
            if (notTakenCount != null) {
                totalCount += notTakenCount;
            }
            if (totalCount == 0) {
                return UNDEFINED_EXECUTION_COUNT;
            }
            if (totalCount > Integer.MAX_VALUE) {
                totalCount = Integer.MAX_VALUE;
            }
            return (int) totalCount;
        }

        // Calculate types execution count
        Integer[] orderedPairs = extractOrderedPairs(bci, TYPE_ID, TYPE_COUNT, TYPE_NULL_SEEN_COUNT);
        if (orderedPairs != null) {
            int pairs = orderedPairs.length / 2;
            long totalCount = 0;
            for (int i = 0; i < pairs - 1; i++) {
                if (orderedPairs[i * 2] != UNDEFINED_TYPE_ID) {
                    totalCount += orderedPairs[i * 2 + 1];
                }
            }
            totalCount += orderedPairs[(pairs - 1) * 2 + 1];
            if (totalCount == 0) {
                return UNDEFINED_EXECUTION_COUNT;
            }
            if (totalCount > Integer.MAX_VALUE) {
                totalCount = Integer.MAX_VALUE;
            }
            return (int) totalCount;
        }

        // Calculate switch execution count
        Integer[] switchProfile = getSwitchProfile(bci);
        if (switchProfile != null) {
            int arrayLength = switchProfile.length;
            long totalCount = 0;

            for (int i = 0; i < arrayLength; i++) {
                totalCount += switchProfile[i];
            }
            if (totalCount == 0) {
                return UNDEFINED_EXECUTION_COUNT;
            }
            if (totalCount > Integer.MAX_VALUE) {
                totalCount = Integer.MAX_VALUE;
            }
            return (int) totalCount;
        }

        // Undefined execution count
        return UNDEFINED_EXECUTION_COUNT;
    }

    /**
     * Returns an index of exception seen counter for a given bci.
     */
    public int getExceptionSeenProfileDataIndex(int bci) {
        return search(bci, EXCEPTION_SEEN_COUNT);
    }

    /**
     * Returns number of times exception was seen for a given bci.
     */
    public int getExceptionSeenCount(int bci) {
        Integer exceptionSeenCount = get(search(bci, EXCEPTION_SEEN_COUNT));
        if (exceptionSeenCount != null) {
            return exceptionSeenCount;
        }
        return UNDEFINED_EXECUTION_COUNT;
    }

    /**
     * Returns number of times null reference was seen for a given bci.
     */
    public int getNullSeenCount(int bci) {
        Integer nullSeenCount = get(search(bci, TYPE_NULL_SEEN_COUNT));
        if (nullSeenCount != null) {
            return nullSeenCount;
        }
        return UNDEFINED_EXECUTION_COUNT;
    }

    /**
     * Returns number of profiled types including anonymous type in type profile ordered pairs.
     */
    private int getProfiledTypesNum(Integer[] orderedPairs) {
        int pairs = orderedPairs.length / 2;
        int resSize = 0;
        for (int i = 0; i < pairs - 1; i++) {
            if (orderedPairs[i * 2] != UNDEFINED_TYPE_ID) {
                resSize++;
            }
        }
        if (orderedPairs[(pairs - 1) * 2 + 1] > 0) {
            resSize++;
        }

        return resSize;
    }

    /**
     * Returns number of profiled types including anonymous type for a given bci.
     */
    public int getProfiledTypesNum(int bci) {
        Integer[] orderedPairs = extractOrderedPairs(bci, TYPE_ID, TYPE_COUNT, TYPE_NULL_SEEN_COUNT);
        if (orderedPairs == null) {
            return 0;
        }
        return getProfiledTypesNum(orderedPairs);
    }

    /**
     * Gets the type profile of the specified bytecode index, if it is available.
     * The data is formatted as an array of integers, in pairs. The first integer in
     * a pair represents the ID of a type, and the second integer represents the number
     * of times that type was seen. The pair with id == {@link #UNDEFINED_TYPE_ID} represents integral
     * profile anonymous information for the rest of profiled types.
     * @param bci the bytecode index for which to get the information
     * @return an array of type id / count pairs;
     * {@code null} if this profile info does not have such an entry
     */
    public Integer[] getTypeProfile(int bci) {
        Integer[] orderedPairs = extractOrderedPairs(bci, TYPE_ID, TYPE_COUNT, TYPE_NULL_SEEN_COUNT);
        if (orderedPairs == null) {
            return null;
        }
        int pairs = orderedPairs.length / 2;
        int resSize = getProfiledTypesNum(orderedPairs);
        if (resSize == 0) {
            return null;
        }
        Integer[] typeProfile = new Integer[resSize * 2];
        int j = 0;
        for (int i = 0; i < pairs - 1; i++) {
            if (orderedPairs[i * 2] != UNDEFINED_TYPE_ID) {
                typeProfile[j * 2] = orderedPairs[i * 2];
                typeProfile[j * 2 + 1] = orderedPairs[i * 2 + 1];
                j++;
            }
        }
        if (orderedPairs[(pairs - 1) * 2 + 1] > 0) {
            typeProfile[j * 2] = UNDEFINED_TYPE_ID;
            typeProfile[j * 2 + 1] = orderedPairs[(pairs - 1) * 2 + 1];
        }
        return typeProfile;
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
    public Integer[] getMethodProfile(int bci) {
        return extractOrderedPairs(bci, METHOD_ID, METHOD_COUNT, METHOD_UNUSED_COUNT);
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
        Integer taken = get(search(bci, BR_TAKEN_COUNT));
        Integer notTaken = get(search(bci, BR_NOT_TAKEN_COUNT));
        if (taken != null || notTaken != null) {
            Integer[] branchCounts = new Integer[2];

            branchCounts[BR_TAKEN_INDEX] = taken;
            branchCounts[BR_NOT_TAKEN_INDEX] = notTaken;

            return branchCounts;
        }
        return null;
    }

    /**
     * Gets the probability for a branch at the specified index, if it is available.
     * @param bci the bytecode index for which to get the information
     * @return double value, if it is available ({@code -1} otherwise)
     */
    public double getBranchTakenProbability(int bci) {
        Integer[] branchCounts = getBranchCounts(bci);
        if (branchCounts == null) {
            return UNDEFINED_EXECUTION_COUNT;
        }
        Integer takenCount = branchCounts[MethodProfile.BR_TAKEN_INDEX];
        Integer notTakenCount = branchCounts[MethodProfile.BR_NOT_TAKEN_INDEX];

        if (takenCount != null) {
            assert takenCount >= 0;
            if (notTakenCount != null) {
                // Calculating branch probability.
                Long totalCount = (long) takenCount + (long) notTakenCount;
                assert notTakenCount >= 0;
                return totalCount <= 0 ? UNDEFINED_EXECUTION_COUNT : takenCount / totalCount.doubleValue();
            } else {
                // Calculating jump probability.
                return takenCount != 0 ? 1 : 0;
            }
        } else {
            assert notTakenCount == null;
            return UNDEFINED_EXECUTION_COUNT;
        }
    }

    /**
     * Gets the switch profile of the specified bytecode index, if it is available.
     * The data is formatted as an array of integers. The integers represent case
     * counts, and the last integer represents the default case.
     *
     * @param bci the bytecode index for which to get the information
     * @return an array of switch case counts;
     * {@code null} if this profile info does not have such an entry
     */
    public Integer[] getSwitchProfile(int bci) {
        return extractSingletons(bci, SWITCH_CASE_COUNT, SWITCH_DEFAULT_COUNT);
    }

    /**
     * Gets the probabilities for a switch at the specified index, if it is available.
     *
     * @param bci the bytecode index for which to get the information
     * @return array of double values for cases with default case as the last element,
     * if it is available ({@code null} otherwise)
     */
    public double[] getSwitchProbabilities(int bci) {
        Integer[] switchProfile = getSwitchProfile(bci);
        if (switchProfile == null) {
            return null;
        }

        int arrayLength = switchProfile.length;
        long switchCount = 0;
        double[] probabilities = new double[arrayLength];

        for (int i = 0; i < arrayLength; i++) {
            Integer caseCount = switchProfile[i];

            switchCount += caseCount;
            probabilities[i] = caseCount;
        }

        if (switchCount == 0) {
            return null;
        } else {
            for (int i = 0; i < arrayLength; i++) {
                probabilities[i] = probabilities[i] / switchCount;
            }
            return probabilities;
        }
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
     *
     * @return the int array that stores the data of this profile
     */
    @INLINE
    public final int[] rawData() {
        return data;
    }

    /**
     * Provides access to the raw type info of this method profile.
     *
     * @return the int array that stores the type info of this method profile
     */
    @INLINE
    public final int[] rawInfo() {
        return info;
    }

    private Integer[] extractSingletons(int bci, byte entryOrdinary, byte entryEndMarker) {
        int index = search(bci, entryOrdinary);
        int oinfo = encodeInfo(bci, entryOrdinary);
        int einfo = encodeInfo(bci, entryEndMarker);
        if (index >= 0) {
            List<Integer> list = new ArrayList<Integer>();
            for (; index < dataLength(); index += 1) {
                list.add(dataAt(index));
                if (infoAt(index) != oinfo) {
                    assert infoAt(index) == einfo;
                    break;
                }
            }
            return list.toArray(new Integer[list.size()]);
        }
        return null;
    }

    private Integer[] extractOrderedPairs(int bci, byte firstEntryOrdinary, byte secondEntry, byte firstEntryEndMarker) {
        int index = search(bci, firstEntryOrdinary);
        int tinfo = encodeInfo(bci, firstEntryOrdinary);
        int cinfo = encodeInfo(bci, secondEntry);
        int einfo = encodeInfo(bci, firstEntryEndMarker);
        if (index >= 0) {
            List<Integer> list = new ArrayList<Integer>();
            for (; index < dataLength() - 1; index += 2) {
                list.add(dataAt(index));
                list.add(dataAt(index + 1));
                assert infoAt(index + 1) == cinfo;
                if (infoAt(index) == einfo) {
                    break;
                } else {
                    assert infoAt(index) == tinfo;
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
        return UNDEFINED_EXECUTION_COUNT;
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

    public boolean protectedEntryCount() {
        return entryBackedgeCount <= MethodInstrumentation.protectionThreshold;
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
        public static final byte UNDEFINED_POS = -1;

        public void addEntryBackedgeCounter(int initialValue) {
            mpo.entryBackedgeCount = initialValue;
        }

        public int addGotoCounter(int bci) {
            return add(bci, BR_TAKEN_COUNT, 0);
        }

        public int addLocationCounter(int initialValue) {
            return add(0, BC_LOCATION, initialValue);
        }

        public int addBranchTakenCounters(int bci) {
            return addGotoCounter(bci);
        }

        public int addBranchNotTakenCounters(int bci) {
            return add(bci, BR_NOT_TAKEN_COUNT, 0);
        }

        public int addExceptionSeenCount(int bci) {
            return add(bci, EXCEPTION_SEEN_COUNT, 0);
        }

        public int addBranchProfile(int bci) {
            int index = infoList.size();
            addBranchTakenCounters(bci);
            addBranchNotTakenCounters(bci);
            return index;
        }

        public int addTypeProfile(int bci, int entries) {
            int index = infoList.size();
            for (int i = 0; i < entries; i++) {
                add(bci, TYPE_ID, MethodProfile.UNDEFINED_TYPE_ID);
                add(bci, TYPE_COUNT, 0);
            }
            add(bci, TYPE_NULL_SEEN_COUNT, 0);
            add(bci, TYPE_COUNT, 0);
            return index;
        }

        public int addMethodProfile(int bci, int entries) {
            int index = infoList.size();
            for (int i = 0; i < entries; i++) {
                add(bci, METHOD_ID, MethodProfile.UNDEFINED_METHOD_ID);
                add(bci, METHOD_COUNT, 0);
            }
            add(bci, METHOD_UNUSED_COUNT, 0);
            add(bci, METHOD_COUNT, 0);
            return index;
        }

        public int addSwitchProfile(int bci, int numberOfCases) {
            int index = infoList.size();
            for (int i = 0; i < numberOfCases; i++) {
                add(bci, SWITCH_CASE_COUNT, 0);
            }
            add(bci, SWITCH_DEFAULT_COUNT, 0);
            return index;
        }

        @INLINE
        public final MethodProfile methodProfileObject() {
            return mpo;
        }

        public MethodProfile finish(TargetMethod method) {
            mpo.method = method;
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
            infoList.add(encodeInfo(bci, type));
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

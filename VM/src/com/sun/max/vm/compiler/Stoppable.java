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
package com.sun.max.vm.compiler;

import java.lang.reflect.*;
import java.util.*;

import com.sun.max.program.*;
import com.sun.max.vm.compiler.b.c.*;
import com.sun.max.vm.compiler.cir.operator.*;

/**
 * Denotes an operation that may produce a <i>stop</i> in the translated code. A stop is a position in the translated
 * code where some metadata about the current execution state can be inspected. This may include a bytecode location, a
 * description of what target locations contain object references (i.e. a reference map for the frame and registers), and/or a
 * mapping from abstract JVM state (i.e. operand stack and locals) to target locations (i.e. native stack slots and
 * registers).
 *
 * @author Doug Simon
 */
public interface Stoppable {

    /**
     * Constant denoting no runtime checks.
     */
    int NONE = 0x00000000;

    /**
     * Constant denoting a runtime check that may result in a {@link NullPointerException}.
     */
    int NULL_POINTER_CHECK = 0x00000002;

    /**
     * Constant denoting a runtime check that may result in an {@link ArrayIndexOutOfBoundsException}.
     */
    int ARRAY_BOUNDS_CHECK = 0x00000004;

    /**
     * Constant denoting a runtime check that may result in an {@link ArrayStoreException}.
     */
    int ARRAY_STORE_CHECK = 0x00000008;

    /**
     * Constant denoting a runtime check that may result in an {@link ArithmeticException}.
     */
    int DIVIDE_BY_ZERO_CHECK = 0x00000010;

    /**
     * Constant denoting a runtime check that may result in a {@link ClassCastException}.
     */
    int CLASS_CAST_CHECK = 0x00000020;

    /**
     * Constant denoting a runtime check that may result in a {@link NegativeArraySizeException}.
     */
    int NEGATIVE_ARRAY_SIZE_CHECK = 0x00000040;

    /**
     * Constant denoting a safepoint operation.
     */
    int SAFEPOINT = 0x00000080;

    /**
     * Constant denoting an operation that makes a call. This is also used by {@link JavaOperator}s to indicate
     * if they are translated (or {@linkplain HCirOperatorLowering lowered}) to one or more snippet calls.
     */
    int CALL = 0x00000100;

    /**
     * Gets a value indicating the reasons this operation may be a stop.
     *
     * @return an integer encoding zero or more of the flags defined in {@link Stoppable}
     */
    int reasonsMayStop();

    public static final class Static {

        private static final Map<Integer, String> reasonToNameMap = new TreeMap<Integer, String>();
        static {
            for (Field field : Stoppable.class.getFields()) {
                if (field.getType().equals(int.class)) {
                    try {
                        final int reasonValue = field.getInt(null);
                        final String reasonName = field.getName();
                        reasonToNameMap.put(reasonValue, reasonName);
                    } catch (Exception exception) {
                        throw ProgramError.unexpected(exception);
                    }
                }
            }
        }

        public static final int ALL_REASONS_CAUSING_EXCEPTIONS =
            NULL_POINTER_CHECK |
            ARRAY_BOUNDS_CHECK |
            ARRAY_STORE_CHECK |
            NEGATIVE_ARRAY_SIZE_CHECK |
            DIVIDE_BY_ZERO_CHECK |
            CLASS_CAST_CHECK |
            CALL;

        public static final int ALL_REASONS =
            ALL_REASONS_CAUSING_EXCEPTIONS | SAFEPOINT;

        public static String reasonsMayStopToString(final int reasonsMayStop) {
            final StringBuilder sb = new StringBuilder();
            for (Map.Entry<Integer, String> entry : reasonToNameMap.entrySet()) {
                final int reason = entry.getKey();
                if ((reasonsMayStop & reason) != 0) {
                    sb.append(entry.getValue().toLowerCase() + " ");
                }
            }
            if (sb.length() > 0) {
                /* trim trailing space */
                sb.setLength(sb.length() - 1);
                return sb.toString();
            }
            return "";
        }

        public static String reasonsMayStopToString(Stoppable operation) {
            return reasonsMayStopToString(operation.reasonsMayStop());
        }

        /**
         * Determines if a given operation performs a null pointer check.
         *
         * @return {@code true} if {@code operation} can raise a {@link NullPointerException}.
         */
        public static boolean checksNullPointer(Stoppable operation) {
            return (operation.reasonsMayStop() & NULL_POINTER_CHECK) != 0;
        }

        /**
         * Determines if a given operation performs a check on the index used to address an element in an array.
         *
         * @return {@code true} if {@code operation} can raise a {@link ArrayIndexOutOfBoundsException}.
         */
        public static boolean checksArrayIndexAgainstBounds(Stoppable operation) {
            return (operation.reasonsMayStop() & ARRAY_BOUNDS_CHECK) != 0;
        }

        /**
         * Determines if a given operation performs a type check on an array store.
         *
         * @return {@code true} if {@code operation} can raise a {@link ArrayStoreException}.
         */
        public static boolean checksArrayStore(Stoppable operation) {
            return (operation.reasonsMayStop() & ARRAY_STORE_CHECK) != 0;
        }

        /**
         * Determines if a given operation performs a divide-by-zero check.
         *
         * @param operation an operation that may throw an exception
         * @return {@code true} if {@code operation} can raise a {@link ArithmeticException}.
         */
        public static boolean checksDivideByZero(Stoppable operation) {
            return (operation.reasonsMayStop() & DIVIDE_BY_ZERO_CHECK) != 0;
        }

        /**
         * Determines if a given operation performs a type check.
         *
         * @return {@code true} if {@code operation} can raise a {@link ClassCastException}.
         */
        public static boolean checksTypeCast(Stoppable operation) {
            return (operation.reasonsMayStop() & CLASS_CAST_CHECK) != 0;
        }

        /**
         * Determines if a given operation performs a check on the size used to create an array.
         *
         * @return {@code true} if {@code operation} can raise a {@link NegativeArraySizeException}.
         */
        public static boolean checksNegativeArraySize(Stoppable operation) {
            return (operation.reasonsMayStop() & NEGATIVE_ARRAY_SIZE_CHECK) != 0;
        }

        /**
         * Determines if a given operation is a call.
         */
        public static boolean isCall(Stoppable operation) {
            return (operation.reasonsMayStop() & CALL) != 0;
        }

        /**
         * Determines if a given operation is a safepoint.
         */
        public static boolean isSafepoint(Stoppable operation) {
            return (operation.reasonsMayStop() & SAFEPOINT) != 0;
        }

        /**
         * Determines if a given operation can produce a stop.
         *
         * @return {@code true} if {@code operation} can produce a stop
         */
        public static boolean canStop(Stoppable operation) {
            return operation.reasonsMayStop() != 0;
        }

        /**
         * Determines if a given operation can throw an exception.
         *
         * @return {@code true} if {@code operation} can raise any exception.
         */
        public static boolean canStopWithException(Stoppable operation) {
            return (operation.reasonsMayStop() & ALL_REASONS_CAUSING_EXCEPTIONS) != 0;
        }
    }
}

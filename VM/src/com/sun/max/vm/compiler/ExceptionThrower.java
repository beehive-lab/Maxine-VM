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


/**
 * Denotes an operation that may throw an exception during execution.
 *
 * @author Doug Simon
 */
public interface ExceptionThrower {

    /**
     * Constant denoting no exception type.
     */
    int NONE = 0x00000000;

    /**
     * Constant denoting any exception type.
     */
    int ANY = 0x00000001;

    /**
     * Constant denoting {@link NullPointerException}.
     */
    int NULL_POINTER_EXCEPTION = 0x00000002;

    /**
     * Constant denoting {@link ArrayIndexOutOfBoundsException}.
     */
    int ARRAY_INDEX_OUT_OF_BOUNDS_EXCEPTION = 0x00000003;

    /**
     * Constant denoting {@link ArrayStoreException}.
     */
    int ARRAY_STORE_EXCEPTION = 0x00000004;

    /**
     * Constant denoting {@link ArithmeticException}.
     */
    int ARITHMETIC_EXCEPTION = 0x00000005;

    /**
     * Constant denoting {@link ClassCastException}.
     */
    int CLASS_CAST_EXCEPTION = 0x00000006;

    /**
     * Constant denoting {@link NegativeArraySizeException}.
     */
    int NEGATIVE_ARRAY_SIZE_EXCEPTION = 0x00000007;

    /**
     * Gets a value indicating what exceptions this operation may throw.
     *
     * @return an integer encoding zero or more of the flags defined in {@link ExceptionThrower}. A return value of {@code 0}
     *         indicates that this operation does not throw any exceptions.
     */
    int thrownExceptions();

    public static final class Static {

        /**
         * Determines if a given operation can raise a {@link NullPointerException}.
         *
         * @param thrower an operation that may throw an exception
         * @return {@code true} if {@code thrower} can raise a {@link NullPointerException}.
         */
        public static boolean throwsNullPointerException(ExceptionThrower thrower) {
            return containsNullPointerException(thrower.thrownExceptions());
        }

        /**
         * Determines if a given operation can raise a {@link ArrayIndexOutOfBoundsException}.
         *
         * @param thrower an operation that may throw an exception
         * @return {@code true} if {@code thrower} can raise a {@link ArrayIndexOutOfBoundsException}.
         */
        public static boolean throwsArrayIndexOutOfBoundsException(ExceptionThrower thrower) {
            return containsArrayIndexOutOfBoundsException(thrower.thrownExceptions());
        }

        /**
         * Determines if a given operation can raise a {@link ArrayStoreException}.
         *
         * @param thrower an operation that may throw an exception
         * @return {@code true} if {@code thrower} can raise a {@link ArrayStoreException}.
         */
        public static boolean throwsArrayStoreException(ExceptionThrower thrower) {
            return containsArrayStoreException(thrower.thrownExceptions());
        }

        /**
         * Determines if a given operation can raise a {@link ArithmeticException}.
         *
         * @param thrower an operation that may throw an exception
         * @return {@code true} if {@code thrower} can raise a {@link ArithmeticException}.
         */
        public static boolean throwsArithmeticException(ExceptionThrower thrower) {
            return containsArithmeticException(thrower.thrownExceptions());
        }

        /**
         * Determines if a given operation can raise a {@link ClassCastException}.
         *
         * @param thrower an operation that may throw an exception
         * @return {@code true} if {@code thrower} can raise a {@link ClassCastException}.
         */
        public static boolean throwsCheckCastException(ExceptionThrower thrower) {
            return containsClassCastException(thrower.thrownExceptions());
        }

        /**
         * Determines if a given operation can raise a {@link NegativeArraySizeException}.
         *
         * @param thrower an operation that may throw an exception
         * @return {@code true} if {@code thrower} can raise a {@link NegativeArraySizeException}.
         */
        public static boolean throwsNegativeArraySizeException(ExceptionThrower thrower) {
            return containsNegativeArraySizeException(thrower.thrownExceptions());
        }

        /**
         * Determines if a given operation can raise any exception.
         *
         * @param thrower an operation that may throw an exception
         * @return {@code true} if {@code thrower} can raise any exception.
         */
        public static boolean throwsAny(ExceptionThrower thrower) {
            return !isEmpty(thrower.thrownExceptions());
        }

        /**
         * Determines if a given encoded set of thrown exceptions includes {@link NullPointerException}.
         *
         * @param thrownExceptions a value composed of zero or more of the flags defined in {@link ExceptionThrower}
         * @return {@code true} if {@code thrownExceptions} includes {@link #NULL_POINTER_EXCEPTION}
         */
        public static boolean containsNullPointerException(int thrownExceptions) {
            return (thrownExceptions & NULL_POINTER_EXCEPTION) != 0;
        }

        /**
         * Determines if a given encoded set of thrown exceptions includes {@link ArrayIndexOutOfBoundsException}.
         *
         * @param thrownExceptions a value composed of zero or more of the flags defined in {@link ExceptionThrower}
         * @return {@code true} if {@code thrownExceptions} includes {@link #ARRAY_INDEX_OUT_OF_BOUNDS_EXCEPTION}
         */
        public static boolean containsArrayIndexOutOfBoundsException(int thrownExceptions) {
            return (thrownExceptions & ARRAY_INDEX_OUT_OF_BOUNDS_EXCEPTION) != 0;
        }

        /**
         * Determines if a given encoded set of thrown exceptions includes {@link ArrayStoreException}.
         *
         * @param thrownExceptions a value composed of zero or more of the flags defined in {@link ExceptionThrower}
         * @return {@code true} if {@code thrownExceptions} includes {@link #ARRAY_STORE_EXCEPTION}
         */
        public static boolean containsArrayStoreException(int thrownExceptions) {
            return (thrownExceptions & ARRAY_STORE_EXCEPTION) != 0;
        }

        /**
         * Determines if a given encoded set of thrown exceptions includes {@link ArithmeticException}.
         *
         * @param thrownExceptions a value composed of zero or more of the flags defined in {@link ExceptionThrower}
         * @return {@code true} if {@code thrownExceptions} includes {@link #ARITHMETIC_EXCEPTION}
         */
        public static boolean containsArithmeticException(int thrownExceptions) {
            return (thrownExceptions & ARITHMETIC_EXCEPTION) != 0;
        }

        /**
         * Determines if a given encoded set of thrown exceptions includes {@link ClassCastException}.
         *
         * @param thrownExceptions a value composed of zero or more of the flags defined in {@link ExceptionThrower}
         * @return {@code true} if {@code thrownExceptions} includes {@link #CLASS_CAST_EXCEPTION}
         */
        public static boolean containsClassCastException(int thrownExceptions) {
            return (thrownExceptions & CLASS_CAST_EXCEPTION) != 0;
        }

        /**
         * Determines if a given encoded set of thrown exceptions includes {@link NegativeArraySizeException}.
         *
         * @param thrownExceptions a value composed of zero or more of the flags defined in {@link ExceptionThrower}
         * @return {@code true} if {@code thrownExceptions} includes {@link #NEGATIVE_ARRAY_SIZE_EXCEPTION}
         */
        public static boolean containsNegativeArraySizeException(int thrownExceptions) {
            return (thrownExceptions & NEGATIVE_ARRAY_SIZE_EXCEPTION) != 0;
        }

        /**
         * Determines if a given encoded set of thrown exceptions is empty.
         *
         * @param thrownExceptions a value composed of zero or more of the flags defined in {@link ExceptionThrower}
         * @return {@code thrownExceptions == 0}
         */
        public static boolean isEmpty(int thrownExceptions) {
            return thrownExceptions != 0;
        }
    }
}

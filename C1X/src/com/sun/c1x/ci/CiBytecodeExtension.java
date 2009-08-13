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
package com.sun.c1x.ci;

/**
 * The <code>CiBytecodeExtension</code> interface allows the runtime to extend the behavior of C1X
 * with extended bytecodes that are <i>not</i> part of the JVM spec. The behavior of existing Java
 * bytecodes <i>cannot</i> be altered. Extended bytecodes can implement
 * additional primitive operations and allow access to non-Java types. The interface is limited and does
 * not allow the addition of bytecodes that change control flow.
 *
 * @author Ben L. Titzer
 */
public interface CiBytecodeExtension {

    /**
     * This interface represents an instance of a particular extended bytecode.
     * It defines methods that allow the rest of the compiler to query the bytecode
     * for the purpose of parsing, optimization, and code generation.
     */
    public interface Bytecode {
        /**
         * Gets the length of this bytecode.
         * @return the length of this bytecode in bytes
         */
        public int length();

        /**
         * Gets the signature type of this bytecode, which expresses the changes to the
         * stack, including the input argument types and the output type.
         * @return a signature describing this bytecode
         */
        public CiSignature signatureType();

        /**
         * Checks whether this bytecode can trap--i.e. generate an exception.
         * @return {@code true} if this bytecode can generate an exception
         */
        public boolean canTrap();

        /**
         * For optimization only. Computes a value number for this bytecode, not considering its
         * inputs. If this bytecode cannot be value numbered, this method should return {@code 0}.
         * @return a non-zero value number for this bytecode if it can be value numbered; {@code 0} otherwise
         */
        public int valueNumber();
    }

    /**
     * Get the bytecode corresponding to the specified opcode at the specified bytecode index
     * in the specified bytecode.
     *
     * @param opcode the opcode
     * @param bci the bytecode index
     * @param code the bytecode array
     * @return an instance of {@link Bytecode} if this bytecode extension defines an extended
     * bytecode for this opcode in this code; {@code null} otherwise
     */
    public Bytecode getBytecode(int opcode, int bci, byte[] code);
}

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
package com.sun.cri.ci;

/**
 * Represents the result of compiling a method. The result can include a target method with machine code and metadata,
 * and/or statistics. If the compiler bailed out due to malformed bytecode, an internal error, or other cause, it will
 * supply the bailout object.
 *
 * @author Ben L. Titzer
 */
public class CiResult {
    private final CiTargetMethod targetMethod;
    private final CiBailout bailout;
    private final CiStatistics stats;

    /**
     * Creates a new compilation result.
     * @param targetMethod the method that was produced, if any
     * @param bailout the bailout condition that occurred
     * @param stats statistics about the compilation
     */
    public CiResult(CiTargetMethod targetMethod, CiBailout bailout, CiStatistics stats) {
        this.targetMethod = targetMethod;
        this.bailout = bailout;
        this.stats = stats;
    }

    /**
     * Gets the target method that was produced by this compilation. If no target method was
     * produced, but a bailout occured, then the bailout exception will be thrown at this point.
     * @return the target method produced
     * @throws {@link CiBailout} if a bailout occurred
     */
    public CiTargetMethod targetMethod() {
        if (bailout != null) {
            throw bailout;
        }
        return targetMethod;
    }

    /**
     * Returns the statistics about the compilation that were produced, if any.
     * @return the statistics
     */
    public CiStatistics statistics() {
        return stats;
    }

    /**
     * Returns the bailout condition that occurred for this compilation, if any.
     * @return the bailout
     */
    public CiBailout bailout() {
        return bailout;
    }
}

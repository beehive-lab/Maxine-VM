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
package com.sun.cri.xir;

import com.sun.cri.ci.*;
import com.sun.cri.ri.*;

/**
 * Encapsulates the notion of a site where XIR can be supplied. It is supplied to the {@link RiXirGenerator} by the
 * compiler for each place where XIR can be generated. This interface allows a number of queries, including the
 * bytecode-level location and optimization hints computed by the compiler.
 *
 * @author Ben L. Titzer
 */
public interface XirSite {

    /**
     * Gets the {@link CiCodePos code position} associated with this site. This is useful for inserting
     * instrumentation at the XIR level.
     * @return the code position if it is available; {@code null} otherwise
     */
    CiCodePos getCodePos();

    /**
     * Checks whether the specified argument is guaranteed to be non-null at this site.
     * @param argument the argument
     * @return {@code true} if the argument is non null at this site
     */
    boolean isNonNull(XirArgument argument);

    /**
     * Checks whether this site requires a null check.
     * @return {@code true} if a null check is required
     */
    boolean requiresNullCheck();

    /**
     * Checks whether this site requires a range check.
     * @return {@code true} if a range check is required
     */
    boolean requiresBoundsCheck();

    /**
     * Checks whether this site requires a read barrier.
     * @return {@code true} if a read barrier is required
     */
    boolean requiresReadBarrier();

    /**
     * Checks whether this site requires a write barrier.
     * @return {@code true} if a write barrier is required
     */
    boolean requiresWriteBarrier();

    /**
     * Checks whether this site requires an array store check.
     * @return {@code true} if an array store check is required
     */
    boolean requiresArrayStoreCheck();

    /**
     * Checks whether an approximation of the type for the specified argument is available.
     * @param argument the argument
     * @return an {@link RiType} indicating the most specific type known for the argument, if any;
     * {@code null} if no particular type is known
     */
    RiType getApproximateType(XirArgument argument);

    /**
     * Checks whether an exact type is known for the specified argument.
     * @param argument the argument
     * @return an {@link RiType} indicating the exact type known for the argument, if any;
     * {@code null} if no particular type is known
     */
    RiType getExactType(XirArgument argument);
}

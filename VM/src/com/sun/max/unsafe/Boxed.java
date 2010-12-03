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
package com.sun.max.unsafe;

import com.sun.max.annotate.*;
import com.sun.max.vm.*;

/**
 * Interface implemented by boxed versions of {@link Word} types.
 * These boxed implementations are used when executing in {@linkplain MaxineVM#isHosted() hosted} mode.
 *
 * A boxed type must be in the same package as its corresponding unboxed type and
 * its name must be composed by added the prefix "Boxed" to the name of the unboxed type.
 * This invariant enables the complete set of boxed types to be derived from the known
 * set of unboxed types.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
@HOSTED_ONLY
public interface Boxed {

    /**
     * Gets the boxed value as a {@code long}.
     */
    long value();
}

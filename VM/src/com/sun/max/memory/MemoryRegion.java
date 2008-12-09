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
package com.sun.max.memory;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;

/**
 * A region of memory in the {@link TeleVM}.
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public interface MemoryRegion {

    /**
     * @return address of the first location in the region.
     */
    @INLINE(override = true)
    Address start();

    /**
     * @return size of the region.
     */
    Size size();

    /**
     * @return address just past the last location in the region.
     */
    Address end();

    /**
     * @return does the region contain the address.
     */
    boolean contains(Address address);

    /**
     * @return does the region have any locations in common with another region.
     */
    boolean overlaps(MemoryRegion memoryRegion);

    /**
     * @return does the region have the same bounds as another region.
     */
    boolean sameAs(MemoryRegion memoryRegion);

    /**
     * @return an optional, short string that describes the role being played by the region, useful for debugging.
     */
    String description();

}

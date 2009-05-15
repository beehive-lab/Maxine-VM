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
package com.sun.max.tele.page;

import com.sun.max.unsafe.*;

/**
 *  Buffered reading/writing of bytes from/to a source/destination that can be identified by an {@link Address},
 *  and which has a natural page size and for which generations of modification can be articulated.
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public interface TeleIO extends DataIO {

    /**
     * @return the natural page size for the I/O source/destination
     */
    int pageSize();

    /**
     * @return the number of times the I/O source/destination has been modified.
     */
    long epoch();
}

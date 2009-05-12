/*
 * Copyright (c) 2008 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.max.util.timer;

import com.sun.max.profile.*;

/**
 * The most abstract interface for a timer.
 *
 * @author Ben L. Titzer
 */
public interface Timer {
    void start();
    void stop();
    Clock getClock();

    /**
     * Gets the number of {@linkplain Clock#getTicks() ticks} of this timer's {@linkplain #getClock() clock}
     * that occurred in between the last pair of calls to {@link #start()} and {@link #stop()}.
     */
    long getLastElapsedTime();

    long getLastNestedTime();
}

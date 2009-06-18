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
 * This class implements a timer that handles multiple threads using the same timer.
 *
 * @author Ben L. Titzer
 */
public class MultiThreadTimer implements Timer {

    private final Clock _clock;

    private ThreadLocal<SingleThreadTimer> _threadLocal = new ThreadLocal<SingleThreadTimer>() {
        @Override
        public SingleThreadTimer initialValue() {
            return new SingleThreadTimer(_clock);
        }
    };

    public MultiThreadTimer(Clock clock) {
        this._clock = clock;
    }

    public void start() {
        getTimer().start();
    }

    public void stop() {
        getTimer().stop();
    }

    public Clock getClock() {
        return _clock;
    }

    public long getLastElapsedTime() {
        return getTimer().getLastElapsedTime();
    }

    public long getLastNestedTime() {
        return getTimer().getLastNestedTime();
    }

    private SingleThreadTimer getTimer() {
        return _threadLocal.get();
    }
}

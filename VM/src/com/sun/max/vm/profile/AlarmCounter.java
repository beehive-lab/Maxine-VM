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
package com.sun.max.vm.profile;


/**
 * This class implements a basic counter that runs a particular routine when
 * the counter value reaches zero. By design, this class's increment() operation is not
 * thread safe, so in some situations it can lose some updates, and thus the
 * count should be considered a lower bound. Though this counter may lose some
 * updates, it will still reliably trigger the alarm given enough increment() operations.
 *
 * Note that even though this implementation is not thread-safe, it uses internal
 * synchronization on the slow path to guarantee that the Runnable will only be triggered once.
 *
 * @author Ben L. Titzer
 */
public class AlarmCounter extends Counter {

    /**
     * The threshold, or the number of times the increment operation can be applied before
     * triggering the alarm.
     */
    private int threshold;

    /**
     * Stores whether the runnable has been triggered, which is used to reliably trigger
     * the runnable only once.
     */
    private boolean triggered;

    /**
     * The runnable to run when the threshold is reached.
     */
    private final Runnable runnable;

    /**
     * Creates a new alarm counter with the specified threshold.
     * @param threshold the number of times this counter can be incremented before it will
     * call the runnable
     * @param runnable the runnable object to run when the threshold is reached
     */
    public AlarmCounter(int threshold, Runnable runnable) {
        super(threshold);
        this.threshold = threshold;
        this.runnable = runnable;
    }

    /**
     * This method simply increments the count and triggers the alarm if the count reaches the
     * threshold.
     */
    @Override
    public final void increment() {
        if (--value == 0) {
            triggerAlarm();
        }
    }

    /**
     * Called when the threshold is reached, this method runs the runnable only once.
     */
    private void triggerAlarm() {
        // trigger the runnable only once.
        synchronized (this) {
            if (!triggered) {
                triggered = true;
                runnable.run();
            }
        }
    }

    /**
     * Get the number of times the increment operation has been invoked.
     * @return the number of increments
     */
    public int getCount() {
        return threshold - value;
    }

    /**
     * Gets the threshold for this counter.
     * @return
     */
    public int getThreshold() {
        return threshold;
    }

    /**
     * Reset the increment count to {@code 0} and set a new threshold.
     * @param threshold
     */
    public void reset(int threshold) {
        synchronized (this) {
            this.threshold = threshold;
            value = threshold;
            this.triggered = false;
        }
    }

    /**
     * Reset the increment count to {@code 0} without altering the threshold.
     */
    @Override
    public void reset() {
        value = threshold;
        triggered = false;
    }
}

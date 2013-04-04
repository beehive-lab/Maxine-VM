/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.max.vm.ext.vma.run.java;

import java.util.concurrent.atomic.*;

/**
 * Many handlers want to associate a "time" with advice methods, for example
 * to determine object lifetime. This value doesn't have to be wall clock time,
 * although that is obviously a useful measure, so long as it increases
 * approximately in step with wall clock time.
 *
 * This enum provides several variants of time.
 */
public enum VMATimeMode {
    /**
     * For handlers that don't care about time.
     */
    NONE(false) {
        @Override
        public long getTime() {
            return 0;
        }
    },

    /**
     * Wall clock time in nanoseconds.
     */
    WALLNS(false) {
        @Override
        public long getTime() {
            return System.nanoTime();
        }
    },

    /**
     * Wall clock in milliseconds.
     */
    WALLMS (false){
        @Override
        public long getTime() {
            return System.currentTimeMillis();
        }
    },

    /**
     * Atomic, monotonically increasing, value.
     */
    IDATOMIC(true) {
        @Override
        public long getTime() {
            return atomicId.getAndIncrement();
        }

        @Override
        public long advance(long increment) {
            return atomicId.addAndGet(increment);
        }
    },

    /**
     * Non-atomic value. Multiple threads may receive the same value
     * from {@link #getTime}, but value will increase monotonically.
     */
    ID(true) {
        @Override
        public long getTime() {
            return id++;
        }

        @Override
        public long advance(long increment) {
            id += increment;
            return id;
        }

    };

    private static long id;
    private static AtomicLong atomicId = new AtomicLong();

    /**
     * Gets the current abstract time.
     * @return
     */
    public abstract long getTime();

    public final boolean canAdvance;

    private VMATimeMode(boolean canAdvance) {
        this.canAdvance = canAdvance;
    }

    /**
     * Optional method that advances the abstract time by {@code increment} and returns the new value.
     * @param time
     * @return
     */
    public long advance(long increment) {
        throw new UnsupportedOperationException();
    }


    /**
     * Support for the notion of absolute versus relative time.
     * Currently, this is simply a tag, the client is responsible for its interpretation.
     */
    private boolean absolute;

    public boolean isAbsolute() {
        return absolute;
    }

    public void setAbsolute() {
        absolute = true;
    }


}

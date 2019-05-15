/*
 * Copyright (c) 2017, 2019, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
 */
package com.sun.max.vm.monitor.modal.modehandlers.inflated;

import static com.sun.max.vm.intrinsics.MaxineIntrinsicIDs.*;

import com.sun.max.annotate.*;
import com.sun.max.platform.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.monitor.modal.modehandlers.*;
import com.sun.max.vm.monitor.modal.sync.*;
import com.sun.max.vm.reference.*;

/**
 * Abstracts access to an inflated lock word's bit fields.
 */
public class InflatedMonitorLockword extends HashableLockword {

    /*
     * Field layout (64 Bit) :
     *
     * bit [63............................... 1 0] Shape Binding Lock-state
     *     [     0    ][        hash       ][0][1] Inflated Unbound Unlocked
     *     [ Pointer to JavaMonitor object ][1][1] Inflated Bound Unlocked or locked
     *     [          Undefined            ][m][0] Lightweight
     *
     * Field layout (32 Bit) :
     *
     * bit [31.................................0] Shape Binding Lock-state
     *     [1/0][         ThreadId        ][0][1] Locking in-flight op ThreadId Inflated Unbound Unlocked
     *     [1/0][         ThreadId        ][1][1] Locking in-flight op ThreadId Inflated Bound Unlocked or locked
     *     [1/0][         ThreadId        ][m][0] Locking in-flight op ThreadId Lightweight
     */

    private static final Address MONITOR_MASK = Platform.target().arch.is64bit() ? Word.allOnes().asAddress().shiftedLeft(NUMBER_OF_MODE_BITS) : Word.allOnes().asAddress();

    @HOSTED_ONLY
    public InflatedMonitorLockword(long value) {
        super(value);
    }

    /**
     * Boxing-safe cast of a {@code Word} to a {@code InflatedMonitorLockword}.
     *
     * @param word the word to cast
     * @return the cast word
     */
    @INTRINSIC(UNSAFE_CAST)
    public static final InflatedMonitorLockword from(Word word) {
        return new InflatedMonitorLockword(word.value);
    }

    /**
     * Tests if the given lock word is an {@code InflatedMonitorLockword}.
     *
     * @param lockword the lock word to test
     * @return true if {@code lockword} is an {@code InflatedMonitorLockword}; false otherwise
     */
    @INLINE
    public static final boolean isInflatedMonitorLockword(ModalLockword lockword) {
        return InflatedMonitorLockword.from(lockword).isInflated();
    }

    /**
     * Tests if this {@code InflatedMonitorLockword} is bound to a {@code JavaMonitor}.
     *
     * @return true if bound, false otherwise
     */
    @INLINE
    public final boolean isBound() {
        return asAddress().isBitSet(MISC_BIT_INDEX);
    }

    /**
     * Returns a new {@code InflatedMonitorLockword} which is bound to the given {@code JavaMonitor} object.
     *
     * Note: The binding is only created one-way, i.e. the lock word points to the inflated monitor, but not the other
     * way-around.
     *
     * @param monitor the monitor to which the {@code InflatedMonitorLockword} should be bound
     * @return a new {@code InflatedMonitorLockword} which is bound to {@code monitor}
     */
    @INLINE
    public static final InflatedMonitorLockword boundFromMonitor(JavaMonitor monitor) {
        if (Platform.target().arch.is64bit()) {
            return from(Reference.fromJava(monitor).toOrigin().asAddress().bitSet(SHAPE_BIT_INDEX).bitSet(MISC_BIT_INDEX));
        } else {
            return from(Reference.fromJava(monitor).toOrigin().asAddress());
        }
    }

    @INLINE
    public static final InflatedMonitorLockword boundFromZero() {
        return InflatedMonitorLockword.from(HashableLockword.from(Address.zero()).asAddress().bitSet(SHAPE_BIT_INDEX).bitSet(MISC_BIT_INDEX));
    }

    /**
     * Gets the bound {@link JavaMonitor JavaMonitor} encoded into this lock word.
     *
     * @return this lock word's bound monitor
     */
    @INLINE
    public final JavaMonitor getBoundMonitor() {
        return (JavaMonitor) Reference.fromOrigin(asAddress().and(MONITOR_MASK).asPointer()).toJava();
    }

    /**
     * Returns a {@link Reference Reference} to the bound {@link JavaMonitor JavaMonitor} encoded into this lock word.
     *
     * @return this lock word's bound monitor
     */
    @INLINE
    public final Word getBoundMonitorReferenceAsWord() {
        return asAddress().and(MONITOR_MASK).asPointer();
    }

    /**
     * (Image build support) Returns a new, unbound {@code InflatedMonitorLockword} with the given hashcode installed
     * into the hashcode field.
     *
     * @param hashcode the hashcode to install
     * @return the lock word
     */
    @INLINE
    public static final InflatedMonitorLockword unboundFromHashcode(int hashcode) {
        if (Platform.target().arch.is64bit()) {
            return InflatedMonitorLockword.from(HashableLockword.from(Address.zero()).setHashcode(hashcode).asAddress().bitSet(SHAPE_BIT_INDEX));
        } else {
            return InflatedMonitorLockword.from(HashableLockword.from(Address.zero()).asAddress().bitSet(SHAPE_BIT_INDEX));
        }
    }

    @INLINE
    public static final InflatedMonitorLockword fromHashcode(int hashcode) {
        return InflatedMonitorLockword.from(HashableLockword.from(Address.zero()).setHashcode(hashcode).asAddress());
    }
}

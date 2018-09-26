/*
 * Copyright (c) 2018, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
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

/*
 * @Harness: java
 * @Runs: 0 = "0"; 1 = "1"; 2 = "2"; 42 = "42"
 */
package jtt.jdk;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

public class AtomicIntegerFieldUpdater01 {

    private static final AtomicIntegerFieldUpdater<AtomicIntegerFieldUpdater01> counterUpdater =
            AtomicIntegerFieldUpdater.newUpdater(AtomicIntegerFieldUpdater01.class, "counter");

    private volatile int counter;

    public static String test(int i) {
        AtomicIntegerFieldUpdater01 atomicIntegerFieldUpdater01 = new AtomicIntegerFieldUpdater01();
        counterUpdater.set(atomicIntegerFieldUpdater01, i);
        return Integer.toString(counterUpdater.get(atomicIntegerFieldUpdater01));
    }
}

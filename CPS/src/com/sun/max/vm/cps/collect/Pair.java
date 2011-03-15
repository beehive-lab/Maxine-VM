/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.collect;

import com.sun.max.*;

/**
 * Unordered pair of elements. Takes no precautions to detect cycles when computing {@link #hashCode()}s from its
 * elements.
 *
 * @author Michael Bebenitia
 */
public class Pair<First_Type, Second_Type> {

    public static <First_Type, Second_Type> Pair<First_Type, Second_Type> from(First_Type kind, Second_Type operation) {
        return new Pair<First_Type, Second_Type>(kind, operation);
    }

    private First_Type first;
    private Second_Type second;

    public First_Type first() {
        return first;
    }

    public Second_Type second() {
        return second;
    }

    public Pair(First_Type first, Second_Type second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public int hashCode() {
        return first.hashCode() ^ second.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        final Pair<First_Type, Second_Type> other = Utils.cast(obj);
        return (first == other.first || first.equals(other.first)) && (second == other.second || second.equals(other.second));
    }
}

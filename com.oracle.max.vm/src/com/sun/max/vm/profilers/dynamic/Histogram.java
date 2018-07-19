/*
 * Copyright (c) 2017, APT Group, School of Computer Science,
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

package com.sun.max.vm.profilers.dynamic;

import java.util.Hashtable;

public class Histogram {

    public Hashtable histogram;
    public int a;

    //constructor
    public Histogram() {
        this.a = 0;
        this.histogram = new Hashtable();

        /*
        this.histogram.put("Zara", new Double(3434.34));
        this.histogram.put("Mahnaz", new Double(123.22));
        this.histogram.put("Ayan", new Double(1378.00));
        this.histogram.put("Daisy", new Double(99.22));
        this.histogram.put("Qadir", new Double(-19.08));
        */
    }




}

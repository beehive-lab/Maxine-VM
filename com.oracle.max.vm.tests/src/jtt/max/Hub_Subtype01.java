/*
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
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
package jtt.max;

import com.sun.max.vm.actor.holder.*;

/*
 * @Harness: java
 * @Runs: 0=true; 1=true; 2=true; 3=true; 4=true; 5=false
 */
public class Hub_Subtype01 {
    @SuppressWarnings("all")
    public static boolean test(int arg) {
        Object obj = null;
        if (arg == 0) {
            obj = Hub_Subtype01.class;
        } else if (arg == 1) {
            obj = ClassActor.fromJava(Hub_Subtype01.class);
        } else if (arg == 2) {
            obj = ClassActor.fromJava(Hub_Subtype01.class).dynamicHub();
        } else if (arg == 3) {
            obj = ClassActor.fromJava(Hub_Subtype01.class).staticHub();
        } else if (arg == 4) {
            obj = ClassActor.fromJava(Hub_Subtype01.class).staticTuple();
        }
        // do an explicit checkcast for each of these Maxine implementation objects
        return obj instanceof Object;
    }
}

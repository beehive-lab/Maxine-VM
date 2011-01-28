/*
 * Copyright (c) 2010, 2010, Oracle and/or its affiliates. All rights reserved.
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
package test.output;

import java.lang.ref.*;
import java.util.*;

/**
 * This class is a variation of {@link WeakReferenceTest03} that doesn't use any
 * synchronized blocks in the code that nulls out the strong reference (i.e. variable 't').
 * The synchronization in this test is exposed when PrintStream.println() is inlined,
 * Synchronization blocks make C1X's liveness analysis quite conservative and effectively
 * undoes the nulling out of the strong reference.
 *
 * @author Doug Simon
 */
public class WeakReferenceTest03_01 {

    private static ReferenceQueue<T> refQueue = new ReferenceQueue<T>();
    private static int count;

    public static void main(String[] args) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            T t = new T();
            out.append("t = " + t + "\n");
            t = null;
            System.gc();
            NT q;
            while ((q = (NT) refQueue.poll()) == null) {
                // do nothing.
            }
            out.append("q = " + q + "\n");
        }
        System.out.println(out);
    }

    static class T {

        private NT nt;
        private int id = count++;

        T() {
            nt = new NT(this);
        }

        @Override
        public String toString() {
            return "a T #" + id;
        }
    }

    static class NT extends WeakReference<T> {

        private static List<NT> refList = new ArrayList<NT>();
        private int id = count++;

        NT(T t) {
            super(t, refQueue);
            refList.add(this);
        }

        @Override
        public String toString() {
            return "a T #" + id;
        }
    }
}

/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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
package test;

/**
 * Test for synchronized access to a non-local object.
 */
public class ThreadLocal03 extends Thread {
    private static Data object;

    private static class Data {
        void invoke() {

        }

    }

    private int id;

    ThreadLocal03(int id) {
        this.id = id;
        setName("ThreadLocal-" + id);
    }

    public static void main(String[] args) throws Exception {
        Thread writer = new ThreadLocal03(0);
        writer.start();
        writer.join();
        Thread reader = new ThreadLocal03(1);
        reader.start();
        reader.join();
        Thread invoker = new ThreadLocal03(2);
        invoker.start();
        invoker.join();
    }

    @Override
    public void run() {
        if (id == 0) {
            object = new Data();
        } else if (id == 1) {
            synchronized (object) {

            }
        } else if (id == 2) {
            object.invoke();
        }
    }

}

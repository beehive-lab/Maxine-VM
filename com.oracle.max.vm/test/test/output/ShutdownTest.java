/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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

public class ShutdownTest extends Thread {
    public static void main(String[] args) {
        boolean exit = false;
        for (String arg : args) {
            if (arg.equals("exit")) {
                exit = true;
            }
        }
        final Thread nonDaemon = new ShutdownTest();
        Runtime.getRuntime().addShutdownHook(new Thread(new MyHook()));
        nonDaemon.start();
        System.out.println(exit ? "explicit exit" : " implicit exit");
        if (exit) {
            System.exit(1);
        }
    }

    @Override
    public void run() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
        }
    }

    static class MyHook implements Runnable {
        public void run() {
            System.out.println("MyHook running");
        }
    }

}

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
package test.bench.net;

/**
 *
 * @author Puneeet Lakhina
 */
public class SocketTests {

    /**
     * @param args
     */
    public static void main(String[] args)throws Exception {
        if (args.length > 0) {
            for (String op : args) {
                if ("all".equalsIgnoreCase(op)) {
                    NewSocket.testall();
                } else if ("open".equalsIgnoreCase(op)) {
                    System.out.println("Starting open");
                    NewSocket.testOpen();
                    System.out.println("Completed open");
                    Thread.sleep(2000);
                } else if ("close".equalsIgnoreCase(op)) {
                    System.out.println("Starting close");
                    NewSocket.testClose();
                    System.out.println("Completed close");
                    Thread.sleep(2000);
                }
            }
        } else {
            NewSocket.test();
        }
    }

}

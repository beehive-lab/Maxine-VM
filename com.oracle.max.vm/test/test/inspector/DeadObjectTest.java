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
package test.inspector;


/**
 * Simple Inspector demonstration class, with focus on interaction of watchpoints and GC.
 */
public class DeadObjectTest {

    /**
     * @param args
     */
    @SuppressWarnings("unused")
    public static void main(String[] args) {
        SimpleObject aSimpleObject;

        aSimpleObject = makeObject1();
        aSimpleObject = makeObject2();

        char[] charArrayToDie = {'a', 'b', 'c'};
        charArrayToDie = null;

        System.gc();
        System.out.println("Object=" + aSimpleObject);

        System.out.println("Demo1 end");
    }

    private static SimpleObject makeObject1() {
        return new SimpleObject("this object will be collected");
    }

    private static SimpleObject makeObject2() {
        return new SimpleObject("this object will not be collected");
    }
    private static class SimpleObject {

        public SimpleObject(String text) {
            this.string = text;
        }

        public String string;
    }
}

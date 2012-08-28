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
 * Test for miscellaneous accesses to a non-local object.
 */
public class ThreadLocal03 extends Thread {
    private static Data data;
    private static Object object;
    private static int[] intArray;
    private static AnEnum anEnum;

    private static class Data {
        int value;
        void invoke() {

        }

        Data(int value) {
            this.value = value;
        }
    }

    private static enum AnEnum {
        VALUE1,
        VALUE2
    }

    /**
     * Poor man's enum to avoid object clutter in run.
     * @author Mick Jordan
     *
     */
    private static class ThreadType {
        static final int CREATE = 0;
        static final int SYNC = 1;
        static final int INVOKE = 2;
        static final int CAST = 3;
        static final int INSTANCEOF = 4;
        static final int IF = 5;
        static final int ARRAYREAD = 6;
        static final int ARRAYWRITE = 7;
        static final int FIELDREAD = 8;
        static final int FIELDWRITE = 9;
        static final int SWITCH = 10;
        static final int OPCOUNT = 11;
        static final int NOOP = -1;

        static String toString(int tt) {
            switch (tt) {
                // Checkstyle: stop
                case CREATE: return "CREATE";
                case SYNC: return "SYNC";
                case INVOKE: return "INVOKE";
                case CAST: return "CAST";
                case INSTANCEOF: return "INSTANCEOF";
                case IF: return "IF";
                case ARRAYREAD: return "ARRAYREAD";
                case ARRAYWRITE: return "ARRAYWRITE";
                case FIELDREAD: return "FIELDREAD";
                case FIELDWRITE: return "FIELDWRITE";
                case SWITCH: return "SWITCH";
                default: return "???";
                // Checkstyle: resume

            }
        }

        static int fromString(String s) {
            for (int i = 0; i < OPCOUNT; i++) {
                if (s.equals(toString(i))) {
                    return i;
                }
            }
            return NOOP;
        }
    }

    private final int tt;

    ThreadLocal03(int tt) {
        this.tt = tt;
        setName("ThreadLocal-" + ThreadType.toString(tt));
    }

    public static void main(String[] args) throws Exception {
        int tt = ThreadType.NOOP;
        for (int i = 0; i < args.length; i++) {
            final String arg = args[i];
            if (arg.equals("-tt")) {
                // Checkstyle: stop
                tt = ThreadType.fromString(args[++i]);
                // Checkstyle: resume
            }
        }
        runThreads(tt);
    }

    private static void runThreads(int xtt) throws Exception {
        for (int i = 0; i < ThreadType.OPCOUNT; i++) {
            if (i == ThreadType.CREATE || xtt == ThreadType.NOOP || xtt == i) {
                Thread thread = new ThreadLocal03(i);
                thread.start();
                thread.join();
            }
        }
    }

    @Override
    public void run() {
        // Checkstyle: stop
        switch (tt) {
            case ThreadType.CREATE:
                data = new Data(100);
                object = data;
                intArray = new int[1];
                anEnum = AnEnum.VALUE2;
                break;

            case ThreadType.SYNC:
                synchronized (object) {

                }
                break;

            case ThreadType.INVOKE:
                data.invoke();
                break;

            case ThreadType.CAST:
                @SuppressWarnings("unused")
                Data dataObject = (Data) object;
                break;

            case ThreadType.INSTANCEOF:
                if (object instanceof Data) {

                }
                break;

            case ThreadType.IF:
                if (object == null) {
                    @SuppressWarnings("unused")
                    int x = 1;
                }
                break;

            case ThreadType.ARRAYREAD: {
                int y = 0;
                @SuppressWarnings("unused")
                int x = intArray[y];
                break;
            }

            case ThreadType.ARRAYWRITE: {
                int y = 0;
                int x = 1;
                intArray[y] = x;
                break;
            }

            case ThreadType.FIELDREAD: {
                @SuppressWarnings("unused")
                int x = data.value;
                break;
            }

            case ThreadType.FIELDWRITE: {
                data.value = 200;
                break;
            }

            case ThreadType.SWITCH: {
                switch (anEnum) {
                    case VALUE2:
                        @SuppressWarnings("unused")
                        int x = anEnum.ordinal();
                        break;
                }
                break;
            }
        }
    }

}

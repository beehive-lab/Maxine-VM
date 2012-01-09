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
package com.sun.max.vm.log.java;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.log.*;
import com.sun.max.vm.log.VMLog.*;

/**
 * Common superclass for implementations using an indexed array of {@link Record} instances.
 */
public abstract class VMLogArray extends VMLog {

    @INSPECTED
    public final Record[] buffer;
    protected VMLogArray() {
        buffer = new Record[logEntries];
    }

    /*
     * Subclasses RecordN, that can hold N arguments, whose values can been read out by the Inspector.
     */

    public static class Record0 extends Record {
        @INSPECTED
        protected volatile int header;

        @Override
        public void setHeader(int header) {
            this.header = header;
        }

        @Override
        public int getHeader() {
            return header;
        }

    }

    public static class Record1 extends Record0 {
        @INSPECTED
        public Word arg1;

        @Override
        public Word getArg(int n) {
            // Checkstyle: stop
            switch (n) {
                case 1: return arg1;
                default: return argError();
            }
            // Checkstyle: resume
        }

        @Override
        public void setArgs(Word arg1) {
            this.arg1 = arg1;
        }
    }

    public static class Record2 extends Record1 {
        @INSPECTED
        public Word arg2;

        @Override
        public Word getArg(int n) {
            // Checkstyle: stop
            switch (n) {
                case 1: return arg1;
                case 2: return arg2;
                default: return argError();
            }
            // Checkstyle: resume
        }

        @Override
        public void setArgs(Word arg1, Word arg2) {
            this.arg1 = arg1;
            this.arg2 = arg2;
        }
    }

    public static class Record3 extends Record2 {
        @INSPECTED
        public Word arg3;
        @Override

        public Word getArg(int n) {
            // Checkstyle: stop
            switch (n) {
                case 1: return arg1;
                case 2: return arg2;
                case 3: return arg3;
                default: return argError();
            }
            // Checkstyle: resume
        }

        @Override
        public void setArgs(Word arg1, Word arg2, Word arg3) {
            this.arg1 = arg1;
            this.arg2 = arg2;
            this.arg3 = arg3;
        }
    }

    public static class Record4 extends Record3 {
        @INSPECTED
        public Word arg4;

        @Override
        public Word getArg(int n) {
            // Checkstyle: stop
            switch (n) {
                case 1: return arg1;
                case 2: return arg2;
                case 3: return arg3;
                case 4: return arg4;
                default: return argError();
            }
            // Checkstyle: resume
        }

        @Override
        public void setArgs(Word arg1, Word arg2, Word arg3, Word arg4) {
            this.arg1 = arg1;
            this.arg2 = arg2;
            this.arg3 = arg3;
            this.arg4 = arg4;
        }
    }

    public static class Record5 extends Record4 {
        @INSPECTED
        public Word arg5;

        @Override
        public Word getArg(int n) {
            // Checkstyle: stop
            switch (n) {
                case 1: return arg1;
                case 2: return arg2;
                case 3: return arg3;
                case 4: return arg4;
                case 5: return arg5;
                default: return argError();
            }
            // Checkstyle: resume
        }

        @Override
        public void setArgs(Word arg1, Word arg2, Word arg3, Word arg4, Word arg5) {
            this.arg1 = arg1;
            this.arg2 = arg2;
            this.arg3 = arg3;
            this.arg4 = arg4;
            this.arg5 = arg5;
        }
    }

    public static class Record6 extends Record5 {
        @INSPECTED
        public Word arg6;
        @Override
        public Word getArg(int n) {
            // Checkstyle: stop
            switch (n) {
                case 1: return arg1;
                case 2: return arg2;
                case 3: return arg3;
                case 4: return arg4;
                case 5: return arg5;
                case 6: return arg6;
                default: return argError();
            }
            // Checkstyle: resume
        }

        @Override
        public void setArgs(Word arg1, Word arg2, Word arg3, Word arg4, Word arg5, Word arg6) {
            this.arg1 = arg1;
            this.arg2 = arg2;
            this.arg3 = arg3;
            this.arg4 = arg4;
            this.arg5 = arg5;
            this.arg6 = arg6;
        }
    }

    public static class Record7 extends Record6 {
        @INSPECTED
        public Word arg7;

        @Override
        public Word getArg(int n) {
            // Checkstyle: stop
            switch (n) {
                case 1: return arg1;
                case 2: return arg2;
                case 3: return arg3;
                case 4: return arg4;
                case 5: return arg5;
                case 6: return arg6;
                case 7: return arg7;
                default: return argError();
            }
            // Checkstyle: resume
        }

        @Override
        public void setArgs(Word arg1, Word arg2, Word arg3, Word arg4, Word arg5, Word arg6, Word arg7) {
            this.arg1 = arg1;
            this.arg2 = arg2;
            this.arg3 = arg3;
            this.arg4 = arg4;
            this.arg5 = arg5;
            this.arg6 = arg6;
            this.arg7 = arg7;
        }
    }


}

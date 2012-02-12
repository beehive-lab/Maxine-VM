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
package com.sun.max.vm.log.hosted;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.log.*;

/**
 * A {@link VMLog} implementation for {@link MaxineVM#isHosted() hosted} mode. Similar to {@link VMLogArrayFixed}.
 *
 * N.B. since this is only used for tracing, we do not need to keep many records (in principle). However,
 * currently we use the same number specified for the target VM.
 */
@HOSTED_ONLY
public class VMLogHosted extends VMLog {
    public final HostedLogRecord[] buffer;

    public VMLogHosted() {
        buffer = new HostedLogRecord[logEntries];
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = new HostedLogRecord();
        }
    }

    public static class HostedLogRecord extends Record {
        protected int header;
        protected int id;
        protected Word[] args;

        @Override
        public void setHeader(int header) {
            this.header = header;
        }

        @Override
        public int getHeader() {
            return header;
        }

        public int getId() {
            return id;
        }

        @Override
        public Word getArg(int n) {
            return args[n - 1];
        }

        void setVarArgs(Word... args) {
            if (this.args == null || this.args.length < args.length) {
                this.args = new Word[args.length];
            }
            System.arraycopy(args, 0, this.args, 0, args.length);
        }

        @Override
        public void setArgs(Word arg1) {
            setVarArgs(arg1);
        }

        @Override
        public void setArgs(Word arg1, Word arg2) {
            setVarArgs(arg1, arg2);
        }

        @Override
        public void setArgs(Word arg1, Word arg2, Word arg3) {
            setVarArgs(arg1, arg2, arg3);
        }

        @Override
        public void setArgs(Word arg1, Word arg2, Word arg3, Word arg4) {
            setVarArgs(arg1, arg2, arg3, arg4);
        }

        @Override
        public void setArgs(Word arg1, Word arg2, Word arg3, Word arg4, Word arg5) {
            setVarArgs(arg1, arg2, arg3, arg4, arg5);
        }

        @Override
        public void setArgs(Word arg1, Word arg2, Word arg3, Word arg4, Word arg5, Word arg6) {
            setVarArgs(arg1, arg2, arg3, arg4, arg5, arg6);
        }

        @Override
        public void setArgs(Word arg1, Word arg2, Word arg3, Word arg4, Word arg5, Word arg6, Word arg7) {
            setVarArgs(arg1, arg2, arg3, arg4, arg5, arg6, arg7);
        }

        @Override
        public void setArgs(Word arg1, Word arg2, Word arg3, Word arg4, Word arg5, Word arg6, Word arg7, Word arg8) {
            setVarArgs(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);
        }

        @Override
        public String toString() {
            if (VMLog.Record.isFree(header)) {
                return "free";
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("id=");
                sb.append(id);
                sb.append(",lid=");
                sb.append(VMLog.Record.getLoggerId(header));
                sb.append(",th=");
                sb.append(VMLog.Record.getThreadId(header));
                sb.append(",op=");
                sb.append(VMLog.Record.getOperation(header));
                sb.append(",ac");
                sb.append(VMLog.Record.getArgCount(header));
                return sb.toString();
            }
        }

    }

    @Override
    protected Record getRecord(int argCount) {
        int myId = getUniqueId();
        HostedLogRecord r = buffer[myId % logEntries];
        r.id = myId;
        return r;
    }


    @Override
    public boolean setThreadState(boolean value) {
        return true;
    }

    @Override
    public boolean threadIsEnabled() {
        return true;
    }
}

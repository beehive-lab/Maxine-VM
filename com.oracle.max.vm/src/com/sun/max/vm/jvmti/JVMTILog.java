/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.jvmti;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.thread.*;

/**
 * A circular buffer of logged JVMTI upcalls.
 * Not every call is subject to logging, see {@link JVMTIFunctionsSource} for {@code LOG} comments.
 *
 */
public class JVMTILog {

    public static class Record {
        @INSPECTED
        public int op;
        @INSPECTED
        public int threadId;
        @INSPECTED
        public int id;
        @INSPECTED
        public Word arg1;

        @HOSTED_ONLY
        public Record(int op, int threadId, int startIndex, Word arg1) {
            this.op = op;
            this.threadId = threadId;
            this.id = startIndex;
            this.arg1 = arg1;
        }

        private Record() {

        }

    }

    @INSPECTED
    private static JVMTILog singleton = new JVMTILog();

    @INSPECTED
    public final Record[] buffer = new Record[8192];
    @INSPECTED
    private volatile int nextId;

    private static final int nextIdOffset = ClassActor.fromJava(JVMTILog.class).findLocalInstanceFieldActor("nextId").offset();

    public JVMTILog() {
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = new Record();
        }
    }

    static void add(int op, Word arg1) {
        int myId = singleton.nextId;
        while (Reference.fromJava(singleton).compareAndSwapInt(nextIdOffset, myId, myId + 1) != myId) {
            myId = singleton.nextId;
        }
        Record record = singleton.buffer[myId % 8192];
        record.op = op;
        record.arg1 = arg1;
        record.threadId = VmThread.current().id();
        record.id = myId;
    }

}

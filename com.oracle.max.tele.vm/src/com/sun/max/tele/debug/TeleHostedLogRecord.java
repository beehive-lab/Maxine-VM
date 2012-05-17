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
package com.sun.max.tele.debug;

import com.sun.max.unsafe.*;
import com.sun.max.vm.log.hosted.*;


public class TeleHostedLogRecord extends VMLogHosted.HostedLogRecord implements Comparable<TeleHostedLogRecord> {

    public TeleHostedLogRecord(int id, int header, Word... args) {
        this.id = id;
        this.header = header;
        this.args = args;
    }

    /**
     * For when we can't access VM but need to create a record.
     */
    public TeleHostedLogRecord() {
        header = 0;
        id = 0;
        args = new Word[0];
    }

    @Override
    public void setHeader(int header) {
        assert false;
    }

    public int compareTo(TeleHostedLogRecord other) {
        if (id < other.id) {
            return -1;
        } else if (id > other.id) {
            return 1;
        } else {
            return 0;
        }
    }
}

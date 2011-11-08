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
package com.sun.max.tele.debug.darwin;

import java.io.*;
import java.nio.*;


/**
 * Access to Mach_O 64-bit format for Darwin core dumps.
 *
 */
public class DarwinMachO {

    public static class Header {
        public int magic;
        public int cputype;
        public int cpusubtype;
        public int filetype;
        public int ncmds;
        public int sizeofcmds;
        public int flags;
        public int reserved;

        public static Header read(RandomAccessFile in) throws IOException {
            Header result = new Header();
            result.magic = readInt(in);
            result.cputype = readInt(in);
            result.cpusubtype = readInt(in);
            result.filetype = readInt(in);
            result.ncmds = readInt(in);
            result.sizeofcmds = readInt(in);
            result.flags = readInt(in);
            readInt(in); // skip reserved
            return result;
        }
    }

    public static class LoadCommand {
        public static final int LC_SEGMENT_64 = 0x19;
        public static final int LC_THREAD = 0x4;

        public int cmd;
        public int cmdsize;

        LoadCommand(int cmd, int cmdsize) {
            this.cmd = cmd;
            this.cmdsize = cmdsize;
        }

        LoadCommand(RandomAccessFile in) throws IOException {
            this.cmd = readInt(in);
            this.cmdsize = readInt(in);
        }

        public static LoadCommand read(RandomAccessFile in) throws IOException {
            LoadCommand result = null;
            final long ptr = in.getFilePointer();
            final int cmd = readInt(in);
            final int cmdsize = readInt(in);
            in.seek(ptr);
            switch (cmd) {
                case LC_SEGMENT_64:
                    result = Segment64LoadCommand.read(in);
                    break;
                case LC_THREAD:
                    result = ThreadLoadCommand.read(in);
                    break;
                default:
                    result = new LoadCommand(in);
            }
            // skip over entire command
            in.seek(ptr + cmdsize);
            return result;
        }

        public String name() {
            switch (cmd) {
                case LC_SEGMENT_64:
                    return "LC_SEGMENT_64";
                case LC_THREAD:
                    return "LC_THREAD";
                default:
                    return "LC #" + cmd;
            }
        }

    }

    public static final class Segment64LoadCommand extends LoadCommand {
        String segName;
        long vmaddr;
        long vmsize;
        long fileoff;
        long filesize;
        // more we don't care about

        private Segment64LoadCommand(RandomAccessFile in) throws IOException {
            super(in);
        }

        public static Segment64LoadCommand read(RandomAccessFile in) throws IOException {
            final Segment64LoadCommand result = new Segment64LoadCommand(in);
            final byte[] segname = new byte[16];
            for (int i = 0; i < 16; i++) {
                segname[i] = in.readByte();
            }
            result.segName = new String(segname);
            result.vmaddr = readLong(in);
            result.vmsize = readLong(in);
            result.fileoff = readLong(in);
            result.filesize = readLong(in);
            return result;
        }
    }

    public static class ThreadLoadCommand extends LoadCommand {
        public ThreadRegState regstate;
        public ThreadFPRegState fpregstate;
        public ThreadExceptionState exstate;

        ThreadLoadCommand(RandomAccessFile in) throws IOException {
            super(in);
        }

        public static ThreadLoadCommand read(RandomAccessFile in) throws IOException {
            final ThreadLoadCommand result = new ThreadLoadCommand(in);
            result.regstate = ThreadRegState.read(in);
            result.fpregstate = ThreadFPRegState.read(in);
            result.exstate = ThreadExceptionState.read(in);
            return result;
        }
    }

    public static class ThreadState {
        int flavor;
        int count;

        protected ThreadState(RandomAccessFile in) throws IOException {
            flavor = readInt(in);
            count = readInt(in);
        }
    }

    public static class ThreadRegState extends ThreadState {
        int tsh_flavor;
        int tsh_count;
        ByteBuffer regbytes; // we also store registers as a directly allocated ByteBuffer for passing to native code
        long      rax;
        long      rbx;
        long      rcx;
        long      rdx;
        long      rdi;
        long      rsi;
        long      rbp;
        long      rsp;
        long      r8;
        long      r9;
        long      r10;
        long      r11;
        long      r12;
        long      r13;
        long      r14;
        long      r15;
        long      rip;
        long      rflags;
        long      cs;
        long      fs;
        long      gs;

        ThreadRegState(RandomAccessFile in) throws IOException {
            super(in);
        }

        public static ThreadRegState read(RandomAccessFile in) throws IOException {
            final ThreadRegState result = new ThreadRegState(in);
            result.tsh_flavor = readInt(in);
            result.tsh_count = readInt(in);
            final long ptr = in.getFilePointer();
            result.rax = readLong(in);
            result.rbx = readLong(in);
            result.rcx = readLong(in);
            result.rdx = readLong(in);
            result.rdi = readLong(in);
            result.rsi = readLong(in);
            result.rbp = readLong(in);
            result.rsp = readLong(in);
            result.r8 = readLong(in);
            result.r9 = readLong(in);
            result.r10 = readLong(in);
            result.r11 = readLong(in);
            result.r12 = readLong(in);
            result.r13 = readLong(in);
            result.r14 = readLong(in);
            result.r15 = readLong(in);
            result.rip = readLong(in);
            result.rflags = readLong(in);
            result.cs = readLong(in);
            result.fs = readLong(in);
            result.gs = readLong(in);
            in.seek(ptr);
            // read and store again as byte array
            result.regbytes = ByteBuffer.allocateDirect(result.tsh_count * 4);
            for (int i = 0; i < result.tsh_count * 4; i++) {
                result.regbytes.put(in.readByte());
            }
            return result;
        }
    }

    public static class ThreadFPRegState extends ThreadState {

        int fsh_flavor;
        int fsh_count;
        ByteBuffer regbytes; // way too complex for individual declarations; do it all via native code

        ThreadFPRegState(RandomAccessFile in) throws IOException {
            super(in);
        }

        public static ThreadFPRegState read(RandomAccessFile in) throws IOException {
            final ThreadFPRegState result = new ThreadFPRegState(in);
            result.fsh_flavor = readInt(in);
            result.fsh_count = readInt(in);
            result.regbytes = ByteBuffer.allocateDirect(result.fsh_count * 4);
            for (int i = 0; i < result.fsh_count * 4; i++) {
                result.regbytes.put(in.readByte());
            }
            return result;
        }
    }

    public static class ThreadExceptionState extends ThreadState {
        int esh_flavor;
        int esh_count;
        int trapno;
        int err;
        long faultvaddr;

        ThreadExceptionState(RandomAccessFile in) throws IOException {
            super(in);
        }

        public static ThreadExceptionState read(RandomAccessFile in) throws IOException {
            final ThreadExceptionState result = new ThreadExceptionState(in);
            result.esh_flavor = readInt(in);
            result.esh_count = readInt(in);
            result.trapno = readInt(in);
            result.err = readInt(in);
            result.faultvaddr = readLong(in);
            return result;
        }
    }


    public static int readInt(RandomAccessFile in) throws IOException {
        final int b1 = in.read();
        final int b2 = in.read();
        final int b3 = in.read();
        final int b4 = in.read();
        return (b4 << 24) | (b3 << 16) | (b2 << 8) | b1;
    }

    public static long readLong(RandomAccessFile in) throws IOException {
        final long lw = readInt(in);
        final long hw = readInt(in);
        return hw << 32 | (lw & 0xFFFFFFFFL);
    }

}

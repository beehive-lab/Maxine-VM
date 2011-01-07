/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.tir;

import com.sun.max.program.*;
import com.sun.max.util.*;
import com.sun.max.vm.cps.tir.pipeline.*;
import com.sun.max.vm.type.*;

public class TirLocal extends TirInstruction {
    public static final class Flags {
        private Kind kind = Kind.VOID;
        private boolean isRead;
        private boolean isWritten;
        private boolean isUndefined;

        private Flags() {

        }

        private Flags(Flags flags) {
            this.kind = flags.kind;
            this.isRead = flags.isRead;
            this.isWritten = flags.isWritten;
            this.isUndefined = flags.isUndefined;
        }

        public boolean isRead() {
            return isRead;
        }

        public boolean isWritten() {
            return isWritten;
        }

        public boolean isUndefined() {
            return isUndefined;
        }

        @Override
        public String toString() {
            String flags = "flags: " + kind;
            if (isRead) {
                flags += ", read";
            }
            if (isWritten) {
                flags += ", written";
            }
            if (isUndefined) {
                flags += ", undefined";
            }
            return flags;
        }

        public void setWritten(boolean written) {
            isWritten = written;
        }

        public void setRead(boolean read) {
            isRead = read;
        }
    }

    public static final Predicate<TirInstruction> IS_READ = new Predicate<TirInstruction>(){
        public boolean evaluate(TirInstruction instruction) {
            final TirLocal local = (TirLocal) instruction;
            return local.flags().isRead();
        }
    };

    private Flags flags;
    private Flags temporaryFlags = new Flags();

    private final int slot;

    public TirLocal(int slot) {
        this.slot = slot;
    }

    @Override
    public Kind kind() {
        return flags().kind;
    }

    // Checkstyle: stop
    @Override
    public void setKind(Kind kind) {
        flags().isRead = true;
        if (flags().kind == Kind.VOID) {
            flags().kind = kind;
        } else {
            if (flags().kind != kind) {
                ProgramError.unexpected("I don't remember what to do here!");
                flags().isUndefined = true;
            }
        }
    }
    // Checkstyle: resume

    @Override
    public void accept(TirInstructionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "LOCAL slot: " + slot;
    }

    public int slot() {
        return slot;
    }

    public Flags flags() {
        if (temporaryFlags != null) {
            return temporaryFlags;
        }
        return flags;
    }

    public void createFlags() {
        temporaryFlags = new Flags(flags);
    }

    public void commitFlags() {
        flags = temporaryFlags;
        temporaryFlags = new Flags(temporaryFlags);
    }

    public void discardFlags() {
        temporaryFlags = null;
    }

    public void complete(TirInstruction tail) {
        if (this != tail) {
            final Flags flags = flags();
            flags.setWritten(true);
            if (flags.isUndefined == false) {
                if (flags.kind == Kind.VOID) {
                    flags.kind = tail.kind();
                } else {
                    if (flags.kind != tail.kind()) {
                        flags.isUndefined = true;
                    }
                }
            }
        }
    }

}

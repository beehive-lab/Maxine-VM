/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
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

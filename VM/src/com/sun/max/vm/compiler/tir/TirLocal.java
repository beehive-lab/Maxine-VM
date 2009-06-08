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
package com.sun.max.vm.compiler.tir;

import com.sun.max.program.*;
import com.sun.max.util.*;
import com.sun.max.vm.compiler.tir.pipeline.*;
import com.sun.max.vm.type.*;

public class TirLocal extends TirInstruction {
    public static final class Flags {
        private Kind _kind = Kind.VOID;
        private boolean _isRead;
        private boolean _isWritten;
        private boolean _isUndefined;

        private Flags() {

        }

        private Flags(Flags flags) {
            _kind = flags._kind;
            _isRead = flags._isRead;
            _isWritten = flags._isWritten;
            _isUndefined = flags._isUndefined;
        }

        public boolean isRead() {
            return _isRead;
        }

        public boolean isWritten() {
            return _isWritten;
        }

        public boolean isUndefined() {
            return _isUndefined;
        }

        @Override
        public String toString() {
            String flags = "flags: " + _kind;
            if (_isRead) {
                flags += ", read";
            }
            if (_isWritten) {
                flags += ", written";
            }
            if (_isUndefined) {
                flags += ", undefined";
            }
            return flags;
        }

        public void setWritten(boolean written) {
            _isWritten = written;
        }

        public void setRead(boolean read) {
            _isRead = read;
        }
    }

    public static final Predicate<TirInstruction> IS_READ = new Predicate<TirInstruction>(){
        public boolean evaluate(TirInstruction instruction) {
            final TirLocal local = (TirLocal) instruction;
            return local.flags().isRead();
        }
    };

    private Flags _flags;
    private Flags _temporaryFlags = new Flags();

    private final int _slot;

    public TirLocal(int slot) {
        _slot = slot;
    }

    @Override
    public Kind kind() {
        return flags()._kind;
    }

    // Checkstyle: stop
    @Override
    public void setKind(Kind kind) {
        flags()._isRead = true;
        if (flags()._kind == Kind.VOID) {
            flags()._kind = kind;
        } else {
            if (flags()._kind != kind) {
                ProgramError.unexpected("I don't remember what to do here!");
                flags()._isUndefined = true;
            }
        }
    }

    @Override
    public void accept(TirInstructionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "LOCAL slot: " + _slot;
    }

    public int slot() {
        return _slot;
    }

    public Flags flags() {
        if (_temporaryFlags != null) {
            return _temporaryFlags;
        }
        return _flags;
    }

    public void createFlags() {
        _temporaryFlags = new Flags(_flags);
    }

    public void commitFlags() {
        _flags = _temporaryFlags;
        _temporaryFlags = new Flags(_temporaryFlags);
    }

    public void discardFlags() {
        _temporaryFlags = null;
    }

    public void complete(TirInstruction tail) {
        if (this != tail) {
            flags().setWritten(true);
            if (flags()._isUndefined == false) {
                if (flags()._kind == Kind.VOID) {
                    flags()._kind = tail.kind();
                } else {
                    if (flags()._kind != tail.kind()) {
                        flags()._isUndefined = true;
                    }
                }
            }
        }
    }

}

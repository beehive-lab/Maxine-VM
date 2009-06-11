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

import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.vm.compiler.tir.pipeline.*;


public abstract class TirMessage implements Cloneable {
    @Override
    protected TirMessage clone() {
        try {
            return (TirMessage) super.clone();
        } catch (CloneNotSupportedException e) {
            ProgramError.unexpected();
            return null;
        }
    }

    protected TirMessage clone(GrowableMapping<TirMessage, TirMessage> map) {
        if (isCloned(map)) {
            return map.get(this);
        }
        final TirMessage message = clone();
        map.put(this, message);
        return message;
    }

    protected final boolean isCloned(GrowableMapping<TirMessage, TirMessage> map) {
        return map.containsKey(this);
    }

    public static class TirTreeBegin extends TirMessage {
        private final TirPipelineOrder _order;
        private final TirTree _tree;

        public TirTreeBegin(TirTree tree, TirPipelineOrder order) {
            _tree = tree;
            _order = order;
        }

        public TirPipelineOrder order() {
            return _order;
        }

        public TirTree tree() {
            return _tree;
        }

        @Override
        public void accept(TirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class TirTreeEnd extends TirMessage {
        @Override
        public void accept(TirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class TirTraceBegin extends TirMessage {
        private final TirTrace _trace;

        @Override
        public void accept(TirInstructionVisitor visitor) {
            visitor.visit(this);
        }

        public TirTraceBegin(TirTrace trace) {
            _trace = trace;
        }

        public TirTrace trace() {
            return _trace;
        }

    }

    public static class TirTraceEnd extends TirMessage {
        @Override
        public void accept(TirInstructionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public abstract void accept(TirInstructionVisitor visitor);

    @Override
    public String toString() {
        return super.getClass().getSimpleName().toUpperCase();
    }
}

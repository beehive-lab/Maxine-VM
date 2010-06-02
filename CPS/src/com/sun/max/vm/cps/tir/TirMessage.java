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

import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.vm.cps.tir.pipeline.*;

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

    protected TirMessage clone(Mapping<TirMessage, TirMessage> map) {
        if (isCloned(map)) {
            return map.get(this);
        }
        final TirMessage message = clone();
        map.put(this, message);
        return message;
    }

    protected final boolean isCloned(Mapping<TirMessage, TirMessage> map) {
        return map.containsKey(this);
    }

    public static class TirTreeBegin extends TirMessage {
        private final TirPipelineOrder order;
        private final TirTree tree;

        public TirTreeBegin(TirTree tree, TirPipelineOrder order) {
            this.tree = tree;
            this.order = order;
        }

        public TirPipelineOrder order() {
            return order;
        }

        public TirTree tree() {
            return tree;
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
        private final TirTrace trace;

        @Override
        public void accept(TirInstructionVisitor visitor) {
            visitor.visit(this);
        }

        public TirTraceBegin(TirTrace trace) {
            this.trace = trace;
        }

        public TirTrace trace() {
            return trace;
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

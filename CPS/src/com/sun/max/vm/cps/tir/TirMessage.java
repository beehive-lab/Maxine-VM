/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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

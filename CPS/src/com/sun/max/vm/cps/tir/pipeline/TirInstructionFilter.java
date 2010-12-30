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
package com.sun.max.vm.cps.tir.pipeline;

import com.sun.max.vm.cps.tir.*;

public class TirInstructionFilter extends TirInstructionAdapter implements TirMessageSink {
    protected final TirMessageSink receiver;

    public TirInstructionFilter(TirMessageSink receiver) {
        this.receiver = TirPipeline.connect(this, receiver);
    }

    public void receive(TirMessage message) {
        if (message instanceof TirInstruction) {
            final TirInstruction instruction = (TirInstruction) message;
            if (filter(instruction) == false) {
                return;
            }
        }
        message.accept(this);
    }

    /**
     * Prevents instructions from being inspected by this filter.
     */
    protected boolean filter(TirInstruction instruction) {
        return true;
    }

    @Override
    public void visit(TirMessage message) {
        forward(message);
    }

    public void forward(TirMessage message) {
        receiver.receive(message);
    }
}

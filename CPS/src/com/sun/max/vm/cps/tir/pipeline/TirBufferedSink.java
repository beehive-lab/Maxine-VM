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
package com.sun.max.vm.cps.tir.pipeline;

import java.util.*;

import com.sun.max.vm.cps.tir.*;
import com.sun.max.vm.cps.tir.TirMessage.*;

public class TirBufferedSink extends TirInstructionAdapter implements TirMessageSink  {
    private List<TirMessage> prolog = new ArrayList<TirMessage>();
    private List<TirMessage> messages = new ArrayList<TirMessage>();

    private TirTreeBegin treeBegin;
    private TirTreeEnd treeEnd;
    private boolean inTrace;

    public void receive(TirMessage message) {
        message.accept(this);
    }

    @Override
    public void visit(TirTreeBegin message) {
        treeBegin = message;
    }

    @Override
    public void visit(TirTreeEnd message) {
        treeEnd = message;
    }

    @Override
    public void visit(TirTraceBegin message) {
        inTrace = true;
        super.visit(message);
    }

    @Override
    public void visit(TirTraceEnd message) {
        super.visit(message);
        inTrace = false;
    }

    @Override
    public void visit(TirMessage message) {
        if (inTrace) {
            messages.add(message);
        } else {
            prolog.add(message);
        }
    }

    public final void replay(TirMessageSink receiver) {
        forward(receiver, treeBegin);
        if (treeBegin.order() == TirPipelineOrder.FORWARD) {
            for (TirMessage message : prolog) {
                forward(receiver, message);
            }
        }
        for (TirMessage message : messages) {
            forward(receiver, message);
        }
        if (treeBegin.order() == TirPipelineOrder.REVERSE) {
            for (TirMessage message : prolog) {
                forward(receiver, message);
            }
        }
        forward(receiver, treeEnd);
    }

    private void forward(TirMessageSink receiver, TirMessage message) {
        receiver.receive(message);
    }
}

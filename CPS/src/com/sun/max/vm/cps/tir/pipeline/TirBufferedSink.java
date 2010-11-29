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

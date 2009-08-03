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
package com.sun.max.vm.verifier;

import com.sun.max.vm.verifier.types.*;

/**
 * An extension of a subroutine frame that also records the JSR instruction effecting a call to the subroutine.
 *
 * @author Doug Simon
 */
public class SubroutineCall extends SubroutineFrame {

    public final InstructionHandle caller;
    private int nextInstuctionHandleIndex;

    public SubroutineCall(Subroutine subroutine, SubroutineCall parent, InstructionHandle caller) {
        super(subroutine, parent);
        this.caller = caller;
    }

    @Override
    public SubroutineCall parent() {
        return (SubroutineCall) super.parent();
    }

    public void setNextInstuctionHandleIndex(int index) {
        nextInstuctionHandleIndex = index;
    }

    public int nextInstuctionHandleIndex() {
        return nextInstuctionHandleIndex;
    }

    public boolean matches(SubroutineFrame subroutineFrame) {
        if (subroutineFrame == this) {
            return true;
        }
        if (depth != subroutineFrame.depth) {
            return false;
        }
        if (subroutine == subroutineFrame.subroutine || subroutine == MERGED_SUBROUTINE || subroutineFrame.subroutine == MERGED_SUBROUTINE) {
            if (subroutineFrame instanceof SubroutineCall) {
                final SubroutineCall subroutineCall = (SubroutineCall) subroutineFrame;
                if (subroutineCall.caller != caller) {
                    return false;
                }
            }
            return parent().matches(subroutineFrame.parent());
        }
        return false;
    }

    public boolean canGoto(SubroutineCall toSubroutineCall) {
        if (toSubroutineCall == this) {
            return true;
        }
        if (toSubroutineCall.depth > depth) {
            return false;
        }
        SubroutineCall fromSubroutineCall = this;
        while (fromSubroutineCall.depth > toSubroutineCall.depth) {
            fromSubroutineCall = fromSubroutineCall.parent();
        }
        return fromSubroutineCall.matches(toSubroutineCall);
    }
}

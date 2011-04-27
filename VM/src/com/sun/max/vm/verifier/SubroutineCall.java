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

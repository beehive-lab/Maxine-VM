/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.ins.value;

import com.sun.max.ins.*;
import com.sun.max.tele.object.*;
import com.sun.max.vm.actor.*;
import com.sun.max.vm.value.*;

/**
 * A textual display label associated with an integer that represents the value of
 * the {@link Actor#flags} field in instances of {@link Actor} in the VM.
 *
 * @see TeleActor
 *
 * @author Michael Van De Vanter
  */
public final class ActorFlagsValueLabel extends ValueLabel {

    private final TeleActor teleActor;
    private String flagsAsHex;
    private String flagsAsString;

    public ActorFlagsValueLabel(Inspection inspection, TeleActor teleActor) {
        super(inspection);
        this.teleActor = teleActor;
        initializeValue();
        redisplay();
    }

    @Override
    protected Value fetchValue() {
        final int flags = teleActor.getFlags();
        flagsAsHex = "Flags: 0x" + Integer.toHexString(flags);
        flagsAsString = teleActor.flagsAsString();
        return IntValue.from(flags);
    }

    @Override
    protected void updateText() {
        setText(flagsAsHex);
        setToolTipText(flagsAsString);
    }

    public void redisplay() {
        setFont(style().primitiveDataFont());
        updateText();
    }

}

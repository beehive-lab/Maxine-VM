/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 *
 * A textual display label associated with a primitive value to be read from the VM.
 */
public class PrimitiveValueLabel extends ValueLabel {

    private final Kind kind;

    public Kind kind() {
        return kind;
    }

    // TODO (mlvdv) this class is very hard to subclass because it does all its work
    // in the constructor, which keeps subclasses from having access to their
    // local state variables when the other methods get called.
    public PrimitiveValueLabel(Inspection inspection, Kind kind) {
        super(inspection, null);
        this.kind = kind;
        initializeValue();
        redisplay();
    }

    public PrimitiveValueLabel(Inspection inspection, Value value) {
        super(inspection, value);
        this.kind = value.kind();
        initializeValue();
        redisplay();
    }

    public void redisplay() {
        setFont(style().primitiveDataFont());
        updateText();
    }

    @Override
    public void updateText() {
        assert value() != null;
        if (kind == Kind.CHAR) {
            setText("'" + value().toString() + "'");
            setWrappedToolTipHtmlText("Int: " + Integer.toString(value().toInt()) + ", " + intTo0xHex(value().toInt()));
        } else if (kind == Kind.INT) {
            setText(value().toString());
            setWrappedToolTipHtmlText(intTo0xHex(value().toInt()));
        } else {
            setText(value().toString());
            setWrappedToolTipHtmlText(value().toString());
        }
    }

}

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
package com.sun.max.ins.value;

import com.sun.max.ins.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * @author Michael Van De Vanter
 *
 * A textual display label associated with a primitive value to be read from the {@link TeleVM}.
 */
public class PrimitiveValueLabel extends ValueLabel {

    private final Kind _kind;

    public Kind kind() {
        return _kind;
    }

    public PrimitiveValueLabel(Inspection inspection, Kind kind) {
        super(inspection, null);
        _kind = kind;
        initializeValue();
        redisplay();
    }

    public PrimitiveValueLabel(Inspection inspection, Value value) {
        super(inspection, value);
        _kind = value.kind();
        initializeValue();
        redisplay();
    }

    public void redisplay() {
        setFont(style().primitiveDataFont());
        setForeground(style().primitiveDataColor());
        setBackground(style().primitiveDataBackgroundColor());
        updateText();
    }

    @Override
    public void updateText() {
        assert value() != null;
        if (_kind == Kind.CHAR) {
            setText("'" + value().toString() + "'");
            setToolTipText("Int: " + Integer.toString(value().toInt()) + ", 0x" + Integer.toHexString(value().toInt()));
        } else if (_kind == Kind.INT) {
            setText(value().toString());
            setToolTipText("0x" + Integer.toHexString(value().toInt()));
        } else {
            setText(value().toString());
        }
    }

}

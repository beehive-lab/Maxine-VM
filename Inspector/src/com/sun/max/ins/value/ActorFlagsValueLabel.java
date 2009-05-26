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
import com.sun.max.tele.object.*;
import com.sun.max.vm.actor.*;
import com.sun.max.vm.value.*;


/**
 * A textual display label associated with an integer that represents the value of
 * the {@link Actor#_flags} field in instances of {@link Actor} in the VM.
 *
 * @see TeleActor
 *
 * @author Michael Van De Vanter
  */
public final class ActorFlagsValueLabel extends ValueLabel {

    private final TeleActor _teleActor;
    private String _flagsAsHex;
    private String _flagsAsString;

    public ActorFlagsValueLabel(Inspection inspection, TeleActor teleActor) {
        super(inspection);
        _teleActor = teleActor;
        initializeValue();
        redisplay();
    }

    @Override
    protected Value fetchValue() {
        final int flags = _teleActor.readFlags();
        _flagsAsHex = "Flags: 0x" + Integer.toHexString(flags);
        _flagsAsString = _teleActor.flagsAsString();
        return IntValue.from(flags);
    }

    @Override
    protected void updateText() {
        setText(_flagsAsHex);
        setToolTipText(_flagsAsString);
    }

    @Override
    public void redisplay() {
        setFont(style().primitiveDataFont());
        setForeground(style().primitiveDataColor());
        setBackground(style().primitiveDataBackgroundColor());
        updateText();
    }

}

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
package com.sun.max.ins.gui;

import com.sun.c1x.bytecode.*;
import com.sun.max.ins.*;

/**
 * A label for presenting a Bytecodes instruction mnemonic.
 *
 * @author Michael Van De Vanter
 *
 */
public class BytecodeMnemonicLabel extends InspectorLabel {

    private int opcode;

    public BytecodeMnemonicLabel(Inspection inspection, int opcode) {
        super(inspection, "");
        this.opcode = opcode;
        redisplay();
    }

    public final void redisplay() {
        setFont(style().bytecodeMnemonicFont());
        updateText();
    }

    public final void setValue(int opcode) {
        this.opcode = opcode;
        updateText();
    }

    private void updateText() {
        try {
            setText(Bytecodes.nameOf(opcode));
            setToolTipText("Opcode " + opcode + " (0x" + Integer.toHexString(opcode) + ")");
        } catch (IllegalArgumentException e) {
            setText(null);
            setToolTipText(null);
        }
    }

    public final void refresh(boolean force) {
        // no remote data to refresh
    }

}

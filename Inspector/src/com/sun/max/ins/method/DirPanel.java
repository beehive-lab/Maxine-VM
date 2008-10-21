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
package com.sun.max.ins.method;

import java.awt.*;

import javax.swing.*;

import com.sun.max.gui.*;
import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.vm.compiler.dir.*;

/**
 * @author Bernd Mathiske
 */
public final class DirPanel extends InspectorPanel {

    private final DirMethod _dirMethod;

    public DirMethod dirMethod() {
        return _dirMethod;
    }

    private void addBlock(DirBlock block) {
        final JPanel panel = new JPanel(new SpringLayout());
        panel.setBorder(BorderFactory.createMatteBorder(2, 0, 0, 0, Color.BLUE));
        final TitleLabel blockTitle = new TitleLabel(inspection(), "BLOCK #" + block.serial());
        blockTitle.setForeground(Color.BLUE);
        panel.add(blockTitle);

        for (int i = 0; i < block.instructions().length(); i++) {
            final DirInstruction instruction = block.instructions().get(i);
            panel.add(new TextLabel(inspection(), instruction.toString()));
        }

        SpringUtilities.makeCompactGrid(panel, 1);
        add(panel);
    }

    DirPanel(Inspection inspection, DirMethod dirMethod) {
        super(inspection);
        _dirMethod = dirMethod;
        for (DirBlock block : dirMethod.blocks()) {
            addBlock(block);
        }
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }

    public void refresh(long epoch) {
    }

    public void redisplay() {
        // TODO (mlvdv)  redisplay this
    }

}

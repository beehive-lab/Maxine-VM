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

import java.awt.event.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.value.*;


/**
 * A label that displays the name of the {@link MemoryRegion} to which a {@link Value}
 * might point; blank if no such region.  Updated with ever refresh.
 *
 * @author Michael Van De Vanter
 *
 */
public class MemoryRegionValueLabel extends ValueLabel {

    private Address _address;
    private MemoryRegion _memoryRegion = null;

    private final class MemoryRegionMouseClickAdapter extends InspectorMouseClickAdapter {

        public MemoryRegionMouseClickAdapter(Inspection inspection) {
            super(inspection);
        }

        @Override
        public void procedure(MouseEvent mouseEvent) {
            if (_memoryRegion != null) {
                switch (MaxineInspector.mouseButtonWithModifiers(mouseEvent)) {
                    case MouseEvent.BUTTON1: {
                        inspection().focus().setMemoryRegion(_memoryRegion);
                        break;
                    }
                    case MouseEvent.BUTTON3: {
                        final InspectorMenu menu = new InspectorMenu();
                        menu.add(inspection().actions().selectMemoryRegion(_memoryRegion));
                        menu.add(inspection().actions().inspectMemory(_address, "Inspect memory"));
                        menu.add(inspection().actions().inspectMemoryWords(_address, "Inspect memory words"));
                        menu.popupMenu().show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
                        break;
                    }
                    default: {
                        break;
                    }
                }
            }
        }
    }

    public MemoryRegionValueLabel(Inspection inspection) {
        super(inspection);
        initializeValue();
        addMouseListener(new MemoryRegionMouseClickAdapter(inspection));
        redisplay();
    }

    public MemoryRegionValueLabel(Inspection inspection, Address address) {
        super(inspection, new WordValue(address));
        addMouseListener(new MemoryRegionMouseClickAdapter(inspection));
    }

    @Override
    protected void updateText() {
        if (value() != null) {
            _address = value().toWord().asAddress();
            _memoryRegion = maxVM().memoryRegionContaining(_address);
        }
        if (_memoryRegion == null) {
            setText("");
            setToolTipText("");
        } else {
            setText(_memoryRegion.description());
            setToolTipText("0x" + _address.toHexString() + " in \"" + _memoryRegion.description() + "\" region");
        }
    }

    @Override
    public void redisplay() {
        setFont(style().javaNameFont());
        setForeground(style().javaNameColor());
        setBackground(style().javaNameBackgroundColor());
        updateText();
    }

}

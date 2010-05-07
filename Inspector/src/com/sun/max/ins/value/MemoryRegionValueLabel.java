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

import java.awt.datatransfer.*;
import java.awt.event.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.tele.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.value.*;

/**
 * A label that displays the name of the {@linkplain MaxMemoryRegion memory region} to which a {@link Value}
 * might point; blank if no such region.  Updated with every refresh.
 *
 * @author Michael Van De Vanter
 */
public class MemoryRegionValueLabel extends ValueLabel {

    private Address address;
    private String regionName;
    private String toolTipText;
    private MaxMemoryRegion memoryRegion = null;

    private final class MemoryRegionMouseClickAdapter extends InspectorMouseClickAdapter {

        public MemoryRegionMouseClickAdapter(Inspection inspection) {
            super(inspection);
        }

        @Override
        public void procedure(MouseEvent mouseEvent) {
            if (memoryRegion != null) {
                switch (Inspection.mouseButtonWithModifiers(mouseEvent)) {
                    case MouseEvent.BUTTON1: {
                        focus().setMemoryRegion(memoryRegion);
                        break;
                    }
                    case MouseEvent.BUTTON3: {
                        final InspectorPopupMenu menu = new InspectorPopupMenu();
                        menu.add(actions().inspectRegionMemoryWords(memoryRegion, regionName));
                        menu.add(actions().selectMemoryRegion(memoryRegion));
                        menu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
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
        memoryRegion = null;
        regionName = null;
        toolTipText = null;
        if (value() != null) {
            address = value().toWord().asAddress();
            memoryRegion = vm().findMemoryRegion(address);
        }
        if (memoryRegion != null) {
            regionName = inspection().nameDisplay().shortName(memoryRegion);
            toolTipText = inspection().nameDisplay().longName(memoryRegion);
        }
        setText(regionName);
        setToolTipText(toolTipText);
    }

    public void redisplay() {
        setFont(style().javaNameFont());
        updateText();
    }

    @Override
    public Transferable getTransferable() {
        if (memoryRegion != null) {
            return new InspectorTransferable.MemoryRegionTransferable(inspection(), memoryRegion);
        }
        return null;
    }

}

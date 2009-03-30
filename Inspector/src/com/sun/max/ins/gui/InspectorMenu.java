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

import javax.swing.*;

import com.sun.max.collect.*;
import com.sun.max.ins.*;
import com.sun.max.ins.memory.MemoryInspector.*;
import com.sun.max.ins.memory.MemoryWordInspector.*;

/**
 * A menu that can be manifest in the GUI by a {@linkplain JPopupMenu pop-up menu} or a {@linkplain JMenu standard menu}.
 *
 * @author Doug Simon
 * @author Michael Van De Vanter
 */
public final class InspectorMenu implements Prober {

    private final Inspector _inspector;
    private AppendableSequence<InspectorMenuItems> _inspectorMenuItems;

    private final JPopupMenu _popupMenu;

    public JPopupMenu popupMenu() {
        return _popupMenu;
    }

    private final JMenu _standardMenu;

    public JMenu standardMenu() {
        return _standardMenu;
    }

    public InspectorMenu(Inspector inspector, JPopupMenu popupMenu, JMenu standardMenu) {
        _inspector = inspector;
        _popupMenu = popupMenu == null ? new JPopupMenu() : popupMenu;
        _standardMenu = standardMenu == null ? new JMenu() : standardMenu;
        if (inspector != null) {
            add(inspector.getCloseOtherInspectorsAction());
            // Undocking isn't supported at this time (mlvdv Jan 2009)
            //add(inspector.createToggleResidenceAction());
            if (inspector instanceof MemoryInspectable) {
                final MemoryInspectable memoryInspectable = (MemoryInspectable) inspector;
                add(memoryInspectable.getMemoryInspectorAction());
            }
            add(inspector.getRefreshAction());
        }
    }

    public InspectorMenu(Inspector inspector, String name) {
        _inspector = inspector;
        _popupMenu =  new JPopupMenu(name);
        _standardMenu = new JMenu(name);
        if (inspector != null) {
            add(inspector.getCloseOtherInspectorsAction());
            // undocking isn't supported at this time  mlvdv Jan 2009
            //add(inspector.createToggleResidenceAction());
            if (inspector instanceof MemoryInspectable) {
                final MemoryInspectable memoryInspectable = (MemoryInspectable) inspector;
                add(memoryInspectable.getMemoryInspectorAction());
            }
            if (inspector instanceof MemoryWordInspectable) {
                final MemoryWordInspectable memoryWordInspectable = (MemoryWordInspectable) inspector;
                add(memoryWordInspectable.getMemoryWordInspectorAction());
            }
            add(inspector.getRefreshAction());
        }
    }

    public InspectorMenu(Inspector inspector) {
        this(inspector, null);
    }

    public InspectorMenu() {
        this(null, null);
    }

    public int length() {
        return _standardMenu.getItemCount();
    }

    public void add(InspectorAction action) {
        action.append(_standardMenu);
        action.append(_popupMenu);
    }

    public void add(InspectorMenuItems inspectorMenuItems) {
        addSeparator();
        inspectorMenuItems.addTo(this);
        if (_inspectorMenuItems == null) {
            _inspectorMenuItems = new LinkSequence<InspectorMenuItems>();
        }
        _inspectorMenuItems.append(inspectorMenuItems);
    }

    public void add(InspectorMenu inspectorMenu) {
        _standardMenu.add(inspectorMenu._standardMenu);
        _popupMenu.add(inspectorMenu._standardMenu);
    }

    /**
     * For menu items that do not change any Inspector or VM state, only local view state.
     * @param menuItem
     */
    public void add(JMenuItem menuItem) {
        _standardMenu.add(menuItem);
        _popupMenu.add(menuItem);
    }

    public void addSeparator() {
        _standardMenu.addSeparator();
        _popupMenu.addSeparator();
    }

    public Inspection inspection() {
        return _inspector.inspection();
    }

    public void refresh(long epoch, boolean force) {
        if (_inspectorMenuItems != null) {
            for (InspectorMenuItems inspectorMenuItems : _inspectorMenuItems) {
                inspectorMenuItems.refresh(epoch, force);
            }
        }
    }

    public void redisplay() {
    }

}

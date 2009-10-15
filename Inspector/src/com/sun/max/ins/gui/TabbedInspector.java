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

import java.awt.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

import com.sun.max.ins.*;
import com.sun.max.ins.InspectionSettings.*;
import com.sun.max.lang.*;


/**
 * An Inspector that contains within it a collection of inspectors in a tabbed frame.
 * Tabbed inspectors manage change explicitly: removes the contained inspectors
 * from the update listeners managed generally and notifies only the visible one
 * when notified of a change.
 *
 * @author Michael Van De Vanter
 */
public abstract class TabbedInspector<Inspector_Type extends Inspector, TabbedInspector_Type extends TabbedInspector<Inspector_Type, TabbedInspector_Type>>
    extends UniqueInspector<TabbedInspector_Type>
    implements InspectorContainer<Inspector_Type> {

    private final JTabbedPane tabbedPane;
    private final SaveSettingsListener saveSettingsListener;

    protected void addChangeListener(ChangeListener listener) {
        tabbedPane.addChangeListener(listener);
    }

    protected void removeChangeListener(ChangeListener listener) {
        tabbedPane.removeChangeListener(listener);
    }

    protected TabbedInspector(Inspection inspection, final String settingsClientName) {
        super(inspection);
        tabbedPane = new JTabbedPane();
        if (settingsClientName != null) {
            saveSettingsListener = createGeometrySettingsClient(this, settingsClientName + "Geometry");
        } else {
            saveSettingsListener = null;
        }
        createFrame(null);
        addChangeListener(tabChangeListener);
    }

    @Override
    protected final SaveSettingsListener saveSettingsListener() {
        return saveSettingsListener;
    }

    private final ChangeListener tabChangeListener = new ChangeListener() {

        public void stateChanged(ChangeEvent event) {
            // An Inspector tab has become visible that was not visible before.
            final Inspector selectedInspector = getSelected();
            if (selectedInspector != null) {
                // Inspector may not have been getting refreshed while not visible.
                selectedInspector.refreshView(true);
            }
        }
    };

    public Inspector_Type inspectorAt(int i) {
        final Component component = tabbedPane.getComponentAt(i);
        if (component instanceof InspectorFrame) {
            final InspectorFrame inspectorFrame = (InspectorFrame) component;
            final Class<Inspector_Type> type = null;
            return StaticLoophole.cast(type, inspectorFrame.inspector());
        }
        return null;
    }

    public int length() {
        return tabbedPane.getTabCount();
    }

    public int getSelectedIndex() {
        return tabbedPane.getSelectedIndex();
    }

    public void setSelected(Inspector_Type inspector) {
        moveToFront();
        tabbedPane.setSelectedComponent(inspector.getJComponent());
    }

    public boolean isSelected(Inspector_Type inspector) {
        return inspector.getJComponent() ==  tabbedPane.getSelectedComponent();
    }

    public Inspector_Type getSelected() {
        final Component component =  tabbedPane.getSelectedComponent();
        for (Inspector_Type  inspector : this) {
            if (inspector.getJComponent() == component) {
                return inspector;
            }
        }
        return null;
    }

    public void add(Inspector_Type inspector) {
        add(inspector, inspector.getTitle());
    }

    public void add(Inspector_Type inspector, String tabTitle) {
        add(inspector, tabTitle, null);
    }

    public void add(Inspector_Type inspector, String tabTitle, String tabToolTipText) {
        final JComponent component = inspector.getJComponent();
        tabbedPane.addTab(tabTitle, component);
        if (tabToolTipText != null) {
            final int index = tabbedPane.indexOfComponent(component);
            tabbedPane.setToolTipTextAt(index, tabToolTipText);
        }
    }

    public void add(Inspector_Type inspector, String tabTitle, String tabToolTipText, String frameTitle) {
        final JComponent component = inspector.getJComponent();
        tabbedPane.addTab(tabTitle, component);
        if (tabToolTipText != null) {
            tabbedPane.setToolTipTextAt(tabbedPane.indexOfComponent(component), tabToolTipText);
        }
        if (frameTitle != null) {
            inspector.setTitle(frameTitle);
        }
    }

    /**
     * Extends the tab of this component with a small icon that, when clicked, will close the inspector.
     * Using this has the curious side-effect that the tabbed pane has additional children that aren't the actual
     * tabbed frames.
     */
    public void addCloseIconToTab(Inspector_Type inspector) {
        final int index = tabbedPane.indexOfComponent(inspector.getJComponent());
        tabbedPane.setTabComponentAt(index, new ButtonTabComponent(inspection(), this, inspector, tabbedPane));
    }

    public Iterator<Inspector_Type> iterator() {
        return InspectorContainer.Static.iterator(this);
    }

    @Override
    public void createView() {
        setContentPane(tabbedPane);
    }

    public void viewConfigurationChanged() {
        // These containers generally have no view configurations that need updating.
    }

    /**
     * Disposes of the specified inspector, presumed to be a tabbed member of this inspector.
     * If no tabs remain, dispose of the whole thing.
     */
    public void close(Inspector inspector) {
        inspector.dispose();
        if (length() == 0) {
            dispose();
        }
    }

    /**
     * Disposes of all but the specified inspector, presumed to be a tabbed member of this inspector.
     */
    public void closeOthers(Inspector keepInspector) {
        for (Inspector_Type inspector : this) {
            if (inspector != keepInspector) {
                inspector.dispose();
            }
        }
        if (length() == 0) {
            // in case the specified frame wasn't a member.
            dispose();
        }
    }

    /**
     * Receives notification that the window system is closing this inspector.
     */
    @Override
    public void inspectorClosing() {
        removeChangeListener(tabChangeListener);
        for (Inspector_Type inspector : this) {
            inspector.dispose();
        }
        super.inspectorClosing();
    }

}

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
package com.sun.max.ins.gui;

import java.awt.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;

import com.sun.max.*;
import com.sun.max.ins.*;
import com.sun.max.ins.view.InspectionViews.ViewKind;

/**
 * An {@link Inspector} that contains within it a collection of inspectors in a tabbed frame.
 * <br>
 * Tabbed inspectors manage change explicitly: removes the contained inspectors
 * from the update listeners managed generally and notifies only the visible one
 * when notified of a change.
 *
 * @author Michael Van De Vanter
 */
public abstract class TabbedInspector<Inspector_Type extends Inspector> extends Inspector implements InspectorContainer<Inspector_Type> {

    private final Set<Inspector_Type> inspectors = new HashSet<Inspector_Type>();
    private final InspectorTabbedPane tabbedPane;

    protected void addChangeListener(ChangeListener listener) {
        tabbedPane.addChangeListener(listener);
    }

    protected void removeChangeListener(ChangeListener listener) {
        tabbedPane.removeChangeListener(listener);
    }

    protected TabbedInspector(Inspection inspection, ViewKind viewKind, String geometrySettingsKey) {
        super(inspection, viewKind, geometrySettingsKey);
        tabbedPane = new InspectorTabbedPane(inspection);
        addChangeListener(tabChangeListener);
    }

    private final ChangeListener tabChangeListener = new ChangeListener() {

        public void stateChanged(ChangeEvent event) {
            // An Inspector tab has become visible that was not visible before.
            final Inspector selectedInspector = getSelected();
            if (selectedInspector != null) {
                // Inspector may not have been getting refreshed while not visible.
                selectedInspector.forceRefresh();
                setTitle();
            }
        }
    };

    public Inspector_Type inspectorAt(int i) {
        final Component component = tabbedPane.getComponentAt(i);
        if (component instanceof InspectorInternalFrame) {
            final InspectorFrame inspectorFrame = (InspectorFrame) component;
            final Class<Inspector_Type> type = null;
            return Utils.cast(type, inspectorFrame.inspector());
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
        inspectors.add(inspector);
        final JComponent component = inspector.getJComponent();
        tabbedPane.addTab(tabTitle, component);
        if (tabToolTipText != null) {
            final int index = tabbedPane.indexOfComponent(component);
            tabbedPane.setToolTipTextAt(index, tabToolTipText);
        }
        assert inspectors.size() == length();
    }

    /**
     * Extends the tab of this component with a small icon that, when clicked, will close the inspector.
     * Using this has the curious side-effect that the tabbed pane has additional children that aren't the actual
     * tabbed frames.
     */
    public void addCloseIconToTab(Inspector_Type inspector, String toolTipText) {

        ButtonTabComponent<Inspector_Type> tabRenderer = new ButtonTabComponent<Inspector_Type>(inspection(), this, inspector, toolTipText);
        final int index = tabbedPane.indexOfComponent(inspector.getJComponent());
        tabbedPane.setTabComponentAt(index, tabRenderer);
    }

    /**
     * Removes a component that was added as a tab.
     * <br>
     * Note that this happens automatically for a
     * {@link JInternalFrame} when {@JInternalFrame#dispose()}
     * is called, but not for other kinds of components that
     * might be put into a tab.
     *
     * @param component a component that was added as tab
     */
    public void remove(JComponent component) {
        tabbedPane.remove(component);
    }

    public Iterator<Inspector_Type> iterator() {
        return inspectors.iterator();
    }

    @Override
    public void createView() {
        setContentPane(tabbedPane);
    }

    public void viewConfigurationChanged() {
        // These containers generally have no view configurations that need updating.
    }

    /**
     * Initiates disposal of the specified inspector, presumed to be a tabbed member of this inspector.
     * If no tabs remain, dispose of the whole thing.
     */
    public void close(Inspector inspector) {
        assert inspectors.size() == length();
        inspectors.remove(inspector);
        inspector.dispose();
    }

    /**
     * Disposes of all but the specified inspector, presumed to be a tabbed member of this inspector.
     */
    public void closeOthers(Inspector_Type keepInspector) {
        final List<Inspector_Type> toClose = new ArrayList<Inspector_Type>();
        for (Inspector_Type inspector : inspectors) {
            if (inspector != keepInspector) {
                toClose.add(inspector);
            }
        }
        for (Inspector_Type inspector : toClose) {
            close(inspector);
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

/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.max.ins.*;
import com.sun.max.ins.view.InspectionViews.ViewKind;
import com.sun.max.tele.*;

/**
 * An {@link AbstractView} that contains within it a collection of views in a tabbed frame.
 * <br>
 * Tabbed views manage change explicitly: removes the contained views
 * from the update listeners managed generally and notifies only the visible one
 * when notified of a change.
 */
public abstract class TabbedView<View_Type extends TabbedView> extends AbstractView<View_Type> implements ViewContainer {

    private final Set<AbstractView> views = new HashSet<AbstractView>();
    private final InspectorTabbedPane tabbedPane;

    protected void addChangeListener(ChangeListener listener) {
        tabbedPane.addChangeListener(listener);
    }

    protected void removeChangeListener(ChangeListener listener) {
        tabbedPane.removeChangeListener(listener);
    }

    protected TabbedView(Inspection inspection, ViewKind viewKind, String geometrySettingsKey) {
        super(inspection, viewKind, geometrySettingsKey);
        tabbedPane = new InspectorTabbedPane(inspection);
        addChangeListener(tabChangeListener);
    }

    private final ChangeListener tabChangeListener = new ChangeListener() {

        public void stateChanged(ChangeEvent event) {
            // A view tab has become visible that was not visible before.
            if (vm().state().processState() == MaxProcessState.TERMINATED) {
                // Tabbed views can "become visible" during the shutdown sequence after the VM process dies and when
                // the tabbed views are being closed; do nothing in that situation.
            }
            final InspectorView selectedView = getSelected();
            if (selectedView != null) {
                // View may not have been getting refreshed while not visible.
                selectedView.forceRefresh();
                setTitle();
            }
        }
    };

    public InspectorView viewAt(int i) {
        final Component component = tabbedPane.getComponentAt(i);
        if (component instanceof InspectorInternalFrame) {
            final InspectorFrame inspectorFrame = (InspectorFrame) component;
            //final Class<Member_Type> type = null;
            //return Utils.cast(type, inspectorFrame.view());
            return inspectorFrame.view();
        }
        return null;
    }

    public int length() {
        return tabbedPane.getTabCount();
    }

    public int getSelectedIndex() {
        return tabbedPane.getSelectedIndex();
    }

    public void setSelected(InspectorView view) {
        moveToFront();
        tabbedPane.setSelectedComponent(view.getJComponent());
    }

    public boolean isSelected(InspectorView view) {
        return view.getJComponent() ==  tabbedPane.getSelectedComponent();
    }

    public InspectorView getSelected() {
        final Component component =  tabbedPane.getSelectedComponent();
        for (InspectorView  view : this) {
            if (view.getJComponent() == component) {
                return view;
            }
        }
        return null;
    }

    public void add(AbstractView view) {
        add(view, view.getTitle());
    }

    public void add(AbstractView view, String tabTitle) {
        add(view, tabTitle, null);
    }

    public void add(AbstractView view, String tabTitle, String tabToolTipText) {
        views.add(view);
        final JComponent component = view.getJComponent();
        tabbedPane.addTab(tabTitle, component);
        if (tabToolTipText != null) {
            final int index = tabbedPane.indexOfComponent(component);
            tabbedPane.setToolTipTextAt(index, tabToolTipText);
        }
        assert views.size() == length();
    }

    /**
     * Extends the tab of this component with a small icon that, when clicked, will close the view.
     * Using this has the curious side-effect that the tabbed pane has additional children that aren't the actual
     * tabbed frames.
     */
    public void addCloseIconToTab(AbstractView view, String toolTipText) {

        ButtonTabComponent tabRenderer = new ButtonTabComponent(inspection(), this, view, toolTipText);
        final int index = tabbedPane.indexOfComponent(view.getJComponent());
        tabbedPane.setTabComponentAt(index, tabRenderer);
    }

    /**
     * Removes a component that was added as a tab.
     * <br>
     * Note that this happens automatically for a
     * {@link JInternalFrame} when {@link JInternalFrame#dispose()}
     * is called, but not for other kinds of components that
     * might be put into a tab.
     *
     * @param component a component that was added as tab
     */
    public void remove(JComponent component) {
        tabbedPane.remove(component);
    }

    public Iterator<AbstractView> iterator() {
        return views.iterator();
    }

    @Override
    public void createViewContent() {
        setContentPane(tabbedPane);
    }

    @Override
    public void viewConfigurationChanged() {
        // These containers generally have no view configurations that need updating.
    }

    /**
     * Initiates disposal of the specified view, presumed to be a tabbed member of this view.
     * If no tabs remain, dispose of the whole thing.
     */
    public void close(InspectorView view) {
        assert views.size() == length();
        views.remove(view);
        view.dispose();
    }

    /**
     * Disposes of all but the specified view, presumed to be a tabbed member of this view.
     */
    public void closeOthers(InspectorView keepView) {
        final List<AbstractView> toClose = new ArrayList<AbstractView>();
        for (AbstractView view : views) {
            if (view != keepView) {
                toClose.add(view);
            }
        }
        for (InspectorView view : toClose) {
            close(view);
        }
    }

    /**
     * Receives notification that the window system is closing this view.
     */
    @Override
    public void viewClosing() {
        removeChangeListener(tabChangeListener);
        for (InspectorView view : this) {
            view.dispose();
        }
        super.viewClosing();
    }

}

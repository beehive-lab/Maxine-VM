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
package com.sun.max.ins.view;

import java.util.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.view.InspectionViews.ViewKind;

/**
 * Abstract manager for a kind of Inspector view that is a singleton which can either be active (the instance
 * exists and is visible) or not.
 * <p>
 * Subclasses must implement {@link SingletonViewManager#activateView()} and set the view in this abstract
 * class.
 *
 * @author Michael Van De Vanter
 * @param <Inspector_Kind> a kind of Inspector that is to be managed as a singleton view
 */
public abstract class AbstractSingletonViewManager<Inspector_Kind extends Inspector> extends AbstractInspectionHolder implements SingletonViewManager {

    private final ViewKind viewKind;
    private final String shortName;
    private final String longName;

    /**
     * The active inspector instance; null when inactive.
     */
    private ArrayList<Inspector_Kind> inspectors = new ArrayList<Inspector_Kind>(1);

    private final InspectorAction activateViewAction;
    private final InspectorAction deactivateAllAction;

    protected AbstractSingletonViewManager(Inspection inspection, ViewKind viewKind, String shortName, String longName) {
        super(inspection);
        this.viewKind = viewKind;
        this.shortName = shortName;
        this.longName = longName;
        this.activateViewAction = new ActivateViewAction(shortName);
        this.deactivateAllAction = new DeactivateAllAction(shortName);
    }

    public final ViewKind viewKind() {
        return viewKind;
    }

    public final String shortName() {
        return shortName;
    }

    public final String longName() {
        return longName;
    }

    public final boolean isSingleton() {
        return true;
    }

    public final boolean isActive() {
        return inspectors.size() > 0;
    }

    public final List<Inspector_Kind> activeViews() {
        return inspectors;
    }

    public Inspector_Kind activateView() {
        if (inspectors.size() == 0) {
            inspectors.add(createView(inspection()));
            refresh();
        }
        return inspectors.get(0);
    }

    public final void deactivateView() {
        assert inspectors.size() == 1;
        inspectors.get(0).dispose();
    }

    public InspectorAction activateSingletonViewAction() {
        return activateViewAction;
    }

    public InspectorAction deactivateAllAction(Inspector exception) {
        if (exception == null) {
            return deactivateAllAction;
        }
        return new DeactivateAllExceptAction(shortName, exception);
    }

    public final void notifyViewClosing(Inspector inspector) {
        assert inspectors.remove(inspector);
        refresh();
    }

    protected abstract Inspector_Kind createView(Inspection inspection);

    /**
     * Update any internal state on occasion of view activation/deactivation.
     */
    private void refresh() {
        deactivateAllAction.refresh(true);
    }

    /**
     * Action: makes visible and highlights a singleton view.
     * <p>
     * Note that this action is enabled, even when the view is already
     * activated (visible); in that case it serves to bring the view
     * forward and highlight it.
     */
    private final class ActivateViewAction extends InspectorAction {

        public ActivateViewAction(String title) {
            super(inspection(), "View " + title);
            refresh(true);
        }

        @Override
        protected void procedure() {
            activateView().highlight();
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(isEnabled());
        }
    }

    /**
     * Action: deactivate all views, which in the case of a singleton
     * means only the one view, if already activated.
     */
    private final class DeactivateAllAction extends InspectorAction {

        public DeactivateAllAction(String title) {
            super(inspection(), "Close " + title + " view");
        }

        @Override
        protected void procedure() {
            if (isActive()) {
                deactivateView();
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(isActive());
        }
    }

    /**
     * Action: deactivate the singleton view, with one exception, which might
     * be the singleton view itself.
     */
    private final class DeactivateAllExceptAction extends InspectorAction {

        private final Inspector exceptInspector;

        public DeactivateAllExceptAction(String title, Inspector exceptInspector) {
            super(inspection(), "Close " + title + " view");
            this.exceptInspector = exceptInspector;
        }

        @Override
        protected void procedure() {
            if (isActive() && !inspectors.get(0).equals(exceptInspector)) {
                deactivateView();
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(isActive());
        }
    }
}

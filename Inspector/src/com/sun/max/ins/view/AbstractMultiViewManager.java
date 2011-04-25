/*
 * Copyright (c) 2011, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * Abstract manager for a kind of Inspector view that can occur in multiple
 * instances.
 *
 * @author Michael Van De Vanter
 */
public abstract class AbstractMultiViewManager<Inspector_Kind extends Inspector> extends AbstractInspectionHolder implements MultiViewManager, InspectionListener {

    private final ViewKind viewKind;
    private final String shortName;
    private final String longName;

    private final ArrayList<Inspector_Kind> inspectors = new ArrayList<Inspector_Kind>();

    private final InspectorAction deactivateAllAction;

    protected AbstractMultiViewManager(Inspection inspection, ViewKind viewKind, String shortName, String longName) {
        super(inspection);
        this.viewKind = viewKind;
        this.shortName = shortName;
        this.longName = longName;
        this.deactivateAllAction = new DeactivateAllAction(shortName);
        refresh();
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
        return false;
    }

    public final boolean isActive() {
        return inspectors.size() > 0;
    }

    public List<Inspector_Kind> activeViews() {
        return inspectors;
    }

    public void deactivateAllViews() {
        for (Inspector inspector : new ArrayList<Inspector_Kind>(inspectors)) {
            inspector.dispose();
        }
        refresh();
        assert !isActive();
    }

    public InspectorAction deactivateAllAction(Inspector exception) {
        if (exception == null) {
            return deactivateAllAction;
        }
        return new DeactivateAllExceptAction(shortName, exception);
    }

    public void notifyViewClosing(Inspector inspector) {
        assert inspectors.remove(inspector);
        refresh();
    }

    protected void notifyAddingView(Inspector_Kind inspector) {
        assert inspectors.add(inspector);
        refresh();
    }

    public void vmStateChanged(boolean force) {
    }

    public void breakpointStateChanged() {
    }

    public void watchpointSetChanged() {
    }

    public void viewConfigurationChanged() {
    }

    public void vmProcessTerminated() {
    }

    public void inspectionEnding() {
    }

    public void deactivateAllViewsExcept(Inspector exceptInspector) {
        for (Inspector inspector : new ArrayList<Inspector_Kind>(inspectors)) {
            if (!inspector.equals(exceptInspector)) {
                inspector.dispose();
            }
        }
        refresh();
    }

    /**
     * Update any internal state on occasion of view activation/deactivation.
     */
    private void refresh() {
        deactivateAllAction.refresh(true);
    }

    private final class DeactivateAllAction extends InspectorAction {

        public DeactivateAllAction(String title) {
            super(inspection(), "Close all " + title + " views");
        }

        @Override
        protected void procedure() {
            deactivateAllViews();
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(isActive());
        }
    }

    private final class DeactivateAllExceptAction extends InspectorAction {

        private final Inspector exceptInspector;

        public DeactivateAllExceptAction(String title, Inspector exceptInspector) {
            super(inspection(), "Close other " + title + " views");
            this.exceptInspector = exceptInspector;
        }

        @Override
        protected void procedure() {
            deactivateAllViewsExcept(exceptInspector);
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(isActive());
        }
    }
}

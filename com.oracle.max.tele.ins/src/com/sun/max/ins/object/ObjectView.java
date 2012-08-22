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
package com.sun.max.ins.object;

import static com.sun.max.tele.MaxProcessState.*;

import java.awt.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.gui.TableColumnVisibilityPreferences.TableColumnViewPreferenceListener;
import com.sun.max.ins.view.InspectionViews.ViewKind;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;

/**
 * A view that displays the content of a low level heap object in the VM.
 */
public abstract class ObjectView<View_Type extends ObjectView> extends AbstractView<View_Type> {

    private static final int MAX_TITLE_STRING_LENGTH = 40;
    private static final int TRACE_VALUE = 1;
    private static final ViewKind VIEW_KIND = ViewKind.OBJECT;

    private static ObjectViewManager viewManager;

    public static ObjectViewManager makeViewManager(Inspection inspection) {
        if (viewManager == null) {
            viewManager = new ObjectViewManager(inspection);
        }
        return viewManager;
    }

    private MaxObject object;

    /** The origin is an actual location in memory of the VM;
     * keep a copy for comparison, since it might change via GC.
     */
    private Pointer currentObjectOrigin;

    /**
     * @return The actual location in VM memory where
     * the object resides at present; this may change via GC.
     */
    Pointer currentOrigin() {
        return currentObjectOrigin;
    }

    private Color backgroundColor = null;

    /**
     * Cache of the most recent update to a textual description of the object; needed in situations where the frame
     * becomes unavailable. This cache does not include the object state modifier or region information.
     */
    private String objectDescription = "";

    private InspectorTable objectHeaderTable;

    protected final ObjectViewPreferences instanceViewPreferences;

    private Rectangle originalFrameGeometry = null;

    private InspectorMenu objectMenu;
    private InspectorAction visitStaticTupleAction = null;
    private InspectorAction visitForwardedToAction = null;
    private InspectorAction visitForwardedFromAction = null;

    protected ObjectView(final Inspection inspection, final MaxObject object) {
        super(inspection, VIEW_KIND, null);
        this.object = object;
        this.currentObjectOrigin = object().origin();
        instanceViewPreferences = new ObjectViewPreferences(ObjectViewPreferences.globalPreferences(inspection)) {
            @Override
            protected void setShowHeader(boolean showHeader) {
                super.setShowHeader(showHeader);
                reconstructView();
            }
            @Override
            protected void setElideNullArrayElements(boolean hideNullArrayElements) {
                super.setElideNullArrayElements(hideNullArrayElements);
                reconstructView();
            }
        };
        instanceViewPreferences.addListener(new TableColumnViewPreferenceListener() {
            public void tableColumnViewPreferencesChanged() {
                reconstructView();
            }
        });
        Trace.line(TRACE_VALUE, tracePrefix() + " creating for " + getTextForTitle());
    }

    @Override
    public InspectorFrame createFrame(boolean addMenuBar) {
        final InspectorFrame frame = super.createFrame(addMenuBar);
        gui().setLocationRelativeToMouse(this, preference().geometry().newFrameDiagonalOffset());
        originalFrameGeometry = getGeometry();
        return frame;
    }

    @Override
    protected void createViewContent() {
        final JPanel panel = new InspectorPanel(inspection(), new BorderLayout());
        if (instanceViewPreferences.showHeader()) {
            objectHeaderTable = new ObjectHeaderTable(inspection(), this);
            objectHeaderTable.setBorder(preference().style().defaultPaneBottomBorder());
            // Will add without column headers
            panel.add(objectHeaderTable, BorderLayout.NORTH);
        }

        setContentPane(panel);

        // Populate menu bar
        final InspectorMenu defaultMenu = makeMenu(MenuKind.DEFAULT_MENU);
        defaultMenu.add(defaultMenuItems(MenuKind.DEFAULT_MENU));
        defaultMenu.addSeparator();
        defaultMenu.add(views().deactivateOtherViewsAction(ViewKind.OBJECT, this));
        defaultMenu.add(views().deactivateAllViewsAction(ViewKind.OBJECT));

        final InspectorMenu memoryMenu = makeMenu(MenuKind.MEMORY_MENU);
        memoryMenu.add(views().memory().makeViewAction(object, "View this object's memory"));
        if (vm().heap().providesHeapRegionInfo()) {
            // TODO: Need to revisit this to better integrate with the Views framework, e.g., have something like:
            // views().heapRegionInfo().makeViewAction(...). This requires adding a factory and other boiler plate.
            InspectorAction action = HeapRegionInfoView.viewManager(inspection()).makeViewAction(object, "View this object's heap region info");
            memoryMenu.add(action);
        }
        if (vm().watchpointManager() != null) {
            memoryMenu.add(actions().setObjectWatchpoint(object, "Watch this object's memory"));
        }
        memoryMenu.add(actions().copyObjectOrigin(object, "Copy this object's origin to clipboard"));
        memoryMenu.add(actions().copyObjectDescription(object, "Copy this object's origin + description to clipboard"));
        memoryMenu.add(defaultMenuItems(MenuKind.MEMORY_MENU));
        memoryMenu.add(views().activateSingletonViewAction(ViewKind.ALLOCATIONS));

        // Ensure that the object menu appears in the right position, but defer its creation
        // to subclasses, so that more view-specific items can be prepended to the standard ones.
        objectMenu = makeMenu(MenuKind.OBJECT_MENU);
        visitStaticTupleAction = actions().viewStaticTupleForObject(object);
        objectMenu.add(visitStaticTupleAction);
        visitForwardedToAction = new VisitForwardedToAction(inspection());
        objectMenu.add(visitForwardedToAction);
        visitForwardedFromAction = new VisitForwardedFromAction(inspection());
        objectMenu.add(visitForwardedFromAction);

        makeMenu(MenuKind.CODE_MENU);

        if (object.getTeleClassMethodActorForObject() != null || TeleTargetMethod.class.isAssignableFrom(object.getClass())) {
            makeMenu(MenuKind.DEBUG_MENU);
        }

        final InspectorMenuItems defaultViewMenuItems = defaultMenuItems(MenuKind.VIEW_MENU);
        final InspectorMenu viewMenu = makeMenu(MenuKind.VIEW_MENU);
        final List<InspectorAction> extraViewMenuActions = extraViewMenuActions();
        if (!extraViewMenuActions.isEmpty()) {
            for (InspectorAction action : extraViewMenuActions) {
                viewMenu.add(action);
            }
            viewMenu.addSeparator();
        }
        viewMenu.add(defaultViewMenuItems);
        refreshBackgroundColor();
    }

    @Override
    public Rectangle defaultGeometry() {
        return originalFrameGeometry;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Constructs a full description of the object, including a possible prefix, the
     * title of the object, and several possible suffixes.
     */
    @Override
    public final String getTextForTitle() {
        final StringBuilder titleText = new StringBuilder();
        refreshObjectDescription();
        final ObjectStatus status = object.status();
        if (!status.isLive()) {
            // Add a prefix describing status, but omit for the normal case (LIVE).
            titleText.append("(").append(status.label());
            if (status.isDead()) {
                // If DEAD and was previously a quasi object, note this
                final ObjectStatus priorStatus = object.reference().priorStatus();
                if (priorStatus != null && priorStatus.isQuasi()) {
                    titleText.append("-").append(priorStatus.label());
                }
            }
            titleText.append(") ");
        }
        titleText.append(objectDescription);
        if (isElided()) {
            titleText.append("(ELIDED)");
        }
        final MaxMemoryRegion memoryRegion = vm().state().findMemoryRegion(currentObjectOrigin);
        titleText.append(" in ").append(memoryRegion == null ? "unknown region" : memoryRegion.regionName());
        return titleText.toString();
    }

    @Override
    public InspectorAction getViewOptionsAction() {
        return new InspectorAction(inspection(), "View Options") {
            @Override
            public void procedure() {
                final ObjectViewPreferences globalPreferences = ObjectViewPreferences.globalPreferences(inspection());
                new TableColumnVisibilityPreferences.ColumnPreferencesDialog<ObjectColumnKind>(inspection(), "View Options", instanceViewPreferences, globalPreferences);
            }
        };
    }

    @Override
    public void threadFocusSet(MaxThread oldThread, MaxThread thread) {
        // Object view displays are sensitive to the current thread selection.
        forceRefresh();
    }

    @Override
    public void addressFocusChanged(Address oldAddress, Address newAddress) {
        forceRefresh();
    }

    @Override
    public void viewGetsWindowFocus() {
        if (object != focus().object()) {
            focus().setHeapObject(object);
        }
        super.viewGetsWindowFocus();
    }

    @Override
    public void viewLosesWindowFocus() {
        if (object == focus().object()) {
            focus().setHeapObject(null);
        }
        super.viewLosesWindowFocus();
    }

    @Override
    public void viewClosing() {
        if (object == focus().object()) {
            focus().setHeapObject(null);
        }
        super.viewClosing();
    }

    @Override
    public void watchpointSetChanged() {
        // TODO (mlvdv)  patch for concurrency issue; not completely safe
        if (vm().state().processState() == STOPPED) {
            forceRefresh();
        }
    }

    @Override
    public void vmProcessTerminated() {
        dispose();
    }

    @Override
    protected void refreshState(boolean force) {
        if (object.reference().forwardedFrom().equals(currentOrigin())) {
            /*
             * The object has just been forwarded, and this view was previously showing what is now the old copy. By
             * policy, we want this view to stick on the old location, so find the "forwarder" object that represents
             * the old copy and reset this view to display that object.
             */
            final MaxObject forwarderObject = vm().objects().findQuasiObjectAt(object.reference().forwardedFrom());
            if (forwarderObject != null) {
                final MaxObject oldObject = object;
                object = forwarderObject;
                viewManager.resetObjectToViewMapEntry(oldObject, forwarderObject, this);
                reconstructView();
            }
        } else if (!object.origin().equals(currentObjectOrigin)) {
            // The object has just been relocated in memory; reset this view to display the new copy of the object.
            currentObjectOrigin = object.origin();
            reconstructView();
        }

        if (objectHeaderTable != null) {
            objectHeaderTable.refresh(force);
        }
        visitForwardedToAction.refresh(force);
        visitForwardedFromAction.refresh(force);
        refreshBackgroundColor();
        setTitle();
    }

    /**
     * @return local surrogate for the VM object being inspected in this object view
     */
    public MaxObject object() {
        return object;
    }

    /**
     * @return the view preferences currently in effect for this object view
     */
    public ObjectViewPreferences viewPreferences() {
        return instanceViewPreferences;
    }

    /**
     * @return a color to use for background, especially cell backgrounds, in the object view; {@code null} if default color should be used.
     */
    public Color viewBackgroundColor() {
        return backgroundColor;
    }

    /**
     * Constructs a string that identifies the object being viewed.
     */
    private void refreshObjectDescription() {
        final ObjectStatus status = object.status();
        if (status.isNotDead()) {
            // Revise the title of the object if we still can
            final StringBuilder sb = new StringBuilder();
            if (status.isLive()) {
                sb.append("Object: ");
            } else {
                sb.append("Quasi object: ");
            }
            sb.append(object.origin().toHexString());
            sb.append(inspection().nameDisplay().referenceLabelText(object, MAX_TITLE_STRING_LENGTH));
            objectDescription = sb.toString();
        }
    }

    /**
     * Changes the background color setting for this view, depending on object status.
     *
     * @return {@code true} iff color has changed
     */
    private boolean refreshBackgroundColor() {
        final Color oldBackgroundColor = backgroundColor;
        final ObjectStatus status = object.status();
        if (status.isLive()) {
            backgroundColor = null;
        } else if (status.isQuasi()) {
            backgroundColor = preference().style().quasiObjectBackgroundColor();
        } else { // DEAD
            backgroundColor = preference().style().deadObjectBackgroundColor();
        }
        setStateColor(backgroundColor);
        objectHeaderTable.setBackground(backgroundColor);
        return backgroundColor != oldBackgroundColor;
    }


    /**
     * Gets any view-specific actions that should appear on the {@link MenuKind#VIEW_MENU}.
     */
    protected List<InspectorAction> extraViewMenuActions() {
        return Collections.emptyList();
    }

    /**
     * @return whether the display mode is hiding some of the members of the object
     */
    protected boolean isElided() {
        return false;
    }

    private final class VisitForwardedToAction extends InspectorAction {

        private MaxObject forwardedToObject;

        public VisitForwardedToAction(Inspection inspection) {
            super(inspection, "View object forwarded to");
            refresh(true);
        }

        @Override
        protected void procedure() {
            focus().setHeapObject(forwardedToObject);
        }

        @Override
        public void refresh(boolean force) {
            super.refresh(force);
            forwardedToObject = null;
            final Address toAddress = object.reference().forwardedTo();
            if (toAddress.isNotZero()) {
                forwardedToObject = vm().objects().findObjectAt(toAddress);
            }
            setEnabled(forwardedToObject != null);
        }
    }

    private final class VisitForwardedFromAction extends InspectorAction {

        private MaxObject forwardedFromObject;

        public VisitForwardedFromAction(Inspection inspection) {
            super(inspection, "View object forwarded from");
            refresh(true);
        }

        @Override
        protected void procedure() {
            focus().setHeapObject(forwardedFromObject);
        }

        @Override
        public void refresh(boolean force) {
            super.refresh(force);
            forwardedFromObject = null;
            final Address fromAddress = object.reference().forwardedFrom();
            if (fromAddress.isNotZero()) {
                forwardedFromObject = vm().objects().findQuasiObjectAt(fromAddress);
            }
            setEnabled(forwardedFromObject != null);
        }
    }
}

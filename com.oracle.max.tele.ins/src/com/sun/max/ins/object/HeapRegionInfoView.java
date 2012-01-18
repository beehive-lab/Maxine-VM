/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;

import javax.swing.event.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.view.*;
import com.sun.max.ins.view.InspectionViews.ViewKind;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.gcx.*;

/**
 * An object view specialized for {@link HeapRegionInfo} object. This adds a pane to the ObjectView for information derived from {@link HeapRegionInfo} fields to
 * ease debugging regionalized heap management. In particular, the pane display the region's id, the start and end addresses for the region, and the address to its first
 * free chunk, if there is one.
 */
public final class HeapRegionInfoView  extends ObjectView<HeapRegionInfoView> {
    private InspectorTabbedPane tabbedPane;
    private ObjectScrollPane fieldsPane;
    private InspectorScrollPane regionInfoPane;
    private boolean alternateDisplay;

    private static final ViewKind VIEW_KIND = ViewKind.HEAP_REGION_INFO;
    private static final String SHORT_NAME = "Heap Region Info";
    private static final String LONG_NAME = "Heap Region Info View";

    public static final class HeapRegionInfoViewManager extends AbstractMultiViewManager<HeapRegionInfoView> {
        private final InspectorAction interactiveViewRegionInfoByAddressAction;
        private final InspectorAction interactiveViewRegionInfoByRegionIDAction;
        private final List<InspectorAction> makeViewActions;

        protected HeapRegionInfoViewManager(Inspection inspection) {
            super(inspection, VIEW_KIND, SHORT_NAME, LONG_NAME);
            makeViewActions = new ArrayList<InspectorAction>(1);

            interactiveViewRegionInfoByAddressAction = new InspectorAction(inspection(),  "View RegionInfo for address...") {
                @Override
                protected void procedure() {
                    new AddressInputDialog(inspection(), Address.zero(), "View RegionInfo for address...", "View") {
                        @Override
                        public void entered(Address address) {
                            MaxMemoryManagementInfo info = vm().heap().getMemoryManagementInfo(address);
                            // TODO: revisit this.
                            if (info.status().equals(MaxMemoryStatus.LIVE)) {
                                final TeleObject teleObject = info.tele();
                                focus().setHeapObject(teleObject);
                            } else {
                                gui().errorMessage("Heap Region Info not found for address "  + address.to0xHexString());
                            }
                        }
                    };
                }
            };
            interactiveViewRegionInfoByRegionIDAction = new InspectorAction(inspection(),  "View RegionInfo for region ID ...") {
                @Override
                protected void procedure() {
                    final String input = gui().inputDialog("View RegionInfo for region ID...", "0");
                    if (input == null) {
                        // User clicked cancel
                        return;
                    }
                    try {
                        final int regionID = Integer.parseInt(input);
                        if (TeleRegionTable.theTeleRegionTable().isValidRegionID(regionID)) {
                            Address regionInfoAddress = TeleRegionTable.theTeleRegionTable().regionInfo(regionID);
                            final TeleObject teleObject = vm().objects().findObjectAt(regionInfoAddress);
                            if (teleObject != null && teleObject instanceof TeleHeapRegionInfo) {
                                focus().setHeapObject(teleObject);
                            }
                        } else {
                            gui().errorMessage("Not a valid region ID"  + input);
                        }
                    } catch (NumberFormatException numberFormatException) {
                        gui().errorMessage("Not a region ID: " + input);
                    }
                }
            };
            makeViewActions.add(interactiveViewRegionInfoByAddressAction);
            makeViewActions.add(interactiveViewRegionInfoByRegionIDAction);
        }

        public InspectorAction makeViewAction(final TeleObject teleObject, String actionTitle) {
            return new InspectorAction(inspection(), actionTitle == null ? "View Heap Region Info" : actionTitle) {
                @Override
                protected void procedure() {
                    TeleHeapRegionInfo teleHeapRegionInfo = (TeleHeapRegionInfo) vm().heap().getMemoryManagementInfo(teleObject.origin()).tele();
                    if (teleHeapRegionInfo != null) {
                        focus().setHeapObject(teleHeapRegionInfo);
                    }
                }
            };
        }

        @Override
        protected List<InspectorAction> makeViewActions() {
            return makeViewActions;
        }
    }

    private static HeapRegionInfoViewManager viewManager;

    public static HeapRegionInfoViewManager viewManager(Inspection inspection) {
        if (viewManager == null) {
            viewManager = new HeapRegionInfoViewManager(inspection);
        }
        return viewManager;
    }

    HeapRegionInfoView(Inspection inspection, TeleObject teleObject) {
        super(inspection, teleObject);
        alternateDisplay = true;
        final InspectorFrame frame = createFrame(true);
        final InspectorMenu objectMenu = frame.makeMenu(MenuKind.OBJECT_MENU);
        objectMenu.add(defaultMenuItems(MenuKind.OBJECT_MENU));
    }

    @Override
    protected void createViewContent() {
        super.createViewContent();
        final TeleHeapRegionInfo teleHeapRegionInfo = (TeleHeapRegionInfo) teleObject();
        final String name = teleHeapRegionInfo.classActorForObjectType().javaSignature(false);

        tabbedPane = new InspectorTabbedPane(inspection());

        fieldsPane = ObjectScrollPane.createFieldsPane(inspection(), teleHeapRegionInfo, instanceViewPreferences);
        tabbedPane.add(name, fieldsPane);
        regionInfoPane =  new HeapRegionInfoTable(inspection(), teleHeapRegionInfo).makeHeapRegionInfoPane();
        tabbedPane.add("Region #" + teleHeapRegionInfo.regionID() + " info", regionInfoPane);

        tabbedPane.setSelectedComponent(alternateDisplay ? regionInfoPane : fieldsPane);
        tabbedPane.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent event) {
                final Prober prober = (Prober) tabbedPane.getSelectedComponent();
                // Remember which display is now selected
                alternateDisplay = prober == regionInfoPane;
                // Refresh the display that is now visible.
                prober.refresh(true);
            }
        });
        getContentPane().add(tabbedPane);
    }

    @Override
    protected void refreshState(boolean force) {
        super.refreshState(force);
        if (teleObject().memoryStatus().isNotDeadYet()) {
            // Only refresh the visible pane
            final Prober pane = (Prober) tabbedPane.getSelectedComponent();
            pane.refresh(force);
        }
    }
}

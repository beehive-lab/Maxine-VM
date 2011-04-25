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
package com.sun.max.ins.memory;

import java.awt.*;

import javax.swing.*;

import com.sun.max.gui.*;
import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.value.*;
import com.sun.max.ins.value.WordValueLabel.ValueMode;
import com.sun.max.ins.view.*;
import com.sun.max.ins.view.InspectionViews.ViewKind;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;

/**
 * Renders memory contents, lets you select the start address, etc.
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public final class MemoryBytesInspector extends Inspector {

    private static final int TRACE_VALUE = 2;
    private static final ViewKind VIEW_KIND = ViewKind.MEMORY_BYTES;
    private static final String SHORT_NAME = "Memory as bytes";
    private static final String LONG_NAME = "Memory Bytes Inspector";

    private static MemoryBytesViewManager viewManager;

    public static MemoryBytesViewManager makeViewManager(Inspection inspection) {
        if (viewManager == null) {
            viewManager = new MemoryBytesViewManager(inspection);
        }
        return viewManager;
    }


    public static final class MemoryBytesViewManager extends AbstractMultiViewManager<MemoryBytesInspector> implements MemoryBytesViewFactory {

        private final InspectorAction interactiveMakeViewAction;

        protected MemoryBytesViewManager(final Inspection inspection) {
            super(inspection, VIEW_KIND, SHORT_NAME, LONG_NAME);
            Trace.begin(TRACE_VALUE, tracePrefix() + "creating");
            interactiveMakeViewAction = new InspectorAction(inspection(), "Inspect memory bytes at address...") {

                @Override
                protected void procedure() {
                    new AddressInputDialog(inspection, inspection.vm().bootImageStart(), "Inspect memory bytes at address...", "Inspect") {

                        @Override
                        public void entered(Address address) {
                            makeView(address).highlight();
                        }
                    };
                }
            };
            Trace.end(TRACE_VALUE, tracePrefix() + "creating");
        }

        public boolean isSupported() {
            return true;
        }

        public boolean isEnabled() {
            return true;
        }

        @Override
        public void notifyViewClosing(Inspector inspector) {
            // TODO (mlvdv)  should be using generics here
            final MemoryBytesInspector memoryBytesInspector = (MemoryBytesInspector) inspector;
            super.notifyViewClosing(memoryBytesInspector);
        }

        public MemoryBytesInspector makeView(Address address) {
            final MemoryBytesInspector memoryBytesInspector = new MemoryBytesInspector(inspection(), address, 64, 1, 8);
            notifyAddingView(memoryBytesInspector);
            return memoryBytesInspector;
        }

        public MemoryBytesInspector makeView(TeleObject teleObject) {
            final MaxMemoryRegion region = teleObject.objectMemoryRegion();
            final long nBytes = region.nBytes();
            assert nBytes < Integer.MAX_VALUE;
            final MemoryBytesInspector memoryBytesInspector = new MemoryBytesInspector(inspection(), region.start(), (int) nBytes, 1, 16);
            notifyAddingView(memoryBytesInspector);
            return memoryBytesInspector;
        }

        public InspectorAction makeViewAction() {
            return interactiveMakeViewAction;
        }

        public InspectorAction makeViewAction(final Address address, String actionTitle) {
            return new InspectorAction(inspection(), actionTitle == null ? "Inspect memory as bytes" : actionTitle) {

                @Override
                protected void procedure() {
                    makeView(address);
                }
            };
        }
    }

    // TODO (mlvdv) actions should be enabled by memory being available to read
    private final Rectangle originalFrameGeometry;
    private Address address;
    private int numberOfGroups;
    private int numberOfBytesPerGroup;
    private int numberOfGroupsPerLine;

    /**
     * @param inspection
     * @param address
     * @param numberOfGroups
     * @param numberOfBytesPerGroup
     * @param numberOfGroupsPerLine
     */
    private MemoryBytesInspector(Inspection inspection, Address address, int numberOfGroups, int numberOfBytesPerGroup, int numberOfGroupsPerLine) {
        super(inspection, VIEW_KIND, null);
        this.address = address;
        this.numberOfGroups = numberOfGroups;
        this.numberOfBytesPerGroup = numberOfBytesPerGroup;
        this.numberOfGroupsPerLine = numberOfGroupsPerLine;
        Trace.begin(TRACE_VALUE, tracePrefix() + " creating for " + getTextForTitle());

        final InspectorFrame frame = createFrame(true);
        final InspectorMenu defaultMenu = frame.makeMenu(MenuKind.DEFAULT_MENU);
        defaultMenu.add(defaultMenuItems(MenuKind.DEFAULT_MENU));
        defaultMenu.addSeparator();
        defaultMenu.add(views().deactivateOtherViewsAction(ViewKind.MEMORY_BYTES, this));
        defaultMenu.add(views().deactivateAllViewsAction(ViewKind.MEMORY_BYTES));

        final InspectorMenu memoryMenu = frame.makeMenu(MenuKind.MEMORY_MENU);
        memoryMenu.add(defaultMenuItems(MenuKind.MEMORY_MENU));
        memoryMenu.add(actions().activateSingletonView(ViewKind.ALLOCATIONS));

        frame.makeMenu(MenuKind.VIEW_MENU).add(defaultMenuItems(MenuKind.VIEW_MENU));
        inspection.gui().setLocationRelativeToMouse(this, inspection().geometry().newFrameDiagonalOffset());
        originalFrameGeometry = getGeometry();
        Trace.end(TRACE_VALUE, tracePrefix() + " creating for " + getTextForTitle());
    }

    private JComponent createController() {
        final JPanel controller = new InspectorPanel(inspection(), new SpringLayout());

        controller.add(new TextLabel(inspection(), "start:"));
        final AddressInputField.Hex addressField = new AddressInputField.Hex(inspection(), address) {
            @Override
            public void update(Address a) {
                if (!a.equals(address)) {
                    address = a;
                    MemoryBytesInspector.this.reconstructView();
                }
            }
        };
        controller.add(addressField);

        controller.add(new TextLabel(inspection(), "bytes/group:"));
        final AddressInputField.Decimal numberOfBytesPerGroupField = new AddressInputField.Decimal(inspection(), Address.fromInt(numberOfBytesPerGroup)) {
            @Override
            public void update(Address value) {
                if (!value.equals(numberOfBytesPerGroup)) {
                    numberOfBytesPerGroup = value.toInt();
                    MemoryBytesInspector.this.reconstructView();
                }
            }
        };
        numberOfBytesPerGroupField.setRange(1, 16);
        controller.add(numberOfBytesPerGroupField);

        controller.add(new TextLabel(inspection(), "groups:"));
        final AddressInputField.Decimal numberOfGroupsField = new AddressInputField.Decimal(inspection(), Address.fromInt(numberOfGroups)) {
            @Override
            public void update(Address value) {
                if (!value.equals(numberOfGroups)) {
                    numberOfGroups = value.toInt();
                    MemoryBytesInspector.this.reconstructView();
                }
            }
        };
        numberOfGroupsField.setRange(1, 1024);
        controller.add(numberOfGroupsField);

        controller.add(new TextLabel(inspection(), "groups/line:"));
        final AddressInputField.Decimal numberOfGroupsPerLineField = new AddressInputField.Decimal(inspection(), Address.fromInt(numberOfGroupsPerLine)) {
            @Override
            public void update(Address value) {
                if (!value.equals(numberOfGroupsPerLine)) {
                    numberOfGroupsPerLine = value.toInt();
                    MemoryBytesInspector.this.reconstructView();
                }
            }
        };
        numberOfGroupsPerLineField.setRange(1, 256);
        controller.add(numberOfGroupsPerLineField);

        SpringUtilities.makeCompactGrid(controller, 4);
        return controller;
    }

    private TextLabel[] memoryLabels;
    // Char labels displayed as Word data (with fixed width font) so that horizontal alignment works
    private TextLabel[] charLabels;

    @Override
    protected void refreshState(boolean force) {
        final byte[] bytes = new byte[numberOfBytesPerGroup];
        for (int i = 0; i < numberOfGroups; i++) {
            final Address address = this.address.plus(i * numberOfBytesPerGroup);
            vm().readBytes(address, bytes);
            memoryLabels[i].setText(byteGroupToString(bytes));
            memoryLabels[i].setToolTipText(address.toHexString());
            switch (numberOfBytesPerGroup) {
                case 1: {
                    final char ch = (char) bytes[0];
                    charLabels[i].setText(Character.toString(ch));
                    break;
                }
                case 2: {
                    final char ch = (char) ((bytes[1] * 256) + bytes[0]);
                    charLabels[i].setText(Character.toString(ch));
                    break;
                }
                default: {
                    break;
                }
            }
        }
    }

    private JPanel contentPane;

    @Override
    protected Rectangle defaultGeometry() {
        return originalFrameGeometry;
    }

    @Override
    public String getTextForTitle() {
        return MemoryBytesInspector.class.getSimpleName() + ": " + address.toHexString();
    }

    @Override
    protected void createView() {

        contentPane = new InspectorPanel(inspection());
        setContentPane(contentPane);
        contentPane.removeAll();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        contentPane.add(createController());

        final JPanel view = new InspectorPanel(inspection(), new SpringLayout());
        contentPane.add(view);

        int numberOfLines = numberOfGroups / numberOfGroupsPerLine;
        if (numberOfGroups % numberOfGroupsPerLine != 0) {
            numberOfLines++;
        }
        final int numberOfLabels = numberOfLines * numberOfGroupsPerLine;

        memoryLabels = new TextLabel[numberOfLabels];
        charLabels = new TextLabel[numberOfLabels];
        final String space = Strings.times(' ', 2 * numberOfBytesPerGroup);

        Address lineAddress = address;
        final int numberOfBytesPerLine = numberOfGroupsPerLine * numberOfBytesPerGroup;

        for (int line = 0; line < numberOfLines; line++) {
            final ValueLabel lineAddressLabel = new WordValueLabel(inspection(), ValueMode.WORD, lineAddress, null);
            view.add(lineAddressLabel);
            lineAddress = lineAddress.plus(numberOfBytesPerLine);
            for (int group = 0; group < numberOfGroupsPerLine; group++) {
                final int index = (line * numberOfGroupsPerLine) + group;
                memoryLabels[index] = new TextLabel(inspection(), space);
                view.add(memoryLabels[index]);
            }
            final Space leftSpace = new Space();
            view.add(leftSpace);
            for (int group = 0; group < numberOfGroupsPerLine; group++) {
                final int index = (line * numberOfGroupsPerLine) + group;
                charLabels[index] = new TextLabel(inspection(), space);
                view.add(charLabels[index]);
            }
        }

        forceRefresh();
        SpringUtilities.makeCompactGrid(view, numberOfLines * 2, 1 + numberOfGroupsPerLine, 0, 0, 5, 5);
    }

    public void viewConfigurationChanged() {
        reconstructView();
    }

    private String byteGroupToString(byte[] bytes) {
        String s = "";
        switch (vm().bootImage().header.endianness()) {
            case LITTLE:
                for (int i = bytes.length - 1; i >= 0; i--) {
                    s += String.format("%02X", bytes[i]);
                }
                break;
            case BIG:
                for (int i = 0; i < bytes.length; i++) {
                    s += String.format("%02X", bytes[i]);
                }
                break;
        }
        return s;
    }

    @Override
    public void inspectorClosing() {
        // don't try to recompute the title, just get the one that's been in use
        Trace.line(1, tracePrefix() + " closing for " + getTitle() + " - process terminated");
        super.inspectorClosing();
    }

    @Override
    public void vmProcessTerminated() {
        dispose();
    }

}

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
package com.sun.max.ins.memory;

import javax.swing.*;

import com.sun.max.collect.*;
import com.sun.max.gui.*;
import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.value.*;
import com.sun.max.ins.value.WordValueLabel.*;
import com.sun.max.lang.*;
import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;

/**
 * Renders memory contents, lets you select the start address, etc.
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public final class MemoryBytesInspector extends Inspector {

    private static final IdentityHashSet<MemoryBytesInspector> memoryInspectors = new IdentityHashSet<MemoryBytesInspector>();

    /**
     * Displays a new inspector for a region of memory.
     */
    public static MemoryBytesInspector create(Inspection inspection, Address address, int numberOfGroups, int numberOfBytesPerGroup, int numberOfGroupsPerLine) {
        return new MemoryBytesInspector(inspection, address, numberOfGroups, numberOfBytesPerGroup, numberOfGroupsPerLine);
    }

    /**
     * Displays a new inspector for a region of memory.
     */
    public static MemoryBytesInspector create(Inspection inspection, Address address) {
        return create(inspection, address, 64, 1, 8);
    }

    /**
     * Displays a new inspector for the currently allocated memory of a heap object in the VM.
     */
    public static MemoryBytesInspector create(Inspection inspection, TeleObject teleObject) {
        final MemoryRegion region = teleObject.getCurrentMemoryRegion();
        return create(inspection, region.start(), region.size().toInt(), 1, 16);
    }

    private MemoryBytesInspector(Inspection inspection, Address address, int numberOfGroups, int numberOfBytesPerGroup, int numberOfGroupsPerLine) {
        super(inspection);
        this.address = address;
        this.numberOfGroups = numberOfGroups;
        this.numberOfBytesPerGroup = numberOfBytesPerGroup;
        this.numberOfGroupsPerLine = numberOfGroupsPerLine;

        final InspectorFrame frame = createFrame(true);
        final InspectorMenu defaultMenu = frame.makeMenu(MenuKind.DEFAULT_MENU);
        defaultMenu.add(defaultMenuItems(MenuKind.DEFAULT_MENU));
        defaultMenu.addSeparator();
        defaultMenu.add(actions().closeViews(otherMemoryInspectorsPredicate, "Close other memory byte inspectors"));
        defaultMenu.add(actions().closeViews(allMemoryInspectorsPredicate, "Close all memory byte inspectors"));

        final InspectorMenu memoryMenu = frame.makeMenu(MenuKind.MEMORY_MENU);
        memoryMenu.add(defaultMenuItems(MenuKind.MEMORY_MENU));
        final JMenuItem viewMemoryRegionsMenuItem = new JMenuItem(actions().viewMemoryRegions());
        viewMemoryRegionsMenuItem.setText("View Memory Regions");
        memoryMenu.add(viewMemoryRegionsMenuItem);

        frame.makeMenu(MenuKind.VIEW_MENU).add(defaultMenuItems(MenuKind.VIEW_MENU));

        inspection.gui().setLocationRelativeToMouse(this);
        memoryInspectors.add(this);
        Trace.line(1, tracePrefix() + " creating for " + getTextForTitle());
    }

    private Address address;
    private int numberOfGroups;
    private int numberOfBytesPerGroup;
    private int numberOfGroupsPerLine;

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
    protected void refreshView(boolean force) {
        final byte[] bytes = new byte[numberOfBytesPerGroup];
        for (int i = 0; i < numberOfGroups; i++) {
            final Address address = this.address.plus(i * numberOfBytesPerGroup);
            maxVM().readFully(address, bytes);
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
        super.refreshView(force);
    }

    private JPanel contentPane;

    private static final Predicate<Inspector> allMemoryInspectorsPredicate = new Predicate<Inspector>() {
        public boolean evaluate(Inspector inspector) {
            return inspector instanceof MemoryBytesInspector;
        }
    };

    private final Predicate<Inspector> otherMemoryInspectorsPredicate = new Predicate<Inspector>() {
        public boolean evaluate(Inspector inspector) {
            return inspector instanceof MemoryBytesInspector && inspector != MemoryBytesInspector.this;
        }
    };

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

        refreshView(true);
        SpringUtilities.makeCompactGrid(view, numberOfLines * 2, 1 + numberOfGroupsPerLine, 0, 0, 5, 5);
    }

    public void viewConfigurationChanged() {
        reconstructView();
    }

    private String byteGroupToString(byte[] bytes) {
        String s = "";
        switch (maxVM().bootImage().header.endianness()) {
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

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
import com.sun.max.program.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;

/**
 * Renders memory contents, lets you select the start address, etc.
 *
 * @author Bernd Mathiske
 */
public final class MemoryInspector extends Inspector {

    /**
     * Implemented by something that can inspected by a memory inspector.
     */
    public static interface MemoryInspectable {
        void makeMemoryInspector();

        InspectorAction getMemoryInspectorAction();
    }

    private static final IdentityHashSet<MemoryInspector> _memoryInspectors = new IdentityHashSet<MemoryInspector>();

    /**
     * Displays a new inspector for a region of memory.
     */
    public static MemoryInspector create(Inspection inspection, Address address, int numberOfGroups, int numberOfBytesPerGroup, int numberOfGroupsPerLine) {
        return new MemoryInspector(inspection, address, numberOfGroups, numberOfBytesPerGroup, numberOfGroupsPerLine);
    }

    /**
     * Displays a new inspector for a region of memory.
     */
    public static MemoryInspector create(Inspection inspection, Address address) {
        return create(inspection, address, 10, 8, 1);
    }

    /**
     * Displays a new inspector for the currently allocated memory of a heap object in the {@link TeleVM}.
     */
    public static MemoryInspector create(Inspection inspection, TeleObject teleObject) {
        final Pointer cell = teleObject.getCurrentCell();
        final int size = teleObject.getCurrentSize().toInt();
        return create(inspection, cell, size, 1, 16);
    }

    private MemoryInspector(Inspection inspection, Address address, int numberOfGroups, int numberOfBytesPerGroup, int numberOfGroupsPerLine) {
        super(inspection);
        _address = address;
        _numberOfGroups = numberOfGroups;
        _numberOfBytesPerGroup = numberOfBytesPerGroup;
        _numberOfGroupsPerLine = numberOfGroupsPerLine;
        createFrame(null);
        setLocationRelativeToMouse();
        _memoryInspectors.add(this);
        Trace.line(1, tracePrefix() + " creating for " + getTextForTitle());
    }

    private Address _address;
    private int _numberOfGroups;
    private int _numberOfBytesPerGroup;
    private int _numberOfGroupsPerLine;

    private JComponent createController() {
        final JPanel controller = new InspectorPanel(inspection(), new SpringLayout());

        controller.add(new TextLabel(inspection(), "start:"));
        final AddressInputField.Hex addressField = new AddressInputField.Hex(inspection(), _address) {
            @Override
            public void update(Address address) {
                if (!address.equals(_address)) {
                    _address = address;
                    MemoryInspector.this.reconstructView();
                }
            }
        };
        controller.add(addressField);

        controller.add(new TextLabel(inspection(), "bytes/group:"));
        final AddressInputField.Decimal numberOfBytesPerGroupField = new AddressInputField.Decimal(inspection(), Address.fromInt(_numberOfBytesPerGroup)) {
            @Override
            public void update(Address numberOfBytesPerGroup) {
                if (!numberOfBytesPerGroup.equals(_numberOfBytesPerGroup)) {
                    _numberOfBytesPerGroup = numberOfBytesPerGroup.toInt();
                    MemoryInspector.this.reconstructView();
                }
            }
        };
        numberOfBytesPerGroupField.setRange(1, 16);
        controller.add(numberOfBytesPerGroupField);

        controller.add(new TextLabel(inspection(), "groups:"));
        final AddressInputField.Decimal numberOfGroupsField = new AddressInputField.Decimal(inspection(), Address.fromInt(_numberOfGroups)) {
            @Override
            public void update(Address numberOfGroups) {
                if (!numberOfGroups.equals(_numberOfGroups)) {
                    _numberOfGroups = numberOfGroups.toInt();
                    MemoryInspector.this.reconstructView();
                }
            }
        };
        numberOfGroupsField.setRange(1, 1024);
        controller.add(numberOfGroupsField);

        controller.add(new TextLabel(inspection(), "groups/line:"));
        final AddressInputField.Decimal numberOfGroupsPerLineField = new AddressInputField.Decimal(inspection(), Address.fromInt(_numberOfGroupsPerLine)) {
            @Override
            public void update(Address numberOfGroupsPerLine) {
                if (!numberOfGroupsPerLine.equals(_numberOfGroupsPerLine)) {
                    _numberOfGroupsPerLine = numberOfGroupsPerLine.toInt();
                    MemoryInspector.this.reconstructView();
                }
            }
        };
        numberOfGroupsPerLineField.setRange(1, 256);
        controller.add(numberOfGroupsPerLineField);

        SpringUtilities.makeCompactGrid(controller, 4);
        return controller;
    }

    private TextLabel[] _memoryLabels;
    // Char labels displayed as Word data (with fixed width font) so that horizontal alignment works
    private TextLabel[] _charLabels;

    @Override
    public synchronized void refreshView(long epoch, boolean force) {
        final byte[] bytes = new byte[_numberOfBytesPerGroup];
        for (int i = 0; i < _numberOfGroups; i++) {
            final Address address = _address.plus(i * _numberOfBytesPerGroup);
            teleVM().dataAccess().readFully(address, bytes);
            _memoryLabels[i].setText(byteGroupToString(bytes));
            _memoryLabels[i].setToolTipText(address.toHexString());
            switch (_numberOfBytesPerGroup) {
                case 1: {
                    final char ch = (char) bytes[0];
                    _charLabels[i].setText(Character.toString(ch));
                    break;
                }
                case 2: {
                    final char ch = (char) ((bytes[1] * 256) + bytes[0]);
                    _charLabels[i].setText(Character.toString(ch));
                    break;
                }
                default: {
                    break;
                }
            }
        }
        super.refreshView(epoch, force);
    }

    private JPanel _contentPane;

    private static final Predicate<Inspector> _allMemoryInspectorsPredicate = new Predicate<Inspector>() {
        public boolean evaluate(Inspector inspector) {
            return inspector instanceof MemoryInspector;
        }
    };

    private final Predicate<Inspector> _otherMemoryInspectorsPredicate = new Predicate<Inspector>() {
        public boolean evaluate(Inspector inspector) {
            return inspector instanceof MemoryInspector && inspector != MemoryInspector.this;
        }
    };

    @Override
    public String getTextForTitle() {
        return MemoryInspector.class.getSimpleName() + ": " + _address.toHexString();
    }

    @Override
    protected synchronized void createView(long epoch) {
        frame().menu().addSeparator();
        frame().menu().add(inspection().getDeleteInspectorsAction(_otherMemoryInspectorsPredicate, "Close other Memory Inspectors"));
        frame().menu().add(inspection().getDeleteInspectorsAction(_allMemoryInspectorsPredicate, "Close all Memory Inspectors"));

        _contentPane = new InspectorPanel(inspection());
        frame().setContentPane(_contentPane);
        _contentPane.removeAll();
        _contentPane.setLayout(new BoxLayout(_contentPane, BoxLayout.Y_AXIS));
        _contentPane.add(createController());

        final JPanel view = new InspectorPanel(inspection(), new SpringLayout());
        _contentPane.add(view);

        int numberOfLines = _numberOfGroups / _numberOfGroupsPerLine;
        if (_numberOfGroups % _numberOfGroupsPerLine != 0) {
            numberOfLines++;
        }
        final int numberOfLabels = numberOfLines * _numberOfGroupsPerLine;

        _memoryLabels = new TextLabel[numberOfLabels];
        _charLabels = new TextLabel[numberOfLabels];
        final String space = Strings.times(' ', 2 * _numberOfBytesPerGroup);

        Address lineAddress = _address;
        final int numberOfBytesPerLine = _numberOfGroupsPerLine * _numberOfBytesPerGroup;

        for (int line = 0; line < numberOfLines; line++) {
            final ValueLabel lineAddressLabel = new WordValueLabel(inspection(), ValueMode.WORD, lineAddress);
            view.add(lineAddressLabel);
            lineAddress = lineAddress.plus(numberOfBytesPerLine);
            for (int group = 0; group < _numberOfGroupsPerLine; group++) {
                final int index = (line * _numberOfGroupsPerLine) + group;
                _memoryLabels[index] = new TextLabel(inspection(), space);
                view.add(_memoryLabels[index]);
            }
            final Space leftSpace = new Space();
            view.add(leftSpace);
            for (int group = 0; group < _numberOfGroupsPerLine; group++) {
                final int index = (line * _numberOfGroupsPerLine) + group;
                _charLabels[index] = new TextLabel(inspection(), space);
                view.add(_charLabels[index]);
            }
        }

        refreshView(epoch, true);
        SpringUtilities.makeCompactGrid(view, numberOfLines * 2, 1 + _numberOfGroupsPerLine, 0, 0, 5, 5);
    }

    public void viewConfigurationChanged(long epoch) {
        reconstructView();
    }

    private String byteGroupToString(byte[] bytes) {
        String s = "";
        switch (teleVM().bootImage().header().endianness()) {
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
        Trace.line(1, tracePrefix() + " closing for " + getCurrentTitle() + " - process terminated");
        super.inspectorClosing();
    }

    @Override
    public void vmProcessTerminated() {
        dispose();
    }

}

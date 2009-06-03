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
/*
 * Copyright (c) 2007 Sun Microsystems, Inc. All rights reserved. Use is subject to license terms.
 */
package com.sun.max.ins.memory;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import com.sun.max.collect.*;
import com.sun.max.gui.*;
import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.value.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.value.*;

/**
 * Renders VM memory contents as a list of words.
 *
 * @author Ben L. Titzer
 * @author Michael Van De Vanter
 */
public final class MemoryWordInspector extends Inspector {

    private Address _address;
    private int _selectedLine = -1;
    private Address _selectedAddress;
    private int _numberOfWords;
    private final int _wordHexChars;

    private JComponent createController() {
        final JPanel controller = new InspectorPanel(inspection(), new SpringLayout());

        final AddressInputField.Hex addressField = new AddressInputField.Hex(inspection(), _address) {
            @Override
            public void update(Address address) {
                if (!address.equals(_address)) {
                    _address = address.aligned();
                    setText(_address.toUnsignedString(16));
                    MemoryWordInspector.this.reconstructView();
                }
            }
        };
        addressField.setColumns(18);

        final AddressInputField.Decimal numberOfWordsField = new AddressInputField.Decimal(inspection(), Address.fromInt(_numberOfWords)) {
            @Override
            public void update(Address numberOfGroups) {
                if (!numberOfGroups.equals(_numberOfWords)) {
                    _numberOfWords = numberOfGroups.toInt();
                    MemoryWordInspector.this.reconstructView();
                }
            }
        };
        numberOfWordsField.setColumns(1 + _wordHexChars);
        numberOfWordsField.setRange(1, 1024);

        // Configure buttons
        final JButton up = new JButton(new AbstractAction("Up") {
            public void actionPerformed(ActionEvent e) {
                _address = _address.minus(maxVM().wordSize());
                addressField.setText(_address.toHexString());
                numberOfWordsField.setText(Integer.toString(++_numberOfWords));
                MemoryWordInspector.this.reconstructView();
            }
        });

        // Configure buttons
        final JButton down = new JButton(new AbstractAction("Down") {
            public void actionPerformed(ActionEvent e) {
                numberOfWordsField.setText(Integer.toString(++_numberOfWords));
                MemoryWordInspector.this.reconstructView();
            }
        });

        controller.add(new TextLabel(inspection(), "start:"));
        controller.add(addressField);
        controller.add(up);
        controller.add(new TextLabel(inspection(), "words:"));
        controller.add(numberOfWordsField);
        controller.add(down);

        SpringUtilities.makeCompactGrid(controller, 2, 3, 0, 2, 2, 2);
        return controller;
    }

    private RegisterAddressLabel[] _addressLabels;
    private LocationLabel[] _offsetLabels;
    private WordValueLabel[] _memoryWords;
    private final InspectorAction _disabledInspectObjectAction;

    private final class RegisterAddressLabel extends WordValueLabel {

        private final int _line;
        private String _registerNameList;
        private String _registerDisplayText;

        RegisterAddressLabel(Inspection inspection, int line, Address address) {
            super(inspection, WordValueLabel.ValueMode.WORD, address);
            _line = line;
            updateText();
        }

        void setRegister(Symbol symbol, int misalignment) {
            String registerName = symbol.toString();
            if (misalignment != 0) {
                registerName = registerName + "-" + misalignment;
            }
            if (_registerNameList == null) {
                _registerNameList = registerName;
            } else {
                _registerNameList = _registerNameList + "," + registerName;
            }
            _registerDisplayText = Strings.padLengthWithSpaces(_wordHexChars - 4, _registerNameList) + " -->";
            updateText();
        }

        void clearRegister() {
            if (_registerNameList != null) {
                _registerNameList = null;
                _registerDisplayText = null;
            }
            updateText();
        }

        @Override
        public void updateText() {
            super.updateText();
            if (_line == _selectedLine) {
                setBackground(selectionColor());
            }
            if (_registerDisplayText != null) {
                setText(_registerDisplayText);
                setToolTipText("Register(s): " + _registerNameList + " point at this location");
                setForeground(style().wordSelectedColor());
            }
        }
    }


    private final InspectorMouseClickAdapter _offsetLabelMouseClickAdapter = new InspectorMouseClickAdapter(inspection()) {

        @Override
        public void procedure(MouseEvent mouseEvent) {
            if (MaxineInspector.mouseButtonWithModifiers(mouseEvent) != MouseEvent.BUTTON1) {
                return;
            }
            final Object source = mouseEvent.getSource();
            final int line = Arrays.find(_offsetLabels, source);
            if (line >= 0) {
                _selectedLine = line;
                _selectedAddress = _address.plus(line * maxVM().wordSize());
                MemoryWordInspector.this.reconstructView();
            }
        }
    };

    private final class MemoryOffsetLabel extends LocationLabel.AsOffset {

        private final int _line;

        MemoryOffsetLabel(Inspection inspection, int line, int offset) {
            super(inspection, offset);
            _line = line;
            updateText();
        }

        @Override
        public void updateText() {
            super.updateText();
            if (_line == _selectedLine) {
                setBackground(selectionColor());
            } else {
                setBackground(style().defaultBackgroundColor());
            }
        }
    }

    private final class MemoryWordLabel extends WordValueLabel {

        private final int _line;

        MemoryWordLabel(Inspection inspection, int line) {
            super(inspection, ValueMode.INTEGER_REGISTER, Word.zero());
            _line = line;
        }

        @Override
        public void updateText() {
            super.updateText();
            if (_line == _selectedLine) {
                setBackground(selectionColor());
            }
        }
    }

    @Override
    protected void refreshView(long epoch, boolean force) {
        final int wordSize = maxVM().wordSize();
        for (int i = 0; i < _numberOfWords; i++) {
            final Address address = _address.plus(i * wordSize);
            try {
                _addressLabels[i].clearRegister();
                _memoryWords[i].setValue(new WordValue(maxVM().readWord(address)));

            } catch (DataIOError e) {
                _memoryWords[i].setValue(VoidValue.VOID);
            }
        }
        final Address lastAddress = _address.plus(_numberOfWords * wordSize);
        final TeleNativeThread selectedThread = focus().thread();
        if (selectedThread != null) {
            final TeleIntegerRegisters registers = selectedThread.integerRegisters();
            for (Symbol s : registers.symbolizer()) {
                final Address registerValue = registers.get(s);
                if (registerValue.greaterEqual(_address) && registerValue.lessThan(lastAddress)) {
                    // if the register points into this range, overwrite the address label with the name of the register
                    final Address offset = registerValue.minus(_address);
                    final int line = offset.dividedBy(wordSize).toInt();
                    final int misAlignment = offset.and(wordSize - 1).toInt();
                    _addressLabels[line].setRegister(s, misAlignment);
                }
            }
        }
        super.refreshView(epoch, force);
    }

    @Override
    public void threadFocusSet(TeleNativeThread oldTeleNativeThread, TeleNativeThread teleNativeThread) {
        // Revise any indications of registers pointing at inspected locations.
        refreshView(true);
    }

    private JPanel _contentPane;

    private static final Predicate<Inspector> _allMemoryWordInspectorsPredicate = new Predicate<Inspector>() {
        public boolean evaluate(Inspector inspector) {
            return inspector instanceof MemoryWordInspector;
        }
    };

    private final Predicate<Inspector> _otherMemoryInspectorsPredicate = new Predicate<Inspector>() {
        public boolean evaluate(Inspector inspector) {
            return inspector instanceof MemoryWordInspector && inspector != MemoryWordInspector.this;
        }
    };

    @Override
    public String getTextForTitle() {
        return "Memory Words @ " + _address.toHexString();
    }

    @Override
    protected void createView(long epoch) {
        _contentPane = new InspectorPanel(inspection());
        frame().setContentPane(_contentPane);
        _contentPane.removeAll();
        _contentPane.setLayout(new BoxLayout(_contentPane, BoxLayout.Y_AXIS));
        _contentPane.add(createController());

        final JPanel view = new InspectorPanel(inspection(), new SpringLayout());
        _contentPane.add(view);

        _addressLabels = new RegisterAddressLabel[_numberOfWords];
        _offsetLabels = new LocationLabel.AsOffset[_numberOfWords];
        _memoryWords = new WordValueLabel[_numberOfWords];

        Address lineAddress = _address;
        for (int line = 0; line < _numberOfWords; line++) {
            // Memory Address
            final RegisterAddressLabel addrLabel = new RegisterAddressLabel(inspection(), line, lineAddress);
            addrLabel.setFont(style().wordDataFont());
            addrLabel.setForeground(style().wordDataColor());
            addrLabel.setBackground(style().wordDataBackgroundColor());
            _addressLabels[line] = addrLabel;
            view.add(addrLabel);

            // Memory Offset
            final int offset = lineAddress.minus(_selectedAddress).toInt();
            //final LocationLabel.AsOffset offsetLabel = new LocationLabel.AsOffset(inspection(), offset);
            final LocationLabel.AsOffset offsetLabel = new MemoryOffsetLabel(inspection(), line, offset);
            offsetLabel.addMouseListener(_offsetLabelMouseClickAdapter);
            _offsetLabels[line] = offsetLabel;
            view.add(offsetLabel);

            // Memory Word
            _memoryWords[line] = new MemoryWordLabel(inspection(), line);
            view.add(_memoryWords[line]);

            lineAddress = lineAddress.plus(maxVM().wordSize());
        }

        refreshView(epoch, true);
        SpringUtilities.makeCompactGrid(view, _numberOfWords, 3, 0, 0, 0, 0);
    }

    public void viewConfigurationChanged(long epoch) {
        reconstructView();
    }

    private Color selectionColor() {
        return style().wordDataBackgroundColor().darker();
    }

    private MemoryWordInspector(Inspection inspection, Address address, int numberOfWords) {
        super(inspection);
        _address = address.aligned();
        _selectedAddress = _address;
        _numberOfWords = numberOfWords;
        _wordHexChars = maxVM().wordSize() * 2;
        createFrame(null);
        frame().menu().addSeparator();
        frame().menu().add(inspection().getDeleteInspectorsAction(_otherMemoryInspectorsPredicate, "Close other Memory Word Inspectors"));
        frame().menu().add(inspection().getDeleteInspectorsAction(_allMemoryWordInspectorsPredicate, "Close all Memory Word Inspectors"));
        setLocationRelativeToMouse();
        _memoryWordInspectors.add(this);
        _disabledInspectObjectAction = new InspectorAction(inspection, "Inspect object (Left-Button)") {
            @Override
            protected void procedure() {
                Problem.error("Should not happen");
            }
        };
        _disabledInspectObjectAction.setEnabled(false);
        Trace.line(1, tracePrefix() + " creating for " + getTextForTitle());
    }

    private static final IdentityHashSet<MemoryWordInspector> _memoryWordInspectors = new IdentityHashSet<MemoryWordInspector>();

    /**
     * Displays and highlights a new word inspector for a region of memory in the VM.
     */
    public static MemoryWordInspector create(Inspection inspection, Address address, int numberOfWords) {
        return new MemoryWordInspector(inspection, address, numberOfWords);
    }

    /**
     * Displays a new word inspector for a region of memory in the VM.
     */
    public static MemoryWordInspector create(Inspection inspection, Address address) {
        return create(inspection, address, 10);
    }

    /**
     * Displays a new word inspector for a region of memory at the beginning of an object in the VM.
     */
    public static MemoryWordInspector create(Inspection inspection, TeleObject teleObject) {
        final Pointer cell = teleObject.getCurrentCell();
        final int size = teleObject.getCurrentSize().toInt();
        return create(inspection, cell, size);
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

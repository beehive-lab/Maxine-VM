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
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.value.*;

/**
 * Renders VM memory contents as a list of words.
 *
 * @author Ben L. Titzer
 * @author Michael Van De Vanter
 */
public final class MemoryWordInspector extends Inspector {

    private Address address;
    private int selectedLine = -1;
    private Address selectedAddress;
    private int numberOfWords;
    private final int wordHexChars;

    private JComponent createController() {
        final JPanel controller = new InspectorPanel(inspection(), new SpringLayout());

        final AddressInputField.Hex addressField = new AddressInputField.Hex(inspection(), address) {
            @Override
            public void update(Address value) {
                if (!value.equals(address)) {
                    address = value.aligned();
                    setText(address.toUnsignedString(16));
                    MemoryWordInspector.this.reconstructView();
                }
            }
        };
        addressField.setColumns(18);

        final AddressInputField.Decimal numberOfWordsField = new AddressInputField.Decimal(inspection(), Address.fromInt(numberOfWords)) {
            @Override
            public void update(Address value) {
                if (!value.equals(numberOfWords)) {
                    numberOfWords = value.toInt();
                    MemoryWordInspector.this.reconstructView();
                }
            }
        };
        numberOfWordsField.setColumns(1 + wordHexChars);
        numberOfWordsField.setRange(1, 1024);

        // Configure buttons
        final JButton up = new JButton(new AbstractAction("Up") {
            public void actionPerformed(ActionEvent e) {
                address = address.minus(maxVM().wordSize());
                addressField.setText(address.toHexString());
                numberOfWordsField.setText(Integer.toString(++numberOfWords));
                MemoryWordInspector.this.reconstructView();
            }
        });

        // Configure buttons
        final JButton down = new JButton(new AbstractAction("Down") {
            public void actionPerformed(ActionEvent e) {
                numberOfWordsField.setText(Integer.toString(++numberOfWords));
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

    private RegisterAddressLabel[] addressLabels;
    private LocationLabel[] offsetLabels;
    private WordValueLabel[] memoryWords;
    private final InspectorAction disabledInspectObjectAction;

    private final class RegisterAddressLabel extends WordValueLabel {

        private final int line;
        private String registerNameList;
        private String registerDisplayText;

        RegisterAddressLabel(Inspection inspection, int line, Address address) {
            super(inspection, WordValueLabel.ValueMode.WORD, address, null);
            this.line = line;
            updateText();
        }

        void setRegister(Symbol symbol, int misalignment) {
            String registerName = symbol.toString();
            if (misalignment != 0) {
                registerName = registerName + "-" + misalignment;
            }
            if (registerNameList == null) {
                registerNameList = registerName;
            } else {
                registerNameList = registerNameList + "," + registerName;
            }
            registerDisplayText = Strings.padLengthWithSpaces(wordHexChars - 4, registerNameList) + " -->";
            updateText();
        }

        void clearRegister() {
            if (registerNameList != null) {
                registerNameList = null;
                registerDisplayText = null;
            }
            updateText();
        }

        @Override
        public void updateText() {
            super.updateText();
            if (line == selectedLine) {
                setBackground(selectionColor());
            }
            if (registerDisplayText != null) {
                setText(registerDisplayText);
                setToolTipText("Register(s): " + registerNameList + " point at this location");
                setForeground(style().wordSelectedColor());
            }
        }
    }


    private final InspectorMouseClickAdapter offsetLabelMouseClickAdapter = new InspectorMouseClickAdapter(inspection()) {

        @Override
        public void procedure(MouseEvent mouseEvent) {
            if (MaxineInspector.mouseButtonWithModifiers(mouseEvent) != MouseEvent.BUTTON1) {
                return;
            }
            final Object source = mouseEvent.getSource();
            final int line = Arrays.find(offsetLabels, source);
            if (line >= 0) {
                selectedLine = line;
                selectedAddress = address.plus(line * maxVM().wordSize());
                MemoryWordInspector.this.reconstructView();
            }
        }
    };

    private final class MemoryOffsetLabel extends LocationLabel.AsOffset {

        private final int line;

        MemoryOffsetLabel(Inspection inspection, int line, int offset) {
            super(inspection, offset);
            this.line = line;
            updateText();
        }

        @Override
        public void updateText() {
            super.updateText();
            if (line == selectedLine) {
                setBackground(selectionColor());
            } else {
                setBackground(style().defaultBackgroundColor());
            }
        }
    }

    private final class MemoryWordLabel extends WordValueLabel {

        private final int line;

        MemoryWordLabel(Inspection inspection, int line) {
            super(inspection, ValueMode.INTEGER_REGISTER, Word.zero(), null);
            this.line = line;
        }

        @Override
        public void updateText() {
            super.updateText();
            if (line == selectedLine) {
                setBackground(selectionColor());
            }
        }
    }

    @Override
    protected void refreshView(boolean force) {
        final int wordSize = maxVM().wordSize();
        for (int i = 0; i < numberOfWords; i++) {
            final Address a = address.plus(i * wordSize);
            try {
                addressLabels[i].clearRegister();
                memoryWords[i].setValue(new WordValue(maxVM().readWord(a)));

            } catch (DataIOError e) {
                memoryWords[i].setValue(VoidValue.VOID);
            }
        }
        final Address lastAddress = address.plus(numberOfWords * wordSize);
        final MaxThread selectedThread = focus().thread();
        if (selectedThread != null) {
            final TeleIntegerRegisters registers = selectedThread.integerRegisters();
            for (Symbol s : registers.symbolizer()) {
                final Address registerValue = registers.get(s);
                if (registerValue.greaterEqual(address) && registerValue.lessThan(lastAddress)) {
                    // if the register points into this range, overwrite the address label with the name of the register
                    final Address offset = registerValue.minus(address);
                    final int line = offset.dividedBy(wordSize).toInt();
                    final int misAlignment = offset.and(wordSize - 1).toInt();
                    addressLabels[line].setRegister(s, misAlignment);
                }
            }
        }
        super.refreshView(force);
    }

    @Override
    public void threadFocusSet(MaxThread oldThread, MaxThread thread) {
        // Revise any indications of registers pointing at inspected locations.
        refreshView(true);
    }

    private JPanel contentPane;

    private static final Predicate<Inspector> allMemoryWordInspectorsPredicate = new Predicate<Inspector>() {
        public boolean evaluate(Inspector inspector) {
            return inspector instanceof MemoryWordInspector;
        }
    };

    private final Predicate<Inspector> otherMemoryInspectorsPredicate = new Predicate<Inspector>() {
        public boolean evaluate(Inspector inspector) {
            return inspector instanceof MemoryWordInspector && inspector != MemoryWordInspector.this;
        }
    };

    @Override
    public String getTextForTitle() {
        return "Memory Words @ " + address.toHexString();
    }

    @Override
    protected void createView() {
        contentPane = new InspectorPanel(inspection());
        frame().setContentPane(contentPane);
        contentPane.removeAll();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        contentPane.add(createController());

        final JPanel view = new InspectorPanel(inspection(), new SpringLayout());
        contentPane.add(view);

        addressLabels = new RegisterAddressLabel[numberOfWords];
        offsetLabels = new LocationLabel.AsOffset[numberOfWords];
        memoryWords = new WordValueLabel[numberOfWords];

        Address lineAddress = address;
        for (int line = 0; line < numberOfWords; line++) {
            // Memory Address
            final RegisterAddressLabel addrLabel = new RegisterAddressLabel(inspection(), line, lineAddress);
            addrLabel.setFont(style().wordDataFont());
            addrLabel.setForeground(style().wordDataColor());
            addrLabel.setBackground(style().wordDataBackgroundColor());
            addressLabels[line] = addrLabel;
            view.add(addrLabel);

            // Memory Offset
            final int offset = lineAddress.minus(selectedAddress).toInt();
            //final LocationLabel.AsOffset offsetLabel = new LocationLabel.AsOffset(inspection(), offset);
            final LocationLabel.AsOffset offsetLabel = new MemoryOffsetLabel(inspection(), line, offset);
            offsetLabel.addMouseListener(offsetLabelMouseClickAdapter);
            offsetLabels[line] = offsetLabel;
            view.add(offsetLabel);

            // Memory Word
            memoryWords[line] = new MemoryWordLabel(inspection(), line);
            view.add(memoryWords[line]);

            lineAddress = lineAddress.plus(maxVM().wordSize());
        }

        refreshView(true);
        SpringUtilities.makeCompactGrid(view, numberOfWords, 3, 0, 0, 0, 0);
    }

    public void viewConfigurationChanged() {
        reconstructView();
    }

    private Color selectionColor() {
        return style().wordDataBackgroundColor().darker();
    }

    private MemoryWordInspector(Inspection inspection, Address address, int numberOfWords) {
        super(inspection);
        this.address = address.aligned();
        selectedAddress = this.address;
        this.numberOfWords = numberOfWords;
        wordHexChars = maxVM().wordSize() * 2;
        createFrame(null);
        frame().menu().addSeparator();
        frame().menu().add(actions().closeViews(otherMemoryInspectorsPredicate, "Close other Memory Word Inspectors"));
        frame().menu().add(actions().closeViews(allMemoryWordInspectorsPredicate, "Close all Memory Word Inspectors"));
        inspection.gui().setLocationRelativeToMouse(this);
        memoryWordInspectors.add(this);
        disabledInspectObjectAction = new InspectorAction(inspection, "Inspect object (Left-Button)") {
            @Override
            protected void procedure() {
                throw FatalError.unexpected("Should not happen");
            }
        };
        disabledInspectObjectAction.setEnabled(false);
        Trace.line(1, tracePrefix() + " creating for " + getTextForTitle());
    }

    private static final IdentityHashSet<MemoryWordInspector> memoryWordInspectors = new IdentityHashSet<MemoryWordInspector>();

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

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
package com.sun.max.ins.debug;

import java.awt.*;

import javax.swing.*;

import com.sun.max.gui.*;
import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.value.*;
import com.sun.max.ins.value.WordValueLabel.*;
import com.sun.max.tele.debug.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * A panel for displaying the values in one of VM thread local storage areas associated
 * with a VM thread.
 *
 * @author Doug Simon
 */
public class VMThreadLocalsPanel extends InspectorPanel {

    private final TeleVMThreadLocalValues _values;
    private final TextLabel[] _nameLabels;
    private final WordValueLabel[] _valueInspectors;

    public VMThreadLocalsPanel(Inspection inspection, TeleVMThreadLocalValues values) {
        super(inspection);
        setLayout(new BorderLayout());

        final JPanel header = new JPanel(new SpringLayout());
        header.setOpaque(true);
        header.setBackground(style().defaultBackgroundColor());
        header.add(new TextLabel(inspection, "start: "));
        header.add(new DataLabel.AddressAsHex(inspection, values.start()));
        header.add(new TextLabel(inspection, "end: "));
        header.add(new DataLabel.AddressAsHex(inspection, values.end()));
        header.add(new TextLabel(inspection, "size: "));
        header.add(new DataLabel.IntAsDecimal(inspection, values.size().toInt()));
        SpringUtilities.makeCompactGrid(header, 2);

        add(header, BorderLayout.NORTH);

        final JPanel valuePanel = new JPanel();
        _values = values;
        final int n = VmThreadLocal.NAMES.length();
        _nameLabels = new TextLabel[n];
        _valueInspectors = new WordValueLabel[n];
        valuePanel.setLayout(new SpringLayout());
        int index = 0;
        int offset = 0;
        for (String name : VmThreadLocal.NAMES) {
            final VmThreadLocal local = index < VmThreadLocal.VALUES.length() ? VmThreadLocal.VALUES.get(index) : null;
            final TextLabel nameLabel = new TextLabel(inspection, name);
            nameLabel.setToolTipText("+" + offset + ", 0x" + values.start().plus(offset).toHexString());
            valuePanel.add(nameLabel);
            _nameLabels[index] = nameLabel;

            final ValueMode valueMode = local != null && local.kind() == Kind.REFERENCE ? ValueMode.REFERENCE : ValueMode.WORD;
            final WordValueLabel wordValueLabel = new WordValueLabel(inspection, valueMode);
            valuePanel.add(wordValueLabel);
            _valueInspectors[index] = wordValueLabel;
            ++index;
            offset += Word.size();
        }
        refresh(teleVM().teleProcess().epoch(), true);
        SpringUtilities.makeCompactGrid(valuePanel, valuePanel.getComponentCount() / 2, 2, 0, 0, 5, 1);

        add(new JScrollPane(valuePanel), BorderLayout.CENTER);
    }

    public final void refresh(long epoch, boolean force) {
        if (isShowing()) {
            int index = 0;
            for (String name : VmThreadLocal.NAMES) {
                if (_values.isValid(name)) {
                    final long value = _values.get(name);
                    _valueInspectors[index].setValue(new WordValue(Address.fromLong(value)));
                } else {
                    _valueInspectors[index].setValue(VoidValue.VOID);
                }
                index++;
            }
        }
    }

    public final void redisplay() {
        for (WordValueLabel wordValueLabel : _valueInspectors) {
            wordValueLabel.redisplay();
        }
    }
}

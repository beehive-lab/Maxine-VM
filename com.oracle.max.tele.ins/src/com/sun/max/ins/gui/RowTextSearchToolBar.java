/*
 * Copyright (c) 2008, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.ins.gui;

import java.awt.*;
import java.awt.event.*;
import java.util.regex.*;

import javax.swing.*;
import javax.swing.event.*;

import com.sun.max.ins.*;
import com.sun.max.ins.util.*;

/**
 * A toolbar containing controls for identifying rows that match a regexp pattern
 * and for navigating forward and backward among the matching rows.
 *
 * @author Michael Van De Vanter
 */
public class RowTextSearchToolBar extends InspectorToolBar {

    private final RowMatchNavigationListener owner;
    private final RowTextMatcher rowMatcher;
    private final InspectorCheckBox regexpCheckBox;
    private final JTextField textField;
    private final Color textFieldDefaultBackground;
    private final JLabel statusLabel;
    private final JButton nextButton;
    private final JButton previousButton;

    private int[] matchingRows = null;

    private class SearchTextListener implements DocumentListener {
        public void changedUpdate(DocumentEvent e) {
            // This should never be called
            InspectorError.unexpected();
        }

        public void insertUpdate(DocumentEvent event) {
            processTextInput();
        }

        public void removeUpdate(DocumentEvent e) {
            processTextInput();
        }
    }

    /**
     * Creates a toolbar with controls for performing regular expression searching over a row-based view.
     *
     * @param inspection
     * @param parent where to send search outcomes and user requests
     * @param rowTextMatcher a regular expression search session wrapped around some row-based data
     */
    public RowTextSearchToolBar(Inspection inspection, RowMatchNavigationListener parent, RowTextMatcher rowTextMatcher) {
        super(inspection);
        this.owner = parent;
        rowMatcher = rowTextMatcher;
        setBorder(style().defaultPaneBorder());
        setFloatable(false);
        setRollover(true);
        add(new TextLabel(inspection, "Search: "));

        regexpCheckBox = new InspectorCheckBox(inspection, "regexp", "Treat search pattern as a regular expression?", false);
        regexpCheckBox.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                processTextInput();
            }
        });

        textField = new JTextField();
        textField.setColumns(10);  // doesn't seem to have an effect
        textFieldDefaultBackground = textField.getBackground();
        textField.setToolTipText("Search code for regexp pattern, case-insensitive, Return=Next");
        textField.getDocument().addDocumentListener(new SearchTextListener());
        textField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                if  (matchingRows.length > 0) {
                    owner.selectNextResult();
                }
            }
        });
        textField.requestFocusInWindow();
        add(textField);

        add(regexpCheckBox);

        add(Box.createHorizontalGlue());

        statusLabel = new JLabel("");
        add(statusLabel);

        previousButton = new JButton(new AbstractAction() {
            public void actionPerformed(ActionEvent actionEvent) {
                owner.selectPreviousResult();
            }
        });
        previousButton.setIcon(style().searchPreviousMatchButtonIcon());
        previousButton.setText(null);
        previousButton.setToolTipText("Scroll to previous matching line");
        previousButton.setEnabled(false);
        add(previousButton);

        nextButton = new JButton(new AbstractAction() {
            public void actionPerformed(ActionEvent actionEvent) {
                owner.selectNextResult();
            }
        });
        nextButton.setIcon(style().searchNextMatchButtonIcon());
        nextButton.setText(null);
        nextButton.setToolTipText("Scroll to next matching line");
        nextButton.setEnabled(false);
        add(nextButton);

        final JButton closeButton = new JButton(new AbstractAction() {
            public void actionPerformed(ActionEvent actionEvent) {
                owner.closeRequested();
            }
        });
        closeButton.setIcon(style().codeViewCloseIcon());
        closeButton.setToolTipText("Close Search");
        add(closeButton);
    }

    /**
     * Causes the keyboard focus to be set to the text field.
     */
    public void getFocus() {
        textField.requestFocusInWindow();
    }

    private void processTextInput() {
        final String text = textField.getText();
        if (text.equals("")) {
            textField.setBackground(textFieldDefaultBackground);
            statusLabel.setText("");
            matchingRows = null;
            owner.setSearchResult(null);
            nextButton.setEnabled(false);
            previousButton.setEnabled(false);
        } else {
            Pattern pattern;
            try {
                if (regexpCheckBox.isSelected()) {
                    pattern = Pattern.compile(text, Pattern.CASE_INSENSITIVE);
                } else {
                    pattern = Pattern.compile(text, Pattern.CASE_INSENSITIVE + Pattern.LITERAL);
                }
            } catch (PatternSyntaxException patternSyntaxException) {
                textField.setBackground(style().searchFailedBackground());
                statusLabel.setText("regexp error");
                matchingRows = null;
                owner.setSearchResult(matchingRows);
                return;
            }
            matchingRows = rowMatcher.findMatches(pattern);
            final int matchCount = matchingRows.length;
            statusLabel.setText(Integer.toString(matchCount) + "/" + rowMatcher.rowCount() + " rows");

            if (matchCount > 0) {
                textField.setBackground(style().searchMatchedBackground());
                nextButton.setEnabled(true);
                previousButton.setEnabled(true);
            } else {
                textField.setBackground(style().searchFailedBackground());
                nextButton.setEnabled(false);
                previousButton.setEnabled(false);
            }
            owner.setSearchResult(matchingRows);
        }
    }
}

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

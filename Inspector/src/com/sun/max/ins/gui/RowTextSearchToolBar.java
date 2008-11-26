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

import com.sun.max.collect.*;
import com.sun.max.ins.*;
import com.sun.max.program.*;


/**
 * A toolbar containing controls for identifying rows that match a regexp pattern
 * and for navigating forward and backward among the matching rows.
 *
 * @author Michael Van De Vanter
 *
 */
public class RowTextSearchToolBar extends JToolBar {

    private final Inspection _inspection;
    private final RowSearchListener _owner;
    private final RowTextSearcher _searcher;
    private final JTextField _textField;
    private final Color _textFieldDefaultBackground;
    private final JLabel _statusLabel;
    private final JButton _nextButton;
    private final JButton _previousButton;

    private IndexedSequence<Integer> _matchingRows = null;

    private class SearchTextListener implements DocumentListener {
        public void changedUpdate(DocumentEvent e) {
            // This should never be called
            ProgramError.unexpected();
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
     * @param owner where to send search outcomes and user requests
     * @param rowTextSearcher a regular expression search session wrapped around some row-based data
     */
    public RowTextSearchToolBar(Inspection inspection, RowSearchListener owner, RowTextSearcher rowTextSearcher) {
        _inspection = inspection;
        _owner = owner;
        _searcher = rowTextSearcher;
        setFloatable(false);
        setRollover(true);
        add(new JLabel("Search: "));
        setBackground(_inspection.style().defaultBackgroundColor());

        _textField = new JTextField();
        _textField.setColumns(10);  // doesn't seem to have an effect
        _textFieldDefaultBackground = _textField.getBackground();
        _textField.setToolTipText("Search code for regexp pattern, case-insensitive, Return=Next");
        _textField.getDocument().addDocumentListener(new SearchTextListener());
        _textField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                if  (_matchingRows.length() > 0) {
                    _owner.selectNextResult();
                }
            }
        });
        _textField.requestFocusInWindow();
        add(_textField);

        _statusLabel = new JLabel("");
        add(_statusLabel);

        _nextButton = new JButton(new AbstractAction() {
            public void actionPerformed(ActionEvent actionEvent) {
                _owner.selectNextResult();
            }
        });
        _nextButton.setIcon(_inspection.style().searchNextMatchButtonIcon());
        _nextButton.setToolTipText("Scroll to next matching line");
        _nextButton.setEnabled(false);
        add(_nextButton);

        _previousButton = new JButton(new AbstractAction() {
            public void actionPerformed(ActionEvent actionEvent) {
                _owner.selectPreviousResult();
            }
        });
        _previousButton.setIcon(_inspection.style().searchPreviousMatchButtonIcon());
        _previousButton.setToolTipText("Scroll to previous matching line");
        _previousButton.setEnabled(false);
        add(_previousButton);

        add(Box.createHorizontalGlue());

        final JButton closeButton = new JButton(new AbstractAction() {
            public void actionPerformed(ActionEvent actionEvent) {
                _owner.closeSearch();
            }
        });
        closeButton.setIcon(_inspection.style().codeViewCloseIcon());
        closeButton.setToolTipText("Close Search");
        add(closeButton);
    }

    /**
     * Causes the keyboard focus to be set to the text field.
     */
    public void getFocus() {
        _textField.requestFocusInWindow();
    }

    private void processTextInput() {
        final String text = _textField.getText();
        if (text.equals("")) {
            _textField.setBackground(_textFieldDefaultBackground);
            _statusLabel.setText("");
            _matchingRows = null;
            _owner.searchResult(null);
            _nextButton.setEnabled(false);
            _previousButton.setEnabled(false);
        } else {
            Pattern pattern;
            try {
                pattern = Pattern.compile(text, Pattern.CASE_INSENSITIVE);
            } catch (PatternSyntaxException patternSyntaxException) {
                _textField.setBackground(_inspection.style().searchFailedBackground());
                _statusLabel.setText("regexp error");
                return;
            }
            _matchingRows = _searcher.search(pattern);
            final int matchCount = _matchingRows.length();
            if (matchCount == 0) {
                _statusLabel.setText("no matches");
            }  else if (matchCount == 1) {
                _statusLabel.setText("1 row matched");
            } else {
                _statusLabel.setText(Integer.toString(matchCount) + " rows matched");
            }
            if (matchCount > 0) {
                _textField.setBackground(_inspection.style().searchMatchedBackground());
                _nextButton.setEnabled(true);
                _previousButton.setEnabled(true);
            } else {
                _textField.setBackground(_inspection.style().searchFailedBackground());
                _nextButton.setEnabled(false);
                _previousButton.setEnabled(false);
            }
            _owner.searchResult(_matchingRows);
        }
    }
}

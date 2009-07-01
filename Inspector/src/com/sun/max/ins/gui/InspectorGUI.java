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

import javax.swing.*;

import com.sun.max.util.*;


/**
 * Basic GUI services for a Maxine VM Inspection session.
 *
 * @author Michael Van De Vanter
 */
public interface InspectorGUI {

    /**
     * Adds an {@link Inspector} to the GUI display, brings it to the front, and makes it visible.
     * Its frame must already have been created.
     */
    void addInspector(Inspector inspector);

    /**
     * Returns an {@link Inspector} currently in the GUI display that matches a predicate.
     */
    Inspector findInspector(Predicate<Inspector> predicate);

    /**
     * Removes and disposes all instances of {@link Inspector} currently in the GUI display that matches a predicate.
     */
    void removeInspectors(Predicate<Inspector> predicate);

    /**
     * Shows a visual indication of the Inspector state with regard to accepting user inputs.
     *
     * @param busy whether the Inspector is busy and not responding to user events
     */
    void showInspectorBusy(boolean busy);

    /**
     * Displays an information message in a modal dialog with specified frame title.
     */
    void informationMessage(String message, String title);

    /**
     * Displays an information message in a modal dialog with default frame title.
     */
    void informationMessage(String message);

    /**
     * Displays an error message in a modal dialog with specified frame title.
     */
    void errorMessage(String message, String title);
    /**
     * Displays an error message in a modal dialog with default frame title.
     */
    void errorMessage(String message);

    /**
     * Collects textual input from user.
     *
     * @param message a prompt
     * @param initialValue an initial value
     * @return text typed by user
     */
    String inputDialog(String message, String initialValue);

    boolean yesNoDialog(String message);

    /**
     * Displays a message and invites text input from user in a modal dialog.
     *
     * @return text typed by user.
     */
    String questionMessage(String message);

    /**
     * Posts a string to the system clipboard.
     */
    void postToClipboard(String text);

    /**
     * Sets Inspector frame location to a point displaced by a default amount from the most recently known mouse position.
     */
    void setLocationRelativeToMouse(Inspector inspector);

    /**
     * Sets Inspector frame location to a point displayed by specified diagonal amount from the most recently known mouse position.
     */
    void setLocationRelativeToMouse(Inspector inspector, int offset);

    /**
     * Sets Inspector frame location to middle of Inspection display.
     */
    void moveToMiddle(Inspector inspector);

    /**
     * Sets Inspector frame location to middle of Inspection display if it would otherwise be completely outside the visible area.
     */
    void moveToMiddleIfNotVisble(Inspector inspector);

    /**
     * Sets dialog frame location to middle of Inspection display.
     */
    void moveToMiddle(JDialog dialog);

    /**
     * Returns an instance of the appliation's parent {@link Frame} for GUI operations that need access.
     */
    Frame frame();

    InspectorLabel getMissingDataTableCellRenderer();
}

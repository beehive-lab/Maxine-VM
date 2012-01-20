/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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

import javax.swing.*;

import com.sun.max.ins.*;
import com.sun.max.util.*;

/**
 * Basic GUI services for a VM Inspection session.
 */
public interface InspectorGUI {

    /**
     * Adds an {@link AbstractView} to the GUI display, brings it to the front, and makes it visible.
     * Its frame must already have been created.
     */
    void addView(AbstractView view);

    /**
     * Removes and disposes all instances of {@link AbstractView} currently in the GUI display that match a predicate.
     */
    void removeViews(Predicate<AbstractView> predicate);

    /**
     * Shows a visual indication of the Inspector state with regard to accepting user inputs.
     *
     * @param busy whether the Inspector is busy and not responding to user events
     */
    void showInspectorBusy(boolean busy);

    /**
     * Displays an information message in a modal dialog with specified frame title.
     *
     * @param message a {@link String}, {@link String[]}, or {@link Object} to be converted to text
     * @param title a title to display on the dialog frame
     * @see JOptionPane#showMessageDialog(Component, Object)
     */
    void informationMessage(Object message, String title);

    /**
     * Displays an information message in a modal dialog with default frame title.
     *
     * @param message a {@link String}, {@link String[]}, or {@link Object} to be converted to text
     * @see JOptionPane#showMessageDialog(Component, Object)
     */
    void informationMessage(Object message);

    /**
     * Displays a warning message in a modal dialog with specified frame title.
     *
     * @param message a {@link String}, {@link String[]}, or {@link Object} to be converted to text
     * @param title a title to display on the dialog frame
     * @see JOptionPane#showMessageDialog(Component, Object)
     */
    void warningMessage(Object message, String title);

    /**
     * Displays a warning message in a modal dialog with default frame title.
     *
     * @param message message a {@link String}, {@link String[]}, or {@link Object} to be converted to text
     * @see JOptionPane#showMessageDialog(Component, Object)
     */
    void warningMessage(Object message);

    /**
     * Displays an error message in a modal dialog with specified frame title.
     *
     * @param message message a {@link String}, {@link String[]}, or {@link Object} to be converted to text
     * @param title a title to display on the dialog frame
     * @see JOptionPane#showMessageDialog(Component, Object)
     */
    void errorMessage(Object message, String title);

    /**
     * Displays an error message in a modal dialog with default frame title.
     *
     * @param message message a {@link String}, {@link String[]}, or {@link Object} to be converted to text
     * @see JOptionPane#showMessageDialog(Component, Object)
     */
    void errorMessage(Object message);

    /**
     * Collects textual input from user.
     *
     * @param message a prompt
     * @param initialValue an initial value
     * @return text typed by user
     */
    String inputDialog(String message, String initialValue);

    /**
     * Solicits a yes/no response from user.
     *
     * @param message a prompt
     * @return a yes or no decision
     */
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
     * Gets the mouse button that just clicked, translating mouse click events specially for Darwin.
     * <br>control click -> button 3 (right);
     * <br>alt click -> button 2 (middle).
     * <p>
     * Defaults to the standard on other platforms.
     *
     * @param mouseEvent
     * @return the button that should be understood by a mouse click.
     * @see MouseEvent#getButton()
     */
    int getButton(MouseEvent mouseEvent);

    /**
     * Sets view frame location to a point displayed by specified diagonal amount from the most recently known mouse position.
     * @param diagonalOffset number of pixels down and to right for new location
     */
    void setLocationRelativeToMouse(AbstractView view, int diagonalOffset);

    /**
     * Sets a window component location to middle of Inspection display.
     */
    void moveToMiddle(Component component);

    /**
     * Sets view frame location to middle of Inspection display.
     */
    void moveToMiddle(AbstractView view);

    /**
     * Gets an action that sets a view frame location to middle of Inspection display.
     */
    InspectorAction moveToMiddleAction(AbstractView view);

    /**
     * Moves a view frame location as little as possible to make it fully visible.
     */
    void moveToFullyVisible(AbstractView view);

    /**
     * If the view is moved out of the frame, either up or to the left, so that the default
     * menu in the upper left corner of the view is not visible, move it down and to the left
     * enough to make the menu visible.
     */
    void moveToExposeDefaultMenu(AbstractView view);

    /**
     * Shrink a view in each dimension, without changing location,
     * to make it fit within the view's frame.
     */
    void resizeToFit(AbstractView view);

    /**
     * Gets an action that will shrink a view in each dimension, without
     * changing location, to make it fit within the view's frame.
     */
    InspectorAction resizeToFitAction(AbstractView view);

    /**
     * Grow an view in each dimension to make it fill the view's frame.
     */
    void resizeToFill(AbstractView view);

    /**
     * Gets an action that will grow an view in each dimension to make it fill the view's frame.
     */
    InspectorAction resizeToFillAction(AbstractView view);

    /**
     * Restores the default size and location of a view.
     */
    void restoreDefaultGeometry(AbstractView view);

    /**
     * Gets an action that restores the default size and location of a view.
     */
    InspectorAction restoreDefaultGeometryAction(AbstractView view);

    /**
     * Moves a {@link JDialog} frame down and to the right of the current mouse location.
     *
     * @param dialog a dialog
     * @param diagonalOffset number of pixels down and to right for new location
     */
    void setLocationRelativeToMouse(JDialog dialog, int diagonalOffset);

    /**
     * Returns an instance of the appliation's parent {@link Frame} for GUI operations that need access.
     */
    Frame frame();

    InspectorLabel getUnavailableDataTableCellRenderer();
}

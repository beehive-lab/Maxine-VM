/*
 * Copyright (c) 2012, 2012, Oracle and/or its affiliates. All rights reserved.
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

import javax.swing.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.AbstractView.*;
import com.sun.max.ins.view.*;

public interface InspectorView<View_Type extends InspectorView> {

    /**
     * Creates a set of standard menu items for this view which are
     * appropriate to one of the standard menu kinds.
     *
     * @param menuKind the kind of menu for which the standard items are intended
     * @return a new set of menu items
     */
    InspectorMenuItems defaultMenuItems(MenuKind menuKind);

    /**
     * Adds a listener for changes in {@link AbstractView} window state.
     */
    void addViewEventListener(ViewEventListener listener);

    /**
     * Removes a listener for changes in {@link AbstractView} window state.
     */
    void removeViewEventListener(ViewEventListener listener);

    /**
     * @return the manager for kind of view
     */
    ViewManager viewManager();

    /**
     * @return the component in which the view displays its content.
     */
    JComponent getJComponent();

    /**
     * Gets a default location for a view.  For singletons, these tend to be statically defined by the
     * view geometry preferences.  For other views, the default might be the location at which it
     * was created originally.
     *
     * @return default geometry for this view, to be used if no prior settings; null if no default specified.
     */
    Rectangle defaultGeometry();

    /**
     * Sets the geometry of the view in the main frame.
     *
     * @param rectangle the new geometry for the view
     */
    void setGeometry(Rectangle rectangle);

    /**
     * @return the current geometry for this view in the main frame
     */
    Rectangle getGeometry();

    /**
     * @return whether the view has been <em>pinned</em> and should be immune from bulk closing commands.
     */
    boolean isPinned();

    /**
     * Gets from subclasses the currently appropriate title for this view's display frame.
     *
     * @return a short string suitable for appearing in the window frame of an view.
     * If this text is expected to change dynamically, a call to {@link AbstractView#setTitle()}
     * will cause this to be called again and the result assigned to the frame.
     */
    String getTextForTitle();

    /**
     * Unconditionally forces a full refresh of this view.
     */
    void forceRefresh();

    void validate();

    void flash();

    void flash(int n);

    /**
     * Calls this view to the users attention:  move to front, select, and flash.
     */
    void highlight();

    /**
     * Explicitly closes a particular view, but
     * many are closed implicitly by a window system
     * event on the frame.  Start the closure by
     * notifying the frame, which will then close
     * the view.
     */
    void dispose();

    /**
     * @return an action that makes visible the view and highlights it.
     */
    InspectorAction getShowViewAction();

}

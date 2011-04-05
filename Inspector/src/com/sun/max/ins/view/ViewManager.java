/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.ins.view;

import com.sun.max.ins.gui.*;
import com.sun.max.ins.view.InspectionViews.ViewKind;


/**
 * A manager for a specific kind of Inspector View, each instance
 * of which is implemented as a subclass of {@Inspector}.  Some kinds
 * of views are singletons, others may have an unbounded number of
 * instances. The intention is that there be a singleton manager for
 * each kind of view.
 * <p>
 * The manager is both a source of meta-information about the kind
 * of view, and an implementation that manages the view(s).
 *
 * @author Michael Van De Vanter
 */
public interface ViewManager  {

    /**
     * @return the kind of view being managed by this manager
     */
    ViewKind viewKind();

    /**
     * @return terse name of this view, typically just what's being viewed, e.g. "Threads"
     */
    String shortName();

    /**
     * @return full name of this view, e.g. "Threads Inspector"
     */
    String longName();

    /**
     * @return whether this kind view is supported by currently running configuration and platform
     */
    boolean isSupported();

    /**
     * @return whether it is possible to create this kind of view under the current circumstances
     */
    boolean isEnabled();

    /**
     * @return whether there can only be one instance of this kind of view.
     */
    boolean isSingleton();

    /**
     * @return whether one or more views of this kind are currently active
     */
    boolean isActive();

    /**
     * Notifies the manager that a view under its management is
     * in the process of being closed.
     */
    void notifyViewClosing(Inspector inspector);

}

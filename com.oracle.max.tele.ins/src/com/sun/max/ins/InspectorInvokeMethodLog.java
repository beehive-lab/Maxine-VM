/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.ins;

/**
 * A holder of text for output of method invocations; persists
 * across sessions.
 */
interface InspectorInvokeMethodLog {

    /**
     * @return the name of the invoke log, even if it has been disposed.
     */
    String getName();

    /**
     * @return the current contents of the invoke log, possibly an empty
     * string; null if it has been disposed.
     */
    String getContents();

    /**
     * Sets the contents of this invoke log to new text.
     *
     * @param contents the new text for the invoke log
     * @throws IllegalArgumentException if {@code contents == null}
     */
    void setContents(String contents) throws IllegalArgumentException;

    /**
     * Removes this invoke log from persistent storage and render it unusable.
     */
    void dispose();

}

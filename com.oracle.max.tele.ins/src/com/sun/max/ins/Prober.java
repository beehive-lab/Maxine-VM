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
package com.sun.max.ins;

import com.sun.max.ins.gui.*;

/**
 * A display element of an {@link Inspector} that presents visually some aspect of the
 * state of the VM, and so may need to take some action should either the VM state or
 * visual display preferences change.
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public interface Prober {

    /**
     * Brings prober/view up to date with the state of the VM.
     *
     * @param force suspend caching behavior; read state unconditionally.
     */
    void refresh(boolean force);

    /**
     * Revises the display to account for any changes in view configuration or style information; does not imply a state change in the VM.
     */
    void redisplay();

}

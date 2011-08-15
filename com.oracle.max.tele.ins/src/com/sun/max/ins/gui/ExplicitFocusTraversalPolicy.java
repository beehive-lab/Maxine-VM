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
package com.sun.max.ins.gui;

import java.awt.*;
import java.util.*;
import java.util.List;

import com.sun.max.*;

/**
 * A focus traversal policy that is given a sequence of components.
 */
public class ExplicitFocusTraversalPolicy extends FocusTraversalPolicy {

    private final List<Component> order;

    public ExplicitFocusTraversalPolicy(List<Component> components) {
        order = components;
    }

    public ExplicitFocusTraversalPolicy(Component... components) {
        this(Arrays.asList(components));
    }

    private int indexOf(Component component) {
        return Utils.indexOfIdentical(order, component);
    }

    @Override
    public Component getComponentAfter(Container focusCycleRoot, Component component) {
        final int currentIndex = indexOf(component);
        int index = currentIndex;
        Component result;
        do {
            index = (index + 1) % order.size();
            result = order.get(index);
        } while (!result.isFocusable() && index != currentIndex);
        return result;
    }

    @Override
    public Component getComponentBefore(Container focusCycleRoot, Component component) {
        final int currentIndex = indexOf(component);
        int index = currentIndex;
        Component result;
        do {
            --index;
            if (index < 0) {
                index = order.size() - 1;
            }
            result = order.get(index);
        } while (!result.isFocusable() && index != currentIndex);
        return result;
    }

    @Override
    public Component getDefaultComponent(Container focusCycleRoot) {
        return order.get(0);
    }

    @Override
    public Component getLastComponent(Container focusCycleRoot) {
        return Utils.last(order);
    }

    @Override
    public Component getFirstComponent(Container focusCycleRoot) {
        return order.get(0);
    }
}

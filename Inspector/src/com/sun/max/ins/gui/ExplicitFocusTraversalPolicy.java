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
import java.util.*;
import java.util.List;

import com.sun.max.*;

/**
 * A focus traversal policy that is given a sequence of components.
 *
 * @author Doug Simon
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

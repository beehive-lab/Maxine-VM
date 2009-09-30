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

import java.util.*;

import javax.swing.*;

import com.sun.max.collect.*;
import com.sun.max.ins.*;


/**
 * A sequence of items suitable for adding to a {@link InspectorPopupMenu}.
 * <br>
 * Contains only instances of {@link InspectorAction} and {@link JMenu}.
 *
 * @author Michael Van De Vanter
 */
public class InspectorPopupMenuItems implements Iterable<Object>{

    private final LinkSequence<Object> items = new LinkSequence<Object>();

    public Iterator<Object> iterator() {
        return items.iterator();
    }

    public void add(JMenu menu) {
        items.append(menu);
    }

    public void add(InspectorAction action) {
        items.append(action);
    }
}

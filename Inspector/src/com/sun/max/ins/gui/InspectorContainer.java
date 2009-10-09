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

import com.sun.max.collect.*;

/**
 * A marker interface for an inspector that contains other inspectors.
 * The contained inspectors have the container as their parent.
 * The contained inspectors are included in {@linkplain UniqueInspector#find searches} for unique inspectors.
 * 
 * @author Mick Jordan
 * @author Doug Simon
 */
public interface InspectorContainer<Inspector_Type extends Inspector> extends Iterable<Inspector_Type> {

    int length();

    Inspector_Type inspectorAt(int i);

    /**
     * Ensures that the inspector is visible and selected.
     */
    void setSelected(Inspector_Type inspector);

    boolean isSelected(Inspector_Type inspector);

    Inspector_Type getSelected();

    int getSelectedIndex();

    public static final class Static {
        private Static() {

        }

        /**
         * Creates an iterator over the inspectors contained in an inspector container.
         * Because of some odd behavior by tabbed frames, sometimes there are contained components
         * that aren't actually Inspectors.
         */
        public static <Inspector_Type extends Inspector> Iterator<Inspector_Type> iterator(final InspectorContainer<Inspector_Type> container) {
            final LinkSequence<Inspector_Type> inspectors = new LinkSequence<Inspector_Type>();
            for (int index = 0; index < container.length(); index++) {
                final Inspector_Type inspector = container.inspectorAt(index);
                if (inspector != null) {
                    inspectors.append(inspector);
                }
            }
            return inspectors.iterator();
        }
    }

}

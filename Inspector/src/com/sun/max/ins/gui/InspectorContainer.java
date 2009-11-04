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


/**
 * A marker interface for an inspector that contains other inspectors.
 * The contained inspectors have the container as their parent.
  *
 * @author Mick Jordan
 * @author Doug Simon
 * @author Michael Van De Vanter
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
}

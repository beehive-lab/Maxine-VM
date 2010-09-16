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
package com.sun.max.ins;

/**
 * A holder of text for unstructured use by users; persists
 * across sessions.
 *
 * @author Michael Van De Vanter
 */
interface InspectorNotepad {

    /**
     * @return the name of the notepad, even if it has been disposed.
     */
    String getName();

    /**
     * @return the current contents of the notepad, possibly an empty
     * string; null if it has been disposed.
     */
    String getContents();

    /**
     * Sets the contents of this notepad to new text.
     *
     * @param contents the new text for the notepad
     * @throws IllegalArgumentException if {@code contents == null}
     */
    void setContents(String contents) throws IllegalArgumentException;

    /**
     * Removes this notepad from persistent storage and render it unusable.
     */
    void dispose();

}

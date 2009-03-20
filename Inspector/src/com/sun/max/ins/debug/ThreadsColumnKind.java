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
package com.sun.max.ins.debug;

import com.sun.max.collect.*;
import com.sun.max.tele.*;


/**
 * Defines the columns that can be displayed describing a thread in the {@link TeleVM}.
 *
 * @author Michael Van De Vanter
 */
public enum ThreadsColumnKind {

    ID("ID", "ID assigned by VM, none if native", true, -1),
    HANDLE("Handle", "Native thread library handle", true, -1),
    KIND("Kind", null, true, -1),
    NAME("Name", null, true, -1) {
        @Override
        public boolean canBeMadeInvisible() {
            return false;
        }
    },
    STATUS("Status", null, true, -1);


    private final String _label;
    private final String _toolTipText;
    private final boolean _defaultVisibility;
    private final int _minWidth;

    private ThreadsColumnKind(String label, String toolTipText, boolean defaultVisibility, int minWidth) {
        _label = label;
        _toolTipText = toolTipText;
        _defaultVisibility = defaultVisibility;
        _minWidth = minWidth;
        assert defaultVisibility || canBeMadeInvisible();
    }

    /**
     * @return text to appear in the column header
     */
    public String label() {
        return _label;
    }

    /**
     * @return text to appear in the column header's toolTip, null if none specified.
     */
    public String toolTipText() {
        return _toolTipText;
    }

    /**
     * @return minimum width allowed for this column when resized by user; -1 if none specified.
     */
    public int minWidth() {
        return _minWidth;
    }

    @Override
    public String toString() {
        return _label;
    }

    /**
     * @return whether this column kind can be made invisible; default true.
     */
    public boolean canBeMadeInvisible() {
        return true;
    }

    /**
     * Determines if this column should be visible by default; default true.
     */
    public boolean defaultVisibility() {
        return _defaultVisibility;
    }

    public static final IndexedSequence<ThreadsColumnKind> VALUES = new ArraySequence<ThreadsColumnKind>(values());
}

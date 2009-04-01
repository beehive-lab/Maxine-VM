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

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.tele.*;


/**
 * Persistent preferences for viewing threads in the {@link TeleVM}.
 *
 * @author Michael Van De Vanter
  */
public class ThreadsViewPreferences extends com.sun.max.ins.gui.TableColumnVisibilityPreferences<ThreadsColumnKind> {

    private static ThreadsViewPreferences _globalPreferences;

    /**
     * @return the global, persistent set of user preferences for viewing a table of memory regions.
     */
    public static synchronized ThreadsViewPreferences globalPreferences(Inspection inspection) {
        if (_globalPreferences == null) {
            _globalPreferences = new ThreadsViewPreferences(inspection);
        }
        return _globalPreferences;
    }

    /**
    * Creates a set of preferences specified for use by singleton instances, where local and
    * persistent global choices are identical.
    */
    public ThreadsViewPreferences(TableColumnVisibilityPreferences<ThreadsColumnKind> otherPreferences) {
        super(otherPreferences, true);
    }

    private ThreadsViewPreferences(Inspection inspection) {
        super(inspection, "threadsViewPrefs", ThreadsColumnKind.class, ThreadsColumnKind.VALUES);
    }

    @Override
    protected boolean canBeMadeInvisible(ThreadsColumnKind columnType) {
        return columnType.canBeMadeInvisible();
    }

    @Override
    protected boolean defaultVisibility(ThreadsColumnKind columnType) {
        return columnType.defaultVisibility();
    }

    @Override
    protected String label(ThreadsColumnKind columnType) {
        return columnType.label();
    }
}

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
/*VCSID=7be2f9db-ac45-477a-9708-ce304316fb1c*/
package com.sun.max.tele.debug;

import com.sun.max.tele.*;
import com.sun.max.tele.method.*;

/**
 * An abstraction over different kind of breakpoints.
 *
 * @author Michael Van De Vanter
 */
public abstract class TeleBreakpoint extends TeleVMHolder {

    private final boolean _isTransient;

    /**
     * Creates a breakpoint.
     *
     * @param isTransient specifies if the created breakpoint is to be deleted when a process execution stops or an
     *            inspection session finishes
     */
    public TeleBreakpoint(TeleVM teleVM, boolean isTransient) {
        super(teleVM);
        _isTransient = isTransient;
    }

    /**
     * Determines whether this breakpoint is to be deleted when a process execution stops or an inspection session finishes.
     */
    public boolean isTransient() {
        return _isTransient;
    }

    /**
     * @return the location of the breakpoint in the tele VM, expressed in a standard, polymorphic format.
     */
    public abstract TeleCodeLocation teleCodeLocation();

    public abstract boolean enabled();

    /**
     * Updates the enabled state of this breakpoint. This method must not be called on a {@linkplain #isTransient() transient} breakpoint.
     *
     * @param enabled new state for this breakpoint
     * @return true if the state was actually changed
     */
    public abstract boolean setEnabled(boolean enabled);

    /**
     * @return optional conditional specification for breakpoint, null if none
     */
    public abstract BreakpointCondition condition();

    /**
     * removes this breakpoint from the tele VM.
     */
    public abstract void remove();

    /**
     * Gets a string description of the attributes of this breakpoints.
     */
    public String attributesToString() {
        final StringBuilder sb = new StringBuilder(enabled() ? "enabled " : "disabled ");
        if (isTransient()) {
            sb.append("transient ");
        }
        // Remove trailing space character
        assert sb.length() > 0 && sb.charAt(sb.length() - 1) == ' ';
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }
}

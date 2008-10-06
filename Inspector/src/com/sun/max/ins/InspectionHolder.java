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
/*VCSID=fc839aa6-86b9-4930-8a52-170b93c79e05*/
package com.sun.max.ins;

import com.sun.max.ins.gui.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;

/**
 * Convenience methods for classes holding various parts of the interactive inspection session.
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public abstract class InspectionHolder {

    private final Inspection _inspection;

    /**
     * @return holder of the interactive inspection state for the session
     */
    public final Inspection inspection() {
        return _inspection;
    }

    /**
     * @return local surrogate for for the remote ("tele") VM being inspected in the session
     */
    public final TeleVM teleVM() {
        return _inspection.teleVM();
    }

    /**
     * @return local surrogate for the process running the remote ("tele") VM being inspected in the session
     */
    public final TeleProcess teleProcess() {
        return teleVM().teleProcess();
    }

    /**
     * @return visual specifications for user interaction during the session
     */
    public final InspectorStyle style() {
        return _inspection.style();
    }

    /**
     * @return information about the user focus of attention in the view state.
     */
    public InspectionFocus focus() {
        return _inspection.focus();
    }

    protected InspectionHolder(Inspection inspection) {
        _inspection = inspection;
    }

}

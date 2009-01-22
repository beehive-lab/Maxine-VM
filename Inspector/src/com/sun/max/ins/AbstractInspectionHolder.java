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

import com.sun.max.ins.gui.*;
import com.sun.max.tele.*;

/**
 * Convenience methods for classes holding various parts of the interactive inspection session.
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public abstract class AbstractInspectionHolder extends AbstractTeleVMHolder implements InspectionHolder{

    private final Inspection _inspection;

    /**
     * @return holder of the interactive inspection state for the session
     */
    public final Inspection inspection() {
        return _inspection;
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
    public final InspectionFocus focus() {
        return _inspection.focus();
    }

    /**
     * @return access to {@link InspectorAction}s of general use.
     */
    public final InspectionActions actions() {
        return _inspection.actions();
    }

    protected AbstractInspectionHolder(Inspection inspection) {
        super(inspection.teleVM());
        _inspection = inspection;
    }

}

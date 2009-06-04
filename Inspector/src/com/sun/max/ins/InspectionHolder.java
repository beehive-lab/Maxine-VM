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
public interface InspectionHolder {

    /**
     * @return holder of the interactive inspection state for the session
     */
    Inspection inspection();

    /**
     * @return the VM associated with this inspection
     */
    MaxVM maxVM();

    /**
     * @return access to basic GUI services.
     */
    InspectorGUI gui();

    /**
     * @return visual specifications for user interaction during the session
     */
    InspectorStyle style();

    /**
     * @return information about the user focus of attention in the view state.
     */
    InspectionFocus focus();

    /**
     * @return access to {@link InspectorAction}s of general use.
     */
    InspectionActions actions();
}

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

import com.sun.max.tele.debug.*;

/**
 * An abstract adapter class for receiving inspection events.
 * The methods in this class are empty.  This class exists
 * as a convenience for creating listener objects.
 *
 * Extend this class, override the methods of interest, and
 * register with the inspection via
 * {@link Inspection#addInspectionListener(InspectionListener)} and
 * {@link Inspection#removeInspectionListener(InspectionListener)}.
 *
 * @author Michael Van De Vanter
 */
public abstract class InspectionListenerAdapter implements InspectionListener {

    public void vmStateChanged(boolean force) {
    }

    public void threadStateChanged(TeleNativeThread teleNativeThread) {
    }

    public void breakpointSetChanged() {
    }

    public void viewConfigurationChanged() {
    }

    public void vmProcessTerminated() {
    }

}

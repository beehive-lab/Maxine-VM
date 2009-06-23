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
package com.sun.max.tele.method;

import com.sun.max.tele.*;
import com.sun.max.tele.object.*;

/**
 * Holds information about a native method on a tele VM.
 * Unfortunately, we don't know very much, other than a block
 * of native code that we've identified somehow, so this object
 * is really a place holder for information we don't have.
 *
 * @author Michael Van De Vanter
 *
 */
public class TeleNativeRoutine implements TeleRoutine {

    private final TeleNativeTargetRoutine teleNativeTargetRoutine;

    /**
     * @return local {@link TeleNativeTargetRoutine} that holds what
     * we know about the native code for this routine
     */
    public TeleNativeTargetRoutine teleNativeTargetRoutine() {
        return teleNativeTargetRoutine;
    }

    public TeleNativeRoutine(TeleNativeTargetRoutine teleNativeTargetRoutine) {
        this.teleNativeTargetRoutine = teleNativeTargetRoutine;
    }

    public String getUniqueName() {
        return "native method @ " + teleNativeTargetRoutine.targetCodeRegion().start().toHexString();
    }

}

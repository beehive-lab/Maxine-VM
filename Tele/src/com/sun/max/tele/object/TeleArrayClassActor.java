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
package com.sun.max.tele.object;

import java.util.logging.*;

import com.sun.max.jdwp.vm.proxy.*;
import com.sun.max.tele.*;
import com.sun.max.vm.reference.*;

/**
 *
 * Class representing a reference to an array class in the {@link TeleVM}.
 *
 * @author Michael Van De Vanter
 * @author Thomas Wuerthinger
 */
public class TeleArrayClassActor extends TeleReferenceClassActor implements ArrayTypeProvider {

    private static final Logger LOGGER = Logger.getLogger(TeleArrayClassActor.class.getName());

    protected TeleArrayClassActor(TeleVM teleVM, Reference referenceClassActorReference) {
        super(teleVM, referenceClassActorReference);
    }

    public ReferenceTypeProvider elementType() {
        return teleVM().findTeleClassActor(this.classActor().componentClassActor().typeDescriptor());
    }

    public ArrayProvider newInstance(int length) {
        // TODO: Implement the creation of objects in the target VM.
        LOGGER.warning("New instance of length " + length + " requested for array " + this + ", returning null");
        return null;
    }
}

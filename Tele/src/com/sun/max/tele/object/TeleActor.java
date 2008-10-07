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
/*VCSID=e96e39b9-558c-4118-ab8f-5dc8d8862417*/
package com.sun.max.tele.object;

import com.sun.max.tele.*;
import com.sun.max.vm.actor.*;
import com.sun.max.vm.reference.*;

/**
 * Canonical surrogate for an object of type {@link Actor} in the tele VM.
 *
 * @author Michael Van De Vanter
  */
public abstract class TeleActor extends TeleTupleObject {

    protected TeleActor(TeleVM teleVM, Reference actorReference) {
        super(teleVM, actorReference);
    }

    /**
     * @return the generic name of this {@link Actor} copied from the tele VM
     */
    public String readName() {
        final Reference utf8ConstantReference = teleVM().fields().Actor_name.readReference(reference());
        final TeleUtf8Constant teleUtf8Constant = (TeleUtf8Constant) TeleObject.make(teleVM(), utf8ConstantReference);
        return teleUtf8Constant.getString();
    }

}

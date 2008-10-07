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
/*VCSID=41f0161c-5cf0-4330-96da-b0d09f3fb1a7*/
package com.sun.max.tele.object;

import com.sun.max.tele.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.reference.*;


/**
 * Inspector's canonical surrogate for an object of type {@link Hub} in the tele VM.
 *
 * @author Michael Van De Vanter
 *
 */
public abstract class TeleHub extends TeleHybridObject {

    protected TeleHub(TeleVM teleVM, Reference hubReference) {
        super(teleVM, hubReference);
    }

    private TeleClassActor _teleClassActor = null;

    /**
     * @return surrogate for the {@ClassActor} in the tele VM that contains this {@link Hub}, i.e. for the type that this hub helps implement
     */
    public TeleClassActor getTeleClassActor() {
        if (_teleClassActor == null) {
            final Reference classActorReference = teleVM().fields().Hub_classActor.readReference(reference());
            _teleClassActor = (TeleClassActor) TeleObject.make(teleVM(), classActorReference);
        }
        return _teleClassActor;
    }

    /**
     * @return local {@link Hub} corresponding to this {@link Hub} in the tele VM.
     */
    public abstract Hub hub();

    @Override
    protected Object createDeepCopy(DeepCopyContext context) {
        // Translate into local equivalent
        return hub();
    }

}

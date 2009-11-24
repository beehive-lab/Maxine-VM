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

import com.sun.max.jdwp.vm.proxy.*;
import com.sun.max.tele.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.reference.*;

/**
 * Canonical surrogate for a {@link InterfaceMethodActor} in the {@link TeleVM}.
 *
 * @author Michael Van De Vanter
 */
public final class TeleInterfaceMethodActor extends TeleMethodActor {

    // Keep construction minimal for both performance and synchronization.
    protected TeleInterfaceMethodActor(TeleVM teleVM, Reference interfaceMethodActorReference) {
        super(teleVM, interfaceMethodActorReference);
    }

    /**
     * @return A local {@link InterfaceMethodActor} corresponding the the {@link TeleVM}'s {@link InterfaceMethodActor} for the method.
     */
    public InterfaceMethodActor interfaceMethodActor() {
        return (InterfaceMethodActor) methodActor();
    }

    @Override
    public TeleCodeAttribute getTeleCodeAttribute() {
        return null;
    }

    @Override
    public String maxineRole() {
        return "InterfaceMethodActor";
    }

    public TargetMethodAccess[] getTargetMethods() {
        return new TargetMethodAccess[0];
    }
}

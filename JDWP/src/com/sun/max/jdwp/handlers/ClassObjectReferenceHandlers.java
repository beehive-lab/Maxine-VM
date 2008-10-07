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
package com.sun.max.jdwp.handlers;

import com.sun.max.jdwp.data.*;
import com.sun.max.jdwp.protocol.*;
import com.sun.max.jdwp.protocol.ClassObjectReferenceCommands.*;
import com.sun.max.jdwp.vm.proxy.*;

/**
 * @author Thomas Wuerthinger
 *
 */
public class ClassObjectReferenceHandlers extends Handlers {

    public ClassObjectReferenceHandlers(JDWPSession session) {
        super(session);
    }

    @Override
    public void registerWith(CommandHandlerRegistry registry) {
        registry.addCommandHandler(new ReflectedTypeHandler());
    }

    /**
     * <a
     * href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_ClassObjectReference_ReflectedType">
     * JDWP specification for command ClassObjectReference-ReflectedType.</a>
     */
    private class ReflectedTypeHandler extends ClassObjectReferenceCommands.ReflectedType.Handler {

        @Override
        public ReflectedType.Reply handle(ReflectedType.IncomingRequest incomingRequest) throws JDWPException {
            final ClassObjectProvider classObjectProvider = session().getClassObject(incomingRequest._classObject);
            final ReferenceTypeProvider refType = classObjectProvider.getReflectedType();
            return new ReflectedType.Reply((byte) JDWPSession.getValueTypeTag(refType.getType()), session().toID(refType));
        }
    }
}

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
/*VCSID=5676ebef-949a-4b29-a54e-564521f66902*/
package com.sun.max.jdwp.handlers;

import com.sun.max.jdwp.data.*;
import com.sun.max.jdwp.protocol.*;
import com.sun.max.jdwp.protocol.ArrayTypeCommands.*;
import com.sun.max.jdwp.vm.proxy.*;

/**
 * @author Thomas Wuerthinger
 * 
 */
public class ArrayTypeHandlers extends Handlers {

    public ArrayTypeHandlers(JDWPSession session) {
        super(session);
    }

    @Override
    public void registerWith(CommandHandlerRegistry registry) {
        registry.addCommandHandler(new NewInstanceHandler());
    }

    /**
     * <a href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_ArrayType_NewInstance">
     * JDWP specification for command ArrayType-NewInstance.</a>
     */
    private class NewInstanceHandler extends ArrayTypeCommands.NewInstance.Handler {

        @Override
        public NewInstance.Reply handle(NewInstance.IncomingRequest incomingRequest) throws JDWPException {
            final ArrayTypeProvider array = session().getArrayType(incomingRequest._arrType);
            final ArrayProvider newInstance = array.newInstance(incomingRequest._length);
            return new NewInstance.Reply(new JDWPValue(session().toID(newInstance)));
        }
    }
}

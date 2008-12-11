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

import java.util.*;

import com.sun.max.jdwp.data.*;
import com.sun.max.jdwp.protocol.*;
import com.sun.max.jdwp.protocol.ArrayReferenceCommands.*;
import com.sun.max.jdwp.vm.proxy.*;

/**
 * @author Thomas Wuerthinger
 *
 */
public class ArrayReferenceHandlers extends Handlers {

    public ArrayReferenceHandlers(JDWPSession session) {
        super(session);
    }

    @Override
    public void registerWith(CommandHandlerRegistry registry) {
        registry.addCommandHandler(new LengthHandler());
        registry.addCommandHandler(new GetValuesHandler());
        registry.addCommandHandler(new SetValuesHandler());
    }

    /**
     * <a href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_ArrayReference_Length">
     * JDWP specification for command ArrayReference-Length.</a>
     */
    private class LengthHandler extends ArrayReferenceCommands.Length.Handler {

        @Override
        public Length.Reply handle(Length.IncomingRequest incomingRequest) throws JDWPException {
            return new Length.Reply(session().getArray(incomingRequest._arrayObject).length());
        }
    }

    /**
     * <a href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_ArrayReference_GetValues">
     * JDWP specification for command ArrayReference-GetValues.</a>
     */
    private class GetValuesHandler extends ArrayReferenceCommands.GetValues.Handler {

        @Override
        public GetValues.Reply handle(GetValues.IncomingRequest incomingRequest) throws JDWPException {

            final ArrayProvider array = session().getArray(incomingRequest._arrayObject);
            final int firstIndex = incomingRequest._firstIndex;
            final int length = incomingRequest._length;

            final List<JDWPValue> list = new ArrayList<JDWPValue>();
            for (int i = firstIndex; i < firstIndex + length; i++) {
                list.add(session().toJDWPValue(array.getValue(i)));
            }

            return new GetValues.Reply(list);
        }
    }

    /**
     * <a href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_ArrayReference_SetValues">
     * JDWP specification for command ArrayReference-SetValues.</a>
     */
    private class SetValuesHandler extends ArrayReferenceCommands.SetValues.Handler {

        /**
         * WARNING: See the interface method comment for this method.
         */
        @Override
        public int helpAtDecodingUntaggedValue(SetValues.IncomingRequest data) throws JDWPException {
            return JDWPSession.getValueTypeTag(session().getArray(data._arrayObject).getArrayType().elementType().getType());
        }

        @Override
        public SetValues.Reply handle(SetValues.IncomingRequest incomingRequest) throws JDWPException {

            final ArrayProvider array = session().getArray(incomingRequest._arrayObject);
            final int firstIndex = incomingRequest._firstIndex;
            final int length = incomingRequest._values.length;

            for (int i = firstIndex; i < firstIndex + length; i++) {
                array.setValue(i, session().toValue(incomingRequest._values[i - firstIndex]));
            }

            return new SetValues.Reply();
        }
    }
}

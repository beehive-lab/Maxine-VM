/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
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
            return new Length.Reply(session().getArray(incomingRequest.arrayObject).length());
        }
    }

    /**
     * <a href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_ArrayReference_GetValues">
     * JDWP specification for command ArrayReference-GetValues.</a>
     */
    private class GetValuesHandler extends ArrayReferenceCommands.GetValues.Handler {

        @Override
        public GetValues.Reply handle(GetValues.IncomingRequest incomingRequest) throws JDWPException {

            final ArrayProvider array = session().getArray(incomingRequest.arrayObject);
            final int firstIndex = incomingRequest.firstIndex;
            final int length = incomingRequest.length;

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
            return JDWPSession.getValueTypeTag(session().getArray(data.arrayObject).getArrayType().elementType().getType());
        }

        @Override
        public SetValues.Reply handle(SetValues.IncomingRequest incomingRequest) throws JDWPException {

            final ArrayProvider array = session().getArray(incomingRequest.arrayObject);
            final int firstIndex = incomingRequest.firstIndex;
            final int length = incomingRequest.values.length;

            for (int i = firstIndex; i < firstIndex + length; i++) {
                array.setValue(i, session().toValue(incomingRequest.values[i - firstIndex]));
            }

            return new SetValues.Reply();
        }
    }
}

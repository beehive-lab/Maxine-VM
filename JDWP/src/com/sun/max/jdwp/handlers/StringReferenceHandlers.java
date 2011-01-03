/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.max.jdwp.data.*;
import com.sun.max.jdwp.protocol.*;
import com.sun.max.jdwp.protocol.StringReferenceCommands.*;
import com.sun.max.jdwp.vm.proxy.*;

/**
 * @author Thomas Wuerthinger
 * 
 */
public class StringReferenceHandlers extends Handlers {

    public StringReferenceHandlers(JDWPSession session) {
        super(session);
    }

    @Override
    public void registerWith(CommandHandlerRegistry registry) {
        registry.addCommandHandler(new ValueHandler());
    }

    /**
     * <a href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_StringReference_Value">
     * JDWP specification for command StringReference-Value.</a>
     */
    private class ValueHandler extends StringReferenceCommands.Value.Handler {

        @Override
        public Value.Reply handle(Value.IncomingRequest incomingRequest) throws JDWPException {
            final ID.ObjectID objectID = incomingRequest.stringObject;

            // Must be a string ID, seems to be an error in the protocol.
            final ID.StringID stringID = new ID.StringID(objectID.value());

            final StringProvider stringProvider = session().getString(stringID);
            final Value.Reply r = new Value.Reply();
            r.stringValue = stringProvider.stringValue();
            return r;
        }
    }
}

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

import java.util.logging.*;

import com.sun.max.jdwp.data.*;
import com.sun.max.jdwp.protocol.*;
import com.sun.max.jdwp.protocol.StackFrameCommands.*;
import com.sun.max.jdwp.vm.proxy.*;

/**
 * @author Thomas Wuerthinger
 *
 */
public class StackFrameHandlers extends Handlers {

    private static final Logger LOGGER = Logger.getLogger(StackFrameHandlers.class.getName());

    public StackFrameHandlers(JDWPSession session) {
        super(session);
    }

    @Override
    public void registerWith(CommandHandlerRegistry registry) {
        registry.addCommandHandler(new GetValuesHandler());
        registry.addCommandHandler(new SetValuesHandler());
        registry.addCommandHandler(new ThisObjectHandler());
        registry.addCommandHandler(new PopFramesHandler());
    }

    /**
     * <a href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_StackFrame_GetValues">
     * JDWP specification for command StackFrame-GetValues.</a>
     */
    private class GetValuesHandler extends StackFrameCommands.GetValues.Handler {

        @Override
        public GetValues.Reply handle(GetValues.IncomingRequest incomingRequest) throws JDWPException {
            final FrameProvider frame = session().getFrame(incomingRequest.thread, incomingRequest.frame);
            final int slotCount = incomingRequest.slots.length;
            final GetValues.Reply reply = new GetValues.Reply();
            reply.values = new JDWPValue[slotCount];
            for (int i = 0; i < slotCount; i++) {
                final int index = incomingRequest.slots[i].slot;
                final byte sigByte = incomingRequest.slots[i].sigbyte;

                final VMValue v = frame.getValue(index);
                final JDWPValue jdwpValue = session().toJDWPValue(v);
                if (jdwpValue.tag() != sigByte) {
                    // TODO: Make better check that can be asserted!
                    LOGGER.warning("WARNING: Tag bytes do not match, tagByte=" + jdwpValue.tag() + ", sigByte=" + sigByte + "!");
                }
                reply.values[i] = jdwpValue;
            }

            return reply;
        }
    }

    /**
     * <a href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_StackFrame_SetValues">
     * JDWP specification for command StackFrame-SetValues.</a>
     */
    private class SetValuesHandler extends StackFrameCommands.SetValues.Handler {

        @Override
        public SetValues.Reply handle(SetValues.IncomingRequest incomingRequest) throws JDWPException {
            final FrameProvider frame = session().getFrame(incomingRequest.thread, incomingRequest.frame);

            for (int i = 0; i < incomingRequest.slotValues.length; i++) {
                final SetValues.SlotInfo si = incomingRequest.slotValues[i];
                frame.setValue(si.slot, session().toValue(si.slotValue));
            }

            return new SetValues.Reply();
        }
    }

    /**
     * <a href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_StackFrame_ThisObject">
     * JDWP specification for command StackFrame-ThisObject.</a>
     */
    private class ThisObjectHandler extends StackFrameCommands.ThisObject.Handler {

        @Override
        public ThisObject.Reply handle(ThisObject.IncomingRequest incomingRequest) throws JDWPException {
            final FrameProvider frame = session().getFrame(incomingRequest.thread, incomingRequest.frame);
            return new ThisObject.Reply(new JDWPValue(session().toID(frame.thisObject())));
        }
    }

    /**
     * <a href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_StackFrame_PopFrames">
     * JDWP specification for command StackFrame-PopFrames.</a>
     */
    private class PopFramesHandler extends StackFrameCommands.PopFrames.Handler {

        @Override
        public PopFrames.Reply handle(PopFrames.IncomingRequest incomingRequest) throws JDWPException {
            // TODO: Consider implementing this.
            throw new JDWPNotImplementedException();
        }
    }
}

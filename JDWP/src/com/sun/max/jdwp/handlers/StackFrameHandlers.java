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
/*VCSID=41285f23-699e-4265-8dd9-b65afecb04de*/
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
            final FrameProvider frame = session().getFrame(incomingRequest._thread, incomingRequest._frame);
            final int slotCount = incomingRequest._slots.length;
            final GetValues.Reply reply = new GetValues.Reply();
            reply._values = new JDWPValue[slotCount];
            for (int i = 0; i < slotCount; i++) {
                final int index = incomingRequest._slots[i]._slot;
                final byte sigByte = incomingRequest._slots[i]._sigbyte;

                final VMValue v = frame.getValue(index);
                final JDWPValue jdwpValue = session().toJDWPValue(v);
                if (jdwpValue.tag() != sigByte) {
                    // TODO: Make better check that can be asserted!
                    LOGGER.warning("WARNING: Tag bytes do not match, tagByte=" + jdwpValue.tag() + ", sigByte=" + sigByte + "!");
                }
                reply._values[i] = jdwpValue;
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
            final FrameProvider frame = session().getFrame(incomingRequest._thread, incomingRequest._frame);

            for (int i = 0; i < incomingRequest._slotValues.length; i++) {
                final SetValues.SlotInfo si = incomingRequest._slotValues[i];
                frame.setValue(si._slot, session().toValue(si._slotValue));
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
            final FrameProvider frame = session().getFrame(incomingRequest._thread, incomingRequest._frame);
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

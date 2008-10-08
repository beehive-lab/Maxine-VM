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

import com.sun.max.collect.*;
import com.sun.max.jdwp.constants.*;
import com.sun.max.jdwp.constants.Error;
import com.sun.max.jdwp.data.*;
import com.sun.max.jdwp.protocol.*;
import com.sun.max.jdwp.protocol.EventRequestCommands.*;

/**
 * @author Thomas Wuerthinger
 *
 */
public class EventRequestHandlers extends Handlers {

    private VariableMapping<Integer, JDWPEventRequest> _eventRequests;

    public EventRequestHandlers(JDWPSession session) {
        super(session);
        _eventRequests = new ChainedHashMapping<Integer, JDWPEventRequest>();
    }

    @Override
    public void registerWith(CommandHandlerRegistry registry) {
        registry.addCommandHandler(new SetHandler());
        registry.addCommandHandler(new ClearHandler());
        registry.addCommandHandler(new ClearAllBreakpointsHandler());
    }

    /**
     * <a href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_EventRequest_Set"> JDWP
     * specification for command EventRequest-Set.</a>
     */
    private class SetHandler extends EventRequestCommands.Set.Handler {

        @Override
        public Set.Reply handle(Set.IncomingRequest incomingRequest, JDWPSender replyChannel) throws JDWPException {

            final byte eventKind = incomingRequest._eventKind;

            JDWPEventRequest eventRequest = null;

            switch (eventKind) {
                case EventKind.BREAKPOINT:
                    eventRequest = new JDWPEventRequest.Breakpoint(incomingRequest, replyChannel, session());
                    break;
                case EventKind.CLASS_LOAD:
                    throw new JDWPNotImplementedException();
                case EventKind.CLASS_PREPARE:
                    eventRequest = new JDWPEventRequest.ClassPrepare(incomingRequest, replyChannel, session());
                    break;
                case EventKind.CLASS_UNLOAD:
                    eventRequest = new JDWPEventRequest.ClassUnload(incomingRequest, replyChannel, session());
                    break;
                case EventKind.EXCEPTION:
                    eventRequest = new JDWPEventRequest.Exception(incomingRequest, replyChannel, session());
                    break;
                case EventKind.EXCEPTION_CATCH:
                    throw new JDWPNotImplementedException();
                case EventKind.FIELD_ACCESS:
                    throw new JDWPNotImplementedException();
                case EventKind.FIELD_MODIFICATION:
                    throw new JDWPNotImplementedException();
                case EventKind.FRAME_POP:
                    throw new JDWPNotImplementedException();
                case EventKind.METHOD_ENTRY:
                    throw new JDWPNotImplementedException();
                case EventKind.METHOD_EXIT:
                    throw new JDWPNotImplementedException();
                case EventKind.METHOD_EXIT_WITH_RETURN_VALUE:
                    throw new JDWPNotImplementedException();
                case EventKind.MONITOR_CONTENDED_ENTER:
                    throw new JDWPNotImplementedException();
                case EventKind.MONITOR_CONTENDED_ENTERED:
                    throw new JDWPNotImplementedException();
                case EventKind.MONITOR_WAIT:
                    throw new JDWPNotImplementedException();
                case EventKind.MONITOR_WAITED:
                    throw new JDWPNotImplementedException();
                case EventKind.SINGLE_STEP:
                    eventRequest = new JDWPEventRequest.SingleStep(incomingRequest, replyChannel, session());
                    break;
                case EventKind.THREAD_END:
                    eventRequest = new JDWPEventRequest.ThreadDeath(incomingRequest, replyChannel, session());
                    break;
                case EventKind.THREAD_START:
                    eventRequest = new JDWPEventRequest.ThreadStart(incomingRequest, replyChannel, session());
                    break;
                case EventKind.USER_DEFINED:
                    throw new JDWPNotImplementedException();
                case EventKind.VM_DEATH:
                    throw new JDWPNotImplementedException();
                case EventKind.VM_DISCONNECTED:
                    throw new JDWPNotImplementedException();
                case EventKind.VM_START:
                    throw new JDWPNotImplementedException();
                default:
                    throw new JDWPNotImplementedException();
            }

            _eventRequests.put(eventRequest.getId(), eventRequest);
            eventRequest.install();

            return new Set.Reply(eventRequest.getId());
        }
    }

    /**
     * <a href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_EventRequest_Clear"> JDWP
     * specification for command EventRequest-Clear.</a>
     */
    private class ClearHandler extends EventRequestCommands.Clear.Handler {

        @Override
        public Clear.Reply handle(Clear.IncomingRequest incomingRequest) throws JDWPException {

            final int id = incomingRequest._requestID;
            if (!_eventRequests.containsKey(id)) {
                throw new JDWPException((short) Error.INVALID_EVENT_TYPE, "Unknown event request ID");
            }

            final JDWPEventRequest request = _eventRequests.get(id);

            final byte eventKind = incomingRequest._eventKind;
            if (eventKind != request.eventKind()) {
                throw new JDWPException((short) Error.INVALID_EVENT_TYPE, "Event kind does not match - got " + eventKind + ", expected " + request.eventKind());
            }

            removeRequest(request);

            return new Clear.Reply();
        }
    }

    private void removeRequest(JDWPEventRequest r) {
        r.uninstall();
        _eventRequests.remove(r.getId());
    }

    /**
     * <a
     * href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_EventRequest_ClearAllBreakpoints">
     * JDWP specification for command EventRequest-ClearAllBreakpoints.</a>
     */
    private class ClearAllBreakpointsHandler extends EventRequestCommands.ClearAllBreakpoints.Handler {

        @Override
        public ClearAllBreakpoints.Reply handle(ClearAllBreakpoints.IncomingRequest incomingRequest) {

            final AppendableSequence<JDWPEventRequest> toRemove = new LinkSequence<JDWPEventRequest>();
            for (JDWPEventRequest r : _eventRequests.values()) {
                if (r.eventKind() == EventKind.BREAKPOINT) {
                    toRemove.append(r);
                }
            }

            for (JDWPEventRequest r : toRemove) {
                removeRequest(r);
            }

            return new ClearAllBreakpoints.Reply();
        }
    }
}

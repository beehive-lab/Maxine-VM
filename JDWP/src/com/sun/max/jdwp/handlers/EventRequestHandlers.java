/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.sun.max.jdwp.constants.Error;
import com.sun.max.jdwp.constants.EventKind;
import com.sun.max.jdwp.data.CommandHandlerRegistry;
import com.sun.max.jdwp.data.JDWPException;
import com.sun.max.jdwp.data.JDWPNotImplementedException;
import com.sun.max.jdwp.data.JDWPSender;
import com.sun.max.jdwp.protocol.EventRequestCommands;
import com.sun.max.jdwp.protocol.EventRequestCommands.Clear;
import com.sun.max.jdwp.protocol.EventRequestCommands.ClearAllBreakpoints;
import com.sun.max.jdwp.protocol.EventRequestCommands.Set;

/**
 * @author Thomas Wuerthinger
 *
 */
public class EventRequestHandlers extends Handlers {

    private Map<Integer, JDWPEventRequest> eventRequests;

    public EventRequestHandlers(JDWPSession session) {
        super(session);
        eventRequests = new HashMap<Integer, JDWPEventRequest>();
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

            final byte eventKind = incomingRequest.eventKind;

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

            eventRequests.put(eventRequest.getId(), eventRequest);
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

            final int id = incomingRequest.requestID;
            if (!eventRequests.containsKey(id)) {
                throw new JDWPException((short) Error.INVALID_EVENT_TYPE, "Unknown event request ID");
            }

            final JDWPEventRequest request = eventRequests.get(id);

            final byte eventKind = incomingRequest.eventKind;
            if (eventKind != request.eventKind()) {
                throw new JDWPException((short) Error.INVALID_EVENT_TYPE, "Event kind does not match - got " + eventKind + ", expected " + request.eventKind());
            }

            removeRequest(request);

            return new Clear.Reply();
        }
    }

    private void removeRequest(JDWPEventRequest r) {
        r.uninstall();
        eventRequests.remove(r.getId());
    }

    /**
     * <a
     * href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_EventRequest_ClearAllBreakpoints">
     * JDWP specification for command EventRequest-ClearAllBreakpoints.</a>
     */
    private class ClearAllBreakpointsHandler extends EventRequestCommands.ClearAllBreakpoints.Handler {

        @Override
        public ClearAllBreakpoints.Reply handle(ClearAllBreakpoints.IncomingRequest incomingRequest) {

            final List<JDWPEventRequest> toRemove = new LinkedList<JDWPEventRequest>();
            for (JDWPEventRequest r : eventRequests.values()) {
                if (r.eventKind() == EventKind.BREAKPOINT) {
                    toRemove.add(r);
                }
            }

            for (JDWPEventRequest r : toRemove) {
                removeRequest(r);
            }

            return new ClearAllBreakpoints.Reply();
        }
    }
}

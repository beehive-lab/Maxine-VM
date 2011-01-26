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

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import com.sun.max.Utils;
import com.sun.max.jdwp.constants.StepDepth;
import com.sun.max.jdwp.constants.SuspendPolicy;
import com.sun.max.jdwp.data.JDWPException;
import com.sun.max.jdwp.data.JDWPLocation;
import com.sun.max.jdwp.data.JDWPNotImplementedException;
import com.sun.max.jdwp.data.JDWPSender;
import com.sun.max.jdwp.protocol.EventRequestCommands;
import com.sun.max.jdwp.protocol.EventCommands.Composite;
import com.sun.max.jdwp.vm.data.LineTableEntry;
import com.sun.max.jdwp.vm.proxy.JdwpCodeLocation;
import com.sun.max.jdwp.vm.proxy.ThreadProvider;
import com.sun.max.jdwp.vm.proxy.VMListener;

public abstract class JDWPEventRequest<EventsCommon_Type extends Composite.Events.EventsCommon> {

    private static final Logger LOGGER = Logger.getLogger(JDWPEventRequest.class.getName());
    private List<JDWPEventModifier> modifiers;
    private JDWPSession session;
    private EventRequestCommands.Set.IncomingRequest incomingRequest;
    private int id;
    private JDWPSender sender;

    private static int idCounter = 0;

    private JDWPEventRequest(EventRequestCommands.Set.IncomingRequest incomingRequest, JDWPSender sender, JDWPSession session) throws JDWPException {
        this.incomingRequest = incomingRequest;
        this.session = session;
        this.modifiers = JDWPEventModifier.Static.createList(session, incomingRequest.modifiers);
        this.sender = sender;
        assignId();
    }

    private void assignId() {
        idCounter++;
        id = idCounter;
    }

    public int getId() {
        return id;
    }

    public byte eventKind() {
        return incomingRequest.eventKind;
    }

    public byte suspendPolicy() {
        return incomingRequest.suspendPolicy;
    }

    public JDWPSession session() {
        return session;
    }

    public List<JDWPEventModifier> modifiers() {
        return modifiers;
    }

    public abstract void install();

    public abstract void uninstall();

    private void removeModifier(JDWPEventModifier modifier) {
        modifiers.remove(modifier);
    }

    private <JDWPEventModifier_Type extends JDWPEventModifier> JDWPEventModifier_Type lookupMandatoryModifier(Class<JDWPEventModifier_Type> klass) throws JDWPException {

        for (JDWPEventModifier m : modifiers) {
            if (m.getClass().equals(klass)) {
                return Utils.cast(klass, m);
            }
        }

        throw new JDWPNotImplementedException();
    }

    public void eventOccurred(JDWPEventContext context, byte suspendPolicy, EventsCommon_Type eventData) {
        for (JDWPEventModifier m : modifiers) {
            if (!m.isAccepted(context)) {
                return;
            }
        }

        LOGGER.info("Event occurred (suspended: " + suspendPolicy + "): " + this);
        final Composite.Reply r = new Composite.Reply();
        r.suspendPolicy = suspendPolicy;
        r.events = new Composite.Events[1];
        r.events[0] = new Composite.Events();
        r.events[0].eventKind = this.eventKind();
        r.events[0].aEventsCommon = eventData;
        try {
            sender.sendCommand(r);
        } catch (IOException e) {
            LOGGER.severe("Could not send event, because of exception: " + e);
        }
    }

    public Class<? extends JDWPEventModifier>[] validModifiers() {
        return Utils.cast(new Class[0]);
    }

    public static class ClassPrepare extends JDWPEventRequest<Composite.Events.ClassUnload> {

        public ClassPrepare(EventRequestCommands.Set.IncomingRequest incomingRequest, JDWPSender sender, JDWPSession session) throws JDWPException {
            super(incomingRequest, sender, session);
        }

        @Override
        public void install() {
        }

        @Override
        public void uninstall() {
        }
    }

    public static class SingleStep extends JDWPEventRequest<Composite.Events.SingleStep> {
        private ThreadProvider thread;
        private int depth;

        public SingleStep(EventRequestCommands.Set.IncomingRequest incomingRequest, JDWPSender sender, JDWPSession session) throws JDWPException {
            super(incomingRequest, sender, session);
            final JDWPEventModifier.Step step = super.lookupMandatoryModifier(JDWPEventModifier.Step.class);
            this.thread = step.thread();
            this.depth = step.depth();
        }

        private final VMListener listener = new VMAdapter() {

            @Override
            public void singleStepMade(ThreadProvider thr, JdwpCodeLocation location) {
                Logger.getLogger(SingleStep.class.getName()).info("SingleStep was made by thread " + thr + " onto location " + location);
                Logger.getLogger(SingleStep.class.getName()).info("Method name: " + location.method().getName() + ", Signature: " + location.method().getSignature());

                for (LineTableEntry entry : location.method().getLineTable()) {
                    Logger.getLogger(SingleStep.class.getName()).info("Line table entry: " + entry.getCodeIndex() + " line number : " + entry.getLineNumber());
                }
                final JDWPLocation locationHit = session().fromCodeLocation(location);
                if (thread.equals(thr)) {
                    final JDWPEventContext context = new JDWPEventContext(thread, null, locationHit);
                    eventOccurred(context, (byte) SuspendPolicy.ALL,
                                    new Composite.Events.SingleStep(getId(), session().toID(thread), locationHit/*session().fromCodeLocation(_thread.getFrames()[0].getLocation())*/));
                }

            }
        };

        @Override
        public void install() {
            session().vm().addListener(listener);
            if (this.depth == StepDepth.OUT) {
                thread.doStepOut();
            } else {
                thread.doSingleStep();
            }
        }

        @Override
        public void uninstall() {
            session().vm().removeListener(listener);
        }
    }

    public static class Exception extends JDWPEventRequest<Composite.Events.ClassUnload> {

        public Exception(EventRequestCommands.Set.IncomingRequest incomingRequest, JDWPSender sender, JDWPSession session) throws JDWPException {
            super(incomingRequest, sender, session);
        }

        @Override
        public void install() {
        }

        @Override
        public void uninstall() {
        }
    }

    public static class ClassUnload extends JDWPEventRequest<Composite.Events.ClassUnload> {

        public ClassUnload(EventRequestCommands.Set.IncomingRequest incomingRequest, JDWPSender sender, JDWPSession session) throws JDWPException {
            super(incomingRequest, sender, session);
        }

        @Override
        public void install() {
        }

        @Override
        public void uninstall() {
        }
    }

    public static class ThreadDeath extends JDWPEventRequest<Composite.Events.ThreadDeath> {

        public ThreadDeath(EventRequestCommands.Set.IncomingRequest incomingRequest, JDWPSender sender, JDWPSession session) throws JDWPException {
            super(incomingRequest, sender, session);
        }

        private final VMListener listener = new VMAdapter() {

            @Override
            public void threadDied(ThreadProvider thread) {
                final JDWPEventContext context = new JDWPEventContext(thread, null, null);
                eventOccurred(context, (byte) SuspendPolicy.NONE, new Composite.Events.ThreadDeath(getId(), session().toID(thread)));
            }
        };

        @Override
        public void install() {
            session().vm().addListener(listener);
        }

        @Override
        public void uninstall() {
            session().vm().removeListener(listener);
        }
    }

    public static class ThreadStart extends JDWPEventRequest<Composite.Events.ThreadStart> {

        public ThreadStart(EventRequestCommands.Set.IncomingRequest incomingRequest, JDWPSender sender, JDWPSession session) throws JDWPException {
            super(incomingRequest, sender, session);
        }

        private final VMListener listener = new VMAdapter() {

            @Override
            public void threadStarted(ThreadProvider thread) {
                final JDWPEventContext context = new JDWPEventContext(thread, null, null);
                eventOccurred(context, (byte) SuspendPolicy.NONE, new Composite.Events.ThreadStart(getId(), session().toID(thread)));
            }
        };

        @Override
        public void install() {
            session().vm().addListener(listener);
        }

        @Override
        public void uninstall() {
            session().vm().removeListener(listener);
        }
    }

    public static class Breakpoint extends JDWPEventRequest<Composite.Events.Breakpoint> {

        private JDWPLocation location;
        private JdwpCodeLocation codeLocation;

        public Breakpoint(EventRequestCommands.Set.IncomingRequest incomingRequest, JDWPSender sender, JDWPSession session) throws JDWPException {
            super(incomingRequest, sender, session);
            final JDWPEventModifier.LocationOnly locationOnly = super.lookupMandatoryModifier(JDWPEventModifier.LocationOnly.class);
            this.location = locationOnly.location();
            super.removeModifier(locationOnly);
            this.codeLocation = session().toCodeLocation(location);
        }

        private final VMListener listener = new VMAdapter() {

            @Override
            public void breakpointHit(ThreadProvider thread, JdwpCodeLocation loc) {

                Logger.getLogger(Breakpoint.class.getName()).info("Breakpoint was hit by thread " + thread + " on location " + loc);

                final JDWPLocation locationHit = session().fromCodeLocation(loc);
                if (locationHit.equals(location)) {
                    final JDWPEventContext context = new JDWPEventContext(thread, session().methodToReferenceType(loc.method()), locationHit);
                    eventOccurred(context, (byte) SuspendPolicy.ALL, new Composite.Events.Breakpoint(getId(), session().toID(thread), locationHit));
                }

            }
        };

        @Override
        public void install() {
            session().vm().addListener(listener);
            session().vm().addBreakpoint(codeLocation, this.suspendPolicy() == SuspendPolicy.ALL);
        }

        @Override
        public void uninstall() {
            session().vm().removeListener(listener);
        }
    }

}

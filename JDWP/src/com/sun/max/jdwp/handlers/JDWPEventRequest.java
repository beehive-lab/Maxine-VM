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

import java.io.*;
import java.util.logging.*;

import com.sun.max.collect.*;
import com.sun.max.jdwp.constants.*;
import com.sun.max.jdwp.data.*;
import com.sun.max.jdwp.protocol.*;
import com.sun.max.jdwp.protocol.EventCommands.*;
import com.sun.max.jdwp.vm.data.*;
import com.sun.max.jdwp.vm.proxy.*;
import com.sun.max.lang.*;

public abstract class JDWPEventRequest<EventsCommon_Type extends Composite.Events.EventsCommon> {

    private static final Logger LOGGER = Logger.getLogger(JDWPEventRequest.class.getName());
    private VariableSequence<JDWPEventModifier> modifiers;
    private JDWPSession session;
    private EventRequestCommands.Set.IncomingRequest incomingRequest;
    private int id;
    private JDWPSender sender;

    private static int idCounter = 0;

    private JDWPEventRequest(EventRequestCommands.Set.IncomingRequest incomingRequest, JDWPSender sender, JDWPSession session) throws JDWPException {
        this.incomingRequest = incomingRequest;
        this.session = session;
        this.modifiers = new ArrayListSequence<JDWPEventModifier>(JDWPEventModifier.Static.createSequence(session, incomingRequest.modifiers));
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

    public Sequence<JDWPEventModifier> modifiers() {
        return modifiers;
    }

    public abstract void install();

    public abstract void uninstall();

    private void removeModifier(JDWPEventModifier modifier) {
        assert Sequence.Static.indexOfIdentical(modifiers, modifier) != -1;
        modifiers.remove(Sequence.Static.indexOfIdentical(modifiers, modifier));
    }

    private <JDWPEventModifier_Type extends JDWPEventModifier> JDWPEventModifier_Type lookupMandatoryModifier(Class<JDWPEventModifier_Type> klass) throws JDWPException {

        for (JDWPEventModifier m : modifiers) {
            if (m.getClass().equals(klass)) {
                return StaticLoophole.cast(klass, m);
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
        return StaticLoophole.cast(new Class[0]);
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
            public void singleStepMade(ThreadProvider thr, CodeLocation location) {
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
        private CodeLocation codeLocation;

        public Breakpoint(EventRequestCommands.Set.IncomingRequest incomingRequest, JDWPSender sender, JDWPSession session) throws JDWPException {
            super(incomingRequest, sender, session);
            final JDWPEventModifier.LocationOnly locationOnly = super.lookupMandatoryModifier(JDWPEventModifier.LocationOnly.class);
            this.location = locationOnly.location();
            super.removeModifier(locationOnly);
            this.codeLocation = session().toCodeLocation(location);
        }

        private final VMListener listener = new VMAdapter() {

            @Override
            public void breakpointHit(ThreadProvider thread, CodeLocation loc) {

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

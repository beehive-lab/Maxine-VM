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
    private VariableSequence<JDWPEventModifier> _modifiers;
    private JDWPSession _session;
    private EventRequestCommands.Set.IncomingRequest _incomingRequest;
    private int _id;
    private JDWPSender _sender;

    private static int _idCounter = 0;

    private JDWPEventRequest(EventRequestCommands.Set.IncomingRequest incomingRequest, JDWPSender sender, JDWPSession session) throws JDWPException {
        _incomingRequest = incomingRequest;
        _session = session;
        _modifiers = new ArrayListSequence<JDWPEventModifier>(JDWPEventModifier.Static.createSequence(session, incomingRequest._modifiers));
        _sender = sender;
        assignId();
    }

    private void assignId() {
        _idCounter++;
        _id = _idCounter;
    }

    public int getId() {
        return _id;
    }

    public byte eventKind() {
        return _incomingRequest._eventKind;
    }

    public byte suspendPolicy() {
        return _incomingRequest._suspendPolicy;
    }

    public JDWPSession session() {
        return _session;
    }

    public Sequence<JDWPEventModifier> modifiers() {
        return _modifiers;
    }

    public abstract void install();

    public abstract void uninstall();

    private void removeModifier(JDWPEventModifier modifier) {
        assert Sequence.Static.indexOfIdentical(_modifiers, modifier) != -1;
        _modifiers.remove(Sequence.Static.indexOfIdentical(_modifiers, modifier));
    }

    private <JDWPEventModifier_Type extends JDWPEventModifier> JDWPEventModifier_Type lookupMandatoryModifier(Class<JDWPEventModifier_Type> klass) throws JDWPException {

        for (JDWPEventModifier m : _modifiers) {
            if (m.getClass().equals(klass)) {
                return StaticLoophole.cast(klass, m);
            }
        }

        throw new JDWPNotImplementedException();
    }

    public void eventOccurred(JDWPEventContext context, byte suspendPolicy, EventsCommon_Type eventData) {
        for (JDWPEventModifier m : _modifiers) {
            if (!m.isAccepted(context)) {
                return;
            }
        }

        LOGGER.info("Event occurred (suspended: " + suspendPolicy + "): " + this);
        final Composite.Reply r = new Composite.Reply();
        r._suspendPolicy = suspendPolicy;
        r._events = new Composite.Events[1];
        r._events[0] = new Composite.Events();
        r._events[0]._eventKind = this.eventKind();
        r._events[0]._aEventsCommon = eventData;
        try {
            _sender.sendCommand(r);
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
        private ThreadProvider _thread;
        private int _depth;

        public SingleStep(EventRequestCommands.Set.IncomingRequest incomingRequest, JDWPSender sender, JDWPSession session) throws JDWPException {
            super(incomingRequest, sender, session);
            final JDWPEventModifier.Step step = super.lookupMandatoryModifier(JDWPEventModifier.Step.class);
            _thread = step.thread();
            _depth = step.depth();
        }


        private final VMListener _listener = new VMAdapter() {

            @Override
            public void singleStepMade(ThreadProvider thread, CodeLocation location) {

                Logger.getLogger(SingleStep.class.getName()).info("SingleStep was made by thread " + thread + " onto location " + location);


                Logger.getLogger(SingleStep.class.getName()).info("Method name: " + location.method().getName() + ", Signature: " + location.method().getSignature());

                for (LineTableEntry entry : location.method().getLineTable()) {
                    Logger.getLogger(SingleStep.class.getName()).info("Line table entry: " + entry.getCodeIndex() + " line number : " + entry.getLineNumber());
                }
                final JDWPLocation locationHit = session().fromCodeLocation(location);
                if (_thread.equals(thread)) {
                    final JDWPEventContext context = new JDWPEventContext(_thread, null, locationHit);
                    eventOccurred(context, (byte) SuspendPolicy.ALL,
                                    new Composite.Events.SingleStep(getId(), session().toID(_thread), locationHit/*session().fromCodeLocation(_thread.getFrames()[0].getLocation())*/));
                }

            }
        };

        @Override
        public void install() {
            session().vm().addListener(_listener);
            if (this._depth == StepDepth.OUT) {
                _thread.doStepOut();
            } else {
                _thread.doSingleStep();
            }
        }

        @Override
        public void uninstall() {
            session().vm().removeListener(_listener);
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

        private final VMListener _listener = new VMAdapter() {

            @Override
            public void threadDied(ThreadProvider thread) {
                final JDWPEventContext context = new JDWPEventContext(thread, null, null);
                eventOccurred(context, (byte) SuspendPolicy.NONE, new Composite.Events.ThreadDeath(getId(), session().toID(thread)));
            }
        };

        @Override
        public void install() {
            session().vm().addListener(_listener);
        }

        @Override
        public void uninstall() {
            session().vm().removeListener(_listener);
        }
    }

    public static class ThreadStart extends JDWPEventRequest<Composite.Events.ThreadStart> {

        public ThreadStart(EventRequestCommands.Set.IncomingRequest incomingRequest, JDWPSender sender, JDWPSession session) throws JDWPException {
            super(incomingRequest, sender, session);
        }

        private final VMListener _listener = new VMAdapter() {

            @Override
            public void threadStarted(ThreadProvider thread) {
                final JDWPEventContext context = new JDWPEventContext(thread, null, null);
                eventOccurred(context, (byte) SuspendPolicy.NONE, new Composite.Events.ThreadStart(getId(), session().toID(thread)));
            }
        };

        @Override
        public void install() {
            session().vm().addListener(_listener);
        }

        @Override
        public void uninstall() {
            session().vm().removeListener(_listener);
        }
    }

    public static class Breakpoint extends JDWPEventRequest<Composite.Events.Breakpoint> {

        private JDWPLocation _location;
        private CodeLocation _codeLocation;

        public Breakpoint(EventRequestCommands.Set.IncomingRequest incomingRequest, JDWPSender sender, JDWPSession session) throws JDWPException {
            super(incomingRequest, sender, session);
            final JDWPEventModifier.LocationOnly locationOnly = super.lookupMandatoryModifier(JDWPEventModifier.LocationOnly.class);
            _location = locationOnly.location();
            super.removeModifier(locationOnly);
            _codeLocation = session().toCodeLocation(_location);
        }

        private final VMListener _listener = new VMAdapter() {

            @Override
            public void breakpointHit(ThreadProvider thread, CodeLocation location) {

                Logger.getLogger(Breakpoint.class.getName()).info("Breakpoint was hit by thread " + thread + " on location " + location);

                final JDWPLocation locationHit = session().fromCodeLocation(location);
                if (locationHit.equals(_location)) {
                    final JDWPEventContext context = new JDWPEventContext(thread, session().methodToReferenceType(location.method()), locationHit);
                    eventOccurred(context, (byte) SuspendPolicy.ALL, new Composite.Events.Breakpoint(getId(), session().toID(thread), locationHit));
                }

            }
        };

        @Override
        public void install() {
            session().vm().addListener(_listener);
            session().vm().addBreakpoint(_codeLocation, this.suspendPolicy() == SuspendPolicy.ALL);
        }

        @Override
        public void uninstall() {
            session().vm().removeListener(_listener);
        }
    }

}

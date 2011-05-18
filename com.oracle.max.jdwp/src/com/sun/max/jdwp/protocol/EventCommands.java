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

// WARNING: Generated file, do not modify!

// To use the original JDWP specification as a basis, some styleguides have to be turned off
// Checkstyle: stop field name check

package com.sun.max.jdwp.protocol;
import com.sun.max.jdwp.data.*;
import com.sun.max.jdwp.constants.*;

public final class EventCommands {
    public static final int COMMAND_SET = 64;
    private EventCommands() { }  // hide constructor

    public static class Composite {
        public static final byte COMMAND = 100;
        public abstract static class Handler implements CommandHandler<IncomingRequest, Reply> {
            public IncomingRequest createIncomingDataObject() { return new IncomingRequest(); }
            public int helpAtDecodingUntaggedValue(IncomingRequest incomingRequest) throws JDWPException {assert false : "If this method can be called, it must be overwritten in subclasses!"; return 0; }
            public Reply handle(IncomingRequest incomingRequest, JDWPSender replyChannel) throws JDWPException { return handle(incomingRequest); }
            public Reply handle(IncomingRequest incomingRequest) throws JDWPException { throw new JDWPNotImplementedException(); }
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }
        }
        public static class IncomingRequest implements IncomingData {
            public void read(JDWPInputStream ps) throws JDWPException { }
        }

        public static class Events {
            public abstract static class EventsCommon {
                public abstract void write(JDWPOutputStream ps) throws java.io.IOException;
                public abstract void read(JDWPInputStream ps) throws java.io.IOException, JDWPException;
                public abstract byte eventKind();
            }

            public byte eventKind;
            public EventsCommon aEventsCommon;

            public Events(byte eventKind, EventsCommon aEventsCommon) {
                this.eventKind = eventKind;
                this.aEventsCommon = aEventsCommon;
            }

            public Events() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                eventKind = ps.readByte();
                switch (eventKind) {
                    case EventKind.VM_START:
                        aEventsCommon = new VMStart();
                        break;
                    case EventKind.SINGLE_STEP:
                        aEventsCommon = new SingleStep();
                        break;
                    case EventKind.BREAKPOINT:
                        aEventsCommon = new Breakpoint();
                        break;
                    case EventKind.METHOD_ENTRY:
                        aEventsCommon = new MethodEntry();
                        break;
                    case EventKind.METHOD_EXIT:
                        aEventsCommon = new MethodExit();
                        break;
                    case EventKind.METHOD_EXIT_WITH_RETURN_VALUE:
                        aEventsCommon = new MethodExitWithReturnValue();
                        break;
                    case EventKind.MONITOR_CONTENDED_ENTER:
                        aEventsCommon = new MonitorContendedEnter();
                        break;
                    case EventKind.MONITOR_CONTENDED_ENTERED:
                        aEventsCommon = new MonitorContendedEntered();
                        break;
                    case EventKind.MONITOR_WAIT:
                        aEventsCommon = new MonitorWait();
                        break;
                    case EventKind.MONITOR_WAITED:
                        aEventsCommon = new MonitorWaited();
                        break;
                    case EventKind.EXCEPTION:
                        aEventsCommon = new Exception();
                        break;
                    case EventKind.THREAD_START:
                        aEventsCommon = new ThreadStart();
                        break;
                    case EventKind.THREAD_DEATH:
                        aEventsCommon = new ThreadDeath();
                        break;
                    case EventKind.CLASS_PREPARE:
                        aEventsCommon = new ClassPrepare();
                        break;
                    case EventKind.CLASS_UNLOAD:
                        aEventsCommon = new ClassUnload();
                        break;
                    case EventKind.FIELD_ACCESS:
                        aEventsCommon = new FieldAccess();
                        break;
                    case EventKind.FIELD_MODIFICATION:
                        aEventsCommon = new FieldModification();
                        break;
                    case EventKind.VM_DEATH:
                        aEventsCommon = new VMDeath();
                        break;
                }
                aEventsCommon.read(ps);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(eventKind);
                aEventsCommon.write(ps);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("eventKind=" + eventKind);
                stringBuilder.append(aEventsCommon);
                return stringBuilder.toString();
            }

            public static class VMStart extends EventsCommon {
                public static final byte ALT_ID = EventKind.VM_START;
                @Override
                public byte eventKind() {
                    return ALT_ID;
                }

                public int requestID;

                public ID.ThreadID thread;
                public VMStart(int requestID,
                    ID.ThreadID thread) {
                    this.requestID = requestID;
                    this.thread = thread;
                }
                public VMStart() {
                }
                @Override
                public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                    requestID = ps.readInt();
                    thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
                }
                @Override
                public void write(JDWPOutputStream ps) throws java.io.IOException {
                    ps.write(requestID);
                    thread.write(ps.getOutputStream());
                }
                @Override
                public String toString() {
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("requestID=" + requestID);
                    stringBuilder.append(", ");
                    stringBuilder.append("thread=" + thread);
                    return stringBuilder.toString();
                }
            }

            public static class SingleStep extends EventsCommon {
                public static final byte ALT_ID = EventKind.SINGLE_STEP;
                @Override
                public byte eventKind() {
                    return ALT_ID;
                }

                public int requestID;

                public ID.ThreadID thread;

                public JDWPLocation location;
                public SingleStep(int requestID,
                    ID.ThreadID thread,
                    JDWPLocation location) {
                    this.requestID = requestID;
                    this.thread = thread;
                    this.location = location;
                }
                public SingleStep() {
                }
                @Override
                public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                    requestID = ps.readInt();
                    thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
                    location = ps.readLocation();
                }
                @Override
                public void write(JDWPOutputStream ps) throws java.io.IOException {
                    ps.write(requestID);
                    thread.write(ps.getOutputStream());
                    ps.write(location);
                }
                @Override
                public String toString() {
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("requestID=" + requestID);
                    stringBuilder.append(", ");
                    stringBuilder.append("thread=" + thread);
                    stringBuilder.append(", ");
                    stringBuilder.append("location=" + location);
                    return stringBuilder.toString();
                }
            }

            public static class Breakpoint extends EventsCommon {
                public static final byte ALT_ID = EventKind.BREAKPOINT;
                @Override
                public byte eventKind() {
                    return ALT_ID;
                }

                public int requestID;

                public ID.ThreadID thread;

                public JDWPLocation location;
                public Breakpoint(int requestID,
                    ID.ThreadID thread,
                    JDWPLocation location) {
                    this.requestID = requestID;
                    this.thread = thread;
                    this.location = location;
                }
                public Breakpoint() {
                }
                @Override
                public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                    requestID = ps.readInt();
                    thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
                    location = ps.readLocation();
                }
                @Override
                public void write(JDWPOutputStream ps) throws java.io.IOException {
                    ps.write(requestID);
                    thread.write(ps.getOutputStream());
                    ps.write(location);
                }
                @Override
                public String toString() {
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("requestID=" + requestID);
                    stringBuilder.append(", ");
                    stringBuilder.append("thread=" + thread);
                    stringBuilder.append(", ");
                    stringBuilder.append("location=" + location);
                    return stringBuilder.toString();
                }
            }

            public static class MethodEntry extends EventsCommon {
                public static final byte ALT_ID = EventKind.METHOD_ENTRY;
                @Override
                public byte eventKind() {
                    return ALT_ID;
                }

                public int requestID;

                public ID.ThreadID thread;

                public JDWPLocation location;
                public MethodEntry(int requestID,
                    ID.ThreadID thread,
                    JDWPLocation location) {
                    this.requestID = requestID;
                    this.thread = thread;
                    this.location = location;
                }
                public MethodEntry() {
                }
                @Override
                public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                    requestID = ps.readInt();
                    thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
                    location = ps.readLocation();
                }
                @Override
                public void write(JDWPOutputStream ps) throws java.io.IOException {
                    ps.write(requestID);
                    thread.write(ps.getOutputStream());
                    ps.write(location);
                }
                @Override
                public String toString() {
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("requestID=" + requestID);
                    stringBuilder.append(", ");
                    stringBuilder.append("thread=" + thread);
                    stringBuilder.append(", ");
                    stringBuilder.append("location=" + location);
                    return stringBuilder.toString();
                }
            }

            public static class MethodExit extends EventsCommon {
                public static final byte ALT_ID = EventKind.METHOD_EXIT;
                @Override
                public byte eventKind() {
                    return ALT_ID;
                }

                public int requestID;

                public ID.ThreadID thread;

                public JDWPLocation location;
                public MethodExit(int requestID,
                    ID.ThreadID thread,
                    JDWPLocation location) {
                    this.requestID = requestID;
                    this.thread = thread;
                    this.location = location;
                }
                public MethodExit() {
                }
                @Override
                public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                    requestID = ps.readInt();
                    thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
                    location = ps.readLocation();
                }
                @Override
                public void write(JDWPOutputStream ps) throws java.io.IOException {
                    ps.write(requestID);
                    thread.write(ps.getOutputStream());
                    ps.write(location);
                }
                @Override
                public String toString() {
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("requestID=" + requestID);
                    stringBuilder.append(", ");
                    stringBuilder.append("thread=" + thread);
                    stringBuilder.append(", ");
                    stringBuilder.append("location=" + location);
                    return stringBuilder.toString();
                }
            }

            public static class MethodExitWithReturnValue extends EventsCommon {
                public static final byte ALT_ID = EventKind.METHOD_EXIT_WITH_RETURN_VALUE;
                @Override
                public byte eventKind() {
                    return ALT_ID;
                }

                public int requestID;

                public ID.ThreadID thread;

                public JDWPLocation location;

                public JDWPValue value;
                public MethodExitWithReturnValue(int requestID,
                    ID.ThreadID thread,
                    JDWPLocation location,
                    JDWPValue value) {
                    this.requestID = requestID;
                    this.thread = thread;
                    this.location = location;
                    this.value = value;
                }
                public MethodExitWithReturnValue() {
                }
                @Override
                public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                    requestID = ps.readInt();
                    thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
                    location = ps.readLocation();
                    value = ps.readValue();
                }
                @Override
                public void write(JDWPOutputStream ps) throws java.io.IOException {
                    ps.write(requestID);
                    thread.write(ps.getOutputStream());
                    ps.write(location);
                    ps.write(value);
                }
                @Override
                public String toString() {
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("requestID=" + requestID);
                    stringBuilder.append(", ");
                    stringBuilder.append("thread=" + thread);
                    stringBuilder.append(", ");
                    stringBuilder.append("location=" + location);
                    stringBuilder.append(", ");
                    stringBuilder.append("value=" + value);
                    return stringBuilder.toString();
                }
            }

            public static class MonitorContendedEnter extends EventsCommon {
                public static final byte ALT_ID = EventKind.MONITOR_CONTENDED_ENTER;
                @Override
                public byte eventKind() {
                    return ALT_ID;
                }

                public int requestID;

                public ID.ThreadID thread;

                public JDWPValue object;

                public JDWPLocation location;
                public MonitorContendedEnter(int requestID,
                    ID.ThreadID thread,
                    JDWPValue object,
                    JDWPLocation location) {
                    this.requestID = requestID;
                    this.thread = thread;
                    this.object = object;
                    this.location = location;
                }
                public MonitorContendedEnter() {
                }
                @Override
                public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                    requestID = ps.readInt();
                    thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
                    object = ps.readValue();
                    location = ps.readLocation();
                }
                @Override
                public void write(JDWPOutputStream ps) throws java.io.IOException {
                    ps.write(requestID);
                    thread.write(ps.getOutputStream());
                    ps.write(object);
                    ps.write(location);
                }
                @Override
                public String toString() {
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("requestID=" + requestID);
                    stringBuilder.append(", ");
                    stringBuilder.append("thread=" + thread);
                    stringBuilder.append(", ");
                    stringBuilder.append("object=" + object);
                    stringBuilder.append(", ");
                    stringBuilder.append("location=" + location);
                    return stringBuilder.toString();
                }
            }

            public static class MonitorContendedEntered extends EventsCommon {
                public static final byte ALT_ID = EventKind.MONITOR_CONTENDED_ENTERED;
                @Override
                public byte eventKind() {
                    return ALT_ID;
                }

                public int requestID;

                public ID.ThreadID thread;

                public JDWPValue object;

                public JDWPLocation location;
                public MonitorContendedEntered(int requestID,
                    ID.ThreadID thread,
                    JDWPValue object,
                    JDWPLocation location) {
                    this.requestID = requestID;
                    this.thread = thread;
                    this.object = object;
                    this.location = location;
                }
                public MonitorContendedEntered() {
                }
                @Override
                public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                    requestID = ps.readInt();
                    thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
                    object = ps.readValue();
                    location = ps.readLocation();
                }
                @Override
                public void write(JDWPOutputStream ps) throws java.io.IOException {
                    ps.write(requestID);
                    thread.write(ps.getOutputStream());
                    ps.write(object);
                    ps.write(location);
                }
                @Override
                public String toString() {
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("requestID=" + requestID);
                    stringBuilder.append(", ");
                    stringBuilder.append("thread=" + thread);
                    stringBuilder.append(", ");
                    stringBuilder.append("object=" + object);
                    stringBuilder.append(", ");
                    stringBuilder.append("location=" + location);
                    return stringBuilder.toString();
                }
            }

            public static class MonitorWait extends EventsCommon {
                public static final byte ALT_ID = EventKind.MONITOR_WAIT;
                @Override
                public byte eventKind() {
                    return ALT_ID;
                }

                public int requestID;

                public ID.ThreadID thread;

                public JDWPValue object;

                public JDWPLocation location;

                public long timeout;
                public MonitorWait(int requestID,
                    ID.ThreadID thread,
                    JDWPValue object,
                    JDWPLocation location,
                    long timeout) {
                    this.requestID = requestID;
                    this.thread = thread;
                    this.object = object;
                    this.location = location;
                    this.timeout = timeout;
                }
                public MonitorWait() {
                }
                @Override
                public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                    requestID = ps.readInt();
                    thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
                    object = ps.readValue();
                    location = ps.readLocation();
                    timeout = ps.readLong();
                }
                @Override
                public void write(JDWPOutputStream ps) throws java.io.IOException {
                    ps.write(requestID);
                    thread.write(ps.getOutputStream());
                    ps.write(object);
                    ps.write(location);
                    ps.write(timeout);
                }
                @Override
                public String toString() {
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("requestID=" + requestID);
                    stringBuilder.append(", ");
                    stringBuilder.append("thread=" + thread);
                    stringBuilder.append(", ");
                    stringBuilder.append("object=" + object);
                    stringBuilder.append(", ");
                    stringBuilder.append("location=" + location);
                    stringBuilder.append(", ");
                    stringBuilder.append("timeout=" + timeout);
                    return stringBuilder.toString();
                }
            }

            public static class MonitorWaited extends EventsCommon {
                public static final byte ALT_ID = EventKind.MONITOR_WAITED;
                @Override
                public byte eventKind() {
                    return ALT_ID;
                }

                public int requestID;

                public ID.ThreadID thread;

                public JDWPValue object;

                public JDWPLocation location;

                public boolean timed_out;
                public MonitorWaited(int requestID,
                    ID.ThreadID thread,
                    JDWPValue object,
                    JDWPLocation location,
                    boolean timed_out) {
                    this.requestID = requestID;
                    this.thread = thread;
                    this.object = object;
                    this.location = location;
                    this.timed_out = timed_out;
                }
                public MonitorWaited() {
                }
                @Override
                public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                    requestID = ps.readInt();
                    thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
                    object = ps.readValue();
                    location = ps.readLocation();
                    timed_out = ps.readBoolean();
                }
                @Override
                public void write(JDWPOutputStream ps) throws java.io.IOException {
                    ps.write(requestID);
                    thread.write(ps.getOutputStream());
                    ps.write(object);
                    ps.write(location);
                    ps.write(timed_out);
                }
                @Override
                public String toString() {
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("requestID=" + requestID);
                    stringBuilder.append(", ");
                    stringBuilder.append("thread=" + thread);
                    stringBuilder.append(", ");
                    stringBuilder.append("object=" + object);
                    stringBuilder.append(", ");
                    stringBuilder.append("location=" + location);
                    stringBuilder.append(", ");
                    stringBuilder.append("timed_out=" + timed_out);
                    return stringBuilder.toString();
                }
            }

            public static class Exception extends EventsCommon {
                public static final byte ALT_ID = EventKind.EXCEPTION;
                @Override
                public byte eventKind() {
                    return ALT_ID;
                }

                public int requestID;

                public ID.ThreadID thread;

                public JDWPLocation location;

                public JDWPValue exception;

                public JDWPLocation catchLocation;
                public Exception(int requestID,
                    ID.ThreadID thread,
                    JDWPLocation location,
                    JDWPValue exception,
                    JDWPLocation catchLocation) {
                    this.requestID = requestID;
                    this.thread = thread;
                    this.location = location;
                    this.exception = exception;
                    this.catchLocation = catchLocation;
                }
                public Exception() {
                }
                @Override
                public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                    requestID = ps.readInt();
                    thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
                    location = ps.readLocation();
                    exception = ps.readValue();
                    catchLocation = ps.readLocation();
                }
                @Override
                public void write(JDWPOutputStream ps) throws java.io.IOException {
                    ps.write(requestID);
                    thread.write(ps.getOutputStream());
                    ps.write(location);
                    ps.write(exception);
                    ps.write(catchLocation);
                }
                @Override
                public String toString() {
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("requestID=" + requestID);
                    stringBuilder.append(", ");
                    stringBuilder.append("thread=" + thread);
                    stringBuilder.append(", ");
                    stringBuilder.append("location=" + location);
                    stringBuilder.append(", ");
                    stringBuilder.append("exception=" + exception);
                    stringBuilder.append(", ");
                    stringBuilder.append("catchLocation=" + catchLocation);
                    return stringBuilder.toString();
                }
            }

            public static class ThreadStart extends EventsCommon {
                public static final byte ALT_ID = EventKind.THREAD_START;
                @Override
                public byte eventKind() {
                    return ALT_ID;
                }

                public int requestID;

                public ID.ThreadID thread;
                public ThreadStart(int requestID,
                    ID.ThreadID thread) {
                    this.requestID = requestID;
                    this.thread = thread;
                }
                public ThreadStart() {
                }
                @Override
                public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                    requestID = ps.readInt();
                    thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
                }
                @Override
                public void write(JDWPOutputStream ps) throws java.io.IOException {
                    ps.write(requestID);
                    thread.write(ps.getOutputStream());
                }
                @Override
                public String toString() {
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("requestID=" + requestID);
                    stringBuilder.append(", ");
                    stringBuilder.append("thread=" + thread);
                    return stringBuilder.toString();
                }
            }

            public static class ThreadDeath extends EventsCommon {
                public static final byte ALT_ID = EventKind.THREAD_DEATH;
                @Override
                public byte eventKind() {
                    return ALT_ID;
                }

                public int requestID;

                public ID.ThreadID thread;
                public ThreadDeath(int requestID,
                    ID.ThreadID thread) {
                    this.requestID = requestID;
                    this.thread = thread;
                }
                public ThreadDeath() {
                }
                @Override
                public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                    requestID = ps.readInt();
                    thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
                }
                @Override
                public void write(JDWPOutputStream ps) throws java.io.IOException {
                    ps.write(requestID);
                    thread.write(ps.getOutputStream());
                }
                @Override
                public String toString() {
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("requestID=" + requestID);
                    stringBuilder.append(", ");
                    stringBuilder.append("thread=" + thread);
                    return stringBuilder.toString();
                }
            }

            public static class ClassPrepare extends EventsCommon {
                public static final byte ALT_ID = EventKind.CLASS_PREPARE;
                @Override
                public byte eventKind() {
                    return ALT_ID;
                }

                public int requestID;

                public ID.ThreadID thread;

                public byte refTypeTag;

                public ID.ReferenceTypeID typeID;

                public String signature;

                public int status;
                public ClassPrepare(int requestID,
                    ID.ThreadID thread,
                    byte refTypeTag,
                    ID.ReferenceTypeID typeID,
                    String signature,
                    int status) {
                    this.requestID = requestID;
                    this.thread = thread;
                    this.refTypeTag = refTypeTag;
                    this.typeID = typeID;
                    this.signature = signature;
                    this.status = status;
                }
                public ClassPrepare() {
                }
                @Override
                public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                    requestID = ps.readInt();
                    thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
                    refTypeTag = ps.readByte();
                    typeID = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
                    signature = ps.readString();
                    status = ps.readInt();
                }
                @Override
                public void write(JDWPOutputStream ps) throws java.io.IOException {
                    ps.write(requestID);
                    thread.write(ps.getOutputStream());
                    ps.write(refTypeTag);
                    typeID.write(ps.getOutputStream());
                    ps.write(signature);
                    ps.write(status);
                }
                @Override
                public String toString() {
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("requestID=" + requestID);
                    stringBuilder.append(", ");
                    stringBuilder.append("thread=" + thread);
                    stringBuilder.append(", ");
                    stringBuilder.append("refTypeTag=" + refTypeTag);
                    stringBuilder.append(", ");
                    stringBuilder.append("typeID=" + typeID);
                    stringBuilder.append(", ");
                    stringBuilder.append("signature=" + signature);
                    stringBuilder.append(", ");
                    stringBuilder.append("status=" + status);
                    return stringBuilder.toString();
                }
            }

            public static class ClassUnload extends EventsCommon {
                public static final byte ALT_ID = EventKind.CLASS_UNLOAD;
                @Override
                public byte eventKind() {
                    return ALT_ID;
                }

                public int requestID;

                public String signature;
                public ClassUnload(int requestID,
                    String signature) {
                    this.requestID = requestID;
                    this.signature = signature;
                }
                public ClassUnload() {
                }
                @Override
                public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                    requestID = ps.readInt();
                    signature = ps.readString();
                }
                @Override
                public void write(JDWPOutputStream ps) throws java.io.IOException {
                    ps.write(requestID);
                    ps.write(signature);
                }
                @Override
                public String toString() {
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("requestID=" + requestID);
                    stringBuilder.append(", ");
                    stringBuilder.append("signature=" + signature);
                    return stringBuilder.toString();
                }
            }

            public static class FieldAccess extends EventsCommon {
                public static final byte ALT_ID = EventKind.FIELD_ACCESS;
                @Override
                public byte eventKind() {
                    return ALT_ID;
                }

                public int requestID;

                public ID.ThreadID thread;

                public JDWPLocation location;

                public byte refTypeTag;

                public ID.ReferenceTypeID typeID;

                public ID.FieldID fieldID;

                public JDWPValue object;
                public FieldAccess(int requestID,
                    ID.ThreadID thread,
                    JDWPLocation location,
                    byte refTypeTag,
                    ID.ReferenceTypeID typeID,
                    ID.FieldID fieldID,
                    JDWPValue object) {
                    this.requestID = requestID;
                    this.thread = thread;
                    this.location = location;
                    this.refTypeTag = refTypeTag;
                    this.typeID = typeID;
                    this.fieldID = fieldID;
                    this.object = object;
                }
                public FieldAccess() {
                }
                @Override
                public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                    requestID = ps.readInt();
                    thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
                    location = ps.readLocation();
                    refTypeTag = ps.readByte();
                    typeID = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
                    fieldID = ID.read(ps.getInputStream(), ID.FieldID.class);
                    object = ps.readValue();
                }
                @Override
                public void write(JDWPOutputStream ps) throws java.io.IOException {
                    ps.write(requestID);
                    thread.write(ps.getOutputStream());
                    ps.write(location);
                    ps.write(refTypeTag);
                    typeID.write(ps.getOutputStream());
                    fieldID.write(ps.getOutputStream());
                    ps.write(object);
                }
                @Override
                public String toString() {
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("requestID=" + requestID);
                    stringBuilder.append(", ");
                    stringBuilder.append("thread=" + thread);
                    stringBuilder.append(", ");
                    stringBuilder.append("location=" + location);
                    stringBuilder.append(", ");
                    stringBuilder.append("refTypeTag=" + refTypeTag);
                    stringBuilder.append(", ");
                    stringBuilder.append("typeID=" + typeID);
                    stringBuilder.append(", ");
                    stringBuilder.append("fieldID=" + fieldID);
                    stringBuilder.append(", ");
                    stringBuilder.append("object=" + object);
                    return stringBuilder.toString();
                }
            }

            public static class FieldModification extends EventsCommon {
                public static final byte ALT_ID = EventKind.FIELD_MODIFICATION;
                @Override
                public byte eventKind() {
                    return ALT_ID;
                }

                public int requestID;

                public ID.ThreadID thread;

                public JDWPLocation location;

                public byte refTypeTag;

                public ID.ReferenceTypeID typeID;

                public ID.FieldID fieldID;

                public JDWPValue object;

                public JDWPValue valueToBe;
                public FieldModification(int requestID,
                    ID.ThreadID thread,
                    JDWPLocation location,
                    byte refTypeTag,
                    ID.ReferenceTypeID typeID,
                    ID.FieldID fieldID,
                    JDWPValue object,
                    JDWPValue valueToBe) {
                    this.requestID = requestID;
                    this.thread = thread;
                    this.location = location;
                    this.refTypeTag = refTypeTag;
                    this.typeID = typeID;
                    this.fieldID = fieldID;
                    this.object = object;
                    this.valueToBe = valueToBe;
                }
                public FieldModification() {
                }
                @Override
                public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                    requestID = ps.readInt();
                    thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
                    location = ps.readLocation();
                    refTypeTag = ps.readByte();
                    typeID = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
                    fieldID = ID.read(ps.getInputStream(), ID.FieldID.class);
                    object = ps.readValue();
                    valueToBe = ps.readValue();
                }
                @Override
                public void write(JDWPOutputStream ps) throws java.io.IOException {
                    ps.write(requestID);
                    thread.write(ps.getOutputStream());
                    ps.write(location);
                    ps.write(refTypeTag);
                    typeID.write(ps.getOutputStream());
                    fieldID.write(ps.getOutputStream());
                    ps.write(object);
                    ps.write(valueToBe);
                }
                @Override
                public String toString() {
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("requestID=" + requestID);
                    stringBuilder.append(", ");
                    stringBuilder.append("thread=" + thread);
                    stringBuilder.append(", ");
                    stringBuilder.append("location=" + location);
                    stringBuilder.append(", ");
                    stringBuilder.append("refTypeTag=" + refTypeTag);
                    stringBuilder.append(", ");
                    stringBuilder.append("typeID=" + typeID);
                    stringBuilder.append(", ");
                    stringBuilder.append("fieldID=" + fieldID);
                    stringBuilder.append(", ");
                    stringBuilder.append("object=" + object);
                    stringBuilder.append(", ");
                    stringBuilder.append("valueToBe=" + valueToBe);
                    return stringBuilder.toString();
                }
            }

            public static class VMDeath extends EventsCommon {
                public static final byte ALT_ID = EventKind.VM_DEATH;
                @Override
                public byte eventKind() {
                    return ALT_ID;
                }

                public int requestID;
                public VMDeath(int requestID) {
                    this.requestID = requestID;
                }
                public VMDeath() {
                }
                @Override
                public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                    requestID = ps.readInt();
                }
                @Override
                public void write(JDWPOutputStream ps) throws java.io.IOException {
                    ps.write(requestID);
                }
                @Override
                public String toString() {
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("requestID=" + requestID);
                    return stringBuilder.toString();
                }
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public byte suspendPolicy;

            public Events[] events;
            public Reply(byte suspendPolicy,
                Events[] events) {
                this.suspendPolicy = suspendPolicy;
                this.events = events;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                suspendPolicy = ps.readByte();
                final int eventsCount = ps.readInt();
                events = new Events[eventsCount];
                for (int i = 0; i < eventsCount; i++) {
                    events[i] = new Events();
                    events[i].read(ps);
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(suspendPolicy);
                ps.write(events.length);
                for (int i = 0; i < events.length; i++) {
                    events[i].write(ps);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("suspendPolicy=" + suspendPolicy);
                stringBuilder.append(", ");
                stringBuilder.append("events=[" + events.length + "]{");
                for (int i = 0; i < events.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("events[i]=" + events[i]);
                }
                stringBuilder.append("}");
                return stringBuilder.toString();
            }
        }
    }
}

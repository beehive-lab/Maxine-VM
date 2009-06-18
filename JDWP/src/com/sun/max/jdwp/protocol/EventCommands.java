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

            public byte _eventKind;
            public EventsCommon _aEventsCommon;

            public Events(byte eventKind, EventsCommon aEventsCommon) {
                this._eventKind = eventKind;
                this._aEventsCommon = aEventsCommon;
            }

            public Events() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _eventKind = ps.readByte();
                switch (_eventKind) {
                    case EventKind.VM_START:
                        _aEventsCommon = new VMStart();
                        break;
                    case EventKind.SINGLE_STEP:
                        _aEventsCommon = new SingleStep();
                        break;
                    case EventKind.BREAKPOINT:
                        _aEventsCommon = new Breakpoint();
                        break;
                    case EventKind.METHOD_ENTRY:
                        _aEventsCommon = new MethodEntry();
                        break;
                    case EventKind.METHOD_EXIT:
                        _aEventsCommon = new MethodExit();
                        break;
                    case EventKind.METHOD_EXIT_WITH_RETURN_VALUE:
                        _aEventsCommon = new MethodExitWithReturnValue();
                        break;
                    case EventKind.MONITOR_CONTENDED_ENTER:
                        _aEventsCommon = new MonitorContendedEnter();
                        break;
                    case EventKind.MONITOR_CONTENDED_ENTERED:
                        _aEventsCommon = new MonitorContendedEntered();
                        break;
                    case EventKind.MONITOR_WAIT:
                        _aEventsCommon = new MonitorWait();
                        break;
                    case EventKind.MONITOR_WAITED:
                        _aEventsCommon = new MonitorWaited();
                        break;
                    case EventKind.EXCEPTION:
                        _aEventsCommon = new Exception();
                        break;
                    case EventKind.THREAD_START:
                        _aEventsCommon = new ThreadStart();
                        break;
                    case EventKind.THREAD_DEATH:
                        _aEventsCommon = new ThreadDeath();
                        break;
                    case EventKind.CLASS_PREPARE:
                        _aEventsCommon = new ClassPrepare();
                        break;
                    case EventKind.CLASS_UNLOAD:
                        _aEventsCommon = new ClassUnload();
                        break;
                    case EventKind.FIELD_ACCESS:
                        _aEventsCommon = new FieldAccess();
                        break;
                    case EventKind.FIELD_MODIFICATION:
                        _aEventsCommon = new FieldModification();
                        break;
                    case EventKind.VM_DEATH:
                        _aEventsCommon = new VMDeath();
                        break;
                }
                _aEventsCommon.read(ps);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_eventKind);
                _aEventsCommon.write(ps);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_eventKind=" + _eventKind);
                stringBuilder.append(_aEventsCommon);
                return stringBuilder.toString();
            }

            public static class VMStart extends EventsCommon {
                public static final byte ALT_ID = EventKind.VM_START;
                @Override
                public byte eventKind() {
                    return ALT_ID;
                }

                public int _requestID;

                public ID.ThreadID _thread;
                public VMStart(int requestID,
                    ID.ThreadID thread) {
                    this._requestID = requestID;
                    this._thread = thread;
                }
                public VMStart() {
                }
                @Override
                public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                    _requestID = ps.readInt();
                    _thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
                }
                @Override
                public void write(JDWPOutputStream ps) throws java.io.IOException {
                    ps.write(_requestID);
                    _thread.write(ps.getOutputStream());
                }
                @Override
                public String toString() {
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("_requestID=" + _requestID);
                    stringBuilder.append(", ");
                    stringBuilder.append("_thread=" + _thread);
                    return stringBuilder.toString();
                }
            }

            public static class SingleStep extends EventsCommon {
                public static final byte ALT_ID = EventKind.SINGLE_STEP;
                @Override
                public byte eventKind() {
                    return ALT_ID;
                }

                public int _requestID;

                public ID.ThreadID _thread;

                public JDWPLocation _location;
                public SingleStep(int requestID,
                    ID.ThreadID thread,
                    JDWPLocation location) {
                    this._requestID = requestID;
                    this._thread = thread;
                    this._location = location;
                }
                public SingleStep() {
                }
                @Override
                public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                    _requestID = ps.readInt();
                    _thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
                    _location = ps.readLocation();
                }
                @Override
                public void write(JDWPOutputStream ps) throws java.io.IOException {
                    ps.write(_requestID);
                    _thread.write(ps.getOutputStream());
                    ps.write(_location);
                }
                @Override
                public String toString() {
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("_requestID=" + _requestID);
                    stringBuilder.append(", ");
                    stringBuilder.append("_thread=" + _thread);
                    stringBuilder.append(", ");
                    stringBuilder.append("_location=" + _location);
                    return stringBuilder.toString();
                }
            }

            public static class Breakpoint extends EventsCommon {
                public static final byte ALT_ID = EventKind.BREAKPOINT;
                @Override
                public byte eventKind() {
                    return ALT_ID;
                }

                public int _requestID;

                public ID.ThreadID _thread;

                public JDWPLocation _location;
                public Breakpoint(int requestID,
                    ID.ThreadID thread,
                    JDWPLocation location) {
                    this._requestID = requestID;
                    this._thread = thread;
                    this._location = location;
                }
                public Breakpoint() {
                }
                @Override
                public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                    _requestID = ps.readInt();
                    _thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
                    _location = ps.readLocation();
                }
                @Override
                public void write(JDWPOutputStream ps) throws java.io.IOException {
                    ps.write(_requestID);
                    _thread.write(ps.getOutputStream());
                    ps.write(_location);
                }
                @Override
                public String toString() {
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("_requestID=" + _requestID);
                    stringBuilder.append(", ");
                    stringBuilder.append("_thread=" + _thread);
                    stringBuilder.append(", ");
                    stringBuilder.append("_location=" + _location);
                    return stringBuilder.toString();
                }
            }

            public static class MethodEntry extends EventsCommon {
                public static final byte ALT_ID = EventKind.METHOD_ENTRY;
                @Override
                public byte eventKind() {
                    return ALT_ID;
                }

                public int _requestID;

                public ID.ThreadID _thread;

                public JDWPLocation _location;
                public MethodEntry(int requestID,
                    ID.ThreadID thread,
                    JDWPLocation location) {
                    this._requestID = requestID;
                    this._thread = thread;
                    this._location = location;
                }
                public MethodEntry() {
                }
                @Override
                public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                    _requestID = ps.readInt();
                    _thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
                    _location = ps.readLocation();
                }
                @Override
                public void write(JDWPOutputStream ps) throws java.io.IOException {
                    ps.write(_requestID);
                    _thread.write(ps.getOutputStream());
                    ps.write(_location);
                }
                @Override
                public String toString() {
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("_requestID=" + _requestID);
                    stringBuilder.append(", ");
                    stringBuilder.append("_thread=" + _thread);
                    stringBuilder.append(", ");
                    stringBuilder.append("_location=" + _location);
                    return stringBuilder.toString();
                }
            }

            public static class MethodExit extends EventsCommon {
                public static final byte ALT_ID = EventKind.METHOD_EXIT;
                @Override
                public byte eventKind() {
                    return ALT_ID;
                }

                public int _requestID;

                public ID.ThreadID _thread;

                public JDWPLocation _location;
                public MethodExit(int requestID,
                    ID.ThreadID thread,
                    JDWPLocation location) {
                    this._requestID = requestID;
                    this._thread = thread;
                    this._location = location;
                }
                public MethodExit() {
                }
                @Override
                public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                    _requestID = ps.readInt();
                    _thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
                    _location = ps.readLocation();
                }
                @Override
                public void write(JDWPOutputStream ps) throws java.io.IOException {
                    ps.write(_requestID);
                    _thread.write(ps.getOutputStream());
                    ps.write(_location);
                }
                @Override
                public String toString() {
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("_requestID=" + _requestID);
                    stringBuilder.append(", ");
                    stringBuilder.append("_thread=" + _thread);
                    stringBuilder.append(", ");
                    stringBuilder.append("_location=" + _location);
                    return stringBuilder.toString();
                }
            }

            public static class MethodExitWithReturnValue extends EventsCommon {
                public static final byte ALT_ID = EventKind.METHOD_EXIT_WITH_RETURN_VALUE;
                @Override
                public byte eventKind() {
                    return ALT_ID;
                }

                public int _requestID;

                public ID.ThreadID _thread;

                public JDWPLocation _location;

                public JDWPValue _value;
                public MethodExitWithReturnValue(int requestID,
                    ID.ThreadID thread,
                    JDWPLocation location,
                    JDWPValue value) {
                    this._requestID = requestID;
                    this._thread = thread;
                    this._location = location;
                    this._value = value;
                }
                public MethodExitWithReturnValue() {
                }
                @Override
                public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                    _requestID = ps.readInt();
                    _thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
                    _location = ps.readLocation();
                    _value = ps.readValue();
                }
                @Override
                public void write(JDWPOutputStream ps) throws java.io.IOException {
                    ps.write(_requestID);
                    _thread.write(ps.getOutputStream());
                    ps.write(_location);
                    ps.write(_value);
                }
                @Override
                public String toString() {
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("_requestID=" + _requestID);
                    stringBuilder.append(", ");
                    stringBuilder.append("_thread=" + _thread);
                    stringBuilder.append(", ");
                    stringBuilder.append("_location=" + _location);
                    stringBuilder.append(", ");
                    stringBuilder.append("_value=" + _value);
                    return stringBuilder.toString();
                }
            }

            public static class MonitorContendedEnter extends EventsCommon {
                public static final byte ALT_ID = EventKind.MONITOR_CONTENDED_ENTER;
                @Override
                public byte eventKind() {
                    return ALT_ID;
                }

                public int _requestID;

                public ID.ThreadID _thread;

                public JDWPValue _object;

                public JDWPLocation _location;
                public MonitorContendedEnter(int requestID,
                    ID.ThreadID thread,
                    JDWPValue object,
                    JDWPLocation location) {
                    this._requestID = requestID;
                    this._thread = thread;
                    this._object = object;
                    this._location = location;
                }
                public MonitorContendedEnter() {
                }
                @Override
                public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                    _requestID = ps.readInt();
                    _thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
                    _object = ps.readValue();
                    _location = ps.readLocation();
                }
                @Override
                public void write(JDWPOutputStream ps) throws java.io.IOException {
                    ps.write(_requestID);
                    _thread.write(ps.getOutputStream());
                    ps.write(_object);
                    ps.write(_location);
                }
                @Override
                public String toString() {
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("_requestID=" + _requestID);
                    stringBuilder.append(", ");
                    stringBuilder.append("_thread=" + _thread);
                    stringBuilder.append(", ");
                    stringBuilder.append("_object=" + _object);
                    stringBuilder.append(", ");
                    stringBuilder.append("_location=" + _location);
                    return stringBuilder.toString();
                }
            }

            public static class MonitorContendedEntered extends EventsCommon {
                public static final byte ALT_ID = EventKind.MONITOR_CONTENDED_ENTERED;
                @Override
                public byte eventKind() {
                    return ALT_ID;
                }

                public int _requestID;

                public ID.ThreadID _thread;

                public JDWPValue _object;

                public JDWPLocation _location;
                public MonitorContendedEntered(int requestID,
                    ID.ThreadID thread,
                    JDWPValue object,
                    JDWPLocation location) {
                    this._requestID = requestID;
                    this._thread = thread;
                    this._object = object;
                    this._location = location;
                }
                public MonitorContendedEntered() {
                }
                @Override
                public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                    _requestID = ps.readInt();
                    _thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
                    _object = ps.readValue();
                    _location = ps.readLocation();
                }
                @Override
                public void write(JDWPOutputStream ps) throws java.io.IOException {
                    ps.write(_requestID);
                    _thread.write(ps.getOutputStream());
                    ps.write(_object);
                    ps.write(_location);
                }
                @Override
                public String toString() {
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("_requestID=" + _requestID);
                    stringBuilder.append(", ");
                    stringBuilder.append("_thread=" + _thread);
                    stringBuilder.append(", ");
                    stringBuilder.append("_object=" + _object);
                    stringBuilder.append(", ");
                    stringBuilder.append("_location=" + _location);
                    return stringBuilder.toString();
                }
            }

            public static class MonitorWait extends EventsCommon {
                public static final byte ALT_ID = EventKind.MONITOR_WAIT;
                @Override
                public byte eventKind() {
                    return ALT_ID;
                }

                public int _requestID;

                public ID.ThreadID _thread;

                public JDWPValue _object;

                public JDWPLocation _location;

                public long _timeout;
                public MonitorWait(int requestID,
                    ID.ThreadID thread,
                    JDWPValue object,
                    JDWPLocation location,
                    long timeout) {
                    this._requestID = requestID;
                    this._thread = thread;
                    this._object = object;
                    this._location = location;
                    this._timeout = timeout;
                }
                public MonitorWait() {
                }
                @Override
                public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                    _requestID = ps.readInt();
                    _thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
                    _object = ps.readValue();
                    _location = ps.readLocation();
                    _timeout = ps.readLong();
                }
                @Override
                public void write(JDWPOutputStream ps) throws java.io.IOException {
                    ps.write(_requestID);
                    _thread.write(ps.getOutputStream());
                    ps.write(_object);
                    ps.write(_location);
                    ps.write(_timeout);
                }
                @Override
                public String toString() {
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("_requestID=" + _requestID);
                    stringBuilder.append(", ");
                    stringBuilder.append("_thread=" + _thread);
                    stringBuilder.append(", ");
                    stringBuilder.append("_object=" + _object);
                    stringBuilder.append(", ");
                    stringBuilder.append("_location=" + _location);
                    stringBuilder.append(", ");
                    stringBuilder.append("_timeout=" + _timeout);
                    return stringBuilder.toString();
                }
            }

            public static class MonitorWaited extends EventsCommon {
                public static final byte ALT_ID = EventKind.MONITOR_WAITED;
                @Override
                public byte eventKind() {
                    return ALT_ID;
                }

                public int _requestID;

                public ID.ThreadID _thread;

                public JDWPValue _object;

                public JDWPLocation _location;

                public boolean _timed_out;
                public MonitorWaited(int requestID,
                    ID.ThreadID thread,
                    JDWPValue object,
                    JDWPLocation location,
                    boolean timed_out) {
                    this._requestID = requestID;
                    this._thread = thread;
                    this._object = object;
                    this._location = location;
                    this._timed_out = timed_out;
                }
                public MonitorWaited() {
                }
                @Override
                public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                    _requestID = ps.readInt();
                    _thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
                    _object = ps.readValue();
                    _location = ps.readLocation();
                    _timed_out = ps.readBoolean();
                }
                @Override
                public void write(JDWPOutputStream ps) throws java.io.IOException {
                    ps.write(_requestID);
                    _thread.write(ps.getOutputStream());
                    ps.write(_object);
                    ps.write(_location);
                    ps.write(_timed_out);
                }
                @Override
                public String toString() {
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("_requestID=" + _requestID);
                    stringBuilder.append(", ");
                    stringBuilder.append("_thread=" + _thread);
                    stringBuilder.append(", ");
                    stringBuilder.append("_object=" + _object);
                    stringBuilder.append(", ");
                    stringBuilder.append("_location=" + _location);
                    stringBuilder.append(", ");
                    stringBuilder.append("_timed_out=" + _timed_out);
                    return stringBuilder.toString();
                }
            }

            public static class Exception extends EventsCommon {
                public static final byte ALT_ID = EventKind.EXCEPTION;
                @Override
                public byte eventKind() {
                    return ALT_ID;
                }

                public int _requestID;

                public ID.ThreadID _thread;

                public JDWPLocation _location;

                public JDWPValue _exception;

                public JDWPLocation _catchLocation;
                public Exception(int requestID,
                    ID.ThreadID thread,
                    JDWPLocation location,
                    JDWPValue exception,
                    JDWPLocation catchLocation) {
                    this._requestID = requestID;
                    this._thread = thread;
                    this._location = location;
                    this._exception = exception;
                    this._catchLocation = catchLocation;
                }
                public Exception() {
                }
                @Override
                public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                    _requestID = ps.readInt();
                    _thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
                    _location = ps.readLocation();
                    _exception = ps.readValue();
                    _catchLocation = ps.readLocation();
                }
                @Override
                public void write(JDWPOutputStream ps) throws java.io.IOException {
                    ps.write(_requestID);
                    _thread.write(ps.getOutputStream());
                    ps.write(_location);
                    ps.write(_exception);
                    ps.write(_catchLocation);
                }
                @Override
                public String toString() {
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("_requestID=" + _requestID);
                    stringBuilder.append(", ");
                    stringBuilder.append("_thread=" + _thread);
                    stringBuilder.append(", ");
                    stringBuilder.append("_location=" + _location);
                    stringBuilder.append(", ");
                    stringBuilder.append("_exception=" + _exception);
                    stringBuilder.append(", ");
                    stringBuilder.append("_catchLocation=" + _catchLocation);
                    return stringBuilder.toString();
                }
            }

            public static class ThreadStart extends EventsCommon {
                public static final byte ALT_ID = EventKind.THREAD_START;
                @Override
                public byte eventKind() {
                    return ALT_ID;
                }

                public int _requestID;

                public ID.ThreadID _thread;
                public ThreadStart(int requestID,
                    ID.ThreadID thread) {
                    this._requestID = requestID;
                    this._thread = thread;
                }
                public ThreadStart() {
                }
                @Override
                public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                    _requestID = ps.readInt();
                    _thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
                }
                @Override
                public void write(JDWPOutputStream ps) throws java.io.IOException {
                    ps.write(_requestID);
                    _thread.write(ps.getOutputStream());
                }
                @Override
                public String toString() {
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("_requestID=" + _requestID);
                    stringBuilder.append(", ");
                    stringBuilder.append("_thread=" + _thread);
                    return stringBuilder.toString();
                }
            }

            public static class ThreadDeath extends EventsCommon {
                public static final byte ALT_ID = EventKind.THREAD_DEATH;
                @Override
                public byte eventKind() {
                    return ALT_ID;
                }

                public int _requestID;

                public ID.ThreadID _thread;
                public ThreadDeath(int requestID,
                    ID.ThreadID thread) {
                    this._requestID = requestID;
                    this._thread = thread;
                }
                public ThreadDeath() {
                }
                @Override
                public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                    _requestID = ps.readInt();
                    _thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
                }
                @Override
                public void write(JDWPOutputStream ps) throws java.io.IOException {
                    ps.write(_requestID);
                    _thread.write(ps.getOutputStream());
                }
                @Override
                public String toString() {
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("_requestID=" + _requestID);
                    stringBuilder.append(", ");
                    stringBuilder.append("_thread=" + _thread);
                    return stringBuilder.toString();
                }
            }

            public static class ClassPrepare extends EventsCommon {
                public static final byte ALT_ID = EventKind.CLASS_PREPARE;
                @Override
                public byte eventKind() {
                    return ALT_ID;
                }

                public int _requestID;

                public ID.ThreadID _thread;

                public byte _refTypeTag;

                public ID.ReferenceTypeID _typeID;

                public String _signature;

                public int _status;
                public ClassPrepare(int requestID,
                    ID.ThreadID thread,
                    byte refTypeTag,
                    ID.ReferenceTypeID typeID,
                    String signature,
                    int status) {
                    this._requestID = requestID;
                    this._thread = thread;
                    this._refTypeTag = refTypeTag;
                    this._typeID = typeID;
                    this._signature = signature;
                    this._status = status;
                }
                public ClassPrepare() {
                }
                @Override
                public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                    _requestID = ps.readInt();
                    _thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
                    _refTypeTag = ps.readByte();
                    _typeID = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
                    _signature = ps.readString();
                    _status = ps.readInt();
                }
                @Override
                public void write(JDWPOutputStream ps) throws java.io.IOException {
                    ps.write(_requestID);
                    _thread.write(ps.getOutputStream());
                    ps.write(_refTypeTag);
                    _typeID.write(ps.getOutputStream());
                    ps.write(_signature);
                    ps.write(_status);
                }
                @Override
                public String toString() {
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("_requestID=" + _requestID);
                    stringBuilder.append(", ");
                    stringBuilder.append("_thread=" + _thread);
                    stringBuilder.append(", ");
                    stringBuilder.append("_refTypeTag=" + _refTypeTag);
                    stringBuilder.append(", ");
                    stringBuilder.append("_typeID=" + _typeID);
                    stringBuilder.append(", ");
                    stringBuilder.append("_signature=" + _signature);
                    stringBuilder.append(", ");
                    stringBuilder.append("_status=" + _status);
                    return stringBuilder.toString();
                }
            }

            public static class ClassUnload extends EventsCommon {
                public static final byte ALT_ID = EventKind.CLASS_UNLOAD;
                @Override
                public byte eventKind() {
                    return ALT_ID;
                }

                public int _requestID;

                public String _signature;
                public ClassUnload(int requestID,
                    String signature) {
                    this._requestID = requestID;
                    this._signature = signature;
                }
                public ClassUnload() {
                }
                @Override
                public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                    _requestID = ps.readInt();
                    _signature = ps.readString();
                }
                @Override
                public void write(JDWPOutputStream ps) throws java.io.IOException {
                    ps.write(_requestID);
                    ps.write(_signature);
                }
                @Override
                public String toString() {
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("_requestID=" + _requestID);
                    stringBuilder.append(", ");
                    stringBuilder.append("_signature=" + _signature);
                    return stringBuilder.toString();
                }
            }

            public static class FieldAccess extends EventsCommon {
                public static final byte ALT_ID = EventKind.FIELD_ACCESS;
                @Override
                public byte eventKind() {
                    return ALT_ID;
                }

                public int _requestID;

                public ID.ThreadID _thread;

                public JDWPLocation _location;

                public byte _refTypeTag;

                public ID.ReferenceTypeID _typeID;

                public ID.FieldID _fieldID;

                public JDWPValue _object;
                public FieldAccess(int requestID,
                    ID.ThreadID thread,
                    JDWPLocation location,
                    byte refTypeTag,
                    ID.ReferenceTypeID typeID,
                    ID.FieldID fieldID,
                    JDWPValue object) {
                    this._requestID = requestID;
                    this._thread = thread;
                    this._location = location;
                    this._refTypeTag = refTypeTag;
                    this._typeID = typeID;
                    this._fieldID = fieldID;
                    this._object = object;
                }
                public FieldAccess() {
                }
                @Override
                public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                    _requestID = ps.readInt();
                    _thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
                    _location = ps.readLocation();
                    _refTypeTag = ps.readByte();
                    _typeID = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
                    _fieldID = ID.read(ps.getInputStream(), ID.FieldID.class);
                    _object = ps.readValue();
                }
                @Override
                public void write(JDWPOutputStream ps) throws java.io.IOException {
                    ps.write(_requestID);
                    _thread.write(ps.getOutputStream());
                    ps.write(_location);
                    ps.write(_refTypeTag);
                    _typeID.write(ps.getOutputStream());
                    _fieldID.write(ps.getOutputStream());
                    ps.write(_object);
                }
                @Override
                public String toString() {
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("_requestID=" + _requestID);
                    stringBuilder.append(", ");
                    stringBuilder.append("_thread=" + _thread);
                    stringBuilder.append(", ");
                    stringBuilder.append("_location=" + _location);
                    stringBuilder.append(", ");
                    stringBuilder.append("_refTypeTag=" + _refTypeTag);
                    stringBuilder.append(", ");
                    stringBuilder.append("_typeID=" + _typeID);
                    stringBuilder.append(", ");
                    stringBuilder.append("_fieldID=" + _fieldID);
                    stringBuilder.append(", ");
                    stringBuilder.append("_object=" + _object);
                    return stringBuilder.toString();
                }
            }

            public static class FieldModification extends EventsCommon {
                public static final byte ALT_ID = EventKind.FIELD_MODIFICATION;
                @Override
                public byte eventKind() {
                    return ALT_ID;
                }

                public int _requestID;

                public ID.ThreadID _thread;

                public JDWPLocation _location;

                public byte _refTypeTag;

                public ID.ReferenceTypeID _typeID;

                public ID.FieldID _fieldID;

                public JDWPValue _object;

                public JDWPValue _valueToBe;
                public FieldModification(int requestID,
                    ID.ThreadID thread,
                    JDWPLocation location,
                    byte refTypeTag,
                    ID.ReferenceTypeID typeID,
                    ID.FieldID fieldID,
                    JDWPValue object,
                    JDWPValue valueToBe) {
                    this._requestID = requestID;
                    this._thread = thread;
                    this._location = location;
                    this._refTypeTag = refTypeTag;
                    this._typeID = typeID;
                    this._fieldID = fieldID;
                    this._object = object;
                    this._valueToBe = valueToBe;
                }
                public FieldModification() {
                }
                @Override
                public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                    _requestID = ps.readInt();
                    _thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
                    _location = ps.readLocation();
                    _refTypeTag = ps.readByte();
                    _typeID = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
                    _fieldID = ID.read(ps.getInputStream(), ID.FieldID.class);
                    _object = ps.readValue();
                    _valueToBe = ps.readValue();
                }
                @Override
                public void write(JDWPOutputStream ps) throws java.io.IOException {
                    ps.write(_requestID);
                    _thread.write(ps.getOutputStream());
                    ps.write(_location);
                    ps.write(_refTypeTag);
                    _typeID.write(ps.getOutputStream());
                    _fieldID.write(ps.getOutputStream());
                    ps.write(_object);
                    ps.write(_valueToBe);
                }
                @Override
                public String toString() {
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("_requestID=" + _requestID);
                    stringBuilder.append(", ");
                    stringBuilder.append("_thread=" + _thread);
                    stringBuilder.append(", ");
                    stringBuilder.append("_location=" + _location);
                    stringBuilder.append(", ");
                    stringBuilder.append("_refTypeTag=" + _refTypeTag);
                    stringBuilder.append(", ");
                    stringBuilder.append("_typeID=" + _typeID);
                    stringBuilder.append(", ");
                    stringBuilder.append("_fieldID=" + _fieldID);
                    stringBuilder.append(", ");
                    stringBuilder.append("_object=" + _object);
                    stringBuilder.append(", ");
                    stringBuilder.append("_valueToBe=" + _valueToBe);
                    return stringBuilder.toString();
                }
            }

            public static class VMDeath extends EventsCommon {
                public static final byte ALT_ID = EventKind.VM_DEATH;
                @Override
                public byte eventKind() {
                    return ALT_ID;
                }

                public int _requestID;
                public VMDeath(int requestID) {
                    this._requestID = requestID;
                }
                public VMDeath() {
                }
                @Override
                public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                    _requestID = ps.readInt();
                }
                @Override
                public void write(JDWPOutputStream ps) throws java.io.IOException {
                    ps.write(_requestID);
                }
                @Override
                public String toString() {
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("_requestID=" + _requestID);
                    return stringBuilder.toString();
                }
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public byte _suspendPolicy;

            public Events[] _events;
            public Reply(byte suspendPolicy,
                Events[] events) {
                this._suspendPolicy = suspendPolicy;
                this._events = events;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _suspendPolicy = ps.readByte();
                final int eventsCount = ps.readInt();
                _events = new Events[eventsCount];
                for (int i = 0; i < eventsCount; i++) {
                    _events[i] = new Events();
                    _events[i].read(ps);
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_suspendPolicy);
                ps.write(_events.length);
                for (int i = 0; i < _events.length; i++) {
                    _events[i].write(ps);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_suspendPolicy=" + _suspendPolicy);
                stringBuilder.append(", ");
                stringBuilder.append("_events=[" + _events.length + "]{");
                for (int i = 0; i < _events.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("_events[i]=" + _events[i]);
                }
                stringBuilder.append("}");
                return stringBuilder.toString();
            }
        }
    }
}

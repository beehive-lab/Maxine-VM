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
@SuppressWarnings("unused")

public final class ThreadReferenceCommands {
    public static final int COMMAND_SET = 11;
    private ThreadReferenceCommands() { }  // hide constructor

    public static class Name {
        public static final byte COMMAND = 1;
        public abstract static class Handler implements CommandHandler<IncomingRequest, Reply> {
            public IncomingRequest createIncomingDataObject() { return new IncomingRequest(); }
            public int helpAtDecodingUntaggedValue(IncomingRequest incomingRequest) throws JDWPException {assert false : "If this method can be called, it must be overwritten in subclasses!"; return 0; }
            public Reply handle(IncomingRequest incomingRequest, JDWPSender replyChannel) throws JDWPException { return handle(incomingRequest); }
            public Reply handle(IncomingRequest incomingRequest) throws JDWPException { throw new JDWPNotImplementedException(); }
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }
        }

        public static class IncomingRequest implements IncomingData {
            public ID.ThreadID _thread;
            public IncomingRequest(ID.ThreadID thread) {
                this._thread = thread;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _thread.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_thread=" + _thread);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public String _threadName;
            public Reply(String threadName) {
                this._threadName = threadName;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _threadName = ps.readString();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_threadName);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_threadName=" + _threadName);
                return stringBuilder.toString();
            }
        }
    }

    public static class Suspend {
        public static final byte COMMAND = 2;
        public abstract static class Handler implements CommandHandler<IncomingRequest, Reply> {
            public IncomingRequest createIncomingDataObject() { return new IncomingRequest(); }
            public int helpAtDecodingUntaggedValue(IncomingRequest incomingRequest) throws JDWPException {assert false : "If this method can be called, it must be overwritten in subclasses!"; return 0; }
            public Reply handle(IncomingRequest incomingRequest, JDWPSender replyChannel) throws JDWPException { return handle(incomingRequest); }
            public Reply handle(IncomingRequest incomingRequest) throws JDWPException { throw new JDWPNotImplementedException(); }
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }
        }

        public static class IncomingRequest implements IncomingData {
            public ID.ThreadID _thread;
            public IncomingRequest(ID.ThreadID thread) {
                this._thread = thread;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _thread.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_thread=" + _thread);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                return stringBuilder.toString();
            }
        }
    }

    public static class Resume {
        public static final byte COMMAND = 3;
        public abstract static class Handler implements CommandHandler<IncomingRequest, Reply> {
            public IncomingRequest createIncomingDataObject() { return new IncomingRequest(); }
            public int helpAtDecodingUntaggedValue(IncomingRequest incomingRequest) throws JDWPException {assert false : "If this method can be called, it must be overwritten in subclasses!"; return 0; }
            public Reply handle(IncomingRequest incomingRequest, JDWPSender replyChannel) throws JDWPException { return handle(incomingRequest); }
            public Reply handle(IncomingRequest incomingRequest) throws JDWPException { throw new JDWPNotImplementedException(); }
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }
        }

        public static class IncomingRequest implements IncomingData {
            public ID.ThreadID _thread;
            public IncomingRequest(ID.ThreadID thread) {
                this._thread = thread;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _thread.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_thread=" + _thread);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                return stringBuilder.toString();
            }
        }
    }

    public static class Status {
        public static final byte COMMAND = 4;
        public abstract static class Handler implements CommandHandler<IncomingRequest, Reply> {
            public IncomingRequest createIncomingDataObject() { return new IncomingRequest(); }
            public int helpAtDecodingUntaggedValue(IncomingRequest incomingRequest) throws JDWPException {assert false : "If this method can be called, it must be overwritten in subclasses!"; return 0; }
            public Reply handle(IncomingRequest incomingRequest, JDWPSender replyChannel) throws JDWPException { return handle(incomingRequest); }
            public Reply handle(IncomingRequest incomingRequest) throws JDWPException { throw new JDWPNotImplementedException(); }
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }
        }

        public static class IncomingRequest implements IncomingData {
            public ID.ThreadID _thread;
            public IncomingRequest(ID.ThreadID thread) {
                this._thread = thread;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _thread.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_thread=" + _thread);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public int _threadStatus;

            public int _suspendStatus;
            public Reply(int threadStatus,
                int suspendStatus) {
                this._threadStatus = threadStatus;
                this._suspendStatus = suspendStatus;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _threadStatus = ps.readInt();
                _suspendStatus = ps.readInt();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_threadStatus);
                ps.write(_suspendStatus);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_threadStatus=" + _threadStatus);
                stringBuilder.append(", ");
                stringBuilder.append("_suspendStatus=" + _suspendStatus);
                return stringBuilder.toString();
            }
        }
    }

    public static class ThreadGroup {
        public static final byte COMMAND = 5;
        public abstract static class Handler implements CommandHandler<IncomingRequest, Reply> {
            public IncomingRequest createIncomingDataObject() { return new IncomingRequest(); }
            public int helpAtDecodingUntaggedValue(IncomingRequest incomingRequest) throws JDWPException {assert false : "If this method can be called, it must be overwritten in subclasses!"; return 0; }
            public Reply handle(IncomingRequest incomingRequest, JDWPSender replyChannel) throws JDWPException { return handle(incomingRequest); }
            public Reply handle(IncomingRequest incomingRequest) throws JDWPException { throw new JDWPNotImplementedException(); }
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }
        }

        public static class IncomingRequest implements IncomingData {
            public ID.ThreadID _thread;
            public IncomingRequest(ID.ThreadID thread) {
                this._thread = thread;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _thread.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_thread=" + _thread);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public ID.ThreadGroupID _group;
            public Reply(ID.ThreadGroupID group) {
                this._group = group;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _group = ID.read(ps.getInputStream(), ID.ThreadGroupID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _group.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_group=" + _group);
                return stringBuilder.toString();
            }
        }
    }

    public static class Frames {
        public static final byte COMMAND = 6;
        public abstract static class Handler implements CommandHandler<IncomingRequest, Reply> {
            public IncomingRequest createIncomingDataObject() { return new IncomingRequest(); }
            public int helpAtDecodingUntaggedValue(IncomingRequest incomingRequest) throws JDWPException {assert false : "If this method can be called, it must be overwritten in subclasses!"; return 0; }
            public Reply handle(IncomingRequest incomingRequest, JDWPSender replyChannel) throws JDWPException { return handle(incomingRequest); }
            public Reply handle(IncomingRequest incomingRequest) throws JDWPException { throw new JDWPNotImplementedException(); }
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }
        }

        public static class IncomingRequest implements IncomingData {
            public ID.ThreadID _thread;

            public int _startFrame;

            public int _length;
            public IncomingRequest(ID.ThreadID thread,
                int startFrame,
                int length) {
                this._thread = thread;
                this._startFrame = startFrame;
                this._length = length;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
                _startFrame = ps.readInt();
                _length = ps.readInt();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _thread.write(ps.getOutputStream());
                ps.write(_startFrame);
                ps.write(_length);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_thread=" + _thread);
                stringBuilder.append(", ");
                stringBuilder.append("_startFrame=" + _startFrame);
                stringBuilder.append(", ");
                stringBuilder.append("_length=" + _length);
                return stringBuilder.toString();
            }
        }

        public static class Frame {

            public ID.FrameID _frameID;

            public JDWPLocation _location;
            public Frame(ID.FrameID frameID,
                JDWPLocation location) {
                this._frameID = frameID;
                this._location = location;
            }
            public Frame() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _frameID = ID.read(ps.getInputStream(), ID.FrameID.class);
                _location = ps.readLocation();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _frameID.write(ps.getOutputStream());
                ps.write(_location);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_frameID=" + _frameID);
                stringBuilder.append(", ");
                stringBuilder.append("_location=" + _location);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public Frame[] _frames;
            public Reply(Frame[] frames) {
                this._frames = frames;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                final int framesCount = ps.readInt();
                _frames = new Frame[framesCount];
                for (int i = 0; i < framesCount; i++) {
                    _frames[i] = new Frame();
                    _frames[i].read(ps);
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_frames.length);
                for (int i = 0; i < _frames.length; i++) {
                    _frames[i].write(ps);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_frames=[" + _frames.length + "]{");
                for (int i = 0; i < _frames.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("_frames[i]=" + _frames[i]);
                }
                stringBuilder.append("}");
                return stringBuilder.toString();
            }
        }
    }

    public static class FrameCount {
        public static final byte COMMAND = 7;
        public abstract static class Handler implements CommandHandler<IncomingRequest, Reply> {
            public IncomingRequest createIncomingDataObject() { return new IncomingRequest(); }
            public int helpAtDecodingUntaggedValue(IncomingRequest incomingRequest) throws JDWPException {assert false : "If this method can be called, it must be overwritten in subclasses!"; return 0; }
            public Reply handle(IncomingRequest incomingRequest, JDWPSender replyChannel) throws JDWPException { return handle(incomingRequest); }
            public Reply handle(IncomingRequest incomingRequest) throws JDWPException { throw new JDWPNotImplementedException(); }
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }
        }

        public static class IncomingRequest implements IncomingData {
            public ID.ThreadID _thread;
            public IncomingRequest(ID.ThreadID thread) {
                this._thread = thread;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _thread.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_thread=" + _thread);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public int _frameCount;
            public Reply(int frameCount) {
                this._frameCount = frameCount;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _frameCount = ps.readInt();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_frameCount);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_frameCount=" + _frameCount);
                return stringBuilder.toString();
            }
        }
    }

    public static class OwnedMonitors {
        public static final byte COMMAND = 8;
        public abstract static class Handler implements CommandHandler<IncomingRequest, Reply> {
            public IncomingRequest createIncomingDataObject() { return new IncomingRequest(); }
            public int helpAtDecodingUntaggedValue(IncomingRequest incomingRequest) throws JDWPException {assert false : "If this method can be called, it must be overwritten in subclasses!"; return 0; }
            public Reply handle(IncomingRequest incomingRequest, JDWPSender replyChannel) throws JDWPException { return handle(incomingRequest); }
            public Reply handle(IncomingRequest incomingRequest) throws JDWPException { throw new JDWPNotImplementedException(); }
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }
        }

        public static class IncomingRequest implements IncomingData {
            public ID.ThreadID _thread;
            public IncomingRequest(ID.ThreadID thread) {
                this._thread = thread;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _thread.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_thread=" + _thread);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public JDWPValue[] _owned;
            public Reply(JDWPValue[] owned) {
                this._owned = owned;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                final int ownedCount = ps.readInt();
                _owned = new JDWPValue[ownedCount];
                for (int i = 0; i < ownedCount; i++) {
                    _owned[i] = ps.readValue();
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_owned.length);
                for (int i = 0; i < _owned.length; i++) {
                    ps.write(_owned[i]);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_owned=[" + _owned.length + "]{");
                for (int i = 0; i < _owned.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("_owned[i]=" + _owned[i]);
                }
                stringBuilder.append("}");
                return stringBuilder.toString();
            }
        }
    }

    public static class CurrentContendedMonitor {
        public static final byte COMMAND = 9;
        public abstract static class Handler implements CommandHandler<IncomingRequest, Reply> {
            public IncomingRequest createIncomingDataObject() { return new IncomingRequest(); }
            public int helpAtDecodingUntaggedValue(IncomingRequest incomingRequest) throws JDWPException {assert false : "If this method can be called, it must be overwritten in subclasses!"; return 0; }
            public Reply handle(IncomingRequest incomingRequest, JDWPSender replyChannel) throws JDWPException { return handle(incomingRequest); }
            public Reply handle(IncomingRequest incomingRequest) throws JDWPException { throw new JDWPNotImplementedException(); }
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }
        }

        public static class IncomingRequest implements IncomingData {
            public ID.ThreadID _thread;
            public IncomingRequest(ID.ThreadID thread) {
                this._thread = thread;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _thread.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_thread=" + _thread);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public JDWPValue _monitor;
            public Reply(JDWPValue monitor) {
                this._monitor = monitor;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _monitor = ps.readValue();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_monitor);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_monitor=" + _monitor);
                return stringBuilder.toString();
            }
        }
    }

    public static class Stop {
        public static final byte COMMAND = 10;
        public abstract static class Handler implements CommandHandler<IncomingRequest, Reply> {
            public IncomingRequest createIncomingDataObject() { return new IncomingRequest(); }
            public int helpAtDecodingUntaggedValue(IncomingRequest incomingRequest) throws JDWPException {assert false : "If this method can be called, it must be overwritten in subclasses!"; return 0; }
            public Reply handle(IncomingRequest incomingRequest, JDWPSender replyChannel) throws JDWPException { return handle(incomingRequest); }
            public Reply handle(IncomingRequest incomingRequest) throws JDWPException { throw new JDWPNotImplementedException(); }
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }
        }

        public static class IncomingRequest implements IncomingData {
            public ID.ThreadID _thread;

            public ID.ObjectID _throwable;
            public IncomingRequest(ID.ThreadID thread,
                ID.ObjectID throwable) {
                this._thread = thread;
                this._throwable = throwable;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
                _throwable = ID.read(ps.getInputStream(), ID.ObjectID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _thread.write(ps.getOutputStream());
                _throwable.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_thread=" + _thread);
                stringBuilder.append(", ");
                stringBuilder.append("_throwable=" + _throwable);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                return stringBuilder.toString();
            }
        }
    }

    public static class Interrupt {
        public static final byte COMMAND = 11;
        public abstract static class Handler implements CommandHandler<IncomingRequest, Reply> {
            public IncomingRequest createIncomingDataObject() { return new IncomingRequest(); }
            public int helpAtDecodingUntaggedValue(IncomingRequest incomingRequest) throws JDWPException {assert false : "If this method can be called, it must be overwritten in subclasses!"; return 0; }
            public Reply handle(IncomingRequest incomingRequest, JDWPSender replyChannel) throws JDWPException { return handle(incomingRequest); }
            public Reply handle(IncomingRequest incomingRequest) throws JDWPException { throw new JDWPNotImplementedException(); }
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }
        }

        public static class IncomingRequest implements IncomingData {
            public ID.ThreadID _thread;
            public IncomingRequest(ID.ThreadID thread) {
                this._thread = thread;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _thread.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_thread=" + _thread);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                return stringBuilder.toString();
            }
        }
    }

    public static class SuspendCount {
        public static final byte COMMAND = 12;
        public abstract static class Handler implements CommandHandler<IncomingRequest, Reply> {
            public IncomingRequest createIncomingDataObject() { return new IncomingRequest(); }
            public int helpAtDecodingUntaggedValue(IncomingRequest incomingRequest) throws JDWPException {assert false : "If this method can be called, it must be overwritten in subclasses!"; return 0; }
            public Reply handle(IncomingRequest incomingRequest, JDWPSender replyChannel) throws JDWPException { return handle(incomingRequest); }
            public Reply handle(IncomingRequest incomingRequest) throws JDWPException { throw new JDWPNotImplementedException(); }
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }
        }

        public static class IncomingRequest implements IncomingData {
            public ID.ThreadID _thread;
            public IncomingRequest(ID.ThreadID thread) {
                this._thread = thread;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _thread.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_thread=" + _thread);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public int _suspendCount;
            public Reply(int suspendCount) {
                this._suspendCount = suspendCount;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _suspendCount = ps.readInt();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_suspendCount);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_suspendCount=" + _suspendCount);
                return stringBuilder.toString();
            }
        }
    }

    public static class OwnedMonitorsStackDepthInfo {
        public static final byte COMMAND = 13;
        public abstract static class Handler implements CommandHandler<IncomingRequest, Reply> {
            public IncomingRequest createIncomingDataObject() { return new IncomingRequest(); }
            public int helpAtDecodingUntaggedValue(IncomingRequest incomingRequest) throws JDWPException {assert false : "If this method can be called, it must be overwritten in subclasses!"; return 0; }
            public Reply handle(IncomingRequest incomingRequest, JDWPSender replyChannel) throws JDWPException { return handle(incomingRequest); }
            public Reply handle(IncomingRequest incomingRequest) throws JDWPException { throw new JDWPNotImplementedException(); }
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }
        }

        public static class IncomingRequest implements IncomingData {
            public ID.ThreadID _thread;
            public IncomingRequest(ID.ThreadID thread) {
                this._thread = thread;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _thread.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_thread=" + _thread);
                return stringBuilder.toString();
            }
        }

        public static class monitor {

            public JDWPValue _monitor;

            public int _stack_depth;
            public monitor(JDWPValue monitor,
                int stack_depth) {
                this._monitor = monitor;
                this._stack_depth = stack_depth;
            }
            public monitor() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _monitor = ps.readValue();
                _stack_depth = ps.readInt();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_monitor);
                ps.write(_stack_depth);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_monitor=" + _monitor);
                stringBuilder.append(", ");
                stringBuilder.append("_stack_depth=" + _stack_depth);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public monitor[] _owned;
            public Reply(monitor[] owned) {
                this._owned = owned;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                final int ownedCount = ps.readInt();
                _owned = new monitor[ownedCount];
                for (int i = 0; i < ownedCount; i++) {
                    _owned[i] = new monitor();
                    _owned[i].read(ps);
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_owned.length);
                for (int i = 0; i < _owned.length; i++) {
                    _owned[i].write(ps);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_owned=[" + _owned.length + "]{");
                for (int i = 0; i < _owned.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("_owned[i]=" + _owned[i]);
                }
                stringBuilder.append("}");
                return stringBuilder.toString();
            }
        }
    }

    public static class ForceEarlyReturn {
        public static final byte COMMAND = 14;
        public abstract static class Handler implements CommandHandler<IncomingRequest, Reply> {
            public IncomingRequest createIncomingDataObject() { return new IncomingRequest(); }
            public int helpAtDecodingUntaggedValue(IncomingRequest incomingRequest) throws JDWPException {assert false : "If this method can be called, it must be overwritten in subclasses!"; return 0; }
            public Reply handle(IncomingRequest incomingRequest, JDWPSender replyChannel) throws JDWPException { return handle(incomingRequest); }
            public Reply handle(IncomingRequest incomingRequest) throws JDWPException { throw new JDWPNotImplementedException(); }
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }
        }

        public static class IncomingRequest implements IncomingData {
            public ID.ThreadID _thread;

            public JDWPValue _value;
            public IncomingRequest(ID.ThreadID thread,
                JDWPValue value) {
                this._thread = thread;
                this._value = value;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
                _value = ps.readValue();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _thread.write(ps.getOutputStream());
                ps.write(_value);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_thread=" + _thread);
                stringBuilder.append(", ");
                stringBuilder.append("_value=" + _value);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                return stringBuilder.toString();
            }
        }
    }
}

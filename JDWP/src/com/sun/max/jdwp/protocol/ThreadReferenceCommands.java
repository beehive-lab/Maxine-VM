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
            public ID.ThreadID thread;
            public IncomingRequest(ID.ThreadID thread) {
                this.thread = thread;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                thread.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("thread=" + thread);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public String threadName;
            public Reply(String threadName) {
                this.threadName = threadName;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                threadName = ps.readString();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(threadName);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("threadName=" + threadName);
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
            public ID.ThreadID thread;
            public IncomingRequest(ID.ThreadID thread) {
                this.thread = thread;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                thread.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("thread=" + thread);
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
            public ID.ThreadID thread;
            public IncomingRequest(ID.ThreadID thread) {
                this.thread = thread;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                thread.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("thread=" + thread);
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
            public ID.ThreadID thread;
            public IncomingRequest(ID.ThreadID thread) {
                this.thread = thread;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                thread.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("thread=" + thread);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public int threadStatus;

            public int suspendStatus;
            public Reply(int threadStatus,
                int suspendStatus) {
                this.threadStatus = threadStatus;
                this.suspendStatus = suspendStatus;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                threadStatus = ps.readInt();
                suspendStatus = ps.readInt();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(threadStatus);
                ps.write(suspendStatus);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("threadStatus=" + threadStatus);
                stringBuilder.append(", ");
                stringBuilder.append("suspendStatus=" + suspendStatus);
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
            public ID.ThreadID thread;
            public IncomingRequest(ID.ThreadID thread) {
                this.thread = thread;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                thread.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("thread=" + thread);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public ID.ThreadGroupID group;
            public Reply(ID.ThreadGroupID group) {
                this.group = group;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                group = ID.read(ps.getInputStream(), ID.ThreadGroupID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                group.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("group=" + group);
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
            public ID.ThreadID thread;

            public int startFrame;

            public int length;
            public IncomingRequest(ID.ThreadID thread,
                int startFrame,
                int length) {
                this.thread = thread;
                this.startFrame = startFrame;
                this.length = length;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
                startFrame = ps.readInt();
                length = ps.readInt();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                thread.write(ps.getOutputStream());
                ps.write(startFrame);
                ps.write(length);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("thread=" + thread);
                stringBuilder.append(", ");
                stringBuilder.append("startFrame=" + startFrame);
                stringBuilder.append(", ");
                stringBuilder.append("length=" + length);
                return stringBuilder.toString();
            }
        }

        public static class Frame {

            public ID.FrameID frameID;

            public JDWPLocation location;
            public Frame(ID.FrameID frameID,
                JDWPLocation location) {
                this.frameID = frameID;
                this.location = location;
            }
            public Frame() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                frameID = ID.read(ps.getInputStream(), ID.FrameID.class);
                location = ps.readLocation();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                frameID.write(ps.getOutputStream());
                ps.write(location);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("frameID=" + frameID);
                stringBuilder.append(", ");
                stringBuilder.append("location=" + location);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public Frame[] frames;
            public Reply(Frame[] frames) {
                this.frames = frames;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                final int framesCount = ps.readInt();
                frames = new Frame[framesCount];
                for (int i = 0; i < framesCount; i++) {
                    frames[i] = new Frame();
                    frames[i].read(ps);
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(frames.length);
                for (int i = 0; i < frames.length; i++) {
                    frames[i].write(ps);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("frames=[" + frames.length + "]{");
                for (int i = 0; i < frames.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("frames[i]=" + frames[i]);
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
            public ID.ThreadID thread;
            public IncomingRequest(ID.ThreadID thread) {
                this.thread = thread;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                thread.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("thread=" + thread);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public int frameCount;
            public Reply(int frameCount) {
                this.frameCount = frameCount;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                frameCount = ps.readInt();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(frameCount);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("frameCount=" + frameCount);
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
            public ID.ThreadID thread;
            public IncomingRequest(ID.ThreadID thread) {
                this.thread = thread;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                thread.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("thread=" + thread);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public JDWPValue[] owned;
            public Reply(JDWPValue[] owned) {
                this.owned = owned;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                final int ownedCount = ps.readInt();
                owned = new JDWPValue[ownedCount];
                for (int i = 0; i < ownedCount; i++) {
                    owned[i] = ps.readValue();
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(owned.length);
                for (int i = 0; i < owned.length; i++) {
                    ps.write(owned[i]);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("owned=[" + owned.length + "]{");
                for (int i = 0; i < owned.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("owned[i]=" + owned[i]);
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
            public ID.ThreadID thread;
            public IncomingRequest(ID.ThreadID thread) {
                this.thread = thread;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                thread.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("thread=" + thread);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public JDWPValue monitor;
            public Reply(JDWPValue monitor) {
                this.monitor = monitor;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                monitor = ps.readValue();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(monitor);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("monitor=" + monitor);
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
            public ID.ThreadID thread;

            public ID.ObjectID throwable;
            public IncomingRequest(ID.ThreadID thread,
                ID.ObjectID throwable) {
                this.thread = thread;
                this.throwable = throwable;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
                throwable = ID.read(ps.getInputStream(), ID.ObjectID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                thread.write(ps.getOutputStream());
                throwable.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("thread=" + thread);
                stringBuilder.append(", ");
                stringBuilder.append("throwable=" + throwable);
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
            public ID.ThreadID thread;
            public IncomingRequest(ID.ThreadID thread) {
                this.thread = thread;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                thread.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("thread=" + thread);
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
            public ID.ThreadID thread;
            public IncomingRequest(ID.ThreadID thread) {
                this.thread = thread;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                thread.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("thread=" + thread);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public int suspendCount;
            public Reply(int suspendCount) {
                this.suspendCount = suspendCount;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                suspendCount = ps.readInt();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(suspendCount);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("suspendCount=" + suspendCount);
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
            public ID.ThreadID thread;
            public IncomingRequest(ID.ThreadID thread) {
                this.thread = thread;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                thread.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("thread=" + thread);
                return stringBuilder.toString();
            }
        }

        public static class monitor {

            public JDWPValue monitor;

            public int stack_depth;
            public monitor(JDWPValue monitor,
                int stack_depth) {
                this.monitor = monitor;
                this.stack_depth = stack_depth;
            }
            public monitor() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                monitor = ps.readValue();
                stack_depth = ps.readInt();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(monitor);
                ps.write(stack_depth);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("monitor=" + monitor);
                stringBuilder.append(", ");
                stringBuilder.append("stack_depth=" + stack_depth);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public monitor[] owned;
            public Reply(monitor[] owned) {
                this.owned = owned;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                final int ownedCount = ps.readInt();
                owned = new monitor[ownedCount];
                for (int i = 0; i < ownedCount; i++) {
                    owned[i] = new monitor();
                    owned[i].read(ps);
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(owned.length);
                for (int i = 0; i < owned.length; i++) {
                    owned[i].write(ps);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("owned=[" + owned.length + "]{");
                for (int i = 0; i < owned.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("owned[i]=" + owned[i]);
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
            public ID.ThreadID thread;

            public JDWPValue value;
            public IncomingRequest(ID.ThreadID thread,
                JDWPValue value) {
                this.thread = thread;
                this.value = value;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
                value = ps.readValue();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                thread.write(ps.getOutputStream());
                ps.write(value);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("thread=" + thread);
                stringBuilder.append(", ");
                stringBuilder.append("value=" + value);
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

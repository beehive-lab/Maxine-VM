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

public final class EventRequestCommands {
    public static final int COMMAND_SET = 15;
    private EventRequestCommands() { }  // hide constructor

    public static class Set {
        public static final byte COMMAND = 1;
        public abstract static class Handler implements CommandHandler<IncomingRequest, Reply> {
            public IncomingRequest createIncomingDataObject() { return new IncomingRequest(); }
            public int helpAtDecodingUntaggedValue(IncomingRequest incomingRequest) throws JDWPException {assert false : "If this method can be called, it must be overwritten in subclasses!"; return 0; }
            public Reply handle(IncomingRequest incomingRequest, JDWPSender replyChannel) throws JDWPException { return handle(incomingRequest); }
            public Reply handle(IncomingRequest incomingRequest) throws JDWPException { throw new JDWPNotImplementedException(); }
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }
        }

        public static class Modifier {
            public abstract static class ModifierCommon {
                public abstract void write(JDWPOutputStream ps) throws java.io.IOException;
                public abstract void read(JDWPInputStream ps) throws java.io.IOException, JDWPException;
            }

            public byte modKind;
            public ModifierCommon aModifierCommon;

            public Modifier(byte modKind, ModifierCommon aModifierCommon) {
                this.modKind = modKind;
                this.aModifierCommon = aModifierCommon;
            }

            public Modifier() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                modKind = ps.readByte();
                switch (modKind) {
                    case 1:
                        aModifierCommon = new Count();
                        break;
                    case 2:
                        aModifierCommon = new Conditional();
                        break;
                    case 3:
                        aModifierCommon = new ThreadOnly();
                        break;
                    case 4:
                        aModifierCommon = new ClassOnly();
                        break;
                    case 5:
                        aModifierCommon = new ClassMatch();
                        break;
                    case 6:
                        aModifierCommon = new ClassExclude();
                        break;
                    case 7:
                        aModifierCommon = new LocationOnly();
                        break;
                    case 8:
                        aModifierCommon = new ExceptionOnly();
                        break;
                    case 9:
                        aModifierCommon = new FieldOnly();
                        break;
                    case 10:
                        aModifierCommon = new Step();
                        break;
                    case 11:
                        aModifierCommon = new InstanceOnly();
                        break;
                    case 12:
                        aModifierCommon = new SourceNameMatch();
                        break;
                }
                aModifierCommon.read(ps);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(modKind);
                aModifierCommon.write(ps);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("modKind=" + modKind);
                stringBuilder.append(aModifierCommon);
                return stringBuilder.toString();
            }

            public static class Count extends ModifierCommon {
                public static final byte ALT_ID = 1;
                public static Modifier create(int count) {
                    return new Modifier(ALT_ID, new Count(count));
                }

                public int count;
                public Count(int count) {
                    this.count = count;
                }
                public Count() {
                }
                @Override
                public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                    count = ps.readInt();
                }
                @Override
                public void write(JDWPOutputStream ps) throws java.io.IOException {
                    ps.write(count);
                }
                @Override
                public String toString() {
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("count=" + count);
                    return stringBuilder.toString();
                }
            }

            public static class Conditional extends ModifierCommon {
                public static final byte ALT_ID = 2;
                public static Modifier create(int exprID) {
                    return new Modifier(ALT_ID, new Conditional(exprID));
                }

                public int exprID;
                public Conditional(int exprID) {
                    this.exprID = exprID;
                }
                public Conditional() {
                }
                @Override
                public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                    exprID = ps.readInt();
                }
                @Override
                public void write(JDWPOutputStream ps) throws java.io.IOException {
                    ps.write(exprID);
                }
                @Override
                public String toString() {
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("exprID=" + exprID);
                    return stringBuilder.toString();
                }
            }

            public static class ThreadOnly extends ModifierCommon {
                public static final byte ALT_ID = 3;
                public static Modifier create(ID.ThreadID thread) {
                    return new Modifier(ALT_ID, new ThreadOnly(thread));
                }

                public ID.ThreadID thread;
                public ThreadOnly(ID.ThreadID thread) {
                    this.thread = thread;
                }
                public ThreadOnly() {
                }
                @Override
                public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                    thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
                }
                @Override
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

            public static class ClassOnly extends ModifierCommon {
                public static final byte ALT_ID = 4;
                public static Modifier create(ID.ReferenceTypeID clazz) {
                    return new Modifier(ALT_ID, new ClassOnly(clazz));
                }

                public ID.ReferenceTypeID clazz;
                public ClassOnly(ID.ReferenceTypeID clazz) {
                    this.clazz = clazz;
                }
                public ClassOnly() {
                }
                @Override
                public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                    clazz = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
                }
                @Override
                public void write(JDWPOutputStream ps) throws java.io.IOException {
                    clazz.write(ps.getOutputStream());
                }
                @Override
                public String toString() {
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("clazz=" + clazz);
                    return stringBuilder.toString();
                }
            }

            public static class ClassMatch extends ModifierCommon {
                public static final byte ALT_ID = 5;
                public static Modifier create(String classPattern) {
                    return new Modifier(ALT_ID, new ClassMatch(classPattern));
                }

                public String classPattern;
                public ClassMatch(String classPattern) {
                    this.classPattern = classPattern;
                }
                public ClassMatch() {
                }
                @Override
                public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                    classPattern = ps.readString();
                }
                @Override
                public void write(JDWPOutputStream ps) throws java.io.IOException {
                    ps.write(classPattern);
                }
                @Override
                public String toString() {
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("classPattern=" + classPattern);
                    return stringBuilder.toString();
                }
            }

            public static class ClassExclude extends ModifierCommon {
                public static final byte ALT_ID = 6;
                public static Modifier create(String classPattern) {
                    return new Modifier(ALT_ID, new ClassExclude(classPattern));
                }

                public String classPattern;
                public ClassExclude(String classPattern) {
                    this.classPattern = classPattern;
                }
                public ClassExclude() {
                }
                @Override
                public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                    classPattern = ps.readString();
                }
                @Override
                public void write(JDWPOutputStream ps) throws java.io.IOException {
                    ps.write(classPattern);
                }
                @Override
                public String toString() {
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("classPattern=" + classPattern);
                    return stringBuilder.toString();
                }
            }

            public static class LocationOnly extends ModifierCommon {
                public static final byte ALT_ID = 7;
                public static Modifier create(JDWPLocation loc) {
                    return new Modifier(ALT_ID, new LocationOnly(loc));
                }

                public JDWPLocation loc;
                public LocationOnly(JDWPLocation loc) {
                    this.loc = loc;
                }
                public LocationOnly() {
                }
                @Override
                public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                    loc = ps.readLocation();
                }
                @Override
                public void write(JDWPOutputStream ps) throws java.io.IOException {
                    ps.write(loc);
                }
                @Override
                public String toString() {
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("loc=" + loc);
                    return stringBuilder.toString();
                }
            }

            public static class ExceptionOnly extends ModifierCommon {
                public static final byte ALT_ID = 8;
                public static Modifier create(ID.ReferenceTypeID exceptionOrNull, boolean caught, boolean uncaught) {
                    return new Modifier(ALT_ID, new ExceptionOnly(exceptionOrNull, caught, uncaught));
                }

                public ID.ReferenceTypeID exceptionOrNull;

                public boolean caught;

                public boolean uncaught;
                public ExceptionOnly(ID.ReferenceTypeID exceptionOrNull,
                    boolean caught,
                    boolean uncaught) {
                    this.exceptionOrNull = exceptionOrNull;
                    this.caught = caught;
                    this.uncaught = uncaught;
                }
                public ExceptionOnly() {
                }
                @Override
                public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                    exceptionOrNull = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
                    caught = ps.readBoolean();
                    uncaught = ps.readBoolean();
                }
                @Override
                public void write(JDWPOutputStream ps) throws java.io.IOException {
                    exceptionOrNull.write(ps.getOutputStream());
                    ps.write(caught);
                    ps.write(uncaught);
                }
                @Override
                public String toString() {
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("exceptionOrNull=" + exceptionOrNull);
                    stringBuilder.append(", ");
                    stringBuilder.append("caught=" + caught);
                    stringBuilder.append(", ");
                    stringBuilder.append("uncaught=" + uncaught);
                    return stringBuilder.toString();
                }
            }

            public static class FieldOnly extends ModifierCommon {
                public static final byte ALT_ID = 9;
                public static Modifier create(ID.ReferenceTypeID declaring, ID.FieldID fieldID) {
                    return new Modifier(ALT_ID, new FieldOnly(declaring, fieldID));
                }

                public ID.ReferenceTypeID declaring;

                public ID.FieldID fieldID;
                public FieldOnly(ID.ReferenceTypeID declaring,
                    ID.FieldID fieldID) {
                    this.declaring = declaring;
                    this.fieldID = fieldID;
                }
                public FieldOnly() {
                }
                @Override
                public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                    declaring = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
                    fieldID = ID.read(ps.getInputStream(), ID.FieldID.class);
                }
                @Override
                public void write(JDWPOutputStream ps) throws java.io.IOException {
                    declaring.write(ps.getOutputStream());
                    fieldID.write(ps.getOutputStream());
                }
                @Override
                public String toString() {
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("declaring=" + declaring);
                    stringBuilder.append(", ");
                    stringBuilder.append("fieldID=" + fieldID);
                    return stringBuilder.toString();
                }
            }

            public static class Step extends ModifierCommon {
                public static final byte ALT_ID = 10;
                public static Modifier create(ID.ThreadID thread, int size, int depth) {
                    return new Modifier(ALT_ID, new Step(thread, size, depth));
                }

                public ID.ThreadID thread;

                public int size;

                public int depth;
                public Step(ID.ThreadID thread,
                    int size,
                    int depth) {
                    this.thread = thread;
                    this.size = size;
                    this.depth = depth;
                }
                public Step() {
                }
                @Override
                public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                    thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
                    size = ps.readInt();
                    depth = ps.readInt();
                }
                @Override
                public void write(JDWPOutputStream ps) throws java.io.IOException {
                    thread.write(ps.getOutputStream());
                    ps.write(size);
                    ps.write(depth);
                }
                @Override
                public String toString() {
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("thread=" + thread);
                    stringBuilder.append(", ");
                    stringBuilder.append("size=" + size);
                    stringBuilder.append(", ");
                    stringBuilder.append("depth=" + depth);
                    return stringBuilder.toString();
                }
            }

            public static class InstanceOnly extends ModifierCommon {
                public static final byte ALT_ID = 11;
                public static Modifier create(ID.ObjectID instance) {
                    return new Modifier(ALT_ID, new InstanceOnly(instance));
                }

                public ID.ObjectID instance;
                public InstanceOnly(ID.ObjectID instance) {
                    this.instance = instance;
                }
                public InstanceOnly() {
                }
                @Override
                public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                    instance = ID.read(ps.getInputStream(), ID.ObjectID.class);
                }
                @Override
                public void write(JDWPOutputStream ps) throws java.io.IOException {
                    instance.write(ps.getOutputStream());
                }
                @Override
                public String toString() {
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("instance=" + instance);
                    return stringBuilder.toString();
                }
            }

            public static class SourceNameMatch extends ModifierCommon {
                public static final byte ALT_ID = 12;
                public static Modifier create(String sourceNamePattern) {
                    return new Modifier(ALT_ID, new SourceNameMatch(sourceNamePattern));
                }

                public String sourceNamePattern;
                public SourceNameMatch(String sourceNamePattern) {
                    this.sourceNamePattern = sourceNamePattern;
                }
                public SourceNameMatch() {
                }
                @Override
                public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                    sourceNamePattern = ps.readString();
                }
                @Override
                public void write(JDWPOutputStream ps) throws java.io.IOException {
                    ps.write(sourceNamePattern);
                }
                @Override
                public String toString() {
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("sourceNamePattern=" + sourceNamePattern);
                    return stringBuilder.toString();
                }
            }
        }

        public static class IncomingRequest implements IncomingData {
            public byte eventKind;

            public byte suspendPolicy;

            public Modifier[] modifiers;
            public IncomingRequest(byte eventKind,
                byte suspendPolicy,
                Modifier[] modifiers) {
                this.eventKind = eventKind;
                this.suspendPolicy = suspendPolicy;
                this.modifiers = modifiers;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                eventKind = ps.readByte();
                suspendPolicy = ps.readByte();
                final int modifiersCount = ps.readInt();
                modifiers = new Modifier[modifiersCount];
                for (int i = 0; i < modifiersCount; i++) {
                    modifiers[i] = new Modifier();
                    modifiers[i].read(ps);
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(eventKind);
                ps.write(suspendPolicy);
                ps.write(modifiers.length);
                for (int i = 0; i < modifiers.length; i++) {
                    modifiers[i].write(ps);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("eventKind=" + eventKind);
                stringBuilder.append(", ");
                stringBuilder.append("suspendPolicy=" + suspendPolicy);
                stringBuilder.append(", ");
                stringBuilder.append("modifiers=[" + modifiers.length + "]{");
                for (int i = 0; i < modifiers.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("modifiers[i]=" + modifiers[i]);
                }
                stringBuilder.append("}");
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public int requestID;
            public Reply(int requestID) {
                this.requestID = requestID;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                requestID = ps.readInt();
            }
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

    public static class Clear {
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
            public byte eventKind;

            public int requestID;
            public IncomingRequest(byte eventKind,
                int requestID) {
                this.eventKind = eventKind;
                this.requestID = requestID;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                eventKind = ps.readByte();
                requestID = ps.readInt();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(eventKind);
                ps.write(requestID);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("eventKind=" + eventKind);
                stringBuilder.append(", ");
                stringBuilder.append("requestID=" + requestID);
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

    public static class ClearAllBreakpoints {
        public static final byte COMMAND = 3;
        public abstract static class Handler implements CommandHandler<IncomingRequest, Reply> {
            public IncomingRequest createIncomingDataObject() { return new IncomingRequest(); }
            public int helpAtDecodingUntaggedValue(IncomingRequest incomingRequest) throws JDWPException {assert false : "If this method can be called, it must be overwritten in subclasses!"; return 0; }
            public Reply handle(IncomingRequest incomingRequest, JDWPSender replyChannel) throws JDWPException { return handle(incomingRequest); }
            public Reply handle(IncomingRequest incomingRequest) throws JDWPException { throw new JDWPNotImplementedException(); }
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }
        }

        public static class IncomingRequest implements IncomingData {            public IncomingRequest() {
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

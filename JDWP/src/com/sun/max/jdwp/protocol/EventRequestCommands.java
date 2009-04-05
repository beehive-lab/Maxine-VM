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

            public byte _modKind;
            public ModifierCommon _aModifierCommon;

            public Modifier(byte modKind, ModifierCommon aModifierCommon) {
                this._modKind = modKind;
                this._aModifierCommon = aModifierCommon;
            }

            public Modifier() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _modKind = ps.readByte();
                switch (_modKind) {
                    case 1:
                        _aModifierCommon = new Count();
                        break;
                    case 2:
                        _aModifierCommon = new Conditional();
                        break;
                    case 3:
                        _aModifierCommon = new ThreadOnly();
                        break;
                    case 4:
                        _aModifierCommon = new ClassOnly();
                        break;
                    case 5:
                        _aModifierCommon = new ClassMatch();
                        break;
                    case 6:
                        _aModifierCommon = new ClassExclude();
                        break;
                    case 7:
                        _aModifierCommon = new LocationOnly();
                        break;
                    case 8:
                        _aModifierCommon = new ExceptionOnly();
                        break;
                    case 9:
                        _aModifierCommon = new FieldOnly();
                        break;
                    case 10:
                        _aModifierCommon = new Step();
                        break;
                    case 11:
                        _aModifierCommon = new InstanceOnly();
                        break;
                    case 12:
                        _aModifierCommon = new SourceNameMatch();
                        break;
                }
                _aModifierCommon.read(ps);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_modKind);
                _aModifierCommon.write(ps);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_modKind=" + _modKind);
                stringBuilder.append(_aModifierCommon);
                return stringBuilder.toString();
            }

            public static class Count extends ModifierCommon {
                public static final byte ALT_ID = 1;
                public static Modifier create(int count) {
                    return new Modifier(ALT_ID, new Count(count));
                }

                public int _count;
                public Count(int count) {
                    this._count = count;
                }
                public Count() {
                }
                @Override
                public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                    _count = ps.readInt();
                }
                @Override
                public void write(JDWPOutputStream ps) throws java.io.IOException {
                    ps.write(_count);
                }
                @Override
                public String toString() {
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("_count=" + _count);
                    return stringBuilder.toString();
                }
            }

            public static class Conditional extends ModifierCommon {
                public static final byte ALT_ID = 2;
                public static Modifier create(int exprID) {
                    return new Modifier(ALT_ID, new Conditional(exprID));
                }

                public int _exprID;
                public Conditional(int exprID) {
                    this._exprID = exprID;
                }
                public Conditional() {
                }
                @Override
                public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                    _exprID = ps.readInt();
                }
                @Override
                public void write(JDWPOutputStream ps) throws java.io.IOException {
                    ps.write(_exprID);
                }
                @Override
                public String toString() {
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("_exprID=" + _exprID);
                    return stringBuilder.toString();
                }
            }

            public static class ThreadOnly extends ModifierCommon {
                public static final byte ALT_ID = 3;
                public static Modifier create(ID.ThreadID thread) {
                    return new Modifier(ALT_ID, new ThreadOnly(thread));
                }

                public ID.ThreadID _thread;
                public ThreadOnly(ID.ThreadID thread) {
                    this._thread = thread;
                }
                public ThreadOnly() {
                }
                @Override
                public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                    _thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
                }
                @Override
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

            public static class ClassOnly extends ModifierCommon {
                public static final byte ALT_ID = 4;
                public static Modifier create(ID.ReferenceTypeID clazz) {
                    return new Modifier(ALT_ID, new ClassOnly(clazz));
                }

                public ID.ReferenceTypeID _clazz;
                public ClassOnly(ID.ReferenceTypeID clazz) {
                    this._clazz = clazz;
                }
                public ClassOnly() {
                }
                @Override
                public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                    _clazz = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
                }
                @Override
                public void write(JDWPOutputStream ps) throws java.io.IOException {
                    _clazz.write(ps.getOutputStream());
                }
                @Override
                public String toString() {
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("_clazz=" + _clazz);
                    return stringBuilder.toString();
                }
            }

            public static class ClassMatch extends ModifierCommon {
                public static final byte ALT_ID = 5;
                public static Modifier create(String classPattern) {
                    return new Modifier(ALT_ID, new ClassMatch(classPattern));
                }

                public String _classPattern;
                public ClassMatch(String classPattern) {
                    this._classPattern = classPattern;
                }
                public ClassMatch() {
                }
                @Override
                public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                    _classPattern = ps.readString();
                }
                @Override
                public void write(JDWPOutputStream ps) throws java.io.IOException {
                    ps.write(_classPattern);
                }
                @Override
                public String toString() {
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("_classPattern=" + _classPattern);
                    return stringBuilder.toString();
                }
            }

            public static class ClassExclude extends ModifierCommon {
                public static final byte ALT_ID = 6;
                public static Modifier create(String classPattern) {
                    return new Modifier(ALT_ID, new ClassExclude(classPattern));
                }

                public String _classPattern;
                public ClassExclude(String classPattern) {
                    this._classPattern = classPattern;
                }
                public ClassExclude() {
                }
                @Override
                public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                    _classPattern = ps.readString();
                }
                @Override
                public void write(JDWPOutputStream ps) throws java.io.IOException {
                    ps.write(_classPattern);
                }
                @Override
                public String toString() {
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("_classPattern=" + _classPattern);
                    return stringBuilder.toString();
                }
            }

            public static class LocationOnly extends ModifierCommon {
                public static final byte ALT_ID = 7;
                public static Modifier create(JDWPLocation loc) {
                    return new Modifier(ALT_ID, new LocationOnly(loc));
                }

                public JDWPLocation _loc;
                public LocationOnly(JDWPLocation loc) {
                    this._loc = loc;
                }
                public LocationOnly() {
                }
                @Override
                public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                    _loc = ps.readLocation();
                }
                @Override
                public void write(JDWPOutputStream ps) throws java.io.IOException {
                    ps.write(_loc);
                }
                @Override
                public String toString() {
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("_loc=" + _loc);
                    return stringBuilder.toString();
                }
            }

            public static class ExceptionOnly extends ModifierCommon {
                public static final byte ALT_ID = 8;
                public static Modifier create(ID.ReferenceTypeID exceptionOrNull, boolean caught, boolean uncaught) {
                    return new Modifier(ALT_ID, new ExceptionOnly(exceptionOrNull, caught, uncaught));
                }

                public ID.ReferenceTypeID _exceptionOrNull;

                public boolean _caught;

                public boolean _uncaught;
                public ExceptionOnly(ID.ReferenceTypeID exceptionOrNull,
                    boolean caught,
                    boolean uncaught) {
                    this._exceptionOrNull = exceptionOrNull;
                    this._caught = caught;
                    this._uncaught = uncaught;
                }
                public ExceptionOnly() {
                }
                @Override
                public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                    _exceptionOrNull = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
                    _caught = ps.readBoolean();
                    _uncaught = ps.readBoolean();
                }
                @Override
                public void write(JDWPOutputStream ps) throws java.io.IOException {
                    _exceptionOrNull.write(ps.getOutputStream());
                    ps.write(_caught);
                    ps.write(_uncaught);
                }
                @Override
                public String toString() {
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("_exceptionOrNull=" + _exceptionOrNull);
                    stringBuilder.append(", ");
                    stringBuilder.append("_caught=" + _caught);
                    stringBuilder.append(", ");
                    stringBuilder.append("_uncaught=" + _uncaught);
                    return stringBuilder.toString();
                }
            }

            public static class FieldOnly extends ModifierCommon {
                public static final byte ALT_ID = 9;
                public static Modifier create(ID.ReferenceTypeID declaring, ID.FieldID fieldID) {
                    return new Modifier(ALT_ID, new FieldOnly(declaring, fieldID));
                }

                public ID.ReferenceTypeID _declaring;

                public ID.FieldID _fieldID;
                public FieldOnly(ID.ReferenceTypeID declaring,
                    ID.FieldID fieldID) {
                    this._declaring = declaring;
                    this._fieldID = fieldID;
                }
                public FieldOnly() {
                }
                @Override
                public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                    _declaring = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
                    _fieldID = ID.read(ps.getInputStream(), ID.FieldID.class);
                }
                @Override
                public void write(JDWPOutputStream ps) throws java.io.IOException {
                    _declaring.write(ps.getOutputStream());
                    _fieldID.write(ps.getOutputStream());
                }
                @Override
                public String toString() {
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("_declaring=" + _declaring);
                    stringBuilder.append(", ");
                    stringBuilder.append("_fieldID=" + _fieldID);
                    return stringBuilder.toString();
                }
            }

            public static class Step extends ModifierCommon {
                public static final byte ALT_ID = 10;
                public static Modifier create(ID.ThreadID thread, int size, int depth) {
                    return new Modifier(ALT_ID, new Step(thread, size, depth));
                }

                public ID.ThreadID _thread;

                public int _size;

                public int _depth;
                public Step(ID.ThreadID thread,
                    int size,
                    int depth) {
                    this._thread = thread;
                    this._size = size;
                    this._depth = depth;
                }
                public Step() {
                }
                @Override
                public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                    _thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
                    _size = ps.readInt();
                    _depth = ps.readInt();
                }
                @Override
                public void write(JDWPOutputStream ps) throws java.io.IOException {
                    _thread.write(ps.getOutputStream());
                    ps.write(_size);
                    ps.write(_depth);
                }
                @Override
                public String toString() {
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("_thread=" + _thread);
                    stringBuilder.append(", ");
                    stringBuilder.append("_size=" + _size);
                    stringBuilder.append(", ");
                    stringBuilder.append("_depth=" + _depth);
                    return stringBuilder.toString();
                }
            }

            public static class InstanceOnly extends ModifierCommon {
                public static final byte ALT_ID = 11;
                public static Modifier create(ID.ObjectID instance) {
                    return new Modifier(ALT_ID, new InstanceOnly(instance));
                }

                public ID.ObjectID _instance;
                public InstanceOnly(ID.ObjectID instance) {
                    this._instance = instance;
                }
                public InstanceOnly() {
                }
                @Override
                public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                    _instance = ID.read(ps.getInputStream(), ID.ObjectID.class);
                }
                @Override
                public void write(JDWPOutputStream ps) throws java.io.IOException {
                    _instance.write(ps.getOutputStream());
                }
                @Override
                public String toString() {
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("_instance=" + _instance);
                    return stringBuilder.toString();
                }
            }

            public static class SourceNameMatch extends ModifierCommon {
                public static final byte ALT_ID = 12;
                public static Modifier create(String sourceNamePattern) {
                    return new Modifier(ALT_ID, new SourceNameMatch(sourceNamePattern));
                }

                public String _sourceNamePattern;
                public SourceNameMatch(String sourceNamePattern) {
                    this._sourceNamePattern = sourceNamePattern;
                }
                public SourceNameMatch() {
                }
                @Override
                public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                    _sourceNamePattern = ps.readString();
                }
                @Override
                public void write(JDWPOutputStream ps) throws java.io.IOException {
                    ps.write(_sourceNamePattern);
                }
                @Override
                public String toString() {
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("_sourceNamePattern=" + _sourceNamePattern);
                    return stringBuilder.toString();
                }
            }
        }

        public static class IncomingRequest implements IncomingData {
            public byte _eventKind;

            public byte _suspendPolicy;

            public Modifier[] _modifiers;
            public IncomingRequest(byte eventKind,
                byte suspendPolicy,
                Modifier[] modifiers) {
                this._eventKind = eventKind;
                this._suspendPolicy = suspendPolicy;
                this._modifiers = modifiers;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _eventKind = ps.readByte();
                _suspendPolicy = ps.readByte();
                final int modifiersCount = ps.readInt();
                _modifiers = new Modifier[modifiersCount];
                for (int i = 0; i < modifiersCount; i++) {
                    _modifiers[i] = new Modifier();
                    _modifiers[i].read(ps);
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_eventKind);
                ps.write(_suspendPolicy);
                ps.write(_modifiers.length);
                for (int i = 0; i < _modifiers.length; i++) {
                    _modifiers[i].write(ps);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_eventKind=" + _eventKind);
                stringBuilder.append(", ");
                stringBuilder.append("_suspendPolicy=" + _suspendPolicy);
                stringBuilder.append(", ");
                stringBuilder.append("_modifiers=[" + _modifiers.length + "]{");
                for (int i = 0; i < _modifiers.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("_modifiers[i]=" + _modifiers[i]);
                }
                stringBuilder.append("}");
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public int _requestID;
            public Reply(int requestID) {
                this._requestID = requestID;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _requestID = ps.readInt();
            }
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
            public byte _eventKind;

            public int _requestID;
            public IncomingRequest(byte eventKind,
                int requestID) {
                this._eventKind = eventKind;
                this._requestID = requestID;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _eventKind = ps.readByte();
                _requestID = ps.readInt();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_eventKind);
                ps.write(_requestID);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_eventKind=" + _eventKind);
                stringBuilder.append(", ");
                stringBuilder.append("_requestID=" + _requestID);
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

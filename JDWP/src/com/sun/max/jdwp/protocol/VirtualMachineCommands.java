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
/*VCSID=560e18c2-d46c-42c7-bfd7-5bbd1f1fb862*/

// WARNING: Generated file, do not modify!

// To use the original JDWP specification as a basis, some styleguides have to be turned off
// Checkstyle: stop field name check

package com.sun.max.jdwp.protocol;
import com.sun.max.jdwp.data.*;
import com.sun.max.jdwp.constants.*;
@SuppressWarnings("unused")

public final class VirtualMachineCommands {
    public static final int COMMAND_SET = 1;
    private VirtualMachineCommands() { }  // hide constructor

    public static class Version {
        public static final byte COMMAND = 1;
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

            public String _description;

            public int _jdwpMajor;

            public int _jdwpMinor;

            public String _vmVersion;

            public String _vmName;
            public Reply(String description,
                int jdwpMajor,
                int jdwpMinor,
                String vmVersion,
                String vmName) {
                this._description = description;
                this._jdwpMajor = jdwpMajor;
                this._jdwpMinor = jdwpMinor;
                this._vmVersion = vmVersion;
                this._vmName = vmName;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _description = ps.readString();
                _jdwpMajor = ps.readInt();
                _jdwpMinor = ps.readInt();
                _vmVersion = ps.readString();
                _vmName = ps.readString();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_description);
                ps.write(_jdwpMajor);
                ps.write(_jdwpMinor);
                ps.write(_vmVersion);
                ps.write(_vmName);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_description=" + _description);
                stringBuilder.append(", ");
                stringBuilder.append("_jdwpMajor=" + _jdwpMajor);
                stringBuilder.append(", ");
                stringBuilder.append("_jdwpMinor=" + _jdwpMinor);
                stringBuilder.append(", ");
                stringBuilder.append("_vmVersion=" + _vmVersion);
                stringBuilder.append(", ");
                stringBuilder.append("_vmName=" + _vmName);
                return stringBuilder.toString();
            }
        }
    }

    public static class ClassesBySignature {
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
            public String _signature;
            public IncomingRequest(String signature) {
                this._signature = signature;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _signature = ps.readString();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_signature);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_signature=" + _signature);
                return stringBuilder.toString();
            }
        }

        public static class ClassInfo {

            public byte _refTypeTag;

            public ID.ReferenceTypeID _typeID;

            public int _status;
            public ClassInfo(byte refTypeTag,
                ID.ReferenceTypeID typeID,
                int status) {
                this._refTypeTag = refTypeTag;
                this._typeID = typeID;
                this._status = status;
            }
            public ClassInfo() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _refTypeTag = ps.readByte();
                _typeID = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
                _status = ps.readInt();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_refTypeTag);
                _typeID.write(ps.getOutputStream());
                ps.write(_status);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_refTypeTag=" + _refTypeTag);
                stringBuilder.append(", ");
                stringBuilder.append("_typeID=" + _typeID);
                stringBuilder.append(", ");
                stringBuilder.append("_status=" + _status);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public ClassInfo[] _classes;
            public Reply(ClassInfo[] classes) {
                this._classes = classes;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                final int classesCount = ps.readInt();
                _classes = new ClassInfo[classesCount];
                for (int i = 0; i < classesCount; i++) {
                    _classes[i] = new ClassInfo();
                    _classes[i].read(ps);
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_classes.length);
                for (int i = 0; i < _classes.length; i++) {
                    _classes[i].write(ps);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_classes=[" + _classes.length + "]{");
                for (int i = 0; i < _classes.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("_classes[i]=" + _classes[i]);
                }
                stringBuilder.append("}");
                return stringBuilder.toString();
            }
        }
    }

    public static class AllClasses {
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

        public static class ClassInfo {

            public byte _refTypeTag;

            public ID.ReferenceTypeID _typeID;

            public String _signature;

            public int _status;
            public ClassInfo(byte refTypeTag,
                ID.ReferenceTypeID typeID,
                String signature,
                int status) {
                this._refTypeTag = refTypeTag;
                this._typeID = typeID;
                this._signature = signature;
                this._status = status;
            }
            public ClassInfo() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _refTypeTag = ps.readByte();
                _typeID = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
                _signature = ps.readString();
                _status = ps.readInt();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_refTypeTag);
                _typeID.write(ps.getOutputStream());
                ps.write(_signature);
                ps.write(_status);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
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

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public ClassInfo[] _classes;
            public Reply(ClassInfo[] classes) {
                this._classes = classes;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                final int classesCount = ps.readInt();
                _classes = new ClassInfo[classesCount];
                for (int i = 0; i < classesCount; i++) {
                    _classes[i] = new ClassInfo();
                    _classes[i].read(ps);
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_classes.length);
                for (int i = 0; i < _classes.length; i++) {
                    _classes[i].write(ps);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_classes=[" + _classes.length + "]{");
                for (int i = 0; i < _classes.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("_classes[i]=" + _classes[i]);
                }
                stringBuilder.append("}");
                return stringBuilder.toString();
            }
        }
    }

    public static class AllThreads {
        public static final byte COMMAND = 4;
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

            public ID.ThreadID[] _threads;
            public Reply(ID.ThreadID[] threads) {
                this._threads = threads;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                final int threadsCount = ps.readInt();
                _threads = new ID.ThreadID[threadsCount];
                for (int i = 0; i < threadsCount; i++) {
                    _threads[i] = ID.read(ps.getInputStream(), ID.ThreadID.class);
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_threads.length);
                for (int i = 0; i < _threads.length; i++) {
                    _threads[i].write(ps.getOutputStream());
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_threads=[" + _threads.length + "]{");
                for (int i = 0; i < _threads.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("_threads[i]=" + _threads[i]);
                }
                stringBuilder.append("}");
                return stringBuilder.toString();
            }
        }
    }

    public static class TopLevelThreadGroups {
        public static final byte COMMAND = 5;
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

            public ID.ThreadGroupID[] _groups;
            public Reply(ID.ThreadGroupID[] groups) {
                this._groups = groups;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                final int groupsCount = ps.readInt();
                _groups = new ID.ThreadGroupID[groupsCount];
                for (int i = 0; i < groupsCount; i++) {
                    _groups[i] = ID.read(ps.getInputStream(), ID.ThreadGroupID.class);
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_groups.length);
                for (int i = 0; i < _groups.length; i++) {
                    _groups[i].write(ps.getOutputStream());
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_groups=[" + _groups.length + "]{");
                for (int i = 0; i < _groups.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("_groups[i]=" + _groups[i]);
                }
                stringBuilder.append("}");
                return stringBuilder.toString();
            }
        }
    }

    public static class Dispose {
        public static final byte COMMAND = 6;
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

    public static class IDSizes {
        public static final byte COMMAND = 7;
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

            public int _fieldIDSize;

            public int _methodIDSize;

            public int _objectIDSize;

            public int _referenceTypeIDSize;

            public int _frameIDSize;
            public Reply(int fieldIDSize,
                int methodIDSize,
                int objectIDSize,
                int referenceTypeIDSize,
                int frameIDSize) {
                this._fieldIDSize = fieldIDSize;
                this._methodIDSize = methodIDSize;
                this._objectIDSize = objectIDSize;
                this._referenceTypeIDSize = referenceTypeIDSize;
                this._frameIDSize = frameIDSize;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _fieldIDSize = ps.readInt();
                _methodIDSize = ps.readInt();
                _objectIDSize = ps.readInt();
                _referenceTypeIDSize = ps.readInt();
                _frameIDSize = ps.readInt();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_fieldIDSize);
                ps.write(_methodIDSize);
                ps.write(_objectIDSize);
                ps.write(_referenceTypeIDSize);
                ps.write(_frameIDSize);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_fieldIDSize=" + _fieldIDSize);
                stringBuilder.append(", ");
                stringBuilder.append("_methodIDSize=" + _methodIDSize);
                stringBuilder.append(", ");
                stringBuilder.append("_objectIDSize=" + _objectIDSize);
                stringBuilder.append(", ");
                stringBuilder.append("_referenceTypeIDSize=" + _referenceTypeIDSize);
                stringBuilder.append(", ");
                stringBuilder.append("_frameIDSize=" + _frameIDSize);
                return stringBuilder.toString();
            }
        }
    }

    public static class Suspend {
        public static final byte COMMAND = 8;
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

    public static class Resume {
        public static final byte COMMAND = 9;
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

    public static class Exit {
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
            public int _exitCode;
            public IncomingRequest(int exitCode) {
                this._exitCode = exitCode;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _exitCode = ps.readInt();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_exitCode);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_exitCode=" + _exitCode);
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

    public static class CreateString {
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
            public String _utf;
            public IncomingRequest(String utf) {
                this._utf = utf;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _utf = ps.readString();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_utf);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_utf=" + _utf);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public ID.StringID _stringObject;
            public Reply(ID.StringID stringObject) {
                this._stringObject = stringObject;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _stringObject = ID.read(ps.getInputStream(), ID.StringID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _stringObject.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_stringObject=" + _stringObject);
                return stringBuilder.toString();
            }
        }
    }

    public static class Capabilities {
        public static final byte COMMAND = 12;
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

            public boolean _canWatchFieldModification;

            public boolean _canWatchFieldAccess;

            public boolean _canGetBytecodes;

            public boolean _canGetSyntheticAttribute;

            public boolean _canGetOwnedMonitorInfo;

            public boolean _canGetCurrentContendedMonitor;

            public boolean _canGetMonitorInfo;
            public Reply(boolean canWatchFieldModification,
                boolean canWatchFieldAccess,
                boolean canGetBytecodes,
                boolean canGetSyntheticAttribute,
                boolean canGetOwnedMonitorInfo,
                boolean canGetCurrentContendedMonitor,
                boolean canGetMonitorInfo) {
                this._canWatchFieldModification = canWatchFieldModification;
                this._canWatchFieldAccess = canWatchFieldAccess;
                this._canGetBytecodes = canGetBytecodes;
                this._canGetSyntheticAttribute = canGetSyntheticAttribute;
                this._canGetOwnedMonitorInfo = canGetOwnedMonitorInfo;
                this._canGetCurrentContendedMonitor = canGetCurrentContendedMonitor;
                this._canGetMonitorInfo = canGetMonitorInfo;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _canWatchFieldModification = ps.readBoolean();
                _canWatchFieldAccess = ps.readBoolean();
                _canGetBytecodes = ps.readBoolean();
                _canGetSyntheticAttribute = ps.readBoolean();
                _canGetOwnedMonitorInfo = ps.readBoolean();
                _canGetCurrentContendedMonitor = ps.readBoolean();
                _canGetMonitorInfo = ps.readBoolean();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_canWatchFieldModification);
                ps.write(_canWatchFieldAccess);
                ps.write(_canGetBytecodes);
                ps.write(_canGetSyntheticAttribute);
                ps.write(_canGetOwnedMonitorInfo);
                ps.write(_canGetCurrentContendedMonitor);
                ps.write(_canGetMonitorInfo);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_canWatchFieldModification=" + _canWatchFieldModification);
                stringBuilder.append(", ");
                stringBuilder.append("_canWatchFieldAccess=" + _canWatchFieldAccess);
                stringBuilder.append(", ");
                stringBuilder.append("_canGetBytecodes=" + _canGetBytecodes);
                stringBuilder.append(", ");
                stringBuilder.append("_canGetSyntheticAttribute=" + _canGetSyntheticAttribute);
                stringBuilder.append(", ");
                stringBuilder.append("_canGetOwnedMonitorInfo=" + _canGetOwnedMonitorInfo);
                stringBuilder.append(", ");
                stringBuilder.append("_canGetCurrentContendedMonitor=" + _canGetCurrentContendedMonitor);
                stringBuilder.append(", ");
                stringBuilder.append("_canGetMonitorInfo=" + _canGetMonitorInfo);
                return stringBuilder.toString();
            }
        }
    }

    public static class ClassPaths {
        public static final byte COMMAND = 13;
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

            public String _baseDir;

            public String[] _classpaths;

            public String[] _bootclasspaths;
            public Reply(String baseDir,
                String[] classpaths,
                String[] bootclasspaths) {
                this._baseDir = baseDir;
                this._classpaths = classpaths;
                this._bootclasspaths = bootclasspaths;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _baseDir = ps.readString();
                final int classpathsCount = ps.readInt();
                _classpaths = new String[classpathsCount];
                for (int i = 0; i < classpathsCount; i++) {
                    _classpaths[i] = ps.readString();
                }
                final int bootclasspathsCount = ps.readInt();
                _bootclasspaths = new String[bootclasspathsCount];
                for (int i = 0; i < bootclasspathsCount; i++) {
                    _bootclasspaths[i] = ps.readString();
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_baseDir);
                ps.write(_classpaths.length);
                for (int i = 0; i < _classpaths.length; i++) {
                    ps.write(_classpaths[i]);
                }
                ps.write(_bootclasspaths.length);
                for (int i = 0; i < _bootclasspaths.length; i++) {
                    ps.write(_bootclasspaths[i]);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_baseDir=" + _baseDir);
                stringBuilder.append(", ");
                stringBuilder.append("_classpaths=[" + _classpaths.length + "]{");
                for (int i = 0; i < _classpaths.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("_classpaths[i]=" + _classpaths[i]);
                }
                stringBuilder.append("}");
                stringBuilder.append(", ");
                stringBuilder.append("_bootclasspaths=[" + _bootclasspaths.length + "]{");
                for (int i = 0; i < _bootclasspaths.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("_bootclasspaths[i]=" + _bootclasspaths[i]);
                }
                stringBuilder.append("}");
                return stringBuilder.toString();
            }
        }
    }

    public static class DisposeObjects {
        public static final byte COMMAND = 14;
        public abstract static class Handler implements CommandHandler<IncomingRequest, Reply> {
            public IncomingRequest createIncomingDataObject() { return new IncomingRequest(); }
            public int helpAtDecodingUntaggedValue(IncomingRequest incomingRequest) throws JDWPException {assert false : "If this method can be called, it must be overwritten in subclasses!"; return 0; }
            public Reply handle(IncomingRequest incomingRequest, JDWPSender replyChannel) throws JDWPException { return handle(incomingRequest); }
            public Reply handle(IncomingRequest incomingRequest) throws JDWPException { throw new JDWPNotImplementedException(); }
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }
        }

        public static class Request {

            public ID.ObjectID _object;

            public int _refCnt;
            public Request(ID.ObjectID object,
                int refCnt) {
                this._object = object;
                this._refCnt = refCnt;
            }
            public Request() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _object = ID.read(ps.getInputStream(), ID.ObjectID.class);
                _refCnt = ps.readInt();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _object.write(ps.getOutputStream());
                ps.write(_refCnt);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_object=" + _object);
                stringBuilder.append(", ");
                stringBuilder.append("_refCnt=" + _refCnt);
                return stringBuilder.toString();
            }
        }

        public static class IncomingRequest implements IncomingData {
            public Request[] _requests;
            public IncomingRequest(Request[] requests) {
                this._requests = requests;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                final int requestsCount = ps.readInt();
                _requests = new Request[requestsCount];
                for (int i = 0; i < requestsCount; i++) {
                    _requests[i] = new Request();
                    _requests[i].read(ps);
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_requests.length);
                for (int i = 0; i < _requests.length; i++) {
                    _requests[i].write(ps);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_requests=[" + _requests.length + "]{");
                for (int i = 0; i < _requests.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("_requests[i]=" + _requests[i]);
                }
                stringBuilder.append("}");
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

    public static class HoldEvents {
        public static final byte COMMAND = 15;
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

    public static class ReleaseEvents {
        public static final byte COMMAND = 16;
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

    public static class CapabilitiesNew {
        public static final byte COMMAND = 17;
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

            public boolean _canWatchFieldModification;

            public boolean _canWatchFieldAccess;

            public boolean _canGetBytecodes;

            public boolean _canGetSyntheticAttribute;

            public boolean _canGetOwnedMonitorInfo;

            public boolean _canGetCurrentContendedMonitor;

            public boolean _canGetMonitorInfo;

            public boolean _canRedefineClasses;

            public boolean _canAddMethod;

            public boolean _canUnrestrictedlyRedefineClasses;

            public boolean _canPopFrames;

            public boolean _canUseInstanceFilters;

            public boolean _canGetSourceDebugExtension;

            public boolean _canRequestVMDeathEvent;

            public boolean _canSetDefaultStratum;

            public boolean _canGetInstanceInfo;

            public boolean _canRequestMonitorEvents;

            public boolean _canGetMonitorFrameInfo;

            public boolean _canUseSourceNameFilters;

            public boolean _canGetConstantPool;

            public boolean _canForceEarlyReturn;

            public boolean _reserved22;

            public boolean _reserved23;

            public boolean _reserved24;

            public boolean _reserved25;

            public boolean _reserved26;

            public boolean _reserved27;

            public boolean _reserved28;

            public boolean _reserved29;

            public boolean _reserved30;

            public boolean _reserved31;

            public boolean _reserved32;
            public Reply(boolean canWatchFieldModification,
                boolean canWatchFieldAccess,
                boolean canGetBytecodes,
                boolean canGetSyntheticAttribute,
                boolean canGetOwnedMonitorInfo,
                boolean canGetCurrentContendedMonitor,
                boolean canGetMonitorInfo,
                boolean canRedefineClasses,
                boolean canAddMethod,
                boolean canUnrestrictedlyRedefineClasses,
                boolean canPopFrames,
                boolean canUseInstanceFilters,
                boolean canGetSourceDebugExtension,
                boolean canRequestVMDeathEvent,
                boolean canSetDefaultStratum,
                boolean canGetInstanceInfo,
                boolean canRequestMonitorEvents,
                boolean canGetMonitorFrameInfo,
                boolean canUseSourceNameFilters,
                boolean canGetConstantPool,
                boolean canForceEarlyReturn,
                boolean reserved22,
                boolean reserved23,
                boolean reserved24,
                boolean reserved25,
                boolean reserved26,
                boolean reserved27,
                boolean reserved28,
                boolean reserved29,
                boolean reserved30,
                boolean reserved31,
                boolean reserved32) {
                this._canWatchFieldModification = canWatchFieldModification;
                this._canWatchFieldAccess = canWatchFieldAccess;
                this._canGetBytecodes = canGetBytecodes;
                this._canGetSyntheticAttribute = canGetSyntheticAttribute;
                this._canGetOwnedMonitorInfo = canGetOwnedMonitorInfo;
                this._canGetCurrentContendedMonitor = canGetCurrentContendedMonitor;
                this._canGetMonitorInfo = canGetMonitorInfo;
                this._canRedefineClasses = canRedefineClasses;
                this._canAddMethod = canAddMethod;
                this._canUnrestrictedlyRedefineClasses = canUnrestrictedlyRedefineClasses;
                this._canPopFrames = canPopFrames;
                this._canUseInstanceFilters = canUseInstanceFilters;
                this._canGetSourceDebugExtension = canGetSourceDebugExtension;
                this._canRequestVMDeathEvent = canRequestVMDeathEvent;
                this._canSetDefaultStratum = canSetDefaultStratum;
                this._canGetInstanceInfo = canGetInstanceInfo;
                this._canRequestMonitorEvents = canRequestMonitorEvents;
                this._canGetMonitorFrameInfo = canGetMonitorFrameInfo;
                this._canUseSourceNameFilters = canUseSourceNameFilters;
                this._canGetConstantPool = canGetConstantPool;
                this._canForceEarlyReturn = canForceEarlyReturn;
                this._reserved22 = reserved22;
                this._reserved23 = reserved23;
                this._reserved24 = reserved24;
                this._reserved25 = reserved25;
                this._reserved26 = reserved26;
                this._reserved27 = reserved27;
                this._reserved28 = reserved28;
                this._reserved29 = reserved29;
                this._reserved30 = reserved30;
                this._reserved31 = reserved31;
                this._reserved32 = reserved32;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _canWatchFieldModification = ps.readBoolean();
                _canWatchFieldAccess = ps.readBoolean();
                _canGetBytecodes = ps.readBoolean();
                _canGetSyntheticAttribute = ps.readBoolean();
                _canGetOwnedMonitorInfo = ps.readBoolean();
                _canGetCurrentContendedMonitor = ps.readBoolean();
                _canGetMonitorInfo = ps.readBoolean();
                _canRedefineClasses = ps.readBoolean();
                _canAddMethod = ps.readBoolean();
                _canUnrestrictedlyRedefineClasses = ps.readBoolean();
                _canPopFrames = ps.readBoolean();
                _canUseInstanceFilters = ps.readBoolean();
                _canGetSourceDebugExtension = ps.readBoolean();
                _canRequestVMDeathEvent = ps.readBoolean();
                _canSetDefaultStratum = ps.readBoolean();
                _canGetInstanceInfo = ps.readBoolean();
                _canRequestMonitorEvents = ps.readBoolean();
                _canGetMonitorFrameInfo = ps.readBoolean();
                _canUseSourceNameFilters = ps.readBoolean();
                _canGetConstantPool = ps.readBoolean();
                _canForceEarlyReturn = ps.readBoolean();
                _reserved22 = ps.readBoolean();
                _reserved23 = ps.readBoolean();
                _reserved24 = ps.readBoolean();
                _reserved25 = ps.readBoolean();
                _reserved26 = ps.readBoolean();
                _reserved27 = ps.readBoolean();
                _reserved28 = ps.readBoolean();
                _reserved29 = ps.readBoolean();
                _reserved30 = ps.readBoolean();
                _reserved31 = ps.readBoolean();
                _reserved32 = ps.readBoolean();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_canWatchFieldModification);
                ps.write(_canWatchFieldAccess);
                ps.write(_canGetBytecodes);
                ps.write(_canGetSyntheticAttribute);
                ps.write(_canGetOwnedMonitorInfo);
                ps.write(_canGetCurrentContendedMonitor);
                ps.write(_canGetMonitorInfo);
                ps.write(_canRedefineClasses);
                ps.write(_canAddMethod);
                ps.write(_canUnrestrictedlyRedefineClasses);
                ps.write(_canPopFrames);
                ps.write(_canUseInstanceFilters);
                ps.write(_canGetSourceDebugExtension);
                ps.write(_canRequestVMDeathEvent);
                ps.write(_canSetDefaultStratum);
                ps.write(_canGetInstanceInfo);
                ps.write(_canRequestMonitorEvents);
                ps.write(_canGetMonitorFrameInfo);
                ps.write(_canUseSourceNameFilters);
                ps.write(_canGetConstantPool);
                ps.write(_canForceEarlyReturn);
                ps.write(_reserved22);
                ps.write(_reserved23);
                ps.write(_reserved24);
                ps.write(_reserved25);
                ps.write(_reserved26);
                ps.write(_reserved27);
                ps.write(_reserved28);
                ps.write(_reserved29);
                ps.write(_reserved30);
                ps.write(_reserved31);
                ps.write(_reserved32);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_canWatchFieldModification=" + _canWatchFieldModification);
                stringBuilder.append(", ");
                stringBuilder.append("_canWatchFieldAccess=" + _canWatchFieldAccess);
                stringBuilder.append(", ");
                stringBuilder.append("_canGetBytecodes=" + _canGetBytecodes);
                stringBuilder.append(", ");
                stringBuilder.append("_canGetSyntheticAttribute=" + _canGetSyntheticAttribute);
                stringBuilder.append(", ");
                stringBuilder.append("_canGetOwnedMonitorInfo=" + _canGetOwnedMonitorInfo);
                stringBuilder.append(", ");
                stringBuilder.append("_canGetCurrentContendedMonitor=" + _canGetCurrentContendedMonitor);
                stringBuilder.append(", ");
                stringBuilder.append("_canGetMonitorInfo=" + _canGetMonitorInfo);
                stringBuilder.append(", ");
                stringBuilder.append("_canRedefineClasses=" + _canRedefineClasses);
                stringBuilder.append(", ");
                stringBuilder.append("_canAddMethod=" + _canAddMethod);
                stringBuilder.append(", ");
                stringBuilder.append("_canUnrestrictedlyRedefineClasses=" + _canUnrestrictedlyRedefineClasses);
                stringBuilder.append(", ");
                stringBuilder.append("_canPopFrames=" + _canPopFrames);
                stringBuilder.append(", ");
                stringBuilder.append("_canUseInstanceFilters=" + _canUseInstanceFilters);
                stringBuilder.append(", ");
                stringBuilder.append("_canGetSourceDebugExtension=" + _canGetSourceDebugExtension);
                stringBuilder.append(", ");
                stringBuilder.append("_canRequestVMDeathEvent=" + _canRequestVMDeathEvent);
                stringBuilder.append(", ");
                stringBuilder.append("_canSetDefaultStratum=" + _canSetDefaultStratum);
                stringBuilder.append(", ");
                stringBuilder.append("_canGetInstanceInfo=" + _canGetInstanceInfo);
                stringBuilder.append(", ");
                stringBuilder.append("_canRequestMonitorEvents=" + _canRequestMonitorEvents);
                stringBuilder.append(", ");
                stringBuilder.append("_canGetMonitorFrameInfo=" + _canGetMonitorFrameInfo);
                stringBuilder.append(", ");
                stringBuilder.append("_canUseSourceNameFilters=" + _canUseSourceNameFilters);
                stringBuilder.append(", ");
                stringBuilder.append("_canGetConstantPool=" + _canGetConstantPool);
                stringBuilder.append(", ");
                stringBuilder.append("_canForceEarlyReturn=" + _canForceEarlyReturn);
                stringBuilder.append(", ");
                stringBuilder.append("_reserved22=" + _reserved22);
                stringBuilder.append(", ");
                stringBuilder.append("_reserved23=" + _reserved23);
                stringBuilder.append(", ");
                stringBuilder.append("_reserved24=" + _reserved24);
                stringBuilder.append(", ");
                stringBuilder.append("_reserved25=" + _reserved25);
                stringBuilder.append(", ");
                stringBuilder.append("_reserved26=" + _reserved26);
                stringBuilder.append(", ");
                stringBuilder.append("_reserved27=" + _reserved27);
                stringBuilder.append(", ");
                stringBuilder.append("_reserved28=" + _reserved28);
                stringBuilder.append(", ");
                stringBuilder.append("_reserved29=" + _reserved29);
                stringBuilder.append(", ");
                stringBuilder.append("_reserved30=" + _reserved30);
                stringBuilder.append(", ");
                stringBuilder.append("_reserved31=" + _reserved31);
                stringBuilder.append(", ");
                stringBuilder.append("_reserved32=" + _reserved32);
                return stringBuilder.toString();
            }
        }
    }

    public static class RedefineClasses {
        public static final byte COMMAND = 18;
        public abstract static class Handler implements CommandHandler<IncomingRequest, Reply> {
            public IncomingRequest createIncomingDataObject() { return new IncomingRequest(); }
            public int helpAtDecodingUntaggedValue(IncomingRequest incomingRequest) throws JDWPException {assert false : "If this method can be called, it must be overwritten in subclasses!"; return 0; }
            public Reply handle(IncomingRequest incomingRequest, JDWPSender replyChannel) throws JDWPException { return handle(incomingRequest); }
            public Reply handle(IncomingRequest incomingRequest) throws JDWPException { throw new JDWPNotImplementedException(); }
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }
        }

        public static class ClassDef {

            public ID.ReferenceTypeID _refType;

            public byte[] _classfile;
            public ClassDef(ID.ReferenceTypeID refType,
                byte[] classfile) {
                this._refType = refType;
                this._classfile = classfile;
            }
            public ClassDef() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _refType = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
                final int classfileCount = ps.readInt();
                _classfile = new byte[classfileCount];
                for (int i = 0; i < classfileCount; i++) {
                    _classfile[i] = ps.readByte();
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _refType.write(ps.getOutputStream());
                ps.write(_classfile.length);
                for (int i = 0; i < _classfile.length; i++) {
                    ps.write(_classfile[i]);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_refType=" + _refType);
                stringBuilder.append(", ");
                stringBuilder.append("_classfile=[" + _classfile.length + "]{");
                for (int i = 0; i < _classfile.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("_classfile[i]=" + _classfile[i]);
                }
                stringBuilder.append("}");
                return stringBuilder.toString();
            }
        }

        public static class IncomingRequest implements IncomingData {
            public ClassDef[] _classes;
            public IncomingRequest(ClassDef[] classes) {
                this._classes = classes;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                final int classesCount = ps.readInt();
                _classes = new ClassDef[classesCount];
                for (int i = 0; i < classesCount; i++) {
                    _classes[i] = new ClassDef();
                    _classes[i].read(ps);
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_classes.length);
                for (int i = 0; i < _classes.length; i++) {
                    _classes[i].write(ps);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_classes=[" + _classes.length + "]{");
                for (int i = 0; i < _classes.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("_classes[i]=" + _classes[i]);
                }
                stringBuilder.append("}");
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

    public static class SetDefaultStratum {
        public static final byte COMMAND = 19;
        public abstract static class Handler implements CommandHandler<IncomingRequest, Reply> {
            public IncomingRequest createIncomingDataObject() { return new IncomingRequest(); }
            public int helpAtDecodingUntaggedValue(IncomingRequest incomingRequest) throws JDWPException {assert false : "If this method can be called, it must be overwritten in subclasses!"; return 0; }
            public Reply handle(IncomingRequest incomingRequest, JDWPSender replyChannel) throws JDWPException { return handle(incomingRequest); }
            public Reply handle(IncomingRequest incomingRequest) throws JDWPException { throw new JDWPNotImplementedException(); }
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }
        }

        public static class IncomingRequest implements IncomingData {
            public String _stratumID;
            public IncomingRequest(String stratumID) {
                this._stratumID = stratumID;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _stratumID = ps.readString();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_stratumID);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_stratumID=" + _stratumID);
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

    public static class AllClassesWithGeneric {
        public static final byte COMMAND = 20;
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

        public static class ClassInfo {

            public byte _refTypeTag;

            public ID.ReferenceTypeID _typeID;

            public String _signature;

            public String _genericSignature;

            public int _status;
            public ClassInfo(byte refTypeTag,
                ID.ReferenceTypeID typeID,
                String signature,
                String genericSignature,
                int status) {
                this._refTypeTag = refTypeTag;
                this._typeID = typeID;
                this._signature = signature;
                this._genericSignature = genericSignature;
                this._status = status;
            }
            public ClassInfo() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _refTypeTag = ps.readByte();
                _typeID = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
                _signature = ps.readString();
                _genericSignature = ps.readString();
                _status = ps.readInt();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_refTypeTag);
                _typeID.write(ps.getOutputStream());
                ps.write(_signature);
                ps.write(_genericSignature);
                ps.write(_status);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_refTypeTag=" + _refTypeTag);
                stringBuilder.append(", ");
                stringBuilder.append("_typeID=" + _typeID);
                stringBuilder.append(", ");
                stringBuilder.append("_signature=" + _signature);
                stringBuilder.append(", ");
                stringBuilder.append("_genericSignature=" + _genericSignature);
                stringBuilder.append(", ");
                stringBuilder.append("_status=" + _status);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public ClassInfo[] _classes;
            public Reply(ClassInfo[] classes) {
                this._classes = classes;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                final int classesCount = ps.readInt();
                _classes = new ClassInfo[classesCount];
                for (int i = 0; i < classesCount; i++) {
                    _classes[i] = new ClassInfo();
                    _classes[i].read(ps);
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_classes.length);
                for (int i = 0; i < _classes.length; i++) {
                    _classes[i].write(ps);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_classes=[" + _classes.length + "]{");
                for (int i = 0; i < _classes.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("_classes[i]=" + _classes[i]);
                }
                stringBuilder.append("}");
                return stringBuilder.toString();
            }
        }
    }

    public static class InstanceCounts {
        public static final byte COMMAND = 21;
        public abstract static class Handler implements CommandHandler<IncomingRequest, Reply> {
            public IncomingRequest createIncomingDataObject() { return new IncomingRequest(); }
            public int helpAtDecodingUntaggedValue(IncomingRequest incomingRequest) throws JDWPException {assert false : "If this method can be called, it must be overwritten in subclasses!"; return 0; }
            public Reply handle(IncomingRequest incomingRequest, JDWPSender replyChannel) throws JDWPException { return handle(incomingRequest); }
            public Reply handle(IncomingRequest incomingRequest) throws JDWPException { throw new JDWPNotImplementedException(); }
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }
        }

        public static class IncomingRequest implements IncomingData {
            public ID.ReferenceTypeID[] _refTypesCount;
            public IncomingRequest(ID.ReferenceTypeID[] refTypesCount) {
                this._refTypesCount = refTypesCount;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                final int refTypesCountCount = ps.readInt();
                _refTypesCount = new ID.ReferenceTypeID[refTypesCountCount];
                for (int i = 0; i < refTypesCountCount; i++) {
                    _refTypesCount[i] = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_refTypesCount.length);
                for (int i = 0; i < _refTypesCount.length; i++) {
                    _refTypesCount[i].write(ps.getOutputStream());
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_refTypesCount=[" + _refTypesCount.length + "]{");
                for (int i = 0; i < _refTypesCount.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("_refTypesCount[i]=" + _refTypesCount[i]);
                }
                stringBuilder.append("}");
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public long[] _counts;
            public Reply(long[] counts) {
                this._counts = counts;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                final int countsCount = ps.readInt();
                _counts = new long[countsCount];
                for (int i = 0; i < countsCount; i++) {
                    _counts[i] = ps.readLong();
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_counts.length);
                for (int i = 0; i < _counts.length; i++) {
                    ps.write(_counts[i]);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_counts=[" + _counts.length + "]{");
                for (int i = 0; i < _counts.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("_counts[i]=" + _counts[i]);
                }
                stringBuilder.append("}");
                return stringBuilder.toString();
            }
        }
    }
}

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
/*VCSID=2a0a482a-f530-4c98-8f81-81884b42776e*/

// WARNING: Generated file, do not modify!

// To use the original JDWP specification as a basis, some styleguides have to be turned off
// Checkstyle: stop field name check

package com.sun.max.jdwp.protocol;
import com.sun.max.jdwp.data.*;
import com.sun.max.jdwp.constants.*;
@SuppressWarnings("unused")

public final class MethodCommands {
    public static final int COMMAND_SET = 6;
    private MethodCommands() { }  // hide constructor

    public static class LineTable {
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
            public ID.ReferenceTypeID _refType;

            public ID.MethodID _methodID;
            public IncomingRequest(ID.ReferenceTypeID refType,
                ID.MethodID methodID) {
                this._refType = refType;
                this._methodID = methodID;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _refType = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
                _methodID = ID.read(ps.getInputStream(), ID.MethodID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _refType.write(ps.getOutputStream());
                _methodID.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_refType=" + _refType);
                stringBuilder.append(", ");
                stringBuilder.append("_methodID=" + _methodID);
                return stringBuilder.toString();
            }
        }

        public static class LineInfo {

            public long _lineCodeIndex;

            public int _lineNumber;
            public LineInfo(long lineCodeIndex,
                int lineNumber) {
                this._lineCodeIndex = lineCodeIndex;
                this._lineNumber = lineNumber;
            }
            public LineInfo() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _lineCodeIndex = ps.readLong();
                _lineNumber = ps.readInt();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_lineCodeIndex);
                ps.write(_lineNumber);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_lineCodeIndex=" + _lineCodeIndex);
                stringBuilder.append(", ");
                stringBuilder.append("_lineNumber=" + _lineNumber);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public long _start;

            public long _end;

            public LineInfo[] _lines;
            public Reply(long start,
                long end,
                LineInfo[] lines) {
                this._start = start;
                this._end = end;
                this._lines = lines;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _start = ps.readLong();
                _end = ps.readLong();
                final int linesCount = ps.readInt();
                _lines = new LineInfo[linesCount];
                for (int i = 0; i < linesCount; i++) {
                    _lines[i] = new LineInfo();
                    _lines[i].read(ps);
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_start);
                ps.write(_end);
                ps.write(_lines.length);
                for (int i = 0; i < _lines.length; i++) {
                    _lines[i].write(ps);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_start=" + _start);
                stringBuilder.append(", ");
                stringBuilder.append("_end=" + _end);
                stringBuilder.append(", ");
                stringBuilder.append("_lines=[" + _lines.length + "]{");
                for (int i = 0; i < _lines.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("_lines[i]=" + _lines[i]);
                }
                stringBuilder.append("}");
                return stringBuilder.toString();
            }
        }
    }

    public static class VariableTable {
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
            public ID.ReferenceTypeID _refType;

            public ID.MethodID _methodID;
            public IncomingRequest(ID.ReferenceTypeID refType,
                ID.MethodID methodID) {
                this._refType = refType;
                this._methodID = methodID;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _refType = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
                _methodID = ID.read(ps.getInputStream(), ID.MethodID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _refType.write(ps.getOutputStream());
                _methodID.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_refType=" + _refType);
                stringBuilder.append(", ");
                stringBuilder.append("_methodID=" + _methodID);
                return stringBuilder.toString();
            }
        }

        public static class SlotInfo {

            public long _codeIndex;

            public String _name;

            public String _signature;

            public int _length;

            public int _slot;
            public SlotInfo(long codeIndex,
                String name,
                String signature,
                int length,
                int slot) {
                this._codeIndex = codeIndex;
                this._name = name;
                this._signature = signature;
                this._length = length;
                this._slot = slot;
            }
            public SlotInfo() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _codeIndex = ps.readLong();
                _name = ps.readString();
                _signature = ps.readString();
                _length = ps.readInt();
                _slot = ps.readInt();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_codeIndex);
                ps.write(_name);
                ps.write(_signature);
                ps.write(_length);
                ps.write(_slot);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_codeIndex=" + _codeIndex);
                stringBuilder.append(", ");
                stringBuilder.append("_name=" + _name);
                stringBuilder.append(", ");
                stringBuilder.append("_signature=" + _signature);
                stringBuilder.append(", ");
                stringBuilder.append("_length=" + _length);
                stringBuilder.append(", ");
                stringBuilder.append("_slot=" + _slot);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public int _argCnt;

            public SlotInfo[] _slots;
            public Reply(int argCnt,
                SlotInfo[] slots) {
                this._argCnt = argCnt;
                this._slots = slots;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _argCnt = ps.readInt();
                final int slotsCount = ps.readInt();
                _slots = new SlotInfo[slotsCount];
                for (int i = 0; i < slotsCount; i++) {
                    _slots[i] = new SlotInfo();
                    _slots[i].read(ps);
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_argCnt);
                ps.write(_slots.length);
                for (int i = 0; i < _slots.length; i++) {
                    _slots[i].write(ps);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_argCnt=" + _argCnt);
                stringBuilder.append(", ");
                stringBuilder.append("_slots=[" + _slots.length + "]{");
                for (int i = 0; i < _slots.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("_slots[i]=" + _slots[i]);
                }
                stringBuilder.append("}");
                return stringBuilder.toString();
            }
        }
    }

    public static class Bytecodes {
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
            public ID.ReferenceTypeID _refType;

            public ID.MethodID _methodID;
            public IncomingRequest(ID.ReferenceTypeID refType,
                ID.MethodID methodID) {
                this._refType = refType;
                this._methodID = methodID;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _refType = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
                _methodID = ID.read(ps.getInputStream(), ID.MethodID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _refType.write(ps.getOutputStream());
                _methodID.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_refType=" + _refType);
                stringBuilder.append(", ");
                stringBuilder.append("_methodID=" + _methodID);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public byte[] _bytes;
            public Reply(byte[] bytes) {
                this._bytes = bytes;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                final int bytesCount = ps.readInt();
                _bytes = new byte[bytesCount];
                for (int i = 0; i < bytesCount; i++) {
                    _bytes[i] = ps.readByte();
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_bytes.length);
                for (int i = 0; i < _bytes.length; i++) {
                    ps.write(_bytes[i]);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_bytes=[" + _bytes.length + "]{");
                for (int i = 0; i < _bytes.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("_bytes[i]=" + _bytes[i]);
                }
                stringBuilder.append("}");
                return stringBuilder.toString();
            }
        }
    }

    public static class IsObsolete {
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
            public ID.ReferenceTypeID _refType;

            public ID.MethodID _methodID;
            public IncomingRequest(ID.ReferenceTypeID refType,
                ID.MethodID methodID) {
                this._refType = refType;
                this._methodID = methodID;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _refType = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
                _methodID = ID.read(ps.getInputStream(), ID.MethodID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _refType.write(ps.getOutputStream());
                _methodID.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_refType=" + _refType);
                stringBuilder.append(", ");
                stringBuilder.append("_methodID=" + _methodID);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public boolean _isObsolete;
            public Reply(boolean isObsolete) {
                this._isObsolete = isObsolete;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _isObsolete = ps.readBoolean();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_isObsolete);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_isObsolete=" + _isObsolete);
                return stringBuilder.toString();
            }
        }
    }

    public static class VariableTableWithGeneric {
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
            public ID.ReferenceTypeID _refType;

            public ID.MethodID _methodID;
            public IncomingRequest(ID.ReferenceTypeID refType,
                ID.MethodID methodID) {
                this._refType = refType;
                this._methodID = methodID;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _refType = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
                _methodID = ID.read(ps.getInputStream(), ID.MethodID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _refType.write(ps.getOutputStream());
                _methodID.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_refType=" + _refType);
                stringBuilder.append(", ");
                stringBuilder.append("_methodID=" + _methodID);
                return stringBuilder.toString();
            }
        }

        public static class SlotInfo {

            public long _codeIndex;

            public String _name;

            public String _signature;

            public String _genericSignature;

            public int _length;

            public int _slot;
            public SlotInfo(long codeIndex,
                String name,
                String signature,
                String genericSignature,
                int length,
                int slot) {
                this._codeIndex = codeIndex;
                this._name = name;
                this._signature = signature;
                this._genericSignature = genericSignature;
                this._length = length;
                this._slot = slot;
            }
            public SlotInfo() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _codeIndex = ps.readLong();
                _name = ps.readString();
                _signature = ps.readString();
                _genericSignature = ps.readString();
                _length = ps.readInt();
                _slot = ps.readInt();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_codeIndex);
                ps.write(_name);
                ps.write(_signature);
                ps.write(_genericSignature);
                ps.write(_length);
                ps.write(_slot);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_codeIndex=" + _codeIndex);
                stringBuilder.append(", ");
                stringBuilder.append("_name=" + _name);
                stringBuilder.append(", ");
                stringBuilder.append("_signature=" + _signature);
                stringBuilder.append(", ");
                stringBuilder.append("_genericSignature=" + _genericSignature);
                stringBuilder.append(", ");
                stringBuilder.append("_length=" + _length);
                stringBuilder.append(", ");
                stringBuilder.append("_slot=" + _slot);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public int _argCnt;

            public SlotInfo[] _slots;
            public Reply(int argCnt,
                SlotInfo[] slots) {
                this._argCnt = argCnt;
                this._slots = slots;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _argCnt = ps.readInt();
                final int slotsCount = ps.readInt();
                _slots = new SlotInfo[slotsCount];
                for (int i = 0; i < slotsCount; i++) {
                    _slots[i] = new SlotInfo();
                    _slots[i].read(ps);
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_argCnt);
                ps.write(_slots.length);
                for (int i = 0; i < _slots.length; i++) {
                    _slots[i].write(ps);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_argCnt=" + _argCnt);
                stringBuilder.append(", ");
                stringBuilder.append("_slots=[" + _slots.length + "]{");
                for (int i = 0; i < _slots.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("_slots[i]=" + _slots[i]);
                }
                stringBuilder.append("}");
                return stringBuilder.toString();
            }
        }
    }
}

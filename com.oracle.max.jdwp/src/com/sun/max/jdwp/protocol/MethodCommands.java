/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
            public ID.ReferenceTypeID refType;

            public ID.MethodID methodID;
            public IncomingRequest(ID.ReferenceTypeID refType,
                ID.MethodID methodID) {
                this.refType = refType;
                this.methodID = methodID;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                refType = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
                methodID = ID.read(ps.getInputStream(), ID.MethodID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                refType.write(ps.getOutputStream());
                methodID.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("refType=" + refType);
                stringBuilder.append(", ");
                stringBuilder.append("methodID=" + methodID);
                return stringBuilder.toString();
            }
        }

        public static class LineInfo {

            public long lineCodeIndex;

            public int lineNumber;
            public LineInfo(long lineCodeIndex,
                int lineNumber) {
                this.lineCodeIndex = lineCodeIndex;
                this.lineNumber = lineNumber;
            }
            public LineInfo() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                lineCodeIndex = ps.readLong();
                lineNumber = ps.readInt();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(lineCodeIndex);
                ps.write(lineNumber);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("lineCodeIndex=" + lineCodeIndex);
                stringBuilder.append(", ");
                stringBuilder.append("lineNumber=" + lineNumber);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public long start;

            public long end;

            public LineInfo[] lines;
            public Reply(long start,
                long end,
                LineInfo[] lines) {
                this.start = start;
                this.end = end;
                this.lines = lines;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                start = ps.readLong();
                end = ps.readLong();
                final int linesCount = ps.readInt();
                lines = new LineInfo[linesCount];
                for (int i = 0; i < linesCount; i++) {
                    lines[i] = new LineInfo();
                    lines[i].read(ps);
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(start);
                ps.write(end);
                ps.write(lines.length);
                for (int i = 0; i < lines.length; i++) {
                    lines[i].write(ps);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("start=" + start);
                stringBuilder.append(", ");
                stringBuilder.append("end=" + end);
                stringBuilder.append(", ");
                stringBuilder.append("lines=[" + lines.length + "]{");
                for (int i = 0; i < lines.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("lines[i]=" + lines[i]);
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
            public ID.ReferenceTypeID refType;

            public ID.MethodID methodID;
            public IncomingRequest(ID.ReferenceTypeID refType,
                ID.MethodID methodID) {
                this.refType = refType;
                this.methodID = methodID;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                refType = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
                methodID = ID.read(ps.getInputStream(), ID.MethodID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                refType.write(ps.getOutputStream());
                methodID.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("refType=" + refType);
                stringBuilder.append(", ");
                stringBuilder.append("methodID=" + methodID);
                return stringBuilder.toString();
            }
        }

        public static class SlotInfo {

            public long codeIndex;

            public String name;

            public String signature;

            public int length;

            public int slot;
            public SlotInfo(long codeIndex,
                String name,
                String signature,
                int length,
                int slot) {
                this.codeIndex = codeIndex;
                this.name = name;
                this.signature = signature;
                this.length = length;
                this.slot = slot;
            }
            public SlotInfo() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                codeIndex = ps.readLong();
                name = ps.readString();
                signature = ps.readString();
                length = ps.readInt();
                slot = ps.readInt();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(codeIndex);
                ps.write(name);
                ps.write(signature);
                ps.write(length);
                ps.write(slot);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("codeIndex=" + codeIndex);
                stringBuilder.append(", ");
                stringBuilder.append("name=" + name);
                stringBuilder.append(", ");
                stringBuilder.append("signature=" + signature);
                stringBuilder.append(", ");
                stringBuilder.append("length=" + length);
                stringBuilder.append(", ");
                stringBuilder.append("slot=" + slot);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public int argCnt;

            public SlotInfo[] slots;
            public Reply(int argCnt,
                SlotInfo[] slots) {
                this.argCnt = argCnt;
                this.slots = slots;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                argCnt = ps.readInt();
                final int slotsCount = ps.readInt();
                slots = new SlotInfo[slotsCount];
                for (int i = 0; i < slotsCount; i++) {
                    slots[i] = new SlotInfo();
                    slots[i].read(ps);
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(argCnt);
                ps.write(slots.length);
                for (int i = 0; i < slots.length; i++) {
                    slots[i].write(ps);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("argCnt=" + argCnt);
                stringBuilder.append(", ");
                stringBuilder.append("slots=[" + slots.length + "]{");
                for (int i = 0; i < slots.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("slots[i]=" + slots[i]);
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
            public ID.ReferenceTypeID refType;

            public ID.MethodID methodID;
            public IncomingRequest(ID.ReferenceTypeID refType,
                ID.MethodID methodID) {
                this.refType = refType;
                this.methodID = methodID;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                refType = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
                methodID = ID.read(ps.getInputStream(), ID.MethodID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                refType.write(ps.getOutputStream());
                methodID.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("refType=" + refType);
                stringBuilder.append(", ");
                stringBuilder.append("methodID=" + methodID);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public byte[] bytes;
            public Reply(byte[] bytes) {
                this.bytes = bytes;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                final int bytesCount = ps.readInt();
                bytes = new byte[bytesCount];
                for (int i = 0; i < bytesCount; i++) {
                    bytes[i] = ps.readByte();
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(bytes.length);
                for (int i = 0; i < bytes.length; i++) {
                    ps.write(bytes[i]);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("bytes=[" + bytes.length + "]{");
                for (int i = 0; i < bytes.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("bytes[i]=" + bytes[i]);
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
            public ID.ReferenceTypeID refType;

            public ID.MethodID methodID;
            public IncomingRequest(ID.ReferenceTypeID refType,
                ID.MethodID methodID) {
                this.refType = refType;
                this.methodID = methodID;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                refType = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
                methodID = ID.read(ps.getInputStream(), ID.MethodID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                refType.write(ps.getOutputStream());
                methodID.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("refType=" + refType);
                stringBuilder.append(", ");
                stringBuilder.append("methodID=" + methodID);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public boolean isObsolete;
            public Reply(boolean isObsolete) {
                this.isObsolete = isObsolete;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                isObsolete = ps.readBoolean();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(isObsolete);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("isObsolete=" + isObsolete);
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
            public ID.ReferenceTypeID refType;

            public ID.MethodID methodID;
            public IncomingRequest(ID.ReferenceTypeID refType,
                ID.MethodID methodID) {
                this.refType = refType;
                this.methodID = methodID;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                refType = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
                methodID = ID.read(ps.getInputStream(), ID.MethodID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                refType.write(ps.getOutputStream());
                methodID.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("refType=" + refType);
                stringBuilder.append(", ");
                stringBuilder.append("methodID=" + methodID);
                return stringBuilder.toString();
            }
        }

        public static class SlotInfo {

            public long codeIndex;

            public String name;

            public String signature;

            public String genericSignature;

            public int length;

            public int slot;
            public SlotInfo(long codeIndex,
                String name,
                String signature,
                String genericSignature,
                int length,
                int slot) {
                this.codeIndex = codeIndex;
                this.name = name;
                this.signature = signature;
                this.genericSignature = genericSignature;
                this.length = length;
                this.slot = slot;
            }
            public SlotInfo() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                codeIndex = ps.readLong();
                name = ps.readString();
                signature = ps.readString();
                genericSignature = ps.readString();
                length = ps.readInt();
                slot = ps.readInt();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(codeIndex);
                ps.write(name);
                ps.write(signature);
                ps.write(genericSignature);
                ps.write(length);
                ps.write(slot);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("codeIndex=" + codeIndex);
                stringBuilder.append(", ");
                stringBuilder.append("name=" + name);
                stringBuilder.append(", ");
                stringBuilder.append("signature=" + signature);
                stringBuilder.append(", ");
                stringBuilder.append("genericSignature=" + genericSignature);
                stringBuilder.append(", ");
                stringBuilder.append("length=" + length);
                stringBuilder.append(", ");
                stringBuilder.append("slot=" + slot);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public int argCnt;

            public SlotInfo[] slots;
            public Reply(int argCnt,
                SlotInfo[] slots) {
                this.argCnt = argCnt;
                this.slots = slots;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                argCnt = ps.readInt();
                final int slotsCount = ps.readInt();
                slots = new SlotInfo[slotsCount];
                for (int i = 0; i < slotsCount; i++) {
                    slots[i] = new SlotInfo();
                    slots[i].read(ps);
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(argCnt);
                ps.write(slots.length);
                for (int i = 0; i < slots.length; i++) {
                    slots[i].write(ps);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("argCnt=" + argCnt);
                stringBuilder.append(", ");
                stringBuilder.append("slots=[" + slots.length + "]{");
                for (int i = 0; i < slots.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("slots[i]=" + slots[i]);
                }
                stringBuilder.append("}");
                return stringBuilder.toString();
            }
        }
    }
}

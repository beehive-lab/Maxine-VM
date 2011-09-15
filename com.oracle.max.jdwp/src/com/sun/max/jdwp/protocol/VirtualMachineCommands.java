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

            public String description;

            public int jdwpMajor;

            public int jdwpMinor;

            public String vmVersion;

            public String vmName;
            public Reply(String description,
                int jdwpMajor,
                int jdwpMinor,
                String vmVersion,
                String vmName) {
                this.description = description;
                this.jdwpMajor = jdwpMajor;
                this.jdwpMinor = jdwpMinor;
                this.vmVersion = vmVersion;
                this.vmName = vmName;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                description = ps.readString();
                jdwpMajor = ps.readInt();
                jdwpMinor = ps.readInt();
                vmVersion = ps.readString();
                vmName = ps.readString();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(description);
                ps.write(jdwpMajor);
                ps.write(jdwpMinor);
                ps.write(vmVersion);
                ps.write(vmName);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("description=" + description);
                stringBuilder.append(", ");
                stringBuilder.append("jdwpMajor=" + jdwpMajor);
                stringBuilder.append(", ");
                stringBuilder.append("jdwpMinor=" + jdwpMinor);
                stringBuilder.append(", ");
                stringBuilder.append("vmVersion=" + vmVersion);
                stringBuilder.append(", ");
                stringBuilder.append("vmName=" + vmName);
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
            public String signature;
            public IncomingRequest(String signature) {
                this.signature = signature;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                signature = ps.readString();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(signature);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("signature=" + signature);
                return stringBuilder.toString();
            }
        }

        public static class ClassInfo {

            public byte refTypeTag;

            public ID.ReferenceTypeID typeID;

            public int status;
            public ClassInfo(byte refTypeTag,
                ID.ReferenceTypeID typeID,
                int status) {
                this.refTypeTag = refTypeTag;
                this.typeID = typeID;
                this.status = status;
            }
            public ClassInfo() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                refTypeTag = ps.readByte();
                typeID = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
                status = ps.readInt();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(refTypeTag);
                typeID.write(ps.getOutputStream());
                ps.write(status);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("refTypeTag=" + refTypeTag);
                stringBuilder.append(", ");
                stringBuilder.append("typeID=" + typeID);
                stringBuilder.append(", ");
                stringBuilder.append("status=" + status);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public ClassInfo[] classes;
            public Reply(ClassInfo[] classes) {
                this.classes = classes;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                final int classesCount = ps.readInt();
                classes = new ClassInfo[classesCount];
                for (int i = 0; i < classesCount; i++) {
                    classes[i] = new ClassInfo();
                    classes[i].read(ps);
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(classes.length);
                for (int i = 0; i < classes.length; i++) {
                    classes[i].write(ps);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("classes=[" + classes.length + "]{");
                for (int i = 0; i < classes.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("classes[i]=" + classes[i]);
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

            public byte refTypeTag;

            public ID.ReferenceTypeID typeID;

            public String signature;

            public int status;
            public ClassInfo(byte refTypeTag,
                ID.ReferenceTypeID typeID,
                String signature,
                int status) {
                this.refTypeTag = refTypeTag;
                this.typeID = typeID;
                this.signature = signature;
                this.status = status;
            }
            public ClassInfo() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                refTypeTag = ps.readByte();
                typeID = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
                signature = ps.readString();
                status = ps.readInt();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(refTypeTag);
                typeID.write(ps.getOutputStream());
                ps.write(signature);
                ps.write(status);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
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

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public ClassInfo[] classes;
            public Reply(ClassInfo[] classes) {
                this.classes = classes;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                final int classesCount = ps.readInt();
                classes = new ClassInfo[classesCount];
                for (int i = 0; i < classesCount; i++) {
                    classes[i] = new ClassInfo();
                    classes[i].read(ps);
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(classes.length);
                for (int i = 0; i < classes.length; i++) {
                    classes[i].write(ps);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("classes=[" + classes.length + "]{");
                for (int i = 0; i < classes.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("classes[i]=" + classes[i]);
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

            public ID.ThreadID[] threads;
            public Reply(ID.ThreadID[] threads) {
                this.threads = threads;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                final int threadsCount = ps.readInt();
                threads = new ID.ThreadID[threadsCount];
                for (int i = 0; i < threadsCount; i++) {
                    threads[i] = ID.read(ps.getInputStream(), ID.ThreadID.class);
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(threads.length);
                for (int i = 0; i < threads.length; i++) {
                    threads[i].write(ps.getOutputStream());
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("threads=[" + threads.length + "]{");
                for (int i = 0; i < threads.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("threads[i]=" + threads[i]);
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

            public ID.ThreadGroupID[] groups;
            public Reply(ID.ThreadGroupID[] groups) {
                this.groups = groups;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                final int groupsCount = ps.readInt();
                groups = new ID.ThreadGroupID[groupsCount];
                for (int i = 0; i < groupsCount; i++) {
                    groups[i] = ID.read(ps.getInputStream(), ID.ThreadGroupID.class);
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(groups.length);
                for (int i = 0; i < groups.length; i++) {
                    groups[i].write(ps.getOutputStream());
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("groups=[" + groups.length + "]{");
                for (int i = 0; i < groups.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("groups[i]=" + groups[i]);
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

            public int fieldIDSize;

            public int methodIDSize;

            public int objectIDSize;

            public int referenceTypeIDSize;

            public int frameIDSize;
            public Reply(int fieldIDSize,
                int methodIDSize,
                int objectIDSize,
                int referenceTypeIDSize,
                int frameIDSize) {
                this.fieldIDSize = fieldIDSize;
                this.methodIDSize = methodIDSize;
                this.objectIDSize = objectIDSize;
                this.referenceTypeIDSize = referenceTypeIDSize;
                this.frameIDSize = frameIDSize;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                fieldIDSize = ps.readInt();
                methodIDSize = ps.readInt();
                objectIDSize = ps.readInt();
                referenceTypeIDSize = ps.readInt();
                frameIDSize = ps.readInt();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(fieldIDSize);
                ps.write(methodIDSize);
                ps.write(objectIDSize);
                ps.write(referenceTypeIDSize);
                ps.write(frameIDSize);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("fieldIDSize=" + fieldIDSize);
                stringBuilder.append(", ");
                stringBuilder.append("methodIDSize=" + methodIDSize);
                stringBuilder.append(", ");
                stringBuilder.append("objectIDSize=" + objectIDSize);
                stringBuilder.append(", ");
                stringBuilder.append("referenceTypeIDSize=" + referenceTypeIDSize);
                stringBuilder.append(", ");
                stringBuilder.append("frameIDSize=" + frameIDSize);
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
            public int exitCode;
            public IncomingRequest(int exitCode) {
                this.exitCode = exitCode;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                exitCode = ps.readInt();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(exitCode);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("exitCode=" + exitCode);
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
            public String utf;
            public IncomingRequest(String utf) {
                this.utf = utf;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                utf = ps.readString();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(utf);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("utf=" + utf);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public ID.StringID stringObject;
            public Reply(ID.StringID stringObject) {
                this.stringObject = stringObject;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                stringObject = ID.read(ps.getInputStream(), ID.StringID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                stringObject.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("stringObject=" + stringObject);
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

            public boolean canWatchFieldModification;

            public boolean canWatchFieldAccess;

            public boolean canGetBytecodes;

            public boolean canGetSyntheticAttribute;

            public boolean canGetOwnedMonitorInfo;

            public boolean canGetCurrentContendedMonitor;

            public boolean canGetMonitorInfo;
            public Reply(boolean canWatchFieldModification,
                boolean canWatchFieldAccess,
                boolean canGetBytecodes,
                boolean canGetSyntheticAttribute,
                boolean canGetOwnedMonitorInfo,
                boolean canGetCurrentContendedMonitor,
                boolean canGetMonitorInfo) {
                this.canWatchFieldModification = canWatchFieldModification;
                this.canWatchFieldAccess = canWatchFieldAccess;
                this.canGetBytecodes = canGetBytecodes;
                this.canGetSyntheticAttribute = canGetSyntheticAttribute;
                this.canGetOwnedMonitorInfo = canGetOwnedMonitorInfo;
                this.canGetCurrentContendedMonitor = canGetCurrentContendedMonitor;
                this.canGetMonitorInfo = canGetMonitorInfo;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                canWatchFieldModification = ps.readBoolean();
                canWatchFieldAccess = ps.readBoolean();
                canGetBytecodes = ps.readBoolean();
                canGetSyntheticAttribute = ps.readBoolean();
                canGetOwnedMonitorInfo = ps.readBoolean();
                canGetCurrentContendedMonitor = ps.readBoolean();
                canGetMonitorInfo = ps.readBoolean();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(canWatchFieldModification);
                ps.write(canWatchFieldAccess);
                ps.write(canGetBytecodes);
                ps.write(canGetSyntheticAttribute);
                ps.write(canGetOwnedMonitorInfo);
                ps.write(canGetCurrentContendedMonitor);
                ps.write(canGetMonitorInfo);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("canWatchFieldModification=" + canWatchFieldModification);
                stringBuilder.append(", ");
                stringBuilder.append("canWatchFieldAccess=" + canWatchFieldAccess);
                stringBuilder.append(", ");
                stringBuilder.append("canGetBytecodes=" + canGetBytecodes);
                stringBuilder.append(", ");
                stringBuilder.append("canGetSyntheticAttribute=" + canGetSyntheticAttribute);
                stringBuilder.append(", ");
                stringBuilder.append("canGetOwnedMonitorInfo=" + canGetOwnedMonitorInfo);
                stringBuilder.append(", ");
                stringBuilder.append("canGetCurrentContendedMonitor=" + canGetCurrentContendedMonitor);
                stringBuilder.append(", ");
                stringBuilder.append("canGetMonitorInfo=" + canGetMonitorInfo);
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

            public String baseDir;

            public String[] classpaths;

            public String[] bootclasspaths;
            public Reply(String baseDir,
                String[] classpaths,
                String[] bootclasspaths) {
                this.baseDir = baseDir;
                this.classpaths = classpaths;
                this.bootclasspaths = bootclasspaths;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                baseDir = ps.readString();
                final int classpathsCount = ps.readInt();
                classpaths = new String[classpathsCount];
                for (int i = 0; i < classpathsCount; i++) {
                    classpaths[i] = ps.readString();
                }
                final int bootclasspathsCount = ps.readInt();
                bootclasspaths = new String[bootclasspathsCount];
                for (int i = 0; i < bootclasspathsCount; i++) {
                    bootclasspaths[i] = ps.readString();
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(baseDir);
                ps.write(classpaths.length);
                for (int i = 0; i < classpaths.length; i++) {
                    ps.write(classpaths[i]);
                }
                ps.write(bootclasspaths.length);
                for (int i = 0; i < bootclasspaths.length; i++) {
                    ps.write(bootclasspaths[i]);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("baseDir=" + baseDir);
                stringBuilder.append(", ");
                stringBuilder.append("classpaths=[" + classpaths.length + "]{");
                for (int i = 0; i < classpaths.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("classpaths[i]=" + classpaths[i]);
                }
                stringBuilder.append("}");
                stringBuilder.append(", ");
                stringBuilder.append("bootclasspaths=[" + bootclasspaths.length + "]{");
                for (int i = 0; i < bootclasspaths.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("bootclasspaths[i]=" + bootclasspaths[i]);
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

            public ID.ObjectID object;

            public int refCnt;
            public Request(ID.ObjectID object,
                int refCnt) {
                this.object = object;
                this.refCnt = refCnt;
            }
            public Request() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                object = ID.read(ps.getInputStream(), ID.ObjectID.class);
                refCnt = ps.readInt();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                object.write(ps.getOutputStream());
                ps.write(refCnt);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("object=" + object);
                stringBuilder.append(", ");
                stringBuilder.append("refCnt=" + refCnt);
                return stringBuilder.toString();
            }
        }

        public static class IncomingRequest implements IncomingData {
            public Request[] requests;
            public IncomingRequest(Request[] requests) {
                this.requests = requests;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                final int requestsCount = ps.readInt();
                requests = new Request[requestsCount];
                for (int i = 0; i < requestsCount; i++) {
                    requests[i] = new Request();
                    requests[i].read(ps);
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(requests.length);
                for (int i = 0; i < requests.length; i++) {
                    requests[i].write(ps);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("requests=[" + requests.length + "]{");
                for (int i = 0; i < requests.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("requests[i]=" + requests[i]);
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

            public boolean canWatchFieldModification;

            public boolean canWatchFieldAccess;

            public boolean canGetBytecodes;

            public boolean canGetSyntheticAttribute;

            public boolean canGetOwnedMonitorInfo;

            public boolean canGetCurrentContendedMonitor;

            public boolean canGetMonitorInfo;

            public boolean canRedefineClasses;

            public boolean canAddMethod;

            public boolean canUnrestrictedlyRedefineClasses;

            public boolean canPopFrames;

            public boolean canUseInstanceFilters;

            public boolean canGetSourceDebugExtension;

            public boolean canRequestVMDeathEvent;

            public boolean canSetDefaultStratum;

            public boolean canGetInstanceInfo;

            public boolean canRequestMonitorEvents;

            public boolean canGetMonitorFrameInfo;

            public boolean canUseSourceNameFilters;

            public boolean canGetConstantPool;

            public boolean canForceEarlyReturn;

            public boolean reserved22;

            public boolean reserved23;

            public boolean reserved24;

            public boolean reserved25;

            public boolean reserved26;

            public boolean reserved27;

            public boolean reserved28;

            public boolean reserved29;

            public boolean reserved30;

            public boolean reserved31;

            public boolean reserved32;
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
                this.canWatchFieldModification = canWatchFieldModification;
                this.canWatchFieldAccess = canWatchFieldAccess;
                this.canGetBytecodes = canGetBytecodes;
                this.canGetSyntheticAttribute = canGetSyntheticAttribute;
                this.canGetOwnedMonitorInfo = canGetOwnedMonitorInfo;
                this.canGetCurrentContendedMonitor = canGetCurrentContendedMonitor;
                this.canGetMonitorInfo = canGetMonitorInfo;
                this.canRedefineClasses = canRedefineClasses;
                this.canAddMethod = canAddMethod;
                this.canUnrestrictedlyRedefineClasses = canUnrestrictedlyRedefineClasses;
                this.canPopFrames = canPopFrames;
                this.canUseInstanceFilters = canUseInstanceFilters;
                this.canGetSourceDebugExtension = canGetSourceDebugExtension;
                this.canRequestVMDeathEvent = canRequestVMDeathEvent;
                this.canSetDefaultStratum = canSetDefaultStratum;
                this.canGetInstanceInfo = canGetInstanceInfo;
                this.canRequestMonitorEvents = canRequestMonitorEvents;
                this.canGetMonitorFrameInfo = canGetMonitorFrameInfo;
                this.canUseSourceNameFilters = canUseSourceNameFilters;
                this.canGetConstantPool = canGetConstantPool;
                this.canForceEarlyReturn = canForceEarlyReturn;
                this.reserved22 = reserved22;
                this.reserved23 = reserved23;
                this.reserved24 = reserved24;
                this.reserved25 = reserved25;
                this.reserved26 = reserved26;
                this.reserved27 = reserved27;
                this.reserved28 = reserved28;
                this.reserved29 = reserved29;
                this.reserved30 = reserved30;
                this.reserved31 = reserved31;
                this.reserved32 = reserved32;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                canWatchFieldModification = ps.readBoolean();
                canWatchFieldAccess = ps.readBoolean();
                canGetBytecodes = ps.readBoolean();
                canGetSyntheticAttribute = ps.readBoolean();
                canGetOwnedMonitorInfo = ps.readBoolean();
                canGetCurrentContendedMonitor = ps.readBoolean();
                canGetMonitorInfo = ps.readBoolean();
                canRedefineClasses = ps.readBoolean();
                canAddMethod = ps.readBoolean();
                canUnrestrictedlyRedefineClasses = ps.readBoolean();
                canPopFrames = ps.readBoolean();
                canUseInstanceFilters = ps.readBoolean();
                canGetSourceDebugExtension = ps.readBoolean();
                canRequestVMDeathEvent = ps.readBoolean();
                canSetDefaultStratum = ps.readBoolean();
                canGetInstanceInfo = ps.readBoolean();
                canRequestMonitorEvents = ps.readBoolean();
                canGetMonitorFrameInfo = ps.readBoolean();
                canUseSourceNameFilters = ps.readBoolean();
                canGetConstantPool = ps.readBoolean();
                canForceEarlyReturn = ps.readBoolean();
                reserved22 = ps.readBoolean();
                reserved23 = ps.readBoolean();
                reserved24 = ps.readBoolean();
                reserved25 = ps.readBoolean();
                reserved26 = ps.readBoolean();
                reserved27 = ps.readBoolean();
                reserved28 = ps.readBoolean();
                reserved29 = ps.readBoolean();
                reserved30 = ps.readBoolean();
                reserved31 = ps.readBoolean();
                reserved32 = ps.readBoolean();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(canWatchFieldModification);
                ps.write(canWatchFieldAccess);
                ps.write(canGetBytecodes);
                ps.write(canGetSyntheticAttribute);
                ps.write(canGetOwnedMonitorInfo);
                ps.write(canGetCurrentContendedMonitor);
                ps.write(canGetMonitorInfo);
                ps.write(canRedefineClasses);
                ps.write(canAddMethod);
                ps.write(canUnrestrictedlyRedefineClasses);
                ps.write(canPopFrames);
                ps.write(canUseInstanceFilters);
                ps.write(canGetSourceDebugExtension);
                ps.write(canRequestVMDeathEvent);
                ps.write(canSetDefaultStratum);
                ps.write(canGetInstanceInfo);
                ps.write(canRequestMonitorEvents);
                ps.write(canGetMonitorFrameInfo);
                ps.write(canUseSourceNameFilters);
                ps.write(canGetConstantPool);
                ps.write(canForceEarlyReturn);
                ps.write(reserved22);
                ps.write(reserved23);
                ps.write(reserved24);
                ps.write(reserved25);
                ps.write(reserved26);
                ps.write(reserved27);
                ps.write(reserved28);
                ps.write(reserved29);
                ps.write(reserved30);
                ps.write(reserved31);
                ps.write(reserved32);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("canWatchFieldModification=" + canWatchFieldModification);
                stringBuilder.append(", ");
                stringBuilder.append("canWatchFieldAccess=" + canWatchFieldAccess);
                stringBuilder.append(", ");
                stringBuilder.append("canGetBytecodes=" + canGetBytecodes);
                stringBuilder.append(", ");
                stringBuilder.append("canGetSyntheticAttribute=" + canGetSyntheticAttribute);
                stringBuilder.append(", ");
                stringBuilder.append("canGetOwnedMonitorInfo=" + canGetOwnedMonitorInfo);
                stringBuilder.append(", ");
                stringBuilder.append("canGetCurrentContendedMonitor=" + canGetCurrentContendedMonitor);
                stringBuilder.append(", ");
                stringBuilder.append("canGetMonitorInfo=" + canGetMonitorInfo);
                stringBuilder.append(", ");
                stringBuilder.append("canRedefineClasses=" + canRedefineClasses);
                stringBuilder.append(", ");
                stringBuilder.append("canAddMethod=" + canAddMethod);
                stringBuilder.append(", ");
                stringBuilder.append("canUnrestrictedlyRedefineClasses=" + canUnrestrictedlyRedefineClasses);
                stringBuilder.append(", ");
                stringBuilder.append("canPopFrames=" + canPopFrames);
                stringBuilder.append(", ");
                stringBuilder.append("canUseInstanceFilters=" + canUseInstanceFilters);
                stringBuilder.append(", ");
                stringBuilder.append("canGetSourceDebugExtension=" + canGetSourceDebugExtension);
                stringBuilder.append(", ");
                stringBuilder.append("canRequestVMDeathEvent=" + canRequestVMDeathEvent);
                stringBuilder.append(", ");
                stringBuilder.append("canSetDefaultStratum=" + canSetDefaultStratum);
                stringBuilder.append(", ");
                stringBuilder.append("canGetInstanceInfo=" + canGetInstanceInfo);
                stringBuilder.append(", ");
                stringBuilder.append("canRequestMonitorEvents=" + canRequestMonitorEvents);
                stringBuilder.append(", ");
                stringBuilder.append("canGetMonitorFrameInfo=" + canGetMonitorFrameInfo);
                stringBuilder.append(", ");
                stringBuilder.append("canUseSourceNameFilters=" + canUseSourceNameFilters);
                stringBuilder.append(", ");
                stringBuilder.append("canGetConstantPool=" + canGetConstantPool);
                stringBuilder.append(", ");
                stringBuilder.append("canForceEarlyReturn=" + canForceEarlyReturn);
                stringBuilder.append(", ");
                stringBuilder.append("reserved22=" + reserved22);
                stringBuilder.append(", ");
                stringBuilder.append("reserved23=" + reserved23);
                stringBuilder.append(", ");
                stringBuilder.append("reserved24=" + reserved24);
                stringBuilder.append(", ");
                stringBuilder.append("reserved25=" + reserved25);
                stringBuilder.append(", ");
                stringBuilder.append("reserved26=" + reserved26);
                stringBuilder.append(", ");
                stringBuilder.append("reserved27=" + reserved27);
                stringBuilder.append(", ");
                stringBuilder.append("reserved28=" + reserved28);
                stringBuilder.append(", ");
                stringBuilder.append("reserved29=" + reserved29);
                stringBuilder.append(", ");
                stringBuilder.append("reserved30=" + reserved30);
                stringBuilder.append(", ");
                stringBuilder.append("reserved31=" + reserved31);
                stringBuilder.append(", ");
                stringBuilder.append("reserved32=" + reserved32);
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

            public ID.ReferenceTypeID refType;

            public byte[] classfile;
            public ClassDef(ID.ReferenceTypeID refType,
                byte[] classfile) {
                this.refType = refType;
                this.classfile = classfile;
            }
            public ClassDef() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                refType = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
                final int classfileCount = ps.readInt();
                classfile = new byte[classfileCount];
                for (int i = 0; i < classfileCount; i++) {
                    classfile[i] = ps.readByte();
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                refType.write(ps.getOutputStream());
                ps.write(classfile.length);
                for (int i = 0; i < classfile.length; i++) {
                    ps.write(classfile[i]);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("refType=" + refType);
                stringBuilder.append(", ");
                stringBuilder.append("classfile=[" + classfile.length + "]{");
                for (int i = 0; i < classfile.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("classfile[i]=" + classfile[i]);
                }
                stringBuilder.append("}");
                return stringBuilder.toString();
            }
        }

        public static class IncomingRequest implements IncomingData {
            public ClassDef[] classes;
            public IncomingRequest(ClassDef[] classes) {
                this.classes = classes;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                final int classesCount = ps.readInt();
                classes = new ClassDef[classesCount];
                for (int i = 0; i < classesCount; i++) {
                    classes[i] = new ClassDef();
                    classes[i].read(ps);
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(classes.length);
                for (int i = 0; i < classes.length; i++) {
                    classes[i].write(ps);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("classes=[" + classes.length + "]{");
                for (int i = 0; i < classes.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("classes[i]=" + classes[i]);
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
            public String stratumID;
            public IncomingRequest(String stratumID) {
                this.stratumID = stratumID;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                stratumID = ps.readString();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(stratumID);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("stratumID=" + stratumID);
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

            public byte refTypeTag;

            public ID.ReferenceTypeID typeID;

            public String signature;

            public String genericSignature;

            public int status;
            public ClassInfo(byte refTypeTag,
                ID.ReferenceTypeID typeID,
                String signature,
                String genericSignature,
                int status) {
                this.refTypeTag = refTypeTag;
                this.typeID = typeID;
                this.signature = signature;
                this.genericSignature = genericSignature;
                this.status = status;
            }
            public ClassInfo() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                refTypeTag = ps.readByte();
                typeID = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
                signature = ps.readString();
                genericSignature = ps.readString();
                status = ps.readInt();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(refTypeTag);
                typeID.write(ps.getOutputStream());
                ps.write(signature);
                ps.write(genericSignature);
                ps.write(status);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("refTypeTag=" + refTypeTag);
                stringBuilder.append(", ");
                stringBuilder.append("typeID=" + typeID);
                stringBuilder.append(", ");
                stringBuilder.append("signature=" + signature);
                stringBuilder.append(", ");
                stringBuilder.append("genericSignature=" + genericSignature);
                stringBuilder.append(", ");
                stringBuilder.append("status=" + status);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public ClassInfo[] classes;
            public Reply(ClassInfo[] classes) {
                this.classes = classes;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                final int classesCount = ps.readInt();
                classes = new ClassInfo[classesCount];
                for (int i = 0; i < classesCount; i++) {
                    classes[i] = new ClassInfo();
                    classes[i].read(ps);
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(classes.length);
                for (int i = 0; i < classes.length; i++) {
                    classes[i].write(ps);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("classes=[" + classes.length + "]{");
                for (int i = 0; i < classes.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("classes[i]=" + classes[i]);
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
            public ID.ReferenceTypeID[] refTypesCount;
            public IncomingRequest(ID.ReferenceTypeID[] refTypesCount) {
                this.refTypesCount = refTypesCount;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                final int refTypesCountCount = ps.readInt();
                refTypesCount = new ID.ReferenceTypeID[refTypesCountCount];
                for (int i = 0; i < refTypesCountCount; i++) {
                    refTypesCount[i] = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(refTypesCount.length);
                for (int i = 0; i < refTypesCount.length; i++) {
                    refTypesCount[i].write(ps.getOutputStream());
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("refTypesCount=[" + refTypesCount.length + "]{");
                for (int i = 0; i < refTypesCount.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("refTypesCount[i]=" + refTypesCount[i]);
                }
                stringBuilder.append("}");
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public long[] counts;
            public Reply(long[] counts) {
                this.counts = counts;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                final int countsCount = ps.readInt();
                counts = new long[countsCount];
                for (int i = 0; i < countsCount; i++) {
                    counts[i] = ps.readLong();
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(counts.length);
                for (int i = 0; i < counts.length; i++) {
                    ps.write(counts[i]);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("counts=[" + counts.length + "]{");
                for (int i = 0; i < counts.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("counts[i]=" + counts[i]);
                }
                stringBuilder.append("}");
                return stringBuilder.toString();
            }
        }
    }
}

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

public final class ThreadGroupReferenceCommands {
    public static final int COMMAND_SET = 12;
    private ThreadGroupReferenceCommands() { }  // hide constructor

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
            public ID.ThreadGroupID group;
            public IncomingRequest(ID.ThreadGroupID group) {
                this.group = group;
            }
            public IncomingRequest() {
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

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public String groupName;
            public Reply(String groupName) {
                this.groupName = groupName;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                groupName = ps.readString();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(groupName);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("groupName=" + groupName);
                return stringBuilder.toString();
            }
        }
    }

    public static class Parent {
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
            public ID.ThreadGroupID group;
            public IncomingRequest(ID.ThreadGroupID group) {
                this.group = group;
            }
            public IncomingRequest() {
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

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public ID.ThreadGroupID parentGroup;
            public Reply(ID.ThreadGroupID parentGroup) {
                this.parentGroup = parentGroup;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                parentGroup = ID.read(ps.getInputStream(), ID.ThreadGroupID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                parentGroup.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("parentGroup=" + parentGroup);
                return stringBuilder.toString();
            }
        }
    }

    public static class Children {
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
            public ID.ThreadGroupID group;
            public IncomingRequest(ID.ThreadGroupID group) {
                this.group = group;
            }
            public IncomingRequest() {
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

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public ID.ThreadID[] childThreads;

            public ID.ThreadGroupID[] childGroups;
            public Reply(ID.ThreadID[] childThreads,
                ID.ThreadGroupID[] childGroups) {
                this.childThreads = childThreads;
                this.childGroups = childGroups;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                final int childThreadsCount = ps.readInt();
                childThreads = new ID.ThreadID[childThreadsCount];
                for (int i = 0; i < childThreadsCount; i++) {
                    childThreads[i] = ID.read(ps.getInputStream(), ID.ThreadID.class);
                }
                final int childGroupsCount = ps.readInt();
                childGroups = new ID.ThreadGroupID[childGroupsCount];
                for (int i = 0; i < childGroupsCount; i++) {
                    childGroups[i] = ID.read(ps.getInputStream(), ID.ThreadGroupID.class);
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(childThreads.length);
                for (int i = 0; i < childThreads.length; i++) {
                    childThreads[i].write(ps.getOutputStream());
                }
                ps.write(childGroups.length);
                for (int i = 0; i < childGroups.length; i++) {
                    childGroups[i].write(ps.getOutputStream());
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("childThreads=[" + childThreads.length + "]{");
                for (int i = 0; i < childThreads.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("childThreads[i]=" + childThreads[i]);
                }
                stringBuilder.append("}");
                stringBuilder.append(", ");
                stringBuilder.append("childGroups=[" + childGroups.length + "]{");
                for (int i = 0; i < childGroups.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("childGroups[i]=" + childGroups[i]);
                }
                stringBuilder.append("}");
                return stringBuilder.toString();
            }
        }
    }
}

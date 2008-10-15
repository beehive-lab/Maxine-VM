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
            public ID.ThreadGroupID _group;
            public IncomingRequest(ID.ThreadGroupID group) {
                this._group = group;
            }
            public IncomingRequest() {
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

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public String _groupName;
            public Reply(String groupName) {
                this._groupName = groupName;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _groupName = ps.readString();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_groupName);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_groupName=" + _groupName);
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
            public ID.ThreadGroupID _group;
            public IncomingRequest(ID.ThreadGroupID group) {
                this._group = group;
            }
            public IncomingRequest() {
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

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public ID.ThreadGroupID _parentGroup;
            public Reply(ID.ThreadGroupID parentGroup) {
                this._parentGroup = parentGroup;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _parentGroup = ID.read(ps.getInputStream(), ID.ThreadGroupID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _parentGroup.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_parentGroup=" + _parentGroup);
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
            public ID.ThreadGroupID _group;
            public IncomingRequest(ID.ThreadGroupID group) {
                this._group = group;
            }
            public IncomingRequest() {
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

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public ID.ThreadID[] _childThreads;

            public ID.ThreadGroupID[] _childGroups;
            public Reply(ID.ThreadID[] childThreads,
                ID.ThreadGroupID[] childGroups) {
                this._childThreads = childThreads;
                this._childGroups = childGroups;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                final int childThreadsCount = ps.readInt();
                _childThreads = new ID.ThreadID[childThreadsCount];
                for (int i = 0; i < childThreadsCount; i++) {
                    _childThreads[i] = ID.read(ps.getInputStream(), ID.ThreadID.class);
                }
                final int childGroupsCount = ps.readInt();
                _childGroups = new ID.ThreadGroupID[childGroupsCount];
                for (int i = 0; i < childGroupsCount; i++) {
                    _childGroups[i] = ID.read(ps.getInputStream(), ID.ThreadGroupID.class);
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_childThreads.length);
                for (int i = 0; i < _childThreads.length; i++) {
                    _childThreads[i].write(ps.getOutputStream());
                }
                ps.write(_childGroups.length);
                for (int i = 0; i < _childGroups.length; i++) {
                    _childGroups[i].write(ps.getOutputStream());
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_childThreads=[" + _childThreads.length + "]{");
                for (int i = 0; i < _childThreads.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("_childThreads[i]=" + _childThreads[i]);
                }
                stringBuilder.append("}");
                stringBuilder.append(", ");
                stringBuilder.append("_childGroups=[" + _childGroups.length + "]{");
                for (int i = 0; i < _childGroups.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("_childGroups[i]=" + _childGroups[i]);
                }
                stringBuilder.append("}");
                return stringBuilder.toString();
            }
        }
    }
}

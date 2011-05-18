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

public final class ClassObjectReferenceCommands {
    public static final int COMMAND_SET = 17;
    private ClassObjectReferenceCommands() { }  // hide constructor

    public static class ReflectedType {
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
            public ID.ClassObjectID classObject;
            public IncomingRequest(ID.ClassObjectID classObject) {
                this.classObject = classObject;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                classObject = ID.read(ps.getInputStream(), ID.ClassObjectID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                classObject.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("classObject=" + classObject);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public byte refTypeTag;

            public ID.ReferenceTypeID typeID;
            public Reply(byte refTypeTag,
                ID.ReferenceTypeID typeID) {
                this.refTypeTag = refTypeTag;
                this.typeID = typeID;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                refTypeTag = ps.readByte();
                typeID = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(refTypeTag);
                typeID.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("refTypeTag=" + refTypeTag);
                stringBuilder.append(", ");
                stringBuilder.append("typeID=" + typeID);
                return stringBuilder.toString();
            }
        }
    }
}

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

public final class ClassLoaderReferenceCommands {
    public static final int COMMAND_SET = 14;
    private ClassLoaderReferenceCommands() { }  // hide constructor

    public static class VisibleClasses {
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
            public ID.ClassLoaderID classLoaderObject;
            public IncomingRequest(ID.ClassLoaderID classLoaderObject) {
                this.classLoaderObject = classLoaderObject;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                classLoaderObject = ID.read(ps.getInputStream(), ID.ClassLoaderID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                classLoaderObject.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("classLoaderObject=" + classLoaderObject);
                return stringBuilder.toString();
            }
        }

        public static class ClassInfo {

            public byte refTypeTag;

            public ID.ReferenceTypeID typeID;
            public ClassInfo(byte refTypeTag,
                ID.ReferenceTypeID typeID) {
                this.refTypeTag = refTypeTag;
                this.typeID = typeID;
            }
            public ClassInfo() {
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
}

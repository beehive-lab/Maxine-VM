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

public final class ArrayReferenceCommands {
    public static final int COMMAND_SET = 13;
    private ArrayReferenceCommands() { }  // hide constructor

    public static class Length {
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
            public ID.ArrayID arrayObject;
            public IncomingRequest(ID.ArrayID arrayObject) {
                this.arrayObject = arrayObject;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                arrayObject = ID.read(ps.getInputStream(), ID.ArrayID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                arrayObject.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("arrayObject=" + arrayObject);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public int arrayLength;
            public Reply(int arrayLength) {
                this.arrayLength = arrayLength;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                arrayLength = ps.readInt();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(arrayLength);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("arrayLength=" + arrayLength);
                return stringBuilder.toString();
            }
        }
    }

    public static class GetValues {
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
            public ID.ArrayID arrayObject;

            public int firstIndex;

            public int length;
            public IncomingRequest(ID.ArrayID arrayObject,
                int firstIndex,
                int length) {
                this.arrayObject = arrayObject;
                this.firstIndex = firstIndex;
                this.length = length;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                arrayObject = ID.read(ps.getInputStream(), ID.ArrayID.class);
                firstIndex = ps.readInt();
                length = ps.readInt();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                arrayObject.write(ps.getOutputStream());
                ps.write(firstIndex);
                ps.write(length);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("arrayObject=" + arrayObject);
                stringBuilder.append(", ");
                stringBuilder.append("firstIndex=" + firstIndex);
                stringBuilder.append(", ");
                stringBuilder.append("length=" + length);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public java.util.List<? extends JDWPValue> values;
            public Reply(java.util.List<? extends JDWPValue> values) {
                this.values = values;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                values = ps.readArrayRegion();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(values);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("values=" + values);
                return stringBuilder.toString();
            }
        }
    }

    public static class SetValues {
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
            public ID.ArrayID arrayObject;

            public int firstIndex;

            public JDWPValue[] values;
            public IncomingRequest(ID.ArrayID arrayObject,
                int firstIndex,
                JDWPValue[] values) {
                this.arrayObject = arrayObject;
                this.firstIndex = firstIndex;
                this.values = values;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                arrayObject = ID.read(ps.getInputStream(), ID.ArrayID.class);
                firstIndex = ps.readInt();
                final int valuesCount = ps.readInt();
                values = new JDWPValue[valuesCount];
                for (int i = 0; i < valuesCount; i++) {
                    values[i] = ps.readUntaggedValue();
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                arrayObject.write(ps.getOutputStream());
                ps.write(firstIndex);
                ps.write(values.length);
                for (int i = 0; i < values.length; i++) {
                    ps.write(values[i]);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("arrayObject=" + arrayObject);
                stringBuilder.append(", ");
                stringBuilder.append("firstIndex=" + firstIndex);
                stringBuilder.append(", ");
                stringBuilder.append("values=[" + values.length + "]{");
                for (int i = 0; i < values.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("values[i]=" + values[i]);
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
}

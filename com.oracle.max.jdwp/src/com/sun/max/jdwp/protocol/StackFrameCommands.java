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

public final class StackFrameCommands {
    public static final int COMMAND_SET = 16;
    private StackFrameCommands() { }  // hide constructor

    public static class GetValues {
        public static final byte COMMAND = 1;
        public abstract static class Handler implements CommandHandler<IncomingRequest, Reply> {
            public IncomingRequest createIncomingDataObject() { return new IncomingRequest(); }
            public int helpAtDecodingUntaggedValue(IncomingRequest incomingRequest) throws JDWPException {assert false : "If this method can be called, it must be overwritten in subclasses!"; return 0; }
            public Reply handle(IncomingRequest incomingRequest, JDWPSender replyChannel) throws JDWPException { return handle(incomingRequest); }
            public Reply handle(IncomingRequest incomingRequest) throws JDWPException { throw new JDWPNotImplementedException(); }
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }
        }

        public static class SlotInfo {

            public int slot;

            public byte sigbyte;
            public SlotInfo(int slot,
                byte sigbyte) {
                this.slot = slot;
                this.sigbyte = sigbyte;
            }
            public SlotInfo() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                slot = ps.readInt();
                sigbyte = ps.readByte();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(slot);
                ps.write(sigbyte);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("slot=" + slot);
                stringBuilder.append(", ");
                stringBuilder.append("sigbyte=" + sigbyte);
                return stringBuilder.toString();
            }
        }

        public static class IncomingRequest implements IncomingData {
            public ID.ThreadID thread;

            public ID.FrameID frame;

            public SlotInfo[] slots;
            public IncomingRequest(ID.ThreadID thread,
                ID.FrameID frame,
                SlotInfo[] slots) {
                this.thread = thread;
                this.frame = frame;
                this.slots = slots;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
                frame = ID.read(ps.getInputStream(), ID.FrameID.class);
                final int slotsCount = ps.readInt();
                slots = new SlotInfo[slotsCount];
                for (int i = 0; i < slotsCount; i++) {
                    slots[i] = new SlotInfo();
                    slots[i].read(ps);
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                thread.write(ps.getOutputStream());
                frame.write(ps.getOutputStream());
                ps.write(slots.length);
                for (int i = 0; i < slots.length; i++) {
                    slots[i].write(ps);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("thread=" + thread);
                stringBuilder.append(", ");
                stringBuilder.append("frame=" + frame);
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

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public JDWPValue[] values;
            public Reply(JDWPValue[] values) {
                this.values = values;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                final int valuesCount = ps.readInt();
                values = new JDWPValue[valuesCount];
                for (int i = 0; i < valuesCount; i++) {
                    values[i] = ps.readValue();
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(values.length);
                for (int i = 0; i < values.length; i++) {
                    ps.write(values[i]);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("values=[" + values.length + "]{");
                for (int i = 0; i < values.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("values[i]=" + values[i]);
                }
                stringBuilder.append("}");
                return stringBuilder.toString();
            }
        }
    }

    public static class SetValues {
        public static final byte COMMAND = 2;
        public abstract static class Handler implements CommandHandler<IncomingRequest, Reply> {
            public IncomingRequest createIncomingDataObject() { return new IncomingRequest(); }
            public int helpAtDecodingUntaggedValue(IncomingRequest incomingRequest) throws JDWPException {assert false : "If this method can be called, it must be overwritten in subclasses!"; return 0; }
            public Reply handle(IncomingRequest incomingRequest, JDWPSender replyChannel) throws JDWPException { return handle(incomingRequest); }
            public Reply handle(IncomingRequest incomingRequest) throws JDWPException { throw new JDWPNotImplementedException(); }
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }
        }

        public static class SlotInfo {

            public int slot;

            public JDWPValue slotValue;
            public SlotInfo(int slot,
                JDWPValue slotValue) {
                this.slot = slot;
                this.slotValue = slotValue;
            }
            public SlotInfo() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                slot = ps.readInt();
                slotValue = ps.readValue();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(slot);
                ps.write(slotValue);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("slot=" + slot);
                stringBuilder.append(", ");
                stringBuilder.append("slotValue=" + slotValue);
                return stringBuilder.toString();
            }
        }

        public static class IncomingRequest implements IncomingData {
            public ID.ThreadID thread;

            public ID.FrameID frame;

            public SlotInfo[] slotValues;
            public IncomingRequest(ID.ThreadID thread,
                ID.FrameID frame,
                SlotInfo[] slotValues) {
                this.thread = thread;
                this.frame = frame;
                this.slotValues = slotValues;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
                frame = ID.read(ps.getInputStream(), ID.FrameID.class);
                final int slotValuesCount = ps.readInt();
                slotValues = new SlotInfo[slotValuesCount];
                for (int i = 0; i < slotValuesCount; i++) {
                    slotValues[i] = new SlotInfo();
                    slotValues[i].read(ps);
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                thread.write(ps.getOutputStream());
                frame.write(ps.getOutputStream());
                ps.write(slotValues.length);
                for (int i = 0; i < slotValues.length; i++) {
                    slotValues[i].write(ps);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("thread=" + thread);
                stringBuilder.append(", ");
                stringBuilder.append("frame=" + frame);
                stringBuilder.append(", ");
                stringBuilder.append("slotValues=[" + slotValues.length + "]{");
                for (int i = 0; i < slotValues.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("slotValues[i]=" + slotValues[i]);
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

    public static class ThisObject {
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
            public ID.ThreadID thread;

            public ID.FrameID frame;
            public IncomingRequest(ID.ThreadID thread,
                ID.FrameID frame) {
                this.thread = thread;
                this.frame = frame;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
                frame = ID.read(ps.getInputStream(), ID.FrameID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                thread.write(ps.getOutputStream());
                frame.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("thread=" + thread);
                stringBuilder.append(", ");
                stringBuilder.append("frame=" + frame);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public JDWPValue objectThis;
            public Reply(JDWPValue objectThis) {
                this.objectThis = objectThis;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                objectThis = ps.readValue();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(objectThis);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("objectThis=" + objectThis);
                return stringBuilder.toString();
            }
        }
    }

    public static class PopFrames {
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
            public ID.ThreadID thread;

            public ID.FrameID frame;
            public IncomingRequest(ID.ThreadID thread,
                ID.FrameID frame) {
                this.thread = thread;
                this.frame = frame;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
                frame = ID.read(ps.getInputStream(), ID.FrameID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                thread.write(ps.getOutputStream());
                frame.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("thread=" + thread);
                stringBuilder.append(", ");
                stringBuilder.append("frame=" + frame);
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

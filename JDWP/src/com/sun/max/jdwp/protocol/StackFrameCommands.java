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
/*VCSID=ebc379e1-0c68-44be-9316-913100f3039f*/

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

            public int _slot;

            public byte _sigbyte;
            public SlotInfo(int slot,
                byte sigbyte) {
                this._slot = slot;
                this._sigbyte = sigbyte;
            }
            public SlotInfo() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _slot = ps.readInt();
                _sigbyte = ps.readByte();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_slot);
                ps.write(_sigbyte);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_slot=" + _slot);
                stringBuilder.append(", ");
                stringBuilder.append("_sigbyte=" + _sigbyte);
                return stringBuilder.toString();
            }
        }

        public static class IncomingRequest implements IncomingData {
            public ID.ThreadID _thread;

            public ID.FrameID _frame;

            public SlotInfo[] _slots;
            public IncomingRequest(ID.ThreadID thread,
                ID.FrameID frame,
                SlotInfo[] slots) {
                this._thread = thread;
                this._frame = frame;
                this._slots = slots;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
                _frame = ID.read(ps.getInputStream(), ID.FrameID.class);
                final int slotsCount = ps.readInt();
                _slots = new SlotInfo[slotsCount];
                for (int i = 0; i < slotsCount; i++) {
                    _slots[i] = new SlotInfo();
                    _slots[i].read(ps);
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _thread.write(ps.getOutputStream());
                _frame.write(ps.getOutputStream());
                ps.write(_slots.length);
                for (int i = 0; i < _slots.length; i++) {
                    _slots[i].write(ps);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_thread=" + _thread);
                stringBuilder.append(", ");
                stringBuilder.append("_frame=" + _frame);
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

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public JDWPValue[] _values;
            public Reply(JDWPValue[] values) {
                this._values = values;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                final int valuesCount = ps.readInt();
                _values = new JDWPValue[valuesCount];
                for (int i = 0; i < valuesCount; i++) {
                    _values[i] = ps.readValue();
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_values.length);
                for (int i = 0; i < _values.length; i++) {
                    ps.write(_values[i]);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_values=[" + _values.length + "]{");
                for (int i = 0; i < _values.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("_values[i]=" + _values[i]);
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

            public int _slot;

            public JDWPValue _slotValue;
            public SlotInfo(int slot,
                JDWPValue slotValue) {
                this._slot = slot;
                this._slotValue = slotValue;
            }
            public SlotInfo() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _slot = ps.readInt();
                _slotValue = ps.readValue();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_slot);
                ps.write(_slotValue);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_slot=" + _slot);
                stringBuilder.append(", ");
                stringBuilder.append("_slotValue=" + _slotValue);
                return stringBuilder.toString();
            }
        }

        public static class IncomingRequest implements IncomingData {
            public ID.ThreadID _thread;

            public ID.FrameID _frame;

            public SlotInfo[] _slotValues;
            public IncomingRequest(ID.ThreadID thread,
                ID.FrameID frame,
                SlotInfo[] slotValues) {
                this._thread = thread;
                this._frame = frame;
                this._slotValues = slotValues;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
                _frame = ID.read(ps.getInputStream(), ID.FrameID.class);
                final int slotValuesCount = ps.readInt();
                _slotValues = new SlotInfo[slotValuesCount];
                for (int i = 0; i < slotValuesCount; i++) {
                    _slotValues[i] = new SlotInfo();
                    _slotValues[i].read(ps);
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _thread.write(ps.getOutputStream());
                _frame.write(ps.getOutputStream());
                ps.write(_slotValues.length);
                for (int i = 0; i < _slotValues.length; i++) {
                    _slotValues[i].write(ps);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_thread=" + _thread);
                stringBuilder.append(", ");
                stringBuilder.append("_frame=" + _frame);
                stringBuilder.append(", ");
                stringBuilder.append("_slotValues=[" + _slotValues.length + "]{");
                for (int i = 0; i < _slotValues.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("_slotValues[i]=" + _slotValues[i]);
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
            public ID.ThreadID _thread;

            public ID.FrameID _frame;
            public IncomingRequest(ID.ThreadID thread,
                ID.FrameID frame) {
                this._thread = thread;
                this._frame = frame;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
                _frame = ID.read(ps.getInputStream(), ID.FrameID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _thread.write(ps.getOutputStream());
                _frame.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_thread=" + _thread);
                stringBuilder.append(", ");
                stringBuilder.append("_frame=" + _frame);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public JDWPValue _objectThis;
            public Reply(JDWPValue objectThis) {
                this._objectThis = objectThis;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _objectThis = ps.readValue();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_objectThis);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_objectThis=" + _objectThis);
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
            public ID.ThreadID _thread;

            public ID.FrameID _frame;
            public IncomingRequest(ID.ThreadID thread,
                ID.FrameID frame) {
                this._thread = thread;
                this._frame = frame;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
                _frame = ID.read(ps.getInputStream(), ID.FrameID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _thread.write(ps.getOutputStream());
                _frame.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_thread=" + _thread);
                stringBuilder.append(", ");
                stringBuilder.append("_frame=" + _frame);
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

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
package com.sun.max.vm.tele;

import java.io.*;

import com.sun.max.collect.*;
import com.sun.max.program.*;

/**
 * @author Bernd Mathiske
 */
public abstract class MaxineMessage<Message_Type extends MaxineMessage> {

    public enum Tag {
        BYTECODE_BREAKPOINT;

        public static final IndexedSequence<Tag> VALUES = new ArraySequence<Tag>(values());
    }

    private final Tag _tag;

    public Tag tag() {
        return _tag;
    }

    protected MaxineMessage(Tag tag) {
        _tag = tag;
    }

    public void readData(DataInputStream dataInputStream) throws IOException {
    }

    public static MaxineMessage read(DataInputStream dataInputStream) throws IOException {
        final Tag tag = Tag.VALUES.get(dataInputStream.readInt());
        MaxineMessage message = null;
        switch (tag) {
            case BYTECODE_BREAKPOINT: {
                message = new BytecodeBreakpointMessage();
                break;
            }
            default: {
                throw ProgramError.unknownCase();
            }
        }
        message.readData(dataInputStream);
        return message;
    }

    public void writeData(DataOutputStream dataOutputStream) throws IOException {
    }

    public final void write(DataOutputStream dataOutputStream) throws IOException {
        dataOutputStream.writeInt(_tag.ordinal());
        writeData(dataOutputStream);
    }

    public interface Receiver<Message_Type> {
        void consume(Message_Type message);
    }

}

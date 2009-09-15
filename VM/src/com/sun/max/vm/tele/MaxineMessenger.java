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

import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;

/**
 * @author Bernd Mathiske
 */
public final class MaxineMessenger {

    private static MaxineMessenger messenger = new MaxineMessenger();

    public static MaxineMessenger messenger() {
        return messenger;
    }

    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;

    public boolean isActivated() {
        return dataInputStream != null && dataOutputStream != null;
    }

    public void activate(DataAccess dataAccess, Pointer inData, Pointer outData, int dataSize) {
        final RingBufferPipe in = new RingBufferPipe(dataAccess, inData, dataSize);
        final RingBufferPipe out = new RingBufferPipe(dataAccess, outData, dataSize);
        dataInputStream = new DataInputStream(in.createInputStream());
        dataOutputStream = new DataOutputStream(out.createOutputStream());
    }

    /**
     * If a non-zero value is put here remotely before we execute initialize() below,
     * then this variable contains a pointer to a struct malloc-ed by
     * the substrate and recorded in the image header.
     * Otherwise it remains zero and there is no messenger traffic.
     */
    @INSPECTED
    private static Pointer info = Pointer.zero();

    public static boolean isVmInspected() {
        return !info.isZero();
    }

    /**
     * @see "messenger.h" - messenger_InfoStruct
     */
    public static void initialize() {
        if (isVmInspected()) {
            final Size dataSize = info.getWord(0).asSize();
            final Pointer inData = info.getWord(1).asPointer();
            final Pointer outData = info.getWord(2).asPointer();
            messenger.activate(MemoryDataAccess.POINTER_DATA_ACCESS, inData, outData, dataSize.toInt());
            messenger.flush();
        }
    }

    private MaxineMessage receive() {
        try {
            if (dataInputStream == null || dataInputStream.available() <= 0) {
                return null;
            }
            return MaxineMessage.read(dataInputStream);
        } catch (IOException ioException) {
            throw ProgramError.unexpected(ioException);
        }
    }

    public void send(MaxineMessage message) {
        try {
            if (dataOutputStream != null) {
                message.write(dataOutputStream);
            }
        } catch (IOException ioException) {
            throw ProgramError.unexpected(ioException);
        }
    }

    private final VariableMapping<MaxineMessage.Tag, MaxineMessage.Receiver> messageTagToReceiver = HashMapping.createVariableIdentityMapping();

    public static void subscribe(MaxineMessage.Tag tag, MaxineMessage.Receiver receiver) {
        messenger.messageTagToReceiver.put(tag, receiver);
    }

    private <Message_Type extends MaxineMessage> void consume(Message_Type message) {
        final Class<MaxineMessage.Receiver<Message_Type>> type = null;
        final MaxineMessage.Receiver<Message_Type> receiver = StaticLoophole.cast(type, messageTagToReceiver.get(message.tag));
        assert receiver != null;
        receiver.consume(message);
    }

    public synchronized void flush() {
        while (true) {
            final MaxineMessage message = receive();
            if (message == null) {
                return;
            }
            consume(message);
        }
    }
}

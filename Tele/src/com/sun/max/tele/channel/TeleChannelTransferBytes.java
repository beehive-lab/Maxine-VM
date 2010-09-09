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
package com.sun.max.tele.channel;

import java.nio.*;

import com.sun.max.unsafe.*;

/**
 * There is some complexity involved in transferring byte arrays to./from the target VM, owing to the possibility of
 * limitations on the size of the array that can be used for a given communication channel. This is handled here.
 *
 * @author Mick Jordan
 *
 */
public class TeleChannelTransferBytes {

    private static int maxByteBufferSize;

    public static int readBytes(TeleChannelProtocol protocol, ByteBuffer dst, int dstOffset, int length, Address address) {
        int lengthLeft = length;
        int localOffset = dstOffset;
        long localAddress = address.toLong();
        checkMaxByteBufferSize(protocol);
        while (lengthLeft > 0) {
            final int toDo = lengthLeft > maxByteBufferSize ? maxByteBufferSize : lengthLeft;
            assert dst.limit() - localOffset >= length;
            final int r = protocol.readBytes(localAddress, dst, localOffset, toDo);
            if (r != toDo) {
                return -1;
            }
            lengthLeft -= toDo;
            localOffset += toDo;
            localAddress += toDo;
        }
        return length;
    }

    public static int writeBytes(TeleChannelProtocol protocol, ByteBuffer src, int offset, int length, Address address) {
        int lengthLeft = length;
        int localOffset = offset;
        long localAddress = address.toLong();
        checkMaxByteBufferSize(protocol);
        while (lengthLeft > 0) {
            final int toDo = lengthLeft > maxByteBufferSize ? maxByteBufferSize : lengthLeft;
            assert src.limit() - localOffset >= length;
            final int r = protocol.writeBytes(localAddress, src, localOffset, toDo);
            if (r != toDo) {
                return -1;
            }
            lengthLeft -= toDo;
            localOffset += toDo;
            localAddress += toDo;
        }
        return length;
    }

    private static void checkMaxByteBufferSize(TeleChannelProtocol protocol) {
        if (maxByteBufferSize == 0) {
            maxByteBufferSize = protocol.maxByteBufferSize();
        }
    }

}

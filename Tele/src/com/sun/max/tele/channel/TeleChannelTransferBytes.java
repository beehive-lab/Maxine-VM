/*
 * Copyright (c) 2010, 2010, Oracle and/or its affiliates. All rights reserved.
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

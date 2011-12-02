/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.graal.snippets;

import com.oracle.max.graal.nodes.extended.*;
import com.sun.cri.ci.*;


public class ArrayCopySnippets implements SnippetsInterface{

    @Snippet
    public static void arraycopy(int[] src, int srcPos, int[] dest, int destPos, int length) {
        if (src == null || dest == null) {
            throw new NullPointerException();
        }
        if (srcPos < 0 || destPos < 0 || length < 0 || srcPos + length > src.length || destPos + length > dest.length) {
            throw new IndexOutOfBoundsException();
        }
        if (src == dest && srcPos < destPos) { // bad aliased case
//            if ((length & 0x01) == 0 && (srcPos & 0x01) == 0 && (destPos & 0x01) == 0) {
//                copyLongsDown(src, srcPos, dest, destPos, length >> 1);
//            } else {
                copyIntsDown(src, srcPos, dest, destPos, length);
//            }
        } else {
//            if ((length & 0x01) == 0 && (srcPos & 0x01) == 0 && (destPos & 0x01) == 0) {
//                copyLongsUp(src, srcPos, dest, destPos, length >> 1);
//            } else {
                copyIntsUp(src, srcPos, dest, destPos, length);
//            }
        }
    }

    @Snippet
    public static void arraycopy(char[] src, int srcPos, char[] dest, int destPos, int length) {
        if (src == null || dest == null) {
            throw new NullPointerException();
        }
        if (srcPos < 0 || destPos < 0 || length < 0 || srcPos + length > src.length || destPos + length > dest.length) {
            throw new IndexOutOfBoundsException();
        }
        if (src == dest && srcPos < destPos) { // bad aliased case
            if ((length & 0x01) == 0 && (srcPos & 0x01) == 0 && (destPos & 0x01) == 0) {
                if ((length & 0x02) == 0 && (srcPos & 0x02) == 0 && (destPos & 0x02) == 0) {
                    copyLongsDown(src, srcPos >> 2, dest, destPos >> 2, length >> 2);
                } else {
                    copyIntsDown(src, srcPos >> 1, dest, destPos >> 1, length >> 1);
                }
            } else {
                copyCharsDown(src, srcPos, dest, destPos, length);
            }
        } else {
            if ((length & 0x01) == 0 && (srcPos & 0x01) == 0 && (destPos & 0x01) == 0) {
                if ((length & 0x02) == 0 && (srcPos & 0x02) == 0 && (destPos & 0x02) == 0) {
                    copyLongsUp(src, srcPos >> 2, dest, destPos >> 2, length >> 2);
                } else {
                    copyIntsUp(src, srcPos >> 1, dest, destPos >> 1, length >> 1);
                }
            } else {
                copyCharsUp(src, srcPos, dest, destPos, length);
            }
        }
    }

    @Snippet
    public static void arraycopy(long[] src, int srcPos, long[] dest, int destPos, int length) {
        if (src == null || dest == null) {
            throw new NullPointerException();
        }
        if (srcPos < 0 || destPos < 0 || length < 0 || srcPos + length > src.length || destPos + length > dest.length) {
            throw new IndexOutOfBoundsException();
        }
        if (src == dest && srcPos < destPos) { // bad aliased case
            copyLongsDown(src, srcPos, dest, destPos, length);
        } else {
            copyLongsUp(src, srcPos, dest, destPos, length);
        }
    }

    @Snippet
    public static void copyBytesDown(Object src, int srcPos, Object dest, int destPos, int length)  {
        for (int i = length - 1; i >= 0; i--) {
            byte a = UnsafeLoadIndexedNode.load(src, i + srcPos, CiKind.Byte);
            UnsafeStoreIndexedNode.store(dest, i + destPos, a, CiKind.Byte);
        }
    }

    @Snippet
    public static void copyCharsDown(Object src, int srcPos, Object dest, int destPos, int length)  {
        for (int i = length - 1; i >= 0; i--) {
            char a = UnsafeLoadIndexedNode.load(src, i + srcPos, CiKind.Char);
            UnsafeStoreIndexedNode.store(dest, i + destPos, a, CiKind.Char);
        }
    }

    @Snippet
    public static void copyIntsDown(Object src, int srcPos, Object dest, int destPos, int length)  {
        for (int i = length - 1; i >= 0; i--) {
            int a = UnsafeLoadIndexedNode.load(src, i + srcPos, CiKind.Int);
            UnsafeStoreIndexedNode.store(dest, i + destPos, a, CiKind.Int);
        }
    }

    @Snippet
    public static void copyLongsDown(Object src, int srcPos, Object dest, int destPos, int length)  {
        for (int i = length - 1; i >= 0; i--) {
            long a = UnsafeLoadIndexedNode.load(src, i + srcPos, CiKind.Long);
            UnsafeStoreIndexedNode.store(dest, i + destPos, a, CiKind.Long);
        }
    }

    @Snippet
    public static void copyBytesUp(Object src, int srcPos, Object dest, int destPos, int length)  {
        for (int i = 0; i < length; i++) {
            byte a = UnsafeLoadIndexedNode.load(src, i + srcPos, CiKind.Byte);
            UnsafeStoreIndexedNode.store(dest, i + destPos, a, CiKind.Byte);
        }
    }

    @Snippet
    public static void copyCharsUp(Object src, int srcPos, Object dest, int destPos, int length)  {
        for (int i = 0; i < length; i++) {
            char a = UnsafeLoadIndexedNode.load(src, i + srcPos, CiKind.Char);
            UnsafeStoreIndexedNode.store(dest, i + destPos, a, CiKind.Char);
        }
    }

    @Snippet
    public static void copyIntsUp(Object src, int srcPos, Object dest, int destPos, int length)  {
        for (int i = 0; i < length; i++) {
            int a = UnsafeLoadIndexedNode.load(src, i + srcPos, CiKind.Int);
            UnsafeStoreIndexedNode.store(dest, i + destPos, a, CiKind.Int);
        }
    }

    @Snippet
    public static void copyLongsUp(Object src, int srcPos, Object dest, int destPos, int length)  {
        for (int i = 0; i < length; i++) {
            long a = UnsafeLoadIndexedNode.load(src, i + srcPos, CiKind.Long);
            UnsafeStoreIndexedNode.store(dest, i + destPos, a, CiKind.Long);
        }
    }
}

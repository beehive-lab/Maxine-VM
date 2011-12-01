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

import com.oracle.max.cri.intrinsics.*;
import com.oracle.max.graal.compiler.gen.*;
import com.oracle.max.graal.nodes.extended.*;
import com.oracle.max.graal.nodes.java.*;
import com.sun.cri.ci.*;
import com.sun.max.annotate.*;

/**
 * Snippets for {@link sun.misc.Unsafe} methods.
 */
@ClassSubstitution(sun.misc.Unsafe.class)
public class UnsafeSnippets implements SnippetsInterface {

    public boolean compareAndSwapObject(Object o, long offset, Object expected, Object x) {
        return CompareAndSwapNode.compareAndSwap(o, offset, expected, x);
    }

    public boolean compareAndSwapInt(Object o, long offset, int expected, int x) {
        return CompareAndSwapNode.compareAndSwap(o, offset, expected, x);
    }

    public boolean compareAndSwapLong(Object o, long offset, long expected, long x) {
        return CompareAndSwapNode.compareAndSwap(o, offset, expected, x);
    }

    // TODO: volatile variants of the following methods, e.g. getObjectVolatile()

    public Object getObject(Object o, long offset) {
        return UnsafeLoadNode.load(o, offset, CiKind.Object);
    }
/*
    public Object getObjectVolatile(Object o, long offset) {
        MembarNode.get(MemoryBarriers.JMM_PRE_VOLATILE_READ);
        Object result = UnsafeLoadNode.load(o, offset, CiKind.Object);
        MembarNode.get(MemoryBarriers.JMM_POST_VOLATILE_READ);
        return result;
    }*/

    public void putObject(Object o, long offset, Object x) {
        UnsafeStoreNode.store(o, offset, x, CiKind.Object);
    }

    public int getInt(Object o, long offset) {
        Integer value = UnsafeLoadNode.load(o, offset, CiKind.Int);
        return value;
    }

    public void putInt(Object o, long offset, int x) {
        UnsafeStoreNode.store(o, offset, x, CiKind.Int);
    }

    public boolean getBoolean(Object o, long offset) {
        @JavacBug(id = 6995200)
        Boolean result = UnsafeLoadNode.load(o, offset, CiKind.Boolean);
        return result;
    }

    public void putBoolean(Object o, long offset, boolean x) {
        UnsafeStoreNode.store(o, offset, x, CiKind.Boolean);
    }

    public byte getByte(Object o, long offset) {
        @JavacBug(id = 6995200)
        Byte result = UnsafeLoadNode.load(o, offset, CiKind.Byte);
        return result;
    }

    public void putByte(Object o, long offset, byte x) {
        UnsafeStoreNode.store(o, offset, x, CiKind.Byte);
    }

    public short getShort(Object o, long offset) {
        @JavacBug(id = 6995200)
        Short result = UnsafeLoadNode.load(o, offset, CiKind.Short);
        return result;
    }

    public void putShort(Object o, long offset, short x) {
        UnsafeStoreNode.store(o, offset, x, CiKind.Short);
    }

    public char getChar(Object o, long offset) {
        @JavacBug(id = 6995200)
        Character result = UnsafeLoadNode.load(o, offset, CiKind.Char);
        return result;
    }

    public void putChar(Object o, long offset, char x) {
        UnsafeStoreNode.store(o, offset, x, CiKind.Char);
    }

    public long getLong(Object o, long offset) {
        @JavacBug(id = 6995200)
        Long result = UnsafeLoadNode.load(o, offset, CiKind.Long);
        return result;
    }

    public void putLong(Object o, long offset, long x) {
        UnsafeStoreNode.store(o, offset, x, CiKind.Long);
    }

    public float getFloat(Object o, long offset) {
        @JavacBug(id = 6995200)
        Float result = UnsafeLoadNode.load(o, offset, CiKind.Float);
        return result;
    }

    public void putFloat(Object o, long offset, float x) {
        UnsafeStoreNode.store(o, offset, x, CiKind.Float);
    }

    public double getDouble(Object o, long offset) {
        @JavacBug(id = 6995200)
        Double result = UnsafeLoadNode.load(o, offset, CiKind.Double);
        return result;
    }

    public void putDouble(Object o, long offset, double x) {
        UnsafeStoreNode.store(o, offset, x, CiKind.Double);
    }
}

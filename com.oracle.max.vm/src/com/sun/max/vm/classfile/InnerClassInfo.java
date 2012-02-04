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
package com.sun.max.vm.classfile;

import com.sun.max.vm.classfile.constant.*;

/**
 * Representations derived from array entries in the inner classes attribute as described in #4.7.5.
 * 
 * @see InnerClassesAttribute
 */
public class InnerClassInfo {

    private final char innerClassIndex;
    private final char outerClassIndex;
    private final String sourceName;
    private final char flags;

    public int innerClassIndex() {
        return innerClassIndex;
    }

    public int outerClassIndex() {
        return outerClassIndex;
    }

    public String sourceName() {
        return sourceName;
    }

    public int flags() {
        return flags;
    }

    public InnerClassInfo(ClassfileStream classfileStream, ConstantPool constantPool) {
        innerClassIndex = (char) classfileStream.readUnsigned2();
        outerClassIndex = (char) classfileStream.readUnsigned2();
        final int sourceNameIndex = classfileStream.readUnsigned2();
        sourceName = (sourceNameIndex == 0) ? null : constantPool.utf8At(sourceNameIndex, "inner class source name").toString();
        flags = (char) classfileStream.readUnsigned2();

        // Access the components to verify the format of this attribute
        if (innerClassIndex != 0) {
            constantPool.classAt(innerClassIndex);
        }
    }

    @Override
    public String toString() {
        return "inner=" + (int) innerClassIndex + ",outer=" + (int) outerClassIndex + ",flags=0x" + Integer.toHexString(flags) + ",source=" + sourceName;
    }
}

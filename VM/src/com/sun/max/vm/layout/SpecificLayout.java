/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.layout;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * A specific layout contains methods for manipulating an object that is known to be
 * either a tuple or a specific kind of array.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public interface SpecificLayout extends GeneralLayout {

    Layout.Category category();

    boolean isHybridLayout();

    boolean isTupleLayout();

    boolean isArrayLayout();

    boolean isReferenceArrayLayout();

    int headerSize();

    Size specificSize(Accessor accessor);

    @HOSTED_ONLY
    void visitObjectCell(Object object, ObjectCellVisitor visitor);

    int getHubReferenceOffsetInCell();

    @HOSTED_ONLY
    public static interface ObjectMirror {

        ClassActor classActor();

        Value readHub();

        Value readMisc();

        Value readElement(Kind kind, int index);

        int readArrayLength();

        Value readField(int offset);

        void writeHub(Value value);

        void writeMisc(Value value);

        void writeElement(Kind kind, int index, Value value);

        void writeArrayLength(Value value);

        void writeField(int offset, Value value);

        int firstWordIndex();

        int firstIntIndex();
    }

    /**
     * An interface implemented by a client that reads the fields and/or
     * elements of an object at the cell-relative offsets defined by a specific
     * layout.
     *
     * @author Doug Simon
     */
    @HOSTED_ONLY
    public static interface ObjectCellVisitor {

        void visitField(int offsetInCell, Utf8Constant name, TypeDescriptor type, Value value);

        void visitHeaderField(int offsetInCell, String name, TypeDescriptor type, Value value);

        void visitElement(int offsetInCell, int arrayIndex, Value value);
    }

    /**
     * Reads a value in an object described by this layout.
     *
     * @param kind the kind of the result value
     */
    @HOSTED_ONLY
    Value readValue(Kind kind, ObjectMirror mirror, int offset);

    /**
     * Writes a value to an object described by this layout.
     *
     * @param kind the kind of the value in its target location
     */
    @HOSTED_ONLY
    void writeValue(Kind kind, ObjectMirror mirror, int offset, Value value);

}

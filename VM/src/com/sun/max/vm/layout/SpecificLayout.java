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
/*VCSID=1bfe9299-f74e-4665-bd9d-4920afc7519a*/
package com.sun.max.vm.layout;

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

    void visitObjectCell(Object object, ObjectCellVisitor visitor);

    int getHubReferenceOffsetInCell();

    public static interface ObjectMirror {

        ClassActor classActor();

        Value readHub();

        Value readMisc();

        Value readElement(Kind kind, int index);

        Value readArrayLength();

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
    Value readValue(Kind kind, ObjectMirror mirror, int offset);

    /**
     * Writes a value to an object described by this layout.
     *
     * @param kind the kind of the value in its target location
     */
    void writeValue(Kind kind, ObjectMirror mirror, int offset, Value value);

}

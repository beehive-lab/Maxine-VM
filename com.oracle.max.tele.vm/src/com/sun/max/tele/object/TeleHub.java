/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.tele.object;

import com.sun.max.tele.*;
import com.sun.max.tele.reference.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.type.*;

/**
 * Inspector's canonical surrogate for an object of type {@link Hub} in the VM.
 * Note that this kind of object cannot be expressed as an ordinary type;
 * although allocated as a single array of words (see {@link TeleHybridObject},
 * the memory is used in several sections with different properties, summarized
 * by the methods here.
 */
public abstract class TeleHub extends TeleHybridObject {

    protected TeleHub(TeleVM vm, RemoteReference hubReference) {
        super(vm, hubReference);
    }

    /**
     * Cache for the TeleClassActor.
     */
    private TeleClassActor teleClassActor = null;

    /**
     * @return surrogate for the {@ClassActor} in the VM that contains this {@link Hub}, i.e. for the type that this hub helps implement
     */
    public final TeleClassActor getTeleClassActor() {
        if (teleClassActor == null) {
            teleClassActor = fetchTeleClassActor();
        }
        return teleClassActor;
    }

    /**
     * Logic to fetch the tele class actor for this tele hub on cache miss.
     */
    protected TeleClassActor fetchTeleClassActor() {
        final RemoteReference classActorReference = jumpForwarder(fields().Hub_classActor.readReference(reference()));
        return (TeleClassActor) objects().makeTeleObject(classActorReference);
    }

    /**
     * @return local {@link Hub} corresponding to this {@link Hub} in the {@link TeleVM}.
     */
    public abstract Hub hub();

    // NOTE:  the following comments and methods treat, in order of memory layout, the regions of a {@link Hub}[

    // The first region of memory, starting at index 0 when viewing the object as an array of words,
    // is occupied by a collection of fields, as if an ordinary tuple, defined by  {@link TeleObject#getFieldActors()}

    // The second region of memory is the vTable: used as an array of words containing pointers to method code.

    /**
     * @return the type category of information stored in the vTable
     */
    public Kind vTableKind() {
        return Kind.WORD;
    }

    public TypeDescriptor vTableType() {
        return JavaTypeDescriptor.WORD;
    }

    /**
     * @return index into the object (viewed as an array of words) of the beginning of the vTable.
     */
    public int vTableStartIndex() {
        return Hub.vTableStartIndex();
    }

    /**
     * @return memory offset, relative to object origin, of the start of the vTable.
     */
    public int vTableOffset() {
        return Layout.wordArrayLayout().getElementOffsetFromOrigin(vTableStartIndex()).toInt();
    }

    /**
     * @return the number of elements in the vTable array.
     */
    public int vTableLength() {
        return hub().vTableLength();
    }

    // The third region of memory is the iTable: used as an array of words containing pointers

    /**
     * @return the type category of information stored in the iTable
     */
    public Kind iTableKind() {
        return Kind.WORD;
    }

    public TypeDescriptor iTableType() {
        return JavaTypeDescriptor.WORD;
    }

    /**
     * @return index into the object (viewed as an array of words) of the beginning of the iTable.
     */
    public int iTableStartIndex() {
        return hub().iTableStartIndex;
    }

    /**
     * @return memory offset, relative to object origin, of the start of the iTable.
     */
    public int iTableOffset() {
        return Layout.wordArrayLayout().getElementOffsetFromOrigin(iTableStartIndex()).toInt();
    }

    /**
     * @return the number of elements in the iTable array.
     */
    public int iTableLength() {
        return hub().iTableLength;
    }

    // The fourth region of memory is the mTable: used as an array of integers

    /**
     * @return the type category of information stored in the mTable
     */
    public Kind mTableKind() {
        return Kind.INT;
    }

    /**
     * @return type descriptor for the kind of information stored in the mTable
     */
    public TypeDescriptor mTableType() {
        return JavaTypeDescriptor.INT;
    }

    /**
     * @return index into the object (viewed as an array of integers) of the beginning of the mTable.
     */
    public int mTableStartIndex() {
        return fields().Hub_mTableStartIndex.readInt(reference());
    }

    /**
     * @return memory offset, relative to object origin, of the start of the mTable.
     */
    public int mTableOffset() {
        return Layout.intArrayLayout().getElementOffsetFromOrigin(mTableStartIndex()).toInt();
    }

    /**
     * @return the number of elements in the mTable array.
     */
    public int mTableLength() {
        return fields().Hub_mTableLength.readInt(reference());
    }

    // The fifth and final region of memory is the reference Map: used as an array of integers

    /**
     * @return the type category of information stored in the reference map
     */
    public Kind referenceMapKind() {
        return Kind.INT;
    }

    /**
     * @return a type descriptor for the kind of information stored in the reference map
     */
    public TypeDescriptor referenceMapType() {
        return JavaTypeDescriptor.INT;
    }

    /**
     * @return index into the object (viewed as an array of integers) of the beginning of the reference map.
     */
    public int referenceMapStartIndex() {
        return fields().Hub_referenceMapStartIndex.readInt(reference());
    }

    /**
     * @return memory offset, relative to object origin, of the start of the reference map.
     */
    public int referenceMapOffset() {
        return Layout.intArrayLayout().getElementOffsetFromOrigin(referenceMapStartIndex()).toInt();
    }

    /**
     * @return the number of elements in the reference map.
     */
    public int referenceMapLength() {
        return fields().Hub_referenceMapLength.readInt(reference());
    }

    @Override
    protected Object createDeepCopy(DeepCopier context) {
        // Translate into local equivalent
        return hub();
    }

}

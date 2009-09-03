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
package com.sun.max.tele.object;

import com.sun.max.tele.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;


/**
 * Inspector's canonical surrogate for an object of type {@link Hub} in the VM.
 * Note that this kind of object cannot be expressed as an ordinary type;
 * although allocated as a single array of words (see {@link TeleHybridObject},
 * the memory is used in several sections with different properties, summarized
 * by the methods here.
 *
 * @author Michael Van De Vanter
 */
public abstract class TeleHub extends TeleHybridObject {

    protected TeleHub(TeleVM teleVM, Reference hubReference) {
        super(teleVM, hubReference);
    }

    private TeleClassActor teleClassActor = null;

    public TeleClassActor getTeleClassActor() {
        Pointer pointer = teleVM().getForwardedObject(reference().toOrigin());
        Reference classActorReference = teleVM().fields().Hub_classActor.readReference(Reference.fromOrigin(pointer));
        pointer = teleVM().getForwardedObject(classActorReference.toOrigin());
        classActorReference = Reference.fromOrigin(pointer);
        teleClassActor = (TeleClassActor) teleVM().makeTeleObject(classActorReference);
        return teleClassActor;
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

    /**
     * @return index into the object (viewed as an array of words) of the beginning of the vTable.
     */
    public int vTableStartIndex() {
        return Hub.vTableStartIndex();
    }

    /**
     * @return memory offset, relative to object origin, of the start of the vTable.
     */
    public Offset vTableOffset() {
        return teleVM().vmConfiguration().layoutScheme().wordArrayLayout.getElementOffsetFromOrigin(vTableStartIndex());
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

    /**
     * @return index into the object (viewed as an array of words) of the beginning of the iTable.
     */
    public int iTableStartIndex() {
        return hub().iTableStartIndex;
    }

    /**
     * @return memory offset, relative to object origin, of the start of the iTable.
     */
    public Offset iTableOffset() {
        return teleVM().vmConfiguration().layoutScheme().wordArrayLayout.getElementOffsetFromOrigin(iTableStartIndex());
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
     * @return index into the object (viewed as an array of integers) of the beginning of the mTable.
     */
    public int mTableStartIndex() {
        return teleVM().fields().Hub_mTableStartIndex.readInt(reference());
    }

    /**
     * @return memory offset, relative to object origin, of the start of the mTable.
     */
    public Offset mTableOffset() {
        return teleVM().vmConfiguration().layoutScheme().intArrayLayout.getElementOffsetFromOrigin(mTableStartIndex());
    }

    /**
     * @return the number of elements in the mTable array.
     */
    public int mTableLength() {
        return teleVM().fields().Hub_mTableLength.readInt(reference());
    }

    // The fifth and final region of memory is the reference Map: used as an array of integers

    /**
     * @return the type category of information stored in the reference map
     */
    public Kind referenceMapKind() {
        return Kind.INT;
    }

    /**
     * @return index into the object (viewed as an array of integers) of the beginning of the reference map.
     */
    public int referenceMapStartIndex() {
        return teleVM().fields().Hub_referenceMapStartIndex.readInt(reference());
    }

    /**
     * @return memory offset, relative to object origin, of the start of the reference map.
     */
    public Offset referenceMapOffset() {
        return teleVM().vmConfiguration().layoutScheme().intArrayLayout.getElementOffsetFromOrigin(referenceMapStartIndex());
    }

    /**
     * @return the number of elements in the reference map.
     */
    public int referenceMapLength() {
        return teleVM().fields().Hub_referenceMapLength.readInt(reference());
    }



    @Override
    protected Object createDeepCopy(DeepCopier context) {
        // Translate into local equivalent
        return hub();
    }

}

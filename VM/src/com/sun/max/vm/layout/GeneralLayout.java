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
/*VCSID=c8e6ec74-593a-4a34-a4fb-96a9b6d40644*/
package com.sun.max.vm.layout;

import com.sun.max.unsafe.*;
import com.sun.max.vm.grip.*;
import com.sun.max.vm.layout.Layout.*;
import com.sun.max.vm.reference.*;

/**
 * A general layout contains methods for manipulating an object without knowing whether
 * it is a tuple or an array or a hub.
 *
 * @author Bernd Mathiske
 */
public interface GeneralLayout {

    String name();

    GripScheme gripScheme();

    Pointer cellToOrigin(Pointer cell);

    Pointer originToCell(Pointer origin);

    Layout.Category category(Accessor accessor);

    boolean isArray(Accessor accessor);

    boolean isTuple(Accessor accessor);

    boolean isHybrid(Accessor accessor);

    SpecificLayout specificLayout(Accessor accessor);

    /**
     * Gets the size of the cell allocated for a given object. This include the size of the object's body,
     * its header as well as any extra bytes required to satisfy alignment constraints.
     */
    Size size(Accessor accessor);

    Reference readHubReference(Accessor accessor);

    Word readHubReferenceAsWord(Accessor accessor);

    void writeHubReference(Accessor accessor, Reference hubReference);

    Word readMisc(Accessor accessor);

    void writeMisc(Accessor accessor, Word value);

    Word compareAndSwapMisc(Accessor accessor, Word suspectedValue, Word newValue);

    Offset getOffsetFromOrigin(HeaderField headerField);

    Grip forwarded(Grip grip);

    /**
     * @return the forward reference stored by the GC in the object, null if none stored
     */
    Grip readForwardGrip(Accessor accessor);

    Grip readForwardGripValue(Accessor accessor);

    void writeForwardGrip(Accessor accessor, Grip forwardGrip);

    Grip compareAndSwapForwardGrip(Accessor accessor, Grip suspectedGrip, Grip forwardGrip);
}

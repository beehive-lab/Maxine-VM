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
package com.sun.max.vm.layout.prototype;

import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.grip.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.object.host.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;

/**
 * @author Bernd Mathiske
 */
public class PrototypeArrayHeaderLayout extends PrototypeGeneralLayout implements ArrayHeaderLayout {

    public PrototypeArrayHeaderLayout(GripScheme gripScheme) {
        super(gripScheme);
    }

    @Override
    public boolean isArrayLayout() {
        return true;
    }

    @INLINE
    public final Size getArraySize(Kind kind, int length) {
        throw ProgramError.unexpected("cannot compute array size in prototype layout");
    }

    @INLINE
    public final int headerSize() {
        throw ProgramError.unexpected();
    }

    public Kind getElementKind(Accessor accessor) {
        return HostObjectAccess.readHub(accessor).classActor.componentClassActor().kind;
    }

    public int arrayLengthOffset() {
        throw ProgramError.unexpected("cannot get array length offset in prototype layout");
    }

    public int readLength(Accessor accessor) {
        final Reference reference = (Reference) accessor;
        return HostObjectAccess.getArrayLength(reference.toJava());
    }

    public void writeLength(Accessor accessor, int length) {
        ProgramError.unexpected();
    }

    @INLINE
    public final int cellDataOffset() {
        throw ProgramError.unexpected();
    }
}

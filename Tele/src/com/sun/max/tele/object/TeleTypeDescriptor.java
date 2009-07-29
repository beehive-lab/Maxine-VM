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
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;


/**
 * Canonical surrogate for a  {@link TypeDescriptor} in the Target VM.
 *
 * @author Michael Van De Vanter
 *
 */
public final class TeleTypeDescriptor extends TeleDescriptor {

    protected TeleTypeDescriptor(TeleVM teleVM, Reference typeDescriptorReference) {
        super(teleVM, typeDescriptorReference);
    }

    private TypeDescriptor typeDescriptor;

    /**
     * @return local equivalent of the {@link TypeDescriptor} in the target VM.
     */
    public TypeDescriptor toJava() {
        if (typeDescriptor == null) {
            typeDescriptor = JavaTypeDescriptor.parseTypeDescriptor(string());
        }
        return typeDescriptor;
    }

    @Override
    protected Object createDeepCopy(DeepCopyContext context) {
        // Translate into local equivalent
        return toJava();
    }
}

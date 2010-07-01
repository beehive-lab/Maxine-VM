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
 * Canonical surrogate for a {@link Descriptor} in the VM.
 *
 * @author Michael Van De Vanter
 */
public abstract class TeleDescriptor extends TeleTupleObject {

    protected TeleDescriptor(TeleVM vm, Reference descriptorReference) {
        super(vm, descriptorReference);
    }

    private Descriptor descriptor;

    /**
     * @return local equivalent of the {@link TypeDescriptor} in the VM.
     */
    public Descriptor descriptor() {
        if (descriptor == null) {
            final Reference stringReference = vm().teleFields().Descriptor_string.readReference(reference());
            final TeleString teleString = (TeleString) heap().makeTeleObject(stringReference);
            String string = teleString.getString();
            if (string != null) {
                if (this instanceof TeleTypeDescriptor) {
                    descriptor = JavaTypeDescriptor.parseTypeDescriptor(string);
                } else {
                    assert this instanceof TeleSignatureDescriptor;
                    descriptor = SignatureDescriptor.create(string);
                }
            }
        }
        return descriptor;
    }

    @Override
    protected Object createDeepCopy(DeepCopier context) {
        // Translate into local equivalent
        return descriptor();
    }
}

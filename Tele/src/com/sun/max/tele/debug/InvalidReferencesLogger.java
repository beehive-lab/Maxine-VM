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
package com.sun.max.tele.debug;

import java.util.*;

import com.sun.max.tele.*;
import com.sun.max.vm.reference.*;

/**
 * In progress support for tolerating invalid references and reporting them to the end-user.
 * Keeps track of invalid references detected during execution, the epoch when it was first detected it, and the
 * type expected, if any.
 *
 * @author Laurent Daynes
 */
public class InvalidReferencesLogger {

    static class InvalidReference {
        final Reference reference;
        final Class expectedType;
        final long epoch;
        // In case a same reference may be expected multiple types, if the VM state is really screwed up.
        private InvalidReference next;

        InvalidReference(Reference reference, Class expectedType, long epoch) {
            this.reference = reference;
            this.expectedType = expectedType;
            this.epoch = epoch;
        }
        void add(InvalidReference ref) {
            ref.next = next;
            next = ref;
        }
    }
    private final HashMap<Reference, InvalidReference> invalidReferences = new HashMap<Reference, InvalidReference>();
    final TeleVM vm;

    public InvalidReferencesLogger(TeleVM vm) {
        this.vm = vm;
    }

    public void record(Reference reference, Class expectedType) {
        final long epoch = vm.teleProcess().epoch();
        InvalidReference iref = invalidReferences.get(reference);
        if (iref == null) {
            invalidReferences.put(reference, new InvalidReference(reference, expectedType, epoch));
        } else if (iref.epoch != epoch || iref.expectedType != expectedType) {
            iref.add(new  InvalidReference(reference, expectedType, epoch));
        }
    }

    public boolean isEmpty() {
        return invalidReferences.isEmpty();
    }
}

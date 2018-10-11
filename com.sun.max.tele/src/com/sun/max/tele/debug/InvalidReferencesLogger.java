/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.tele.debug;

import java.util.*;

import com.sun.max.tele.*;
import com.sun.max.tele.reference.*;

/**
 * In progress support for tolerating invalid references and reporting them to the end-user.
 * Keeps track of invalid references detected during execution, the epoch when it was first detected it, and the
 * type expected, if any.
 */
public class InvalidReferencesLogger {

    static class InvalidReference {
        final RemoteReference reference;
        final Class expectedType;
        final long epoch;
        // In case a same reference may be expected multiple types, if the VM state is really screwed up.
        private InvalidReference next;

        InvalidReference(RemoteReference reference, Class expectedType, long epoch) {
            this.reference = reference;
            this.expectedType = expectedType;
            this.epoch = epoch;
        }
        void add(InvalidReference ref) {
            ref.next = next;
            next = ref;
        }
    }
    private final HashMap<RemoteReference, InvalidReference> invalidReferences = new HashMap<RemoteReference, InvalidReference>();
    final TeleVM vm;

    public InvalidReferencesLogger(TeleVM vm) {
        this.vm = vm;
    }

    public void record(RemoteReference reference, Class expectedType) {
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

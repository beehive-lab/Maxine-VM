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
package com.sun.max.tele.reference;

import java.lang.ref.*;
import java.rmi.*;
import java.util.*;

import com.sun.max.tele.*;
import com.sun.max.tele.interpreter.*;
import com.sun.max.tele.reference.direct.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.reference.Reference;


/**
 * Manager local objects that are wrapped so that they can substitute for a remote
 * object via a {@link RemoteReference}. Getting the raw address from such a reference
 * is unsupported.  The {@link RemoteReferenceScheme} used for remote inspection
 * handles these references specially.
 * <p>
 * These are used typically by the remote interpreter.
 * <p>
 * These references are canonical, and their
 * memory status of these references is permanently {@link ObjectStatus#LIVE}.
 *
 * @see Reference
 * @see RemoteReferenceScheme
 * @see TeleInterpreter
 */
public final class LocalObjectRemoteReferenceManager extends AbstractVmHolder {


    private final Map<Object, WeakReference<LocalObjectRemoteReference>> objectToLocalObjectRemoteReference = new HashMap<Object, WeakReference<LocalObjectRemoteReference>>();

    public LocalObjectRemoteReferenceManager(TeleVM vm) {
        super(vm);
    }

    /**
     * Returns a canonicalized reference that encapsulates a local object, disguised as a remote object in the VM.
     */
    public RemoteReference make(Object object) {
        if (object == null) {
            return vm().referenceManager().zeroReference();
        }
        synchronized (objectToLocalObjectRemoteReference) {
            final WeakReference<LocalObjectRemoteReference> r = objectToLocalObjectRemoteReference.get(object);
            if (r != null) {
                return r.get();
            }
            final LocalObjectRemoteReference localTeleReference = new LocalObjectRemoteReference(vm(), object);
            objectToLocalObjectRemoteReference.put(object, new WeakReference<LocalObjectRemoteReference>(localTeleReference));
            return localTeleReference;
        }
    }

    public int referenceCount() {
        return objectToLocalObjectRemoteReference.size();
    }

    /**
     * A local object wrapped into a {@link Remote Reference}, allowing it to stand in for an object in the VM, for
     * example by the remote interpreter.
     * <p>
     * The memory status is permanently {@link ObjectStatus#LIVE}.
     *
     * @see {@link TeleInterpreter}
     */
    public final class LocalObjectRemoteReference extends RemoteReference {

        private final Object object;

        public LocalObjectRemoteReference(TeleVM vm, Object object) {
            super(vm);
            this.object = object;
        }

        public Object object() {
            return object;
        }

        @Override
        public ObjectStatus status() {
            return ObjectStatus.LIVE;
        }

        @Override
        public Address origin() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Address lastValidOrigin() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isForwarded() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Address forwardedFrom() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String gcDescription() {
            return null;
        }

        @Override
        protected void finalize() throws Throwable {
            synchronized (objectToLocalObjectRemoteReference) {
                objectToLocalObjectRemoteReference.remove(object);
            }
            super.finalize();
        }

        @Override
        public boolean isLocal() {
            return true;
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof LocalObjectRemoteReference) {
                final LocalObjectRemoteReference localTeleRef = (LocalObjectRemoteReference) other;
                return object == localTeleRef.object();
            }
            return false;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(object);
        }

        @Override
        public String toString() {
            return object.toString();
        }

    }

}

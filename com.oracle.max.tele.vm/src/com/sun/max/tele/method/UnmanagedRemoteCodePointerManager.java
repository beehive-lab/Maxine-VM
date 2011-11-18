/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.tele.method;

import java.lang.ref.*;
import java.util.*;

import com.sun.max.tele.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;

/**
 * A manager for pointers to machine code in a region of VM memory where:
 * <ul>
 * <li>The region is assumed not to move during its lifetime; and</li>
 * <li>Code, once created, never moves and is never collected/evicted.</li>
 * </ul>
 */
public class UnmanagedRemoteCodePointerManager extends AbstractRemoteCodePointerManager {

    private final VmCodeCacheRegion codeCacheRegion;

    /**
     * Map:  address in VM --> a {@link RemoteCodePointer} that refers to the machine code at that location.
     */
    private Map<Long, WeakReference<RemoteCodePointer>> addressToCodePointer = new HashMap<Long, WeakReference<RemoteCodePointer>>();

    /**
     * Creates a manager for pointers to machine code a particular region
     * of memory in the VM, presumed to be an unmanaged region in which code
     * never moves and is never evicted.
     */
    public UnmanagedRemoteCodePointerManager(TeleVM vm, VmCodeCacheRegion codeCacheRegion) {
        super(vm);
        this.codeCacheRegion = codeCacheRegion;
    }

    public CodeHoldingRegion codeRegion() {
        return codeCacheRegion;
    }

    public boolean isValidCodePointer(Address address) throws TeleError {
        TeleError.check(codeCacheRegion.memoryRegion().contains(address), "Location is outside region");
        final TeleCompilation compilation = codeCacheRegion.findCompilation(address);
        if (compilation != null) {
            return compilation.isValidCodeLocation(address);
        }
        return false;
    }

    public RemoteCodePointer makeCodePointer(Address address) throws TeleError {
        TeleError.check(codeCacheRegion.memoryRegion().contains(address), "Location is outside region");
        RemoteCodePointer codePointer = null;
        final WeakReference<RemoteCodePointer> existingRef = addressToCodePointer.get(address.toLong());
        if (existingRef != null) {
            codePointer = existingRef.get();
        }
        if (codePointer == null && isValidCodePointer(address)) {
            codePointer = new ConstantRemoteCodePointer(address);
            addressToCodePointer.put(address.toLong(), new WeakReference<RemoteCodePointer>(codePointer));
        }
        return codePointer;
    }

    public int activePointerCount() {
        int count = 0;
        for (WeakReference<RemoteCodePointer> weakRef : addressToCodePointer.values()) {
            if (weakRef.get() != null) {
                count++;
            }
        }
        return count;
    }

    public int totalPointerCount() {
        return addressToCodePointer.size();
    }

}

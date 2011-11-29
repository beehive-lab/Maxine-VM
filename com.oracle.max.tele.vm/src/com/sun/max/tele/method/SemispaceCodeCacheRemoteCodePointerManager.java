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
import com.sun.max.tele.object.*;
import com.sun.max.tele.object.TeleTargetMethod.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.type.*;


/**
 * A manager for pointers to machine code allocated in a {@link SemiSpaceCodeRegion}.
 *  This manager:
 * <ul>
 * <li>assumes that machine code allocated can be relocated and eventually collected/evicted.</li>
 * <li>assumes that there can only be code in the region of the kind managed and described by
 * (heap) object instances of {@link TeleMethocActor}.</li>
 * <li>creates <em>canonical pointers</em>.
 * </ul>
 * This implementation depends on knowledge of the internal workings of {@link TargetMethod}.
 *
 * @see TargetMethod
 * @see VmCodeCacheRegion
 * @see TeleTargetMethod
 */
public class SemispaceCodeCacheRemoteCodePointerManager extends AbstractRemoteCodePointerManager {

    private final VmCodeCacheRegion codeCacheRegion;

    /**
     * A two level map.  For each compilation for which code pointers have been created,
     * map integers (offset into the byte array containing code) to code pointers.
     * <pre>
     *   Compilation  -->  [ Integer  -->  WeakReference&lt;RemoteCodePointer&gt; ]
     * </pre>
     */
    private final Map<TeleCompilation, Map<Integer, WeakReference<RelativeRemoteCodePointer> > > pointerMaps =
        new HashMap<TeleCompilation, Map<Integer, WeakReference<RelativeRemoteCodePointer> > >();

    /**
     * Creates a manager for pointers to machine code a particular region
     * of memory in the VM, presumed to be an unmanaged region in which code
     * never moves and is never evicted.
     */
    public SemispaceCodeCacheRemoteCodePointerManager(TeleVM vm, VmCodeCacheRegion codeCacheRegion) {
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
        final TeleCompilation compilation = codeCacheRegion.findCompilation(address);
        if (compilation == null || !compilation.isValidCodeLocation(address)) {
            return null;
        }
        RelativeRemoteCodePointer codePointer = null;
        final TeleTargetMethod teleTargetMethod = compilation.teleTargetMethod();
        final Address codeByteArrayOrigin = teleTargetMethod.codeCacheObjectOrigin(CodeCacheReferenceKind.CODE);
        final int codeOffset = objects().unsafeArrayElementAddressToIndex(Kind.BYTE, codeByteArrayOrigin, address);
        Map<Integer, WeakReference<RelativeRemoteCodePointer>> pointerMap = pointerMaps.get(compilation);
        if (pointerMap == null) {
            pointerMap = new HashMap<Integer, WeakReference<RelativeRemoteCodePointer> >();
            pointerMaps.put(compilation, pointerMap);
        } else {
            final WeakReference<RelativeRemoteCodePointer> weakReference = pointerMap.get(codeOffset);
            if (weakReference != null) {
                codePointer = weakReference.get();
            }
        }
        if (codePointer == null) {
            codePointer = new RelativeRemoteCodePointer(teleTargetMethod, codeOffset);
            pointerMap.put(codeOffset, new WeakReference<RelativeRemoteCodePointer>(codePointer));
        }
        return codePointer;
    }

    public int activePointerCount() {
        int count = 0;
        for (Map<Integer, WeakReference<RelativeRemoteCodePointer>> pointerMap : pointerMaps.values()) {
            for (WeakReference<RelativeRemoteCodePointer> weakReference : pointerMap.values()) {
                if (weakReference.get() != null) {
                    count++;
                }
            }
        }
        return count;
    }

    public int totalPointerCount() {
        int count = 0;
        for (Map<Integer, WeakReference<RelativeRemoteCodePointer>> pointerMap : pointerMaps.values()) {
            count += pointerMap.size();
        }
        return count;
    }

    /**
     * A pointer a machine code location in a VM method compilation, represented as an
     * offset relative to the beginning of the code array so that it can track possible
     * code location.
     */
    private final class RelativeRemoteCodePointer implements RemoteCodePointer {

        private final TeleTargetMethod teleTargetMethod;
        private final int codeOffset;

        public RelativeRemoteCodePointer(TeleTargetMethod teleTargetMethod, int codeOffset) {
            this.teleTargetMethod = teleTargetMethod;
            this.codeOffset = codeOffset;
        }

        public Address getAddress() {
            if (teleTargetMethod.isLive()) {
                final Address codeByteArrayOrigin = teleTargetMethod.codeCacheObjectOrigin(CodeCacheReferenceKind.CODE);
                return objects().unsafeArrayIndexToAddress(Kind.BYTE, codeByteArrayOrigin, codeOffset);
            }
            return Address.zero();
        }

        public boolean isLive() {
            return teleTargetMethod.isLive();
        }

    }

}

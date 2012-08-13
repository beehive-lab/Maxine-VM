/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.util.*;

import com.sun.max.tele.MaxPlatform.OS;
import com.sun.max.tele.*;
import com.sun.max.tele.method.*;
import com.sun.max.tele.reference.*;
import com.sun.max.unsafe.*;

/**
 * Provides access to native functions defined in shared libraries loaded into the VM.
 */
public class TeleNativeLibraries {

    private static class TargetVMLibInfo {
        private RemoteReference reference;
        private TeleVM vm;

        protected TargetVMLibInfo(TeleVM vm, RemoteReference teleNativeLibraryReference) {
            this.vm = vm;
            this.reference = teleNativeLibraryReference;
        }

        TeleNativeLibrary getTeleNativeLibrary(TeleNativeLibrary oldTeleNativeLibrary) {
            TeleNativeLibrary result;
            if (oldTeleNativeLibrary == null) {
                String libPath;
                final Pointer pathAsCString = vm.fields().DynamicLinker$LibInfo_pathAsCString.readWord(reference).asPointer();
                OS os = vm.platform().getOS();
                if (pathAsCString.isNotZero()) {
                    libPath = stringFromCString(vm, pathAsCString);
                } else {
                    // this is mainHandle
                    libPath = new File(vm.bootImageFile().getParent(), os.libjvmName() + "." + os.libSuffix()).getAbsolutePath();
                }
                result = TeleNativeLibrary.create(vm, os, libPath);
            } else {
                result = oldTeleNativeLibrary;
            }
            // These values may change between calls to this method, although once set they are constant.
            final Pointer sentinelAsCString = vm.fields().DynamicLinker$LibInfo_sentinelAsCString.readWord(reference).asPointer();
            String sentinel = sentinelAsCString.isZero() ? null : stringFromCString(vm, sentinelAsCString);
            final Address sentinelAddress = vm.fields().DynamicLinker$LibInfo_sentinelAddress.readWord(reference).asAddress();
            result.setSentinel(sentinel, sentinelAddress);
            return result;
        }

        private static String stringFromCString(TeleVM vm, Pointer cString) {
            byte[] bytes = new byte[1024];
            int index = 0;
            while (true) {
                byte b = vm.memoryIO().readByte(cString, index);
                if (b == 0) {
                    break;
                }
                bytes[index++] = b;
            }
            return new String(bytes, 0, index);

        }
    }

    private final static ArrayList<TeleNativeLibrary> libs = new ArrayList<TeleNativeLibrary>();

    public final static List<TeleNativeLibrary> libs() {
        return libs;
    }

    /**
     * Add any new libraries since last refresh. It is possible that a sentinel value has appeared
     * since the last refresh, so we check that.
     * @param vm
     */
    public static void update(TeleVM vm) throws Exception {
        int length = vm.fields().DynamicLinker_libInfoIndex.readInt(vm);
        RemoteReference libInfoArrayReference = vm.fields().DynamicLinker_libInfoArray.readRemoteReference(vm);
        for (int index = 0; index < length; index++) {
            boolean newLib = index >= libs.size();
            if (newLib || libs.get(index).base().isZero()) {
                RemoteReference libInfoReference = libInfoArrayReference.readArrayAsRemoteReference(index);
                TargetVMLibInfo targetVMLibInfo = new TargetVMLibInfo(vm, libInfoReference);
                TeleNativeLibrary teleNativeLibrary = processLibrary(targetVMLibInfo, newLib ? null : libs.get(index));
                if (index >= libs.size()) {
                    libs.add(teleNativeLibrary);
                }
            }
        }

    }

    /**
     * Read the symbols from the library using OS-specific object format.
     * If we know the sentinel symbol's address, then gather all the native functions.
     * @param TargetVMLibInfo
     * @param oldTeleNativeLibrary null if new library else existing TeleNativeLibrary
     * @return associated TeleNativeLibrary
     * @throws Exception
     */
    private static TeleNativeLibrary processLibrary(TargetVMLibInfo targetVMLibInfo, TeleNativeLibrary oldTeleNativeLibrary) throws Exception {
        TeleNativeLibrary teleNativeLibrary = targetVMLibInfo.getTeleNativeLibrary(oldTeleNativeLibrary);
        teleNativeLibrary.gatherFunctions();
        return teleNativeLibrary;
    }

}

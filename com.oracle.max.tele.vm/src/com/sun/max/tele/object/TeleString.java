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
package com.sun.max.tele.object;

import com.sun.max.jdwp.vm.proxy.*;
import com.sun.max.tele.*;
import com.sun.max.tele.data.*;
import com.sun.max.tele.field.*;
import com.sun.max.tele.reference.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.type.*;

/**
 * Canonical surrogate for an object of type {@link String} in the VM.
 */
public final class TeleString extends TeleTupleObject implements StringProvider {

    private static final TeleInstanceIntFieldAccess String_count;
    private static final TeleInstanceIntFieldAccess String_offset;
    private static boolean jdk7u6orNewer = true;

    static {
        /**
         * Starting with JDK 7 update 6, the String class no longer has the offset and count fields.
         * We want to support both String variants (this is necessary until we drop support for JDK 6).
         */
        try {
            String.class.getDeclaredField("offset");
            jdk7u6orNewer = false;
        } catch (NoSuchFieldException e) {
        }
        if (jdk7u6orNewer) {
            String_count = null;
            String_offset = null;
        } else {
            String_count = new TeleInstanceIntFieldAccess(String.class, "count");
            String_offset = new TeleInstanceIntFieldAccess(String.class, "offset");
        }
    }

    /**
     * Returns a local copy of a Java {@link String} object in the VM's heap, without creating an instance of {@link TeleString}.
     * <p>
     * <strong>Note:</strong> no check is made that the reference points to a {@link String} object in VM.
     *
     * @param stringRef assumed to refer to a {@link String} object in the VM.
     * @return A local {@link String} duplicating the remote object's contents.
     * @throws InvalidReferenceException if the argument does not point a valid heap object.
     */
    public static String getString(TeleVM vm, RemoteReference stringRef) throws InvalidReferenceException {
        vm.referenceManager().checkReference(stringRef);
        try {
            final RemoteReference charArrayRef = vm.fields().String_value.readRemoteReference(stringRef);
            if (charArrayRef.isZero()) {
                return null;
            }
            vm.referenceManager().checkReference(charArrayRef);
            int charCount = 0;
            int charOffset = 0;
            if (jdk7u6orNewer) {
                charCount = vm.objects().unsafeReadArrayLength(charArrayRef);
            } else {
                charCount = String_count.readInt(stringRef);
                charOffset = String_offset.readInt(stringRef);
            }
            final char[] chars = new char[charCount];
            vm.objects().unsafeCopyElements(Kind.CHAR, charArrayRef, charOffset, chars, 0, charCount);
            return new String(chars);
        } catch (DataIOError dataIOError) {
            return null;
        }
    }

    /**
     * Returns a local copy of the contents of a {@link String} object in the VM's heap,
     * using low level mechanisms and performing no checking that the location
     * or object are valid.
     * <p>
     * The intention is to provide a fast, low-level mechanism for reading strings that
     * can be used outside of the AWT event thread without danger of deadlock,
     * for example on the canonical reference machinery.
     * <p>
     * <strong>Unsafe:</strong> this method depends on knowledge of the implementation of
     * class {@link String}.
     *
     * @param origin a {@link String} object in the VM
     * @return A local {@link String} duplicating the remote object's contents, null if it can't be read.
     */
    public static String getStringUnsafe(TeleVM vm, Address origin) {
        // Work only with temporary references that are unsafe across GC
        // Do no testing to determine if the reference points to a valid String object in live memory.
        try {
            final RemoteReference stringRef = vm.referenceManager().makeTemporaryRemoteReference(origin);
            final Address charArrayAddress = stringRef.readWord(vm.fields().String_value.fieldActor().offset()).asAddress();
            final RemoteReference charArrayRef = vm.referenceManager().makeTemporaryRemoteReference(charArrayAddress);
            int charCount = 0;
            int charOffset = 0;
            if (jdk7u6orNewer) {
                charCount = vm.objects().unsafeReadArrayLength(charArrayRef);
            } else {
                charCount = String_count.readInt(stringRef);
                charOffset = String_offset.readInt(stringRef);
            }
            final char[] chars = new char[charCount];
            vm.objects().unsafeCopyElements(Kind.CHAR, charArrayRef, charOffset, chars, 0, charCount);
            return new String(chars);
        } catch (DataIOError dataIOError) {
            return null;
        }
    }

    private String string;

    public String getString() {
        if (status().isNotDead()) {
            String s = getString(vm(), reference());
            if (s != null) {
                string = SymbolTable.intern(s);
            }
        }
        return string;
    }

    protected TeleString(TeleVM vm, RemoteReference stringReference) {
        super(vm, stringReference);
    }

    @Override
    protected Object createDeepCopy(DeepCopier context) {
        // Translate into local equivalent
        return getString();
    }

    @Override
    public String maxineRole() {
        return "String";
    }

    @Override
    public String maxineTerseRole() {
        return "String.";
    }

    public String stringValue() {
        return getString();
    }

    @Override
    public boolean hasTextualVisualization() {
        return true;
    }

    @Override
    public String textualVisualization() {
        return stringValue();
    }

}

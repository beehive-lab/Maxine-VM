/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.unsafe;

import static com.sun.max.vm.MaxineVM.*;

import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.reference.*;

/**
 * A {@code CodePointer} is a tagged pointer that is known to reference native code.
 *
 * The least significant bit is 1. The remaining 63 bits are interpreted as an offset from the lowest existing {@link CodeRegion}
 * start address. This is typically the start address of the {@linkplain Code#bootCodeRegion() boot code region}.
 *
 * Note that the interpretation as a 63-bit offset from the boot code region start is solely internal. All values passed into
 * creation methods are supposed to be full 64-bit pointers. All values returned from conversion methods are full 64-bit pointers.
 *
 * {@code CodePointer}s, in spite of their pointer characteristics, inherit from {@link Object} instead of {@link Word} to remain
 * visible to the memory manager. They are recognised as references but treated specially.
 */
public final class CodePointer {

    private static long BASE_ADDRESS = MaxineVM.isHosted() ? 0x1000000000000000L : Code.bootCodeRegion().start().toLong();

    /**
     * Set the base address. This is needed since the static field will otherwise not be properly initialised
     * if the VM is running properly. Initialisation is done along with code manager initialisation in
     * {@link Code#initialize()}.
     */
    public static void initialize(Address baseAddress) {
        BASE_ADDRESS = baseAddress.toLong();
    }

    @HOSTED_ONLY
    private long tagged;

    private CodePointer() { }

    @HOSTED_ONLY
    private CodePointer(long value) {
        tagged = value;
    }

    @Override
    public int hashCode() {
        return (int) toTaggedLong();
    }

    @INLINE
    public boolean equals(CodePointer other) {
        if (isHosted()) {
            return this.tagged == other.tagged;
        }
        return this.toTaggedLong() == other.toTaggedLong();
    }

    @INLINE
    public boolean equals(Word other) {
        return this.toAddress().equals(other.asAddress());
    }

    @Override
    public boolean equals(Object other) {
        throw ProgramError.unexpected("must not call equals(Object) with CodePointer argument");
    }

    @INLINE
    public boolean isZero() {
        return toLong() == 0L;
    }

    @INLINE
    public static CodePointer zero() {
        return from(0L);
    }

    @INLINE
    private static long tag(long value) {
        return ((value - BASE_ADDRESS) << 1) | 1;
    }

    @INLINE
    private static long untag(long value) {
        return BASE_ADDRESS + (value >> 1);
    }

    /**
     * Relocates a {@code CodePointer} by a given {@link Offset}.
     *
     * @param offset the offset by which the value is to be relocated
     * @return the relocated raw tagged value
     */
    @INLINE
    public CodePointer relocate(Offset offset) {
        return from(toLong() + offset.toLong());
    }

    @INLINE
    public static CodePointer from(long value) {
        if (isHosted()) {
            return new CodePointer(tag(value));
        }
        return UnsafeCast.asCodePointer(tag(value));
    }

    @INLINE
    public static CodePointer from(Address address) {
        return from(address.toLong());
    }

    @INLINE
    public static CodePointer from(Word word) {
        return from(word.asAddress());
    }

    /**
     * Cast an already tagged word into a {@code CodePointer}.
     */
    @INLINE
    public static CodePointer fromTaggedLong(long value) {
        if (isHosted()) {
            return new CodePointer(value);
        }
        return UnsafeCast.asCodePointerTagged(value);
    }

    /**
     * Get the tagged raw value of a {@code CodePointer}.
     */
    @INLINE
    public long toTaggedLong() {
        if (isHosted()) {
            return tagged;
        }
        return UnsafeCast.asTaggedLong(this);
    }

    @INLINE
    public long toLong() {
        if (isHosted()) {
            return untag(tagged);
        }
        return untag(UnsafeCast.asLong(this));
    }

    @INLINE
    public int toInt() {
        return (int) toLong();
    }

    @INLINE
    public Address toAddress() {
        return Address.fromLong(toLong());
    }

    @INLINE
    public Pointer toPointer() {
        return Pointer.fromLong(toLong());
    }

    @INLINE
    public Offset toOffset() {
        return Offset.fromLong(toLong());
    }

    @INLINE
    public TargetMethod toTargetMethod() {
        return Code.codePointerToTargetMethod(this.toPointer());
    }

    @INLINE
    public static boolean isCodePointer(Reference ref) {
        return ref.isTagged();
    }

    @INLINE
    public CodePointer plus(CodePointer cp) {
        return from(toLong() + cp.toLong());
    }

    @INLINE
    public CodePointer plus(int d) {
        return from(toLong() + d);
    }

    @INLINE
    public CodePointer plus(long d) {
        return from(toLong() + d);
    }

    @INLINE
    public CodePointer plus(Offset d) {
        return from(toLong() + d.toLong());
    }

    @INLINE
    public CodePointer minus(CodePointer cp) {
        return from(toLong() - cp.toLong());
    }

    @INLINE
    public CodePointer minus(int d) {
        return from(toLong() - d);
    }

    @INLINE
    public CodePointer minus(Offset d) {
        return from(toLong() - d.toLong());
    }

    @INLINE
    public CodePointer minus(Address d) {
        return from(toLong() - d.toLong());
    }

    @INLINE
    public String toHexString() {
        return toPointer().toHexString();
    }

    @INLINE
    public String to0xHexString() {
        return toPointer().to0xHexString();
    }

    @INLINE
    @Override
    public String toString() {
        return to0xHexString();
    }

}

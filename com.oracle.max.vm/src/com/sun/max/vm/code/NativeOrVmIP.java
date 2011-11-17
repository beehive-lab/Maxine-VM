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
package com.sun.max.vm.code;

import com.sun.max.unsafe.*;
import com.sun.max.vm.compiler.target.*;

/**
 * A concurrency-safe representation of an IP (instruction pointer) that
 * abstracts away from the IP's nature (native function, or Java method). A native function
 * IP is stored as a raw {@link Pointer}, while a virtual-machine (VM, Java) IP is represented
 * by a {@link TargetMethod} and offset.
 * <p>
 * This class is useful for storing IPs in fields in a concurrency-safe way. For storing IPs
 * in local variables, {@link CodePointer}s should be used.
 */
public class NativeOrVmIP {

    public static final NativeOrVmIP ZERO = new NativeOrVmIP();

    private TargetMethod tm;
    private int pos;

    private Pointer nativeIP;

    /**
     * The default constructor should be used with extreme care, as it creates an ill-formed
     * instance of this class.
     */
    public NativeOrVmIP() {
        tm = null;
        pos = 0;
        nativeIP = Pointer.zero();
    }

    /**
     * Returns a special pointer to code in a method compilation.
     * Fails if the IP points into native code somewhere and not at a VM method compilation.
     */
    public final CodePointer vmIP() {
        assert tm != null : "target method must not be null";
        assert nativeIP.isZero() : "raw IP must be zero";
        return tm.codeAt(pos);
    }

    /**
     * Returns an absolute location pointer to code in native code not known to the VM.
     * Fails if the IP points into a VM method compilation.
     */
    public final Pointer nativeIP() {
        assert tm == null : "target method must be null";
        return nativeIP;
    }

    /**
     * Returns the IP represented by this object as a {@link Pointer}, no matter what kind of
     * IP is represented.
     */
    public final Pointer asPointer() {
        if (tm == null) {
            return nativeIP();
        } else {
            return vmIP().toPointer();
        }
    }

    /**
     * Smart setter: determines whether the IP is a native or VM one.
     */
    public final void derive(Pointer ip) {
        tm = targetMethodFor(ip);
        if (tm == null) {
            pos = 0;
            nativeIP = ip;
        } else {
            pos = tm.posFor(CodePointer.from(ip));
            nativeIP = Pointer.zero();
        }
    }

    /**
     * Primitive setter, accepting values for all three fields. Illegal value combinations are
     * rejected.
     */
    public final void primitiveSet(TargetMethod tm, int pos, Pointer nativeIP) {
        assert !(tm != null && !nativeIP.isZero());
        this.tm = tm;
        this.pos = pos;
        this.nativeIP = nativeIP;
    }

    /**
     * Copy the values from another instance into this one.
     */
    public final void copyFrom(NativeOrVmIP other) {
        primitiveSet(other.tm, other.pos, other.nativeIP);
    }

    public final TargetMethod targetMethod() {
        return tm;
    }

    /**
     * Gets the {@link TargetMethod}, if any, whose code currently
     * includes the specified location.
     * May be overridden for use by the Inspector.
     */
    protected TargetMethod targetMethodFor(Pointer ip) {
        return Code.codePointerToTargetMethod(ip);
    }

}

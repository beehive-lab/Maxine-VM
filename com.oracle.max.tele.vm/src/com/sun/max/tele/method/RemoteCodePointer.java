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

import com.sun.max.unsafe.*;

/**
 * Represents a machine code location in the VM.
 * <p>
 * The absolute location may change, or may become obsolete in a managed code region.
 */
public interface RemoteCodePointer {

    /**
     * Gets the current absolute location in VM memory of a byte in an area of machine code.
     *
     * @return non-null: the current memory location of the code if live, {@link Address#zero()} if not live.
     */
    Address getAddress();

    /**
     * Gets the status of the machine code with respect to possible code eviction.
     *
     * @return non-null:  {@code true} if the machine code is still used by the VM, {@code false} if it has been evicted
     * and is no longer used.
     */
    boolean isCodeLive();
}

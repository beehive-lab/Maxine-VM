/*
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.tele;

import com.sun.max.tele.reference.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Access to low-level data reading from VM memory.
 */
public interface MaxMemoryIO {

    /**
     * Low-level read of a word as a generic boxed value from memory of the VM.
     *
     * @param address a location in VM memory
     * @return a generic boxed value based on the contents of the word in VM memory.
     */
    Value readWordValue(Address address);

    /**
     * Low-level read of a array element word as a generic boxed value from memory of the VM.
     * <p>
     * Does not check that there is a valid array at the specified location.
     *
     * @param kind identifies one of the basic VM value types
     * @param reference location in the VM presumed (but not checked) to be an array origin
     * @param index offset into the array
     * @return a generic boxed value based on the contents of the word in VM memory.
     */
    Value readArrayElementValue(Kind kind, RemoteReference reference, int index);

    /**
     * Uses low level memory access to read an array of bytes from VM memory.
     *
     * @param address a location in VM memory
     * @param bytes an array into which as many bytes as possible copied from
     * VM memory will be written.
     */
    void readBytes(Address address, byte[] bytes);

    /**
     * Low level memory read of an integer from VM memory.
     *
     * @param address  a location in VM memory
     * @return the integer value the memory location.
     */
    int readInt(Address address);

    /**
     * Reads an {@code int} from VM memory.
     *
     * @param address a location in VM memory
     * @param offset from the location at which to read
     * @return the current contents of VM memory at that location as an int
     */
    int readInt(Address address, int idOffset);

}

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
package com.sun.max.tele.method;

import com.sun.max.unsafe.*;


/**
 * Exception thrown when an memory address is known to be an
 * illegal location for machine code, e.g. in a heap.
 */
public class InvalidCodeAddressException extends Exception {

    private final Address address;

    public InvalidCodeAddressException(Address address, String message) {
        super(message);
        this.address = address;
    }

    /**
     * @return the offending address, possibly null or 0
     */
    public Address getAddress() {
        return address;
    }

    /**
     * @return text describing the offending address, possibly "null".
     */
    public String getAddressString() {
        return address == null ? "null" : address.to0xHexString();
    }

}

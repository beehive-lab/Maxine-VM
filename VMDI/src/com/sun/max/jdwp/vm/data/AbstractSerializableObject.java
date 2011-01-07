/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.jdwp.vm.data;

import java.io.*;
import java.util.logging.*;

import com.sun.max.jdwp.vm.core.*;

/**
 * Convenience class that provides a default implementation of SerializableObject that is useful in most cases. Objects
 * that should be transmitted over the JDWP stream instead of generating proxy objects should be subclasses of this
 * class. Transmitting the object can be a performance improvement.
 *
 * @author Thomas Wuerthinger
 */
class AbstractSerializableObject implements SerializableObject {

    private static final Logger LOGGER = Logger.getLogger(AbstractSerializableObject.class.getName());

    public byte[] getSerializedData() {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(out);
            oos.writeObject(this);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error while serializing object " + this + "!", e);
        }

        return out.toByteArray();
    }
}

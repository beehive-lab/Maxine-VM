/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.max.jdwp.vm.data;

import java.io.*;
import java.util.logging.*;

import com.sun.max.jdwp.vm.core.*;

/**
 * Convenience class that provides a default implementation of SerializableObject that is useful in most cases. Objects that should be transmitted over the JDWP stream instead of generating proxy objects should
 * be subclasses of this class. Transmitting the object can be a performance improvement.
 *
 * @author Thomas Wuerthinger
 *
 */
class AbstractSerializableObject implements SerializableObject {

    private static final Logger LOGGER = Logger.getLogger(AbstractSerializableObject.class.getName());

    @Override
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

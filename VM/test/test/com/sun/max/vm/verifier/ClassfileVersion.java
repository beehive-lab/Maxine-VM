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
/*VCSID=ce74a339-f99a-4df5-80c2-0272b15aa5cc*/
package test.com.sun.max.vm.verifier;

import java.io.*;

import com.sun.max.program.*;
import com.sun.max.vm.prototype.*;

/**
 * Utility class to determine the class file version of a given class without actually loading the class.
 *
 * @author Doug Simon
 */
public class ClassfileVersion {
    final int _major;
    final int _minor;

    public ClassfileVersion(int major, int minor) {
        _major = major;
        _minor = minor;
    }

    public ClassfileVersion(String className, Classpath classpath) {
        try {
            final ClasspathFile classpathFile = PrototypeClassLoader.readClassFile(classpath, className);
            final DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(classpathFile._contents));
            try {
                final int magic = dataInputStream.readInt();
                if (magic != 0xcafebabe) {
                    ProgramWarning.message("invalid magic number (0x" + Integer.toHexString(magic) + ") in class file for " + className);
                }
                _minor = dataInputStream.readUnsignedShort();
                _major = dataInputStream.readUnsignedShort();
                return;

            } catch (IOException e) {
                ProgramWarning.message("IO error while trying to read version info from " + className + ": " + e);
            } finally {
                try {
                    dataInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (ClassNotFoundException classNotFoundException) {
        }
        throw new NoClassDefFoundError(className);
    }
}

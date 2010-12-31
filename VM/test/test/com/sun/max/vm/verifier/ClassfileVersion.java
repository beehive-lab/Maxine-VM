/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package test.com.sun.max.vm.verifier;

import java.io.*;

import com.sun.max.program.*;
import com.sun.max.vm.hosted.*;

/**
 * Utility class to determine the class file version of a given class without actually loading the class.
 *
 * @author Doug Simon
 */
public class ClassfileVersion {
    final int major;
    final int minor;

    public ClassfileVersion(int major, int minor) {
        this.major = major;
        this.minor = minor;
    }

    public ClassfileVersion(String className, Classpath classpath) {
        try {
            final ClasspathFile classpathFile = HostedBootClassLoader.readClassFile(classpath, className);
            final DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(classpathFile.contents));
            try {
                final int magic = dataInputStream.readInt();
                if (magic != 0xcafebabe) {
                    ProgramWarning.message("invalid magic number (0x" + Integer.toHexString(magic) + ") in class file for " + className);
                }
                minor = dataInputStream.readUnsignedShort();
                major = dataInputStream.readUnsignedShort();
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

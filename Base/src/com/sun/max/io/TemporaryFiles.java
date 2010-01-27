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
package com.sun.max.io;

import java.io.*;

import com.sun.max.program.*;

public final class TemporaryFiles {
    private TemporaryFiles() {
    }

    public static void cleanup(final String prefix, final String suffix) {
        if ((prefix == null || prefix.length() == 0) && (suffix == null || suffix.length() == 0)) {
            return;
        }
        try {
            final File tempFile = File.createTempFile(prefix, suffix);
            final File directory = tempFile.getParentFile();
            final FilenameFilter filter = new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    if (prefix != null && prefix.length() > 0 && !name.startsWith(prefix)) {
                        return false;
                    }
                    if (suffix != null && suffix.length() > 0 && !name.endsWith(suffix)) {
                        return false;
                    }
                    return true;
                }
            };
            for (File file : directory.listFiles(filter)) {
                if (!file.delete()) {
                    ProgramWarning.message("could not delete temporary file: " + file.getAbsolutePath());
                }
            }
        } catch (IOException ioException) {
            ProgramWarning.message("could not delete temporary files");
        }
    }

    public static void cleanup(String prefix) {
        cleanup(prefix, null);
    }
}

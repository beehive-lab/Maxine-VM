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
package com.sun.max.program;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import com.sun.max.io.*;
import com.sun.max.program.Classpath.*;

/**
 * Provides a facility for processing all the resources reachable on a given {@linkplain Classpath classpath}.
 *
 * @author Doug Simon
 */
public class ClasspathTraversal {

    /**
     * Handles a standard file resource encountered during the traversal.
     *
     * @param parent the classpath directory entry under which the resource is located
     * @param resource the path of the resource relative to {@code parent}. The
     *            {@linkplain File#separatorChar platform specific} character is used as the path separator in this
     *            value.
     * @return true if the traversal should continue, false if it should terminate
     */
    protected boolean visitFile(File parent, String resource) {
        return true;
    }

    /**
     * Handles an archive entry resource encountered during the traversal.
     *
     * @param archive the classpath .zip or .jar entry in which the resource is located
     * @param resource the archive entry holding the resource
     * @return true if the traversal should continue, false if it should terminate
     */
    protected boolean visitArchiveEntry(ZipFile archive, ZipEntry resource) {
        return true;
    }

    /**
     * Traverses all the resources reachable on a given classpath.
     *
     * @param classpath the classpath to search
     */
    public void run(final Classpath classpath) {
        run(classpath, null);
    }

    /**
     * Traverses all the resources reachable on a given classpath.
     *
     * @param classpath the classpath to search
     * @param resourcePrefixFilter if non-null, then only resources whose name begins with this value are traversed. The
     *            '/' character must be used in this value as the path separator regardless of the
     *            {@linkplain File#separatorChar default} for the underlying platform.
     */
    public void run(final Classpath classpath, String resourcePrefixFilter) {
        for (final Entry entry : classpath.entries()) {
            if (entry.isDirectory()) {
                final String prefix = entry.path() + File.separator;
                final File startFile;
                if (resourcePrefixFilter == null) {
                    startFile = entry.file();
                } else {
                    if (File.separatorChar != '/') {
                        startFile = new File(entry.file(), resourcePrefixFilter.replace('/', File.separatorChar));
                    } else {
                        startFile = new File(entry.file(), resourcePrefixFilter);
                    }
                }

                final FileTraversal fileTraversal = new FileTraversal() {
                    @Override
                    protected void visitFile(File file) {
                        final String path = file.getPath();
                        assert path.startsWith(prefix);
                        final String resource = path.substring(prefix.length());
                        if (!ClasspathTraversal.this.visitFile(entry.file(), resource)) {
                            stop();
                        }
                    }
                };
                fileTraversal.run(startFile);
                if (fileTraversal.wasStopped()) {
                    return;
                }
            } else if (entry.isArchive()) {
                final ZipFile zipFile = entry.zipFile();
                if (zipFile != null) {
                    for (final Enumeration<? extends ZipEntry> e = zipFile.entries(); e.hasMoreElements();) {
                        final ZipEntry zipEntry = e.nextElement();
                        if (resourcePrefixFilter == null || zipEntry.getName().startsWith(resourcePrefixFilter)) {
                            if (!visitArchiveEntry(zipFile, zipEntry)) {
                                return;
                            }
                        }
                    }
                }
            }
        }
    }
}

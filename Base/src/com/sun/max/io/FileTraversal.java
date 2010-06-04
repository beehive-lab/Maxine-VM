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

/**
 * Provides a facility for walking a file system hierarchy similar to that provided by the Unix find(1) facility.
 *
 * @author Doug Simon
 */
public class FileTraversal {

    private boolean stopped;

    /**
     * Handles a standard file resource encountered during the traversal.
     *
     * @param file a file resource for which {@link File#isFile()} returns {@code true}
     */
    protected void visitFile(File file) {
    }

    /**
     * Handles a directory encountered during the traversal.
     *
     * @param directory a file resource for which {@link File#isDirectory()} returns {@code true}
     * @return true if the traversal should process the file system hierarchy rooted at {@code directory}, false if it
     *         should be skipped
     */
    protected boolean visitDirectory(File directory) {
        return true;
    }

    /**
     * Handles a file resource encountered during the traversal that is neither a standard file or directory.
     *
     * @param other a file resource for which neither {@link File#isFile()} nor {@link File#isDirectory()} returns
     *            {@code true}
     */
    protected void visitOther(File other) {
    }

    /**
     * Stops the traversal after the current file resource has been processed. This can be called from within an
     * overriding implementation of {@link #visitFile(File)}, {@link #visitDirectory(File)} or
     * {@link #visitOther(File)} to prematurely terminate a traversal.
     */
    protected final void stop() {
        stopped = true;
    }

    /**
     * Traverses the file hierarchy rooted at a given file. The {@linkplain #wasStopped() stopped} status of this
     * traversal object is reset to {@code false} before the traversal begins.
     *
     * @param file the file or directory at which to start the traversal
     */
    public void run(File file) {
        stopped = false;
        visit(file);
    }

    /**
     * Determines if the traversal was stopped before every file in the file hierarchy was traversed.
     */
    public boolean wasStopped() {
        return stopped;
    }

    private boolean visit(File entry) {
        File subdirectoryToTraverse = null;
        if (entry.isFile()) {
            visitFile(entry);
        } else if (entry.isDirectory()) {
            if (visitDirectory(entry)) {
                subdirectoryToTraverse = entry;
            }
        } else {
            visitOther(entry);
        }
        if (stopped) {
            return false;
        }
        if (subdirectoryToTraverse != null) {
            traverse(subdirectoryToTraverse);
            if (stopped) {
                return false;
            }
        }
        return true;
    }

    private void traverse(File directory) {
        final File[] entries = directory.listFiles();
        if (entries != null) {
            for (File entry : entries) {
                if (!visit(entry)) {
                    return;
                }
            }
        }
    }
}

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
package com.sun.max.ins.file;

import java.io.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.util.*;

/**
 * Base class for inspectors that display information from files.
 *
 * @author Michael Van De Vanter
 */
public abstract class FileInspector extends Inspector {

    private File file;

    public File file() {
        return file;
    }

    protected FileInspector(Inspection inspection, File file) {
        super(inspection);
        this.file = file;
    }

    public abstract void highlightLine(int lineNumber);

    protected String readFile() {
        String text = null;
        try {
            final FileInputStream fileInputStream = new FileInputStream(file);
            final int length = fileInputStream.available();
            final byte[] bytes = new byte[length];
            fileInputStream.read(bytes);
            text = new String(bytes);
        } catch (IOException exception) {
            InspectorError.unexpected(exception);
        }
        return text;
    }

}

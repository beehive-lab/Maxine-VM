/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.ins.file;

import java.io.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.util.*;
import com.sun.max.ins.view.InspectionViews.ViewKind;

/**
 * Base class for views that display information from files.
 */
public abstract class FileView<View_Kind extends FileView> extends AbstractView<View_Kind> {

    private File file;

    public File file() {
        return file;
    }

    protected FileView(Inspection inspection, File file, ViewKind viewKind) {
        super(inspection, viewKind, null);
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

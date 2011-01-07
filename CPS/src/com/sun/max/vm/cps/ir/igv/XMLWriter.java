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
package com.sun.max.vm.cps.ir.igv;

import java.io.*;
import java.util.*;

/**
 * Utility class for writing XML data.
 *
 * @author Thomas Wuerthinger
 */
public class XMLWriter {

    private Writer out;

    public XMLWriter(Writer out) {
        this.out = out;
    }

    public void close() throws IOException {
        out.close();
    }

    public void begin(String name, Properties properties) throws IOException {
        startBegin(name);
        writeAttributes(properties);
        endBegin();
    }

    public void begin(String name, String attrName, String attrValue) throws IOException {
        final Properties p = new Properties();
        p.setProperty(attrName, attrValue);
        begin(name, p);
    }

    public void end(String name) throws IOException {
        out.write("</" + name + ">");
    }

    public void write(String text) throws IOException {
        String s = text.replace("&", "&amp;");
        s = s.replace("<", "&lt;");
        s = s.replace(">", "&gt;");
        out.write(s);
    }

    public void writeData(String text) throws IOException {
        out.write("<![CDATA[");
        out.write(text);
        out.write("]]>");
    }

    public void simple(String name) throws IOException {
        startBegin(name);
        endSimple();
    }

    public void simple(String name, Properties properties) throws IOException {
        startBegin(name);
        writeAttributes(properties);
        endSimple();
    }

    public void simple(String name, String attrName, String attrValue) throws IOException {
        final Properties p = new Properties();
        p.setProperty(attrName, attrValue);
        simple(name, p);
    }

    private void startBegin(String name) throws IOException {
        out.write("<" + name);
    }

    private void endBegin() throws IOException {
        out.write(">");
    }

    private void endSimple() throws IOException {
        out.write("/>");
    }

    public void begin(String name) throws IOException {
        startBegin(name);
        endBegin();
    }

    private void writeAttribute(String name, String value) throws IOException {
        out.write(" " + name + "=\"" + value + "\"");
    }

    private void writeAttributes(Properties properties) throws IOException {
        final Enumeration e = properties.propertyNames();
        while (e.hasMoreElements()) {
            final String propertyName = (String) e.nextElement();
            writeAttribute(propertyName, properties.getProperty(propertyName));
        }
    }
}

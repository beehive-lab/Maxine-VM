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
/*VCSID=dca14761-94d0-48b3-853f-10903ebd2916*/
package com.sun.max.io;

import java.io.*;
import java.util.*;

/**
 * Utility class for writing XML data.
 *
 * @author Thomas Wuerthinger
 */
public class XMLWriter {

    private Writer _out;

    public XMLWriter(Writer out) {
        this._out = out;
    }

    public void close() throws IOException {
        _out.close();
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
        _out.write("</" + name + ">");
    }

    public void write(String text) throws IOException {
        String s = text.replace("&", "&amp;");
        s = s.replace("<", "&lt;");
        s = s.replace(">", "&gt;");
        _out.write(s);
    }

    public void writeData(String text) throws IOException {
        _out.write("<![CDATA[");
        _out.write(text);
        _out.write("]]>");
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
        _out.write("<" + name);
    }

    private void endBegin() throws IOException {
        _out.write(">");
    }

    private void endSimple() throws IOException {
        _out.write("/>");
    }

    public void begin(String name) throws IOException {
        startBegin(name);
        endBegin();
    }

    private void writeAttribute(String name, String value) throws IOException {
        _out.write(" " + name + "=\"" + value + "\"");
    }

    private void writeAttributes(Properties properties) throws IOException {
        final Enumeration e = properties.propertyNames();
        while (e.hasMoreElements()) {
            final String propertyName = (String) e.nextElement();
            writeAttribute(propertyName, properties.getProperty(propertyName));
        }
    }
}

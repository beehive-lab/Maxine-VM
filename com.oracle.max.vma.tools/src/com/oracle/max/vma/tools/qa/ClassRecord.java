/*
 * Copyright (c) 2004, 2011, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.max.vma.tools.qa;

import java.util.ArrayList;

/**
 * Maintains the basic information on a class instance that occurred in a trace.
 * Specifically, its name, the id of the {@link ClassLoader} that loaded it, and
 * a list of all the associated {@link ObjectRecord object instances} in the
 * trace.
 *
 * @author Mick Jordan
 *
 */
public class ClassRecord {
    /**
     * By convention, the first element of the list contains the traces for
     * static fields of this class.
     */
    private ArrayList<ObjectRecord> objects;
    /**
     * This is the canonical name in "language" format (modulo $ in nested classes)
     * even if the trace contains JVM style names.
     */
    private String name;

    /**
     * A unique id that identifies the {@link ClassLoader} that loaded this class.
     */
    private long classLoaderId;

    public ClassRecord(String name, long classLoaderId,
            ArrayList<ObjectRecord> objects) {
        this.name = name;
        this.classLoaderId = classLoaderId;
        this.objects = objects;
    }

    public ArrayList<ObjectRecord> getObjects() {
        return objects;
    }

    public void addObject(ObjectRecord td) {
        objects.add(td);
    }

    public static String getCanonicalName(String name) {
        String result = name;
        if (result.charAt(0) == '[') {
            // array type in signature format
            switch (result.charAt(1)) {
                case 'B':
                    result = "byte";
                    break;
                case 'C':
                    result = "char";
                    break;
                case 'J':
                    result = "long";
                    break;
                case 'S':
                    result = "short";
                    break;
                case 'I':
                    result = "int";
                    break;
                case 'F':
                    result = "float";
                    break;
                case 'D':
                    result = "double";
                    break;
                case 'Z':
                    result = "boolean";
                    break;
                case '[':
                    // multi-dimensional array
                    result = getCanonicalName(name.substring(1));
                    break;
                default:
                    result = result.substring(2, result.length() - 1);
            }
            result += "[]";
        } else if (result.charAt(0) == 'L') {
            // class type in signature format
            result = result.substring(1, result.length() - 1);
        }
        if (result.indexOf('/') > 0) {
            result = result.replace('/', '.');
        }
        return result;
    }

    public String getName() {
        return name;
    }

    /**
     * Use to update a forward reference from a short form.
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    public long getClassLoaderId() {
        return classLoaderId;
    }

    public boolean isArray() {
        return name.contains("[");
    }

    public boolean isString() {
        return name.equals("java.lang.String");
    }

}



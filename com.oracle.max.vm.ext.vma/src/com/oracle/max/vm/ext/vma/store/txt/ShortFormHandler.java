/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.vma.store.txt;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Provides generic and, somewhat expensive, support for converting full names for classes, threads, fields
 * and methods into short forms, represented a integers, optionally preceded by a prefix.
 *
 * The short form maps are (necessarily) global to all threads and must be thread-safe.
 * The values used to represent short forms are immutable when stored in the map.
 * However, the class definitions do not define the fields as {@code final} because
 * mutable, thread-local, instances are used to do the initial lookup in the map
 * to avoid unnecessary allocation.
 *
 * A concrete subclass must implement the {@link #defineShortForm(ShortForm, Object, String, String)} method,
 * which is called whenever a new short form is created.
 */

public abstract class ShortFormHandler {

    private static boolean doPrefix;

    protected ShortFormHandler() {
        doPrefix = System.getProperty(VMATextStoreFormat.PREFIX_PROPERTY) != null;
    }

    /**
     * Denotes a class name and classloader id.
     * Used as the value in the short forms map for a class.
     */
    public static class ClassNameId {
        public String name;
        public long clId;
        public ClassNameId(String name, long clId) {
            this.name = name;
            this.clId = clId;
        }

        @Override
        public int hashCode() {
            return (int) (name.hashCode() ^ clId);
        }

        @Override
        public boolean equals(Object other) {
            ClassNameId otherClassName = (ClassNameId) other;
            return name.equals(otherClassName.name) && clId == otherClassName.clId;
        }
    }

    /**
     * Denotes a qualified name. Used as the value in the short form maps for fields and methods.
     */
    public static class QualName {
        public ClassNameId className;
        public String name;
        public QualName(String className, long clId, String name) {
            this.className = new ClassNameId(className, clId);
            this.name = name;
        }

        public QualName(ClassNameId className, String name) {
            this.className = className;
            this.name = name;
        }

        @Override
        public int hashCode() {
            return className.hashCode() ^ name.hashCode();
        }

        @Override
        public boolean equals(Object other) {
            QualName otherQualName = (QualName) other;
            return className.equals(otherQualName.className) && name.equals(otherQualName.name);
        }
    }

    /**
     * Per-thread value that is used to check the map for existence.
     */
    private static class ClassNameIdTL extends ThreadLocal<ClassNameId> {
        @Override
        public ClassNameId initialValue() {
            return new ClassNameId(null, 0);
        }
    }

    private static final ClassNameIdTL classNameIdTL = new ClassNameIdTL();

    /**
     * Per-thread value that is used to check the map for existence.
     */
    private static class QualNameTL extends ThreadLocal<QualName> {
        @Override
        public QualName initialValue() {
            return new QualName(classNameIdTL.get(), null);
        }
    }

    private static final QualNameTL qualNameTL = new QualNameTL();

    public static enum ShortForm {
        C("C"),
        F("F"),
        T("T"),
        M("M");

        public final String code;

        private ConcurrentMap<Object, String> shortForms = new ConcurrentHashMap<Object, String>();
        private AtomicInteger nextIdA = new AtomicInteger();

        String createShortForm(ShortFormHandler handler, Object key) {
            String shortForm = shortForms.get(key);
            String classShortForm = null;
            if (shortForm == null) {
                Object newKey = key;
                int nextId = nextIdA.incrementAndGet();
                shortForm = doPrefix ? (code + nextId) : Integer.toString(nextId);
                switch (this) {
                    case T:
                        break;
                    case C:
                        ClassNameId tlKey = (ClassNameId) key;
                        newKey = new ClassNameId(tlKey.name, tlKey.clId);
                        break;
                    case F:
                    case M:
                        QualName tlQualName = (QualName) key;
                        newKey = new QualName(new ClassNameId(tlQualName.className.name, tlQualName.className.clId), tlQualName.name);
                        classShortForm = ShortForm.C.createShortForm(handler, tlQualName.className);
                        break;
                }
                String winner = shortForms.putIfAbsent(newKey, shortForm);
                // Another thread may have beaten us to it.
                if (winner != null) {
                    shortForm = winner;
                } else {
                    handler.defineShortForm(this, key, shortForm, classShortForm);
                }
            }
            return shortForm;
        }

        ShortForm(String code) {
            this.code = code;
        }
    }

    public String getClassShortForm(String className, long clId) {
        ClassNameId classNameId = classNameIdTL.get();
        classNameId.name = className;
        classNameId.clId = clId;
        return ShortForm.C.createShortForm(this, classNameId);
    }

    public String getFieldShortForm(String className, long clId, String fieldName) {
        QualName qualNameId = qualNameTL.get();
        qualNameId.className.name = className;
        qualNameId.className.clId = clId;
        qualNameId.name = fieldName;
        return ShortForm.F.createShortForm(this, qualNameId);
    }

    public String getThreadShortForm(String threadName) {
        // per thread store, thread implicit
        if (threadName == null) {
            return null;
        }
        return ShortForm.T.createShortForm(this, threadName);
    }

    public String getMethodShortForm(String className, long clId, String fieldName) {
        QualName qualNameId = qualNameTL.get();
        qualNameId.className.name = className;
        qualNameId.className.clId = clId;
        qualNameId.name = fieldName;
        return ShortForm.M.createShortForm(this, qualNameId);
    }

    /**
     * Define a short form of {@code key}.
     *
     * @param type
     * @param key
     * @param shortForm
     * @param classShortForm only for fields and methods
     */
    protected abstract void defineShortForm(ShortForm type, Object key, String shortForm, String classShortForm);


}

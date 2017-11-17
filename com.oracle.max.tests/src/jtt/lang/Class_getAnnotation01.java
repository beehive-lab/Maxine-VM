/*
 * Copyright (c) 2017, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
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
 */
package jtt.lang;

/*
 * @Harness: java
 * @Runs: 0 = "@java.lang.Deprecated()";
 * @Runs: 1 = "null";
 */
public final class Class_getAnnotation01 {
    private Class_getAnnotation01() {
    }

    @Deprecated
    final class Annotated { }

    final class NotAnnotated { }

    public static String test(int i)  {
        switch (i) {
            case 0:
                return Annotated.class.getAnnotation(Deprecated.class).toString();
            case 1:
                Deprecated deprecated = NotAnnotated.class.getAnnotation(Deprecated.class);
                return deprecated == null ? "null" : deprecated.toString();
            default:
                return null;
        }
    }
}

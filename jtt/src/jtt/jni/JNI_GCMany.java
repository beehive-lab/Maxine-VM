/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
package jtt.jni;

import java.lang.reflect.*;


public class JNI_GCMany {
    private native Object jniGC(Method gcMethod, Class<?> systemClass, Object o1, Object o2, Object o3, Object o4, Object o5);

    public static boolean test(int arg) throws Exception {
        Method gcMethod = System.class.getDeclaredMethod("gc");
        Object o1 = "o1";
        Object o2 = "o2";
        Object o3 = "o3";
        Object o4 = "o4";
        Object o5 = "o5";
        JNI_GCMany self = new JNI_GCMany();
        return o5 == self.jniGC(gcMethod, System.class, o1, o2, o3, o4, o5);
    }

}

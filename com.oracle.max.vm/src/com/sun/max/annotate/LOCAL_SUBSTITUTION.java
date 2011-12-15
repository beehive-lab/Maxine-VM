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
package com.sun.max.annotate;

import static com.sun.max.vm.classfile.ErrorContext.*;

import java.lang.annotation.*;

import com.sun.max.vm.*;
import com.sun.max.vm.classfile.*;

/**
 * Indicates that the annotated method is a substitute for another method (the <i>substitutee</i>) in the same class.
 * The substitutee must have exactly the same signature as the local substitute and the name of the local substitute must be the name
 * of the substitutee concatenated with the suffix "_". Any execution (or compilation) of the substitutee will be
 * replaced with an execution (or compilation) of the local substitute in the {@linkplain MaxineVM#vm() target}.
 * <p>
 * The {@link ClassfileReader} will make the substitution and assumes that the the substitutee precedes the local substitute in
 * the class file. This ordering requirement will have to be removed if the MaxineVM source code is compiled with a Java
 * source compiler that does not preserve source file ordering of methods in its generated class files.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface LOCAL_SUBSTITUTION {

    public static final class Static {

        private Static() {
        }

        public static String toSubstituteName(String substituteeName) {
            return substituteeName + "_";
        }

        public static String toSubstituteeName(String localSubstituteName) {
            if (!localSubstituteName.endsWith("_")) {
                classFormatError("Name of local substitute method must end with '_': " + localSubstituteName);
            }
            return localSubstituteName.substring(0, localSubstituteName.length() - 1);
        }
    }
}

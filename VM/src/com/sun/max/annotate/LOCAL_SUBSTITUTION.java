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
package com.sun.max.annotate;

import static com.sun.max.vm.classfile.ErrorContext.*;

import java.lang.annotation.*;

import com.sun.max.vm.*;
import com.sun.max.vm.classfile.*;

/**
 * Indicates that the annotated method is a substitute for another method (the <i>substitutee</i>) in the same class.
 * The substitutee must have exactly the same signature as the local substitute and the name of the local substitute must be the name
 * of the substitutee concatenated with the suffix "_". Any execution (or compilation) of the substitutee will be
 * replaced with an execution (or compilation) of the local substitute in the {@linkplain MaxineVM#target() target}.
 * <p>
 * The {@link ClassfileReader} will make the substitution and assumes that the the substitutee precedes the local substitute in
 * the class file. This ordering requirement will have to be removed if the MaxineVM source code is compiled with a Java
 * source compiler that does not preserve source file ordering of methods in its generated class files.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
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

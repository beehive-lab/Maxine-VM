/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.type;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import com.sun.max.annotate.*;
import com.sun.max.ide.*;
import com.sun.max.io.*;
import com.sun.max.program.*;
import com.sun.max.vm.runtime.*;

/**
 * Since enums cannot have type parameters, we see ourselves forced to declare two parallel classes.
 * This one represents kinds as bare enums and 'Kind' represents kinds parameterized
 * with their corresponding 'Value' type.
 * Since operations on kinds usually have type parameters, we have not placed any of them here.
 *
 * @see Kind
 */
public enum KindEnum {

    BOOLEAN,
    BYTE,
    SHORT,
    CHAR,
    INT,
    FLOAT,
    LONG,
    DOUBLE,
    REFERENCE,
    WORD,
    VOID;

    public final Kind asKind() {
        return kind;
    }

    Kind kind;

    @HOSTED_ONLY
    void setKind(Kind kind) {
        assert this.kind == null;
        this.kind = kind;
    }

    /**
     * Convenient way to get an immutable view of all the values of this enumerated type without paying the cost of
     * an array clone operation.
     */
    public static final List<KindEnum> VALUES = Arrays.asList(values());

    static {
        // Sanity check to ensure that the values in Native/substrate/kind.h match those of this enum
        final File file = new File(new File(JavaProject.findHgRoot(), "com.oracle.max.vm.native/substrate/kind.h").getAbsolutePath());
        try {
            String content = new String(Files.toChars(file));
            for (KindEnum kind : VALUES) {
                String regex = ".*#define kind_" + kind.name() + "\\s+(\\d+).*";
                Matcher matcher = Pattern.compile(regex, Pattern.DOTALL).matcher(content);
                if (!matcher.matches()) {
                    throw FatalError.unexpected("Could not find #define for kind_" + kind + " in " + file);
                }
                int value = Integer.parseInt(matcher.group(1));
                if (value != kind.ordinal()) {
                    throw FatalError.unexpected(String.format("Mismatch ordinal for kind %s: %s.%s.ordinal() = %d, kind_%s = %d (%s)",
                                    kind, KindEnum.class.getName(), kind, kind.ordinal(), kind, value, file));
                }
            }
        } catch (IOException e) {
            throw ProgramError.unexpected("Error reading native header file " + file, e);
        }
    }
}

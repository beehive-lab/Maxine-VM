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
package com.sun.max.vm.type;

import java.io.*;
import java.util.regex.*;

import com.sun.max.annotate.*;
import com.sun.max.collect.*;
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
 *
 * @author Bernd Mathiske
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
    public static final IndexedSequence<KindEnum> VALUES = new ArraySequence<KindEnum>(values());

    static {
        // Sanity check to ensure that the values in Native/substrate/kind.h match those of this enum
        final File file = new File(new File(JavaProject.findVcsProjectDirectory().getParentFile().getAbsoluteFile(), "Native/substrate/kind.h").getAbsolutePath());
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
            ProgramError.unexpected("Error reading native header file " + file, e);
        }
    }
}

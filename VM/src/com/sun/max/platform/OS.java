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
package com.sun.max.platform;

import static com.sun.max.platform.OS.UnixFlag.*;

import com.sun.max.annotate.*;
import com.sun.max.program.*;

/**
 * Enumerated type for operating systems.
 *
 * @author Doug Simon
 */
public enum OS {

    DARWIN("Darwin",    UNIX),
    LINUX("Linux",      UNIX),
    SOLARIS("Solaris",  UNIX),
    WINDOWS("Windows", !UNIX),
    GUESTVM("GuestVM", !UNIX);

    /**
     * Specifies if this is a Unix OS.
     */
    public final boolean unix;

    /**
     * The identifier of this OS as part of a class name.
     */
    public final String className;

    public int serial() {
        return ordinal();
    }
    /**
     * Returns a string that can be used in a package name.
     * @return
     */
    public String asPackageName() {
        return name().toLowerCase();
    }

    private OS(String className) {
        this(className, true);
    }

    private OS(String className, boolean unix) {
        this.className = className;
        this.unix = unix;
    }

    public static OS fromName(String name) {
        return OS.valueOf(name.toUpperCase());
    }

    private static OS getCurrent() {
        final String name = System.getProperty("os.name");
        if (name.equals("Linux")) {
            return LINUX;
        }
        if (name.equals("SunOS")) {
            return SOLARIS;
        }
        if (name.equals("Mac OS X") || name.equals("Darwin")) {
            return DARWIN;
        }
        if (name.equals("GuestVM")) {
            return GUESTVM;
        }
        throw ProgramError.unexpected("unknown OS: " + name);
    }

    @RESET
    private static OS current;

    public static OS current() {
        if (current == null) {
            current = getCurrent();
        }
        return current;
    }

    static class UnixFlag {
        public static final boolean UNIX = true;
    }
}

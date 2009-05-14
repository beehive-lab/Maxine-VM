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

import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;

public enum OperatingSystem implements PoolObject {
    DARWIN(4),
    LINUX(4),
    SOLARIS(8),
    WINDOWS(4),
    GUESTVM(4);

    private final int _defaultPageSize;

    public int serial() {
        return ordinal();
    }

    public int defaultPageSize() {
        return _defaultPageSize;
    }

    private OperatingSystem(int pageKBytes) {
        _defaultPageSize = pageKBytes * Ints.K;
    }

    private static OperatingSystem getCurrent() {
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
        throw ProgramError.unexpected("unknown operating system: " + name);
    }

    private static transient OperatingSystem _current;

    public static OperatingSystem current() {
        if (_current == null) {
            _current = getCurrent();
        }
        return _current;
    }

    private static final Pool<OperatingSystem> VALUE_POOL = new ArrayPool<OperatingSystem>(values());

    public static final PoolSet<OperatingSystem> UNIX = PoolSet.of(VALUE_POOL, DARWIN, LINUX, SOLARIS);

    public static final PoolSet<OperatingSystem> UNIX_GUESTVM = PoolSet.of(VALUE_POOL, DARWIN, LINUX, SOLARIS, GUESTVM);
}

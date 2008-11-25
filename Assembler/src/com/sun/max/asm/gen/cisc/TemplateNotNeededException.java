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
package com.sun.max.asm.gen.cisc;

import com.sun.max.asm.dis.*;

/**
 * Thrown to abruptly stop template creation in some corner cases
 * that would otherwise be hard to describe.
 *
 * The {@link Disassembler disassembler} works by matching instruction patterns
 * against a set of templates. Creation of these templates can be noticeably
 * sped up by reusing a shared instance of this exception.
 * A client using the disassembler should {@linkplain #enableSharedInstance() enable}
 * before creating the first Disassembler object.
 *
 * @author Bernd Mathiske
 */
public class TemplateNotNeededException extends Exception {

    public TemplateNotNeededException() {
        super();
    }

    public static synchronized void enableSharedInstance() {
        if (_sharedInstance == null) {
            _sharedInstance = new TemplateNotNeededException();
        }
    }

    public static synchronized void disableSharedInstance() {
        _sharedInstance = null;
    }

    public static TemplateNotNeededException raise() throws TemplateNotNeededException {
        final TemplateNotNeededException instance = _sharedInstance;
        if (instance != null) {
            throw instance;
        }
        throw new TemplateNotNeededException();
    }

    // Checkstyle: stop
    private static TemplateNotNeededException _sharedInstance;
}

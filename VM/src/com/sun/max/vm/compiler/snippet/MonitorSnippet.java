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
/*VCSID=36a3dd29-9d27-43d7-883c-e907475e0b39*/
package com.sun.max.vm.compiler.snippet;

import com.sun.max.annotate.*;
import com.sun.max.vm.monitor.*;

/**
 * Snippets for monitorentry/exit that delegate to the monitor scheme.
 *
 * @author Mick Jordan
 * @author Bernd Mathiske
 */
public abstract class MonitorSnippet extends NonFoldableSnippet {

    private MonitorSnippet() {
        super();
    }

    public static final class MonitorEnter extends MonitorSnippet {
        @SNIPPET
        @INLINE
        public static void monitorEnter(Object object) {
            Monitor.enter(object);
        }

        public static final MonitorEnter SNIPPET = new MonitorEnter();
    }

    public static final class MonitorExit extends MonitorSnippet {
        @SNIPPET
        @INLINE
        public static void monitorExit(Object object) {
            Monitor.exit(object);
        }

        public static final MonitorExit SNIPPET = new MonitorExit();
    }

}

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

package com.sun.max.vm.cps.hotpath;

import com.sun.max.annotate.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.cps.hotpath.compiler.*;
import com.sun.max.vm.cps.tir.*;
import com.sun.max.vm.object.*;

public abstract class HotpathSnippet extends Snippet {
    public static class GuardException extends RuntimeException {

    }

    /**
     * We need to explicitly check for null pointers when tracing.
     */
    public static final class CheckNullPointer extends HotpathSnippet {
        @SNIPPET
        @INLINE
        public static void checkNullPointer(Object object) throws NullPointerException {
            if (object == null) {
                throw new NullPointerException();
            }
        }
        public static final CheckNullPointer SNIPPET = new CheckNullPointer();
    }

    /**
     * We need to explicitly check for runtime types when tracing.
     */
    public static final class CheckType extends HotpathSnippet {
        @SNIPPET
        @INLINE
        public static void checkType(ClassActor classActor, Object object) throws NullPointerException {
            if (ObjectAccess.readHub(object).classActor != classActor) {
                throw new ClassCastException();
            }
        }
        public static final CheckType SNIPPET = new CheckType();
    }

    public static final class CallBailout extends HotpathSnippet {
        @SNIPPET
        public static void callBailout(TirGuard guard) {
            Bailout.bailout(guard);
        }
        public static final CallBailout SNIPPET = new CallBailout();
    }

    public static final class SaveRegisters extends HotpathSnippet {
        @SNIPPET
        public static void saveRegisters() {

        }
        public static final SaveRegisters SNIPPET = new SaveRegisters();
    }
}

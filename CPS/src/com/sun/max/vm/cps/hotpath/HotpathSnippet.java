/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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

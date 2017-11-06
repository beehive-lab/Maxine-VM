/*
 * Copyright (c) 2017, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
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
 */
package com.sun.max.vm.jdk;

import static com.sun.max.vm.type.ClassRegistry.*;
import static com.sun.max.vm.intrinsics.MaxineIntrinsicIDs.*;

import com.sun.max.annotate.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.value.*;

import sun.misc.*;

/**
 * Substitutions for java.lang.ref.Finalizer.
 */
@METHOD_SUBSTITUTIONS(className = "java.lang.ref.Finalizer")
final class JDK_java_lang_ref_Finalizer {

    private static boolean TraceFinalization;
    static {
        VMOptions.addFieldOption("-XX:", "TraceFinalization", JDK_java_lang_ref_Finalizer.class, "Trace calls to Object.finalize() by the finalization subsystem.");
    }

    @ALIAS(declaringClassName = "java.lang.ref.Finalizer")
    private native boolean hasBeenFinalized();

    @ALIAS(declaringClassName = "java.lang.ref.Finalizer")
    private native void remove();

    @INTRINSIC(UNSAFE_CAST)
    private static native java.lang.ref.Reference asReference(Object cl);

    /**
     * This substitution provides a point at which finalization can be traced.
     */
    @SUBSTITUTE
    private void runFinalizer(JavaLangAccess jla) {
        synchronized (this) {
            if (hasBeenFinalized()) {
                return;
            }
            remove();
        }
        try {
            Object finalizee = asReference(this).get();

            if (finalizee != null && !(finalizee instanceof java.lang.Enum)) {
                ClassActor holder = ObjectAccess.readClassActor(finalizee);
                MethodActor selectedMethod = (MethodActor) holder.resolveMethodImpl(Object_finalize);

                if (TraceFinalization) {
                    Log.print("Finalizing ");
                    Log.print(ObjectAccess.readClassActor(finalizee).name.string);
                    Log.print(" instance at ");
                    Log.print(Reference.fromJava(finalizee).toOrigin());
                    Log.println(" by calling " + selectedMethod);
                }

                selectedMethod.invoke(ReferenceValue.from(finalizee));
                /* Clear stack slot containing this variable, to decrease
                   the chances of false retention with a conservative GC */
                finalizee = null;
            }
        } catch (Throwable x) { }
        asReference(this).clear();
    }
}

/*
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
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.vm.verifier;

import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;

/**
 */
public class TypeCheckingVerifier extends ClassVerifier {

    public static boolean FailOverToOldVerifier = true;
    static {
        VMOptions.addFieldOption("-XX:", "FailOverToOldVerifier", TypeCheckingVerifier.class,
            "Fail over to old verifier when the new type checker fails.");
    }

    public TypeCheckingVerifier(ClassActor classActor) {
        super(classActor);
        if (classActor.majorVersion < 50) {
            throw new IllegalArgumentException("Cannot perform type checking verification on class " + classActor.name + " with version number less than 50: " + classActor.majorVersion);
        }
    }

    @Override
    public synchronized void verify() {
        try {
            super.verify();
        } catch (VerifyError verifyError) {
            if (classActor.majorVersion == 50 && FailOverToOldVerifier) {
                failoverVerifier().verify();
            }
            throw verifyError;
        }
    }

    TypeInferencingVerifier failoverVerifier;

    private TypeInferencingVerifier failoverVerifier() {
        if (failoverVerifier == null) {
            failoverVerifier = new TypeInferencingVerifier(classActor);
        }
        return failoverVerifier;
    }

    @Override
    public synchronized CodeAttribute verify(ClassMethodActor classMethodActor, CodeAttribute codeAttribute) {
        try {
            new TypeCheckingMethodVerifier(this, classMethodActor, codeAttribute).verify();
            return codeAttribute;
        } catch (VerifyError verifyError) {
            if (classActor.majorVersion == 50 && FailOverToOldVerifier) {
                return failoverVerifier().verify(classMethodActor, codeAttribute);
            }
            throw verifyError;
        }
    }
}

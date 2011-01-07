/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.ir.igv;

import com.sun.max.vm.cps.bir.*;
import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.dir.*;
import com.sun.max.vm.cps.eir.*;
import com.sun.max.vm.cps.ir.*;

/**
 * Interface for visitors of IrMethod objects.
 *
 * @author Thomas Wuerthinger
 */
public interface IrMethodVisitor {

    /**
     * Visits a method of type BirMethod.
     * @param method the method to be visited
     */
    void visit(BirMethod method);

    /**
     * Visits a method of type CirMethod.
     * @param method the method to be visited
     */
    void visit(CirMethod method);

    /**
     * Visits a method of type DirMethod.
     * @param method the method to be visited
     */
    void visit(DirMethod method);

    /**
     * Visits a method of type EirMethod.
     * @param method the method to be visited
     */
    void visit(EirMethod method);

    public static class Static {

        /**
         * Calls the appropriate visit method of the given IrMethodVisitor object with the given IrMethod object as a parameter.
         *
         * @param irMethod the IrMethod object to be visited
         * @param methodVisitor the visitor of the IrMethod object
         * @return whether an appropriate visit method could be found or not
         */
        public static boolean visit(IrMethod irMethod, IrMethodVisitor methodVisitor) {
            assert irMethod != null && methodVisitor != null : "Arguments must not be null.";

            if (irMethod instanceof BirMethod) {
                methodVisitor.visit((BirMethod) irMethod);
            } else if (irMethod instanceof CirMethod) {
                methodVisitor.visit((CirMethod) irMethod);
            } else if (irMethod instanceof DirMethod) {
                methodVisitor.visit((DirMethod) irMethod);
            } else if (irMethod instanceof EirMethod) {
                methodVisitor.visit((EirMethod) irMethod);
            } else {
                // Unknown IrMethod type.
                return false;
            }

            return true;
        }
    }
}

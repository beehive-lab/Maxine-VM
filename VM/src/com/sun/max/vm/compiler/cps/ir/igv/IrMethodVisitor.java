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
package com.sun.max.vm.compiler.cps.ir.igv;

import com.sun.max.vm.compiler.cps.bir.*;
import com.sun.max.vm.compiler.cps.cir.*;
import com.sun.max.vm.compiler.cps.dir.*;
import com.sun.max.vm.compiler.cps.eir.*;
import com.sun.max.vm.compiler.cps.ir.*;

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

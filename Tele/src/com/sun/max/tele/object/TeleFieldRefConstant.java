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
package com.sun.max.tele.object;

import com.sun.max.tele.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.reference.*;


/**
 * Inspector's canonical surrogate for an object of type {@link FieldRefConstant} in the {@link TeleVM}.
 *
 * @author Michael Van De Vanter
 */
public abstract class TeleFieldRefConstant extends TelePoolConstant {

    public TeleFieldRefConstant(TeleVM teleVM, Reference fieldRefConstantReference) {
        super(teleVM, fieldRefConstantReference);
    }

    @Override
    public String maxineTerseRole() {
        return "FieldRefConst";
    }

     /**
     * Inspector's canonical surrogate for an object of type {@link FieldRefConstant.Resolved} in the {@link TeleVM}.
     *
     * @author Michael Van De Vanter
     */
    public static final class Resolved extends TeleFieldRefConstant {

        public Resolved(TeleVM teleVM, Reference resolvedFieldRefConstantReference) {
            super(teleVM, resolvedFieldRefConstantReference);
        }

        @Override
        public boolean isResolved() {
            return true;
        }

        /**
         * @return surrogate for the {@FieldActor} in the {@link TeleVM} to which the constant was resolved
         */
        public TeleFieldActor getTeleFieldActor() {
            final Reference fieldActorReference = teleVM().fields().FieldRefConstant$Resolved_fieldActor.readReference(reference());
            return (TeleFieldActor) teleVM().makeTeleObject(fieldActorReference);
        }

        @Override
        public String maxineRole() {
            return "Resolved FieldRefConstant";
        }

    }

    /**
     * Inspector's canonical surrogate for an object of type {@link FieldRefConstant.Unresolved} in the {@link TeleVM}.
     *
     * @author Michael Van De Vanter
     */
    public static final class Unresolved extends TeleFieldRefConstant {

        public Unresolved(TeleVM teleVM, Reference unresolvedFieldRefConstantReference) {
            super(teleVM, unresolvedFieldRefConstantReference);
        }

        @Override
        public boolean isResolved() {
            return false;
        }

        @Override
        public String maxineRole() {
            return "Unresolved FieldRefConstant";
        }

    }

    /**
     * Inspector's canonical surrogate for an object of type {@link FieldRefConstant.Unresolved} in the {@link TeleVM}.
     *
     * @author Michael Van De Vanter
     */
    public static final class UnresolvedIndices extends TeleFieldRefConstant {

        public UnresolvedIndices(TeleVM teleVM, Reference unresolvedFieldRefConstantReference) {
            super(teleVM, unresolvedFieldRefConstantReference);
        }

        @Override
        public boolean isResolved() {
            return false;
        }

        @Override
        public String maxineRole() {
            return "UnresolvedIndices FieldRefConstant";
        }

    }

}

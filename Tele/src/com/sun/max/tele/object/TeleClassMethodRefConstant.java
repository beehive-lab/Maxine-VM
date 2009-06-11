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
 * Inspector's canonical surrogate for an object of type {@link ClassMethodRefConstant} in the {@link TeleVM}.
 *
 * @author Michael Van De Vanter
 */
public abstract class TeleClassMethodRefConstant extends TelePoolConstant {

    public TeleClassMethodRefConstant(TeleVM teleVM, Reference classMethodRefConstantReference) {
        super(teleVM, classMethodRefConstantReference);
    }

    @Override
    public String maxineTerseRole() {
        return "ClassMethodRefConst";
    }

    /**
     * Inspector's canonical surrogate for an object of type {@link ClassMethodRefConstant.Resolved} in the {@link TeleVM}.
     *
     * @author Michael Van De Vanter
     */
    public static final class Resolved extends TeleClassMethodRefConstant {

        private TeleClassMethodActor _teleClassMethodActor;

        public Resolved(TeleVM teleVM, Reference resolvedClassMethodRefConstantReference) {
            super(teleVM, resolvedClassMethodRefConstantReference);
        }

        @Override
        public boolean isResolved() {
            return true;
        }

        /**
         * @return surrogate for the {@ClassMethodActor} in the {@link TeleVM} to which the constant was resolved
         */
        public TeleClassMethodActor getTeleClassMethodActor() {
            if (_teleClassMethodActor == null) {
                final Reference methodActorReference = teleVM().fields().ResolvedMethodRefConstant_methodActor.readReference(reference());
                _teleClassMethodActor = (TeleClassMethodActor) teleVM().makeTeleObject(methodActorReference);
            }
            return _teleClassMethodActor;
        }


        @Override
        public TeleClassMethodActor getTeleClassMethodActorForObject() {
            return getTeleClassMethodActor();
        }

        @Override
        public String maxineRole() {
            return "Resolved ClassMethodRefConstant";
        }

    }

    /**
     * Inspector's canonical surrogate for an object of type {@link ClassMethodRefConstant.Unresolved} in the {@link TeleVM}.
     *
     * @author Michael Van De Vanter
     */
    public static final class Unresolved extends TeleClassMethodRefConstant {

        public Unresolved(TeleVM teleVM, Reference resolvedClassMethodRefConstantReference) {
            super(teleVM, resolvedClassMethodRefConstantReference);
        }

        @Override
        public boolean isResolved() {
            return false;
        }

        @Override
        public String maxineRole() {
            return "Unresolved ClassMethodRefConstant";
        }

    }

    /**
     * Inspector's canonical surrogate for an object of type {@link ClassMethodRefConstant.UnresolvedIndices} in the {@link TeleVM}.
     *
     * @author Michael Van De Vanter
     */
    public static final class UnresolvedIndices extends TeleClassMethodRefConstant {

        public UnresolvedIndices(TeleVM teleVM, Reference resolvedClassMethodRefConstantReference) {
            super(teleVM, resolvedClassMethodRefConstantReference);
        }

        @Override
        public boolean isResolved() {
            return false;
        }

        @Override
        public String maxineRole() {
            return "UnresolvedIndices ClassMethodRefConstant";
        }

    }

}

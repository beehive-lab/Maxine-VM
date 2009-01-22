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
 * Inspector's canonical surrogate for an object of type {@link InterfaceMethodRefConstant} in the {@link TeleVM}.
 *
 * @author Michael Van De Vanter
 */
public abstract class TeleInterfaceMethodRefConstant extends TelePoolConstant {

    public TeleInterfaceMethodRefConstant(TeleVM teleVM, Reference classMethodRefConstantReference) {
        super(teleVM, classMethodRefConstantReference);
    }

    @Override
    public String maxineTerseRole() {
        return "InterfaceMethodRefConst";
    }

    /**
     * Inspector's canonical surrogate for an object of type {@link InterfaceMethodRefConstant.Resolved} in the {@link TeleVM}.
     *
     * @author Michael Van De Vanter
     */
    public static final class Resolved extends TeleInterfaceMethodRefConstant {

        private TeleInterfaceMethodActor _teleInterfaceMethodActor;

        public Resolved(TeleVM teleVM, Reference resolvedInterfaceMethodRefConstantReference) {
            super(teleVM, resolvedInterfaceMethodRefConstantReference);
        }

        @Override
        public boolean isResolved() {
            return true;
        }

        /**
         * @return surrogate for the {@InterfaceMethodActor} in the {@link TeleVM} to which the constant was resolved
         */
        public TeleInterfaceMethodActor getTeleInterfaceMethodActor() {
            if (_teleInterfaceMethodActor == null) {
                final Reference methodActorReference = teleVM().fields().ResolvedMethodRefConstant_methodActor.readReference(reference());
                _teleInterfaceMethodActor = (TeleInterfaceMethodActor) teleVM().makeTeleObject(methodActorReference);
            }
            return _teleInterfaceMethodActor;
        }

        @Override
        public String maxineRole() {
            return "Resolved InterfaceMethodRefConstant";
        }

    }

    /**
     * Inspector's canonical surrogate for an object of type {@link InterfaceMethodRefConstant.Unresolved} in the {@link TeleVM}.
     *
     * @author Michael Van De Vanter
     */
    public static final class Unresolved extends TeleInterfaceMethodRefConstant {

        public Unresolved(TeleVM teleVM, Reference resolvedInterfaceMethodRefConstantReference) {
            super(teleVM, resolvedInterfaceMethodRefConstantReference);
        }

        @Override
        public boolean isResolved() {
            return false;
        }

        @Override
        public String maxineRole() {
            return "Unresolved InterfaceMethodRefConstant";
        }

    }

    /**
     * Inspector's canonical surrogate for an object of type {@link InterfaceMethodRefConstant.UnresolvedIndices} in the {@link TeleVM}.
     *
     * @author Michael Van De Vanter
     */
    public static final class UnresolvedIndices extends TeleInterfaceMethodRefConstant {

        public UnresolvedIndices(TeleVM teleVM, Reference resolvedInterfaceMethodRefConstantReference) {
            super(teleVM, resolvedInterfaceMethodRefConstantReference);
        }

        @Override
        public boolean isResolved() {
            return false;
        }

        @Override
        public String maxineRole() {
            return "UnresolvedIndices InterfaceMethodRefConstant";
        }

    }

}

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
 * Inspector's canonical surrogate for an object of type {@link ClassConstant} in the tele VM.
 *
 * @author Michael Van De Vanter
 */
public abstract class TeleClassConstant extends TelePoolConstant {

    protected TeleClassConstant(TeleVM teleVM, Reference classConstantReference) {
        super(teleVM, classConstantReference);
    }

    /**
     * Inspector's canonical surrogate for an object of type {@link ClassConstant.Unresolved} in the tele VM.
     *
     * @author Michael Van De Vanter
     */
    public static final class Unresolved extends TelePoolConstant {

        public Unresolved(TeleVM teleVM, Reference unresolvedClassConstantReference) {
            super(teleVM, unresolvedClassConstantReference);
        }

        @Override
        public boolean isResolved() {
            return false;
        }

    }

    /**
     * Inspector's canonical surrogate for an object of type {@link ClassConstant.Resolved} in the tele VM.
     *
     * @author Michael Van De Vanter
     */
    public static final class Resolved extends TeleClassConstant{

        private TeleClassActor teleClassActor;

        public Resolved(TeleVM teleVM, Reference resolvedClassConstantReference) {
            super(teleVM, resolvedClassConstantReference);
        }

        @Override
        public boolean isResolved() {
            return true;
        }
        /**
         * @return surrogate for the {@ClassActor} in the teleVM to which the constant was resolved
         */
        public TeleClassActor getTeleClassActor() {
            if (teleClassActor == null) {
                final Reference classActorReference = teleVM().fields().ClassConstant$Resolved_classActor.readReference(reference());
                teleClassActor = (TeleClassActor) teleVM().makeTeleObject(classActorReference);
            }
            return teleClassActor;
        }

        @Override
        public String maxineRole() {
            return "Resolved ClassRefConstant";
        }

        @Override
        public String maxineTerseRole() {
            return "ClassRefConst";
        }

    }

}

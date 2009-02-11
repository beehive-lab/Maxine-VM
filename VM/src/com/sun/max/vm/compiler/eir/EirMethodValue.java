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
package com.sun.max.vm.compiler.eir;

import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.target.*;

/**
 * Per-translation placeholder for an EIR method.
 * We need separate instances of these per translation.
 *
 * Also, instead of EirMethod we store ClassMethodActor to
 * enable the inspector to limit the transitive closure when
 * loading EIR from the inspectee.
 * (ClassMethodActor instances can be substituted with corresponding
 * inspector objects, for EirMethod there aren't any in most cases,
 * which would lead to unwanted extra loading.)
 *
 * @author Bernd Mathiske
 */
public final class EirMethodValue extends EirValue {

    private ClassMethodActor _classMethodActor;

    public ClassMethodActor classMethodActor() {
        return _classMethodActor;
    }

    @Override
    public boolean isConstant() {
        return true;
    }

    @Override
    public String toString() {
        return "<" + _classMethodActor.name() + ">";
    }

    public EirMethodValue(ClassMethodActor classMethodActor) {
        _classMethodActor = classMethodActor;
        fixLocation(new Location());
    }

    public final class Location extends EirLocation {

        private Location() {
            super();
        }

        public MethodActor classMethodActor() {
            return _classMethodActor;
        }

        @Override
        public EirLocationCategory category() {
            return EirLocationCategory.METHOD;
        }

        @Override
        public String toString() {
            return "<method:" + _classMethodActor + ">";
        }

        @Override
        public TargetLocation toTargetLocation() {
            return new TargetLocation.Method(_classMethodActor);
        }
    }

}

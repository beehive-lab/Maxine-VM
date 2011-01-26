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
package com.sun.max.vm.cps.eir;

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

    private ClassMethodActor classMethodActor;

    public ClassMethodActor classMethodActor() {
        return classMethodActor;
    }

    @Override
    public boolean isConstant() {
        return true;
    }

    @Override
    public String toString() {
        return "<" + classMethodActor.name + ">";
    }

    public EirMethodValue(ClassMethodActor classMethodActor) {
        this.classMethodActor = classMethodActor;
        fixLocation(new Location());
    }

    public final class Location extends EirLocation {

        private Location() {
            super();
        }

        public MethodActor classMethodActor() {
            return classMethodActor;
        }

        @Override
        public EirLocationCategory category() {
            return EirLocationCategory.METHOD;
        }

        @Override
        public String toString() {
            return "<method:" + classMethodActor + ">";
        }

        @Override
        public TargetLocation toTargetLocation() {
            return new TargetLocation.Method(classMethodActor);
        }
    }

}

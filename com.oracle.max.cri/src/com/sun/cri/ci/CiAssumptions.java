/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.cri.ci;

import java.io.*;

import com.sun.cri.ri.*;

/**
 * Class for recording optimistic assumptions made during compilation.
 * Recorded assumption can be visited for subsequent processing using
 * an implementation of the {@link CiAssumptionProcessor} interface.
 */
public final class CiAssumptions implements Serializable {

    public abstract static class Assumption implements Serializable {
        /**
         * Apply an assumption processor to the assumption.
         *
         * @param processor the assumption processor to apply
         * @return true if a next assumption in a list should be fed to the processor.
         */
        abstract boolean visit(CiAssumptionProcessor processor);
    }

    public static final class ConcreteSubtype extends Assumption {
        /**
         * Type the assumption is made about.
         */
        public final RiType context;
        /**
         * Assumed unique concrete sub-type of the context type.
         */
        public final RiType subtype;

        public ConcreteSubtype(RiType context, RiType subtype) {
            this.context = context;
            this.subtype = subtype;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + context.hashCode();
            result = prime * result + subtype.hashCode();
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ConcreteSubtype) {
                ConcreteSubtype other = (ConcreteSubtype) obj;
                return other.context == context && other.subtype == subtype;
            }
            return false;
        }

        @Override
        public boolean visit(CiAssumptionProcessor processor) {
            return processor.doConcreteSubtype(this);
        }
    }

    public static final class ConcreteMethod extends Assumption {
        public final RiMethod context;
        public final RiMethod method;

        public ConcreteMethod(RiMethod context, RiMethod method) {
            this.context = context;
            this.method = method;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + context.hashCode();
            result = prime * result + method.hashCode();
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ConcreteMethod) {
                ConcreteMethod other = (ConcreteMethod) obj;
                return other.context == context && other.method == method;
            }
            return false;
        }

        @Override
        public boolean visit(CiAssumptionProcessor processor) {
            return processor.doConcreteMethod(this);
        }
    }

    /**
     * Array with the assumptions. This field is directly accessed from C++ code in the Graal/HotSpot implementation.
     */
    private Assumption[] list;

    private int count;

    /**
     * Returns whether any assumptions have been registered.
     * @return {@code true} if at least one assumption has been registered, {@code false} otherwise.
     */
    public boolean isEmpty() {
        return count == 0;
    }

    /**
     * Records an assumption that the specified type has no finalizable subclasses.
     *
     * @param receiverType the type that is assumed to have no finalizable subclasses
     * @return {@code true} if the assumption was recorded and can be assumed; {@code false} otherwise
     */
    public boolean recordNoFinalizableSubclassAssumption(RiType receiverType) {
        return false;
    }

    /**
     * Records that "subtype" is the only concrete subtype in the class hierarchy below "context".
     * @param context the root of the subtree of the class hierarchy that this assumptions is about
     * @param subtype the one concrete subtype
     */
    public void recordConcreteSubtype(RiType context, RiType subtype) {
        record(new ConcreteSubtype(context, subtype));
    }

    /**
     * Records that "method" is the only possible concrete target for a virtual call to "context".
     * @param context the method that is the target of the virtual call
     * @param method the concrete method that is the only possible target for the virtual call
     */
    public void recordConcreteMethod(RiMethod context, RiMethod method) {
        record(new ConcreteMethod(context, method));
    }

    /**
     * Iterate over assumptions using an assumption processor.
     * @param processor the processor that is called back for each assumption
     */
    public void visit(CiAssumptionProcessor processor) {
        for (int i = 0; i < count; i++) {
            if (!list[i].visit(processor)) {
                return;
            }
        }
    }

    private void record(Assumption assumption) {
        if (list == null) {
            list = new Assumption[4];
        } else {
            for (int i = 0; i < count; ++i) {
                if (assumption.equals(list[i])) {
                    return;
                }
            }
        }
        if (list.length == count) {
            Assumption[] newList = new Assumption[list.length * 2];
            for (int i = 0; i < list.length; ++i) {
                newList[i] = list[i];
            }
            list = newList;
        }
        list[count] = assumption;
        count++;
    }

}

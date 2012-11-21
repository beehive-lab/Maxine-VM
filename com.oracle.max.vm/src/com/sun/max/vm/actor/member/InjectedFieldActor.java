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
package com.sun.max.vm.actor.member;

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Support for injecting fields into JDK classes. The actor for such fields is denoted by having a
 * {@link com.sun.max.vm.actor.member.FieldActor#INJECTED} attribute in its {@link FieldActor#flags() flags}.
 * <p>
 */
public interface InjectedFieldActor<Value_Type extends Value<Value_Type>> {

    /**
     * Gets the type descriptor of this injected field's holder.
     * @return the type descriptor of the holder
     */
    TypeDescriptor holderTypeDescriptor();

    /**
     * Support for reading an injected field while bootstrapping.
     */
    @HOSTED_ONLY
    Value_Type readInjectedValue(Object object);

    public static final class Static {

        private static InjectedFieldActor[] injectedFieldActors = new InjectedFieldActor[0];

        static {
            Classes.initialize(InjectedReferenceFieldActor.class);
        }

        private Static() {
        }

        @HOSTED_ONLY
        static void registerInjectedFieldActor(InjectedFieldActor injectedFieldActor) {
            assert ClassRegistry.BOOT_CLASS_REGISTRY.get(injectedFieldActor.holderTypeDescriptor()) == null :
                "cannot inject field into pre-existing class " + injectedFieldActor.holderTypeDescriptor().toJavaString();
            final int length = injectedFieldActors.length;
            injectedFieldActors = java.util.Arrays.copyOf(injectedFieldActors, length + 1);
            injectedFieldActors[length] = injectedFieldActor;

        }

        /**
         * Appends the injected fields (if any) for a given class.
         *
         * @param isStatic specifies if {@code fieldActors} contains only static fields or only instance fields
         * @param fieldActors the fields explicitly declared for the class (e.g. in a class file). This list will
         *            have one or more fields appended to it if {@code holder} denotes a class for which there are
         *            injected fields
         * @param holder the holder of {@code fieldActors}
         */
        public static void injectFieldActors(boolean isStatic, List<FieldActor> fieldActors, TypeDescriptor holder) {
            for (InjectedFieldActor injectedFieldActor : injectedFieldActors) {
                if (injectedFieldActor.holderTypeDescriptor().equals(holder)) {
                    final FieldActor fieldActor = (FieldActor) injectedFieldActor;
                    if (fieldActor.isStatic() == isStatic) {
                        fieldActors.add(fieldActor);
                    }
                }
            }
        }
    }
}

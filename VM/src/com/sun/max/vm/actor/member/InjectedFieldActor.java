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
package com.sun.max.vm.actor.member;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Support for injecting fields into JDK classes. The actor for such fields is denoted by having a
 * {@link com.sun.max.vm.actor.member.FieldActor#INJECTED} attribute in its {@link FieldActor#flags() flags}.
 * <p>
 *
 * @author Doug Simon
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
    Value_Type readInjectedValue(Reference reference);

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
         * @param staticFields specifies if {@code fieldActors} contains only static fields or only dynamic fields
         * @param fieldActors the fields explicitly declared for the class (e.g. in a class file). This sequence will
         *            have one or more fields appended to it if {@code holder} denotes a class for which there are
         *            injected fields
         * @param holder the holder of {@code fieldActors}
         * @return the result of appending the injected fields (if any) to {@code fieldActors}
         */
        public static FieldActor[] injectFieldActors(boolean staticFields, FieldActor[] fieldActors, TypeDescriptor holder) {
            FieldActor[] result = fieldActors;
            for (InjectedFieldActor injectedFieldActor : injectedFieldActors) {
                if (injectedFieldActor.holderTypeDescriptor().equals(holder)) {
                    final FieldActor fieldActor = (FieldActor) injectedFieldActor;
                    if (fieldActor.isStatic() == staticFields) {
                        result = Arrays.append(result, fieldActor);
                    }
                }
            }
            return result;
        }
    }
}

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
package com.sun.max.vm.actor.holder;

import java.lang.annotation.*;

import com.sun.max.annotate.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.prototype.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Internal representations of "primitive classes" such as 'int', 'char', etc.
 *
 * @author Bernd Mathiske
 */
public final class PrimitiveClassActor<Value_Type extends Value<Value_Type>> extends ClassActor {

    @HOSTED_ONLY
    public PrimitiveClassActor(Kind<Value_Type> kind) {
        super(kind,
              NO_SPECIFIC_LAYOUT,
              HostedBootClassLoader.HOSTED_BOOT_CLASS_LOADER,
              kind.name,
              NO_MAJOR_VERSION,
              NO_MINOR_VERSION,
              ACC_PUBLIC | ACC_FINAL,
              kind.typeDescriptor,
              NO_SUPER_CLASS_ACTOR,
              NO_COMPONENT_CLASS_ACTOR,
              InterfaceActor.NONE,
              FieldActor.NONE,
              MethodActor.NONE,
              NO_GENERIC_SIGNATURE,
              NO_RUNTIME_VISIBLE_ANNOTATION_BYTES,
              NO_SOURCE_FILE_NAME,
              NO_INNER_CLASSES,
              NO_OUTER_CLASS,
              NO_ENCLOSING_METHOD_INFO);
    }

    public TupleClassActor toWrapperClassActor() {
        return (TupleClassActor) ClassActor.fromJava(kind.boxedClass);
    }

    @Override
    public boolean isPrimitiveClassActor() {
        return true;
    }

    @Override
    public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
        return null;
    }
}

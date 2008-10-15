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

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.type.*;

/**
 * Class actors for hybrid objects (currently only hubs).
 * 
 * @author Bernd Mathiske
 */
public class HybridClassActor extends ReferenceClassActor {

    @INSPECTED
    private final ConstantPool _constantPool;

    HybridClassActor(ConstantPool constantPool,
                     ClassLoader classLoader,
                     Utf8Constant name,
                     char majorVersion,
                     char minorVersion,
                     int flags,
                     ClassActor superClassActor,
                     InterfaceActor[] interfaceActors,
                     FieldActor[] fieldActors,
                     MethodActor[] methodActors) {
        super(Kind.REFERENCE,
              Layout.hybridLayout(),
              classLoader,
              name,
              majorVersion,
              minorVersion,
              flags,
              JavaTypeDescriptor.getDescriptorForWellFormedTupleName(name.toString()),
              superClassActor,
              NO_COMPONENT_CLASS_ACTOR,
              interfaceActors,
              fieldActors,
              methodActors,
              NO_GENERIC_SIGNATURE,
              NO_RUNTIME_VISIBLE_ANNOTATION_BYTES,
              NO_SOURCE_FILE_NAME,
              NO_INNER_CLASSES,
              NO_OUTER_CLASS,
              NO_ENCLOSING_METHOD_INFO);
        _constantPool = constantPool;
        constantPool.setHolder(this);
    }

    @Override
    public final ConstantPool constantPool() {
        return _constantPool;
    }

    @Override
    protected Size layoutFields(SpecificLayout specificLayout) {
        final HybridLayout hybridLayout = (HybridLayout) specificLayout;
        return hybridLayout.layoutFields(superClassActor(), localInstanceFieldActors());
    }
}

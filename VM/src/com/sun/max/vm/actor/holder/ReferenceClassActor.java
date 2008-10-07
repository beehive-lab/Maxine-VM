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
/*VCSID=f7f90f57-7f9c-458f-9ba4-8cdf5fa3fb2b*/
package com.sun.max.vm.actor.holder;

import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.type.*;

/**
 * Internal representations of non-primitive classes whose instance values are object references.
 * 
 * @author Bernd Mathiske
 */
public abstract class ReferenceClassActor extends ClassActor {

    @Override
    public final boolean isReferenceClassActor() {
        return true;
    }

    protected ReferenceClassActor(Kind kind,
                                  SpecificLayout specificLayout,
                                  ClassLoader classLoader,
                                  Utf8Constant name,
                                  char majorVersion,
                                  char minorVersion,
                                  int flags,
                                  TypeDescriptor typeDescriptor,
                                  ClassActor superClassActor,
                                  ClassActor componentClassActor,
                                  InterfaceActor[] interfaceActors,
                                  FieldActor[] fieldActors,
                                  MethodActor[] methodActors,
                                  Utf8Constant genericSignature,
                                  byte[] runtimeVisibleAnnotationsBytes,
                                  String sourceFileName,
                                  TypeDescriptor[] innerClasses,
                                  TypeDescriptor outerClass,
                                  EnclosingMethodInfo enclosingMethodInfo) {
        super(kind,
              specificLayout,
              classLoader,
              name,
              majorVersion,
              minorVersion,
              flags,
              typeDescriptor,
              superClassActor,
              componentClassActor,
              interfaceActors,
              fieldActors,
              methodActors,
              genericSignature,
              runtimeVisibleAnnotationsBytes,
              sourceFileName,
              innerClasses,
              outerClass,
              enclosingMethodInfo);
    }

}

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
package com.sun.max.vm.classfile.create;

import com.sun.max.vm.type.*;

/**
 * @author Bernd Mathiske
 */
class MillMethod {

    final int modifiers;
    final int nameIndex;
    final int descriptorIndex;
    final MillCode code;
    final MillClassConstant[] exceptions;
    final int numberOfBytes;
    final MillMethod next;

    MillMethod(MillClass millClass, int modifiers, String name, SignatureDescriptor signatureDescriptor, MillCode code, MillClassConstant[] exceptions) {
        this.modifiers = modifiers;
        this.nameIndex = millClass.makeUtf8Constant(name).index;
        this.descriptorIndex = millClass.makeUtf8Constant(signatureDescriptor.toString()).index;
        this.code = code;
        this.exceptions = exceptions;
        this.numberOfBytes = 8 + 14 + code.nBytes() + 4 + ((exceptions.length > 0) ? 8 + (2 * exceptions.length) : 0);
        this.next = millClass.methodList;
        millClass.methodList = this;
    }

}

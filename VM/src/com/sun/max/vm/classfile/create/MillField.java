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
/*VCSID=d683397e-085c-4cb0-8248-9d969399100d*/
package com.sun.max.vm.classfile.create;

/**
 * Field data.
 * 
 * @author Bernd Mathiske
 */
class MillField {

    final int _modifiers;
    final int _nameIndex;
    final int _descriptorIndex;
    final MillField _next;

    MillField(MillClass millClass, int modifiers, String name, String descriptor) {
        this._modifiers = modifiers;
        this._nameIndex = millClass.makeUtf8Constant(name)._index;
        this._descriptorIndex = millClass.makeUtf8Constant(descriptor)._index;
        this._next = millClass._fieldList;
        millClass._fieldList = this;
    }

}

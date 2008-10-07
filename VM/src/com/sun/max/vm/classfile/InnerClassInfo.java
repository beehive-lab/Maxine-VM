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
/*VCSID=0c90cfcf-67f2-4596-a77c-2d46c86d34da*/
package com.sun.max.vm.classfile;

import com.sun.max.vm.classfile.constant.*;

/**
 * Representations derived from array entries in the inner classes attribute as described in #4.7.5.
 *
 * @author Bernd Mathiske
 * 
 * @see InnerClassesAttribute
 */
public class InnerClassInfo {

    private final char _innerClassIndex;
    private final char _outerClassIndex;
    private final String _sourceName;
    private final char _flags;

    public int innerClassIndex() {
        return _innerClassIndex;
    }

    public int outerClassIndex() {
        return _outerClassIndex;
    }

    public String sourceName() {
        return _sourceName;
    }

    public int flags() {
        return _flags;
    }

    public InnerClassInfo(ClassfileStream classfileStream, ConstantPool constantPool) {
        _innerClassIndex = (char) classfileStream.readUnsigned2();
        _outerClassIndex = (char) classfileStream.readUnsigned2();
        final int sourceNameIndex = classfileStream.readUnsigned2();
        _sourceName = (sourceNameIndex == 0) ? null : constantPool.utf8At(sourceNameIndex, "inner class source name").toString();
        _flags = (char) classfileStream.readUnsigned2();

        // Access the components to verify the format of this attribute
        if (_innerClassIndex != 0) {
            constantPool.classAt(_innerClassIndex);
        }
    }

    @Override
    public String toString() {
        return "inner=" + (int) _innerClassIndex + ",outer=" + (int) _outerClassIndex + ",flags=0x" + Integer.toHexString(_flags) + ",source=" + _sourceName;
    }
}

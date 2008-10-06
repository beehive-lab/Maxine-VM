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
/*VCSID=303a0f48-4833-44d8-a0b2-c8dabd1ca187*/
package com.sun.max.jdwp.vm.data;

/**
 * This class represents an entry in the line table information of a Java method.
 *
 * @author Thomas Wuerthinger
 *
 */
public class LineTableEntry extends AbstractSerializableObject {

    private long _codeIndex;
    private int _lineNumber;

    public LineTableEntry(long codeIndex, int lineNumber) {
        _codeIndex = codeIndex;
        _lineNumber = lineNumber;
    }

    public long getCodeIndex() {
        return _codeIndex;
    }

    public int getLineNumber() {
        return _lineNumber;
    }
}

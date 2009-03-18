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
package com.sun.max.vm.bytecode.graft;

import com.sun.max.io.*;
import com.sun.max.vm.classfile.constant.*;

/**
 * A bytecode assembler that assembles into a byte array.
 * 
 * @author Doug Simon
 */
public class ByteArrayBytecodeAssembler extends BytecodeAssembler {

    public ByteArrayBytecodeAssembler(ConstantPoolEditor constantPoolEditor) {
        super(constantPoolEditor);
        _codeStream = new SeekableByteArrayOutputStream();
    }

    private final SeekableByteArrayOutputStream _codeStream;

    @Override
    protected void setWritePosition(int position) {
        _codeStream.seek(position);
    }

    @Override
    protected void writeByte(byte b) {
        _codeStream.write(b);
    }

    @Override
    public byte[] code() {
        fixup();
        return _codeStream.toByteArray();
    }
}

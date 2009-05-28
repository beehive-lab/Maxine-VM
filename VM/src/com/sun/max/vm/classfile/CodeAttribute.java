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
package com.sun.max.vm.classfile;

import java.io.*;

import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.vm.classfile.constant.*;

/**
 * "Code" attributes in class files, see #4.7.3.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 * @author David Liu
 */
public final class CodeAttribute {

    public static final Sequence<ExceptionHandlerEntry> NO_EXCEPTION_HANDLER_TABLE = Sequence.Static.empty(ExceptionHandlerEntry.class);

    @INSPECTED
    private final ConstantPool _constantPool;

    @INSPECTED
    private final byte[] _code;

    private StackMapTable _stackMapTableAttribute;
    private final byte[] _encodedData;
    private final int _exceptionHandlerTableOffset;
    private final int _lineNumberTableOffset;
    private final int _localVariableTableOffset;
    private final char _maxStack;
    private final char _maxLocals;

    public CodeAttribute(ConstantPool constantPool,
                    byte[] code,
                    char maxStack,
                    char maxLocals,
                    Sequence<ExceptionHandlerEntry> exceptionHandlerTable,
                    LineNumberTable lineNumberTable,
                    LocalVariableTable localVariableTable,
                    StackMapTable stackMapTable) {
        _constantPool = constantPool;
        _code = code;
        _maxStack = maxStack;
        _maxLocals = maxLocals;
        _stackMapTableAttribute = stackMapTable;

        final ByteArrayOutputStream encodingStream = new ByteArrayOutputStream();
        final DataOutputStream dataOutputStream = new DataOutputStream(encodingStream);

        int exceptionHandlerTableOffset = -1;
        int lineNumberTableOffset = -1;
        int localVariableTableOffset = -1;

        try {
            dataOutputStream.write(_code);
            if (!exceptionHandlerTable.isEmpty()) {
                exceptionHandlerTableOffset = encodingStream.size();
                ExceptionHandlerEntry.encode(exceptionHandlerTable, dataOutputStream);
            }
            if (!lineNumberTable.isEmpty()) {
                lineNumberTableOffset = encodingStream.size();
                lineNumberTable.encode(dataOutputStream);
            }
            if (!localVariableTable.isEmpty()) {
                localVariableTableOffset = encodingStream.size();
                localVariableTable.encode(dataOutputStream);
            }
        } catch (IOException e) {
            ProgramError.unexpected(e);
        }

        _exceptionHandlerTableOffset = exceptionHandlerTableOffset;
        _lineNumberTableOffset = lineNumberTableOffset;
        _localVariableTableOffset = localVariableTableOffset;
        _encodedData = encodingStream.toByteArray();

    }

    static void writeCharArray(DataOutputStream dataOutputStream, char[] buf) throws IOException {
        assert buf.length <= Short.MAX_VALUE;
        dataOutputStream.writeShort(buf.length);
        for (char c : buf) {
            dataOutputStream.writeChar(c);
        }
    }

    static char[] readCharArray(DataInputStream dataInputStream) throws IOException {
        final int length = dataInputStream.readUnsignedShort();
        assert length != 0;
        final char[] buf = new char[length];
        for (int i = 0; i != length; ++i) {
            buf[i] = dataInputStream.readChar();
        }
        return buf;
    }

    /**
     * Gets the constant pool that must be used when processing the code in this CodeAttribute. This is required as one
     * CodeAttribute may replace another whenever {@linkplain METHOD_SUBSTITUTIONS method substitution} occurs.
     */
    public ConstantPool constantPool() {
        return _constantPool;
    }

    public byte[] code() {
        return _code;
    }

    public byte[] encodedData() {
        return _encodedData;
    }

    /**
     * Gets the maximum depth of the operand stack of this method at any point during execution of the method.
     */
    public int maxStack() {
        return _maxStack;
    }

    /**
     * Gets the number of local variables in the local variable array allocated upon invocation of this method,
     * including the local variables used to pass parameters to the method on its invocation. The greatest local
     * variable index for a value of type {@code long} or {@code double} is {@code maxLocals() - 2}. The greatest local
     * variable index for a value of any other type is {@code maxLocals() - 1}.
     */
    public int maxLocals() {
        return _maxLocals;
    }

    private DataInputStream encodedData(int offset) {
        return new DataInputStream(new ByteArrayInputStream(_encodedData, offset, _encodedData.length - offset));
    }

    public Sequence<ExceptionHandlerEntry> exceptionHandlerTable() {
        try {
            return _exceptionHandlerTableOffset == -1 ? Sequence.Static.empty(ExceptionHandlerEntry.class) : ExceptionHandlerEntry.decode(encodedData(_exceptionHandlerTableOffset));
        } catch (IOException e) {
            throw ProgramError.unexpected(e);
        }
    }

    public LineNumberTable lineNumberTable() {
        try {
            return _lineNumberTableOffset == -1 ? LineNumberTable.EMPTY : LineNumberTable.decode(encodedData(_lineNumberTableOffset));
        } catch (IOException e) {
            throw ProgramError.unexpected(e);
        }
    }

    public LocalVariableTable localVariableTable() {
        try {
            return _localVariableTableOffset == -1 ? LocalVariableTable.EMPTY : LocalVariableTable.decode(encodedData(_localVariableTableOffset));
        } catch (IOException e) {
            throw ProgramError.unexpected(e);
        }
    }

    /**
     * @return null if there is no stack map table associated with this code attribute
     */
    public StackMapTable stackMapTable() {
        return _stackMapTableAttribute;
    }

    public void setStackMapTableAttribute(StackMapTable stackMapTable) {
        _stackMapTableAttribute = stackMapTable;
    }
}

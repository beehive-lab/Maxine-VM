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
/*VCSID=2e8a9900-3b46-4db3-a0af-9e952a772a5a*/
package com.sun.max.vm.template.source;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.template.*;
import com.sun.max.vm.type.*;

/**
 * Templates for implementation of getfield and putfield when the field is resolved at compile-time
 * (TemplateChooser.Selector.RESOLVED selector).
 *
 * @author Laurent Daynes
 */
@TEMPLATE(resolved = TemplateChooser.Resolved.YES)
public class ResolvedFieldAccessTemplateSource {


    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETFIELD, kind = KindEnum.REFERENCE)
    public static void rgetfield(int offset) {
        final Object object = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.pokeReference(0, TupleAccess.readObject(object, offset));
    }


    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETFIELD, kind = KindEnum.WORD)
    public static void wgetfield(int offset) {
        final Object object = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.pokeWord(0, TupleAccess.readWord(object, offset));
    }


    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETFIELD, kind = KindEnum.BYTE)
    public static void bgetfield(int offset) {
        final Object object = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.pokeInt(0, TupleAccess.readByte(object, offset));
    }


    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETFIELD, kind = KindEnum.CHAR)
    public static void cgetfield(int offset) {
        final Object object = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.pokeInt(0, TupleAccess.readChar(object, offset));
    }


    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETFIELD, kind = KindEnum.DOUBLE)
    public static void dgetfield(int offset) {
        final Object object = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.addSlots(1);
        JitStackFrameOperation.pokeDouble(0, TupleAccess.readDouble(object, offset));
    }


    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETFIELD, kind = KindEnum.FLOAT)
    public static void fgetfield(int offset) {
        final Object object = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.pokeFloat(0, TupleAccess.readFloat(object, offset));
    }


    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETFIELD, kind = KindEnum.INT)
    public static void igetfield(int offset) {
        final Object object = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.pokeInt(0, TupleAccess.readInt(object, offset));
    }


    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETFIELD, kind = KindEnum.LONG)
    public static void jgetfield(int offset) {
        final Object object = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.addSlots(1);
        JitStackFrameOperation.pokeLong(0, TupleAccess.readLong(object, offset));
    }


    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETFIELD, kind = KindEnum.SHORT)
    public static void sgetfield(int offset) {
        final Object object = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.pokeInt(0, TupleAccess.readShort(object, offset));
    }


    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETFIELD, kind = KindEnum.BOOLEAN)
    public static void zgetfield(int offset) {
        final Object object = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.pokeInt(0, UnsafeLoophole.booleanToByte(TupleAccess.readBoolean(object, offset)));
    }


    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTFIELD, kind = KindEnum.REFERENCE)
    public static void rputfield(int offset) {
        final Object object = JitStackFrameOperation.peekReference(1);
        final Object value = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.removeSlots(2);
        TupleAccess.noinlineWriteObject(object, offset, value);
    }


    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTFIELD, kind = KindEnum.WORD)
    public static void wputfield(int offset) {
        final Object object = JitStackFrameOperation.peekReference(1);
        final Word value = JitStackFrameOperation.peekWord(0);
        JitStackFrameOperation.removeSlots(2);
        TupleAccess.writeWord(object, offset, value);
    }


    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTFIELD, kind = KindEnum.BYTE)
    public static void bputfield(int offset) {
        final Object object = JitStackFrameOperation.peekReference(1);
        final byte value = (byte) JitStackFrameOperation.peekInt(0);
        JitStackFrameOperation.removeSlots(2);
        TupleAccess.writeByte(object, offset, value);
    }


    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTFIELD, kind = KindEnum.CHAR)
    public static void cputfield(int offset) {
        final Object object = JitStackFrameOperation.peekReference(1);
        final char value = (char) JitStackFrameOperation.peekInt(0);
        JitStackFrameOperation.removeSlots(2);
        TupleAccess.writeChar(object, offset, value);
    }


    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTFIELD, kind = KindEnum.DOUBLE)
    public static void dputfield(int offset) {
        final Object object = JitStackFrameOperation.peekReference(2);
        final double value = JitStackFrameOperation.peekDouble(0);
        JitStackFrameOperation.removeSlots(3);
        TupleAccess.writeDouble(object, offset, value);
    }


    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTFIELD, kind = KindEnum.FLOAT)
    public static void fputfield(int offset) {
        final Object object = JitStackFrameOperation.peekReference(1);
        final float value = JitStackFrameOperation.peekFloat(0);
        JitStackFrameOperation.removeSlots(2);
        TupleAccess.writeFloat(object, offset, value);
    }


    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTFIELD, kind = KindEnum.INT)
    public static void iputfield(int offset) {
        final Object object = JitStackFrameOperation.peekReference(1);
        final int value = JitStackFrameOperation.peekInt(0);
        JitStackFrameOperation.removeSlots(2);
        TupleAccess.writeInt(object, offset, value);
    }


    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTFIELD, kind = KindEnum.LONG)
    public static void jputfield(int offset) {
        final Object object = JitStackFrameOperation.peekReference(2);
        final long value = JitStackFrameOperation.peekLong(0);
        JitStackFrameOperation.removeSlots(3);
        TupleAccess.writeLong(object, offset, value);
    }


    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTFIELD, kind = KindEnum.SHORT)
    public static void sputfield(int offset) {
        final Object object = JitStackFrameOperation.peekReference(1);
        final short value = (short) JitStackFrameOperation.peekInt(0);
        JitStackFrameOperation.removeSlots(2);
        TupleAccess.writeShort(object, offset, value);
    }


    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTFIELD, kind = KindEnum.BOOLEAN)
    public static void zputfield(int offset) {
        final Object object = JitStackFrameOperation.peekReference(1);
        final boolean value = UnsafeLoophole.byteToBoolean((byte) JitStackFrameOperation.peekInt(0));
        JitStackFrameOperation.removeSlots(2);
        TupleAccess.writeBoolean(object, offset, value);
    }
}

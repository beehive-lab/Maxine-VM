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
/*VCSID=e3d17597-6cfb-4953-a1e5-b459646d5ac6*/
package com.sun.max.vm.template.source;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.template.*;
import com.sun.max.vm.type.*;

@TEMPLATE(initialized = TemplateChooser.Initialized.YES)
public class InitializedStaticFieldAccessTemplateSource {

    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETSTATIC, kind = KindEnum.BYTE)
    public static void bgetstatic(Object staticTuple, int offset) {
        JitStackFrameOperation.pushInt(TupleAccess.readByte(staticTuple, offset));
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETSTATIC, kind = KindEnum.CHAR)
    public static void cgetstatic(Object staticTuple, int offset) {
        JitStackFrameOperation.pushInt(TupleAccess.readChar(staticTuple, offset));
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETSTATIC, kind = KindEnum.DOUBLE)
    public static void dgetstatic(Object staticTuple, int offset) {
        JitStackFrameOperation.pushDouble(TupleAccess.readDouble(staticTuple, offset));
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETSTATIC, kind = KindEnum.FLOAT)
    public static void fgetstatic(Object staticTuple, int offset) {
        JitStackFrameOperation.pushFloat(TupleAccess.readFloat(staticTuple, offset));
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETSTATIC, kind = KindEnum.INT)
    public static void igetstatic(Object staticTuple, int offset) {
        JitStackFrameOperation.pushInt(TupleAccess.readInt(staticTuple, offset));
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETSTATIC, kind = KindEnum.LONG)
    public static void jgetstatic(Object staticTuple, int offset) {
        JitStackFrameOperation.pushLong(TupleAccess.readLong(staticTuple, offset));
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETSTATIC, kind = KindEnum.REFERENCE)
    public static void rgetstatic(Object staticTuple, int offset) {
        JitStackFrameOperation.pushReference(TupleAccess.readObject(staticTuple, offset));
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETSTATIC, kind = KindEnum.SHORT)
    public static void sgetstatic(Object staticTuple, int offset) {
        JitStackFrameOperation.pushInt(TupleAccess.readShort(staticTuple, offset));
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETSTATIC, kind = KindEnum.BOOLEAN)
    public static void zgetstatic(Object staticTuple, int offset) {
        JitStackFrameOperation.pushInt(UnsafeLoophole.booleanToByte(TupleAccess.readBoolean(staticTuple, offset)));
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTSTATIC, kind = KindEnum.BYTE)
    public static void bputstatic(Object staticTuple, int offset) {
        final byte value = (byte) JitStackFrameOperation.popInt();
        TupleAccess.writeByte(staticTuple, offset, value);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTSTATIC, kind = KindEnum.CHAR)
    public static void cputstatic(Object staticTuple, int offset) {
        final char value = (char) JitStackFrameOperation.popInt();
        TupleAccess.writeChar(staticTuple, offset, value);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTSTATIC, kind = KindEnum.DOUBLE)
    public static void dputstatic(Object staticTuple, int offset) {
        final double value = JitStackFrameOperation.popDouble();
        TupleAccess.writeDouble(staticTuple, offset, value);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTSTATIC, kind = KindEnum.FLOAT)
    public static void fputstatic(Object staticTuple, int offset) {
        final float value = JitStackFrameOperation.popFloat();
        TupleAccess.writeFloat(staticTuple, offset, value);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTSTATIC, kind = KindEnum.INT)
    public static void iputstatic(Object staticTuple, int offset) {
        final int value = JitStackFrameOperation.popInt();
        TupleAccess.writeInt(staticTuple, offset, value);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTSTATIC, kind = KindEnum.LONG)
    public static void jputstatic(Object staticTuple, int offset) {
        final long value = JitStackFrameOperation.popLong();
        TupleAccess.writeLong(staticTuple, offset, value);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTSTATIC, kind = KindEnum.REFERENCE)
    public static void rputstatic(Object staticTuple, int offset) {
        final Object value = JitStackFrameOperation.popReference();
        TupleAccess.noinlineWriteObject(staticTuple, offset, value);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTSTATIC, kind = KindEnum.SHORT)
    public static void sputstatic(Object staticTuple, int offset) {
        final short value = (short) JitStackFrameOperation.popInt();
        TupleAccess.writeShort(staticTuple, offset, value);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTSTATIC, kind = KindEnum.BOOLEAN)
    public static void zputstatic(Object staticTuple, int offset) {
        final boolean value = UnsafeLoophole.byteToBoolean((byte) JitStackFrameOperation.popInt());
        TupleAccess.writeBoolean(staticTuple, offset, value);
    }
}

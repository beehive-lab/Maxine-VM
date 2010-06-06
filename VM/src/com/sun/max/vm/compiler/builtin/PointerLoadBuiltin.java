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
package com.sun.max.vm.compiler.builtin;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.object.host.*;

/**
 * @author Bernd Mathiske
 */
public abstract class PointerLoadBuiltin extends PointerBuiltin {

    private PointerLoadBuiltin() {
        super();
    }

    @Override
    public final boolean hasSideEffects() {
        return false;
    }

    @Override
    public int reasonsMayStop() {
        return Stoppable.NULL_POINTER_CHECK;
    }

    public static class ReadByte extends PointerLoadBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitReadByte(this, result, arguments);
        }

        public static final ReadByte BUILTIN = new ReadByte();
    }

    public static class ReadByteAtIntOffset extends PointerLoadBuiltin {
        @BUILTIN(value = ReadByteAtIntOffset.class)
        private static byte readByteAtIntOffset(Object tuple, int offset) {
            final Hub hub = HostObjectAccess.readHub(tuple);
            return HostTupleAccess.readByte(tuple, hub.findFieldActor(offset));
        }

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitReadByteAtIntOffset(this, result, arguments);
        }

        public static final ReadByteAtIntOffset BUILTIN = new ReadByteAtIntOffset();
    }

    public static class GetByte extends PointerLoadBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 3;
            visitor.visitGetByte(this, result, arguments);
        }

        public static final GetByte BUILTIN = new GetByte();
    }

    public static class ReadShort extends PointerLoadBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitReadShort(this, result, arguments);
        }

        public static final ReadShort BUILTIN = new ReadShort();
    }

    public static class ReadShortAtIntOffset extends PointerLoadBuiltin {
        @BUILTIN(value = ReadShortAtIntOffset.class)
        private static short readShortAtIntOffset(Object tuple, int offset) {
            final Hub hub = HostObjectAccess.readHub(tuple);
            return HostTupleAccess.readShort(tuple, hub.findFieldActor(offset));
        }

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            visitor.visitReadShortAtIntOffset(this, result, arguments);
        }

        public static final ReadShortAtIntOffset BUILTIN = new ReadShortAtIntOffset();
    }

    public static class GetShort extends PointerLoadBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 3;
            visitor.visitGetShort(this, result, arguments);
        }

        public static final GetShort BUILTIN = new GetShort();
    }

    public static class ReadChar extends PointerLoadBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitReadChar(this, result, arguments);
        }

        public static final ReadChar BUILTIN = new ReadChar();
    }

    public static class ReadCharAtIntOffset extends PointerLoadBuiltin {
        @BUILTIN(value = ReadCharAtIntOffset.class)
        private static char readCharAtIntOffset(Object tuple, int offset) {
            final Hub hub = HostObjectAccess.readHub(tuple);
            return HostTupleAccess.readChar(tuple, hub.findFieldActor(offset));
        }

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitReadCharAtIntOffset(this, result, arguments);
        }

        public static final ReadCharAtIntOffset BUILTIN = new ReadCharAtIntOffset();
    }

    public static class GetChar extends PointerLoadBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 3;
            visitor.visitGetChar(this, result, arguments);
        }

        public static final GetChar BUILTIN = new GetChar();
    }

    public static class ReadInt extends PointerLoadBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitReadInt(this, result, arguments);
        }

        public static final ReadInt BUILTIN = new ReadInt();
    }

    public static class ReadIntAtIntOffset extends PointerLoadBuiltin {
        @BUILTIN(value = ReadIntAtIntOffset.class)
        private static int readIntAtIntOffset(Object tuple, int offset) {
            final Hub hub = HostObjectAccess.readHub(tuple);
            return HostTupleAccess.readInt(tuple, hub.findFieldActor(offset));
        }

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitReadIntAtIntOffset(this, result, arguments);
        }

        public static final ReadIntAtIntOffset BUILTIN = new ReadIntAtIntOffset();
    }

    public static class GetInt extends PointerLoadBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 3;
            visitor.visitGetInt(this, result, arguments);
        }

        public static final GetInt BUILTIN = new GetInt();
    }

    public static class ReadFloat extends PointerLoadBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitReadFloat(this, result, arguments);
        }

        public static final ReadFloat BUILTIN = new ReadFloat();
    }

    public static class ReadFloatAtIntOffset extends PointerLoadBuiltin {
        @BUILTIN(value = ReadFloatAtIntOffset.class)
        private static float readFloatAtIntOffset(Object tuple, int offset) {
            final Hub hub = HostObjectAccess.readHub(tuple);
            return HostTupleAccess.readFloat(tuple, hub.findFieldActor(offset));
        }

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitReadFloatAtIntOffset(this, result, arguments);
        }

        public static final ReadFloatAtIntOffset BUILTIN = new ReadFloatAtIntOffset();
    }

    public static class GetFloat extends PointerLoadBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 3;
            visitor.visitGetFloat(this, result, arguments);
        }

        public static final GetFloat BUILTIN = new GetFloat();
    }

    public static class ReadLong extends PointerLoadBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitReadLong(this, result, arguments);
        }

        public static final ReadLong BUILTIN = new ReadLong();
    }

    public static class ReadLongAtIntOffset extends PointerLoadBuiltin {
        @BUILTIN(value = ReadLongAtIntOffset.class)
        private static long readLongAtIntOffset(Object tuple, int offset) {
            final Hub hub = HostObjectAccess.readHub(tuple);
            return HostTupleAccess.readLong(tuple, hub.findFieldActor(offset));
        }

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitReadLongAtIntOffset(this, result, arguments);
        }

        public static final ReadLongAtIntOffset BUILTIN = new ReadLongAtIntOffset();
    }

    public static class GetLong extends PointerLoadBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 3;
            visitor.visitGetLong(this, result, arguments);
        }

        public static final GetLong BUILTIN = new GetLong();
    }

    public static class ReadDouble extends PointerLoadBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitReadDouble(this, result, arguments);
        }

        public static final ReadDouble BUILTIN = new ReadDouble();
    }

    public static class ReadDoubleAtIntOffset extends PointerLoadBuiltin {
        @BUILTIN(value = ReadDoubleAtIntOffset.class)
        private static double readDoubleAtIntOffset(Object tuple, int offset) {
            final Hub hub = HostObjectAccess.readHub(tuple);
            return HostTupleAccess.readDouble(tuple, hub.findFieldActor(offset));
        }

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitReadDoubleAtIntOffset(this, result, arguments);
        }

        public static final ReadDoubleAtIntOffset BUILTIN = new ReadDoubleAtIntOffset();
    }

    public static class GetDouble extends PointerLoadBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 3;
            visitor.visitGetDouble(this, result, arguments);
        }

        public static final GetDouble BUILTIN = new GetDouble();
    }

    public static class ReadWord extends PointerLoadBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitReadWord(this, result, arguments);
        }

        public static final ReadWord BUILTIN = new ReadWord();
    }

    public static class ReadWordAtIntOffset extends PointerLoadBuiltin {
        @BUILTIN(value = ReadWordAtIntOffset.class)
        private static Word readWordAtIntOffset(Object tuple, int offset) {
            final Hub hub = HostObjectAccess.readHub(tuple);
            return HostTupleAccess.readWord(tuple, hub.findFieldActor(offset));
        }

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitReadWordAtIntOffset(this, result, arguments);
        }

        public static final ReadWordAtIntOffset BUILTIN = new ReadWordAtIntOffset();
    }

    public static class GetWord extends PointerLoadBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 3;
            visitor.visitGetWord(this, result, arguments);
        }

        public static final GetWord BUILTIN = new GetWord();
    }

    public static class ReadReference extends PointerLoadBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitReadReference(this, result, arguments);
        }

        public static final ReadReference BUILTIN = new ReadReference();
    }

    public static class ReadReferenceAtIntOffset extends PointerLoadBuiltin {
        @BUILTIN(value = ReadReferenceAtIntOffset.class)
        private static Object readReferenceAtIntOffset(Object tuple, int offset) {
            final Hub hub = HostObjectAccess.readHub(tuple);
            return HostTupleAccess.readObject(tuple, hub.findFieldActor(offset));
        }

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitReadReferenceAtIntOffset(this, result, arguments);
        }

        public static final ReadReferenceAtIntOffset BUILTIN = new ReadReferenceAtIntOffset();
    }

    public static class GetReference extends PointerLoadBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 3;
            visitor.visitGetReference(this, result, arguments);
        }

        public static final GetReference BUILTIN = new GetReference();
    }
}

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
import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.object.host.*;
import com.sun.max.vm.type.*;

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

    /**
     * After optimizing CIR, given a direct reference scheme, we may find objects/references in the position of pointers.
     * If this occurs at a foldable variant of a pointer load instruction,
     * then we can deduce a field access that we can meta-evaluate on the host VM.
     * Typically, this is the case when we access the 'offset' field of a FieldActor.
     */
    @Override
    public boolean isHostFoldable(IrValue[] arguments) {
        return IrValue.Static.areConstant(arguments) && arguments[0].kind() == Kind.REFERENCE;
    }

    public static class ReadByteAtLongOffset extends PointerLoadBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitReadByteAtLongOffset(this, result, arguments);
        }

        public static final ReadByteAtLongOffset BUILTIN = new ReadByteAtLongOffset();
    }

    public static class ReadByteAtIntOffset extends PointerLoadBuiltin {
        @BUILTIN(builtinClass = ReadByteAtIntOffset.class)
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

    public static class ReadShortAtLongOffset extends PointerLoadBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitReadShortAtLongOffset(this, result, arguments);
        }

        public static final ReadShortAtLongOffset BUILTIN = new ReadShortAtLongOffset();
    }

    public static class ReadShortAtIntOffset extends PointerLoadBuiltin {
        @BUILTIN(builtinClass = ReadShortAtIntOffset.class)
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

    public static class ReadCharAtLongOffset extends PointerLoadBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitReadCharAtLongOffset(this, result, arguments);
        }

        public static final ReadCharAtLongOffset BUILTIN = new ReadCharAtLongOffset();
    }

    public static class ReadCharAtIntOffset extends PointerLoadBuiltin {
        @BUILTIN(builtinClass = ReadCharAtIntOffset.class)
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

    public static class ReadIntAtLongOffset extends PointerLoadBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitReadIntAtLongOffset(this, result, arguments);
        }

        public static final ReadIntAtLongOffset BUILTIN = new ReadIntAtLongOffset();
    }

    public static class ReadIntAtIntOffset extends PointerLoadBuiltin {
        @BUILTIN(builtinClass = ReadIntAtIntOffset.class)
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

    public static class ReadFloatAtLongOffset extends PointerLoadBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitReadFloatAtLongOffset(this, result, arguments);
        }

        public static final ReadFloatAtLongOffset BUILTIN = new ReadFloatAtLongOffset();
    }

    public static class ReadFloatAtIntOffset extends PointerLoadBuiltin {
        @BUILTIN(builtinClass = ReadFloatAtIntOffset.class)
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

    public static class ReadLongAtLongOffset extends PointerLoadBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitReadLongAtLongOffset(this, result, arguments);
        }

        public static final ReadLongAtLongOffset BUILTIN = new ReadLongAtLongOffset();
    }

    public static class ReadLongAtIntOffset extends PointerLoadBuiltin {
        @BUILTIN(builtinClass = ReadLongAtIntOffset.class)
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

    public static class ReadDoubleAtLongOffset extends PointerLoadBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitReadDoubleAtLongOffset(this, result, arguments);
        }

        public static final ReadDoubleAtLongOffset BUILTIN = new ReadDoubleAtLongOffset();
    }

    public static class ReadDoubleAtIntOffset extends PointerLoadBuiltin {
        @BUILTIN(builtinClass = ReadDoubleAtIntOffset.class)
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

    public static class ReadWordAtLongOffset extends PointerLoadBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitReadWordAtLongOffset(this, result, arguments);
        }

        public static final ReadWordAtLongOffset BUILTIN = new ReadWordAtLongOffset();
    }

    public static class ReadWordAtIntOffset extends PointerLoadBuiltin {
        @BUILTIN(builtinClass = ReadWordAtIntOffset.class)
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

    public static class ReadReferenceAtLongOffset extends PointerLoadBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitReadReferenceAtLongOffset(this, result, arguments);
        }

        public static final ReadReferenceAtLongOffset BUILTIN = new ReadReferenceAtLongOffset();
    }

    public static class ReadReferenceAtIntOffset extends PointerLoadBuiltin {
        @BUILTIN(builtinClass = ReadReferenceAtIntOffset.class)
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

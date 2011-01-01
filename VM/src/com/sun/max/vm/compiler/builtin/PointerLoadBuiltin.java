/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.vm.compiler.builtin;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.object.*;

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
            final Hub hub = ObjectAccess.readHub(tuple);
            return hub.findFieldActor(offset).getByte(tuple);
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
            final Hub hub = ObjectAccess.readHub(tuple);
            return hub.findFieldActor(offset).getShort(tuple);
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
            final Hub hub = ObjectAccess.readHub(tuple);
            return hub.findFieldActor(offset).getChar(tuple);
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
            final Hub hub = ObjectAccess.readHub(tuple);
            return hub.findFieldActor(offset).getInt(tuple);
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
            final Hub hub = ObjectAccess.readHub(tuple);
            return hub.findFieldActor(offset).getFloat(tuple);
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
            final Hub hub = ObjectAccess.readHub(tuple);
            return hub.findFieldActor(offset).getLong(tuple);
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
            final Hub hub = ObjectAccess.readHub(tuple);
            return hub.findFieldActor(offset).getDouble(tuple);
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
            final Hub hub = ObjectAccess.readHub(tuple);
            return hub.findFieldActor(offset).getWord(tuple);
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
            final Hub hub = ObjectAccess.readHub(tuple);
            return hub.findFieldActor(offset).getObject(tuple);
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

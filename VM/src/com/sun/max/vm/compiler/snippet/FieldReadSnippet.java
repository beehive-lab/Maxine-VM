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
package com.sun.max.vm.compiler.snippet;

import static com.sun.max.vm.classfile.ErrorContext.*;

import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.type.*;

/**
 * TODO: must not constant fold when an object initializer is still ongoing.
 *
 * @author Bernd Mathiske
 */
public class FieldReadSnippet extends BuiltinsSnippet {

    private TupleOffsetSnippet tupleOffsetSnippet;

    protected FieldReadSnippet(TupleOffsetSnippet tupleOffsetSnippet) {
        this.tupleOffsetSnippet = tupleOffsetSnippet;
    }

    public TupleOffsetSnippet tupleOffsetSnippet() {
        return tupleOffsetSnippet;
    }

    public static final class ReadByte extends FieldReadSnippet {
        private ReadByte(TupleOffsetSnippet tupleOffsetSnippet) {
            super(tupleOffsetSnippet);
        }

        @SNIPPET
        @INLINE
        public static byte readByte(Object tuple, FieldActor byteFieldActor) {
            if (MaxineVM.isHosted()) {
                return byteFieldActor.getByte(tuple);
            }
            return TupleAccess.readByte(tuple, byteFieldActor.offset());
        }

        public static final ReadByte SNIPPET = new ReadByte(TupleOffsetSnippet.ReadByte.SNIPPET);
    }

    public static final class ReadBoolean extends FieldReadSnippet {
        private ReadBoolean(TupleOffsetSnippet tupleOffsetSnippet) {
            super(tupleOffsetSnippet);
        }

        @SNIPPET
        @INLINE
        public static boolean readBoolean(Object tuple, FieldActor booleanFieldActor) {
            if (MaxineVM.isHosted()) {
                return booleanFieldActor.getBoolean(tuple);
            }
            return TupleAccess.readBoolean(tuple, booleanFieldActor.offset());
        }

        public static final ReadBoolean SNIPPET = new ReadBoolean(TupleOffsetSnippet.ReadBoolean.SNIPPET);
    }

    public static final class ReadShort extends FieldReadSnippet {
        private ReadShort(TupleOffsetSnippet tupleOffsetSnippet) {
            super(tupleOffsetSnippet);
        }

        @SNIPPET
        @INLINE
        public static short readShort(Object tuple, FieldActor shortFieldActor) {
            if (MaxineVM.isHosted()) {
                return shortFieldActor.getShort(tuple);
            }
            return TupleAccess.readShort(tuple, shortFieldActor.offset());
        }

        public static final ReadShort SNIPPET = new ReadShort(TupleOffsetSnippet.ReadShort.SNIPPET);
    }

    public static final class ReadChar extends FieldReadSnippet {
        private ReadChar(TupleOffsetSnippet tupleOffsetSnippet) {
            super(tupleOffsetSnippet);
        }

        @SNIPPET
        @INLINE
        public static char readChar(Object tuple, FieldActor charFieldActor) {
            if (MaxineVM.isHosted()) {
                return charFieldActor.getChar(tuple);
            }
            return TupleAccess.readChar(tuple, charFieldActor.offset());
        }

        public static final ReadChar SNIPPET = new ReadChar(TupleOffsetSnippet.ReadChar.SNIPPET);
    }

    public static final class ReadInt extends FieldReadSnippet {
        private ReadInt(TupleOffsetSnippet tupleOffsetSnippet) {
            super(tupleOffsetSnippet);
        }

        @SNIPPET
        @INLINE
        public static int readInt(Object tuple, FieldActor intFieldActor) {
            if (MaxineVM.isHosted()) {
                return intFieldActor.getInt(tuple);
            }
            return TupleAccess.readInt(tuple, intFieldActor.offset());
        }

        public static final ReadInt SNIPPET = new ReadInt(TupleOffsetSnippet.ReadInt.SNIPPET);
    }

    public static final class ReadFloat extends FieldReadSnippet {
        private ReadFloat(TupleOffsetSnippet tupleOffsetSnippet) {
            super(tupleOffsetSnippet);
        }

        @SNIPPET
        @INLINE
        public static float readFloat(Object tuple, FieldActor floatFieldActor) {
            if (MaxineVM.isHosted()) {
                return floatFieldActor.getFloat(tuple);
            }
            return TupleAccess.readFloat(tuple, floatFieldActor.offset());
        }

        public static final ReadFloat SNIPPET = new ReadFloat(TupleOffsetSnippet.ReadFloat.SNIPPET);
    }

    public static final class ReadLong extends FieldReadSnippet {
        private ReadLong(TupleOffsetSnippet tupleOffsetSnippet) {
            super(tupleOffsetSnippet);
        }

        @SNIPPET
        @INLINE
        public static long readLong(Object tuple, FieldActor longFieldActor) {
            if (MaxineVM.isHosted()) {
                return longFieldActor.getLong(tuple);
            }
            return TupleAccess.readLong(tuple, longFieldActor.offset());
        }

        public static final ReadLong SNIPPET = new ReadLong(TupleOffsetSnippet.ReadLong.SNIPPET);
    }

    public static final class ReadDouble extends FieldReadSnippet {
        private ReadDouble(TupleOffsetSnippet tupleOffsetSnippet) {
            super(tupleOffsetSnippet);
        }

        @SNIPPET
        @INLINE
        public static double readDouble(Object tuple, FieldActor doubleFieldActor) {
            if (MaxineVM.isHosted()) {
                return doubleFieldActor.getDouble(tuple);
            }
            return TupleAccess.readDouble(tuple, doubleFieldActor.offset());
        }

        public static final ReadDouble SNIPPET = new ReadDouble(TupleOffsetSnippet.ReadDouble.SNIPPET);
    }

    public static final class ReadWord extends FieldReadSnippet {
        private ReadWord(TupleOffsetSnippet tupleOffsetSnippet) {
            super(tupleOffsetSnippet);
        }

        @SNIPPET
        @INLINE
        public static Word readWord(Object tuple, FieldActor wordFieldActor) {
            if (MaxineVM.isHosted()) {
                return wordFieldActor.getWord(tuple);
            }
            return TupleAccess.readWord(tuple, wordFieldActor.offset());
        }

        public static final ReadWord SNIPPET = new ReadWord(TupleOffsetSnippet.ReadWord.SNIPPET);
    }

    public static final class ReadReference extends FieldReadSnippet {
        private ReadReference(TupleOffsetSnippet tupleOffsetSnippet) {
            super(tupleOffsetSnippet);
        }

        @SNIPPET
        @INLINE
        public static Object readReference(Object tuple, FieldActor referenceFieldActor) {
            if (MaxineVM.isHosted()) {
                return referenceFieldActor.getObject(tuple);
            }
            return TupleAccess.readObject(tuple, referenceFieldActor.offset());
        }

        public static final ReadReference SNIPPET = new ReadReference(TupleOffsetSnippet.ReadReference.SNIPPET);
    }

    public static FieldReadSnippet selectSnippet(Kind kind) {
        switch (kind.asEnum) {
            case BYTE:
                return FieldReadSnippet.ReadByte.SNIPPET;
            case BOOLEAN:
                return FieldReadSnippet.ReadBoolean.SNIPPET;
            case SHORT:
                return FieldReadSnippet.ReadShort.SNIPPET;
            case CHAR:
                return FieldReadSnippet.ReadChar.SNIPPET;
            case INT:
                return FieldReadSnippet.ReadInt.SNIPPET;
            case FLOAT:
                return FieldReadSnippet.ReadFloat.SNIPPET;
            case LONG:
                return  FieldReadSnippet.ReadLong.SNIPPET;
            case DOUBLE:
                return FieldReadSnippet.ReadDouble.SNIPPET;
            case WORD:
                return FieldReadSnippet.ReadWord.SNIPPET;
            case REFERENCE:
                return FieldReadSnippet.ReadReference.SNIPPET;
            case VOID:
                throw classFormatError("Fields cannot have type void");
        }
        ProgramError.unexpected();
        return null;
    }
}

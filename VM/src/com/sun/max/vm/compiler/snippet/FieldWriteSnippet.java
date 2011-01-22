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
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.type.*;

public class FieldWriteSnippet extends BuiltinsSnippet {

    protected FieldWriteSnippet() {
        super();
    }

    public static final class WriteByte extends FieldWriteSnippet {
        @SNIPPET
        @INLINE
        public static void writeByte(Object tuple, FieldActor byteFieldActor, byte value) {
            if (MaxineVM.isHosted()) {
                byteFieldActor.setByte(tuple, value);
            } else {
                TupleAccess.writeByte(tuple, byteFieldActor.offset(), value);
            }
        }
        public static final WriteByte SNIPPET = new WriteByte();
    }

    public static final class WriteBoolean extends FieldWriteSnippet {
        @SNIPPET
        @INLINE
        public static void writeBoolean(Object tuple, FieldActor booleanFieldActor, boolean value) {
            if (MaxineVM.isHosted()) {
                booleanFieldActor.setBoolean(tuple, value);
            } else {
                TupleAccess.writeBoolean(tuple, booleanFieldActor.offset(), value);
            }
        }
        public static final WriteBoolean SNIPPET = new WriteBoolean();
    }

    public static final class WriteShort extends FieldWriteSnippet {
        @SNIPPET
        @INLINE
        public static void writeShort(Object tuple, FieldActor shortFieldActor, short value) {
            if (MaxineVM.isHosted()) {
                shortFieldActor.setShort(tuple, value);
            } else {
                TupleAccess.writeShort(tuple, shortFieldActor.offset(), value);
            }
        }
        public static final WriteShort SNIPPET = new WriteShort();
    }

    public static final class WriteChar extends FieldWriteSnippet {
        @SNIPPET
        @INLINE
        public static void writeChar(Object tuple, FieldActor charFieldActor, char value) {
            if (MaxineVM.isHosted()) {
                charFieldActor.setChar(tuple, value);
            } else {
                TupleAccess.writeChar(tuple, charFieldActor.offset(), value);
            }
        }
        public static final WriteChar SNIPPET = new WriteChar();
    }

    public static final class WriteInt extends FieldWriteSnippet {
        @SNIPPET
        @INLINE
        public static void writeInt(Object tuple, FieldActor intFieldActor, int value) {
            if (MaxineVM.isHosted()) {
                intFieldActor.setInt(tuple, value);
            } else {
                TupleAccess.writeInt(tuple, intFieldActor.offset(), value);
            }
        }
        public static final WriteInt SNIPPET = new WriteInt();
    }

    public static final class WriteFloat extends FieldWriteSnippet {
        @SNIPPET
        @INLINE
        public static void writeFloat(Object tuple, FieldActor floatFieldActor, float value) {
            if (MaxineVM.isHosted()) {
                floatFieldActor.setFloat(tuple, value);
            } else {
                TupleAccess.writeFloat(tuple, floatFieldActor.offset(), value);
            }
        }
        public static final WriteFloat SNIPPET = new WriteFloat();
    }

    public static final class WriteLong extends FieldWriteSnippet {
        @SNIPPET
        @INLINE
        public static void writeLong(Object tuple, FieldActor longFieldActor, long value) {
            if (MaxineVM.isHosted()) {
                longFieldActor.setLong(tuple, value);
            } else {
                TupleAccess.writeLong(tuple, longFieldActor.offset(), value);
            }
        }
        public static final WriteLong SNIPPET = new WriteLong();
    }

    public static final class WriteDouble extends FieldWriteSnippet {
        @SNIPPET
        @INLINE
        public static void writeDouble(Object tuple, FieldActor doubleFieldActor, double value) {
            if (MaxineVM.isHosted()) {
                doubleFieldActor.setDouble(tuple, value);
            } else {
                TupleAccess.writeDouble(tuple, doubleFieldActor.offset(), value);
            }
        }
        public static final WriteDouble SNIPPET = new WriteDouble();
    }

    public static final class WriteWord extends FieldWriteSnippet {
        @SNIPPET
        @INLINE
        public static void writeWord(Object tuple, FieldActor wordFieldActor, Word value) {
            if (MaxineVM.isHosted()) {
                wordFieldActor.setWord(tuple, value);
            } else {
                TupleAccess.writeWord(tuple, wordFieldActor.offset(), value);
            }
        }
        public static final WriteWord SNIPPET = new WriteWord();
    }

    public static final class WriteReference extends FieldWriteSnippet {
        @SNIPPET
        @INLINE
        public static void writeReference(Object tuple, FieldActor referenceFieldActor, Object value) {
            if (MaxineVM.isHosted()) {
                referenceFieldActor.setObject(tuple, value);
            } else {
                TupleAccess.writeObject(tuple, referenceFieldActor.offset(), value);
            }
        }

        @NEVER_INLINE
        public static void noninlineWriteReference(Object tuple, FieldActor referenceFieldActor, Object value) {
            writeReference(tuple, referenceFieldActor, value);
        }

        public static final WriteReference SNIPPET = new WriteReference();
    }

    public static final FieldWriteSnippet selectSnippetByKind(Kind kind) {
        switch (kind.asEnum) {
            case BYTE:
                return FieldWriteSnippet.WriteByte.SNIPPET;
            case BOOLEAN:
                return FieldWriteSnippet.WriteBoolean.SNIPPET;
            case SHORT:
                return FieldWriteSnippet.WriteShort.SNIPPET;
            case CHAR:
                return FieldWriteSnippet.WriteChar.SNIPPET;
            case INT:
                return FieldWriteSnippet.WriteInt.SNIPPET;
            case FLOAT:
                return FieldWriteSnippet.WriteFloat.SNIPPET;
            case LONG:
                return FieldWriteSnippet.WriteLong.SNIPPET;
            case DOUBLE:
                return FieldWriteSnippet.WriteDouble.SNIPPET;
            case WORD:
                return FieldWriteSnippet.WriteWord.SNIPPET;
            case REFERENCE:
                return FieldWriteSnippet.WriteReference.SNIPPET;
            default:
                throw classFormatError("Fields cannot have type void");
        }
    }
}

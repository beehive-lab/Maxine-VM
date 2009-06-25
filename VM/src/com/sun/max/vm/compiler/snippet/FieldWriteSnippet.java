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
package com.sun.max.vm.compiler.snippet;

import static com.sun.max.vm.classfile.ErrorContext.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.object.host.*;
import com.sun.max.vm.object.TupleAccess;
import com.sun.max.vm.type.*;


public class FieldWriteSnippet extends BuiltinsSnippet {

    protected FieldWriteSnippet() {
        super();
    }

    @Override
    public boolean isFoldable(IrValue[] arguments) {
        return false;
    }

    public static final class WriteByte extends FieldWriteSnippet {
        @SNIPPET
        @INLINE
        public static void writeByte(Object tuple, FieldActor byteFieldActor, byte value) {
            if (MaxineVM.isPrototyping()) {
                HostTupleAccess.writeByte(tuple, byteFieldActor, value);
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
            if (MaxineVM.isPrototyping()) {
                HostTupleAccess.writeBoolean(tuple, booleanFieldActor, value);
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
            if (MaxineVM.isPrototyping()) {
                HostTupleAccess.writeShort(tuple, shortFieldActor, value);
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
            if (MaxineVM.isPrototyping()) {
                HostTupleAccess.writeChar(tuple, charFieldActor, value);
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
            if (MaxineVM.isPrototyping()) {
                HostTupleAccess.writeInt(tuple, intFieldActor, value);
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
            if (MaxineVM.isPrototyping()) {
                HostTupleAccess.writeFloat(tuple, floatFieldActor, value);
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
            if (MaxineVM.isPrototyping()) {
                HostTupleAccess.writeLong(tuple, longFieldActor, value);
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
            if (MaxineVM.isPrototyping()) {
                HostTupleAccess.writeDouble(tuple, doubleFieldActor, value);
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
            if (MaxineVM.isPrototyping()) {
                HostTupleAccess.writeWord(tuple, wordFieldActor, value);
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
            if (MaxineVM.isPrototyping()) {
                HostTupleAccess.writeObject(tuple, referenceFieldActor, value);
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

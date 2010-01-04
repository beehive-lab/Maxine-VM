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
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.object.host.*;
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
                return HostTupleAccess.readByte(tuple, byteFieldActor);
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
                return HostTupleAccess.readBoolean(tuple, booleanFieldActor);
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
                return HostTupleAccess.readShort(tuple, shortFieldActor);
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
                return HostTupleAccess.readChar(tuple, charFieldActor);
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
                return HostTupleAccess.readInt(tuple, intFieldActor);
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
                return HostTupleAccess.readFloat(tuple, floatFieldActor);
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
                return HostTupleAccess.readLong(tuple, longFieldActor);
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
                return HostTupleAccess.readDouble(tuple, doubleFieldActor);
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
                return HostTupleAccess.readWord(tuple, wordFieldActor);
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
                return HostTupleAccess.readObject(tuple, referenceFieldActor);
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

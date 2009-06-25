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

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.object.host.*;

/**
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public class TupleOffsetSnippet extends BuiltinsSnippet {

    protected TupleOffsetSnippet() {
        super();
    }

    @Override
    public boolean isFoldable(IrValue[] arguments) {
        return false;
    }

    /**
     * Gets the Java field corresponding to an offset in a given tuple.
     *
     * @param tuple   a {@link TupleNode} instance holding the static fields of a class or a normal object
     * @param offset  the field's offset
     */
    @PROTOTYPE_ONLY
    static FieldActor findFieldActor(Object tuple, int offset) {
        return HostObjectAccess.readHub(tuple).findFieldActor(offset);
    }

    public static final class ReadByte extends TupleOffsetSnippet {
        @SNIPPET
        @INLINE
        public static byte readByte(Object tuple, int offset) {
            if (MaxineVM.isPrototyping()) {
                return HostTupleAccess.readByte(tuple, findFieldActor(tuple, offset));
            }
            return TupleAccess.readByte(tuple, offset);
        }
        public static final ReadByte SNIPPET = new ReadByte();
    }

    public static final class ReadBoolean extends TupleOffsetSnippet {
        @SNIPPET
        @INLINE
        public static boolean readBoolean(Object tuple, int offset) {
            if (MaxineVM.isPrototyping()) {
                return HostTupleAccess.readBoolean(tuple, findFieldActor(tuple, offset));
            }
            return TupleAccess.readBoolean(tuple, offset);
        }
        public static final ReadBoolean SNIPPET = new ReadBoolean();
    }

    public static final class ReadShort extends TupleOffsetSnippet {
        @SNIPPET
        @INLINE
        public static short readShort(Object tuple, int offset) {
            if (MaxineVM.isPrototyping()) {
                return HostTupleAccess.readShort(tuple, findFieldActor(tuple, offset));
            }
            return TupleAccess.readShort(tuple, offset);
        }

        public static final ReadShort SNIPPET = new ReadShort();
    }

    public static final class ReadChar extends TupleOffsetSnippet {
        @SNIPPET
        @INLINE
        public static char readChar(Object tuple, int offset) {
            if (MaxineVM.isPrototyping()) {
                return HostTupleAccess.readChar(tuple, findFieldActor(tuple, offset));
            }
            return TupleAccess.readChar(tuple, offset);
        }

        public static final ReadChar SNIPPET = new ReadChar();
    }

    public static final class ReadInt extends TupleOffsetSnippet {
        @SNIPPET
        @INLINE
        public static int readInt(Object tuple, int offset) {
            if (MaxineVM.isPrototyping()) {
                return HostTupleAccess.readInt(tuple, findFieldActor(tuple, offset));
            }
            return TupleAccess.readInt(tuple, offset);
        }

        public static final ReadInt SNIPPET = new ReadInt();
    }

    public static final class ReadFloat extends TupleOffsetSnippet {
        @SNIPPET
        @INLINE
        public static float readFloat(Object tuple, int offset) {
            if (MaxineVM.isPrototyping()) {
                return HostTupleAccess.readFloat(tuple, findFieldActor(tuple, offset));
            }
            return TupleAccess.readFloat(tuple, offset);
        }

        public static final ReadFloat SNIPPET = new ReadFloat();
    }

    public static final class ReadLong extends TupleOffsetSnippet {
        @SNIPPET
        @INLINE
        public static long readLong(Object tuple, int offset) {
            if (MaxineVM.isPrototyping()) {
                return HostTupleAccess.readLong(tuple, findFieldActor(tuple, offset));
            }
            return TupleAccess.readLong(tuple, offset);
        }

        public static final ReadLong SNIPPET = new ReadLong();
    }

    public static final class ReadDouble extends TupleOffsetSnippet {
        @SNIPPET
        @INLINE
        public static double readDouble(Object tuple, int offset) {
            if (MaxineVM.isPrototyping()) {
                return HostTupleAccess.readDouble(tuple, findFieldActor(tuple, offset));
            }
            return TupleAccess.readDouble(tuple, offset);
        }

        public static final ReadDouble SNIPPET = new ReadDouble();
    }

    public static final class ReadWord extends TupleOffsetSnippet {
        @SNIPPET
        @INLINE
        public static Word readWord(Object tuple, int offset) {
            if (MaxineVM.isPrototyping()) {
                return HostTupleAccess.readWord(tuple, findFieldActor(tuple, offset));
            }
            return TupleAccess.readWord(tuple, offset);
        }

        public static final ReadWord SNIPPET = new ReadWord();
    }

    public static final class ReadReference extends TupleOffsetSnippet {
        @SNIPPET
        @INLINE
        public static Object readReference(Object tuple, int offset) {
            if (MaxineVM.isPrototyping()) {
                return HostTupleAccess.readObject(tuple, (ReferenceFieldActor) findFieldActor(tuple, offset));
            }
            return TupleAccess.readObject(tuple, offset);
        }

        public static final ReadReference SNIPPET = new ReadReference();
    }
}

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

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.object.*;

/**
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public class TupleOffsetSnippet extends BuiltinsSnippet {

    protected TupleOffsetSnippet() {
        super();
    }

    /**
     * Gets the Java field corresponding to an offset in a given tuple.
     *
     * @param tuple   a {@link TupleNode} instance holding the static fields of a class or a normal object
     * @param offset  the field's offset
     */
    @HOSTED_ONLY
    static FieldActor findFieldActor(Object tuple, int offset) {
        return ObjectAccess.readHub(tuple).findFieldActor(offset);
    }

    public static final class ReadByte extends TupleOffsetSnippet {
        @SNIPPET
        @INLINE
        public static byte readByte(Object tuple, int offset) {
            if (MaxineVM.isHosted()) {
                return findFieldActor(tuple, offset).getByte(tuple);
            }
            return TupleAccess.readByte(tuple, offset);
        }
        public static final ReadByte SNIPPET = new ReadByte();
    }

    public static final class ReadBoolean extends TupleOffsetSnippet {
        @SNIPPET
        @INLINE
        public static boolean readBoolean(Object tuple, int offset) {
            if (MaxineVM.isHosted()) {
                return findFieldActor(tuple, offset).getBoolean(tuple);
            }
            return TupleAccess.readBoolean(tuple, offset);
        }
        public static final ReadBoolean SNIPPET = new ReadBoolean();
    }

    public static final class ReadShort extends TupleOffsetSnippet {
        @SNIPPET
        @INLINE
        public static short readShort(Object tuple, int offset) {
            if (MaxineVM.isHosted()) {
                return findFieldActor(tuple, offset).getShort(tuple);
            }
            return TupleAccess.readShort(tuple, offset);
        }

        public static final ReadShort SNIPPET = new ReadShort();
    }

    public static final class ReadChar extends TupleOffsetSnippet {
        @SNIPPET
        @INLINE
        public static char readChar(Object tuple, int offset) {
            if (MaxineVM.isHosted()) {
                return findFieldActor(tuple, offset).getChar(tuple);
            }
            return TupleAccess.readChar(tuple, offset);
        }

        public static final ReadChar SNIPPET = new ReadChar();
    }

    public static final class ReadInt extends TupleOffsetSnippet {
        @SNIPPET
        @INLINE
        public static int readInt(Object tuple, int offset) {
            if (MaxineVM.isHosted()) {
                return findFieldActor(tuple, offset).getInt(tuple);
            }
            return TupleAccess.readInt(tuple, offset);
        }

        public static final ReadInt SNIPPET = new ReadInt();
    }

    public static final class ReadFloat extends TupleOffsetSnippet {
        @SNIPPET
        @INLINE
        public static float readFloat(Object tuple, int offset) {
            if (MaxineVM.isHosted()) {
                return findFieldActor(tuple, offset).getFloat(tuple);
            }
            return TupleAccess.readFloat(tuple, offset);
        }

        public static final ReadFloat SNIPPET = new ReadFloat();
    }

    public static final class ReadLong extends TupleOffsetSnippet {
        @SNIPPET
        @INLINE
        public static long readLong(Object tuple, int offset) {
            if (MaxineVM.isHosted()) {
                return findFieldActor(tuple, offset).getLong(tuple);
            }
            return TupleAccess.readLong(tuple, offset);
        }

        public static final ReadLong SNIPPET = new ReadLong();
    }

    public static final class ReadDouble extends TupleOffsetSnippet {
        @SNIPPET
        @INLINE
        public static double readDouble(Object tuple, int offset) {
            if (MaxineVM.isHosted()) {
                return findFieldActor(tuple, offset).getDouble(tuple);
            }
            return TupleAccess.readDouble(tuple, offset);
        }

        public static final ReadDouble SNIPPET = new ReadDouble();
    }

    public static final class ReadWord extends TupleOffsetSnippet {
        @SNIPPET
        @INLINE
        public static Word readWord(Object tuple, int offset) {
            if (MaxineVM.isHosted()) {
                return findFieldActor(tuple, offset).getWord(tuple);
            }
            return TupleAccess.readWord(tuple, offset);
        }

        public static final ReadWord SNIPPET = new ReadWord();
    }

    public static final class ReadReference extends TupleOffsetSnippet {
        @SNIPPET
        @INLINE
        public static Object readReference(Object tuple, int offset) {
            if (MaxineVM.isHosted()) {
                return findFieldActor(tuple, offset).getObject(tuple);
            }
            return TupleAccess.readObject(tuple, offset);
        }

        public static final ReadReference SNIPPET = new ReadReference();
    }
}

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
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.type.*;

public abstract class ArrayGetSnippet extends BuiltinsSnippet {

    private ArrayGetSnippet() {
        super();
    }

    public static final class ReadLength extends ArrayGetSnippet {
        @SNIPPET
        @INLINE
        public static int readLength(Object array) {
            if (MaxineVM.isHosted()) {
                return ArrayAccess.readArrayLength(array);
            }
            return ArrayAccess.readArrayLength(array);
        }

        public static final ReadLength SNIPPET = new ReadLength();
    }

    public static final class GetByte extends ArrayGetSnippet {
        @SNIPPET
        @INLINE
        public static byte getByte(Object array, int index) {
            return ArrayAccess.getByte(array, index);
        }

        public static final GetByte SNIPPET = new GetByte();
    }

    public static final class GetShort extends ArrayGetSnippet {
        @SNIPPET
        @INLINE
        public static short getShort(short[] array, int index) {
            if (MaxineVM.isHosted()) {
                return array[index];
            }
            return ArrayAccess.getShort(array, index);
        }

        public static final GetShort SNIPPET = new GetShort();
    }

    public static final class GetChar extends ArrayGetSnippet {
        @SNIPPET
        @INLINE
        public static char getChar(char[] array, int index) {
            if (MaxineVM.isHosted()) {
                return array[index];
            }
            return ArrayAccess.getChar(array, index);
        }

        public static final GetChar SNIPPET = new GetChar();
    }

    public static final class GetInt extends ArrayGetSnippet {
        @SNIPPET
        @INLINE
        public static int getInt(int[] array, int index) {
            if (MaxineVM.isHosted()) {
                return array[index];
            }
            return ArrayAccess.getInt(array, index);
        }

        public static final GetInt SNIPPET = new GetInt();
    }

    public static final class GetFloat extends ArrayGetSnippet {
        @SNIPPET
        @INLINE
        public static float getFloat(float[] array, int index) {
            if (MaxineVM.isHosted()) {
                return array[index];
            }
            return ArrayAccess.getFloat(array, index);
        }

        public static final GetFloat SNIPPET = new GetFloat();
    }

    public static final class GetLong extends ArrayGetSnippet {
        @SNIPPET
        @INLINE
        public static long getLong(long[] array, int index) {
            if (MaxineVM.isHosted()) {
                return array[index];
            }
            return ArrayAccess.getLong(array, index);
        }

        public static final GetLong SNIPPET = new GetLong();
    }

    public static final class GetDouble extends ArrayGetSnippet {
        @SNIPPET
        @INLINE
        public static double getDouble(double[] array, int index) {
            if (MaxineVM.isHosted()) {
                return array[index];
            }
            return ArrayAccess.getDouble(array, index);
        }

        public static final GetDouble SNIPPET = new GetDouble();
    }

    public static final class GetReference extends ArrayGetSnippet {
        @SNIPPET
        @INLINE
        public static Object getReference(Object[] array, int index) {
            if (MaxineVM.isHosted()) {
                return array[index];
            }
            return ArrayAccess.getObject(array, index);
        }

        public static final GetReference SNIPPET = new GetReference();
    }

    public static ArrayGetSnippet getSnippet(Kind kind) {
        switch (kind.asEnum) {
            case BYTE:
                return GetByte.SNIPPET;
            case CHAR:
                return GetChar.SNIPPET;
            case DOUBLE:
                return GetDouble.SNIPPET;
            case FLOAT:
                return GetFloat.SNIPPET;
            case INT:
                return GetInt.SNIPPET;
            case LONG:
                return GetLong.SNIPPET;
            case REFERENCE:
                return GetReference.SNIPPET;
            case SHORT:
                return GetShort.SNIPPET;
            default:
                ProgramError.unexpected("no array load snippet for kind " + kind);
                return null;
        }
    }
}

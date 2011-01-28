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
import com.sun.max.vm.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.type.*;

public abstract class ArraySetSnippet extends BuiltinsSnippet {

    private ArraySetSnippet() {
        super();
    }

    public static final class SetByte extends ArraySetSnippet {
        // We use 'int' for the 'value' parameter, because only thus it acts as either 'byte' and 'boolean'
        @SNIPPET
        @INLINE
        public static void setByte(Object array, int index, int value) {
            ArrayAccess.setByte(array, index, (byte) value);
        }
        public static final SetByte SNIPPET = new SetByte();
    }

    public static final class SetShort extends ArraySetSnippet {
        @SNIPPET
        @INLINE
        public static void setShort(short[] array, int index, short value) {
            if (MaxineVM.isHosted()) {
                array[index] = value;
            } else {
                ArrayAccess.setShort(array, index, value);
            }
        }
        public static final SetShort SNIPPET = new SetShort();
    }

    public static final class SetChar extends ArraySetSnippet {
        @SNIPPET
        @INLINE
        public static void setChar(char[] array, int index, char value) {
            if (MaxineVM.isHosted()) {
                array[index] = value;
            } else {
                ArrayAccess.setChar(array, index, value);
            }
        }
        public static final SetChar SNIPPET = new SetChar();
    }

    public static final class SetInt extends ArraySetSnippet {
        @SNIPPET
        @INLINE
        public static void setInt(int[] array, int index, int value) {
            if (MaxineVM.isHosted()) {
                array[index] = value;
            } else {
                ArrayAccess.setInt(array, index, value);
            }
        }
        public static final SetInt SNIPPET = new SetInt();
    }

    public static final class SetFloat extends ArraySetSnippet {
        @SNIPPET
        @INLINE
        public static void setFloat(float[] array, int index, float value) {
            if (MaxineVM.isHosted()) {
                array[index] = value;
            } else {
                ArrayAccess.setFloat(array, index, value);
            }
        }
        public static final SetFloat SNIPPET = new SetFloat();
    }

    public static final class SetLong extends ArraySetSnippet {
        @SNIPPET
        @INLINE
        public static void setLong(long[] array, int index, long value) {
            if (MaxineVM.isHosted()) {
                array[index] = value;
            } else {
                ArrayAccess.setLong(array, index, value);
            }
        }
        public static final SetLong SNIPPET = new SetLong();
    }

    public static final class SetDouble extends ArraySetSnippet {
        @SNIPPET
        @INLINE
        public static void setDouble(double[] array, int index, double value) {
            if (MaxineVM.isHosted()) {
                array[index] = value;
            } else {
                ArrayAccess.setDouble(array, index, value);
            }
        }
        public static final SetDouble SNIPPET = new SetDouble();
    }

    public static final class SetReference extends ArraySetSnippet {
        @SNIPPET
        @INLINE
        public static void setReference(Object[] array, int index, Object value) {
            if (MaxineVM.isHosted()) {
                array[index] = value;
            } else {
                ArrayAccess.setObject(array, index, value);
            }
        }
        public static final SetReference SNIPPET = new SetReference();
    }

    public static ArraySetSnippet selectSnippetByKind(Kind kind) {
        switch (kind.asEnum) {
            case BYTE:
                return ArraySetSnippet.SetByte.SNIPPET;
            case SHORT:
                return ArraySetSnippet.SetShort.SNIPPET;
            case CHAR:
                return ArraySetSnippet.SetChar.SNIPPET;
            case INT:
                return ArraySetSnippet.SetInt.SNIPPET;
            case FLOAT:
                return ArraySetSnippet.SetFloat.SNIPPET;
            case LONG:
                return  ArraySetSnippet.SetLong.SNIPPET;
            case DOUBLE:
                return ArraySetSnippet.SetDouble.SNIPPET;
            case REFERENCE:
                return ArraySetSnippet.SetReference.SNIPPET;
            default:
                throw classFormatError("elements cannot have type " + kind);
        }
    }
}


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
import com.sun.max.vm.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.object.host.*;
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
            if (MaxineVM.isHosted()) {
                HostArrayAccess.setByte(array, index, (byte) value);
            } else {
                ArrayAccess.setByte(array, index, (byte) value);
            }
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


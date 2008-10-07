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
/*VCSID=cd119568-7428-41a0-9aed-50a2cf53a44f*/
package com.sun.max.vm.compiler.snippet;

import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.object.host.*;
import com.sun.max.vm.type.*;

public abstract class ArrayGetSnippet extends BuiltinsSnippet {

    private ArrayGetSnippet() {
        super();
    }

    public static final class ReadLength extends ArrayGetSnippet {
        @SNIPPET
        @INLINE
        public static int readLength(Object array) {
            if (MaxineVM.isPrototyping()) {
                return HostObjectAccess.getArrayLength(array);
            }
            return ArrayAccess.readArrayLength(array);
        }

        public static final ReadLength SNIPPET = new ReadLength();
    }

    public static final class GetByte extends ArrayGetSnippet {
        @SNIPPET
        @INLINE
        public static byte getByte(Object array, int index) {
            if (MaxineVM.isPrototyping()) {
                return HostArrayAccess.getByte(array, index);
            }
            return ArrayAccess.getByte(array, index);
        }

        public static final GetByte SNIPPET = new GetByte();
    }

    public static final class GetShort extends ArrayGetSnippet {
        @SNIPPET
        @INLINE
        public static short getShort(short[] array, int index) {
            if (MaxineVM.isPrototyping()) {
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
            if (MaxineVM.isPrototyping()) {
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
            if (MaxineVM.isPrototyping()) {
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
            if (MaxineVM.isPrototyping()) {
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
            if (MaxineVM.isPrototyping()) {
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
            if (MaxineVM.isPrototyping()) {
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
            if (MaxineVM.isPrototyping()) {
                return array[index];
            }
            return ArrayAccess.getObject(array, index);
        }

        public static final GetReference SNIPPET = new GetReference();
    }

    public static ArrayGetSnippet getSnippet(Kind kind) {
        switch (kind.asEnum()) {
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

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
/*VCSID=75f949f5-258a-4f04-a4a6-025d99f14454*/
package com.sun.max.vm.compiler.builtin;

import com.sun.max.vm.type.*;

public abstract class PointerStoreBuiltin extends PointerBuiltin {

    private final Kind _kind;

    public Kind kind() {
        return _kind;
    }

    private PointerStoreBuiltin() {
        final Kind[] parameterKinds = foldingMethodActor().descriptor().getParameterKinds();
        _kind = parameterKinds[parameterKinds.length - 1];
    }

    public static class WriteByteAtLongOffset extends PointerStoreBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 3;
            visitor.visitWriteByteAtLongOffset(this, result, arguments);
        }

        public static final WriteByteAtLongOffset BUILTIN = new WriteByteAtLongOffset();
    }

    public static class WriteByteAtIntOffset extends PointerStoreBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 3;
            visitor.visitWriteByteAtIntOffset(this, result, arguments);
        }

        public static final WriteByteAtIntOffset BUILTIN = new WriteByteAtIntOffset();
    }

    public static class SetByte extends PointerStoreBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 4;
            visitor.visitSetByte(this, result, arguments);
        }

        public static final SetByte BUILTIN = new SetByte();
    }

    public static class WriteShortAtLongOffset extends PointerStoreBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 3;
            visitor.visitWriteShortAtLongOffset(this, result, arguments);
        }

        public static final WriteShortAtLongOffset BUILTIN = new WriteShortAtLongOffset();
    }

    public static class WriteShortAtIntOffset extends PointerStoreBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 3;
            visitor.visitWriteShortAtIntOffset(this, result, arguments);
        }

        public static final WriteShortAtIntOffset BUILTIN = new WriteShortAtIntOffset();
    }

    public static class SetShort extends PointerStoreBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 4;
            visitor.visitSetShort(this, result, arguments);
        }

        public static final SetShort BUILTIN = new SetShort();
    }

    public static class WriteIntAtLongOffset extends PointerStoreBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 3;
            visitor.visitWriteIntAtLongOffset(this, result, arguments);
        }

        public static final WriteIntAtLongOffset BUILTIN = new WriteIntAtLongOffset();
    }

    public static class WriteIntAtIntOffset extends PointerStoreBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 3;
            visitor.visitWriteIntAtIntOffset(this, result, arguments);
        }

        public static final WriteIntAtIntOffset BUILTIN = new WriteIntAtIntOffset();
    }

    public static class SetInt extends PointerStoreBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 4;
            visitor.visitSetInt(this, result, arguments);
        }

        public static final SetInt BUILTIN = new SetInt();
    }

    public static class WriteFloatAtLongOffset extends PointerStoreBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 3;
            visitor.visitWriteFloatAtLongOffset(this, result, arguments);
        }

        public static final WriteFloatAtLongOffset BUILTIN = new WriteFloatAtLongOffset();
    }

    public static class WriteFloatAtIntOffset extends PointerStoreBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 3;
            visitor.visitWriteFloatAtIntOffset(this, result, arguments);
        }

        public static final WriteFloatAtIntOffset BUILTIN = new WriteFloatAtIntOffset();
    }

    public static class SetFloat extends PointerStoreBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 4;
            visitor.visitSetFloat(this, result, arguments);
        }

        public static final SetFloat BUILTIN = new SetFloat();
    }

    public static class WriteLongAtLongOffset extends PointerStoreBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 3;
            visitor.visitWriteLongAtLongOffset(this, result, arguments);
        }

        public static final WriteLongAtLongOffset BUILTIN = new WriteLongAtLongOffset();
    }

    public static class WriteLongAtIntOffset extends PointerStoreBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 3;
            visitor.visitWriteLongAtIntOffset(this, result, arguments);
        }

        public static final WriteLongAtIntOffset BUILTIN = new WriteLongAtIntOffset();
    }

    public static class SetLong extends PointerStoreBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 4;
            visitor.visitSetLong(this, result, arguments);
        }

        public static final SetLong BUILTIN = new SetLong();
    }

    public static class WriteDoubleAtLongOffset extends PointerStoreBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 3;
            visitor.visitWriteDoubleAtLongOffset(this, result, arguments);
        }

        public static final WriteDoubleAtLongOffset BUILTIN = new WriteDoubleAtLongOffset();
    }

    public static class WriteDoubleAtIntOffset extends PointerStoreBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 3;
            visitor.visitWriteDoubleAtIntOffset(this, result, arguments);
        }

        public static final WriteDoubleAtIntOffset BUILTIN = new WriteDoubleAtIntOffset();
    }

    public static class SetDouble extends PointerStoreBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 4;
            visitor.visitSetDouble(this, result, arguments);
        }

        public static final SetDouble BUILTIN = new SetDouble();
    }

    public static class WriteWordAtLongOffset extends PointerStoreBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 3;
            visitor.visitWriteWordAtLongOffset(this, result, arguments);
        }

        public static final WriteWordAtLongOffset BUILTIN = new WriteWordAtLongOffset();
    }

    public static class WriteWordAtIntOffset extends PointerStoreBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 3;
            visitor.visitWriteWordAtIntOffset(this, result, arguments);
        }

        public static final WriteWordAtIntOffset BUILTIN = new WriteWordAtIntOffset();
    }

    public static class SetWord extends PointerStoreBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 4;
            visitor.visitSetWord(this, result, arguments);
        }

        public static final SetWord BUILTIN = new SetWord();
    }

    public static class WriteReferenceAtLongOffset extends PointerStoreBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 3;
            visitor.visitWriteReferenceAtLongOffset(this, result, arguments);
        }

        public static final WriteReferenceAtLongOffset BUILTIN = new WriteReferenceAtLongOffset();
    }

    public static class WriteReferenceAtIntOffset extends PointerStoreBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 3;
            visitor.visitWriteReferenceAtIntOffset(this, result, arguments);
        }

        public static final WriteReferenceAtIntOffset BUILTIN = new WriteReferenceAtIntOffset();
    }

    public static class SetReference extends PointerStoreBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 4;
            visitor.visitSetReference(this, result, arguments);
        }

        public static final SetReference BUILTIN = new SetReference();
    }
}

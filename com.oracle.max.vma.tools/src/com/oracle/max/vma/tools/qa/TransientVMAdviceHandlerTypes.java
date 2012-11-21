/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vma.tools.qa;

import java.util.*;

import com.oracle.max.vm.ext.vma.*;

/**
 * Definitions of the types used by {@link TransientVMAdviceHandler} to record advice events.
 *
 * The number of actual types is minimized to facilitate event storage management.
 *
 * Each event carries the ordinal value of the associated {@link #RecordType}, which is encoded as the low-order byte of
 * a word that also carries the before/after code and may also carry a scalar value, e.g. an array index.
 *
 */
public class TransientVMAdviceHandlerTypes {
    public enum RecordType {
        Unseen,
        Removal,

// START GENERATED CODE
// EDIT AND RUN TransientVMAdviceHandlerTypesGenerator.main() TO MODIFY

        ArrayLength,
        ArrayLoad,
        ArrayLoadObject,
        ArrayStoreDouble,
        ArrayStoreFloat,
        ArrayStoreLong,
        ArrayStoreObject,
        CheckCast,
        ConstLoadDouble,
        ConstLoadFloat,
        ConstLoadLong,
        ConstLoadObject,
        ConversionDouble,
        ConversionFloat,
        ConversionLong,
        GC,
        GetField,
        GetStatic,
        Goto,
        IfInt,
        IfObject,
        InstanceOf,
        InvokeInterface,
        InvokeSpecial,
        InvokeStatic,
        InvokeVirtual,
        Load,
        LoadObject,
        MethodEntry,
        MonitorEnter,
        MonitorExit,
        MultiNewArray,
        New,
        NewArray,
        OperationDouble,
        OperationFloat,
        OperationLong,
        PutFieldDouble,
        PutFieldFloat,
        PutFieldLong,
        PutFieldObject,
        PutStaticDouble,
        PutStaticFloat,
        PutStaticLong,
        PutStaticObject,
        Return,
        ReturnByThrow,
        ReturnDouble,
        ReturnFloat,
        ReturnLong,
        ReturnObject,
        StackAdjust,
        StoreDouble,
        StoreFloat,
        StoreLong,
        StoreObject,
        ThreadStarting,
        ThreadTerminating,
        Throw;

        public AdviceRecord newAdviceRecord() {
            switch (this) {
                case ConstLoadLong:
                case ConversionLong:
                case ReturnLong:
                case StoreLong:
                    return new LongAdviceRecord();
                case GC:
                case Goto:
                case Load:
                case Return:
                case StackAdjust:
                case ThreadStarting:
                case ThreadTerminating:
                    return new AdviceRecord();
                case IfObject:
                    return new ObjectObjectTBciAdviceRecord();
                case OperationLong:
                    return new LongLongAdviceRecord();
                case ArrayStoreFloat:
                case PutFieldFloat:
                case PutStaticFloat:
                    return new ObjectFloatAdviceRecord();
                case ArrayLoadObject:
                case ArrayStoreObject:
                case CheckCast:
                case InstanceOf:
                case MultiNewArray:
                case PutFieldObject:
                case PutStaticObject:
                    return new ObjectObjectAdviceRecord();
                case ConstLoadDouble:
                case ConversionDouble:
                case ReturnDouble:
                case StoreDouble:
                    return new DoubleAdviceRecord();
                case ArrayLength:
                case ArrayLoad:
                case ConstLoadObject:
                case GetField:
                case GetStatic:
                case LoadObject:
                case MonitorEnter:
                case MonitorExit:
                case New:
                case NewArray:
                case ReturnObject:
                case StoreObject:
                case Throw:
                    return new ObjectAdviceRecord();
                case OperationDouble:
                    return new DoubleDoubleAdviceRecord();
                case ConstLoadFloat:
                case ConversionFloat:
                case ReturnFloat:
                case StoreFloat:
                    return new FloatAdviceRecord();
                case InvokeInterface:
                case InvokeSpecial:
                case InvokeStatic:
                case InvokeVirtual:
                case MethodEntry:
                    return new ObjectMethodAdviceRecord();
                case OperationFloat:
                    return new FloatFloatAdviceRecord();
                case ArrayStoreDouble:
                case PutFieldDouble:
                case PutStaticDouble:
                    return new ObjectDoubleAdviceRecord();
                case ArrayStoreLong:
                case PutFieldLong:
                case PutStaticLong:
                case ReturnByThrow:
                    return new ObjectLongAdviceRecord();
                case IfInt:
                    return new LongLongTBciAdviceRecord();
// END GENERATED CODE

                case Unseen:
                    return new ObjectAdviceRecord();
                case Removal:
                    return new LongAdviceRecord();

                default :
                    assert false;
                    return null;
            }
        }

        public static final EnumSet<RecordType> MODIFY_OPERATIONS = EnumSet.of(
            RecordType.PutFieldLong, RecordType.PutFieldFloat, RecordType.PutFieldDouble, RecordType.PutFieldObject,
            RecordType.PutStaticLong, RecordType.PutStaticFloat, RecordType.PutStaticDouble, RecordType.PutStaticObject,
            RecordType.ArrayStoreLong, RecordType.ArrayStoreFloat, RecordType.ArrayStoreDouble, RecordType.ArrayStoreObject);

        public static final RecordType[] RECORD_TYPE_VALUES = RecordType.values();

    }

    public static class AdviceRecord {
        private static final int CODE_SHIFT = 1;
        private static final int VALUE_SHIFT = 32;
        private static final int BCI_SHIFT = 8;

        public volatile Object thread; // a class that denotes a thread
        public long time;
        /**
         * Stores the advice mode (bit 0), ordinal value of record type (bits 1-7), bci (bits 8-31) and a record-specific int (bits 32-63).
         */
        private long codeAndValue;

        public void setCodeModeBci(RecordType rt, AdviceMode adviceMode, short bci) {
            codeAndValue = adviceMode.ordinal() | (rt.ordinal() << AdviceRecord.CODE_SHIFT) | (bci << BCI_SHIFT);
            assert getRecordType() == rt;
            assert adviceMode.ordinal() == getAdviceModeAsInt();
            assert bci == getBci();
        }

        public void setPackedValue(int value) {
            long lvalue = value;
            codeAndValue |= lvalue << AdviceRecord.VALUE_SHIFT;
        }

        public RecordType getRecordType() {
            int recordOrd = (int) ((codeAndValue >> CODE_SHIFT) & 0x7F);
            return RecordType.RECORD_TYPE_VALUES[recordOrd];
        }

        public int getAdviceModeAsInt() {
            return (int) (codeAndValue & 1);
        }

        public AdviceMode getAdviceMode() {
            return AdviceMode.values()[getAdviceModeAsInt()];
        }

        public int getPackedValue() {
            return (int) (codeAndValue >> AdviceRecord.VALUE_SHIFT);
        }

        public int getArrayIndex() {
            return getPackedValue();
        }

        public void setBci(short bci) {
            codeAndValue |= bci << BCI_SHIFT;
        }

        public short getBci() {
            return (short) ((codeAndValue >> BCI_SHIFT) & 0xFFFF);
        }

        @Override
        public String toString() {
            return "@" + time + " th: " + thread + " " + getRecordType() + ": " + getAdviceMode() + " bci: " +
            getBci() + " pv: " + getPackedValue();
        }
    }

    public static class ObjectAdviceRecord extends AdviceRecord {
        public Object value;
    }

    public static class LongAdviceRecord extends AdviceRecord {
        public long value;
    }

    public static class LongLongAdviceRecord extends LongAdviceRecord {
        public long value2;
    }

    public static class LongLongTBciAdviceRecord extends LongLongAdviceRecord {
        public short targetBci;
    }

    public static class FloatAdviceRecord extends AdviceRecord {
        public float value;
    }

    public static class FloatFloatAdviceRecord extends FloatAdviceRecord {
        public float value2;
    }

    public static class DoubleAdviceRecord extends AdviceRecord {
        public double value;
    }

    public static class DoubleDoubleAdviceRecord extends DoubleAdviceRecord {
        public double value2;
    }

    public static class ObjectObjectAdviceRecord extends ObjectAdviceRecord {
        public Object value2;
    }

    public static class ObjectObjectTBciAdviceRecord extends ObjectObjectAdviceRecord {
        public short targetBci;
    }

    public static class ObjectLongAdviceRecord extends ObjectAdviceRecord {
        public long value2;
    }

    public static class ObjectFloatAdviceRecord extends ObjectAdviceRecord {
        public float value2;
    }

    public static class ObjectDoubleAdviceRecord extends ObjectAdviceRecord {
        public double value2;
    }

    public static class ObjectMethodAdviceRecord extends ObjectAdviceRecord {
        public Object value2;  // a class that denotes a method
    }

    /*
     * The following types are not currently used at VM runtime since field offsets
     * are recorded by their integer offset. If we used FieldActor, similar to
     * the way method invocations use MethodActor, they would be used.
     * However, they are used in the analysis tool so are defined here for completeness.
     *
     * No multiple inheritance, so we pick "field" as the super type, so we can't cast
     * to the non-field value forms.
     */

    public static class ObjectFieldAdviceRecord extends ObjectAdviceRecord {
        public Object field;  // a class that denotes a field
    }

    public static class ObjectFieldLongAdviceRecord extends ObjectFieldAdviceRecord {
        public long value2;
    }

    public static class ObjectFieldFloatAdviceRecord extends ObjectFieldAdviceRecord {
        public float value2;
    }

    public static class ObjectFieldDoubleAdviceRecord extends ObjectFieldAdviceRecord {
        public double value2;
    }

    public static class ObjectFieldObjectAdviceRecord extends ObjectFieldAdviceRecord {
        public Object value2;
    }
}

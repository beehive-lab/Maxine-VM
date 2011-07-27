/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.vma.runtime;

import java.util.*;

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

        // BEGIN GENERATED CODE

        // GENERATED -- EDIT AND RUN TransientVMAdviceHandlerTypesGenerator.main() TO MODIFY
        GC,
        ThreadStarting,
        ThreadTerminating,
        ConstLoadLong,
        ConstLoadObject,
        ConstLoadFloat,
        ConstLoadDouble,
        IPush,
        Load,
        ArrayLoad,
        StoreLong,
        StoreFloat,
        StoreDouble,
        StoreObject,
        ArrayStoreFloat,
        ArrayStoreLong,
        ArrayStoreDouble,
        ArrayStoreObject,
        StackAdjust,
        OperationLong,
        OperationFloat,
        OperationDouble,
        IInc,
        ConversionLong,
        ConversionFloat,
        ConversionDouble,
        IfInt,
        IfObject,
        Return,
        ReturnLong,
        ReturnFloat,
        ReturnDouble,
        ReturnObject,
        GetStatic,
        PutStaticDouble,
        PutStaticLong,
        PutStaticFloat,
        PutStaticObject,
        GetField,
        PutFieldDouble,
        PutFieldLong,
        PutFieldFloat,
        PutFieldObject,
        InvokeVirtual,
        InvokeSpecial,
        InvokeStatic,
        InvokeInterface,
        ArrayLength,
        Throw,
        CheckCast,
        InstanceOf,
        MonitorEnter,
        MonitorExit,
        Bytecode,
        New,
        NewArray,
        MultiNewArray,
        MethodEntry;

        public AdviceRecord newAdviceRecord() {
            switch (this) {
                case ConstLoadLong:
                case ConversionLong:
                case ReturnLong:
                case StoreLong:
                    return new LongAdviceRecord();
                case Bytecode:
                case GC:
                case IPush:
                case Load:
                case Return:
                case StackAdjust:
                case ThreadStarting:
                case ThreadTerminating:
                    return new AdviceRecord();
                case IInc:
                case IfInt:
                case OperationLong:
                    return new LongLongAdviceRecord();
                case ArrayStoreFloat:
                case PutFieldFloat:
                case PutStaticFloat:
                    return new ObjectFloatAdviceRecord();
                case ArrayStoreObject:
                case CheckCast:
                case IfObject:
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
                    return new ObjectLongAdviceRecord();

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
                        RecordType.PutFieldLong, RecordType.PutFieldFloat, RecordType.PutFieldDouble,
                        RecordType.PutStaticLong, RecordType.PutStaticFloat, RecordType.PutStaticDouble,
                        RecordType.ArrayStoreLong, RecordType.ArrayStoreFloat, RecordType.ArrayStoreDouble);

        public static final RecordType[] RECORD_TYPE_VALUES = RecordType.values();

    }

    public static class AdviceRecord {
        private static final int ADVICE_MODE_SHIFT = 8;
        private static final int VALUE_SHIFT = 16;

        public Object thread; // a class that denotes a thread
        public long time;
        private long codeAndValue;

        public void setCodeAndMode(RecordType rt, int adviceMode) {
            codeAndValue = rt.ordinal() | (adviceMode << AdviceRecord.ADVICE_MODE_SHIFT);
        }

        public void setValue(int value) {
            codeAndValue |= value << AdviceRecord.VALUE_SHIFT;
        }

        public RecordType getRecordType() {
            int recordOrd = (int) (codeAndValue & 0xFF);
            return RecordType.RECORD_TYPE_VALUES[recordOrd];
        }

        public int getAdviceMode() {
            return (int) ((codeAndValue >> AdviceRecord.ADVICE_MODE_SHIFT) & 0xFF);
        }

        public int getPackedValue() {
            return (int) (codeAndValue >> AdviceRecord.VALUE_SHIFT);
        }

        public int getArrayIndex() {
            return getPackedValue();
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

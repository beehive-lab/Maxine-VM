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

import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.thread.*;

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
        ReturnObject,
        ReturnLong,
        ReturnFloat,
        ReturnDouble,
        Return,
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
        MultiNewArray;

        public AdviceRecord newAdviceRecord() {
            switch (this) {
                case ConstLoadLong:
                case ConversionLong:
                case ReturnLong:
                case StoreLong:
                    return new LongAdviceRecord();
                case ArrayLoad:
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
                case InvokeInterface:
                case InvokeSpecial:
                case InvokeStatic:
                case InvokeVirtual:
                    return new ObjectMethodActorAdviceRecord();
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

    }

    public static class AdviceRecord {
        static final int ADVICE_MODE_SHIFT = 8;
        static final int VALUE_SHIFT = 16;

        VmThread owner;
        long time;
        long codeAndValue;
    }

    static class ObjectAdviceRecord extends AdviceRecord {
        Object value;
    }

    static class LongAdviceRecord extends AdviceRecord {
        long value;
    }

    static class LongLongAdviceRecord extends LongAdviceRecord {
        long value2;
    }

    static class FloatAdviceRecord extends AdviceRecord {
        float value;
    }

    static class FloatFloatAdviceRecord extends FloatAdviceRecord {
        float value2;
    }

    static class DoubleAdviceRecord extends AdviceRecord {
        double value;
    }

    static class DoubleDoubleAdviceRecord extends DoubleAdviceRecord {
        double value2;
    }

    static class ObjectObjectAdviceRecord extends ObjectAdviceRecord {
        Object value2;
    }

    static class ObjectLongAdviceRecord extends ObjectAdviceRecord {
        long value2;
    }

    static class ObjectFloatAdviceRecord extends ObjectAdviceRecord {
        float value2;
    }

    static class ObjectDoubleAdviceRecord extends ObjectAdviceRecord {
        double value2;
    }

    static class ObjectMethodActorAdviceRecord extends ObjectAdviceRecord {
        MethodActor value2;
    }

}

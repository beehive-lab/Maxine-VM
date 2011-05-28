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

import com.sun.max.vm.thread.*;

/**
 * Definitions of the event types for {@link JavaEventVMAdviceHandler}.
 *
 * The number of actual event types is minimized to facilitate event storage management.
 *
 * Each event carries the ordinal value of the associated {@link #EventType}, which is encoded as the low-order byte of
 * a word that may also carry the array index for array operations.
 *
 */
public class JavaVMAdviceHandlerEvents {
    public enum EventType {
        NewArray, NewObject, InvokeSpecial, GetStatic,
        PutStaticObject, PutStaticLong, PutStaticFloat, PutStaticDouble, GetField,
        PutFieldObject, PutFieldLong, PutFieldFloat, PutFieldDouble,
        ArrayLoad, ArrayStoreObject, ArrayStoreLong, ArrayStoreFloat, ArrayStoreDouble,
        UnseenObject, GC, Removal;

        Event newEvent() {
            switch (this) {
                case NewArray:
                case NewObject:
                case InvokeSpecial:
                    return new ObjectEvent();
                case PutStaticObject:
                case PutFieldObject:
                    return new ObjectObjectValueEvent();
                case PutStaticLong:
                case PutFieldLong:
                    return new ObjectLongValueEvent();
                case PutStaticFloat:
                case PutFieldFloat:
                    return new ObjectFloatValueEvent();
                case PutStaticDouble:
                case PutFieldDouble:
                    return new ObjectDoubleValueEvent();
                case GetStatic:
                case GetField:
                    return new ObjectEvent();
                case ArrayStoreObject:
                    return new ArrayObjectValueEvent();
                case ArrayStoreLong:
                    return new ArrayLongValueEvent();
                case ArrayStoreFloat:
                    return new ArrayFloatValueEvent();
                case ArrayStoreDouble:
                    return new ArrayDoubleValueEvent();
                case ArrayLoad:
                    return new ObjectEvent();
                case UnseenObject:
                    return new ObjectEvent();
                case Removal:
                case GC:
                    return new Event();
                default:
                    assert false;
                    return null;
            }
        }
    }

    public static class Event {
        VmThread owner;
        long time;
        long evtCodeAndValue;
    }

    static class ObjectEvent extends Event {
        Object obj;
    }

    static class ObjectObjectValueEvent extends ObjectEvent {
        Object value;
    }

    static class ObjectLongValueEvent extends ObjectEvent {
        long value;
    }

    static class ObjectFloatValueEvent extends ObjectEvent {
        float value;
    }

    static class ObjectDoubleValueEvent extends ObjectEvent {
        double value;
    }

    static class ArrayObjectValueEvent extends ObjectEvent {
        Object value;
    }

    static class ArrayLongValueEvent extends ObjectEvent {
        long value;
    }

    static class ArrayFloatValueEvent extends ObjectEvent {
        float value;
    }

    static class ArrayDoubleValueEvent extends ObjectEvent {
        double value;
    }

}

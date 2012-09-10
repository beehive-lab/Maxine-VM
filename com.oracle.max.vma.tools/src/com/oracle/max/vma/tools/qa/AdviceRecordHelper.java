/*
 * Copyright (c) 2004, 2012, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.max.vma.tools.qa.TransientVMAdviceHandlerTypes.*;

/**
 * Finesses the use of {@link Object} in {@link TransientVMAdviceHandlerTypes} when at analysis
 * time the value contains either a {@link ThreadRecord}, {@link FieldRecord} or {@link MethodRecord}.
 */
public class AdviceRecordHelper {

    public enum AccessType {
        READ, WRITE;

        public String getName() {
            return this.name().toLowerCase();
        }

        @Override
        public String toString() {
            return getName();
        }
    }

    public static FieldRecord getField(AdviceRecord ar) {
        ObjectFieldAdviceRecord far = (ObjectFieldAdviceRecord) ar;
        return (FieldRecord) far.field;
    }

    public static ObjectRecord getObjectRecord(AdviceRecord ar) {
        ObjectAdviceRecord far = (ObjectAdviceRecord) ar;
        return (ObjectRecord) far.value;
    }

    public static ClassRecord getClassRecord(AdviceRecord ar) {
        ObjectAdviceRecord far = (ObjectAdviceRecord) ar;
        return (ClassRecord) far.value;
    }

    public static MethodRecord getMethod(AdviceRecord ar) {
        ObjectMethodAdviceRecord far = (ObjectMethodAdviceRecord) ar;
        return (MethodRecord) far.value2;
    }

    public static ThreadRecord getThread(AdviceRecord ar) {
        return (ThreadRecord) ar.thread;
    }

    public static AccessType accessType(AdviceRecord ar) {
        if (RecordType.MODIFY_OPERATIONS.contains(ar.getRecordType())) {
            return AccessType.WRITE;
        } else {
            return AccessType.READ;
        }
    }

}

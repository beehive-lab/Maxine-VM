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
package com.oracle.max.vm.ext.vma.store;

/**
 * A variant of a subset of {@link VMATextStore} that uses {@code int} id values to denote
 * class, method, field values instead of {@link String}. This only supports per-thread
 * stores so the thread is also elided.
 *
 */
public interface VMAIdTextStore {

// START GENERATED CODE
// EDIT AND RUN VMAIdStoreGenerator.main() TO MODIFY

    void unseenObject(long time, long objId, int classId);
    void adviseAfterNew(long time, int bci, long arg4, int classId);
    void adviseAfterNewArray(long time, int bci, long arg4, int classId, int arg7);
    void adviseAfterMultiNewArray(long time, int bci, long arg4, int classId, int arg7);
    void adviseBeforeGetStatic(long time, int bci, int fieldId);
    void adviseBeforePutStatic(long time, int bci, int fieldId, float arg7);
    void adviseBeforePutStatic(long time, int bci, int fieldId, double arg7);
    void adviseBeforePutStatic(long time, int bci, int fieldId, long arg7);
    void adviseBeforePutStaticObject(long time, int bci, int fieldId, long arg7);
    void adviseBeforeGetField(long time, int bci, long arg4, int fieldId);
    void adviseBeforePutField(long time, int bci, long arg4, int fieldId, float arg8);
    void adviseBeforePutField(long time, int bci, long arg4, int fieldId, long arg8);
    void adviseBeforePutField(long time, int bci, long arg4, int fieldId, double arg8);
    void adviseBeforePutFieldObject(long time, int bci, long arg4, int fieldId, long arg8);
    void adviseBeforeInvokeVirtual(long time, int bci, long arg4, int methodId);
    void adviseBeforeInvokeSpecial(long time, int bci, long arg4, int methodId);
    void adviseBeforeInvokeStatic(long time, int bci, long arg4, int methodId);
    void adviseBeforeInvokeInterface(long time, int bci, long arg4, int methodId);
    void adviseBeforeCheckCast(long time, int bci, long arg4, int classId);
    void adviseBeforeInstanceOf(long time, int bci, long arg4, int classId);
    void adviseAfterMethodEntry(long time, int bci, long arg4, int methodId);
// END GENERATED CODE

}

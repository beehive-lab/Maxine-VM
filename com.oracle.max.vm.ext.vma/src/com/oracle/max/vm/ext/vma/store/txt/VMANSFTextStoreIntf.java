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
package com.oracle.max.vm.ext.vma.store.txt;


/**
 * This is derived from {@link VMATextStore} but does not assume the use of short forms.
 * Rather it uses multiple arguments to uniquely identify a class/field/method, which
 * can be converted into a short form before passing to {@link VMATextStore}.
 */
public interface VMANSFTextStoreIntf extends VMATextStore {
// START GENERATED CODE
// EDIT AND RUN VMANSFTextStoreGenerator.main() TO MODIFY

    void unseenObject(long time, String threadName, int bci, long objId, String className, long clId);
    void adviseBeforeGetStatic(long time, String threadName, int bci, String className, long clId, String fieldName);
    void adviseBeforePutStaticObject(long time, String threadName, int bci, String className, long clId, String fieldName, long value);
    void adviseBeforePutStatic(long time, String threadName, int bci, String className, long clId, String fieldName, double value);
    void adviseBeforePutStatic(long time, String threadName, int bci, String className, long clId, String fieldName, long value);
    void adviseBeforePutStatic(long time, String threadName, int bci, String className, long clId, String fieldName, float value);
    void adviseBeforeGetField(long time, String threadName, int bci, long objId, String className, long clId, String fieldName);
    void adviseBeforePutFieldObject(long time, String threadName, int bci, long objId, String className, long clId, String fieldName, long value);
    void adviseBeforePutField(long time, String threadName, int bci, long objId, String className, long clId, String fieldName, double value);
    void adviseBeforePutField(long time, String threadName, int bci, long objId, String className, long clId, String fieldName, long value);
    void adviseBeforePutField(long time, String threadName, int bci, long objId, String className, long clId, String fieldName, float value);
    void adviseBeforeInvokeVirtual(long time, String threadName, int bci, long objId, String className, long clId, String methodName);
    void adviseBeforeInvokeSpecial(long time, String threadName, int bci, long objId, String className, long clId, String methodName);
    void adviseBeforeInvokeStatic(long time, String threadName, int bci, long objId, String className, long clId, String methodName);
    void adviseBeforeInvokeInterface(long time, String threadName, int bci, long objId, String className, long clId, String methodName);
    void adviseBeforeCheckCast(long time, String threadName, int bci, long objId, String className, long clId);
    void adviseBeforeInstanceOf(long time, String threadName, int bci, long objId, String className, long clId);
    void adviseAfterNew(long time, String threadName, int bci, long objId, String className, long clId);
    void adviseAfterNewArray(long time, String threadName, int bci, long objId, String className, long clId, int length);
    void adviseAfterMultiNewArray(long time, String threadName, int bci, long objId, String className, long clId, int length);
    void adviseAfterMethodEntry(long time, String threadName, int bci, long objId, String className, long clId, String methodName);
// END GENERATED CODE

}

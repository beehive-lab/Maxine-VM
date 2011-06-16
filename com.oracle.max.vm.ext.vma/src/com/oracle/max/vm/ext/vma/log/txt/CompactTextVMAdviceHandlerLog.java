/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.vma.log.txt;

import java.util.*;

import com.sun.max.program.*;

/**
 * Defines a compact textual format that uses short forms for class, field and thread names.
 * In addition, a repeated id (but not when used as a value) is, by default, passed as
 * {@value TextVMAdviceHandlerLog#REPEAT_ID} and the
 * underlying format knows how to interpret that. This can be suppressed by setting the
 * system property {@value NO_REPEATS_PROPERTY). Repeated ids are only generated
 * for records created by the same originating thread.
 *
 * <li>{@link #classDefinitionTracking}: G shortClass class clid
 * <li>{@link #classDefinitionFieldTracking}: F shortField field
 * <li>{@link #threadDefinitionTracking}: T shortThread thread
 * <li>{@link #methodDefinitionTracking}: M shortMethod method
 * </ul>
 *
 * By default instances of the short forms are represented simply by their
 * integer value. This minimises log size but makes the log very hard to read by a human.
 * A single character prefix (C, F, T) can be generated by setting the
 * system property {@value PREFIX_PROPERTY}.
 *
 * We do not know the multi-threaded behavior of callers. Many threads may be synchronously
 * logging or one or more threads may be asynchronously logging records generated by
 * other threads.
 *
 * The short form maps are (necessarily) global to all threads and must be thread-safe.
 * The repeated id handling uses a thread-local as it operates across multiple
 * methods and is thread-specific.
 */

public abstract class CompactTextVMAdviceHandlerLog extends TextVMAdviceHandlerLog {

    protected final TextVMAdviceHandlerLog del;

    private static Map<String, LastId> repeatedIds;
    private static int classInt;
    private static int fieldInt;
    private static int threadInt;
    private static int methodInt;
    private static final String NO_REPEATS_PROPERTY = "max.vma.norepeatids";
    private static final String PREFIX_PROPERTY = "max.vma.shortprefix";
    private static boolean doRepeats;
    private static boolean doPrefix;

    public static enum ShortForm {
        C("C"),
        F("F"),
        T("T"),
        M("M");

        public final String code;

        private Map<Object, Object> shortForms = new HashMap<Object, Object>();
        private int nextId;

        Object createShortForm(CompactTextVMAdviceHandlerLog handler, Object key) {
            synchronized (shortForms) {
                Object cached = shortForms.get(key);
                if (cached == null) {
                    Object defKey = key;
                    String shortForm = doPrefix ? (code + nextId) : Integer.toString(nextId);
                    nextId++;
                    switch (this) {
                        case T:
                            cached = shortForm;
                            break;
                        case C:
                            cached = new ClassName(shortForm, ((ClassName) key).clId);
                            break;
                        case F:
                        case M:
                            QualName qualName = (QualName) key;
                            ClassName cachedClassName = (ClassName) C.createShortForm(handler, qualName.className);
                            cached = new QualName(cachedClassName, shortForm);
                            // need a hybrid QualName for the define
                            defKey = new QualName(cachedClassName, qualName.name);
                            break;
                    }
                    shortForms.put(key, cached);
                    handler.defineShortForm(this, defKey, shortForm);
                }
                return cached;
            }

        }

        ShortForm(String code) {
            this.code = code;
        }
    }

    static class LastId {
        long id = -1;
    }

    protected CompactTextVMAdviceHandlerLog(TextVMAdviceHandlerLog del) {
        super();
        this.del = del;
    }

    /**
     * Check if this {@code objId} is the same as the previous one.
     * Note that all traces that start with an {@code objId} must call this method!
     * @param id
     * @param threadName
     * @return -1 for a match, {@code id} otherwise
     */
    private static long checkRepeatId(long id, String threadName) {
        LastId lastId;
        if (doRepeats) {
            lastId = getLastId(threadName);
            if (lastId.id == id) {
                return REPEAT_ID_VALUE;
            } else {
                lastId.id = id;
            }
        }
        return id;
    }

    private static LastId getLastId(String threadName) {
        synchronized (repeatedIds) {
            LastId lastId = repeatedIds.get(threadName);
            if (lastId == null) {
                lastId = new LastId();
                repeatedIds.put(threadName, lastId);
            }
            return lastId;
        }
    }

    @Override
    public boolean initializeLog() {
        repeatedIds = new HashMap<String, LastId>();
        doRepeats = System.getProperty(NO_REPEATS_PROPERTY) == null;
        doPrefix = System.getProperty(PREFIX_PROPERTY) != null;
        return del.initializeLog();
    }

    @Override
    public void finalizeLog() {
        del.finalizeLog();
    }

    @Override
    public void removal(long id) {
        del.removal(id);
    }

    @Override
    public void unseenObject(String threadName, long objId, ClassName className) {
        del.unseenObject(getThreadShortForm(threadName), objId, getClassShortForm(className));
    }

    @Override
    public void resetTime() {
        del.resetTime();
    }


    protected ClassName getClassShortForm(ClassName className) {
        return (ClassName) ShortForm.C.createShortForm(this, className);
    }

    protected QualName getFieldShortForm(QualName fieldName) {
        return (QualName) ShortForm.F.createShortForm(this, fieldName);
    }

    protected String getThreadShortForm(String threadName) {
        return (String) ShortForm.T.createShortForm(this, threadName);
    }

    protected QualName getMethodShortForm(QualName methodName) {
        return (QualName) ShortForm.M.createShortForm(this, methodName);
    }

    /**
     * Define a short form of {@code key}.
     *
     * @param type
     * @param key
     * @param shortForm
     */
    public abstract void defineShortForm(ShortForm type, Object key, String shortForm);

    // BEGIN GENERATED CODE

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseAfterMultiNewArray(String arg1, long arg2, ClassName arg3, int arg4) {
        ProgramError.unexpected("adviseAfterMultiNewArray");
    }

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeGC(String arg1) {
        del.adviseBeforeGC(getThreadShortForm(arg1));
    }

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseAfterGC(String arg1) {
        del.adviseAfterGC(getThreadShortForm(arg1));
    }

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeThreadStarting(String arg1) {
        del.adviseBeforeThreadStarting(getThreadShortForm(arg1));
    }

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeThreadTerminating(String arg1) {
        del.adviseBeforeThreadTerminating(getThreadShortForm(arg1));
    }

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeConstLoad(String arg1, long arg2) {
        del.adviseBeforeConstLoad(getThreadShortForm(arg1), arg2);
    }

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeConstLoad(String arg1, float arg2) {
        del.adviseBeforeConstLoad(getThreadShortForm(arg1), arg2);
    }

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeConstLoad(String arg1, double arg2) {
        del.adviseBeforeConstLoad(getThreadShortForm(arg1), arg2);
    }

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeConstLoadObject(String arg1, long arg2) {
        del.adviseBeforeConstLoadObject(getThreadShortForm(arg1), arg2);
    }

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeIPush(String arg1, int arg2) {
        del.adviseBeforeIPush(getThreadShortForm(arg1), arg2);
    }

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeLoad(String arg1, int arg2) {
        del.adviseBeforeLoad(getThreadShortForm(arg1), arg2);
    }

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeArrayLoad(String arg1, long arg2, int arg3) {
        del.adviseBeforeArrayLoad(getThreadShortForm(arg1),  checkRepeatId(arg2, arg1), arg3);
    }

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeStore(String arg1, int arg2, float arg3) {
        del.adviseBeforeStore(getThreadShortForm(arg1), arg2, arg3);
    }

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeStore(String arg1, int arg2, double arg3) {
        del.adviseBeforeStore(getThreadShortForm(arg1), arg2, arg3);
    }

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeStore(String arg1, int arg2, long arg3) {
        del.adviseBeforeStore(getThreadShortForm(arg1), arg2, arg3);
    }

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeStoreObject(String arg1, int arg2, long arg3) {
        del.adviseBeforeStoreObject(getThreadShortForm(arg1), arg2, arg3);
    }

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeArrayStore(String arg1, long arg2, int arg3, long arg4) {
        del.adviseBeforeArrayStore(getThreadShortForm(arg1),  checkRepeatId(arg2, arg1), arg3, arg4);
    }

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeArrayStore(String arg1, long arg2, int arg3, double arg4) {
        del.adviseBeforeArrayStore(getThreadShortForm(arg1),  checkRepeatId(arg2, arg1), arg3, arg4);
    }

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeArrayStore(String arg1, long arg2, int arg3, float arg4) {
        del.adviseBeforeArrayStore(getThreadShortForm(arg1),  checkRepeatId(arg2, arg1), arg3, arg4);
    }

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeArrayStoreObject(String arg1, long arg2, int arg3, long arg4) {
        del.adviseBeforeArrayStoreObject(getThreadShortForm(arg1),  checkRepeatId(arg2, arg1), arg3, arg4);
    }

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeStackAdjust(String arg1, int arg2) {
        del.adviseBeforeStackAdjust(getThreadShortForm(arg1), arg2);
    }

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeOperation(String arg1, int arg2, long arg3, long arg4) {
        del.adviseBeforeOperation(getThreadShortForm(arg1), arg2, arg3, arg4);
    }

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeOperation(String arg1, int arg2, float arg3, float arg4) {
        del.adviseBeforeOperation(getThreadShortForm(arg1), arg2, arg3, arg4);
    }

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeOperation(String arg1, int arg2, double arg3, double arg4) {
        del.adviseBeforeOperation(getThreadShortForm(arg1), arg2, arg3, arg4);
    }

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeIInc(String arg1, int arg2, int arg3, int arg4) {
        del.adviseBeforeIInc(getThreadShortForm(arg1), arg2, arg3, arg4);
    }

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeConversion(String arg1, int arg2, double arg3) {
        del.adviseBeforeConversion(getThreadShortForm(arg1), arg2, arg3);
    }

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeConversion(String arg1, int arg2, float arg3) {
        del.adviseBeforeConversion(getThreadShortForm(arg1), arg2, arg3);
    }

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeConversion(String arg1, int arg2, long arg3) {
        del.adviseBeforeConversion(getThreadShortForm(arg1), arg2, arg3);
    }

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeIf(String arg1, int arg2, int arg3, int arg4) {
        del.adviseBeforeIf(getThreadShortForm(arg1), arg2, arg3, arg4);
    }

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeIfObject(String arg1, int arg2, long arg3, long arg4) {
        del.adviseBeforeIfObject(getThreadShortForm(arg1), arg2, arg3, arg4);
    }

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeReturnObject(String arg1, long arg2) {
        del.adviseBeforeReturnObject(getThreadShortForm(arg1), arg2);
    }

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeReturn(String arg1, float arg2) {
        del.adviseBeforeReturn(getThreadShortForm(arg1), arg2);
    }

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeReturn(String arg1, double arg2) {
        del.adviseBeforeReturn(getThreadShortForm(arg1), arg2);
    }

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeReturn(String arg1) {
        del.adviseBeforeReturn(getThreadShortForm(arg1));
    }

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeReturn(String arg1, long arg2) {
        del.adviseBeforeReturn(getThreadShortForm(arg1), arg2);
    }

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeGetStatic(String arg1, QualName arg2) {
        del.adviseBeforeGetStatic(getThreadShortForm(arg1), getFieldShortForm(arg2));
    }

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforePutStatic(String arg1, QualName arg2, long arg3) {
        del.adviseBeforePutStatic(getThreadShortForm(arg1), getFieldShortForm(arg2), arg3);
    }

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforePutStatic(String arg1, QualName arg2, float arg3) {
        del.adviseBeforePutStatic(getThreadShortForm(arg1), getFieldShortForm(arg2), arg3);
    }

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforePutStatic(String arg1, QualName arg2, double arg3) {
        del.adviseBeforePutStatic(getThreadShortForm(arg1), getFieldShortForm(arg2), arg3);
    }

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforePutStaticObject(String arg1, QualName arg2, long arg3) {
        del.adviseBeforePutStaticObject(getThreadShortForm(arg1), getFieldShortForm(arg2), arg3);
    }

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeGetField(String arg1, long arg2, QualName arg3) {
        del.adviseBeforeGetField(getThreadShortForm(arg1), checkRepeatId(arg2, arg1), getFieldShortForm(arg3));
    }

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforePutField(String arg1, long arg2, QualName arg3, long arg4) {
        del.adviseBeforePutField(getThreadShortForm(arg1), checkRepeatId(arg2, arg1), getFieldShortForm(arg3), arg4);
    }

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforePutField(String arg1, long arg2, QualName arg3, float arg4) {
        del.adviseBeforePutField(getThreadShortForm(arg1), checkRepeatId(arg2, arg1), getFieldShortForm(arg3), arg4);
    }

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforePutField(String arg1, long arg2, QualName arg3, double arg4) {
        del.adviseBeforePutField(getThreadShortForm(arg1), checkRepeatId(arg2, arg1), getFieldShortForm(arg3), arg4);
    }

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforePutFieldObject(String arg1, long arg2, QualName arg3, long arg4) {
        del.adviseBeforePutFieldObject(getThreadShortForm(arg1), checkRepeatId(arg2, arg1), getFieldShortForm(arg3), arg4);
    }

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeInvokeVirtual(String arg1, long arg2, QualName arg3) {
        del.adviseBeforeInvokeVirtual(getThreadShortForm(arg1), checkRepeatId(arg2, arg1), getMethodShortForm(arg3));
    }

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeInvokeSpecial(String arg1, long arg2, QualName arg3) {
        del.adviseBeforeInvokeSpecial(getThreadShortForm(arg1), checkRepeatId(arg2, arg1), getMethodShortForm(arg3));
    }

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeInvokeStatic(String arg1, long arg2, QualName arg3) {
        del.adviseBeforeInvokeStatic(getThreadShortForm(arg1), checkRepeatId(arg2, arg1), getMethodShortForm(arg3));
    }

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeInvokeInterface(String arg1, long arg2, QualName arg3) {
        del.adviseBeforeInvokeInterface(getThreadShortForm(arg1), checkRepeatId(arg2, arg1), getMethodShortForm(arg3));
    }

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeArrayLength(String arg1, long arg2, int arg3) {
        del.adviseBeforeArrayLength(getThreadShortForm(arg1), arg2, arg3);
    }

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeThrow(String arg1, long arg2) {
        del.adviseBeforeThrow(getThreadShortForm(arg1), checkRepeatId(arg2, arg1));
    }

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeCheckCast(String arg1, long arg2, ClassName arg3) {
        del.adviseBeforeCheckCast(getThreadShortForm(arg1), checkRepeatId(arg2, arg1), getClassShortForm(arg3));
    }

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeInstanceOf(String arg1, long arg2, ClassName arg3) {
        del.adviseBeforeInstanceOf(getThreadShortForm(arg1), checkRepeatId(arg2, arg1), getClassShortForm(arg3));
    }

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeMonitorEnter(String arg1, long arg2) {
        del.adviseBeforeMonitorEnter(getThreadShortForm(arg1), checkRepeatId(arg2, arg1));
    }

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeMonitorExit(String arg1, long arg2) {
        del.adviseBeforeMonitorExit(getThreadShortForm(arg1), checkRepeatId(arg2, arg1));
    }

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeBytecode(String arg1, int arg2) {
        del.adviseBeforeBytecode(getThreadShortForm(arg1), arg2);
    }

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseAfterInvokeVirtual(String arg1, long arg2, QualName arg3) {
        del.adviseAfterInvokeVirtual(getThreadShortForm(arg1), checkRepeatId(arg2, arg1), getMethodShortForm(arg3));
    }

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseAfterInvokeSpecial(String arg1, long arg2, QualName arg3) {
        del.adviseAfterInvokeSpecial(getThreadShortForm(arg1), checkRepeatId(arg2, arg1), getMethodShortForm(arg3));
    }

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseAfterInvokeStatic(String arg1, long arg2, QualName arg3) {
        del.adviseAfterInvokeStatic(getThreadShortForm(arg1), checkRepeatId(arg2, arg1), getMethodShortForm(arg3));
    }

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseAfterInvokeInterface(String arg1, long arg2, QualName arg3) {
        del.adviseAfterInvokeInterface(getThreadShortForm(arg1), checkRepeatId(arg2, arg1), getMethodShortForm(arg3));
    }

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseAfterNew(String arg1, long arg2, ClassName arg3) {
        del.adviseAfterNew(getThreadShortForm(arg1), checkRepeatId(arg2, arg1), getClassShortForm(arg3));
    }

    // GENERATED -- EDIT AND RUN CompactTextVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseAfterNewArray(String arg1, long arg2, ClassName arg3, int arg4) {
        del.adviseAfterNewArray(getThreadShortForm(arg1), checkRepeatId(arg2, arg1), getClassShortForm(arg3), arg4);
    }


}

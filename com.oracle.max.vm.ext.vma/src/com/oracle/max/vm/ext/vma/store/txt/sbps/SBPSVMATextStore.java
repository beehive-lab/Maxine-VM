/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.vma.store.txt.sbps;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import static com.oracle.max.vm.ext.vma.store.txt.CVMATextStore.Key.*;

import com.oracle.max.vm.ext.vma.store.*;
import com.oracle.max.vm.ext.vma.store.txt.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.*;

/**
 * An implementation of {@link CVMATextStore} using a {@link PrintStream} and {@link StringBuilder}.
 *
 * The default {@link StringBuilder buffer size} is {@link DEFAULT_BUFSIZE} but this can be changed
 * with the {@link BUFSIZE_PROPERTY} system property. The buffer is normally flushed when it is full,
 * but this can be changed to a specific value by setting the {@link FLUSH_PROPERTY} system property.
 *
 * N.B. The classloader id associated with a class, cf. {@link ClassNameId} is only output when the
 * short form is defined.
 *
 * At one time this class could be used directly as an implementation of {@link CVMATextStore}.
 * However, it is now dependent on {@link SBPSCSFVMATextStore}. For example
 * the optimization on classloader id only works with short forms. In any event, for serious
 * use short forms are essential.
 *
 * This class is unsynchronized for use with a single thread. For multiple-threads use
 * {@link SBPSLockedVMATextStore} subclass which synchronizes and then invokes the methods
 * in this class.
 *
 * In per-thread mode each thread has its own buffer and log file.
 * The log file name is used as a stem and each thread's file is named by suffixing with its id, which
 * will be the short form created by {@link SBPSCSFVMATextStore}. The file/stream/buffer
 * is created in {@link #defineThread}.
 */
public class SBPSVMATextStore extends CVMATextStore {

    /**
     * Records per-thread output info.
     */
    static class PSBuilder {
        final PrintStream ps;
        final StringBuilder sb;
        long lastTime;
        PSBuilder(PrintStream ps, StringBuilder sb) {
            this.ps = ps;
            this.sb = sb;
            this.lastTime = System.nanoTime();
        }

    }

    private static final String FLUSH_PROPERTY = "max.vma.flushbufat";
    private static final String BUFSIZE_PROPERTY = "max.vma.storebufsize";
    private static final String ABSTIME_PROPERTY = "max.vma.abstime";
    private static final int DEFAULT_BUFSIZE = 1024 * 1024;

    private boolean absTimeFlag;
    private int flushLogAt;
    private int logSize = DEFAULT_BUFSIZE;
    private boolean threadBatched;
    private boolean perThread;
    @CONSTANT_WHEN_NOT_ZERO
    private File logFileDir;
    private Map<String, PSBuilder> psBuilderMap;

    /**
     * In per-thread mode, the output for the "current" thread, set in {@link #threadSwitch}, else the global stream.
     */
    PSBuilder psb;

    @Override
    public boolean initializeStore(boolean threadBatched, boolean perThread) {
        this.threadBatched = threadBatched;
        this.perThread = perThread;
        final String logSizeProp = System.getProperty(BUFSIZE_PROPERTY);
        if (logSizeProp != null) {
            logSize = Integer.parseInt(logSizeProp);
        }
        flushLogAt = System.getProperty(FLUSH_PROPERTY) != null ? 0 : logSize - 80;
        absTimeFlag = System.getProperty(ABSTIME_PROPERTY) != null;

        logFileDir = new File(VMAStoreFile.getStoreDir());
        cleanOutputDir();

        if (!perThread) {
            psb = createPSBuilder(VMAStoreFile.GLOBAL_STORE);
            return psb != null;
        } else {
            // threads setup in defineThread
            // N.B. main thread does get that call (soon after this)
            psBuilderMap = new ConcurrentHashMap<String, PSBuilder>();
            return true;
        }
    }

    StringBuilder sb() {
        return psb.sb;
    }

    private void cleanOutputDir() {
        if (logFileDir.exists()) {
            for (String fn : logFileDir.list()) {
                if (!new File(logFileDir, fn).delete()) {
                    System.err.println("failed to delete VMA output file: " + fn);
                }
            }
        } else {
            logFileDir.mkdir();
        }
    }

    /**
     * Creates a {@link PrintStream} and a {@link StringBuilder}.
     * @param fileName
     * @return
     */
    PSBuilder createPSBuilder(String fileName) {
        File file = new File(logFileDir, fileName);
        try {
            PrintStream ps = new PrintStream(new FileOutputStream(file));
            psb = new PSBuilder(ps, new StringBuilder(logSize));
            // Format log buffer with header information
            appendCode(INITIALIZE_LOG);
            appendSpace();
            psb.sb.append(psb.lastTime);
            appendSpace();
            psb.sb.append(absTimeFlag);
            appendSpace();
            psb.sb.append(threadBatched);
            end();
            return psb;
        } catch (IOException ex) {
            System.err.println("failed to open log file " + file + ": " + ex);
            return null;
        }
    }

    /**
     * Must create the per-thread buffer before the short form for a thread is defined.
     * @param threadName
     */
    @NEVER_INLINE
    void defineThread(String threadName) {
        Log.printCurrentThread(false); Log.print(": defineThread: "); Log.println(threadName);
        if (perThread) {
            psb = createPSBuilder(threadName);
            psBuilderMap.put(threadName, psb);
        }
    }

    @Override
    public void finalizeStore() {
        if (perThread) {
            for (PSBuilder psBuilder : psBuilderMap.values()) {
                psb = psBuilder;
                finalizeLogBuffer();
            }
        } else {
            finalizeLogBuffer();
        }
    }

    private void finalizeLogBuffer() {
        appendCode(FINALIZE_LOG);
        appendSpace();
        psb.sb.append(System.nanoTime());
        flushLogAt = 0;
        end();
        psb.ps.close();
    }

    void end() {
        psb.sb.append('\n');
        if (psb.sb.length()  >= flushLogAt) {
            psb.ps.print(psb.sb);
            psb.ps.flush();
            psb.sb.setLength(0);
        }
    }

    private void appendCheckRepeatId(long objId) {
        if (objId == REPEAT_ID_VALUE) {
            psb.sb.append('*');
        } else {
            psb.sb.append(objId);
        }
    }

    private void appendTime(long time) {
        if (absTimeFlag) {
            psb.sb.append(time);
        } else {
            psb.sb.append(time - psb.lastTime);
            psb.lastTime = time;
        }
    }

    private void appendSpace() {
        psb.sb.append(' ');
    }

    private void appendCode(Key key) {
        psb.sb.append(key.code);
    }

    /**
     * Append the log entry key code, then the time associated with the entry, followed by the thread.
     * @param time TODO
     * @param key
     * @param threadName
     */
    private void appendTT(long time, Key key, String threadName) {
        appendCode(key);
        appendSpace();
        appendTime(time);
        appendSpace();
        psb.sb.append(threadName);
    }

    /**
     * As {@link #appendTT} followed by the {@code objId}.
     * @param time TODO
     * @param key
     * @param objId
     * @param threadName
     */
    private void appendTTId(long time, Key key, long objId, String threadName) {
        appendTT(time, key, threadName);
        appendSpace();
        appendCheckRepeatId(objId);
        appendSpace();
    }

    /**
     * As {@link #appendTTId} followed by an array index.
     * @param time TODO
     * @param key
     * @param objId
     * @param threadName
     * @param index
     */
    private void appendTTIdIndex(long time, Key key, long objId, String threadName, int index) {
        appendTT(time, key, threadName);
        appendSpace();
        appendCheckRepeatId(objId);
        appendSpace();
        psb.sb.append(index);
        appendSpace();
    }

    /**
     * Append a qualified name.
     * First append the class name and then the (short form) of the qualified name.
     * @param qualName
     */
    private void appendQualName(String className, long clId, String memberName) {
        psb.sb.append(className);
        appendSpace();
        // clId elided as in short form of className
        psb.sb.append(memberName);
    }

    /**
     * As {@link #appendTT} then append a class name.
     * @param time TODO
     * @param key
     * @param className
     * @param threadName
     */
    private void appendTTC(long time, Key key, String className, String threadName) {
        appendTT(time, key, threadName);
        appendSpace();
        psb.sb.append(className);
        appendSpace();
    }

    private void appendPutFieldPrefix(long time, long objId, String className, long clId, String memberName, String threadName) {
        appendTTId(time, ADVISE_BEFORE_PUT_FIELD, objId, threadName);
        appendQualName(className, clId, memberName);
        appendSpace();
    }

    private void appendPutStaticPrefix(long time, String className, long clId, String memberName, String threadName) {
        appendTT(time, ADVISE_BEFORE_PUT_STATIC, threadName);
        appendSpace();
        appendQualName(className, clId, memberName);
        appendSpace();
    }

    private void prefixAdviseBeforeOperation(long time, String threadName, int arg1) {
        appendTT(time, ADVISE_BEFORE_OPERATION, threadName);
        appendSpace();
        psb.sb.append(arg1);
        appendSpace();
    }

    @Override
    public void adviseAfterGC(long time, String threadName) {
        appendTT(time, ADVISE_AFTER_GC, threadName);
        end();
    }

    @Override
    public void removal(long id) {
        appendCode(REMOVAL);
        appendSpace();
        psb.sb.append(id);
        end();
    }

    @Override
    public void unseenObject(long time, String threadName, long objId, String className, long clId) {
        appendTTId(time, UNSEEN, objId, threadName);
        psb.sb.append(className);
        // clId elided
        end();
    }

    @Override
    public void threadSwitch(long time, String threadName) {
        if (perThread) {
            psb = psBuilderMap.get(threadName);
            assert psb != null;
        } else {
            appendCode(THREAD_SWITCH);
            appendSpace();
            psb.lastTime = time;
            psb.sb.append(psb.lastTime);
            end();
        }
    }

    @Override
    public void adviseBeforeThreadStarting(long time, String threadName) {
    }

    @Override
    public void adviseBeforeThreadTerminating(long time, String threadName) {
    }

    @Override
    public void adviseBeforeGetStatic(long time, String threadName, String className, long clId, String memberName) {
        appendTT(time, ADVISE_BEFORE_GET_STATIC, threadName);
        appendSpace();
        appendQualName(className, clId, memberName);
        end();
    }

    @Override
    public void adviseBeforePutStatic(long time, String threadName, String className, long clId, String memberName, double value) {
        appendPutStaticPrefix(time, className, clId, memberName, threadName);
        psb.sb.append(DOUBLE_VALUE);
        appendSpace();
        psb.sb.append(value);
        end();
    }

    @Override
    public void adviseBeforePutStatic(long time, String threadName, String className, long clId, String memberName, long value) {
        appendPutStaticPrefix(time, className, clId, memberName, threadName);
        psb.sb.append(LONG_VALUE);
        appendSpace();
        psb.sb.append(value);
        end();
    }

    @Override
    public void adviseBeforePutStatic(long time, String threadName, String className, long clId, String memberName, float value) {
        appendPutStaticPrefix(time, className, clId, memberName, threadName);
        psb.sb.append(FLOAT_VALUE);
        appendSpace();
        psb.sb.append(value);
        end();
    }

    @Override
    public void adviseBeforePutStaticObject(long time, String threadName, String className, long clId, String memberName, long valueId) {
        appendPutStaticPrefix(time, className, clId, memberName, threadName);
        psb.sb.append(OBJ_VALUE);
        appendSpace();
        psb.sb.append(valueId);
        end();
    }

    @Override
    public void adviseBeforeGetField(long time, String threadName, long objId, String className, long clId, String memberName) {
        appendTTId(time, ADVISE_BEFORE_GET_FIELD, objId, threadName);
        appendQualName(className, clId, memberName);
        end();
    }

    @Override
    public void adviseBeforePutField(long time, String threadName, long objId, String className, long clId, String memberName, double value) {
        appendPutFieldPrefix(time, objId, className, clId, memberName, threadName);
        psb.sb.append(DOUBLE_VALUE);
        appendSpace();
        psb.sb.append(value);
        end();
    }

    @Override
    public void adviseBeforePutField(long time, String threadName, long objId, String className, long clId, String memberName, long value) {
        appendPutFieldPrefix(time, objId, className, clId, memberName, threadName);
        psb.sb.append(LONG_VALUE);
        appendSpace();
        psb.sb.append(value);
        end();
    }

    @Override
    public void adviseBeforePutField(long time, String threadName, long objId, String className, long clId, String memberName, float value) {
        appendPutFieldPrefix(time, objId, className, clId, memberName, threadName);
        psb.sb.append(FLOAT_VALUE);
        appendSpace();
        psb.sb.append(value);
        end();
    }

    @Override
    public void adviseBeforePutFieldObject(long time, String threadName, long objId, String className, long clId, String memberName, long valueId) {
        appendPutFieldPrefix(time, objId, className, clId, memberName, threadName);
        psb.sb.append(OBJ_VALUE);
        appendSpace();
        psb.sb.append(valueId);
        end();
    }

    @Override
    public void adviseBeforeArrayLoad(long time, String threadName, long objId, int index) {
        appendTTIdIndex(time, ADVISE_BEFORE_ARRAY_LOAD, objId, threadName, index);
        end();
    }

    @Override
    public void adviseBeforeArrayStore(long time, String threadName, long objId, int index, float value) {
        appendTTIdIndex(time, ADVISE_BEFORE_ARRAY_STORE, objId, threadName, index);
        psb.sb.append(FLOAT_VALUE);
        appendSpace();
        psb.sb.append(value);
        end();
    }

    @Override
    public void adviseBeforeArrayStore(long time, String threadName, long objId, int index, long value) {
        appendTTIdIndex(time, ADVISE_BEFORE_ARRAY_STORE, objId, threadName, index);
        psb.sb.append(LONG_VALUE);
        appendSpace();
        psb.sb.append(value);
        end();
    }

    @Override
    public void adviseBeforeArrayStore(long time, String threadName, long objId, int index, double value) {
        appendTTIdIndex(time, ADVISE_BEFORE_ARRAY_STORE, objId, threadName, index);
        psb.sb.append(DOUBLE_VALUE);
        appendSpace();
        psb.sb.append(value);
        end();
    }

    @Override
    public void adviseBeforeArrayStoreObject(long time, String threadName, long objId, int index, long valueId) {
        appendTTIdIndex(time, ADVISE_BEFORE_ARRAY_STORE, objId, threadName, index);
        psb.sb.append(OBJ_VALUE);
        appendSpace();
        psb.sb.append(valueId);
        end();
    }

    @Override
    public void adviseAfterNew(long time, String threadName, long objId, String className, long clId) {
        appendTTId(time, ADVISE_AFTER_NEW, objId, threadName);
        psb.sb.append(className);
        end();
    }

    @Override
    public void adviseAfterNewArray(long time, String threadName, long objId, String className, long clId, int length) {
        appendTTId(time, ADVISE_AFTER_NEW_ARRAY, objId, threadName);
        psb.sb.append(className);
        appendSpace();
        psb.sb.append(length);
        end();
    }

    @Override
    public void adviseAfterMultiNewArray(long time, String threadName, long objId, String className, long clId, int length) {
        // MultiArrays are explicitly handled by multiple calls to adviseAfterNewArray so we just
        // log the top level array.
        adviseAfterNewArray(time, threadName, objId, className, clId, length);
    }

    @Override
    public void adviseBeforeGC(long time, String threadName) {
        appendTT(time, ADVISE_BEFORE_GC, threadName);
        end();
    }

    @Override
    public void adviseBeforeConstLoad(long time, String threadName, long value) {
        appendTT(time, ADVISE_BEFORE_CONST_LOAD, threadName);
        appendSpace();
        psb.sb.append(LONG_VALUE);
        appendSpace();
        psb.sb.append(value);
        end();
    }

    @Override
    public void adviseBeforeConstLoadObject(long time, String threadName, long value) {
        appendTT(time, ADVISE_BEFORE_CONST_LOAD, threadName);
        appendSpace();
        psb.sb.append(OBJ_VALUE);
        appendSpace();
        psb.sb.append(value);
        end();
    }

    @Override
    public void adviseBeforeConstLoad(long time, String threadName, float value) {
        appendTT(time, ADVISE_BEFORE_CONST_LOAD, threadName);
        appendSpace();
        psb.sb.append(FLOAT_VALUE);
        appendSpace();
        psb.sb.append(value);
        end();
    }

    @Override
    public void adviseBeforeConstLoad(long time, String threadName, double value) {
        appendTT(time, ADVISE_BEFORE_CONST_LOAD, threadName);
        appendSpace();
        psb.sb.append(DOUBLE_VALUE);
        appendSpace();
        psb.sb.append(value);
        end();
    }

    @Override
    public void adviseBeforeLoad(long time, String threadName, int arg1) {
        appendTT(time, ADVISE_BEFORE_LOAD, threadName);
        appendSpace();
        psb.sb.append(arg1);
        end();
    }

    @Override
    public void adviseBeforeStore(long time, String threadName, int dispToLocalSlot, long value) {
        appendTT(time, ADVISE_BEFORE_STORE, threadName);
        appendSpace();
        psb.sb.append(dispToLocalSlot);
        appendSpace();
        psb.sb.append(LONG_VALUE);
        appendSpace();
        psb.sb.append(value);
        end();
    }

    @Override
    public void adviseBeforeStore(long time, String threadName, int dispToLocalSlot, float value) {
        appendTT(time, ADVISE_BEFORE_STORE, threadName);
        appendSpace();
        psb.sb.append(dispToLocalSlot);
        appendSpace();
        psb.sb.append(FLOAT_VALUE);
        appendSpace();
        psb.sb.append(value);
        end();
    }

    @Override
    public void adviseBeforeStore(long time, String threadName, int dispToLocalSlot, double value) {
        appendTT(time, ADVISE_BEFORE_STORE, threadName);
        appendSpace();
        psb.sb.append(dispToLocalSlot);
        appendSpace();
        psb.sb.append(DOUBLE_VALUE);
        appendSpace();
        psb.sb.append(value);
        end();
    }

    @Override
    public void adviseBeforeStoreObject(long time, String threadName, int dispToLocalSlot, long value) {
        appendTT(time, ADVISE_BEFORE_STORE, threadName);
        appendSpace();
        psb.sb.append(dispToLocalSlot);
        appendSpace();
        psb.sb.append(OBJ_VALUE);
        appendSpace();
        psb.sb.append(value);
        end();
    }

    @Override
    public void adviseBeforeStackAdjust(long time, String threadName, int arg1) {
        appendTT(time, ADVISE_BEFORE_STACK_ADJUST, threadName);
        appendSpace();
        psb.sb.append(arg1);
        end();
    }

    @Override
    public void adviseBeforeOperation(long time, String threadName, int arg1, long arg2, long arg3) {
        prefixAdviseBeforeOperation(time, threadName, arg1);
        psb.sb.append(LONG_VALUE);
        appendSpace();
        psb.sb.append(arg2);
        appendSpace();
        psb.sb.append(arg3);
        end();
    }

    @Override
    public void adviseBeforeOperation(long time, String threadName, int arg1, float arg2, float arg3) {
        prefixAdviseBeforeOperation(time, threadName, arg1);
        psb.sb.append(FLOAT_VALUE);
        appendSpace();
        psb.sb.append(arg2);
        appendSpace();
        psb.sb.append(arg3);
        end();
    }

    @Override
    public void adviseBeforeOperation(long time, String threadName, int arg1, double arg2, double arg3) {
        prefixAdviseBeforeOperation(time, threadName, arg1);
        psb.sb.append(DOUBLE_VALUE);
        appendSpace();
        psb.sb.append(arg2);
        appendSpace();
        psb.sb.append(arg3);
        end();
    }

    @Override
    public void adviseBeforeConversion(long time, String threadName, int arg1, long arg2) {
        appendTT(time, ADVISE_BEFORE_CONVERSION, threadName);
        appendSpace();
        psb.sb.append(arg1);
        appendSpace();
        psb.sb.append(LONG_VALUE);
        appendSpace();
        psb.sb.append(arg2);
        appendSpace();
        end();
    }

    @Override
    public void adviseBeforeConversion(long time, String threadName, int arg1, float arg2) {
        appendTT(time, ADVISE_BEFORE_CONVERSION, threadName);
        appendSpace();
        psb.sb.append(arg1);
        appendSpace();
        psb.sb.append(FLOAT_VALUE);
        appendSpace();
        psb.sb.append(arg2);
        appendSpace();
        end();
    }

    @Override
    public void adviseBeforeConversion(long time, String threadName, int arg1, double arg2) {
        appendTT(time, ADVISE_BEFORE_CONVERSION, threadName);
        appendSpace();
        psb.sb.append(arg1);
        appendSpace();
        psb.sb.append(DOUBLE_VALUE);
        appendSpace();
        psb.sb.append(arg2);
        appendSpace();
        end();
    }

    @Override
    public void adviseBeforeIf(long time, String threadName, int opcode, int op1, int op2) {
        appendTT(time, ADVISE_BEFORE_IF, threadName);
        appendSpace();
        psb.sb.append(opcode);
        appendSpace();
        psb.sb.append(LONG_VALUE);
        appendSpace();
        psb.sb.append(op1);
        appendSpace();
        psb.sb.append(op2);
        appendSpace();
        end();
    }

    @Override
    public void adviseBeforeIfObject(long time, String threadName, int opcode, long objId1, long objId2) {
        appendTT(time, ADVISE_BEFORE_IF, threadName);
        appendSpace();
        psb.sb.append(opcode);
        appendSpace();
        psb.sb.append(OBJ_VALUE);
        appendSpace();
        psb.sb.append(objId1);
        appendSpace();
        psb.sb.append(objId2);
        appendSpace();
        end();
    }

    @Override
    public void adviseBeforeReturnObject(long time, String threadName, long value) {
        appendTT(time, ADVISE_BEFORE_RETURN, threadName);
        appendSpace();
        psb.sb.append(OBJ_VALUE);
        appendSpace();
        psb.sb.append(value);
        end();
    }

    @Override
    public void adviseBeforeReturn(long time, String threadName, long value) {
        appendTT(time, ADVISE_BEFORE_RETURN, threadName);
        appendSpace();
        psb.sb.append(LONG_VALUE);
        appendSpace();
        psb.sb.append(value);
        end();
    }

    @Override
    public void adviseBeforeReturn(long time, String threadName, float value) {
        appendTT(time, ADVISE_BEFORE_RETURN, threadName);
        appendSpace();
        psb.sb.append(FLOAT_VALUE);
        appendSpace();
        psb.sb.append(value);
        end();
    }

    @Override
    public void adviseBeforeReturn(long time, String threadName, double value) {
        appendTT(time, ADVISE_BEFORE_RETURN, threadName);
        appendSpace();
        psb.sb.append(DOUBLE_VALUE);
        appendSpace();
        psb.sb.append(value);
        end();
    }

    @Override
    public void adviseBeforeReturn(long time, String threadName) {
        appendTT(time, ADVISE_BEFORE_RETURN, threadName);
        end();
    }

    @Override
    public void adviseBeforeInvokeVirtual(long time, String threadName, long objId, String className, long clId, String memberName) {
        appendTTId(time, ADVISE_BEFORE_INVOKE_VIRTUAL, objId, threadName);
        appendQualName(className, clId, memberName);
        end();
    }

    @Override
    public void adviseBeforeInvokeSpecial(long time, String threadName, long objId, String className, long clId, String memberName) {
        appendTTId(time, ADVISE_BEFORE_INVOKE_SPECIAL, objId, threadName);
        appendQualName(className, clId, memberName);
        end();
    }

    @Override
    public void adviseBeforeInvokeStatic(long time, String threadName, long objId, String className, long clId, String memberName) {
        appendTTId(time, ADVISE_BEFORE_INVOKE_STATIC, objId, threadName);
        appendQualName(className, clId, memberName);
        end();
    }

    @Override
    public void adviseBeforeInvokeInterface(long time, String threadName, long objId, String className, long clId, String memberName) {
        appendTTId(time, ADVISE_BEFORE_INVOKE_INTERFACE, objId, threadName);
        appendQualName(className, clId, memberName);
        end();
    }

    @Override
    public void adviseBeforeArrayLength(long time, String threadName, long objId, int length) {
        appendTTId(time, ADVISE_BEFORE_ARRAY_LENGTH, objId, threadName);
        psb.sb.append(length);
        end();
    }

    @Override
    public void adviseBeforeThrow(long time, String threadName, long objId) {
        appendTTId(time, ADVISE_BEFORE_THROW, objId, threadName);
        end();
    }

    @Override
    public void adviseBeforeCheckCast(long time, String threadName, long objId, String className, long clId) {
        appendTTId(time, ADVISE_BEFORE_CHECK_CAST, objId, threadName);
        psb.sb.append(className);
        end();
    }

    @Override
    public void adviseBeforeInstanceOf(long time, String threadName, long objId, String className, long clId) {
        appendTTId(time, ADVISE_BEFORE_INSTANCE_OF, objId, threadName);
        psb.sb.append(className);
        end();
    }

    @Override
    public void adviseBeforeBytecode(long time, String threadName, int arg1) {
        appendTT(time, ADVISE_BEFORE_BYTECODE, threadName);
        appendSpace();
        psb.sb.append(arg1);
        end();
    }

    @Override
    public void adviseBeforeMonitorEnter(long time, String threadName, long objId) {
        appendTTId(time, ADVISE_BEFORE_MONITOR_ENTER, objId, threadName);
        end();
    }

    @Override
    public void adviseBeforeMonitorExit(long time, String threadName, long objId) {
        appendTTId(time, ADVISE_BEFORE_MONITOR_EXIT, objId, threadName);
        end();
    }

    @Override
    public void adviseAfterInvokeVirtual(long time, String threadName, long objId, String className, long clId, String memberName) {
        appendTTId(time, ADVISE_AFTER_INVOKE_VIRTUAL, objId, threadName);
        appendQualName(className, clId, memberName);
        end();
    }

    @Override
    public void adviseAfterInvokeStatic(long time, String threadName, long objId, String className, long clId, String memberName) {
        appendTTId(time, ADVISE_AFTER_INVOKE_STATIC, objId, threadName);
        appendQualName(className, clId, memberName);
        end();
    }

    @Override
    public void adviseAfterInvokeInterface(long time, String threadName, long objId, String className, long clId, String memberName) {
        appendTTId(time, ADVISE_AFTER_INVOKE_INTERFACE, objId, threadName);
        appendQualName(className, clId, memberName);
        end();
    }

    @Override
    public void adviseAfterInvokeSpecial(long time, String threadName, long objId, String className, long clId, String memberName) {
        appendTTId(time, ADVISE_AFTER_INVOKE_SPECIAL, objId, threadName);
        appendQualName(className, clId, memberName);
        end();
    }

    @Override
    public void adviseAfterMethodEntry(long time, String threadName, long objId, String className, long clId, String memberName) {
        appendTTId(time, ADVISE_AFTER_METHOD_ENTRY, objId, threadName);
        appendQualName(className, clId, memberName);
        end();
    }

    @Override
    public void adviseBeforeReturnByThrow(long time, String threadName, long objId, int poppedFrames) {
        appendTTId(time, ADVISE_BEFORE_RETURN_BY_THROW, objId, threadName);
        psb.sb.append(poppedFrames);
        end();

    }

}

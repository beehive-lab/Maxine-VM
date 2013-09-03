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
import java.util.concurrent.locks.*;

import static com.oracle.max.vm.ext.vma.store.txt.VMATextStoreFormat.*;
import static com.oracle.max.vm.ext.vma.store.txt.VMATextStoreFormat.Key.*;

import com.oracle.max.vm.ext.vma.run.java.*;
import com.oracle.max.vm.ext.vma.store.*;
import com.oracle.max.vm.ext.vma.store.txt.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.runtime.*;

/**
 * An implementation of {@link VMATextStore} and {@link VMAIdTextStoreIntf} using a {@link PrintStream} and {@link StringBuilder}.
 *
 * The default {@link StringBuilder buffer size} is {@link DEFAULT_BUFSIZE} but this can be changed
 * with the {@link BUFSIZE_PROPERTY} system property. The buffer is normally flushed when it is full,
 * but this can be changed to a specific value by setting the {@link FLUSH_PROPERTY} system property.
 *
 * In per-thread mode each thread has its own buffer and log file.
 * The log file name is used as a stem and each thread's file is named by suffixing with its name.
 * The file/stream/buffer is created in {@link #defineThread} which may, due to the use of short forms, be called
 * before {@link #adviseBeforeThreadStarting(long, String)}.
 *
 * This class is unsynchronized for use in per-thread mode.
 *
 */
public abstract class SBPSVMAIdTextStore implements VMAIdTextStoreIntf {

    private static final String FLUSH_PROPERTY = "max.vma.store.flush";
    private static final String BUFSIZE_PROPERTY = "max.vma.store.bufsize";
    private static final String TEXTKEY_PROPERTY = "max.vma.store.textkey";
    private static final int DEFAULT_BUFSIZE = 1024 * 1024;

    /**
     * {@code true} iff storing absolute time.
     */
    @CONSTANT_WHEN_NOT_ZERO
    private static VMATimeMode timeMode;

    @CONSTANT_WHEN_NOT_ZERO
    private static File storeFileDir;

    @CONSTANT_WHEN_NOT_ZERO
    private static int globalBufSize = DEFAULT_BUFSIZE;

    @CONSTANT_WHEN_NOT_ZERO
    private static String flushProperty;

    @CONSTANT_WHEN_NOT_ZERO
    private static boolean textKey;

    private static volatile boolean finalizing;

    /**
     * The main thread owns this lock after initialization.
     * It is used to block any daemon threads at store finalization.
     */
    private static Lock daemonLock = new ReentrantLock();

    /**
     * Buffer size at which the buffer is flushed to the output stream.
     * Zero flushes every record (testing).
     */
    private int flushLogAt;

    /**
     * Size of the {@link StringBuilder} buffer.
     */
    private int bufSize = DEFAULT_BUFSIZE;

    private PrintStream ps;

    protected StringBuilder sb;
    /**
     * Holds time of last record written for relative time generation.
     */
    private long lastTime;
    /**
     * Set to {@code false} at start of record output, {@code true} at the end.
     * Used to handle daemon threads that are writing a record when store finalization is called.
     */
    private volatile boolean done = true;

    private boolean threadBatched;
    protected boolean perThread;

    /**
     * Non-null when per-thread stores, the associated thread.
     */
    private String threadName;

    private RepeatIdHandler repeatIdHandler;

    private PerThreadStoreOwner storeOwner;

    protected SBPSVMAIdTextStore() {
    }

    protected SBPSVMAIdTextStore(String threadName) {
        this.threadName = threadName;
    }

    private void initStaticState(boolean perThread) {
        if (storeFileDir == null) {
            timeMode = VMAOptions.getTimeMode();
            final String bsp = System.getProperty(BUFSIZE_PROPERTY);
            if (bsp != null) {
                globalBufSize = Integer.parseInt(bsp);
            }
            flushProperty = System.getProperty(FLUSH_PROPERTY);
            storeFileDir = new File(VMAStoreFile.getStoreDir());
            textKey = System.getProperty(TEXTKEY_PROPERTY) != null;
            cleanOutputDir();
            daemonLock.lock();
        }
    }

    private static void cleanOutputDir() {
        if (storeFileDir.exists()) {
            for (String fn : storeFileDir.list()) {
                if (!new File(storeFileDir, fn).delete()) {
                    System.err.println("failed to delete VMA output file: " + fn);
                }
            }
        } else {
            storeFileDir.mkdir();
        }
    }

    StringBuilder sb() {
        return sb;
    }

    @Override
    public boolean initializeStore(boolean threadBatched, boolean perThread, PerThreadStoreOwner storeOwner) {
        this.perThread = perThread;
        this.threadBatched = threadBatched;
        if (perThread) {
            assert storeOwner != null;
        }
        this.storeOwner = storeOwner;
        initStaticState(perThread);
        repeatIdHandler = RepeatIdHandler.create(perThread);
        bufSize = globalBufSize;
        flushLogAt = flushProperty != null ? 0 : bufSize - 80;
        lastTime = timeMode.getTime();
        if (!perThread) {
            return createPersistentStore(this, VMAStoreFile.GLOBAL_STORE);
        } else {
            // per-thread stores setup in defineThread
            // N.B. the main thread does get that call (soon after this)
            return true;
        }
    }

    /**
     * Creates a {@link PrintStream} and a {@link StringBuilder}.
     * @param fileName to use for store
     * @return {@code true} iff the persistent store was created ok
     */
    private static boolean createPersistentStore(SBPSVMAIdTextStore store, String fileName) {
        File file = new File(storeFileDir, fileName);
        try {
            store.ps = new PrintStream(new FileOutputStream(file));
            store.sb = new StringBuilder(store.bufSize);
            // Format log buffer with header information
            store.appendStoreHeader();
            return true;
        } catch (IOException ex) {
            System.err.println("failed to open store file " + file + ": " + ex);
            return false;
        }
    }

    @Override
    public VMATextStore newThread(String threadName) {
        if (perThread) {
            SBPSVMAIdTextStore store = createThreadStore(threadName);
            store.initializeStore(true, true, storeOwner);
            if (!createPersistentStore(store, threadName)) {
                FatalError.unexpected("failed to create per-thread VMA store");
            }
            return store;
        } else {
            return this;
        }
    }

    private void appendStoreHeader() {
        appendCode(INITIALIZE_STORE);
        appendSpace();
        sb.append(lastTime);
        appendSpace();
        sb.append(timeMode.isAbsolute());
        appendSpace();
        sb.append((threadBatched ? BATCHED : 0) | (perThread ? PER_THREAD : 0) | (textKey ? TEXT_KEY : 0));
        end();
    }

    protected abstract SBPSVMAIdTextStore createThreadStore(String threadName);

    @Override
    public void finalizeStore() {
        // Daemon threads pose problems in correctly finalizing the buffer without interleaving
        // as they continue to execute and therefore modify the buffer.
        // The following statement will block any daemon threads from starting a new record
        finalizing = true;
        // However, there may be daemon threads part way through a record
        if (perThread) {
            synchronized (storeOwner) {
                Iterator<VMAStore> allStores = storeOwner.getThreadStores();
                while (allStores.hasNext()) {
                    SBPSVMAIdTextStore store = (SBPSVMAIdTextStore) allStores.next();
                    store.waitForDaemon();
                    store.finalizeLogBuffer();
                }
            }
        } else {
            // wait for any daemon thread to finish an inflight record
            waitForDaemon();
            finalizeLogBuffer();
        }
    }

    /**
     * Wait for a daemon thread to finish an inflight record.
     * No need to synchronize as only interested in state change.
     */
    void waitForDaemon() {
        while (!done) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
            }
        }
    }

    protected void finalizeLogBuffer() {
        // Must not call appendCode else will block!
        if (textKey) {
            sb.append(FINALIZE_STORE.text);
        } else {
            sb.append(FINALIZE_STORE.code);
        }
        appendSpace();
        appendTime(timeMode.getTime());
        flushLogAt = 0; // force ps.flush
        end();
        ps.close();
    }

    /*
     * Short form support
     */
    @Override
    public void addClassShortFormDef(String name, long clId, String shortName) {
        sb.append(VMATextStoreFormat.Key.CLASS_DEFINITION.code);
        appendSpace();
        sb.append(name);
        appendSpace();
        sb.append(clId);
        appendSpace();
        sb.append(shortName);
        end();
    }

    @Override
    public void addThreadShortFormDef(String name, String shortName) {
        sb.append(VMATextStoreFormat.Key.THREAD_DEFINITION.code);
        appendSpace();
        // quote because name may contain a space
        sb.append('"');
        sb.append(name);
        sb.append('"');
        appendSpace();
        sb.append(shortName);
        end();
    }

    @Override
    public void addMemberShortFormDef(VMATextStoreFormat.Key key, String classShortForm, String name, String shortName) {
        sb.append(key.code);
        appendSpace();
        sb.append(classShortForm);
        appendSpace();
        sb.append(name);
        appendSpace();
        sb.append(shortName);
        end();
    }


    /**
     * All records start by calling this method.
     * @param key
     */
    private void appendCode(Key key) {
        if (finalizing) {
            // any daemon thread will block here
            daemonLock.lock();
        }
        done = false;
        if (textKey) {
            sb.append(key.text);
        } else {
            sb.append(key.code);
        }
    }

    protected void end() {
        sb.append('\n');
        if (sb.length()  >= flushLogAt) {
            ps.print(sb);
            ps.flush();
            sb.setLength(0);
        }
        done = true;
    }

    private void appendCheckRepeatId(long objId) {
        if (objId == REPEAT_ID_VALUE) {
            sb.append('*');
        } else {
            sb.append(objId);
        }
    }

    private void appendTime(long time) {
        if (timeMode.isAbsolute()) {
            sb.append(time);
        } else {
            sb.append(time - lastTime);
            lastTime = time;
        }
    }

    private void appendSpace() {
        sb.append(' ');
    }


    /**
     * Append the log entry key code, then the time associated with the entry, followed by the thread.
     * @param time time record generated
     * @param key
     * @param threadName (maybe null for per-thread stores)
     * @param bci byte code index
     */
    private void appendTT(long time, Key key, String threadName, int bci) {
        appendCode(key);
        appendSpace();
        appendTime(time);
        if (threadName != null) {
            appendSpace();
            sb.append(threadName);
        } else {
            assert perThread;
        }
        if (bci >= 0) {
            appendSpace();
            sb.append(bci);
        }
    }

    /**
     * As {@link #appendTT} followed by the {@code objId}.
     */
    private void appendTTId(long time, Key key, long objId, String threadName, int bci) {
        appendTT(time, key, threadName, bci);
        appendSpace();
        appendCheckRepeatId(objId);
        appendSpace();
    }

    /**
     * As {@link #appendTTId} followed by an array index.
     * @param time time record generated
     * @param key
     * @param objId
     * @param threadName
     * @param index
     */
    private void appendTTIdIndex(long time, Key key, long objId, String threadName, int bci, int index) {
        appendTT(time, key, threadName, bci);
        appendSpace();
        appendCheckRepeatId(objId);
        appendSpace();
        sb.append(index);
        appendSpace();
    }

    /**
     * Append a qualified name.
     * First append the class name and then the (short form) of the qualified name.
     * @param qualName
     */
    private void appendQualName(String className, long clId, String memberName) {
        sb.append(className);
        appendSpace();
        // clId elided as in short form of className
        sb.append(memberName);
    }

    private void appendQualId(int classId, int memberId) {
        sb.append(classId);
        appendSpace();
        sb.append(memberId);
    }

    /**
     * As {@link #appendTT} then append a class name.
     * @param time time record generated
     * @param key
     * @param className
     * @param threadName
     * @param bci bytecode index
     */
    private void appendTTC(long time, Key key, String className, String threadName, int bci) {
        appendTT(time, key, threadName, bci);
        appendSpace();
        sb.append(className);
        appendSpace();
    }

    private void appendPutFieldPrefix(long time, long objId, String memberName, String threadName, int bci) {
        appendTTId(time, ADVISE_BEFORE_PUT_FIELD, objId, threadName, bci);
        sb.append(memberName);
        appendSpace();
    }

    private void appendPutFieldPrefix(long time, long objId, int memberId, int bci) {
        appendTTId(time, ADVISE_BEFORE_PUT_FIELD, objId, null, bci);
        sb.append(memberId);
        appendSpace();
    }

    private void appendPutStaticPrefix(long time, String memberName, String threadName, int bci) {
        appendTT(time, ADVISE_BEFORE_PUT_STATIC, threadName, bci);
        appendSpace();
        sb.append(memberName);
        appendSpace();
    }

    private void appendPutStaticPrefix(long time, int memberId, int bci) {
        appendTT(time, ADVISE_BEFORE_PUT_STATIC, null, bci);
        appendSpace();
        sb.append(memberId);
        appendSpace();
    }

    private void prefixAdviseBeforeOperation(long time, String threadName, int bci, int arg1) {
        appendTT(time, ADVISE_BEFORE_OPERATION, threadName, bci);
        appendSpace();
        sb.append(arg1);
        appendSpace();
    }

    @Override
    public void removal(long id) {
        appendCode(REMOVAL);
        appendSpace();
        sb.append(id);
        end();
    }

    public long checkRepeatId(long objId, String threadName) {
        return repeatIdHandler.checkRepeatId(objId, threadName);
    }

    @Override
    public void threadSwitch(long time, String threadName) {
        if (!perThread) {
            appendCode(THREAD_SWITCH);
            appendSpace();
            lastTime = time;
            sb.append(lastTime);
            end();
        }
    }

    @Override
    public void unseenObject(long time, String threadName, int bci, long objId, String shortClassName) {
        // There is no "bci" field for this, but we pass zero so that the format of the record is
        // the same as that for a NEW etc.
        appendTTId(time, UNSEEN, objId, threadName, bci);
        sb.append(shortClassName);
        end();
    }

    @Override
    public void adviseAfterGC(long time, String threadName) {
        appendTT(time, ADVISE_AFTER_GC, threadName, -1);
        end();
    }

    @Override
    public void adviseBeforeThreadStarting(long time, String threadName) {
        appendTT(time, ADVISE_BEFORE_THREAD_STARTING, threadName, -1);
        end();
    }

    @Override
    public void adviseBeforeThreadTerminating(long time, String threadName) {
        appendTT(time, ADVISE_BEFORE_THREAD_TERMINATING, threadName, -1);
        end();
        // TODO finalizeLogBuffer?
    }

    @Override
    public void adviseBeforeGetStatic(long time, String threadName, int bci, String shortFieldName) {
        appendTT(time, ADVISE_BEFORE_GET_STATIC, threadName, bci);
        appendSpace();
        sb.append(shortFieldName);
        end();
    }

    @Override
    public void adviseBeforePutStatic(long time, String threadName, int bci, String shortFieldName, double value) {
        appendPutStaticPrefix(time, shortFieldName, threadName, bci);
        sb.append(DOUBLE_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public void adviseBeforePutStatic(long time, String threadName, int bci, String shortFieldName, long value) {
        appendPutStaticPrefix(time, shortFieldName, threadName, bci);
        sb.append(LONG_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public void adviseBeforePutStatic(long time, String threadName, int bci, String shortFieldName, float value) {
        appendPutStaticPrefix(time, shortFieldName, threadName, bci);
        sb.append(FLOAT_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public void adviseBeforePutStaticObject(long time, String threadName, int bci, String shortFieldName, long value) {
        appendPutStaticPrefix(time, shortFieldName, threadName, bci);
        sb.append(OBJ_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public void adviseBeforeGetField(long time, String threadName, int bci, long objId, String shortFieldName) {
        appendTTId(time, ADVISE_BEFORE_GET_FIELD, objId, threadName, bci);
        sb.append(shortFieldName);
        end();
    }

    @Override
    public void adviseBeforePutField(long time, String threadName, int bci, long objId, String shortFieldName, long value) {
        appendPutFieldPrefix(time, objId, shortFieldName, threadName, bci);
        sb.append(LONG_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public void adviseBeforePutField(long time, String threadName, int bci, long objId, String shortFieldName, float value) {
        appendPutFieldPrefix(time, objId, shortFieldName, threadName, bci);
        sb.append(FLOAT_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public void adviseBeforePutField(long time, String threadName, int bci, long objId, String shortFieldName, double value) {
        appendPutFieldPrefix(time, objId, shortFieldName, threadName, bci);
        sb.append(DOUBLE_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public void adviseBeforePutFieldObject(long time, String threadName, int bci, long objId, String shortFieldName, long value) {
        appendPutFieldPrefix(time, objId, shortFieldName, threadName, bci);
        sb.append(LONG_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public void adviseBeforeArrayLoad(long time, String threadName, int bci, long objId, int index) {
        appendTTIdIndex(time, ADVISE_BEFORE_ARRAY_LOAD, objId, threadName, bci, index);
        end();
    }

    @Override
    public void adviseBeforeArrayStore(long time, String threadName, int bci, long objId, int index, float value) {
        appendTTIdIndex(time, ADVISE_BEFORE_ARRAY_STORE, objId, threadName, bci, index);
        sb.append(FLOAT_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public void adviseBeforeArrayStore(long time, String threadName, int bci, long objId, int index, long value) {
        appendTTIdIndex(time, ADVISE_BEFORE_ARRAY_STORE, objId, threadName, bci, index);
        sb.append(LONG_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public void adviseBeforeArrayStore(long time, String threadName, int bci, long objId, int index, double value) {
        appendTTIdIndex(time, ADVISE_BEFORE_ARRAY_STORE, objId, threadName, bci, index);
        sb.append(DOUBLE_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public void adviseBeforeArrayStoreObject(long time, String threadName, int bci, long objId, int index, long valueId) {
        appendTTIdIndex(time, ADVISE_BEFORE_ARRAY_STORE, objId, threadName, bci, index);
        sb.append(OBJ_VALUE);
        appendSpace();
        sb.append(valueId);
        end();
    }

    @Override
    public void adviseAfterArrayLoadObject(long time, String threadName, int bci, long objId, int index, long valueId) {
        appendTTIdIndex(time, ADVISE_AFTER_ARRAY_LOAD, objId, threadName, bci, index);
        sb.append(OBJ_VALUE);
        appendSpace();
        sb.append(valueId);
        end();
    }

    @Override
    public void adviseAfterNew(long time, String threadName, int bci, long objId, String shortClassName) {
        appendTTId(time, ADVISE_AFTER_NEW, objId, threadName, bci);
        sb.append(shortClassName);
        end();
    }

    @Override
    public void adviseAfterNewArray(long time, String threadName, int bci, long objId, String shortClassName, int length) {
        appendTTId(time, ADVISE_AFTER_NEW_ARRAY, objId, threadName, bci);
        sb.append(shortClassName);
        appendSpace();
        sb.append(length);
        end();
    }

    @Override
    public void adviseAfterMultiNewArray(long time, String threadName, int bci, long objId, String shortClassName, int length) {
        // MultiArrays are explicitly handled by multiple calls to adviseAfterNewArray so we just
        // log the top level array.
        adviseAfterNewArray(time, threadName, bci, objId, shortClassName, length);
    }

    @Override
    public void adviseBeforeGC(long time, String threadName) {
        appendTT(time, ADVISE_BEFORE_GC, threadName, -1);
        end();
    }

    @Override
    public void adviseBeforeConstLoad(long time, String threadName, int bci, long value) {
        appendTT(time, ADVISE_BEFORE_CONST_LOAD, threadName, bci);
        appendSpace();
        sb.append(LONG_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public void adviseBeforeConstLoadObject(long time, String threadName, int bci, long value) {
        appendTT(time, ADVISE_BEFORE_CONST_LOAD, threadName, bci);
        appendSpace();
        sb.append(OBJ_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public void adviseBeforeConstLoad(long time, String threadName, int bci, float value) {
        appendTT(time, ADVISE_BEFORE_CONST_LOAD, threadName, bci);
        appendSpace();
        sb.append(FLOAT_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public void adviseBeforeConstLoad(long time, String threadName, int bci, double value) {
        appendTT(time, ADVISE_BEFORE_CONST_LOAD, threadName, bci);
        appendSpace();
        sb.append(DOUBLE_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public void adviseBeforeLoad(long time, String threadName, int bci, int dispToLocalSlot) {
        appendTT(time, ADVISE_BEFORE_LOAD, threadName, bci);
        appendSpace();
        sb.append(dispToLocalSlot);
        end();
    }

    @Override
    public void adviseBeforeStore(long time, String threadName, int bci, int dispToLocalSlot, long value) {
        appendTT(time, ADVISE_BEFORE_STORE, threadName, bci);
        appendSpace();
        sb.append(dispToLocalSlot);
        appendSpace();
        sb.append(LONG_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public void adviseBeforeStore(long time, String threadName, int bci, int dispToLocalSlot, float value) {
        appendTT(time, ADVISE_BEFORE_STORE, threadName, bci);
        appendSpace();
        sb.append(dispToLocalSlot);
        appendSpace();
        sb.append(FLOAT_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public void adviseBeforeStore(long time, String threadName, int bci, int dispToLocalSlot, double value) {
        appendTT(time, ADVISE_BEFORE_STORE, threadName, bci);
        appendSpace();
        sb.append(dispToLocalSlot);
        appendSpace();
        sb.append(DOUBLE_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public void adviseBeforeStoreObject(long time, String threadName, int bci, int dispToLocalSlot, long value) {
        appendTT(time, ADVISE_BEFORE_STORE, threadName, bci);
        appendSpace();
        sb.append(dispToLocalSlot);
        appendSpace();
        sb.append(OBJ_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public void adviseAfterLoadObject(long time, String threadName, int bci, int dispToLocalSlot, long value) {
        appendTT(time, ADVISE_AFTER_LOAD, threadName, bci);
        appendSpace();
        sb.append(dispToLocalSlot);
        appendSpace();
        sb.append(OBJ_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public void adviseBeforeStackAdjust(long time, String threadName, int bci, int arg1) {
        appendTT(time, ADVISE_BEFORE_STACK_ADJUST, threadName, bci);
        appendSpace();
        sb.append(arg1);
        end();
    }

    @Override
    public void adviseBeforeOperation(long time, String threadName, int bci, int arg1, long arg2, long arg3) {
        prefixAdviseBeforeOperation(time, threadName, bci, arg1);
        sb.append(LONG_VALUE);
        appendSpace();
        sb.append(arg2);
        appendSpace();
        sb.append(arg3);
        end();
    }

    @Override
    public void adviseBeforeOperation(long time, String threadName, int bci, int arg1, float arg2, float arg3) {
        prefixAdviseBeforeOperation(time, threadName, bci, arg1);
        sb.append(FLOAT_VALUE);
        appendSpace();
        sb.append(arg2);
        appendSpace();
        sb.append(arg3);
        end();
    }

    @Override
    public void adviseBeforeOperation(long time, String threadName, int bci, int arg1, double arg2, double arg3) {
        prefixAdviseBeforeOperation(time, threadName, bci, arg1);
        sb.append(DOUBLE_VALUE);
        appendSpace();
        sb.append(arg2);
        appendSpace();
        sb.append(arg3);
        end();
    }

    @Override
    public void adviseBeforeConversion(long time, String threadName, int bci, int arg1, long arg2) {
        appendTT(time, ADVISE_BEFORE_CONVERSION, threadName, bci);
        appendSpace();
        sb.append(arg1);
        appendSpace();
        sb.append(LONG_VALUE);
        appendSpace();
        sb.append(arg2);
        end();
    }

    @Override
    public void adviseBeforeConversion(long time, String threadName, int bci, int arg1, float arg2) {
        appendTT(time, ADVISE_BEFORE_CONVERSION, threadName, bci);
        appendSpace();
        sb.append(arg1);
        appendSpace();
        sb.append(FLOAT_VALUE);
        appendSpace();
        sb.append(arg2);
        end();
    }

    @Override
    public void adviseBeforeConversion(long time, String threadName, int bci, int arg1, double arg2) {
        appendTT(time, ADVISE_BEFORE_CONVERSION, threadName, bci);
        appendSpace();
        sb.append(arg1);
        appendSpace();
        sb.append(DOUBLE_VALUE);
        appendSpace();
        sb.append(arg2);
        end();
    }

    @Override
    public void adviseBeforeIf(long time, String threadName, int bci, int opcode, int op1, int op2, int branchOffset) {
        appendTT(time, ADVISE_BEFORE_IF, threadName, bci);
        appendSpace();
        sb.append(opcode);
        appendSpace();
        sb.append(LONG_VALUE);
        appendSpace();
        sb.append(op1);
        appendSpace();
        sb.append(op2);
        appendSpace();
        sb.append(branchOffset);
        end();
    }

    @Override
    public void adviseBeforeIfObject(long time, String threadName, int bci, int opcode, long objId1, long objId2, int branchOffset) {
        appendTT(time, ADVISE_BEFORE_IF, threadName, bci);
        appendSpace();
        sb.append(opcode);
        appendSpace();
        sb.append(OBJ_VALUE);
        appendSpace();
        sb.append(objId1);
        appendSpace();
        sb.append(objId2);
        appendSpace();
        sb.append(branchOffset);
        end();
    }

    @Override
    public void adviseBeforeGoto(long time, String threadName, int bci, int branchOffset) {
        appendTT(time, ADVISE_BEFORE_GOTO, threadName, bci);
        appendSpace();
        sb.append(branchOffset);
        end();
    }

    @Override
    public void adviseBeforeReturnObject(long time, String threadName, int bci, long value) {
        appendTT(time, ADVISE_BEFORE_RETURN, threadName, bci);
        appendSpace();
        sb.append(OBJ_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public void adviseBeforeReturn(long time, String threadName, int bci, long value) {
        appendTT(time, ADVISE_BEFORE_RETURN, threadName, bci);
        appendSpace();
        sb.append(LONG_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public void adviseBeforeReturn(long time, String threadName, int bci, float value) {
        appendTT(time, ADVISE_BEFORE_RETURN, threadName, bci);
        appendSpace();
        sb.append(FLOAT_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public void adviseBeforeReturn(long time, String threadName, int bci, double value) {
        appendTT(time, ADVISE_BEFORE_RETURN, threadName, bci);
        appendSpace();
        sb.append(DOUBLE_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public void adviseBeforeReturn(long time, String threadName, int bci) {
        appendTT(time, ADVISE_BEFORE_RETURN, threadName, bci);
        end();
    }

    @Override
    public void adviseBeforeInvokeVirtual(long time, String threadName, int bci, long objId, String shortMethodName) {
        appendTTId(time, ADVISE_BEFORE_INVOKE_VIRTUAL, objId, threadName, bci);
        sb.append(shortMethodName);
        end();
    }

    @Override
    public void adviseBeforeInvokeSpecial(long time, String threadName, int bci, long objId, String shortMethodName) {
        appendTTId(time, ADVISE_BEFORE_INVOKE_SPECIAL, objId, threadName, bci);
        sb.append(shortMethodName);
        end();
    }

    @Override
    public void adviseBeforeInvokeStatic(long time, String threadName, int bci, long objId, String shortMethodName) {
        appendTTId(time, ADVISE_BEFORE_INVOKE_STATIC, objId, threadName, bci);
        sb.append(shortMethodName);
        end();
    }

    @Override
    public void adviseBeforeInvokeInterface(long time, String threadName, int bci, long objId, String shortMethodName) {
        appendTTId(time, ADVISE_BEFORE_INVOKE_INTERFACE, objId, threadName, bci);
        sb.append(shortMethodName);
        end();
    }

    @Override
    public void adviseAfterArrayLength(long time, String threadName, int bci, long objId, int length) {
        appendTTId(time, ADVISE_BEFORE_ARRAY_LENGTH, objId, threadName, bci);
        sb.append(length);
        end();
    }

    @Override
    public void adviseBeforeThrow(long time, String threadName, int bci, long objId) {
        appendTTId(time, ADVISE_BEFORE_THROW, objId, threadName, bci);
        end();
    }

    @Override
    public void adviseBeforeCheckCast(long time, String threadName, int bci, long objId, String shortClassName) {
        appendTTId(time, ADVISE_BEFORE_CHECK_CAST, objId, threadName, bci);
        sb.append(shortClassName);
        end();
    }

    @Override
    public void adviseBeforeInstanceOf(long time, String threadName, int bci, long objId, String shortClassName) {
        appendTTId(time, ADVISE_BEFORE_INSTANCE_OF, objId, threadName, bci);
        sb.append(shortClassName);
        end();
    }

    @Override
    public void adviseBeforeMonitorEnter(long time, String threadName, int bci, long objId) {
        appendTTId(time, ADVISE_BEFORE_MONITOR_ENTER, objId, threadName, bci);
        end();
    }

    @Override
    public void adviseBeforeMonitorExit(long time, String threadName, int bci, long objId) {
        appendTTId(time, ADVISE_BEFORE_MONITOR_EXIT, objId, threadName, bci);
        end();
    }

    @Override
    public void adviseAfterMethodEntry(long time, String threadName, int bci, long objId, String shortMethodName) {
        appendTTId(time, ADVISE_AFTER_METHOD_ENTRY, objId, threadName, bci);
        sb.append(shortMethodName);
        end();
    }
    @Override
    public void adviseBeforeReturnByThrow(long time, String threadName, int bci, long objId, int poppedFrames) {
        appendTTId(time, ADVISE_BEFORE_RETURN_BY_THROW, objId, threadName, bci);
        sb.append(poppedFrames);
        end();

    }

    @Override
    public void unseenObject(long time, int bci, long objId, int classId) {
        appendTTId(time, UNSEEN, objId, null, bci);
        sb.append(classId);
        end();
    }

    @Override
    public void adviseAfterNew(long time, int bci, long objId, int classId) {
        appendTTId(time, ADVISE_AFTER_NEW, objId, null, bci);
        sb.append(classId);
        end();
    }

    @Override
    public void adviseAfterNewArray(long time, int bci, long objId, int classId, int length) {
        appendTTId(time, ADVISE_AFTER_NEW_ARRAY, objId, null, bci);
        sb.append(classId);
        appendSpace();
        sb.append(length);
        end();
    }

    @Override
    public void adviseAfterMultiNewArray(long time, int bci, long objId, int classId, int length) {
        adviseAfterNewArray(time, bci, objId, classId, length);
    }

    @Override
    public void adviseBeforeGetStatic(long time, int bci, int fieldId) {
        appendTT(time, ADVISE_BEFORE_GET_STATIC, null, bci);
        appendSpace();
        sb.append(fieldId);
        end();
    }

    @Override
    public void adviseBeforePutStatic(long time, int bci, int fieldId, float value) {
        appendPutStaticPrefix(time, fieldId, bci);
        sb.append(FLOAT_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public void adviseBeforePutStatic(long time, int bci, int fieldId, double value) {
        appendPutStaticPrefix(time, fieldId, bci);
        sb.append(DOUBLE_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public void adviseBeforePutStatic(long time, int bci, int fieldId, long value) {
        appendPutStaticPrefix(time, fieldId, bci);
        sb.append(LONG_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public void adviseBeforePutStaticObject(long time, int bci, int fieldId, long value) {
        appendPutStaticPrefix(time, fieldId, bci);
        sb.append(OBJ_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public void adviseBeforeGetField(long time, int bci, long objId, int fieldId) {
        appendTTId(time, ADVISE_BEFORE_GET_FIELD, objId, null, bci);
        sb.append(fieldId);
        end();
    }

    @Override
    public void adviseBeforePutField(long time, int bci, long objId, int fieldId, float value) {
        appendPutFieldPrefix(time, objId, fieldId, bci);
        sb.append(FLOAT_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public void adviseBeforePutField(long time, int bci, long objId, int fieldId, long value) {
        appendPutFieldPrefix(time, objId, fieldId, bci);
        sb.append(LONG_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public void adviseBeforePutField(long time, int bci, long objId, int fieldId, double value) {
        appendPutFieldPrefix(time, objId, fieldId, bci);
        sb.append(DOUBLE_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public void adviseBeforePutFieldObject(long time, int bci, long objId, int fieldId, long value) {
        appendPutFieldPrefix(time, objId, fieldId, bci);
        sb.append(OBJ_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public void adviseBeforeInvokeVirtual(long time, int bci, long objId, int methodId) {
        appendTTId(time, ADVISE_BEFORE_INVOKE_VIRTUAL, objId, null, bci);
        sb.append(methodId);
        end();
    }

    @Override
    public void adviseBeforeInvokeSpecial(long time, int bci, long objId, int methodId) {
        appendTTId(time, ADVISE_BEFORE_INVOKE_SPECIAL, objId, null, bci);
        sb.append(methodId);
        end();
    }

    @Override
    public void adviseBeforeInvokeStatic(long time, int bci, long objId, int methodId) {
        appendTTId(time, ADVISE_BEFORE_INVOKE_STATIC, objId, null, bci);
        sb.append(methodId);
        end();
    }

    @Override
    public void adviseBeforeInvokeInterface(long time, int bci, long objId, int methodId) {
        appendTTId(time, ADVISE_BEFORE_INVOKE_INTERFACE, objId, null, bci);
        sb.append(methodId);
        end();
    }

    @Override
    public void adviseBeforeCheckCast(long time, int bci, long objId, int classId) {
        appendTTId(time, ADVISE_BEFORE_CHECK_CAST, objId, null, bci);
        sb.append(classId);
        end();
    }

    @Override
    public void adviseBeforeInstanceOf(long time, int bci, long objId, int classId) {
        appendTTId(time, ADVISE_BEFORE_INSTANCE_OF, objId, null, bci);
        sb.append(classId);
        end();
    }

    @Override
    public void adviseAfterMethodEntry(long time, int bci, long objId, int methodId) {
        appendTTId(time, ADVISE_AFTER_METHOD_ENTRY, objId, null, bci);
        sb.append(methodId);
        end();
    }



}

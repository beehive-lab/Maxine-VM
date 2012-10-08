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
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

import static com.oracle.max.vm.ext.vma.store.txt.CVMATextStore.Key.*;

import com.oracle.max.vm.ext.vma.store.*;
import com.oracle.max.vm.ext.vma.store.txt.*;
import com.oracle.max.vm.ext.vma.store.txt.CSFVMATextStore.*;
import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.vm.runtime.*;

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
 * However, it is now dependent on {@link CSFVMATextStore} and subclasses it. For example
 * the optimization on classloader id only works with short forms. In any event, for serious
 * use short forms are essential.
 *
 * This class is unsynchronized for use in per-thread mode. For a shared store accessed by multiple-threads use
 * the {@link SBPSLockedVMATextStore} subclass which synchronizes and then invokes the methods
 * in this class.
 *
 * In per-thread mode each thread has its own buffer and log file.
 * The log file name is used as a stem and each thread's file is named by suffixing with its id, which
 * will be the short form created by {@link CSFVMATextStore}. The file/stream/buffer
 * is created in {@link #defineThread} which may, due to the use of short forms, be called
 * before {@link #adviseBeforeThreadStarting(long, String)}.
 */
public class SBPSVMATextStore extends CSFVMATextStore {

    /**
     * Subclass used for per-thread mode.
     */
    private static class ThreadSBPSVMATextStore extends SBPSVMATextStore {
        final String shortThreadName;
        ThreadSBPSVMATextStore(String shortThreadName) {
            this.shortThreadName = shortThreadName;
        }
    }

    private static final String FLUSH_PROPERTY = "max.vma.storeflush";
    private static final String BUFSIZE_PROPERTY = "max.vma.storebufsize";
    private static final String ABSTIME_PROPERTY = "max.vma.abstime";
    private static final int DEFAULT_BUFSIZE = 1024 * 1024;

    /**
     * {@code true} iff storing absolute time.
     */
    @CONSTANT_WHEN_NOT_ZERO
    private static boolean absTimeFlag;

    @CONSTANT_WHEN_NOT_ZERO
    private static File storeFileDir;

    @CONSTANT_WHEN_NOT_ZERO
    private static int globalBufSize = DEFAULT_BUFSIZE;

    @CONSTANT_WHEN_NOT_ZERO
    private static String flushProperty;

    /**
     * Map from thread short form names to associated store; only when {@link #perThread}.
     */
    private static ConcurrentMap<String, ThreadSBPSVMATextStore> storeMap;

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
    private StringBuilder sb;
    /**
     * Holds time of last record written for relative time generation.
     */
    private long lastTime;
    /**
     * Set to {@code false} at start of record output, {@code true} at the end.
     * Used to handle daemon threads that are writing a record when store finalization is called.
     */
    private volatile boolean done = true;

    @Override
    protected void initStaticState(boolean perThread) {
        if (storeFileDir == null) {
            absTimeFlag = System.getProperty(ABSTIME_PROPERTY) != null;
            final String bsp = System.getProperty(BUFSIZE_PROPERTY);
            if (bsp != null) {
                globalBufSize = Integer.parseInt(bsp);
            }
            flushProperty = System.getProperty(FLUSH_PROPERTY);
            storeFileDir = new File(VMAStoreFile.getStoreDir());
            cleanOutputDir();
            daemonLock.lock();
            if (perThread) {
                storeMap = new ConcurrentHashMap<String, ThreadSBPSVMATextStore>();
            }
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
    public boolean initializeStore(boolean threadBatched, boolean perThread) {
        super.initializeStore(threadBatched, perThread);
        initStaticState(perThread);
        bufSize = globalBufSize;
        flushLogAt = flushProperty != null ? 0 : bufSize - 80;
        lastTime = System.nanoTime();
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
    private static boolean createPersistentStore(SBPSVMATextStore store, String fileName) {
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

    private void appendStoreHeader() {
        appendCode(INITIALIZE_STORE);
        appendSpace();
        sb.append(lastTime);
        appendSpace();
        sb.append(absTimeFlag);
        appendSpace();
        sb.append((threadBatched ? BATCHED : 0) | (perThread ? PER_THREAD : 0));
        end();
    }

    @Override
    public VMATextStore newThread(String threadName) {
        // causes a call to defineThread
        String shortThreadName = getThreadShortForm(threadName);
        if (perThread) {
            ThreadSBPSVMATextStore result = storeMap.get(shortThreadName);
            return result;
        } else {
            return this;
        }
    }

    /**
     * Must create the per-thread buffer before the short form for a thread is defined.
     * @param shortThreadName
     */
    private SBPSVMATextStore defineThread(String shortThreadName) {
        if (perThread) {
            ThreadSBPSVMATextStore store = new ThreadSBPSVMATextStore(shortThreadName);
            store.initializeStore(true, true);
            if (!createPersistentStore(store, shortThreadName)) {
                FatalError.unexpected("failed to create per-thread VMA store");
            }
            assert storeMap.put(shortThreadName, store) == null;
            return store;
        } else {
            return this;
        }
    }

    @Override
    protected void defineShortForm(CSFVMATextStore.ShortForm type, Object key, String shortForm, String classShortForm) {
        ClassNameId className = null;
        SBPSVMATextStore store = this;

        if (type == ShortForm.T) {
            // This is where we first find out about a new thread, when creating the short form in adviseBeoreThreadStarting.
            // If we are in per-thread mode, we continue with the returned thread-specific store.
            store = defineThread(shortForm);
        }
        store.sb.append(type.code);
        store.sb.append(' ');
        if (type == ShortForm.C) {
            className = (ClassNameId) key;
            store.sb.append(className.name);
            store.sb.append(' ');
            store.sb.append(className.clId);
        } else if (type == ShortForm.T) {
            // quote because name may contain a space
            store.sb.append('"');
            store.sb.append(key);
            store.sb.append('"');
        } else {
            // F/M
            QualName qualName = (QualName) key;
            // guaranteed to have already created the short form for the class name
            store.sb.append(classShortForm);
            store.sb.append(' ');
            store.sb.append(qualName.name);
        }
        store.sb.append(' ');
        store.sb.append(shortForm);
        store.end();
    }

    @Override
    public void finalizeStore() {
        // Daemon threads pose problems in correctly finalizing the buffer without interleaving
        // as they continue to execute and therefore modify the buffer.
        // The following statement will block any daemon threads from starting a new record
        finalizing = true;
        // However, there may be daemon threads part way through a record
        if (perThread) {
            for (SBPSVMATextStore store : storeMap.values()) {
                store.waitForDaemon();
                store.finalizeLogBuffer();
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
        sb.append(FINALIZE_STORE.code);
        appendSpace();
        sb.append(System.nanoTime());
        flushLogAt = 0; // force ps.flush
        end();
        ps.close();
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
        sb.append(key.code);
    }

    void end() {
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
        if (absTimeFlag) {
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
     * @param time time record generated
     * @param key
     * @param objId
     * @param threadName
     * @param bci TODO
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

    private void appendPutFieldPrefix(long time, long objId, String className, long clId, String memberName, String threadName, int bci) {
        appendTTId(time, ADVISE_BEFORE_PUT_FIELD, objId, threadName, bci);
        appendQualName(className, clId, memberName);
        appendSpace();
    }

    private void appendPutStaticPrefix(long time, String className, long clId, String memberName, String threadName, int bci) {
        appendTT(time, ADVISE_BEFORE_PUT_STATIC, threadName, bci);
        appendSpace();
        appendQualName(className, clId, memberName);
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

    @Override
    public void unseenObject(long time, String threadName, long objId, String className, long clId) {
        store_unseenObject(time, getThreadShortForm(threadName), checkRepeatId(objId, threadName), getClassShortForm(className, clId), clId);
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

    public void store_unseenObject(long time, String threadName, long objId, String className, long clId) {
        // There is no "bci" field for this, but we pass zero so that the format of the record is
        // the same as that for a NEW etc.
        appendTTId(time, UNSEEN, objId, threadName, 0);
        sb.append(className);
        // clId elided
        end();
    }

    private void store_adviseAfterGC(long time, String threadName) {
        appendTT(time, ADVISE_AFTER_GC, threadName, -1);
        end();
    }

    private void store_adviseBeforeThreadStarting(long time, String threadName) {
        appendTT(time, ADVISE_BEFORE_THREAD_STARTING, threadName, -1);
        end();
    }

    private void store_adviseBeforeThreadTerminating(long time, String threadName) {
        appendTT(time, ADVISE_BEFORE_THREAD_TERMINATING, threadName, -1);
        end();
        // TODO finalizeLogBuffer?
    }

    private void store_adviseBeforeGetStatic(long time, String threadName, int bci, String className, long clId, String memberName) {
        appendTT(time, ADVISE_BEFORE_GET_STATIC, threadName, bci);
        appendSpace();
        appendQualName(className, clId, memberName);
        end();
    }

    private void store_adviseBeforePutStatic(long time, String threadName, int bci, String className, long clId, String memberName, double value) {
        appendPutStaticPrefix(time, className, clId, memberName, threadName, bci);
        sb.append(DOUBLE_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    private void store_adviseBeforePutStatic(long time, String threadName, int bci, String className, long clId, String memberName, long value) {
        appendPutStaticPrefix(time, className, clId, memberName, threadName, bci);
        sb.append(LONG_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    private void store_adviseBeforePutStatic(long time, String threadName, int bci, String className, long clId, String memberName, float value) {
        appendPutStaticPrefix(time, className, clId, memberName, threadName, bci);
        sb.append(FLOAT_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    private void store_adviseBeforePutStaticObject(long time, String threadName, int bci, String className, long clId, String memberName, long valueId) {
        appendPutStaticPrefix(time, className, clId, memberName, threadName, bci);
        sb.append(OBJ_VALUE);
        appendSpace();
        sb.append(valueId);
        end();
    }

    private void store_adviseBeforeGetField(long time, String threadName, int bci, long objId, String className, long clId, String memberName) {
        appendTTId(time, ADVISE_BEFORE_GET_FIELD, objId, threadName, bci);
        appendQualName(className, clId, memberName);
        end();
    }

    private void store_adviseBeforePutField(long time, String threadName, int bci, long objId, String className, long clId, String memberName, double value) {
        appendPutFieldPrefix(time, objId, className, clId, memberName, threadName, bci);
        sb.append(DOUBLE_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    private void store_adviseBeforePutField(long time, String threadName, int bci, long objId, String className, long clId, String memberName, long value) {
        appendPutFieldPrefix(time, objId, className, clId, memberName, threadName, bci);
        sb.append(LONG_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    private void store_adviseBeforePutField(long time, String threadName, int bci, long objId, String className, long clId, String memberName, float value) {
        appendPutFieldPrefix(time, objId, className, clId, memberName, threadName, bci);
        sb.append(FLOAT_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    private void store_adviseBeforePutFieldObject(long time, String threadName, int bci, long objId, String className, long clId, String memberName, long valueId) {
        appendPutFieldPrefix(time, objId, className, clId, memberName, threadName, bci);
        sb.append(OBJ_VALUE);
        appendSpace();
        sb.append(valueId);
        end();
    }

    private void store_adviseBeforeArrayLoad(long time, String threadName, int bci, long objId, int index) {
        appendTTIdIndex(time, ADVISE_BEFORE_ARRAY_LOAD, objId, threadName, bci, index);
        end();
    }

    private void store_adviseBeforeArrayStore(long time, String threadName, int bci, long objId, int index, float value) {
        appendTTIdIndex(time, ADVISE_BEFORE_ARRAY_STORE, objId, threadName, bci, index);
        sb.append(FLOAT_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    private void store_adviseBeforeArrayStore(long time, String threadName, int bci, long objId, int index, long value) {
        appendTTIdIndex(time, ADVISE_BEFORE_ARRAY_STORE, objId, threadName, bci, index);
        sb.append(LONG_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    private void store_adviseBeforeArrayStore(long time, String threadName, int bci, long objId, int index, double value) {
        appendTTIdIndex(time, ADVISE_BEFORE_ARRAY_STORE, objId, threadName, bci, index);
        sb.append(DOUBLE_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    private void store_adviseBeforeArrayStoreObject(long time, String threadName, int bci, long objId, int index, long valueId) {
        appendTTIdIndex(time, ADVISE_BEFORE_ARRAY_STORE, objId, threadName, bci, index);
        sb.append(OBJ_VALUE);
        appendSpace();
        sb.append(valueId);
        end();
    }

    private void store_adviseAfterArrayLoadObject(long time, String threadName, int bci, long objId, int index, long valueId) {
        appendTTIdIndex(time, ADVISE_AFTER_ARRAY_LOAD, objId, threadName, bci, index);
        sb.append(OBJ_VALUE);
        appendSpace();
        sb.append(valueId);
        end();
    }

    private void store_adviseAfterNew(long time, String threadName, int bci, long objId, String className, long clId) {
        appendTTId(time, ADVISE_AFTER_NEW, objId, threadName, bci);
        sb.append(className);
        end();
    }

    private void store_adviseAfterNewArray(long time, String threadName, int bci, long objId, String className, long clId, int length) {
        appendTTId(time, ADVISE_AFTER_NEW_ARRAY, objId, threadName, bci);
        sb.append(className);
        appendSpace();
        sb.append(length);
        end();
    }

    private void store_adviseAfterMultiNewArray(long time, String threadName, int bci, long objId, String className, long clId, int length) {
        // MultiArrays are explicitly handled by multiple calls to adviseAfterNewArray so we just
        // log the top level array.
        adviseAfterNewArray(time, threadName, bci, objId, className, clId, length);
    }

    private void store_adviseBeforeGC(long time, String threadName) {
        appendTT(time, ADVISE_BEFORE_GC, threadName, -1);
        end();
    }

    private void store_adviseBeforeConstLoad(long time, String threadName, int bci, long value) {
        appendTT(time, ADVISE_BEFORE_CONST_LOAD, threadName, bci);
        appendSpace();
        sb.append(LONG_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    private void store_adviseBeforeConstLoadObject(long time, String threadName, int bci, long value) {
        appendTT(time, ADVISE_BEFORE_CONST_LOAD, threadName, bci);
        appendSpace();
        sb.append(OBJ_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    private void store_adviseBeforeConstLoad(long time, String threadName, int bci, float value) {
        appendTT(time, ADVISE_BEFORE_CONST_LOAD, threadName, bci);
        appendSpace();
        sb.append(FLOAT_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    private void store_adviseBeforeConstLoad(long time, String threadName, int bci, double value) {
        appendTT(time, ADVISE_BEFORE_CONST_LOAD, threadName, bci);
        appendSpace();
        sb.append(DOUBLE_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    private void store_adviseBeforeLoad(long time, String threadName, int bci, int dispToLocalSlot) {
        appendTT(time, ADVISE_BEFORE_LOAD, threadName, bci);
        appendSpace();
        sb.append(dispToLocalSlot);
        end();
    }

    private void store_adviseBeforeStore(long time, String threadName, int bci, int dispToLocalSlot, long value) {
        appendTT(time, ADVISE_BEFORE_STORE, threadName, bci);
        appendSpace();
        sb.append(dispToLocalSlot);
        appendSpace();
        sb.append(LONG_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    private void store_adviseBeforeStore(long time, String threadName, int bci, int dispToLocalSlot, float value) {
        appendTT(time, ADVISE_BEFORE_STORE, threadName, bci);
        appendSpace();
        sb.append(dispToLocalSlot);
        appendSpace();
        sb.append(FLOAT_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    private void store_adviseBeforeStore(long time, String threadName, int bci, int dispToLocalSlot, double value) {
        appendTT(time, ADVISE_BEFORE_STORE, threadName, bci);
        appendSpace();
        sb.append(dispToLocalSlot);
        appendSpace();
        sb.append(DOUBLE_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    private void store_adviseBeforeStoreObject(long time, String threadName, int bci, int dispToLocalSlot, long value) {
        appendTT(time, ADVISE_BEFORE_STORE, threadName, bci);
        appendSpace();
        sb.append(dispToLocalSlot);
        appendSpace();
        sb.append(OBJ_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    private void store_adviseAfterLoadObject(long time, String threadName, int bci, int dispToLocalSlot, long value) {
        appendTT(time, ADVISE_AFTER_LOAD, threadName, bci);
        appendSpace();
        sb.append(dispToLocalSlot);
        appendSpace();
        sb.append(OBJ_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    private void store_adviseBeforeStackAdjust(long time, String threadName, int bci, int arg1) {
        appendTT(time, ADVISE_BEFORE_STACK_ADJUST, threadName, bci);
        appendSpace();
        sb.append(arg1);
        end();
    }

    private void store_adviseBeforeOperation(long time, String threadName, int bci, int arg1, long arg2, long arg3) {
        prefixAdviseBeforeOperation(time, threadName, bci, arg1);
        sb.append(LONG_VALUE);
        appendSpace();
        sb.append(arg2);
        appendSpace();
        sb.append(arg3);
        end();
    }

    private void store_adviseBeforeOperation(long time, String threadName, int bci, int arg1, float arg2, float arg3) {
        prefixAdviseBeforeOperation(time, threadName, bci, arg1);
        sb.append(FLOAT_VALUE);
        appendSpace();
        sb.append(arg2);
        appendSpace();
        sb.append(arg3);
        end();
    }

    private void store_adviseBeforeOperation(long time, String threadName, int bci, int arg1, double arg2, double arg3) {
        prefixAdviseBeforeOperation(time, threadName, bci, arg1);
        sb.append(DOUBLE_VALUE);
        appendSpace();
        sb.append(arg2);
        appendSpace();
        sb.append(arg3);
        end();
    }

    private void store_adviseBeforeConversion(long time, String threadName, int bci, int arg1, long arg2) {
        appendTT(time, ADVISE_BEFORE_CONVERSION, threadName, bci);
        appendSpace();
        sb.append(arg1);
        appendSpace();
        sb.append(LONG_VALUE);
        appendSpace();
        sb.append(arg2);
        end();
    }

    private void store_adviseBeforeConversion(long time, String threadName, int bci, int arg1, float arg2) {
        appendTT(time, ADVISE_BEFORE_CONVERSION, threadName, bci);
        appendSpace();
        sb.append(arg1);
        appendSpace();
        sb.append(FLOAT_VALUE);
        appendSpace();
        sb.append(arg2);
        end();
    }

    private void store_adviseBeforeConversion(long time, String threadName, int bci, int arg1, double arg2) {
        appendTT(time, ADVISE_BEFORE_CONVERSION, threadName, bci);
        appendSpace();
        sb.append(arg1);
        appendSpace();
        sb.append(DOUBLE_VALUE);
        appendSpace();
        sb.append(arg2);
        end();
    }

    private void store_adviseBeforeIf(long time, String threadName, int bci, int opcode, int op1, int op2, int branchOffset) {
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

    private void store_adviseBeforeIfObject(long time, String threadName, int bci, int opcode, long objId1, long objId2, int branchOffset) {
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

    private void store_adviseBeforeGoto(long time, String threadName, int bci, int branchOffset) {
        appendTT(time, ADVISE_BEFORE_GOTO, threadName, bci);
        appendSpace();
        sb.append(branchOffset);
        end();
    }

    private void store_adviseBeforeReturnObject(long time, String threadName, int bci, long value) {
        appendTT(time, ADVISE_BEFORE_RETURN, threadName, bci);
        appendSpace();
        sb.append(OBJ_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    private void store_adviseBeforeReturn(long time, String threadName, int bci, long value) {
        appendTT(time, ADVISE_BEFORE_RETURN, threadName, bci);
        appendSpace();
        sb.append(LONG_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    private void store_adviseBeforeReturn(long time, String threadName, int bci, float value) {
        appendTT(time, ADVISE_BEFORE_RETURN, threadName, bci);
        appendSpace();
        sb.append(FLOAT_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    private void store_adviseBeforeReturn(long time, String threadName, int bci, double value) {
        appendTT(time, ADVISE_BEFORE_RETURN, threadName, bci);
        appendSpace();
        sb.append(DOUBLE_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    private void store_adviseBeforeReturn(long time, String threadName, int bci) {
        appendTT(time, ADVISE_BEFORE_RETURN, threadName, bci);
        end();
    }

    private void store_adviseBeforeInvokeVirtual(long time, String threadName, int bci, long objId, String className, long clId, String memberName) {
        appendTTId(time, ADVISE_BEFORE_INVOKE_VIRTUAL, objId, threadName, bci);
        appendQualName(className, clId, memberName);
        end();
    }

    private void store_adviseBeforeInvokeSpecial(long time, String threadName, int bci, long objId, String className, long clId, String memberName) {
        appendTTId(time, ADVISE_BEFORE_INVOKE_SPECIAL, objId, threadName, bci);
        appendQualName(className, clId, memberName);
        end();
    }

    private void store_adviseBeforeInvokeStatic(long time, String threadName, int bci, long objId, String className, long clId, String memberName) {
        appendTTId(time, ADVISE_BEFORE_INVOKE_STATIC, objId, threadName, bci);
        appendQualName(className, clId, memberName);
        end();
    }

    private void store_adviseBeforeInvokeInterface(long time, String threadName, int bci, long objId, String className, long clId, String memberName) {
        appendTTId(time, ADVISE_BEFORE_INVOKE_INTERFACE, objId, threadName, bci);
        appendQualName(className, clId, memberName);
        end();
    }

    private void store_adviseBeforeArrayLength(long time, String threadName, int bci, long objId, int length) {
        appendTTId(time, ADVISE_BEFORE_ARRAY_LENGTH, objId, threadName, bci);
        sb.append(length);
        end();
    }

    private void store_adviseBeforeThrow(long time, String threadName, int bci, long objId) {
        appendTTId(time, ADVISE_BEFORE_THROW, objId, threadName, bci);
        end();
    }

    private void store_adviseBeforeCheckCast(long time, String threadName, int bci, long objId, String className, long clId) {
        appendTTId(time, ADVISE_BEFORE_CHECK_CAST, objId, threadName, bci);
        sb.append(className);
        end();
    }

    private void store_adviseBeforeInstanceOf(long time, String threadName, int bci, long objId, String className, long clId) {
        appendTTId(time, ADVISE_BEFORE_INSTANCE_OF, objId, threadName, bci);
        sb.append(className);
        end();
    }

    /*
    private void store_adviseBeforeBytecode(long time, String threadName, int bci, int arg1) {
        appendTT(time, ADVISE_BEFORE_BYTECODE, threadName, bci);
        appendSpace();
        sb.append(arg1);
        end();
    }
    */

    private void store_adviseBeforeMonitorEnter(long time, String threadName, int bci, long objId) {
        appendTTId(time, ADVISE_BEFORE_MONITOR_ENTER, objId, threadName, bci);
        end();
    }

    private void store_adviseBeforeMonitorExit(long time, String threadName, int bci, long objId) {
        appendTTId(time, ADVISE_BEFORE_MONITOR_EXIT, objId, threadName, bci);
        end();
    }

    /*
    private void store_adviseAfterInvokeVirtual(long time, String threadName, int bci, long objId, String className, long clId, String memberName) {
        appendTTId(time, ADVISE_AFTER_INVOKE_VIRTUAL, objId, threadName, bci);
        appendQualName(className, clId, memberName);
        end();
    }

    private void store_adviseAfterInvokeStatic(long time, String threadName, int bci, long objId, String className, long clId, String memberName) {
        appendTTId(time, ADVISE_AFTER_INVOKE_STATIC, objId, threadName, bci);
        appendQualName(className, clId, memberName);
        end();
    }

    private void store_adviseAfterInvokeInterface(long time, String threadName, int bci, long objId, String className, long clId, String memberName) {
        appendTTId(time, ADVISE_AFTER_INVOKE_INTERFACE, objId, threadName, bci);
        appendQualName(className, clId, memberName);
        end();
    }

    private void store_adviseAfterInvokeSpecial(long time, String threadName, int bci, long objId, String className, long clId, String memberName) {
        appendTTId(time, ADVISE_AFTER_INVOKE_SPECIAL, objId, threadName, bci);
        appendQualName(className, clId, memberName);
        end();
    }
    */

    private void store_adviseAfterMethodEntry(long time, String threadName, int bci, long objId, String className, long clId, String memberName) {
        appendTTId(time, ADVISE_AFTER_METHOD_ENTRY, objId, threadName, bci);
        appendQualName(className, clId, memberName);
        end();
    }

    private void store_adviseBeforeReturnByThrow(long time, String threadName, int bci, long objId, int poppedFrames) {
        appendTTId(time, ADVISE_BEFORE_RETURN_BY_THROW, objId, threadName, bci);
        sb.append(poppedFrames);
        end();

    }

// START GENERATED CODE
// EDIT AND RUN SBPSVMATextStoreGenerator.main() TO MODIFY

    @Override
    public void adviseAfterMultiNewArray(long arg1, String arg2, int arg3, long arg4, String arg5, long arg6, int arg7) {
        ProgramError.unexpected("adviseAfterMultiNewArray");
    }

    @Override
    public void adviseBeforeGC(long arg1, String arg2) {
        store_adviseBeforeGC(arg1, getThreadShortForm(arg2));
    }

    @Override
    public void adviseAfterGC(long arg1, String arg2) {
        store_adviseAfterGC(arg1, getThreadShortForm(arg2));
    }

    @Override
    public void adviseBeforeThreadStarting(long arg1, String arg2) {
        store_adviseBeforeThreadStarting(arg1, getThreadShortForm(arg2));
    }

    @Override
    public void adviseBeforeThreadTerminating(long arg1, String arg2) {
        store_adviseBeforeThreadTerminating(arg1, getThreadShortForm(arg2));
    }

    @Override
    public void adviseBeforeReturnByThrow(long arg1, String arg2, int arg3, long arg4, int arg5) {
        store_adviseBeforeReturnByThrow(arg1, getThreadShortForm(arg2), arg3, checkRepeatId(arg4, arg2), arg5);
    }

    @Override
    public void adviseBeforeConstLoad(long arg1, String arg2, int arg3, float arg4) {
        store_adviseBeforeConstLoad(arg1, getThreadShortForm(arg2), arg3, arg4);
    }

    @Override
    public void adviseBeforeConstLoad(long arg1, String arg2, int arg3, double arg4) {
        store_adviseBeforeConstLoad(arg1, getThreadShortForm(arg2), arg3, arg4);
    }

    @Override
    public void adviseBeforeConstLoad(long arg1, String arg2, int arg3, long arg4) {
        store_adviseBeforeConstLoad(arg1, getThreadShortForm(arg2), arg3, arg4);
    }

    @Override
    public void adviseBeforeConstLoadObject(long arg1, String arg2, int arg3, long arg4) {
        store_adviseBeforeConstLoadObject(arg1, getThreadShortForm(arg2), arg3, arg4);
    }

    @Override
    public void adviseBeforeLoad(long arg1, String arg2, int arg3, int arg4) {
        store_adviseBeforeLoad(arg1, getThreadShortForm(arg2), arg3, arg4);
    }

    @Override
    public void adviseBeforeArrayLoad(long arg1, String arg2, int arg3, long arg4, int arg5) {
        store_adviseBeforeArrayLoad(arg1, getThreadShortForm(arg2), arg3,  checkRepeatId(arg4, arg2), arg5);
    }

    @Override
    public void adviseBeforeStore(long arg1, String arg2, int arg3, int arg4, float arg5) {
        store_adviseBeforeStore(arg1, getThreadShortForm(arg2), arg3, arg4, arg5);
    }

    @Override
    public void adviseBeforeStore(long arg1, String arg2, int arg3, int arg4, double arg5) {
        store_adviseBeforeStore(arg1, getThreadShortForm(arg2), arg3, arg4, arg5);
    }

    @Override
    public void adviseBeforeStore(long arg1, String arg2, int arg3, int arg4, long arg5) {
        store_adviseBeforeStore(arg1, getThreadShortForm(arg2), arg3, arg4, arg5);
    }

    @Override
    public void adviseBeforeStoreObject(long arg1, String arg2, int arg3, int arg4, long arg5) {
        store_adviseBeforeStoreObject(arg1, getThreadShortForm(arg2), arg3, arg4, arg5);
    }

    @Override
    public void adviseBeforeArrayStore(long arg1, String arg2, int arg3, long arg4, int arg5, long arg6) {
        store_adviseBeforeArrayStore(arg1, getThreadShortForm(arg2), arg3,  checkRepeatId(arg4, arg2), arg5, arg6);
    }

    @Override
    public void adviseBeforeArrayStore(long arg1, String arg2, int arg3, long arg4, int arg5, double arg6) {
        store_adviseBeforeArrayStore(arg1, getThreadShortForm(arg2), arg3,  checkRepeatId(arg4, arg2), arg5, arg6);
    }

    @Override
    public void adviseBeforeArrayStore(long arg1, String arg2, int arg3, long arg4, int arg5, float arg6) {
        store_adviseBeforeArrayStore(arg1, getThreadShortForm(arg2), arg3,  checkRepeatId(arg4, arg2), arg5, arg6);
    }

    @Override
    public void adviseBeforeArrayStoreObject(long arg1, String arg2, int arg3, long arg4, int arg5, long arg6) {
        store_adviseBeforeArrayStoreObject(arg1, getThreadShortForm(arg2), arg3,  checkRepeatId(arg4, arg2), arg5, arg6);
    }

    @Override
    public void adviseBeforeStackAdjust(long arg1, String arg2, int arg3, int arg4) {
        store_adviseBeforeStackAdjust(arg1, getThreadShortForm(arg2), arg3, arg4);
    }

    @Override
    public void adviseBeforeOperation(long arg1, String arg2, int arg3, int arg4, long arg5, long arg6) {
        store_adviseBeforeOperation(arg1, getThreadShortForm(arg2), arg3, arg4, arg5, arg6);
    }

    @Override
    public void adviseBeforeOperation(long arg1, String arg2, int arg3, int arg4, float arg5, float arg6) {
        store_adviseBeforeOperation(arg1, getThreadShortForm(arg2), arg3, arg4, arg5, arg6);
    }

    @Override
    public void adviseBeforeOperation(long arg1, String arg2, int arg3, int arg4, double arg5, double arg6) {
        store_adviseBeforeOperation(arg1, getThreadShortForm(arg2), arg3, arg4, arg5, arg6);
    }

    @Override
    public void adviseBeforeConversion(long arg1, String arg2, int arg3, int arg4, float arg5) {
        store_adviseBeforeConversion(arg1, getThreadShortForm(arg2), arg3, arg4, arg5);
    }

    @Override
    public void adviseBeforeConversion(long arg1, String arg2, int arg3, int arg4, double arg5) {
        store_adviseBeforeConversion(arg1, getThreadShortForm(arg2), arg3, arg4, arg5);
    }

    @Override
    public void adviseBeforeConversion(long arg1, String arg2, int arg3, int arg4, long arg5) {
        store_adviseBeforeConversion(arg1, getThreadShortForm(arg2), arg3, arg4, arg5);
    }

    @Override
    public void adviseBeforeIf(long arg1, String arg2, int arg3, int arg4, int arg5, int arg6, int arg7) {
        store_adviseBeforeIf(arg1, getThreadShortForm(arg2), arg3, arg4, arg5, arg6, arg7);
    }

    @Override
    public void adviseBeforeIfObject(long arg1, String arg2, int arg3, int arg4, long arg5, long arg6, int arg7) {
        store_adviseBeforeIfObject(arg1, getThreadShortForm(arg2), arg3, arg4, arg5, arg6, arg7);
    }

    @Override
    public void adviseBeforeGoto(long arg1, String arg2, int arg3, int arg4) {
        store_adviseBeforeGoto(arg1, getThreadShortForm(arg2), arg3, arg4);
    }

    @Override
    public void adviseBeforeReturnObject(long arg1, String arg2, int arg3, long arg4) {
        store_adviseBeforeReturnObject(arg1, getThreadShortForm(arg2), arg3, arg4);
    }

    @Override
    public void adviseBeforeReturn(long arg1, String arg2, int arg3, float arg4) {
        store_adviseBeforeReturn(arg1, getThreadShortForm(arg2), arg3, arg4);
    }

    @Override
    public void adviseBeforeReturn(long arg1, String arg2, int arg3, double arg4) {
        store_adviseBeforeReturn(arg1, getThreadShortForm(arg2), arg3, arg4);
    }

    @Override
    public void adviseBeforeReturn(long arg1, String arg2, int arg3) {
        store_adviseBeforeReturn(arg1, getThreadShortForm(arg2), arg3);
    }

    @Override
    public void adviseBeforeReturn(long arg1, String arg2, int arg3, long arg4) {
        store_adviseBeforeReturn(arg1, getThreadShortForm(arg2), arg3, arg4);
    }

    @Override
    public void adviseBeforeGetStatic(long arg1, String arg2, int arg3, String arg4, long arg5, String arg6) {
        store_adviseBeforeGetStatic(arg1, getThreadShortForm(arg2), arg3, getClassShortForm(arg4, arg5), arg5, getFieldShortForm(arg4, arg5, arg6));
    }

    @Override
    public void adviseBeforePutStaticObject(long arg1, String arg2, int arg3, String arg4, long arg5, String arg6, long arg7) {
        store_adviseBeforePutStaticObject(arg1, getThreadShortForm(arg2), arg3, getClassShortForm(arg4, arg5), arg5, getFieldShortForm(arg4, arg5, arg6), arg7);
    }

    @Override
    public void adviseBeforePutStatic(long arg1, String arg2, int arg3, String arg4, long arg5, String arg6, long arg7) {
        store_adviseBeforePutStatic(arg1, getThreadShortForm(arg2), arg3, getClassShortForm(arg4, arg5), arg5, getFieldShortForm(arg4, arg5, arg6), arg7);
    }

    @Override
    public void adviseBeforePutStatic(long arg1, String arg2, int arg3, String arg4, long arg5, String arg6, float arg7) {
        store_adviseBeforePutStatic(arg1, getThreadShortForm(arg2), arg3, getClassShortForm(arg4, arg5), arg5, getFieldShortForm(arg4, arg5, arg6), arg7);
    }

    @Override
    public void adviseBeforePutStatic(long arg1, String arg2, int arg3, String arg4, long arg5, String arg6, double arg7) {
        store_adviseBeforePutStatic(arg1, getThreadShortForm(arg2), arg3, getClassShortForm(arg4, arg5), arg5, getFieldShortForm(arg4, arg5, arg6), arg7);
    }

    @Override
    public void adviseBeforeGetField(long arg1, String arg2, int arg3, long arg4, String arg5, long arg6, String arg7) {
        store_adviseBeforeGetField(arg1, getThreadShortForm(arg2), arg3, checkRepeatId(arg4, arg2), getClassShortForm(arg5, arg6), arg6, getFieldShortForm(arg5, arg6, arg7));
    }

    @Override
    public void adviseBeforePutFieldObject(long arg1, String arg2, int arg3, long arg4, String arg5, long arg6, String arg7, long arg8) {
        store_adviseBeforePutFieldObject(arg1, getThreadShortForm(arg2), arg3, checkRepeatId(arg4, arg2), getClassShortForm(arg5, arg6), arg6, getFieldShortForm(arg5, arg6, arg7), arg8);
    }

    @Override
    public void adviseBeforePutField(long arg1, String arg2, int arg3, long arg4, String arg5, long arg6, String arg7, double arg8) {
        store_adviseBeforePutField(arg1, getThreadShortForm(arg2), arg3, checkRepeatId(arg4, arg2), getClassShortForm(arg5, arg6), arg6, getFieldShortForm(arg5, arg6, arg7), arg8);
    }

    @Override
    public void adviseBeforePutField(long arg1, String arg2, int arg3, long arg4, String arg5, long arg6, String arg7, long arg8) {
        store_adviseBeforePutField(arg1, getThreadShortForm(arg2), arg3, checkRepeatId(arg4, arg2), getClassShortForm(arg5, arg6), arg6, getFieldShortForm(arg5, arg6, arg7), arg8);
    }

    @Override
    public void adviseBeforePutField(long arg1, String arg2, int arg3, long arg4, String arg5, long arg6, String arg7, float arg8) {
        store_adviseBeforePutField(arg1, getThreadShortForm(arg2), arg3, checkRepeatId(arg4, arg2), getClassShortForm(arg5, arg6), arg6, getFieldShortForm(arg5, arg6, arg7), arg8);
    }

    @Override
    public void adviseBeforeInvokeVirtual(long arg1, String arg2, int arg3, long arg4, String arg5, long arg6, String arg7) {
        store_adviseBeforeInvokeVirtual(arg1, getThreadShortForm(arg2), arg3, checkRepeatId(arg4, arg2), getClassShortForm(arg5, arg6), arg6, getMethodShortForm(arg5, arg6, arg7));
    }

    @Override
    public void adviseBeforeInvokeSpecial(long arg1, String arg2, int arg3, long arg4, String arg5, long arg6, String arg7) {
        store_adviseBeforeInvokeSpecial(arg1, getThreadShortForm(arg2), arg3, checkRepeatId(arg4, arg2), getClassShortForm(arg5, arg6), arg6, getMethodShortForm(arg5, arg6, arg7));
    }

    @Override
    public void adviseBeforeInvokeStatic(long arg1, String arg2, int arg3, long arg4, String arg5, long arg6, String arg7) {
        store_adviseBeforeInvokeStatic(arg1, getThreadShortForm(arg2), arg3, checkRepeatId(arg4, arg2), getClassShortForm(arg5, arg6), arg6, getMethodShortForm(arg5, arg6, arg7));
    }

    @Override
    public void adviseBeforeInvokeInterface(long arg1, String arg2, int arg3, long arg4, String arg5, long arg6, String arg7) {
        store_adviseBeforeInvokeInterface(arg1, getThreadShortForm(arg2), arg3, checkRepeatId(arg4, arg2), getClassShortForm(arg5, arg6), arg6, getMethodShortForm(arg5, arg6, arg7));
    }

    @Override
    public void adviseBeforeArrayLength(long arg1, String arg2, int arg3, long arg4, int arg5) {
        store_adviseBeforeArrayLength(arg1, getThreadShortForm(arg2), arg3, arg4, arg5);
    }

    @Override
    public void adviseBeforeThrow(long arg1, String arg2, int arg3, long arg4) {
        store_adviseBeforeThrow(arg1, getThreadShortForm(arg2), arg3, checkRepeatId(arg4, arg2));
    }

    @Override
    public void adviseBeforeCheckCast(long arg1, String arg2, int arg3, long arg4, String arg5, long arg6) {
        store_adviseBeforeCheckCast(arg1, getThreadShortForm(arg2), arg3, checkRepeatId(arg4, arg2), getClassShortForm(arg5, arg6), arg6);
    }

    @Override
    public void adviseBeforeInstanceOf(long arg1, String arg2, int arg3, long arg4, String arg5, long arg6) {
        store_adviseBeforeInstanceOf(arg1, getThreadShortForm(arg2), arg3, checkRepeatId(arg4, arg2), getClassShortForm(arg5, arg6), arg6);
    }

    @Override
    public void adviseBeforeMonitorEnter(long arg1, String arg2, int arg3, long arg4) {
        store_adviseBeforeMonitorEnter(arg1, getThreadShortForm(arg2), arg3, checkRepeatId(arg4, arg2));
    }

    @Override
    public void adviseBeforeMonitorExit(long arg1, String arg2, int arg3, long arg4) {
        store_adviseBeforeMonitorExit(arg1, getThreadShortForm(arg2), arg3, checkRepeatId(arg4, arg2));
    }

    @Override
    public void adviseAfterLoadObject(long arg1, String arg2, int arg3, int arg4, long arg5) {
        store_adviseAfterLoadObject(arg1, getThreadShortForm(arg2), arg3, arg4, arg5);
    }

    @Override
    public void adviseAfterArrayLoadObject(long arg1, String arg2, int arg3, long arg4, int arg5, long arg6) {
        store_adviseAfterArrayLoadObject(arg1, getThreadShortForm(arg2), arg3,  checkRepeatId(arg4, arg2), arg5, arg6);
    }

    @Override
    public void adviseAfterNew(long arg1, String arg2, int arg3, long arg4, String arg5, long arg6) {
        store_adviseAfterNew(arg1, getThreadShortForm(arg2), arg3, checkRepeatId(arg4, arg2), getClassShortForm(arg5, arg6), arg6);
    }

    @Override
    public void adviseAfterNewArray(long arg1, String arg2, int arg3, long arg4, String arg5, long arg6, int arg7) {
        store_adviseAfterNewArray(arg1, getThreadShortForm(arg2), arg3, checkRepeatId(arg4, arg2), getClassShortForm(arg5, arg6), arg6, arg7);
    }

    @Override
    public void adviseAfterMethodEntry(long arg1, String arg2, int arg3, long arg4, String arg5, long arg6, String arg7) {
        store_adviseAfterMethodEntry(arg1, getThreadShortForm(arg2), arg3, checkRepeatId(arg4, arg2), getClassShortForm(arg5, arg6), arg6, getMethodShortForm(arg5, arg6, arg7));
    }

// END GENERATED CODE

}

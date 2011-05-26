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
package com.oracle.max.vm.ext.vma.log.txt.sbps;

import java.io.*;

import com.oracle.max.vm.ext.vma.log.*;
import com.oracle.max.vm.ext.vma.log.txt.*;

/**
 * An implementation of {@link TextVMAdviceHandlerLog} in Java using a {@link PrintStream} and {@link StringBuilder}.
 *
 * The default {@link StringBuilder buffer size} is {@value DEFAULT_BUFSIZE} but this can be changed
 * with the {@value BUFSIZE_PROPERTY} system property. The buffer is normally flushed when it is full,
 * but this can be changed to a specific value by setting the {@value FLUSH_PROPERTY} system property.
 *
 * Since the buffer is global to all threads all entry methods are synchronized.
 *
 * @author Mick Jordan
 *
 */
public class SBPSTextVMAdviceHandlerLog extends TextVMAdviceHandlerLog {

    private static final String FLUSH_PROPERTY = "max.vma.flushlogat";
    private static final String BUFSIZE_PROPERTY = "max.vma.logbufsize";
    private static final String ABSTIME_PROPERTY = "max.vma.abstime";
    private static final int DEFAULT_BUFSIZE = 1024 * 1024;
    private PrintStream ps;
    private long lastTime;
    private boolean absTime;
    StringBuilder sb;
    private int flushLogAt;

    public SBPSTextVMAdviceHandlerLog() {
        super();
    }

    public SBPSTextVMAdviceHandlerLog(TimeStampGenerator timeStampGenerator) {
        super(timeStampGenerator);
    }

    @Override
    public boolean initializeLog() {
        final String logFile = VMAdviceHandlerLogFile.getLogFile();
        try {
            ps = new PrintStream(new FileOutputStream(logFile));
            lastTime = timeStampGenerator.getTimeStamp();
            int logSize = DEFAULT_BUFSIZE;
            final String logSizeProp = System.getProperty(BUFSIZE_PROPERTY);
            if (logSizeProp != null) {
                logSize = Integer.parseInt(logSizeProp);
            }
            sb = new StringBuilder(logSize);
            flushLogAt = System.getProperty(FLUSH_PROPERTY) != null ? 0 : logSize - 80;
            absTime = System.getProperty(ABSTIME_PROPERTY) != null;
            sb.append(INITIALIZE_ID);
            appendSpace();
            sb.append(lastTime);
            appendSpace();
            sb.append(absTime);
            end();
            return true;
        } catch (IOException ex) {
            System.err.println("failed to open log file " + logFile + ": " + ex);
            return false;
        }
    }

    @Override
    public synchronized void finalizeLog() {
        sb.append(FINALIZE_ID);
        appendSpace();
        sb.append(timeStampGenerator.getTimeStamp());
        flushLogAt = 0;
        end();
        ps.close();
    }

    void end() {
        sb.append('\n');
        if (sb.length()  >= flushLogAt) {
            ps.print(sb);
            ps.flush();
            sb.setLength(0);
        }
    }

    private void appendId(long objId) {
        if (objId == REPEAT_ID_VALUE) {
            sb.append('*');
        } else {
            sb.append(objId);
        }
    }

    private void appendTime() {
        final long now = timeStampGenerator.getTimeStamp();
        if (absTime) {
            sb.append(now);
        } else {
            sb.append(now - lastTime);
            lastTime = now;
        }
    }

    private void appendSpace() {
        sb.append(' ');
    }

    private void appendIdTC(StringBuilder sb, char key, long objId,
            String threadName) {
        sb.append(key);
        appendSpace();
        appendTime();
        appendSpace();
        sb.append(threadName);
        appendSpace();
        appendId(objId);
        appendSpace();
    }

    private void appendTC(StringBuilder sb, char key, String className, String threadName) {
        sb.append(key);
        appendSpace();
        appendTime();
        appendSpace();
        sb.append(threadName);
        appendSpace();
        sb.append(className);
        appendSpace();
    }

    private void appendWriteTrackingPrefix(long objId, String fieldName, String threadName) {
        appendIdTC(sb, OBJECT_WRITE_ID, objId, threadName);
        sb.append(fieldName);
        appendSpace();
    }

    private void appendWriteStaticTrackingPrefix(String fieldName, String className, long clId, String threadName) {
        appendTC(sb, STATIC_WRITE_ID, className, threadName);
        sb.append(clId);
        appendSpace();
        sb.append(fieldName);
        appendSpace();
    }

    @Override
    public synchronized void adviseGC(String threadName) {
        sb.append(GC_ID);
        appendSpace();
        appendTime();
        appendSpace();
        sb.append(threadName);
        end();
    }

    @Override
    public synchronized void removal(long id) {
        sb.append(REMOVAL_ID);
        appendSpace();
        appendId(id);
        end();
    }

    @Override
    public synchronized void unseenObject(long objId,
            String className, long clId) {
        sb.append(UNSEEN_OBJECT_ID);
        appendSpace();
        appendTime();
        appendSpace();
        appendId(objId);
        appendSpace();
        sb.append(className);
        appendSpace();
        sb.append(clId);
        end();
    }

    @Override
    public synchronized void resetTime() {
        sb.append(RESET_TIME_ID);
        appendSpace();
        lastTime = timeStampGenerator.getTimeStamp();
        sb.append(lastTime);
        end();
    }

    @Override
    public void adviseThreadStarting(String threadName) {
    }

    @Override
    public void adviseThreadTerminating(String threadName) {
    }

    @Override
    public synchronized void adviseBeforeGetStatic(String threadName, String className, long clId, String fieldName) {
        appendTC(sb, STATIC_READ_ID, className, threadName);
        sb.append(fieldName);
        appendSpace();
        sb.append(clId);
        end();
    }

    @Override
    public synchronized void adviseBeforePutStatic(String threadName, String className, long clId, String fieldName, double value) {
        appendWriteStaticTrackingPrefix(fieldName, className, clId, threadName);
        sb.append(DOUBLE_TYPE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public synchronized void adviseBeforePutStatic(String threadName, String className, long clId, String fieldName, long value) {
        appendWriteStaticTrackingPrefix(fieldName, className, clId, threadName);
        sb.append(LONG_TYPE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public synchronized void adviseBeforePutStatic(String threadName, String className, long clId, String fieldName, float value) {
        appendWriteStaticTrackingPrefix(fieldName, className, clId, threadName);
        sb.append(FLOAT_TYPE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public synchronized void adviseBeforePutStaticObject(String threadName, String className, long clId, String fieldName, long valueId) {
        appendWriteStaticTrackingPrefix(fieldName, className, clId, threadName);
        sb.append(OBJ_TYPE);
        appendSpace();
        appendId(valueId);
        end();
    }

    @Override
    public synchronized void adviseBeforeGetField(String threadName, long objId, String fieldName) {
        appendIdTC(sb, OBJECT_READ_ID, objId, threadName);
        sb.append(fieldName);
        end();
    }

    @Override
    public synchronized void adviseBeforePutField(String threadName, long objId, String fieldName, double value) {
        appendWriteTrackingPrefix(objId, fieldName, threadName);
        sb.append(DOUBLE_TYPE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public synchronized void adviseBeforePutField(String threadName, long objId, String fieldName, long value) {
        appendWriteTrackingPrefix(objId, fieldName, threadName);
        sb.append(LONG_TYPE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public synchronized void adviseBeforePutField(String threadName, long objId, String fieldName, float value) {
        appendWriteTrackingPrefix(objId, fieldName, threadName);
        sb.append(FLOAT_TYPE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public synchronized void adviseBeforePutFieldObject(String threadName, long objId, String fieldName, long valueId) {
        appendWriteTrackingPrefix(objId, fieldName, threadName);
        sb.append(OBJ_TYPE);
        appendSpace();
        appendId(valueId);
        end();
    }

    @Override
    public void adviseBeforeArrayLoad(String threadName, long objId, int index) {
        appendIdTC(sb, ARRAY_READ_ID, objId, threadName);
        sb.append(index);
        end();
    }

    @Override
    public void adviseBeforeArrayStore(String threadName, long objId, int index, float value) {
        appendIdTC(sb, ARRAY_WRITE_ID, objId, threadName);
        sb.append(FLOAT_TYPE);
        appendSpace();
        sb.append(value);
        appendSpace();
        sb.append(index);
        end();
    }

    @Override
    public void adviseBeforeArrayStore(String threadName, long objId, int index, long value) {
        appendIdTC(sb, ARRAY_WRITE_ID, objId, threadName);
        sb.append(LONG_TYPE);
        appendSpace();
        sb.append(value);
        appendSpace();
        sb.append(index);
        end();
    }

    @Override
    public void adviseBeforeArrayStore(String threadName, long objId, int index, double value) {
        appendIdTC(sb, ARRAY_WRITE_ID, objId, threadName);
        sb.append(DOUBLE_TYPE);
        appendSpace();
        sb.append(value);
        appendSpace();
        sb.append(index);
        end();
    }

    @Override
    public void adviseBeforeArrayStoreObject(String threadName, long objId, int index, long valueId) {
        appendIdTC(sb, ARRAY_WRITE_ID, objId, threadName);
        sb.append(OBJ_TYPE);
        appendSpace();
        appendId(valueId);
        appendSpace();
        sb.append(index);
        end();
    }

    @Override
    public synchronized void adviseAfterInvokeSpecial(String threadName, long objId) {
        appendIdTC(sb, OBJECT_CREATION_END_ID, objId, threadName);
        end();
    }

    @Override
    public synchronized void adviseAfterNew(String threadName, long objId, String className, long clId) {
        appendIdTC(sb, OBJECT_CREATION_BEGIN_ID, objId, threadName);
        sb.append(className);
        appendSpace();
        sb.append(clId);
        end();
    }

    @Override
    public synchronized void adviseAfterNewArray(String threadName, long objId, String className, long clId, int length) {
        appendIdTC(sb, ARRAY_CREATION_ID, objId, threadName);
        sb.append(className);
        appendSpace();
        sb.append(clId);
        appendSpace();
        sb.append(length);
        end();
    }

    @Override
    public synchronized void adviseAfterMultiNewArray(String threadName, long objId, String className, long clId, int length) {
        // MultiArrays are explicitly handled by multiple calls to adviseAfterNewArray so we just
        // log the top level array.
        adviseAfterNewArray(threadName, objId, className, clId, length);
    }

}

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

import com.oracle.max.vm.ext.vma.log.*;

/**
 * Defines a textual format for the the {@link VMAdviceHandlerLog} output.
 * The format corresponding to each method is given below. The terms used are:
 * <ul>
 * <li>atime absolute wall clock time
 * <li>time - if log uses absolute time then atime, else time increment from last trace
 * <li>thread - name for thread
 * <li>class - name for class
 * <li>field - name of field
 * <li>id - object id. Special case is '*', means same as last id (not used in a "value" context)
 * <li>clid - id of the associated classloader
 * <li>type - O object, J long, F float, D double (N.B. boolean, char, short, int all appear as long)
 * <li>value - id if type==O, toString representation otherwise
 * <li>index - array index
 * </ul>
 * Trace syntax by method:
 *
 * <ul>
 * <li>initializeLog: M atime isAbsTime (isAbsTime==true|false)
 * <li>finalizeLog: Z atime
 * <li>resetTime: K atime
 * <li>adviseAfteNew: B time thread id class clid
 * <li>adviseAfterInvokeSpecial: E time thread id
 * <li>adviseAfterNewArray: A time thread id class clid
 * <li>adviseBeforePutField: W time thread id field type value
 * <li>adviseBeforePutStatic: V time thread class clid field type value
 * <li>adviseBeforeGetField: R time thread id field
 * <li>adviseBeforeGetStatic: Q time thread class clid field
 * <li>adviseBeforeArrayStore: S time thread id type value @ index
 * <li>adviseBeforeArrayLoad: T time thread id
 * <li>arraycopyTrackingLog: C time thread srcid srcpos destid destpos length TODO
 * <li>removal: D id
 * <li>adviseGC: N time thread
 * <li>unseenObject: X time id class clid
 * </ul>
 *
 * There is an explicit assumption that log records are ordered in time to support
 * the relative time optimization. However, an embedded resetTimeLog record can be
 * used to "reset" the time for logs that are created from, say, a set of records for a set
 * of threads.
 *
 * N.B. There is absolutely no logic to the choice of characters used to represent the
 * method name!
 *
 */

public abstract class TextVMAdviceHandlerLog extends VMAdviceHandlerLog {

    public static final char OBJECT_CREATION_BEGIN_ID = 'B';
    public static final char OBJECT_CREATION_END_ID   = 'E';
    public static final char ARRAY_CREATION_ID   = 'A';

    public static final char OBJECT_WRITE_ID          = 'W';
    public static final char ARRAY_WRITE_ID           = 'S';
    public static final char STATIC_WRITE_ID          = 'V';

    public static final char OBJECT_READ_ID           = 'R';
    public static final char ARRAY_READ_ID            = 'T';
    public static final char STATIC_READ_ID           = 'Q';

    public static final char ARRAY_COPY_ID            = 'C';

    public static final char REMOVAL_ID               = 'D';

    public static final char UNSEEN_OBJECT_ID         = 'X';

    public static final char CLASS_DEFINITION_ID      = 'G';
    public static final char FIELD_DEFINITION_ID      = 'F';
    public static final char THREAD_DEFINITION_ID      = 'H';
    public static final char INITIALIZE_ID = 'M';
    public static final char FINALIZE_ID = 'Z';
    public static final char RESET_TIME_ID = 'K';
    public static final char GC_ID = 'N';

    public static final int REPEAT_ID_VALUE = -1;
    public static final char REPEAT_ID = '*';

    public static final char OBJ_TYPE = 'O';
    public static final char INT_TYPE = 'I';
    public static final char LONG_TYPE = 'J';
    public static final char FLOAT_TYPE = 'F';
    public static final char DOUBLE_TYPE = 'D';

    protected TextVMAdviceHandlerLog() {
        super();
    }

    protected TextVMAdviceHandlerLog(TimeStampGenerator timeStampGenerator) {
        super(timeStampGenerator);
    }

    public static boolean hasId(char c) {
        switch (c) {
            case OBJECT_CREATION_BEGIN_ID:
            case OBJECT_CREATION_END_ID:
            case ARRAY_CREATION_ID:
            case OBJECT_WRITE_ID:
            case ARRAY_WRITE_ID:
            case OBJECT_READ_ID:
            case ARRAY_READ_ID:
            case ARRAY_COPY_ID:
                return true;
            default:
                return false;
        }
    }

    public static boolean hasTime(char c) {
        switch (c) {
            case OBJECT_CREATION_BEGIN_ID:
            case OBJECT_CREATION_END_ID:
            case ARRAY_CREATION_ID:
            case OBJECT_WRITE_ID:
            case ARRAY_WRITE_ID:
            case STATIC_WRITE_ID:
            case OBJECT_READ_ID:
            case ARRAY_READ_ID:
            case STATIC_READ_ID:
            case ARRAY_COPY_ID:
            case UNSEEN_OBJECT_ID:
                return true;
            default:
                return false;
        }
    }

    public static boolean hasTimeAndThread(char c) {
        return hasTime(c) && c != UNSEEN_OBJECT_ID;
    }

}

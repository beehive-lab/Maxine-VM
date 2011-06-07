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
import com.oracle.max.vm.ext.vma.log.*;

/**
 * Defines a textual format for the the {@link VMAdviceHandlerLog} output.
 *
 * There is an explicit assumption that log records are ordered in time to support
 * the relative time optimization. However, an embedded resetTimeLog record can be
 * used to "reset" the time for logs that are created from, say, a set of records for a set
 * of threads. Normally the log uses relative time, recording the offset from the previous
 * record for that thread. However, it is possible to use absolute time and this is
 * indicated by a boolean value to the {@link Key#INITIALIZE_LOG} record.
 *
 * Each log record occupies one line starting with the string code for the {@link Key}.
 * The key is followed by the time (either absolute or relative) and then, for most records,
 * the thread that created the record. This is then followed by arguments that are specific
 * to the record, generally in same order as the parameters to the methods in {@link VMAdviceHandlerLog}.
 *
 */

public abstract class TextVMAdviceHandlerLog extends VMAdviceHandlerLog {

    public static final int REPEAT_ID_VALUE = -1;
    public static final char REPEAT_ID = '*';

    public static final EnumSet<Key> noTimeSet = EnumSet.of(Key.CLASS_DEFINITION, Key.FIELD_DEFINITION, Key.THREAD_DEFINITION,
                                                      Key.REMOVAL);
    public static final Map<String, Key> commandMap = new HashMap<String, Key>();

    static {
        for (Key key : Key.values()) {
            commandMap.put(key.code, key);
        }
    }

    public static final char OBJ_VALUE = 'O';
    public static final char LONG_VALUE = 'J';
    public static final char FLOAT_VALUE = 'F';
    public static final char DOUBLE_VALUE = 'D';

    public static boolean hasId(Key code) {
        return hasIdSet.contains(code);
    }

    public static boolean hasTime(Key key) {
        return !noTimeSet.contains(key);
    }

    public static boolean hasTimeAndThread(Key key) {
        return hasTime(key) && !(key == Key.UNSEEN || key == Key.INITIALIZE_LOG | key == Key.FINALIZE_LOG);
    }

    // BEGIN GENERATED CODE

    // GENERATED -- EDIT AND RUN TextVMAdviceHandlerLogGenerator.main() TO MODIFY
    public enum Key {
        CLASS_DEFINITION("C"),
        FIELD_DEFINITION("F"),
        THREAD_DEFINITION("T"),
        ADVISE_BEFORE_THROW("BT"),
        ADVISE_BEFORE_IF("BI"),
        ADVISE_BEFORE_LOAD("BL"),
        ADVISE_BEFORE_BYTECODE("BB"),
        ADVISE_AFTER_MULTI_NEW_ARRAY("AMNA"),
        ADVISE_BEFORE_INVOKE_INTERFACE("BII"),
        ADVISE_BEFORE_STORE("BS"),
        ADVISE_BEFORE_INSTANCE_OF("BIO"),
        ADVISE_BEFORE_ARRAY_STORE("BAS"),
        ADVISE_BEFORE_GET_STATIC("BGS"),
        ADVISE_BEFORE_PUT_FIELD("BPF"),
        ADVISE_AFTER_INVOKE_INTERFACE("AII"),
        ADVISE_BEFORE_INVOKE_STATIC("BIS"),
        ADVISE_BEFORE_PUT_STATIC("BPS"),
        ADVISE_AFTER_INVOKE_VIRTUAL("AIV"),
        ADVISE_BEFORE_MONITOR_EXIT("BMX"),
        ADVISE_BEFORE_ARRAY_LENGTH("BAG"),
        REMOVAL("D"),
        ADVISE_BEFORE_CHECK_CAST("BCC"),
        ADVISE_BEFORE_IPUSH("BIP"),
        ADVISE_AFTER_GC("AGC"),
        ADVISE_BEFORE_IINC("BIN"),
        ADVISE_BEFORE_GET_FIELD("BGF"),
        ADVISE_BEFORE_OPERATION("BO"),
        ADVISE_AFTER_INVOKE_SPECIAL("AIZ"),
        ADVISE_BEFORE_INVOKE_SPECIAL("BIZ"),
        ADVISE_BEFORE_STACK_ADJUST("BSA"),
        ADVISE_BEFORE_GC("BGC"),
        ADVISE_AFTER_INVOKE_STATIC("AIS"),
        ADVISE_BEFORE_RETURN("BR"),
        ADVISE_BEFORE_ARRAY_LOAD("BAL"),
        ADVISE_BEFORE_CONVERSION("BC"),
        ADVISE_BEFORE_MONITOR_ENTER("BME"),
        ADVISE_BEFORE_THREAD_TERMINATING("BTT"),
        INITIALIZE_LOG("IL"),
        ADVISE_BEFORE_THREAD_STARTING("BTS"),
        ADVISE_BEFORE_CONST_LOAD("BCL"),
        FINALIZE_LOG("FL"),
        ADVISE_AFTER_NEW("AN"),
        RESET_TIME("ZT"),
        ADVISE_AFTER_NEW_ARRAY("ANA"),
        ADVISE_BEFORE_INVOKE_VIRTUAL("BIV"),
        UNSEEN("U");
        public final String code;
        private Key(String code) {
            this.code = code;
        }
    }

    // GENERATED -- EDIT AND RUN TextVMAdviceHandlerLogGenerator.main() TO MODIFY
    public static final EnumSet<Key> hasIdSet = EnumSet.of(
        Key.ADVISE_BEFORE_ARRAY_LOAD,
        Key.ADVISE_BEFORE_ARRAY_STORE,
        Key.ADVISE_BEFORE_GET_FIELD,
        Key.ADVISE_BEFORE_PUT_FIELD,
        Key.ADVISE_BEFORE_INVOKE_VIRTUAL,
        Key.ADVISE_BEFORE_INVOKE_SPECIAL,
        Key.ADVISE_BEFORE_INVOKE_STATIC,
        Key.ADVISE_BEFORE_INVOKE_INTERFACE,
        Key.ADVISE_BEFORE_ARRAY_LENGTH,
        Key.ADVISE_BEFORE_THROW,
        Key.ADVISE_BEFORE_CHECK_CAST,
        Key.ADVISE_BEFORE_INSTANCE_OF,
        Key.ADVISE_BEFORE_MONITOR_ENTER,
        Key.ADVISE_BEFORE_MONITOR_EXIT,
        Key.ADVISE_AFTER_INVOKE_VIRTUAL,
        Key.ADVISE_AFTER_INVOKE_SPECIAL,
        Key.ADVISE_AFTER_INVOKE_STATIC,
        Key.ADVISE_AFTER_INVOKE_INTERFACE,
        Key.ADVISE_AFTER_NEW,
        Key.ADVISE_AFTER_NEW_ARRAY,
        Key.ADVISE_AFTER_MULTI_NEW_ARRAY);

}

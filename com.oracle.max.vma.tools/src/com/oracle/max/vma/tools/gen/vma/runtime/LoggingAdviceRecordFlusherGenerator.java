/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vma.tools.gen.vma.runtime;

import static com.sun.max.vm.t1x.T1XTemplateGenerator.*;
import static com.oracle.max.vm.ext.vma.runtime.TransientVMAdviceHandlerTypes.*;

import com.oracle.max.vm.ext.vma.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.t1x.*;

/**
 * Generates the case statements for logging the records.
 */

@HOSTED_ONLY
public class LoggingAdviceRecordFlusherGenerator {
    public static void main(String[] args) {
        T1XTemplateGenerator.setGeneratingClass(LoggingAdviceRecordFlusherGenerator.class);
        generateAutoComment();
        for (RecordType m : RecordType.values()) {
            generate(m);
        }
    }

    private static void generate(RecordType e) {
        String[] splitType = null;
        out.printf("            case %s: {%n", e.name());
        switch (e) {
            case ThreadStarting:
            case ThreadTerminating:
                outAssertMode(AdviceMode.BEFORE.ordinal());
                // TODO
                break;

            case PutStaticObject:
            case PutStaticLong:
            case PutStaticFloat:
            case PutStaticDouble:
                splitType = splitType(e, "Static");
                // Checkstyle: stop FallThrough check
            case PutFieldObject:
            case PutFieldLong:
            case PutFieldFloat:
            case PutFieldDouble:
                // Checkstyle: resume FallThrough check
                if (splitType == null) {
                    splitType = splitType(e, "Field");
                }
                outAssertMode(AdviceMode.BEFORE.ordinal());
                out.printf("                Object%sAdviceRecord record = (Object%sAdviceRecord) thisRecord;%n", splitType[1], splitType[1]);
                out.printf("                logHandler.adviseBefore%s(record.value, getPackedValue(record), record.value2);%n", splitType[0]);
                break;

            case GetStatic:
            case GetField:
                outAssertMode(AdviceMode.BEFORE.ordinal());
                out.printf("                ObjectAdviceRecord record = (ObjectAdviceRecord) thisRecord;%n");
                out.printf("                logHandler.adviseBefore%s(record.value, getPackedValue(record));%n", e.name());
                break;

            case ArrayStoreObject:
            case ArrayStoreLong:
            case ArrayStoreFloat:
            case ArrayStoreDouble:
                splitType = splitType(e, "ArrayStore");
                outAssertMode(AdviceMode.BEFORE.ordinal());
                out.printf("                Object%sAdviceRecord record = (Object%sAdviceRecord) thisRecord;%n", splitType[1], splitType[1]);
                out.printf("                logHandler.adviseBefore%s(record.value, getArrayIndex(record), record.value2);%n", splitType[0]);
                break;

            case New:
                outAssertMode(AdviceMode.AFTER.ordinal());
                out.printf("                ObjectAdviceRecord record = (ObjectAdviceRecord) thisRecord;%n");
                out.printf("                logHandler.adviseAfterNew(record.value);%n");
                break;

            case NewArray:
                outAssertMode(AdviceMode.AFTER.ordinal());
                out.printf("                ObjectAdviceRecord record = (ObjectAdviceRecord) thisRecord;%n");
                out.printf("                logHandler.adviseAfterNewArray(record.value, getPackedValue(record));%n");
                break;

            case Unseen:
                out.printf("                ObjectAdviceRecord record = (ObjectAdviceRecord) thisRecord;%n");
                out.printf("                logHandler.unseenObject(record.value);%n");
                break;

            case Removal:
                out.printf("                logHandler.removal(getPackedValue(thisRecord));%n");
                break;

            case ConversionLong:
            case ConversionFloat:
            case ConversionDouble:
                splitType = splitType(e, "Conversion");
                outAssertMode(AdviceMode.BEFORE.ordinal());
                out.printf("                %sAdviceRecord record = (%sAdviceRecord) thisRecord;%n", splitType[1], splitType[1]);
                out.printf("                logHandler.adviseBefore%s(getPackedValue(record), record.value);%n", splitType[0]);
                break;

            case ReturnFloat:
            case ReturnLong:
            case ReturnDouble:
            case ReturnObject:
                splitType = splitType(e, "Return");
                outAssertMode(AdviceMode.BEFORE.ordinal());
                out.printf("                %sAdviceRecord record = (%sAdviceRecord) thisRecord;%n", splitType[1], splitType[1]);
                out.printf("                logHandler.adviseBefore%s(record.value);%n", splitType[0]);
                break;

            case ConstLoadLong:
            case ConstLoadFloat:
            case ConstLoadDouble:
            case ConstLoadObject:
                splitType = splitType(e, "ConstLoad");
                outAssertMode(AdviceMode.BEFORE.ordinal());
                out.printf("                %sAdviceRecord record = (%sAdviceRecord) thisRecord;%n", splitType[1], splitType[1]);
                out.printf("                logHandler.adviseBefore%s(record.value);%n", splitType[0]);
                break;

            case StoreLong:
            case StoreFloat:
            case StoreDouble:
            case StoreObject:
                splitType = splitType(e, "Store");
                outAssertMode(AdviceMode.BEFORE.ordinal());
                out.printf("                %sAdviceRecord record = (%sAdviceRecord) thisRecord;%n", splitType[1], splitType[1]);
                out.printf("                logHandler.adviseBefore%s(getPackedValue(record), record.value);%n", splitType[0]);
                break;

            case ArrayLoad:
            case ArrayLength:
                outAssertMode(AdviceMode.BEFORE.ordinal());
                out.printf("                ObjectAdviceRecord record = (ObjectAdviceRecord) thisRecord;%n");
                out.printf("                logHandler.adviseBefore%s(record.value, getArrayIndex(record));%n", e.name());
                break;

            case GC:
                outAssertMode(AdviceMode.AFTER.ordinal());
                out.printf("                logHandler.adviseAfter%s();%n", e.name());
                break;

            case Bytecode:
            case IPush:
            case Load:
            case Return:
            case StackAdjust:
                outAssertMode(AdviceMode.BEFORE.ordinal());
                out.printf("                logHandler.adviseBefore%s(getPackedValue(thisRecord));%n", e.name());
                break;

            case OperationDouble:
            case OperationLong:
            case OperationFloat:
                splitType = splitType(e, "Operation");
                outAssertMode(AdviceMode.BEFORE.ordinal());
                out.printf("                %s%sAdviceRecord record = (%s%sAdviceRecord) thisRecord;%n",
                                splitType[1], splitType[1], splitType[1], splitType[1]);
                out.printf("                logHandler.adviseBefore%s(getPackedValue(record), record.value, record.value2);%n", splitType[0]);
                break;

            case IInc:
            case IfInt:
                outAssertMode(AdviceMode.BEFORE.ordinal());
                out.printf("                LongLongAdviceRecord record = (LongLongAdviceRecord) thisRecord;%n");
                out.printf("                logHandler.adviseBefore%s(getPackedValue(record), (int) record.value, (int) record.value2);%n", e == RecordType.IfInt ? "If" : e.name());
                break;

            case CheckCast:
            case InstanceOf:
                outAssertMode(AdviceMode.BEFORE.ordinal());
                out.printf("                ObjectObjectAdviceRecord record = (ObjectObjectAdviceRecord) thisRecord;%n");
                out.printf("                logHandler.adviseBefore%s(record.value, record.value2);%n", e.name());
                break;

            case InvokeInterface:
            case InvokeSpecial:
            case InvokeStatic:
            case InvokeVirtual:
                out.printf("                ObjectMethodActorAdviceRecord record = (ObjectMethodActorAdviceRecord) thisRecord;%n");
                out.printf("                if (getAdviceMode(thisRecord) == 0) {%n");
                out.printf("                    logHandler.adviseBefore%s(record.value, record.value2);%n", e.name());
                out.printf("                } else {%n");
                out.printf("                    logHandler.adviseAfter%s(record.value, record.value2);%n", e.name());
                out.printf("                }%n");
                break;

            case IfObject:
                outAssertMode(AdviceMode.BEFORE.ordinal());
                out.printf("                ObjectObjectAdviceRecord record = (ObjectObjectAdviceRecord) thisRecord;%n");
                out.printf("                logHandler.adviseBeforeIf(getPackedValue(record), record.value, record.value2);%n");
                break;

            case MultiNewArray:
                out.printf("                assert false;%n");
                break;

            case MonitorEnter:
            case MonitorExit:
            case Throw:
                outAssertMode(AdviceMode.BEFORE.ordinal());
                out.printf("                ObjectAdviceRecord record = (ObjectAdviceRecord) thisRecord;%n");
                out.printf("                logHandler.adviseBefore%s(record.value);%n", e.name());
                break;

            default:
                assert false : "unimplemented case " + e;

        }
        out.printf("                break;%n");
        out.printf("            }%n");
    }

    private static void outAssertMode(int mode) {
        out.printf("                assert getAdviceMode(thisRecord) == %d;%n", mode);
    }

    private static String[] splitType(RecordType e, String k) {
        final String[] result = new String[2];
        final String name = e.name();
        final int sx = name.indexOf(k);
        final int ex = sx + k.length();
        result[0] = name.substring(0, ex);
        result[1] = name.substring(ex);
        return result;
    }
}

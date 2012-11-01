/*
 * Copyright (c) 2004, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vma.tools.qa;

import static com.oracle.max.vma.tools.qa.TransientVMAdviceHandlerTypes.RecordType.*;

import java.io.*;
import java.util.*;

import com.oracle.max.vm.ext.vma.*;
import com.oracle.max.vma.tools.qa.TransientVMAdviceHandlerTypes.AdviceRecord;
import com.oracle.max.vma.tools.qa.TransientVMAdviceHandlerTypes.*;

/**
 * Finesses the use of {@link Object} in {@link TransientVMAdviceHandlerTypes} when at analysis
 * time the value contains either a {@link ThreadRecord}, {@link FieldRecord} or {@link MethodRecord}.
 */
public class AdviceRecordHelper {

    public enum AccessType {
        READ, WRITE;

        public String getName() {
            return this.name().toLowerCase();
        }

        @Override
        public String toString() {
            return getName();
        }
    }

    public static FieldRecord getField(AdviceRecord ar) {
        ObjectFieldAdviceRecord far = (ObjectFieldAdviceRecord) ar;
        return (FieldRecord) far.field;
    }

    public static ObjectRecord getObjectRecord(AdviceRecord ar) {
        ObjectAdviceRecord far = (ObjectAdviceRecord) ar;
        return (ObjectRecord) far.value;
    }

    public static ClassRecord getClassRecord(AdviceRecord ar) {
        ObjectAdviceRecord far = (ObjectAdviceRecord) ar;
        return (ClassRecord) far.value;
    }

    public static MethodRecord getMethod(AdviceRecord ar) {
        ObjectMethodAdviceRecord far = (ObjectMethodAdviceRecord) ar;
        return (MethodRecord) far.value2;
    }

    public static ThreadRecord getThread(AdviceRecord ar) {
        return (ThreadRecord) ar.thread;
    }

    public static AccessType accessType(AdviceRecord ar) {
        if (RecordType.MODIFY_OPERATIONS.contains(ar.getRecordType())) {
            return AccessType.WRITE;
        } else {
            return AccessType.READ;
        }
    }

    public static int print(QueryBase qb, TraceRun traceRun, PrintStream ps, AdviceRecord ar, int showIndex, int indent, boolean newline) {
        RecordType rt = ar.getRecordType();
        if (showIndex >= 0) {
            ps.printf("[%d] ", showIndex);
        }
        ps.printf("%-10d %s %c%s %s ", qb.timeValue(traceRun, ar.time), ar.thread, adviceId(ar), toBci(ar), rt);
        switch (rt) {
            case GC:
            case ThreadStarting:
            case ThreadTerminating:
                break;
            case ConstLoadLong:
            case ConstLoadObject:
            case ConstLoadFloat:
            case ConstLoadDouble:
                printValue(ps, rt, ar);
                break;

            case Load:
                ps.print(ar.getPackedValue());
                break;

            case StoreLong:
            case StoreFloat:
            case StoreDouble:
            case StoreObject:
                ps.print(ar.getPackedValue());
                printValue(ps, rt, ar);
                break;

            case ArrayLoad:
            case ArrayStoreFloat:
            case ArrayStoreLong:
            case ArrayStoreDouble:
            case ArrayStoreObject:
                ps.printf("%s[%d]", getObjectRecord(ar), ar.getArrayIndex());
                if (rt != ArrayLoad) {
                    ps.print(' ');
                    printValue(ps, rt, ar);
                }
                break;

            case OperationLong:
            case OperationFloat:
            case OperationDouble:
                printValue(ps, rt, ar);
                ps.printf(" %s ", VMABytecodes.values()[ar.getPackedValue()]);
                switch (rt) {
                    case OperationLong:
                        ps.print(((LongLongAdviceRecord) ar).value2);
                        break;
                    case OperationDouble:
                        ps.print(((DoubleDoubleAdviceRecord) ar).value2);
                        break;
                    case OperationFloat:
                        ps.print(((FloatFloatAdviceRecord) ar).value2);
                        break;
                }
                break;

            case ConversionLong:
            case ConversionFloat:
            case ConversionDouble:
                ps.printf(" %s ", VMABytecodes.values()[ar.getPackedValue()]);
                printValue(ps, rt, ar);
                break;

            case IfInt: {
                LongLongTBciAdviceRecord llar = (LongLongTBciAdviceRecord) ar;
                ps.printf("%s %d %d -> %d", VMABytecodes.values()[llar.getPackedValue()], llar.value, llar.value2,
                                llar.targetBci);
                break;
            }

            case IfObject: {
                ObjectObjectAdviceRecord ooar = (ObjectObjectAdviceRecord) ar;
                ps.printf("%s %s %s", VMABytecodes.values()[ooar.getPackedValue()], ooar.value, ooar.value2);
                break;
            }

            case Goto: {
                ps.printf("-> %d", ar.getPackedValue());
                break;
            }

            case ReturnObject:
            case ReturnLong:
            case ReturnFloat:
            case ReturnDouble:
                printValue(ps, rt, ar);
                indent--;
                break;
            case Return:
                indent--;
                break;

            case ReturnByThrow: {
                int pop = ar.getPackedValue();
                ps.printf("%s %d", getObjectRecord(ar), pop);
                indent -= pop;
                break;
            }

            case GetStatic:
            case PutStaticDouble:
            case PutStaticLong:
            case PutStaticFloat:
            case PutStaticObject:
            case GetField:
            case PutFieldDouble:
            case PutFieldLong:
            case PutFieldFloat:
            case PutFieldObject:
                ps.print(getField(ar).getQualName());
                if (rt != GetField && rt != GetStatic) {
                    printValue(ps, rt, ar);
                }
                break;

            case InvokeVirtual:
            case InvokeSpecial:
            case InvokeStatic:
            case InvokeInterface:
                ps.print(getMethod(ar).getQualName());
                if (rt != InvokeStatic) {
                    ps.printf("(%s)", getObjectRecord(ar));
                }
                break;

            case MethodEntry: {
                ps.print(getMethod(ar).getQualName());
                ObjectRecord oar = getObjectRecord(ar);
                if (oar != null) {
                    ps.printf("(%s)", oar);
                }
                indent++;
                break;
            }

            case StackAdjust:
                ps.printf("%s", VMABytecodes.values()[ar.getPackedValue()]);
                break;

            case ArrayLength:
                ps.printf("%d", ar.getPackedValue());
                break;

            case CheckCast:
            case InstanceOf:
                ObjectObjectAdviceRecord ooar = (ObjectObjectAdviceRecord) ar;
                ps.printf("%s %s", ooar.value, ooar.value2);
                break;

            case Throw:
            case MonitorEnter:
            case MonitorExit:
                ps.print(getObjectRecord(ar));
                break;

            case Removal:
            case Unseen:
            case New:
            case NewArray:
                ps.print(getObjectRecord(ar));
                if (rt == NewArray) {
                    ps.printf("%d", ar.getPackedValue());
                }
                break;

            case MultiNewArray:
                assert false : "MultiNewArray unexpected";

        }
        if (newline) {
            ps.println();
        }
        return indent;
    }

    private static void printValue(PrintStream ps, RecordType rt, AdviceRecord ar) {
        ps.print(' ');
        switch (rt) {
            case PutFieldObject:
            case PutStaticObject:
                ps.print(((ObjectFieldObjectAdviceRecord) ar).value);
                break;
            case ConstLoadObject:
            case StoreObject:
            case ReturnObject:
                ps.print(((ObjectAdviceRecord) ar).value);
                break;
            case ArrayStoreObject:
                ps.print(((ObjectObjectAdviceRecord) ar).value2);
                break;

            case PutFieldLong:
            case PutStaticLong:
                ps.print(((ObjectFieldLongAdviceRecord) ar).value2);
                break;
            case ArrayStoreLong:
                ps.print(((ObjectLongAdviceRecord) ar).value2);
                break;

            case ConstLoadLong:
            case StoreLong:
            case ConversionLong:
            case ReturnLong:
                ps.print(((LongAdviceRecord) ar).value);
                break;
            case OperationLong:
                ps.print(((LongLongAdviceRecord) ar).value);
                break;
            case PutFieldFloat:
            case PutStaticFloat:
                ps.print(((ObjectFieldFloatAdviceRecord) ar).value2);
                break;
            case ArrayStoreFloat:
                ps.print(((ObjectFloatAdviceRecord) ar).value2);
                break;
            case ConstLoadFloat:
            case StoreFloat:
            case ConversionFloat:
            case ReturnFloat:
                ps.print(((FloatAdviceRecord) ar).value);
                break;
            case OperationFloat:
                ps.print(((FloatFloatAdviceRecord) ar).value);
                break;
            case PutFieldDouble:
            case PutStaticDouble:
                ps.print(((ObjectFieldDoubleAdviceRecord) ar).value2);
                break;
            case ArrayStoreDouble:
                ps.print(((ObjectDoubleAdviceRecord) ar).value2);
                break;
            case ConstLoadDouble:
            case StoreDouble:
            case ConversionDouble:
            case ReturnDouble:
                ps.print(((DoubleAdviceRecord) ar).value);
                break;
            case OperationDouble:
                ps.print(((DoubleDoubleAdviceRecord) ar).value);
                break;

        }
    }

    private static char adviceId(AdviceRecord ar) {
        return AdviceMode.values()[ar.getAdviceModeAsInt()].name().charAt(0);
    }

    private static String toBci(AdviceRecord ar) {
        switch (ar.getRecordType()) {
            case GC:
            case ThreadStarting:
            case ThreadTerminating:
            case Removal:
                return "";

            default:
                return " " + Short.toString(ar.getBci());
        }
    }

    /**
     * Find the index of the given {@link AdviceRecord} in {@code adviceRecordList}.
     * @param ar
     * @return
     */
    public static int getRecordListIndex(ArrayList<AdviceRecord> adviceRecordList, AdviceRecord ar) {
        // list is sorted by time, so binary search.
        int lwb = 0;
        int upb = adviceRecordList.size() - 1;
        while (lwb <= upb) {
            int mid = (lwb + upb) >>> 1;
            AdviceRecord candidate = adviceRecordList.get(mid);
            if (candidate == ar) {
                return mid;
            } else if (candidate.time < ar.time) {
                lwb = mid + 1;
            } else if (candidate.time > ar.time) {
                upb = mid - 1;
            } else {
                // equal but several records (either side of index) may have the same time
                int sindex = mid;
                while (sindex >= 0 && candidate.time == ar.time) {
                    if (candidate == ar) {
                        return sindex;
                    }
                    candidate = adviceRecordList.get(--sindex);
                }
                sindex = mid;
                candidate = adviceRecordList.get(sindex);
                while (sindex < adviceRecordList.size() && candidate.time == ar.time) {
                    if (candidate == ar) {
                        return sindex;
                    }
                    candidate = adviceRecordList.get(++sindex);
                }
                return -1; // fail, should never happen
            }
        }
        return -1;
    }

}

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
package com.oracle.max.vma.tools.qa.queries;

import static com.oracle.max.vma.tools.qa.AdviceRecordHelper.*;
import static com.oracle.max.vma.tools.qa.TransientVMAdviceHandlerTypes.RecordType.*;

import java.io.*;
import java.util.*;

import com.oracle.max.vm.ext.vma.*;
import com.oracle.max.vma.tools.qa.*;
import com.oracle.max.vma.tools.qa.TransientVMAdviceHandlerTypes.*;


public class AdviceRecordsQuery extends QueryBase {
    @Override
    public Object execute(ArrayList<TraceRun> traceRuns, int traceFocus, PrintStream ps, String[] args) {
        TraceRun traceRun = traceRuns.get(traceFocus);
        long chunkStartTime = System.currentTimeMillis();
        long processStartTime = chunkStartTime;
        long count = 0;
        for (AdviceRecord ar : traceRun.adviceRecordList) {
            RecordType rt = ar.getRecordType();
            ps.printf("%-10d %s %c%s %s ", timeValue(traceRun, ar.time), ar.thread, adviceId(ar), toBci(ar), rt);
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
                    break;
                case Return:
                    break;

                case GetStatic:
                case PutStaticDouble:
                case PutStaticLong:
                case PutStaticFloat:
                case PutStaticObject:
                    ps.print(getField(ar).getQualName());
                    break;

                case GetField:
                case PutFieldDouble:
                case PutFieldLong:
                case PutFieldFloat:
                case PutFieldObject:
                    ps.print(getField(ar).getQualName());
                    printValue(ps, rt, ar);
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

                case ReturnByThrow:
                    ps.printf("%s %d", getObjectRecord(ar), ar.getPackedValue());
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
            ps.println();
            count++;
            if (verbose && ((count % 100000) == 0)) {
                long endTime = System.currentTimeMillis();
                System.out.printf("processed %d records in %d ms (%d)%n", count, endTime - processStartTime, endTime - chunkStartTime);
                chunkStartTime = endTime;
            }
        }
        return null;
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
        return AdviceMode.values()[ar.getAdviceMode()].name().charAt(0);
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
}

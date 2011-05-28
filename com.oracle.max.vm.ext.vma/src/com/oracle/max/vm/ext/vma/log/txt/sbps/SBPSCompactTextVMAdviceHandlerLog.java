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

import com.oracle.max.vm.ext.vma.log.txt.*;

/**
 * A compact variant of {@link SBPSTextVMAdviceHandlerLog} using {@link CompactTextVMAdviceHandlerLog}.
 *
 * @author Mick Jordan
 *
 */


public class SBPSCompactTextVMAdviceHandlerLog extends CompactTextVMAdviceHandlerLog {

    private final SBPSTextVMAdviceHandlerLog jdel;

    public SBPSCompactTextVMAdviceHandlerLog() {
        super(new SBPSTextVMAdviceHandlerLog());
        this.jdel = (SBPSTextVMAdviceHandlerLog) del;
    }

    @Override
    public void classDefinitionTracking(String className, String shortForm,
            long clId) {
        synchronized (jdel) {
            jdel.sb.append(CLASS_DEFINITION_ID);
            jdel.sb.append(' ');
            jdel.sb.append(className);
            jdel.sb.append(' ');
            jdel.sb.append(shortForm);
            jdel.sb.append(' ');
            jdel.sb.append(clId);
            jdel.end();
        }
    }

    @Override
    public void classDefinitionFieldTracking(String fieldName,
            String shortForm) {
        synchronized (jdel) {
            jdel.sb.append(FIELD_DEFINITION_ID);
            jdel.sb.append(' ');
            jdel.sb.append(fieldName);
            jdel.sb.append(' ');
            jdel.sb.append(shortForm);
            jdel.end();
        }
    }

    @Override
    public void threadDefinitionTracking(String threadName, String shortForm) {
        synchronized (jdel) {
            jdel.sb.append(THREAD_DEFINITION_ID);
            // thread names can have spaces, so quote
            jdel.sb.append(" \"");
            jdel.sb.append(threadName);
            jdel.sb.append("\" ");
            jdel.sb.append(shortForm);
            jdel.end();
        }
    }


}

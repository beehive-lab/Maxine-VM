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
package com.oracle.max.vm.ext.vma.log;

import com.oracle.max.vm.ext.vma.log.txt.sbps.*;

/**
 * Factory for controlling which subclass of {@link VMAdviceHandlerLog} is used.
 *
 * The default choice is {@link SBPSCompactTextVMAdviceHandlerLog} but this
 * can be changed with the {@value LOGCLASS_PROPERTY} system property,
 * which should be the fully qualified name of the class.
 */

public class VMAdviceHandlerLogFactory {
    public static final String LOGCLASS_PROPERTY = "max.vma.logclass";

    public static VMAdviceHandlerLog create() {
        VMAdviceHandlerLog result = null;
        final String logClass = System.getProperty(LOGCLASS_PROPERTY);
        if (logClass == null) {
            result = new SBPSCompactTextVMAdviceHandlerLog();
        } else {
            try {
                result = (VMAdviceHandlerLog) Class.forName(logClass).newInstance();
            } catch (Exception exception) {
                System.err.println("Error instantiating " + logClass + ": "
                        + exception);
            }
        }
        return result;
    }

}

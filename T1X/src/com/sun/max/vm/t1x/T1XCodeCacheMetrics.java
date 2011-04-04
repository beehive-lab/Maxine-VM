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
package com.sun.max.vm.t1x;

import com.sun.max.vm.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.profile.*;

/**
 * An instance of this class is added as a VM shutdown hook if the -T1X:+PrintCodeCacheMetrics option is given.
 * It prints usage data related to the code cache for T1X-compiled methods, i.e., the size of the method (bytecode and
 * machine code), and invocation count.
 */
public class T1XCodeCacheMetrics extends Thread {

    @Override
    public void run() {
        final CodeManager codeManager = Code.getCodeManager();
        final CodeRegion runtimeCodeRegion = codeManager.getRuntimeCodeRegion();
        Log.println("#bc\t#mc\t#ent\tname");
        for (TargetMethod targetMethod : runtimeCodeRegion.currentTargetMethods()) {
            if (targetMethod instanceof T1XTargetMethod) {
                int bcSize = targetMethod.classMethodActor().code().length;
                int mcSize = targetMethod.codeLength();
                int entries = MethodInstrumentation.initialEntryCount - ((T1XTargetMethod) targetMethod).methodProfile.entryCount;
                Log.println(String.format("%d\t%d\t%d\t%s", bcSize, mcSize, entries, targetMethod));
            }
        }
    }

}

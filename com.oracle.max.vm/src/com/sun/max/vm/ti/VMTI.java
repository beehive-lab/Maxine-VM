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
package com.sun.max.vm.ti;

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.hosted.*;


/**
 * Virtual Machine Tooling Interface setup and handler facade.
 */
public class VMTI {
    @CONSTANT
    private static VMTIHandler handler;

    @INLINE
    public static VMTIHandler handler() {
        if (MaxineVM.isHosted()) {
            // this is a bit ugly but, since InitializationCompleteCallbacks are unordered,
            // we can get called by some other callback before ours has been invoked.
            if (handler == null) {
                initializationCompleteCallback.initializationComplete();
            }
        }
        return handler;
    }

    @HOSTED_ONLY
    private static class InitializationCompleteCallback implements JavaPrototype.InitializationCompleteCallback {

        @Override
        public void initializationComplete() {
            if (handler == null) {
                int size = hostedHandlers.size();
                if (size > 2) {
                    handler = new MDVMTIHandler(hostedHandlers);
                } else if (size > 1) {
                    handler = new DDVMTIHandler(hostedHandlers.get(0), hostedHandlers.get(1));
                } else if (size > 0) {
                    handler = new SDVMTIHandler(hostedHandlers.get(0));
                } else {
                    handler = new NullVMTIHandler();
                }
            }
        }

    }

    @HOSTED_ONLY
    private static final JavaPrototype.InitializationCompleteCallback initializationCompleteCallback = JavaPrototype.registerInitializationCompleteCallback(new InitializationCompleteCallback());

    @HOSTED_ONLY
    private static ArrayList<VMTIHandler> hostedHandlers = new ArrayList<VMTIHandler>();

    @HOSTED_ONLY
    public static void registerEventHandler(VMTIHandler eventHandler) {
        ProgramError.check(handler == null, "too late to register a VMTI Handler");
        hostedHandlers.add(eventHandler);
    }
}

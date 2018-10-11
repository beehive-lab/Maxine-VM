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
package demo.vmext.fatal;

import java.util.*;

import com.sun.max.vm.*;
import com.sun.max.vm.runtime.FatalError;

/**
 * An extension that provokes a FatalError some time after the VM has started (default 5 seconds).
 * Tests the behavior of the VM in that case.
 */
public class FatalVMExtension {
    private static final int UNEXPECTED = 1;
    private static int crashMode;

    private static class MyTimerTask extends TimerTask {

        @Override
        public void run() {
            if (crashMode == UNEXPECTED) {
                FatalError.unexpected("FatalError.unexpected induced by FatalError VM extension");
            } else {
                FatalError.crash("FatalError.crash induced by FatalError VM extension");
            }
        }

    }

    public static void onLoad(String extArg) {
        long delay = 5;
        if (extArg != null) {
            String[] args = extArg.split(",");
            for (String arg : args) {
                if (arg.startsWith("delay")) {
                    delay = Long.parseLong(getValue(arg));
                } else if (arg.startsWith("mode")) {
                    String mode = getValue(arg);
                    if (mode.equals("crash")) {
                        // default
                    } else if (mode.equals("unexpected")) {
                        crashMode = UNEXPECTED;
                    } else {
                        usage();
                    }
                }
            }
        }
        MyTimerTask myTimerTask = new MyTimerTask();
        if (delay == 0) {
            myTimerTask.run();
        } else {
            new Timer(true).schedule(myTimerTask, delay * 1000);
        }
    }

    private static String getValue(String arg) {
        int ix = arg.indexOf('=');
        if (ix < 0) {
            usage();
            return null;
        }
        return arg.substring(ix + 1);
    }

    private static void usage() {
        Log.println("argument syntax: delay=n,mode=crash|unexpected");
        MaxineVM.native_exit(1);
    }

}

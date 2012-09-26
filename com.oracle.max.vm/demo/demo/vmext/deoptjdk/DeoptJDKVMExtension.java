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
package demo.vmext.deoptjdk;

import java.util.*;
import java.util.regex.*;

import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.deopt.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.type.*;

/**
 * A VM extension that attempt to deoptimize (a subset of) the JDK classes in the boot image.
 */
public class DeoptJDKVMExtension {
    private static boolean verbose;
    private static boolean list;

    public static void onLoad(String extArg) {
        String includeClassPatternString = ".*";
        String excludeClassPatternString = null;
        if (extArg != null) {
            String[] args = extArg.split(",");
            for (int i = 0; i < args.length; i++) {
                final String arg = args[i];
                if (arg.startsWith("ci")) {
                    includeClassPatternString = getValue(arg);
                } else if (arg.startsWith("cx")) {
                    excludeClassPatternString = getValue(arg);
                } else if (arg.startsWith("v")) {
                    verbose = true;
                } else if (arg.startsWith("l")) {
                    list = true;
                }
            }
        }

        Pattern includeClassPattern = Pattern.compile(includeClassPatternString);
        Pattern excludeClassPattern = excludeClassPatternString == null ? null : Pattern.compile(excludeClassPatternString);

        Collection<ClassActor> bootClassActors = ClassRegistry.BOOT_CLASS_REGISTRY.getClassActors();
        if (list) {
            listMethodStates(bootClassActors);
        }

        ArrayList<TargetMethod> deoptMethods = new ArrayList<TargetMethod>();
        for (ClassActor classActor : bootClassActors) {
            String className = classActor.qualifiedName();
            if (includeClassPattern.matcher(className).matches() && (excludeClassPattern == null || !excludeClassPattern.matcher(className).matches())) {
                for (StaticMethodActor staticMethodActor : classActor.localStaticMethodActors()) {
                    TargetMethod tm = isOpt(staticMethodActor);
                    if (tm != null) {
                        deoptMethods.add(tm);
                    }
                }
                for (VirtualMethodActor virtualMethodActor : classActor.localVirtualMethodActors()) {
                    TargetMethod tm = isOpt(virtualMethodActor);
                    if (tm != null) {
                        deoptMethods.add(tm);
                    }
                }
            }
        }

        new Deoptimization(deoptMethods).go();

    }

    private static TargetMethod isOpt(ClassMethodActor classMethodActor) {
        TargetMethod tm = classMethodActor.currentTargetMethod();
        if (tm != null && !tm.isBaseline()) {
            return tm;
        } else {
            return null;
        }
    }

    private static void listMethodStates(Collection<ClassActor> bootClassActors) {
        for (ClassActor classActor : bootClassActors) {
            Log.println(classActor.qualifiedName());
            for (StaticMethodActor staticMethodActor : classActor.localStaticMethodActors()) {
                listMethod(staticMethodActor);
            }
            for (VirtualMethodActor virtualMethodActor : classActor.localVirtualMethodActors()) {
                listMethod(virtualMethodActor);
            }
        }
    }

    private static void listMethod(ClassMethodActor classMethodActor) {
        TargetMethod tm = classMethodActor.currentTargetMethod();
        if (tm != null) {
            Log.print(classMethodActor.format("  %H.%n(%p)"));
            Log.print(" compiled ");
            Log.print(tm.getClass().getSimpleName());
            Log.println();
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
        Log.println("argument syntax: [ci=pattern,cx=pattern,v]");
        MaxineVM.native_exit(1);
    }


}

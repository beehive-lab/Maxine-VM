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

import static com.sun.max.vm.MaxineVM.*;

import java.io.*;
import java.lang.ref.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.regex.*;

import com.sun.max.vm.*;
import com.sun.max.vm.actor.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.RuntimeCompiler.*;
import com.sun.max.vm.compiler.deopt.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.object.ObjectAccess;
import com.sun.max.vm.runtime.*;
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
                    checkDeopt(staticMethodActor, deoptMethods);
                }
                for (VirtualMethodActor virtualMethodActor : classActor.localVirtualMethodActors()) {
                    checkDeopt(virtualMethodActor, deoptMethods);
                }
            }
        }

        // Force the compilation of methods not compiled into the boot image when using C1X that
        // are needed when T1X uses a T1X-compiled JDK, to break meta-circularity.
        // This list was experimentally determined.
        forceCompile(HashMap.class, "hash");
        forceCompile(HashMap.class, "indexFor");
        forceCompile(getClass("java.util.HashMap$Entry"), "<init>");
        forceCompile(ObjectAccess.class, "makeHashCode");
        forceCompile(AbstractList.class, "<init>");
        forceCompile(AbstractCollection.class, "<init>");
        forceCompile(ArrayList.class, "rangeCheck");
        forceCompile(ArrayList.class, "ensureCapacityInternal");
        forceCompile(Array.class, "newInstance");
        forceCompile(Math.class, "min", SignatureDescriptor.fromJava(int.class, int.class, int.class));
        forceCompile(ThreadLocal.class, "access$400");
        forceCompile(getClass("java.lang.ThreadLocal$ThreadLocalMap"), "access$000");
        forceCompile(getClass("java.lang.ThreadLocal$ThreadLocalMap"), "setThreshold");
        forceCompile(getClass("java.lang.ThreadLocal$ThreadLocalMap$Entry"), "<init>");
        forceCompile(Enum.class, "ordinal");
        forceCompile(EnumMap.class, "unmaskNull");
        forceCompile(FilterInputStream.class, "<init>");
        forceCompile(Reference.class, "<init>");
        forceCompile(Reference.class, "access$100");
        forceCompile(WeakReference.class, "<init>");

        // Compile the methods first, in case of a method used by the compilation system,
        // which would cause runaway recursion.
        Iterator<TargetMethod> iter = deoptMethods.iterator();
        while (iter.hasNext()) {
            TargetMethod tm = iter.next();
            try {
                vm().compilationBroker.compile(tm.classMethodActor, Nature.BASELINE, false, true);
            } catch (Throwable t) {
                // some failure that (likely) can't easily be expressed in cantBaseline
                Log.print("can't baseline compile ");
                Log.println(tm.classMethodActor.format(" %H.%n(%p) " + t));
                iter.remove();
            }
        }
        new Deoptimization(deoptMethods).go();

    }

    private static Class<?> getClass(String name) {
        try {
            return Class.forName(name);
        } catch (Throwable t) {
            FatalError.unexpected("can't find class " + name);
            return null;
        }
    }

    private static void forceCompile(Class<?> klass, String methodName) {
        forceCompile(klass, methodName, null);
    }

    private static void forceCompile(Class<?> klass, String methodName, SignatureDescriptor sig) {
        ClassActor.fromJava(klass).findLocalClassMethodActor(
                        SymbolTable.makeSymbol(methodName), sig).makeTargetMethod();
    }

    private static void checkDeopt(ClassMethodActor classMethodActor, ArrayList<TargetMethod> deoptMethods) {
        TargetMethod tm = classMethodActor.currentTargetMethod();
        if (tm != null && !tm.isBaseline()) {
            if (cantBaseline(classMethodActor)) {
                Log.print("can't baseline compile ");
                Log.println(classMethodActor.format(" %H.%n(%p)"));
            } else {
                deoptMethods.add(tm);
            }
        }
    }

    private static boolean cantBaseline(ClassMethodActor classMethodActor) {
        ClassMethodActor compilee = classMethodActor.compilee();
        return Actor.isUnsafe(compilee.flags()) || (compilee.flags() & (Actor.FOLD | Actor.INLINE)) != 0;
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

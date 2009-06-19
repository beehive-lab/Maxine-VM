/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.max.vm.prototype;

import java.io.*;
import java.util.*;
import java.util.Arrays;

import com.sun.max.lang.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.jit.*;
import com.sun.max.vm.object.host.*;
import com.sun.max.vm.prototype.GraphPrototype.*;

/**
 * This class collects and reports statistics about the size of a {@link GraphPrototype}, which
 * contains objects, classes, and code.
 *
 * @author Ben L. Titzer
 */
public class GraphStats {

    static class ClassStats {
        final GraphPrototype.ClassInfo _classInfo;
        int _objectCount;
        int _objectSize;
        int _hubSize;
        int _classActorSize;
        int _staticSize;
        int _staticHubSize;
        int _methodsSize;
        int _targetSize;
        int _methodsCount;

        ClassStats(GraphPrototype.ClassInfo classInfo) {
            this._classInfo = classInfo;
        }

        int classSize() {
            return _hubSize + _classActorSize + _staticSize + _staticHubSize + _methodsSize + _targetSize;
        }
    }

    static class MethodStats {
        final MethodActor _methodActor;
        int _actorSize;
        int _bytecodeSize;
        int _targetMethodSize;
        int _targetCodeSize;

        MethodStats(MethodActor methodActor) {
            _methodActor = methodActor;
        }

        int methodSize() {
            return _actorSize + _bytecodeSize + _targetMethodSize + _targetCodeSize;
        }
    }

    private final GraphPrototype _graphPrototype;
    private final Map<MethodActor, MethodStats> _methodStats = new HashMap<MethodActor, MethodStats>();

    public GraphStats(GraphPrototype graphPrototype) {
        _graphPrototype = graphPrototype;
    }

    public void dumpStats(PrintStream printStream) {
        printObjectStats(printStream, computeObjectStats());
        printClassStats(printStream, computeClassStats());
        printMethodStats(printStream);
    }

    private void printObjectStats(PrintStream printStream, final int total) {
        final ClassInfo[] classInfos = _graphPrototype._classInfos.values().toArray(new ClassInfo[0]);
        Arrays.sort(classInfos, BY_OBJECT_SIZE);
        printStream.println("Object Histogram End");
        printStream.println("Cumul     Size                      Objects      Avg        Class");
        printStream.println("==============================================================================");
        long cumul = 0;
        for (ClassInfo info : classInfos) {
            final ClassStats s = getClassStats(info);
            if (s._objectCount != 0) {
                cumul += s._objectSize;
                final String fixedDouble = Strings.padLengthWithSpaces(6, Strings.fixedDouble(cumul * 100.0d / total, 2));
                printStream.printf("(%s%%) %-10d (%6d kb) / %-10d = %-10d %s\n", fixedDouble, s._objectSize, s._objectSize / 1024, s._objectCount, s._objectSize / s._objectCount, info._clazz.getName());
            }
        }
        printStream.println("Object Histogram End\n");
    }

    private int computeObjectStats() {
        int total = 0;
        for (Object o : _graphPrototype._objects) {
            final ClassInfo classInfo = _graphPrototype._classInfos.get(o.getClass());
            final ClassStats classStats = getClassStats(classInfo);
            classStats._objectCount++;
            final int size = sizeOf(o);
            classStats._objectSize += size;
            total += size;
        }
        return total;
    }

    private static ClassStats getClassStats(final ClassInfo classInfo) {
        ClassStats classStats = classInfo._stats;
        if (classStats == null) {
            classStats = new ClassStats(classInfo);
            classInfo._stats = classStats;
        }
        return classStats;
    }

    private void printClassStats(PrintStream printStream, int total) {
        final ClassInfo[] classInfos = _graphPrototype._classInfos.values().toArray(new ClassInfo[0]);
        Arrays.sort(classInfos, BY_CLASS_SIZE);
        printStream.println("Class Histogram Start");
        printStream.println("Total      Hub        Actor      StHub      Static     MethSize   TargSize   Count            Class");
        printStream.println("==============================================================================");
        for (ClassInfo info : classInfos) {
            final ClassStats s = getClassStats(info);
            printStream.printf("%-10d %-10d %-10d %-10d %-10d %-10d %-10d %-6d %s\n", s.classSize(), s._hubSize, s._classActorSize,
                s._staticHubSize, s._staticSize, s._methodsSize, s._targetSize, s._methodsCount, info._clazz.getName());
        }
        printStream.println("Class Histogram End\n");
    }

    private void printMethodStats(PrintStream printStream) {
        final MethodStats[] methodStats = _methodStats.values().toArray(new MethodStats[0]);
        Arrays.sort(methodStats, BY_METHOD_SIZE);
        printStream.println("Method Histogram Start");
        printStream.println("Total      Actor      Bytecode   TargMeth   TargCode   Ratio  Method");
        printStream.println("==============================================================================");
        for (MethodStats s : methodStats) {
            final double ratio = s._targetCodeSize / (double) s._bytecodeSize;
            final String rstr = Strings.fixedDouble(ratio, 2);
            printStream.printf("%-10d %-10d %-10d %-10d %-10d %6s %s\n", s.methodSize(), s._actorSize, s._bytecodeSize,
                s._targetMethodSize, s._targetCodeSize, rstr, s._methodActor.toString());
        }
        printStream.println("Method Histogram End\n");
    }

    private int computeClassStats() {
        for (ClassInfo classInfo : _graphPrototype._classInfos.values()) {
            final ClassStats classStats = classInfo._stats;
            final ClassActor classActor = ClassActor.fromJava(classInfo._clazz);
            classStats._hubSize = sizeOf(classActor.dynamicHub());
            classStats._classActorSize = computeClassActorSize(classActor);
            classStats._staticHubSize = sizeOf(classActor.staticHub());
            classStats._staticSize = sizeOf(classActor.staticTuple());
            computeMethodStats(classActor, classStats);
        }
        return 0;
    }

    private int computeClassActorSize(ClassActor classActor) {
        int total = sizeOf(classActor);
        total += nondefaultSize(classActor.allVirtualMethodActors(), ClassActor.NO_VIRTUAL_METHODS);
        total += nondefaultSize(classActor.localInstanceFieldActors(), ClassActor.NO_FIELDS);
        total += nondefaultSize(classActor.localStaticFieldActors(), ClassActor.NO_FIELDS);
        total += nondefaultSize(classActor.localInterfaceActors(), ClassActor.NO_INTERFACES);
        total += nondefaultSize(classActor.localInterfaceMethodActors(), ClassActor.NO_INTERFACE_METHODS);
        total += nondefaultSize(classActor.localStaticMethodActors(), ClassActor.NO_STATIC_METHODS);
        total += nondefaultSize(classActor.localVirtualMethodActors(), ClassActor.NO_VIRTUAL_METHODS);
        // total += classActor.iToV(); TODO: cannot get access to this field
        return total;
    }

    private void computeMethodStats(ClassActor classActor, ClassStats classStats) {
        final Set<MethodActor> methodActors = new HashSet<MethodActor>();
        methodActors.addAll(Arrays.asList(classActor.localInterfaceMethodActors()));
        methodActors.addAll(Arrays.asList(classActor.localStaticMethodActors()));
        methodActors.addAll(Arrays.asList(classActor.localVirtualMethodActors()));
        for (MethodActor methodActor : methodActors) {
            final MethodStats methodStats = computeMethodStats(methodActor);
            classStats._methodsSize += methodStats._actorSize + methodStats._bytecodeSize;
            classStats._targetSize += methodStats._targetCodeSize + methodStats._targetMethodSize;
            classStats._methodsCount++;
        }
    }

    private int computeMethodActorSize(MethodActor methodActor) {
        int total = sizeOf(methodActor);
        if (methodActor instanceof ClassMethodActor) {
            final ClassMethodActor classMethodActor = (ClassMethodActor) methodActor;
            total += computeCodeAttributeSize(classMethodActor.codeAttribute());
        }
        return total;
    }

    private MethodStats computeMethodStats(MethodActor methodActor) {
        final MethodStats methodStats = getMethodStats(methodActor);
        methodStats._actorSize = sizeOf(methodActor);
        if (methodActor instanceof ClassMethodActor) {
            final ClassMethodActor classMethodActor = (ClassMethodActor) methodActor;
            methodStats._bytecodeSize = computeCodeAttributeSize(classMethodActor.codeAttribute());
            final TargetMethod targetMethod = CompilationScheme.Static.getCurrentTargetMethod(classMethodActor);
            if (targetMethod != null) {
                methodStats._targetMethodSize = computeTargetMethodSize(targetMethod);
                methodStats._targetCodeSize = sizeOf(targetMethod.code());
            }
        }
        return methodStats;
    }

    private int computeTargetMethodSize(TargetMethod targetMethod) {
        int total = sizeOf(targetMethod);
        total += sizeOf(targetMethod.code());
        total += sizeOf(targetMethod.catchBlockPositions());
        total += sizeOf(targetMethod.catchRangePositions());
        total += sizeOf(targetMethod.referenceLiterals());
        total += sizeOf(targetMethod.directCallees());
        total += sizeOf(targetMethod.referenceMaps());
        total += sizeOf(targetMethod.scalarLiteralBytes());
        total += sizeOf(targetMethod.stopPositions());
        if (targetMethod instanceof JitTargetMethod) {
            final JitTargetMethod jitTargetMethod = (JitTargetMethod) targetMethod;
            total += sizeOf(jitTargetMethod.bytecodeInfos());
            // total += sizeOf(jitTargetMethod._isDirectCallToRunTime);  TODO
            // total += sizeOf(jitTargetMethod._bytecodeToTargetCodePositionMap); TODO
        }
        return total;
    }

    private int computeCodeAttributeSize(CodeAttribute codeAttribute) {
        if (codeAttribute != null) {
            int total = sizeOf(codeAttribute);
            total += sizeOf(codeAttribute.code());
            total += sizeOf(codeAttribute.encodedData());
            return total;
        }
        return 0;
    }

    private static int nondefaultSize(Object object, Object def) {
        if (object == def) {
            return 0;
        }
        return sizeOf(object);
    }

    private static int sizeOf(Object object) {
        if (object == null) {
            return 0;
        }
        return HostObjectAccess.getSize(object).toInt();
    }

    private MethodStats getMethodStats(MethodActor methodActor) {
        MethodStats methodStats = _methodStats.get(methodActor);
        if (methodStats == null) {
            methodStats = new MethodStats(methodActor);
            _methodStats.put(methodActor, methodStats);
        }
        return methodStats;
    }

    abstract static class ClassInfoComparator implements Comparator<ClassInfo> {
        public int compare(ClassInfo o1, ClassInfo o2) {
            final int m1 = metric(getClassStats(o1));
            final int m2 = metric(getClassStats(o2));
            return m1 < m2 ? 1 : m1 > m2 ? -1 : o1.toString().compareTo(o2.toString());
        }
        public abstract int metric(ClassStats stats);
    }

    /**
     * A comparator that sorts by the cumulative size of objects and then the name.
     */
    static final Comparator<ClassInfo> BY_OBJECT_SIZE = new ClassInfoComparator() {
        @Override
        public int metric(ClassStats stats) {
            return stats._objectSize;
        }
    };

    /**
     * A comparator that sorts by the class size and then the name.
     */
    static final Comparator<ClassInfo> BY_CLASS_SIZE = new ClassInfoComparator() {
        @Override
        public int metric(ClassStats stats) {
            return stats.classSize();
        }
    };

    /**
     * A comparator that sorts by the class size and then the name.
     */
    static final Comparator<MethodStats> BY_METHOD_SIZE = new Comparator<MethodStats>() {
        public int compare(MethodStats o1, MethodStats o2) {
            final int m1 = o1.methodSize();
            final int m2 = o2.methodSize();
            return m1 < m2 ? 1 : m1 > m2 ? -1 : o1._methodActor.toString().compareTo(o2._methodActor.toString());
        }
    };
}

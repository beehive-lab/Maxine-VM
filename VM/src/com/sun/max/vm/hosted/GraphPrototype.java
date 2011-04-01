/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.hosted;

import java.io.*;
import java.lang.ref.*;
import java.lang.reflect.*;
import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.hosted.GraphStats.ClassStats;
import com.sun.max.vm.hosted.JDKInterceptor.InterceptedField;
import com.sun.max.vm.jdk.*;
import com.sun.max.vm.type.*;

/**
 * A graph prototype represents the pre-initialized prototype of the virtual
 * machine as a graph of objects and contains extra information about each link
 * that allow diagnosis and analysis of space consumption.
 *
 */
public class GraphPrototype extends Prototype {

    private final boolean debuggingPaths;
    public final CompiledPrototype compiledPrototype;
    private final Map<Object, Link> objectToParent = new IdentityHashMap<Object, Link>();
    private LinkedList<Object> worklist = new LinkedList<Object>();
    private List<Object> fixedObjects;

    final IdentitySet<Object> objects = new IdentitySet<Object>(Ints.M);
    final Map<Class, ClassInfo> classInfos = new IdentityHashMap<Class, ClassInfo>();

    /**
     * Create a new graph prototype from the specified compiled prototype and compute the transitive closure
     * of all references.
     *
     * @param compiledPrototype the compiled prototype from which to begin creating the graph prototype
     */
    public GraphPrototype(CompiledPrototype compiledPrototype) {
        this.compiledPrototype = compiledPrototype;
        debuggingPaths = true;
        add(null, ClassRegistry.BOOT_CLASS_REGISTRY, "[root]");
        gatherObjects();
    }

    /**
     * The information gathered for a reference field of a class during exploration of the graph.
     */
    abstract static class ReferenceFieldInfo {
        /**
         * Determines if this field is mutable.
         *
         * @return whether the value of this field can be modified in the target VM
         */
        boolean isMutable() {
            return !fieldActor().isConstant();
        }

        /**
         * Gets the name of this field.
         *
         * @return the name of this field
         */
        abstract String getName();

        /**
         * Gets the value this field should have in the boot image.
         *
         * @param object the object from which the field value can be read if this is a non-static field; otherwise this
         *            value is ignored
         * @return the value of this field in {@code object} if this is an instance field, otherwise the value of this
         *         static field
         */
        abstract Object getValue(Object object);

        /**
         * Gets the {@code FieldActor} corresponding to this field.
         */
        abstract FieldActor fieldActor();

        /**
         * Gets a string representation of this field.
         *
         * @return the value of {@link #getName()} with "*" appended if this field is {@linkplain #isMutable() mutable}
         */
        @Override
        public final String toString() {
            if (!isMutable()) {
                return getName();
            }
            return getName() + "*";
        }
    }

    /**
     * A specialization of {@link ReferenceFieldInfo} for fields whose boot image value is
     * read via {@linkplain Field#get(Object) reflection}.
     */
    static final class ReflectedReferenceFieldInfo extends ReferenceFieldInfo {
        final Field field;
        final FieldActor fieldActor;

        ReflectedReferenceFieldInfo(FieldActor fieldActor) {
            final Field field = fieldActor.toJava();
            field.setAccessible(true);
            this.field = field;
            this.fieldActor = fieldActor;
        }
        @Override
        String getName() {
            return field.getName();
        }

        @Override
        Object getValue(Object object) {
            if (fieldActor().getAnnotation(RESET.class) != null) {
                return fieldActor.kind.zeroValue();
            }
            try {
                return JavaPrototype.hostToTarget(field.get(object));
            } catch (IllegalArgumentException e) {
                throw ProgramError.unexpected(e);
            } catch (IllegalAccessException e) {
                throw ProgramError.unexpected(e);
            }
        }
        @Override
        FieldActor fieldActor() {
            return fieldActor;
        }
    }

    /**
     * A specialization of {@link ReferenceFieldInfo} for fields of the JDK that are {@linkplain InterceptedField intercepted} for
     * the purpose of modifying their value in the boot image.
     */
    static final class InterceptedReferenceFieldInfo extends ReferenceFieldInfo {
        final InterceptedField interceptedField;

        InterceptedReferenceFieldInfo(InterceptedField interceptedField) {
            this.interceptedField = interceptedField;
        }
        @Override
        String getName() {
            return interceptedField.getName();
        }

        @Override
        Object getValue(Object object) {
            return interceptedField.getValue(object, interceptedField.fieldActor).unboxObject();
        }
        @Override
        FieldActor fieldActor() {
            return interceptedField.fieldActor;
        }
        @Override
        boolean isMutable() {
            return interceptedField.isMutable();
        }
    }

    /**
     * The information gathered for a class during exploration of the graph, including
     * the reference fields that need to be scanned to traverse the object graph.
     *
     * This info is also used to partition the objects in the boot heap into those
     * that {@linkplain ClassInfo#containsMutableReferences(Object) contain mutable references}
     * and those that do not.
     */
    static class ClassInfo {
        final Class clazz;
        final boolean instanceIsMutable;
        final boolean staticTupleIsMutable;

        final List<ReferenceFieldInfo> instanceFields;
        final List<ReferenceFieldInfo> staticFields;

        ClassStats stats;

        /**
         * Creates a class info data structure for the specified Java class.
         *
         * @param clazz the java class
         */
        ClassInfo(Class clazz, ClassInfo superInfo) {
            this.clazz = clazz;

            final List<ReferenceFieldInfo> instanceFields = new LinkedList<ReferenceFieldInfo>();
            final List<ReferenceFieldInfo> staticFields = new LinkedList<ReferenceFieldInfo>();

            if (superInfo != null) {
                // propagate information from super class field lists to this new class info
                instanceFields.addAll(superInfo.instanceFields);
            }

            // Need to iterate over the fields using actors instead of reflection otherwise we'll
            // miss fields that are hidden to reflection (see sun.reflection.Reflection.filterFields(Class, Field[])).
            final ClassActor classActor = ClassActor.fromJava(clazz);

            this.instanceIsMutable = addClassInfoFields(instanceFields, classActor.localInstanceFieldActors()) ||
                                 (superInfo != null && superInfo.instanceIsMutable) ||
                                 isReferenceArray() ||
                                 Reference.class.isAssignableFrom(clazz);
            this.staticTupleIsMutable = addClassInfoFields(staticFields, classActor.localStaticFieldActors());

            this.instanceFields = instanceFields;
            this.staticFields = staticFields;

            if (Trace.hasLevel(4)) {
                printTo(Trace.stream());
            }
        }

        boolean isReferenceArray() {
            final Class componentType = clazz.getComponentType();
            return componentType != null && !componentType.isPrimitive() && !Word.class.isAssignableFrom(componentType);
        }

        private static boolean addClassInfoFields(List<ReferenceFieldInfo> fieldInfos, FieldActor[] fieldActors) {
            boolean foundMutableField = false;
            for (FieldActor fieldActor : fieldActors) {
                if (fieldActor.kind.isReference) {
                    final InterceptedField interceptedField = JDKInterceptor.getInterceptedField(fieldActor);
                    ReferenceFieldInfo fieldInfo = null;
                    if (interceptedField != null) {
                        interceptedField.fieldActor = fieldActor;
                        fieldInfo = new InterceptedReferenceFieldInfo(interceptedField);
                        fieldInfos.add(fieldInfo);
                    } else {
                        if (!fieldActor.isInjected()) {
                            try {
                                fieldInfo = new ReflectedReferenceFieldInfo(fieldActor);
                                fieldInfos.add(fieldInfo);
                            } catch (NoSuchFieldError noSuchFieldError) {
                                ProgramWarning.message("Ignoring field hidden by JDK to reflection: " + fieldActor.format("%H.%n"));
                            }
                        }
                    }
                    if (!foundMutableField && fieldInfo != null) {
                        foundMutableField = fieldInfo.isMutable();
                    }
                }
            }
            return foundMutableField;
        }

        /**
         * Checks if the specified object is equal to this one. This implementation
         * considers the other object equal to this one if the underlying Java classes
         * are equal.
         *
         * @param obj the other object
         */
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ClassInfo) {
                return ((ClassInfo) obj).clazz.equals(clazz);
            }
            return false;
        }

        /**
         * Computes a hashcode for this object.
         *
         * @return the hashcode of the underlying Java class
         */
        @Override
        public int hashCode() {
            return clazz.hashCode();
        }

        /**
         * Convert this object to a string.
         *
         * @return the result of calling {@code .toString()} on the underlying Java class
         */
        @Override
        public String toString() {
            return clazz.toString();
        }

        /**
         * Gets the instance or static fields of this class.
         *
         * @param object determines if the static or instance fields are being requested
         * @return the static field of this class if {@code object} is a {@link StaticTuple} instance; otherwise the
         *         instance fields
         */
        public List<ReferenceFieldInfo> fieldInfos(Object object) {
            if (object instanceof StaticTuple) {
                return staticFields;
            }
            return instanceFields;
        }

        /**
         * Determines if a given object may contain mutable references.
         *
         * @param object an object being queried for mutability
         * @return {@code true} if {@code object} may contain mutable references; {@code false} otherwise
         */
        public boolean containsMutableReferences(Object object) {
            if (object instanceof StaticTuple) {
                return staticTupleIsMutable;
            }
            return instanceIsMutable;
        }

        /**
         * Prints the details of this class to a given stream.
         *
         * @param stream
         */
        public void printTo(PrintStream stream) {
            stream.println(clazz.getName() + ": mutable-instance=" + instanceIsMutable + ", mutable-static-tuple=" + staticTupleIsMutable);
            if (!instanceFields.isEmpty()) {
                stream.println("  instance fields:");
                for (ReferenceFieldInfo fieldInfo : instanceFields) {
                    stream.println("    " + fieldInfo);
                }
            }
            if (!staticFields.isEmpty()) {
                stream.println("  static fields:");
                for (ReferenceFieldInfo fieldInfo : staticFields) {
                    stream.println("    " + fieldInfo);
                }
            }
        }

    }

    /**
     * Returns an iterable collection of all the objects in the graph prototype. Once this method has been called, no
     * further objects can be added to the graph prototype.
     *
     * @return a collection of all objects in this graph
     */
    public synchronized List<Object> objects() {
        if (this.fixedObjects == null) {
            final List<Object> fixedObjects = new ArrayList<Object>(objects.numberOfElements());
            for (Object object : objects) {
                fixedObjects.add(object);
            }
            this.fixedObjects = fixedObjects;
        } else {
            ProgramError.check(this.fixedObjects.size() == objects.numberOfElements());
        }
        return this.fixedObjects;
    }

    public ClassInfo classInfoFor(Object object) {
        if (object instanceof StaticTuple) {
            return classInfos.get(((StaticTuple) object).classActor().toJava());
        }
        return classInfos.get(object.getClass());
    }

    /**
     * A link between objects in the graph, e.g. a field that contains a reference from
     * one object to another, an array element, etc.
     */
    static class Link {
        final Object parent;
        final Object name;

        /**
         * Creates a new link from the specified parent object the the child object.
         *
         * @param parent the parent object (i.e. the one containing the reference)
         * @param name the name of the link
         */
        Link(Object parent, Object name) {
            this.parent = parent;
            this.name = name;
        }

        /**
         * Converts this link to a string.
         *
         * @return the name of this link as a string
         */
        public String name() {
            if (name instanceof String) {
                return name.toString();
            }
            return "[" + name + "]";
        }

        /**
         * Converts this link to a string that is suitable for use as a sufix.
         *
         * @return the name of this link
         */
        public String nameAsSuffix() {
            if (name instanceof String) {
                return "." + name.toString();
            }
            return "[" + name + "]";
        }
    }

    /**
     * Get the set of all links.
     *
     * @return the set of all links
     */
    public Set<Map.Entry<Object, Link>> links() {
        return objectToParent.entrySet();
    }

    /**
     * Print the path from the root to the specified object.
     *
     * @param object the object for which to print the path
     * @param out the stream to which to print the output
     */
    public void printPath(Object object, PrintStream out) {
        out.println("BEGIN path");
        out.println(object.getClass().getName() + "    [" + Strings.truncate(object.toString(), 60) + "]");
        Link link = objectToParent.get(object);
        while (link != null) {
            final Object parent = link.parent;
            out.println(parent.getClass().getName() + link.nameAsSuffix() + "    [" + Strings.truncate(parent.toString(), 60) + "]");
            link = objectToParent.get(parent);
        }
        out.println("END path");
    }

    /**
     * Print the path from the root to the specified object to the standard output stream.
     *
     * @param object
     */
    public void printPath(Object object) {
        printPath(object, System.out);
    }

    /**
     * Add a link between the parent object and the child object with the specified link name.
     *
     * @param parent the parent object
     * @param child the child object
     * @param fieldNameOrArrayIndex the name of the field or the array index which characterizes the
     * link between the parent object and the child object
     */
    private void add(Object parent, Object child, Object fieldNameOrArrayIndex) {
        final Object object = JavaPrototype.hostToTarget(child);
        if (object != null && !objects.contains(object)) {
            assert fixedObjects == null : "Cannot add more objects to graph prototype once the fixed set objects has been created";
            objects.add(object);
            worklist.add(object);

            if (debuggingPaths) {
                objectToParent.put(object, parent == null ? null : new Link(parent, fieldNameOrArrayIndex));
            }

            if (object instanceof Proxy || object instanceof OmittedClassError) {
                printPath(object, System.err);
                ProgramError.unexpected("There should not be any instances of " + Proxy.class + " in the boot image");
            }
        }
    }

    /**
     * Gather all objects by transitive closure on the object references.
     */
    private void gatherObjects() {
        Trace.begin(1, "gatherObjects");
        int n = 0;
        while (!worklist.isEmpty()) {
            final Object object = worklist.removeFirst();
            try {
                explore(object);
            } catch (Throwable e) {
                printPath(object, System.err);
                ProgramError.unexpected("Problem while gathering instance of " + object.getClass(), e);
            }

            if (++n % 100000 == 0) {
                Trace.line(1, "gatherObjects: " + n);
            }
        }
        Trace.end(1, "gatherObjects: " + n + " objects");
    }

    /**
     * Create the class info for a specified java class, if it doesn't already exist. This requires
     * first creating the class info for the super class, and then building a list of all fields
     * in the class that are used for walking the graph.
     * @param javaClass the java class for which to get or create the class info
     * @return the class info for the class
     */
    private synchronized ClassInfo makeClassInfo(Class javaClass) {
        ClassInfo classInfo = classInfos.get(javaClass);
        if (classInfo == null) {
            final Class superClass = javaClass.getSuperclass();
            final ClassInfo superInfo = superClass == null ? null : makeClassInfo(superClass);
            classInfo = new ClassInfo(javaClass, superInfo);
            classInfos.put(javaClass, classInfo);
        }
        return classInfo;
    }

    /**
     * Explore a live object in the graph. Walk the object's class and read any reference fields to transitively
     * explore the object graph.
     * @param object the object to explore
     */
    private void explore(Object object) {
        if (object instanceof StaticTuple) {
            // Static fields are explored in exporeClass()
            return;
        }

        // get the class info for the object's class
        final Class<?> objectClass = object.getClass();
        final ClassInfo classInfo = makeClassInfo(objectClass);

        // add the object's class to the graph
        add(object, objectClass, "class");

        if (object instanceof Class) {
            // must ensure that any Class instances in the image are referenced by the mirror field of the ClassActor
            exploreClass((Class) object);
        } else if (object instanceof ClassActor) {
            // must ensure that any ClassActor instances reference the java.lang.Class instance
            exploreClassActor((ClassActor) object);
        } else if (object instanceof JDK.ClassRef) {
            // resolve class ref's at bootstrapping time
            exploreClassRef((JDK.ClassRef) object);
        }

        // walk the reference fields of the object
        walkFields(object, classInfo.instanceFields);

        // if this is a reference array, walk its elements
        if (object instanceof Object[] && !(object instanceof Word[])) {
            final Object[] array = (Object[]) object;
            for (int i = 0; i < array.length; i++) {
                add(array, JavaPrototype.hostToTarget(array[i]), i);
            }
        }
    }

    private void exploreClassActor(ClassActor classActor) {
        final Class<?> javaClass = classActor.toJava();
        classActor.setJavaClass(javaClass);
        // add the class actor object
        add(classActor, javaClass, "mirror");
    }

    private void exploreClass(Class javaClass) throws ProgramError {
        final ClassActor classActor = ClassActor.fromJava(javaClass);
        if (classActor == null) {
            if (MaxineVM.isHostedOnly(javaClass)) {
                throw ProgramError.unexpected("Instance of prototype only class " + javaClass + " should not be reachable.");
            }
            throw ProgramError.unexpected("Could not get class actor for " + javaClass);
        }
        classActor.setJavaClass(javaClass);
        // add the class actor object
        add(javaClass, classActor, "classActor");
        // walk the static fields of the class
        walkFields(javaClass, makeClassInfo(javaClass).staticFields);
    }

    private void exploreClassRef(JDK.ClassRef classRef) {
        classRef.resolveClassActor();
    }

    private void walkFields(Object object, List<ReferenceFieldInfo> fieldInfos) throws ProgramError {
        for (ReferenceFieldInfo fieldInfo : fieldInfos) {
            try {
                final Object value = JavaPrototype.hostToTarget(fieldInfo.getValue(object));
                add(object, value, fieldInfo.getName());
            } catch (IllegalArgumentException e) {
                throw ProgramError.unexpected(e);
            }
        }
    }
}

/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
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
import java.lang.reflect.*;
import java.util.*;
import java.util.Arrays;

import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.object.host.*;
import com.sun.max.vm.prototype.HackJDK.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * A graph prototype represents the pre-initialized prototype of the virtual
 * machine as a graph of objects and contains extra information about each link
 * that allow diagnosis and analysis of space consumption.
 *
 */
public class GraphPrototype extends Prototype {

    private final boolean _debuggingPaths;
    public final CompiledPrototype _compiledPrototype;
    private final Map<Object, Link> _objectToParent = new IdentityHashMap<Object, Link>();
    private LinkedList<Object> _worklist = new LinkedList<Object>();
    private IdentitySet<Object> _objects = new IdentitySet<Object>(Object.class, Ints.M);

    private Map<Class, ClassInfo> _classInfos = new IdentityHashMap<Class, ClassInfo>();

    /**
     * Create a new graph prototype from the specified compiled prototype and compute the transitive closure
     * of all references.
     *
     * @param compiledPrototype the compiled prototype from which to begin creating the graph prototype
     * @param tree a boolean indicating whether to generate a tree file that contains connectivity information
     * about the graph that is useful for debugging
     */
    public GraphPrototype(CompiledPrototype compiledPrototype, boolean tree) {
        super(compiledPrototype.vmConfiguration());
        _compiledPrototype = compiledPrototype;
        _debuggingPaths = true;
        add(null, ClassRegistry.vmClassRegistry(), "[root]");
        gatherObjects();
    }


    /**
     * The information gathered for a class during exploration of the graph, including
     * the reference fields that need to be scanned to traverse the object graph.
     */
    static class ClassInfo {
        final Class _class;
        final AppendableSequence<Field> _mutableReferenceFields = new LinkSequence<Field>();
        final AppendableSequence<Field> _immutableReferenceFields = new LinkSequence<Field>();
        final AppendableSequence<SpecialField> _specialReferenceFields = new LinkSequence<SpecialField>();
        final AppendableSequence<ClassInfo> _subClasses = new ArrayListSequence<ClassInfo>();

        ClassInfo _classClassInfo; // the class info object for the static members of the class

        long _numberOfObjects;
        long _totalSize;

        /**
         * Creates a class info data structure for the specified Java class.
         *
         * @param clazz the java class
         */
        ClassInfo(Class clazz) {
            _class = clazz;
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
                return ((ClassInfo) obj)._class.equals(_class);
            }
            return false;
        }

        /**
         * Calculates and returns the aggregate number of objects of this class and all its
         * subclasses.
         *
         * @return the number of instances of this class and all its subclasses
         */
        long aggregateNumberOfObjects() {
            long result = _numberOfObjects;
            for (ClassInfo subClassInfo : _subClasses) {
                result += subClassInfo.aggregateNumberOfObjects();
            }
            return result;
        }

        /**
         * Calculates and returns the aggregate size of all objects of this class and all of its
         * subclasses.
         *
         * @return the size of all instances of this class and its subclasses
         */
        long aggregateTotalSize() {
            long result = _totalSize;
            for (ClassInfo subClassInfo : _subClasses) {
                result += subClassInfo.aggregateTotalSize();
            }
            return result;
        }

        /**
         * Computes a hashcode for this object.
         *
         * @return the hashcode of the underlying Java class
         */
        @Override
        public int hashCode() {
            return _class.hashCode();
        }

        /**
         * Convert this object to a string.
         *
         * @return the result of calling {@code .toString()} on the underlying Java class
         */
        @Override
        public String toString() {
            return _class.toString();
        }

        /**
         * Returns average instance size (i.e. total size divided by number of objects).
         *
         * @return the average instance size of instances of this classes
         */
        long averageInstanceSize() {
            return _totalSize / _numberOfObjects;
        }

        /**
         * A comparator that sorts by the total size and then the name.
         */
        static final Comparator<ClassInfo> BY_TOTAL_SIZE_AND_NAME = new Comparator<ClassInfo>() {
            public int compare(ClassInfo o1, ClassInfo o2) {
                return o1._totalSize < o2._totalSize ? -1 : o1._totalSize > o2._totalSize ? 1 : o1.toString().compareTo(o2.toString());
            }
        };

        /**
         * A comparator that sorts by the aggregrate total size.
         */
        static final Comparator<ClassInfo> BY_AGGREGATE_TOTAL_SIZE = new Comparator<ClassInfo>() {
            public int compare(ClassInfo o1, ClassInfo o2) {
                final long o2TotalSize = o2.aggregateTotalSize();
                final long o1TotalSize = o1.aggregateTotalSize();
                return o1TotalSize < o2TotalSize ? -1 : o1TotalSize == o2TotalSize ? 0 : 1;
            }
        };

        /**
         * A comparator that sorts by the number of objects.
         */
        static final Comparator<ClassInfo> BY_NUMBER_OF_OBJECTS = new Comparator<ClassInfo>() {
            public int compare(ClassInfo o1, ClassInfo o2) {
                return o1._numberOfObjects < o2._numberOfObjects ? -1 : o1._numberOfObjects == o2._numberOfObjects ? 0 : 1;
            }
        };

        /**
         * A comparator that sorts by the aggregate number of objects.
         */
        static final Comparator<ClassInfo> BY_AGGREGATE_NUMBER_OF_OBJECTS = new Comparator<ClassInfo>() {
            public int compare(ClassInfo o1, ClassInfo o2) {
                final long o1NumberOfObjects = o1.aggregateNumberOfObjects();
                final long o2NumberOfObjects = o2.aggregateNumberOfObjects();
                return o1NumberOfObjects < o2NumberOfObjects ? -1 : o1NumberOfObjects == o2NumberOfObjects ? 0 : 1;
            }
        };

        /**
         * A comparator that sorts by the average instance size.
         */
        static final Comparator<ClassInfo> BY_AVERAGE_INSTANCE_SIZE = new Comparator<ClassInfo>() {
            public int compare(ClassInfo o1, ClassInfo o2) {
                final long o1Size = o1.averageInstanceSize();
                final long o2Size = o2.averageInstanceSize();
                return o1Size < o2Size ? -1 : o1Size == o2Size ? 0 : 1;
            }
        };

        /**
         * A comparator that sorts by name.
         */
        static final Comparator<ClassInfo> BY_NAME = new Comparator<ClassInfo>() {
            public int compare(ClassInfo o1, ClassInfo o2) {
                return o1._class.getName().compareTo(o2._class.getName());
            }
        };
    }


    /**
     * Returns an iterable collection of all the objects in the graph prototype.
     * @return a collection of all objects in this graph
     */
    public Iterable<Object> objects() {
        return _objects;
    }

    /**
     * A link between objects in the graph, e.g. a field that contains a reference from
     * one object to another, an array element, etc.
     */
    static class Link {
        final Object _parent;
        final Object _name;

        /**
         * Creates a new link from the specified parent object the the child object.
         *
         * @param parent the parent object (i.e. the one containing the reference)
         * @param name the name of the link
         */
        Link(Object parent, Object name) {
            _parent = parent;
            _name = name;
        }

        /**
         * Converts this link to a string.
         *
         * @return the name of this link as a string
         */
        public String name() {
            if (_name instanceof String) {
                return _name.toString();
            }
            return "[" + _name + "]";
        }

        /**
         * Converts this link to a string that is suitable for use as a sufix.
         *
         * @return the name of this link
         */
        public String nameAsSuffix() {
            if (_name instanceof String) {
                return "." + _name.toString();
            }
            return "[" + _name + "]";
        }
    }

    /**
     * Get the set of all links.
     *
     * @return the set of all links
     */
    public Set<Map.Entry<Object, Link>> links() {
        return _objectToParent.entrySet();
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
        Link link = _objectToParent.get(object);
        while (link != null) {
            final Object parent = link._parent;
            out.println(parent.getClass().getName() + link.nameAsSuffix() + "    [" + Strings.truncate(parent.toString(), 60) + "]");
            link = _objectToParent.get(parent);
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
        final Object object = HostObjectAccess.hostToTarget(child);
        if (object != null && !_objects.contains(object)) {
            _objects.add(object);
            _worklist.add(object);

            if (_debuggingPaths) {
                _objectToParent.put(object, parent == null ? null : new Link(parent, fieldNameOrArrayIndex));
            }

            if (object instanceof Proxy) {
                printPath(object, System.err);
                ProgramError.unexpected("There should not be any instances of " + Proxy.class + " in the boot image");
            }
        }
    }

    /**
     * Checks whether the specified field is static.
     *
     * @param field the field to check
     * @return {@code true} if the specified field is static; {@code false} otherwise
     */
    private boolean isStatic(Field field) {
        return (field.getModifiers() & Modifier.STATIC) != 0;
    }

    /**
     * Checks whether the specified field is of reference type.
     *
     * @param field the field to check
     * @return {@code true} if the field is a reference type; {@code false} otherwise
     */
    private boolean isReferenceField(Field field) {
        final Class type = field.getType();
        return !(type.isPrimitive() || Word.class.isAssignableFrom(type));
    }

    /**
     * Gather all objects by transitive closure on the object references.
     */
    private void gatherObjects() {
        Trace.begin(1, "gatherObjects");
        int n = 0;
        while (!_worklist.isEmpty()) {
            final Object object = _worklist.removeFirst();
            try {
                explore(object);
            } catch (ProgramError e) {
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
     * Dumps the histogram without formatting the quantities which allows the rows of the histogram to be
     * sorted by fields other than total size by tools such as the Unix 'sort' command.
     */
    public void dumpHistogram(PrintStream printStream) {
        final ClassInfo[] classInfos = _classInfos.values().toArray(new ClassInfo[0]);
        Arrays.sort(classInfos, ClassInfo.BY_TOTAL_SIZE_AND_NAME);
        printStream.println("Flat Histogram Start");
        for (ClassInfo info : classInfos) {
            if (info._numberOfObjects != 0) {
                printStream.printf("%-10d / %-10d = %-10d %s\n", info._totalSize, info._numberOfObjects, info._totalSize / info._numberOfObjects, info._class.getName());
            }
        }
        printStream.println("Flat Histogram End");

        Arrays.sort(classInfos, ClassInfo.BY_AGGREGATE_TOTAL_SIZE);
        printStream.println("Aggregate Histogram Start");
        for (ClassInfo info : classInfos) {
            final long aggregateNumberOfObjects = info.aggregateNumberOfObjects();
            final long aggregateTotalSize = info.aggregateTotalSize();
            if (aggregateNumberOfObjects > 0) {
                printStream.printf("%-10d / %-10d = %-10d %s\n", aggregateTotalSize, aggregateNumberOfObjects, aggregateTotalSize / aggregateNumberOfObjects, info._class.getName());
            }
        }
        printStream.println("Aggregate Histogram End");

    }

    /**
     * Create the class info for a specified java class, if it doesn't already exist. This requires
     * first creating the class info for the super class, and then building a list of all fields
     * in the class that are used for walking the graph.
     * @param javaClass the java class for which to get or create the class info
     * @return the class info for the class
     */
    private ClassInfo makeClassInfo(Class javaClass) {
        ClassInfo classInfo = _classInfos.get(javaClass);
        if (classInfo == null) {
            final Class superClass = javaClass.getSuperclass();
            final ClassInfo superInfo = superClass == null ? null : makeClassInfo(superClass);
            classInfo = new ClassInfo(javaClass);
            final ClassInfo staticInfo = new ClassInfo(javaClass);
            classInfo._classClassInfo = staticInfo;
            _classInfos.put(javaClass, classInfo);

            if (superInfo != null) {
                // propagate information from super class field lists to this new class info
                superInfo._subClasses.append(classInfo);
                AppendableSequence.Static.appendAll(classInfo._immutableReferenceFields, superInfo._immutableReferenceFields);
                AppendableSequence.Static.appendAll(classInfo._mutableReferenceFields, superInfo._mutableReferenceFields);
                AppendableSequence.Static.appendAll(classInfo._specialReferenceFields, superInfo._specialReferenceFields);
            }

            for (Field field : javaClass.getDeclaredFields()) {
                if (isReferenceField(field) && !MaxineVM.isPrototypeOnly(field)) {
                    field.setAccessible(true);
                    final ClassInfo info = isStatic(field) ? staticInfo : classInfo;
                    final SpecialField specialField = HackJDK.getSpecialField(field);
                    if (specialField != null) {
                        specialField._field = field;
                        if (!(specialField instanceof ZeroField)) {
                            info._specialReferenceFields.append(specialField);
                        }
                    } else {
                        // TODO: mutable vs. immutable fields
                        info._mutableReferenceFields.append(field);
                    }
                }
            }
        }
        return classInfo;
    }

    /**
     * Explore a live object in the graph. Walk the object's class and read any reference fields to transitively
     * explore the object graph.
     * @param object the object to explore
     */
    private void explore(Object object) {
        // get the class info for the object's class
        final Class<?> objectClass = object.getClass();
        final ClassInfo classInfo = makeClassInfo(objectClass);

        // add the object's class to the graph
        add(object, objectClass, "class");

        if (object instanceof Class) {
            // must ensure that any Class instances in the image are referenced by the mirror field of the ClassActor
            exploreClass(object);
        } else if (object instanceof ClassActor) {
            // must ensure that any ClassActor instances reference the java.lang.Class instance
            exploreClassActor(object);
        }

        // walk the reference fields of the object
        walkFields(object, classInfo);

        // if this is a reference array, walk its elements
        if (object instanceof Object[] && !(object instanceof Word[])) {
            final Object[] array = (Object[]) object;
            for (int i = 0; i < array.length; i++) {
                add(array, HostObjectAccess.hostToTarget(array[i]), i);
            }
        }
    }

    private void exploreClassActor(Object object) {
        final ClassActor classActor = (ClassActor) object;
        final Class<?> javaClass = classActor.toJava();
        classActor.setMirror(javaClass);
        // add the class actor object
        add(object, javaClass, "mirror");
    }

    private void exploreClass(Object object) throws ProgramError {
        final Class<?> javaClass = (Class<?>) object;
        final ClassActor classActor = ClassActor.fromJava(javaClass);
        if (classActor == null) {
            if (MaxineVM.isPrototypeOnly(javaClass)) {
                throw ProgramError.unexpected("Instance of prototype only class " + javaClass + " should not be reachable.");
            }
            throw ProgramError.unexpected("Could not get class actor for " + javaClass);
        }
        classActor.setMirror(javaClass);
        // add the class actor object
        add(object, classActor, "classActor");
        // walk the static fields of the class
        walkFields(object, makeClassInfo(javaClass)._classClassInfo);
    }

    private void walkFields(Object object, ClassInfo classInfo) throws ProgramError {
        // TODO: mutable vs. immutable fields
        for (Field field : classInfo._mutableReferenceFields) {
            try {
                final Object value = HostObjectAccess.hostToTarget(field.get(object));
                add(object, value, field.getName());
            } catch (IllegalArgumentException e) {
                throw ProgramError.unexpected(e);
            } catch (IllegalAccessException e) {
                throw ProgramError.unexpected(e);
            }
        }

        for (SpecialField specialField : classInfo._specialReferenceFields) {
            final Value value = specialField.getValue(object, FieldActor.fromJava(specialField._field));
            final Object result = value.unboxObject();
            add(object, result, specialField.getName());
        }
    }
}

/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

import static com.sun.max.lang.Classes.*;
import static com.sun.max.vm.MaxineVM.*;
import static com.sun.max.vm.VMConfiguration.*;
import static com.sun.max.vm.compiler.CallEntryPoint.*;
import static com.sun.max.vm.type.ClassRegistry.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.Phase;
import com.sun.max.vm.actor.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.deopt.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.hosted.CompiledPrototype.Link.Relationship;
import com.sun.max.vm.jdk.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.run.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;

/**
 * The bootstrapping phase responsible for compiling and linking methods in the target.
 */
public class CompiledPrototype extends Prototype {

    class ClassInfo {
        public ClassInfo(ClassActor classActor) {
            this.classActor = classActor;
        }
        final ClassActor classActor;
        final HashSet<MethodActor> indirectCalls = new HashSet<MethodActor>();
        final HashSet<ClassActor> subClasses = new HashSet<ClassActor>();
        final HashSet<ClassActor> implementors = new HashSet<ClassActor>();
        @Override
        public String toString() {
            return classActor.toString();
        }
    }

    private final HashMap<ClassActor, ClassInfo> classActorInfo = new HashMap<ClassActor, ClassInfo>();
    private final HashMap<MethodActor, Set<ClassActor>> anonymousClasses = new HashMap<MethodActor, Set<ClassActor>>();

    private final HashMap<MethodActor, Link> methodActors = new HashMap<MethodActor, Link>();
    private final HashSet<MethodActor> methodActorsWithInlining = new HashSet<MethodActor>();
    private final HashMap<String, Link> stubs = new HashMap<String, Link>();
    private final LinkedList<MethodActor> worklist = new LinkedList<MethodActor>();

    /**
     * The link from a <i>parent</i> method to a <i>child</i> method where the parent caused the child to be
     * compiled in the image.
     */
    public static class Link {

        enum Relationship {
            DIRECT_CALL("directly calls", "is directly called by"),
            VIRTUAL_CALL("virtually calls", "is virtually called by"),
            INTERFACE_CALL("interfacially calls", "is interfacially called by"),
            LITERAL("has a literal reference to", "is referenced as a literal by"),
            IMPLEMENTS("is implemented by", "implements"),
            OVERRIDES("is overridden by", "overrides");

            final String asParent;
            final String asChild;

            private Relationship(String asParent, String asChild) {
                this.asParent = asParent;
                this.asChild = asChild;
            }
        }
        public final Object child;
        public final Object parent;
        public final Relationship relationship;

        public Link(Object child, Object parent, Relationship relationship) {
            assert child != null;
            assert (parent == null) == (relationship == null);
            assert child != parent;
            assert parent == null || !id(child).equals(id(parent)) : child;
            this.child = child;
            this.parent = parent;
            this.relationship = relationship;
        }

        static String id(Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof TargetMethod) {
                TargetMethod targetMethod = (TargetMethod) object;
                if (targetMethod.classMethodActor != null) {
                    return targetMethod.classMethodActor.format("%H.%n(%P):%R");
                }
                return targetMethod.toString();
            }
            if (object instanceof MethodActor) {
                MethodActor methodActor = (MethodActor) object;
                return methodActor.format("%H.%n(%P):%R");
            }
            return String.valueOf(object);
        }

        public String parentId() {
            return id(parent);
        }

        public String childId() {
            return id(child);
        }

        @Override
        public String toString() {
            if (parent == null) {
                return childId() + " is a VM entry point";
            }
            return parentId() + " " + relationship.asParent + " " + childId();
        }
    }

    public Collection<Link> links() {
        ArrayList<Link> links = new ArrayList<Link>(methodActors.size() + stubs.size());
        links.addAll(methodActors.values());
        links.addAll(stubs.values());
        return links;
    }

    private ClassInfo lookupInfo(ClassActor classActor) {
        return classActorInfo.get(classActor);
    }

    private ClassInfo getInfo(ClassActor classActor) {
        ClassInfo info = classActorInfo.get(classActor);
        if (info == null) {
            info = processNewClass(classActor);
        }
        return info;
    }

    private Set<ClassActor> getAnonymousClasses(MethodActor actor) {
        Set<ClassActor> anonymousClasses = this.anonymousClasses.get(actor);
        if (anonymousClasses == null) {
            anonymousClasses = new HashSet<ClassActor>();
            this.anonymousClasses.put(actor, anonymousClasses);
        }
        return anonymousClasses;
    }

    private Set<ClassActor> lookupAnonymousClasses(MethodActor actor) {
        return anonymousClasses.get(actor);
    }

    private void gatherNewClasses() {
        Trace.begin(1, "gatherNewClasses");
        final LinkedList<ClassActor> newClasses = new LinkedList<ClassActor>();
        for (ClassActor classActor : ClassRegistry.BOOT_CLASS_REGISTRY.bootImageClasses()) {
            if (lookupInfo(classActor) == null) {
                final Method enclosingMethod = classActor.toJava().getEnclosingMethod();
                if (enclosingMethod != null) {
                    // if this is an anonymous class, add it to the anonymous classes set of the enclosing method
                    gatherNewAnonymousClass(newClasses, classActor, enclosingMethod);
                } else {
                    traceNewClass(classActor);
                    newClasses.add(classActor);
                }
            }
        }
        Trace.end(1, "gatherNewClasses");
        Trace.begin(1, "processNewClasses " + newClasses.size());
        for (ClassActor classActor : newClasses) {
            getInfo(classActor);
        }
        Trace.end(1, "processNewClasses");
    }

    private void traceNewClass(ClassActor classActor) {
        if (Trace.hasLevel(2)) {
            Trace.line(2, "new class: " + classActor);
        }
    }

    private void gatherNewAnonymousClass(final LinkedList<ClassActor> newClasses, ClassActor classActor, final Method enclosingMethod) {
        if (!MaxineVM.isHostedOnly(enclosingMethod)) {
            final MethodActor methodActor = MethodActor.fromJava(enclosingMethod);
            if (methodActor != null) {
                getAnonymousClasses(methodActor).add(classActor);
                if (methodActorsWithInlining.contains(methodActor)) {
                    traceNewClass(classActor);
                    newClasses.add(classActor);
                }
            }
        }
    }

    private ClassInfo processNewClass(ClassActor classActor) {
        assert lookupInfo(classActor) == null;
        ClassInfo info = new ClassInfo(classActor);
        classActorInfo.put(classActor, info);

        ClassActor superClassActor = classActor.superClassActor;
        while (superClassActor != null) {
            // for each super class of this class, add this class's implementation of its methods used so far
            final ClassInfo superInfo = getInfo(superClassActor);
            superInfo.subClasses.add(classActor);
            for (MethodActor methodActor : superInfo.indirectCalls) {
                add(classActor.findVirtualMethodActor(methodActor), methodActor, Relationship.OVERRIDES);
            }
            superClassActor = superClassActor.superClassActor;
        }
        if (!classActor.isInterface()) {
            // for each interface that this class implements, add this class's implementation of its methods used so far
            for (InterfaceActor interfaceActor : classActor.getAllInterfaceActors()) {
                final ClassInfo interfaceInfo = getInfo(interfaceActor);
                interfaceInfo.implementors.add(classActor);
                for (MethodActor methodActor : interfaceInfo.indirectCalls) {
                    add(classActor.findVirtualMethodActor(methodActor), methodActor, Relationship.IMPLEMENTS);
                }
            }
        }
        return info;
    }

    private <M extends MethodActor> void addMethods(Object parent, Iterable<M> children, Relationship relationship) {
        for (M child : children) {
            add(child, parent, relationship);
        }
    }

    private void addMethods(Object parent, MethodActor[] children, Relationship relationship) {
        for (MethodActor child : children) {
            add(child, parent, relationship);
        }
    }

    private void addMethods(Set<String> imageMethods) {
        for (String classNameAndMethod : imageMethods) {
            final int ix = classNameAndMethod.lastIndexOf('.');
            if (ix < 0) {
                throw ProgramError.unexpected(classNameAndMethod + " not correct format");
            }
            final String className = classNameAndMethod.substring(0, ix);
            final String methodName = classNameAndMethod.substring(ix + 1);
            try {
                final ClassActor classActor = ClassActor.fromJava(Classes.load(HostedBootClassLoader.HOSTED_BOOT_CLASS_LOADER, className));
                forAllClassMethodActors(classActor, new Procedure<ClassMethodActor>() {

                    public void run(ClassMethodActor classMethodActor) {
                        if (classMethodActor.holder().name.string.equals(className) && (
                                        methodName.equals("*") ||
                                        methodName.equals(classMethodActor.name.string))) {
                            Trace.line(1, "forcing compilation of method " + classMethodActor.qualifiedName());
                            add(classMethodActor, null, null);
                        }
                    }
                });
            } catch (Exception ex) {
                FatalError.unexpected("failed to load: " + className + "." + methodName);
            }
        }
    }

    private void processNewTargetMethod(TargetMethod targetMethod) {
        traceNewTargetMethod(targetMethod);
        final ClassMethodActor classMethodActor = targetMethod.classMethodActor();
        // add the methods referenced in the target method's literals
        if (targetMethod.referenceLiterals() != null) {
            for (Object literal : targetMethod.referenceLiterals()) {
                if (literal instanceof MethodActor) {
                    add((MethodActor) literal, targetMethod, Relationship.LITERAL);
                }
            }
        }
        final Set<MethodActor> directCalls = new HashSet<MethodActor>();
        final Set<MethodActor> virtualCalls = new HashSet<MethodActor>();
        final Set<MethodActor> interfaceCalls = new HashSet<MethodActor>();
        final Set<MethodActor> inlinedMethods = new HashSet<MethodActor>();
        // gather all direct, virtual, and interface calls and add them
        targetMethod.gatherCalls(directCalls, virtualCalls, interfaceCalls, inlinedMethods);
        addMethods(targetMethod, directCalls, Relationship.DIRECT_CALL);
        addMethods(targetMethod, virtualCalls, Relationship.VIRTUAL_CALL);
        addMethods(targetMethod, interfaceCalls, Relationship.INTERFACE_CALL);

        // if this method (or any that it inlines) contains anonymous classes, add them:
        if (classMethodActor != null) {
            inlinedMethods.add(classMethodActor);
        }
        for (MethodActor m : inlinedMethods) {
            if (m != null) {
                methodActorsWithInlining.add(m);
                final Set<ClassActor> anonymousClasses = lookupAnonymousClasses(m);
                if (anonymousClasses != null) {
                    for (ClassActor classActor : anonymousClasses) {
                        getInfo(classActor);
                    }
                }
            }
        }
    }

    private void traceNewTargetMethod(TargetMethod targetMethod) {
        if (Trace.hasLevel(2)) {
            Trace.line(2, "new target method: " + (targetMethod.classMethodActor() == null ? targetMethod.regionName() : targetMethod.classMethodActor().format("%H.%n(%P)")));
        }
    }

    private final int numberOfCompilerThreads;

    private static CompiledPrototype instance;

    /**
     * Set accumulating all the methods invalidated during boot image generation.
     * The boot image generator must unlink these, and produce new target methods.
     */
    public HashSet<TargetMethod> invalidatedTargetMethods = new HashSet<TargetMethod>();

    CompiledPrototype(int numberCompilerThreads) {
        assert instance == null;
        instance = this;
        vmConfig().initializeSchemes(Phase.COMPILING);
        numberOfCompilerThreads = numberCompilerThreads;
        Trace.line(1, "# compiler threads:" + numberOfCompilerThreads);
    }

    private boolean isIndirectCall(Relationship relationship) {
        return relationship == Relationship.VIRTUAL_CALL || relationship == Relationship.INTERFACE_CALL;
    }

    boolean add(MethodActor child, Object parent, Relationship relationship) {
        if (child == null) {
            return false;
        }
        if (child == parent) {
            return false;
        }

        if (isIndirectCall(relationship)) {
            // if this is an indirect call that has not been seen before, add all possibly reaching implementations
            // --even if this actual method implementation may not be compiled.
            final ClassInfo info = getInfo(child.holder());
            if (!info.indirectCalls.contains(child)) {
                info.indirectCalls.add(child);
                if (relationship == Relationship.VIRTUAL_CALL) {
                    for (ClassActor subClass : info.subClasses) {
                        add(subClass.findVirtualMethodActor(child.name, child.descriptor()), child, Relationship.OVERRIDES);
                    }
                }
                if (relationship == Relationship.INTERFACE_CALL) {
                    for (ClassActor subClass : info.implementors) {
                        add(subClass.findVirtualMethodActor(child.name, child.descriptor()), child, Relationship.IMPLEMENTS);
                    }
                }
            }
        }
        if (methodActors.containsKey(child)) {
            // this method is already processed or on the queue.
            return false;
        }

        if (Actor.isDeclaredFoldable(child.flags())) {
            // All foldable methods must have their stubs precompiled in the image
            final ClassActor stubClassActor = ClassActor.fromJava(child.makeInvocationStub().getClass());
            addMethods(parent, stubClassActor.localVirtualMethodActors(), relationship);
        }
        Link existing = methodActors.put(child, new Link(child, parent, relationship));
        assert existing == null : existing;

        if (child instanceof ClassMethodActor && ((ClassMethodActor) child).currentTargetMethod() != null) {
            assert parent == null && relationship == null : "Methods with hand-crafted pre-existing machine code must be entry points";
            return false;
        }

        worklist.add(child);
        return true;
    }

    private void addMethodsReferencedByExistingTargetCode() {
        for (TargetMethod targetMethod : Code.bootCodeRegion().copyOfTargetMethods()) {
            ClassMethodActor classMethodActor = targetMethod.classMethodActor;
            if (classMethodActor != null) {
                Link existing = methodActors.put(classMethodActor, new Link(classMethodActor, null, null));
                assert existing == null : existing;
            } else {
                // stub
                Link link = new Link(targetMethod, null, null);
                Link existing = stubs.put(targetMethod.toString(), link);
                assert existing == null : existing;
            }
            processNewTargetMethod(targetMethod);
        }
    }

    /**
     * Methods that must be statically compiled in the boot image.
     */
    private static Set<MethodActor> extraVMEntryPoints = new HashSet<MethodActor>();
    private static Set<String> extraVMEntryPointNames = new HashSet<String>();
    private static Set<MethodActor> imageInvocationStubMethodActors = new HashSet<MethodActor>();
    private static Set<MethodActor> imageConstructorStubMethodActors = new HashSet<MethodActor>();

    /**
     * Registers a given method that must be statically compiled in the boot image.
     * Internal use.
     */
    public static void registerVMEntryPoint(ClassMethodActor m) {
        if (instance != null) {
            instance.add(m, null, null);
        } else {
            assert extraVMEntryPoints != null : "too late to add VM entry point " + m;
            ProgramError.check(m != null);
            extraVMEntryPoints.add(m);
        }
    }

    /**
     * Registers a given method that must be statically compiled in the boot image.
     * External use (extension support).
     * @param classAndMethodActor fully qualified name, e.g. a.b.C.m where m may be * to denote all methods
     */
    public static void registerVMEntryPoint(String classAndMethodActor) {
        if (instance != null) {
            instance.addMethods(Collections.singleton(classAndMethodActor));
        } else {
            assert extraVMEntryPointNames != null : "too late to add VM entry point " + classAndMethodActor;
            extraVMEntryPointNames.add(classAndMethodActor);
        }
    }

    /**
     * Request the given method have a statically generated and compiled invocation stub in the boot image.
     */
    public static void registerImageInvocationStub(MethodActor m) {
        if (instance != null) {
            instance.addMethods(null, ClassActor.fromJava(m.makeInvocationStub().getClass()).localVirtualMethodActors(), null);
        } else {
            assert imageInvocationStubMethodActors != null : "too late to add VM entry point " + m;
            imageInvocationStubMethodActors.add(m);
        }
    }

    /**
     * Request that the given method have a statically generated and compiled constructor stub in the boot image.
     * @param m
     */
    public static void registerImageConstructorStub(MethodActor m) {
        if (instance != null) {
            instance.addStaticAndVirtualMethods(JDK_sun_reflect_ReflectionFactory.createPrePopulatedConstructorStub(m));
        } else {
            assert imageConstructorStubMethodActors != null : "too late to add VM entry point " + m;
            imageConstructorStubMethodActors.add(m);
        }
    }

    private boolean entryPointsDone;

    private void addEntrypoints0() {
        final Relationship entryPoint = null;

        final RunScheme runScheme = vmConfig().runScheme();
        add(ClassRegistry.MaxineVM_run, null, entryPoint);
        add(ClassRegistry.VmThread_run, null, entryPoint);
        add(ClassRegistry.VmThread_attach, null, entryPoint);
        add(ClassRegistry.VmThread_detach, null, entryPoint);
        add(ClassRegistry.findMethod("run", runScheme.getClass()), null, entryPoint);

        addMethods(null, ClassActor.fromJava(JVMFunctions.class).localStaticMethodActors(), entryPoint);
        addMethods(null, extraVMEntryPoints, entryPoint);
        addMethods(extraVMEntryPointNames);
        // we would prefer not to invoke stub-generation/compilation for the shutdown hooks procedure, e.g., after an OutOfMemoryError
        registerImageInvocationStub(MethodActor.fromJava(getDeclaredMethod(JDK.java_lang_Shutdown.javaClass(), "shutdown")));

        for (MethodActor methodActor : imageInvocationStubMethodActors) {
            addMethods(null, ClassActor.fromJava(methodActor.makeInvocationStub().getClass()).localVirtualMethodActors(), entryPoint);
        }
        for (MethodActor methodActor : imageConstructorStubMethodActors) {
            addStaticAndVirtualMethods(JDK_sun_reflect_ReflectionFactory.createPrePopulatedConstructorStub(methodActor));
        }

        // pre-compile the dynamic linking methods, which reduces startup time
        add(ClassRegistry.findMethod("loadLibrary0", Runtime.class), null, entryPoint);
        add(ClassRegistry.findMethod("loadLibrary", Runtime.class), null, entryPoint);
        add(ClassRegistry.findMethod("loadLibrary", System.class), null, entryPoint);
        add(ClassRegistry.findMethod("loadLibrary0", ClassLoader.class), null, entryPoint);
        add(ClassRegistry.findMethod("loadLibrary", ClassLoader.class), null, entryPoint);

        // It's too late now to register any further methods to be compiled into the boot image
        extraVMEntryPoints = null;
        extraVMEntryPointNames = null;
        imageConstructorStubMethodActors = null;
        imageInvocationStubMethodActors = null;
    }

    private void addStaticAndVirtualMethods(ClassActor classActor) {
        addMethods(null, classActor.localVirtualMethodActors(), null);
        addMethods(null, classActor.localStaticMethodActors(), null);
    }

    private int totalCompilations;

    private boolean compileWorklist() {
        Trace.begin(1, "compile: " + worklist.size() + " new methods");
        final CodeRegion region = Code.bootCodeRegion();
        final Address oldMark = region.getAllocationMark();
        final int initialNumberOfCompilations = totalCompilations;
        final CompilationBroker cb = vm().compilationBroker;

        if (numberOfCompilerThreads == 1) {
            while (!worklist.isEmpty() || !invalidatedTargetMethods.isEmpty()) {
                processInvalidatedTargetMethods();
                final MethodActor methodActor = worklist.removeFirst();
                if (needsCompilation(methodActor)) {
                    TargetMethod targetMethod;
                    try {
                        targetMethod = cb.compile((ClassMethodActor) methodActor, null);
                    } catch (Throwable error) {
                        throw reportCompilationError(methodActor, error);
                    }
                    processNewTargetMethod(targetMethod);
                    ++totalCompilations;
                    if (totalCompilations % 200 == 0) {
                        Trace.line(1, "compiled: " + totalCompilations + " (" + methodActors.size() + " methods)");
                    }
                }
            }
        } else {
            int submittedCompilations = totalCompilations;

            final ExecutorService compilationService = Executors.newFixedThreadPool(numberOfCompilerThreads);
            final CompletionService<TargetMethod> compilationCompletionService = new ExecutorCompletionService<TargetMethod>(compilationService);

            while (true) {
                while (!worklist.isEmpty() || !invalidatedTargetMethods.isEmpty()) {
                    processInvalidatedTargetMethods();
                    final MethodActor methodActor = worklist.removeFirst();
                    if (needsCompilation(methodActor)) {
                        ++submittedCompilations;
                        compilationCompletionService.submit(new Callable<TargetMethod>() {
                            public TargetMethod call() throws Exception {
                                try {
                                    TargetMethod result = cb.compile((ClassMethodActor) methodActor, null);
                                    assert result != null;
                                    return result;
                                } catch (Throwable error) {
                                    throw reportCompilationError(methodActor, error);
                                }
                            }
                        });
                    }
                }
                if (totalCompilations >= submittedCompilations) {
                    if (!worklist.isEmpty()) {
                        continue;
                    }
                    break;
                }
                try {
                    final TargetMethod targetMethod = compilationCompletionService.take().get();
                    assert targetMethod != null;
                    processNewTargetMethod(targetMethod);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException executionException) {
                    compilationService.shutdownNow();
                    throw ProgramError.unexpected(executionException.getCause());
                }
                ++totalCompilations;
                if (totalCompilations % 200 == 0) {
                    Trace.line(1, "compiled: " + totalCompilations + " (" + methodActors.size() + " methods)");
                }
            }

            compilationService.shutdown();
        }

        final int newCompilations = totalCompilations - initialNumberOfCompilations;
        Trace.end(1, "new compilations: " + newCompilations);
        if (newCompilations == 0) {
            ProgramError.check(region.getAllocationMark().equals(oldMark));
        }
        return newCompilations > 0;
    }

    private ProgramError reportCompilationError(final MethodActor classMethodActor, Throwable error) throws ProgramError {
        System.err.println("Error occurred while compiling " + classMethodActor + ": " + error);
        printParentChain(classMethodActor);
        error.printStackTrace(System.err);
        throw ProgramError.unexpected("Error occurred while compiling " + classMethodActor, error);
    }

    private void printParentChain(final MethodActor classMethodActor) {
        System.err.println("Parent chain:");
        System.err.println("    " + classMethodActor.format("%H.%n(%p)"));
        MethodActor child = classMethodActor;
        while (child != null) {
            final Link link = methodActors.get(child);
            if (link == null) {
                System.err.println("  (no parent chain available)");
                break;
            }
            if (child == link.parent) {
                System.err.println("  which references itself recursively");
                break;
            }
            child = null;
            if (link.parent instanceof MethodActor) {
                child = (MethodActor) link.parent;
            } else if (link.parent instanceof TargetMethod) {
                TargetMethod tm = (TargetMethod) link.parent;
                if (tm.classMethodActor != null) {
                    child = tm.classMethodActor;
                }
            }
            if (child == null) {
                System.err.println("    which is a VM entry point");
            } else {
                System.err.println("    which " + link.relationship.asChild + " " + child.format("%H.%n(%p)"));
            }
        }
    }

    private void forAllClassMethodActors(ClassActor classActor, Procedure<ClassMethodActor> procedure) {
        for (VirtualMethodActor virtualMethodActor : classActor.allVirtualMethodActors()) {
            procedure.run(virtualMethodActor);
        }
        do {
            for (StaticMethodActor staticMethodActor : classActor.localStaticMethodActors()) {
                procedure.run(staticMethodActor);
            }
            classActor = classActor.superClassActor;
        } while (classActor != null);
    }



    public void compileFoldableMethods() {
        Trace.begin(1, "compiling foldable methods");
        for (ClassActor classActor : BOOT_CLASS_REGISTRY.bootImageClasses()) {
            forAllClassMethodActors(classActor, new Procedure<ClassMethodActor>() {
                public void run(ClassMethodActor classMethodActor) {
                    if (classMethodActor.isDeclaredFoldable()) {
                        add(classMethodActor, null, null);
                    }
                }
            });
        }
        compileWorklist();
        Trace.end(1, "compiling foldable methods");
    }

    public void resolveAlias() {
        Trace.begin(1, "resolving alias annotations");
        for (ClassActor classActor : BOOT_CLASS_REGISTRY.bootImageClasses()) {
            forAllClassMethodActors(classActor, new Procedure<ClassMethodActor>() {
                public void run(ClassMethodActor classMethodActor) {
                    ALIAS.Static.aliasedMethod(classMethodActor);
                }
            });

            for (FieldActor field : classActor.localInstanceFieldActors()) {
                ALIAS.Static.aliasedField(field);
            }
            for (FieldActor field : classActor.localStaticFieldActors()) {
                ALIAS.Static.aliasedField(field);
            }
        }
        Trace.end(1, "resolving alias annotations");
    }

    private void processInvalidatedTargetMethods() {
        synchronized (invalidatedTargetMethods) {
            if (!invalidatedTargetMethods.isEmpty()) {
                for (TargetMethod targetMethod : invalidatedTargetMethods) {
                    final ClassMethodActor methodActor = targetMethod.classMethodActor;
                    assert methodActors.containsKey(methodActor);
                    assert methodActor.compiledState instanceof Compilations;
                    assert ((Compilations) methodActor.compiledState).optimized == targetMethod;
                    methodActor.compiledState = Compilations.EMPTY;
                    worklist.add(methodActor);
                }
                invalidatedTargetMethods.clear();
            }
        }
    }

    /**
     * Recompile methods whose assumptions were invalidated.
     * @return true if the recompilation brought new methods in the compiled prototype.
     */
    public synchronized static void invalidateTargetMethod(TargetMethod targetMethod) {
        assert targetMethod != null;

        // Ensures that calling currentTargetMethod on targetMethod.classMethodActor returns null
        // which is need for needsCompilation() to return true for the method.
        targetMethod.invalidate(new InvalidationMarker(targetMethod));

        synchronized (instance.invalidatedTargetMethods) {
            instance.invalidatedTargetMethods.add(targetMethod);
        }
    }

    private boolean needsCompilation(MethodActor methodActor) {
        if (methodActor instanceof ClassMethodActor &&
            !methodActor.isAbstract() &&
            !methodActor.isIntrinsic()) {

            String holderName = methodActor.holder().typeDescriptor.toJavaString();
            if (matches(holderName, compilationBlacklist) && !matches(holderName, compilationWhitelist)) {
                return false;
            }

            ClassMethodActor cma = (ClassMethodActor) methodActor;
            return cma.currentTargetMethod() == null;
        }
        return false;
    }

    private static final List<String> compilationBlacklist = new ArrayList<String>();
    private static final List<String> compilationWhitelist = new ArrayList<String>();

    public static void addCompilationBlacklist(String classPrefix) {
        compilationBlacklist.add(classPrefix);
    }

    public static void addCompilationWhitelist(String classPrefix) {
        compilationWhitelist.add(classPrefix);
    }

    private static boolean matches(String match, List<String> list) {
        for (String element : list) {
            if (match.startsWith(element)) {
                return true;
            }
        }
        return false;
    }

    public void addEntrypoints() {
        // 1. create bootcode region.
        final CodeRegion region = Code.bootCodeRegion();
        region.setSize(Size.fromInt(Integer.MAX_VALUE / 4)); // enable virtually infinite allocations
        // 2. add only entrypoint methods and methods not to be compiled.
        addMethodsReferencedByExistingTargetCode();
        addEntrypoints0();
    }

    public boolean compile() {
        boolean compiledAny = false;
        boolean compiledSome = false;
        do {
            // 3. add all new class implementations
            gatherNewClasses();
            // 4. compile all new methods
            compiledSome = compileWorklist();
            compiledAny |= compiledSome;
        } while (compiledSome);

        return compiledAny;
    }

    private void linkNonVirtualCalls() {
        Trace.begin(1, "linkNonVirtualCalls");
        for (TargetMethod targetMethod : Code.bootCodeRegion().copyOfTargetMethods()) {
            if (!(targetMethod instanceof Adapter)) {
                targetMethod.linkDirectCalls();
            }
        }
        Trace.end(1, "linkNonVirtualCalls");
    }

    private void linkVTableEntries() {
        Trace.begin(1, "linkVTableEntries");
        for (ClassActor classActor : BOOT_CLASS_REGISTRY.bootImageClasses()) {
            if (classActor.isReferenceClassActor()) {
                linkVTable(classActor);
            }
        }
        Trace.end(1, "linkVTableEntries");
    }

    private void linkVTable(ClassActor classActor, Hub hub) {
        Word[] words = hub.expansion.words;
        for (int vTableIndex = Hub.vTableStartIndex(); vTableIndex < Hub.vTableStartIndex() + hub.vTableLength(); vTableIndex++) {
            final VirtualMethodActor virtualMethodActor = classActor.getVirtualMethodActorByVTableIndex(vTableIndex);
            final TargetMethod targetMethod = virtualMethodActor.currentTargetMethod();
            if (targetMethod != null) {
                words[vTableIndex] = VTABLE_ENTRY_POINT.in(targetMethod);
            }
        }
    }

    private void linkVTable(ClassActor classActor) {
        linkVTable(classActor, classActor.dynamicHub());
        linkVTable(ClassRegistry.OBJECT, classActor.staticHub());
    }

    private void linkITableEntries() {
        Trace.begin(1, "linkITableEntries");

        final IntHashMap<InterfaceActor> serialToInterfaceActor = new IntHashMap<InterfaceActor>();
        ClassActor[] classes = BOOT_CLASS_REGISTRY.bootImageClasses();
        for (ClassActor classActor : classes) {
            if (classActor instanceof InterfaceActor) {
                final InterfaceActor interfaceActor = (InterfaceActor) classActor;
                serialToInterfaceActor.put(interfaceActor.id, interfaceActor);
            }
        }

        for (ClassActor classActor : classes) {
            if (classActor.isReferenceClassActor()) {
                linkITable(classActor, serialToInterfaceActor);
            }
        }
        Trace.end(1, "linkITableEntries");
    }

    private void linkITable(ClassActor classActor, final IntHashMap<InterfaceActor> serialToInterfaceActor) {
        final DynamicHub hub = classActor.dynamicHub();
        Word[] words = hub.expansion.words;
        for (InterfaceActor interfaceActor : classActor.getAllInterfaceActors()) {
            final int interfaceIndex = hub.getITableIndex(interfaceActor.id);
            for (InterfaceMethodActor interfaceMethodActor : interfaceActor.localInterfaceMethodActors()) {
                final int iTableIndex = interfaceIndex + interfaceMethodActor.iIndexInInterface();
                final int iIndex = iTableIndex - hub.iTableStartIndex;
                final VirtualMethodActor virtualMethodActor = classActor.getVirtualMethodActorByIIndex(iIndex);
                final TargetMethod targetMethod = virtualMethodActor.currentTargetMethod();
                assert virtualMethodActor.name == interfaceMethodActor.name;
                if (targetMethod != null) {
                    words[iTableIndex] = VTABLE_ENTRY_POINT.in(targetMethod);
                }
            }
        }
    }

    public void link() {
        linkNonVirtualCalls();
        linkVTableEntries();
        linkITableEntries();
    }
}

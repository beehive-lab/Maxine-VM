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

import static com.sun.max.vm.compiler.CallEntryPoint.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.debug.*;
import com.sun.max.vm.jdk.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.prototype.CompiledPrototype.Link.*;
import com.sun.max.vm.run.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;

/**
 * The prototyping phase responsible for compiling and linking methods in the target.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 * @author Ben L. Titzer
 */
public class CompiledPrototype extends Prototype {

    private class ClassInfo {
        private final IdentityHashSet<MethodActor> _indirectCalls = new IdentityHashSet<MethodActor>();
        private final IdentityHashSet<ClassActor> _subClasses = new IdentityHashSet<ClassActor>();
        private final IdentityHashSet<ClassActor> _implementors = new IdentityHashSet<ClassActor>();
    }

    private final Map<ClassActor, ClassInfo> _classActorInfo = new IdentityHashMap<ClassActor, ClassInfo>();
    private final Map<MethodActor, GrowableDeterministicSet<ClassActor>> _anonymousClasses = new IdentityHashMap<MethodActor, GrowableDeterministicSet<ClassActor>>();

    private static final GrowableDeterministicSet<ClassActor> _jitCompiledClasses = new LinkedIdentityHashSet<ClassActor>();
    private static final GrowableDeterministicSet<MethodActor> _jitCompiledMethods = new LinkedIdentityHashSet<MethodActor>();
    private static final GrowableDeterministicSet<MethodActor> _unlinkedMethods = new LinkedIdentityHashSet<MethodActor>();
    private static final GrowableDeterministicSet<ClassActor> _unlinkedClasses = new LinkedIdentityHashSet<ClassActor>();

    private final VariableMapping<MethodActor, Link> _methodActors = new ChainedHashMapping<MethodActor, Link>();
    private final LinkedList<MethodActor> _worklist = new LinkedList<MethodActor>();

    /**
     * Methods that must be statically compiled in the boot image.
     */
    private static final GrowableDeterministicSet<ClassMethodActor> _imageMethodActors = new LinkedIdentityHashSet<ClassMethodActor>();

    /**
     * The link from a <i>referrer</i> method to a <i>referent</i> method where the referrer caused the referent to be
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

            final String _asReferrer;
            final String _asReferent;

            private Relationship(String asReferrer, String asReferent) {
                _asReferrer = asReferrer;
                _asReferent = asReferent;
            }
        }
        final MethodActor _referent;
        final MethodActor _referrer;
        final Relationship _relationship;

        public Link(MethodActor referent, MethodActor referrer, Relationship relationship) {
            assert referent != null;
            //assert referent != referrer;
            _referent = referent;
            _referrer = referrer;
            _relationship = relationship;
        }

        private static String name(MethodActor methodActor) {
            if (methodActor == null) {
                return null;
            }
            return methodActor.format("%H.%n(%p)") + ":" + methodActor.descriptor().resultDescriptor().toJavaString(false);
        }

        public String referrerName() {
            return name(_referrer);
        }

        public String referentName() {
            return name(_referent);
        }

        public MethodActor referent() {
            return _referent;
        }

        public MethodActor referrer() {
            return _referrer;
        }

        public Relationship relationship() {
            return _relationship;
        }

        @Override
        public String toString() {
            if (_referrer == null) {
                return referentName() + " is a VM entry point";
            }
            return referrerName() + " " + _relationship._asReferrer + " " + referentName();
        }
    }

    public IterableWithLength<Link> links() {
        return _methodActors.values();
    }

    public CompilerScheme compilerScheme() {
        return vmConfiguration().compilerScheme();
    }

    public TargetGeneratorScheme targetGeneratorScheme() {
        return (TargetGeneratorScheme) compilerScheme();
    }

    public DynamicCompilerScheme jitScheme() {
        return vmConfiguration().jitScheme();
    }

    private ClassInfo lookupInfo(ClassActor classActor) {
        return _classActorInfo.get(classActor);
    }

    private ClassInfo getInfo(ClassActor classActor) {
        ClassInfo info = _classActorInfo.get(classActor);
        if (info == null) {
            info = new ClassInfo();
            _classActorInfo.put(classActor, info);
        }
        return info;
    }

    private GrowableDeterministicSet<ClassActor> getAnonymousClasses(MethodActor actor) {
        GrowableDeterministicSet<ClassActor> anonymousClasses = _anonymousClasses.get(actor);
        if (anonymousClasses == null) {
            anonymousClasses = new LinkedIdentityHashSet<ClassActor>();
            _anonymousClasses.put(actor, anonymousClasses);
        }
        return anonymousClasses;
    }

    private GrowableDeterministicSet<ClassActor> lookupAnonymousClasses(MethodActor actor) {
        return _anonymousClasses.get(actor);
    }

    private void gatherNewClasses() {
        Trace.begin(1, "gatherNewClasses");
        final LinkSequence<ClassActor> newClasses = new LinkSequence<ClassActor>();
        for (ClassActor classActor : ClassRegistry.vmClassRegistry()) {
            if (lookupInfo(classActor) == null) {
                final Method enclosingMethod = classActor.toJava().getEnclosingMethod();
                if (enclosingMethod != null) {
                    // if this is an anonymous class, add it to the anonymous classes set of the enclosing method
                    gatherNewAnonymousClass(newClasses, classActor, enclosingMethod);
                } else {
                    traceNewClass(classActor);
                    newClasses.append(classActor);
                }
            }
        }
        Trace.end(1, "gatherNewClasses");
        Trace.begin(1, "processNewClasses " + newClasses.length());
        for (ClassActor classActor : newClasses) {
            processNewClass(classActor);
        }
        Trace.end(1, "processNewClasses");
    }


    private void traceNewClass(ClassActor classActor) {
        if (Trace.hasLevel(2)) {
            Trace.line(2, "new class: " + classActor);
        }
    }


    private void gatherNewAnonymousClass(final LinkSequence<ClassActor> newClasses, ClassActor classActor, final Method enclosingMethod) {
        if (!MaxineVM.isPrototypeOnly(enclosingMethod)) {
            final MethodActor methodActor = MethodActor.fromJava(enclosingMethod);
            if (methodActor != null) {
                getAnonymousClasses(methodActor).add(classActor);
                if (_methodActors.containsKey(methodActor)) {
                    traceNewClass(classActor);
                    newClasses.append(classActor);
                }
            }
        }
    }

    private void processNewClass(ClassActor classActor) {
        getInfo(classActor); // build the class info for this class
        ClassActor superClassActor = classActor.superClassActor();
        while (superClassActor != null) {
            // for each super class of this class, add this class's implementation of its methods used so far
            final ClassInfo superInfo = getInfo(superClassActor);
            superInfo._subClasses.add(classActor);
            for (MethodActor methodActor : superInfo._indirectCalls) {
                add(classActor.findVirtualMethodActor(methodActor), methodActor, Relationship.OVERRIDES);
            }
            superClassActor = superClassActor.superClassActor();
        }
        if (!classActor.isInterfaceActor()) {
            // for each interface that this class implements, add this class's implementation of its methods used so far
            for (InterfaceActor interfaceActor : classActor.getAllInterfaceActors()) {
                final ClassInfo interfaceInfo = getInfo(interfaceActor);
                interfaceInfo._implementors.add(classActor);
                for (MethodActor methodActor : interfaceInfo._indirectCalls) {
                    add(classActor.findVirtualMethodActor(methodActor), methodActor, Relationship.IMPLEMENTS);
                }
            }
        }

    }

    private <M extends MethodActor> void addMethods(MethodActor referrer, Iterable<M> methodActors, Relationship relationship) {
        for (M methodActor : methodActors) {
            add(methodActor, referrer, relationship);
        }
    }

    private void addMethods(MethodActor referrer, MethodActor[] methodActors, Relationship relationship) {
        for (MethodActor methodActor : methodActors) {
            add(methodActor, referrer, relationship);
        }
    }

    private void processNewTargetMethod(DynamicCompilerScheme dynamicCompilerScheme, TargetMethod targetMethod) {
        traceNewTargetMethod(targetMethod);
        final ClassMethodActor classMethodActor = targetMethod.classMethodActor();
        // if this method contains anonymous classes, add them:
        final GrowableDeterministicSet<ClassActor> anonymousClasses = lookupAnonymousClasses(classMethodActor);
        if (anonymousClasses != null) {
            for (ClassActor classActor : anonymousClasses) {
                processNewClass(classActor);
            }
        }
        // add the methods referenced in the target method's literals
        if (targetMethod.referenceLiterals() != null) {
            for (Object literal : targetMethod.referenceLiterals()) {
                if (literal instanceof MethodActor) {
                    add((MethodActor) literal, classMethodActor, Relationship.LITERAL);
                } else if (literal instanceof ResolutionGuard) {
                    // resolve any unresolved guards now if possible
                    final ResolutionGuard guard = (ResolutionGuard) literal;
                    if (guard.get() == null) {
                        final ConstantPool pool = guard.constantPool();
                        final ResolvableConstant resolvable = pool.resolvableAt(guard.constantPoolIndex());
                        if (resolvable.isResolvableWithoutClassLoading(pool)) {
                            try {
                                guard.set(resolvable.resolve(pool, guard.constantPoolIndex()));
                            } catch (PrototypeOnlyFieldError prototypeOnlyFieldError) {
                            } catch (PrototypeOnlyMethodError prototypeOnlyMethodError) {
                            }
                        }
                    }
                }
            }
        }
        final AppendableSequence<MethodActor> directCalls = new LinkSequence<MethodActor>();
        final AppendableSequence<MethodActor> virtualCalls = new LinkSequence<MethodActor>();
        final AppendableSequence<MethodActor> interfaceCalls = new LinkSequence<MethodActor>();
        // gather all direct, virtual, and interface calls and add them
        dynamicCompilerScheme.gatherCalls(targetMethod, directCalls, virtualCalls, interfaceCalls);
        addMethods(classMethodActor, directCalls, Relationship.DIRECT_CALL);
        addMethods(classMethodActor, virtualCalls, Relationship.VIRTUAL_CALL);
        addMethods(classMethodActor, interfaceCalls, Relationship.INTERFACE_CALL);
        clearCirCache(targetMethod);
    }

    private void clearCirCache(TargetMethod targetMethod) {
        final ClassMethodActor classMethodActor = targetMethod.classMethodActor();
        if (!classMethodActor.isInline()) {
            // TODO: what exactly should be the policy for discarding CIR?
            final CirGenerator cirGenerator = ((CirGeneratorScheme) compilerScheme()).cirGenerator();
            cirGenerator.removeCirMethod(classMethodActor);
        }
    }

    private void traceNewTargetMethod(TargetMethod targetMethod) {
        if (Trace.hasLevel(2)) {
            Trace.line(2, "new target method: " + targetMethod.classMethodActor().format("%H.%n(%P)"));
        }
    }

    private final int _numberOfCompilerThreads;

    CompiledPrototype(JavaPrototype javaPrototype, int numberCompilerThreads) {
        super(javaPrototype.vmConfiguration());

        compilerScheme().compileSnippets();
        // Initialization of the JIT compiler comes second as it may rely on features of the optimizing compiler.
        jitScheme().initializeForJitCompilations();

        _numberOfCompilerThreads = numberCompilerThreads;
        Trace.line(1, "# compiler threads:" + _numberOfCompilerThreads);
    }

    private boolean isIndirectCall(Relationship relationship) {
        return relationship == Relationship.VIRTUAL_CALL || relationship == Relationship.INTERFACE_CALL;
    }

    boolean add(MethodActor methodActor, MethodActor referrer, Relationship relationship) {
        if (methodActor == null) {
            return false;
        }
        if (isIndirectCall(relationship)) {
            // if this is an indirect call that has not been seen before, add all possibly reaching implementations
            // --even if this actual method implementation may not be compiled.
            final ClassInfo info = getInfo(methodActor.holder());
            if (!info._indirectCalls.contains(methodActor)) {
                info._indirectCalls.add(methodActor);
                if (relationship == Relationship.VIRTUAL_CALL) {
                    for (ClassActor subClass : info._subClasses) {
                        add(subClass.findVirtualMethodActor(methodActor), methodActor, Relationship.OVERRIDES);
                    }
                }
                if (relationship == Relationship.INTERFACE_CALL) {
                    for (ClassActor subClass : info._implementors) {
                        add(subClass.findVirtualMethodActor(methodActor), methodActor, Relationship.IMPLEMENTS);
                    }
                }
            }
        }
        if (_methodActors.containsKey(methodActor)) {
            // this method is already processed or on the queue.
            return false;
        }
        if (methodActor.isAnnotationPresent(BOOT_IMAGE_DIRECTIVE.class)) {
            final BOOT_IMAGE_DIRECTIVE annotation = methodActor.toJava().getAnnotation(BOOT_IMAGE_DIRECTIVE.class);
            if (annotation.keepUnlinked()) {
                // if there is an annotation to keep this method unlinked, add to the unlinked methods set
                registerMethodUnlinked(methodActor);
            } else if (annotation.useJitCompiler()) {
                // if there is an explicit annotation to use the JIT compiler
                registerJitMethod(methodActor);
            } else if (annotation.exclude()) {
                return false;
            }
        }
        if (methodActor.isBuiltin() && !SpecialBuiltin.class.isAssignableFrom(methodActor.holder().toJava())) {
//            final ClassActor stubClassActor = ClassActor.fromJava(methodActor.makeInvocationStub().getClass());
//            addMethods(methodActor, stubClassActor.localVirtualMethodActors(), Relationship.INTERFACE_CALL);
        }
        _methodActors.put(methodActor, new Link(methodActor, referrer, relationship));
        _worklist.add(methodActor);
        return true;
    }

    private void addMethodsReferencedByExistingTargetCode() {
        final DynamicCompilerScheme dynamicCompilerScheme = compilerScheme();
        for (TargetMethod targetMethod : Code.bootCodeRegion().targetMethods()) {
            processNewTargetMethod(dynamicCompilerScheme, targetMethod);
        }
    }

    /**
     * Registers a given method that must be statically compiled in the boot image.
     */
    public static void registerImageMethod(ClassMethodActor imageMethodActor) {
        ProgramError.check(imageMethodActor != null);
        _imageMethodActors.add(imageMethodActor);
    }

    public static void registerMethodUnlinked(MethodActor methodActor) {
        _unlinkedMethods.add(methodActor);
    }

    public static void registerClassUnlinked(ClassActor classActor) {
        _unlinkedClasses.add(classActor);
    }

    public static boolean jitCompile(ClassMethodActor classMethodActor) {
        return _jitCompiledMethods.contains(classMethodActor) || _jitCompiledClasses.contains(classMethodActor.holder());
    }

    public static void registerJitClass(Class javaClass) {
        _jitCompiledClasses.add(ClassActor.fromJava(javaClass));
    }

    public static void registerJitMethod(MethodActor methodActor) {
        _jitCompiledMethods.add(methodActor);
    }

    private static AppendableSequence<MethodActor> _imageInvocationStubMethodActors = new LinkSequence<MethodActor>();
    private static AppendableSequence<MethodActor> _imageConstructorStubMethodActors = new LinkSequence<MethodActor>();

    /**
     * Request the given method have a statically generated and compiled invocation stub in the boot image.
     */
    public static void registerImageInvocationStub(MethodActor methodActorWithInvocationStub) {
        _imageInvocationStubMethodActors.append(methodActorWithInvocationStub);
    }

    /**
     * Request that the given method have a statically generated and compiled constructor stub in the boot image.
     * @param methodActor
     */
    public static void registerImageConstructorStub(MethodActor methodActor) {
        _imageConstructorStubMethodActors.append(methodActor);
    }

    private void addVMEntryPoints() {
        final Relationship vmEntryPoint = null;

        final RunScheme runScheme = vmConfiguration().runScheme();
        add(BootImage.getRunMethodActor(MaxineVM.class), null, vmEntryPoint);
        add(BootImage.getRunMethodActor(VmThread.class), null, vmEntryPoint);
        add(BootImage.getRunMethodActor(runScheme.getClass()), null, vmEntryPoint);

        addMethods(null, ClassActor.fromJava(JVMFunctions.class).localStaticMethodActors(), vmEntryPoint);
        addMethods(null, ClassActor.fromJava(JniFunctions.class).localStaticMethodActors(), vmEntryPoint);
        addMethods(null, _imageMethodActors, vmEntryPoint);
        // we would prefer not to invoke stub-generation/compilation for the shutdown hooks procedure, e.g., after an OutOfMemoryError
        try {
            registerImageInvocationStub(ClassActor.fromJava(Class.forName("java.lang.Shutdown")).findLocalStaticMethodActor("shutdown"));
        } catch (ClassNotFoundException classNotFoundException) {
            FatalError.unexpected("cannot load java.lang.Shutdown");
        }

        for (MethodActor methodActor : _imageInvocationStubMethodActors) {
            if (methodActor.holder().toJava().isEnum() && methodActor.name().equals("values")) {
                // add a method stub for the "values" method of the enum
                final ClassActor classActor = ClassActor.fromJava(methodActor.holder().toJava());
                final ClassMethodActor valuesMethod = classActor.findLocalClassMethodActor(SymbolTable.makeSymbol("values"), SignatureDescriptor.fromJava(Enum[].class));
                addStaticAndVirtualMethods(JDK_sun_reflect_ReflectionFactory.createPrePopulatedMethodStub(valuesMethod));
            }
            final ClassActor stubClassActor = ClassActor.fromJava(methodActor.makeInvocationStub().getClass());
            addMethods(null, stubClassActor.localVirtualMethodActors(), vmEntryPoint);
        }
        for (MethodActor methodActor : _imageConstructorStubMethodActors) {
            addStaticAndVirtualMethods(JDK_sun_reflect_ReflectionFactory.createPrePopulatedConstructorStub(methodActor));
        }

        add(ClassActor.fromJava(DebugBreak.class).findLocalStaticMethodActor("here"), null, vmEntryPoint);
        // pre-compile the dynamic linking methods, which reduces startup time
        add(ClassActor.fromJava(Runtime.class).findLocalVirtualMethodActor("loadLibrary0"), null, vmEntryPoint);
        add(ClassActor.fromJava(Runtime.class).findLocalStaticMethodActor("loadLibrary"), null, vmEntryPoint);
        add(ClassActor.fromJava(System.class).findLocalStaticMethodActor("loadLibrary"), null, vmEntryPoint);
        add(ClassActor.fromJava(ClassLoader.class).findLocalStaticMethodActor("loadLibrary0"), null, vmEntryPoint);
        add(ClassActor.fromJava(ClassLoader.class).findLocalStaticMethodActor("loadLibrary"), null, vmEntryPoint);
        add(ClassActor.fromJava(Classes.forName("java.lang.ProcessEnvironment")).findLocalStaticMethodActor("<clinit>"), null, vmEntryPoint);
    }

    private void addStaticAndVirtualMethods(ClassActor classActor) {
        addMethods(null, classActor.localVirtualMethodActors(), (classActor instanceof InterfaceActor) ? Relationship.INTERFACE_CALL : Relationship.VIRTUAL_CALL);
        addMethods(null, classActor.localStaticMethodActors(), Relationship.DIRECT_CALL);
    }

    private int _totalCompilations;

    private boolean compileWorklist() {
        Trace.begin(1, "compile: " + _worklist.size() + " new methods");
        final CodeRegion region = Code.bootCodeRegion();
        final Address oldMark = region.getAllocationMark();
        int submittedCompilations = _totalCompilations;
        final long initialNumberOfCompilations = numberOfCompilations();

        final ExecutorService compilationService = Executors.newFixedThreadPool(_numberOfCompilerThreads);
        final CompletionService<TargetMethod> compilationCompletionService = new ExecutorCompletionService<TargetMethod>(compilationService);
        final CompilationScheme compilationScheme = vmConfiguration().compilationScheme();

        while (true) {
            while (!_worklist.isEmpty()) {
                final MethodActor methodActor = _worklist.removeFirst();
                if (hasCode(methodActor)) {
                    ++submittedCompilations;
                    compilationCompletionService.submit(new Callable<TargetMethod>() {
                        @Override
                        public TargetMethod call() throws Exception {
                            try {
                                return compilationScheme.synchronousCompile((ClassMethodActor) methodActor, CompilationDirective.DEFAULT);
                            } catch (Throwable error) {
                                throw reportCompilationError(methodActor, error);
                            }
                        }
                    });
                }
            }
            if (_totalCompilations >= submittedCompilations) {
                if (!_worklist.isEmpty()) {
                    continue;
                }
                break;
            }
            try {
                final TargetMethod targetMethod = compilationCompletionService.take().get();
                processNewTargetMethod(targetMethod.compilerScheme(), targetMethod);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException executionException) {
                compilationService.shutdownNow();
                ProgramError.unexpected(executionException.getCause());
            }
            ++_totalCompilations;
            if (_totalCompilations % 100 == 0) {
                Trace.line(1, "compiled: " + _totalCompilations + " (" + _methodActors.length() + " methods)");
            }
        }

        compilationService.shutdown();
        final long newCompilations = numberOfCompilations() - initialNumberOfCompilations;
        Trace.end(1, "new compilations: " + newCompilations);
        if (newCompilations == 0) {
            ProgramError.check(region.getAllocationMark().equals(oldMark));
        }
        return newCompilations > 0;
    }


    private ProgramError reportCompilationError(final MethodActor classMethodActor, Throwable error) throws ProgramError {
        System.err.println("Error occurred while compiling " + classMethodActor + ": " + error);
        System.err.println("Referrer chain:");
        System.err.println("    " + classMethodActor.format("%H.%n(%p)"));
        MethodActor referent = classMethodActor;
        while (referent != null) {
            final Link link = _methodActors.get(referent);
            if (link == null) {
                System.err.println("  (no referrer chain available)");
                break;
            }
            if (referent == link._referrer) {
                System.err.println("  which references itself recursively");
                break;
            }
            referent = link._referrer;
            if (referent == null) {
                System.err.println("    which is a VM entry point");
            } else {
                System.err.println("    which " + link._relationship._asReferent + " " + referent.format("%H.%n(%p)"));
            }
        }
        error.printStackTrace(System.err);
        return ProgramError.unexpected("Error occurred while compiling " + classMethodActor, error);
    }

    public void compileUnsafeMethods() {
        Trace.begin(1, "compiling unsafe methods");
        for (ClassMethodActor classMethodActor : UNSAFE.Static.methods()) {
            _worklist.add(classMethodActor);
        }
        compileWorklist();
        Trace.end(1, "compiling unsafe methods");
    }

    private long numberOfCompilations() {
        return jitScheme().numberOfCompilations() + compilerScheme().numberOfCompilations();
    }

    private boolean hasCode(MethodActor methodActor) {
        return methodActor instanceof ClassMethodActor &&
            !methodActor.isAbstract() &&
            (methodActor.isHiddenToReflection() || !methodActor.isBuiltin());
    }

    public void addEntrypoints() {
        // 1. create bootcode region.
        final CodeRegion region = Code.bootCodeRegion();
        region.setSize(Size.fromInt(Integer.MAX_VALUE / 4)); // enable virtually infinite allocations
        // 2. add only entrypoint methods and methods not to be compiled.
        addMethodsReferencedByExistingTargetCode();
        addVMEntryPoints();
    }

    public boolean compile() {
        boolean compiledAny = false;
        boolean compiledSome = false;
        do {
            // 3. add all new class implementations
            gatherNewClasses();
            // 4. compile all new methods
            compiledSome = compileWorklist();
            compiledAny = compiledAny | compiledSome;
        } while (compiledSome);
        return compiledAny;
    }

    private void linkNonVirtualCalls() {
        Trace.begin(1, "linkNonVirtualCalls");
        for (TargetMethod targetMethod : Code.bootCodeRegion().targetMethods()) {
            if (!_unlinkedClasses.contains(targetMethod.classMethodActor().holder()) && !_unlinkedMethods.contains(targetMethod.classMethodActor())) {
                if (!targetMethod.linkDirectCalls()) {
                    targetMethod.linkDirectCalls();
                    ProgramError.unexpected("did not link all direct calls in method: " + targetMethod);
                }
            } else {
                // Link at least direct calls in method prologue
                if (!targetMethod.linkDirectCallsInPrologue()) {
                    ProgramError.unexpected("did not link all direct calls in method: " + targetMethod);
                }
            }
        }
        Trace.end(1, "linkNonVirtualCalls");
    }

    private void linkVTableEntries() {
        Trace.begin(1, "linkVTableEntries");
        for (ClassActor classActor : ClassRegistry.vmClassRegistry()) {
            if (classActor.isReferenceClassActor() && !_unlinkedClasses.contains(classActor)) {
                linkVTable(classActor);
            }
        }
        Trace.end(1, "linkVTableEntries");
    }

    private void linkVTable(ClassActor classActor) {
        final DynamicHub dynamicHub = classActor.dynamicHub();
        for (int vTableIndex = Hub.vTableStartIndex(); vTableIndex < Hub.vTableStartIndex() + dynamicHub.vTableLength(); vTableIndex++) {
            final VirtualMethodActor dynamicMethodActor = classActor.getVirtualMethodActorByVTableIndex(vTableIndex);
            final TargetMethod targetMethod = CompilationScheme.Static.getCurrentTargetMethod(dynamicMethodActor);
            if (targetMethod != null && !_unlinkedMethods.contains(dynamicMethodActor)) {
                dynamicHub.setWord(vTableIndex, VTABLE_ENTRY_POINT.in(targetMethod));
            }
        }
    }

    private void linkITableEntries() {
        Trace.begin(1, "linkITableEntries");

        final IntHashMap<InterfaceActor> serialToInterfaceActor = new IntHashMap<InterfaceActor>();
        for (ClassActor classActor : ClassRegistry.vmClassRegistry()) {
            if (classActor instanceof InterfaceActor) {
                final InterfaceActor interfaceActor = (InterfaceActor) classActor;
                serialToInterfaceActor.put(interfaceActor.id(), interfaceActor);
            }
        }

        for (ClassActor classActor : ClassRegistry.vmClassRegistry()) {
            if (classActor.isReferenceClassActor() && !_unlinkedClasses.contains(classActor)) {
                linkITable(classActor, serialToInterfaceActor);
            }
        }
        Trace.end(1, "linkITableEntries");
    }

    private void linkITable(ClassActor classActor, final IntHashMap<InterfaceActor> serialToInterfaceActor) {
        final DynamicHub hub = classActor.dynamicHub();
        for (int mTableIndex = hub.mTableStartIndex(); mTableIndex < hub.mTableStartIndex() + hub.mTableLength(); mTableIndex++) {
            final int interfaceITableIndex = hub.getInt(mTableIndex);
            if (interfaceITableIndex > 0) {
                final int serial = hub.getWord(interfaceITableIndex).asAddress().toInt();
                final InterfaceActor interfaceActor = serialToInterfaceActor.get(serial);
                if (interfaceActor != null) {
                    for (InterfaceMethodActor interfaceMethodActor : interfaceActor.localInterfaceMethodActors()) {
                        final int methodITableIndex = interfaceITableIndex + interfaceMethodActor.iIndexInInterface();
                        final int iIndex = methodITableIndex - hub.iTableStartIndex();
                        final VirtualMethodActor dynamicMethodActor = classActor.getVirtualMethodActorByIIndex(iIndex);
                        final TargetMethod targetMethod = CompilationScheme.Static.getCurrentTargetMethod(dynamicMethodActor);
                        if (targetMethod != null && !_unlinkedMethods.contains(dynamicMethodActor)) {
                            hub.setWord(methodITableIndex, VTABLE_ENTRY_POINT.in(targetMethod));
                        }
                    }
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

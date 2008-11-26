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

import java.lang.reflect.*;
import java.util.*;

import com.sun.max.*;
import com.sun.max.annotate.*;
import com.sun.max.asm.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.lang.Arrays;
import com.sun.max.program.*;
import com.sun.max.util.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.type.*;

/**
 * This class loads and initializes important JDK packages needed during prototyping.
 *
 * @author Bernd Mathiske
 */
public class JavaPrototype extends Prototype {

    private static JavaPrototype _theJavaPrototype;
    private Sequence<MaxPackage> _basePackages;
    private final Map<MaxPackage, MaxPackage> _excludedMaxPackages = new HashMap<MaxPackage, MaxPackage>();
    private final Set<MaxPackage> _loadedMaxPackages = new HashSet<MaxPackage>();
    private final Map<MethodActor, AccessibleObject> _methodActorMap = new HashMap<MethodActor, AccessibleObject>();
    private final Map<FieldActor, Field> _fieldActorMap = new HashMap<FieldActor, Field>();
    private final Map<ClassActor, Class> _classActorMap = new HashMap<ClassActor, Class>();

    /**
     * Gets a reference to the singleton java prototype.
     *
     * @return the global java prototype
     */
    public static JavaPrototype javaPrototype() {
        return _theJavaPrototype;
    }

    /**
     * Gets all packages in the specified root package that extend the specified package class.
     *
     * @param maxPackageClass the package class that the package must extend
     * @param rootPackage the root package in which to begin the search
     * @return a sequence of the packages that match the criteria
     */
    private Sequence<MaxPackage> getPackages(final Class<? extends MaxPackage> maxPackageClass, MaxPackage rootPackage) {
        final Sequence<MaxPackage> packages = Sequence.Static.filter(rootPackage.getTransitiveSubPackages(PrototypeClassLoader.PROTOTYPE_CLASS_LOADER.classpath()), new Predicate<MaxPackage>() {
            public boolean evaluate(MaxPackage maxPackage) {
                return maxPackageClass.isInstance(maxPackage) && vmConfiguration().isMaxineVMPackage(maxPackage);
            }
        });
        return Sequence.Static.sort(packages, MaxPackage.class);
    }

    /**
     * Returns a sequence of all the basic Maxine packages.
     *
     * @return a sequence of all basic packages
     */
    public Sequence<MaxPackage> basePackages() {
        if (_basePackages == null) {
            _basePackages = getPackages(BasePackage.class, new com.sun.max.Package());
        }
        return _basePackages;
    }

    private Sequence<MaxPackage> _asmPackages;

    /**
     * Returns a sequence of all the assembler packages.
     *
     * @return a sequence of all the assembler packages
     */
    public Sequence<MaxPackage> asmPackages() {
        if (_asmPackages == null) {
            _asmPackages = getPackages(AsmPackage.class, new com.sun.max.asm.Package());
        }
        return _asmPackages;
    }

    private Sequence<MaxPackage> _vmPackages;

    /**
     * Returns a sequence of all the VM packages.
     *
     * @return a sequence of VM packages
     */
    public Sequence<MaxPackage> vmPackages() {
        if (_vmPackages == null) {
            _vmPackages = getPackages(VMPackage.class, new com.sun.max.vm.Package());
        }
        return _vmPackages;
    }

    /**
     * Loads a single java class into the prototype, building the corresponding Actor
     * representation.
     *
     * @param javaClass the class to load into the prototype
     */
    private void loadClass(Class javaClass) {
        assert !MaxineVM.isPrototypeOnly(javaClass);
        Classes.load(PrototypeClassLoader.PROTOTYPE_CLASS_LOADER, javaClass.getName());
    }

    /**
     * Loads a single java class into the prototype, building the corresponding Actor
     * representation.
     *
     * @param name the name of the java class as a string
     */
    public void loadClass(String name) {
        Classes.load(PrototypeClassLoader.PROTOTYPE_CLASS_LOADER, name);
    }

    private final PackageLoader _packageLoader;

    /**
     * Gets the package loader for this java prototype.
     *
     * @return the package loader for this prototype
     */
    public PackageLoader packageLoader() {
        return _packageLoader;
    }

    /**
     * Loads a package into the prototype, building the internal Actor representations
     * of all the classes in the package.
     *
     * @param maxPackage the package to load
     */
    private void loadPackage(MaxPackage maxPackage) {
        final MaxPackage excludedBy = _excludedMaxPackages.get(maxPackage);
        if (excludedBy != null) {
            Trace.line(1, "Excluding " + maxPackage + " (excluded by " + excludedBy + ")");
            return;
        }

        for (MaxPackage excludedPackage : maxPackage.excludes()) {
            if (_loadedMaxPackages.contains(excludedPackage)) {
                ProgramError.unexpected("Package " + excludedPackage + " is excluded by " + maxPackage + ". " +
                                "Adjust class path to ensure " + maxPackage + " is loaded before " + excludedPackage);
            }
            _excludedMaxPackages.put(excludedPackage, maxPackage);
        }
        _packageLoader.load(maxPackage, false);
        _loadedMaxPackages.add(maxPackage);
    }

    /**
     * Loads a package into the prototype, building the internal Actor representations of all the classes in the
     * prototype.
     *
     * @param name the name of the package as a string
     * @param recursive a boolean indicating whether to load all subpackages of the specified package
     * @return a sequence of all the classes loaded from the specified package (and potentially its subpackages).
     */
    public Sequence<Class> loadPackage(String name, boolean recursive) {
        return _packageLoader.load(name, recursive);
    }

    /**
     * Load packages corresponding to VM configurations.
     */
    private void loadVMConfigurationPackages() {
        for (MaxPackage p : vmConfiguration().packages()) {
            loadPackage(p);
        }
    }

    /**
     * Loads a sequence of packages.
     *
     * @param packages the packages to load
     */
    private void loadPackages(Sequence<MaxPackage> packages) {
        for (MaxPackage p : packages) {
            loadPackage(p);
        }
    }

    /**
     * Loads java packages that are necessary to build the prototype.
     */
    public void loadCoreJavaPackages() {
        if (System.getProperty("max.allow.all.core.packages") == null) {
            // Don't want the static Map fields initialized
            PrototypeClassLoader.omitClass(java.lang.reflect.Proxy.class);

            // LogManager and FileSystemPreferences have many side effects
            // that we do not wish to account for before running the target VM.
            // In particular they install shutdown hooks,
            // which then end up in the boot image and cause bugs at target runtime.
            PrototypeClassLoader.omitPackage("java.util.logging");
            PrototypeClassLoader.omitPackage("java.util.prefs");
        }

        HackJDK.checkVMFlags();
        // TODO: determine which of these classes and packages are actually needed
        loadPackage("java.lang", false);
        loadPackage("java.lang.reflect", false); // needed to compile and to invoke the main method
        loadPackage("java.io", false);
        loadPackage("java.nio", false);
        loadPackage("java.nio.charset", false);
        loadPackage("java.util", false);
        loadPackage("java.util.zip", false); // needed to load classes from jar/zip files
        loadPackage("java.util.jar", false); // needed to load classes from jar files
        loadPackage("java.security", false); // needed to create a Thread
        loadClass(sun.nio.cs.StreamEncoder.class);
        loadClass(java.util.Arrays.class);
        loadClass(sun.misc.VM.class);

        // These classes need to be compiled and in the boot image in order to be able to run the optimizing compiler at
        // run time.
        loadClass(sun.misc.SharedSecrets.class);
        loadClass(sun.reflect.annotation.AnnotationParser.class);
        loadClass(sun.reflect.Reflection.class);

        // Necessary for Java Run Scheme to initialize the System class:
        loadClass(sun.misc.Version.class);
        loadPackage("sun.nio.cs", false);

        // Necessary for early tracing:
        loadPackage("java.util.regex", false);
        loadClass(sun.security.action.GetPropertyAction.class);
    }

    private static Sequence<Class> _mainPackageClasses = new ArrayListSequence<Class>();

    public static Sequence<Class> mainPackageClasses() {
        return _mainPackageClasses;
    }

    /**
     * Ensures that all the Maxine classes currently in the {@linkplain ClassRegistry#vmClassRegistry() VM class
     * registry} are {@linkplain Classes#initialize(Class) initialized}. Any class in a subpackage of {@code
     * com.sun.max} is deemed to be a Maxine class. These initializers are never re-run in the target VM
     * and so they are omitted from the boot image (as if they had the {@link PROTOTYPE_ONLY} annotation
     * applied to them).
     */
    private static void initializeMaxClasses() {
        final ClassActor[] classActors = Arrays.from(ClassActor.class, ClassRegistry.vmClassRegistry());
        for (ClassActor classActor : classActors) {
            if (com.sun.max.Package.contains(classActor.toJava())) {
                Classes.initialize(classActor.toJava());
            }
        }
    }

    /**
     * Create a new Java prototype with the specified VM configuration.
     *
     * @param vmConfiguration the VM configuration
     * @param loadPackages a boolean indicating whether to load the Java packages and VM packages
     */
    public JavaPrototype(final VMConfiguration vmConfiguration, final boolean loadPackages) {
        super(vmConfiguration);

        _packageLoader = new PackageLoader(PrototypeClassLoader.PROTOTYPE_CLASS_LOADER, PrototypeClassLoader.PROTOTYPE_CLASS_LOADER.classpath());
        _theJavaPrototype = this;

        MaxineVM.setTarget(new MaxineVM(vmConfiguration));
        vmConfiguration.loadAndInstantiateSchemes();

        Trace.line(1, "Host VM configuration:\n" + MaxineVM.host().configuration());
        Trace.line(1, "Target VM configuration:\n" + MaxineVM.target().configuration());
        Trace.line(1, "JDK: " + System.getProperty("java.version"));
        Trace.line(1);

        MaxineVM.usingTarget(new Runnable() {

            public void run() {
                loadVMConfigurationPackages();

                ClassActor.DEFERRABLE_QUEUE_1.runAll();

                initializeMaxClasses();

                vmConfiguration.compilerScheme().createBuiltins(_packageLoader);
                Builtin.register(vmConfiguration.compilerScheme());
                vmConfiguration.compilerScheme().createSnippets(_packageLoader);
                Snippet.register();

                if (loadPackages) {

                    // TODO: Load the following package groups in parallel
                    loadPackages(basePackages());
                    loadPackages(vmPackages());
                    loadPackages(asmPackages());

                    initializeMaxClasses();

                    vmConfiguration.initializeSchemes(MaxineVM.Phase.PROTOTYPING);

                    // VM implementation classes ending up in the bootstrap image
                    // are supposed to be limited to those loaded up to here.
                    //
                    // This enables detection of violations of said requirement:
                    ClassActor.prohibitPackagePrefix(new com.sun.max.Package());

                    UNSAFE.Static.determineMethods();
                } else {
                    vmConfiguration.initializeSchemes(MaxineVM.Phase.PROTOTYPING);
                }
            }
        });
    }

    /**
     * Gets the corresponding Java reflection representation of the specified method.
     *
     * @param methodActor the method actor for which to retrieve the Java equivalent
     * @return the Java reflection method for the specified method actor
     */
    public synchronized Method toJava(MethodActor methodActor) {
        assert MaxineVM.isPrototyping();
        Method javaMethod = (Method) _methodActorMap.get(methodActor);
        if (javaMethod == null) {
            final Class<?> holder = methodActor.holder().toJava();
            final SignatureDescriptor descriptor = methodActor.descriptor();
            final Class[] parameterTypes = descriptor.getParameterTypes(holder.getClassLoader());
            final ClassLoader classLoader = holder.getClassLoader();
            final String name = methodActor.isSurrogate() ? com.sun.max.annotate.SURROGATE.Static.toSurrogateName(methodActor.name().toString()) : methodActor.name().toString();
            javaMethod = Classes.getDeclaredMethod(holder, descriptor.getResultDescriptor().toJava(classLoader), name, parameterTypes);
            _methodActorMap.put(methodActor, javaMethod);
        }
        assert MethodActor.fromJava(javaMethod) == methodActor;
        return javaMethod;
    }

    /**
     * Gets the corresponding Java reflection representation of the specified constructor.
     *
     * @param methodActor the method actor for which to retrieve the Java equivalent
     * @return the Java reflection method for the specified method actor
     */
    public synchronized Constructor toJavaConstructor(MethodActor methodActor) {
        assert MaxineVM.isPrototyping();
        Constructor javaConstructor = (Constructor) _methodActorMap.get(methodActor);
        if (javaConstructor == null) {
            final Class<?> holder = methodActor.holder().toJava();
            final Class[] parameterTypes = methodActor.descriptor().getParameterTypes(holder.getClassLoader());
            javaConstructor = Classes.getDeclaredConstructor(holder, parameterTypes);
            _methodActorMap.put(methodActor, javaConstructor);
        }
        assert MethodActor.fromJavaConstructor(javaConstructor) == methodActor;
        return javaConstructor;
    }

    /**
     * Gets the corresponding Java reflection representation of the specified field.
     *
     * @param fieldActor the field actor for which to get the Java equivalent
     * @return the Java reflection field for the specified field actor
     */
    public synchronized Field toJava(FieldActor fieldActor) {
        assert MaxineVM.isPrototyping();
        Field javaField = _fieldActorMap.get(fieldActor);
        if (javaField == null) {
            final Class javaHolder = fieldActor.holder().toJava();
            javaField = Classes.getDeclaredField(javaHolder, fieldActor.name().toString());
            _fieldActorMap.put(fieldActor, javaField);
        }
        return javaField;
    }

    /**
     * Gets the corresponding Java class for the specified class actor.
     *
     * @param classActor the class actor for which to get the Java equivalent
     * @return the Java reflection class for the specified class actor
     */
    public synchronized Class toJava(ClassActor classActor) {
        assert MaxineVM.isPrototyping();
        Class javaClass = _classActorMap.get(classActor);
        if (javaClass == null) {
            try {
                javaClass = classActor.typeDescriptor().toJava(classActor.classLoader());
            } catch (NoClassDefFoundError noClassDefFoundError) {
                // try again with the prototype class loader.
                javaClass = classActor.typeDescriptor().toJava(VmClassLoader.VM_CLASS_LOADER); // TODO: Shouldn't this be PROTOTYPE_CLASS_LOADER?
            }
            _classActorMap.put(classActor, javaClass);
        }
        return javaClass;
    }
}

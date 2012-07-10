/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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

import static com.sun.max.annotate.LOCAL_SUBSTITUTION.Static.*;
import static com.sun.max.vm.VMConfiguration.*;
import static com.sun.max.vm.hosted.HostedVMClassLoader.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

import com.sun.max.annotate.*;
import com.sun.max.config.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.jdk.*;
import com.sun.max.vm.jdk.Package;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * The {@link ClassActor} context when {@linkplain BootImageGenerator generating} the
 * boot image or otherwise executing code that loads and uses {@link ClassActor}s.
 *
 * There is a single global {@code JavaPrototype} object which is {@linkplain #initialize(int, boolean) initialized} once.
 *
 */
public final class JavaPrototype extends Prototype {

    /**
     * The name of the system property that can be used to specify extra classes and packages to be loaded
     * into a Java prototype by {@link #loadExtraClassesAndPackages()}. The value of the property is
     * parsed as a space separated list of class and package names. Package names are those
     * prefixed by '^'.
     */
    public static final String EXTRA_CLASSES_AND_PACKAGES_PROPERTY_NAME = "max.image.extraClassesAndPackages";

    private static JavaPrototype theJavaPrototype;
    private final Set<BootImagePackage> loadedBootImagePackages = new HashSet<BootImagePackage>();
    private final ConcurrentHashMap<MethodActor, AccessibleObject> methodActorMap = new ConcurrentHashMap<MethodActor, AccessibleObject>();
    private final ConcurrentHashMap<FieldActor, Field> fieldActorMap = new ConcurrentHashMap<FieldActor, Field>();
    private final ConcurrentHashMap<ClassActor, Class> classActorMap = new ConcurrentHashMap<ClassActor, Class>();
    private final ConcurrentHashMap<Method, MethodActor> javaMethodMap = new ConcurrentHashMap<Method, MethodActor>();
    private final ConcurrentHashMap<Constructor, MethodActor> javaConstructorMap = new ConcurrentHashMap<Constructor, MethodActor>();
    private final ConcurrentHashMap<Field, FieldActor> javaFieldMap = new ConcurrentHashMap<Field, FieldActor>();

    /**
     * Gets a reference to the singleton Java prototype.
     *
     * @return the global java prototype
     */
    public static JavaPrototype javaPrototype() {
        return theJavaPrototype;
    }

    /**
     * Loads a single java class into the prototype, building the corresponding Actor
     * representation.
     *
     * @param javaClass the class to load into the prototype
     */
    private void loadClass(Class javaClass) {
        assert !MaxineVM.isHostedOnly(javaClass);
        loadClass(javaClass.getName());
    }

    /**
     * Loads a single java class into the prototype, building the corresponding Actor
     * representation.
     *
     * @param name the name of the java class as a string
     */
    public void loadClass(String name) {
        Class clazz = Classes.load(HOSTED_VM_CLASS_LOADER, name);
        Classes.initialize(clazz);
    }

    private final PackageLoader packageLoader;

    /**
     * A object to signal that all references to a particular object should be set to null.
     */
    public static final Object NULL = new Object();

    /**
     * A map used during bootstrapping to replace references to a particular object with
     * references to another object during graph reachability.
     */
    private Map<Object, Object> objectMap;

    /**
     * A list of external contributors to the {@linkplain #objectMap}.
     */
    private static List<ObjectIdentityMapContributor> objectMapContributors = new ArrayList<ObjectIdentityMapContributor>();

    /**
     * Interface that must be implemented by external contributors to the {@linkplain #objectMap}.
     */
    public interface ObjectIdentityMapContributor {
        void initializeObjectIdentityMap(Map<Object, Object> objectMap);
    }

    /**
     * A map used to canonicalize instances of the Maxine value classes.
     */
    private final Map<Object, Object> valueMap = new HashMap<Object, Object>();

    /**
     * Gets the package loader for this java prototype.
     *
     * @return the package loader for this prototype
     */
    public PackageLoader packageLoader() {
        return packageLoader;
    }

    /**
     * Loads a package into the prototype, building the internal Actor representations
     * of all the classes in the package.
     *
     * @param maxPackage the package to load
     */
    private void loadBootImagePackage(BootImagePackage maxPackage) {
        packageLoader.load(maxPackage, true);
        loadedBootImagePackages.add(maxPackage);
    }

    /**
     * Load packages corresponding to VM configurations.
     */
    private void loadVMConfigurationPackages() {
        for (BootImagePackage p : vmConfig().packages()) {
            loadBootImagePackage(p);
        }
    }

    /**
     * Loads extra packages and classes that are necessary to build a self-sufficient VM image.
     */
    public void loadExtraClassesAndPackages() {
        String value = System.getProperty(EXTRA_CLASSES_AND_PACKAGES_PROPERTY_NAME);
        if (value != null) {
            for (String s : value.split("\\s+")) {
                if (s.charAt(0) == '^') {
                    packageLoader.load(new BootImagePackage(s.substring(1), false) {}, true);
                } else {
                    loadClass(s);
                }
            }
        }

    }

    private static List<Class> mainPackageClasses = new ArrayList<Class>();

    public static List<Class> mainPackageClasses() {
        return mainPackageClasses;
    }


    /**
     * Loads all classes annotated with {@link METHOD_SUBSTITUTIONS} and performs the relevant substitutions.
     * Substitutor classes are only treated if the JDK version is appropriate.
     */
    private void loadMethodSubstitutions(final VMConfiguration vmConfiguration) {
        for (BootImagePackage bootImagePackage : vmConfiguration.bootImagePackages) {
            if (bootImagePackage.isPartOfMaxineVM(vmConfiguration) && bootImagePackage.containsMethodSubstitutions()) {
                String[] classes = bootImagePackage.listClasses(packageLoader.classpath);
                for (String cn : classes) {
                    try {
                        Class<?> c = Class.forName(cn, false, Package.class.getClassLoader());
                        if (JDK.thisVersionOrNewer(c.getAnnotation(JDK_VERSION.class))) {
                            METHOD_SUBSTITUTIONS annotation = c.getAnnotation(METHOD_SUBSTITUTIONS.class);
                            if (annotation != null) {
                                loadClass(c);
                                ClassActor subs = toClassActor(c);
                                METHOD_SUBSTITUTIONS.Static.processAnnotationInfo(annotation, subs);
                                for (VirtualMethodActor sub : subs.localVirtualMethodActors()) {
                                    if (!sub.isInstanceInitializer() && !sub.isPrivate()) {
                                        VirtualMethodActor original = (VirtualMethodActor) METHOD_SUBSTITUTIONS.Static.findOriginal(sub);
                                        if (original == null) {
                                            // We cannot do vtable dispatch to methods on a substitute class.
                                            FatalError.unexpected("Non-static method in substitution class must be private: " + sub);
                                        } else {
                                            sub.resetVTableIndexForSubstitute(original.vTableIndex());
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        throw ProgramError.unexpected(e);
                    }
                }
            }
        }
    }

    /**
     * Extends {@link PackageLoader} to ignore classes that are {@link HostOnlyClassError prototype only} or
     * explicitly {@linkplain OmittedClassError omitted} from the boot image.
     *
     */
    static class PrototypePackageLoader extends PackageLoader {

        public PrototypePackageLoader(ClassLoader classLoader, Classpath classpath) {
            super(classLoader, classpath);
        }

        @Override
        protected Class loadClass(String className) {
            try {
                return super.loadClass(className);
            } catch (HostOnlyClassError e) {
                Trace.line(2, "Ignoring hosted only type: " + className);
            } catch (OmittedClassError e) {
                Trace.line(2, "Ignoring explicitly omitted type: " + className);
            }
            return null;
        }
    }

    /**
     * Callback interface for classes that want to optimize/check some static data once all classes are loaded and initialized.
     *
     */
    public interface InitializationCompleteCallback {
        /**
         * Invoked on all registered callbacks after all classes are loaded and initialized.
         */
        void initializationComplete();
    }

    private static ArrayList<InitializationCompleteCallback> initializationCompleteCallbacks = new ArrayList<InitializationCompleteCallback>();

    public static InitializationCompleteCallback registerInitializationCompleteCallback(InitializationCompleteCallback completionCallback) {
        initializationCompleteCallbacks.add(completionCallback);
        return completionCallback;
    }

    /**
     * Callback interface for classes that may want to check that their generated code is up to date.
     *
     */
    public interface GeneratedCodeCheckerCallback {
        void checkGeneratedCode();
    }

    private static ArrayList<GeneratedCodeCheckerCallback> checkGeneratedCodeCallbacks = new ArrayList<GeneratedCodeCheckerCallback>();

    public static GeneratedCodeCheckerCallback registerGeneratedCodeCheckerCallback(GeneratedCodeCheckerCallback checkGeneratedCodeCallback) {
        checkGeneratedCodeCallbacks.add(checkGeneratedCodeCallback);
        return checkGeneratedCodeCallback;
    }

    /**
     * Initializes the global Java prototype. This also initializes the global {@linkplain MaxineVM#vm() VM}
     * context if it hasn't been set.
     * @param checkautogen whether to check the automatically generated code for consistency
     */
    public static void initialize(boolean checkautogen) {
        initialize(1, checkautogen);
    }

    /**
     * Initializes the global Java prototype. This also initializes the global {@linkplain MaxineVM#vm() VM}
     * context if it hasn't been set.
     * @param threadCount the number of threads that can be used to load packages in parallel. Anything less than or
     *            equal to 1 implies package loading is to be single-threaded.
     * @param checkautogen whether to check the automatically generated code for consistency
     */
    public static void initialize(int threadCount, boolean checkautogen) {
        assert theJavaPrototype == null : "Cannot initialize the JavaPrototype more than once";
        if (MaxineVM.vm() == null) {
            new VMConfigurator(null).create();
        }
        theJavaPrototype = new JavaPrototype(threadCount, checkautogen);
    }

    /**
     * Create a new Java prototype with the specified VM configuration.
     * @param threadCount the number of threads that can be used to load packages in parallel. Anything less than or
     *            equal to 1 implies package loading is to be single-threaded.
     * @param checkautogen whether to check the automatically generated code for consistency
     */
    private JavaPrototype(int threadCount, boolean checkautogen) {
        VMConfiguration config = vmConfig();
        packageLoader = new PrototypePackageLoader(HOSTED_VM_CLASS_LOADER, HOSTED_VM_CLASS_LOADER.classpath());
        theJavaPrototype = this;

        if (Trace.hasLevel(1)) {
            PrintStream out = Trace.stream();
            out.println("======================== VM Configuration ========================");
            out.print(vmConfig());
            out.println("JDK: " + System.getProperty("java.version"));
            out.println("==================================================================");
        }

        loadVMConfigurationPackages();

        AdapterGenerator.init();

        ClassActor.DEFERRABLE_QUEUE_1.runAll();

        loadMethodSubstitutions(config);

        for (BootImagePackage maxPackage : config.bootImagePackages) {
            loadBootImagePackage(maxPackage);
        }
        loadExtraClassesAndPackages();

        if (checkautogen) {
            for (GeneratedCodeCheckerCallback checkGeneratedCodeCallback : checkGeneratedCodeCallbacks) {
                checkGeneratedCodeCallback.checkGeneratedCode();
            }
        }

        config.initializeSchemes(MaxineVM.Phase.BOOTSTRAPPING);

        for (InitializationCompleteCallback completionCallback : initializationCompleteCallbacks) {
            completionCallback.initializationComplete();
        }

    }

    /**
     * Gets the corresponding Java reflection representation of the specified method.
     *
     * @param methodActor the method actor for which to retrieve the Java equivalent
     * @return the Java reflection method for the specified method actor
     */
    public Method toJava(MethodActor methodActor) {
        Method javaMethod = (Method) methodActorMap.get(methodActor);
        if (javaMethod == null) {
            final Class<?> holder = methodActor.holder().toJava();
            final SignatureDescriptor descriptor = methodActor.descriptor();
            final Class[] parameterTypes = descriptor.resolveParameterTypes(holder.getClassLoader());
            final ClassLoader classLoader = holder.getClassLoader();
            final String name = methodActor.isLocalSubstitute() ? LOCAL_SUBSTITUTION.Static.toSubstituteName(methodActor.name.toString()) : methodActor.name.toString();
            javaMethod = Classes.getDeclaredMethod(holder, descriptor.resultDescriptor().resolveType(classLoader), name, parameterTypes);
            methodActorMap.put(methodActor, javaMethod);
        }
        MethodActor jMethodActor = MethodActor.fromJava(javaMethod);
        assert jMethodActor == methodActor;
        return javaMethod;
    }

    /**
     * Gets the corresponding Java reflection representation of the specified constructor.
     *
     * @param methodActor the method actor for which to retrieve the Java equivalent
     * @return the Java reflection method for the specified method actor
     */
    public Constructor toJavaConstructor(MethodActor methodActor) {
        Constructor javaConstructor = (Constructor) methodActorMap.get(methodActor);
        if (javaConstructor == null) {
            final Class<?> holder = methodActor.holder().toJava();
            final Class[] parameterTypes = methodActor.descriptor().resolveParameterTypes(holder.getClassLoader());
            javaConstructor = Classes.getDeclaredConstructor(holder, parameterTypes);
            methodActorMap.put(methodActor, javaConstructor);
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
    public Field toJava(FieldActor fieldActor) {
        Field javaField = fieldActorMap.get(fieldActor);
        if (javaField == null) {
            final Class javaHolder = fieldActor.holder().toJava();
            javaField = Classes.getDeclaredField(javaHolder, fieldActor.name.toString());
            fieldActorMap.put(fieldActor, javaField);
        }
        return javaField;
    }

    /**
     * Gets the corresponding Java class for the specified class actor.
     *
     * @param classActor the class actor for which to get the Java equivalent
     * @return the Java reflection class for the specified class actor
     */
    public Class toJava(ClassActor classActor) {
        Class javaClass = classActorMap.get(classActor);
        if (javaClass == null) {
            try {
                javaClass = classActor.typeDescriptor.resolveType(classActor.classLoader);
            } catch (OmittedClassError e) {
                // Failed with the prototype loader: try again with the VM class loader.
                javaClass = classActor.typeDescriptor.resolveType(BootClassLoader.BOOT_CLASS_LOADER);
            }
            classActorMap.put(classActor, javaClass);
        }
        return javaClass;
    }

    /**
     * Gets the corresponding class actor for the specified Java class.
     *
     * @param javaClass the Java class for which to get the class actor
     * @return the class actor for {@code javaClass} or {@code null} if {@code javaClass} is annotated with {@link HOSTED_ONLY}
     */
    public ClassActor toClassActor(Class javaClass) {
        if (MaxineVM.isHostedOnly(javaClass)) {
            return null;
        }

        TypeDescriptor typeDescriptor = JavaTypeDescriptor.forJavaClass(javaClass);
        ClassActor classActor = ClassRegistry.getInBootOrVM(typeDescriptor);
        if (classActor != null) {
            return classActor;
        }
        return typeDescriptor.resolveHosted(javaClass.getClassLoader());
    }

    /**
     * Gets the corresponding method actor for the specified Java method.
     *
     * @param javaMethod the Java method for which to get the method actor
     * @return the method actor for {@code javaMethod}
     */
    public MethodActor toMethodActor(Method javaMethod) {
        MethodActor methodActor = javaMethodMap.get(javaMethod);
        if (methodActor == null) {
            final Utf8Constant name = SymbolTable.makeSymbol(javaMethod.getAnnotation(LOCAL_SUBSTITUTION.class) != null ? toSubstituteeName(javaMethod.getName()) : javaMethod.getName());
            final ClassActor holder = toClassActor(javaMethod.getDeclaringClass());
            ProgramError.check(holder != null, "Could not find " + javaMethod.getDeclaringClass());
            final SignatureDescriptor signature = SignatureDescriptor.fromJava(javaMethod);
            methodActor = holder.findLocalMethodActor(name, signature);
            if (methodActor == null) {
                throw new NoSuchMethodError("Could not find " + name + signature + " in " + holder);
            }
            javaMethodMap.put(javaMethod, methodActor);
        }
        return methodActor;
    }

    /**
     * Gets the corresponding method actor for the specified Java constructor.
     *
     * @param javaConstructor the Java constructor for which to get the method actor
     * @return the method actor for {@code javaConstructor}
     */
    public MethodActor toMethodActor(Constructor javaConstructor) {
        MethodActor methodActor = javaConstructorMap.get(javaConstructor);
        if (methodActor == null) {
            final ClassActor holder = toClassActor(javaConstructor.getDeclaringClass());
            final SignatureDescriptor signature = SignatureDescriptor.fromJava(javaConstructor);
            methodActor = holder.findLocalMethodActor(SymbolTable.INIT, signature);
            if (methodActor == null) {
                throw new NoSuchMethodError("Could not find <init>" + signature + " in " + holder);
            }
            javaConstructorMap.put(javaConstructor, methodActor);
        }
        return methodActor;
    }

    /**
     * Gets the corresponding field actor for the specified Java field.
     *
     * @param javaField the Java field for which to get the field actor
     * @return the field actor for {@code javaField}
     */
    public FieldActor toFieldActor(Field javaField) {
        FieldActor fieldActor = javaFieldMap.get(javaField);
        if (fieldActor == null) {
            final ClassActor holder = toClassActor(javaField.getDeclaringClass());
            final TypeDescriptor signature = JavaTypeDescriptor.forJavaClass(javaField.getType());
            final Utf8Constant name = SymbolTable.makeSymbol(javaField.getName());
            fieldActor = holder.findFieldActor(name, signature);
            if (fieldActor == null) {
                throw new NoSuchFieldError("Could not find " + name + signature + " in " + holder);
            }
            javaFieldMap.put(javaField, fieldActor);
        }
        return fieldActor;
    }

    /**
     * Gets the system thread group.
     *
     * @return the thread group for the entire system
     */
    private static ThreadGroup getSystemThreadGroup() {
        ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
        while (true) {
            final ThreadGroup parent = threadGroup.getParent();
            if (parent == null) {
                assert threadGroup.getName().equals("system");
                return threadGroup;
            }
            threadGroup = parent;
        }
    }

    /**
     * This method maps a host object to a target object. For most objects, this method will return the parameter
     * object, but some objects are not portable from the host VM to the target VM and references to them must be
     * updated with references to a different object (perhaps {@code null}).
     *
     * @param object the host object to translate
     * @return a reference to the corresponding object in the target VM
     */
    public static Object hostToTarget(Object object) {
        return theJavaPrototype.hostToTarget0(object);
    }

    private Object hostToTarget0(Object object) {
        if (object instanceof String || object instanceof Value || object instanceof NameAndTypeConstant) {
            // canonicalize all instances of these classes using .equals()
            Object result = valueMap.get(object);
            if (result == null) {
                result = object;
                valueMap.put(object, object);
            }
            return result;
        }

        final Object replace = getObjectReplacement(object);
        if (replace != null) {
            return replace == NULL ? null : replace;
        }
        if (object instanceof Thread || object instanceof ThreadGroup) {
            throw FatalError.unexpected("Instance of thread class " + object.getClass().getName() + " will be null in the image");
        }

        if (object instanceof FileDescriptor) {
            throw FatalError.unexpected("Instance of " + object.getClass().getName() + " will have host file descriptor in the image");
        }
        if (object instanceof java.util.zip.ZipFile) {
            throw FatalError.unexpected("Instance of " + object.getClass().getName() + " will have host native zip file pointer");
        }
        return object;
    }

    private Object getObjectReplacement(Object object) {
        if (objectMap == null) {
            // check the object identity map certain objects to certain other objects
            initializeObjectIdentityMap();
        }
        final Object replace = objectMap.get(object);
        return replace;
    }

    @SuppressWarnings("unchecked")
    private void initializeObjectIdentityMap() {
        objectMap = new IdentityHashMap<Object, Object>();

        objectMap.put(HostedBootClassLoader.HOSTED_BOOT_CLASS_LOADER, BootClassLoader.BOOT_CLASS_LOADER);
        objectMap.put(BootClassLoader.BOOT_CLASS_LOADER.getParent(), NULL);
        objectMap.put(HostedVMClassLoader.HOSTED_VM_CLASS_LOADER, VMClassLoader.VM_CLASS_LOADER);

        objectMap.put(VmThread.hostSystemThreadGroup, VmThread.systemThreadGroup);
        objectMap.put(VmThread.hostMainThreadGroup, VmThread.mainThreadGroup);
        objectMap.put(VmThread.hostReferenceHandlerThread, VmThread.referenceHandlerThread.javaThread());
        objectMap.put(VmThread.hostFinalizerThread, VmThread.finalizerThread.javaThread());
        objectMap.put(VmThread.hostMainThread, VmThread.mainThread.javaThread());

        objectMap.put(VmThread.systemThreadGroup, VmThread.systemThreadGroup);
        objectMap.put(VmThread.mainThreadGroup, VmThread.mainThreadGroup);
        objectMap.put(VmThread.referenceHandlerThread.javaThread(), VmThread.referenceHandlerThread.javaThread());
        objectMap.put(VmThread.finalizerThread.javaThread(), VmThread.finalizerThread.javaThread());
        objectMap.put(VmThread.mainThread.javaThread(), VmThread.mainThread.javaThread());
        objectMap.put(VmThread.vmOperationThread.javaThread(), VmThread.vmOperationThread.javaThread());
        objectMap.put(VmThread.signalDispatcherThread.javaThread(), VmThread.signalDispatcherThread.javaThread());

        // These are the only FileDescriptors allowed in the image
        objectMap.put(FileDescriptor.in, FileDescriptor.in);
        objectMap.put(FileDescriptor.out, FileDescriptor.out);
        objectMap.put(FileDescriptor.err, FileDescriptor.err);

        objectMap.put(Trace.stream(), Log.out);
        objectMap.put(WithoutAccessCheck.getStaticField(System.class, "props"), JDKInterceptor.initialSystemProperties);

        objectMap.put(WithoutAccessCheck.getStaticField(Proxy.class, "loaderToCache"), new WeakHashMap());
        objectMap.put(WithoutAccessCheck.getStaticField(Proxy.class, "proxyClasses"), Collections.synchronizedMap(new WeakHashMap()));

        for (ObjectIdentityMapContributor contributor : objectMapContributors) {
            contributor.initializeObjectIdentityMap(objectMap);
        }
    }

    public static void addObjectIdentityMapContributor(ObjectIdentityMapContributor contributor) {
        objectMapContributors.add(contributor);
    }
}

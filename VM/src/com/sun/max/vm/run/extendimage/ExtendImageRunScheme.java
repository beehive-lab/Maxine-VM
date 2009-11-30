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
package com.sun.max.vm.run.extendimage;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.io.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.object.TupleAccess;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.prototype.*;
import com.sun.max.vm.run.java.*;

/**
 * A run scheme that allows additional classes to be added to the boot image but otherwise reuses the Java run scheme.
 * To ensure that everything needed is built into the image, controls are provided for forcing compilation of methods
 * and generating stubs used in reflection. The expected way to specify all this is in a command file, passed using the
 * SPECFILE_PROPERTY_NAME property.
 *
 * The command file must contain a sequence of commands, one per line. The following commands are available. All names
 * must be fully qualified. Empty lines are ignored as are lines beginning with a # character.
 * <dl>
 * <dt> package packagename </dt>
 * <dd>Loads all the classes in package packagename.</dd>
 * <dt> class classname</dt>
 * <dd>Loads the class classname.</dd>
 * <dd>omitclass classname
 * <dt> Do not include the class classname (typically used to suppress nested classes)
 * <dt> classinit classname</dt>
 * <dd>Loads and eagerly initializes the class classname.</dd>
 * <dt> forcemethod methodname</dt>
 * <dd>Force the compilation of methodname. The final component of the name may be an * to indicate forcing compilation
 * of all methods in the class. E.g. forcemethod acme.someclass.*.</dd>
 * <dt> forceconstructorstubs classname</dt>
 * <dd>Force the generation of constructor stubs for class classname.</dd>
 * <dt> forceinvocationstub methodname</dt>
 * <dd>Force the generation of a (reflective) invocation stub for method methodname.</dd>
 * <dt> setproperty property[=value]</dt>
 * <dd>Set the property to the given value, or null if no value given, before executing the main program.</dd>
 * <dt> reinitclass classname</dt>
 * <dd>Reinitialize the class classname before executing the main program.</dd>
 * <dt> resetfield fieldname
 * <dd> Reset the static field fieldname to its default value in the image.</dd>
 * <dt> resetlauncher
 * <dd> Reset the state of sun.misc.Launcher during startup</dd>
 * <dt> callmethod methodname</dt>
 * <dd>Call the method methodname before executing the main program.</dd>
 * <dt> include filename</dt>
 * <dd>Process commands from the file filename.</dd>
 * <dt> mainclass classname</dt>
 * <dd>This is equivalent to the following sequence of commands:
 * <ul>
 * <li>class classname</li>
 * <li>forcecompilemethod classname.main</li>
 * <li>forceinvocationstub classname.main</li>
 * </ul>
 * </dd>
 * <dt> imagefile filename [asname]</dt>
 * <dd> Include the file filename in the image file system. If filename is a directory all files in the directory (and
 * subdirectories recursively) are added. If asname is given, it is used as the name in the image.</dd>
 * <dt> imagefsprefix path</dt>
 * <dd> Use path as the prefix to all files stored with imagefile. The default is the empty string. Any trailing '/' is
 * removed.</dd>
 * </dl>
 * <P>
 * Not all of these commands are strictly necessary any more. They evolved during the period when dynamic compilation
 * was not available and JDK startup was incomplete. However, they do make it possible to build a completely
 * self-contained image entirely compiled with the bootstrap compiler. A more typical use is for environments where
 * more of the native libraries are written in Java (but not included in the VM source base) and need to be present in
 * the image at startup.
 *
 * It is also possible to include the standard Maxine tests in the image and run them instead of a main program. To do
 * this set the property "max.vm.run.extendimage.testrun" to the name of a package that contains an appropriate
 * JavaTesterRunScheme class, e.g. test.com.sun.max.vm.testrun.all.
 *
 * @author Mick Jordan
 */
public class ExtendImageRunScheme extends JavaRunScheme {

    private static final String MAINCLASS_PROPERTY_NAME = "max.vm.run.extendimage.mainclass";
    private static final String SPECFILE_PROPERTY_NAME = "max.vm.run.extendimage.specfile";
    private static final String TESTER_PROPERTY_NAME = "max.vm.run.extendimage.testrun";
    private static String mainClassName;
    private static AppendableSequence<StaticMethodActor> callMethods = new LinkSequence<StaticMethodActor>();
    private static AppendableSequence<ClassActor> reinitClasses = new LinkSequence<ClassActor>();
    private static AppendableSequence<FieldActor> reinitFields = new LinkSequence<FieldActor>();
    private static Properties properties = new Properties();
    private static Map<String, byte[]> imageFS = new HashMap<String, byte[]>();
    private static String imageFSPrefix = "";
    private static Set<File> includeSet = new HashSet<File>();
    private static JavaRunScheme tester;
    private static boolean resetLauncher;

    public ExtendImageRunScheme(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
    }

    @Override
    public void initialize(MaxineVM.Phase phase) {
        if (MaxineVM.isHosted()) {
            if (MaxineVM.isHosted()) {
                extendImage();
            }
        }

        if (phase == MaxineVM.Phase.STARTING) {
            for (ClassActor classActor : reinitClasses) {
                classActor.callInitializer();
            }
        }

        super.initialize(phase);

        if (phase == MaxineVM.Phase.RUNNING) {
            final Enumeration<?> propEnum = properties.propertyNames();
            while (propEnum.hasMoreElements()) {
                final String propertyName = (String) propEnum.nextElement();
                System.setProperty(propertyName, properties.getProperty(propertyName));
            }
            for (StaticMethodActor methodActor : callMethods) {
                try {
                    methodActor.invoke();
                } catch (Exception ex) {
                    ProgramError.unexpected("error invoking callmethod" + methodActor.qualifiedName(), ex);
                }
            }

        }
    }

    @Override
    public void run() throws Throwable {
        // The JavaTesterRunScheme no longer overrides run.
        // Instead it checks whether  to run tests in the STARTING
        // phase of the initialize method.
        if (tester == null) {
            super.run();
        } else {
            initializeBasicFeatures();
            tester.initialize(MaxineVM.Phase.STARTING);
        }
    }

    @Override
    protected void resetLauncher(ClassActor launcherClassActor) {
        if (resetLauncher) {
            final FieldActor rfa = ClassActor.fromJava(sun.misc.Launcher.class).findLocalStaticFieldActor("bootstrapClassPath");
            if (rfa != null) {
                TupleAccess.writeObject(rfa.holder().staticTuple(), rfa.offset(), null);
            }
            launcherClassActor.callInitializer();
        }
    }

    @HOSTED_ONLY
    private void extendImage() {
        final String mainClass = System.getProperty(MAINCLASS_PROPERTY_NAME);
        final String fileList = System.getProperty(SPECFILE_PROPERTY_NAME);

        if (mainClass != null) {
            forceClass(mainClass, true);
        }
        if (fileList != null) {
            processSpecFile(new File(fileList));
        }
        final String testRunPackageName = System.getProperty(TESTER_PROPERTY_NAME);
        if (testRunPackageName != null) {
            final String testerRunSchemeClassname = testRunPackageName + ".JavaTesterRunScheme";
            try {
                final Class<?> klass = Class.forName(testerRunSchemeClassname);
                final Constructor<?> cons = klass.getConstructor(new Class<?>[] {VMConfiguration.class});
                tester = (JavaRunScheme) cons.newInstance(new Object[] {vmConfiguration()});
                Trace.line(1, "extending image with " + testerRunSchemeClassname);
                forceCompileMethod(testerRunSchemeClassname + ".run");
                forceClass(testRunPackageName + ".JavaTesterTests", false);
                tester.initialize(MaxineVM.Phase.BOOTSTRAPPING);
            } catch (Exception ex) {
                ProgramError.unexpected(ex.getMessage());
            }
        }
    }

    @HOSTED_ONLY
    private void processSpecFile(File fileList) {
        final File parent = fileList.getParentFile();
        BufferedReader bs = null;
        try {
            bs = new BufferedReader(new FileReader(fileList));
            int lineNumber = 1;
            while (true) {
                final String line = bs.readLine();
                if (line == null) {
                    break;
                }
                if (line.length() == 0 || line.charAt(0) == '#') {
                    continue;
                }
                String command;
                String argument = null;
                final int ix = line.indexOf(' ');
                if (ix > 0) {
                    command = line.substring(0, ix);
                    argument = line.substring(ix + 1).trim();
                } else {
                    command = line;
                }
                if (command.equals("class")) {
                    forceClass(argument, false);
                } else if (command.equals("omitclass")) {
                    omitClass(argument);
                } else if (command.equals("classinit")) {
                    forceClassInit(argument);
                } else if (command.equals("package")) {
                    forceLoadPackage(argument);
                } else if (command.equals("forcemethod")) {
                    forceCompileMethod(argument);
                } else if (command.equals("mainclass")) {
                    forceClass(argument, true);
                } else if (command.equals("forceconstructorstubs")) {
                    forceConstructorStubs(argument);
                } else if (command.equals("forceinvocationstub")) {
                    forceInvocationStub(argument);
                } else if (command.equals("include")) {
                    doInclude(parent, argument);
                } else if (command.equals("setproperty")) {
                    doSetProperty(argument);
                } else if (command.equals("callmethod")) {
                    doCallMethod(argument);
                } else if (command.equals("reinitclass")) {
                    doReinitClass(argument);
                } else if (command.equals("resetfield")) {
                    doResetField(argument);
                } else if (command.equals("resetlauncher")) {
                    doResetLauncher();
                } else if (command.equals("imagefile")) {
                    doImageFile(argument);
                } else if (command.equals("imagefsprefix")) {
                    doImagefsPrefix(argument);
                } else {
                    throw new Exception("unknown command at line " + lineNumber + " in file " + fileList.getName());
                }
                lineNumber++;
            }
        } catch (Exception ex) {
            ProgramError.unexpected(ex.getMessage());
        } finally {
            if (bs != null) {
                try {
                    bs.close();
                } catch (IOException ex) {
                }
            }
        }
    }

    @HOSTED_ONLY
    private void doInclude(File parent, String fileName) {
        final File f = new File(parent, fileName);
        if (includeSet.contains(f)) {
            return;
        }
        includeSet.add(f);
        processSpecFile(f);
    }

    @HOSTED_ONLY
    protected void forceClass(String className, boolean isMain) {
        Trace.line(1, "extending image with class " + className);
        try {
            JavaPrototype.javaPrototype().loadClass(className);
        } catch (NoClassDefFoundError ex) {
            Trace.line(1, "WARNING: class " + className + " not found");
        }
        if (isMain) {
            mainClassName = className;
            try {
                final ClassActor mainClassActor = ClassActor.fromJava(Classes.load(HostedBootClassLoader.HOSTED_BOOT_CLASS_LOADER, className));
                final StaticMethodActor mainMethodActor = mainClassActor.findLocalStaticMethodActor("main");
                Trace.line(1, "registering " + className + ".main");
                CompiledPrototype.registerImageMethod(mainMethodActor);
                CompiledPrototype.registerImageInvocationStub(mainMethodActor);
            } catch (Exception ex) {
                ProgramError.unexpected("failed to find main method in class: " + className);
            }
        }
    }

    @HOSTED_ONLY
    protected void omitClass(String className) {
        Trace.line(1, "omitting class " + className + " from image");
        try {
            HostedBootClassLoader.omitClass(Class.forName(className));
        } catch (ClassNotFoundException classNotFoundException) {
            Trace.line(1, "WARNING: omitclass: " + className + " not found");
        }
    }

    @HOSTED_ONLY
    protected void forceClassInit(String className) {
        forceClass(className, false);
        Trace.line(1, "initializing class " + className);
        try {
            Class.forName(className, true, HostedBootClassLoader.HOSTED_BOOT_CLASS_LOADER);
        } catch (Exception ex) {
            ProgramError.unexpected("failed to find class: " + className);
        }
    }

    @HOSTED_ONLY
    protected void forceLoadPackage(String packageName) {
        Trace.line(1, "extending image with classes in package " + packageName);
        JavaPrototype.javaPrototype().loadPackage(packageName, false);
    }

    @HOSTED_ONLY
    protected void forceCompileMethod(String forceMethodName) {
        final int ix = forceMethodName.lastIndexOf('.');
        if (ix < 0) {
            ProgramError.unexpected(forceMethodName + " not correct format");
        }
        final String className = forceMethodName.substring(0, ix);
        final String methodName = forceMethodName.substring(ix + 1);
        try {
            final ClassActor classActor = ClassActor.fromJava(Classes.load(HostedBootClassLoader.HOSTED_BOOT_CLASS_LOADER, className));
            classActor.forAllClassMethodActors(new Procedure<ClassMethodActor>() {
                public void run(ClassMethodActor classMethodActor) {
                    if (methodName.equals("*") || methodName.equals(classMethodActor.name.string)) {
                        Trace.line(1, "forcing compilation of method " + classMethodActor.qualifiedName());
                        CompiledPrototype.registerImageMethod(classMethodActor);
                    }
                }
            });
        } catch (Exception ex) {
            ProgramError.unexpected("failed to find: " + className + "." + methodName);
        }
    }

    @HOSTED_ONLY
    protected void forceConstructorStubs(final String className) {
        try {
            final ClassActor classActor = ClassActor.fromJava(Classes.load(HostedBootClassLoader.HOSTED_BOOT_CLASS_LOADER, className));
            for (VirtualMethodActor virtualMethodActor : classActor.localVirtualMethodActors()) {
                if (virtualMethodActor.isInitializer()) {
                    Trace.line(1, "forcing generation of constructor stubs for class " + virtualMethodActor.qualifiedName());
                    CompiledPrototype.registerImageConstructorStub(virtualMethodActor);
                }
            }
        } catch (Exception ex) {
            ProgramError.unexpected("failed to find: " + className);
        }

    }

    @HOSTED_ONLY
    protected void forceInvocationStub(String forceMethodName) {
        final int ix = forceMethodName.lastIndexOf('.');
        if (ix < 0) {
            ProgramError.unexpected(forceMethodName + " not correct format");
        }
        final String className = forceMethodName.substring(0, ix);
        final String methodName = forceMethodName.substring(ix + 1);
        try {
            final ClassActor classActor = ClassActor.fromJava(Classes.load(HostedBootClassLoader.HOSTED_BOOT_CLASS_LOADER, className));
            classActor.forAllClassMethodActors(new Procedure<ClassMethodActor>() {
                public void run(ClassMethodActor classMethodActor) {
                    if (methodName.equals("*") || methodName.equals(classMethodActor.name.string)) {
                        Trace.line(1, "creating invocation stub for " +  classMethodActor.qualifiedName());
                        CompiledPrototype.registerImageInvocationStub(classMethodActor);
                    }
                }
            });
        } catch (Exception ex) {
            ProgramError.unexpected("failed to find: " + className);
        }

    }

    @HOSTED_ONLY
    protected void doCallMethod(String argument) {
        final int ix = argument.lastIndexOf('.');
        if (ix < 0) {
            ProgramError.unexpected(argument + " not correct format");
        }
        final String className = argument.substring(0, ix);
        final String methodName = argument.substring(ix + 1);
        try {
            final ClassActor classActor = ClassActor.fromJava(Classes.load(HostedBootClassLoader.HOSTED_BOOT_CLASS_LOADER, className));
            final StaticMethodActor methodActor = classActor.findLocalStaticMethodActor(methodName);
            Trace.line(1, "arranging to call " +  methodActor.qualifiedName() + " prior to main");
            callMethods.append(methodActor);
            forceInvocationStub(argument);
        }  catch (Exception ex) {
            ProgramError.unexpected("failed to find: " + argument);
        }
    }

    @HOSTED_ONLY
    protected void doSetProperty(String argument) {
        final int ix = argument.lastIndexOf('=');
        String propertyName;
        String propertyValue = "";
        if (ix < 0) {
            propertyName = argument;
        } else {
            propertyName = argument.substring(0, ix);
            propertyValue = argument.substring(ix + 1);
        }
        properties.put(propertyName, propertyValue);
    }

    @HOSTED_ONLY
    protected void doReinitClass(String argument) {
        doForceInitClass(argument, true);
    }

    @HOSTED_ONLY
    protected ClassActor doForceInitClass(String argument, boolean reinit) {
        final String className = argument;
        try {
            final ClassActor classActor = ClassActor.fromJava(Classes.load(HostedBootClassLoader.HOSTED_BOOT_CLASS_LOADER, className));
            Trace.line(1, "forcing compilation of " +  className + ".<clinit>");
            forceCompileMethod(argument + ".<clinit>");
            if (reinit) {
                Trace.line(1, "arranging to reinitialize " +  className + " prior to main");
                reinitClasses.append(classActor);
            }
            return classActor;
        }  catch (Exception ex) {
            ProgramError.unexpected("failed to find: " + argument);
        }
        return null;
    }

    @HOSTED_ONLY
    protected void doResetLauncher() {
        resetLauncher = true;
        Trace.line(1, "arranging to reset sun.misc.Launcher prior to main");
        forceCompileMethod("sun.misc.Launcher.<clinit>");
    }

    @HOSTED_ONLY
    protected void doResetField(String argument) {
        final int ix = argument.lastIndexOf('.');
        if (ix < 0) {
            ProgramError.unexpected(argument + " not correct format");
        }
        final String className = argument.substring(0, ix);
        final String fieldName = argument.substring(ix + 1);
        try {
            Trace.line(1, "resetting field " +  argument + " to default value");
            final Class<?> javaClass = Classes.load(HostedBootClassLoader.HOSTED_BOOT_CLASS_LOADER, className);
            JDKInterceptor.resetField(javaClass, fieldName);
            /*
            final ClassActor classActor = ClassActor.fromJava(javaClass);
            final StaticTuple staticTuple =  (StaticTuple) classActor.staticTuple();
            staticTuple.resetField(fieldName, true);
            */
        }  catch (Exception ex) {
            ProgramError.unexpected("failed to reset: " + argument, ex);
        }
    }

    @HOSTED_ONLY
    protected void doImageFile(String argument) {
        String pathName = argument;
        String asName = argument;
        final int ix = argument.lastIndexOf(' ');
        if (ix > 0) {
            pathName = argument.substring(0, ix);
            asName = argument.substring(ix + 1);
        }
        final File file = new File(pathName);
        final File asFile = new File(asName);
        if (file.exists()) {
            if (file.isDirectory()) {
                putDirectory(file, asFile);
            } else {
                putImageFile(file, asFile);
            }
        } else {
            ProgramError.unexpected("failed to find file: " + argument);
        }
    }

    @HOSTED_ONLY
    protected void putImageFile(File file, File asFile) {
        try {
            String path = asFile.getPath();
            if (asFile.isAbsolute() || imageFSPrefix.equals("")) {
                path = imageFSPrefix + path;
            } else {
                path = imageFSPrefix + File.separator + path;
            }
            imageFS.put(path, Files.toBytes(file));
            Trace.line(1, "added file " + file.getPath() + " to image file system as path " + path);
        } catch (IOException ioException) {
            ProgramError.unexpected("Error reading from " + file + ": " + ioException);
        }
    }

    @HOSTED_ONLY
    protected void putDirectory(File dir, File asFile) {
        final String[] files = dir.list();
        for (String path : files) {
            final File child = new File(dir, path);
            final File asChild = new File(asFile, path);
            if (child.isDirectory()) {
                putDirectory(child, asChild);
            } else {
                putImageFile(child, asChild);
            }
        }
    }
    @HOSTED_ONLY
    protected void doImagefsPrefix(String argument) {
        final int last = argument.length() - 1;
        if (argument.charAt(last) == File.separatorChar) {
            imageFSPrefix = argument.substring(0, last);
        } else {
            imageFSPrefix = argument;
        }
        Trace.line(1, "set imagefs prefix to " + imageFSPrefix);
    }

    public static Map<String, byte[]> getImageFS() {
        return imageFS;
    }

    public static String getImageFSPrefix() {
        return imageFSPrefix;
    }
}

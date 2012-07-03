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
package com.sun.max.vm.jdk;

import static com.sun.max.platform.Platform.*;
import static com.sun.max.vm.MaxineVM.*;
import static com.sun.max.vm.VMConfiguration.*;
import static com.sun.max.vm.VMOptions.*;
import static com.sun.max.vm.intrinsics.MaxineIntrinsicIDs.*;
import static com.sun.max.vm.VMProperty.*;

import java.io.*;
import java.nio.charset.*;
import java.util.*;

import sun.misc.*;
import sun.reflect.*;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.NativeProperty;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;

/**
 * Implements method substitutions for {@link java.lang.System java.lang.System}.
 */
@METHOD_SUBSTITUTIONS(System.class)
public final class JDK_java_lang_System {

    public static final String DARWIN_JAVA_HOME_JDK6_DEFAULT = "/System/Library/Frameworks/JavaVM.framework/Versions/1.6/Home";
    public static final String DARWIN_JAVA_HOME_JDK7_DEFAULT = "/Library/Java/JavaVirtualMachines/1.7.0.jdk/Contents/Home";

    /**
     * Register any native methods through JNI.
     */
    @SUBSTITUTE
    private static void registerNatives() {
    }

    @ALIAS(declaringClass = System.class)
    private static PrintStream out;

    @ALIAS(declaringClass = System.class)
    private static PrintStream err;

    @ALIAS(declaringClass = System.class)
    private static InputStream in;

    /**
     * Sets the input stream {@link java.lang.System#in System.in} to the specified input stream.
     *
     * @param is the new system input stream
     */
    @SUBSTITUTE
    private static void setIn0(InputStream is) {
        in = is;
    }

    /**
     * Sets the output stream {@link java.lang.System#out System.out} to the specified output stream.
     *
     * @param ps the new system output stream
     */
    @SUBSTITUTE
    private static void setOut0(PrintStream ps) {
        out = ps;
    }

    /**
     * Sets the error stream {@link java.lang.System#err System.err} to the specified error stream.
     *
     * @param ps the new system error stream
     */
    @SUBSTITUTE
    private static void setErr0(PrintStream ps) {
        err = ps;
    }

    /**
     * Return the milliseconds elapsed since some fixed, but arbitrary time in the past.
     *
     * @return the number of milliseconds elapsed since some epoch time
     */
    @SUBSTITUTE
    public static long currentTimeMillis() {
        return MaxineVM.native_currentTimeMillis();
    }

    /**
     * Returns the nanoseconds elapsed since some fixed, but arbitrary time in the past.
     *
     * @return the number of nanoseconds elapsed since some epoch time
     */
    @SUBSTITUTE
    public static long nanoTime() {
        return MaxineVM.native_nanoTime();
    }

    /**
     * Performs an array copy in the forward direction.
     *
     * @param kind the element kind
     * @param fromArray the source array
     * @param fromIndex the start index in the source array
     * @param toArray the destination array
     * @param toIndex the start index in the destination array
     * @param length the number of elements to copy
     * @param toComponentClassActor the class actor representing the component type of the destination array
     */
    private static void arrayCopyForward(final Kind kind, Object fromArray, int fromIndex, Object toArray, int toIndex, int length, ClassActor toComponentClassActor) {
        switch (kind.asEnum) {
            case BYTE: {
                for (int i = 0; i < length; i++) {
                    ArrayAccess.setByte(toArray, toIndex + i, ArrayAccess.getByte(fromArray, fromIndex + i));
                }
                break;
            }
            case BOOLEAN: {
                for (int i = 0; i < length; i++) {
                    ArrayAccess.setBoolean(toArray, toIndex + i, ArrayAccess.getBoolean(fromArray, fromIndex + i));
                }
                break;
            }
            case SHORT: {
                for (int i = 0; i < length; i++) {
                    ArrayAccess.setShort(toArray, toIndex + i, ArrayAccess.getShort(fromArray, fromIndex + i));
                }
                break;
            }
            case CHAR: {
                for (int i = 0; i < length; i++) {
                    ArrayAccess.setChar(toArray, toIndex + i, ArrayAccess.getChar(fromArray, fromIndex + i));
                }
                break;
            }
            case INT: {
                for (int i = 0; i < length; i++) {
                    ArrayAccess.setInt(toArray, toIndex + i, ArrayAccess.getInt(fromArray, fromIndex + i));
                }
                break;
            }
            case FLOAT: {
                for (int i = 0; i < length; i++) {
                    ArrayAccess.setFloat(toArray, toIndex + i, ArrayAccess.getFloat(fromArray, fromIndex + i));
                }
                break;
            }
            case LONG: {
                for (int i = 0; i < length; i++) {
                    ArrayAccess.setLong(toArray, toIndex + i, ArrayAccess.getLong(fromArray, fromIndex + i));
                }
                break;
            }
            case DOUBLE: {
                for (int i = 0; i < length; i++) {
                    ArrayAccess.setDouble(toArray, toIndex + i, ArrayAccess.getDouble(fromArray, fromIndex + i));
                }
                break;
            }
            case WORD: {
                for (int i = 0; i < length; i++) {
                    ArrayAccess.setWord(toArray, toIndex + i, ArrayAccess.getWord(fromArray, fromIndex + i));
                }
                break;
            }
            case REFERENCE: {
                for (int i = 0; i < length; i++) {
                    final Object object = ArrayAccess.getObject(fromArray, fromIndex + i);
                    if (toComponentClassActor != null && !toComponentClassActor.isNullOrInstance(object)) {
                        throw new ArrayStoreException();
                    }
                    ArrayAccess.setObject(toArray, toIndex + i, object);
                }
                break;
            }
            default: {
                throw ProgramError.unknownCase();
            }
        }
    }

    /**
     * Performs an array copy in the backward direction.
     *
     * @param kind the element kind
     * @param fromArray the source array
     * @param fromIndex the start index in the source array
     * @param toArray the destination array
     * @param toIndex the start index in the destination array
     * @param length the number of elements to copy
     */
    private static void arrayCopyBackward(final Kind kind, Object fromArray, int fromIndex, Object toArray, int toIndex, int length) {
        switch (kind.asEnum) {
            case BYTE: {
                for (int i = length - 1; i >= 0; i--) {
                    ArrayAccess.setByte(toArray, toIndex + i, ArrayAccess.getByte(fromArray, fromIndex + i));
                }
                break;
            }
            case BOOLEAN: {
                for (int i = length - 1; i >= 0; i--) {
                    ArrayAccess.setBoolean(toArray, toIndex + i, ArrayAccess.getBoolean(fromArray, fromIndex + i));
                }
                break;
            }
            case SHORT: {
                for (int i = length - 1; i >= 0; i--) {
                    ArrayAccess.setShort(toArray, toIndex + i, ArrayAccess.getShort(fromArray, fromIndex + i));
                }
                break;
            }
            case CHAR: {
                for (int i = length - 1; i >= 0; i--) {
                    ArrayAccess.setChar(toArray, toIndex + i, ArrayAccess.getChar(fromArray, fromIndex + i));
                }
                break;
            }
            case INT: {
                for (int i = length - 1; i >= 0; i--) {
                    ArrayAccess.setInt(toArray, toIndex + i, ArrayAccess.getInt(fromArray, fromIndex + i));
                }
                break;
            }
            case FLOAT: {
                for (int i = length - 1; i >= 0; i--) {
                    ArrayAccess.setFloat(toArray, toIndex + i, ArrayAccess.getFloat(fromArray, fromIndex + i));
                }
                break;
            }
            case LONG: {
                for (int i = length - 1; i >= 0; i--) {
                    ArrayAccess.setLong(toArray, toIndex + i, ArrayAccess.getLong(fromArray, fromIndex + i));
                }
                break;
            }
            case DOUBLE: {
                for (int i = length - 1; i >= 0; i--) {
                    ArrayAccess.setDouble(toArray, toIndex + i, ArrayAccess.getDouble(fromArray, fromIndex + i));
                }
                break;
            }
            case WORD: {
                for (int i = length - 1; i >= 0; i--) {
                    ArrayAccess.setWord(toArray, toIndex + i, ArrayAccess.getWord(fromArray, fromIndex + i));
                }
                break;
            }
            case REFERENCE: {
                for (int i = length - 1; i >= 0; i--) {
                    ArrayAccess.setObject(toArray, toIndex + i, ArrayAccess.getObject(fromArray, fromIndex + i));
                }
                break;
            }
            default: {
                throw ProgramError.unknownCase();
            }
        }
    }

    /**
     * Copies a portion of an array from one array to another (possibly the same) array.
     *
     * @see java.lang.System#arraycopy(Object, int, Object, int, int)
     * @param fromArray the source array
     * @param fromIndex the start index in the source array
     * @param toArray the destination array
     * @param toIndex the start index in the destination array
     * @param length the number of elements to copy
     */
    @SUBSTITUTE
    public static void arraycopy(Object fromArray, int fromIndex, Object toArray, int toIndex, int length) {
        if (fromArray == null || toArray == null) {
            throw new NullPointerException();
        }
        final Hub fromHub = ObjectAccess.readHub(fromArray);
        final ClassActor fromArrayClassActor = fromHub.classActor;
        if (!fromArrayClassActor.isArrayClass()) {
            throw new ArrayStoreException();
        }
        final Kind kind = fromArrayClassActor.componentClassActor().kind;
        if (fromArray == toArray) {
            if (fromIndex < toIndex) {
                if (fromIndex < 0 || length < 0 || toIndex + length > ArrayAccess.readArrayLength(fromArray)) {
                    throw new IndexOutOfBoundsException();
                }
                arrayCopyBackward(kind, fromArray, fromIndex, fromArray, toIndex, length);
            } else if (fromIndex != toIndex) {
                if (toIndex < 0 || length < 0 || fromIndex + length > ArrayAccess.readArrayLength(fromArray)) {
                    throw new IndexOutOfBoundsException();
                }
                arrayCopyForward(kind, fromArray, fromIndex, fromArray, toIndex, length, null);
            }
            return;
        }
        final Hub toHub = ObjectAccess.readHub(toArray);
        if (toHub == fromHub) {
            if (fromIndex < 0 || toIndex < 0 || length < 0 ||
                    fromIndex + length > ArrayAccess.readArrayLength(fromArray) ||
                    toIndex + length > ArrayAccess.readArrayLength(toArray)) {
                throw new IndexOutOfBoundsException();
            }
            arrayCopyForward(kind, fromArray, fromIndex, toArray, toIndex, length, null);
        } else {
            final ClassActor toArrayClassActor = toHub.classActor;
            if (!toArrayClassActor.isArrayClass()) {
                throw new ArrayStoreException();
            }
            final ClassActor toComponentClassActor = toArrayClassActor.componentClassActor();
            if (kind != Kind.REFERENCE || toComponentClassActor.kind != Kind.REFERENCE) {
                throw new ArrayStoreException();
            }
            if (fromIndex < 0 || toIndex < 0 || length < 0 ||
                    fromIndex + length > ArrayAccess.readArrayLength(fromArray) ||
                    toIndex + length > ArrayAccess.readArrayLength(toArray)) {
                throw new IndexOutOfBoundsException();
            }
            arrayCopyForward(kind, fromArray, fromIndex, toArray, toIndex, length, toComponentClassActor);
        }
    }

    /**
     * Gets the default (identity) hashcode for an object.
     *
     * @see java.lang.System#identityHashCode(Object)
     * @param object the object for which to get the hashcode
     * @return the identity hashcode of the specified object; {@code 0} if the object reference is null
     */
    @SUBSTITUTE
    public static int identityHashCode(Object object) {
        if (object == null) {
            return 0;
        }
        return ObjectAccess.makeHashCode(object);
    }

    /**
     * Sets a property in the specified property set, if it has not already been set.
     *
     * @param properties the properties set in which to set the property
     * @param name the name of the property
     * @param value the value to which the property will be set if it doesn't already have a value
     * @return the value,whether already set of set by this call
     */
    private static String setIfAbsent(Properties properties, String name, String value) {
        if (properties.containsKey(name)) {
            return properties.getProperty(name); // property has already been set by command line argument parsing
        }
        if (value != null) {
            properties.setProperty(name, value);
        }
        return value;
    }

    /**
     * Finds the Java home path, where the JDK libraries are found.
     *
     * @return a string representing the path of the Java home
     */
    private static String findJavaHome() {
        switch (platform().os) {
            case MAXVE:
            case SOLARIS:
            case LINUX: {
                // TODO: Assume we are in the JRE and walk around from there.

                // For now, we just rely on the JAVA_HOME environment variable being set:
                String javaHome = getenv("JAVA_HOME", false);
                FatalError.check(javaHome != null, "Environment variable JAVA_HOME not set");

                if (!javaHome.endsWith("/jre")) {
                    // A lot of JDK code expects java.home to point to the "jre" subdirectory in a JDK.
                    // This makes interpreting java.home easier if only a JRE is installed.
                    // However, quite often JAVA_HOME points to the top level directory of a JDK
                    // installation and so it needs to be adjusted to append the "jre" subdirectory.
                    javaHome = javaHome + "/jre";
                }
                return javaHome;
            }
            case WINDOWS: {
                throw FatalError.unexpected("Initialization of java.home is unimplemented");
            }
            case DARWIN: {
                // TODO: Assume we are in the JRE and walk around from there.

                // For now, we just rely on the JAVA_HOME environment variable being set:
                String javaHome = getenv("JAVA_HOME", false);
                if (JDK.JDK_VERSION == JDK.JDK_6) {
                    if (javaHome == null) {
                        javaHome = DARWIN_JAVA_HOME_JDK6_DEFAULT;
                    }
                    if (!javaHome.endsWith("/Home")) {
                        javaHome = javaHome + "/Home";
                    }
                } else if (JDK.JDK_VERSION == JDK.JDK_7) {
                    if (javaHome == null) {
                        javaHome = DARWIN_JAVA_HOME_JDK7_DEFAULT;
                    }
                    if (!javaHome.endsWith("/jre")) {
                        javaHome = javaHome + "/jre";
                    }
                } else {
                    throw FatalError.unexpected("Unsupported DARWIN JDK version");
                }
                return javaHome;
            }
        }
        throw FatalError.unexpected("should not reach here");
    }

    /**
     * Retrieves the java library path from OS-specific environment variable(s).
     *
     * @return a string representing the java library path as determined from the OS environment
     */
    private static String getenvJavaLibraryPath() {
        switch (platform().os) {
            case DARWIN:
            case LINUX:
            case SOLARIS: {
                return getenv("LD_LIBRARY_PATH", false);
            }
            case WINDOWS:
            case MAXVE:
            default: {
                return "";
            }
        }
    }

    /**
     * Retrieves the java class path from OS-specified environment variable(s). This method
     * does not consult the value of the command line arguments to the virtual machine.
     *
     * @return a string representing the Java class path as determined from the OS environment
     */
    private static String getenvClassPath() {
        switch (platform().os) {
            case DARWIN:
            case LINUX:
            case SOLARIS: {
                return getenv("CLASSPATH", false);
            }
            case WINDOWS:
            case MAXVE:
            default: {
                return "";
            }
        }
    }

    private static String getenv(String name, boolean mustBePresent) {
        final String value = System.getenv(name);
        if (mustBePresent && value == null) {
            FatalError.unexpected("Required environment variable " + name + " is undefined.");
        }
        return value;
    }

    /**
     * Retrieves the path to the executable binary which launched this virtual machine, which is
     * used to derived the java home, bootstrap classpath, etc.
     *
     * @return a path representing the location of the virtual machine executable, as determined
     * by the OS environment
     */
    private static String getenvExecutablePath() {
        final String executablePath = MaxineVM.getExecutablePath();
        if (executablePath == null) {
            FatalError.unexpected("Path to VM executable cannot be null.");
        }
        return executablePath;
    }

    /**
     * Gets the name of this VM's target instruction set, which is used to locate
     * platform-specific native libraries.
     *
     * @return the name of this VM's target ISA
     */
    private static String getISA() {
        switch (Platform.platform().isa) {
            case ARM:
                FatalError.unimplemented();
                break;
            case AMD64:
                return "amd64";
            case IA32:
                return "x86";
            case SPARC:
                return (Word.width() == 64) ? "sparcv9" : "sparc";
            case PPC:
                FatalError.unimplemented();
                break;
        }
        return null;
    }

    /**
     * Gets a list of this VM's target instruction sets.
     * @return a list of this VM's target ISAs
     */
    private static String getISAList() {
        switch (Platform.platform().isa) {
            case ARM:
                FatalError.unimplemented();
                break;
            case AMD64:
                return "amd64";
            case IA32:
                return "x86";
            case SPARC:
                return (Word.width() == 64) ? "sparcv9" : "sparc";
            case PPC:
                FatalError.unimplemented();
                break;
        }
        return null;
    }

    static String fileSeparator;

    static String pathSeparator;

    /**
     * Joins an array of strings into a single string.
     *
     * @param separator the separator string
     * @param strings an array of strings to be joined. Any element of this array that is null or has a length of zero
     *            is ignored.
     * @return the result of joining all the non-null and non-empty values in {@code strings} as a single string with
     *         {@code separator} inserted between each pair of strings
     */
    private static String join(String separator, String... strings) {
        int length = 0;
        for (String path : strings) {
            if (path != null && !path.isEmpty()) {
                length += path.length() + separator.length();
            }
        }
        final StringBuilder result = new StringBuilder(length);
        for (String path : strings) {
            if (path != null && !path.isEmpty()) {
                if (result.length() != 0) {
                    result.append(separator);
                }
                result.append(path);
            }
        }
        return result.toString();
    }

    private static String asFilesystemPath(String... directoryNames) {
        return join(fileSeparator, directoryNames);
    }

    private static String asClasspath(String... filesystemPaths) {
        return join(pathSeparator, filesystemPaths);
    }

    private static final VMStringOption classpathOption = register(new VMStringOption("-cp", true, null,
        "A " + File.pathSeparatorChar + " separated list of directories, JAR archives, and ZIP archives to search for class files.") {
        @Override
        public boolean matches(Pointer arg) {
            return CString.equals(arg, prefix) || CString.equals(arg, "-classpath");
        }
        @Override
        public void printHelp() {
            VMOptions.printHelpForOption(category(), "-cp", " <class search path of directories and zip/jar files>", null);
            VMOptions.printHelpForOption(category(), "-classpath",
                " <class search path of directories and zip/jar files>", help);
        }
    }, MaxineVM.Phase.PRISTINE);

    private static final BootClasspathVMOption bootClasspathOption = BootClasspathVMOption.create(":", "set search path for bootstrap classes and resources.");
    private static final BootClasspathVMOption aBootClasspathOption = BootClasspathVMOption.create("/a:", "append to end of bootstrap class path");
    private static final BootClasspathVMOption pBootClasspathOption = BootClasspathVMOption.create("/p:", "prepend in front of bootstrap class path");

    static class BootClasspathVMOption extends VMOption {
        private String path;
        private static final String USAGE_VALUE = "<directories and zip/jar files separated by " + File.pathSeparatorChar + ">";

        @HOSTED_ONLY
        static BootClasspathVMOption create(String suffix, String help) {
            return register(new BootClasspathVMOption(suffix, help), MaxineVM.Phase.STARTING);
        }

        @HOSTED_ONLY
        BootClasspathVMOption(String suffix, String help) {
            super("-Xbootclasspath" + suffix, help);
        }

        @Override
        public boolean parseValue(Pointer optionValue) {
            try {
                path = CString.utf8ToJava(optionValue);
                return true;
            } catch (Utf8Exception utf8Exception) {
                return false;
            }
        }

        @Override
        public void printHelp() {
            VMOptions.printHelpForOption(category(), prefix, USAGE_VALUE, help);
        }

        String path() {
            return path;
        }
    }

    // Checkstyle: stop method name check
    @ALIAS(declaringClassName = "java.lang.ProcessEnvironment", name = "<clinit>")
    private static native void ProcessEnvironment_clinit();

    @ALIAS(declaringClassName = "java.lang.ApplicationShutdownHooks", name = "<clinit>")
    private static native void ApplicationShutdownHooks_clinit();

    @ALIAS(declaringClassName = "java.lang.ApplicationShutdownHooks", name = "hooks")
    private static IdentityHashMap<Thread, Thread> ApplicationShutdownHooks_hooks;

    @ALIAS(declaringClass = File.class, name = "<clinit>")
    private static native void File_clinit();

    @ALIAS(declaringClass = Perf.class, name = "<clinit>")
    private static native void Perf_clinit();

    @ALIAS(declaringClass = Launcher.class, name = "<clinit>")
    private static native void Launcher_clinit();

    // Checkstyle: resume method name check

    /**
     * Initializes system properties from a wide variety of sources.
     */
    @SUBSTITUTE
    private static Properties initProperties(Properties properties) {
        // 1. parse any properties from command line
        VMOptions.addParsedSystemProperties(properties);

        // 2. set up basic Maxine configuration information

        JAVA_VM_INFO.updateImmutableValue(vm().compilationBroker.mode());
        // Copy the VM properties that have values already
        for (VMProperty vmProperty : VMProperty.VALUES) {
            String value = vmProperty.value();
            if (value != null) {
                // value is either set at boot image time, or was set by a VMTI agent or was specified on the command line
                properties.setProperty(vmProperty.property, value);
            }
        }

        setIfAbsent(properties, "sun.arch.data.model", Integer.toString(Word.width()));
        setIfAbsent(properties, "sun.cpu.endian", Word.endianness().name().toLowerCase());

        String isa = properties.getProperty("os.arch");
        if (isa == null) {
            isa = getISA();
            setIfAbsent(properties, "os.arch", isa);
        }

        String isaList = properties.getProperty("sun.cpu.isalist");
        if (isaList == null) {
            isaList = getISAList(); // TODO: reconcile with code below
            setIfAbsent(properties, "sun.cpu.isalist", isaList);
        }

        // 3. reinitialize java.lang.ProcessEnvironment with this process's environment
        ProcessEnvironment_clinit();

        // 3.1. reinitialize java.lang.ApplicationShutdownHooks
        assert ApplicationShutdownHooks_hooks.isEmpty() : "One or more shutdown hooks were registered too early";
        ApplicationShutdownHooks_clinit();

        // 4. perform OS-specific initialization
        switch (Platform.platform().os) {
            case DARWIN:
                setIfAbsent(properties, "os.name", "Mac OS X");
                initBasicUnixProperties(properties);
                break;
            case MAXVE:
                setIfAbsent(properties, "os.name", "Maxine VE");
                setIfAbsent(properties, "java.io.tmpdir", "/tmp");
                initBasicUnixProperties(properties);
                break;
            case LINUX:
                setIfAbsent(properties, "os.name", "Linux");
                initBasicUnixProperties(properties);
                break;
            case SOLARIS:
                setIfAbsent(properties, "os.name", "SunOS");
                initBasicUnixProperties(properties);
                break;
            default:
                throw ProgramError.unknownCase();
        }

        // 5. set up user-specific information
        final Pointer nativeJavaProperties = MaxineVM.native_properties();
        final String userDir = NativeProperty.USER_DIR.value(nativeJavaProperties);
        if (userDir == null) {
            throw FatalError.unexpected("Could not determine current working directory.");
        }

        setIfAbsent(properties, "user.name", NativeProperty.USER_NAME.value(nativeJavaProperties));
        setIfAbsent(properties, "user.home", NativeProperty.USER_HOME.value(nativeJavaProperties));
        setIfAbsent(properties, "user.dir", userDir);
        setIfAbsent(properties, "java.java2d.fontpath", getenv("JAVA2D_FONTPATH", false));

        // 6. set up the java home
        String javaHome = properties.getProperty(JAVA_HOME.property);
        if (javaHome == null) {
            javaHome = findJavaHome();
            setIfAbsent(properties, JAVA_HOME.property, javaHome);
        }
        JAVA_HOME.setValue(javaHome);

        // 7. set up classpath and library path
        final String[] javaAndZipLibraryPaths = new String[2];
        if (Platform.platform().os == OS.DARWIN) {
            if (JDK.JDK_VERSION == JDK.JDK_6) {
                initDarwinJDK6PathProperties(properties, javaHome, javaAndZipLibraryPaths);
            } else {
                initUnixPathProperties(properties, javaHome, isa, javaAndZipLibraryPaths);
            }
        } else if (Platform.platform().os == OS.WINDOWS) {
            initWindowsPathProperties(properties, javaHome, javaAndZipLibraryPaths);
        } else {
            initUnixPathProperties(properties, javaHome, isa, javaAndZipLibraryPaths);
        }
        JAVA_LIBRARY_PATH.setValue(setIfAbsent(properties, JAVA_LIBRARY_PATH.property, getenvJavaLibraryPath()));

        // 8. set up the class path
        // N.B. -jar overrides any other classpath setting
        String javaClassPath = null;
        if (VMOptions.jarFile() == null) {
            // classpath search order (copying the semantic from the java command):
            // (1) the -cp command line option
            javaClassPath = classpathOption.getValue();
            if (javaClassPath == null) {
                // (2) the property java.class.path
                javaClassPath = properties.getProperty("java.class.path");
                if (javaClassPath == null) {
                    // (3) the environment variable CLASSPATH
                    javaClassPath = getenvClassPath();
                    if (javaClassPath == null) {
                        // (4) the current working directory only
                        javaClassPath = ".";
                    }
                }
            }
        } else {
            javaClassPath = VMOptions.jarFile();
        }
        properties.setProperty(JAVA_CLASS_PATH.property, javaClassPath);
        JAVA_CLASS_PATH.setValue(javaClassPath);

        // 9. load the native code for zip and java libraries
        BootClassLoader.BOOT_CLASS_LOADER.loadJavaAndZipNativeLibraries(javaAndZipLibraryPaths[0], javaAndZipLibraryPaths[1]);

        // 10. initialize the file system with current runtime values as opposed to bootstrapping values
        File_clinit();

        // 11. initialize the management performance class with current runtime values
        Perf_clinit();

        // 12. load the character encoding class
        final String sunJnuEncodingValue = properties.getProperty("sun.jnu.encoding");
        properties.remove("sun.jnu.encoding"); // Avoids endless recursion in the next statement
        Charset.isSupported(sunJnuEncodingValue); // We are only interested in the side effect: loading the char set if supported and initializing related JNU variables
        setIfAbsent(properties, "sun.jnu.encoding", sunJnuEncodingValue); // Now that we have loaded the char set, the recursion is broken and we can move on

        if (verboseOption.verboseProperties) {
            Log.println("Initial system properties:");
            final Map<String, String> sortedProperties = new TreeMap<String, String>();
            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                sortedProperties.put((String) entry.getKey(), (String) entry.getValue());
            }
            for (Map.Entry<String, String> entry : sortedProperties.entrySet()) {
                Log.print(entry.getKey());
                Log.print('=');
                Log.println(entry.getValue());
            }
        }

        // 13. reinitialize the Java launcher so that it reflects the actual application classpath
        Launcher_clinit();

        return properties;
    }

    /**
     * Initializes the sun.boot.library.path, sun.boot.path, java.endorsed.dirs and java.ext.dirs system properties when running on Unix.
     *
     * @param properties the system properties
     * @param javaHome the value of the java.home system property
     * @param javaAndZipLibraryPaths an array of size 2 in which the path to the libjava.jnilib will be returned in
     *            element 0 and the path to libzip.jnilib will be returned in element 1
     */
    @PLATFORM(os = "!windows")
    private static void initUnixPathProperties(Properties properties, String javaHome, String isa, String[] javaAndZipLibraryPaths) {
        FatalError.check(javaHome.endsWith("/jre"), "The java.home system property should end with \"/jre\"");
        final OS os = platform().os;
        final String jrePath = javaHome;
        final String jreLibPath = asFilesystemPath(jrePath, "lib");
        final String jreLibIsaPath = os == OS.DARWIN ? jreLibPath : asFilesystemPath(jreLibPath, isa);

        checkSetBootLibraryPath(properties, asClasspath(getenvExecutablePath(), jreLibIsaPath));

        String bootClassPath = null;
        if (bootClasspathOption.isPresent()) {
            bootClassPath = bootClasspathOption.path();
        } else {
            String bootClassPathA = join(pathSeparator,
                            asFilesystemPath(jreLibPath, "resources.jar"),
                            asFilesystemPath(jreLibPath, "rt.jar"),
                            asFilesystemPath(jreLibPath, "sunrsasign.jar"),
                            asFilesystemPath(jreLibPath, "jsse.jar"),
                            asFilesystemPath(jreLibPath, "jce.jar"),
                            asFilesystemPath(jreLibPath, "charsets.jar"));
            if (JDK.JDK_VERSION == JDK.JDK_7) {
                bootClassPathA = join(pathSeparator, bootClassPathA, asFilesystemPath(jreLibPath, "jfr.jar"));
                if (os == OS.DARWIN) {
                    bootClassPathA = join(pathSeparator, bootClassPathA, asFilesystemPath(jreLibPath, "JObjC.jar"));
                }
            }
            bootClassPath = join(pathSeparator, bootClassPathA, asFilesystemPath(jrePath, "classes"));
        }
        SUN_BOOT_CLASS_PATH.setValue(setIfAbsent(properties, SUN_BOOT_CLASS_PATH.property, checkAugmentBootClasspath(bootClassPath)));

        javaAndZipLibraryPaths[0] = jreLibIsaPath;
        javaAndZipLibraryPaths[1] = jreLibIsaPath;

        String javaEndorsedDirs = asFilesystemPath(javaHome, "lib/endorsed");
        JAVA_ENDORSED_DIRS.setValue(setIfAbsent(properties, JAVA_ENDORSED_DIRS.property, javaEndorsedDirs));

        String javaExtDirs = null;
        String extPath = asFilesystemPath(javaHome, "lib/ext");
        if (os == OS.LINUX) {
            javaExtDirs = asClasspath(extPath, "/usr/java/packages/lib/ext");
        } else if (os == OS.SOLARIS) {
            javaExtDirs = asClasspath(extPath, "/usr/jdk/packages/lib/ext");
        } else if (os == OS.DARWIN) {
            // laundry list (cf Hotspot)!
            final String userHome = properties.getProperty("user.home");
            if (userHome != null) {
                javaExtDirs = new File(userHome, "Library/Java/Extensions").getAbsolutePath();
            }
            javaExtDirs = join(pathSeparator, javaExtDirs, asClasspath(extPath), "/Library/Java/Extensions", "/Network/Library/Java/Extensions",
                            "/System/Library/Java/Extensions", "/usr/lib/java");
        } else if (os == OS.MAXVE) {
            javaExtDirs = asClasspath(extPath);
        } else {
            throw ProgramError.unknownCase(os.toString());
        }
        JAVA_EXT_DIRS.setValue(setIfAbsent(properties, JAVA_EXT_DIRS.property, javaExtDirs));
    }

    static String checkAugmentBootClasspath(final String xBootClassPath) {
        String bootClassPath = xBootClassPath;
        if (aBootClasspathOption.isPresent()) {
            bootClassPath = join(pathSeparator, bootClassPath, aBootClasspathOption.path());
        }
        if (pBootClasspathOption.isPresent()) {
            bootClassPath = join(pathSeparator, pBootClasspathOption.path(), bootClassPath);
        }
        return bootClassPath;
    }

    /**
     * Unlike similar properties, e.g. java.ext.dirs, Hotspot appends any command line definition of sun.boot.library.path
     * to the default value, instead of replacing it, so we copy that behavior.
     * @param properties
     * @param librariesPath
     */
    static void checkSetBootLibraryPath(Properties properties, String librariesPath) {
        String value = properties.getProperty(SUN_BOOT_LIBRARY_PATH.property);
        if (value != null) {
            value = librariesPath + ":" + value;
        } else {
            value = librariesPath;
        }
        properties.setProperty(SUN_BOOT_LIBRARY_PATH.property, value);
        SUN_BOOT_LIBRARY_PATH.setValue(value);
    }

    /**
     * Initializes the sun.boot.library.path and sun.boot.path system properties when running on Windows.
     *
     * @param properties the system properties
     * @param javaHome the value of the java.home system property
     * @param javaAndZipLibraryPaths an array of size 2 in which the path to the java.dll will be returned in
     *            element 0 and the path to zip.dll will be returned in element 1
     */
    @PLATFORM(os = "windows")
    private static void initWindowsPathProperties(Properties properties, String javaHome, String[] javaAndZipLibraryPaths) {
        throw FatalError.unexpected("Initialization of paths on Windows is unimplemented");
    }

    /**
     * Initializes the sun.boot.library.path, sun.boot.path, java.endorsed.dirs and java.ext.dirs system properties when running on Darwin with JDK6.
     *
     * @param properties the system properties
     * @param javaHome the value of the java.home system property
     * @param javaAndZipLibraryPaths an array of size 2 in which the path to the {@code libjava.jnilib} will be returned in
     *            element 0 and the path to {@code libzip.jnilib} will be returned in element 1
     */
    @PLATFORM(os = "darwin")
    private static void initDarwinJDK6PathProperties(Properties properties, String javaHome, String[] javaAndZipLibraryPaths) {
        FatalError.check(javaHome.endsWith("/Home"), "The java.home system property should end with \"/Home\"");
        final String javaPath = Strings.chopSuffix(javaHome, "/Home");

        final String librariesPath = javaPath + "/Libraries";
        checkSetBootLibraryPath(properties, asClasspath(getenvExecutablePath(), librariesPath));

        final String classesPath = javaPath + "/Classes";
        String bootClassPath = null;
        if (bootClasspathOption.isPresent()) {
            bootClassPath = bootClasspathOption.path();
        } else {
            bootClassPath = join(pathSeparator,
                        asFilesystemPath(classesPath, "classes.jar"),
                        asFilesystemPath(classesPath, "ui.jar"),
                        asFilesystemPath(classesPath, "laf.jar"),
                        asFilesystemPath(classesPath, "sunrsasign.jar"),
                        asFilesystemPath(classesPath, "jsse.jar"),
                        asFilesystemPath(classesPath, "jce.jar"),
                        asFilesystemPath(classesPath, "charsets.jar"));
        }
        SUN_BOOT_CLASS_PATH.setValue(setIfAbsent(properties, SUN_BOOT_CLASS_PATH.property, checkAugmentBootClasspath(bootClassPath)));
        javaAndZipLibraryPaths[0] = librariesPath;
        javaAndZipLibraryPaths[1] = librariesPath;

        String extDirs = "/Library/Java/Extensions:/System/Library/Java/Extensions:" + javaHome + "/lib/ext";
        String endorsedDirs = javaHome + "/lib/endorsed";
        final String userHome = properties.getProperty("user.home");
        if (userHome != null) {
            final File userExtDir = new File(userHome, "Library/Java/Extensions/");
            extDirs += ":" + userExtDir;
        }
        JAVA_EXT_DIRS.setValue(setIfAbsent(properties, JAVA_EXT_DIRS.property, extDirs));
        JAVA_ENDORSED_DIRS.setValue(setIfAbsent(properties, JAVA_ENDORSED_DIRS.property, endorsedDirs));
    }

    @PLATFORM(os = "!windows")
    /**
     * Set basic Unix properties.
     * Most properties are inherited from the values set when the boot image was built.
     * For most uses of Maxine this is acceptable but, to be correct, the environment sensitive
     * properties, e.g. {@code user.country} should not be baked into the image, but reset
     * appropriately.
     */
    private static void initBasicUnixProperties(Properties properties) {
        setIfAbsent(properties, "java.io.tmpdir", "/var/tmp"); // this is questionable, and inconsistent with Hotspot on some platforms

        if (getenv("GNOME_DESKTOP_SESSION_ID", false) != null) {
            setIfAbsent(properties, "sun.desktop", "gnome");
        }

        fileSeparator = properties.getProperty("file.separator");
        pathSeparator = properties.getProperty("path.separator");
        FatalError.check(fileSeparator != null && pathSeparator != null, "file.separator or path.separator property is null");
    }

    /**
     * Converts a simple library name to a platform-specific name with either or both
     * a prefix and a suffix.
     *
     * @param libraryName the name of the library
     * @return a string representing the traditional name of a native library on this platform
     */
    @SUBSTITUTE
    public static String mapLibraryName(String libraryName) {
        if (libraryName == null) {
            throw new NullPointerException();
        }
        switch (platform().os) {
            case DARWIN:
                // System.loadLibrary() first wants to look for a library with the extension ".jnilib",
                // then if the library was not found, try again with extension ".dylib".
                // We support this by returning its first choice here:
                // NOTE: The above seems to no longer be true for the JDK7 port to Mac OS X, which
                // does away with .jnilib files. For JDK 7, we return .dylib.
                if (JDK.JDK_VERSION == JDK.JDK_6) {
                    return "lib" + libraryName + ".jnilib";
                } else {
                    return "lib" + libraryName + ".dylib";
                }
            case LINUX:
            case MAXVE:
            case SOLARIS:
                return "lib" + libraryName + ".so";
            case WINDOWS:
                return libraryName + ".dll";
            default:
                throw ProgramError.unknownCase();
        }
    }

    @ALIAS(declaringClass = Runtime.class)
    private native void loadLibrary0(Class fromClass, String libname);

    @INTRINSIC(UNSAFE_CAST)
    private static native JDK_java_lang_System asThis(Runtime runtime);

    /**
     * Loads a native library, searching the library paths as necessary.
     *
     * @param name the name of the library to load
     * @see BootClassLoader#loadJavaAndZipNativeLibraries(String, String)
     */
    @SUBSTITUTE
    @NEVER_INLINE
    public static void loadLibrary(String name) throws Throwable {
        if (name.equals("zip")) {
            // Do nothing, since we already loaded this library ahead of time
            // to avoid bootstrap issues with class loading (and thus dynamic constant pool resolution).
        } else {
            // ATTENTION: these statements must have the exact same side effects as the original code of the substitutee:
            final Class callerClass = Reflection.getCallerClass(2);

            Runtime runtime = Runtime.getRuntime();
            asThis(runtime).loadLibrary0(callerClass, name);
        }

        // This has been added to re-initialize those classes in the boot image that had to wait for this library to appear:
        vmConfig().runScheme().runNativeInitializationMethods();
    }
}

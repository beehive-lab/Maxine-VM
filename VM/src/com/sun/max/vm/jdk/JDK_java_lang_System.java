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
package com.sun.max.vm.jdk;

import static com.sun.max.vm.VMOptions.*;

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;

import sun.reflect.*;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Implements method substitutions for {@link java.lang.System java.lang.System}.
 *
 * @author Bernd Mathiske
 * @author Ben L. Titzer
 */
@METHOD_SUBSTITUTIONS(System.class)
public final class JDK_java_lang_System {

    /**
     * Register any native methods through JNI.
     */
    @SUBSTITUTE
    private static void registerNatives() {
    }

    /**
     * Sets the input stream {@link java.lang.System#in System.in} to the specified input stream.
     *
     * @param in the new system input stream
     */
    @SUBSTITUTE
    private static void setIn0(InputStream in) {
        ReferenceFieldActor.findStatic(System.class, "in").writeStatic(in);
    }

    /**
     * Sets the output stream {@link java.lang.System#out System.out} to the specified output stream.
     *
     * @param out the new system output stream
     */
    @SUBSTITUTE
    private static void setOut0(PrintStream out) {
        ReferenceFieldActor.findStatic(System.class, "out").writeStatic(out);
    }

    /**
     * Sets the error stream {@link java.lang.System#err System.err} to the specified error stream.
     *
     * @param out the new system error stream
     */
    @SUBSTITUTE
    private static void setErr0(PrintStream err) {
        ReferenceFieldActor.findStatic(System.class, "err").writeStatic(err);
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
                ProgramError.unknownCase();
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
                ProgramError.unknownCase();
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
        final ClassActor fromArrayClassActor = fromHub.classActor();
        if (!fromArrayClassActor.isArrayClassActor()) {
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
            final ClassActor toArrayClassActor = toHub.classActor();
            if (!toArrayClassActor.isArrayClassActor()) {
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
     */
    private static void setIfAbsent(Properties properties, String name, String value) {
        if (properties.containsKey(name)) {
            return; // property has already been set by command line argument parsing
        }
        if (value != null) {
            properties.setProperty(name, value);
        }
    }

    /**
     * Finds the Java home path, where the JDK libraries are found.
     *
     * @return a string representing the path of the Java home
     */
    private static String findJavaHome() {
        switch (VMConfiguration.hostOrTarget().platform().operatingSystem()) {
            case GUESTVM:
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
                if (javaHome == null) {
                    javaHome = "/System/Library/Frameworks/JavaVM.framework/Versions/1.6/Home";
                }

                if (!javaHome.endsWith("/Home")) {
                    javaHome = javaHome + "/Home";
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
        switch (VMConfiguration.hostOrTarget().platform().operatingSystem()) {
            case DARWIN:
            case LINUX:
            case SOLARIS: {
                return getenv("LD_LIBRARY_PATH", false);
            }
            case WINDOWS:
            case GUESTVM:
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
        switch (VMConfiguration.hostOrTarget().platform().operatingSystem()) {
            case DARWIN:
            case LINUX:
            case SOLARIS: {
                return getenv("CLASSPATH", false);
            }
            case WINDOWS:
            case GUESTVM:
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
        switch (Platform.hostOrTarget().processorKind().instructionSet()) {
            case ARM:
                FatalError.unimplemented();
                break;
            case AMD64:
                return "amd64";
            case IA32:
                return "x86";
            case SPARC:
                return (Word.width() == WordWidth.BITS_64) ? "sparcv9" : "sparc";
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
        switch (Platform.hostOrTarget().processorKind().instructionSet()) {
            case ARM:
                FatalError.unimplemented();
                break;
            case AMD64:
                return "amd64";
            case IA32:
                return "x86";
            case SPARC:
                return (Word.width() == WordWidth.BITS_64) ? "sparcv9" : "sparc";
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

    // TODO: report the correct path separator from the target here
    private static final String CLASSPATH_HELP_MESSAGE = "A list of paths to search for Java classes, separated by the : character.";

    @CONSTANT_WHEN_NOT_ZERO
    private static VMStringOption classpathOption = register(new VMStringOption("-classpath", true, null, CLASSPATH_HELP_MESSAGE), MaxineVM.Phase.PRISTINE);

    @CONSTANT_WHEN_NOT_ZERO
    private static VMStringOption cpOption = register(new VMStringOption("-cp", true, null, CLASSPATH_HELP_MESSAGE) {
        @Override
        public boolean parseValue(Pointer optionValue) {
            return classpathOption.parseValue(optionValue);
        }
        @Override
        public boolean isPresent() {
            return classpathOption.isPresent();
        }
        @Override
        public String getValue() {
            return classpathOption.getValue();
        }
    }, MaxineVM.Phase.PRISTINE);

    @CONSTANT_WHEN_NOT_ZERO
    private static BootClasspathVMOption bootClasspathOption = BootClasspathVMOption.create(":", "set search path for bootstrap classes and resources.");

    @CONSTANT_WHEN_NOT_ZERO
    private static BootClasspathVMOption aBootClasspathOption = BootClasspathVMOption.create("/a:", "append to end of bootstrap class path");

    @CONSTANT_WHEN_NOT_ZERO
    private static BootClasspathVMOption pBootClasspathOption = BootClasspathVMOption.create("/p:", "prepend in front of bootstrap class path");


    static class BootClasspathVMOption extends VMOption {
        private String path;

        @PROTOTYPE_ONLY
        static BootClasspathVMOption create(String suffix, String help) {
            return register(new BootClasspathVMOption(suffix, help), MaxineVM.Phase.STARTING);
        }

        @PROTOTYPE_ONLY
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

        String path() {
            return path;
        }
    }

    /**
     * Determines if information should be displayed about the {@linkplain System#getProperties() system properties} when
     * they initialized during VM startup.
     */
    private static final VMOption verbosePropertiesOption = register(new VMOption("-verbose:props", "Report the initial values of the system properties."), MaxineVM.Phase.PRISTINE);

    /**
     * Initializes system properties from a wide variety of sources.
     */
    @SUBSTITUTE
    private static Properties initProperties(Properties properties) {
        // 1. parse any properties from command line
        VMOptions.addParsedSystemProperties(properties);

        // 2. set up basic Maxine configuration information
        setIfAbsent(properties, "java.runtime.name", MaxineVM.name());
        setIfAbsent(properties, "java.runtime.version", MaxineVM.version());

        setIfAbsent(properties, "java.vm.name", MaxineVM.name());
        setIfAbsent(properties, "java.vm.version", MaxineVM.version());
        setIfAbsent(properties, "java.vm.info", VMConfiguration.hostOrTarget().compilationScheme().mode().name().toLowerCase() + " mode");

        setIfAbsent(properties, "sun.arch.data.model", Integer.toString(Word.width().numberOfBits));
        setIfAbsent(properties, "sun.cpu.endian", Word.endianness().name().toLowerCase());

        switch (Platform.hostOrTarget().processorKind().dataModel().endianness()) {
            case LITTLE:
                setIfAbsent(properties, "sun.io.unicode.encoding", "UnicodeLittle");
                break;
            case BIG:
                setIfAbsent(properties, "sun.io.unicode.encoding", "UnicodeBig");
                break;
        }

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
        ClassActor.fromJava(Classes.forName("java.lang.ProcessEnvironment")).callInitializer();

        // 4. perform OS-specific initialization
        switch (Platform.hostOrTarget().operatingSystem()) {
            case DARWIN:
                setIfAbsent(properties, "os.name", "Mac OS X");
                initBasicUnixProperties(properties);
                break;
            case GUESTVM:
                setIfAbsent(properties, "os.name", "GuestVM");
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
            case WINDOWS:
                setIfAbsent(properties, "os.name", "Windows");
                initBasicWindowsProperties(properties);
                break;
            default:
                ProgramError.unknownCase();
                break;
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
        String javaHome = properties.getProperty("java.home");
        if (javaHome == null) {
            javaHome = findJavaHome();
            setIfAbsent(properties, "java.home", javaHome);
        }

        // 7. set up classpath and library path
        final String[] javaAndZipLibraryPaths = new String[2];
        if (Platform.hostOrTarget().operatingSystem() == OperatingSystem.DARWIN) {
            initDarwinPathProperties(properties, javaHome, javaAndZipLibraryPaths);
        } else if (Platform.hostOrTarget().operatingSystem() == OperatingSystem.WINDOWS) {
            initWindowsPathProperties(properties, javaHome, javaAndZipLibraryPaths);
        } else {
            initUnixPathProperties(properties, javaHome, isa, javaAndZipLibraryPaths);
        }
        setIfAbsent(properties, "java.library.path", getenvJavaLibraryPath());

        // 8. set up the class path
        // N.B. -jar overrides any other classpath setting
        if (VMOptions.jarFile() == null) {
            String javaClassPath = classpathOption.getValue();
            if (javaClassPath == null) {
                javaClassPath = getenvClassPath();
            }
            setIfAbsent(properties, "java.class.path", javaClassPath);
        } else {
            properties.put("java.class.path", VMOptions.jarFile());
        }

        // 9. load the native code for zip and java libraries
        VmClassLoader.VM_CLASS_LOADER.loadJavaAndZipNativeLibraries(javaAndZipLibraryPaths[0], javaAndZipLibraryPaths[1]);

        // 10. initialize the file system with current runtime values as opposed to prototyping time values
        ClassActor.fromJava(File.class).callInitializer();

        // 11. load the character encoding class
        final String sunJnuEncodingValue = properties.getProperty("sun.jnu.encoding");
        properties.remove("sun.jnu.encoding"); // Avoids endless recursion in the next statement
        Charset.isSupported(sunJnuEncodingValue); // We are only interested in the side effect: loading the char set if supported and initializing related JNU variables
        setIfAbsent(properties, "sun.jnu.encoding", sunJnuEncodingValue); // Now that we have loaded the char set, the recursion is broken and we can move on

        if (verbosePropertiesOption.isPresent()) {
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

        return properties;
    }

    /**
     * Initializes the sun.boot.library.path, sun.boot.path and java.ext.dirs system properties when running on Unix.
     *
     * @param properties the system properties
     * @param javaHome the value of the java.home system property
     * @param javaAndZipLibraryPaths an array of size 2 in which the path to the libjava.jnilib will be returned in
     *            element 0 and the path to libzip.jnilib will be returned in element 1
     */
    private static void initUnixPathProperties(Properties properties, String javaHome, String isa, String[] javaAndZipLibraryPaths) {
        FatalError.check(javaHome.endsWith("/jre"), "The java.home system property should end with \"/jre\"");
        final String jrePath = javaHome;
        final String jreLibPath = asFilesystemPath(jrePath, "lib");
        final String jreLibIsaPath = asFilesystemPath(jreLibPath, isa);

        setIfAbsent(properties, "sun.boot.library.path", asClasspath(getenvExecutablePath(), jreLibIsaPath));

        String bootClassPath = null;
        if (bootClasspathOption.isPresent()) {
            bootClassPath = bootClasspathOption.path();
        } else {
            bootClassPath = join(pathSeparator,
                            asFilesystemPath(jreLibPath, "resources.jar"),
                            asFilesystemPath(jreLibPath, "rt.jar"),
                            asFilesystemPath(jreLibPath, "sunrsasign.jar"),
                            asFilesystemPath(jreLibPath, "jsse.jar"),
                            asFilesystemPath(jreLibPath, "jce.jar"),
                            asFilesystemPath(jreLibPath, "charsets.jar"),
                            asFilesystemPath(jrePath, "classes"));
        }
        setIfAbsent(properties, "sun.boot.class.path", checkAugmentBootClasspath(bootClassPath));

        javaAndZipLibraryPaths[0] = jreLibIsaPath;
        javaAndZipLibraryPaths[1] = jreLibIsaPath;

        final OperatingSystem os = Platform.hostOrTarget().operatingSystem();
        if (os == OperatingSystem.LINUX) {
            setIfAbsent(properties, "java.ext.dirs", asClasspath(asFilesystemPath(javaHome, "lib/ext"), "/usr/java/packages/lib/ext"));
        } else if (os == OperatingSystem.SOLARIS) {
            setIfAbsent(properties, "java.ext.dirs", asClasspath(asFilesystemPath(javaHome, "lib/ext"), "/usr/jdk/packages/lib/ext"));
        } else if (os == OperatingSystem.GUESTVM) {
            setIfAbsent(properties, "java.ext.dirs", asClasspath(asFilesystemPath(javaHome, "lib/ext"), "/usr/java/packages/lib/ext"));
        } else {
            ProgramError.unknownCase(os.toString());
        }
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
     * Initializes the sun.boot.library.path and sun.boot.path system properties when running on Windows.
     *
     * @param properties the system properties
     * @param javaHome the value of the java.home system property
     * @param javaAndZipLibraryPaths an array of size 2 in which the path to the java.dll will be returned in
     *            element 0 and the path to zip.dll will be returned in element 1
     */
    private static void initWindowsPathProperties(Properties properties, String javaHome, String[] javaAndZipLibraryPaths) {
        throw FatalError.unexpected("Initialization of paths on Windows is unimplemented");
    }

    /**
     * Initializes the sun.boot.library.path, sun.boot.path and java.ext.dirs system properties when running on Darwin.
     *
     * @param properties the system properties
     * @param javaHome the value of the java.home system property
     * @param javaAndZipLibraryPaths an array of size 2 in which the path to the {@code libjava.jnilib} will be returned in
     *            element 0 and the path to {@code libzip.jnilib} will be returned in element 1
     */
    private static void initDarwinPathProperties(Properties properties, String javaHome, String[] javaAndZipLibraryPaths) {
        FatalError.check(javaHome.endsWith("/Home"), "The java.home system property should end with \"/Home\"");
        final String javaPath = Strings.chopSuffix(javaHome, "/Home");

        final String librariesPath = javaPath + "/Libraries";
        setIfAbsent(properties, "sun.boot.library.path", asClasspath(getenvExecutablePath(), librariesPath));

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
        setIfAbsent(properties, "sun.boot.class.path", checkAugmentBootClasspath(bootClassPath));
        javaAndZipLibraryPaths[0] = getenvExecutablePath();
        javaAndZipLibraryPaths[1] = librariesPath;

        String extDirs = "/Library/Java/Extensions:/System/Library/Java/Extensions:" + javaHome + "/lib/ext";
        final String userHome = properties.getProperty("user.home");
        if (userHome != null) {
            final File userExtDir = new File(userHome, "Library/Java/Extensions/");
            extDirs += ":" + userExtDir;
        }
        setIfAbsent(properties, "java.ext.dirs", extDirs);
    }

    private static void initBasicWindowsProperties(Properties properties) {
        setIfAbsent(properties, "awt.toolkit", "sun.awt.windows.WToolkit");
        setIfAbsent(properties, "java.io.tmpdir", ""); // TODO
        setIfAbsent(properties, "java.awt.printerjob", "sun.awt.windows.WPrinterJob");
        setIfAbsent(properties, "java.awt.graphicsenv", "sun.awt.Win32GraphicsEnvironment");
        setIfAbsent(properties, "os.name", "windows"); // TODO
        setIfAbsent(properties, "os.version", ""); // TODO
        setIfAbsent(properties, "sun.os.patch.level", ""); // TODO
        setIfAbsent(properties, "sun.desktop", "windows");
        setIfAbsent(properties, "user.language", "en"); // TODO
        setIfAbsent(properties, "user.country", "US"); // TODO
        setIfAbsent(properties, "user.variant", ""); // TODO
        setIfAbsent(properties, "file.encoding", "Cp1253");
        fileSeparator = "\\";
        pathSeparator = ";";
        setIfAbsent(properties, "line.separator", "\r\n");
        setIfAbsent(properties, "file.separator", fileSeparator);
        setIfAbsent(properties, "path.separator", pathSeparator);
    }

    private static void initBasicUnixProperties(Properties properties) {
        setIfAbsent(properties, "java.io.tmpdir", "/var/tmp");
        setIfAbsent(properties, "java.awt.printerjob", "sun.print.PSPrinterJob");
        setIfAbsent(properties, "os.version", ""); // TODO
        setIfAbsent(properties, "sun.os.patch.level", "unknown");
        setIfAbsent(properties, "java.awt.graphicsenv", "sun.awt.X11GraphicsEnvironment");
        setIfAbsent(properties, "file.encoding", "UTF-8");
        setIfAbsent(properties, "sun.jnu.encoding", "UTF-8");

        if (getenv("GNOME_DESKTOP_SESSION_ID", false) != null) {
            setIfAbsent(properties, "sun.desktop", "gnome");
        }
        setIfAbsent(properties, "user.language", "en"); // TODO
        setIfAbsent(properties, "user.country", "US"); // TODO
        setIfAbsent(properties, "user.variant", ""); // TODO

        fileSeparator = "/";
        pathSeparator = ":";
        setIfAbsent(properties, "line.separator", "\n");
        setIfAbsent(properties, "file.separator", fileSeparator);
        setIfAbsent(properties, "path.separator", pathSeparator);
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
        switch (VMConfiguration.hostOrTarget().platform().operatingSystem()) {
            case DARWIN:
                // System.loadLibrary() first wants to look for a library with the extension ".jnilib",
                // then if the library was not found, try again with extension ".dylib".
                // We support this by returning its first choice here:
                return "lib" + libraryName + ".jnilib";
            case LINUX:
            case GUESTVM:
            case SOLARIS:
                return "lib" + libraryName + ".so";
            case WINDOWS:
                return libraryName + ".dll";
            default:
                ProgramError.unknownCase();
                return null;
        }
    }

    /**
     * Loads a native library, searching the library paths as necessary.
     *
     * @param name the name of the library to load
     */
    @SUBSTITUTE
    public static void loadLibrary(String name) throws Throwable {
        if (name.equals("zip")) {
            // Do nothing, since we already loaded this library ahead of time
            // to avoid bootstrap issues with class loading (and thus dynamic constant pool resolution).
            // See {@link VMClassLoader.loadJavaAndZipLibraries()}.
        } else {
            // ATTENTION: these statements must have the exact same side effects as the original code of the substitutee:
            final Class callerClass = Reflection.getCallerClass(3);
            final VirtualMethodActor method = ClassActor.fromJava(Runtime.class).findLocalVirtualMethodActor("loadLibrary0");
            try {
                method.invoke(ReferenceValue.from(Runtime.getRuntime()), ReferenceValue.from(callerClass), ReferenceValue.from(name));
            } catch (InvocationTargetException invocationTargetException) {
                throw invocationTargetException.getTargetException();
            } catch (IllegalAccessException illegalAccessException) {
                ProgramError.unexpected();
            }
        }

        // This has been added to re-initialize those classes in the boot image that had to wait for this library to appear:
        VMConfiguration.hostOrTarget().runScheme().runNativeInitializationMethods();
    }
}

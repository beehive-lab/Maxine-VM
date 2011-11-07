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
package com.sun.max.tele;

import static com.sun.max.tele.debug.ProcessState.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.util.logging.*;

import javax.swing.*;

import com.sun.max.config.*;
import com.sun.max.ide.*;
import com.sun.max.jdwp.vm.core.*;
import com.sun.max.jdwp.vm.proxy.*;
import com.sun.max.jdwp.vm.proxy.VMValue.Type;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.program.Classpath.Entry;
import com.sun.max.program.option.*;
import com.sun.max.tele.channel.*;
import com.sun.max.tele.channel.tcp.*;
import com.sun.max.tele.data.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.debug.TeleBytecodeBreakpoint.BytecodeBreakpointManager;
import com.sun.max.tele.debug.no.*;
import com.sun.max.tele.field.*;
import com.sun.max.tele.interpreter.*;
import com.sun.max.tele.jdwputil.*;
import com.sun.max.tele.memory.*;
import com.sun.max.tele.method.*;
import com.sun.max.tele.method.CodeLocation.BytecodeLocation;
import com.sun.max.tele.method.CodeLocation.MachineCodeLocation;
import com.sun.max.tele.object.*;
import com.sun.max.tele.reference.*;
import com.sun.max.tele.type.*;
import com.sun.max.tele.util.*;
import com.sun.max.tele.value.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.debug.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.reference.hosted.*;
import com.sun.max.vm.tele.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Implementation of remote access to an instance of the Maxine VM.
 * Access from the Inspector or other clients of this implementation
 * gain access through the {@link MaxVM} interface.
 * <p>
 * <strong>Concurrency policy:</strong> VM access is protected
 * by a reentrant lock that must be honored by all client-visible
 * methods that are not thread-safe.  Consequences of failure to
 * do so can result in either (a) undefined behavior of the VM
 * process (when inappropriate process operations are made
 * while the process is running), or (b) race conditions in the
 * data caches being revised a the conclusion of each process
 * execution.  The lock is managed differently by the process and
 * by client methods.
 * <ol>
 * <li>the VM process (see {@link TeleProcess}) enqueues requests for VM
 * execution; these requests may be made on client threads.  The requests
 * are executed on a separate "request handling" thread, which acquires
 * and holds the lock unconditionally during the entire cycle of request
 * execution:  setup of state, VM execution, waiting for VM execution
 * to conclude, refreshing caches of VM state.</li>
 * <li>any method made available to clients (see {@link MaxVM} and
 * related interfaces) must either be made thread-safe (and documented
 * as such) or must be wrapped in a conditional attempt to acquire the
 * lock.  Client attempts to acquire the lock that fail, must respond
 * immediately by throwing an {@link MaxVMBusyException}.</li>
 * <li>note that the lock is reentrant, so that nested attempts to
 * acquire/release the lock will behave identically to standard
 * Java synchronization semantics.</li>
 * </ol>
 */
public abstract class TeleVM implements MaxVM {

    private static final int TRACE_VALUE = 1;

    /**
     * The of the binary file in which the VM executable is stored.
     */
    private static final String BOOTIMAGE_FILE_NAME = "maxvm";

    /**
     * The name of the native library that supports the Inspector.
     */
    public static final String TELE_LIBRARY_NAME = "tele";

    private static final List<MaxMemoryRegion> EMPTY_MAXMEMORYREGION_LIST = Collections.emptyList();

    /**
     * Defines whether the target VM running locally or on a remote machine, or is core-dump.
     */
    public static final class TargetLocation {
        public enum Kind {
            LOCAL("Native"),      // target VM is on the same machine as Inspector
            REMOTE("TCP"),     // target VM is on a remote machine
            FILE("Dump");          // target VM is a core dump

            String classNameComponent;

            Kind(String name) {
                classNameComponent = name;
            }
        }

        public final Kind kind;
        public final String target;  // pathname to dump file if kind == FILE, else remote machine id
        public final int port;         // port to communicate on
        public final int id;            // process id (to attach to)

        private TargetLocation(Kind kind, String target, int port, int id) {
            this.kind = kind;
            this.target = target;
            this.port = port;
            this.id = id;
        }

        static void set(Options options) {
            final String targetKind = options.targetKindOption.getValue();
            String target = null;
            Kind kind = Kind.LOCAL;
            int port = TCPTeleChannelProtocol.DEFAULT_PORT;
            int id = -1;
            final List<String> targetLocationValue = options.targetLocationOption.getValue();
            if (targetKind.equals("remote")) {
                kind = Kind.REMOTE;
                final int size = targetLocationValue.size();
                if (size == 0 || size > 3) {
                    usage(options.targetLocationOption);
                }
                if (size >= 1) {
                    target = targetLocationValue.get(0);
                }
                if (size >= 2) {
                    final String portString = targetLocationValue.get(1);
                    if (!portString.isEmpty()) {
                        port = Integer.parseInt(portString);
                    }
                }
                if (size == 3) {
                    id = Integer.parseInt(targetLocationValue.get(2));
                }
            } else if (targetKind.equals("file")) {
                kind = Kind.FILE;
                if (targetLocationValue.size() > 0) {
                    target = targetLocationValue.get(0);
                }
            } else if (targetKind.equals("local")) {
                kind = Kind.LOCAL;
                if (targetLocationValue.size() == 1) {
                    id = Integer.parseInt(targetLocationValue.get(0));
                } else if (targetLocationValue.size() != 0) {
                    usage(options.targetLocationOption);
                }
            } else {
                TeleError.unexpected("usage: " + options.targetKindOption.getHelp());
            }
            if (mode == MaxInspectionMode.ATTACH || mode == MaxInspectionMode.ATTACHWAITING) {
                if (kind == Kind.FILE) {
                    // must have a dump file, if not provided put up a dialog to get it.
                    if (target == null) {
                        target = JOptionPane.showInputDialog(null, "Enter the path to the VM dump file");
                    }
                } else {
                // must have an id, if not provided put up a dialog to get it.
                    if (id < 0) {
                        id = Integer.parseInt(JOptionPane.showInputDialog(null, "Enter the target VM id"));
                    }
                }
            }
            targetLocation = new TargetLocation(kind, target, port, id);
        }

        private static void usage(Option<List<String>> locationOption) {
            TeleError.unexpected("usage: " + locationOption.getHelp());
        }
    }

    /**
     * The mode of the inspection, which require different startup behavior.
     */
    public static MaxInspectionMode mode;

    /**
     * Information about where the (running/dumped) target VM is located.
     */
    private static TargetLocation targetLocation;

    /**
     * Where the meta-data associated with the target VM is located {@see #vmDirectoryOption}.
     */
    private static File vmDirectory;

    /**
     * An abstraction description of the VM's platform, suitable for export.
     */
    private TelePlatform telePlatform;

    /**
     * If {@code true}, always prompt for native code frame view when entering native code.
     */
    public static boolean promptForNativeCodeView;

    /**
     * The options controlling how a tele VM instance is {@linkplain #newAllocator(String...) created}.
     */
    public static class Options extends OptionSet {

        /**
         * Specifies if these options apply when creating a {@linkplain TeleVM#createReadOnly(File, Classpath) read-only} Tele VM.
         */
        public final Option<String> modeOption = newStringOption("mode", "create",
            "Mode of operation: create | attach | attachwaiting | image");
        public final Option<String> targetKindOption = newStringOption("target", "local",
            "Location kind of target VM: local | remote | file");
        public final Option<List<String>> targetLocationOption = newStringListOption("location", "",
            "Location info of target VM: hostname[, port, id] | pathname");
        public final Option<File> vmDirectoryOption = newFileOption("vmdir", BootImageGenerator.getDefaultVMDirectory(),
            "Path to directory containing VM executable, shared libraries and boot image.");
        public final Option<List<String>> classpathOption = newStringListOption("cp", null, File.pathSeparatorChar,
            "Additional locations to use when searching for Java class files. These locations are searched after the jar file containing the " +
            "boot image classes but before the locations corresponding to the class path of this JVM process.");
        public final Option<List<String>> sourcepathOption = newStringListOption("sourcepath", null, File.pathSeparatorChar,
            "Additional locations to use when searching for Java source files. These locations are searched before the default locations.");
        public final Option<File> commandFileOption = newFileOption("c", "",
            "Executes the commands in a file on startup.");
        public final Option<String> logLevelOption = newStringOption("logLevel", Level.SEVERE.getName(),
            "Level to set for java.util.logging root logger.");
        public final Option<Boolean> usePrecompilationBreakpoints = newBooleanOption("precomp-bp", false,
            "Method entry bytecode breakpoints also stop VM prior to compilation of matching methods.");
        public final Option<Boolean> nativePrompt = newBooleanOption("ncv", false,
            "Prompt for native code view when entering native code");

        /**
         * This field is {@code null} if {@link #readOnly} is {@code false}.
         */
        public final Option<String> heapOption;

        /**
         * This field is {@code null} if {@link #readOnly} is {@code true}.
         */
        public final Option<String> vmArguments;

        /**
         * Creates command line options that are specific to certain operation modes. No longer tries to customise the
         * options based on mode.
         */
        public Options() {
            heapOption = newStringOption("heap", null, "Relocation address for the heap and code in the boot image.");
            vmArguments = newStringOption("a", "", "Specifies the arguments to the target VM.");
        }
    }

    private static boolean needTeleLibrary() {
        return targetLocation.kind == TargetLocation.Kind.LOCAL;
    }

    public boolean isAttaching() {
        return mode == MaxInspectionMode.ATTACH;
    }

    public static boolean isDump() {
        return mode == MaxInspectionMode.ATTACH && targetLocation.kind == TargetLocation.Kind.FILE;
    }

    /**
     * Create the correct instance of {@link TeleChannelProtocol} based on {@link #targetLocation} and
     * {@link OS}.
     *
     * @param os
     */
    protected void setTeleChannelProtocol(OS os) {
        if (mode == MaxInspectionMode.IMAGE) {
            teleChannelProtocol = new ReadOnlyTeleChannelProtocol();
            return;
        }
        /*
         * To avoid boilerplate switch statements, the format of the class is required to be:
         * com.sun.max.tele.debug.<ospackage>.<os><kind>TeleChannelProtocol, where Kind == Native for LOCAL, TCP for
         * REMOTE and Dump for FILE. os is sanitized to conform to standard class naming rules. E.g. SOLARIS -> Solaris
         */
        final String className = "com.sun.max.tele.debug." + os.asPackageName() + "." + os.className +
                        targetLocation.kind.classNameComponent + "TeleChannelProtocol";
        try {
            final Class< ? > klass = Class.forName(className);
            Constructor< ? > cons;
            Object[] args;

            if (targetLocation.kind == TargetLocation.Kind.REMOTE) {
                cons = klass.getDeclaredConstructor(new Class[] {String.class, int.class});
                args = new Object[] {targetLocation.target, targetLocation.port};
            } else if (targetLocation.kind == TargetLocation.Kind.FILE) {
                // dump
                final File dumpFile = new File(targetLocation.target);
                if (!dumpFile.exists()) {
                    TeleError.unexpected("core dump file: " + targetLocation.target + " does not exist or is not accessible");
                }
                final File vmFile = new File(vmDirectory, "maxvm");
                if (!vmFile.exists()) {
                    TeleError.unexpected("vm file: " + vmFile + " does not exist or is not accessible");
                }
                cons = klass.getDeclaredConstructor(new Class[] {TeleVM.class, File.class, File.class});
                args = new Object[] {this, vmFile, dumpFile};
            } else {
                cons = klass.getDeclaredConstructor(new Class[] {});
                args = new Object[0];
            }
            teleChannelProtocol = (TeleChannelProtocol) cons.newInstance(args);
        } catch (Exception ex) {
            TeleError.unexpected("failed to create instance of " + className, ex);
        }

    }

    /**
     * Creates a new VM instance based on a given set of options.
     *
     * @param options the options controlling specifics of the VM instance to be created
     * @return a new VM instance
     */
    public static TeleVM create(Options options) throws BootImageException {
        mode = MaxInspectionMode.valueOf(options.modeOption.getValue().toUpperCase());

        TargetLocation.set(options);

        // Ensure that method actors are available for class initializers loaded at runtime.
        MaxineVM.preserveClinitMethods = true;

        if (options.usePrecompilationBreakpoints.getValue()) {
            BytecodeBreakpointManager.usePrecompilationBreakpoints = true;
        }

        promptForNativeCodeView = options.nativePrompt.getValue();

        final String logLevel = options.logLevelOption.getValue();
        try {
            LogManager.getLogManager().getLogger("").setLevel(Level.parse(logLevel));
        } catch (IllegalArgumentException e) {
            TeleWarning.message("Invalid level specified for java.util.logging root logger: " + logLevel + " [using " + Level.SEVERE + "]");
            LogManager.getLogManager().getLogger("").setLevel(Level.SEVERE);
        }

        TeleVM vm = null;

        // Configure the prototype class loader gets the class files used to build the image
        Classpath classpathPrefix = Classpath.EMPTY;
        final List<String> classpathList = options.classpathOption.getValue();
        if (classpathList != null) {
            final Classpath extraClasspath = new Classpath(classpathList.toArray(new String[classpathList.size()]));
            classpathPrefix = classpathPrefix.prepend(extraClasspath);
        }
        vmDirectory = options.vmDirectoryOption.getValue();
        classpathPrefix = classpathPrefix.prepend(BootImageGenerator.getBootImageJarFile(vmDirectory).getAbsolutePath());
        checkClasspath(classpathPrefix);
        final Classpath classpath = Classpath.fromSystem().prepend(classpathPrefix);
        HostedBootClassLoader.setClasspath(classpath);

        if (needTeleLibrary()) {
            Prototype.loadLibrary(TELE_LIBRARY_NAME);
        }
        final File bootImageFile = BootImageGenerator.getBootImageFile(vmDirectory);

        Classpath sourcepath = JavaProject.getSourcePath(TeleVM.class, true);
        final List<String> sourcepathList = options.sourcepathOption.getValue();
        if (sourcepathList != null) {
            sourcepath = sourcepath.prepend(new Classpath(sourcepathList.toArray(new String[sourcepathList.size()])));
        }
        checkClasspath(sourcepath);
        String heap = options.heapOption.getValue();

        if (heap != null) {
            System.setProperty(TeleHeap.HEAP_ADDRESS_PROPERTY, heap);
        }

        switch (mode) {
            case CREATE:
            case ATTACHWAITING:
                final String value = options.vmArguments.getValue();
                final String[] commandLineArguments = "".equals(value) ? new String[0] : value.trim().split(" ");
                vm = create(bootImageFile, sourcepath, commandLineArguments);
                vm.lock();
                try {
                    vm.updateVMCaches(0L);
                    vm.teleProcess().initializeState();
                    vm.modifyInspectableFlags(Inspectable.INSPECTED, true);
                } finally {
                    vm.unlock();
                }
                try {
                    vm.advanceToJavaEntryPoint();
                } catch (IOException ioException) {
                    throw new BootImageException(ioException);
                }
                break;

            case ATTACH:
                /* The fundamental difference in this mode is that VM has executed for a while.
                 * This means that boot heap relocation has (almost certainly) been performed
                 * AND the boot heap will contain references to the dynamic heap.
                 * So the delicate dance that us normally performed when setting up the
                 * TeleClassRegistry is neither entirely necessary nor sufficient.
                 * This is handled by doing two passes over the class registry and
                 * deferring resolution of those references that are outside the boot heap
                 * until the second pass, after the TeleHeap is fully initialized.
                 * We also need to explicitly refresh the threads and update state.
                 */
                vm = create(bootImageFile, sourcepath, null);
                vm.lock();
                try {
                    vm.updateVMCaches(0L);
                    vm.teleProcess().initializeStateOnAttach();
                } finally {
                    vm.unlock();
                }
                break;

            case IMAGE:
                vm = createReadOnly(bootImageFile, sourcepath);
                vm.updateVMCaches(0L);
        }

        final File commandFile = options.commandFileOption.getValue();
        if (commandFile != null && !commandFile.equals("")) {
            vm.executeCommandsFromFile(commandFile.getPath());
        }

        return vm;
    }

    public static TargetLocation targetLocation() {
        return targetLocation;
    }

    /**
     * Creates and installs the {@linkplain MaxineVM#vm() global VM} context based on a given
     * configuration loaded from a boot image.
     *
     * @param bootImageConfig
     */
    public static void initializeVM(VMConfiguration bootImageConfig) {
        MaxineVM vm = new MaxineVM(bootImageConfig);
        MaxineVM.set(vm);
        bootImageConfig.loadAndInstantiateSchemes(null);
        final VMConfiguration config = new VMConfiguration(
                        bootImageConfig.buildLevel,
                        Platform.platform(),
                        getInspectorReferencePackage(bootImageConfig.referencePackage),
                        bootImageConfig.layoutPackage,
                        bootImageConfig.heapPackage,
                        bootImageConfig.monitorPackage,
                        bootImageConfig.runPackage);
        vm = new MaxineVM(config);
        MaxineVM.set(vm);
        config.loadAndInstantiateSchemes(bootImageConfig.vmSchemes());
        JavaPrototype.initialize(false);
    }

    /**
     * Create the appropriate subclass of {@link TeleVM} based on VM configuration.
     *
     * @param bootImageFile
     * @param sourcepath
     * @param commandlineArguments {@code null} if {@code processId > 0} else command line arguments for new VM process
     * @return appropriate subclass of TeleVM for target VM
     * @throws BootImageException
     */
    private static TeleVM create(File bootImageFile, Classpath sourcepath, String[] commandlineArguments) throws BootImageException {
        final BootImage bootImage = new BootImage(bootImageFile);
        initializeVM(bootImage.vmConfiguration);

        TeleVM teleVM = null;
        final OS os = Platform.platform().os;
        final String className = "com.sun.max.tele.debug." + os.asPackageName() + "." + os.className + "TeleVM";
        try {
            final Class< ? > klass = Class.forName(className);
            final Constructor< ? > cons = klass.getDeclaredConstructor(new Class[] {BootImage.class, Classpath.class, String[].class});
            teleVM = (TeleVM) cons.newInstance(new Object[] {bootImage, sourcepath, commandlineArguments});
        } catch (Exception ex) {
            TeleError.unexpected("failed to instantiate " + className, ex);
        }
        return teleVM;
    }

    private static void checkClasspath(Classpath classpath) {
        for (Entry classpathEntry : classpath.entries()) {
            if (classpathEntry.isPlainFile()) {
                TeleWarning.message("Class path entry is neither a directory nor a JAR file: " + classpathEntry);
            }
        }
    }

    /**
     * Creates a tele VM instance that is read-only and is only useful for inspecting a boot image.
     *
     * @param bootImageFile the file containing the boot image
     * @param sourcepath the source code path to search for class or interface definitions
     * @return
     * @throws BootImageException
     * @throws IOException
     */
    private static TeleVM createReadOnly(File bootImageFile, Classpath sourcepath) throws BootImageException {
        final BootImage bootImage = new BootImage(bootImageFile);
        initializeVM(bootImage.vmConfiguration);
        return new ReadOnlyTeleVM(bootImage, sourcepath);
    }

    private static final Logger LOGGER = Logger.getLogger(TeleVM.class.getName());

    /**
     * An object that delays evaluation of a trace message for controller actions.
     */
    private class Tracer {

        private final String message;

        /**
         * An object that delays evaluation of a trace message.
         * @param message identifies what is being traced
         */
        public Tracer(String message) {
            this.message = message;
        }

        @Override
        public String toString() {
            return tracePrefix() + message;
        }
    }

    private final Tracer refreshTracer = new Tracer("refresh");


    private static BootImagePackage getInspectorReferencePackage(BootImagePackage referencePackage) {
        final String suffix = referencePackage.name().substring("com.sun.max.vm.reference".length());
        final BootImagePackage inspectorReferenceRootPackage = new com.sun.max.tele.reference.Package();
        return BootImagePackage.fromName(inspectorReferenceRootPackage.name() + suffix);
    }

    private String  tracePrefix() {
        return "[TeleVM: " + Thread.currentThread().getName() + "] ";
    }

    private final BootImage bootImage;

    private final File bootImageFile;

    final File programFile;

    private final TeleHeap heap;

    private TeleCodeCache teleCodeCache = null;

    private final CodeManager codeManager;

    /**
     * Breakpoint manager, for both target and bytecode breakpoints.
     */
    private final TeleBreakpointManager teleBreakpointManager;

    private final TeleBytecodeBreakpoint.BytecodeBreakpointManager bytecodeBreakpointManager;

    private final TeleWatchpoint.WatchpointManager watchpointManager;

    private final TeleThreadManager threadManager;

    /**
     * The immutable history of all VM states, as of the last state transition; thread safe
     * for access by client methods on any thread.
     */
    private volatile TeleVMState teleVMState;

    private List<MaxVMStateListener> vmStateListeners = new CopyOnWriteArrayList<MaxVMStateListener>();

    /**
     * Dispatcher for GC start events.
     */
    private VMEventDispatcher<MaxGCStartedListener> gcStartedListeners;

    /**
     * Dispatcher for GC completion events.
     */
    private VMEventDispatcher<MaxGCCompletedListener> gcCompletedListeners;

    /**
     * Dispatcher for thread entry events (i.e., when a {@link VmThread} enters its run method).
     */
    private VMEventDispatcher<MaxVMThreadEntryListener> threadEntryListeners;

    /**
     * Dispatcher for thread detaching events (i.e., when a {@link VmThread} has detached  itself from the {@link VmThreadMap#ACTIVE}  list of threads).
     */
    private VMEventDispatcher<MaxVMThreadDetachedListener> threadDetachListeners;

    private final TeleProcess teleProcess;

    public static TeleChannelProtocol teleChannelProtocol() {
        return teleChannelProtocol;
    }

    public final TeleProcess teleProcess() {
        return teleProcess;
    }

    public boolean isBootImageRelocated() {
        return true;
    }

    private final Pointer bootImageStart;

    public final Pointer bootImageStart() {
        return bootImageStart;
    }

    private final TeleFields teleFields;

    public final TeleFields teleFields() {
        return teleFields;
    }

    private final TeleMethods teleMethods;

    public final TeleMethods teleMethods() {
        return teleMethods;
    }

    private final VMConfiguration vmConfiguration;

    private final Classpath sourcepath;

    private int interpreterUseLevel = 0;

    private TeleClassRegistry teleClassRegistry;

    private final TimedTrace updateTracer;

    private final InvalidReferencesLogger invalidReferencesLogger;

    public final InvalidReferencesLogger invalidReferencesLogger() {
        return invalidReferencesLogger;
    }

    /**
     * A lock designed to keep all non-thread-safe client calls from being handled during the VM setup/execute/refresh cycle.
     */
    private ReentrantLock lock = new ReentrantLock();

    /**
     * The protocol that is being used to communicate with the target VM.
     */
    private static TeleChannelProtocol teleChannelProtocol;

    /**
     * Creates a tele VM instance by creating or attaching to a Maxine VM process.
     *
     * @param bootImageFile path to the boot image file loaded by the VM
     * @param bootImage the metadata describing the contents in the boot image
     * @param sourcepath path used to search for Java source files
     * @param commandLineArguments the command line arguments to be used when creating a new VM process. If this value
     *            is {@code null}, then an attempt is made to attach to the process whose id is {@code processID}.
     * @param processID the process ID of an existing VM instance to which this debugger should be attached. This
     *            argument is ignored if {@code commandLineArguments != null}.
     * @param agent the agent that opens a socket for the VM to communicate the address of the boot image once it has
     *            been loaded and relocated. This parameter may be null if {@link #loadBootImage(TeleVMAgent)} is
     *            overridden by this object to use a different mechanism for discovering the boot image address.
     * @throws BootImageException
     */
    protected TeleVM(BootImage bootImage, Classpath sourcepath, String[] commandLineArguments) throws BootImageException {
        final TimedTrace tracer = new TimedTrace(TRACE_VALUE, tracePrefix() + " creating");
        tracer.begin();
        this.teleVMState = TeleVMState.nullState(mode);
        this.bootImageFile = bootImage.imageFile;
        this.bootImage = bootImage;

        this.sourcepath = sourcepath;
        this.telePlatform = new TelePlatform(Platform.platform());
        setTeleChannelProtocol(Platform.platform().os);

        this.updateTracer = new TimedTrace(TRACE_VALUE, tracePrefix() + " updating all");

        // Pre-initialize the disassembler to save time.
        TeleDisassembler.initialize(Platform.platform());

        this.programFile = new File(bootImageFile.getParent(), BOOTIMAGE_FILE_NAME);

        if (mode == MaxInspectionMode.ATTACH || mode == MaxInspectionMode.ATTACHWAITING) {
            this.teleProcess = attachToTeleProcess();
        } else {
            this.teleProcess = createTeleProcess(commandLineArguments);
        }
        this.bootImageStart = loadBootImage();
        this.vmConfiguration = VMConfiguration.vmConfig();
        final TeleReferenceScheme teleReferenceScheme = (TeleReferenceScheme) this.vmConfiguration.referenceScheme();
        teleReferenceScheme.setTeleVM(this);

        if (!tryLock(DEFAULT_MAX_LOCK_TRIALS)) {
            TeleError.unexpected("unable to lock during creation");
        }
        this.teleFields = new TeleFields(this);
        this.teleMethods = new TeleMethods(this);
        this.heap = TeleHeap.make(this);
        unlock();

        // Provide access to JDWP server
        this.jdwpAccess = new VMAccessImpl();
        addVMStateListener(jdwpStateModel);
        this.javaThreadGroupProvider = new ThreadGroupProviderImpl(this, true);
        this.nativeThreadGroupProvider = new ThreadGroupProviderImpl(this, false);

        this.threadManager = new TeleThreadManager(this);
        this.codeManager = new CodeManager(this);
        this.bytecodeBreakpointManager = new TeleBytecodeBreakpoint.BytecodeBreakpointManager(this);
        this.teleBreakpointManager = new TeleBreakpointManager(this, this.bytecodeBreakpointManager);
        this.watchpointManager = teleProcess.getWatchpointManager();
        this.invalidReferencesLogger = new InvalidReferencesLogger(this);

        this.gcStartedListeners = new VMEventDispatcher<MaxGCStartedListener>(teleMethods.gcStarted(), "before gc begins") {
            @Override
            protected void listenerDo(MaxThread thread, MaxGCStartedListener listener) {
                listener.gcStarted();
            }
        };

        this.gcCompletedListeners = new VMEventDispatcher<MaxGCCompletedListener>(teleMethods.gcCompleted(), "after gc completion") {
            @Override
            protected void listenerDo(MaxThread thread, MaxGCCompletedListener listener) {
                listener.gcCompleted();
            }
        };

        this.threadEntryListeners =  new VMEventDispatcher<MaxVMThreadEntryListener>(teleMethods.vmThreadRun(), "at VmThread entry") {
            @Override
            protected void listenerDo(MaxThread thread, MaxVMThreadEntryListener listener) {
                listener.entered(thread);
            }
        };

        this.threadDetachListeners =  new VMEventDispatcher<MaxVMThreadDetachedListener>(teleMethods.vmThreadDetached(), "after VmThread detach") {
            @Override
            protected void listenerDo(MaxThread thread, MaxVMThreadDetachedListener listener) {
                listener.detached(thread);
            }
        };

        tracer.end(null);
    }

    /**
     * Updates information about the state of the VM that is read
     * and cached at the end of each VM execution cycle.
     * <p>
     * This must be called in a context where thread-safe read access to the VM can
     * be achieved.
     * <p>
     * Some lazy initialization is done, in order to avoid cycles during startup.
     * @param epoch the number of times the process has run so far
     * @throws TeleError if unable to acquire the VM lock
     * @see #lock
     */
    public final void updateVMCaches(long epoch) {
        if (!tryLock(DEFAULT_MAX_LOCK_TRIALS)) {
            TeleError.unexpected("TeleVM unable to acquire VM lock for update at epoch=" + epoch);
        }
        try {
            updateTracer.begin("epoch=" + epoch);
            if (teleClassRegistry == null) {
                /*
                 * Must delay creation/initialization of the {@link TeleClassRegistry} until after
                 * we hit the first execution breakpoint; otherwise addresses won't have been relocated.
                 * This depends on the {@link TeleHeap} already existing.
                 */
                teleClassRegistry = new TeleClassRegistry(this, epoch);
                /*
                 *  Can only fully initialize the {@link TeleHeap} once
                 *  the {@TeleClassRegistry} is fully created, otherwise there's a cycle.
                 */
                heap.initialize(epoch);

                // Now set up the map of the compiled code cache
                teleCodeCache = new TeleCodeCache(this);
                teleCodeCache.initialize(epoch);
                if (isAttaching()) {
                    // Check that the target was run with option MakeInspectable otherwise the dynamic heap info will not be available
                    TeleError.check((teleFields().Inspectable_flags.readInt(this) & Inspectable.INSPECTED) != 0, "target VM was not run with -XX:+MakeInspectable option");
                    teleClassRegistry.processAttachFixupList();
                }
            }
            heap.updateCache(epoch);
            teleClassRegistry.updateCache(epoch);
            heap.updateObjectCache(epoch);
            teleCodeCache.updateCache(epoch);
            updateTracer.end("epoch=" + epoch);
        } finally {
            unlock();
        }
    }


    public final TeleVM vm() {
        return this;
    }

    public final String entityName() {
        return MaxineVM.name();
    }

    public final String entityDescription() {
        return MaxineVM.description();
    }

    public final MaxEntityMemoryRegion<MaxVM> memoryRegion() {
        return null;
    }

    public final boolean contains(Address address) {
        return findMemoryRegion(address) != null;
    }

    public final TeleObject representation() {
        // No distinguished object in VM runtime represents the VM.
        return null;
    }

    public final String getVersion() {
        return MaxineVM.VERSION_STRING;
    }

    public final String getDescription() {
        return MaxineVM.description();
    }

    public final TelePlatform platform() {
        return telePlatform;
    }

    public final File vmDirectory() {
        return vmDirectory;
    }

    public final BootImage bootImage() {
        return bootImage;
    }

    public final File bootImageFile() {
        return bootImageFile;
    }

    public final File programFile() {
        return programFile;
    }

    public final MaxInspectionMode inspectionMode() {
        return mode;
    }

    public final TeleClassRegistry classRegistry() {
        return teleClassRegistry;
    }

    public final TeleHeap heap() {
        return heap;
    }

    public final TeleCodeCache codeCache() {
        return teleCodeCache;
    }

    public final CodeManager codeManager() {
        return codeManager;
    }

    public final TeleBreakpointManager breakpointManager() {
        return teleBreakpointManager;
    }

    public final TeleWatchpoint.WatchpointManager watchpointManager() {
        return watchpointManager;
    }

    public final TeleThreadManager threadManager() {
        return threadManager;
    }

    /**
     * Returns the most recently notified VM state.  Note that this
     * isn't updated until the very end of a refresh cycle after VM
     * halt, so it should be considered out of date until the refresh
     * cycle is complete.  This is especially important when making
     * decisions concerning the process epoch.
     * Use {@link TeleProcess#epoch()} directly during the refresh
     * cycle, which is updated at the beginning of the refresh cycle.
     *
     * @return VM state; thread safe.
     */
    public final TeleVMState state() {
        return teleVMState;
    }

    public final void addVMStateListener(MaxVMStateListener listener) {
        vmStateListeners.add(listener);
    }

    public final void removeVMStateListener(MaxVMStateListener listener) {
        vmStateListeners.remove(listener);
    }

    public final void addGCStartedListener(MaxGCStartedListener listener) throws MaxVMBusyException {
        gcStartedListeners.add(listener, teleProcess);
    }

    public final void removeGCStartedListener(MaxGCStartedListener listener) throws MaxVMBusyException {
        gcStartedListeners.remove(listener);
    }

    public final void addThreadEnterListener(MaxVMThreadEntryListener listener) throws MaxVMBusyException {
        threadEntryListeners.add(listener, teleProcess);
    }

    public final void addThreadDetachedListener(MaxVMThreadDetachedListener listener) throws MaxVMBusyException {
        threadDetachListeners.add(listener, teleProcess);
    }

    public final void removeThreadEnterListener(MaxVMThreadEntryListener listener) throws MaxVMBusyException {
        threadEntryListeners.remove(listener);
    }

    public final void removeThreadDetachedListener(MaxVMThreadDetachedListener listener) throws MaxVMBusyException {
        threadDetachListeners.remove(listener);
    }

    public final void addGCCompletedListener(MaxGCCompletedListener listener) throws MaxVMBusyException {
        gcCompletedListeners.add(listener, teleProcess);
    }

    public final void removeGCCompletedListener(MaxGCCompletedListener listener) throws MaxVMBusyException {
        gcCompletedListeners.remove(listener);
    }

    public final MaxMemoryRegion findMemoryRegion(Address address) {
        for (MaxMemoryRegion memoryRegion : state().memoryAllocations()) {
            if (memoryRegion != null && memoryRegion.contains(address)) {
                return memoryRegion;
            }
        }
        return null;
    }

    public final MaxMemoryManagementInfo getMemoryManagementInfo(Address address) {
        return heap.getMemoryManagementInfo(address);
    }

    /**
     * Acquires a lock on the VM process and related cached state; blocks until lock
     * can be acquired.  The lock is reentrant, so that nested lock acquisition behaves with
     * standard Java synchronization semantics.
     */
    public final void lock() {
        lock.lock();
    }

    /**
     * Attempts to acquire a lock on the VM process and related cached state; returns
     * immediately. The lock is reentrant, so that nested lock acquisition behaves with
     * standard Java synchronization semantics.
     *
     * @return whether the lock was acquired
     */
    public final boolean tryLock() {
        return lock.tryLock();
    }

    public final boolean tryLock(int maxTrials) {
        int trials = 0;
        while (!vm().tryLock()) {
            if (++trials > maxTrials) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determines whether the calling thread holds the VM lock.
     * <p>
     * <strong>Note: this device is mainly used at present to
     * support the re-engineering effort to add reliable thread safety.
     * It may be set to be always {@code true} in released versions
     * of the code.
     *
     * @return whether the VM lock is held (now always TRUE)
     * @see #lock
     * @see #tryLock()
     * @see #unlock()
     */
    public final boolean lockHeldByCurrentThread() {

        // TODO (mlvdv)  restore thread lock predicate to operation; always true now
        return true;
        // return lock.isHeldByCurrentThread();
    }

    /**
     * Releases the lock on the VM process and related cached state; returns
     * immediately. The lock is reentrant, so that nested lock acquisition behaves with
     * standard Java synchronization semantics.
     */
    public final void unlock() {
        lock.unlock();
    }

    private static final int DEFAULT_MAX_LOCK_TRIALS = 100;

    public final void acquireLegacyVMAccess() throws MaxVMBusyException {
        if (!tryLock(DEFAULT_MAX_LOCK_TRIALS)) {
            throw new MaxVMBusyException();
        }
    }

    public final void releaseLegacyVMAccess() {
        assert lockHeldByCurrentThread();
        unlock();
    }

    /**
     * Sets or clears some bits of the {@link Inspectable#flags} field in the VM process.
     * <p>
     * Must be called in a thread holding the VM lock.
     *
     * @param flags specifies which bits to set or clear
     * @param set if {@code true}, then the bits are set otherwise they are cleared
     */
    public final void modifyInspectableFlags(int flags, boolean set) {
        assert lockHeldByCurrentThread();
        int newFlags = teleFields.Inspectable_flags.readInt(this);
        if (set) {
            newFlags |= flags;
        } else {
            newFlags &= ~flags;
        }
        teleFields.Inspectable_flags.writeInt(this, newFlags);
    }

    /**
     * Starts a new VM process and returns a handle to it.
     *
     * @param commandLineArguments the command line arguments to use when starting the VM process
     * @return a handle to the created VM process
     * @throws BootImageException if there was an error launching the VM process
     */
    protected abstract TeleProcess createTeleProcess(String[] commandLineArguments) throws BootImageException;

    /**
     * Gets any memory regions of potential interest that are specific to a particular VM platform.
     *
     * @return a list of platform-specific memory regions, empty if none.
     */
    protected List<MaxMemoryRegion> platformMemoryRegions() {
        return EMPTY_MAXMEMORYREGION_LIST;
    }


    /**
     * Attach to an existing VM process or code dump file.
     * @return TeleProcess instance
     * @throws BootImageException
     */
    protected TeleProcess attachToTeleProcess() throws BootImageException {
        throw TeleError.unimplemented();
    }

    /**
     * Gets a pointer to the boot image in the remote VM.
     *
     * @throws BootImageException if the address of the boot image could not be obtained
     */
    protected Pointer loadBootImage() throws BootImageException {
        final long value = teleChannelProtocol.getBootHeapStart();
        if (value == 0) {
            throw new BootImageException("failed to get boot image start from target VM");
        }
        return Pointer.fromLong(value);
    }

    private static void addNonNull(ArrayList<MaxMemoryRegion> regions, MaxMemoryRegion region) {
        if (region != null) {
            regions.add(region);
        }
    }

    /**
     * Notifies all registered listeners that the state of the process has changed,
     * for example started, stopped, or terminated.  Gathers up summary information
     * and creates a (top-level) immutable record of the state to accompany the notification.
     *
     * @param processState the new process state
     * @param epoch
     * @param singleStepThread the thread, if any, that just completed a single step
     * @param threads currently existing threads
     * @param threadsStarted threads newly created since last notification
     * @param threadsDied threads newly died since last notification
     * @param breakpointEvents breakpoint events, if any, that caused this state change
     * @param teleWatchpointEvent watchpoint, if any, that caused this state change
     * @see ProcessState
     */
    public final void notifyStateChange(
                    ProcessState processState,
                    long epoch,
                    TeleNativeThread singleStepThread,
                    Collection<TeleNativeThread> threads,
                    List<TeleNativeThread> threadsStarted,
                    List<TeleNativeThread> threadsDied,
                    List<TeleBreakpointEvent> breakpointEvents,
                    TeleWatchpointEvent teleWatchpointEvent) {

        // Rebuild list of all allocated memory regions
        final ArrayList<MaxMemoryRegion> memoryAllocations = new ArrayList<MaxMemoryRegion>(teleVMState.memoryAllocations().size());
        for (MaxHeapRegion heapRegion : heap.heapRegions()) {
            addNonNull(memoryAllocations, heapRegion.memoryRegion());
        }
        if (heap.rootsMemoryRegion() != null) {
            addNonNull(memoryAllocations, heap.rootsMemoryRegion());
        }
        for (MaxThread thread : threads) {
            addNonNull(memoryAllocations, thread.stack().memoryRegion());
            addNonNull(memoryAllocations, thread.localsBlock().memoryRegion());
        }
        for (MaxCompiledCodeRegion compiledCodeRegion : teleCodeCache.compiledCodeRegions()) {
            addNonNull(memoryAllocations, compiledCodeRegion.memoryRegion());
        }
        for (MaxMemoryRegion memoryRegion : platformMemoryRegions()) {
            addNonNull(memoryAllocations, memoryRegion);
        }

        this.teleVMState = new TeleVMState(
            mode,
            processState,
            epoch,
            memoryAllocations,
            threads,
            singleStepThread,
            threadsStarted,
            threadsDied,
            breakpointEvents,
            teleWatchpointEvent,
            heap.isInGC(), teleVMState);
        for (final MaxVMStateListener listener : vmStateListeners) {
            listener.stateChanged(teleVMState);
        }
    }

    public final int getInterpreterUseLevel() {
        return interpreterUseLevel;
    }

    public final void setInterpreterUseLevel(int interpreterUseLevel) {
        this.interpreterUseLevel = interpreterUseLevel;
    }

    public final int getVMTraceLevel() {
        return teleFields().Trace_level.readInt(this);
    }

    public final void setVMTraceLevel(int newLevel) {
        teleFields().Trace_level.writeInt(this, newLevel);
    }

    public final long getVMTraceThreshold() {
        return teleFields().Trace_threshold.readLong(this);
    }

    public final void setVMTraceThreshold(long newThreshold) {
        teleFields().Trace_threshold.writeLong(this, newThreshold);
    }

    public final TeleReferenceScheme referenceScheme() {
        return (TeleReferenceScheme) vmConfiguration.referenceScheme();
    }

    public final HeapScheme heapScheme() {
        return vmConfiguration.heapScheme();
    }

    /**
     * @return access to low-level reading and writing of memory in the VM.
     */
    public final DataAccess dataAccess() {
        return teleProcess.dataAccess();
    }

    public final Value readWordValue(Address address) {
        return WordValue.from(dataAccess().readWord(address));
    }

    public final void readBytes(Address address, byte[] bytes) {
        dataAccess().readFully(address, bytes);
    }

    private RemoteTeleReference createTemporaryRemoteTeleReference(Word rawReference) {
        return referenceScheme().createTemporaryRemoteTeleReference(rawReference.asAddress());
    }

    private RemoteTeleReference temporaryRemoteTeleReferenceFromOrigin(Word origin) {
        return referenceScheme().temporaryRemoteTeleReferenceFromOrigin(origin);
    }

    public final Reference originToReference(final Pointer origin) {
        return vmConfiguration.referenceScheme().fromOrigin(origin);
    }

    public final Reference bootClassRegistryReference() {
        return originToReference(bootImageStart.plus(bootImage.header.classRegistryOffset));
    }

    public final boolean isValidOrigin(Pointer origin) {
        if (origin.isZero()) {
            return false;
        }

        try {
            if (!heap().contains(origin) && (codeCache() == null || !codeCache().contains(origin))) {
                return false;
            }
            if (false && heap.isInGC() && heap().containsInDynamicHeap(origin)) {
                //  Assume that any reference to the dynamic heap is invalid during GC.
                return false;
            }
            if (false && bootImage.vmConfiguration.debugging()) {
                final Pointer cell = Layout.originToCell(origin);
                // Checking is easy in a debugging build; there's a special word preceding each object
                final Word tag = dataAccess().getWord(cell, 0, -1);
                return DebugHeap.isValidCellTag(tag);
            }

            // Check the hard way, using none of the higher level services in the Inspector,
            // since this predicate is necessary to build those services.
            //
            // Keep following hub pointers until the same hub is traversed twice or
            // an address outside of heap or code region(s) is encountered.
            //
            // For all objects other than a {@link StaticTuple}, the maximum chain takes only two hops
            // find the distinguished object with self-referential hub pointer:  the {@link DynamicHub} for
            // class {@link DynamicHub}.
            //          tuple -> dynamicHub of the tuple's class -> dynamicHub of DynamicHub
            Word hubWord = Layout.readHubReferenceAsWord(temporaryRemoteTeleReferenceFromOrigin(origin));
            for (int i = 0; i < 3; i++) {
                final RemoteTeleReference hubRef = createTemporaryRemoteTeleReference(hubWord);
                final Pointer hubOrigin = hubRef.toOrigin();
                if (!heap().contains(hubOrigin) && !codeCache().contains(hubOrigin)) {
                    return false;
                }
                final Word nextHubWord = Layout.readHubReferenceAsWord(hubRef);
                if (nextHubWord.equals(hubWord)) {
                    // We arrived at a DynamicHub for the class DynamicHub
                    if (i < 2) {
                        // All ordinary cases will have stopped by now
                        return true;
                    }
                    // This longer chain can only happen when we started with a {@link StaticTuple}.
                    // Perform a more precise test to check for this.
                    return isStaticTuple(origin);
                }
                hubWord = nextHubWord;
            }
        } catch (TerminatedProcessIOException terminatedProcessIOException) {
            return false;
        } catch (DataIOError dataAccessError) {
            return false;
        } catch (IndexOutOfBoundsException indexOutOfBoundsException) {
            return false;
        }
        return false;
    }

    /**
     * Low level predicate for identifying the special case of a {@link StaticTuple} in the VM,
     * using only the most primitive operations, since it is needed for building all the higher-level
     * services in the Inspector.
     * <p>
     * Note that this predicate is not precise; it may very rarely return a false positive.
     * <p>
     * The predicate depends on the following chain in the VM heap layout:
     * <ol>
     *  <li>The hub of a {@link StaticTuple} points at a {@link StaticHub}</li>
     *  <li>A field in a {@link StaticHub} points at the {@link ClassActor} for the class being implemented.</li>
     *  <li>A field in a {@link ClassActor} points at the {@link StaticTuple} for the class being implemented,
     *  which will point back at the original location if it is in fact a {@link StaticTuple}.</li>
     *  </ol>
     *  No type checks are performed, however, since this predicate must not depend on such higher-level information.
     *
     * @param origin a memory location in the VM
     * @return whether the object (probably)  points at an instance of {@link StaticTuple}
     * @see #isValidOrigin(Pointer)
     */
    private boolean isStaticTuple(Pointer origin) {
        // If this is a {@link StaticTuple} then a field in the header points at a {@link StaticHub}
        Word staticHubWord = Layout.readHubReferenceAsWord(temporaryRemoteTeleReferenceFromOrigin(origin));
        final RemoteTeleReference staticHubRef = createTemporaryRemoteTeleReference(staticHubWord);
        final Pointer staticHubOrigin = staticHubRef.toOrigin();
        if (!heap().contains(staticHubOrigin) && !codeCache().contains(staticHubOrigin)) {
            return false;
        }
        // If we really have a {@link StaticHub}, then a known field points at a {@link ClassActor}.
        final int hubClassActorOffset = teleFields().Hub_classActor.fieldActor().offset();
        final Word classActorWord = dataAccess().readWord(staticHubOrigin, hubClassActorOffset);
        final RemoteTeleReference classActorRef = createTemporaryRemoteTeleReference(classActorWord);
        final Pointer classActorOrigin = classActorRef.toOrigin();
        if (!heap().contains(classActorOrigin) && !codeCache().contains(classActorOrigin)) {
            return false;
        }
        // If we really have a {@link ClassActor}, then a known field points at the {@link StaticTuple} for the class.
        final int classActorStaticTupleOffset = teleFields().ClassActor_staticTuple.fieldActor().offset();
        final Word staticTupleWord = dataAccess().readWord(classActorOrigin, classActorStaticTupleOffset);
        final RemoteTeleReference staticTupleRef = createTemporaryRemoteTeleReference(staticTupleWord);
        final Pointer staticTupleOrigin = staticTupleRef.toOrigin();
        // If we really started with a {@link StaticTuple}, then this field will point at it
        return staticTupleOrigin.equals(origin);
    }

    /**
     * Checks that a {@link Reference} points to a heap object in the VM;
     * throws an unchecked exception if not.  This is a low-level method
     * that uses a debugging tag or (if no tags in image) a heuristic; it does
     * not require access to the {@link TeleClassRegistry}.
     *
     * @param reference memory location in the VM
     * @throws InvalidReferenceException when the location does <strong>not</strong> point
     * at a valid heap object.
     */
    public final void checkReference(Reference reference) throws InvalidReferenceException {
        if (!isValidOrigin(reference.toOrigin())) {
            throw new InvalidReferenceException(reference);
        }
    }

    public final Reference wordToReference(Word word) {
        return referenceScheme().fromOrigin(word.asPointer());
    }

    /**
     * Creates a temporary reference for access to VM memory without invoking the
     * canonicalization machinery.
     *
     * @return a reference to a location in VM memory that is not safe across GC
     */
    public final Reference wordToTemporaryReference(Address address) {
        return referenceScheme().createTemporaryRemoteTeleReference(address);
    }

    /**
     * Reads an element from an array of {@linkplain Reference references} in the
     * VM. Gets the address from the reference and create an Inspector reference
     * pointing at that object.
     *
     * @param reference an array of references in the VM
     * @param index offset into an array of references
     * @return the contents of the array at the index, interpreted as an address and wrapped in a Reference.
     * @throws InvalidReferenceException (unchecked)
     */
    public final Reference readReference(Reference reference, int index) throws InvalidReferenceException {
        checkReference(reference);
        // Read as an array of words
        return wordToReference(Layout.getWord(reference, index));
    }

    /**
     * Returns a local copy of a {@link String} object in the VM's heap.
     *
     * @param stringReference A {@link String} object in the VM.
     * @return A local {@link String} duplicating the object's contents.
     * @throws InvalidReferenceException if the argument does not point a valid heap object.
     */
    public final String getString(Reference stringReference)  throws InvalidReferenceException {
        checkReference(stringReference);
        final Reference charArrayReference = teleFields().String_value.readReference(stringReference);
        if (charArrayReference.isZero()) {
            return null;
        }
        checkReference(charArrayReference);
        int offset = teleFields().String_offset.readInt(stringReference);
        final int count = teleFields().String_count.readInt(stringReference);
        final char[] chars = new char[count];
        for (int i = 0; i < count; i++) {
            chars[i] = Layout.getChar(charArrayReference, offset);
            offset++;
        }
        return new String(chars);
    }

    /**
     * Returns a local copy of the contents of a {@link String} object in the VM's heap,
     * using low level mechanisms and performing no checking that the location
     * or object are valid.
     * <p>
     * The intention is to provide a fast, low-level mechanism for reading strings that
     * can be used outside of the AWT event thread without danger of deadlock,
     * for example on the canonical reference machinery.
     *
     * @param stringReference a {@link String} object in the VM
     * @return A local {@link String} duplicating the remote object's contents, null if it can't be read.
     */
    public final String getStringUnsafe(Reference stringReference) {
        // Work only with temporary references that are unsafe across GC
        // Do no testing to determine if the reference points to a valid String object in live memory.
        try {
            final RemoteTeleReference stringRef = temporaryRemoteTeleReferenceFromOrigin(stringReference.toOrigin());
            final Word valueWord = stringRef.readWord(teleFields().String_value.fieldActor().offset());
            final RemoteTeleReference valueRef = createTemporaryRemoteTeleReference(valueWord);
            int offset = stringRef.readInt(teleFields.String_offset.fieldActor().offset());
            final int count = stringRef.readInt(teleFields.String_count.fieldActor().offset());
            final char[] chars = new char[count];
            for (int i = 0; i < count; i++) {
                chars[i] = Layout.getChar(valueRef, offset);
                offset++;
            }
            return new String(chars);
        } catch (TerminatedProcessIOException terminatedProcessIOException) {
            return null;
        } catch (DataIOError dataIOError) {
            return null;
        }
    }

    /**
     * Returns a local copy of the contents of the inspectable list of dynamic
     * heap regions in the VM, using low level mechanisms and performing no checking that
     * the location or objects are valid.
     * <p>
     * The intention is to provide a way to read this data without needing any of the
     * usual type-based mechanisms for reading data, all of which rely on a populated
     * {@link TeleClassRegistry}.  This is needed when attaching to a process or reading
     * a dump, where a description of the dynamic heap must be determined before the
     * {@link TeleClassRegistry} can be built.
     *
     * @return a list of objects, each of which describes a dynamically allocated heap region
     * in the VM, empty array if no such heap regions
     */
    public final List<MaxMemoryRegion> getDynamicHeapRegionsUnsafe() {
        // Work only with temporary references that are unsafe across GC
        // Do no testing to determine if the reference points to a valid object in live memory of the correct types.

        final List<MaxMemoryRegion> regions = new ArrayList<MaxMemoryRegion>();

        // Location of the inspectable field that might point to an array of dynamically allocated heap regions
        final Pointer dynamicHeapRegionsArrayFieldPointer = bootImageStart.plus(bootImage.header.dynamicHeapRegionsArrayFieldOffset);

        // Value of the field, possibly a pointer to an array of dynamically allocated heap regions
        final Word fieldValue = dataAccess().readWord(dynamicHeapRegionsArrayFieldPointer.asAddress());

        if (!fieldValue.isZero()) {
            // Assert that this points to an array of references, read as words
            final RemoteTeleReference wordArrayReference = createTemporaryRemoteTeleReference(fieldValue);
            final int length = Layout.readArrayLength(wordArrayReference);

            // Read the references as words to avoid using too much machinery
            for (int index = 0; index < length; index++) {
                // Read an entry from the array
                final Word regionReferenceWord = Layout.getWord(wordArrayReference, index);
                // Assert that this points to an object of type {@link MemoryRegion} in the VM
                RemoteTeleReference regionReference = createTemporaryRemoteTeleReference(regionReferenceWord);
                final Address address = regionReference.readWord(teleFields.MemoryRegion_start.fieldActor().offset()).asAddress();
                final int size = regionReference.readInt(teleFields.MemoryRegion_size.fieldActor().offset());
                regions.add(new TeleFixedMemoryRegion(vm(), "Fake", address, size));
            }
        }
        return regions;
    }

    public final Value getElementValue(Kind kind, Reference reference, int index) throws InvalidReferenceException {
        switch (kind.asEnum) {
            case BYTE:
                return ByteValue.from(Layout.getByte(reference, index));
            case BOOLEAN:
                return BooleanValue.from(Layout.getBoolean(reference, index));
            case SHORT:
                return ShortValue.from(Layout.getShort(reference, index));
            case CHAR:
                return CharValue.from(Layout.getChar(reference, index));
            case INT:
                return IntValue.from(Layout.getInt(reference, index));
            case FLOAT:
                return FloatValue.from(Layout.getFloat(reference, index));
            case LONG:
                return LongValue.from(Layout.getLong(reference, index));
            case DOUBLE:
                return DoubleValue.from(Layout.getDouble(reference, index));
            case WORD:
                return new WordValue(Layout.getWord(reference, index));
            case REFERENCE:
                checkReference(reference);
                return TeleReferenceValue.from(this, wordToReference(Layout.getWord(reference, index)));
            default:
                throw TeleError.unknownCase("unknown array kind");
        }
    }

    public final void copyElements(Kind kind, Reference src, int srcIndex, Object dst, int dstIndex, int length) {
        switch (kind.asEnum) {
            case BYTE:
                Layout.byteArrayLayout().copyElements(src, srcIndex, dst, dstIndex, length);
                break;
            case BOOLEAN:
                Layout.booleanArrayLayout().copyElements(src, srcIndex, dst, dstIndex, length);
                break;
            case SHORT:
                Layout.shortArrayLayout().copyElements(src, srcIndex, dst, dstIndex, length);
                break;
            case CHAR:
                Layout.charArrayLayout().copyElements(src, srcIndex, dst, dstIndex, length);
                break;
            case INT:
                Layout.intArrayLayout().copyElements(src, srcIndex, dst, dstIndex, length);
                break;
            case FLOAT:
                Layout.floatArrayLayout().copyElements(src, srcIndex, dst, dstIndex, length);
                break;
            case LONG:
                Layout.longArrayLayout().copyElements(src, srcIndex, dst, dstIndex, length);
                break;
            case DOUBLE:
                Layout.doubleArrayLayout().copyElements(src, srcIndex, dst, dstIndex, length);
                break;
            case WORD:
                Layout.wordArrayLayout().copyElements(src, srcIndex, dst, dstIndex, length);
                break;
            default:
                throw TeleError.unknownCase("unknown array kind");
        }
    }

    public final List<MaxCodeLocation> inspectableMethods() {
        final List<MaxCodeLocation> methods = new ArrayList<MaxCodeLocation>(teleMethods.clientInspectableMethods());
        methods.addAll(heap.inspectableMethods());
        return methods;
    }

    public final <TeleMethodActor_Type extends TeleMethodActor> TeleMethodActor_Type findTeleMethodActor(Class<TeleMethodActor_Type> teleMethodActorType, MethodActor methodActor) {
        final TeleClassActor teleClassActor = teleClassRegistry.findTeleClassActor(methodActor.holder().typeDescriptor);
        if (teleClassActor != null) {
            for (TeleMethodActor teleMethodActor : teleClassActor.getTeleMethodActors()) {
                if (teleMethodActor.methodActor().equals(methodActor)) {
                    return teleMethodActorType.cast(teleMethodActor);
                }
            }
        }
        return null;
    }

    public final void setTransportDebugLevel(int level) {
        teleProcess.setTransportDebugLevel(level);
    }

    public final int transportDebugLevel() {
        return teleProcess.transportDebugLevel();
    }

    public void advanceToJavaEntryPoint() throws IOException {
        final Address startEntryAddress = bootImageStart().plus(bootImage().header.vmRunMethodOffset);
        final MachineCodeLocation entryLocation = codeManager().createMachineCodeLocation(startEntryAddress, "vm start address");

        try {
            runToInstruction(entryLocation, true, false);
        } catch (Exception exception) {
            throw new IOException(exception);
        }
        try {
            addGCCompletedListener(new MaxGCCompletedListener() {
                // The purpose of this listener, which doesn't do anything explicitly,
                // is to force a VM stop at the end of each GC cycle, even if there are
                // no other listeners.  This presents an opportunity for the Reference/Object
                // code to update heap-related information that may have been changed as
                // a result of the GC.
                public void gcCompleted() {
                    Trace.line(TRACE_VALUE, tracePrefix() + "GC complete");
                }
            });
        } catch (MaxVMBusyException maxVMBusyException) {
            TeleError.unexpected("Unable to set initial GC completed listener");
        }
    }

    public final Value interpretMethod(ClassMethodActor classMethodActor, Value... arguments) throws InvocationTargetException {
        return TeleInterpreter.execute(this, classMethodActor, arguments);
    }

    public final void resume(final boolean synchronous, final boolean withClientBreakpoints) throws InvalidVMRequestException, OSExecutionRequestException {
        teleProcess.resume(synchronous, withClientBreakpoints);
    }

    public final void singleStepThread(final MaxThread maxThread, boolean synchronous) throws InvalidVMRequestException, OSExecutionRequestException {
        final TeleNativeThread teleNativeThread = (TeleNativeThread) maxThread;
        teleProcess.singleStepThread(teleNativeThread, synchronous);
    }

    public final void stepOver(final MaxThread maxThread, boolean synchronous, final boolean withClientBreakpoints) throws InvalidVMRequestException, OSExecutionRequestException {
        final TeleNativeThread teleNativeThread = (TeleNativeThread) maxThread;
        teleProcess.stepOver(teleNativeThread, synchronous, withClientBreakpoints);
    }

    public final void runToInstruction(final MaxCodeLocation maxCodeLocation, final boolean synchronous, final boolean withClientBreakpoints) throws OSExecutionRequestException, InvalidVMRequestException {
        final CodeLocation codeLocation = (CodeLocation) maxCodeLocation;
        teleProcess.runToInstruction(codeLocation, synchronous, withClientBreakpoints);
    }

    public final  void returnFromFrame(final MaxThread thread, final boolean synchronous, final boolean withClientBreakpoints) throws OSExecutionRequestException, InvalidVMRequestException {
        final TeleNativeThread teleNativeThread = (TeleNativeThread) thread;
        final CodeLocation returnLocation = teleNativeThread.stack().returnLocation();
        if (returnLocation == null) {
            throw new InvalidVMRequestException("No return location available");
        }
        teleProcess.runToInstruction(returnLocation, synchronous, withClientBreakpoints);
    }

    public final  void pauseVM() throws InvalidVMRequestException, OSExecutionRequestException {
        teleProcess.pauseProcess();
    }

    public final void terminateVM() throws Exception {
        teleProcess.terminateProcess();
    }

    public final ReferenceValue createReferenceValue(Reference reference) {
        if (reference instanceof TeleReference) {
            return TeleReferenceValue.from(this, reference);
        } else if (reference instanceof HostedReference) {
            return TeleReferenceValue.from(this, Reference.fromJava(reference.toJava()));
        }
        throw TeleError.unexpected("Got a non-Prototype, non-Tele reference in createReferenceValue");
    }

    public final File findJavaSourceFile(ClassActor classActor) {
        final String sourceFilePath = classActor.sourceFilePath();
        return sourcepath.findFile(sourceFilePath);
    }

    public final void executeCommandsFromFile(String fileName) {
        FileCommands.executeCommandsFromFile(this, fileName);
    }

    //
    // Code from here to end of file supports the Maxine JDWP server
    //

   /**
     * Provides access to the VM from a JDWP server.
     */
    private final VMAccess jdwpAccess;

    /**
     * @return access to the VM for the JDWP server.
     * @see com.sun.max.jdwp.maxine.Main
     */
    public final VMAccess vmAccess() {
        return jdwpAccess;
    }

    public final void fireJDWPThreadEvents() {
        for (MaxThread thread : teleVMState.threadsDied()) {
            fireJDWPThreadDiedEvent((TeleNativeThread) thread);
        }
        for (MaxThread thread : teleVMState.threadsStarted()) {
            fireJDWPThreadStartedEvent((TeleNativeThread) thread);
        }
    }

    private final ArrayList<VMListener> jdwpListeners = new ArrayList<VMListener>();

    /**
     * Informs all JDWP listeners that the VM died.
     */
    private void fireJDWPVMDiedEvent() {
        LOGGER.info("VM EVENT: VM died");
        for (VMListener listener : jdwpListeners) {
            listener.vmDied();
        }
    }

    /**
     * Informs all JDWP listeners that a single step has been completed.
     *
     * @param thread the thread that did the single step
     * @param location the code location onto which the thread just stepped
     */
    private void fireJDWPSingleStepEvent(ThreadProvider thread, JdwpCodeLocation location) {
        LOGGER.info("VM EVENT: Single step was made at thread " + thread
                + " to location " + location);
        for (VMListener listener : jdwpListeners) {
            listener.singleStepMade(thread, location);
        }
    }

    /**
     * Informs all JDWP listeners that a breakpoint has been hit.
     *
     * @param thread the thread that hit the breakpoint
     * @param location the code location at which the breakpoint was hit
     */
    private void fireJDWPBreakpointEvent(ThreadProvider thread, JdwpCodeLocation location) {
        LOGGER.info("VM EVENT: Breakpoint hit at thread " + thread
                + " at location " + location);
        for (VMListener listener : jdwpListeners) {
            listener.breakpointHit(thread, location);
        }
    }

    /**
     * Informs all JDWP listeners that a thread has started.
     *
     * @param thread the thread that has started
     */
    private void fireJDWPThreadStartedEvent(ThreadProvider thread) {
        LOGGER.info("VM EVENT: Thread started: " + thread);
        for (VMListener listener : jdwpListeners) {
            listener.threadStarted(thread);
        }
    }

    /**
     * Informs all JDWP listeners that a thread has died.
     *
     * @param thread the thread that has died
     */
    private void fireJDWPThreadDiedEvent(ThreadProvider thread) {
        LOGGER.info("VM EVENT: Thread died: " + thread);
        for (VMListener listener : jdwpListeners) {
            listener.threadDied(thread);
        }
    }

    private final MaxVMStateListener jdwpStateModel = new MaxVMStateListener() {

        public void stateChanged(MaxVMState maxVMState) {
            Trace.begin(TRACE_VALUE, tracePrefix() + "handling " + maxVMState);
            fireJDWPThreadEvents();
            switch(maxVMState.processState()) {
                case TERMINATED:
                    fireJDWPVMDiedEvent();
                    break;
                case STOPPED:
                    if (!jdwpListeners.isEmpty()) {
                        for (MaxBreakpointEvent maxBreakpointEvent : maxVMState.breakpointEvents()) {
                            final TeleNativeThread teleNativeThread = (TeleNativeThread) maxBreakpointEvent.thread();
                            fireJDWPBreakpointEvent(teleNativeThread, teleNativeThread.getFrames()[0].getLocation());
                        }
                        final MaxThread singleStepThread = maxVMState.singleStepThread();
                        if (singleStepThread != null) {
                            final TeleNativeThread thread = (TeleNativeThread) singleStepThread;
                            fireJDWPSingleStepEvent(thread, thread.getFrames()[0].getLocation());
                        }
                    }
                    break;
                case RUNNING:
                    LOGGER.info("VM continued to RUN!");
                    break;
            }
            Trace.end(TRACE_VALUE, tracePrefix() + "handling " + maxVMState);
        }
    };

    /**
     * Reads a value of a certain kind from the Maxine VM process.
     *
     * @param kind the type of the value that should be read
     * @param pointer pointer to the memory location where the value should be read
     * @param offset offset that should be added to the pointer before reading the value
     * @return the value read from the Maxine VM process
     */
    public final Value readValue(Kind kind, Pointer pointer, int offset) {

        final Reference reference = originToReference(pointer);

        if (kind.isReference) {
            final Word word = dataAccess().readWord(pointer, offset);
            return TeleReferenceValue.from(this, wordToReference(word));
        }

        final Value result = kind.readValue(reference, offset);

        if (result.kind().isWord) {
            LOGGER.info("Creating WORD reference! " + result.asWord());
            return LongValue.from(result.asWord().asAddress().toLong());
        }

        if (result.kind().isReference
                && !isValidOrigin(result.asReference().toOrigin())) {
            LOGGER.severe("Wrong reference encountered ("
                    + result.asReference() + "), returning null reference!");
            return ReferenceValue.fromReference(Reference.fromOrigin(Pointer.zero()));
        }

        return result;
    }

    /**
     * Tries to find a JDWP ObjectProvider that represents the object that is
     * referenced by the parameter.
     *
     * @param reference
     *            a reference to the object that should be represented as a JDWP
     *            ObjectProvider
     * @return a JDWP ObjectProvider object or null, if no object is found at
     *         the address specified by the reference
     */
    private ObjectProvider findObject(Reference reference) {
        if (isValidOrigin(reference.toOrigin())) {
            return heap().makeTeleObject(reference);
        }
        return null;
    }

    private final ThreadGroupProvider javaThreadGroupProvider;

    /**
     * @return Thread group that should be used to logically group Java threads in the VM.
     */
    public final ThreadGroupProvider javaThreadGroupProvider() {
        return javaThreadGroupProvider;
    }

    private final ThreadGroupProvider nativeThreadGroupProvider;

   /**
     * @return Thread group that should be used to logically group native threads.
     */
    public final ThreadGroupProvider nativeThreadGroupProvider() {
        return nativeThreadGroupProvider;
    }

    /**
     * Converts a value kind as seen by the Maxine world to a VMValue type as
     * seen by the VM interface used by the JDWP server.
     *
     * @param kind the Maxine kind value
     * @return the type as seen by the JDWP server
     */
    public static Type maxineKindToJDWPType(Kind kind) {

        final KindEnum e = kind.asEnum;
        switch (e) {
            case BOOLEAN:
                return VMValue.Type.BOOLEAN;
            case BYTE:
                return VMValue.Type.BYTE;
            case CHAR:
                return VMValue.Type.CHAR;
            case DOUBLE:
                return VMValue.Type.DOUBLE;
            case FLOAT:
                return VMValue.Type.FLOAT;
            case INT:
                return VMValue.Type.INT;
            case LONG:
                return VMValue.Type.LONG;
            case REFERENCE:
                return VMValue.Type.PROVIDER;
            case SHORT:
                return VMValue.Type.SHORT;
            case VOID:
                return VMValue.Type.VOID;
            case WORD:
                break;
        }

        throw new IllegalArgumentException("Typeype " + kind
                + " cannot be resolved to a virtual machine value type");
    }

    /**
     * Converts a value as seen by the Maxine VM to a value as seen by the JDWP
     * server.
     *
     * @param value   the value as seen by the Maxine VM
     * @return the value as seen by the JDWP server
     */
    public final VMValue maxineValueToJDWPValue(Value value) {
        switch (value.kind().asEnum) {
            case BOOLEAN:
                return jdwpAccess.createBooleanValue(value.asBoolean());
            case BYTE:
                return jdwpAccess.createByteValue(value.asByte());
            case CHAR:
                return jdwpAccess.createCharValue(value.asChar());
            case DOUBLE:
                return jdwpAccess.createDoubleValue(value.asDouble());
            case FLOAT:
                return jdwpAccess.createFloatValue(value.asFloat());
            case INT:
                return jdwpAccess.createIntValue(value.asInt());
            case LONG:
                return jdwpAccess.createLongValue(value.asLong());
            case REFERENCE:
                return jdwpAccess.createObjectProviderValue(findObject(value.asReference()));
            case SHORT:
                return jdwpAccess.createShortValue(value.asShort());
            case VOID:
                return jdwpAccess.getVoidValue();
            case WORD:
                final Word word = value.asWord();
                LOGGER.warning("Tried to convert a word, this is not implemented yet! (word="
                            + word + ")");
                return jdwpAccess.getVoidValue();
        }

        throw new IllegalArgumentException("Unkown kind: " + value.kind());
    }

    /**
     * Converts a JDWP value object to a Maxine value object.
     *
     * @param vmValue  the value as seen by the JDWP server
     * @return a newly created value as seen by the Maxine VM
     */
    public final Value jdwpValueToMaxineValue(VMValue vmValue) {
        if (vmValue.isVoid()) {
            return VoidValue.VOID;
        } else if (vmValue.asBoolean() != null) {
            return BooleanValue.from(vmValue.asBoolean());
        } else if (vmValue.asByte() != null) {
            return ByteValue.from(vmValue.asByte());
        } else if (vmValue.asChar() != null) {
            return CharValue.from(vmValue.asChar());
        } else if (vmValue.asDouble() != null) {
            return DoubleValue.from(vmValue.asDouble());
        } else if (vmValue.asFloat() != null) {
            return FloatValue.from(vmValue.asFloat());
        } else if (vmValue.asInt() != null) {
            return IntValue.from(vmValue.asInt());
        } else if (vmValue.asLong() != null) {
            return LongValue.from(vmValue.asLong());
        } else if (vmValue.asShort() != null) {
            return ShortValue.from(vmValue.asShort());
        } else if (vmValue.asProvider() != null) {
            final Provider p = vmValue.asProvider();
            if (p instanceof TeleObject) {
                return TeleReferenceValue.from(this, ((TeleObject) p).getReference());
            }
            throw new IllegalArgumentException(
                    "Could not convert the provider object " + p
                            + " to a reference!");
        }
        throw new IllegalArgumentException("Unknown VirtualMachineValue type!");
    }

    private TeleNativeThread registeredSingleStepThread;

    public final void registerSingleStepThread(TeleNativeThread teleNativeThread) {
        if (registeredSingleStepThread != null) {
            LOGGER.warning("Overwriting registered single step thread! "
                    + registeredSingleStepThread);
        }
        registeredSingleStepThread = teleNativeThread;
    }

    private TeleNativeThread registeredStepOutThread;

    public final void registerStepOutThread(TeleNativeThread teleNativeThread) {
        if (registeredStepOutThread != null) {
            LOGGER.warning("Overwriting registered step out thread! "
                    + registeredStepOutThread);
        }
        registeredStepOutThread = teleNativeThread;
    }

    /**
     * Provides access to a VM by a JDWP server.
     * Not fully implemented
     * TeleVM might eventually implement the interfaced {@link VMAccess} directly; moving in that direction.
     *
     */
    private final class VMAccessImpl implements VMAccess {

        // Factory for creating fake object providers that represent Java objects
        // living in the JDWP server.
        private final JavaProviderFactory javaProviderFactory;

        private final Set<JdwpCodeLocation> breakpointLocations = new HashSet<JdwpCodeLocation>();

        public VMAccessImpl() {
            javaProviderFactory = new JavaProviderFactory(this, null);
        }

        public String getName() {
            return TeleVM.this.entityName();
        }

        public String getVersion() {
            return TeleVM.this.getVersion();
        }

        public String getDescription() {
            return TeleVM.this.getDescription();
        }

        public void dispose() {
            // TODO: Consider implementing disposal of the VM when told so by a JDWP
            // command.
            LOGGER.warning("Asked to DISPOSE VM, doing nothing");
        }

        public void suspend() {

            if (teleProcess.processState() == RUNNING) {
                LOGGER.info("Pausing VM...");
                try {
                    TeleVM.this.pauseVM();
                } catch (OSExecutionRequestException osExecutionRequestException) {
                    LOGGER.log(Level.SEVERE,
                            "Unexpected error while pausing the VM", osExecutionRequestException);
                } catch (InvalidVMRequestException invalidProcessRequestException) {
                    LOGGER.log(Level.SEVERE,
                            "Unexpected error while pausing the VM", invalidProcessRequestException);
                }
            } else {
                LOGGER.warning("Suspend called while VM not running!");
            }
        }

        public void resume() {

            if (teleProcess.processState() == STOPPED) {

                if (registeredSingleStepThread != null) {

                    // There has been a thread registered for performing a single
                    // step => perform single step instead of resume.
                    try {
                        LOGGER.info("Doing single step instead of resume!");
                        TeleVM.this.singleStepThread(registeredSingleStepThread, false);
                    } catch (OSExecutionRequestException osExecutionRequestException) {
                        LOGGER.log(
                                        Level.SEVERE,
                                        "Unexpected error while performing a single step in the VM",
                                        osExecutionRequestException);
                    } catch (InvalidVMRequestException e) {
                        LOGGER.log(
                                        Level.SEVERE,
                                        "Unexpected error while performing a single step in the VM",
                                        e);
                    }

                    registeredSingleStepThread = null;

                } else if (registeredStepOutThread != null
                        && registeredStepOutThread.stack().returnLocation().address() != null) {

                    // There has been a thread registered for performing a step out
                    // => perform a step out instead of resume.
                    final CodeLocation returnLocation = registeredStepOutThread.stack().returnLocation();
                    assert returnLocation != null;
                    try {
                        TeleVM.this.runToInstruction(returnLocation, false, true);
                    } catch (OSExecutionRequestException osExecutionRequestException) {
                        LOGGER.log(
                                        Level.SEVERE,
                                        "Unexpected error while performing a run-to-instruction in the VM",
                                        osExecutionRequestException);
                    } catch (InvalidVMRequestException invalidProcessRequestException) {
                        LOGGER.log(
                                        Level.SEVERE,
                                        "Unexpected error while performing a run-to-instruction in the VM",
                                        invalidProcessRequestException);
                    }

                    registeredStepOutThread = null;

                } else {

                    // Nobody registered for special commands => resume the Vm.
                    try {
                        LOGGER.info("Client tried to resume the VM!");
                        TeleVM.this.resume(false, true);
                    } catch (OSExecutionRequestException e) {
                        LOGGER.log(Level.SEVERE,
                                "Unexpected error while resuming the VM", e);
                    } catch (InvalidVMRequestException e) {
                        LOGGER.log(Level.SEVERE,
                                "Unexpected error while resuming the VM", e);
                    }
                }
            } else {
                LOGGER.severe("Client tried to resume the VM, but tele process is not in stopped state!");
            }
        }

        public void exit(int code) {
            try {
                TeleVM.this.terminateVM();
            } catch (Exception exception) {
                LOGGER.log(Level.SEVERE,
                    "Unexpected error while exidting the VM", exception);
            }
        }

        public void addListener(VMListener listener) {
            jdwpListeners.add(listener);
        }

        public void removeListener(VMListener listener) {
            jdwpListeners.remove(listener);
        }

        /**
         * Sets a breakpoint at the specified code location. This function currently has the following severe limitations:
         * Always sets the breakpoint at the call entry point of a method. Does ignore the suspendAll parameter, there will
         * always be all threads suspended when the breakpoint is hit.
         *
         * TODO: Fix the limitations for breakpoints.
         *
         * @param codeLocation specifies the code location at which the breakpoint should be set
         * @param suspendAll if true, all threads should be suspended when the breakpoint is hit
         */
        public void addBreakpoint(JdwpCodeLocation codeLocation, boolean suspendAll) {

            // For now ignore duplicates
            if (breakpointLocations.contains(codeLocation)) {
                return;
            }

            assert codeLocation.method() instanceof TeleClassMethodActor : "Only tele method actors allowed here";

            assert !breakpointLocations.contains(codeLocation);
            breakpointLocations.add(codeLocation);
            assert breakpointLocations.contains(codeLocation);
            final TeleClassMethodActor teleClassMethodActor = (TeleClassMethodActor) codeLocation.method();
            final BytecodeLocation methodCodeLocation = codeManager().createBytecodeLocation(teleClassMethodActor, 0, "");
            try {
                TeleVM.this.breakpointManager().makeBreakpoint(methodCodeLocation);
            } catch (MaxVMBusyException maxVMBusyException) {
                TeleError.unexpected("breakpoint creation failed");
            }
            Trace.line(TRACE_VALUE, tracePrefix() + "Breakpoint set at: " + methodCodeLocation);
        }

        public void removeBreakpoint(JdwpCodeLocation codeLocation) {
            if (codeLocation.isMachineCode()) {
                final MachineCodeLocation location = codeManager().createMachineCodeLocation(Address.fromLong(codeLocation.position()), "jdwp location");
                final MaxBreakpoint breakpoint = TeleVM.this.breakpointManager().findBreakpoint(location);
                if (breakpoint != null) {
                    try {
                        breakpoint.remove();
                    } catch (MaxVMBusyException maxVMBusyException) {
                        TeleError.unexpected("breakpoint removal failed");
                    }
                }
            }
            assert breakpointLocations.contains(codeLocation);
            breakpointLocations.remove(codeLocation);
            assert !breakpointLocations.contains(codeLocation);
        }

        public byte[] accessMemory(long start, int length) {
            final byte[] bytes = new byte[length];
            TeleVM.this.dataAccess().readFully(Address.fromLong(start), bytes);
            return bytes;
        }

        public VMValue createBooleanValue(boolean b) {
            return createJavaObjectValue(b, Boolean.TYPE);
        }

        public VMValue createByteValue(byte b) {
            return createJavaObjectValue(b, Byte.TYPE);
        }

        public VMValue createCharValue(char c) {
            return createJavaObjectValue(c, Character.TYPE);
        }

        public JdwpCodeLocation createCodeLocation(MethodProvider method, long position, boolean isMachineCode) {
            return new JdwpCodeLocationImpl(method, position, isMachineCode);
        }

        public VMValue createDoubleValue(double d) {
            return createJavaObjectValue(d, Double.TYPE);
        }

        public VMValue createFloatValue(float f) {
            return createJavaObjectValue(f, Float.TYPE);
        }

        public VMValue createIntValue(int i) {
            return createJavaObjectValue(i, Integer.TYPE);
        }

        public VMValue createJavaObjectValue(Object o, Class expectedClass) {
            return VMValueImpl.fromJavaObject(o, this, expectedClass);
        }

        public VMValue createLongValue(long l) {
            return VMValueImpl.fromJavaObject(l, this, Long.TYPE);
        }

        public VMValue createObjectProviderValue(ObjectProvider p) {
            return createJavaObjectValue(p, null);
        }

        public VMValue createShortValue(short s) {
            return VMValueImpl.fromJavaObject(s, this, Short.TYPE);
        }

        public StringProvider createString(String s) {
            final VMValue vmValue = createJavaObjectValue(s, String.class);
            assert vmValue.asProvider() != null : "Must be a provider value object";
            assert vmValue.asProvider() instanceof StringProvider : "Must be a String provider object";
            return (StringProvider) vmValue.asProvider();
        }

        public TargetMethodAccess[] findTargetMethods(long[] addresses) {
            final TargetMethodAccess[] result = new TargetMethodAccess[addresses.length];
            for (int i = 0; i < addresses.length; i++) {
                result[i] = TeleVM.this.codeCache().findCompiledCode(Address.fromLong(addresses[i])).teleTargetMethod();
            }
            return result;
        }

        public ReferenceTypeProvider[] getAllReferenceTypes() {
            return teleClassRegistry.teleClassActors();
        }

        public ThreadProvider[] getAllThreads() {
            final Collection<TeleNativeThread> threads = teleProcess().threads();
            final ThreadProvider[] threadProviders = new ThreadProvider[threads.size()];
            return threads.toArray(threadProviders);
        }

        public String[] getBootClassPath() {
            return Classpath.bootClassPath().toStringArray();
        }

        public String[] getClassPath() {
            return HostedBootClassLoader.HOSTED_BOOT_CLASS_LOADER.classpath().toStringArray();
        }

        /**
         * Looks up a JDWP reference type object based on a Java class object.
         *
         * @param klass
         *            the class object whose JDWP reference type should be looked up
         * @return a JDWP reference type representing the Java class
         */
        public ReferenceTypeProvider getReferenceType(Class klass) {
            ReferenceTypeProvider referenceTypeProvider = null;

            // Always fake the Object class, otherwise try to find a class in the
            // Maxine VM that matches the signature.
            if (!klass.equals(Object.class)) {
                referenceTypeProvider = TeleVM.this.classRegistry().findTeleClassActor(klass);
            }

            // If no class was found within the Maxine VM, create a faked reference
            // type object.
            if (referenceTypeProvider == null) {
                LOGGER.info("Creating Java provider for class " + klass);
                referenceTypeProvider = javaProviderFactory.getReferenceTypeProvider(klass);
            }
            return referenceTypeProvider;
        }

        public ReferenceTypeProvider[] getReferenceTypesBySignature(String signature) {

            // Always fake the Object type. This means that calls to all methods of
            // the Object class will be reflectively delegated to the Object class
            // that lives
            // on the Tele side not to the Object class in the VM.
            if (signature.equals("Ljava/lang/Object;")) {
                return new ReferenceTypeProvider[] {getReferenceType(Object.class)};
            }

            // Try to find a matching class actor that lives within the VM based on
            // the signature.
            final List<ReferenceTypeProvider> result = new LinkedList<ReferenceTypeProvider>();
            for (TypeDescriptor typeDescriptor : TeleVM.this.classRegistry().typeDescriptors()) {
                if (typeDescriptor.toString().equals(signature)) {
                    final TeleClassActor teleClassActor = TeleVM.this.classRegistry().findTeleClassActor(typeDescriptor);

                    // Do not include array types, there should always be faked in
                    // order to be able to call newInstance on them. Arrays that are
                    // created this way then do
                    // not really live within the VM, but on the JDWP server side.
                    if (!(teleClassActor instanceof TeleArrayClassActor)) {
                        result.add(teleClassActor);
                    }
                }
            }

            // If no class living in the VM was found, try to lookup Java class
            // known to the JDWP server. If such a class is found, then a JDWP
            // reference type is faked for it.
            if (result.size() == 0) {
                try {
                    final Class klass = JavaTypeDescriptor.resolveToJavaClass(
                            JavaTypeDescriptor.parseTypeDescriptor(signature), getClass().getClassLoader());
                    result.add(javaProviderFactory.getReferenceTypeProvider(klass));
                } catch (NoClassDefFoundError noClassDefFoundError) {
                    LOGGER.log(Level.SEVERE,
                            "Error while looking up class based on signature", noClassDefFoundError);
                }
            }

            return result.toArray(new ReferenceTypeProvider[result.size()]);
        }

        public ThreadGroupProvider[] getThreadGroups() {
            return new ThreadGroupProvider[] {javaThreadGroupProvider, nativeThreadGroupProvider};
        }

        public VMValue getVoidValue() {
            return VMValueImpl.VOID_VALUE;
        }
    }

}

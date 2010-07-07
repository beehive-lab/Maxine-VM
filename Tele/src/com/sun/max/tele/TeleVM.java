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
package com.sun.max.tele;

import static com.sun.max.tele.debug.ProcessState.*;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.util.logging.*;

import com.sun.max.*;
import com.sun.max.ide.*;
import com.sun.max.jdwp.vm.core.*;
import com.sun.max.jdwp.vm.proxy.*;
import com.sun.max.jdwp.vm.proxy.VMValue.Type;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.program.Classpath.*;
import com.sun.max.program.option.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.debug.darwin.*;
import com.sun.max.tele.debug.linux.*;
import com.sun.max.tele.debug.no.*;
import com.sun.max.tele.debug.solaris.*;
import com.sun.max.tele.field.*;
import com.sun.max.tele.grip.*;
import com.sun.max.tele.interpreter.*;
import com.sun.max.tele.jdwputil.*;
import com.sun.max.tele.method.*;
import com.sun.max.tele.method.CodeLocation.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.reference.*;
import com.sun.max.tele.type.*;
import com.sun.max.tele.util.*;
import com.sun.max.tele.value.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.debug.*;
import com.sun.max.vm.grip.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.object.host.*;
import com.sun.max.vm.prototype.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.reference.prototype.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.tele.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Implementation of remote access to an instance of the Maxine VM.
 * Access from the Inspector or other clients of this implementation
 * gain access through the {@link MaxVM} interface.
 * <br>
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
 *
 * @author Bernd Mathiske
 * @author Athul Acharya
 * @author Michael Van De Vanter
 * @author Doug Simon
 * @author Thomas Wuerthinger
 * @author Hannes Payer
 * @author Mick Jordan
 */
public abstract class TeleVM implements MaxVM {

    private static final int TRACE_VALUE = 1;

    private static final String PROGRAM_NAME = "maxvm";

    private static final String TELE_LIBRARY_NAME = "tele";

    /**
     * Some configurations of the Inspector (tcp-based remote) do not need the tele library locally.
     */
    private static final String NO_TELE_PROPERTY = "max.ins.no.tele";

    /**
     * Modes in which the Inspector operates, which require different startup behavior.
     * @author Mick Jordan
     *
     */
    public enum Mode {
        /**
         * Create and start a new process to execute the VM, passing arguments.
         */
        CREATE,
        /**
         * Attach to an existing VM process.
         */
        ATTACH,
        /**
         * Browse a VM image as produced by the {@link BootImageGenerator}.
         */
        IMAGE,
        /**
         * Attach to a dump of a VM process.
         */
        DUMP
    }

    private static Mode mode;

    /**
     * The options controlling how a tele VM instance is {@linkplain #newAllocator(String...) created}.
     */
    public static class Options extends OptionSet {

        /**
         * Specifies if these options apply when creating a {@linkplain TeleVM#createReadOnly(File, Classpath) read-only} Tele VM.
         */
        public final Option<String> modeOption = newStringOption("mode", "create",
            "Mode of operation: create | attach | image | dump");
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

        /**
         * This field is {@code null} if {@link #readOnly} is {@code false}.
         */
        public final Option<String> heapOption;

        /**
         * This field is {@code null} if {@link #readOnly} is {@code true}.
         */
        public final Option<String> vmArguments;

        /**
         * This field is {@code null} if {@link #readOnly} is {@code true}.
         */
        public final Option<Integer> debuggeeIdOption;

        /**
         * Creates command line options that are specific to certain operation modes. No longer tries to customise the
         * options based on mode.
         */
        public Options() {
            heapOption = newStringOption("heap", null, "Relocation address for the heap and code in the boot image.");
            vmArguments = newStringOption("a", "", "Specifies the arguments to the target VM.");
            debuggeeIdOption = newIntegerOption("id", -1, "Process id of VM instance to which this debugger should attach");
        }
    }

    private static boolean needTeleLibrary() {
        return System.getProperty(NO_TELE_PROPERTY) == null;
    }

    public static boolean isAttaching() {
        return mode == Mode.DUMP || mode == Mode.ATTACH;
    }

    /**
     * Creates a new VM instance based on a given set of options.
     *
     * @param options the options controlling specifics of the VM instance to be created
     * @return a new VM instance
     */
    public static TeleVM create(Options options) throws BootImageException {
        HostObjectAccess.setMainThread(Thread.currentThread());

        mode = Mode.valueOf(options.modeOption.getValue().toUpperCase());

        final String logLevel = options.logLevelOption.getValue();
        try {
            LogManager.getLogManager().getLogger("").setLevel(Level.parse(logLevel));
        } catch (IllegalArgumentException e) {
            ProgramWarning.message("Invalid level specified for java.util.logging root logger: " + logLevel + " [using " + Level.SEVERE + "]");
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
        File vmdir = options.vmDirectoryOption.getValue();
        classpathPrefix = classpathPrefix.prepend(BootImageGenerator.getBootImageJarFile(vmdir).getAbsolutePath());
        checkClasspath(classpathPrefix);
        final Classpath classpath = Classpath.fromSystem().prepend(classpathPrefix);
        HostedBootClassLoader.setClasspath(classpath);

        if (needTeleLibrary()) {
            Prototype.loadLibrary(TELE_LIBRARY_NAME);
        }
        final File bootImageFile = BootImageGenerator.getBootImageFile(vmdir);

        Classpath sourcepath = JavaProject.getSourcePath(true);
        final List<String> sourcepathList = options.sourcepathOption.getValue();
        if (sourcepathList != null) {
            sourcepath = sourcepath.prepend(new Classpath(sourcepathList.toArray(new String[sourcepathList.size()])));
        }
        checkClasspath(sourcepath);

        switch (mode) {
            case CREATE:
                final String value = options.vmArguments.getValue();
                final String[] commandLineArguments = "".equals(value) ? new String[0] : value.trim().split(" ");
                // Guest VM CREATE is more like ATTACH in that it needs the process id, but also needs to be advanced to entry point
                vm = create(bootImageFile, sourcepath, commandLineArguments, options.debuggeeIdOption.getValue());
                vm.lock();
                try {
                    vm.updateVMCaches();
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
            case DUMP:
                /* The fundamental difference in this mode is that VM has executed for a while.
                 * This means that boot heap relocation has (almost certainly) been performed
                 * AND the boot heap will contain references to the dynamic heap.
                 * So the delicate dance that us normally performed when setting up the
                 * TeleClassRegistry is neither entirely necessary nor sufficient.
                 * The is handled by doing two passes over the class registry and
                 * deferring resolution of those references that are outside the boot heap
                 * until the second pass, after the TeleHeap is fully initialized.
                 * We also need to explicitly refresh the threads and update state.
                 *
                 */
                vm = create(bootImageFile, sourcepath, null, options.debuggeeIdOption.getValue());
                vm.lock();
                try {
                    vm.updateVMCaches();
                    vm.teleProcess().initializeStateOnAttach();
                } finally {
                    vm.unlock();
                }
                break;

            case IMAGE:
                String heap = options.heapOption.getValue();
                if (heap != null) {
                    assert System.getProperty(ReadOnlyTeleProcess.HEAP_PROPERTY) == null;
                    System.setProperty(ReadOnlyTeleProcess.HEAP_PROPERTY, heap);
                }
                vm = createReadOnly(bootImageFile, sourcepath);
                vm.updateVMCaches();
        }

        final File commandFile = options.commandFileOption.getValue();
        if (commandFile != null && !commandFile.equals("")) {
            vm.executeCommandsFromFile(commandFile.getPath());
        }

        return vm;
    }

    public static Mode mode() {
        return mode;
    }

    /**
     * Create the appropriate subclass of {@link TeleVM} based on VM configuration.
     *
     * @param bootImageFile
     * @param sourcepath
     * @param commandlineArguments {@code null} if {@code processId > 0} else command line arguments for new VM process
     * @param processID {@code -1} for new VM process, else id of process to attach to
     * @return
     * @throws BootImageException
     */
    private static TeleVM create(File bootImageFile, Classpath sourcepath, String[] commandlineArguments, int processID) throws BootImageException {
        final BootImage bootImage = new BootImage(bootImageFile);
        TeleVM teleVM = null;
        switch (bootImage.vmConfiguration.platform().operatingSystem) {
            case DARWIN:
                teleVM = new DarwinTeleVM(bootImageFile, bootImage, sourcepath, commandlineArguments, processID);
                break;
            case LINUX:
                teleVM = new LinuxTeleVM(bootImageFile, bootImage, sourcepath, commandlineArguments, processID);
                break;
            case SOLARIS:
                teleVM = new SolarisTeleVM(bootImageFile, bootImage, sourcepath, commandlineArguments, processID);
                break;
            case GUESTVM:
                try {
                    final Class< ? > klass = Class.forName("com.sun.max.tele.debug.guestvm.GuestVMTeleVM");
                    final Constructor< ? > cons = klass.getDeclaredConstructor(new Class[] {File.class, BootImage.class, Classpath.class, String[].class, int.class});
                    teleVM = (TeleVM) cons.newInstance(new Object[] {bootImageFile, bootImage, sourcepath, commandlineArguments, processID});
                } catch (Exception ex) {
                    FatalError.unexpected("failed to instantiate TeleVM class for GuestVM", ex);
                }
                break;
            default:
                FatalError.unimplemented();
        }
        return teleVM;
    }

    private static void checkClasspath(Classpath classpath) {
        for (Entry classpathEntry : classpath.entries()) {
            if (classpathEntry.isPlainFile()) {
                ProgramWarning.message("Class path entry is neither a directory nor a JAR file: " + classpathEntry);
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
        return new ReadOnlyTeleVM(bootImageFile, bootImage, sourcepath);
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


    private static VMPackage getInspectorGripPackage(VMPackage gripPackage) {
        final MaxPackage vmGripRootPackage = new com.sun.max.vm.grip.Package();
        final String suffix = gripPackage.name().substring(vmGripRootPackage.name().length());
        final MaxPackage inspectorGripRootPackage = new com.sun.max.tele.grip.Package();
        return (VMPackage) MaxPackage.fromName(inspectorGripRootPackage.name() + suffix);
    }

    private static MaxineVM createVM(BootImage bootImage) {
        final VMConfiguration b = bootImage.vmConfiguration;
        final VMConfiguration vmConfiguration = new VMConfiguration(
                b.buildLevel(),
                b.platform(),
                getInspectorGripPackage(b.gripPackage),
                new com.sun.max.tele.reference.plain.Package(),
                b.layoutPackage, b.heapPackage, b.monitorPackage,
                b.bootCompilerPackage, b.jitCompilerPackage, null, b.trampolinePackage, b.targetABIsPackage,
                b.runPackage);
        vmConfiguration.loadAndInstantiateSchemes(true);

        final MaxineVM vm = new MaxineVM(vmConfiguration);
        MaxineVM.setTarget(vm);
        MaxineVM.setGlobalHostOrTarget(vm);
        new JavaPrototype(vm.configuration, false);
        return vm;
    }

    private String  tracePrefix() {
        return "[TeleVM: " + Thread.currentThread().getName() + "] ";
    }

    private final VMConfiguration vmConfiguration;

    private final Size wordSize;

    private final Size pageSize;

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
    private volatile TeleVMState teleVMState = TeleVMState.NONE;

    private List<MaxVMStateListener> vmStateListeners = new CopyOnWriteArrayList<MaxVMStateListener>();

    private List<MaxGCStartedListener> gcStartedListeners = new CopyOnWriteArrayList<MaxGCStartedListener>();

    /**
     * Active breakpoint that triggers at start of GC if {@link gcStartedListeners} is non-empty; null if no listeners.
     */
    private MaxBreakpoint gcStartedBreakpoint = null;

    private List<MaxGCCompletedListener> gcCompletedListeners = new CopyOnWriteArrayList<MaxGCCompletedListener>();

    /**
     * An always active breakpoint that triggers at end of GC if {@link gcCompletedListeners} is non-empty; null if no listeners.
     */
    private MaxBreakpoint gcCompletedBreakpoint = null;

    private List<MaxMemoryRegion> allMemoryRegions = new ArrayList<MaxMemoryRegion>(0);

    private final TeleProcess teleProcess;

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

    private final Classpath sourcepath;

    /**
     * Classes, possibly not loaded, available on the classpath.
     * Lazily initialized; can re re-initialized.
     * @see #updateLoadableTypeDescriptorsFromClasspath()
     */
    private Set<TypeDescriptor> typesOnClasspath;

    /**
     * @return classes, possibly loaded, not available on the classpath.
     */
    private Set<TypeDescriptor> typesOnClasspath() {
        if (typesOnClasspath == null) {
            // Delayed initialization, because this can take some time.
            updateLoadableTypeDescriptorsFromClasspath();
        }
        return typesOnClasspath;
    }

    private int interpreterUseLevel = 0;

    private TeleClassRegistry teleClassRegistry;

    private final TimedTrace updateTracer;

    /**
     * A lock designed to keep all non-thread-safe client calls from being handled during the VM setup/execute/refresh cycle.
     */
    private ReentrantLock lock = new ReentrantLock();

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
    protected TeleVM(File bootImageFile, BootImage bootImage, Classpath sourcepath, String[] commandLineArguments, int processID, TeleVMAgent agent) throws BootImageException {
        final TimedTrace tracer = new TimedTrace(TRACE_VALUE, tracePrefix() + " creating");
        tracer.begin();
        this.bootImageFile = bootImageFile;
        this.bootImage = bootImage;
        this.sourcepath = sourcepath;
        if (needTeleLibrary()) {
            nativeInitialize(bootImage.header.threadLocalsAreaSize);
        }
        final MaxineVM vm = createVM(this.bootImage);
        this.vmConfiguration = vm.configuration;

        this.updateTracer = new TimedTrace(TRACE_VALUE, tracePrefix() + " updating all");

        // Pre-initialize an appropriate disassembler to save time.
        TeleDisassembler.initialize(vmConfiguration.platform().processorKind);

        this.wordSize = Size.fromInt(vmConfiguration.platform().processorKind.dataModel.wordWidth.numberOfBytes);
        this.pageSize = Size.fromInt(vmConfiguration.platform.pageSize);
        this.programFile = new File(bootImageFile.getParent(), PROGRAM_NAME);

        if (commandLineArguments == null) {
            this.teleProcess = attachToTeleProcess(processID);
            switch (bootImage.vmConfiguration.platform().operatingSystem) {
                case GUESTVM:
                    this.bootImageStart = loadBootImage(agent);
                    break;
                default:
                    FatalError.unexpected("need to get the boot image address from attached process somehow");
                    this.bootImageStart = Pointer.zero();
            }
        } else {
            if (agent != null) {
                agent.start();
            }
            try {
                this.teleProcess = createTeleProcess(commandLineArguments, agent);
                this.bootImageStart = loadBootImage(agent);
            } catch (BootImageException e) {
                if (agent != null) {
                    agent.close();
                }
                throw e;
            }
        }

        if (!tryLock()) {
            ProgramError.unexpected("unable to lock during creation");
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

        final TeleGripScheme teleGripScheme = (TeleGripScheme) vmConfiguration.gripScheme();
        teleGripScheme.setTeleVM(this);
        this.threadManager = new TeleThreadManager(this);
        this.codeManager = new CodeManager(this);
        this.bytecodeBreakpointManager = new TeleBytecodeBreakpoint.BytecodeBreakpointManager(this);
        this.teleBreakpointManager = new TeleBreakpointManager(this, this.bytecodeBreakpointManager);
        this.watchpointManager = teleProcess.getWatchpointManager();

        tracer.end(null);
    }

    /**
     * Updates information about the state of the VM that is read
     * and cached at the end of each VM execution cycle.
     * <br>
     * This must be called in a context where thread-safe read access to the VM can
     * be achieved.
     * <br>
     * Some lazy initialization is done, in order to avoid cycles during startup.
     *
     * @throws ProgramError if unable to acquire the VM lock
     * @see #lock
     */
    public final void updateVMCaches() {
        if (!tryLock()) {
            ProgramError.unexpected("TeleVM unable to acquire VM lock for update");
        }
        try {
            updateTracer.begin();
            if (teleClassRegistry == null) {
                /*
                 * Must delay creation/initialization of the {@link TeleClassRegistry} until after
                 * we hit the first execution breakpoint; otherwise addresses won't have been relocated.
                 * This depends on the {@link TeleHeap} already existing.
                 */
                teleClassRegistry = new TeleClassRegistry(this);
                /*
                 *  Can only fully initialize the {@link TeleHeap} once
                 *  the {@TeleClassRegistry} is fully initialized, otherwise there's a cycle.
                 */
                heap.initialize();

                // Now set up the map of the compiled code cache
                teleCodeCache = new TeleCodeCache(this);
                teleCodeCache.initialize();
                if (isAttaching()) {
                    // Check that the target was run with option MakeInspectable otherwise the dynamic heap info will not be available
                    ProgramError.check((teleFields().Inspectable_flags.readInt(this) & Inspectable.INSPECTED) != 0, "target VM was not run with -XX:+MakeInspectable option");
                    teleClassRegistry.processAttachFixupList();
                }
            }
            heap.updateCache();
            teleClassRegistry.updateCache();
            heap.updateObjectCache();
            teleCodeCache.updateCache();
            updateTracer.end(null);
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

    public final String getVersion() {
        return MaxineVM.VERSION;
    }

    public final String getDescription() {
        return MaxineVM.description();
    }

    public final VMConfiguration vmConfiguration() {
        return vmConfiguration;
    }

    public final Size wordSize() {
        return wordSize;
    }

    public final Size pageSize() {
        return pageSize;
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

    public void addGCStartedListener(MaxGCStartedListener listener) throws MaxVMBusyException {
        assert listener != null;
        gcStartedListeners.add(listener);
        if (!gcStartedListeners.isEmpty() && gcStartedBreakpoint == null) {
            final VMTriggerEventHandler triggerEventHandler = new VMTriggerEventHandler() {

                public boolean handleTriggerEvent(TeleNativeThread teleNativeThread) {
                    Trace.line(TRACE_VALUE, tracePrefix() + " updating GCStartedListeners");
                    for (MaxGCStartedListener listener : gcStartedListeners) {
                        listener.gcStarted();
                    }
                    return false;
                }
            };
            try {
                gcStartedBreakpoint = teleProcess.targetBreakpointManager().makeSystemBreakpoint(teleMethods.gcStarted(), triggerEventHandler);
                gcStartedBreakpoint.setDescription("Internal breakpoint, just after start of GC, to notify listeners");
            } catch (MaxVMBusyException maxVMBusyException) {
                gcStartedListeners.remove(listener);
                throw maxVMBusyException;
            }
        }
    }

    public void removeGCStartedListener(MaxGCStartedListener listener) throws MaxVMBusyException {
        assert listener != null;
        gcStartedListeners.remove(listener);
        if (gcStartedListeners.isEmpty() && gcStartedBreakpoint != null) {
            try {
                gcStartedBreakpoint.remove();
            } catch (MaxVMBusyException maxVMBusyException) {
                gcStartedListeners.add(listener);
                throw maxVMBusyException;
            }
            gcStartedBreakpoint = null;
        }
    }

    public void addGCCompletedListener(MaxGCCompletedListener listener) throws MaxVMBusyException {
        assert listener != null;
        gcCompletedListeners.add(listener);
        if (!gcCompletedListeners.isEmpty() && gcCompletedBreakpoint == null) {
            final VMTriggerEventHandler triggerEventHandler = new VMTriggerEventHandler() {

                public boolean handleTriggerEvent(TeleNativeThread teleNativeThread) {
                    Trace.line(TRACE_VALUE, tracePrefix() + " updating GCCompletedListeners");
                    for (MaxGCCompletedListener listener : gcCompletedListeners) {
                        listener.gcCompleted();
                    }
                    return false;
                }
            };
            try {
                gcCompletedBreakpoint = teleProcess.targetBreakpointManager().makeSystemBreakpoint(teleMethods.gcCompleted(), triggerEventHandler);
                gcCompletedBreakpoint.setDescription("Internal breakpoint, just after end of GC, to notify listeners");
            } catch (MaxVMBusyException maxVMBusyException) {
                gcCompletedListeners.remove(listener);
                throw maxVMBusyException;
            }
        }
    }

    public void removeGCCompletedListener(MaxGCCompletedListener listener) throws MaxVMBusyException {
        assert listener != null;
        gcCompletedListeners.remove(listener);
        if (gcCompletedListeners.isEmpty() && gcCompletedBreakpoint != null) {
            try {
                gcCompletedBreakpoint.remove();
            } catch (MaxVMBusyException maxVMBusyException) {
                gcCompletedListeners.add(listener);
                throw maxVMBusyException;
            }
            gcCompletedBreakpoint = null;
        }
    }

    public final MaxMemoryRegion findMemoryRegion(Address address) {
        for (MaxMemoryRegion memoryRegion : state().memoryRegions()) {
            if (memoryRegion != null && memoryRegion.contains(address)) {
                return memoryRegion;
            }
        }
        return null;
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

    /**
     * Determines whether the calling thread holds the VM lock.
     * <br>
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

    public void acquireLegacyVMAccess() throws MaxVMBusyException {
        if (!tryLock()) {
            throw new MaxVMBusyException();
        }
    }

    public void releaseLegacyVMAccess() {
        assert lockHeldByCurrentThread();
        unlock();
    }

    /**
     * Initializes native tele code.
     *
     * @param threadLocalsSize the size of thread local storage as read from the image
     */
    private static native void nativeInitialize(int threadLocalsSize);

    /**
     * Sets or clears some bits of the {@link Inspectable#flags} field in the VM process.
     * <br>
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
    protected abstract TeleProcess createTeleProcess(String[] commandLineArguments, TeleVMAgent agent) throws BootImageException;

    protected TeleProcess attachToTeleProcess(int processID) {
        throw FatalError.unimplemented();
    }

    /**
     * Gets a pointer to the boot image in the remote VM. The implementation of this method in the VM uses a
     * provided agent to receive the address from the VM via a socket.
     *
     * @throws BootImageException if the address of the boot image could not be obtained
     */
    protected Pointer loadBootImage(TeleVMAgent agent) throws BootImageException {
        try {
            final Socket socket = agent.waitForVM();
            final InputStream stream = socket.getInputStream();
            final Endianness endianness = vmConfiguration.platform().processorKind.dataModel.endianness;
            final Pointer heap = Word.read(stream, endianness).asPointer();
            Trace.line(1, "Received boot image address from VM: 0x" + heap.toHexString());
            socket.close();
            agent.close();
            return heap;
        } catch (IOException ioException) {
            throw new BootImageException("Error while reading boot image address from VM process", ioException);
        }
    }

    private static void addNonNull(ArrayList<MaxMemoryRegion> regions, MaxMemoryRegion region) {
        if (region != null) {
            regions.add(region);
        }
    }

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
        final ArrayList<MaxMemoryRegion> memoryRegions = new ArrayList<MaxMemoryRegion>(teleVMState.memoryRegions().size());
        for (MaxHeapRegion heapRegion : heap.heapRegions()) {
            addNonNull(memoryRegions, heapRegion.memoryRegion());
        }
        if (heap.rootsMemoryRegion() != null) {
            addNonNull(memoryRegions, heap.rootsMemoryRegion());
        }
        for (MaxThread thread : threads) {
            addNonNull(memoryRegions, thread.stack().memoryRegion());
            addNonNull(memoryRegions, thread.localsBlock().memoryRegion());
        }
        for (MaxCompiledCodeRegion compiledCodeRegion : teleCodeCache.compiledCodeRegions()) {
            addNonNull(memoryRegions, compiledCodeRegion.memoryRegion());
        }

        this.teleVMState = new TeleVMState(processState,
            epoch,
            memoryRegions,
            threads,
            singleStepThread,
            threadsStarted,
            threadsDied,
            breakpointEvents,
            teleWatchpointEvent,
            heap.isInGC(),
            teleVMState);
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

    public TeleGripScheme gripScheme() {
        return (TeleGripScheme) vmConfiguration.gripScheme();
    }

    /**
     * @return the scheme used to manage object layouts in this VM.
     */
    public final LayoutScheme layoutScheme() {
        return vmConfiguration.layoutScheme();
    }

    /**
     * @return access to low-level reading and writing of memory in the VM.
     */
    public final DataAccess dataAccess() {
        return teleProcess.dataAccess();
    }

    public final Word readWord(Address address) {
        return teleProcess.dataAccess().readWord(address);
    }

    public final Word readWord(Address address, int offset) {
        return teleProcess.dataAccess().readWord(address, offset);
    }

    public final Word readWord(Address address, Offset offset) {
        return teleProcess.dataAccess().readWord(address, offset);
    }

    public final void readFully(Address address, byte[] bytes) {
        teleProcess.dataAccess().readFully(address, bytes);
    }

    private RemoteTeleGrip createTemporaryRemoteTeleGrip(Word rawGrip) {
        return gripScheme().createTemporaryRemoteTeleGrip(rawGrip.asAddress());
    }

    private RemoteTeleGrip temporaryRemoteTeleGripFromOrigin(Word origin) {
        return gripScheme().temporaryRemoteTeleGripFromOrigin(origin);
    }

    public final Reference originToReference(final Pointer origin) {
        return vmConfiguration.referenceScheme().fromGrip(gripScheme().fromOrigin(origin));
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
                final Pointer cell = layoutScheme().generalLayout.originToCell(origin);
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
            Word hubWord = layoutScheme().generalLayout.readHubReferenceAsWord(temporaryRemoteTeleGripFromOrigin(origin));
            for (int i = 0; i < 3; i++) {
                final RemoteTeleGrip hubGrip = createTemporaryRemoteTeleGrip(hubWord);
                final Pointer hubOrigin = hubGrip.toOrigin();
                if (!heap().contains(hubOrigin) && !codeCache().contains(hubOrigin)) {
                    return false;
                }
                final Word nextHubWord = layoutScheme().generalLayout.readHubReferenceAsWord(hubGrip);
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
     * <br>
     * Note that this predicate is not precise; it may very rarely return a false positive.
     * <br>
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
        Word staticHubWord = layoutScheme().generalLayout.readHubReferenceAsWord(temporaryRemoteTeleGripFromOrigin(origin));
        final RemoteTeleGrip staticHubGrip = createTemporaryRemoteTeleGrip(staticHubWord);
        final Pointer staticHubOrigin = staticHubGrip.toOrigin();
        if (!heap().contains(staticHubOrigin) && !codeCache().contains(staticHubOrigin)) {
            return false;
        }
        // If we really have a {@link StaticHub}, then a known field points at a {@link ClassActor}.
        final int hubClassActorOffset = teleFields().Hub_classActor.fieldActor().offset();
        final Word classActorWord = dataAccess().readWord(staticHubOrigin, hubClassActorOffset);
        final RemoteTeleGrip classActorGrip = createTemporaryRemoteTeleGrip(classActorWord);
        final Pointer classActorOrigin = classActorGrip.toOrigin();
        if (!heap().contains(classActorOrigin) && !codeCache().contains(classActorOrigin)) {
            return false;
        }
        // If we really have a {@link ClassActor}, then a known field points at the {@link StaticTuple} for the class.
        final int classActorStaticTupleOffset = teleFields().ClassActor_staticTuple.fieldActor().offset();
        final Word staticTupleWord = dataAccess().readWord(classActorOrigin, classActorStaticTupleOffset);
        final RemoteTeleGrip staticTupleGrip = createTemporaryRemoteTeleGrip(staticTupleWord);
        final Pointer staticTupleOrigin = staticTupleGrip.toOrigin();
        // If we really started with a {@link StaticTuple}, then this field will point at it
        return staticTupleOrigin.equals(origin);
    }

    private boolean isValidGrip(Grip grip) {
//        if (isInGC()) {
//            final TeleGrip teleGrip = (TeleGrip) grip;
//            if (teleGrip instanceof MutableTeleGrip) {
//                // Assume invalid during GC.
//                return false;//TODO: check for forwarding pointer
//            }
//        }
        if (grip instanceof LocalTeleGrip) {
            return true;
        }
        return isValidOrigin(grip.toOrigin());
    }

    public final boolean isValidReference(Reference reference) {
        return isValidGrip(reference.toGrip());
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
    private void checkReference(Reference reference) throws InvalidReferenceException {
        if (!isValidOrigin(reference.toGrip().toOrigin())) {
            throw new InvalidReferenceException(reference);
        }
    }

    public final Reference wordToReference(Word word) {
        return vmConfiguration.referenceScheme().fromGrip(gripScheme().fromOrigin(word.asPointer()));
    }

    /**
     * Creates a temporary reference for access to VM memory without invoking the
     * canonicalization machinery.
     *
     * @return a reference to a location in VM memory that is not safe across GC
     */
    public final Reference wordToTemporaryReference(Address address) {
        return vmConfiguration.referenceScheme().fromGrip(gripScheme().createTemporaryRemoteTeleGrip(address));
    }

    /**
     * @param reference a {@link Reference} to memory in the VM.
     * @param index offset into an array of references
     * @return the contents of the array at the index, interpreted as an address and wrapped in a Reference.
     * @throws InvalidReferenceException (unchecked)
     */
    public final Reference readReference(Reference reference, int index) throws InvalidReferenceException {
        checkReference(reference);
        return wordToReference(layoutScheme().wordArrayLayout.getWord(reference, index));
    }

    /**
     * Returns a local copy of the contents of a {@link String} object in the VM's heap.
     *
     * @param stringReference A {@link String} object in the VM.
     * @return A local {@link String} representing the object's contents.
     * @throws InvalidReferenceException if the argument does not point a valid heap object.
     */
    public final String getString(Reference stringReference)  throws InvalidReferenceException {
        checkReference(stringReference);
        final Reference valueReference = teleFields().String_value.readReference(stringReference);
        checkReference(valueReference);
        int offset = teleFields().String_offset.readInt(stringReference);
        final int count = teleFields().String_count.readInt(stringReference);
        final char[] chars = new char[count];
        final CharArrayLayout charArrayLayout = layoutScheme().charArrayLayout;
        for (int i = 0; i < count; i++) {
            chars[i] = charArrayLayout.getChar(valueReference, offset);
            offset++;
        }
        return new String(chars);
    }

    /**
     * Returns a local copy of the contents of a {@link String} object in the VM's heap,
     * using low level mechanisms and performing no checking that the location
     * or object are valid.
     * <br>
     * The intention is to provide a fast, low-level mechanism for reading strings that
     * can be used outside of the AWT event thread without danger of deadlock,
     * for example on the canonical grip machinery.
     *
     * @param stringReference a {@link String} object in the VM
     * @return A local {@link String} representing the remote object's contents, null if it can't be read.
     */
    public final String getStringUnsafe(Reference stringReference) {
        // Work only with temporary grips that are unsafe across GC
        // Do no testing to determine if the reference points to a valid String object in live memory.
        try {
            final RemoteTeleGrip stringGrip = temporaryRemoteTeleGripFromOrigin(stringReference.toOrigin());
            final Word valueWord = stringGrip.readWord(teleFields().String_value.fieldActor().offset());
            final RemoteTeleGrip valueGrip = createTemporaryRemoteTeleGrip(valueWord);
            int offset = stringGrip.readInt(teleFields.String_offset.fieldActor().offset());
            final int count = stringGrip.readInt(teleFields.String_count.fieldActor().offset());
            final char[] chars = new char[count];
            final CharArrayLayout charArrayLayout = layoutScheme().charArrayLayout;
            for (int i = 0; i < count; i++) {
                chars[i] = charArrayLayout.getChar(valueGrip, offset);
                offset++;
            }
            return new String(chars);
        } catch (DataIOError dataIOError) {
            return null;
        }
    }

    /**
     * Gets a canonical local {@link ClassActor} for the named class, creating one if needed by loading the class from
     * the classpath using the {@link HostedBootClassLoader#HOSTED_BOOT_CLASS_LOADER}.
     *
     * @param name the name of a class
     * @return Local {@link ClassActor} corresponding to the class, possibly created by loading it from classpath.
     * @throws ClassNotFoundException if not already loaded and unavailable on the classpath.
     */
    private ClassActor makeClassActor(String name) throws ClassNotFoundException {
        // The VM registry includes all ClassActors for classes loaded locally
        // using the prototype class loader
        HostedBootClassLoader classLoader = HostedBootClassLoader.HOSTED_BOOT_CLASS_LOADER;
        synchronized (classLoader) {
            ClassActor classActor = ClassRegistry.BOOT_CLASS_REGISTRY.get(JavaTypeDescriptor.getDescriptorForJavaString(name));
            if (classActor == null) {
                // Try to load the class from the local classpath.
                if (name.endsWith("[]")) {
                    classActor = ClassActorFactory.createArrayClassActor(makeClassActor(name.substring(0, name.length() - 2)));
                } else {
                    classActor = classLoader.makeClassActor(
                                    JavaTypeDescriptor.getDescriptorForWellFormedTupleName(name));
                }
            }
            return classActor;
        }
    }

    /**
     * Gets a canonical local {@link ClassActor} corresponding to a
     * {@link ClassActor} in the VM, creating one if needed by
     * loading the class using the
     * {@link HostedBootClassLoader#HOSTED_BOOT_CLASS_LOADER} from either the
     * classpath, or if not found on the classpath, by copying the classfile
     * from the VM.
     *
     * @param classActorReference  a {@link ClassActor} in the VM.
     * @return Local, equivalent {@link ClassActor}, possibly created by
     *         loading from the classpath, or if not found, by copying and
     *         loading the classfile from the VM.
     * @throws InvalidReferenceException if the argument does not point to a valid heap object in the VM.
     */
    public final ClassActor makeClassActor(Reference classActorReference) throws InvalidReferenceException {
        checkReference(classActorReference);
        final Reference utf8ConstantReference = teleFields().Actor_name.readReference(classActorReference);
        checkReference(utf8ConstantReference);
        final Reference stringReference = teleFields().Utf8Constant_string.readReference(utf8ConstantReference);
        final String name = getString(stringReference);
        try {
            return makeClassActor(name);
        } catch (ClassNotFoundException classNotFoundException) {
            // Not loaded and not available on local classpath; load by copying classfile from the VM
            final Reference byteArrayReference = teleFields().ClassActor_classfile.readReference(classActorReference);
            final TeleArrayObject teleByteArrayObject = (TeleArrayObject) heap().makeTeleObject(byteArrayReference);
            if (teleByteArrayObject == null) {
                throw new NoClassDefFoundError(String.format("Could not retrieve class file from VM for %s%nTry using '%s' VM option to access generated class files.",
                    name, ClassfileReader.saveClassDir));
            }
            final byte[] classfile = (byte[]) teleByteArrayObject.shallowCopy();
            return HostedBootClassLoader.HOSTED_BOOT_CLASS_LOADER.makeClassActor(name, classfile);
        }
    }

    public final ClassActor makeClassActorForTypeOf(Reference objectReference)  throws InvalidReferenceException {
        checkReference(objectReference);
        final Reference hubReference = wordToReference(layoutScheme().generalLayout.readHubReferenceAsWord(objectReference));
        final Reference classActorReference = teleFields().Hub_classActor.readReference(hubReference);
        return makeClassActor(classActorReference);
    }

    /**
     * @param objectReference    An {@link Object} in the VM.
     * @return Local {@link Hub}, equivalent to the hub of the object.
     * @throws InvalidReferenceException
     */
    public final Hub makeLocalHubForObject(Reference objectReference) throws InvalidReferenceException {
        checkReference(objectReference);
        final Reference hubReference = wordToReference(layoutScheme().generalLayout.readHubReferenceAsWord(objectReference));
        final Reference classActorReference = teleFields().Hub_classActor.readReference(hubReference);
        final ClassActor objectClassActor = makeClassActor(classActorReference);
        final ClassActor hubClassActor = makeClassActorForTypeOf(hubReference);
        return (StaticHub.class.isAssignableFrom(hubClassActor.toJava())) ? objectClassActor.staticHub()
                : objectClassActor.dynamicHub();
    }

    public final Value getElementValue(Kind kind, Reference reference, int index) throws InvalidReferenceException {
        switch (kind.asEnum) {
            case BYTE:
                return ByteValue.from(layoutScheme().byteArrayLayout.getByte(reference, index));
            case BOOLEAN:
                return BooleanValue.from(layoutScheme().booleanArrayLayout.getBoolean(reference, index));
            case SHORT:
                return ShortValue.from(layoutScheme().shortArrayLayout.getShort(reference, index));
            case CHAR:
                return CharValue.from(layoutScheme().charArrayLayout.getChar(reference, index));
            case INT:
                return IntValue.from(layoutScheme().intArrayLayout.getInt(reference, index));
            case FLOAT:
                return FloatValue.from(layoutScheme().floatArrayLayout.getFloat(reference, index));
            case LONG:
                return LongValue.from(layoutScheme().longArrayLayout.getLong(reference, index));
            case DOUBLE:
                return DoubleValue.from(layoutScheme().doubleArrayLayout.getDouble(reference, index));
            case WORD:
                return new WordValue(layoutScheme().wordArrayLayout.getWord(reference, index));
            case REFERENCE:
                checkReference(reference);
                return TeleReferenceValue.from(this, wordToReference(layoutScheme().wordArrayLayout.getWord(reference, index)));
            default:
                throw ProgramError.unknownCase("unknown array kind");
        }
    }

    public final void copyElements(Kind kind, Reference src, int srcIndex, Object dst, int dstIndex, int length) {
        switch (kind.asEnum) {
            case BYTE:
                layoutScheme().byteArrayLayout.copyElements(src, srcIndex, dst, dstIndex, length);
                break;
            case BOOLEAN:
                layoutScheme().booleanArrayLayout.copyElements(src, srcIndex, dst, dstIndex, length);
                break;
            case SHORT:
                layoutScheme().shortArrayLayout.copyElements(src, srcIndex, dst, dstIndex, length);
                break;
            case CHAR:
                layoutScheme().charArrayLayout.copyElements(src, srcIndex, dst, dstIndex, length);
                break;
            case INT:
                layoutScheme().intArrayLayout.copyElements(src, srcIndex, dst, dstIndex, length);
                break;
            case FLOAT:
                layoutScheme().floatArrayLayout.copyElements(src, srcIndex, dst, dstIndex, length);
                break;
            case LONG:
                layoutScheme().longArrayLayout.copyElements(src, srcIndex, dst, dstIndex, length);
                break;
            case DOUBLE:
                layoutScheme().doubleArrayLayout.copyElements(src, srcIndex, dst, dstIndex, length);
                break;
            case WORD:
                layoutScheme().wordArrayLayout.copyElements(src, srcIndex, dst, dstIndex, length);
                break;
            default:
                throw ProgramError.unknownCase("unknown array kind");
        }
    }

    public final TeleClassActor findTeleClassActor(int id) {
        return teleClassRegistry.findTeleClassActorByID(id);
    }

    public final TeleClassActor findTeleClassActor(TypeDescriptor typeDescriptor) {
        return teleClassRegistry.findTeleClassActorByType(typeDescriptor);
    }

    public final TeleClassActor findTeleClassActor(Class javaClass) {
        return teleClassRegistry.findTeleClassActorByClass(javaClass);
    }

    public final Set<TypeDescriptor> typeDescriptors() {
        return teleClassRegistry.typeDescriptors();
    }

    public final synchronized Iterable<TypeDescriptor> loadableTypeDescriptors() {
        final SortedSet<TypeDescriptor> typeDescriptors = new TreeSet<TypeDescriptor>();
        for (TypeDescriptor typeDescriptor : teleClassRegistry.typeDescriptors()) {
            typeDescriptors.add(typeDescriptor);
        }
        typeDescriptors.addAll(typesOnClasspath());
        return typeDescriptors;
    }

    public final void updateLoadableTypeDescriptorsFromClasspath() {
        final Set<TypeDescriptor> typesOnClasspath = new TreeSet<TypeDescriptor>();
        Trace.begin(TRACE_VALUE, tracePrefix() + "searching classpath for class files");
        new ClassSearch() {
            @Override
            protected boolean visitClass(String className) {
                if (!className.endsWith("package-info")) {
                    final String typeDescriptorString = "L" + className.replace('.', '/') + ";";
                    typesOnClasspath.add(JavaTypeDescriptor.parseTypeDescriptor(typeDescriptorString));
                }
                return true;
            }
        }.run(HostedBootClassLoader.HOSTED_BOOT_CLASS_LOADER.classpath());
        Trace.end(TRACE_VALUE, tracePrefix() + "searching classpath for class files ["
                + typesOnClasspath.size() + " types found]");
        this.typesOnClasspath = typesOnClasspath;
    }

    public final List<MaxCodeLocation> inspectableMethods() {
        final List<MaxCodeLocation> methods = new ArrayList<MaxCodeLocation>(teleMethods.clientInspectableMethods());
        methods.addAll(heap.inspectableMethods());
        return methods;
    }

    public final <TeleMethodActor_Type extends TeleMethodActor> TeleMethodActor_Type findTeleMethodActor(Class<TeleMethodActor_Type> teleMethodActorType, MethodActor methodActor) {
        final TeleClassActor teleClassActor = teleClassRegistry.findTeleClassActorByType(methodActor.holder().typeDescriptor);
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
                // no other listeners.  This presents an opportunity for the Reference/Grip/Object
                // code to update heap-related information that may have been changed as
                // a result of the GC.
                public void gcCompleted() {
                    Trace.line(TRACE_VALUE, tracePrefix() + "GC complete");
                }
            });
        } catch (MaxVMBusyException maxVMBusyException) {
            ProgramError.unexpected("Unable to set initial GC completed listener");
        }
    }

    public final Value interpretMethod(ClassMethodActor classMethodActor, Value... arguments) throws TeleInterpreterException {
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
        } else if (reference instanceof PrototypeReference) {
            return TeleReferenceValue.from(this, Reference.fromJava(reference.toJava()));
        }
        throw ProgramError.unexpected("Got a non-Prototype, non-Tele reference in createReferenceValue");
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
     * @author Thomas Wuerthinger
     * @author Michael Van De Vanter
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
                ProgramError.unexpected("breakpoint creation failed");
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
                        ProgramError.unexpected("breakpoint removal failed");
                    }
                }
            }
            assert breakpointLocations.contains(codeLocation);
            breakpointLocations.remove(codeLocation);
            assert !breakpointLocations.contains(codeLocation);
        }

        public byte[] accessMemory(long start, int length) {
            final byte[] bytes = new byte[length];
            TeleVM.this.readFully(Address.fromLong(start), bytes);
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
                referenceTypeProvider = TeleVM.this.findTeleClassActor(klass);
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
            for (TypeDescriptor typeDescriptor : TeleVM.this.typeDescriptors()) {
                if (typeDescriptor.toString().equals(signature)) {
                    final TeleClassActor teleClassActor = TeleVM.this.findTeleClassActor(typeDescriptor);

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

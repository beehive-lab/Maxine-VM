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
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.util.logging.*;

import com.sun.max.*;
import com.sun.max.collect.*;
import com.sun.max.ide.*;
import com.sun.max.jdwp.vm.core.*;
import com.sun.max.jdwp.vm.proxy.*;
import com.sun.max.jdwp.vm.proxy.VMValue.*;
import com.sun.max.lang.*;
import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.program.Classpath.*;
import com.sun.max.program.option.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.debug.darwin.*;
import com.sun.max.tele.debug.guestvm.xen.*;
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
 */
public abstract class TeleVM implements MaxVM {

    private static final int TRACE_VALUE = 1;

    private static final String PROGRAM_NAME = "maxvm";

    private static final String TELE_LIBRARY_NAME = "tele";

    /**
     * The options controlling how a tele VM instance is {@linkplain #newAllocator(String...) created}.
     */
    public static class Options extends OptionSet {

        /**
         * Specifies if these options apply when creating a {@linkplain TeleVM#createReadOnly(File, Classpath) read-only} Tele VM.
         */
        public final boolean readOnly;

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

        public Options(boolean readOnly) {
            this.readOnly = readOnly;
            if (readOnly) {
                heapOption = newStringOption("heap", null, "Relocation address for the heap and code in the boot image.");
                vmArguments = null;
                debuggeeIdOption = null;
            } else {
                heapOption = null;
                vmArguments = newStringOption("a", null, "Specifies the arguments to the target VM.");
                debuggeeIdOption = newIntegerOption("id", -1,
                    "Process id of VM instance to which this debugger should attach. A value of -1 indicates that a new VM " +
                    "process should be started using the arguments specified by the -" + vmArguments + " option.");
            }
        }
    }

    /**
     * Creates a new VM instance based on a given set of options.
     *
     * @param options the options controlling specifics of the VM instance to be created
     * @return a new VM instance
     */
    public static TeleVM create(Options options) throws BootImageException {
        HostObjectAccess.setMainThread(Thread.currentThread());

        final String logLevel = options.logLevelOption.getValue();
        try {
            LogManager.getLogManager().getLogger("").setLevel(Level.parse(logLevel));
        } catch (IllegalArgumentException e) {
            ProgramWarning.message("Invalid level specified for java.util.logging root logger: " + logLevel + " [using " + Level.SEVERE + "]");
            LogManager.getLogManager().getLogger("").setLevel(Level.SEVERE);
        }

        TeleVM teleVM = null;

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

        Prototype.loadLibrary(TELE_LIBRARY_NAME);
        final File bootImageFile = BootImageGenerator.getBootImageFile(vmdir);

        Classpath sourcepath = JavaProject.getSourcePath(true);
        final List<String> sourcepathList = options.sourcepathOption.getValue();
        if (sourcepathList != null) {
            sourcepath = sourcepath.prepend(new Classpath(sourcepathList.toArray(new String[sourcepathList.size()])));
        }
        checkClasspath(sourcepath);

        if (!options.readOnly) {
            final String value = options.vmArguments.getValue();
            final String[] commandLineArguments = value == null ? null : ("".equals(value) ? new String[0] : value.trim().split(" "));
            teleVM = create(bootImageFile, sourcepath, commandLineArguments, options.debuggeeIdOption.getValue());
            teleVM.teleProcess().initializeState();
            try {
                teleVM.advanceToJavaEntryPoint();
            } catch (IOException ioException) {
                throw new BootImageException(ioException);
            }

            final File commandFile = options.commandFileOption.getValue();
            if (commandFile != null && !commandFile.equals("")) {
                teleVM.executeCommandsFromFile(commandFile.getPath());
            }

        } else {
            String heap = options.heapOption.getValue();
            if (heap != null) {
                assert System.getProperty(ReadOnlyTeleProcess.HEAP_PROPERTY) == null;
                System.setProperty(ReadOnlyTeleProcess.HEAP_PROPERTY, heap);
            }
            teleVM = createReadOnly(bootImageFile, sourcepath);
            teleVM.refresh(0);
        }

        return teleVM;
    }

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
                teleVM = new GuestVMXenTeleVM(bootImageFile, bootImage, sourcepath, commandlineArguments, processID);
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

    public final String getName() {
        return MaxineVM.name();
    }

    public final String getVersion() {
        return MaxineVM.VERSION;
    }

    public final String getDescription() {
        return MaxineVM.description();
    }

    private final VMConfiguration vmConfiguration;

    public final VMConfiguration vmConfiguration() {
        return vmConfiguration;
    }

    private final Size wordSize;

    public final Size wordSize() {
        return wordSize;
    }

    private final Size pageSize;

    public final Size pageSize() {
        return pageSize;
    }

    private final BootImage bootImage;

    public final BootImage bootImage() {
        return bootImage;
    }

    private final File bootImageFile;

    public final File bootImageFile() {
        return bootImageFile;
    }

    final File programFile;

    public final File programFile() {
        return programFile;
    }

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

    private final TeleHeapManager teleHeapManager;
    private final TeleObjectFactory teleObjectFactory;
    private TeleClassRegistry teleClassRegistry;
    private TeleCodeRegistry teleCodeRegistry;

    private final CodeManager codeManager;

    /**
     * Breakpoint factory, for both target and bytecode breakpoints.
     */
    private final TeleBreakpointFactory teleBreakpointFactory;

    private final TeleBytecodeBreakpoint.Factory bytecodeBreakpointFactory;

    private final TeleWatchpoint.Factory watchpointFactory;

    /**
     * The immutable history of all VM states, as of the last state transition; thread safe
     * for access by client methods on any thread.
     */
    private volatile TeleVMState teleVMState = TeleVMState.NONE;

    /**
     * @return VM state; thread safe.
     */
    public final MaxVMState maxVMState() {
        return teleVMState;
    }

    private boolean isInGC = false;

    /**
     * @return whether the VM is currently performing Garbage Collection
     */
    public final boolean isInGC() {
        return isInGC;
    }

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
        this.bootImageFile = bootImageFile;
        this.bootImage = bootImage;
        this.sourcepath = sourcepath;
        nativeInitialize(bootImage.header.threadLocalsAreaSize);
        final MaxineVM vm = createVM(this.bootImage);
        this.vmConfiguration = vm.configuration;

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
        this.teleFields = new TeleFields(this);
        this.teleMethods = new TeleMethods(this);
        this.teleObjectFactory = TeleObjectFactory.make(this);
        this.teleHeapManager = TeleHeapManager.make(this);

        // Provide access to JDWP server
        this.jdwpAccess = new VMAccessImpl();
        addVMStateListener(jdwpStateModel);
        this.javaThreadGroupProvider = new ThreadGroupProviderImpl(this, true);
        this.nativeThreadGroupProvider = new ThreadGroupProviderImpl(this, false);

        final TeleGripScheme teleGripScheme = (TeleGripScheme) vmConfiguration.gripScheme();
        teleGripScheme.setTeleVM(this);

        this.codeManager = new CodeManager(this);

        this.bytecodeBreakpointFactory = new TeleBytecodeBreakpoint.Factory(this);

        this.teleBreakpointFactory = new TeleBreakpointFactory(this, this.bytecodeBreakpointFactory);

        this.watchpointFactory = teleProcess.getWatchpointFactory();
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

    public final boolean lockHeldByCurrentThread() {
        return lock.isHeldByCurrentThread();
    }

    /**
     * Releases the lock on the VM process and related cached state; returns
     * immediately. The lock is reentrant, so that nested lock acquisition behaves with
     * standard Java synchronization semantics.
     */
    public final void unlock() {
        lock.unlock();
    }

    /**
     * Initializes native tele code.
     *
     * @param threadLocalsSize the size of thread local storage as read from the image
     */
    private static native void nativeInitialize(int threadLocalsSize);

    /**
     * Sets or clears some bits of the {@link Inspectable#flags} field in the VM process.
     *
     * @param flags specifies which bits to set or clear
     * @param set if {@code true}, then the bits are set otherwise they are cleared
     */
    public final void modifyInspectableFlags(int flags, boolean set) {
        int newFlags = teleFields.Inspectable_flags.readInt(this);
        if (set) {
            newFlags |= flags;
        } else {
            newFlags &= ~flags;
        }
        teleFields.Inspectable_flags.writeInt(this, newFlags);
    }

    /**
     * Enables inspector facilities in the VM.
     */
    private void setVMInspectable() {
        modifyInspectableFlags(Inspectable.INSPECTED, true);
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

    public final void addVMStateListener(MaxVMStateListener listener) {
        vmStateListeners.add(listener);
    }

    public final void removeVMStateListener(MaxVMStateListener listener) {
        vmStateListeners.remove(listener);
    }

    public final void notifyStateChange(ProcessState processState,
                    long epoch,
                    TeleNativeThread singleStepThread,
                    Collection<TeleNativeThread> threads,
                    Sequence<TeleNativeThread> threadsStarted,
                    Sequence<TeleNativeThread> threadsDied,
                    Sequence<TeleBreakpointEvent> breakpointEvents,
                    TeleWatchpointEvent teleWatchpointEvent) {
        this.teleVMState = new TeleVMState(processState,
            epoch,
            threads,
            singleStepThread,
            threadsStarted,
            threadsDied,
            breakpointEvents,
            teleWatchpointEvent,
            isInGC,
            teleVMState);
        for (final MaxVMStateListener listener : vmStateListeners) {
            listener.stateChanged(teleVMState);
        }
    }

    public final void describeVMStateHistory(PrintStream printStream) {
        teleVMState.writeSummaryToStream(printStream);
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

    private TeleGripScheme gripScheme() {
        return (TeleGripScheme) vmConfiguration.gripScheme();
    }

    /**
     * @return the scheme used to manage object layouts in this VM.
     */
    public final LayoutScheme layoutScheme() {
        return vmConfiguration.layoutScheme();
    }

    public final String visualizeStateRegister(long flags) {
        return TeleStateRegisters.flagsToString(this, flags);
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

    public final void readFully(Address address, byte[] bytes) {
        teleProcess.dataAccess().readFully(address, bytes);
    }

    public final IndexedSequence<MemoryRegion> allocatedMemoryRegions() {
        final IndexedSequence<TeleRuntimeMemoryRegion> teleHeapRegions = teleHeapRegions();
        final TeleCodeRegion teleRuntimeCodeRegion = teleCodeManager().teleRuntimeCodeRegion();
        final IterableWithLength<TeleNativeThread> threads = teleProcess.threads();
        final VariableSequence<MemoryRegion> regions = new ArrayListSequence<MemoryRegion>(teleHeapRegions.length() + 1 + threads.length() + 2);
        // Special "tele roots" region
        if (teleRootsRegion() != null) {
            regions.append(teleRootsRegion());
        }
        // Heap regions
        regions.append(teleBootHeapRegion());
        if (teleImmortalHeapRegion() != null) {
            regions.append(teleImmortalHeapRegion());
        }
        for (MemoryRegion region : teleHeapRegions) {
            assert region != null;
            regions.append(region);
        }
        // Code regions
        regions.append(teleCodeManager().teleBootCodeRegion());
        if (teleRuntimeCodeRegion.isAllocated()) {
            regions.append(teleRuntimeCodeRegion);
        }
        // Thread memory (stack + thread locals)
        for (TeleNativeThread thread : threads) {
            final TeleNativeStackMemoryRegion stackRegion = thread.stackRegion();
            if (!stackRegion.size().isZero()) {
                regions.append(stackRegion);
            }
            TeleThreadLocalsMemoryRegion threadLocalsRegion = thread.threadLocalsRegion();
            if (!threadLocalsRegion.size().isZero()) {
                regions.append(threadLocalsRegion);
            }
        }
        return regions;
    }

    public final MemoryRegion memoryRegionContaining(Address address) {
        MemoryRegion memoryRegion = null;
        try {
            memoryRegion = teleHeapManager.regionContaining(address);
            if (memoryRegion == null) {
                memoryRegion = teleCodeManager().regionContaining(address);
                if (memoryRegion == null) {
                    MaxThread maxThread = threadStackContaining(address);
                    if (maxThread != null) {
                        memoryRegion = maxThread.stackRegion();
                    } else {
                        maxThread = threadLocalsBlockContaining(address);
                        if (maxThread != null) {
                            memoryRegion = maxThread.threadLocalsRegion();
                        }
                    }

                }
            }
        } catch (DataIOError dataIOError) {
        }
        return memoryRegion;
    }

    public final boolean contains(Address address) {
        return containsInHeap(address) || containsInCode(address) || containsInThread(address);
    }

    public final boolean containsInHeap(Address address) {
        return teleHeapManager.contains(address);
    }

    public final boolean containsInDynamicHeap(Address address) {
        return teleHeapManager.dynamicHeapContains(address);
    }

    public final boolean isInLiveMemory(Address address) {
        return teleHeapManager.isInLiveMemory(address);
    }

    public final TeleRuntimeMemoryRegion teleBootHeapRegion() {
        return teleHeapManager.teleBootHeapRegion();
    }

    public final TeleRuntimeMemoryRegion teleImmortalHeapRegion() {
        return teleHeapManager.teleImmortalHeapRegion();
    }

    public final IndexedSequence<TeleRuntimeMemoryRegion> teleHeapRegions() {
        return teleHeapManager.teleHeapRegions();
    }

    public final TeleRuntimeMemoryRegion[] teleHeapRegionsArray() {
        return teleHeapManager.teleHeapRegionsArray();
    }

    public final TeleRuntimeMemoryRegion teleRootsRegion() {
        return teleHeapManager.teleRootsRegion();
    }

    public final Pointer teleRootsPointer() {
        return teleHeapManager.teleRootsPointer();
    }

    /**
     * Determines whether an object formerly at a particular location
     * has been relocated.
     *
     * @param origin an object location in the VM
     * @return whether the object at the location has been relocated.
     */
    public final boolean isObjectForwarded(Pointer origin) {
        return teleHeapManager.isObjectForwarded(origin);
    }

    /**
     * Finds an object in the VM that has been located at a particular place in memory, but which
     * may have been relocated.
     *
     * @param origin an object origin in the VM
     * @return the object originally at the origin, possibly relocated
     */
    public final TeleObject getForwardedObject(Pointer origin) {
        final Reference forwardedObjectReference = originToReference(teleHeapManager.getForwardedOrigin(origin));
        return teleObjectFactory.make(forwardedObjectReference);
    }

    /**
     * @return offset in heap objects of the word used by GC to assign forwarding pointer
     */
    public final Offset gcForwardingPointerOffset() {
        return teleHeapManager.gcForwardingPointerOffset();
    }

    /**
     * Address of the field incremented each time a GC begins.
     * @return memory location of the field holding the collection epoch
     * @see #readCollectionEpoch()
     */
    public final Address collectionEpochAddress() {
        return teleHeapManager.collectionEpochAddress();
    }

    /**
     * Address of the field incremented each time a GC completes.
     * @return memory location of the field holding the root epoch
     * @see #readRootEpoch()
     */
    public final Address rootEpochAddress() {
        return teleHeapManager.rootEpochAddress();
    }

    /**
     * @return manager for {@link MemoryRegion}s containing target code in the VM.
     */
    private TeleCodeManager teleCodeManager() {
        // Instantiate lazily to avoid circularities in startup sequence.
        return TeleCodeManager.make(this);
    }

    public final boolean containsInCode(Address address) {
        return teleCodeManager().contains(address);
    }

    public final TeleCodeRegion teleBootCodeRegion() {
        return teleCodeManager().teleBootCodeRegion();
    }

    public final TeleCodeRegion teleRuntimeCodeRegion() {
        return teleCodeManager().teleRuntimeCodeRegion();
    }

    public final IndexedSequence<TeleCodeRegion> teleCodeRegions() {
        return new ArraySequence<TeleCodeRegion>(teleBootCodeRegion(), teleRuntimeCodeRegion());
    }

    public final boolean containsInThread(Address address) {
        return threadStackContaining(address) != null || threadLocalsBlockContaining(address) != null;
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
            if (!containsInHeap(origin) && !containsInCode(origin)) {
                return false;
            }
            if (false && isInGC() && containsInDynamicHeap(origin)) {
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
                if (!containsInHeap(hubOrigin) && !containsInCode(hubOrigin)) {
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
        if (!containsInHeap(staticHubOrigin) && !containsInCode(staticHubOrigin)) {
            return false;
        }
        // If we really have a {@link StaticHub}, then a known field points at a {@link ClassActor}.
        final int hubClassActorOffset = teleFields().Hub_classActor.fieldActor().offset();
        final Word classActorWord = dataAccess().readWord(staticHubOrigin, hubClassActorOffset);
        final RemoteTeleGrip classActorGrip = createTemporaryRemoteTeleGrip(classActorWord);
        final Pointer classActorOrigin = classActorGrip.toOrigin();
        if (!containsInHeap(classActorOrigin) && !containsInCode(classActorOrigin)) {
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
            final TeleArrayObject teleByteArrayObject = (TeleArrayObject) makeTeleObject(byteArrayReference);
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

    public final TeleObject makeTeleObject(Reference reference) {
        return teleObjectFactory.make(reference);
    }

    public final TeleObject findObjectByOID(long id) {
        return teleObjectFactory.lookupObject(id);
    }

    public final TeleObject findObjectAt(Address origin) {
        try {
            return makeTeleObject(originToReference(origin.asPointer()));
        } catch (Throwable throwable) {
        }
        return null;
    }

    public final TeleObject findObjectFollowing(Address cellAddress, long maxSearchExtent) {

        // Search limit expressed in words
        long wordSearchExtent = Long.MAX_VALUE;
        if (maxSearchExtent > 0) {
            wordSearchExtent = maxSearchExtent / wordSize().toInt();
        }
        try {
            Pointer origin = cellAddress.asPointer();
            for (long count = 0; count < wordSearchExtent; count++) {
                origin = origin.plus(wordSize());
                if (isValidOrigin(origin)) {
                    return makeTeleObject(originToReference(origin));
                }
            }
        } catch (Throwable throwable) {
        }
        return null;
    }

    public final TeleObject findObjectPreceding(Address cellAddress, long maxSearchExtent) {

        // Search limit expressed in words
        long wordSearchExtent = Long.MAX_VALUE;
        if (maxSearchExtent > 0) {
            wordSearchExtent = maxSearchExtent / wordSize().toInt();
        }
        try {
            Pointer origin = cellAddress.asPointer();
            for (long count = 0; count < wordSearchExtent; count++) {
                origin = origin.minus(wordSize());
                if (isValidOrigin(origin)) {
                    return makeTeleObject(originToReference(origin));
                }
            }
        } catch (Throwable throwable) {
        }
        return null;
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

    private synchronized TeleCodeRegistry teleCodeRegistry() {
        if (teleCodeRegistry == null) {
            teleCodeRegistry = new TeleCodeRegistry(this);
        }
        return teleCodeRegistry;
    }

    public final Sequence<MaxCodeLocation> inspectableMethods() {
        final AppendableSequence<MaxCodeLocation> methods = new ArrayListSequence<MaxCodeLocation>(teleMethods.clientInspectableMethods());
        for (MaxCodeLocation method : teleHeapManager.inspectableMethods()) {
            methods.append(method);
        }
        return methods;
    }

    /**
     * Registers the description of a newly discovered block of target code so that it can be located later by address.
     *
     * @param teleTargetRoutine a newly created description for a block of target code in the VM.
     */
    public final void registerTeleTargetRoutine(TeleTargetRoutine teleTargetRoutine) {
        teleCodeRegistry().add(teleTargetRoutine);
    }

    public final TeleTargetMethod makeTeleTargetMethod(Address address) {
        return TeleTargetMethod.make(this, address);
    }

    public final TeleNativeTargetRoutine createTeleNativeTargetRoutine(Address codeStart, Size codeSize, String name) {
        return TeleNativeTargetRoutine.create(this, codeStart, codeSize, name);
    }

    public final <TeleTargetRoutine_Type extends TeleTargetRoutine> TeleTargetRoutine_Type findTeleTargetRoutine(Class<TeleTargetRoutine_Type> teleTargetRoutineType, Address address) {
        return teleCodeRegistry().get(teleTargetRoutineType, address);
    }

    public final <TeleMethodActor_Type extends TeleMethodActor> TeleMethodActor_Type findTeleMethodActor(Class<TeleMethodActor_Type> teleMethodActorType, MethodActor methodActor) {
        final TeleClassActor teleClassActor = teleClassRegistry.findTeleClassActorByType(methodActor.holder().typeDescriptor);
        if (teleClassActor != null) {
            for (TeleMethodActor teleMethodActor : teleClassActor.getTeleMethodActors()) {
                if (teleMethodActorType.isInstance(teleMethodActor) && methodActor.memberIndex() == teleMethodActor.getMemberIndex()) {
                    return teleMethodActorType.cast(teleMethodActor);
                }
            }
        }
        return null;
    }

    public final void describeTeleTargetRoutines(PrintStream printStream) {
        teleCodeRegistry().writeSummaryToStream(printStream);
    }

    public final MaxThread getThread(long threadID) {
        for (MaxThread maxThread : teleVMState.threads()) {
            if (maxThread.id() == threadID) {
                return maxThread;
            }
        }
        return null;
    }

    public final MaxThread threadStackContaining(Address address) {
        for (MaxThread thread : teleVMState.threads()) {
            if (thread.stackRegion().contains(address)) {
                return thread;
            }
        }
        return null;
    }

    public final MaxThread threadLocalsBlockContaining(Address address) {
        for (MaxThread thread : teleVMState.threads()) {
            if (thread.threadLocalsRegion().contains(address)) {
                return thread;
            }
        }
        return null;
    }

    public final CodeManager codeManager() {
        return codeManager;
    }

    public final TeleBreakpointFactory breakpointFactory() {
        return teleBreakpointFactory;
    }

    public final TeleWatchpoint.Factory watchpointFactory() {
        return watchpointFactory;
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
                gcStartedBreakpoint = teleProcess.targetBreakpointFactory().makeSystemBreakpoint(teleMethods.gcStarted(), triggerEventHandler);
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
                gcCompletedBreakpoint = teleProcess.targetBreakpointFactory().makeSystemBreakpoint(teleMethods.gcCompleted(), triggerEventHandler);
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

    public final void setTransportDebugLevel(int level) {
        teleProcess.setTransportDebugLevel(level);
    }

    public final int transportDebugLevel() {
        return teleProcess.transportDebugLevel();
    }

    /**
     * Identifies the most recent GC for which the local copy of the tele root
     * table in the VM is valid.
     */
    private long cachedCollectionEpoch;

    private final Tracer refreshReferencesTracer = new Tracer("refresh references");

    /**
     * Refreshes the values that describe VM state such as the
     * current GC epoch.
     */
    private void refreshReferences() {
        Trace.begin(TRACE_VALUE, refreshReferencesTracer);
        final long startTimeMillis = System.currentTimeMillis();
        final long teleRootEpoch = teleHeapManager.readRootEpoch();
        final long teleCollectionEpoch = teleHeapManager.readCollectionEpoch();
        if (teleCollectionEpoch != teleRootEpoch) {
            // A GC is in progress, local cache is out of date by definition but can't update yet
            assert teleCollectionEpoch != cachedCollectionEpoch;
            isInGC = true;
        } else if (teleCollectionEpoch == cachedCollectionEpoch) {
            // GC not in progress, local cache is up to date
            assert !isInGC;
        } else {
            // GC not in progress, local cache is out of date
            gripScheme().refresh();
            cachedCollectionEpoch = teleCollectionEpoch;
            isInGC = false;
        }
        Trace.end(TRACE_VALUE, refreshReferencesTracer, startTimeMillis);
    }

    private final Tracer refreshTracer = new Tracer("refresh");

    /**
     * Updates all cached information about the state of the running VM.
     * Does some initialization that is delayed to avoid cycles during startup.
     */
    public final synchronized void refresh(long processEpoch) {
        Trace.begin(TRACE_VALUE, refreshTracer);
        final long startTimeMillis = System.currentTimeMillis();
        if (teleClassRegistry == null) {
            // Must delay creation/initialization of the {@link TeleClassRegistry} until after
            // we hit the first execution breakpoint; otherwise addresses won't have been relocated.
            // This depends on the {@TeleHeapManager} already existing.
            teleClassRegistry = new TeleClassRegistry(this);
            // Can only fully initialize the {@link TeleHeapManager} once
            // the {@TeleClassRegistry} is fully initialized, otherwise there's a cycle.
            teleHeapManager.initialize(processEpoch);
        }
        refreshReferences();
        //if (!isInGC()) { ATTETION: Could produce bugs.
        teleHeapManager.refresh(processEpoch);
        teleClassRegistry.refresh(processEpoch);
        //}
        teleObjectFactory.refresh(processEpoch);
        Trace.end(TRACE_VALUE, refreshTracer, startTimeMillis);
    }

    public void advanceToJavaEntryPoint() throws IOException {
        setVMInspectable();
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

    private final VariableSequence<VMListener> jdwpListeners = new ArrayListSequence<VMListener>();

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

        if (kind == Kind.REFERENCE) {
            final Word word = dataAccess().readWord(pointer, offset);
            return TeleReferenceValue.from(this, wordToReference(word));
        }

        final Value result = kind.readValue(reference, offset);

        if (result.kind() == Kind.WORD) {
            LOGGER.info("Creating WORD reference! " + result.asWord());
            return LongValue.from(result.asWord().asAddress().toLong());
        }

        if (result.kind() == Kind.REFERENCE
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
            return makeTeleObject(reference);
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
            return TeleVM.this.getName();
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
                        && registeredStepOutThread.getReturnLocation().address() != null) {

                    // There has been a thread registered for performing a step out
                    // => perform a step out instead of resume.
                    final CodeLocation returnLocation = registeredStepOutThread.getReturnLocation();
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
            jdwpListeners.append(listener);
        }

        public void removeListener(VMListener listener) {
            jdwpListeners.remove(Sequence.Static.indexOfIdentical(jdwpListeners, listener));
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
                TeleVM.this.breakpointFactory().makeBreakpoint(methodCodeLocation);
            } catch (MaxVMBusyException maxVMBusyException) {
                ProgramError.unexpected("breakpoint creation failed");
            }
            Trace.line(TRACE_VALUE, tracePrefix() + "Breakpoint set at: " + methodCodeLocation);
        }

        public void removeBreakpoint(JdwpCodeLocation codeLocation) {
            final TeleClassMethodActor teleClassMethodActor = (TeleClassMethodActor) codeLocation.method();
            final MaxBreakpoint breakpoint = TeleVM.this.breakpointFactory().findBreakpoint(teleClassMethodActor.getCurrentJavaTargetMethod().callEntryLocation());
            if (breakpoint != null) {
                try {
                    breakpoint.remove();
                } catch (MaxVMBusyException maxVMBusyException) {
                    ProgramError.unexpected("breakpoint removal failed");
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
                result[i] = TeleVM.this.makeTeleTargetMethod(Address.fromLong(addresses[i]));
            }
            return result;
        }

        public ReferenceTypeProvider[] getAllReferenceTypes() {
            return teleClassRegistry.teleClassActors();
        }

        public ThreadProvider[] getAllThreads() {
            final IterableWithLength<TeleNativeThread> threads = teleProcess().threads();
            final ThreadProvider[] threadProviders = new ThreadProvider[threads.length()];
            return Iterables.toCollection(threads).toArray(threadProviders);
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
            final AppendableSequence<ReferenceTypeProvider> result = new LinkSequence<ReferenceTypeProvider>();
            for (TypeDescriptor typeDescriptor : TeleVM.this.typeDescriptors()) {
                if (typeDescriptor.toString().equals(signature)) {
                    final TeleClassActor teleClassActor = TeleVM.this.findTeleClassActor(typeDescriptor);

                    // Do not include array types, there should always be faked in
                    // order to be able to call newInstance on them. Arrays that are
                    // created this way then do
                    // not really live within the VM, but on the JDWP server side.
                    if (!(teleClassActor instanceof TeleArrayClassActor)) {
                        result.append(teleClassActor);
                    }
                }
            }

            // If no class living in the VM was found, try to lookup Java class
            // known to the JDWP server. If such a class is found, then a JDWP
            // reference type is faked for it.
            if (result.length() == 0) {
                try {
                    final Class klass = JavaTypeDescriptor.resolveToJavaClass(
                            JavaTypeDescriptor.parseTypeDescriptor(signature), getClass().getClassLoader());
                    result.append(javaProviderFactory.getReferenceTypeProvider(klass));
                } catch (NoClassDefFoundError noClassDefFoundError) {
                    LOGGER.log(Level.SEVERE,
                            "Error while looking up class based on signature", noClassDefFoundError);
                }
            }

            return Sequence.Static.toArray(result, new ReferenceTypeProvider[result.length()]);
        }

        public ThreadGroupProvider[] getThreadGroups() {
            return new ThreadGroupProvider[] {javaThreadGroupProvider, nativeThreadGroupProvider};
        }

        public VMValue getVoidValue() {
            return VMValueImpl.VOID_VALUE;
        }
    }

}

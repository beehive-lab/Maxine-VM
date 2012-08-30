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
import com.sun.max.tele.debug.VmBytecodeBreakpoint.BytecodeBreakpointManager;
import com.sun.max.tele.debug.VmWatchpoint.VmWatchpointManager;
import com.sun.max.tele.debug.no.*;
import com.sun.max.tele.field.*;
import com.sun.max.tele.heap.*;
import com.sun.max.tele.interpreter.*;
import com.sun.max.tele.jdwputil.*;
import com.sun.max.tele.memory.*;
import com.sun.max.tele.method.*;
import com.sun.max.tele.method.CodeLocation.BytecodeLocation;
import com.sun.max.tele.method.CodeLocation.MachineCodeLocation;
import com.sun.max.tele.method.CodeLocation.VmCodeLocationManager;
import com.sun.max.tele.object.*;
import com.sun.max.tele.reference.*;
import com.sun.max.tele.reference.direct.*;
import com.sun.max.tele.type.*;
import com.sun.max.tele.util.*;
import com.sun.max.tele.value.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.layout.*;
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

    private static final List<MaxEntityMemoryRegion<? extends MaxEntity> > EMPTY_MAXMEMORYREGION_LIST = Collections.emptyList();

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
     * Where the meta-data associated with the target VM is located {link #vmDirectoryOption}.
     */
    private static File vmDirectory;

    /**
     * The VM object that represents the VM itself.
     */
    private static TeleMaxineVM teleMaxineVM;

    /**
     * The VM object that holds configuration information, including scheme implementations.
     */
    private static TeleVMConfiguration teleVMConfiguration;

    /**
     * An abstraction description of the VM's platform, suitable for export.
     */
    private VmPlatform platform;

    /**
     * If {@code true}, always prompt for native code frame view when entering native code.
     */
    public static boolean promptForNativeCodeView;

    /**
     * Interface for notification that all of the initialization for a remote inspection session
     * are substantially complete; some services have needs for setup that can only happen very
     * late, most notably anything that requires setting a breakpoint.
     */
    public interface InitializationListener {

        /**
         * Notifies listener that all of the remote inspection services are substantially complete,
         * and that it is safe to use them, for example setting breakpoints.
         */
        void initialiationComplete(long epoch);
    }

    /**
     * The options controlling how a VM instance is created}.
     */
    public static class Options extends OptionSet {

        public final Option<String> modeOption = newStringOption("mode", "create",
            "Mode of operation: create | attach | attachwaiting | image");
        public final Option<String> targetKindOption = newStringOption("target", "local",
            "Location kind of target VM: local | remote | file");
        public final Option<List<String>> targetLocationOption = newStringListOption("location", "",
            "Location info of target VM: hostname[, port, id] | pathname");
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
        public final Option<String> vmLogFileOption = newStringOption("vmlog", null, "file containg VMLog for mode==image");

        /**
         * An option to explicitly set the boot heap address (maybe useful for core dump).
         */
        public final Option<String> heapOption;

        /**
         * This field is {@code null} if inspecting read-only.
         */
        public final Option<String> vmArguments;

        /**
         * Creates command line options that are specific to certain operation modes. No longer tries to customize the
         * options based on mode.
         */
        public Options() {
            heapOption = newStringOption("heap", null, "Relocation address for the heap and code in the boot image.");
            vmArguments = newStringOption("a", "", "Specifies the arguments to the target VM.");
            // We do not want to check the auto generated code for consistency (by default), so change default value
            BootImageGenerator.checkGeneratedCodeOption.setDefaultValue(false);
            addOptions(BootImageGenerator.inspectorSharedOptions);
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
                cons = klass.getDeclaredConstructor(new Class[] {MaxVM.class, File.class, File.class});
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
        vmDirectory = BootImageGenerator.getDefaultVMDirectory(true);
        classpathPrefix = classpathPrefix.prepend(BootImageGenerator.getBootImageJarFile(vmDirectory).getAbsolutePath());
        checkClasspath(classpathPrefix);
        final Classpath classpath = Classpath.fromSystem().prepend(classpathPrefix);
        HostedVMClassLoader.HOSTED_VM_CLASS_LOADER.setClasspath(classpath);
        HostedBootClassLoader.noOmittedClassExceptions();

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
            System.setProperty(VmObjectAccess.HEAP_ADDRESS_PROPERTY, heap);
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
                 * {@link VmClassRegistry} is neither entirely necessary nor sufficient.
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
                System.setProperty(VmObjectAccess.HEAP_ADDRESS_PROPERTY, "1024");
                vm = createReadOnly(bootImageFile, sourcepath);
                String vmLogFileOption = options.vmLogFileOption.getValue();
                if (vmLogFileOption != null) {
                    vm.vmLogFile = new File(vmLogFileOption);
                }
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
     * @param bootImageConfig information about the particular build, extracted from the boot image
     */
    public static void initializeVM(VMConfiguration bootImageConfig) {
        MaxineVM vm = new MaxineVM(bootImageConfig);
        MaxineVM.set(vm);
        bootImageConfig.loadAndInstantiateSchemes(null);
        // Create a mirror of the VM's configuration, substituting an implementation of ReferenceScheme specialized for the Inspector.
        final VMConfiguration config = new VMConfiguration(
                        bootImageConfig.buildLevel,
                        Platform.platform(),
                        getInspectorReferencePackage(bootImageConfig.referencePackage),
                        bootImageConfig.layoutPackage,
                        bootImageConfig.heapPackage,
                        bootImageConfig.monitorPackage,
                        bootImageConfig.runPackage).gatherBootImagePackages();
        vm = new MaxineVM(config);
        MaxineVM.set(vm);
        config.loadAndInstantiateSchemes(bootImageConfig.vmSchemes());
        JavaPrototype.initialize(BootImageGenerator.checkGeneratedCodeOption.getValue());
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

        TeleVM vm = null;
        final OS os = Platform.platform().os;
        final String className = "com.sun.max.tele.debug." + os.asPackageName() + "." + os.className + "TeleVM";
        try {
            final Class< ? > klass = Class.forName(className);
            final Constructor< ? > cons = klass.getDeclaredConstructor(new Class[] {BootImage.class, Classpath.class, String[].class});
            vm = (TeleVM) cons.newInstance(new Object[] {bootImage, sourcepath, commandlineArguments});
        } catch (Exception ex) {
            TeleError.unexpected("failed to instantiate " + className, ex);
        }
        return vm;
    }

    private static void checkClasspath(Classpath classpath) {
        for (Entry classpathEntry : classpath.entries()) {
            if (classpathEntry.isPlainFile()) {
                TeleWarning.message("Class path entry is neither a directory nor a JAR file: " + classpathEntry);
            }
        }
    }

    /**
     * Creates a VM instance that is read-only and is only useful for inspecting a boot image.
     *
     * @param bootImageFile the file containing the boot image
     * @param sourcepath the source code path to search for class or interface definitions
     * @throws BootImageException
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

    private File vmLogFile;

    final File programFile;

    private final VmAddressSpace addressSpace;

    private final VmMemoryIO memoryIO;

    private final VmObjectAccess objectAccess;

    private final VmReferenceManager referenceManager;

    private final VmHeapAccess heapAccess;

    private VmCodeCacheAccess codeCacheAccess = null;

    private NativeCodeAccess nativeCodeAccess = null;

    private final VmCodeLocationManager codeLocationManager;

    private final VmMachineCodeAccess machineCodeAccess;

    /**
     * Breakpoint manager, for both target and bytecode breakpoints.
     */
    private final VmBreakpointManager breakpointManager;

    private final VmWatchpoint.VmWatchpointManager watchpointManager;

    private final VmThreadAccess threadAccess;

    /**
     * The immutable history of all VM states, as of the last state transition; thread safe
     * for access by client methods on any thread.
     */
    private volatile TeleVMState teleVMState;

    private List<InitializationListener> initializationListeners = new ArrayList<InitializationListener>();

    private List<MaxVMStateListener> vmStateListeners = new CopyOnWriteArrayList<MaxVMStateListener>();

    /**
     * Dispatcher for GC start events, i.e. when entering the {@link HeapPhase#ANALYZING} phase.
     */
    private VMEventDispatcher<MaxGCPhaseListener> gcAnalyzingListeners;

    /**
     * Dispatcher for GC start events, i.e. when entering the {@link HeapPhase#RECLAIMING} phase.
     */
    private VMEventDispatcher<MaxGCPhaseListener> gcReclaimingListeners;

    /**
     * Dispatcher for GC completion events, i.e. when entering the {@link HeapPhase#MUTATING} phase.
     */
    private VMEventDispatcher<MaxGCPhaseListener> gcMutatingListeners;

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

    private final VmFieldAccess fieldAccess;

    public final VmFieldAccess fields() {
        return fieldAccess;
    }

    private final VmMethodAccess methodAccess;

    /**
     * Clone of the configuration descriptor for the current VM, with inspector-specific adjustments.
     */
    private final VMConfiguration vmConfiguration;

    private final Classpath sourcepath;

    private int interpreterUseLevel = 0;

    private VmClassAccess classAccess;

    private final TimedTrace updateTracer;

    private final InvalidReferencesLogger invalidReferencesLogger;

    public final InvalidReferencesLogger invalidReferencesLogger() {
        return invalidReferencesLogger;
    }

    protected VMLock makeVMLock() {
        return new ReentrantVMLock();
    }

    protected VMLock getLock() {
        return lock;
    }

    /**
     * A lock designed to keep all non-thread-safe client calls from being handled during the VM setup/execute/refresh cycle.
     */
    private VMLock lock;

    protected interface VMLock {
        void lock();
        boolean tryLock();
        void unlock();
        boolean isHeldByCurrentThread();
    }

    private static class ReentrantVMLock implements VMLock {
        private ReentrantLock lock = new ReentrantLock();

        @Override
        public void lock() {
            lock.lock();
        }

        @Override
        public boolean tryLock() {
            return lock.tryLock();
        }

        @Override
        public void unlock() {
            lock.unlock();
        }

        @Override
        public boolean isHeldByCurrentThread() {
            return lock.isHeldByCurrentThread();
        }
    }

    /**
     * The protocol that is being used to communicate with the target VM.
     */
    private static TeleChannelProtocol teleChannelProtocol;

    /**
     * Creates a VM instance by creating or attaching to a Maxine VM process.
     *
     * @param bootImage the metadata describing the contents in the boot image
     * @param sourcepath path used to search for Java source files
     * @param commandLineArguments the command line arguments to be used when creating a new VM process. If this value
     *            is {@code null}, then an attempt is made to attach to the process whose id is {@code processID}.
     * @throws BootImageException
     */
    protected TeleVM(BootImage bootImage, Classpath sourcepath, String[] commandLineArguments) throws BootImageException {
        this.lock = makeVMLock();
        final TimedTrace tracer = new TimedTrace(TRACE_VALUE, tracePrefix() + " creating");
        tracer.begin();
        this.teleVMState = TeleVMState.nullState(mode);
        this.bootImageFile = bootImage.imageFile;
        this.bootImage = bootImage;

        this.sourcepath = sourcepath;
        this.platform = new VmPlatform(Platform.platform());
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
        this.addressSpace = VmAddressSpace.make(this);
        this.memoryIO = VmMemoryIO.make(this, this.teleProcess);
        this.referenceManager = VmReferenceManager.make(this, (RemoteReferenceScheme) this.vmConfiguration.referenceScheme());

        this.threadAccess = VmThreadAccess.make(this);
        this.codeLocationManager = VmCodeLocationManager.make(this);
        this.machineCodeAccess = VmMachineCodeAccess.make(this);

        if (!tryLock(DEFAULT_MAX_LOCK_TRIALS)) {
            TeleError.unexpected("unable to lock during creation");
        }
        this.fieldAccess = VmFieldAccess.make(this);
        this.methodAccess = VmMethodAccess.make(this, codeLocationManager);
        this.objectAccess = VmObjectAccess.make(this);
        this.heapAccess = VmHeapAccess.make(this, this.addressSpace);
        unlock();

        // Provide access to JDWP server - DISABLED - not being used now.
        this.jdwpAccess = new VMAccessImpl();
        // addVMStateListener(jdwpStateModel);
        this.javaThreadGroupProvider = new ThreadGroupProviderImpl(this, true);
        this.nativeThreadGroupProvider = new ThreadGroupProviderImpl(this, false);

        this.breakpointManager = VmBreakpointManager.make(this);
        this.watchpointManager = teleProcess.watchpointsEnabled() ? VmWatchpointManager.make(this, teleProcess) : null;
        this.invalidReferencesLogger = new InvalidReferencesLogger(this);

        this.gcAnalyzingListeners = new VMEventDispatcher<MaxGCPhaseListener>(methodAccess.gcAnalyzingMethodLocation(), "at GC start") {
            @Override
            protected void listenerDo(MaxThread thread, MaxGCPhaseListener listener) {
                listener.gcPhaseChange(HeapPhase.ANALYZING);
            }
        };

        this.gcReclaimingListeners = new VMEventDispatcher<MaxGCPhaseListener>(methodAccess.gcReclaimingMethodLocation(), "at GC transition to reclaiming") {
            @Override
            protected void listenerDo(MaxThread thread, MaxGCPhaseListener listener) {
                listener.gcPhaseChange(HeapPhase.RECLAIMING);
            }
        };

        this.gcMutatingListeners =  new VMEventDispatcher<MaxGCPhaseListener>(methodAccess.gcMutatingMethodLocation(), "at GC completion") {
            @Override
            protected void listenerDo(MaxThread thread, MaxGCPhaseListener listener) {
                listener.gcPhaseChange(HeapPhase.MUTATING);
            }
        };

        this.threadEntryListeners =  new VMEventDispatcher<MaxVMThreadEntryListener>(methodAccess.vmThreadRunMethodLocation(), "at VmThread entry") {
            @Override
            protected void listenerDo(MaxThread thread, MaxVMThreadEntryListener listener) {
                listener.entered(thread);
            }
        };

        this.threadDetachListeners =  new VMEventDispatcher<MaxVMThreadDetachedListener>(methodAccess.vmThreadDetachedMethodLocation(), "after VmThread detach") {
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
     * <p>
     * Note that gathering of thread information happens <em>after</em> this during
     * the normal refresh cycle.  See {@link TeleProcess}.
     *
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
            if (classAccess == null) {
                /**
                 * Must delay creation/initialization of the {@linkplain VmClassAccess "class registry"} until after
                 * we hit the first execution breakpoint; otherwise addresses won't have been relocated.
                 * This depends on the {@link VmHeapAccess} already existing.
                 */
                classAccess = VmClassAccess.make(this, epoch);

                /**
                 *  Can only fully initialize the {@link VmHeapAccess} once
                 *  the {@link VmClassAccess} is fully created, otherwise there's a cycle.
                 */
                heapAccess.initialize(epoch);

                // Now set up the initial map of the compiled code cache
                codeCacheAccess = new VmCodeCacheAccess(this);
                codeCacheAccess.initialize(epoch);

                nativeCodeAccess = new NativeCodeAccess(this);

                // Locate the root object in the VM that holds the VM's configuration.
                // We can determine most things from the local instance, but the remote
                // object is needed for references to specific objects in the VM.
                teleMaxineVM = (TeleMaxineVM) objects().makeTeleObject(fields().MaxineVM_vm.readRemoteReference(this));
                teleVMConfiguration = teleMaxineVM.teleVMConfiguration();

                if (isAttaching()) {
                    // Check that the target was run with option MakeInspectable otherwise the dynamic heap info will not be available
                    TeleError.check((fields().Inspectable_flags.readInt(this) & Inspectable.INSPECTED) != 0, "target VM was not run with -XX:+MakeInspectable option");
                    classAccess.processAttachFixupList();
                }

                // read the list of actual VmThreadLocal values from the target
                TeleThreadLocalsArea.Static.values(this);

                // At this point everything should be read to go; handle any requests
                // for late initialization.
                for (InitializationListener listener : initializationListeners) {
                    listener.initialiationComplete(epoch);
                }
            }

            // The standard update cycle follows; it is sensitive to ordering.
            // The general ordering is:
            // 1. Identify any new memory locations that can hold objects and/or code
            // 2. Update any existing remote object references or code pointers, based
            //    on the state of the manager for each region.  As much as possible, this
            //    should be independent of object state, since that hasn't been updated yet.
            //    In cases where there are dependencies, steps must be taken to avoid
            //    circularities:  (a) have the state be static in the boot heap, (b) have
            //    the state be in some other non-managed heap, (c) force any depended-upon
            //    objects to refresh before depending on their state.
            // 3. Update information about classes loaded since the previous refresh; this
            //    will be needed to model correctly any newly allocated objects or references
            //    of the newly-loaded types.
            // 4. Update the status, including any cached information, concerning every
            //    remote heap object. Remote object state can depend on just about everything else,
            //    especially the location and status of every allocated memory region, their
            //    management(GC) status, and any remote object references that point into those
            //    regions.
            // 5. Update the status of remote code pointers and remote object references that
            //    point at objects in code cache memory.

            // Update status of the heap:  any new heap allocations, the management (GC) status
            // of each region, and updating any references whose state may have changed.
            heapAccess.updateMemoryStatus(epoch);

            // Update the general status of the code cache, including eviction status and any new
            // allocations.  This also includes updating existing remote object references that
            // point into any code cache that is managed.  This latter requirement creates a
            // circularity, since some remote object references depend on the status of objects
            // (TeleTargetMethod)s in the dynamic heap, which will not have been refreshed yet.
            // This is resolved by having any such update to a reference first force an explicit
            // object refresh on any TaleTargetMethod whose state matters.  That refresh, in turn,
            // depends on the reference to that TeleTargetMethod having been updated.  That creates
            // the requirement that references in the dynamic heap be updated before any references
            // that might be in the code cache, i.e. why this update follows the heap update.
            codeCacheAccess.updateMemoryStatus(epoch);

            // Update the general status of any native, dynamically loaded libraries in the address space
            nativeCodeAccess.updateMemoryStatus(epoch);

            // Update registry of loaded classes, so we can understand object types
            classAccess.updateCache(epoch);

            // Update every local surrogate for a VM object.
            // All these updates depend on remote object references, all of which must have been
            // refreshed earlier.
            objectAccess.updateCache(epoch);

            // Detailed update of the contents of every code cache region, as well as information about native code.
            machineCodeAccess.updateCache(epoch);

            // Check the status of breakpoints, for example if any are set in recently evicted compilations.
            // This requires that the status of any managed code cache has already been updated.
            breakpointManager.updateCache(epoch);

            // At this point in the refresh cycle, we should be current with every VM-allocated memory region.
            // What's not done yet is updating the thread memory regions, which happens by refresh calls in TeleProcess.

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

    /**
     * {@inheritDoc}
     * <p>
     * Note that this implementation does not use the most current
     * information if called during the VM refresh cycle.
     */
    public final boolean contains(Address address) {
        return teleVMState.findMemoryRegion(address) != null;
    }

    public final TeleObject representation() {
        return teleMaxineVM;
    }

    public final String getVersion() {
        return MaxineVM.VERSION_STRING;
    }

    public final String getDescription() {
        return MaxineVM.description();
    }

    public final VmPlatform platform() {
        return platform;
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

    public final File vmLogFile() {
        return vmLogFile;
    }

    public final File programFile() {
        return programFile;
    }

    public final MaxInspectionMode inspectionMode() {
        return mode;
    }

    public final VmClassAccess classes() {
        return classAccess;
    }

    public final VmAddressSpace addressSpace() {
        return addressSpace;
    }

    public final VmMemoryIO memoryIO() {
        return memoryIO;
    }

    public final VmObjectAccess objects() {
        return objectAccess;
    }

    public final VmReferenceManager referenceManager() {
        return referenceManager;
    }

    public final VmHeapAccess heap() {
        return heapAccess;
    }

    public final VmMethodAccess methods() {
        return methodAccess;
    }

    public final VmCodeCacheAccess codeCache() {
        return codeCacheAccess;
    }

    public final NativeCodeAccess nativeCode() {
        return nativeCodeAccess;
    }

    public final VmCodeLocationManager codeLocations() {
        return codeLocationManager;
    }

    public final VmMachineCodeAccess machineCode() {
        return machineCodeAccess;
    }

    public final VmBreakpointManager breakpointManager() {
        return breakpointManager;
    }

    public final VmWatchpoint.VmWatchpointManager watchpointManager() {
        return watchpointManager;
    }

    public final VmThreadAccess threadManager() {
        return threadAccess;
    }

    public final TeleVMLog vmLog() {
        return TeleVMLog.getVMLog(this);
    }

    /**
     * Register an action that will take place once, all of the initialization for a remote inspection session
     * are substantially complete; some services have needs for setup that can only happen very
     * late, most notably anything that requires setting a breakpoint.
     */
    public final void addInitializationListener(InitializationListener initializationListener) {
        initializationListeners.add(initializationListener);
    }

    /**
     * Returns the most recently notified VM state.  Note that this
     * isn't updated until the very end of a refresh cycle after VM
     * halt, so it should be considered out of date until the refresh
     * cycle is complete.  This is especially important when making
     * decisions concerning the process epoch.
     * <p>
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

    /**
     * {@inheritDoc}
     * <p>
     * Registering this listener will cause one or more breakpoints to be created, if they don't exist,
     * so this must be called after all the other inspection services are in place.
     */
    public final void addGCPhaseListener(HeapPhase phase, MaxGCPhaseListener listener) throws MaxVMBusyException {
        if (phase == null) {
            gcAnalyzingListeners.add(listener, teleProcess);
            gcReclaimingListeners.add(listener, teleProcess);
            gcMutatingListeners.add(listener, teleProcess);
        } else {
            switch (phase) {
                case ANALYZING:
                    gcAnalyzingListeners.add(listener, teleProcess);
                    break;
                case RECLAIMING:
                    gcReclaimingListeners.add(listener, teleProcess);
                    break;
                case MUTATING:
                    gcMutatingListeners.add(listener, teleProcess);
                    break;
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Registering this listener will cause one or more breakpoints to be created, if they don't exist,
     * so this must be called after all the other inspection services are in place.
     */
    public final void removeGCPhaseListener(MaxGCPhaseListener listener, HeapPhase phase) throws MaxVMBusyException {
        if (phase == null) {
            gcAnalyzingListeners.remove(listener);
            gcReclaimingListeners.remove(listener);
            gcMutatingListeners.remove(listener);
        } else {
            switch (phase) {
                case ANALYZING:
                    gcAnalyzingListeners.remove(listener);
                    break;
                case RECLAIMING:
                    gcReclaimingListeners.remove(listener);
                    break;
                case MUTATING:
                    gcMutatingListeners.remove(listener);
                    break;
            }
        }
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

    public final MaxMemoryManagementInfo getMemoryManagementInfo(Address address) {
        return heapAccess.getMemoryManagementInfo(address);
    }

    /**
     * Acquires a lock on the VM process and related cached state; blocks until lock
     * can be acquired.  The lock is reentrant, so that nested lock acquisition behaves with
     * standard Java synchronization semantics.
     */
    public final void lock() {
        lock.lock();
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
     * Sets or clears some bits of the {@link Inspectable} field in the VM process.
     * <p>
     * Must be called in a thread holding the VM lock.
     *
     * @param flags specifies which bits to set or clear
     * @param set if {@code true}, then the bits are set otherwise they are cleared
     */
    public final void modifyInspectableFlags(int flags, boolean set) {
        assert lockHeldByCurrentThread();
        int newFlags = fieldAccess.Inspectable_flags.readInt(this);
        if (set) {
            newFlags |= flags;
        } else {
            newFlags &= ~flags;
        }
        fieldAccess.Inspectable_flags.writeInt(this, newFlags);
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
     * This gets called during the VM refresh cycle and the results are cached along with all other
     * known allocations.
     *
     * @return a list of platform-specific memory regions, empty if none.
     */
    protected List<MaxEntityMemoryRegion<? extends MaxEntity> > platformMemoryRegions() {
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
     * <p>
     * <strong>Notes:</strong> This data gets posted only at the very end of the VM update
     * cycle, and so should not be relied upon during the update.
     *
     * @param processState the new process state
     * @param epoch
     * @param singleStepThread the thread, if any, that just completed a single step
     * @param threads currently existing threads
     * @param threadsStarted threads newly created since last notification
     * @param threadsDied threads newly died since last notification
     * @param breakpointEvents breakpoint events, if any, that caused this state change
     * @param watchpointEvent watchpoint, if any, that caused this state change
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
                    VmWatchpointEvent watchpointEvent) {

        this.teleVMState = new TeleVMState(
            mode,
            processState,
            epoch,
            addressSpace.allocations(),
            threads,
            singleStepThread,
            threadsStarted,
            threadsDied,
            breakpointEvents,
            watchpointEvent,
            heapAccess.phase(), codeCacheAccess.isInEviction(), teleVMState);
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
        return fields().Trace_level.readInt(this);
    }

    public final void setVMTraceLevel(int newLevel) {
        fields().Trace_level.writeInt(this, newLevel);
    }

    public final long getVMTraceThreshold() {
        return fields().Trace_threshold.readLong(this);
    }

    public final void setVMTraceThreshold(long newThreshold) {
        fields().Trace_threshold.writeLong(this, newThreshold);
    }

    public final HeapScheme heapScheme() {
        return vmConfiguration.heapScheme();
    }

    public final LayoutScheme layoutScheme() {
        return vmConfiguration.layoutScheme();
    }

    public final RemoteReferenceScheme referenceScheme() {
        return (RemoteReferenceScheme) vmConfiguration.referenceScheme();
    }

    public final RemoteReference makeReference(Address origin) {
        return referenceManager.makeReference(origin);
    }

    public final RemoteReference makeQuasiObjectReference(Address origin) {
        return referenceManager.makeQuasiReference(origin);
    }

    public final ReferenceValue createReferenceValue(RemoteReference reference) {
        return referenceManager.createReferenceValue(reference);
    }

    /**
     * Returns a local copy of a {@link String} object in the VM's heap.
     *
     * @param stringRef A {@link String} object in the VM.
     * @return A local {@link String} duplicating the object's contents.
     * @throws InvalidReferenceException if the argument does not point a valid heap object.
     */
    public final String getString(RemoteReference stringRef) throws InvalidReferenceException {
        return TeleString.getString(this, stringRef);
    }

    /**
     * Returns a local copy of the contents of a {@link String} object in the VM's heap,
     * using low level mechanisms and performing no checking that the location
     * or object are valid.
     * <p>
     * The intention is to provide a fast, low-level mechanism for reading strings that
     * can be used outside of the AWT event thread without danger of deadlock,
     * for example on the canonical reference machinery.
     * <p>
     * <strong>Unsafe:</strong> this method depends on knowledge of the implementation of
     * class {@link String}.
     *
     * @param origin a {@link String} object in the VM
     * @return A local {@link String} duplicating the remote object's contents, null if it can't be read.
     */
    public String getStringUnsafe(Address origin) {
        return TeleString.getStringUnsafe(this, origin);
    }

    public final List<MaxObject> inspectableObjects() {
        final List<MaxObject> inspectableObjects = new ArrayList<MaxObject>();
        inspectableObjects.add(teleMaxineVM);
        inspectableObjects.add(teleVMConfiguration);
        try {
            inspectableObjects.add(objectAccess.vmBootClassRegistry());
        } catch (MaxVMBusyException e) {
        }
        inspectableObjects.addAll(objectAccess.schemes());
        inspectableObjects.addAll(codeCacheAccess.codeCacheInspectableObjects());
        inspectableObjects.addAll(heapAccess.heapInspectableObjects());
        return inspectableObjects;
    }


    public final List<MaxCodeLocation> inspectableMethods() {
        final List<MaxCodeLocation> inspectableMethods = new ArrayList<MaxCodeLocation>(methods().clientInspectableMethods());
        inspectableMethods.addAll(heapAccess.heapInspectableMethods());
        return inspectableMethods;
    }

    public final <TeleMethodActor_Type extends TeleMethodActor> TeleMethodActor_Type findTeleMethodActor(Class<TeleMethodActor_Type> teleMethodActorType, MethodActor methodActor) {
        final TeleClassActor teleClassActor = classAccess.findTeleClassActor(methodActor.holder().typeDescriptor);
        if (teleClassActor != null) {
            for (TeleMethodActor teleMethodActor : teleClassActor.getTeleMethodActors()) {
                final MethodActor actor = teleMethodActor.methodActor();
                if (actor != null && actor.equals(methodActor)) {
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
        try {
            final MachineCodeLocation entryLocation = codeLocations().createMachineCodeLocation(startEntryAddress, "vm start address");
            runToInstruction(entryLocation, true, false);
        } catch (InvalidCodeAddressException exception) {
            TeleError.unexpected("Unable to set breakpoint at Java entry point " + exception.getAddressString() + ": " + exception.getMessage());
        } catch (Exception exception) {
            throw new IOException(exception);
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

    public final File findJavaSourceFile(ClassActor classActor) {
        final String sourceFilePath = classActor.sourceFilePath();
        return sourcepath.findFile(sourceFilePath);
    }

    public final void executeCommandsFromFile(String fileName) {
        FileCommands.executeCommandsFromFile(this, fileName);
    }

    /**
     * Gets the configuration descriptor instance on the VM; the local clone can be used
     * for most things, but for references to other VM objects we require the remote object.
     *
     * @return the VM object that holds configuration information.
     */
    public final TeleVMConfiguration teleVMConfiguration() {
        return teleVMConfiguration;
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
     * Tries to find a JDWP ObjectProvider that represents the object that is
     * referenced by the parameter.
     *
     * @param reference
     *            a reference to the object that should be represented as a JDWP
     *            ObjectProvider
     * @return a JDWP ObjectProvider object or null, if no object is found at
     *         the address specified by the reference
     */
    private ObjectProvider findObject(RemoteReference reference) {
        return objects().makeTeleObject(reference);
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
                return jdwpAccess.createObjectProviderValue(findObject((RemoteReference) value.asReference()));
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
                return TeleReferenceValue.from(this, ((TeleObject) p).reference());
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
            final BytecodeLocation methodCodeLocation = codeLocations().createBytecodeLocation(teleClassMethodActor, 0, "");
            try {
                TeleVM.this.breakpointManager().makeBreakpoint(methodCodeLocation);
            } catch (MaxVMBusyException maxVMBusyException) {
                TeleError.unexpected("breakpoint creation failed");
            }
            Trace.line(TRACE_VALUE, tracePrefix() + "Breakpoint set at: " + methodCodeLocation);
        }

        public void removeBreakpoint(JdwpCodeLocation codeLocation) {
            if (codeLocation.isMachineCode()) {
                MachineCodeLocation location = null;
                try {
                    location = codeLocations().createMachineCodeLocation(Address.fromLong(codeLocation.position()), "jdwp location");
                    final MaxBreakpoint breakpoint = TeleVM.this.breakpointManager().findBreakpoint(location);
                    if (breakpoint != null) {
                        breakpoint.remove();
                    }
                } catch (MaxVMBusyException maxVMBusyException) {
                    TeleError.unexpected("breakpoint removal failed");
                } catch (InvalidCodeAddressException e) {
                    TeleError.unexpected("bad breakpoint address");
                }
            }
            assert breakpointLocations.contains(codeLocation);
            breakpointLocations.remove(codeLocation);
            assert !breakpointLocations.contains(codeLocation);
        }

        public byte[] accessMemory(long start, int length) {
            final byte[] bytes = new byte[length];
            TeleVM.this.memoryIO().readBytes(Address.fromLong(start), bytes);
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
                result[i] = TeleVM.this.machineCode().findCompilation(Address.fromLong(addresses[i])).teleTargetMethod();
            }
            return result;
        }

        public ReferenceTypeProvider[] getAllReferenceTypes() {
            return classAccess.teleClassActors();
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
            return HostedVMClassLoader.HOSTED_VM_CLASS_LOADER.classpath().toStringArray();
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
                referenceTypeProvider = TeleVM.this.classes().findTeleClassActor(klass);
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
            for (TypeDescriptor typeDescriptor : TeleVM.this.classes().typeDescriptors()) {
                if (typeDescriptor.toString().equals(signature)) {
                    final TeleClassActor teleClassActor = TeleVM.this.classes().findTeleClassActor(typeDescriptor);

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

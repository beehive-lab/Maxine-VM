Schemes: Interfaces for Maxine VM Configuration
===============================================

The Maxine VM is designed to be highly configurable, and it exploits
standard Java language features as much as possible to accomplish
this.
Maxine schemes are Java interfaces that define the interaction between a
subsystem and the rest of the VM.

This design encourages the creation of alternate implementations of
those schemes, for example to achieve different performance
characteristics.
It also enables the creation of specialized implementations for
development and testing, for example monitors that have no effect, a
heap that doesn't collect, or any implementation with extensive internal
checking and tracing.

The current design of schemes where by all logic is expressed purely in
Java code is heavily tied to the use of Java snippets which, in turn,
are only supported by the C1X compiler.
Given that this compiler is scheduled to be deprecated and replaced by
the Graal compiler, the design of schemes will be re-visited to work
with Graal.

VM configuration
----------------

All Maxine schemes extend the interface ``com.sun.max.vm.VMScheme``.

A specific implementation of a scheme is by convention located in its
own package and constitutes a Maxine Package.
It is documented, following Javadoc convention, by a file named
``package-info.java`` in the package directory.
It may also contain a class named ``Package``, which may contain
additional configuration information.

A complete VM configuration includes bindings to a specific
implementation of each scheme, specified as command line options,
processed by the class ``VMConfigurator``, and represented during boot
image generation and VM runtime by an instance of class
``VMConfiguration``.

The Boot Image Inspector view in the :doc:`Maxine Inspector <./Inspector>`
displays, among the other information about the specific boot image
being viewed, the specific bindings for each of Maxine's schemes.

.. image:: images/Inspector-BootImage.jpg

Scheme Initialization
---------------------

Each scheme implementation is notified by a call to the method
``initialize(MaxineVM.Phase phase)`` when the VM enters different phases
of its lifecycle.
These phases are defined in the enum Phase in class
``com.sun.max.vm.MaxineVM``.

Phase ``BOOTSTRAPPING`` is assigned during
:doc:`boot image generation <./Boot-Image>` when the
scheme implementation is loaded, something of a misnomer since the term
:doc:`bootstrapping <./Boot-Image>` is generally used to describe the startup
sequence of the VM.

The following list describes the initialization calls that each scheme
will receive, in order, during startup, along with the presumed state of
the VM in each instance:

-  ``PRIMORDIAL`` VM code has started executing, but many features do
   not
   work yet.
-  ``PRISTINE`` Java thread synchronization has become operational, but
   may not do anything yet.
-  ``STARTING`` the VM is functional (i.e. threads and heap), but the
   JDK
   is not yet operational.
-  ``RUNNING`` any necessary re-initializion of JDK classes is complete,
   system properties have been processed, all pure Java language
   features are operational, and the VM is about to start executing
   application code.
-  ``TERMINATING`` the VM is about to terminate, many VM features have
   shut down, and this the last chance to interpose (but with very
   limited VM functionality).

Object Layout (LayoutScheme)
----------------------------

An implementation of the scheme defined by interface
``com.sun.max.vm.layout.LayoutScheme`` configures how objects are
represented in memory, including header and fields.
See :doc:`Object layout <./Objects>` for more details.

Object References (ReferenceScheme)
-----------------------------------

An implementation of the scheme defined by interface
``com.sun.max.vm.reference.ReferenceScheme`` configures how objects are
accessed for mutator use, for example direct pointers or handles.
The default binding is ``DirectReferenceScheme``.

Note also that the :doc:`Inspector <./Inspector>`, which runs in a separate
process than the VM, is able to reuse a considerable amount of VM code
in a uniform way by creating a pseudo-configuration and binding a custom
implementation of ``ReferenceScheme`` that encapsulates a boxed address in
the address space of the VM process.

Heap allocation and garbage collection (HeapScheme)
---------------------------------------------------

An implementation of the scheme defined by interface
``com.sun.max.vm.heap.HeapScheme`` provides basic heap memory management,
object allocation, and garbage collection.

Different implementations may include very different kinds of
collectors.
For example, the default binding is ``GenSSHeapScheme``, which implements
a simple generational collector.
Another binding is ``SemiSpaceHeapScheme``, which implements a
straightforward semi-space collector.
A third heap scheme is the ``MSHeapScheme``, which implements a pure
mark-sweep algorithm.

For certain kinds of testing it can be useful to bind implementations
with limited or specialized functionality, for example with an
implementation that allocates but never collects or one with extensive
heap checking or tracing.

Thread Synchronization (MonitorScheme)
--------------------------------------

An implementation of the scheme defined by interface
``com.sun.max.vm.monitor.MonitorScheme`` supports synchronization in the
VM.

The interface represents an abstraction of monitors.
It specifically includes translation of the ``monitorenter`` and
``monitorexit`` bytecodes, as well as the implementation of ``wait`` and
``notify`` methods.
Implementations might include thin locks, biased locking, hybrids, etc.

Some experimentation has already been done in this area, and the
implementation currently in use is part of a framework for "n-modal"
locking schemes, a generalization of bimodal (as in JikesRVM) and
trimodal designs.
The framework is implemented by abstract class ``ModalMonitorScheme`` in
package ``com.sun.max.vm.monitor.modal.schemes``.
The default binding at present (implemented by class
``ThinInflatedMonitorScheme``) transitions between thin locks and inflated
native monitors.

For certain kinds of testing it can be useful to disable monitor
checking completely; this can be done by binding the class
``IgnoreMonitorScheme`` into the VM configuration.

VM startup sequence (RunScheme)
-------------------------------

An implementation of the scheme defined by interface
``com.sun.max.vm.run.RunScheme`` is invoked by the VM after it has started
basic services and is ready to set up and run a language environment
such as Java or some other language.

The default binding is the standard Java runtime: ``JavaRunScheme`` (in
package ``com.sun.max.vm.run.java``) starts up normal JDK services (a
somewhat delicate piece of business), and then loads and runs a
user-specified Java class.

Compiler strategy (CompilationBroker)
-------------------------------------

The class ``com.sun.max.vm.compiler.CompilationBroker`` implements an
adaptive compilation system with multiple compilers with different
compilation time / code quality tradeoffs.
It encapsulates the necessary infrastructure for recording profiling
data, selecting what and when to recompile, etc.

The class ``CompilationBroker`` can be subclassed by using the
``max.CompilationBroker.class`` system property with the boot image
generator.

This is done as follows:

::

    max -J/a-Dmax.CompilationBroker.class=com.acme.MyCompilationBroker image ...

**Note:** that the ``CompilationBroker`` is going to be removed as well
and be replaced by
the `JVM Compiler Interface (JVMCI) <http://openjdk.java.net/jeps/243>`__.

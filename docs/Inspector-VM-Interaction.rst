How the Inspector interacts with the Maxine VM
==============================================

This page describes how the :doc:`Maxine Inspector <./Inspector>`'s
interaction with a running VM is *implemented*.

General goals for the Inspector all deal with making development and
experimentation in the Maxine VM as productive and widely accessible as
possible:

-  Support basic debugging of the Maxine VM, something not possible
   with standard tools.
-  Make visible as much internal VM state as possible, both in terms of
   design abstractions and in terms of low-level representations, even
   when the VM is broken.
-  Provide new developers with a fast path to understanding VM
   internals.

A few general strategies guide the Inspector's implementation:

-  Run in a separate process (usually local, but see Guest VM), so that
   inspection depends neither on a correctly running VM process nor
   there being any process that all.
-  Require as little active support from the VM as possible, in
   particular require no active agent.
-  Reuse as much VM code as possible, especially
   reading/writing/understanding the low-level representations of data
   on the target platform (possibly different than the platform on
   which the Inspector runs).
-  Load VM classes into the Inspector for reflective use in
   understanding VM data.
-  Rely on platform-specific implementations for low-level interaction
   with a running VM: process control, threads, breakpoints, access to
   memory and registers.

Low-level VM Interaction
------------------------

This section describes the Inspector's access to the lowest level
abstractions in the VM process, namely the resources provided by the
underlying platform: memory, threads, and execution state.

Process control
~~~~~~~~~~~~~~~

One of the most difficult and frustrating parts of the Inspector's
implementation is the need to implement low-level process controls on
the several supported platforms.
These controls include reading and writing from the VM's memory, finding
and inspecting threads, setting breakpoints, setting watchpoints, and
deciphering process state.

Generic controls are implemented in class
``com.sun.max.tele.debug.TeleProcess``.
Concrete subclasses using native methods implement the controls for
specific platforms:

-  *Solaris*: platform support is best on Solaris, where libproc
   provides a programmatic interface to the Solaris ``/proc``
   pseudo-filesystem.
   Watchpoints are supported with no limit on their number (see
   ``Native/tele/darwin/*.[ch]``).
-  *Linux*: the Inspector uses a mixture of ``ptrace(2)`` and ``/proc``
   (see ``Native/tele/linux/*.[ch]``).
-  *Mac OS X*: on the Mac the Inspector uses a mixture of ``ptrace(2)``
   and the Mach API (see ``Native/tele/darwin/*.[ch]``).
-  *Guest VM*: the Guest VM variant of the VM runs in a Xen domain
   where such OS services are unavailable, so controls must be
   implemented using Xen inter-domain communication.

This code can be very subtle.
It now seems to work fairly reliably, but at the cost of many hours
deciphering non-documentation and gdb source code.
In our experience, programming a debugger is a very niche activity.

Reading and writing VM memory
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Low-level memory access is implemented using basic process control
methods in class ``TeleProcess``:

::

    read(Address address, BtyeBuffer buffer, int offset, int length)
    write(Address address, ByteBuffer buffer, int offset, int length)

However, interpreting the bits presents more of a challenge, since this
must be done for a VM running on a potentially different
platform.
Fortunately, the Inspector is able to load the Java classes that
describe the target platform and then reuse the VM's own code for
reading and writing bits representing the VM's internal primitive data
types.
Methods for reading and writing those types appears in interface
``com.sun.max.tele.data.DataAccess``, and all but the lowest-level read
methods are implemented by class
``com.sun.max.tele.data.DataAccessAdapter``.

For performance reasons, especially for non-local debugging such as with
the Guest VM, the Inspector caches pages of memory read since the most
recent process execution (see class
``com.sun.max.tele.page.PageDataAccess``).

Logging
~~~~~~~

The Inspector's low-level interaction with the VM process can be
observed.
See :doc:`Low-level logging <./Debugging>` for instructions
on enabling all low-level VM logging.
In order to observe only Inspector-related events, change ``log_TELE`` to 1
in ``Native/share/log.h``, rather than ``log-ALL``.

Passive VM support
------------------

Although the Inspector is designed to rely as little as possible on the
internals of the VM, there are a number of ways in which the VM is
constructed to make inspection as easy as possible.
The mechanisms described in this section incur zero runtime overhead in
the VM, and involve no writing into VM memory.

Locating critical VM resources
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The Inspector leverages considerable knowledge of the VM's internal data
representations to build its model of VM state, but it must have
somewhere to start when beginning to read from a memory image.
The boot image generator stores in
the boot image header a number of
addresses and other data that help the Inspector (and VM) find things.
These addresses get relocated, along with the contents of the heap,
during :doc:`Bootstrap <./Glossary>`.
The Inspector leverages detailed knowledge of the header's contents in
order to locate, among others:

-  the VM's :doc:`schemes <./Schemes>` bindings, which are loaded into the
   Inspector
-  the :doc:`boot heap <./Boot-Image>`
-  the boot code region
-  the class registry
-  the list of dynamically allocated heap segments
-  the list of `thread local
   areas <./Threads#thread-locals-area-(tla)>`__
-  the entry location of key methods

Field access
~~~~~~~~~~~~

The Inspector uses a variety of mechanisms to locate instance or class
fields in the heap.
During the Inspector's starting sequence (when little is yet known about
VM state), fields are typically located by relying on specific knowledge
of a few key object types, possibly using Java reflection on the VM
classes (which are all loaded into the Inspector).
This kind of access is relatively unsafe, since it bypasses the type
system in the running VM.
There are more abstract ways to access fields, but they rely on the
Inspector's model of VM's class registry, which must first be created
using the low-level mechanisms.

The simplest way to exploit higher-level field access mechanisms is to
annotate (in VM code) fields of interest using ``@INSPECTED``.
The main method in ``com.sun.max.tele.field.TeleFields`` reads VM sources,
generates field access methods, and writes them back into itself for use
by the Inspector.
These access method implementations hide all the indirections necessary
to read or write field data (taking into account the hardware platform,
the layout being used, the particular representation for the object, and
the class layout) and return values of the desired types.

Method access
~~~~~~~~~~~~~

The Inspector uses a variety of mechanisms to locate methods and their
compilations (either instance or class).
Specific methods can be called out for enhanced access by the Inspector
by annotating (in VM code) those methods using ``@INSPECTED``.
The offline program TeleMethods reads VM sources, generates method
access methods, and writes them into class
``com.sun.max.tele.method.TeleMethods``.
These access method implementations hide all the indirection necessary
to locate the annotated methods and their meta-information.

Method interpretation
~~~~~~~~~~~~~~~~~~~~~

VM methods annotated with ``@INSPECTED`` can be interpreted by the
Inspector (for example, see ``TeleMethodAccess.interpret()``).
Interpretation takes place in the Inspector's process, but in the
execution context of the VM: object references are boxed locations in VM
memory, reading/writing is redirected through VM data access, class ID
lookup is redirected to the Inspector's model of the VM's class
registry, and bytecodes are located using reflection on the VM's code
loaded in the Inspector.

The Inspector's interpreter runs very slowly.
It is used routinely by the Inspector in only a few situations, where VM
data structures to be navigated are too complex (e.g. a hash table) to
be navigated robustly using low-level techniques.
For example, see the Inspector method
``TeleCodeCache.findCompiledCode(Address)``, which interprets remotely the
VM method ``Code.codePointerToTargetMethod(Address)``.

Although the interpreter is in principle capable of writing into VM
memory, it is not used in any situations where this happens.

Active VM support
-----------------

Active VM support for inspection is kept to an absolute minimum, but in
most cases either incur very little VM overhead or are enabled only when
the VM is being inspected.
There are several flavors of support mechanisms:

-  Distinguished fields, usually static, where the VM records
   information exclusively for the consumption by the inspector.
-  Distinguished methods, usually static and usually empty, called by
   VM code exclusively as potential breakpoint locations for the
   inspector; this is a weak kind of event mechanism.
-  Special VM memory locations into which the Inspector writes for
   consumption by specific VM mechanisms.

As a matter of organization, this kind of support is implemented mainly
by VM classes in the package ``com.sun.max.vm.tele``, but it often imposes
some obligations on specific :doc:`scheme <./Schemes>` implementations, for
example to store a value or call a method.
These obligations are increasingly specified and documented in scheme
definitions.

The remainder of this section describes a few areas of active VM support
for inspection.

Enabling inspection support
~~~~~~~~~~~~~~~~~~~~~~~~~~~

Many support mechanisms in the VM operate conditionally, depending on
the value of static method
``com.sun.max.vm.tele.Inspectable.isVmInspected()``.
This predicate checks one of the bits in the static field
``Inspectable.flags`` in VM memory, which can be set in one of two ways:

-  When the VM is started by the Inspector, the Inspector sets that bit
   in VM memory early in its startup sequence (see Inspector method
   ``TeleVM.modifyInspectableFlags()``).
-  When the VM is not started by the Inspector, but when it is
   anticipated that the Inspector might subsequently attach the VM
   process, a command line option to the VM makes it inspectable.

At present, the VM cannot be made inspectable unless this bit is set
early during the VM startup sequence.

Class-related support
~~~~~~~~~~~~~~~~~~~~~

The Inspector tracks every class loaded in the VM, as represented by the
current contents of the VM's ``ClassRegistry``; the Inspector maintains
that information using the Inspector class ``TeleClassRegistry``.

The Inspector initializes its ``TeleClassRegistry`` at VM startup,
effectively identifying the classes already loaded in the boot heap; it
does this by directly reading (using low-level operations that rely on
significant knowledge of the data structures involved) the contents of
the VM's ``ClassRegistry`` in the boot heap. As noted earlier, this data
structure cannot be read using the more abstract, relatively more
type-safe techniques in the inspector because those techniques rely on
type information stored in the ``TeleClassRegistry``.
This is one of many circularities in the Inspector that reflect the
underlying meta-circularity of the Maxine VM.

As the VM loads additional classes dynamically, and when inspection is
enabled, the VM records them using the following static fields in VM
memory:

.. code:: java

    package com.sun.max.vm.tele;

    public final class InspectableClassInfo {
        ...
        @INSPECTED
        private static ClassActor[] classActors;

        @INSPECTED
        private static int classActorCount = 0;
        ...
    }

The Inspector refreshes the ``TeleClassRegistry`` each time the VM process
halts: it checks the VM's count against its cache and reads information
from VM memory about any newly loaded classes.

No provision is made for tracking classes that the VM *unloads*.
In fact, the VM implements class unloading by garbage collection, and a
regrettable consequence of this inspection mechanism is that it prevents
class unloading.
This is by far the most egregious interference visited upon the VM by
the Inspector, and it might be corrected in the future.

Heap-related support
~~~~~~~~~~~~~~~~~~~~

Implementations of the Maxine VM's
:doc:`heap scheme <./Schemes>`
are obliged to make certain calls, as documented and supported by the
scheme's static inner class ``com.sun.max.vm.heap.HeapScheme.Inspect``.
All of these calls delegate to the VM class
``com.sun.max.vm.tele.InspectableHeapInfo``, which provides several kinds
of services when the VM is being inspected (described below): heap
allocations, object relocations, and events.

Allocated heap segments
^^^^^^^^^^^^^^^^^^^^^^^

An inspectable, static field in the VM class
``com.sun.max.vm.tele.InspectableHeapInfo`` holds the list of memory
regions currently allocated as heap segments.
This list is read from VM memory by the Inspector each time the VM
process halts; any additional heap segment allocations to the
information are tracked in the inspector class ``TeleHeap``.
This enables the inspector to make a quick first check about whether a
VM memory location could hold a valid heap object, and permits a
visualization of all memory allocations made by the VM.

Object locations
^^^^^^^^^^^^^^^^

The Inspector tracks heap objects of interest: sometimes because the
user is viewing them, but much more frequently because they represent
vital information about the execution state of the VM.
In the presence of relocating garbage collection that can take place at
any time (with respect to the Inspector), there is no practical way for
the Inspector to track object locations without some support from the
VM.

When the VM is being inspected, it actively supports object tracking by
allocating in VM memory an additional root table: an array of addresses
that are treated by garbage collection implementations as roots to be
updated as needed when objects move.
Entries in this table are treated by the VM as weak references: both to
minimize disruption of VM operation and for the Inspector to discover
when objects have become garbage.
Access to the root table is provided via inspectable static fields in
the VM class ``com.sun.max.vm.tele.InspectableHeapInfo``.

The Inspector checks the root table each time the VM halts.
It does so by reading two static fields in
``com.sun.max.vm.tele.InspectableHeapInfo`` that are incremented by the
garbage collectors: one counts the number of collections initiated so
far and one counts the number of collections completed.
The Inspector compares those two counters with their previous values.
If a new collection has concluded since the last refresh, then the
entire contents of the VM's root table are copied into the Inspector's
cache, where they are available for the Inspector's implementation of
remote object references.
When the Inspector creates a new object Reference, based on a specific
address in the VM's heap, that value is added to an empty slot in the
Inspector's root table cache and is written through to the corresponding
location in the VM's root table.

The Inspector can also observe object relocation directly, if needed, by
setting a breakpoint on the following method:

::

    InspectableHeapInfo.inspectableObjectRelocated(Address oldCellLocation, Address newCellLocation){}

This empty method is called each time an object is relocated and it
exists for just this purpose.

Heap events
^^^^^^^^^^^

The VM makes it convenient for the Inspector to halt the VM process at
certain interesting events.
It does so by creating special methods that are called at those times,
methods that do nothing in the VM, but which are convenient for the
Inspector to set breakpoints.
The VM class ``com.sun.max.vm.tele.InspectableHeapInfo`` contains the
following methods of this sort:

-  ``inspectableGCStarted()``
-  ``inspectableGCCompleted()``
-  ``inspectableObjectRelocated()``
-  ``inspectableIncreaseMemoryRequested()``
-  ``inspectableDecreaseMemoryRequested()``

Code-related support
~~~~~~~~~~~~~~~~~~~~

The Inspector's breakpoint mechanism requires active support from the
Maxine VM's
:doc:`compilation scheme <./Schemes>`.
As a machine-level debugger, the natural kind of breakpoint supported by
the Inspector (and by the underlying platform) is specified in terms of
a memory location in compiled machine code.
However, the Inspector also supports breakpoints specified in terms of a
method's signature, so-called
:doc:`bytecode breakpoints <./Glossary>`.
The Maxine VM runs only compiled code, so a bytecode breakpoint is
understood to mean that there should be a corresponding machine code
breakpoint set in every compilation of the method, present or future.
A bytecode breakpoint can even be set (at location 0) for methods not
yet loaded into the VM.

An early implementation of bytecode breakpoints divided responsibility
for setting these breakpoints: the Inspector set them for existing
compilations and a request was written into a queue in the VM for the
runtime compiler, which would create the machine code breakpoints in any
subsequent compilation.
This approach had an irreconcilable race and was replaced by the simpler
approach of halting the VM immediately after every method compilation.
The Inspector would compare the compiled method against its current list
and set a machine code breakpoint if needed.
This implementation proved to incur too much overhead for non-local
debugging, notably for Guest VM.

The current implementation (see Inspector class
``TeleBytecodeBreakpoint``) halts the VM after method compilations, but
filters those events.
Each time the Inspector's list of bytecode breakpoints changes, the
Inspector writes into VM memory an easily parsed list of textual type
descriptors for those classes for which one or more bytecode breakpoints
are currently set.
Implementations of the VM's
:doc:`compilation scheme <./Schemes>`
are required to call a static notification method in the scheme's static
inner class ``com.sun.max.vm.heap.HeapScheme.Inspect`` at the beginning
and end of each method compilation.
This delegates to VM class ``com.sun.max.vm.tele.InspectableCodeInfo``,
where the current list of classes is consulted.
If the class of the method just compiled is in the list, it results in a
call to the empty method ``inspectableCompilationEvent()`` where the
Inspector can set a breakpoint.
Filtering only by class, not by method, results in some false positives,
but the mechanism is simple, fully synchronous, and reduces the
interruptions more than enough.

Inspector evolution
-------------------

The Inspector's life began long before the Maxine VM could run usefully,
a period during which the novel meta-circular, highly modular
architecture was refined and techniques for generating the Maxine
:doc:`boot image <./Boot-Image>` developed. The Inspector's original role was
static visualization and exploration of the binary boot image in terms
of the higher level abstractions of the VM, something that could not be
done by any existing tool.

As the VM became increasingly able to run through its startup
(:doc:`bootstrap sequence <./Glossary>`), basic debugging features
were added: process controls and breakpoints, along with register and
stack visualization.
The Inspector remained monolithic (with no model/view separation) and
single-threaded (the GUI froze during VM process execution).

As the VM began to execute application code, work on the Inspector
proceeded incrementally along several fronts simultaneously:

-  *features on demand*: as the VM became more functional and the
   concerns of the development team evolved, many more features were
   added: additional views of internal state, more debugging controls,
   more user options, etc.
   These were, and continue to be, demand-driven according to the needs
   of the project.
-  *UI functionality and consistency*: the early window implementations
   were rewritten for code reuse and standardized around new
   conventions, the menu system was standardized and extended, Java
   Look & Feel compliance was added, and more.
-  *re-architecting internals*: model/view separation was added, direct
   interaction among views was replaced by a user event model, change
   propagation was refined, generalized notion of user selection
   defined, etc.

Once model/view separation became explicit in the previously monolithic
code base, the Inspector sources were incrementally split into two
"projects" with distinct concerns:

-  **Tele**: responsible for communicating with and managing the VM
   process, essentially being the keeper of the model of the VM's state
   at any point during the session.
-  **Inspector**: responsible for user interaction, state
   visualization, and command handling.

Dependence between the two projects eventually became one-way, but
remained complex: the ``Inspector`` project depends directly on many
implementation classes from both the ``Tele`` and VM projects.
A subsequent effort to further separate the two by re-engineering around
new, well-documented interfaces is only partially complete.

As the Inspector evolved into a heavily used debugger, demand grew for
multi-threaded management of the VM process so that the GUI would remain
live and in particular so that a user could interrupt ("Pause") a
running VM. Concurrent operation is now supported, but the retrofit
(over complex, distributed interactions in the reading and modeling of
VM state) is incomplete Occasional concurrency problems appear as the VM
and Inspector evolve.

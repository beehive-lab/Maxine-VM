Threads in the Maxine VM
========================

Maxine threads are implemented using native threads of the underlying
operating system, in contrast to the "green threads" approach.
Each thread is represented in the VM as an instance of class
``com.sun.max.vm.thread.VmThread``.

The *Threads Inspector View* of the :doc:`Maxine Inspector <./Inspector>`
displays information about all currently existing threads in the VM.

.. image:: images/Inspector-Threads.jpg

The main thread
---------------

The main thread running in a new Maxine VM process executes the
preliminary steps taken during VM startup, up until the creation and
running of the main Java thread.
The C main function runs on this thread.

The main thread (which appears initially in the Inspector as an "unnamed
native" thread), eventually becomes the thread on which the Java main
thread runs.

Thread local memory
-------------------

The VM allocates an area of memory for each thread known as the thread
locals block, separate from the thread's stack.
This area includes copies of thread local variables for VM internal use,
and a stack reference map, among others.

Thread local variables
~~~~~~~~~~~~~~~~~~~~~~

The VM provides an extensible mechanism for allocating per-thread
variables, often referred to simply as *thread locals*.
This mechanism is used by VM internals; it should not be confused with
language-level Thread local storage, which is supported in Java by the
``class java.lang.ThreadLocal``.

Each instance of class ``com.sun.max.vm.thread.VmThreadLocal``
automatically creates a per-thread word-length variable with a
well-defined read/write protocol.
That protocol depends on each variable's nature (an instance of the enum
``com.sun.max.vm.thread.VmThreadLocal.Nature``), which is fixed at
creation.
Many thread local variables are created as static members of the
defining class ``VmThreadLocal``.
Thread locals are also defined by other parts of the VM as needed.

Each thread local has a fixed name, assigned at creation, by which it
can be referenced within the VM, along with a ``boolean`` that specifies
whether the variable will hold references.
Each variable is also assigned a description, a terse human-readable
description of the variable's purpose that is accessed by the Maxine
Inspector and made available to users in the Thread Locals View.

.. image:: images/Inspector-ThreadLocals.jpg

Thread locals area (TLA)
~~~~~~~~~~~~~~~~~~~~~~~~

During boot image generation, a contiguous block of memory known as the
thread locals area (TLA) is defined to contain a word for each
thread local variable, and each variable is assigned an offset into the
TLA.
The first location in each TLA is reserved for the "safepoint latch".

Three TLAs (with identical layout) are defined for each thread, one
corresponding to each of the VM's three safepoint states: Enabled,
Disabled, and Triggered.
This design permits efficient implementation of both safepoints and
thread locals access.
Since a dedicated register (R14 on x64) points to the TLA for the
current safepoint state, this means both safepoints and thread local
variable access can be performed with one or two loads and, more
importantly, without control flow operations.

The base location of the three TLAs is recorded in the thread locals
named ``ETLA``, ``DTLA``, and ``TTLA`` respectively.

Stack reference map
^^^^^^^^^^^^^^^^^^^

Each thread has an associated stack reference map, a data structure that
identifies the thread's stack locations that hold references to the
heap.
A ``StackReferenceMapPreparer`` prepares reference maps on demand (using a
stack walk, see "Stack reference map preparation"), just after a thread
has entered the frozen state (and is therefore at a safepoint) because
of a GC operation.

The map uses one bit per word on the stack so it is about 3% of the
stack size on a 32-bit system and about 1.5% on a 64-bit system.

See class ``com.sun.max.vm.stack.StackReferenceMapPreparer``.

Thread locals block (TLB)
~~~~~~~~~~~~~~~~~~~~~~~~~

Per-thread VM storage is in a separate thread locals block (TLB) that is
allocated in native code and freed by the native thread library
mechanism for destructing thread specific keys (e.g. the second argument
of ``pthreadkeycreate(3)``).
This permits attaching native threads via JNI, where there is no way to
carve out a piece of the stack for the VM.

The TLB includes not only three Thread locals areas (TLAs) but also
other thread-specific data such as the stack reference map.
The layout of the TLB is shown in the following diagram, copied from the
JavaDoc comments for class ``com.sun.max.vm.thread.VmThreadLocal``:

::

    (low addresses)

      page aligned --> +---------------------------------------------+
                       | X X X          unmapped page          X X X |
                       | X X X                                 X X X |
      page aligned --> +---------------------------------------------+
                       |               TLA (triggered)               |
                       +---------------------------------------------+
                       |               TLA (enabled)                 |
                       +---------------------------------------------+
                       |               TLA (disabled)                |
                       +---------------------------------------------+
                       |           NativeThreadLocalsStruct          |
                       +---------------------------------------------+
                       |                                             |
                       |               reference map                 |
                       |                                             |
                       +---------------------------------------------+

    (high addresses)

You can use the ``-XX:+TraceThreads`` VM option to see the layout of the
stack and TLB for each thread as it starts.

::

    Initialization completed for thread[id=3, name="main", native id=0x100096000]:
    Stack layout:

     +--------- 0x100096000  [+262144]
     |
     | OS thread specific data and native frames [720 bytes, 0.274658%]
     |
     +--------- 0x100095d30 [+261424]
     |
     | Frame of Java methods, native stubs and native functions [257328 bytes, 98.162842%]
     |
     +--------- 0x100057000 [+4096]
     |
     | Stack yellow zone [4096 bytes, 1.562500%]
     |
     +--------- 0x100056000 [+0]
     |
     | Stack red zone [4096 bytes, 1.562500%]
     |
     +--------- 0x100055000 [-4096]

    Thread locals block layout:
     +--------- 0x10083a380  [+9088]
     |
     | reference map [4104 bytes, 45.158451%]
     |
     +--------- 0x100839378 [+4984]
     |
     | native thread locals [80 bytes, 0.880282%]
     |
     +--------- 0x100839328 [+4904]
     |
     | safepoints-disabled thread locals area [272 bytes, 2.992958%]
     |
     +--------- 0x100839218 [+4632]
     |
     | safepoints-enabled thread locals area [272 bytes, 2.992958%]
     |
     +--------- 0x100839108 [+4360]
     |
     | safepoints-triggered thread locals area [272 bytes, 2.992958%]
     |
     +--------- 0x100838ff8 [+4088]
     |
     | unmapped page [4088 bytes, 44.982395%]
     |
     +--------- 0x100838000 [+0]

Safepoints
----------

A safepoint is a special instruction in compiled VM code, at a location
where a thread can be frozen with guaranteed consistency between the
thread's stack and the heap, which is required for safe garbage
collection.
Maxine compilers insert safepoints in branches, goto, and switch
statements.

A safepoint incurs very low overhead in normal operation, but causes a
trap when triggered in the thread; this typically happens when the VM is
preparing for garbage collection.

Abstract class ``com.sun.max.vm.runtime.Safepoint`` is specialized by
subclasses with platform-specific details of safepoint implementation.

Stack overflow detection
------------------------

To implement stack overflow detection (which can result in raising a
``StackOverflowError``), Maxine places guard pages at the limit of the
stack.
More precisely, Maxine uses OS page protection facilities (see
``mprotect(2)``) to make a couple of pages at the end of the stack
non-readable and non-writable.
This enables stack overflow detection to be performed by a single
instruction in the prologue of a method.
Mostly this instruction is effectively a no-op (i.e. has no side-effect
visible to the program).
For example, the following stack banging instruction is used in Maxine
on AMD64 to load a value from a fixed (negative) offset from the stack
pointer:

::

    mov r11, [rsp - 12288] # load from 3 pages below %rsp

To understand how this may cause a trap, consider the following layout
of a thread's stack in Maxine:

::

    High addresses

                           +---------------------------------------------+
                           |          OS thread specific data            |
                           |           and native frames                 |
                           +---------------------------------------------+
                           |                                             |
                           |           Frames of Java methods,           |
         stack pointer --> |              native stubs, and              |
                           |              native functions               |
                           |                                             |
                           +---------------------------------------------+
                           | X X X     Stack overflow detection    X X X |
                           | X X X          (yellow zone)          X X X |
          page aligned --> +---------------------------------------------+
                           | X X X     Stack overflow detection    X X X |
                           | X X X           (red zone)            X X X |
          page aligned --> +---------------------------------------------+

    Low addresses

If the value of ``%rsp - 12288`` lies within the yellow zone, then a
``SIGSEGV`` signal will be raised.
Maxine's VM signal handler will then test whether or not the faulting
address lies within the yellow zone.
If it does, then the protection bits of the yellow zone are modified
such that further reads/writes to this page will not cause a trap.
This should allow the code that allocates and raises a
``StackOverflowError`` to execute without causing stack overflow
itself.
Just before returning to the exception handler, the yellow zone is
re-guarded.
The red zone exists to detect the situation where the stack overflow
raising code uses too much stack.
This is a fatal VM error.
It's also a fatal VM error if stack overflow occurs when execution is in
native code (called via JNI).

Thread local allocation buffer (TLAB)
-------------------------------------

A thread local allocation buffer (TLAB) is a portion of heap storage
reserved for allocation by a single thread.
This allows heap allocation without synchronization, typically via a
simple pointer increment.
Fast access to the thread's TLAB is provided via thread local variables
stored in the `Thread locals area (TLA)`_.
Most object allocation goes via the TLAB of the thread requesting the
allocation first.

When a thread has exhausted its TLAB, it is refilled with a new
one.
TLAB refill decisions are driven by a ``TLABRefillPolicy``.

Because the logic of TLAB management and allocation is common to all
implementations of ``HeapScheme``, it is factored in the adaptor class
``com.sun.max.vm.heap.HeapSchemeWithTLAB``.

Aspects of TLAB management that depend on ``HeapScheme``'s details are
delegated to the concrete implementations.
These includes: handling requests that overflow the TLAB's current free
space, refilling the TLAB with new heap space, actions to be taken on
TLAB refill, making the TLAB parseable at GC safepoint, or the choice of
TLAB refill policy.

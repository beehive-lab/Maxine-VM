VM Operations
=============

A VM operation, implemented by class
``com.sun.max.vm.runtime.VmOperation`` is an operation that can be
performed on one or more target threads by the ``VmOperationThread`` VM
operation thread.
In the normal case a VM operation is performed after the target threads
are frozen at a :doc:`safepoint <./Threads>`, in which case every
frame of a compiled/interpreted method on a frozen thread's stack is
guaranteed to be at an execution point where the complete frame state of
the method is available.
A VM operation is formed by creating an instance of a subclass of
``com.sun.max.vm.runtime.VmOperation`` and invoking the submit method on
the instance.
The behavior of the operation is specified by overriding the ``doThread``
method which, by default does nothing.
I.e, such a default operation would simply freeze the threads, do
nothing, and then release them.
The details of the relationship between the ``VmOperation`` thread and the
target threads is specified by an instance of the ``VmOperation.Mode``
class.
The normal case is indicated by ``Mode.Safepoint``, which causes all
target threads to be frozen and the ``VmOoperation`` thread to block until
the operation completes.
We will focus on the normal case in what follows and defer discussion of
the other, more unusual modes, until later.
Note that the ``VmOperation`` class is only intended for use within the VM
implementation.
Consequently, in the interface, threads are specified by the
``com.sun.max.vm.thread.VmThread`` class and not ``java.lang.Thread``.

A VM operation may target a subset of the threads in the VM, the
degenerate case being a single thread, which is specified explicitly in
the constructor for ``VmOperation`` by the ``singleThread`` argument.
If this value is not null it denotes an operation solely on the
specified thread.
If ``singleThread`` is null it specifies a multi-thread operation.
This design simplifies the single thread case and avoids having to
provide the set of target threads explicitly in the multi-thread
case.
By default the multi-thread variant acts on all threads (except the
``VmOperation`` thread itself).
However, the ``VmOperation`` instance can provide finer control by
overriding the ``operateOnThread`` method.
If this method returns false for any ``VmThread`` passed as argument, that
thread is ignored.
I.e. it will neither be frozen nor have the operation performed.

The following is a trivial example that simply prints the name of each
frozen thread.

.. code:: java

    VmOperation op = new VmOperation("Example", null, Mode.Safepoint) {
        @Override
        public void doThread(VmThread vmThread, Pointer ip, Pointer sp, Pointer fp) {
            System.out.println("Thread " + vmThread.getName() + " is stopped at " + Long.toHexString(ip.toLong()));
        }
    op.submit();

The first argument to the constructor is only used when tracing the
operation for debugging purposes, which can be enabled with the VM
command line argument ``-XX:+TraceVmOperations``.
The three ``Pointer`` arguments to ``doThread`` are the code address at
which the thread is stopped and the stack pointer and frame pointer,
respectively.
These values are generally used in operations that need to access the
execution stack, for example to generate a stack trace.
Note that these values are all guaranteed to be valid, in particular a
thread that has been started but is not yet executing Java code will be
filtered out and not passed to ``doThread``.
Note that the constructor only initializes the instance, no part of the
operation occurs until the submit method is invoked.
**TBA:** can a VMOperation instance be reused?

Many internal operations within the VM are implemented using
``VMOperation``, most notably garbage collection.
So what happens if the code in ``doThread`` allocates memory and happens
to cause a garbage collection?
Note that it can be quite difficult to determine by inspection whether a
method allocates objects.
The above example contains no new keywords, but the string concatenation
does so implicitly.
In fact it is very difficult to write allocation free code, so for
``VmOperation`` to be useful there must be a solution to what is in effect
a nested ``VmOperation``.
Since most operations can cause a garbage collection, ``VmOperation``
supports nested operations by default, but they can be disabled by
invoking the more general constructor that takes a
``disAllowNestedOperations`` argument.

The output from running the above example should look like this:

::

    Thread Signal Dispatcher is stopped at 0x103fcb029
    Thread Finalizer is stopped at 0x103fb2c44
    Thread Reference Handler is stopped at 0x103fb2c44
    Thread main is stopped at 0x103fb2c44

Notice that three non-application (system) threads are included in the
list.
Note also that all but ``Signal Dispatcher`` are stopped at the same
address.
It doesn't matter how many time you run the application, this will
always be the case.
The reason related to the mechanism that is used to freeze the threads,
and is explained in the section on implementation details.

What if we only wanted the ``VmOperation`` to operate on application
threads?
One way, although not a very stable solution, would be to provide an
``Override`` for ``operateOnThread`` that compared the textual names of the
threads.
A better way would be to exploit the fact that system threads exist in a
separate ``ThreadGroup`` from application threads.
For example, this ``operateOnThread`` method would do:

::

    protected boolean operateOnThread(VmThread vmThread) {
        if (!systemThreads) {
            return vmThread.javaThread().getThreadGroup() != VmThread.systemThreadGroup;
        } else {
            return true;
        }
    }

Implementation Details
----------------------

A thread is frozen at a safepoint when it is blocked in native code
(typically on an OS-level lock) and cannot (re)enter
compiled/interpreted Java code without being thawed (see class
``ThawThread``) by the VM operation thread.

Freezing a thread is a co-operative action between the VM operation
thread and the thread(s) being frozen.
There are two alternative implementations of this mechanism
provided.
The first uses atomic instructions and the second uses memory
fences.
They are named ``CAS`` and ``FENCE`` and are described further below.

CAS
~~~

Atomic compare-and-swap (CAS) instructions are used to enforce
transitions through the following state machine:

::

    +------+                            +--------+                                +---------+
    |      |--- M:JNI-Prolog{STORE} --->|        |--- VM:WaitUntilFrozen{CAS} --->|         |
    | JAVA |                            | NATIVE |                                | FROZEN  |
    |      |<--- M:JNI-Epilog{CAS} -----|        |<----- VM:ThawThread{STORE} ----|         |
    +------+                            +--------+                                +---------+

The syntax for each transition operation is:

::

      thread ':' code '{' update-instruction '}'

The state pertains to the mutator thread and is recorded in the thread
local variable of the mutator thread.
Each transition describes which thread makes the transition (``M`` is the
mutator thread, and ``VM`` is the VM operation thread), the VM code
implementing the transition ``JNI-Prolog``, ``JNI-Epilog``,
``WaitUntilFrozen`` and ``ThawThread`` and the instruction used to update
the state variable (``CAS`` is atomic compare-and-swap and ``STORE`` is
normal memory store)

FENCE
~~~~~

Memory fences are used to implement Dekkers algorithm to ensure that a
thread is never mutating during a GC.
This mechanism uses both the ``MUTATOR_STATE`` and ``FROZEN`` thread local
variables of the mutator thread.
The operations that access these variables are in
``Snippets.nativeCallPrologue()``, ``Snippets.nativeCallEpilogue()``,
``WaitUntilFrozen`` and ``ThawThread``.

The choice of which synchronization mechanism to use is specified by the
``UseCASBasedThreadFreezing`` variable.

Freezing a thread requires making it enter native code.
For threads already in native code, this is trivial, i.e., there's
nothing to do except to transition them to the frozen state.
For threads executing in Java code, :doc:`safepoints <./Threads>`
are employed.
Safepoints are small polling code sequences injected by the compiler at
prudently chosen execution points.
The effect of executing a triggered safepoint is for the thread to
trap.
The trap handler will then call a specified ``AtSafepoint``
procedure.
This procedure synchronizes on the global GC and thread lock.
Since the VM operation thread holds this lock, a trapped thread will
eventually enter native code to block on the native monitor associated
with the lock.

This mechanism is similar to but not exactly the same as the ``@code VM_Operation`` facility in HotSpot except that Maxine ``VmOperations`` can
freeze a partial set of the running threads as Maxine implements
per-thread safepoints (HotSpot doesn't).

Implementation note
~~~~~~~~~~~~~~~~~~~

It is simplest for a mutator thread to be blocked this way.
Only under this condition can the GC find every reference on a slave
thread's stack.
If the mutator thread blocked in a spin loop instead, finding the
references in the frame of the spinning method is hard (what refmap
would be used?).
Even if the VM operation is not a GC, it may want to walk the stack of
the mutator thread.
Doing so requires the VM operation thread to be able to find the
starting point for the stack walk and this can only reliably be done
(through use of the Java frame anchors) when the mutator thread is
blocked in native code.

Suspend and Resume Thread Operations
------------------------------------

The ability to suspend and resume threads, which is required by the
JVMTI interface, is implemented using ``VmOperation``, and nested classes
``SuspendThreadSet`` and ``ResumeThreadSet`` are provided in
``VmOperation``.
These operations are also used by the (deprecated) methods
``Thread.suspend`` and ``Thread.resume``.

A normal VM operation suspends (freezes in ``VMOperation`` terminology)
the thread set temporarily, runs the operation, and then resumes the
thread set.
All the machinery to safepoint a running Java thread or handle a thread
in native code is appropriate for the suspend operation, but the thread
must stay suspended after the operation completes until the resume
operation is invoked.
Ordinarily a thread is frozen either by blocking on the ``THREAD_LOCK``
monitor held by the ``VmOperationThread`` (thread in Java) or spinning in
the return sequence from native code (thread in native).
Evidently the monitor must be released to exit the ``VmOperation`` so an
additional mechanism is necessary to actually suspend (as opposed to
freeze) the thread. Consider the two cases:

#. Thread in Java: The thread is blocked on the ``THREAD_LOCK`` monitor,
   called from the trap handler that handled the safepoint. Note that
   because it is blocked on the monitor, it is also actually in native
   code.
   The entire monitor acquisition process, which in Maxine currently
   can comprise several stack frames, must be unwound in order to
   release the ``THREAD_LOCK`` monitor. In fact we unwind all the way
   back to the trap handler.
#. Thread in Native: There are actually two cases here.
   Either the thread is truly blocked in native code, for example, on
   some other monitor or performing I/O, or it is caught in the native
   code return sequence and is spinning waiting to be unfrozen.
   In either case, when the thread actually returns it must then
   suspend (unless a resume occurs before the thread actually
   returns).

A thread is marked for suspend by setting bit zero in the ``SUSPEND``
field of the ``VmThreadLocal`` area.
This value is only ever written while the thread is frozen in the body
of the ``VmOperation.SuspendThreadSet`` or ``VmOperation.ResumeThreadSet``
operation.
When a thread is unfrozen it will promptly check the ``SUSPEND`` bit and
if it is set, will actually suspend on a native OS monitor (suspend
monitor) that is pre-allocated to every thread.
For a thread in native this check happens as the final act of the native
return epilogue.
To handle the special case of a thread that was safepointed and is
executing that sequence to release the ``THREAD_LOCK`` monitor, bit 1 of
the ``SUSPEND`` field is also set for safepointed threads, and the native
epilogue checks that bit and does not suspend.

The Resume operation clears the ``SUSPEND`` field in the ``VmOperation``
body and notifies the suspend monitor, which will cause any thread that
actually suspended to become runnable again.
Note that a resumed thread must recheck the ``SUSPEND`` field since it is
possible that another suspend operation occurred before the thread
actually got on CPU.

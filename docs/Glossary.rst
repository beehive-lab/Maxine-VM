Glossary of Maxine terminology and concepts
===========================================

Welcome to the Maxine Glossary, a very informal and evolving set of
brief notes to help orient newcomers to some of the terminology and
concepts we use when talking about the Maxine VM.
Please feel free to
write `to us <https://groups.google.com/forum/#!forum/maxinevm>`__ with
comments and suggestions.
It is definitely a work in progress.

You might also want to browse the `Maxine FAQ <./FAQ>`__.

We thank our collaborators who have been contributing documentation as
well; we link to it from this page and others whenever possible.

Actor
-----

An actor is an object that represents a Java language entity (e.g. a
Class, Method, or Field) in the VM and implements the entity's runtime
behavior.
All Maxine actors are instances of classes that extend abstract class
``com.sun.max.vm.actor.Actor``.

Maxine actors can be viewed as enhanced reflection classes (i.e. classes
such as ``java.lang.reflect.Method`` and ``java.lang.Class``).
Java reflection classes by design hide implementation details specific
to any VM (including in most cases information about the underlying
class file).
Maxine actors, on the other hand, exist precisely to implement those
internal details specifically for the Maxine VM.

Actors and their JDK counterparts
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The implementation of Maxine actors and their JDK ``java.lang.reflect``
counterparts are typically intertwined.
Since the Maxine VM is designed to operate with a standard, unmodified
JDK, modifications must be made dynamically to some JDK classes at VM
startup so that the two can be coordinated.

Three techniques make this possible:

#. **Aliases:** direct, non-reflective access to JDK fields and
   methods, even when prohibited by standard Java access rules;
#. **Field injection:** adding a field to a JDK class, for example a
   pointer from an instance of ``java.lang.reflect.Class`` to its
   corresponding ``ClassActor``; and
#. **Method substitution:** replacement of a JDK method.

See `JDK interoperation <./JDK-Interoperation>`__ for details and
examples.

Flags
~~~~~

The abstract class ``Actor`` contains exactly one field, a word used as a
bit field, along with a number of accessor methods for those values.
These provide efficient and flexible access to properties of interest
for all actors.

Many of the flags correspond to properties defined by the Java language,
for example the presence of keywords such as public, private, and
final.
These are documented at the head of the the file and are
cross-referenced to the *Java Language Specification*.
Other flags are used strictly for internal implementation.

The Actor types
~~~~~~~~~~~~~~~

This section mentions a few members of the ``Actor`` type hierarchy; the
actual type hierarchy is a bit more complex.

ClassActor
^^^^^^^^^^

A ``ClassActor`` represents many of the implementation details for a Java
class.
For example, it includes a reference to the corresponding instance of
``java.lang.Class``, which in turn contains an injected field reference
that points back at the ``ClassActor``.

A ``ClassActor`` also holds references to the class's methods (instances
of ``MethodActor``), fields(instances of ``FieldActor``), its superclass,
its static and dynamic hubs, and more.

``ClassActor`` is abstract, with three subclasses:

1, ``InterfaceActor`` represents Java interfaces.

#. ``PrimitiveClassActor`` represents primitive Java types, as described
   by ``KindEnum`` (corresponding to the primitive Java types plus some
   created only for VM internal use).
#. ``ReferenceClassActor`` represents non-primitive Java types using
   three concrete subclasses:

   #. ``ArrayClassActor<Value_Type>`` represents Java arrays;
   #. ``TupleClassActor`` represents ordinary Java objects;
   #. ``HybridClassActor`` represents a kind of object that cannot be
      expressed in Java: a combination of array plus fields that is
      used internally to represents Maxine hubs.

An ordinary object instance in the VM's heap contains a header that,
among other things, identifies the object's type.
This field points not at the ``ClassActor`` for the object's type, but
rather at the dynamic hub for the class.
In the case of the exceptional object that holds the static fields of a
class (the static tuple), the header points to the static hub for the
class.

FieldActor
^^^^^^^^^^

A ``FieldActor`` contains the implementation details for a field in a Java
class.
Such details include a reference to the representation of the field's
type and to its holder: the instance of ``ClassActor`` representing the
implementation of the class to which the field belongs.

A subclass of ``FieldActor``, ``InjectedReferenceFieldActor``, represents a
synthesized field that has been added dynamically to a JDK class.

See `field injection <./JDK-Interoperation#field-injection>`__ for
details and examples.

MethodActor
^^^^^^^^^^^

A ``MethodActor`` contains the implementation details for a method in a
Java class.
Such details include a reference to the representation of the method's
signature, to it's holder (the instance of ``ClassActor`` representing the
implementation of the class to which the method belongs), and to zero or
more possible compilations of the method.

The ``MethodActor`` class is itself abstract, with concrete subclasses
defined to implement various flavors of implementation: static methods,
virtual methods, interface methods, and so-called *miranda methods*.

Alias
-----

A specially marked field or method in a VM class that refers to a field
or method in another class that would otherwise be inaccessible due to
Java language access control rules.
Used mainly for VM access to private members of JDK classes.

`Read more <./JDK-Interoperation#aliases>`__.

Annotations
-----------

The Maxine VM code makes heavy use of Java Annotations as a form of
language extension.
These extensions, which are recognized and treated specially by the
Maxine compilers, permit the kind of low-level, unsafe programming that
is otherwise not possible with Java.
By using the Java annotation mechanism, which is a first class part of
the language, the Maxine sources are completely compatible with Java
IDEs.
See package ``com.sun.max.annotate``.

Here are a few of the important Maxine annotations:

-  ``@ALIAS``: denotes a field or method as an alias, which can be used
   to access a field or method in another class that would otherwise be
   inaccessible due to Java language access control rules.
-  ``@BUILTIN``: denotes a method whose calls are translated directly by
   the compiler into machine code.
-  ``@C_FUNCTION``: denotes a native function for which a lightweight
   JNI
   stub should be generated.
-  ``@CONSTANT_WHEN_NOT_ZERO``: denotes a field whose value is final
   once
   it is non-zero.
-  ``@CONSTANT``: denotes a field whose value is final before it's first
   read (i.e. a stationary field).
-  ``@FOLD``: calls to these methods are evaluated (as opposed to
   translated) at compile time.
-  ``@INLINE``: forced inlining.
-  ``@INSPECTED``: used by an offline tool to generate field and method
   accessors for the Maxine Inspector.
-  ``@METHOD_SUBSTITUTIONS``: denotes a class containing.
   ``MethodSubstitutions``
-  ``@NEVER_INLINE``: denotes a method that this compiler must never
   inline.
-  ``@SUBSTITUTE``: denotes a ``MethodSubstitution``.
-  ``@UNSAFE``: marks a method that requires special compilation; some
   other annotations imply ``@UNSAFE``.

Boot heap
---------

An object heap embedded in the VM boot image.
It is a normal heap, with the exception that objects in it never move
(although they may become permanent garbage).
As the name implies, the objects in this heap are those allocated during
boot image generation.
The boot image is really just this heap plus a little meta-data in
front.

`Read more <./Boot-Image#boot-image-contents>`__.

Boot Image
----------

See `Boot Image <./Boot-Image>`__.

Bootstrap
---------

The process of loading and executing a `boot image <#boot-image>`__ of
Maxine, up to the point where the VM is ready, either to execute a
specified application class or other action specified by the run scheme.

Currently a boot image of Maxine is not a native executable but just a
binary blob containing machine code and data for a dedicated target
platform.
Thus a boot image is not executable by itself.
To start it a very small native C application is required.

See `Boot Image <./Boot-Image>`__.

Bytecode breakpoint
-------------------

See `breakpoints in Inspector <./Inspector#breakpoints>`__.

ClassActor
----------

See `ClassActor <#classactor>`__.

Code eviction
-------------

See `Code Eviction <./Code-Eviction>`__.

CompilationBroker
-----------------

See `Schemes <./Schemes#compiler-strategy-(compilationbroker)>`__.

Dynamic hub
-----------

See `Objects <./Objects#dynamic-hubs>`__.

FieldActor
----------

See `FieldActor <#fieldactor>`__.

Graal Compiler
--------------

See `Graal <https://github.com/graalvm/graal-core>`__.

HOM layout
----------

See `Objects <./Objects#hom-layout>`__.

HeapScheme
----------

See
`Schemes <./Schemes#heap-allocation-and-garbage-collection-(heapscheme)>`__.

Hub
---

See `Objects <./Objects#hubs>`__.

Hybrid object representation
----------------------------

See `Objects <./Objects#hybrid-representation>`__.

Immortal memory
---------------

See the ``ImmortalHeap`` class as well as the various ``ImmortalHeap_*``
classes that test this functionality.

See class ``com.sun.max.vm.heap.ImmortalHeap``

Injected fields
---------------

During startup the VM synthesizes and injects additional fields into
core JDK classes.
Injected fields typically link instances of JDK objects to their
internal VM representation.
`Read more <./JDK-Interoperation#field-injection>`__.

Inspector
---------

See `Inspector <./Inspector>`__

Interpreter
-----------

The VM does not have an interpreter; it only runs compiled code.
See `Bootstrap <#bootstrap>`__.

LayoutScheme
------------

See `Schemes <./Schemes#object-layout-(layoutscheme)>`__.

.. _logging-tracing-label:

Logging and Tracing
-------------------

Maxine provides two related mechanisms for logging and/or tracing the
behavior of the VM, manual string-based logging using the
``com.sun.max.vm.Log`` class, or more automated, type-based logging, that
is integrated with the `Inspector <./Inspector>`__, using
``com.sun.max.vm.log.VMLogger``.
These are related in that ``VMLogger`` includes string based logging as an
option and so can replace the use of ``Log``.
Currently the VM uses a mixture of these two mechanisms, with conversion
being done opportunistically.
For simplicity, we will use the term tracing to describe string-based
logging in the following.
If you are adding logging to a VM component you are strongly encouraged
to use the ``VMLogger`` approach.

Manual Tracing
~~~~~~~~~~~~~~

Use the class ``com.sun.max.vm.Log`` to do manual tracing.
The class includes a variety of methods for printing objects of various
types.
By default the output goes to the standard output but can be re-directed
to a file by setting the environment variable ``MAXINE_LOG_FILE`` before
running the VM.
To selectively enable specific tracing in the VM, define a
``com.sun.max.vm.VMOption`` with the name ``-XX:+TraceXXX``, where ``XXX``
identifies the tracing.

You should avoid string concatenation (or any other code involving
allocation) in tracing code, especially inside a ``VmOperation``.
While this should not break the VM (allocation will fail fast with an
error message if a VM operation does not allow it), allocation can add
noise to your logs.
Lastly, if the logging sequence involves more than one logging
statement, you should bound the sequence with this pattern:

.. code:: java

    boolean lockDisabledSafepoints = Log.lock();
    // multiple calls to Log.print...() methods
    Log.unlock(lockDisabledSafepoints);

This will serialize logging performed by multiple threads.
Of course, it will also serialize the execution of the VM and may well
make the race you are trying to debug disappear!

Native Code Tracing
~~~~~~~~~~~~~~~~~~~

Maxine provides some tracing of the small amount of native code that
supports the VM.
By default this is conditionally compiled out of the VM image but can be
selectively enabled by editing ``com.oracle.max.vm.native/share/log.h``
and rebuilding with ``mx build`` and rebuilding the VM image.
This is particularly useful if the the VM crashes during startup.
For example to enable all tracing set ``log-ALL`` to 1.

Type-based Logging
~~~~~~~~~~~~~~~~~~

In type-based logging, the actual values that you want to log are passed
to an instance of the ``com.sun.max.vm.log.VMLogger`` class using methods
defined in the class.
Evidently, at the ``VMLogger`` level, type-based logging is something of a
misnomer, as it cannot know the types of the actual values.
In practice the values are logged as untyped ``Word`` values, but
extensive automated support is provided to handle the conversion to/from
``Word`` types.
The optional tracing support is driven from the values in the log.
For more details see `Type-based Logging <./Type‐based-Loging>`__.

Maxine packages
---------------

A mechanism for treating groups of classes in Java package as a de facto
"module" for purposes of system configuration and evolution.
This requires implementing more functionality than is provided by the
Java language via ``java.lang.Package``.

This main application of this mechanism is to define the classes to be
including during Maxine
`boot image generation <./Boot-Image#boot-image-generation>`__, and in
particular to specify which implementations to bind to VM schemes.

Strictly speaking, a Maxine package is a collection of classes in a Java
package that includes a class named ``Package``.
The class Package must extend class
``com.sun.max.config.BootImagePackage`` in order to be considered for
inclusion in the VM.
The ``Package`` class, other than acting as a marker, may contain
additional specifications directed at the Maxine package system.
In many cases, however, trivial ``Package`` class can be synthesized
dynamically and need not be explicitly defined.

Metacircular VM
---------------

In a conventional VM implementation (left in the figure below) there is
a language barrier between the language being implemented (Java in the
figure) and the implementation language (C++).
No such barrier exists in Maxine, where the VM is itself implemented in
the language being implemented.

.. image:: images/ConventionalMetacircular.jpg

See also: Ungar, D., Spitz, A., and Ausch, A. 2005. Constructing a
metacircular Virtual machine in an exploratory programming
environment. In *Companion To the 20th Annual ACM SIGPLAN Conference on Object-Oriented Programming, Systems, Languages, and Applications* (San
Diego, CA, USA, October 16 - 20, 2005). OOPSLA '05. ACM, New York, NY,
11-20. `DOI <http://doi.acm.org/10.1145/1094855.1094865>`__

MethodActor
-----------

The VM's runtime representation of a Java method.
`Read more <#methodactor>`__.

Method substitution
-------------------

Guided by Annotations, the Maxine VM substitutes certain JDK methods
with alternative implementations and compiles those in their stead. It
does not matter whether the original methods are native.
`Read more <./JDK-Interoperation#method-substitution>`__.

MonitorScheme
-------------

See `Schemes <./Schemes#thread-synchronization-(monitorscheme)>`__.

OHM layout
----------

See `Objects <./Objects#ohm-layout>`__.

package-info.java
-----------------

A documentation class, following Javadoc convention, for the classes and
interfaces in a Java package; this is especially encouraged for packages
that constitute Maxine package and serve as modules for VM
configuration.

Package.java
------------

A class used for configuration purposes by the Maxine Package mechanism.

ReferenceMap
------------

See `Threads <./Threads#stack-reference-map>`__.

ReferenceMapInterpreter
-----------------------

The ReferenceMapInterpreter performs an iterative data flow analysis via
abstract interpretation.
The following option maybe useful to watch it in action:

::

    -XX:TraceRefMapInterpretationOf=<value>

The help message for this option is: "Trace ref map interpretation of
methods whose name or declaring class contains ."

A short summary of its operation follows, contributed by Arian Treffer.

-  To collect GC roots, the GC needs to know which variable and stack
   slots in a stack frame contain references.
-  For the beginning of each code block, a bitmap (called "frame") that
   indicates used reference slots is cached.
-  A block is a sequence of byte codes in a method that can be executed
   without jumping (out or into).
   A block either ends with a (implicit) fall through, a jump, or a
   return.
-  To create frames, the blocks are pseudo interpreted: their pop and
   push behavior is simulated.
   The slot configuration at the end of a block is the frame for all
   blocks that can be reached from here (2 in case of a conditional
   jump, 0 in case of a return, otherwise 1).
-  When a block can be reached from multiple other blocks, its frame is
   the intersection of the final slot configuration of its
   predecessors.
   If one predecessor stored a reference in a slot, and another did
   not, the current block may not read this slot, for it doesn't know
   its contents.
-  The stack size at the beginning of a block is always the same.
   There is no Java code that first pushes N items (i.e. in a loop),
   and later pops them, even though this could be expressed with byte
   codes.
-  To get the slot configuration at the current execution point, the
   current block is interpreted again up until the current byte code,
   where the slot configuration is converted into a bitmap that
   indicates references on the current stack frame.

ReferenceScheme
---------------

See `Schemes <./Schemes#object-references-(referencescheme)>`__.

RunScheme
---------

See `Schemes <./Schemes#vm-startup-sequence-(runscheme)>`__.

Safepoint
---------

See `Threads <./Threads#safepoints>`__.

Scheme
------

A Java interface that specifies a configurable subsystem of the VM.
A complete VM configuration includes bindings to a specific
implementation of each scheme.
`Read more <./Schemes>`__.

Snippet
-------

See `Snippets <./Snippets>`__.

Static hub
----------

See `Objects <./Objects#hubs>`__.

Static tuple
------------

See `Objects <./Objects#static-tuple>`__.

Stop positions
--------------

A list of call and `Safepoint <#safepoint>`__ instructions within a target
method.
These locations correspond to all possible addresses the instruction
pointer of a frame may have when its thread is stopped at a
safepoint.
The location of all references on the stack are precisely known when at
a stop position.
See `Threads <./Threads>`__.

.. _t1x-compiler-label:

T1X compiler
------------

T1X is a template-based baseline compiler and is Maxine's first line of
execution (Maxine has no interpreter).
As such, it's primary goal is to produce code as fast as possible. Code
quality is of secondary concern.
It also closely matches the JVM specification's execution models.
That is, the JVM operand stack and local variable variable array is
modeled directly.
This makes it suitable for implementing bytecode level debugging as well
being the execution mode the de-optimization process uses as its end
target.

The templates for each bytecode instruction are written in Java (see
``T1XTemplateSource``) and compiled to machine code by C1X (which is to be
replaced by Graal).
These machine code snippets are stored in a table and used to translate
bytecodes at T1X compile time.
The translation is done in a single pass (see ``T1XCompilation``) and GC
maps are lazily generated via an abstract interpreter at GC time.
The latter strategy pays off as a GC map is only generated for a T1X
compiled method if it is currently active during GC root
scanning.
Another strategy to improve compile time is to minimize allocation
during compilation.
This is achieved by (re)using thread local data structures for each
compilation.

Having the templates written in Java makes modifying or extending the
compiler fairly easy.
More importantly, it also means the compiler is very portable and it
mostly relies on the optimizing compiler.
It performs very little direct machine code generation.

The source code for T1X is entirely contained in the top level T1X
directory of the Maxine source code base.

Target method
-------------

A target method in the Maxine VM is the entity that contains some
machine code produced by one of the compilers in Maxine.
It also contains all the other data required by the VM for some machine
code.
In particular, target methods (implemented by heap objects in the class
hierarchy rooted at ``TargetMethod.java``) encapsulate the following
information, including some that resides not in the heap but in the
region of code cache memory allocated for the compilation.

-  Machine code, represented as a reference to a ``byte[]`` that is
   stored in the method's code cache allocation.
-  Reference literals (optional, but common): represented as a
   reference to an ``Object[]`` that is stored in the method's code
   cache
   allocation.
-  Scalar literals (optional, much less common): represented as a
   reference to a ``byte[]`` that is stored in the method's code cache
   allocation.
-  Exception handler information. This is a data structure that can be
   used to answer the question "for an exception of type t thrown at
   position n in the target method, what is the position, if any, of an
   exception handler in the target method that will handle the thrown
   exception?".
-  The stop positions.
   A stop is a machine code position for which extra information is
   known about the execution state at that position.
   There types of stop positions in Maxine and the information recorded
   for them are shown below:

   -  **Call**.
      This is the position of a call (direct or indirect)
      instruction. For a call, the following is recorded:

      -  Frame reference map.
         This is a bit map with one bit per slot in the frame of the
         method.
         A set bit in this bit map indicates that the corresponding
         frame slot holds an object reference at the call.
      -  Java frame descriptor.
         This is a map from locations in the bytecode-level frame
         state to locations in the machine state.
         The bytecode level frame state is composed of the local
         variables and operand stack slots addressed by the JVM
         bytecodes from which the machine code was produced.
         The machine state is composed of frame slots, registers and
         immediate instruction operands.
         The mapping enables the JVM state to be completely
         reconstructed at the stop position.
         This is useful for implementing source level debugging and
         deoptimization.

   -  **Safepoint**.
      This is the position of a safepoint instruction.
      For a safepoint, all the information described for a call is
      recorded as well as:

      -  Register reference map.
         This is a bit map with one bit per register that can be used
         to store an object reference.
         This includes the complete set of general purpose registers
         for the platform but exclude all the floating point and
         state registers.
         Like a frame reference map, a set bit in the register
         reference map indicates that the corresponding register is
         holding an object reference at the safepoint.

[STRIKEOUT:Currently register reference maps are not recorded for calls
as all
registers are caller saved by the compilers in Maxine.
This will mostly likely change in the near future as C1X will implement
callee-save registers when compiling certain methods.] (Out of date?)

See abstract ``com.sun.max.vm.compiler.target.TargetMethod``

Threads
-------

See `Threads <./Threads>`__.

Thread local
------------

See `Threads <./Threads#thread-local-variables>`__.

Thread local variable
---------------------

See `Threads <./Threads#thread-local-variables>`__.

Thread locals area (TLA)
------------------------

See `Threads <./Threads#thread-locals-area>`__.

Thread local allocation buffer (TLAB)
-------------------------------------

See `Threads <./Threads#thread-local-allocation-buffer>`__.

Thread locals block (TLB)
-------------------------

See `Threads <./Threads#thread-locals-block>`__.

Trampoline
----------

The mechanism used to defer binding a call site to a target method.
When compiling a call, an address is needed for the machine level call
instruction.
One option is to eagerly resolve the callee during compilation of the
call but this will end up compiling the world!
Instead, a piece of code is called that knows how to find and compile
(if necessary) the intended target method and redirect the call
there.
For static calls, the call site itself is patched so that subsequent
calls go straight to the resolved method.
For virtual calls, the trampoline patches the entry in the relevant
dispatch table.

Tuple
-----

See `Objects <./Objects#tuple>`__.

VM Operation
------------

See `VM Operations <./VM-Operations>`__.

Type-based Logging
==================

Type-based logging is the preferred way to log, and optionally trace,
values of interest to aid in the development and debugging of
Maxine.
The term type-based indicates that the logging methods use statically
typed values, rather than doing up-front conversion to String values as
in string-based logging.

The standard method for debugging Maxine is interactively with the
Maxine Inspector.
However, there are times when pure interactive debugging is inadequate,
for example, in complex multi-threaded situations.
To address this Maxine has, historically, used tracing calls, embedded
in the VM source code and using the Log class, that output specific data
to either the standard output or a file.
This approach has some drawbacks:

-  Although largely string based, the tracing calls must follow strict
   rules regarding GC interaction and multi-threading.
-  The source code can become obfuscated by the tracing code.
-  There format of the tracing output is fixed by the tracing calls.
-  There is no connection to the Maxine Inspector.

The framework provided by ``VMLogger`` attempts to address these drawbacks
in the following ways:

-  Replace the tracing generation calls with more abstract calls to
   methods in ``VMLogger``.
-  Handle multi-threading, GC issues automatically.
-  Integrate the VMLogger data with the Maxine Inspector.
-  Provide optional custom tracing in the style of Log but driven from
   the log.

Note, the name log is overloaded.
The existing ``Log`` class is not a log in the sense defined by
``VMLogger``, rather it is a mechanism for printing strings, scalars and
some object types, e.g., threads, to an output stream.
In other words it is message oriented, similar to the platform logging
framework.
``VMLogger`` is more "type" oriented and is targeted towards in-memory log
storage, with log inspection handled by
the :doc:`Maxine Inspector <./Inspector>`.
By storing object values directly in the log, rather than a string
encoding, the Inspector mechanisms for drilling down into the fields of
an object can be exploited.
In the following, we refer to string-based logging as tracing.

The expectation is that each component, or module, of the VM has one or
more associated loggers.
Loggers are identified by a short name and a longer description.
A given logger is disabled by default but can be enabled with a command
line option at VM startup.
**Note:** A Logger can be enabled in hosted mode if that is appropriate
for the VM component.
All logger state is reset when the target VM starts, so host settings do
not persist.

It is also expected is that most loggers will be implemented using the
automatic generation features of ``VMLoggerGenerator`` and not be
hand-written, except as regards custom tracing support.
See the section below entitled
`Automatic Generation`_.

``VMLogger`` does not define the implementation of the log storage.
This is handled by ``VMLog``, which is an abstract class that is capable
of several implementations, with various tradeoffs regarding space
requirements and performance.

VMLogger
--------

A ``VMLogger`` defines a set of operations, cardinality ``N`` each
identified by an ``int`` code in the range ``[0 .. N-1]``.
A series of log methods are provided, that take the operation code and a
varying number of ``Word`` arguments (up to ``VMLog.Record.MAX_ARGS``).
Each log operation creates a log record that is stored in a circular
buffer, the size of which is determined when the VM image is built.
The thread (id) generating the log record is automatically recorded.

In order to connect the operation code with a ``String`` value that can be
used to identify the operation, e.g. for tracing, VM startup options,
etc., a logger should provide an overriding implementation of
``VMLogger.operationName(int)`` that returns a descriptive name for the
operation.

Enabling Logging
~~~~~~~~~~~~~~~~

Logging is enabled on a per logger basis through the use of a standard
``-XX:+LogMMM`` option derived from the logger name, in this case
``MMM``.
Tracing to the Log stream is also available through ``-XX:+TraceMMM``.
A default tracing implementation is provided, although this can be
overridden by a given logger.
Enabling tracing also enables logging, as the trace is driven from the
log.
**Note:** It is not possible to check the options until the VM startup
has reached a certain point.
In order not to lose logging in the early phases, logging, but not
tracing, is always enabled on VM startup.

Fine control over which operations are logged (and therefore traced) is
provided by the ``-XX:LogMMMInclude=pattern`` and
``-XX:LogMMMExclude=pattern`` options.
The pattern is a regular expression in the syntax expected by ``Pattern``
and refers to the operation names returned by
``VMLogger.operationName``.
By default all operations are logged.
However, if the include option is set, only those operations that match
the pattern are logged.
In either case, if the exclude option is provided, the set is reduced by
those operations that match the exclude pattern.

The management of log records is handled in a separate class; a subclass
of ``VMLog``.
A instance requests a record that can store a given number of arguments
from the singleton ``VMLog.vmLog`` instance and then records the
values.
The format of the log record is opaque to allow a variety of
implementations.

Performance Concerns
~~~~~~~~~~~~~~~~~~~~

In simple use logging affects performance even when disabled because the
disabled check happens inside the ``VMLogger`` log methods, so the cost of
the argument evaluation and method call is always paid when used in the
straightforward manner, e.g.: ``logger.log(op, arg1, arg2)``;

It is recommended that all log calls be guarded as follows:

.. code:: java

    if (logger.enabled()) {
        logger.log(op, arg1, arg2);
    }

The enabled method is always inlined.

**Note:** The guard can be a more complex condition.
However, it is important not to use disjunctive conditions that could
result in a value of true for the guard when ``logger.enabled()`` would
return false, E.g.,

.. code:: java

    if {a || b} {
        logger.log(op, arg1, arg2);
    }

Conjunctive conditions can be useful.
For example, say we wanted to suppress logging until a counter reaches a
certain value:

.. code:: java

    if (logger.enabled() && count >= value) {
        logger.log(op, arg1, arg2);
    }

Dependent Loggers
~~~~~~~~~~~~~~~~~

It is possible to have one logger override the default settings for
other loggers.
E.g., say we have loggers ``A`` and ``B``, but we want a way to turn both
loggers on with a single overriding option.
The way to do this is to create a logger, say ``ALL``, typically with no
operations, that forces ``A`` and ``B`` into the enabled state if, and only
if, it is itself enabled.
This can be achieved by overriding ``VMLogger.checkOptions()`` for the
``ALL`` logger, and calling the ``VMLogger.forceDependentLoggerState``
method.
See ``Heap.gcAllLogger`` for an example of this.

It is also possible for a logger, say ``C``, to inherit the settings of
another logger, say ``ALL``, again by forcing ``ALL`` to check its options
from within ``C``'s checkOptions and then use ``ALL``'s values to set ``C``'s
settings.
This is appropriate when ``ALL`` cannot know about ``C`` for abstraction
reasons.
See ``VMLogger.checkDominantLoggerOptions``.

**Note:** The order in which loggers have their options checked by the
normal VM startup is unspecified.
Hence, a logger must always force the checking of a dependent logger's
options before accessing its state.

Logging (for all loggers) may be enabled/disabled for a given thread,
which can be useful to avoid unwanted recursion in low-level code, see
``VMLog.setThreadState``.

Automatic Generation
~~~~~~~~~~~~~~~~~~~~

The standard type-safe way to log a collection of heteregenously typed
values would be to first define a class containing fields that
correspond to the values, then acquire an instance of such a class,
store the values in the fields and then save the instance in the
log.
Note that this generally involves allocation; at best it involves
acquiring a pre-allocated instance in some way. It also necessarily
involves a level of indirection in the log buffer itself, as the buffer
is constrained to be a container of reference values.
Since VM logging is a low level mechanism that must function in parts of
the VM where allocation is impossible, for example, during garbage
collection, the standard approach is not appropriate.
It is also important to minimize the storage overhead for log records
and the performance overhead of logging the data.
Therefore, a less type safe approach is adopted, that is partly
mitigated by automatic generation of logger code at VM image build time.

The automatic generation, see ``VMLoggerGenerator``, is driven from an
interface defining the logger operations that is tagged with the
``VMLoggerInterface`` annotation.
Since this is only used during image generation the interface should
also be tagged with ``HOSTED_ONLY``.
The logging operations are defined as methods in the interface.
In order to preserve the parameter names in the generated code, each
parameter should also be annotated with ``VMLogParam``, e.g.:

.. code:: java

    @HOSTED_ONLY
    @VMLoggerInterface
    private interface ExampleLoggerInterface {
      void foo(
          @VMLogParam(name = "classActor") ClassActor classActor,
          @VMLogParam(name = "base") Pointer base);

      void bar(
          @VMLogParam(name = "count") SomeClass someClass, int count);
    }

The logger class should contain the comment pair:

::

    // START GENERATED CODE
    // END GENERATED CODE

somewhere in the source, typically at the end of the class.
When ``VMLoggerGenerator`` is executed it scans all VM classes for
interfaces annotated with ``VMLoggerInterface`` and then generates an
abstract class containing the log methods, abstract method definitions
for the associated trace methods, and an implementation of the
``VMLogger.trace`` method that decodes the operation and invokes the
appropriate trace method.

The developer then defines the concrete implementation class that
inherits from the automatically generated class and, if required
implements the trace methods, e.g, from the ``ExampleLoggerOwner`` class:

.. code:: java

    public static final class ExampleLogger extends ExampleLoggerAuto {
         ExampleLogger() {
            super("Example", "an example logger.");
         }

        @Override
        protected void traceFoo(ClassActor classActor, Pointer base) {
            Log.print("Class "); Log.print(classActor.name.string);
            Log.print(", base:"); Log.println(base);
        }

        @Override
        protected void traceBar(SomeClass someClass, int count) {
          // SomeClass specific tracing
        }
    }

Note that if an argument name is not identified with ``VMLogParam`` it
will be defined as ``argN``, where ``N`` is the argument index.

``VMLogger`` has built-in support for several standard reference types,
that have alternate representations as scalar values, such as
``ClassActor``.
As a general principle, reference types without an alternate, unique,
scalar representation should be avoided as log method
arguments.
However, this is sometimes difficult or inconvenient, so it is possible
to store references types.
These should be passed using ``VMLogger.objectArg`` and retrieved using
``VMLogger.toObject``.
This is automatically handled by the generator.
**Note:** Storing reference types in the log makes them reachable until
such time as they are overwritten.
It is assumed that ``Enum`` types are always stored using their ordinal
value.
The generator creates the appropriate conversions methods.
It assumes that the enum declares the following field:

.. code:: java

    public static final EnumType[] VALUES = values();

Tracing
~~~~~~~

When the tracing option for a logger is enabled, ``VMLogger.doTrace`` is
invoked immediately after the log record is created.
After checking that calls to the Log class are possible, ``Log.lock`` is
called, then ``VMLogger.trace`` is called, followed by ``Log.unlock``.

A default implementation of ``VMLogger`` is provided that calls methods in
the ``Log`` class to print the logger name, thread name and
arguments.
There are two ways to customize the output.
The first is to override the ``VMLogger.logArg(int, Word)`` method to
customize the output of a particular argument - the default action is to
print the value as a hex number.
The second is to override ``VMLogger.trace`` and do full
customization.
**Note:** Although the log is locked automatically and safepoints are
disabled, custom tracing must still take care not to invoke object
allocation.
In particular, string concatenation and formatting should not be used.

Inspector Integration
---------------------

The :doc:`Inspector <./Inspector>` is generally able to display the log
arguments appropriately, by using reflection to discover the types of
the arguments.

Two additional mechanisms are available for Inspector customization.
The first is an override to generate a custom String representation of a
log argument:

.. code:: java

    @HOSTED_ONLY
    public String inspectedArgValue(int op, int argNum, Word argValue);

If this method is defined for a given logger then the Inspector will
call it for the given operation and argument and, if it returns a
non-null value, use the result.

The second is an override for a logger-defined argument value class:

.. code:: java

    @HOSTED_ONLY
    public static String inspectedValue(Word argValue);

If this method is defined for the class and no standard customization is
available, it will be called and, if the result is non-null it will be
used.

VMLog
-----

``VMLog`` maintains the global table of ``VMLogger`` instances, and provides
the log storage implementation and support for interacting with the
garbage collector.
The actual log storage implementation is specified by abstract methods
and a particular implementation is chosen at VM image build time.
The default implementation is ``VMLogNativeThreadVariable`` which stores
log records in a per-thread native buffer.
The other implementation that is provided with Maxine is
``VMLogArrayFixed``, which can be enabled by setting the ``max.vmlog.class``
system property to ``java.fix.VMLogArrayFixed``.
This is an all-Java implementation that uses a global buffer comprising
an array of fixed length ``VMLog.Record`` instances.
It should be used as a check if there is a suspicion that the default
implementation is manifesting a bug.

VMLog Flushing
--------------

By default, older log records are overwritten when the circular buffer
wraps around.
In normal use this is not a problem, as the Inspector maintains all the
log records in its own non-circular buffer.
However, in exceptional circumstances, for example when not running the
Inspector, it may be convenient to flush the log, say on a VM crash,
rather than tracing every log operation.
This can be enabled with the ``-XX:VMLogFlush=setting`` VM option.
The value of setting should be a comma separated string contains one of
the following:

-  ``crash``: flush the log on a VM crash
-  ``exit``: flush the log on normal VM exit
-  ``full``: flush the log whenever it becomes full (i.e., is about to
   overwrite old records)
-  ``raw``: output the log records as uninterpreted, raw, bits.
-  ``trace``: output the log records using the ``VMLogger.trace`` method

The default output mode is ``raw``, which is robust, but requires offline
interpretation.
``Trace`` mode may be unstable after a VM crash as it may provoke a
recursive crash.

Note that flushing the log when full, using ``trace`` mode output, is
essentially equivalent to setting the associated trace options, except
for that the data might be "stale" by delaying the interpretation until
the log is flushed.

The :doc:`Maxine Inspector <./Inspector>` can interpret a file of ``VMLog``
records using ``mx view -vmlog=file``.
The simplest way to create the file is to redirect the log output to a
file by setting export ``MAXINE_LOG_FILE=maxine.log`` before running the
VM, and then copying the file.
The last step is important because the Inspector will overwrite the log
file when it executes (meta-circularity!).

--------------

Automatically generated from com.sun.max.vm.log.package-info

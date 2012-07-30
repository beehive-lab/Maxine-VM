/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
/**
 * <h1>Type-based Logging</h1>
 * <p>
 * Type-based logging is the preferred way to log, and optionally trace, values of interest to
 * aid in the development and debugging of Maxine. The term <i>type-based</i> indicates
 * that the logging methods use statically typed values, rather than doing up-front conversion
 * to {@link java.lang.String} values as in <i>string-based</i> logging.
 * <p>
 * The standard method for debugging Maxine is interactively with the Maxine Inspector.
 * However, there are times when pure interactive debugging is inadequate, for example, in complex
 * multi-threaded situations. To address this Maxine has, historically, used tracing calls,
 * embedded in the VM source code and using the {@link com.sun.max.vm.Log} class, that output specific data
 * to either the standard output or a file. This approach has some drawbacks:
 * <p>
 * <ul>
 * <li>Although largely string based, the tracing calls must follow strict rules regarding
 * GC interaction and multi-threading.</li>
 * <li>The source code can become obfuscated by the tracing code.</li>
 * <li>There format of the tracing output is fixed by the tracing calls.</li>
 * <li>There is no connection to the Maxine Inspector.</li>
 * </ul>
 * <p>
 * The framework provided by {@link com.sun.max.vm.log.VMLogger} attempts to address these drawbacks in the following ways:
 * <ul>
 * <li>Replace the tracing generation calls with more abstract calls to methods in {@link com.sun.max.vm.log.VMLogger}.</li>
 * <li>Handle multi-threading, GC issues automatically.</li>
 * <li>Integrate the {@link com.sun.max.vm.log.VMLogger} data with the Maxine Inspector.</li>
 * <li>Provide optional custom tracing in the style of {@link com.sun.max.vm.Log} but driven from the log.</li>
 * </ul>
 * <p>
 * Note, the name {@code log} is overloaded. The existing {@link com.sun.max.vm.Log} class
 * is not a log in the sense defined by {@link com.sun.max.vm.log.VMLogger}, rather it is a mechanism for
 * printing strings, scalars and some object types, e.g., threads, to an output stream.
 * In other words it is message oriented, similar to the {@link java.util.logging.Logger platform logging} framework.
 * {@link com.sun.max.vm.log.VMLogger} is more "type" oriented and is targeted towards in-memory log storage,
 * with log inspection handled by the Maxine Inspector. By storing object values directly in the log,
 * rather than a string encoding, the Inspector mechanisms for drilling down into the fields of an
 * object can be exploited. In the following, we refer to string-based logging as <i>tracing</i>.
 * <p>
 * The expectation is that each component, or module, of the VM has one or more associated loggers. Loggers are
 * identified by a short name and a longer description. A given logger is disabled by default but
 * can be enabled with a command line option at VM startup. N.B. A Logger can be enabled in
 * {@link com.sun.max.vm.MaxineVM#isHosted() hosted mode} if that is appropriate for the VM component.
 * All logger state is reset when the target VM starts, so host settings do not persist.
 * <p>
 * It is also expected is that most loggers will be implemented using the automatic generation
 * features of {@link com.sun.max.vm.log.hosted.VMLoggerGenerator} and not be hand-written, except as
 * regards custom tracing support. See the section below entitled Automatic Generation.
 * <p>
 * {@link com.sun.max.vm.log.VMLogger} does not define the implementation of the log storage.
 * This is handled by {@link com.sun.max.vm.log.VMLog}, which is an abstract class that is capable
 * of several implementations, with various tradeoffs regarding space requirements and performance.
 * <h2>VMLogger</h2>
 * <p>
 * A {@link com.sun.max.vm.log.VMLogger} defines a set of operations, cardinality {@code N} each identified by an {@code int} code in the
 * range {@code [0 .. N-1]}. A series of {@code log} methods are provided, that take the operation code and a varying number
 * of {@link com.sun.max.unsafe.Word} arguments (up to {@link com.sun.max.vm.log.VMLog.Record#MAX_ARGS}). Each {@code log} operation creates a {@link com.sun.max.vm.log.VMLog.Record log record}
 * that is stored in a circular buffer, the size of which is determined when the VM image is built.
 * The thread (id) generating the log record is automatically recorded.
 * <p>
 * In order to connect the operation code with a {@link java.lang.String} value that can be used to identify the operation,
 * e.g. for tracing, VM startup options, etc., a logger should provide an overriding implementation of {@link com.sun.max.vm.log.VMLogger#operationName(int)}
 * that returns a descriptive name for the operation.
 * <h3>Enabling Logging</h3>
 * <p>
 * Logging is enabled on a per logger basis through the use of a standard {@code -XX:+LogMMM} option derived from the
 * logger name, in this case {@code MMM}. Tracing to the {@link com.sun.max.vm.Log} stream is also available through {@code -XX:+TraceMMM}.
 * A default tracing implementation is provided, although this can be overridden by a given logger. Enabling tracing also enables logging, as the trace is
 * driven from the log. <b>N.B.</b> It is not possible to check the options until the VM startup has reached a certain
 * point. In order not to lose logging in the early phases, logging, but not tracing, is always enabled on VM startup.
 * <p>
 * Fine control over which operations are logged (and therefore traced) is provided by the
 * {@code -XX:LogMMMInclude=pattern} and {@code -XX:LogMMMExclude=pattern} options. The {@code pattern} is a regular
 * expression in the syntax expected by {@link java.util.regex.Pattern} and refers to the operation names returned by
 * {@link com.sun.max.vm.log.VMLogger#operationName}. By default all operations are logged. However, if the include option is set,
 * only those operations that match the pattern are logged. In either case, if the exclude option is provided, the set
 * is reduced by those operations that match the exclude pattern.
 * <p>
 * The management of log records is handled in a separate class; a subclass of {@link com.sun.max.vm.log.VMLog}. A {@link com.sun.max.vm.log.VMLogger instance}
 * requests a {@link com.sun.max.vm.log.VMLog.Record record} that can store a given number of arguments from the singleton {@link com.sun.max.vm.log.VMLog#vmLog}
 * instance and then records the values. The format of the log record is opaque to allow a variety of implementations.
 *
 * <h3>Performance Concerns</h3>
 * <p>
 * In simple use logging affects performance even when disabled because the disabled check happens inside the
 * {@link com.sun.max.vm.log.VMLogger} log methods, so the cost of the argument evaluation and method call is always paid when used in the
 * straightforward manner, e.g.:
 *
 * {@code logger.log(op, arg1, arg2);}
 *
 * <p>It is recommended that all log calls be guarded as follows:</p>
 *
 * <pre>
 * if (logger.enabled()) {
 *     logger.log(op, arg1, arg2);
 * }
 * </pre>
 * <p>
 * The {@code enabled} method is always {@linkplain com.sun.max.annotate.INLINE inlined}.
 * <p>
 * N.B. The guard can be a more complex condition. However, it is important not to use disjunctive conditions that could
 * result in a value of {@code true} for the guard when {@code logger.enabled()} would return false, E.g.,
 *
 * <pre>
 * if {a || b} {
 *     logger.log(op, arg1, arg2);
 * }
 * </pre>
 * <p>
 * Conjunctive conditions can be useful. For example, say we wanted to suppress logging until a counter reaches a
 * certain value:
 *
 * <pre>
 * if (logger.enabled() &amp;&amp; count &gt;= value) {
 *     logger.log(op, arg1, arg2);
 * }
 * </pre>
 * <p>
 * <h3>Dependent Loggers</h3>
 * <p>
 * It is possible to have one logger override the default settings for other loggers. E.g., say we have loggers {@code A} and {@code B},
 * but we want a way to turn both loggers on with a single overriding option. The way to do this is to create a logger,
 * say {@code ALL}, typically with no operations, that forces {@code A} and {@code B} into the enabled state if, and only if, it is itself
 * enabled. This can be achieved by overriding {@link com.sun.max.vm.log.VMLogger#checkOptions()} for the {@code ALL} logger, and calling the
 * {@link com.sun.max.vm.log.VMLogger#forceDependentLoggerState} method. See {@link com.sun.max.vm.heap.Heap#gcAllLogger} for an example of this.
 * <p>
 * It is also possible for a logger, say {@code C}, to inherit the settings of another logger, say {@code ALL}, again by forcing {@code ALL} to
 * check its options from within {@code C}'s {@code checkOptions} and then use {@code ALL}'s values to set {@code C}'s settings. This is
 * appropriate when {@code ALL} cannot know about {@code C} for abstraction reasons. See {@link com.sun.max.vm.log.VMLogger#checkDominantLoggerOptions}.
 * <p>
 * N.B. The order in which loggers have their options checked by the normal VM startup is unspecified. Hence, a logger
 * must always force the checking of a dependent logger's options before accessing its state.
 * <p>
 * Logging (for all loggers) may be enabled/disabled for a given thread, which can be useful to avoid unwanted recursion
 * in low-level code, see {@link com.sun.max.vm.log.VMLog#setThreadState}.
 *
 * <h3>Automatic Generation</h3>
 * <p>
 * The standard type-safe way to log a collection of heterogeneously typed values would be to first define a class
 * containing fields that correspond to the values, then acquire an instance of such a class, store the values in the
 * fields and then save the instance in the log. Note that this generally involves allocation; at best it involves
 * acquiring a pre-allocated instance in some way. It also necessarily involves a level of indirection in the log buffer
 * itself, as the buffer is constrained to be a container of reference values.
 *
 * Since VM logging is a low level mechanism that must function in parts of the VM where allocation is impossible, for
 * example, during garbage collection, the standard approach is not appropriate. It is also important to minimize the
 * storage overhead for log records and the performance overhead of logging the data. Therefore, a less type safe
 * approach is adopted, that is partly mitigated by automatic generation of logger code at VM image build time.
 * <p>
 * The automatic generation, see {@link com.sun.max.vm.log.hosted.VMLoggerGenerator}, is driven from an interface defining the logger operations
 * that is tagged with the {@link com.sun.max.vm.log.hosted.VMLoggerInterface} annotation. Since this is only used during image generation the
 * interface should also be tagged with {@link com.sun.max.annotate.HOSTED_ONLY}. The logging operations are defined as methods
 * in the interface. In order to preserve the parameter names in the generated code, each parameter should also be annotated
 * with {@link com.sun.max.vm.log.hosted.VMLogParam}, e.g.:
 *
 * <pre>
 * &#64HOSTED_ONLY
 * &#64VMLoggerInterface
 * private interface ExampleLoggerInterface {
 *   void foo(
 *       &#64VMLogParam(name = "classActor") ClassActor classActor,
 *       &#64VMLogParam(name = "base") Pointer base);
 *
 *   void bar(
 *       &#64VMLogParam(name = "count") SomeClass someClass, int count);
 * }
 * </pre>
 * The logger class should contain the comment pair:
 * <pre>
 * // START GENERATED CODE
 * // END GENERATED CODE
 * </pre>
 * <p>
 * somewhere in the source, typically at the end of the class.
 * When {@link com.sun.max.vm.log.hosted.VMLoggerGenerator} is executed it scans all VM classes for interfaces annotated with {@link com.sun.max.vm.log.hosted.VMLoggerInterface}
 * and then generates an abstract class containing the log methods, abstract method definitions for the associated {@code trace}
 * methods, and an implementation of the {@link com.sun.max.vm.log.VMLogger#trace} method that decodes the operation and invokes the
 * appropriate {@code trace} method.
 * <p>
 * The developer then defines the concrete implementation class that inherits from the automatically generated class
 * and, if required implements the trace methods, e.g, from the {@link demo.ExampleLoggerOwner} class:
 * <pre>
 * public static final class ExampleLogger extends ExampleLoggerAuto {
 *      ExampleLogger() {
 *         super("Example", "an example logger.");
 *      }
 *
 *     &#64Override
 *     protected void traceFoo(ClassActor classActor, Pointer base) {
 *         Log.print("Class "); Log.print(classActor.name.string);
 *         Log.print(", base:"); Log.println(base);
 *     }
 *
 *     &#64Override
 *     protected void traceBar(SomeClass someClass, int count) {
 *       // SomeClass specific tracing
 *     }
 * }
 * </pre>
 * <p>
 * Note that if an argument name is not identified with {@link com.sun.max.vm.log.hosted.VMLogParam} it will be defined as {@code argN}, where
 * {@code N} is the argument index.
 * <p>
 * {@link com.sun.max.vm.log.VMLogger} has built-in support for several standard reference types, that have alternate representations
 * as scalar values, such as {@link com.sun.max.vm.actor.holder.ClassActor}. As a general principle, reference types without an alternate, unique, scalar
 * representation should be avoided as log method arguments. However, this is sometimes difficult or inconvenient, so
 * it is possible to store references types. These should be passed using {@link com.sun.max.vm.log.VMLogger#objectArg}
 * and retrieved using {@link com.sun.max.vm.log.VMLogger#toObject}. This is automatically handled by the generator.
 * <b>N.B.</b> Storing reference types in the log makes them reachable until such time as they are overwritten.
 * It is assumed that {@code Enum} types are always stored using their ordinal value. The generator creates the
 * appropriate conversions methods. It assumes that the {@code enum} declares the following field:
 * <pre>
 * public static final EnumType[] VALUES = values();
 * </pre>
 *
 * <h4>Tracing</h4>
 * <p>
 * When the tracing option for a logger is enabled, {@link com.sun.max.vm.log.VMLogger#doTrace} is invoked immediately after the log record is created.
 * After checking that calls to the {@link com.sun.max.vm.Log} class are possible, {@link com.sun.max.vm.Log#lock} is called, then
 * {@link com.sun.max.vm.log.VMLogger#trace} is called, followed by {@link com.sun.max.vm.Log#unlock}.
 * <p>
 * A default implementation of {@link com.sun.max.vm.log.VMLogger} is provided that calls methods in the {@link com.sun.max.vm.Log} class to print the
 * logger name, thread name and arguments. There are two ways to customize the output. The first is to override the
 * {@link com.sun.max.vm.log.VMLogger#logArg(int, com.sun.max.unsafe.Word)} method to customize the output of a particular argument - the default action is to print
 * the value as a hex number. The second is to override {@link com.sun.max.vm.log.VMLogger#trace} and do full customization.
 * <b>N.B.</b> Although the log is locked automatically and safepoints are disabled, custom tracing must still
 * take care not to invoke object allocation. In particular, string concatenation and formatting should not be used.
 * <h2>Inspector Integration</h2>
 * <p>
 * The Inspector is generally able to display the log arguments appropriately, by using reflection to discover the types of the
 * arguments.
 * <p>
 * Two additional mechanisms are available for Inspector customization. The first is an override to generate a custom
 * {@link java.lang.String} representation of a log argument:
 *
 * <pre>
 * &#64HOSTED_ONLY
 * public String inspectedArgValue(int op, int argNum, Word argValue);
 * </pre>
 *
 * If this method is defined for a given logger then the Inspector will call it for the given operation and argument
 * and, if it returns a non-null value, use the result.
 * <p>
 * The second is an override for a logger-defined argument value class:
 *
 * <pre>
 * &#64HOSTED_ONLY
 * public static String inspectedValue(Word argValue);
 * </pre>
 *
 * If this method is defined for the class and no standard customization is available, it will be called and, if the
 * result is non-null it will be used.
 *
 *<h2>VMLog</h2>
 * {@link com.sun.max.vm.log.VMLog} maintains the global table of {@link com.sun.max.vm.log.VMLogger} instances, and
 * provides the log storage implementation and support for interacting with the garbage collector.
 * The actual log storage implementation is specified by {@code abstract} methods and a particular implementation
 * is chosen at VM image build time. The default implementation is {@link com.sun.max.vm.log.nat.thread.var.VMLogNativeThreadVariableUnbound}
 * which stores log records in a per-thread native buffer. The other implementation that is provided with Maxine is
 * {@link com.sun.max.vm.log.java.fix.VMLogArrayFixed}, which can be enabled by setting the
 * {@code max.vmlog.class} system property to {@code java.fix.VMLogArrayFixed}. This is an all-Java implementation
 * that uses a global buffer comprising an array of fixed length {@link com.sun.max.vm.log.VMLog.Record} instances.
 * It should be used as a check if there is a suspicion that the default implementation is manifesting a bug.
 *
 * <h3>VMLog Flushing</h3>
 * By default, older log records are overwritten when the circular buffer wraps around. In normal use this is not a problem,
 * as the Inspector maintains all the log records in its own non-circular buffer. However, in exceptional circumstances,
 * for example when not running the Inspector, it may be convenient to flush the log, say on a VM crash, rather than tracing
 * every log operation. This can be enabled with the {@code -XX:VMLogFlush=setting} VM option.
 * The value of {@code setting} should be a comma separated string contains one of the following:
 * <ul>
 * <li>crash: flush the log on a VM crash</li>
 * <li>exit: flush the log on normal VM exit</li>
 * <li>full: flush the log whenever it becomes full (i.e., is about to overwrite old records)</li>
 * <li>raw: output the log records as uninterpreted, raw, bits.</li>
 * <li>trace: output the log records using the {@link com.oracle.mac.vm.log.VMLogger#trace} method</li>
 * </ul>
 * The default output mode is raw, which is robust, but requires offline interpretation. Trace mode
 * may be unstable after a VM crash as it may provoke a recursive crash.
 * <p>
 * Note that flushing the log when full, using trace mode output, is essentially equivalent to setting
 * the associated trace options, <i>except</i> that the data might be "stale" by delaying the
 * interpretation until the log is flushed.
 * <p>
 * The Maxine Inspector can interpret a file of {@link VMLog} records using {@code mx view -vmlog=file}.
 * The simplest way to create the file is to redirect the log output to a file by setting
 * {@code export MAXINE_LOG_FILE=maxine.log} before running the VM, and then copying the file.
 * The last step is important because the Inspector will overwrite the log file when it executes (meta-circularity!).
 */
package com.sun.max.vm.log;


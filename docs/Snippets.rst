Snippets in the Maxine VM
=========================

Snippets are designed to provide a clean separation between the runtime
system and compiler in a JVM.
A clean separation allows either component to be modified or extended
independently in the pursuit of features or optimization.
Furthermore, snippets are designed to isolate the implementation of
runtime features from each other.
For example, the implementation of virtual dispatch need not be aware of
the object model or memory management implementation.
The rest of this page provides the implementation details of snippets in
the Maxine VM and how they measure up against these design goals.
This is done by focusing on how the ``invokevirtual`` bytecode is
implemented via snippets.
This is a non-trivial bytecode whose implementation depends on numerous
runtime features.

When implementing any given JVM bytecode instruction in a compiler, it
is useful to think of the implementation in two parts: the JVM
specification semantics and the VM runtime details.
The compiler has little or no flexibility with respect to JVM
specification semantics; developers write code with these semantics in
mind and expect the JVM to be compliant.
The runtime details however are completely at the discretion of the VM
implementer.
Should vtables be used? If so, how are they laid out? How can runtime
information inform which optimizations (such as inlining) should be
performed or even reversed (de-optimized)?

With this context in mind, one description of snippets is that they are
simply pieces of Java source code that express the (partial) semantics
and implementation of a bytecode instruction.
There are typically a number of snippets which, together, comprise the
implementation of a single bytecode instruction.
Continuing with the ``invokevirtual`` example, the following summarizes
its specification:

#. Resolve the method denoted by a symbolic reference in a constant
   pool.
#. Select the target method to be invoked based on the resolved method
   and the type of the receiver (i.e. this at the call site).
#. Call the selected target method.

The first two steps are expressed by the following snippets:

.. code:: java

    @SNIPPET
    @INLINE(afterSnippetsAreCompiled = true)
    public static VirtualMethodActor resolveVirtualMethod(ResolutionGuard guard) {
        if (guard.value == null) {
            resolve(guard);
        }
        return UnsafeCast.asVirtualMethodActor(guard.value);
    }

.. code:: java

    @INLINE(afterSnippetsAreCompiled = true)
    public static Word selectNonPrivateVirtualMethod(Object receiver, VirtualMethodActor declaredMethod) {
        final Hub hub = ObjectAccess.readHub(receiver);
        return hub.getWord(declaredMethod.vTableIndex());
    }

    @SNIPPET
    @INLINE(afterSnippetsAreCompiled = true)
    public static Word selectVirtualMethod(Object receiver, VirtualMethodActor declaredMethod) {
        if (declaredMethod.isPrivate()) {
            // private methods do not have a vtable index, so directly compile and link the receiver.
            return CompilationScheme.Static.compile(declaredMethod, CallEntryPoint.VTABLE_ENTRY_POINT);
        }
        return selectNonPrivateVirtualMethod(receiver, declaredMethod);
    }

The second step (i.e. ``selectVirtualMethod``) yields the machine code
address (of the target method) to be called.
The compiler is expected to know how to emit a call (given a target
address and signature), and so a snippet is not used for this step.

The last point exemplifies an aspect of a compiler built around
snippets.
The compiler is expected to be able to translate certain operations
intrinsically.
This includes all primitive operations such integer, float and long
arithmetic, local control flow constructs as well as the aforementioned
capability for emitting a call given a target address and signature.
In Maxine, these operations are called compiler ***builtins***.
Like snippets, they are also expressed as Java methods. For example,
here is the builtin for integer addition:

.. code:: java

    public static class IntPlus extends JavaBuiltin {
        @BUILTIN(builtinClass = IntPlus.class)
        public static int intPlus(int a, int b) {
            return a + b;
        }
    }

The body of the ``intPlus`` method above exists solely for the purpose of
folding (i.e. compile-time evaluation) as well as IR interpretation.
The compiler (backend) knows how to emit machine code whenever it comes
across a call to a builtin (i.e. any method with the ``@BUILTIN``
annotation).

VM feature isolation
--------------------

Before drilling down to the details of how snippets are built and
consumed by the compiler, it's worth using the invokevirtual example to
demonstrate how snippets isolate the implementation details of VM
features from one other.
Consider the following line in the ``ResolveVirtualMethod`` snippet:

.. code:: java

    if (guard.value == null) {

Depending on the VM configuration, a number of VM features (detailed
below) are exercised by the read-access of the ``value`` field from the
``guard`` object.
While reading these, keep in mind that not one of them is explicitly
present in the snippet source code.

-  **Object model:** An object model specifies how fields, array
   elements and object metadata are layed out in the memory allocated
   for an object.
   The object model in the Maxine VM is a configurable component
   represented by the ``LayoutScheme`` interface.
   There are currently two different object model implementations in
   Maxine.
   With respect to snippets, the point to note is that when switching
   between object models, there is no need to modify the code of the
   ``ResolveVirtualMethod`` and ``SelectVirtualMethod`` snippets.
-  **Garbage collector barriers:** If the VM is configured with a
   garbage collector that uses read-barriers, then using a barrier (if
   necessary) for the read of the value value is solely the
   responsibility of the snippet implementing reading of reference
   fields.
-  **Garbage collector handles:** The compiler tracks the types of Java
   variables and generates the appropriate reference maps such that a
   GC can find all the object references in method activation.
-  **Object references:** Maxine includes two (related) abstractions
   for specifying how object references are implemented.
   The first, represented by the ``ReferenceScheme`` interface,
   encapsulates the operations that can be applied to an object
   reference (a value of type ``java.lang.Object``) such as reading or
   writing a char from a reference at a given offset.
   This abstraction has support for read or write barriers and so is
   used when compiling mutator (i.e. non-GC) code.
   The second abstraction, represented by the GripScheme interface, has
   the same operations as the first except that it omits any notion of
   barriers.
   A ``GripScheme`` deals with values of type ``Grip`` and is used when
   implementing a garbage collector.
   Typically, an implementation of a ``ReferenceScheme`` is bound to an
   implementation of a ``GripScheme``.
   The default implementation of ``GripScheme`` is ``DirectGripScheme``
   which treats object references as direct memory pointers.
   However, alternative ``GripScheme`` implementations could be used to
   implement:

   -  compressed oops
   -  indirect object references via a handle table
   -  object references on a system that has hardware support for
      objects

IR Notation
-----------

The following sections include compiler IR examples.
To aid comprehension of these examples, the IR notation is informally
described here.

The IR is composed of values, operations and procedure/function
calls.
Calls are composed of a target followed by a set of (comma separated)
arguments enclosed by '(' and ')'. A target enclosed by '<' and '>' is a
builtin.

Values are named variables (e.g. method), constant objects prefixed with
'@' (e.g. @GUARD\_FOR\_NAME) or primitive constants (e.g. 32).

All values and targets are typed.
The type is indicated by a '#' suffix followed by one of the type
characters in this table:

+-------------+-----------------------+-------------------------+
| Character   | Description           | Bit width               |
+=============+=======================+=========================+
| R           | an object reference   | width of machine word   |
+-------------+-----------------------+-------------------------+
| W           | an unsigned word      | width of machine word   |
+-------------+-----------------------+-------------------------+
| I           | int                   | 32                      |
+-------------+-----------------------+-------------------------+
| J           | long                  | 64                      |
+-------------+-----------------------+-------------------------+
| F           | float                 | 32                      |
+-------------+-----------------------+-------------------------+
| D           | double                | 64                      |
+-------------+-----------------------+-------------------------+

The IR also has expressions, assignments, control flow and return
constructs that should be self explanatory to anyone familiar with Java.

Using snippets
--------------

So how does the compiler actually use snippets when translating
bytecode?
The basic idea is that the compiler translates each snippet into an IR
(intermediate representation) graph which is stored in a
compiler-internal data structure.
The issue of how the compiler initializes the collection of IR snippets
is described in the next section.

Here is an example of the IR that may be produced for the
``ResolveVirtualMethod`` and ``SelectVirtualMethod`` snippets:

.. code:: java

    resolveVirtualMethod(guard#R)#R {
        value#R := <readReferenceAtIntOffset>#R(guard#R, 24#I);
        if (value#R == null#R) {
            resolve#V(guard#R);
        }
        result#R := <readReferenceAtIntOffset>#R(guard#R, 24#I);
        return#R result#R;
    }

    selectVirtualMethod(rcvr#R, method#W)#R {
        flags#I := <readIntAtIntOffset>#R(method#R, 32#I);
        tmp#I := <intAnd>#I(flags#I, 2#I);
        if (tmp#I == 0) {
            result#W := vtableDispatch#W(rcvr#R, method#R);
        } else {
            result#W := compile#W(method#R, @VTABLE_ENTRY_POINT#R);
        }
        return#W result#W;
    }

When compiling other (non-snippet) methods, the front-end of the
compiler responsible for parsing bytecodes produces IR by weaving
hand-crafted IR with the relevant snippet IR.
For example, consider the following Java source code method:

.. code:: java

    public String toString() {
        return name();
    }

The bytecode produced by javac for this method is:

::

    aload_0
    invokevirtual "name()"
    areturn

When compiling this method, the compiler will weave in the pre-built IR
for the ``ResolveVirtualMethod`` and ``SelectVirtualMethod`` snippets to
produce the following:

.. code:: java

    asString(this#R)#R {
        value#R := <readReferenceAtIntOffset>#R(@GUARD_FOR_NAME#R, 24#I);
        if (value#R == null#R) {
            resolve(guard#R);
        }
        method#R := <readReferenceAtIntOffset>#R(@GUARD_FOR_NAME#R, 24#I);
        flags#I := <readIntAtIntOffset>#I(method#R, 32#I);
        tmp#I := <intAnd>#I(flags#I, 2#I);
        if (tmp#I == 0) {
            address#W := vtableDispatch#W(this#R, method#R);
        } else {
            address#W := compile#W(method#R, VTABLE_ENTRY_POINT#R);
        }
        result#R := <call>#R(address#W, this#R);
        return#R result#R;
    }

Note that this is the code produced when the compiler has determined
that the name method has not yet been resolved. To determine that a
method has been resolved, a compiler based on snippets can rely upon
folding and inlining during compilation.
For this example, the guard object is a compile time constant wrapping a
resolved symbolic reference to method.
Constant propagation combined with inlining and folding will therefore
reduce the above IR to a vtable dispatch:

.. code:: java

    asString(this#R)#R {
        hub#R := <readReferenceAtIntOffset>#R(this#R, 0#I);
        address#W := <builtinGetWord>#W(hub#R, 24#I, 64#I);
        result#R := <call>#R(address#W, this#R);
        return#R result#R;
    }

Here is the source for ``vtableDispatch``:

.. code:: java

    @INLINE
    public static Word vtableDispatch(Object receiver, VirtualMethodActor declaredMethod) {
        Hub hub = ObjectAccess.readHub(receiver);
        return hub.getWord(declaredMethod.vTableIndex());
    }

Note that the vtable dispatch logic also benefits from the VM feature
isolation offered by snippets.
That is, it does not explicitly mention how to read a hub from an
object - it just calls a method that does it (which in turn is inlined).

While this compilation strategy produces optimal and correct code, its
performance can suffer if the pursuit of non-redundancy is
uncompromising.
The mechanism by which folding is performed in the CPS compiler is to
call Java methods via reflection.
The overhead of reflection is significant:

-  the IR values must be unboxed from their IR boxing types and then
   re-boxed to their Java boxing types
-  the reverse unboxing and reboxing is required for the return value
-  a new array of arguments is constructed by stripping the
   continuation arguments from the CPS call IR construct
-  there is no chance for the compiler to inline the call and elide the
   boxing as these reflective calls are made from general purpose
   folding logic
-  reflection mandates type checking for all arguments, something that
   is redundant with the type checking performed by the compiler itself

To address these performance concerns, the compiler can intrinsify some
of the logic expressed in the snippets.
For example, it can do the resolution check itself.
It can also determine if a resolved method is private in which case it
would simply prefer to use the ``vtableDispatch`` method as a snippet.
In general, there's a need to revisit how the logic is split between the
compiler and snippets so that compiler performance is maximized while
benefits of snippets are not lost.

Bootstrapping snippets
----------------------

As seen in the previous section, the compiler uses pre-built IR snippets
when compiling Java bytecode methods.
We've also shown how snippets are expressed as Java bytecode methods
(derived from Java source code).
These two facts combined represent a cyclic dependency between the
compiler and the pre-built snippets.
Snippets may also have cyclic dependencies among themselves.
For example, the Java source code for the ``ResolveVirtualMethod`` and
``SelectVirtualMethod`` snippets use virtual method invocation themselves.
In fact, almost all snippets depend on virtual method invocation.
These cyclic dependencies pose a bootstrap problem to the compiler
implementer.

The general strategy to resolve all of these circular dependencies is to
prepare the snippets using two passes over all snippets:

#. The first pass (***snippet creation***) translates each snippet to
   IR without engaging in any optimizations at all except mandatory
   inlining as directed by an ``@INLINE`` annotation.
   The snippets are carefully crafted in such a way that they can make
   use of each other on an inlining basis, practically using other
   snippets as macros.
#. The second pass (***snippet optimization***) optimizes the output
   of the first pass and stores the optimized IR in a table.

A predicate is maintained by the compiler indicating whether the second
phase has completed or not.
This information is used by the compiler to interpret the
``afterSnippetsAreCompiled`` flag of the ``@INLINE`` annotation.
When the annotation is present at a method declaration, then a call to
the method is inlined *iff* its compilation occurs after the second
pass.
This mechanism allows snippets to contain method calls so that
bootstrapping the snippets themselves bottoms out.
Nevertheless these calls can later be inlined after all snippets are
available, while compiling other code.
In other words, pre-built snippet IR may not be fully optimized, but
once woven into user code, they are subject to full optimization.

Annotations
-----------

The Java code for snippets relies on the following annotations, which
serve as pragmas for the compiler:

-  ``@SNIPPET``: Denotes the entry point for a snippet.
-  ``@FOLD``: The annotated method must must have no arguments (apart
   from the implicit this if it is a not ``static`` method).
   If the method is ``static``, it is evaluated unconditionally by the
   compiler.
   If the method is not ``static``, it will be evaluated by the compile
   whenever its receiver is a compile time constant.
-  ``@INLINE``: The annotated method is inlined by the compiler.
   If the ``afterSnippetsAreCompiled`` flag has the default value
   (i.e. ``false``), then the inlining is performed unconditionally.
   Otherwise, inlining is conditional upon the snippet bootstrapping
   phase as described above.
-  ``@NEVER_INLINE``: The annotated method is never inlined by the
   compiler.
   In the context of snippets, this is useful for denoting a slow path
   when generating code.
   That is, code the is rarely expected to be called and so should not
   be inlined in the method being compiled.

Evaluation
----------

#. **Performance**: To what extent do snippets affect the runtime
   and/or code quality of a compiler?

   -  [STRIKEOUT:Snippets in the Maxine VM are supported and used by the
      CPS
      compiler, the only compiler currently capable of bootstrapping
      Maxine.
      The CPS compiler was co-designed with the snippet mechanism.
      Unfortunately, the performance of this compiler is sub-optimal
      both in terms of compilation speed and quality of compiled
      code.
      Given the many factors affecting the quality of a compiler
      (choice of IR, register allocation algorithm employed,
      optimizations performed, memory usage during compilation, etc.),
      it is hard measure the impact of snippets on the compiler's
      performance.
      To perform a meaningful assessment of snippets, one ideally needs
      to start with a compiler that is not based on snippets and then
      modify it to use snippets.
      By doing so, once can measure the extent to which snippets
      improve/degrade the runtime and/or code quality produced by a
      compiler.
      In addition, this experiment will reveal the architectural impact
      of making a compiler snippet aware.
      That is, to what extent do snippets complicate (or simplify) a
      compiler's design.
      Once the C1X compiler is integrated into Maxine, it will form the
      basis for such an experiment and thus provide an answer to the
      performance question.] (outdated)

#. **Expressiveness**: How easy is it to express/comprehend the
   semantics of a bytecode instruction?

   -  Being written in Java source code, snippets can mostly be as
      easily written and comprehended as any other piece of Java
      code.
      The qualification is that one needs to be very aware of the
      potential for causing infinite recursion.
      For example, when implementing the athrow bytecode, it is
      important not to include any code that explicitly throws an
      exception.
      Fortunately, infinite recursion is usually fail-fast and so one
      knows fairly quickly that something is wrong.

#. **Re-use**: How easy is it to ensure that the semantics of a
   bytecode instruction are expressed in as few places as possible?

   -  Other parts of the VM can simply call snippet code as normal Java
      methods.

#. **Portability**: How much needs to be changed when porting the VM
   to a new platform?

   -  The snippets include no machine code or even any
      compiler-specific code.
      Any platform dependent code in a snippet is expressed as Java
      code that tests a compile-time constant platform configuration
      value.
      As long as the compiler implements the protocol required for
      bootstrapping the snippets, there should no need to modify any
      other parts of a replacement compiler.

#. **Syntactic correctness**: How easy it is to verify that snippets
   are syntactically correct?

   -  As easy as having the Java source code compiler successfully
      compile the snippet source code!

#. **Optimization potential**: How much do snippets enhance or inhibit
   optimization potential in a compiler?

   -  Snippet IR is designed to be woven into the IR of a method before
      optimization.
      This means all snippet IR is subject to complete optimization in
      the context of the method being compiled.
      So, in theory, a compiler based on snippets should allow maximum
      optimization of the code paths that implement the
      runtime/compiler interface.
      However, it also means that the quality of code generated for
      these code paths is at the mercy of the compiler.
      [STRIKEOUT:Due to the sub-optimal CPS compiler in Maxine, the code
      derived
      from snippets is far from optimal.]\ (outdated)

#. **Compiler design**: How much do snippets complicate or simplify a
   compiler's design?

   -  This point can only be accurately addressed in the same way
      proposed for the **Performance** question.
      Only then can one accurately comment on the architectural impact
      of making a compiler snippet aware.

#. **Locality**: How easy is it to find and navigate the code related
   to a single snippet?

   -  This is one of the weaker aspects of snippets as they are
      currently implemented in the Maxine VM.
      The source code for the snippets is distributed amongst many
      classes, one class per snippet.
      The properties of some snippets are encoded in the snippet class
      hierarchy.
      For example, all snippets whose optimized IR must not include any
      calls (except to builtins) must subclass the ``BuiltinsSnippet``
      class while those that cannot be folded must subclass the
      ``NonfoldableSnippet`` class.
      All such compilation-properties of snippets should really be
      associated with the snippet entry point, possibly as elements of
      the ``@SNIPPET`` annotation.
      In addition, the way in which snippets are discovered and
      registered with the compiler is more complicated than it should
      be, relying on class initialization.
      [STRIKEOUT:Most of these issues however, are simply code
      engineering issues
      that are relatively easy to remedy, especially if modeled after
      the way in which XIR snippets are organized.]\ (outdated)

#. **Code Layout**: What's the granularity of control over how the
   generated code is organized?

   -  The code path for snippets is either inlined or involves a
      runtime call.
      [STRIKEOUT:Like XIR, one would ideally like to be able to express
      fast
      inline path, out-of-line but in method path, global stub and
      runtime call paths.]\ (outdated)
      With some careful thought, modifying or augmenting the ``@INLINE``
      annotation may enable such code-layout to be expressed.

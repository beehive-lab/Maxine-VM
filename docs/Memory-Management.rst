Memory Management
=================

Automatic memory management has become an essential feature of modern
programming languages as it frees programmers from explicit memory
management, a time-consuming and error-prone activity.

Meta-circularity and memory management
--------------------------------------

As a :doc:`meta-circular virtual machine <./Glossary>`, Maxine
can benefit from automatic memory management as well, something that VMs
written in lower-level languages such as C or C++ cannot.

An initial design decision in Maxine was to manage (almost) all memory
used internally by the VM in the same way as memory used by
applications.
All internal data structures required for class loading, compilation,
verification, etc., are represented as normal heap-allocated Java
objects.
Although this decision has indeed simplified VM development, an
unfortunate consequence is that (internal) VM operations pollute the
application heap, with consequences for application performance.

A meta-circular VM can avoid perturbing the application heap and
optimize VM performance by exploiting knowledge of the allocation and
object lifetime profiles of its internal subsystems.
For example, intermediate objects allocated during compilation have a
limited lifetime.
Once a compilation has finished, only the objects representing the final
product remain alive.
It would be advantageous to segregate these objects from application
objects and to reclaim them with specialized mechanisms that are faster
than for general application objects.
Similar reasoning can be applied to objects allocated by other
sub-systems.

Maxine's current Generational GC
--------------------------------

Maxine has recently adopted a simple generational collector implemented
by the ``GenSSHeapScheme`` heap scheme.
Details on this new heap scheme and its performance with respect to the
original semi-space GC are presented
`here <https://web.archive.org/web/20150516045756/https://wikis.oracle.com/display/MaxineVM/Generational+Heap+Scheme>`__.

Maxine's semi-space GC
----------------------

Maxine still includes its original simple semi-space copying collector
implemented by ``SemiSpaceHeapScheme``.
This allows to fall back on a very simple and robust implementation both
for experimentation purposes and to diagnose problems.

Next generation GC in Maxine
----------------------------

To address the issues sketched above, we are engaged in the design and
implementation of a novel region-based garbage collection sub-system for
Maxine.
Our intentions are to make Maxine competitive with state of the art GC
work and to better address issues specific to meta-circular VMs.
In this design a heap may be composed of fixed-size, possibly
non-contiguous regions, in order to favor incremental collections and to
support multiple, independently collectible heaps.
The GC itself will follow an incremental hybrid mark-sweep approach with
policy-driven evacuation.

The long term goals of this effort are to:

-  support generational and incremental collection;
-  support multiple, independently collectible heaps, with dedicated
   heaps for VM activities such as compiling, verifying, class loading,
   etc.;
-  foster research on the use and implementation of region-based
   multiple-heap such as:

   -  user-level use of multiple isolated heaps to address pause time
      issues with large monolithic heaps;
   -  investigate GC-heaps that do not requires a contiguous virtual
      address space; and
   -  dynamically attachable object heaps, for example to enable
      constructs such as shared object memories and persistent
      pre-populated heaps.

The building blocks for this new GC framework are in place and have been
tested with the addition of a pure mark-sweep heap scheme.
The mark-sweep heap scheme allows testing of some of the base components
of the future region-based garbage collector (namely a tricolor tracing
algorithm).

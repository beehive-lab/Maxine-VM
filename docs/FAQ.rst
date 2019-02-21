Frequently Asked Questions
==========================

Here are some of the most frequently asked questions, along with answers
that have been updated as the project evolves.
You might also want to browse the Maxine ":doc:`Glossary <./Glossary>`".

Does Maxine support the GNU classpath libraries?
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

No.
Maxine is designed to run with openJDK.

How modular is the Maxine VM architecture?
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Maxine provides abstractions for coarse-grained configurability of many
VM subsystems, which we call schemes.
For example, the static and runtime compilers, garbage collector,
reference representation, object layout, monitor implementation, are all
configurable with schemes.

What kind of GC does the Maxine VM use?
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Currently Maxine employs a semi-space collector as the default.
As with many other parts of the Maxine architecture, garbage collection
is abstracted as a separate scheme with limited interactions with other
schemes.
We also aim to make the garbage collector scheme MMTK compatible.

How much optimization does the baseline compiler do?
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Essentially none.
See :ref:`t1x-compiler-label`.

Does the Maxine VM use Green Threads?
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

No; Maxine does not use green threads.
Maxine uses native threads and a state-of-the-art safepoint mechanism
for preemption.
See :doc:`Threads <./Threads>`.

Can I use my favorite debugger?
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

No, instead we use the The Maxine Inspector for debugging and inspecting
the VM while it is running.

What kind of development environment do I need to build and run the Maxine VM?
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Maxine can be built and run from the command line, no special
development environment is needed.

How does Maxine relate to the Project Maxwell Assembler System?
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The Maxine distribution provides an advanced variant of the Project
Maxwell Assembler System, now called the Maxine Assembler System.

Are there other attempts to bootstrap the Java programming language?
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Yes, some examples include Jikes RVM, Joeq, OVM, and Moxie.
The design of Maxine has benefited from these previous systems and
enjoys the advantage of the new source language features in Java 5.0,
particularly annotations and generics.

Can I suspend and resume VM execution?
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The simplest way to get application suspend/resume currently is to run
Guest VM (Maxine on Xen).
Suspend/resume is built into the hypervisor support and "just works".
The Guest VM is server-side only though, no GUI at this point.

Does the Maxine VM have an interpreter?
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Maxine doesn't do interpretation.
It instead uses a very fast baseline compiler.
See :ref:`t1x-compiler-label`.

Why am I getting an error message about "hosted" being missing when trying to build an image?
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

::

    Exception in thread "main" java.lang.UnsatisfiedLinkError: no hosted in java.library.path

The native library named "hosted" is used during the boot image
generation to get information on the host platform.
Very likely this has not been built for some reason.

First, be sure that you have a C development environment installed.
Also ensure that the CDT plugin is installed if you are using Eclipse.

Lastly, try rebuilding the native code:

::

    mx build -clean com.oracle.max.vm.native

Why was the Grip abstraction from the original VM design removed?
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Re: changeset 4423

The original rational for Grips was to provide an abstraction for object
references that does not involve write barriers.
Apart from that, it was pretty much an exact mirror of the
``ReferenceScheme``.
The thinking was that the GC should be written against grips as it does
not need to update write barriers.
However, it turns out that object reference fix up is done via Pointers
in the current GC implementations and I don't see why this won't/can't
be true for any other GC.
That is, we had a whole extra (and confusing) abstraction whose whole
reason for existence was not being used! Additionally, even if references were being fixed
up via ``Reference.setReference(...)`` and ``Reference.writeReference(...)``
instead of ``Pointer.setReference(...)`` and ``Pointer.writeReference(...)``
there is still no need for an extra abstraction.
It would be far simpler to annotate the method(s) doing the update with
an annotation (e.g. ``@NO_BARRIERS``) that would instruct the compiler not to insert write
barriers.

Of course, Maxine's abstractions should support more than just write
barriers for generational GCs.
Other interesting barriers include read barriers for concurrent GCs,
read & write barriers for all data types in an software transactional
memory implementation, etc.
I cannot say for certain that the support for these is sufficient right
now, but I'm confident they can be programmed without grips.

How does the Inspector process communicate with the inspected Maxine VM process?
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The VM is almost entirely passive with respect to the Inspector process.
There is no internal agent; the VM neither sends nor receives messages;
in fact the VM barely knows that it is being inspected.
Other than process controls (thread management, start, stop, set
breakpoints, etc.), the Inspector works mostly by reading from VM
memory.
However, VM code is arranged in some places to make inspection easier,
and there are a few critical places where the VM does respond to
information written into its memory by the Inspector.
See :doc:`Inspector-VM Interaction <./Inspector-VM-Interaction>`.

What happened to the "primordial thread"?
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Until February 2011 the original thread in a new Maxine VM process was
known as the primordial thread; its job was to execute the preliminary
steps needed to bootstrap the VM and then wait until the Java VM exited.
From February 2011 onward, the original process thread eventually
becomes the main thread, i.e.
the thread on which the Java main thread runs.
See :doc:`Threads <./Threads>`.

Welcome to the Maxine VM project
================================

A next generation, highly productive platform for virtual machine research.

Project Overview
----------------

In this era of modern, managed languages we demand ever more from our virtual machines: better performance, more scalability, and support for the latest new languages.
Research and experimentation is essential but challenging in the context of mature, complex, production VMs written in multiple languages.
The Maxine VM is a next generation platform that establishes a new standard of productivity in this area of research.
It is written entirely in Java, completely compatible with modern Java IDEs and the standard JDK, features a modular architecture that permits alternate implementations of subsystems such as GC and compilation to be plugged in, and is accompanied by a dedicated development tool (`the Maxine Inspector <./Inspector>`__) for debugging and visualizing nearly every aspect of the VM's runtime state.

As of the 2.0 release, September 2013, Maxine is no longer an active project at Oracle Labs.
As of the 2.1 release, April 2017 Maxine VM is actively maintained and developed at the University of Manchester.

We believe that Maxine represents the state of the art in a research VM, and actively encourage community involvement.
The Maxine sources, including VM, Inspector, and other supporting tools, are Open Source and are licensed under GPL version 2.0.

Citation
--------

For the original Maxine VM please cite:
`C. Wimmer et al, “Maxine: An approachable virtual machine for, and in, java”, In ACM TACO 2013. <http://dl.acm.org/citation.cfm?id=2400689&dl=ACM&coll=DL&CFID=748733895&CFTOKEN=73017278>`__

For Maxine VM >= v2.1 please cite:
`Christos Kotselidis, et al. Heterogeneous Managed Runtime Systems: A Computer Vision Case Study. In 13th ACM SIGPLAN/SIGOPS International Conference on Virtual Execution Environments (VEE), 2017. <http://dl.acm.org/citation.cfm?id=3050764>`__

Getting Started
---------------

-  `Download and build Maxine <./build>`__ from source on any of the supported `platforms <./build#platform>`__.
-  Read the technical report `"Maxine: An Approachable Virtual Machine For, and In, Java" <https://community.oracle.com/docs/DOC-917520>`__
-  Send any questions to `this mailing list <https://groups.google.com/forum/#!forum/maxinevm>`__ (maxinevm@googlegroups.com).
-  Read about the current `status of the VM <./Status>`__.
-  Learn more about `the Maxine Inspector <./Inspector>`__, the companion tool for visualizing internal state and debugging the VM: video introduction, video demos, and written documentation.
-  Learn more about `Virtual Machine Level Analysis <./Virtual-Machine-Level-Analysis>`__, an experimental extension for analysis the behavior of application (and eventually the VM).
-  View `publications and presentations <./Publications>`__ about Maxine.
-  Read the `Glossary <./Glossary>`__ and `FAQ <./FAQ>`__.
-  Contact us on the `the mailing list <https://groups.google.com/forum/#!forum/maxinevm>`__ (or `in private <mailto:christos.kotselidis@manchester.ac.uk>`__) and tell us about your work.

Roadmap
-------

-  Upgrade to latest Graal, Implement JVMCI
-  Run Truffle on top of Maxine VM/Graal
-  Port MMTk to Maxine VM
-  ARM AArch64 Port

Contributing to Maxine
----------------------

Maxine is an open-source project, and we invite researchers and developers to make contributions in the form of bug fixes, performance enhancements, features, and so forth.

To push your code upstream use pull requests on GitHub.
However, note that we are doing most development in a private git tree and we are working on a number of features which are not quite ready for public release.
Therefore, we would strongly encourage you to get in touch before starting to work on anything large, to avoid duplication of effort.
We can probably expedite our release of any work-in-progress (WIP) features you might be interested in, if you do that

Contributors should adopt the following process once they think their changes are ready to push:

#. Merge the latest changes from the Maxine master branch.
#. Test their code.
#. Make sure the code complies to the coding style of Maxine VM using ``mx checkstyle``.
#. Open a new pull request where they explain their contribution and how it works in case of complex implementations.

The changes will then be reviewed and tested.
In case there are no errors, the contribution will be included in the head repository.
In any other case the maintainer will be asked to resolve any issues and update his code.

Reporting Bugs
~~~~~~~~~~~~~~

If you think you have found a bug which is not in the list of Known issues, please open a new issue `here <https://github.com/beehive-lab/Maxine-VM/issues>`__, on GitHub.
However, note that we have limited time available to investigate and fix bugs which are not affecting the workloads we are using.
Therefore, if you can't pinpoint the cause of the bug yourself, we ask that you provide as many details on how to reproduce it, and preferably provide a statically linked executable which triggers it.

Who writes this wiki?
---------------------

-  Members of the core Maxine team at the Manchester University have write access.
-  External contributions are always welcome; please open an issue on GitHub with your contribution and a member of the Maxine team will review and incorporate it.
-  We encourage public comments on these pages, especially where a page should answer your question but doesn't.
   Please open an issue on GitHub or post on our user mailing list if this doesn't work for you.

Acknowledgements
----------------

This wiki is heavily based on the original wiki pages (by Oracle) that can be found `here <https://web.archive.org/web/20150516045940/https://wikis.oracle.com/display/MaxineVM/Home>`__ and `here <https://community.oracle.com/community/java/java_hotspot_virtual_machine/maxine-vm>`__.

Quick Link Summary
------------------

-  `Team <./People>`__, `Publications <./Publications>`__, `FAQ <./FAQ>`__
-  `Contributing <#contributing-to-maxine>`__
-  `Download, Build & Run <./build>`__
-  `Status <./Status>`__, `Bugs <https://github.com/beehive-lab/Maxine-VM/issues>`__,
   `Mailing List <https://groups.google.com/forum/#!forum/maxinevm>`__,
   `Testing <./build#testing-maxine>`__,
   `Profiling <./build#profiling>`__,
   `Virtual Machine Level Analysis <./Virtual-Machine-Level-Analysis>`__
-  IDEs: `Eclipse <./IDEs#eclipse>`__, `Netbeans <./IDEs#netbeans>`__,
   `IntelliJ <./IDEs#intellij>`__
-  Maxine Tools: `Inspector <./Inspector>`__,
   `mx script <./build#the-mx-script>`__
-  Tech Topics:
   `Glossary <./Glossary>`__,
   `Actors <./Glossary#Actors>`__,
   `Boot Image <./Boot-Image>`__,
   `Code Eviction <./Code-Eviction>`__,
   `Code Dependencies <./Code-Dependencies>`__,
   `CompilationBroker <./Schemes.md#compiler-strategy-(compilationbroker)>`__,
   `Inspector-VM Interaction <./Inspector‐VM-Interaction>`__,
   `JDK Interoperation <./JDK-Interoperation>`__,
   `Logging and Tracing <./Glossary#logging-and-tracing>`__,
   `Memory Management <./Memory-Management>`__,
   `Objects <./Objects>`__,
   `ReferenceMapInterpreter <./Glossary#referencemapinterpreter>`__,
   `Schemes <./Schemes>`__,
   `Snippets <./Snippets>`__,
   `Stack Walking <./Stack-Walking>`__,
   `T1X <./Glossary#t1x-compiler>`__,
   `Target Methods <./Glossary#target-method>`__,
   `Threads <./Threads>`__,
   `VM Tooling Interface <./VM-Tooling-Interface>`__,
   `VM Operation <./VM-Operations>`__

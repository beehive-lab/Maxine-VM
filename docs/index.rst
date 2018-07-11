Welcome to the Maxine VM project
================================

A next generation, highly productive platform for virtual machine research.

Project Overview
----------------

In this era of modern, managed languages we demand ever more from our virtual machines: better performance, more scalability, and support for the latest new languages.
Research and experimentation is essential but challenging in the context of mature, complex, production VMs written in multiple languages.
The Maxine VM is a next generation platform that establishes a new standard of productivity in this area of research.
It is written entirely in Java, completely compatible with modern Java IDEs and the standard JDK, features a modular architecture that permits alternate implementations of subsystems such as GC and compilation to be plugged in, and is accompanied by a dedicated development tool (:doc:`the Maxine Inspector <./Inspector>`) for debugging and visualizing nearly every aspect of the VM's runtime state.

As of the 2.0 release, September 2013, Maxine is no longer an active project at Oracle Labs.
As of the 2.1 release, April 2017, Maxine VM is actively maintained and developed at the University of Manchester.

We believe that Maxine represents the state of the art in a research VM, and actively encourage community involvement.
The Maxine sources, including VM, Inspector, and other supporting tools, are Open Source and are licensed under GPL version 2.0.
To obtain the code please visit `<https://github.com/beehive-lab>`_.

Citation
--------

For Maxine VM >= v2.1 please cite:
`Christos Kotselidis, et al. Heterogeneous Managed Runtime Systems: A Computer Vision Case Study. In 13th ACM SIGPLAN/SIGOPS International Conference on Virtual Execution Environments (VEE), 2017. <http://dl.acm.org/citation.cfm?id=3050764>`_

For the original Maxine VM please cite:
`C. Wimmer et al, “Maxine: An approachable virtual machine for, and in, java”, In ACM TACO 2013. <http://dl.acm.org/citation.cfm?id=2400689&dl=ACM&coll=DL&CFID=748733895&CFTOKEN=73017278>`_

Getting Started
---------------

-  :doc:`Download and build Maxine <./build>` from source on any of the supported :ref:`platform-label`.
-  Read the technical report `"Maxine: An Approachable Virtual Machine For, and In, Java" <https://community.oracle.com/docs/DOC-917520>`__
-  Send any questions to `this mailing list <https://groups.google.com/forum/#!forum/maxinevm>`__ (maxinevm@googlegroups.com).
-  Read about the current :doc:`status of the VM <./Status>`.
-  Learn more about :doc:`the Maxine Inspector <./Inspector>`, the companion tool for visualizing internal state and debugging the VM: video introduction, video demos, and written documentation.
-  Learn more about :doc:`Virtual Machine Level Analysis <./Virtual-Machine-Level-Analysis>`, an experimental extension for analysis the behavior of application (and eventually the VM).
-  View :doc:`publications and presentations <./Publications>` about Maxine.
-  Read the :doc:`Glossary <./Glossary>` and :doc:`FAQ <./FAQ>`.
-  Contact us on the `the mailing list <https://groups.google.com/forum/#!forum/maxinevm>`__ (or `in private <mailto:christos.kotselidis@manchester.ac.uk>`__) and tell us about your work.

Roadmap
-------

-  Implement JVMCI, Upgrade to latest Graal
-  Run Truffle on top of Maxine VM/Graal
-  Port MMTk to Maxine VM

.. _contributing-label:

Contributing to Maxine
----------------------

Maxine is an open-source project, and we invite researchers and developers to make contributions in the form of bug fixes, performance enhancements, features, documentation, and so forth.

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

.. _reporting-bugs-label:

Reporting Bugs
~~~~~~~~~~~~~~

If you think you have found a bug which is not in the list of Known issues, please open a new issue `on GitHub <https://github.com/beehive-lab/Maxine-VM/issues>`__.
However, note that we have limited time available to investigate and fix bugs which are not affecting the workloads we are using.
Therefore, if you can't pinpoint the cause of the bug yourself, we ask that you provide as many details on how to reproduce it, and preferably provide a statically linked executable which triggers it.

Acknowledgements
----------------

This documentation is heavily based on the original wiki pages (by Oracle) that can be found `here <https://web.archive.org/web/20150516045940/https://wikis.oracle.com/display/MaxineVM/Home>`__ and `here <https://community.oracle.com/community/java/java_hotspot_virtual_machine/maxine-vm>`__.

Table of Contents
=====================================

.. toctree::
   :maxdepth: 2

   Status
   build
   IDEs
   Debugging
   FAQ
   Glossary
   JDK-Interoperation
   Boot-Image
   Memory-Management
   Code-Dependencies
   Code-Eviction
   Objects
   Schemes
   Snippets
   Stack-Walking
   Threads
   Type-based-Logging
   Virtual-Machine-Level-Analysis
   VM-Operations
   VM-Tooling-Interface
   Inspector
   Inspector-VM-Interaction
   Publications
   People

Indices and tables
==================

* :ref:`genindex`
* :ref:`modindex`
* :ref:`search`

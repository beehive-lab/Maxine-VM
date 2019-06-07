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

To cite the software itself please use |zenodoDOI|.

.. |zenodoDOI| image:: https://zenodo.org/badge/86729772.svg
   :target: https://zenodo.org/badge/latestdoi/86729772
   :alt: Zenodo generated DOI

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

Features
--------

Some of the features of Maxine that make it a compelling platform for (J)VM research include:

-  Nearly all of the code base is written in Java and exploits advanced language features appearing in JDK 5 and beyond: for example annotations, static imports, and generics.
-  The VM integrates with openJDK.
   There's no need to download (and build) other implementations of the standard Java classes.
-  The source code supports development in Eclipse, Netbeans or IntelliJ all of which provide excellent support for cross-referencing and browsing the code.
   It also means that refactoring can be confidently employed to continuously improve the structure of the code.
-  :doc:`The Maxine Inspector <./Inspector>` produces visualizations of nearly every aspect of the VM runtime state, and provides advanced, VM-specific debugging.
-  The source code is hosted on GitHub making downloading and collaboration easier.

Roadmap
-------

-  Implement JVMCI, Upgrade to latest Graal
-  Run Truffle on top of Maxine VM/Graal
-  Port MMTk to Maxine VM

Acknowledgements
----------------

This documentation is heavily based on the original wiki pages (by Oracle) that can be found `here <https://web.archive.org/web/20150516045940/https://wikis.oracle.com/display/MaxineVM/Home>`__ and `here <https://community.oracle.com/community/java/java_hotspot_virtual_machine/maxine-vm>`__.

Table of Contents
=====================================

.. toctree::
   :maxdepth: 2

   build
   Status
   IDEs
   Publications
   People
   Debugging
   Offline-Compilation
   FAQ
   Glossary
   Actors
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

Indices and tables
==================

* :ref:`genindex`
* :ref:`modindex`
* :ref:`search`

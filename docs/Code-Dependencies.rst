Management of Code Dependencies
===============================

The Management of dependencies from compiled methods to classes, methods and other entities where such dependencies may change and result in some action (e.g. deoptimization) being applied to a compiled method.

Overall Architecture
--------------------

A dependency is a relationship between a ```TargetMethod`` <./Glossary#target-method>`__, that is, the result of a compilation, and an assumption that was made by the compiler during the compilation.
The assumption may be any invariant that can be checked for validity at a future time.
Assumptions are specified by subclasses of ``CiAssumptions.Assumption``.
Instances of such classes typically contain references to VM objects that, for example, represent methods, i.e., ``RiResolvedMethod``.
Note that assumptions at this level are generally specified using compiler and VM independent types, and are defined in a compiler and VM independent project (package).
However, there is nothing that prevents a VM specific assumption being defined using VM specific types.

Since an assumption has to be validated any time the global state of the VM changes, for example, a new class is loaded, it must persist as long as the associated ``TargetMethod``.
To minimize the amount of storage space occupied by assumptions, and to simplify analysis in a concrete VM, validated assumptions are converted to dependencies, which use a densely encoded form of the concrete VM types using small integers, such as ``ClassID``.

All assumptions have an associated context class which identifies the class that the assumption affects.
For example, the ConcreteSubtype assumption specifies that a class ``T`` has a single unique subtype ``U``.
In this case, ``T`` is defined to be the context class.

The possible set of assumptions and associated dependencies is open-ended.
In order to provide for easy extensibility while keeping the core of the system independent, the concept of a ``DependencyProcessor`` is introduced.
A ``DependencyProcessor`` is responsible for the following:

-  the validation of the associated assumption.
-  the encoding of the assumption into an efficient packed form
-  the processing of the packed form, converting back to an object form for ease of analysis
-  supporting the application of a dependency visitor for analysis
-  providing a string based representation of the dependency for tracing

Analysing Dependencies
----------------------

A visitor pattern is used to support the analysis of a ``Dependencies`` instance.
Recall that each such instance relates to a single ``TargetMethod``, may contain dependencies related to several context classes and each of these may contain dependencies corresponding to several dependency processors.

Since the set of ``DependencyProcessor``\ s is open ended, and a visitor may want to visit the data corresponding to several dependency processors in one visit, implementation class inheritance cannot be used to create a specific visitor.
Instead, a two-level type structure is used, with interfaces defined in the specific ``DependencyProcessor`` class that declare the statically typed methods that result from decoding the packed form of the dependency.
Note that these typically correspond closely to the original ``CiAssumptions.Assumption`` but with compiler/VM independent types replaced with Maxine specific types.
E.g., ``RiResolvedType`` replaced with ``ClassActor``.

Dependencies Visitor
--------------------

``Dependencies.DependencyVisitor`` handles the aspects of the iteration that are independent of the dependency processors.
See ``Dependencies.DependencyVisitor`` for more details.

The data for each dependency processor is visited by invoking ``Dependencies.DependencyVisitor.visit`` for each individual dependency.
This method is generic since it cannot know anything about the types of the data associated with the dependency.
The default implementation handles this by calling ``DependencyProcessor.match`` which returns ``dependencyVisitor`` if the visitor implements the ``DependencyProcessorVisitor`` interface defined by the processor that specifies the types of the data in the dependency, or ``null`` if not.
It then invokes ``DependencyProcessor.visit`` with this value, which invokes the typed method in the interface if the value is non-null, and steps the index to the next dependency.
Defining ``DependencyProcessor.visit`` this way allows a different ``DependencyProcessorVisitor`` to be called by an overriding implementation of ``Dependencies.DependencyVisitor.visit``.
For example, a visitor that cannot know all the dependency processors in the system, yet wants to invoke the ``DependencyProcessor.ToStringDependencyProcessorVisitor``.

Defining a new Dependency Processor
-----------------------------------

The first step is to define a new subclass of ``CiAssumptions.Assumption``.
If, as is typical, the dependency is used within the optimizing compiler, then this subclass should be defined by adding it to ``CiAssumptions``.

Next define a subclass of ``DependencyProcessor`` that will handle this assumption in Maxine, and place it in the ``com.sun.max.vm.compiler.deps`` package.
Define a nested interface that extends of ``DependencyProcessorVisitor`` and defines a method with the same arguments as the method in the ``CiAssumptions.Assumption`` subclass.
To support generic tracing of dependencies you should also define a subclass of ``DependencyProcessor.ToStringDependencyProcessorVisitor`` that implements your interface method(s) and appends appropriate tracing data to the ``StringBuilder`` variable in ``DependencyProcessor.ToStringDependencyProcessorVisitor``.

Define a static final instance of the ``DependencyProcessor`` subclass, which will cause it to be registered with ``DependenciesManager`` during boot image generation.

Finally, implement the remaining abstract methods:

-  ``DependencyProcessor.match``
-  ``DependencyProcessor.getToStringDependencyProcessorVisitor``
-  ``DependencyProcessor.visit``

The first two have trivial implementations.
The visit method must step over the specific dependency data and, if the ``dependencyProcessorVisitor`` is not null, invoke the associated method, with the encoded data transformed into the appropriate argument types.
Evidently, if the visitor is null, processing related to transforming the encoded data should be avoided.

Automatically generated from ``com.sun.max.vm.compiler.deps.package-info``

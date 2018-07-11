Actors
======

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
---------------------------------

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

See :doc:`JDK interoperation <./JDK-Interoperation>` for details and
examples.

Flags
-----

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
---------------

This section mentions a few members of the ``Actor`` type hierarchy; the
actual type hierarchy is a bit more complex.

ClassActor
~~~~~~~~~~

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
~~~~~~~~~~

A ``FieldActor`` contains the implementation details for a field in a Java
class.
Such details include a reference to the representation of the field's
type and to its holder: the instance of ``ClassActor`` representing the
implementation of the class to which the field belongs.

A subclass of ``FieldActor``, ``InjectedReferenceFieldActor``, represents a
synthesized field that has been added dynamically to a JDK class.

See :doc:`field injection <./JDK-Interoperation>` for
details and examples.

MethodActor
~~~~~~~~~~~

A ``MethodActor`` contains the implementation details for a method in a
Java class.
Such details include a reference to the representation of the method's
signature, to it's holder (the instance of ``ClassActor`` representing the
implementation of the class to which the method belongs), and to zero or
more possible compilations of the method.

The ``MethodActor`` class is itself abstract, with concrete subclasses
defined to implement various flavors of implementation: static methods,
virtual methods, interface methods, and so-called *miranda methods*.

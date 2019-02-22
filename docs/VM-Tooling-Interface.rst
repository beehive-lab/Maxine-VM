VM Tooling interface
====================

As a research VM, Maxine should make it is easy as possible to analyze
the behavior of applications and Maxine itself.
Since Maxine is meta-circular, many of the
techniques used to analyze applications are also applicable to Maxine,
although some aspects of meta-circularity can cause problems that can be
hard to foresee, such as trying to allocate on the heap during a garbage
collection.

Maxine supports (most of) the standard JVMTI API, which supports agents
written in native code.
In particular, the standard jdwp agent is supported which allows
debugging of applications and, experimentally, the VM itself, with a
Java IDE.
Since Maxine is written in Java, the native JVMTI interface is not very
appropriate, and Maxine provides a Java version of the JVMTI API, that
is much easier to use than the native interface.

VMTI
----

Maxine does not make reference to any specific tooling interface or
implementation in the core VM code.
Instead it defines an interface VMTI that defines methods that a tooling
implementation must implement to be included in Maxine.
Multiple tooling implementations can be active in a single VM.
Currently Maxine supports two tooling implementations, JVMTI and Virtual
Machine Level Analysis.
The latter, which overlaps somewhat with JVMTI, is specific to Maxine
and primarily supports the advising of the execution of the virtual
machine at the bytecode abstraction level.

JVMTI
-----

The JVMTI implementation is contained in the package
``com.oracle.max.vm.ext.jvmti``.
Although separately specified in an extension package, currently it is
included by default in the default boot image.
The implementation is incomplete but sufficient for many purposes,
including debugging.
Notable omissions are the methods related to monitor contention and the
reference walking heap iterators.
The implementation will be completed in due course.

JJVMTI
------

JJVMTI is a Java version of the standard JVMTI native interface.
As far as possible it is equivalent the native version, so translation
between the two should be straightforward.
Some design choices were changed to reflect the nature of Java.
For example, whereas JVMTI returns errors as the function result, and
uses pointers to caller defined variables to pass data, JJVMTI throws an
exception in the event of an error and returns data as the method
result.
Also, whereas JVMTI necessarily uses either JNI handles or scalar values
to represent classes and methods, JJVMTI uses Maxine's actor classes,
e.g., ``ClassActor`` Using JJVMTI

Writing a JJVMTI agent is considerably simpler than writing the
equivalent JVMTI native agent, as there is no need to deal with all the
complexity of the JVMTI and JNI native interfaces.
However, since the agent must necessarily access Maxine VM classes, it
must either be included in the boot image or dynamically loaded as a VM
extension (preferred).
Note that, unlike JVMTI native agents, JJVMTI agents cannot get control
early in the VM startup, so certain changes to the VM environment cannot
be made.
This currently does not affect Maxine as it does not reconfigure itself
in response to JVMTI capability requests.

The standard form of an agent that can be included in the boot image or
loaded dynamically is as follows:

.. code:: java

    public class Agent extends NullJJVMTICallbacks {

        private static Agent agent;
        private static String AgentArgs;

        static {
            agent = (Agent) JJVMTIAgentAdapter.register(new Agent());
            if (MaxineVM.isHosted()) {
                VMOptions.addFieldOption("-XX:", "AgentArgs", "arguments for exemplar JJVMTI agent");
            }
        }

        /***
         * VM extension entry point.
         * @param args
         */
        public static void onLoad(String agentArgs) {
            AgentArgs = agentArgs;
            agent.onBoot();
        }

        /**
         * Boot image entry point.
         */
        @Override
        public void onBoot() {
            agent.setEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_INIT, null);
        }

        @Override
        public void vmInit() {
            if (AgentArgs != null) {
                // process arguments and enable needed JVMTI capabilities
            }
        }
    }

Note that the mechanism for communicating arguments to the agent is
necessarily different between a boot image agent and a dynamically
loaded agent.
In the former case a new VM command line option is defined, whereas in
the latter case the arguments are passed in the VM extension option,
``-vmextension:jar[=args]``.
Note also that onBoot is only called in boot image mode and onLoad is
only called when dynamically loaded, as per the specification for VM
extensions.
Since the VM is still in ``PRIMORDIAL`` mode in ``onBoot`` the recommended
idiom is to enable the ``VMINIT`` event and do all further processing in
the ``vmInit`` event callback, which is invoked (if enabled) in either
case.

An example of a complete JJVMTI agent is the conversion of the native
heap viewer agent that is supplied as a demo with the JDK.
This agent also demonstrates one of the meta-circularity issues with
JJVMTI.
The ``heapIteration`` callback is not callback safe in JVMTI terminology,
in particular, it cannot allocate.
However, when the agent is dynamically loaded, Maxine will attempt to
allocate implicitly as the ``heapIteration`` method will be compiled at
the point that it is first invoked.
This is finessed by the agent by forcing the compilation of
``heapIteration`` in the ``vmInit`` method.
Note that this is not an issue if the agent is included in the boot
image owing to ahead of time compilation.
It would also be mitigated if Maxine kept a separate VM and application
heap.

Developing Maxine on IDEs
=========================

Eclipse
-------

Launch Configuration
~~~~~~~~~~~~~~~~~~~~

Once you have installed Eclipse, we recommend modifying its launch
configuration by editing the ``eclipse.ini`` file in the Eclipse
installation directory.
In particular, you will want to give it more memory with the following
options:

::

    -XX:MaxPermSize=512m
    -Xmx1g
    -Xms512m

On GNU/Linux the ``eclipse.ini`` file is located under
``/usr/lib/eclipse/``.

On Mac OS X the ``eclipse.ini`` file is hidden inside the application
bundle Eclipse.app, which shows up as a single executable in the Finder.
Open the bundle, either with the Finder *Show Package Contents*
contextual menu item, or with an editor such as Emacs.
The file can be found at: ``Eclipse.app/Contents/MacOS/eclipse.ini``.

Configuring Eclipse
~~~~~~~~~~~~~~~~~~~

When first launching Eclipse for Maxine development, you should create a new workspace in the directory where you have checked out the Maxine sources.

You will then need to install the CDT plugin if you want to edit and
build the Maxine C code from within Eclipse.
We also strongly recommend installing the Checkstyle plugin to simplify conforming with the Maxine coding conventions.

Next, ensure that the default JRE being used for development is at least a JDK 7 installation.
This will be the case by default when Eclipse is running on
JDK 7.
Otherwise you will manually have to change this setting in the ``Java > Installed JREs`` preference page.
Once a JDK has been selected, you should set some default VM options for it by selecting it and hitting the ``Edit...`` button.
In the ``Default VM Arguments`` field in the dialog that comes up, add the ``-ea`` option.
Also add the ``-d64`` option if you are on a platform (such as Linux or Solaris) where the JVM can be launched in either 32-bit or 64-bit mode.
Obviously this requirement will change should there ever be a 32-bit
Maxine port.

Creating and Importing the Maxine Eclipse projects
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Once Eclipse has been configured, you need to run a short ``mx`` command that will create the Eclipse project configurations for all the projects in the Maxine workspace:

::

    mx ideinit

The above command will actually create IDE project configurations for
all supported IDEs (currently Eclipse and NetBeans).
To create only Eclipse project configurations, replace ideinit with
eclipseinit.

You use the Import Wizard to import the created/updated projects.

#. From the main menu bar, select ``File > Import...`` to open the
   Import Wizard.
#. Select ``General > Existing Project into Workspace`` and click
   ``Next``.
#. Choose ``Select root directory`` and click the associated ``Browse``
   button to locate the top level Maxine directory containing the
   projects.
#. Under ``Projects`` select all the projects.
#. Click ``Finish`` to complete the import.

Once the sources have finished importing, Eclipse will automatically
compile them.
It will also run ``gmake`` to build the C code in the Native project.
If the latter process appears to fail, the most common cause is ``gmake``
not being on the ``PATH`` of the environment from which Eclipse was
launched.

**Note:** Occasionally, a new Eclipse project is added to the Maxine
source code.
This usually results in an Eclipse error message indicating that a
project is missing another required Java project.
To handle this, you simply need to repeat the steps above for
discovering and importing projects.

Building the boot image
~~~~~~~~~~~~~~~~~~~~~~~
Assuming all the sources compiled successfully, you can build the VM boot image by following the instructions :doc:`here <./build>`.

To run the VM, open a console and use ``mx vm``.

Cloning Git workspaces
~~~~~~~~~~~~~~~~~~~~~~

Git makes it very easy to clone an existing workspace for the purpose of experimentation.
Eclipse on the other hand does not have a simple mechanism for copying settings from one Eclipse workspace to another.
We've found that the simplest thing to do is to copy the .metadata directory from an existing Eclipse workspace to the workspace created by the git clone operation.

For example:

.. code-block:: shell

    % ls
    maxine
    % git clone maxine sandbox
    % cp -r maxine/.metadata sandbox/.metadata

Then select all the projects (in the Package Explorer view) and perform ``File > Refresh`` in Eclipse for the cloned workspace.

Netbeans
--------

Ensure that you select a JDK 7 during the installation process.
The result of the installation process is a directory named netbeans (hereafter referred to as ``$IDE_HOME``).
Note that on Mac OS X, the installation directory will be ``/Applications/NetBeans`` and the directory denoted by ``$IDE_HOME`` in these instructions is ``/Applications/NetBeans/NetBeans<version>.app/Contents/Resources/NetBeans``.

Before starting NetBeans, it's useful to tune its configuration by editing the ``$IDE_HOME/etc/netbeans.conf`` file.
In particular, the ``netbeans_default_options`` value can be modified to increase the heap size of the JVM running NetBeans (e.g. add ``-J-Xmx1g`` to the value).

Generate the NetBean configuration files
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The mx script can be used to generate NetBean project configurations for each project in the Maxine code base.
Simply run ``mx netbeansinit`` and follow the instructions it prints out to the console.

IntelliJ
--------

Generate eclipse project files describing module dependencies
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Executing the following command will create eclipse and netbeans project files describing the dependencies between the different modules of Maxine.
These project files will later be parsed by IntelliJ to understand and import the module dependencies.

::

    mx ideinit

Create a new IntelliJ project
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Open IntelliJ and:

#. Select ``File > New Project``.
#. Select the ``Create new Java project from existing sources`` option.
#. Use Maxine as the name of the project and for ``Project file  location``, select the directory where you checked out the Maxine code.
   When you click ``Next``, IntelliJ should find the source directories automatically.
#. IntelliJ will not find any libraries for the project, click ``Next``.
#. IntelliJ should infer correct modules for the project, click ``Next``.
#. IntelliJ should not infer any facets for the project, click ``Finish``.

Add JUnit4 library
~~~~~~~~~~~~~~~~~~

You will need JUnit 4.0+ in order to compile Maxine.
It is probably best not try to compile Maxine from within IntelliJ before this step; its caches may become confused later, and it won't work anyway.

#. Select ``File > Settings``.
#. Select ``Project Settings``.
#. Select ``Libraries``.
#. Click the plus icon to add a new library.
#. Use the name *JUnit4* for the library.
#. Apply the library to all the modules.
#. Click ``Add Classes`` and navigate to the location of your junit4.jar file.
#. Click ``OK``.

More memory for Java Compiler
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Maxine has some rather large source files, and javac will likely run out of memory.
You need to increase the amount of memory available to it by:

#. Select ``File > Settings``.
#. Select ``Compiler``.
#. Change the value for ``Maximum heap size`` to 1024.

Use the create_jar script to create a JAR file with the class files from the C1X, CRI, and HotSpotVM project.
This JAR file has to be added to the bootclasspath of the modified VM.
Example command line arguments for HotSpot:
-XX:+UseC1X -XX:TraceC1X=5 -Xbootclasspath/a:THIS_DIRECTORY/c1x4hotspot.jar SomeClass
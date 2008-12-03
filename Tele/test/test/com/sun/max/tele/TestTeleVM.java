/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package test.com.sun.max.tele;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.LogManager;

import com.sun.max.ide.JavaProject;
import com.sun.max.program.Classpath;
import com.sun.max.tele.TeleVM;
import com.sun.max.tele.grip.TeleGripScheme;
import com.sun.max.tele.method.TeleDisassembler;
import com.sun.max.vm.object.host.HostObjectAccess;
import com.sun.max.vm.prototype.BinaryImageGenerator;
import com.sun.max.vm.prototype.BootImageException;
import com.sun.max.vm.prototype.Prototype;
import com.sun.max.vm.prototype.PrototypeClassLoader;


/**
 * Creates a running {@link TeleVM}, suitable for testing.
 * 
 * @author Michael Van De Vanter
 */
public class TestTeleVM {
    
    // Should be the TELE library
    private static final String _INSPECTOR_LIBRARY_NAME = "inspector";

    private static TeleVM _teleVM = null;
    
    public final static TeleVM create(boolean advanceToEntry) {
        final File bootJar = BinaryImageGenerator.getDefaultBootImageJarFilePath();                
        Classpath classpathPrefix = Classpath.EMPTY;
        // May want to add something later
        classpathPrefix = classpathPrefix.prepend(bootJar.getAbsolutePath());
        final Classpath classpath = Classpath.fromSystem().prepend(classpathPrefix);
        PrototypeClassLoader.setClasspath(classpath);        
        Prototype.loadLibrary(_INSPECTOR_LIBRARY_NAME);
        final File bootImageFile = BinaryImageGenerator.getDefaultBootImageFilePath();       
        final Classpath sourcepath = JavaProject.getSourcePath(true);        
        final File projectDirectory = JavaProject.findVcsProjectDirectory();        
        final String[] commandLineArguments = {
                        "-verbose:class",
                        "-classpath",
                        (projectDirectory.toString() + "/bin"),
                        "test.com.sun.max.tele.HelloWorld"
        };        
        final int debuggeeId = -1;        
        try {
            _teleVM = TeleVM.createNewChild(bootImageFile, sourcepath, commandLineArguments, debuggeeId);
            TeleDisassembler.initialize(_teleVM);
            final TeleGripScheme teleGripScheme = (TeleGripScheme) _teleVM.maxineVM().configuration().gripScheme();
            teleGripScheme.setTeleVM(_teleVM);
            if (advanceToEntry) { 
                _teleVM.advanceToJavaEntryPoint();
            }
        } catch (BootImageException e) {
            System.out.println("Failed to load boot image " + bootImageFile.toString());
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("IOException at startup");
            e.printStackTrace();
        }
        
        return _teleVM;        
    }
    
    public final static void main(String[] argv) {
        HostObjectAccess.setMainThread(Thread.currentThread());
        LogManager.getLogManager().getLogger("").setLevel(Level.ALL);
        System.out.println("creating VM");
        final TeleVM teleVM = create(true);
        System.out.println("end creating VM");
        try {
            
            teleVM.teleProcess().controller().resume(true, true);
        } catch (Exception e) {            
            e.printStackTrace();
        }
    }
    
}

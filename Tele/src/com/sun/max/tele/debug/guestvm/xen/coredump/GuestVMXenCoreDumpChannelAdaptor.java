/*
 * Copyright (c) 2009 Sun Microsystems, Inc., 4150 Network Circle, Santa
 * Clara, California 95054, U.S.A. All rights reserved.
 *
 * U.S. Government Rights - Commercial software. Government users are
 * subject to the Sun Microsystems, Inc. standard license agreement and
 * applicable provisions of the FAR and its supplements.
 *
 * Use is subject to license terms.
 *
 * This distribution may include materials developed by third parties.
 *
 * Parts of the product may be derived from Berkeley BSD systems,
 * licensed from the University of California. UNIX is a registered
 * trademark in the U.S.  and in other countries, exclusively licensed
 * through X/Open Company, Ltd.
 *
 * Sun, Sun Microsystems, the Sun logo and Java are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other
 * countries.
 *
 * This product is covered and controlled by U.S. Export Control laws and
 * may be subject to the export or import laws in other
 * countries. Nuclear, missile, chemical biological weapons or nuclear
 * maritime end uses or end users, whether direct or indirect, are
 * strictly prohibited. Export or reexport to countries subject to
 * U.S. embargo or to entities identified on U.S. export exclusion lists,
 * including, but not limited to, the denied persons and specially
 * designated nationals lists is strictly prohibited.
 *
 */
package com.sun.max.tele.debug.guestvm.xen.coredump;

import java.io.*;

import com.sun.max.elf.xen.*;
import com.sun.max.tele.debug.guestvm.xen.dbchannel.*;

/**
 * @author Puneeet Lakhina
 *
 */
public class GuestVMXenCoreDumpChannelAdaptor {

    File imageFile;
    File dumpFile;

    public GuestVMXenCoreDumpChannelAdaptor(File imageFile, File dumpFile) {
        this.imageFile = imageFile;
        this.dumpFile = dumpFile;
    }



    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        File dumpFile = null;
        File imageFile = null;

        for (int i = 0; i < args.length; i++) {
            if ((args[i].equals("-d") || args[i].equals("--dump-file")) && i < (args.length - 1)) {
                dumpFile = new File(args[++i]);
                if (!dumpFile.exists()) {
                    System.err.println("Dump File does not exist");
                    return;
                }
            }
            if ((args[i].equals("-i") || args[i].equals("--image-file")) && i < (args.length - 1)) {
                imageFile = new File(args[++i]);
                if (!imageFile.exists()) {
                    System.err.println("Image File does not exist");
                    return;
                }
            }
        }

        if (dumpFile == null || imageFile == null) {
            System.err.println("Usage: [-d|--dump-file] [coredumpfile] [-i|--image-file] [guestvmimage] ");
            return;
        }
        RandomAccessFile dumpraf = new RandomAccessFile(dumpFile, "r");
        XenCoreDumpELFReader reader = new XenCoreDumpELFReader(dumpraf);
        System.out.println(reader.getNotesSection());
        reader.getAllGuestContexts();
        System.out.println(ImageFileHandler.open(imageFile.getAbsolutePath()).getBootHeapStartSymbolAddress());
    }

}

/**
 *
 */
package com.sun.max.tele.debug.guestvm.xen.coredump;

import java.io.*;
import java.util.logging.*;

import com.sun.max.elf.*;

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

    private static final Logger logger = LogManager.getLogManager().getLogger(GuestVMXenCoreDumpChannelAdaptor.class.toString());

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
                    logger.severe("Dump File does not exist");
                    return;
                }
            }
            if ((args[i].equals("-i") || args[i].equals("--image-file")) && i < (args.length - 1)) {
                imageFile = new File(args[++i]);
                if (!imageFile.exists()) {
                    logger.severe("Image File does not exist");
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
    }

}

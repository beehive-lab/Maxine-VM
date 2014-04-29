package test.arm.asm;
import java.io.*;

import com.sun.max.platform.*;

public class MaxineARMTester {

    public static boolean debug = false;

    public static final String ENABLE_QEMU = "max.arm.qemu";
    public static boolean ENABLE_SIMULATOR = false;

    public static final int NUM_REGS = 17;
    // Checkstyle: off
    public enum BitsFlag {
        Bit0(0x1),
        Bit1(0x2),
        Bit2(0x4),
        Bit3(0x8),
        Bit4(0x10),
        Bit5(0x20),
        Bit6(0x40),
        Bit7(0x80),
        Bit8(0x100),
        Bit9(0x200),
        Bit10(0x400),
        Bit11(0x800),
        Bit12(0x1000),
        Bit13(0x2000),
        Bit14(0x4000),
        Bit15(0x8000),
        Bit16(0x10000),
        Bit17(0x20000),
        Bit18(0x40000),
        Bit19(0x80000),
        Bit20(0x100000),
        Bit21(0x200000),
        Bit22(0x400000),
        Bit23(0x800000),
        Bit24(0x1000000),
        Bit25(0x2000000),
        Bit26(0x4000000),
        Bit27(0x8000000),
        Bit28(0x10000000),
        Bit29(0x20000000),
        Bit30(0x40000000),
        Bit31(0x80000000),
        NZCBits(0xe0000000),
        NZCVBits(0xf0000000),
        Lower16Bits(0x0000ffff),
        Upper16Bits(0xffff0000),
        All32Bits (0xffffffff);
        // Checkstyle: on

        public static final BitsFlag[] values = values();
        private final long value;
        private BitsFlag(long value) {
            this.value = value;
        }

        public long value() {
            return value;
        }
    }

     private BitsFlag bitMasks[];
     private Process qemu;
     private Process gdb;
     private char chars[];
     private long[] expectRegs = new long[NUM_REGS];
     private long[] gotRegs = new long[NUM_REGS];
     private boolean[] testRegs = new boolean[NUM_REGS];
     public static int oldpos = 0;

    /*
     * arm-unknown-eabi-gcc -c -march=armv7-a -g test.c -o test.o arm-unknown-eabi-as -mcpu=cortex-a9 -g startup.s -o
     * startup.o arm-unknown-eabi-as -mcpu=cortex-a9 -g asm_entry.s -o asm_entry.o arm-unknown-eabi-ld -T test.ld test.o
     * startup.o asm_entry.o -o test.elf arm-unknown-eabi-objcopy -O binary test.elf test.bin qemu-system-arm -cpu
     * cortex-a9 -M versatilepb -m 128M -nographic -s -S -kernel test.bin
     */
    @SuppressWarnings("unused")
    public boolean objcopy() {
        boolean success = true;
        final ProcessBuilder objcopy = new ProcessBuilder("arm-unknown-eabi-objcopy", "-O", "binary", "test.elf", "test.bin");
        objcopy.redirectOutput(new File("objcopy-output"));
        objcopy.redirectError(new File("objcopy-errors"));

        try {
            Process done = objcopy.start();
            final int objCopyStatus = done.waitFor();
        } catch (Exception e) {
            System.err.println(e);
            e.printStackTrace();
            success = false;
        }
        return success;
    }

    @SuppressWarnings("unused")
    public boolean compile() {
        boolean success = true;
        final ProcessBuilder removeFiles = new ProcessBuilder("/bin/rm", "-rR", "gdb-fullOutput", "gdb-output", "test.bin", "test.elf");
        final ProcessBuilder compile = new ProcessBuilder("arm-unknown-eabi-gcc", "-c", "-march=armv7-a", "-g", "test.c", "-o", "test.o");
        compile.redirectOutput(new File("gcc-output"));
        compile.redirectError(new File("gcc-errors"));

        try {
            removeFiles.start().waitFor();
            final Process done = compile.start();
            final int compileStatus = done.waitFor();

        } catch (Exception e) {
            System.err.println(e);
            e.printStackTrace();
            success = false;
        }
        return success;
    }

    @SuppressWarnings("unused")
    public boolean assembleStartup() {
        boolean success = true;
        final ProcessBuilder assemble = new ProcessBuilder("arm-unknown-eabi-as", "-mcpu=cortex-a9", "-g", "startup.s", "-o", "startup.o");
        assemble.redirectOutput(new File("as-output"));
        assemble.redirectError(new File("as-errors"));
        try {
            final Process done = assemble.start();
            final int assembleStatus = done.waitFor();
        } catch (Exception e) {
            System.err.println(e);
            e.printStackTrace();
            success = false;
        }
        return success;
    }

    @SuppressWarnings("unused")
    public boolean assembleEntry() {
        boolean success = true;
        final ProcessBuilder assemble = new ProcessBuilder("arm-unknown-eabi-as", "-mcpu=cortex-a9", "-g", "asm_entry.s", "-o", "asm_entry.o");
        assemble.redirectOutput(new File("as-Eoutput"));
        assemble.redirectError(new File("as-Eerrors"));
        try {
            final Process done = assemble.start();
            final int assembleStatus = done.waitFor();
        } catch (Exception e) {
            System.err.println(e);
            e.printStackTrace();
            success = false;
        }
        return success;
    }

    @SuppressWarnings("unused")
    public boolean link() {
        boolean success = true;
        final ProcessBuilder link = new ProcessBuilder("arm-unknown-eabi-ld", "-T", "test.ld", "test.o", "startup.o", "asm_entry.o", "-o", "test.elf");
        link.redirectOutput(new File("link-output"));
        link.redirectError(new File("link-errors"));
        try {
            final Process done = link.start();
            final int linkStatus = done.waitFor();
        } catch (Exception e) {
            System.err.println(e);
            e.printStackTrace();
            success = false;
        }
        return success;
    }
    private void cleanProcesses() throws Exception {
        /*
            Necessary as sometime stale processes might be left around
         */
        ProcessBuilder processes = new ProcessBuilder("killall","qemu-system-arm");
        processes.start().waitFor();
        processes = new ProcessBuilder("killall","qemu-system-arm");
        processes.start().waitFor();
        processes = new ProcessBuilder("killall","arm-unknown-eabi-gdb");
        processes.start().waitFor();
        processes = new ProcessBuilder("killall","arm-unknown-eabi-gdb");
        processes.start().waitFor();
    }
    @SuppressWarnings("unused")
    public long[] runRegisterSimulation() {
        boolean sucess = true;
        boolean qemuReady = false;
        int qemuStatus, gdbStatus, grepStatus, bitmask = 0;
        String registers = null;
        ProcessBuilder gdbB = null;

        ProcessBuilder qemuB = new ProcessBuilder("qemu-system-arm", "-cpu", "cortex-a9", "-M", "versatilepb", "-m", "128M", "-nographic", "-s", "-S", "-kernel", "test.bin");
        qemuB.redirectOutput(new File("qemuoutput"));
        qemuB.redirectError(new File("qeemmerserrs"));
        try {
            cleanProcesses();
            qemu = qemuB.start();
            while (new File("qemuoutput").exists() == false) {
                Thread.sleep(500);
            }
            do{

                ProcessBuilder bindTest = new ProcessBuilder("lsof", "-i", "TCP:1234");
                bindTest.redirectOutput(new File("bindOutput"));
                bindTest.start().waitFor();
                FileInputStream inputStream = new FileInputStream("bindOutput");
                if(inputStream.available() == 0) {
                    qemuReady = false;
                    Thread.sleep(500);
                }
                else qemuReady = true;
                inputStream = null;
                log("IS qemu ready" + qemuReady);

            }while(!qemuReady);


            log("MaxineARMTester::QEMU started");
            log("MaxineARMTester::arm-unknown-eabi-gdb started");

            gdbB = new ProcessBuilder("arm-unknown-eabi-gdb");
            gdbB.redirectInput(new File("gdbCommands"));
            gdbB.redirectOutput(new File("gdb-fullOUTPUT"));
            gdbB.redirectError(new File("ERRORS"));
            gdb = gdbB.start();
            gdbStatus = gdb.waitFor();

            log("GDB STATUS " + gdbStatus);

            qemuStatus = qemu.waitFor();

            log("MaxineARMTester::arm-unknown-eabi-gdb started");

            qemuB = new ProcessBuilder("grep", "-A", "16", "r0*0x*", "gdb-fullOUTPUT");
            qemuB.redirectOutput(new File("gdb-output"));
            qemuB.redirectError(new File("gdb-grep-ERROR"));
            qemu = qemuB.start();
            grepStatus = qemu.waitFor();
            registers = readFile("gdb-output");
        } catch (Exception e) {
            System.err.println(e);
            System.out.println(e);
            e.printStackTrace();
            System.exit(-1);
        }

        log("MaxineARMTester::about to read results started");

        chars = registers.toCharArray();
        for (int i = 0; i < NUM_REGS; i++) {
            gotRegs[i] = findRegister(0, i);
            gotRegs[i] = gotRegs[i] & bitMasks[i].value();
            if (gotRegs[i] >= 0x80000000L) {
                gotRegs[i] = gotRegs[i] - 0x100000000L;
            }
        }
        return gotRegs;
    }



    @SuppressWarnings("unused")
    public boolean runSimulation() throws Exception {
        boolean success = false;
        boolean qemuReady = false;
        int qemuStatus, gdbStatus, grepStatus, bitmask = 0;
        String registers = null;
        ProcessBuilder gdbB = null;
        ProcessBuilder qemuB = new ProcessBuilder("qemu-system-arm", "-cpu", "cortex-a9", "-M", "versatilepb", "-m", "128M", "-nographic", "-s", "-S", "-kernel", "test.bin");
        qemuB.redirectOutput(new File("qemuoutput"));
        qemuB.redirectError(new File("qeemmerserrs"));
        try {
            cleanProcesses();

            qemu = qemuB.start();
            while (new File("qemuoutput").exists() == false) {
                Thread.sleep(500);
            }
            do{

                ProcessBuilder bindTest = new ProcessBuilder("lsof", "-i", "TCP:1234");
                bindTest.redirectOutput(new File("bindOutput"));
                bindTest.start().waitFor();
                FileInputStream inputStream = new FileInputStream("bindOutput");
                if(inputStream.available() == 0) qemuReady = false;
                else qemuReady = true;
                if(qemuReady)
                    log("MaxineARMTester:: qemu ready");
                else      {
                    log("MaxineARMTester: gemu NOT READY");
                    Thread.sleep(500);

                }
                inputStream = null;

            }while(!qemuReady);

            gdbB = new ProcessBuilder("arm-unknown-eabi-gdb");
            gdbB.redirectInput(new File("gdbCommands"));
            gdbB.redirectOutput(new File("gdb-fullOUTPUT"));
            gdbB.redirectError(new File("ERRORS"));
            gdb = gdbB.start();
            qemuStatus = qemu.waitFor();
            gdbStatus = gdb.waitFor();
            qemuB = new ProcessBuilder("grep", "-A", "16", "r0*0x*", "gdb-fullOUTPUT");
            qemuB.redirectOutput(new File("gdb-output"));
            qemuB.redirectError(new File("gdb-grep-ERROR"));
            qemu = qemuB.start();
            grepStatus = qemu.waitFor();
            registers = readFile("gdb-output");
        } catch (Exception e) {
            System.out.println(e);
            System.err.println(e);
            e.printStackTrace();
            System.exit(-1);
        }
        chars = registers.toCharArray();
        log("MaxineARMTester registers");
        log(registers);
        success = false;
        for (int i = 0; i < NUM_REGS; i++) {
            gotRegs[i] = findRegister(0, i);
            gotRegs[i] = gotRegs[i] & bitMasks[i].value();
            if (gotRegs[i] >= 0x80000000L) {
                gotRegs[i] = gotRegs[i] - 0x100000000L;
            }
            if (testRegs[i]) {
                if (gotRegs[i] != expectRegs[i]) {
                    System.out.println("Register " + i + " Expected " + expectRegs[i] + " " + Long.toString(expectRegs[i], 16) + " Simulated " + gotRegs[i] + " " + Long.toString(gotRegs[i], 16));
                    bitmask = bitmask | (1 << i);
                    success = false;
                }
            }
            if (testRegs[i] == true) {
                System.out.println("REGISTER " + i + " EXPECTED " + Long.toString(expectRegs[i], 16) + " Simulated " + Long.toString(gotRegs[i], 16));
            }
        }
        assert (success == true) : "Bit mask of incorrect registers " + bitmask;
        assert(success == true);
        return success;
    }

    public MaxineARMTester(long expected[], boolean test[], BitsFlag range[]) {
        initializeQemu();
        bitMasks = range;
        for (int i = 0; i < NUM_REGS; i++) {
            expectRegs[i] = expected[i];
            testRegs[i] = test[i];
        }
    }

    public MaxineARMTester(String args[]) {
        initializeQemu();
        for (int i = 0; i < NUM_REGS; i++) {
            testRegs[i] = false;
        }
        for (int i = 0; i < args.length; i += 2) {
            expectRegs[Integer.parseInt(args[i])] = Long.parseLong(args[i + 1]);
            testRegs[Integer.parseInt(args[i])] = true;
        }
    }

    private void initializeQemu() {
        //String enableQemu = System.getProperty(ENABLE_QEMU);
        //if (enableQemu != null) {
        //    ENABLE_SIMULATOR =  Integer.parseInt(enableQemu).intValue() > 0 ? true : false;
        //} else {
        //    ENABLE_SIMULATOR = false;
        //}
        ENABLE_SIMULATOR = Integer.getInteger(ENABLE_QEMU) != null && Integer.getInteger(ENABLE_QEMU) > 0 ? true : false;
 }

    private byte[] readBin(String file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[fis.available()];
        fis.read(data);
        fis.close();
        return data;
    }

    private void writeBin(String file, byte[] data) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(data, 0, data.length);
        fos.close();
    }

    private void writeToFile(String file, String content) throws IOException {
        PrintWriter out = new PrintWriter(new File(file));
        out.print(content);
        out.close();
    }

    private String readFile(String file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line = null;
        StringBuilder stringBuilder = new StringBuilder();
        String ls = System.getProperty("line.separator");
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
            stringBuilder.append(ls);
        }
        return stringBuilder.toString();
    }

    /*
     * Returns an integer (-1 if not found) indicating where a register lies in the byte array of the object
     * representing output of a gdb/qemu hosted simulation. NOTE: names of registers such as sp, pc, lr are emitted
     * instead of r13,r14,r15.
     */
    private long findRegister(int startPos, int regNumber) {
        assert (regNumber >= 0 && regNumber <= 16);
        if (regNumber == 0) {
            oldpos = 0;
            assert (startPos == 0);
        }
        if (regNumber < 13) {
            while (chars[oldpos] != 'r') {
                oldpos++; // find the position of the 'r'
            }
        }
        if (regNumber == 13) { //sp
            while (chars[oldpos] != 's') {
                oldpos++;
            }
        }
        if (regNumber == 14) { //lr
            while (chars[oldpos] != 'l') {
                oldpos++;
            }
        }
        if (regNumber == 15) { //pc
            while (chars[oldpos] != 'p') {
                oldpos++;
            }
        }
        if (regNumber == 16) { // cpsr
            while (chars[oldpos] != '>') {
                oldpos++;
            }
        }
        switch (regNumber) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
                oldpos += 17;
                break;
            case 10:
            case 11:
            case 12:
                oldpos += 17;
                break;

            case 13: // sp
            case 14: // lr
            case 15: // pc
                oldpos += 17;
                break;
            case 16: // cpsr
                while (chars[oldpos] != 'x') {
                    oldpos++;
                }
                oldpos++;
                break;
        }
        startPos = oldpos;
        while (chars[oldpos] != ' ') {
            if (chars[oldpos] == '\t') {
                oldpos++;
                break;
            }
            oldpos++;
        }
        oldpos--;
        final String val = new String(chars, startPos, oldpos - startPos);
        log(val);
        return Long.parseLong(val, 16);

    }

    public void run() throws Exception {
        assembleStartup();
        assembleEntry();
        compile();
        link();
        objcopy();
        runSimulation();
    }

    public static void enableDebug() {
        debug = true;
    }

    public static void disableDebug() {
        debug = false;
    }

    private void log(String msg) {
        if (debug) {
            System.out.println(msg);
        }
    }

    public static void main(String[] args) throws Exception {
        MaxineARMTester tester = new MaxineARMTester(args);
        tester.run();
    }
}

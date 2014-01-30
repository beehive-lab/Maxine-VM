package test.arm;

import com.oracle.max.asm.target.armv7.ARMV7;
import com.oracle.max.asm.target.armv7.ARMV7Assembler;
import com.sun.cri.ci.*;
import com.sun.max.ide.MaxTestCase;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: yaman
 * Date: 10/12/13
 * Time: 14:55
 * To change this template use File | Settings | File Templates.
 */
public class ARMV7AssemblerTest extends MaxTestCase {
    ARMV7Assembler asm;
    CiTarget armv7;

    static final class Pair {
        public final int first;
        public final int second;
        public Pair(int first, int second) {
            this.first = first;
            this.second = second;
        }
    }


    public ARMV7AssemblerTest() {
        // TODO: get correct spillSlotSize (using 4 for now)
        armv7 = new CiTarget(new ARMV7(),
                false,
                4,
                0,
                4096,
                0,
                false,
                false,
                false);

        asm = new ARMV7Assembler(armv7, null);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ARMV7AssemblerTest.class);
    }

    public void testPatchJumpTarget() throws Exception {

    }
    // public void adc(final ConditionFlag cond, final boolean s, final CiRegister Rd, final CiRegister Rn, final int immed_8, final int rotate_amount)


    public void testAdc() throws Exception {
        // public CiRegister(int number, int encoding, int spillSlotSize, String name, RegisterFlag... flags)


        // testing conditionflags (signed/unsigned)
        for(int i=0; i < ARMV7Assembler.ConditionFlag.values().length; i++) {
            asm.adc(ARMV7Assembler.ConditionFlag.values()[i], false, armv7.arch.registers[0], armv7.arch.registers[1], 0, 0);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x02A10000 | ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
            asm.adc(ARMV7Assembler.ConditionFlag.values()[i], true, armv7.arch.registers[0], armv7.arch.registers[1], 0, 0);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x02B10000 | ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
        }
    }

    public void testAdclsl() throws Exception {
        for(int i=0; i < ARMV7Assembler.ConditionFlag.values().length; i++) {
            asm.adclsl(ARMV7Assembler.ConditionFlag.values()[i], false, armv7.arch.registers[0], armv7.arch.registers[1],
                    armv7.arch.registers[2], 0);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x00A10002 | ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
            asm.adclsl(ARMV7Assembler.ConditionFlag.values()[i], true, armv7.arch.registers[0], armv7.arch.registers[1],
                    armv7.arch.registers[2], 0);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x00B10002 | ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
        }
    }

    public void testAdd() throws Exception {
        for(int i=0; i < ARMV7Assembler.ConditionFlag.values().length; i++) {
            asm.add(ARMV7Assembler.ConditionFlag.values()[i], false, armv7.arch.registers[0], armv7.arch.registers[1], 0, 0);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x02810000 | ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
            asm.add(ARMV7Assembler.ConditionFlag.values()[i], true, armv7.arch.registers[0], armv7.arch.registers[1], 0, 0);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x02910000 | ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
        }
    }

    public void testAddror() throws Exception {
        for(int i=0; i < ARMV7Assembler.ConditionFlag.values().length; i++) {
            asm.addror(ARMV7Assembler.ConditionFlag.values()[i], false, armv7.arch.registers[0], armv7.arch.registers[1],
                    armv7.arch.registers[2], 0);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x00810062 | ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
            asm.addror(ARMV7Assembler.ConditionFlag.values()[i], true, armv7.arch.registers[0], armv7.arch.registers[1],
                    armv7.arch.registers[2], 0);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x00910062 | ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
        }
    }

    public void testBiclsr() throws Exception {
        for(int i=0; i < ARMV7Assembler.ConditionFlag.values().length; i++) {
            asm.biclsr(ARMV7Assembler.ConditionFlag.values()[i], false, armv7.arch.registers[0], armv7.arch.registers[1],
                    armv7.arch.registers[2], armv7.arch.registers[3]);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x01C10332 | ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
            asm.biclsr(ARMV7Assembler.ConditionFlag.values()[i], true, armv7.arch.registers[0], armv7.arch.registers[1],
                    armv7.arch.registers[2], armv7.arch.registers[3]);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x01D10332 | ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
        }
    }

    public void testCmnasr() throws Exception {
        for(int i=0; i < ARMV7Assembler.ConditionFlag.values().length; i++) {
            asm.cmnasr(ARMV7Assembler.ConditionFlag.values()[i], armv7.arch.registers[0], armv7.arch.registers[1],
                    armv7.arch.registers[2]);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x01700251 | ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
        }

    }

    public void testCmnror() throws Exception {
        for(int i=0; i < ARMV7Assembler.ConditionFlag.values().length; i++) {
            asm.cmnror(ARMV7Assembler.ConditionFlag.values()[i], armv7.arch.registers[0], armv7.arch.registers[1],
                    armv7.arch.registers[2]);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x01700271 | ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
        }
    }

    public void testCmpasr() throws Exception {
        for(int i=0; i < ARMV7Assembler.ConditionFlag.values().length; i++) {
            asm.cmpasr(ARMV7Assembler.ConditionFlag.values()[i], armv7.arch.registers[0], armv7.arch.registers[1],
                    0);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x01500041 | ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
        }
    }

    public void testEorlsr() throws Exception {
        for(int i=0; i < ARMV7Assembler.ConditionFlag.values().length; i++) {
            asm.eorlsr(ARMV7Assembler.ConditionFlag.values()[i], false, armv7.arch.registers[0], armv7.arch.registers[1],
                    armv7.arch.registers[2], armv7.arch.registers[3]);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x00210332| ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
            asm.eorlsr(ARMV7Assembler.ConditionFlag.values()[i], true, armv7.arch.registers[0], armv7.arch.registers[1],
                    armv7.arch.registers[2], armv7.arch.registers[3]);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x00310332 | ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
        }
    }

    public void testMovror() throws Exception {
        for(int i=0; i < ARMV7Assembler.ConditionFlag.values().length; i++) {
            asm.movror(ARMV7Assembler.ConditionFlag.values()[i], false, armv7.arch.registers[0], armv7.arch.registers[1],
                    0);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x01A00061| ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
            asm.movror(ARMV7Assembler.ConditionFlag.values()[i], true, armv7.arch.registers[0], armv7.arch.registers[1],
                    0);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x01B00061 | ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
        }
    }

    public void testMvnror() throws Exception {
        for(int i=0; i < ARMV7Assembler.ConditionFlag.values().length; i++) {
            asm.mvnror(ARMV7Assembler.ConditionFlag.values()[i], false, armv7.arch.registers[0], armv7.arch.registers[1],
                    armv7.arch.registers[2]);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x01E00271| ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
            asm.mvnror(ARMV7Assembler.ConditionFlag.values()[i], true, armv7.arch.registers[0], armv7.arch.registers[1],
                    armv7.arch.registers[2]);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x01F00271 | ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
        }
    }

    public void testOrrlsl() throws Exception {
        for(int i=0; i < ARMV7Assembler.ConditionFlag.values().length; i++) {
            asm.orrlsl(ARMV7Assembler.ConditionFlag.values()[i], false, armv7.arch.registers[0], armv7.arch.registers[1],
                    armv7.arch.registers[2], 0);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x01810002| ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
            asm.orrlsl(ARMV7Assembler.ConditionFlag.values()[i], true, armv7.arch.registers[0], armv7.arch.registers[1],
                    armv7.arch.registers[2], 0);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x01910002 | ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
        }
    }

    public void testRsb() throws Exception {
        for(int i=0; i < ARMV7Assembler.ConditionFlag.values().length; i++) {
            asm.rsb(ARMV7Assembler.ConditionFlag.values()[i], false, armv7.arch.registers[0], armv7.arch.registers[1],
                    0, 0);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x02610000 | ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
            asm.rsb(ARMV7Assembler.ConditionFlag.values()[i], true, armv7.arch.registers[0], armv7.arch.registers[1],
                    0, 0);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x02710000| ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
        }
    }

    public void testRsblsl() throws Exception {
        for(int i=0; i < ARMV7Assembler.ConditionFlag.values().length; i++) {
            asm.rsblsl(ARMV7Assembler.ConditionFlag.values()[i], false, armv7.arch.registers[0], armv7.arch.registers[1],
                    armv7.arch.registers[2], 0);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x00610002 | ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
            asm.rsblsl(ARMV7Assembler.ConditionFlag.values()[i], true, armv7.arch.registers[0], armv7.arch.registers[1],
                    armv7.arch.registers[2], 0);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x00710002 | ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
        }
    }

    public void testRsc() throws Exception {
        for(int i=0; i < ARMV7Assembler.ConditionFlag.values().length; i++) {
            asm.rsc(ARMV7Assembler.ConditionFlag.values()[i], false, armv7.arch.registers[0], armv7.arch.registers[1],
                    armv7.arch.registers[2]);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x00E10002 | ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
            asm.rsc(ARMV7Assembler.ConditionFlag.values()[i], true, armv7.arch.registers[0], armv7.arch.registers[1],
                    armv7.arch.registers[2]);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x00F10002 | ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
        }
    }

    public void testRsclsr() throws Exception {
        for(int i=0; i < ARMV7Assembler.ConditionFlag.values().length; i++) {
            asm.rsclsr(ARMV7Assembler.ConditionFlag.values()[i], false, armv7.arch.registers[0], armv7.arch.registers[1],
                    armv7.arch.registers[2], armv7.arch.registers[3]);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x00E10332 | ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
            asm.rsclsr(ARMV7Assembler.ConditionFlag.values()[i], true, armv7.arch.registers[0], armv7.arch.registers[1],
                    armv7.arch.registers[2], armv7.arch.registers[3]);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x00F10332 | ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
        }
    }

    public void testSbcror() throws Exception {
        for(int i=0; i < ARMV7Assembler.ConditionFlag.values().length; i++) {
            asm.sbcror(ARMV7Assembler.ConditionFlag.values()[i], false, armv7.arch.registers[0], armv7.arch.registers[1],
                    armv7.arch.registers[2], armv7.arch.registers[3]);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x00C10372 | ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
            asm.sbcror(ARMV7Assembler.ConditionFlag.values()[i], true, armv7.arch.registers[0], armv7.arch.registers[1],
                    armv7.arch.registers[2], armv7.arch.registers[3]);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x00D10372 | ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
        }
    }

    public void testSub() throws Exception {
        for(int i=0; i < ARMV7Assembler.ConditionFlag.values().length; i++) {
            asm.sub(ARMV7Assembler.ConditionFlag.values()[i], false, armv7.arch.registers[0], armv7.arch.registers[1],
                    0, 0);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x02410000| ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
            asm.sub(ARMV7Assembler.ConditionFlag.values()[i], true, armv7.arch.registers[0], armv7.arch.registers[1],
                    0, 0);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x02510000 | ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
        }
    }

    public void testTst() throws Exception {
        for(int i=0; i < ARMV7Assembler.ConditionFlag.values().length; i++) {
            asm.tst(ARMV7Assembler.ConditionFlag.values()[i], armv7.arch.registers[0], 0);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x03100000| ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
        }
    }

    public void testTstlsr() throws Exception {
        for(int i=0; i < ARMV7Assembler.ConditionFlag.values().length; i++) {
            asm.tstlsr(ARMV7Assembler.ConditionFlag.values()[i], armv7.arch.registers[0], armv7.arch.registers[0], 0);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x01100020| ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
        }
    }

    public void testSmlal() throws Exception {
        for(int i=0; i < ARMV7Assembler.ConditionFlag.values().length; i++) {
            asm.smlal(ARMV7Assembler.ConditionFlag.values()[i], false, armv7.arch.registers[0], armv7.arch.registers[1],
                    armv7.arch.registers[2], armv7.arch.registers[3]);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x00E10293| ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
            asm.smlal(ARMV7Assembler.ConditionFlag.values()[i], true, armv7.arch.registers[0], armv7.arch.registers[1],
                    armv7.arch.registers[2], armv7.arch.registers[3]);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x00F10293 | ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
        }
    }

    public void testUmull() throws Exception {
        for(int i=0; i < ARMV7Assembler.ConditionFlag.values().length; i++) {
            asm.umull(ARMV7Assembler.ConditionFlag.values()[i], false, armv7.arch.registers[0], armv7.arch.registers[1],
                    armv7.arch.registers[2], armv7.arch.registers[3]);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x00810293| ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
            asm.umull(ARMV7Assembler.ConditionFlag.values()[i], true, armv7.arch.registers[0], armv7.arch.registers[1],
                    armv7.arch.registers[2], armv7.arch.registers[3]);
            assertTrue(asm.codeBuffer.getInt(0) == ( 0x00910293 | ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
            asm.codeBuffer.reset();
        }
    }

    public void testLdradd() throws Exception {

    }

    public void testLdrsubw() throws Exception {

    }

    public void testLdraddrorpost() throws Exception {

    }

    public void testStrsubasr() throws Exception {

    }

    public void testStrsubrorw() throws Exception {

    }

    public void testStraddpost() throws Exception {

    }

    public void testSwi() throws Exception {

    }

    public void testCheckConstraint() throws Exception {

    }
}

package test.arm;

import com.oracle.max.asm.target.armv7_real.ARMV7;
import com.oracle.max.asm.target.armv7_real.ARMV7Assembler;
import com.sun.cri.ci.*;
import com.sun.max.ide.MaxTestCase;

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

        // testing conditionflags
//        for(int i=0; i < ARMV7Assembler.ConditionFlag.values().length; i++) {
//            asm.adc(ARMV7Assembler.ConditionFlag.values()[i], false, armv7_real.arch.registers[0], armv7_real.arch.registers[1], 0, 0);
//            assertTrue(asm.codeBuffer.getInt(0) == ( 0x00A10000 | ARMV7Assembler.ConditionFlag.values()[i].value() <<28));
//            asm.codeBuffer.reset();
//        }

        asm.adc(ARMV7Assembler.ConditionFlag.Always, true, armv7.arch.registers[4], armv7.arch.registers[2], 0, 0);
        assertTrue(asm.codeBuffer.getInt(0) == 0xE0B10000);
        System.out.println("PASSED: assertTrue(asm.codeBuffer.getInt(0) == 0xE3A10000);");
        asm.codeBuffer.reset();
        asm.adc(ARMV7Assembler.ConditionFlag.Always, true, armv7.arch.registers[0], armv7.arch.registers[1], 0, 0);
        assertTrue(asm.codeBuffer.getInt(0) == 0xE0B10000);
        System.out.println("PASSED: assertTrue(asm.codeBuffer.getInt(0) == 0xE3A10000);");
    }

    public void testAdclsl() throws Exception {

    }

    public void testAdd() throws Exception {

    }

    public void testAddror() throws Exception {

    }

    public void testBiclsr() throws Exception {

    }

    public void testCmnasr() throws Exception {

    }

    public void testCmnror() throws Exception {

    }

    public void testCmpasr() throws Exception {

    }

    public void testEorlsr() throws Exception {

    }

    public void testMovror() throws Exception {

    }

    public void testMvnror() throws Exception {

    }

    public void testOrrlsl() throws Exception {

    }

    public void testRsb() throws Exception {

    }

    public void testRsblsl() throws Exception {

    }

    public void testRsc() throws Exception {

    }

    public void testRsclsr() throws Exception {

    }

    public void testSbcror() throws Exception {

    }

    public void testSub() throws Exception {

    }

    public void testTst() throws Exception {

    }

    public void testTstlsr() throws Exception {

    }

    public void testSmlal() throws Exception {

    }

    public void testUmull() throws Exception {

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

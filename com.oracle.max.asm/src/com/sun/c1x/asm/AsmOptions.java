package com.sun.c1x.asm;

public class AsmOptions {
    public static int     InitialCodeBufferSize         = 232;
    public static int     Atomics                       = 0;
    public static boolean UseNormalNop                  = true;
    public static boolean UseAddressNop                 = true;
    public static boolean UseIncDec                     = false;
    public static boolean UseXmmLoadAndClearUpper       = false;
    public static boolean UseXmmRegToRegMoveAll         = false;

    public static boolean GenAssertionCode              = false;
}

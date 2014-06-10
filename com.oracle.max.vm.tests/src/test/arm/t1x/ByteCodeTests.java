package test.arm.t1x;

public class ByteCodeTests {

    public int run() {
        int mycounter = 0;
        for (int i = 0; i < 20; i++) {
            mycounter++;
        }
        return mycounter;
    }
}

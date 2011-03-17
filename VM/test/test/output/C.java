package test.output;


public class C extends B {

    @Override
    public void m1() {
        System.out.print("C.m1");
    }
    @Override
    public void m2() {
        System.out.print("C.m2");
    }

    @Override
    public void m3() {
        System.out.print("C.m3");
    }

    public void m5() {
        System.out.print("C.m5");
    }

    public static void main(String [] args) {
        C c = new C();
        A ca = new C();
        A a = new B();

        c.m1();
        c.m2();
        a.m6();
        ca.m6();
    }
}

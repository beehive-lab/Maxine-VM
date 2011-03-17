package test.output;


public abstract class D extends C {
    abstract void m7();

    public static void main(String [] args) {
        D [] ad = new D[0];
        D [] [] add = new D[0][0];
        C [] [] cc = add;
        C [] c = ad;
        Object [] od = new Object[0];

        Object [] od2 =  ad;

        D [][][] d3 = new D[0][0][0];
        Object [][][] o3 = d3;
        Object [][] o2 = o3;
        o2 = d3;
        Object [] o1 = o3;
        o1 = d3;
        Cloneable cl = d3;
        Cloneable [][] cl2 = d3;


        System.out.println("Super class of D []  :" + D[].class.getSuperclass());
        System.out.println("Super class of D [][]  :" + D[][].class.getSuperclass());

    }
}

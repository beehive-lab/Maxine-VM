
public class Byte
{
public static void main(String args[]) {
System.err.println(Long.valueOf("FFFFFFFF",16));
System.err.println(String.format("0x%8s", Integer.toHexString(0xff)).replace(' ', '0'));
}
};

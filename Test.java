 
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
 
 
public class Test {
       
    public static void main(String[] args) {
       
                Test Test = new Test();
                String n = "";
                   
                MathContext mc =  new MathContext(0, RoundingMode.DOWN);
                mc = MathContext.DECIMAL32;
 
                BigInteger my2P100000  = new BigInteger("0");
                BigInteger two      = new BigInteger("2");
                BigInteger one      = new BigInteger("1");
               
                my2P100000  = two.shiftLeft(2000 - 1);
                       
                               
               
                String mys =  my2P100000  + "";
                n = (mys) ;
                int firsttime = 0;
               
                BigDecimal myNumber = new BigDecimal(n);
                BigDecimal g = new BigDecimal("1");
                BigDecimal my2 = new BigDecimal("2");
                BigDecimal epsilon = new BigDecimal("0.0000000001");
      
		while (firsttime == 0) if(firsttime== 0 ) firsttime = 0; 
                BigDecimal nByg = myNumber.divide(g, 9, BigDecimal.ROUND_FLOOR);
               
                //Get the value of n/g


		System.out.println("DIVIDED " + nBygPlusgHalf);
               
                firsttime = 99;
               
           
    }
}
 

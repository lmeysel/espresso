package rs.blif;

public class Latch {
 public String input;
 public String output;
 public String type = "as"; // fe = falling edge,  re = rising edge,  ah = active high,  al = active low,  as = asynchronous
 public String control = "NIL";
 public byte initVal = 3; // 0 = zero  1 = one  2 = don't care  3 = unknown
}

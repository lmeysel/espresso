package rs.binfunction;

import java.util.ArrayList;
import java.util.List;

/**
 * BinFunction describes a logic function, that has an onset and a don't-care set.
 * @author Mitja Stachowiak, Ludwig Meysel
 */
public class BinFunction {
 private final Set on;    public Set on () { return this.on; }
 private final Set dc;    public Set dc () { return this.dc; }
 public int numInputs () { return this.on.width(); }
 private String[] names;    public String[] names () { return this.names; }
 // definition of two-bit representations
 public final static int INV = 0;
 public final static int ONE = 2;
 public final static int ZERO = 1;
 public final static int DC = 3;
 public final static int inverseONE = 1;
 public final static int inverseZERO = 2;
 
 public BinFunction (int numInputs) {
  this.on = new Set(numInputs);
  this.dc = new Set(numInputs);
  names = new String[numInputs+1]; 
 }
 
 /**
  * Complementation: Computes the off-set from on-set and dc-set (Extremely slow)
  * @return
  * The off-set
  * @throws Exception
  * Fails, if there are invalid cubes in the function.
  */
 public List<Cube> computeOff () throws Exception {
  List<Cube> onDc = new ArrayList<Cube>(); // onDc contains all implicants that are not in the offset. So not onDC is the offset, which has to be expanded to get a disjunctive form
  onDc.addAll(this.on);
  onDc.addAll(this.dc);
  // ToDo: sort cube for reaching don't cares early and merging overlaps
  Cube n;
  n = new Cube(numInputs());
  List<Cube> off = new ArrayList<Cube>();
  multiply(0, n, onDc, off);
  return off;
 }
 /**
  * Computes recursively the disjunctive, expanded function of src' i.E. (ab'c + ac+ a'bc')' --> (a'+b+c') & (a'+c') & (a+b'+c) --> c'b' + c'a + c'a'b' + bc'a + ba'c + ba'c + a'c'b' 
  * Do first call with impl = 0, prod = DC DC DC..., dst = {}
  * @param c
  * maybe create the output-cubes as expanded cubes
  * @param impl
  * the index of the current implicant in src
  * @param prod
  * the product of the previous implicants
  * @param src
  * the source interpreted as negated konjunctive function
  * @param dst
  * the expanded disjunctive result
  * @throws Exception
  */
 private void multiply (int impl, Cube prod, List<Cube> src, List<Cube> dst) throws Exception {
  if (impl >= src.size()) {
   dst.add(prod);
   return;
  }
  for (int i = 0; i < numInputs(); i++) {
   Cube n = prod.clone(Cube.class);
   switch (src.get(impl).getVar(i)) {
    case inverseONE :
     n.andVar(i, ONE);
     break;
    case inverseZERO :
     n.andVar(i, ZERO);
     break;
    case DC :
     continue; // don't expand DCs
    default :
     throw new Exception("Invalid function input!");
   }
   if (n.getVar(i) == INV) continue;
   multiply(impl+1, n, src, dst);
  }
 }
 
 @Override
 public String toString () { return toString(on); }
 public String toString (Set set) {
  String s = "";
  boolean inv = false;
  boolean one = false;
  String v;
  for (int i = 0; i < set.size(); i++) {
   if (!s.equals("")) s += " + ";
   int dcCnt = 0;
   for (int j = 0; j < numInputs(); j++) {
    if (names[j] != null) v = names[j];
    else v = ""+j;
    switch (((Cube)set.get(i)).getVar(j)) {
     case INV :
      s += v+"!";
      inv = true;
      break;
     case ONE :
      s += v+" ";
      break;
     case ZERO :
      s += v+"'";
      break;
     case DC :
      dcCnt++;
      break;
    }
    if (dcCnt == numInputs()) one = true;
   }
  }
  if (inv) s = "INVALID  ("+s+")";
  else if (one) s = "ONE  ("+s+")";
  else if (s.length() == 0) s = "ZERO";
  if (names[numInputs()] != null) s = names[numInputs()] + " = " + s;
  return s;
 }

}
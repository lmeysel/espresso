package rs.binfunction;

/**
 * BinFunction describes a logic function, that has an onset and a don't-care set.
 * @author Mitja Stachowiak, Ludwig Meysel
 */
public class BinFunction {
 private final Set on;    public Set on () { return this.on; }
 private final Set dc;    public Set dc () { return this.dc; }
 public int numInputs () { return this.on.width(); }
 private String[] names;    public String[] names () { return this.names; }
 public String name () { return this.names[this.names.length-1]; }
 // definition of two-bit representations
 public final static int INV = 0;
 public final static int ONE = 2;
 public final static int ZERO = 1;
 public final static int DC = 3;
 private final static int inverseONE = 1;
 private final static int inverseZERO = 2;
 
 public BinFunction (int numInputs) {
  this.on = new Set(numInputs);
  this.dc = new Set(numInputs);
  this.names = new String[numInputs+1]; 
 }
 public BinFunction (int numInputs, String[] names) {
  this(numInputs);
  this.names = names; 
 }
 
 /**
  * Computes the off-set of this function by applying the Presto-Algorithm on the function.
  * @return
  * the off-set, which is not (highly) optimized but intersect-free.
  */
 public IntersectFreeSet computeOff () {
  IntersectFreeSet off = new IntersectFreeSet(numInputs());
  Cube c = new Cube(numInputs()); // initialized with DCs
  addToOffset(off, c);
  return off;
 }
 private void addToOffset(IntersectFreeSet off, Cube c) {
  // regard on-set
  for (int i = 0; i < this.on.size(); i++) {
   Cube a = c.and(this.on.get(i));
   if (!a.isValid()) continue; // c has no intersection with on[i]
   if (a.equals(c)) return; // c is completely covered by one existing on[i]
   for (int j = 0; j < c.width; j++) if (c.getVar(j) == BinFunction.DC && a.getVar(j) != BinFunction.DC) {
    // split into smaller cubes and try again...
    Cube c1 = c.clone();
    Cube c2 = c.clone();
    c1.setVar(j, BinFunction.ONE);
    c2.setVar(j, BinFunction.ZERO);
    addToOffset(off, c1);
    addToOffset(off, c2);
    return;
   }
  }
  // regard dc-set
  for (int i = 0; i < this.dc.size(); i++) {
   Cube a = c.and(this.dc.get(i));
   if (!a.isValid()) continue; // c has no intersection with dc[i]
   if (a.equals(c)) return; // c is completely covered by one existing dc[i]
   for (int j = 0; j < c.width; j++) if (c.getVar(j) == BinFunction.DC && a.getVar(j) != BinFunction.DC) {
    // split into smaller cubes and try again...
    Cube c1 = c.clone();
    Cube c2 = c.clone();
    c1.setVar(j, BinFunction.ONE);
    c2.setVar(j, BinFunction.ZERO);
    addToOffset(off, c1);
    addToOffset(off, c2);
    return;
   }
  }
  // c has no intersects with on- or dc-set
  off.forceAdd(c); // after the this algorithm, off will automatically be intersect-free (no further check required)
 }
 
 /**
  * Complementation: Computes the off-set from on-set and dc-set by applying DeMorgan's law (Extremely slow)
  * @return
  * The off-set
  * @throws Exception
  * Fails, if there are invalid cubes in the function.
  */
 public Set computeOff_deMorgan () throws Exception {
  Set onDc = new Set(numInputs()); // onDc contains all implicants that are not in the offset. So not onDC is the offset, which has to be expanded to get a disjunctive form
  onDc.addAll(this.on);
  onDc.addAll(this.dc);
  // ToDo: sort cube for reaching don't cares early and merging overlaps
  Cube n;
  n = new Cube(numInputs());
  Set off = new Set(numInputs());
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
 private void multiply (int impl, Cube prod, Set src, Set dst) throws Exception {
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
 public String toString () {
  String r = "";
  if (on.intersects(dc)) r += "[on and dc intersect!]";
  r += on.toString(this.names);
  if (dc.size() > 0) r += "     DC_"+dc.toString(this.names);
  return r;
 }
 
 public String isEquivalent(BinFunction foreign) {
  if (this.numInputs() != foreign.numInputs()) return "no (different number of inputs)";
  boolean namesMatch = true;
  for (int i = 0; i < this.names.length; i++) if ((this.names[i] == null) != (foreign.names[i] == null) || (this.names[i] != null) && !this.names[i].equals(foreign.names[i])) { namesMatch = false; break;}
  Set fOnDc = new Set(foreign.on().width());
  fOnDc.addAll(foreign.on);
  fOnDc.addAll(foreign.dc);
  for (int i = 0; i < this.on.size(); i++) if (!fOnDc.covers(this.on.get(i))) return "no (the "+i+"-th cube of tested function is not covered by the input function)";
  for (int i = 0; i < foreign.on.size(); i++) if (!this.on.covers(foreign.on.get(i))) return "no (the "+i+"-th cube of input function is not covered by the tested function)";
  if (namesMatch) return "yes";
  return "yes (names do not match)";
 }
 
 public int cost() {
  int r = 0;
  for (int i = 0; i < on.size(); i++) r += on.width() - on.get(i).cardinality2();
  return r;
 }

}
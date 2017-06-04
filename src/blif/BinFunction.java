package blif;

import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * BinFunction describes a logic function, that has an onset and a don't-care set.
 * @author Mitja Stachowiak, Ludwig Meysel
 *
 */
public class BinFunction {
 private final Set on;    public Set on () { return this.on; }
 private final Set dc;    public Set dc () { return this.dc; }
 public int numInputs () { return this.on.width; }
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
  * Computes the off-set from onset and dc-set (Extremely slow)
  * @return
  * @throws Exception
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
 
 public String toString () { return toString(on); }
 public String toString (List<Cube> set) {
  String s = "";
  String v;
  for (int i = 0; i < set.size(); i++) {
   if (!s.equals("")) s += " + ";
   for (int j = 0; j < numInputs(); j++) {
    if (names[j] != null) v = names[j];
    else v = ""+j;
    switch (((Cube)set.get(i)).getVar(j)) {
     case INV :
      return "invalid";
     case ONE :
      s += v+" ";
      break;
     case ZERO :
      s += v+"'";
      break;
    }
   }
  }
  if (names[numInputs()] != null) s = names[numInputs()] + " = " + s;
  return s;
 }
 
 
 
 
 
 /**
  * The class Cube stores one and-Combination of variable dependencies of a Function.
  * This class is meant to be extended in other units to store additional info about the cube.
  * @author Mitja Stachowiak, Ludwig Meysel
  *
  */
 public static class Cube {
  protected static final long TOTAL_INVALID = Long.parseUnsignedLong("FFFFFFFFFFFFFFFF", 16);
  public final int width;
  public final long[] cube;
  
  public Cube(int width) {
   this.width = width;
   cube = new long[(int)Math.ceil((width) / 32.0)];
   Arrays.fill(cube, TOTAL_INVALID);
  }

  public Cube (String s) throws Exception {
   this(s.length());
   for (int i = 0; i < width; i++) switch (s.charAt(i)) {
    case '0' :
     setVar(i, ZERO);
     break;
    case '1' :
     setVar(i, ONE);
     break;
    case '-' :
     setVar(i, DC);
     break;
    default :
     throw new Exception("Unknown logic character '"+s.charAt(i)+"'!");
   }
  }
  
  /**
   * returns the n-th variable of cube
   */
  public int getVar(int n) {
   return (int) ((cube[n / 32] >>> (n % 32)*2) & 3);
  }

  /**
   * sets the n-th variable of cube to v
   */
  public void setVar(int n, int v) {
   cube[n / 32] &= ~(3 << (n % 32)*2);
   cube[n / 32] |= v << (n % 32)*2;
  }
  
  public void andVar(int n, int v) {
   cube[n / 32] &= (v << (n % 32)*2) | ~(3 << (n % 32)*2);
  }
  
  public void orVar(int n, int v) {
   cube[n / 32] |= (v << (n % 32)*2);
  }
  
  /**
   * Copies the current cube and can also be used, to convert it up to an extending class. This class can override clone() to add copies of the additional information.
   * @param c
   * A class, that extends Cube can be given here. This class must have a constructor like Cube (int width)
   * @return
   * A new cube of class cubeType, that contains all Cube information of the current cube (this)
   */
  public Cube clone (Class<?> cubeType) {
   Cube r;
   try {
    r = (Cube)cubeType.getConstructor(int.class).newInstance(width);
   } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
    return null;
   }
   for (int i = 0; i < cube.length; i++) r.cube[i] = this.cube[i];
   return r;
  }
  
  public Cube clone () {
   return clone(this.getClass());
  }
  
  public boolean equals (Cube foreign) {
   if (width != foreign.width) return false;
   for (int i = 0; i < cube.length; i++) if (cube[i] != foreign.cube[i]) return false;
   return true;
  }
  
  /**
   * @return
   * returns the cardinality of the set M specified by this cube
   */
  public BigInteger cardinality () { // untested!
   BigInteger two = new BigInteger("2");
   BigInteger n = new BigInteger("1");
   for (int i = 0; i < width / 32; i++) {
    long p = cube[i];
    for (int j = 0; j < 32; j++) {
     if ((p & INV) == INV) return new BigInteger("0");
     if ((p & DC) == DC) n = n.multiply(two);
     p = p >>> 2;
    }
   }
   long p = cube[cube.length-1];
   for (int j = 0; j < width % 32; j++) {
    if ((p & INV) == INV) return new BigInteger("0");
    if ((p & DC) == DC) n = n.multiply(two);
    p = p >>> 2;
   }
   return n;
  }
  
  /**
   * Or-combines with another cube.
   *
   * @param foreign
   *           The "other" cube.
   * @return The or-combined result of this and another cube.
   */
  public Cube or (Cube foreign) {
   if (this.width != foreign.width) throw new IllegalArgumentException("Cubes must be of same width.");
   Cube ret = this.clone();
   for (int i = 0; i < ret.cube.length; i++) ret.cube[i] = this.cube[i] | foreign.cube[i];
   return ret;
  }

  /**
   * And-combines with another cube.
   * 
   * @param foreign
   *           The "other" cube.
   * @return The and-combined result of this and another cube.
   */
  public Cube and (Cube foreign) {
   if (this.width != foreign.width) throw new IllegalArgumentException("Cubes must be of same width.");
   Cube ret = this.clone();
   for (int i = 0; i < ret.cube.length; i++) ret.cube[i] = this.cube[i] & foreign.cube[i];
   return ret;
  }

  /**
   * Gets a flag indicating whether the cube is valid (i.e. contains no invalid-bit)
   */
  public boolean isValid() {
   for (int i = 0; i < width; i++) if (getVar(i) == INV) return false; // One variable is invalid
   for (int i = width; i < cube.length*32; i++) if (getVar(i) != DC) return false; // Unused variable > width is not don't care
   return true;
  }
 }


  
  
  public static class Set extends ArrayList<Cube> {
   private static final long serialVersionUID = -8446625631682699485L;
   private final int width;    public int width () { return this.width; }
   public Set (int width) {
    super();
    this.width = width;
   }
   
   public boolean add(Cube c) {
    if (c.width != this.width) return false;
    return super.add(c);
   }
   
   /**
    * Checks, weather the given cube u is completely covered by this set.
    * If u is element of this, the function checks, weather u is covered by this without u
    * @param u
    * @return
    */
   public boolean covers(final Cube u) { return covers(u, u); }
   private boolean covers(final Cube u, final Cube ignore) {
    int i = 0;
    for (i = 0; i < this.size(); i++) {
     Cube a = this.get(i);
     if (a == ignore) continue; // this without u
     a = u.and(a);
     if (!a.isValid()) continue; // u has no intersection with cube[i]
     if (a.equals(u)) return true; // u is completely covered by the existing cube[i]
     for (int j = 0; j < u.width; j++) if (u.getVar(j) == DC && a.getVar(j) != DC) {
      // split into smaller cubes and try again...
      Cube c1 = u.clone();
      Cube c2 = u.clone();
      c1.setVar(j, ONE);
      c2.setVar(j, ZERO);
      return covers(c1, ignore) && covers(c2, ignore);
     }
    }
    // u has no intersects with existing cubes
    return false;
   }
  }
  
  
  
 /*
 public static class IntersectFreeSet extends Set {
  private static final long serialVersionUID = 8905066173022612097L;

  public IntersectFreeSet(int width) { super(width); }
  
  /**
   * Adds cube c intersect-free:
   *  - Adds c, if c has no intersects with the existing cubes in the Set.
   *  - Don't adds c, if c is completely covered by one existing cube.
   *  - Splits up c into smaller cubes and tries to add them, if c has intersections with the existing cubes.
   * @return
   * Returns true, if something was added
   
  public boolean add(final Cube c) {
   for (int i = 0; i < this.size(); i++) {
    Cube a = c.and(this.get(i));
    if (!a.isValid()) continue; // c has no intersection with cube[i]
    if (a.equals(c)) return false; // c is completely covered by one existing cube[i]
    for (int j = 0; j < c.width; j++) if (c.getVar(j) == DC && a.getVar(j) != DC) {
     // split into smaller cubes and try again...
     Cube c1 = c.clone();
     Cube c2 = c.clone();
     c1.setVar(j, ONE);
     c2.setVar(j, ZERO);
     return add(c1) | add(c2);
    }
   }
   // c has no intersects with existing cubes
   super.add(c);
   return true;
  }
  
  /**
   * Checks, wheather the given cube u is completely covered by this set
   * @param u
   * @return
   
  public boolean covers(final Cube u) {
   BigInteger cover_card = new BigInteger("0");
   for (int i = 0; i < this.size(); i++) {
    Cube a = u.and(this.get(i));
    cover_card = cover_card.add(a.cardinality());
   }
   return cover_card.equals(u.cardinality());
  }
 }
 */
}
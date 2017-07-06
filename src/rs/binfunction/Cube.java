package rs.binfunction;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

/**
 * The class Cube stores one and-Combination of variable dependencies of a Function.
 * This class is meant to be extended in other units to store additional info about the cube.
 * @author Mitja Stachowiak, Ludwig Meysel
 *
 */
public class Cube {
 protected static final long TOTAL_DC = Long.parseUnsignedLong("FFFFFFFFFFFFFFFF", 16);
 public final int width;
 protected final long[] cube;
 private int card = -1;
 
 public Cube(int width) {
  this.width = width;
  cube = new long[(int)Math.ceil((width) / 32.0)];
  Arrays.fill(cube, TOTAL_DC);
 }

 public Cube (String s) throws Exception {
  this(s.length());
  for (int i = 0; i < width; i++) switch (s.charAt(i)) {
   case '0' :
    setVar(i, BinFunction.ZERO);
    break;
   case '1' :
    setVar(i, BinFunction.ONE);
    break;
   case '-' :
    setVar(i, BinFunction.DC);
    break;
   default :
    throw new Exception("Unknown logic character '"+s.charAt(i)+"'!");
  }
 }
 
 /**
  * Call this method, if this cube's literals will never be changed again, to cache important values.
  */
 protected void keepFixed () {
  card = cardinality2();
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
  * (Optional) A class, that extends Cube can be given here. This class must have a constructor like Cube (int width)
  * @return
  * A new cube of class cubeType, that contains all Cube information of the current cube (this)
  */
 @Override
 public Cube clone () { return clone(this.getClass()); }
 public Cube clone (Class<?> cubeType) {
  Cube r;
  try {
   r = (Cube)cubeType.getConstructor(int.class).newInstance(width);
  } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
   return null;
  }
  for (int i = 0; i < cube.length; i++) r.cube[i] = this.cube[i];
  r.card = this.card;
  return r;
 }
 
 @Override
 public boolean equals (Object foreign) { return equals((Cube)foreign); }
 public boolean equals (Cube foreign) {
  if (width != foreign.width) return false;
  for (int i = 0; i < cube.length; i++) if (cube[i] != foreign.cube[i]) return false;
  return true;
 }
 
 public void invalidate () {
  if (width > 0) cube[0] = 0; // set the 0-th part to zero for faster isValid-Check.
 }
 
 /**
  * @return
  * Returns the (cardinality ld 2) of the set specified by this cube so that 2^result = cardinality; result = -1, if cardinality is 0 (cube contains invalid)
  */
 public int cardinality2 () {
  if (card != -1) return card;
  int r = 0;
  for (int i = 0; i < width / 32; i++) {
   long p = cube[i];
   for (int j = 0; j < 32; j++) {
    if ((p & BinFunction.DC) == BinFunction.INV) return -1;
    if ((p & BinFunction.DC) == BinFunction.DC) r++;
    p = p >>> 2;
   }
  }
  long p = cube[cube.length-1];
  for (int j = 0; j < width % 32; j++) {
   if ((p & BinFunction.DC) == BinFunction.INV) return -1;
   if ((p & BinFunction.DC) == BinFunction.DC) r++;
   p = p >>> 2;
  }
  return r;
 }
 
 /**
  * Or-combines with another cube.
  *
  * @param foreign
  * The "other" cube.
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
  * The "other" cube.
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
  // TODO: Time critical function! Don't check each literal separately but continuously shift each long or try mask operations
  for (int i = 0; i < width; i++) if (getVar(i) == BinFunction.INV) return false; // One variable is invalid
  return true;
 }
 
 @Override
 public String toString () {
  String s = "";
  for (int i = 0; i < this.width; i++) switch (this.getVar(i)) {
   case BinFunction.INV :
    s += i+"!";
    break;
   case BinFunction.ZERO :
    s += i+"'";
    break;
   case BinFunction.ONE :
    s += i+" ";
    break;
  }
  return s;
 }
}
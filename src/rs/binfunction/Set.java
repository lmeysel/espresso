package rs.binfunction;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

/**
 * The class Set represents one set of several cubes.
 * @author Mitja Stachowiak
 *
 */
public class Set extends ArrayList<Cube> {
 private static final long serialVersionUID = -8446625631682699485L;
 private final int width;    public int width () { return this.width; }
 
 
 public Set (int width) {
  super();
  this.width = width;
 }
 
 @Override
 public boolean add(Cube c) {
  if (c.width != this.width) return false;
  return super.add(c);
 }
 
 public boolean intersects (Cube c) {
  for (int i = 0; i < this.size(); i++) if (this.get(i).and(c).isValid()) return true;
  return false;
 }
 
 public boolean intersects (Set s) {
  for (int i = 0; i < s.size(); i++) if (this.intersects(s.get(i))) return true;
  return false;
 }
 
 /**
  * Checks, weather the given cube u is completely covered by this set.
  * If u is element of this, the function checks, weather u is covered by this without u
  * @param u
  * @return
  * true, if u is covered
  */
 public boolean covers(final Cube u) { return covers(u, u, 0, this.size(), null); }
 public boolean covers(final Cube u, int from, int to) { return covers(u, u, from, to, null); }
 public boolean covers(final Cube u, final Cube ignore, final ForeignCoverer foreignCoverer) { return covers(u, ignore, 0, this.size(), foreignCoverer); }
 public boolean covers(final Cube u, final Cube ignore, int from, int to, final ForeignCoverer foreignCoverer) {
  int i = 0;
  int intersectVar = -1;
  CUBEITERATOR: for (i = from; i < to; i++) {
   Cube a = this.get(i);
   if (a == ignore) continue; // this \ ignore
   a = u.and(a);
   boolean covered = true;
   for (int j = 0; j < u.width; j++) {
    int v = a.getVar(j);
    if (v == BinFunction.INV) continue CUBEITERATOR; // u has no intersection with cube[i]
    if (u.getVar(j) == BinFunction.DC && v != BinFunction.DC) { covered = false; intersectVar = j; } // u has partial intersection with cube[i]
   }
   if (covered) return true; // u is completely covered by cube[i]
  }
  if (intersectVar == -1) {
   // u has no intersects with existing cubes
   if (foreignCoverer == null) return false;
   else return foreignCoverer.isCovered(u);
  } else {
   // split into smaller cubes and try again...
   Cube c1 = u.clone();
   Cube c2 = u.clone();
   c1.setVar(intersectVar, BinFunction.ONE);
   c2.setVar(intersectVar, BinFunction.ZERO);
   return covers(c1, ignore, from, to, foreignCoverer) && covers(c2, ignore, from, to, foreignCoverer);
  }
 }
 
 @Override
 public Set clone () {
  Set r;
  try {
   r = (Set)this.getClass().getConstructor(int.class).newInstance(width);
  } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
   return null;
  }
  for (int i = 0; i < size(); i++) r.add(this.get(i).clone());
  return r;
 }
 
 @Override
 public String toString () { return toString(null, null, 0, this.size()); }
 public String toString (String[] names) { return toString(names, null, 0, this.size()); }
 public String toString (String[] names, Cube ignore, int from, int to) {
  String s = "";
  boolean inv = false;
  boolean one = false;
  String v;
  for (int i = from; i < to; i++) {
   if (this.get(i) == ignore) continue; 
   if (!s.equals("")) s += " + ";
   int dcCnt = 0;
   for (int j = 0; j < this.width; j++) {
    if (names != null && names[j] != null) v = names[j];
    else v = ""+j;
    switch (((Cube)this.get(i)).getVar(j)) {
     case BinFunction.INV :
      s += v+"!";
      inv = true;
      break;
     case BinFunction.ONE :
      s += v+" ";
      break;
     case BinFunction.ZERO :
      s += v+"'";
      break;
     case BinFunction.DC :
      dcCnt++;
      break;
    }
    if (dcCnt == this.width) one = true;
   }
  }
  if (inv) s = "INVALID  ("+s+")";
  else if (one) s = "ONE  ("+s+")";
  else if (s.length() == 0) s = "ZERO";
  if (names != null && names[this.width] != null) s = names[this.width] + " = " + s;
  return s;
 }
 
 
 
 
 /**
  * Functionpointer: Override isCovered to run a covering-check over more than one set.
  * @author Mitja Stachowiak
  */
 public static abstract class ForeignCoverer {
  protected abstract boolean isCovered(Cube c);
 }
}
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
 
 /**
  * Checks, weather the given cube u is completely covered by this set.
  * If u is element of this, the function checks, weather u is covered by this without u
  * @param u
  * @return
  * true, if u is covered
  */
 public boolean covers(final Cube u) { return covers(u, u, null); }
 public boolean covers(final Cube u, final Cube ignore, final ForeignCoverer foreignCoverer) {
  int i = 0;
  for (i = 0; i < this.size(); i++) {
   Cube a = this.get(i);
   if (a == ignore) continue; // this without u
   a = u.and(a);
   if (!a.isValid()) continue; // u has no intersection with cube[i]
   if (a.equals(u)) return true; // u is completely covered by the existing cube[i]
   for (int j = 0; j < u.width; j++) if (u.getVar(j) == BinFunction.DC && a.getVar(j) != BinFunction.DC) {
    // split into smaller cubes and try again...
    Cube c1 = u.clone();
    Cube c2 = u.clone();
    c1.setVar(j, BinFunction.ONE);
    c2.setVar(j, BinFunction.ZERO);
    return covers(c1, ignore, foreignCoverer) && covers(c2, ignore, foreignCoverer);
   }
  }
  // u has no intersects with existing cubes
  if (foreignCoverer == null) return false;
  else return foreignCoverer.isCovered(u);
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
 
 
 
 
 /**
  * Functionpointer: Override isCovered to run a covering-check over more than one set.
  * @author Mitja Stachowiak
  */
 public static abstract class ForeignCoverer {
  protected abstract boolean isCovered(Cube c);
 }
}
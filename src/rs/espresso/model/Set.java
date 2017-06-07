package rs.espresso.model;

import java.util.ArrayList;

public class Set extends ArrayList<Cube> {
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
   for (int j = 0; j < u.width; j++) if (u.getVar(j) == BinFunction.DC && a.getVar(j) != BinFunction.DC) {
    // split into smaller cubes and try again...
    Cube c1 = u.clone();
    Cube c2 = u.clone();
    c1.setVar(j, BinFunction.ONE);
    c2.setVar(j, BinFunction.ZERO);
    return covers(c1, ignore) && covers(c2, ignore);
   }
  }
  // u has no intersects with existing cubes
  return false;
 }
}
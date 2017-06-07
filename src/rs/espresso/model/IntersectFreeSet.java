package rs.espresso.model;

public class IntersectFreeSet extends Set {
 private static final long serialVersionUID = 8905066173022612097L;

 public IntersectFreeSet(int width) { super(width); }
 
 /**
  * Adds cube c intersect-free:
  *  - Adds c, if c has no intersects with the existing cubes in the Set.
  *  - Don't adds c, if c is completely covered by one existing cube.
  *  - Splits up c into smaller cubes and tries to add them, if c has intersections with the existing cubes.
  * The list is kept sorted by the cardinality of the cubes.
  * @return
  * Returns true, if something was added
  */
 @Override
 public boolean add(final Cube c) {
  for (int i = 0; i < this.size(); i++) {
   Cube a = c.and(this.get(i));
   if (!a.isValid()) continue; // c has no intersection with cube[i]
   if (a.equals(c)) return false; // c is completely covered by one existing cube[i]
   for (int j = 0; j < c.width; j++) if (c.getVar(j) == BinFunction.DC && a.getVar(j) != BinFunction.DC) {
    // split into smaller cubes and try again...
    Cube c1 = c.clone();
    Cube c2 = c.clone();
    c1.setVar(j, BinFunction.ONE);
    c2.setVar(j, BinFunction.ZERO);
    return add(c1) | add(c2);
   }
  }
  // c has no intersects with existing cubes
  c.keepFixed();
  /*int l = 0; // keep sorted
  int u = this.size();
  if (u > 0) do {
   int m = (l+u)/2;
   if (c.cardinality2() >= this.get(m).cardinality2()) l = m+1;
   else u = m;
  } while (l < u);
  super.add(u, c);*/
  super.add(c);
  return true;
 }
 
 /**
  * Faster Check for Coverage than with super.covers()
  */
 @Override
 public boolean covers(final Cube u) {
  boolean[] cover_cardinality = new boolean[u.width+1]; // init with false
  for (int i = 0; i < this.size(); i++) {
   Cube a = this.get(i);
   if (a == u) continue;
   a = u.and(a);
   int ca = a.cardinality2();
   if (ca == -1) continue; // no intersection between this[i] and u
   while (ca < cover_cardinality.length) if (cover_cardinality[ca]) {
    cover_cardinality[ca] = false;
    ca++;
   } else {
    cover_cardinality[ca] = true;
    break;
   }
  }
  return cover_cardinality[u.cardinality2()];
 }
}
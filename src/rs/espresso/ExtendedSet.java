package rs.espresso;

import rs.binfunction.Set;

public class ExtendedSet extends Set {
 private static final long serialVersionUID = 6851988378582470592L;
 public ExtendedSet(int width) { super(width); }
 public boolean add(ExtCube c) { return super.add(c); }
 public void add(int i, ExtCube c) { super.add(i, c); }
 public void set(int i, ExtCube c) { super.set(i, c); }
 public ExtCube get(int i) { return (ExtCube)super.get(i); }
 public ExtendedSet clone () { return (ExtendedSet)super.clone(); }
}

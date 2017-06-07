package rs.espresso;

import rs.espresso.model.Cube;

public class ExtCube extends Cube {
 //private static final long[] bigmask = new long[32], smallmask = new long[64];
 /*static {
 for (int i = 0; i < 32; i++) {
  bigmask[i] = 3l << (62 - 2 * i);
  smallmask[i] = 1l << (63 - i);
  smallmask[i + 32] = 1l << (31 - i);
 }
}*/
 
 int coverCnt = 0; // stores, how many other cubes of Rp this cube covers
 int processDepth = 0;
 boolean[] nonExpandableLiterals;

 public ExtCube(int width) {
  super(width);
  nonExpandableLiterals = new boolean[width]; // initialized with 0
 }
 
 /**
  * Compares this Cube with an other ExtCube regarding coverCnt and cardinality.
  */
 public int compareCoverageTo(Object foreign) {
  int r = this.coverCnt - ((ExtCube)foreign).coverCnt;
  if (r == 0) r = this.cardinality2() - ((ExtCube)foreign).cardinality2();
  return r;
 }
 
 @Override
 public Cube clone (Class<?> cubeType) {
  if (cubeType != ExtCube.class) return super.clone(cubeType);
  ExtCube r = (ExtCube)super.clone(cubeType);
  for (int i = 0; i < cube.length; i++) r.cube[i] = this.cube[i];
  r.coverCnt = this.coverCnt;
  r.processDepth = this.processDepth;
  r.nonExpandableLiterals = this.nonExpandableLiterals.clone();
  return r;
 }
 
 /**
  * Expands the cube at the given position.
  * 
  * @return A new cube object cloned from this but with the expansion.
  */
 /*public ExtCube expand (int pos) {
  int c = pos / 64, p = pos % 64;
  ExtCube ret = (ExtCube)this.clone(ExtCube.class);
  if (bits != null) ret.bits[pos] = true;
  ret.cube[c] |= smallmask[p];
  return ret;
 }*/

 /**
  * Gets a flag indicating whether this cube intersects with another one.
  * 
  * @param c
  *           The cube where the intersection should be checked.
  * @return True when the cubes intersect, false otherwise.
  */
 /*public boolean intersects(Cube c) {
  if (c.width != this.width) throw new IllegalArgumentException("Cubes must have the same width.");
  for (int i = 0; i < cube.length; i++) {
   long l = cube[i] & c.cube[i];
   int len = i == cube.length - 1 ? width : bigmask.length;
   for (int m = 0; m < len; m++) {
    if ((l & bigmask[m]) == 0) return false;
   }
  }
  return true;
 }*/

 /**
  * Gets the bits of the cube as boolean array.
  */
 /*public boolean[] getBits() {
  if (bits == null) {
   bits = new boolean[width * 2];
   int p = -1, c = 0;
   for (int i = 0; i < bits.length; i++) {
    if (p >= 64) {
     c++;
     p = 0;
    } else p++;
    bits[i] = (cube[c] & smallmask[p]) != 0;
   }
  }
  return bits;
 }*/

 /**
  * Gets a flag indicating whether this cube is covered.
  */
 /*public boolean isCovered() {
  return covered;
 }*/

 /**
  * Sets a flag indicating whether this cube is covered by another one.
  * 
  * @param covered
  *           True when the cube is covered by an implicant.
  */
 /*public void setCovered(boolean covered) {
  this.covered = covered;
 }*/

 /**
  * Gets a flag indicating whether this cube is a cover.
  */
 /*public boolean isCover() {
  return isCover;
 }*/

 /**
  * Sets a flag inndicating whether this cube covers another one.
  * 
  * @param isCover
  *           True when this cube is a cover.
  */
 /*public void setIsCover(boolean isCover) {
  this.isCover = isCover;
 }*/
 
 /**
  * Gets a value indicating whether the bit at a specific position is set or not.
  * 
  * @param position
  *           The zero-based position of the requested bit. The max position is pos = 2*width-1,
  *           whereas width is the number of bits in the cube.
  */
 /*public boolean bitAt(int position) {
  return getBits()[position];
 }*/

 
}
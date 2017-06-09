package rs.espresso;

import rs.binfunction.Cube;

public class ExtCube extends Cube {
 int coverCnt = 0; // stores, how many other cubes of Rp this cube covers
 int processDepth = 0;
 boolean[] nonExpandableLiterals;
 boolean expanded = false; // stores, weather this cube has been expanded in current expansion-procedure
 boolean isEssential = false;

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
  r.expanded = this.expanded;
  r.isEssential = this.isEssential;
  return r;
 }
 
 public int scalarProduct(int[] v) {
  int r = 0;
  for (int i = 0; i < this.width; i++) {
   int l = this.getVar(i);
   if ((l & 1) != 0) r += v[i*2];
   if ((l & 2) != 0) r += v[i*2+1];
  }
  return r;
 }
}
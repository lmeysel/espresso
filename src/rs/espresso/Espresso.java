package rs.espresso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

import rs.binfunction.BinFunction;
import rs.binfunction.Cube;
import rs.binfunction.IntersectFreeSet;
import rs.binfunction.Set;

/**
 * The Espresso class manages the execution of the espresso-algorithm.
 * @author Ludwig Meysel, Mitja Stachowiak
 * @version 9.6.2017
 */
public class Espresso {
 private Set onDc = null; // TODO: Check, how much faster the cover-check with the IntersectFreeSet is, compared to the Presto-Approach
 private int onCnt;
 private final Logger log = Logger.getLogger("espresso");;
 private ExtendedSet Rp = null;
 private BinFunction fkt;
 
 public boolean searchForBestExpansion = true; // if false, use fastExpansion instead
 public boolean markEssentials = false; // if true, cubes, that cover implicants that are not covered by any other cube, are marked as essentials and become neither reduces or expanded during the rest of the algorithm
 public boolean randomizedReduction = false; // if true, not the largest cube is reduced first, but the order is random. ATTENTION: No deterministic result!
 public int expansionSearchLimit = 65535; // When searching for the best expansion, paths, that are not in the best [expansionSearchLimit]-part of the list, are not longer regarded.
 public boolean useIntersectFreeSet = true; // Create the onDc as intersect-free-set
 public boolean logAllActions = false;
 
 public Espresso() { }
    
 
 /**
  * Runs the espresso algorithm.
  * @param fkt
  * the function to be minimized
  * @return
  * The minimized function
  */
 public BinFunction run (final BinFunction fkt) {
  log.info("Load function into Espresso...");
  this.fkt = fkt;
  if (useIntersectFreeSet) onDc = new IntersectFreeSet(fkt.numInputs());
  else onDc = new Set(fkt.numInputs());
  Rp = new ExtendedSet(fkt.numInputs());
  for (int i = 0; i < fkt.on().size(); i++) {
   onDc.add(fkt.on().get(i).clone());
   Rp.add(fkt.on().get(i).clone(ExtCube.class));
  }
  onCnt = onDc.size();
  for (int i = 0; i < fkt.dc().size(); i++) onDc.add(fkt.dc().get(i).clone());
  
  log.info("Expand Rp...");
  expand();
  
  if (markEssentials) {
   log.info("Mark essentials...");
   for (int i = 0; i < Rp.size(); i++) if (!Rp.covers(Rp.get(i))) Rp.get(i).isEssential = true;
  }
  
  ExtendedSet Rp_;
  do {
   Rp_ = Rp.clone();
   
   log.info("Reduce...");
   reduce();
  
   log.info("Expand Rp...");
   expand();
   
  } while (costAisLower(Rp, Rp_));
  Rp = Rp_;
  
  log.info("Write result function...");
  BinFunction minimized = new BinFunction(fkt.numInputs());
  for (int i = 0; i < Rp.size(); i++) minimized.on().add(Rp.get(i).clone(Cube.class));
  for (int i = 0; i < fkt.names().length; i++) minimized.names()[i] = fkt.names()[i];

  // free memory
  onDc.clear();
  onDc = null;
  Rp.clear();
  Rp = null;
  this.fkt = null;
  return minimized;
 }
 
 
 /**
  * goes through all cubes of Rp (Larger cubes first) and tries to reduce them.
  * Deletes cubes, that are reduced to invalid.
  * Leaves cubes, that are marked essential unchanged.
  */
 private void reduce() {
  // sort: Reduce large cubes first
  if (randomizedReduction) Collections.shuffle(Rp);
  else Collections.sort((List<Cube>)Rp, new Comparator<Cube>() {
   @Override
   public int compare(Cube c1, Cube c2) {
    return c2.cardinality2()-c1.cardinality2();
   }
  });
  // start reduction
  for (int i = 0; i < Rp.size(); i++) {
   ExtCube c = Rp.get(i);
   if (c.isEssential) continue;
   for (int j = 0; j < Rp.width(); j++) {
    if (c.getVar(j) != BinFunction.DC) continue;
    // check if the one-half can be reduced
    c.andVar(j,  BinFunction.ONE);
    boolean oneUnneccessary = Rp.covers(c, c, new Set.ForeignCoverer() {
     @Override public boolean isCovered (Cube c) {
      return onDc.covers(c, onCnt, onDc.size());
     }
    });
    // check if the zero-half can be reduced
    c.setVar(j, BinFunction.ZERO);
    boolean zeroUnneccessary = Rp.covers(c, c, new Set.ForeignCoverer() {
     @Override public boolean isCovered (Cube c) {
      return onDc.covers(c, onCnt, onDc.size());
     }
    });
    // apply/keep possible reductions
    if (oneUnneccessary) {
     if (zeroUnneccessary) c.invalidate(); // both sides can be reduced; cube reduces to INV
    } else {
     if (zeroUnneccessary) c.setVar(j, BinFunction.ONE);
     else c.orVar(j, BinFunction.DC);
    }
   }
  }
  // remove covered/invalid cubes from Rp
  Rp.removeIf((Cube c) -> !c.isValid());
  if (logAllActions) {
   BinFunction fkt = new BinFunction(Rp.width());
   fkt.on().addAll(Rp);
   log.info("Reduced: "+fkt.toString()+"     valid: "+fkt.isEquivalent(this.fkt));
  }
 }

 
 /**
  * Calcs the sum-vector and the scalar product of each cube with this vector. Tries to expand the
  * cube with the lowest product (bestExpansion or fastExpansion). Invalidates all cubes, that are fully covered by this cube.
  * Repeats this procedure until all cubes are extended. Removes invalidated cubes from Rp.
  */
 private void expand () {
  for (int i = 0; i < Rp.size(); i++) if (!((ExtCube)Rp.get(i)).isEssential) ((ExtCube)Rp.get(i)).expanded = false;
  do {
   // find next cube to expand
   // TODO: Don't re-calc sum vector, if previous expansion was unsuccessfull
   int[] v = calcSumVector();
   int foundS = Integer.MAX_VALUE;
   int foundN = -1;
   for (int i = 0; i < Rp.size(); i++) {
    ExtCube c = Rp.get(i);
    if (!c.isValid() || c.expanded) continue;
    int s = c.scalarProduct(v);
    if (s < foundS) {
     foundS = s;
     foundN = i;
    }
   }
   if (foundN == -1) break;
   // expand this cube
   ExtCube c;
   if (searchForBestExpansion) c = bestExpansion(Rp.get(foundN));
   else c = fastExpansion(Rp.get(foundN));
   c.expanded = true;
   Rp.set(foundN, c);
   // invalidate cubes, covered by the expanded one
   for (int j = 0; j < Rp.size(); j++) {
    ExtCube co = Rp.get(j);
    if (!co.expanded && co.and(c).equals(co)) co.invalidate();
   }
  } while (true);
  // remove covered/invalid cubes from Rp
  Rp.removeIf((Cube c) -> !c.isValid());
  if (logAllActions) {
   BinFunction fkt = new BinFunction(Rp.width());
   fkt.on().addAll(Rp);
   log.info("Expanded: "+fkt.toString()+"     valid: "+fkt.isEquivalent(this.fkt));
  }
 }
 
 /**
  * @return
  * The sum-vector of Rp
  */
 private int[] calcSumVector () {
  int[] v = new int[Rp.width()*2]; // initialized with 0
  for (int i = 0; i < Rp.size(); i++) {
   ExtCube c = Rp.get(i);
   if (!c.isValid() || c.expanded) continue;
   for (int j = 0; j < Rp.width(); j++) {
    int l = c.getVar(j);
    if ((l & 1) != 0) v[j*2]++;
    if ((l & 2) != 0) v[j*2+1]++;
   }
  }
  return v;
 }
 
 /**
  * Searches all possible expansions (tries all different orders, requires OnDc to be initialized)
  * and takes this expansion, that covers the most other cubes in Rp.
  * @param c
  * @return
  */
 private ExtCube bestExpansion(ExtCube c) {
  List<ExtCube> expOrder = new ArrayList<ExtCube>();
  expOrder.add(c);
  expOrder.get(0).processDepth = 0;
  expOrder.get(0).coverCnt = 1;
  expOrder.get(0).nonExpandableLiterals = new boolean[Rp.width()];
  boolean f;
  int processDepth = 0;
  do f = furtherExpand(expOrder, processDepth++); while (f);
  ExtCube r = expOrder.get(expOrder.size()-1);
  expOrder.clear();
  return r;
 }
 
 /**
  * furtherExpand processes the given expOrder-List.
  * @param expOrder
  * An array list, which is kept sorted
  * @param processDepth
  * furtherExpand only expands there cubes of expOrder, which have the given processDepth.
  * The expanded results will have processDepth+1 and get added (sorted) to expOrder.
  * @return
  * returns true, if any new expansion was added to expOrder
  */
 private boolean furtherExpand(List<ExtCube> expOrder, int processDepth) {
  boolean found = false;
  int ci = -1;
  while (++ci < expOrder.size()) {
   ExtCube c = expOrder.get(ci);
   if (c.processDepth != processDepth) continue;
   LITERALITERATOR: for (int i = 0; i < c.width; i++) if (!c.nonExpandableLiterals[i]) {
    int oldV = c.getVar(i);
    if (oldV == BinFunction.DC) continue; // this literal is already don't care, cannot be expanded
    c.orVar(i, BinFunction.DC); // expand c for expand-test before clone
    if (onDc.covers(c)) {
     ExtCube e = (ExtCube)c.clone();
     c.andVar(i,  oldV); // leave c unchanged!
     // compute coverCnt of the expanded cube
     e.coverCnt = 0;
     for (int j = 0; j < Rp.size(); j++) {
      Cube cmp = Rp.get(j);
      if (!cmp.isValid()) continue; // while expanding, there can be invalid cubes in the list
      Cube a = e.and(cmp);
      if (a.equals(cmp)) e.coverCnt++; // this expansion of n covers +1 cube (if c is part of the on-set, this operation occurs at least one time!)
     }
     // search for position to insert expanded cube
     int l = 0;
     int u = expOrder.size();
     if (u > 0) do {
      int m = (l+u)/2;
      if (e.compareCoverageTo(expOrder.get(m)) >= 0) l = m+1;
      else u = m;
     } while (l < u);
     for (l = u-1; l >= 0; l--) if (e.compareCoverageTo(expOrder.get(l)) != 0) break;
     else if (e.equals(expOrder.get(l))) continue LITERALITERATOR; // e already exists in expOrder and has been regarded!
     if (expOrder.size() - u < expansionSearchLimit) { // don't follow implausible expansion paths
      // add the new, expanded cube to expList
      e.processDepth = processDepth+1;
      expOrder.add(u, e);
      if (u <= ci) ci++;
      // search for further expansions
      found = true;
     }
    } else c.nonExpandableLiterals[i] = true; // this literal would intersect with the off-set and cannot be expanded in any further expansion of this cube
    c.andVar(i, oldV); // leave c unchanged!
   }
  }
  return found;
 }
  
 /**
  * Expands a cube by testing for valid expansion for each literal after each other
  * @param c
  * @return
  */
 private ExtCube fastExpansion (ExtCube c) {
  ExtCube r = (ExtCube)c.clone();
  for (int i = 0; i < r.width; i++) {
   int oldV = r.getVar(i);
   if (oldV == BinFunction.DC) continue;
   r.orVar(i, BinFunction.DC);
   if (!onDc.covers(r)) r.andVar(i, oldV);
  }
  return r;
 }
 
 
 /**
  * @param a
  * @param b
  * @return
  * returns true, if a has less cubes than b or if a and b have the same number of cubes,
  * but a has less literals than b.
  */
 private boolean costAisLower (ExtendedSet a, ExtendedSet b) {
  if (a.size() < b.size()) return true;
  if (a.size() > b.size()) return false;
  int na = 0;
  for (int i = 0; i < a.size(); i++) na += a.width() - a.get(i).cardinality2();
  int nb = 0;
  for (int i = 0; i < b.size(); i++) nb += b.width() - b.get(i).cardinality2();
  return na < nb;
 }
}
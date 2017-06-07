package rs.espresso;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import rs.espresso.model.BinFunction;
import rs.espresso.model.Cube;
import rs.espresso.model.IntersectFreeSet;
import rs.espresso.model.Set;

/**
 * 
 * @author Ludwig Meysel
 * 
 * @version 19.05.2017
 */
public class Espresso {
	private static final Logger log = Logger.getLogger("espresso");
	private Set onDc = null; // TODO: Try weather the covering-check is faster with an intersect-free-set or with the Presto-Approach
	//private BinFunction fkt = null;
	private Set Er = null;
	private Set Rp = null;

	/**
	 * Creates a new object from the {@see Espresso} class.
	 */
	public Espresso() {
	}

	/**
	 * Runs the espresso algorithm.
	 */
	public BinFunction run(final BinFunction fkt) {

		log.info("Load function into Espresso...");
		//this.fkt = fkt;
		onDc = new IntersectFreeSet(fkt.numInputs());
		Er = new Set(fkt.numInputs());
		Rp = new Set(fkt.numInputs());
		for (int i = 0; i < fkt.on().size(); i++) {
			onDc.add(fkt.on().get(i).clone());
			Rp.add(fkt.on().get(i).clone());
		}
		for (int i = 0; i < fkt.dc().size(); i++)
			onDc.add(fkt.dc().get(i).clone());

		log.info("Expand Rp...");
		for (int i = 0; i < Rp.size(); i++)
			Rp.set(i, bestExpansion(Rp.get(i)));

		log.info("Remove equal cubes and copy essential cubes to Er...");
		for (int i = 0; i < Rp.size(); i++) {
			Cube c = Rp.get(i);
			for (int j = Rp.size() - 1; j > i; j--)
				if (c.equals(Rp.get(j)))
					Rp.remove(j);
			if (!Rp.covers(c))
				Er.add(c); // don't remove cube from Rp here!
		}

		log.info("Remove totally redundant cubes from Rp..."); // Essential cubes, that are in Rp and Er, are now totally redundant in Rp
		//for (int i = Rp.size()-1; i >= 0; i--) if (Er.covers(Rp.get(i))) Rp.remove(i);
		Rp.removeIf((Cube c) -> Er.covers(c));

		// hier! // Espresso - find best cover and add all required cubes to Er

		//     log.info("Expand...");
		//     ArrayList<ExtCube> cover = new ArrayList<ExtCube>();
		//     for (int i = 0; i < fkt.on().size(); i++) cover.add((ExtCube)fkt.on().get(i).clone(ExtCube.class));
		//     int counter = -1;
		//     while (++counter < cover.size()) {
		//      weightAndSort(cover);
		//      ExtCube current = maximumExpansion(cover.get(0));
		//      if (current.isCovered()) {
		//       log.warning("It should not be possible to find a covered cube in the list at this point.");
		//       continue;
		//      }
		//
		//      cover.removeIf((ExtCube c) -> {
		//       if (!c.isCover() && current.intersects(c)) {
		//        c.setCovered(true);
		//        return true;
		//       }
		//       return false;
		//      });
		//      current.setIsCover(true);
		//      cover.add(current);
		//     }

		log.info("Reduce...");

		log.info("Write result function function...");
		BinFunction minimized = new BinFunction(fkt.numInputs());
		minimized.on().addAll(Er);
		for (int i = 0; i < fkt.names().length; i++)
			minimized.names()[i] = fkt.names()[i];

		// free memory
		onDc.clear();
		onDc = null;
		Er.clear();
		Er = null;
		Rp.clear();
		Rp = null;
		//this.fkt = null;
		return minimized;
	}

	private void weightAndSort(ArrayList<ExtCube> set) {
		log.info("Weight and sort for expansion heuristic");
		int width = set.get(0).width * 2;
		boolean[][] matrix = new boolean[set.size()][width];
		int[] soc = new int[width]; // sum of columns
		for (int i = 0; i < set.size(); i++) {
			ExtCube c = set.get(i);
			matrix[i] = c.getBits();
			for (int j = 0; j < width; j++) {
				if (matrix[i][j]) {
					soc[j]++;
				}
			}
		}

		for (int i = 0; i < set.size(); i++) {
			ExtCube c = set.get(i);
			int w = 0;
			for (int j = 0; j < width; j++) {
				if (matrix[i][j])
					w += soc[j];
			}
			c.weight = w;
		}
		set.sort((c1, c2) -> c1.weight - c2.weight);
		System.out.println();
	}

	private int[] calcSumVector() {
		int[] v = new int[Rp.width() * 2]; // initialized with 0
		for (int i = 0; i < Rp.size(); i++) {
			Cube c = Rp.get(i);
			for (int j = 0; j < Rp.width(); j++) {
				int l = c.getVar(j);
				if ((l & 1) != 0)
					v[j * 2]++;
				if ((l & 2) != 0)
					v[j * 2 + 1]++;
			}
		}
		return v;
	}

	/**
	 * Searches all possible expansions (tries all different orders, requires OnDc to be initialized)
	 * and takes this expansion, that covers the most other cubes in Rp.
	 * 
	 * @param c
	 * @return
	 */
	private Cube bestExpansion(Cube c) {
		List<ExtCube> expOrder = new ArrayList<ExtCube>();
		expOrder.add((ExtCube)c.clone(ExtCube.class));
		furtherExpand(expOrder, expOrder.get(0), new boolean[c.width]);
		Cube r = expOrder.get(expOrder.size() - 1).clone(Cube.class);
		expOrder.clear();
		return r;
	}
	private void furtherExpand(List<ExtCube> expOrder, ExtCube c, boolean[] nonExpandableLiterals) {
		LITERALITERATOR: for (int i = 0; i < c.width; i++)
			if (!nonExpandableLiterals[i]) {
				int oldV = c.getVar(i);
				if (oldV == BinFunction.DC)
					continue; // this literal is already don't care, cannot be expanded
				c.orVar(i, BinFunction.DC); // expand c for expand-test before clone
				if (onDc.covers(c)) {
					ExtCube e = (ExtCube)c.clone();
					c.andVar(i, oldV); // leave c unchanged!
					// compute coverCnt of the expanded cube
					e.coverCnt = 0;
					for (int j = 0; j < Rp.size(); j++) {
						Cube a = e.and(onDc.get(j));
						if (a.equals(onDc.get(j)))
							e.coverCnt++; // this expansion of n covers +1 cube (if c is part of the on-set, this operation occurs at least one time!)
					}
					// search for position to insert expanded cube
					int l = 0;
					int u = expOrder.size();
					do {
						int m = (l + u) / 2;
						if (e.compareTo(expOrder.get(m)) >= 0)
							l = m + 1;
						else
							u = m;
					} while (l < u);
					for (l = u - 1; l >= 0; l--)
						if (e.compareTo(expOrder.get(l)) != 0)
							break;
						else if (e.equals(expOrder.get(l)))
							continue LITERALITERATOR; // e already exists in expOrder and has been regarded!
					if (expOrder.size() - u < 65535) { // don't follow implausible expansion paths
						// add the new, expanded cube to expList
						expOrder.add(u, e);
						// search for further expansions
						furtherExpand(expOrder, e, nonExpandableLiterals);
					}
				} else { // this literal would intersect with the off-set and cannot be expanded
					nonExpandableLiterals[i] = true;
					c.andVar(i, oldV); // leave c unchanged!
				}
			}
	}

	/**
	 * Expands a cube by testing for valid expansion for each literal after each other
	 * 
	 * @param c
	 * @return
	 */
	private Cube fastExpansion(Cube c) {
		Cube r = c.clone();
		for (int i = 0; i < r.width; i++) {
			int oldV = r.getVar(i);
			if (oldV == BinFunction.DC)
				continue;
			r.orVar(i, BinFunction.DC);
			if (!onDc.covers(r))
				r.andVar(i, oldV);
		}
		return r;
	}

	/*
	 * private ExtCube maximumExpansion(ExtCube cube) { boolean[] b = cube.getBits(); for (int i = 0;
	 * i < b.length; i++) if (!b[i]) { ExtCube tmp = cube.expand(i); Iterator<Cube> it2 =
	 * onDc.iterator(); while (it2.hasNext()) { Cube nxt = it2.next(); if (tmp.intersects(nxt)) { tmp
	 * = null; break; } } if (tmp != null) { cube = tmp; } } return cube; }
	 */

	public static class ExtCube extends Cube implements Comparable {
		private static final long[] bigmask = new long[32],
				smallmask = new long[64];

		@Override
		public int compareTo(Object foreign) {
			int r = this.coverCnt - ((ExtCube)foreign).coverCnt;
			if (r == 0)
				r = this.cardinality2() - ((ExtCube)foreign).cardinality2();
			return r;
		}

		static {
			for (int i = 0; i < 32; i++) {
				bigmask[i] = 3l << (62 - 2 * i);
				smallmask[i] = 1l << (63 - i);
				smallmask[i + 32] = 1l << (31 - i);
			}
		}

		boolean covered = false;
		boolean isCover = false;
		int weight = -1;
		boolean[] bits = null;
		public int coverCnt = 0;

		public ExtCube(int width) {
			super(width);
		}

		@Override
		public Cube clone(Class<?> cubeType) {
			if (cubeType != ExtCube.class)
				return super.clone(cubeType);
			ExtCube r = (ExtCube)super.clone(cubeType);
			for (int i = 0; i < cube.length; i++)
				r.cube[i] = this.cube[i];
			r.covered = covered;
			r.isCover = isCover;
			r.weight = weight;
			if (bits != null)
				r.bits = bits.clone();
			r.coverCnt = this.coverCnt;
			return r;
		}

		/**
		 * Expands the cube at the given position.
		 * 
		 * @return A new cube object cloned from this but with the expansion.
		 */
		public ExtCube expand(int pos) {
			int c = pos / 64, p = pos % 64;
			ExtCube ret = (ExtCube)this.clone(ExtCube.class);
			if (bits != null)
				ret.bits[pos] = true;
			ret.cube[c] |= smallmask[p];
			return ret;
		}

		/**
		 * Gets a flag indicating whether this cube intersects with another one.
		 * 
		 * @param c
		 *           The cube where the intersection should be checked.
		 * @return True when the cubes intersect, false otherwise.
		 */
		public boolean intersects(Cube c) {
			if (c.width != this.width)
				throw new IllegalArgumentException("Cubes must have the same width.");
			for (int i = 0; i < cube.length; i++) {
				long l = cube[i] & c.cube[i];
				int len = i == cube.length - 1 ? width : bigmask.length;
				for (int m = 0; m < len; m++) {
					if ((l & bigmask[m]) == 0)
						return false;
				}
			}
			return true;
		}

		/**
		 * Gets the bits of the cube as boolean array.
		 */
		public boolean[] getBits() {
			if (bits == null) {
				bits = new boolean[width * 2];
				int p = -1, c = 0;
				for (int i = 0; i < bits.length; i++) {
					if (p >= 64) {
						c++;
						p = 0;
					} else
						p++;
					bits[i] = (cube[c] & smallmask[p]) != 0;
				}
			}
			return bits;
		}

		/**
		 * Gets a flag indicating whether this cube is covered.
		 */
		public boolean isCovered() {
			return covered;
		}

		/**
		 * Sets a flag indicating whether this cube is covered by another one.
		 * 
		 * @param covered
		 *           True when the cube is covered by an implicant.
		 */
		public void setCovered(boolean covered) {
			this.covered = covered;
		}

		/**
		 * Gets a flag indicating whether this cube is a cover.
		 */
		public boolean isCover() {
			return isCover;
		}

		/**
		 * Sets a flag inndicating whether this cube covers another one.
		 * 
		 * @param isCover
		 *           True when this cube is a cover.
		 */
		public void setIsCover(boolean isCover) {
			this.isCover = isCover;
		}

		/**
		 * Gets a value indicating whether the bit at a specific position is set or not.
		 * 
		 * @param position
		 *           The zero-based position of the requested bit. The max position is pos =
		 *           2*width-1, whereas width is the number of bits in the cube.
		 */
		public boolean bitAt(int position) {
			return getBits()[position];
		}

	}
}

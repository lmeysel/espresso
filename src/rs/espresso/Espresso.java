package rs.espresso;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import blif.BinFunction;
import blif.BinFunction.Cube;

/**
 * 
 * @author Ludwig Meysel
 * 
 * @version 19.05.2017
 */
public class Espresso {
    private List<ExtCube> implicants = new ArrayList<ExtCube>();
    private List<Cube> off = null;
    private BinFunction fkt = null;
	private final Logger log;

	/**
	 * Creates a new object from the {@see Espresso} class.
	 */
	public Espresso(Logger log) {
	 this.log = log;
	}
	
    /**
     * Runs the espresso algorithm.
     */
    public BinFunction run (BinFunction fkt) {
     implicants.clear();
     this.fkt = fkt;
     
     log.info("Load function into Espresso (Compute offset)...");
     try {
      off = fkt.computeOff();
      System.out.println("Offset:     "+fkt.toString(off));
     } catch (Exception e) {
      log.severe("Cannot compute Offset: "+e.getMessage());
     }
     
     log.info("Expand...");
     ArrayList<ExtCube> cover = new ArrayList<ExtCube>();
     for (int i = 0; i < fkt.on().size(); i++) cover.add((ExtCube)fkt.on().get(i).clone(ExtCube.class));
     int counter = -1;
     while (++counter < cover.size()) {
      weightAndSort(cover);
      ExtCube current = maximumExpansion(cover.get(0));
      if (current.isCovered()) {
       log.warning("It should not be possible to find a covered cube in the list at this point.");
       continue;
      }

      cover.removeIf((ExtCube c) -> {
       if (!c.isCover() && current.intersects(c)) {
        c.setCovered(true);
        return true;
       }
       return false;
      });
      current.setIsCover(true);
      cover.add(current);
     }

     log.info("Reduce...");
     
     log.info("Create resulting function...");
     BinFunction r = new BinFunction(fkt.numInputs());
     for (int i = 0; i < implicants.size(); i++) r.on().add(implicants.get(i).clone(BinFunction.Cube.class));

     // free memory
     off.clear();
     off = null;
     implicants.clear();
     this.fkt = null;
     return r;
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

	private ExtCube maximumExpansion(ExtCube cube) {
	/* for (int i = 0; i < cube.width; i++) if (cube.getVar(i) == BinFunction.ZERO) {
	  cube.orVar(i, BinFunction.ONE);
	  boolean b = false;
	  for (int j = 0; j < fkt.on().size(); j++) if (fkt.on().get(j).andVar(n, v);)
	 }*/
		boolean[] b = cube.getBits();
		for (int i = 0; i < b.length; i++)
			if (!b[i]) {
			    ExtCube tmp = cube.expand(i);
				Iterator<Cube> it2 = off.iterator();
				while (it2.hasNext()) {
				    Cube nxt = it2.next();
					if (tmp.intersects(nxt)) {
						tmp = null;
						break;
					}
				}
				if (tmp != null) {
					cube = tmp;
				}
			}
		return cube;
	}
	
	
	
	
	
	
	
 public static class ExtCube extends BinFunction.Cube {
  private static final long[] bigmask = new long[32], smallmask = new long[64];

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

  public ExtCube(int width) {
   super(width);
  }
  
  public Cube clone (Class<?> cubeType) {
   ExtCube r = (ExtCube)super.clone(cubeType);
   for (int i = 0; i < cube.length; i++) r.cube[i] = this.cube[i];
   r.covered = covered;
   r.isCover = isCover;
   r.weight = weight;
   r.bits = bits.clone();
   return r;
  }
  
  /**
   * Expands the cube at the given position.
   * 
   * @return A new cube object cloned from this but with the expansion.
   */
  public ExtCube expand (int pos) {
   int c = pos / 64, p = pos % 64;
   ExtCube ret = (ExtCube)this.clone(ExtCube.class);
   if (bits != null) ret.bits[pos] = true;
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
   if (c.width != this.width) throw new IllegalArgumentException("Cubes must have the same width.");
   for (int i = 0; i < cube.length; i++) {
    long l = cube[i] & c.cube[i];
    int len = i == cube.length - 1 ? width : bigmask.length;
    for (int m = 0; m < len; m++) {
     if ((l & bigmask[m]) == 0) return false;
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
     } else p++;
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
   *           The zero-based position of the requested bit. The max position is pos = 2*width-1,
   *           whereas width is the number of bits in the cube.
   */
  public boolean bitAt(int position) {
   return getBits()[position];
  }

  
 }
}

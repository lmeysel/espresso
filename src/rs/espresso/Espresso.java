package rs.espresso;

import java.util.Iterator;

/**
 * 
 * @author Ludwig Meysel
 * 
 * @version 19.05.2017
 */
public class Espresso {
	private Set on, off, implicants;

	/**
	 * Creates a new object from the {@see Espresso} class.
	 */
	public Espresso() {
		on = new Set();
		off = new Set();
		implicants = new Set();
	}

	/**
	 * Gets the on-set.
	 */
	public Set onSet() {
		return on;
	}

	/**
	 * Gets the off-set
	 */
	public Set offSet() {
		return off;
	}

	/**
	 * Gets the implicants.
	 */
	public Set getImplicants() {
		return implicants;
	}

	private void weightAndSort(Set set) {
		Program.Log.info("Weight and sort for expansion heuristic");
		int width = set.get(0).getWidth() * 2;
		boolean[][] matrix = new boolean[set.size()][width];
		int[] soc = new int[width]; // sum of columns
		for (int i = 0; i < set.size(); i++) {
			Cube c = set.get(i);
			matrix[i] = c.getBits();
			for (int j = 0; j < width; j++) {
				if (matrix[i][j]) {
					soc[j]++;
				}
			}
		}

		for (int i = 0; i < set.size(); i++) {
			Cube c = set.get(i);
			int w = 0;
			for (int j = 0; j < width; j++) {
				if (matrix[i][j])
					w += soc[j];
			}
			c.setWeight(w);
		}
		set.sort((c1, c2) -> c1.getWeight() - c2.getWeight());
		System.out.println();
	}

	private Cube maximumExpansion(Cube cube) {
		boolean[] b = cube.getBits();
		for (int i = 0; i < b.length; i++)
			if (!b[i]) {
				Cube tmp = cube.expand(i);
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

	/**
	 * Runs the espresso algorithm.
	 */
	public void run() {

		Program.Log.info("Expand...");

		Set cover = new Set(on);
		int counter = -1;
		while (++counter < cover.size()) {
			weightAndSort(cover);
			Cube current = maximumExpansion(cover.get(0));
			if (current.isCovered()) {
				Program.Log.warning("It should not be possible to find a covered cube in the list at this point.");
				continue;
			}

			cover.removeIf((Cube c) -> {
				if (!c.isCover() && current.intersects(c)) {
					c.setCovered(true);
					return true;
				}
				return false;
			});
			current.setIsCover(true);
			cover.add(current);
		}

		Program.Log.info("Reduce...");

	}
}

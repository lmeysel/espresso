package rs.espresso;

/**
 * 
 * @author Ludwig Meysel
 * 
 * @version 18.05.2017
 */
public class Cube {
	public static final int ZERO = 1, ONE = 2, DC = 3, INV = 0;
	private static final long[] bigmask = new long[32], smallmask = new long[64];

	static {
		for (int i = 0; i < 32; i++) {
			bigmask[i] = 3l << (62 - 2 * i);
			smallmask[i] = 1l << (63 - i);
			smallmask[i + 32] = 1l << (31 - i);
		}
	}

	private int width, weight = -1;
	private long[] cube;
	private boolean valid = true, covered = false, isCover = false;
	private boolean[] bits = null;

	private Cube(Cube c) {
		this.width = c.width;
		cube = new long[c.cube.length];
		for (int i = 0; i < c.cube.length; i++) {
			cube[i] = c.cube[i];
		}
		if (c.bits != null) {
			bits = new boolean[c.bits.length];
			for (int i = 0; i < c.bits.length; i++)
				bits[i] = c.bits[i];
		}
	}

	/**
	 * Creates a new object from the {@see PositionalCubeNotation} class.
	 * 
	 * @param width
	 *           The width of the cube.
	 */
	public Cube(int width) {
		this.width = width;
		cube = new long[(int)Math.ceil((width) / 32.0)];
	}

	/**
	 * Creates a new object from the {@see PositionalCubeNotation} class.
	 * 
	 * @param seq
	 *           The sequence of inputs or outputs.
	 * @throws Exception
	 */
	public Cube(String seq) throws Exception {
		this(seq.length());

		int n = seq.length();
		char c;
		for (int i = 0; i < n; i++) {
			c = seq.charAt(i);
			int shift = 62 - ((2 * i) % 64);
			long bits = 0;

			switch (c) {
				case '0':
					bits = ZERO;
					break;
				case '1':
					bits = ONE;
					break;
				case '-':
					bits = DC;
					break;
				default:
					throw new Exception("Parser error: Invalid char '" + c + "'");
			}
			cube[i / 32] |= (bits << shift);
		}
		validate();
	}

	/**
	 * Checks whether the cube contains an invalid.
	 */
	private void validate() {
		int c = 0, m = -1;
		for (int i = 0; i < width; i++) {
			if (m >= 32) {
				m = 0;
				c++;
			} else
				m++;
			if ((cube[c] & bigmask[m]) == 0) {
				this.valid = false;
				return;
			}
		}
	}

	/**
	 * Or-combines with another cube.
	 * 
	 * @param foreign
	 *           The "other" cube.
	 * @return The or-combined result of this and another cube.
	 */
	public Cube or(Cube foreign) {
		if (this.width != foreign.width)
			throw new IllegalArgumentException("Cubes must be of same width.");

		Cube ret = new Cube(width);
		for (int i = 0; i < ret.cube.length; i++)
			ret.cube[i] = this.cube[i] | foreign.cube[i];

		return ret;
	}

	/**
	 * And-combines with another cube.
	 * 
	 * @param foreign
	 *           The "other" cube.
	 * @return The and-combined result of this and another cube.
	 */
	public Cube and(Cube foreign) {
		if (this.width != foreign.width)
			throw new IllegalArgumentException("Cubes must be of same width.");

		Cube ret = new Cube(width);
		for (int i = 0; i < ret.cube.length; i++)
			ret.cube[i] = this.cube[i] & foreign.cube[i];
		return ret;
	}

	/**
	 * Gets the value at the given position in the cube.
	 * 
	 * @param position
	 *           The zero-based position in the cube. The max position is pos = width-1, whereas
	 *           width is the number of bits in the cube.
	 * @return An integer value representing one of the constants ZERO, ONE, DC, INV.
	 */
	public int valueAt(int position) {
		int i = (int)(position / 32), c = position % 32;
		return (int)(cube[i] >>> (62 - c * 2)) & 3;
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

	/**
	 * Gets a flag indicating whether the cube is valid (i.e. contains no invalid-bit)
	 */
	public boolean isValid() {
		return valid;
	}

	/**
	 * Gets the weight of the algorithm.
	 * 
	 * @return The weight. -1 if no weight has been set explicitly.
	 */
	public int getWeight() {
		return weight;
	}

	/**
	 * Sets the weigth of this cube, calculated by the espresso-algorithm.
	 * 
	 * @param weight
	 */
	public void setWeight(int weight) {
		this.weight = weight;
	}

	/**
	 * Expands the cube at the given position.
	 * 
	 * @return A new cube object cloned from this but with the expansion.
	 */
	public Cube expand(int pos) {
		int c = pos / 64, p = pos % 64;
		Cube ret = new Cube(this);
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
	 * Gets the cube's width.
	 */
	public int getWidth() {
		return width;
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
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		String ret = "";
		for (int i = 0; i < width; i++) {
			int shift = 62 - ((2 * i) % 64);
			int val = (int)((cube[(int)(i / 32)] >>> shift) & 3);
			switch (val) {
				case ONE:
					ret += " 01";
					break;
				case ZERO:
					ret += " 10";
					break;
				case DC:
					ret += " 11";
					break;
				case INV:
					ret += " 00";
			}
		}
		return ret.substring(1);
	}

}

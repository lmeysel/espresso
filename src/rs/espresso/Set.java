package rs.espresso;

import java.util.Iterator;
import java.util.Vector;
import java.util.function.Predicate;

/**
 * 
 * @author Ludwig Meysel
 * 
 * @version 19.05.2017
 */
public class Set extends Vector<Cube> {
	private static final long serialVersionUID = 687150773968023371L;

	/**
	 * Creates a new object from the {@see Set} class.
	 */
	public Set() {
		super();
	}
	/**
	 * Copies a {@see Set} from the given one. This is no deep-copy, only a new set with the same
	 * objects will be created.
	 * 
	 * @param set
	 *           The set to copy from.
	 */
	public Set(Set set) {
		super(set);
	}

	/**
	 * Finds the first element matching a predicate.
	 */
	public synchronized Cube firstElement(Predicate<Cube> predicate) {
		Iterator<Cube> it = iterator();
		while (it.hasNext()) {
			Cube c = it.next();
			if (predicate.test(c))
				return c;
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized String toString() {
		StringBuilder ret = new StringBuilder();
		Iterator<Cube> it = iterator();
		while (it.hasNext()) {
			Cube c = it.next();
			ret.append(c.toString() + "\n");
		}
		return ret.toString();
	}
}

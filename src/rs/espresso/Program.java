package rs.espresso;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * 
 * @author Ludwig Meysel
 * 
 * @version 19.05.2017
 */
public class Program {
	public static final Logger Log = Logger.getLogger("espresso");

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		setUpLogger();

		Espresso esp = getTestData();
		esp.run();
	}

	/**
	 * Sets up the logger.
	 */
	private static void setUpLogger() {
		Log.setLevel(Level.ALL);
		ConsoleHandler h = new ConsoleHandler();
		Formatter fmt = new Formatter() {
			@Override
			public synchronized String format(LogRecord record) {
				return record.getLevel() + "  " + record.getMessage() + "\n";
			}
		};
		h.setFormatter(fmt);
		h.setLevel(Level.ALL);
		Log.addHandler(h);

		Log.finest("Logger set up.");
	}

	/**
	 * Gets the test data.
	 */
	private static Espresso getTestData() {
		Espresso e = new Espresso();
		try {
			BufferedReader rdr = new BufferedReader(new FileReader("testfile2"));
			Set current = null;
			String ln;
			while ((ln = rdr.readLine()) != null) {
				if (ln.isEmpty())
					continue;
				else if (ln.equals("#onset"))
					current = e.onSet();
				else if (ln.equals("#offset"))
					current = e.offSet();
				else if (ln.equals("#implicants"))
					current = e.getImplicants();
				else {
					current.add(new Cube(ln));
				}
			}
			rdr.close();
		} catch (Exception x) {
			x.printStackTrace();
		}
		Log.info("Testdata successful loaded.");
		return e;
	}
}

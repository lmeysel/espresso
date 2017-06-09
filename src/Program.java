

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import blif.*;
import rs.binfunction.BinFunction;
import rs.espresso.Espresso;

/**
 * 
 * @author Ludwig Meysel
 * @author Mitja Stachowiak
 * @version 19.05.2017
 */
public class Program {
	private static final Logger log = Logger.getLogger("espresso");

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		setUpLogger();
		Espresso esp = new Espresso();
		// specify operation modi
		esp.markEssentials = false;
		esp.searchForBestExpansion = true;
		esp.randomizedReduction = false;
		// read data
		Parser dat = new Parser();
		if (args.length > 0) dat.addFromFile(args[0]);
		// Minimize all functions
		Iterator<Entry<String, Model>> it = dat.models().entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, Model> pair = it.next();
			System.out.println(" ========== Model " + pair.getKey() + " ========== ");
			for (int i = 0; i < pair.getValue().functions().size(); i++) {
				BinFunction in = pair.getValue().functions().get(i);
				BinFunction out = esp.run(in);
				System.out.println("in:          " + in.toString());
				System.out.println("minimized:   " + out.toString());
				System.out.println("Result is valid? (Ludwig: Search for this println ;-)");
			}
		}
	}

	/**
	 * Sets up the logger.
	 */
	private static void setUpLogger() {
		log.setLevel(Level.ALL);
		ConsoleHandler h = new ConsoleHandler();
		Formatter fmt = new Formatter() {
			@Override
			public synchronized String format(LogRecord record) {
				return record.getLevel() + "  " + record.getMessage() + "\n";
			}
		};
		h.setFormatter(fmt);
		h.setLevel(Level.ALL);
		log.addHandler(h);

		log.finest("Logger set up.");
	}
}

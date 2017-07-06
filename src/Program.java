
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import rs.blif.*;
import rs.binfunction.BinFunction;
import rs.espresso.Espresso;

/**
 * 
 * @author Ludwig Meysel
 * @author Mitja Stachowiak
 * @version 9.6.2017
 */
public class Program {
	private static final Logger log = Logger.getLogger("espresso");

	/**
	 * @param args
	 */
	public static void main(String[] args) {
	    setUpLogger();
		Espresso esp = new Espresso();
		// read data
		BLIF dat = new BLIF();
		if (args.length == 0)
			return;
		if (args.length > 0)
			dat.addFromFile(args[0]);
		// Minimize all functions
		Iterator<Entry<String, Model>> it = dat.models().entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, Model> pair = it.next();
			log.info(" ========== Model " + pair.getKey() + " ========== ");
			for (int i = 0; i < pair.getValue().functions.size(); i++) {
				BinFunction in = pair.getValue().functions.get(i);
				BinFunction out = esp.run(in);
				pair.getValue().functions.set(i, out);
				log.info("in:          " + in.toString());
				log.info("complement:    " + in.computeOff().toString(in.names()));
				log.info("minimized:   " + out.toString());
				log.info("Result is valid: " + out.isEquivalent(in) + "   shrinked " + in.cost() + " literals --> " + out.cost() + " literals");
			}
		}
		// Save data
		String dst = null;
		if (args.length < 2) {
			log.warning("No save-directory specified, falling back to './output'.");
			dst = "./output/";
		} else
			dst = args[1];
		System.out.println("Save output to " + dst);
		dat.saveToFolder(dst);
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

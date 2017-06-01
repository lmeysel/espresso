

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import blif.*;
import rs.espresso.Espresso;

/**
 * 
 * @author Ludwig Meysel
 * @author Mitja Stachowiak
 * @version 19.05.2017
 */
public class Program {
	public static final Logger log = Logger.getLogger("espresso");

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		setUpLogger();
		Espresso esp = new Espresso(log);
		BLIF dat = new BLIF(log);
		dat.addFromFile(args[0]);
		// Minimize all functions
		Iterator<Entry<String, BLIF.Model>> it = dat.models().entrySet().iterator();
	    while (it.hasNext()) {
	     Map.Entry<String, BLIF.Model> pair = it.next();
	     System.out.println(" ========== Model "+pair.getKey()+" ========== ");
	     for (int i = 0; i < pair.getValue().functions().size(); i++) {
	      BinFunction in = pair.getValue().functions().get(i);
          System.out.println("in:          "+in.toString());
	      BinFunction out = esp.run(in);
          System.out.println("minimized:   "+out.toString());
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

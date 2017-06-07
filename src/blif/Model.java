package blif;

import java.util.ArrayList;
import java.util.List;

import rs.espresso.model.BinFunction;

public class Model {
	private Parser parent;
	public Parser parent() {
		return this.parent;
	}
	private String name;
	public String name() {
		return this.name;
	}
	public List<BinFunction> functions = new ArrayList<BinFunction>();
	public List<BinFunction> functions() {
		return this.functions;
	}
	public String[] inputs;
	public String[] outputs;

	public Model(String name, Parser parent) throws Exception {
		if (parent.models.containsKey(name))
			throw new Exception("Model " + name + " already exists in BLIF-project!");
		this.parent = parent;
		parent.models.put(name, this);
	}
}
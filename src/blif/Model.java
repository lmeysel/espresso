package blif;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import rs.binfunction.BinFunction;

/**
 * The class Model represents one BLIF-Model. Only data-fields necessary for Espresso are implemented yet.
 * @author Mitja Stachowiak
 */
public class Model {
 private BLIF parent;    public BLIF parent() { return this.parent; }
 private String name;   public String name() { return this.name; }
 public List<BinFunction> functions = new ArrayList<BinFunction>();   public List<BinFunction> functions() { return this.functions; }
 public String[] inputs = new String[0];
 public String[] outputs = new String[0];
 public boolean isSeparateFile = false;
 boolean saved = false;

 public Model(String name, BLIF parent) throws Exception {
  if (parent.models.containsKey(name)) throw new Exception("Model " + name + " already exists in BLIF-project!");
  this.parent = parent;
  this.name = name;
  parent.models.put(name, this);
 }

 public void appendToFile(FileWriter fileWriter, boolean firstModel) throws IOException {
  //write opening of model
  if (!firstModel) fileWriter.write(".model "+this.name+"\n");
  if (this.inputs.length != 0) {
   String s = ".inputs";
   for (int i = 0; i < this.inputs.length; i++) s += " "+this.inputs[i];
   fileWriter.write(s+"\n");
  }
  if (this.outputs.length != 0) {
   String s = ".outputs";
   for (int i = 0; i < this.outputs.length; i++) s += " "+this.outputs[i];
   fileWriter.write(s+"\n");
  }
  // write functions
  for (int i = 0; i < functions.size(); i++) {
   BinFunction f = functions.get(i);
   if (f.numInputs() == 0) continue;
   if (f.names()[0] == null) {
    fileWriter.write(".i "+f.numInputs()+"\n");
    fileWriter.write(".o 1\n");
   } else {
    String s = "\n.names";
    for (int j = 0; j < f.names().length; j++) s += " "+f.names()[j];
    fileWriter.write(s+"\n");
   }
   for (int j = 0; j < f.on().size(); j++) {
    String s = "";
    for (int k = 0; k < f.numInputs(); k++) switch (f.on().get(j).getVar(k)) {
     case BinFunction.INV :
      s += "!";
      break;
     case BinFunction.ZERO :
      s += "0";
      break;
     case BinFunction.ONE :
      s += "1";
      break;
     case BinFunction.DC :
      s += "-";
      break;
    }
    fileWriter.write(s+" 1\n");
   }
  }
  // write closing of model
  if (!firstModel) fileWriter.write(".end\n");
 }
}
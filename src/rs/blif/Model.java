package rs.blif;

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
 public final List<BinFunction> functions = new ArrayList<BinFunction>();
 public final List<Latch> latches = new ArrayList<Latch>();
 public final List<SubCircuit> subCircuits = new ArrayList<SubCircuit>();
 public final List<String> inputs = new ArrayList<String>();
 public final List<String> outputs = new ArrayList<String>();
 protected Boolean inputsAndOutputsDeclared = false;
 public boolean isSeparateFile = false;
 boolean saved = false;

 public Model(String name, BLIF parent) throws Exception {
  if (parent.models.containsKey(name)) throw new Exception("Model " + name + " already exists in BLIF-project!");
  this.parent = parent;
  this.name = name;
  parent.models.put(name, this);
 }
 
 /**
  * BLIF allows to specify the model's input and output parameters implicitly. This function turns implicitly given
  * input and output declarations into an explicit declaration.
  */
 public void checkDeclarations () {
  if (inputsAndOutputsDeclared) return;
  List<BinFunction> unusedOutputs = new ArrayList<BinFunction>();
  unusedOutputs.addAll(functions);
  for (int i = 0; i < this.functions.size(); i++) {
   BinFunction fi = this.functions.get(i);
   InpIt: for (int j = 0; j < fi.numInputs(); j++) {
    String s = fi.names()[j];
    for (int k = 0; k < this.functions.size(); k++) {
     BinFunction fk = this.functions.get(k);
     if (!s.equals(fk.name())) continue;
     // functions[i].inputs[j] is an existing function. Remove this function from unused inputs.
     int u = unusedOutputs.indexOf(fk);
     if (u >= 0) unusedOutputs.set(u, null);
     continue InpIt;
    }
    // functions[i].inputs[j] is not an existing function. Add this input to the model's inputs.
    this.inputs.add(s);
   }
  }
  // set the remaining unusedOutputs as the model's outputs
  for (int i = 0; i < unusedOutputs.size(); i++) if (unusedOutputs.get(i) != null) {
   this.outputs.add(unusedOutputs.get(i).name());
  }
  // mark inputs and outputs to be declared
  inputsAndOutputsDeclared = true;
 }

 public void appendToFile(FileWriter fileWriter, boolean firstModel) throws IOException {
  //write opening of model
  if (!firstModel) fileWriter.write(".model "+this.name+"\n");
  if (this.inputs.size() != 0) {
   String s = ".inputs";
   for (int i = 0; i < this.inputs.size(); i++) s += " "+this.inputs.get(i);
   fileWriter.write(s+"\n");
  }
  if (this.outputs.size() != 0) {
   String s = ".outputs";
   for (int i = 0; i < this.outputs.size(); i++) s += " "+this.outputs.get(i);
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
 
 
 
 public static class ModelCreator {
  public Model newModel (String name, BLIF parent) {
   Model m;
   try {
    m = new Model(name, parent);
   } catch (Exception e) {
    m = parent.models().get(name);
   }
   return m;
  }
 }
}
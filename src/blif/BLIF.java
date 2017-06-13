package blif;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import rs.binfunction.BinFunction;
import rs.binfunction.Cube;

/**
 * Describes one BLIF-Project, which can contain several models
 * @author Mitja Stachowiak
 */
public class BLIF {
 HashMap<String, Model> models = new HashMap<String, Model>();    public HashMap<String, Model> models() { return models; }
 private static final Logger log = Logger.getLogger("espresso");
 
 public BLIF() { }

 /**
  * Adds all models from the given file to the BLIF-project
  * @param fileName
  */
 public Model addFromFile(String fileName) {
  Model currentModel = null;
  Model firstModel = null;
  BinFunction[] currentFunctions = null;
  int nextOutCnt = 1;
  int nextInCnt = 0;
  String[] nextNames = null;
  try {
   File file = new File(fileName);
   BufferedReader f = new BufferedReader(new FileReader(file));
   String[] sp;
   String s;
   int n = 0;
   LINEREADER: do {
    // read current line
    s = f.readLine();
    if (s == null) {
     log.config("File "+fileName+" successfully parsed!");
     break LINEREADER;
    }
    n++;
    sp = s.split(" +");
    if (firstModel == null && currentModel != null) firstModel = currentModel;
    // interpret current line
    if (sp[0].length() == 0 || sp[0].charAt(0) == '#') continue LINEREADER; // no nothing on empty lines or comments
    if (currentModel == null && !sp[0].equals(".model")) {
     // implicit model begin using fileName
     try { currentModel = new Model(file.getName(), this); } catch (Exception e) { log.severe("Parsing stopped at line "+n+" of file "+file.getName()+": "+e.getMessage()); break LINEREADER; }
    }
    if (sp[0].charAt(0) == '.') {
     // BLIF control sequence
     currentFunctions = null;
     switch (sp[0]) {
      case ".model" :
       if (sp.length < 2) { log.severe("Parsing stopped at line "+n+" of file "+file.getName()+": Name of model not specified!"); break LINEREADER; }
       try { currentModel = new Model(sp[1], this); } catch (Exception e) { log.severe("Parsing stopped at line "+n+" of file "+file.getName()+": "+e.getMessage()); break LINEREADER; }
       break;
      case ".i" :
       if (sp.length < 2) { log.severe("Parsing stopped at line "+n+" of file "+file.getName()+": No paramater given for .i"); break LINEREADER; }
       try { nextInCnt = Integer.parseInt(sp[1]); } catch (NumberFormatException e) { log.severe("Parsing stopped at line "+n+" of file "+file.getName()+": Invalid integer!"); break LINEREADER; }
       if (nextInCnt < 0) { log.severe("Parsing stopped at line "+n+" of file "+file.getName()+": .i must be >= 0!"); break LINEREADER; }
       break;
      case ".o" :
       if (sp.length < 2) { log.severe("Parsing stopped at line "+n+" of file "+file.getName()+": No paramater given for .o"); break LINEREADER; }
       try { nextOutCnt = Integer.parseInt(sp[1]); } catch (NumberFormatException e) { log.severe("Parsing stopped at line "+n+" of file "+file.getName()+": Invalid integer!"); break LINEREADER; }
       if (nextOutCnt <= 0) { log.severe("Parsing stopped at line "+n+" of file "+file.getName()+": .o must be > 0!"); break LINEREADER; }
       break;
      case ".names" :
       nextInCnt = sp.length - 2;
       nextOutCnt = 1;
       nextNames = new String[nextInCnt+nextOutCnt];
       for (int i = 0; i < nextInCnt; i++) nextNames[i] = sp[i+1];
       nextNames[nextInCnt] = sp[nextInCnt+1];
       break;
      case ".subckt" :
       if (sp.length < 2) { log.severe("Line "+n+" of file "+file.getName()+" ignored: Subcircuit file name not specified!"); continue LINEREADER; }
       addFromFile(file.getParent() + File.separator + sp[1]);
       // save sub-circuit reference in the current model!
       break;
      case ".end" :
       currentModel = null;
       break;
      default :
       log.warning("Line "+n+" of file "+file.getName()+" ignored: Unknown control sequence "+sp[0]+"!");
       continue LINEREADER;
     }
    } else {
     // BLIF data line
     if (sp.length < 2) { log.severe("Parsing stopped at line "+n+" of file "+file.getName()+": No output specified!"); break LINEREADER; }
     if (currentModel == null) { log.warning("Line "+n+" ignored of file "+file.getName()+": Not inside a model"); continue LINEREADER; }
     if (currentFunctions == null) { // init the functions currently read
      currentFunctions = new BinFunction[nextOutCnt];
      for (int i = 0; i < nextOutCnt; i++) {
       currentFunctions[i] = new BinFunction(nextInCnt);
       if (nextNames != null) { // copy stored variable names to the new functions
        for (int j = 0; j < nextInCnt; j++) if (j < nextNames.length) currentFunctions[i].names()[j] = nextNames[j];
        if (i + nextInCnt < nextNames.length) currentFunctions[i].names()[nextInCnt] = nextNames[i + nextInCnt];
       }
       currentModel.functions.add(currentFunctions[i]);
      }
      nextOutCnt = 1;
      nextInCnt = 0;
      nextNames = null;
     }
     if (sp[1].length() != currentFunctions.length) { log.severe("Parsing stopped at line "+n+" of file "+file.getName()+": Number of output variables noes not fit the declared number!"); break LINEREADER; }
     for (int i = 0; i < currentFunctions.length; i++) switch (sp[1].charAt(i)) { // create separate function for each output variable
      case '0' :
       break;
      case '1' :
       try {
        if (!currentFunctions[i].on().add(new Cube(sp[0]))) log.warning("Warning at line "+n+" of file "+file.getName()+": Could not add cube to on-set!");
       } catch (Exception e) { log.severe("Parsing stopped at line "+n+" of file "+file.getName()+": "+e.getMessage()); break LINEREADER; }
       break;
      case '-' :
       try {
        if (!currentFunctions[i].dc().add(new Cube(sp[0]))) log.warning("Warning at line "+n+" of file "+file.getName()+": Could not add cube to dc-set!");
       } catch (Exception e) { log.severe("Parsing stopped at line "+n+" of file "+file.getName()+": "+e.getMessage()); break LINEREADER; }
       break;
      default :
       log.severe("Parsing stopped at line "+n+" of file "+file.getName()+": Illegal output variable value '"+sp[1].charAt(i)+"'!");
       break LINEREADER;
     }
    }
   } while (true);
   f.close();
  } catch (FileNotFoundException e) {
   log.severe("File "+fileName+" not accessible!");
  } catch (IOException e) {
   log.severe("Error while reading file "+fileName+"!");
  }
  if (firstModel != null) firstModel.isSeparateFile = true;
  return firstModel;
 }
 
 public void saveToFolder(String path) {
  if (path.lastIndexOf("/") != path.length()-1 && path.lastIndexOf("\\") != path.length()-1) path += "/";
  // mark all models unsaved
  Iterator<Entry<String, Model>> it = this.models().entrySet().iterator();
  while (it.hasNext()) {
   Map.Entry<String, Model> pair = it.next();
   pair.getValue().saved = false;
  }
  // save models
  it = this.models().entrySet().iterator();
  FileWriter fileWriter = null;  
  File file = null;
  boolean firstModel;
  while (it.hasNext()) {
   Map.Entry<String, Model> pair = it.next();
   Model model = pair.getValue();
   if (model.saved) continue;
   model.saved = true;
   firstModel = false;
   if (model.isSeparateFile) {
    firstModel = true;
    String n = path+model.name();
    if (n.lastIndexOf(".blif") != n.length()-5) n += ".blif";
    file = new File(n);
    try {
     if (fileWriter != null) {
      fileWriter.flush();
      fileWriter.close();
      fileWriter = null;
     }
     fileWriter = new FileWriter(file);
    } catch (IOException e) {
     log.severe("Error while writing to file "+file.getName()+"!");
    }
   }
   if (fileWriter != null)
    try {
     model.appendToFile(fileWriter, firstModel);
    } catch (IOException e) {
     log.severe("Error while writing to file "+file.getName()+"!");
    }
  }
  try { if (fileWriter != null) {
   fileWriter.flush();
   fileWriter.close();
   fileWriter = null;
  } } catch (IOException e) { log.severe("Error while closing file!"); }
 }

}
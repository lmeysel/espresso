package rs.blif;

public class SubCircuit {
 public final Model model;
 public final Model subModel;
 private String[] rel = null;
 private int inCnt = 0;
 
 public int relCnt () { return rel.length; }
 public String getRel (int i) { return rel[i]; }
 
 public SubCircuit (Model model, Model subModel) {
  if (model == null || subModel == null) throw new NullPointerException();
  this.model = model;
  this.subModel = subModel;
  updateRel();
  model.subCircuits.add(this);
 }
 
 private void updateRel () {
  if (rel != null && subModel.inputs.size() + subModel.outputs.size() == rel.length && subModel.inputs.size() == inCnt) return;
  String[] newRel = new String[subModel.inputs.size() + subModel.outputs.size()];
  if (this.rel != null) {
   for (int i = 0; i < this.inCnt; i++) if (i < subModel.inputs.size()) newRel[i] = this.rel[i]; // copy input rels
   for (int i = 0; i < this.rel.length-this.inCnt; i++) if (i < subModel.outputs.size()) newRel[i+subModel.inputs.size()] = this.rel[i+this.inCnt]; // copy output rels
  }
  this.rel = newRel;
  this.inCnt = this.subModel.inputs.size();
 }
 
 public boolean setRel (String subModelInp, String modelParam) {
  updateRel();
  for (int i = 0; i < subModel.inputs.size(); i++) if (subModel.inputs.get(i).equals(subModelInp)) {
   rel[i] = new String(modelParam); // IMPORTANT: create a new String-Object here
   return true;
  }
  for (int i = 0; i < subModel.outputs.size(); i++) if (subModel.outputs.get(i).equals(subModelInp)) {
   rel[i + subModel.inputs.size()] = new String(modelParam); // IMPORTANT: create a new String-Object here
   return true;
  }
  return false;
 }
 
 public String getRelByParam (String modelParam) {
  updateRel();
  for (int i = 0; i < this.rel.length; i++) if (this.rel[i].equals(modelParam)) return this.rel[i];
  return null;
 }
 
 public String getSubRelByParam (String modelParam) {
  updateRel();
  for (int i = 0; i < this.rel.length; i++) if (this.rel[i].equals(modelParam)) {
   if (i < this.subModel.inputs.size()) return this.subModel.inputs.get(i);
   else return this.subModel.outputs.get(i-this.subModel.inputs.size());
  }
  return null;
 }
 
 public boolean isValid () {
  updateRel();
  for (int i = 0; i < this.rel.length; i++) if (this.rel[i] == null) return false;
  return true;
 }
}

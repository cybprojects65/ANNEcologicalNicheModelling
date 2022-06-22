package org.cnr.datanalysis.ecomod.featureextraction;

public class FeatureVector {

	public double x;
	public double y;
	public double ANNTarget;
	public Double [] features;
	public boolean incomplete = false;
	public int incompleteIdx = 0;
	
	public FeatureVector(double x, double y, double ANNTarget, int nfeatures) {
		this.x = x;
		this.y = y;
		this.ANNTarget = ANNTarget;
		this.features = new Double[nfeatures];
		this.incomplete = false;
	}
	
	public void addFeature(Double value,int pos) {
		features[pos] = value;
		if (value == null) {
			incomplete = true;
			incompleteIdx = pos;
		}
	}
	
}

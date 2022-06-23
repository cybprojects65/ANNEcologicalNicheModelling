package org.cnr.datanalysis.ecomod.featureselection;

import java.io.Serializable;

import org.cnr.datanalysis.ecomod.featureextraction.FeatureVector;
import org.cnr.datanalysis.ecomod.utils.Operations;
import org.cnr.datanalysis.ecomod.utils.PrincipalComponentAnalysis;

public class FeatureSelector implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	FeatureVector[] features;
	
	public FeatureSelector() {
		
	}
	
	PrincipalComponentAnalysis pca;
	public double[] means;
	public double[] variances;
	
	public FeatureVector[] selectFeaturesWithPCA (FeatureVector[] features) throws Exception{
		
		this.features = features;
		double[][] featureMatrix = featureVector2matrix(features);
		
		Operations operations = new Operations();
		double[][] standardisedFeatures = operations.standardize(featureMatrix);
		means = operations.means;
		variances = operations.variances;
		//double[][] standardisedFeatures = featureMatrix;
		
		//calculate PCA
		pca = new PrincipalComponentAnalysis();
		pca.calcPCA(standardisedFeatures,0.95);
		// get the pca components for all the vector
		double[][] pcafeatures = pca.getProjectionsMatrix(standardisedFeatures);
		return matrix2FeatureVector(features,pcafeatures);
		
	}
	
	public FeatureVector[] resizeFeaturesWithPCA (FeatureVector[] features) throws Exception{
		
		this.features = features;
		double[][] featureMatrix = featureVector2matrix(features);
		Operations operations = new Operations();
		double[][] standardisedFeatures = operations.standardize(featureMatrix,means,variances);//use previous standardization ranges
		//double[][] standardisedFeatures = featureMatrix;
		// get the pca components for all the vector
		double[][] pcafeatures = pca.getProjectionsMatrix(standardisedFeatures);
		return matrix2FeatureVector(features,pcafeatures);
		
	}

	public static FeatureVector[] matrix2FeatureVector(FeatureVector[] originalfeatures, double [][] featureMatrix){
		
		int nrows = featureMatrix.length;
		int ncols = featureMatrix[0].length;
		
		for (int i = 0;i<nrows;i++){
			originalfeatures[i].features = new Double[ncols];
			for (int j = 0; j<ncols; j++) {
				originalfeatures[i].addFeature(featureMatrix[i][j], j);
			}
		}
		
		return originalfeatures;
		
	}

	public static double [][] featureVector2matrix(FeatureVector[] features){
		
		int nrows = features.length;
		int ncols = features[0].features.length;
		double[][] matrix = new double[nrows][ncols];
		
		for (int i = 0;i<nrows;i++){
			for (int j = 0; j<ncols; j++) {
				matrix[i][j] = features[i].features[j];
			}
		}
		
		return matrix;
		
	}
}

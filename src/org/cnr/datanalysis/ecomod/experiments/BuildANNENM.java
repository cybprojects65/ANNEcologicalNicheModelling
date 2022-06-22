package org.cnr.datanalysis.ecomod.experiments;

import java.io.File;

import org.cnr.datanalysis.ecomod.modelling.ANNEcologicalNicheModel;
import org.cnr.datanalysis.ecomod.modelling.ANNTraining;

public class BuildANNENM {

	public static void main(String[] args) throws Exception{
		
		File basePathOccurrences = new File("observations");
		File basePathEnvironmentalFeatures = new File("environmentalfeatures/Global");
		String species = "Carcharodon carcharias";
		double learningThreshold = 0.0001;//0.0001;
		int numberOfCycles = 5000;//5000;
		boolean balance = false;
		float learningRate = 0.5f;//0.5f;
		int nfolds = 0;
		
		/*
		int minNeurons []= {90};
		int maxNeurons []= {90};
		int neuronStep []= {1};
		 */
		/*
		int minNeurons []= {30,10,10};
		int maxNeurons []= {30,10,10};
		*/
		/*
		int minNeurons []= {10,20,10};
		int maxNeurons []= {10,20,10};
		*/
		int minNeurons []= {85};
		int maxNeurons []= {85};
		int neuronStep []= {0,0};
		
		System.out.println("-----TRAINING-----");
		File bestModel = ANNTraining.findOptimalModel(minNeurons, maxNeurons, neuronStep, 
				species, basePathOccurrences, basePathEnvironmentalFeatures, nfolds, 
				learningThreshold, numberOfCycles, learningRate, balance);
		
		System.out.println("-----PROJECTING-----");
		ANNEcologicalNicheModel annENM = new ANNEcologicalNicheModel(bestModel);
		File  projectedAnnENM = annENM.ENM(basePathEnvironmentalFeatures);
		
		System.out.println("ANN projection is in "+projectedAnnENM.getAbsolutePath());
		System.out.println("ASC ANN projection is in "+projectedAnnENM.getAbsolutePath().replace(".csv", ".asc"));
		
	}
	
	
}

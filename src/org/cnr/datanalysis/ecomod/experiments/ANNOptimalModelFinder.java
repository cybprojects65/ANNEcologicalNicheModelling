package org.cnr.datanalysis.ecomod.experiments;

import java.io.File;

import org.cnr.datanalysis.ecomod.modelling.ANNTraining;

public class ANNOptimalModelFinder {

	public static void main(String[] args) throws Exception {

		File basePathOccurrences = new File("observations");
		File basePathEnvironmentalFeatures = new File("environmentalfeatures/Global");

		String species = "Carcharodon carcharias";
		//String layerS = "50";//"100";
		
		double learningThreshold = 0.0001;//0.0001;
		int numberOfCycles = 5000;//5000;
		boolean balance = false;
		float learningRate = 0.5f;//0.5f;
		int nfolds = 10;
		
		//ANNTraining trainer = new ANNTraining(species, basePathOccurrences, basePathEnvironmentalFeatures, nfolds, learningThreshold, numberOfCycles, learningRate, layerS);
		//trainer.skipFeature = -1;
		//trainer.init(balance);
		//trainer.evaluate();
		
		//three layers
		/*		
			int minNeurons []= {10,10,10};
			int maxNeurons []= {40,20,20};
			int neuronStep []= {20,10,10};
		*/		
				
		
		//two layers
		/*
			int minNeurons []= {10,10};
			int maxNeurons []= {170,100};
			int neuronStep []= {50,50};
		*/
				
		
		//one layer refinement 1
		
		int minNeurons []= {10};
		int maxNeurons []= {150};
		int neuronStep []= {10};
		
		//one layer refinement 2
		/*
		int minNeurons []= {80};
		int maxNeurons []= {100};
		int neuronStep []= {5};
		*/		
		
		//one layer selected
		/*		
		int minNeurons []= {90};
		int maxNeurons []= {90};
		int neuronStep []= {1};
		*/
					
					
		File bestModel = ANNTraining.findOptimalModel(minNeurons, maxNeurons, neuronStep, 
				species, basePathOccurrences, basePathEnvironmentalFeatures, nfolds, 
				learningThreshold, numberOfCycles, learningRate, balance);
		
	}
}

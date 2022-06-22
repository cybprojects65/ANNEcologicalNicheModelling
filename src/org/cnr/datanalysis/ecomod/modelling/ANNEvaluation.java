package org.cnr.datanalysis.ecomod.modelling;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class ANNEvaluation {

	
	public static void main(String[] args)  throws Exception {
		
		File projection = new File("C:\\Users\\Utente\\eclipse-workspace-Ecosystems\\ANNEcologicalNicheModelling\\trainingsets\\Carcharodon carcharias_1140debe-bc6c-464b-a7eb-3d0bd54ab51e\\carcharodon_carcharias_trainingReprojected.csv");
		ANNEvaluation evaluator = new ANNEvaluation();
		evaluator.calcOptimalAccuracy(projection);
		
		evaluator.init(projection);
		//System.out.println("->"+evaluator.calcAccuracy(0.96));
		//System.out.println("->"+evaluator.calcAccuracy(0.001));
		System.out.println("->"+evaluator.calcAccuracy(0.69));
		
	}
	double bestAccuracy = 0;
	double bestThreshold = 0;
	double accuracy = 0;

	public double calcOptimalAccuracy(File projectedFile) throws Exception {
		init(projectedFile);
		
		bestAccuracy = 0;
		bestThreshold = 0;
		double thresholdstep = 0.001;
		double threshold = thresholdstep;
		while (threshold < 1) {
			double accuracy = calcAccuracy(threshold);
			
			if (accuracy > bestAccuracy) {
				bestAccuracy = accuracy;
				bestThreshold = threshold;
			}
			threshold = threshold + thresholdstep;
		}
		
		if (bestThreshold==thresholdstep || bestThreshold>=1)
			bestAccuracy = 0;
		
		System.out.println("Optimal accuracy: " + bestAccuracy + " Threshold: " + bestThreshold);
		return bestAccuracy;
	}

	Double[] references;
	Double[] predictions;

	public void init(File projectedFile) throws Exception {
		List<String> fileLines = Files.readAllLines(projectedFile.toPath());
		List<Double> refs = new ArrayList<>();
		List<Double> preds = new ArrayList<>();
		int linecounter = 0;
		for (String line : fileLines) {
			if (linecounter > 0) {// skip header
				String elements[] = line.split(",");

				double ref = Double.parseDouble(elements[elements.length - 2]);
				double pred = Double.parseDouble(elements[elements.length - 1]);
				refs.add(ref);
				preds.add(pred);
			}
			linecounter++;
		}
		references = new Double[refs.size()];
		references = refs.toArray(references);
		predictions = new Double[preds.size()];
		predictions = preds.toArray(predictions);
	}

	public double calcAccuracy(double threshold) throws Exception {
		int matches = 0;
		for (int i = 0; i < predictions.length; i++) {
			double ref = references[i];
			double pred = predictions[i];
			double predcat = -1;
			if (pred<=threshold)
				predcat=0;
			else
				predcat=1;
			
			if (ref == predcat) {
				//System.out.println("----OK-p:"+pred+"<>t:"+threshold+" r:"+ref+"");
				matches++;
			}else {
				//System.out.println("----NO-p:"+pred+"<>t:"+threshold+" r:"+ref+"");
			}
		}
		
		
		double accuracy = (double) matches / (double) (predictions.length);
		
		//System.out.println("--Acc: " + accuracy + " Thr.: " + threshold+" matches "+matches+"/"+predictions.length);
		
		return accuracy;
	}

}

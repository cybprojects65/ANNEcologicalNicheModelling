package org.cnr.datanalysis.ecomod.modelling;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.math3.stat.inference.TTest;
import org.cnr.datanalysis.ecomod.featureextraction.Observations;
import org.cnr.datanalysis.ecomod.featureextraction.OccurrenceEnrichment;

public class TrainingSetManager {

	public static void main(String[] args) throws Exception{
		
		File basePathOccurrences = new File("observations");
		File basePathEnvironmentalFeatures = new File("environmentalfeatures/Global");
		File basePathTrainingSet = new File("trainingsets");
		File basePathNFoldTrainingSet = new File("nfolds");
		
		String species = "Carcharodon carcharias";
		int folds = 1;
		boolean balance = true;
		
		TrainingSetManager manager = new TrainingSetManager();
		manager.generateTrainingSet(species, basePathTrainingSet, basePathOccurrences, basePathEnvironmentalFeatures,balance);
		//manager.sample8020ManyFold(basePathNFoldTrainingSet, folds);
	}
	
	public void featureAnalysis(File trainingSetFile) throws Exception{
		
		List<List<Double>> abs = new ArrayList();
		List<List<Double>> pres = new ArrayList();
		
		BufferedReader brr = new BufferedReader(new FileReader(trainingSetFile));
		String liner = brr.readLine();
		liner = brr.readLine();
		
		while(liner!=null) {
			String []els = liner.split(",");
			double target = Double.parseDouble(els[els.length-1]);
			if (target==0d) {
				for (int i=2;i<(els.length-1);i++) {
					Double d = Double.parseDouble( els[i] );
					List<Double> absd = null; 
					if (abs.size()<=(i-2)) {
						absd = new ArrayList();
						absd.add(d);
						abs.add(absd);
					}
					else {
						absd = abs.get(i-2);
						absd.add(d);
						abs.set(i-2,absd);
					}
				}
			}else {
				for (int i=2;i<(els.length-1);i++) {
					Double d = Double.parseDouble( els[i] );
					//d = d+0.1E-100;
					
					List<Double> press = null;
					if (pres.size()<=(i-2)) {
						press = new ArrayList();
						press.add(d);
						pres.add(press);
					}
					else {
						press  = pres.get(i-2);
						press.add(d);
						pres.set(i-2, press);
						
					}					
					
				}
			}
				
			
			liner = brr.readLine();
		}
		
		brr.close();
		
		
		
		for (int k=0;k< abs.size() ;k++) {
			List<Double> v1d = abs.get(k);
			List<Double> v2d = pres.get(k);
			double v1 []= new double[v1d.size()];
			for (int g=0;g<v1.length;g++)
				v1[g] = v1d.get(g);
			double v2 []= new double[v2d.size()];
			for (int g=0;g<v2.length;g++)
				v2[g] = v2d.get(g);
			
			TTest ttest = new TTest();
			//If the p value is large, the data do not give you any reason to conclude that the overall means differ-> the distributions are the same! (https://www.graphpad.com/guides/prism/latest/statistics/interpreting_a_large_p_value_from_an_unpaired_t_test.htm) 
			//double p = ttest.pairedTTest(v1,v2);
			
			boolean p = ttest.pairedTTest(v1,v2,0.05); //are the distribution means different? true (yes) or false (no)
			//double p = ttest.pairedTTest(abs[k], abs[k]);
			//System.out.println("F"+k+" p:"+p);
			System.out.println("F"+k+" features are different? "+p);
		}
		
		
		/*
		System.out.println("FEATURE COMPARISON:");
		for (int j=0;j<abs.length;j++) {
			abs[j] = abs[j]/(double) abs.length;
			pres[j] = pres[j]/(double) pres.length;
			System.out.println("F"+j+": "+abs[j]+" vs "+pres[j]);
		}
		System.out.println("########");
		*/
		
	}

	
	
	public File trainingSetFile;
	public File generateTrainingSet(String species, File basePathTrainingSet, File basePathOccurrences, File basePathEnvironmentalFeatures, boolean balance) throws Exception{
		File trainingSetFile = new File(basePathTrainingSet,species.toLowerCase().replace(" ", "_")+".csv");
		Observations observations = new Observations();
		observations.buildObservations(species, basePathOccurrences,balance);
		OccurrenceEnrichment enricher = new OccurrenceEnrichment(observations);
		enricher.enrichOccurrences(basePathEnvironmentalFeatures);
		enricher.save(trainingSetFile);
		this.trainingSetFile = trainingSetFile;
		System.out.println("Training set file saved to "+trainingSetFile.getAbsolutePath());
		if (balance)
			featureAnalysis(trainingSetFile);
		
		return trainingSetFile;
	}
	

	public void sample8020ManyFold(File outputfolder, int fold) throws Exception{
		
		for (int i = 1;i<=fold;i++) {
			sample8020(this.trainingSetFile, outputfolder);
		}
	}

	public void sample8020ManyFold(File trainingSet, File outputfolder, int fold) throws Exception{
		
		for (int i = 1;i<=fold;i++) {
			sample8020(trainingSet, outputfolder);
		}
	}
	
	public void sample8020(File trainingSet, File outputfolder) throws Exception{
		
		List<String> allLines = Files.readAllLines(trainingSet.toPath());
		
		List<String> absences = new ArrayList<>();
		List<String> presences = new ArrayList<>();
		String header = allLines.get(0);
		
		for (String line:allLines) {
			
			String elements[] = line.split(",");
			String target = elements[elements.length-1];
			double dtarget = -1;
			
				try {
					dtarget=Double.parseDouble(target);
				} catch (NumberFormatException e) {
				
				}
				
			
			if (dtarget == 0d)
				absences.add(line);
			else if (dtarget == 1d)
				presences.add(line);
		}
		
		lines80training = new ArrayList<>(); 
		lines20testing = new ArrayList<>();
		System.out.print("Absences: ");
		take8020(absences);
		System.out.print("Presences: ");
		take8020(presences);
		String ID = ""+UUID.randomUUID();
		File trainingFile = new File(outputfolder,"TRAINING_"+ID+".csv");
		File testFile = new File(outputfolder,"TEST_"+ID+".csv");
		
		BufferedWriter bw = new BufferedWriter(new FileWriter(trainingFile));
		bw.write(header+"\n");
		for (String line:lines80training) {
			bw.write(line+"\n");
		}
		bw.close();
		
		bw = new BufferedWriter(new FileWriter(testFile));
		bw.write(header+"\n");
		for (String line:lines20testing) {
			bw.write(line+"\n");
		}
		bw.close();
	}
	
	List<String> lines80training; 
	List<String> lines20testing;
	
	public void take8020(List<String> lines){
		int nlines = lines.size();
		int n80 = (int) Math.round(nlines*0.8);
		System.out.println("Taking "+n80+" features for training and "+(nlines-n80)+" for testing");
		ArrayList<Integer> random80 = generateRandoms(n80, 0, nlines-1);
		Integer counter = 0;
		for (String line:lines) {
			if (random80.contains(counter)) {
				lines80training.add(line);
			}else {
				lines20testing.add(line);
			}
			counter++;
		}
	}

	public static ArrayList<Integer> generateRandoms(int numberOfRandoms, int min, int max) {

		ArrayList<Integer> randomsSet = new ArrayList<Integer>();
		
		// if number of randoms is equal to -1 generate all numbers
		if (numberOfRandoms == -1) {
			for (int i = min; i < max; i++) {
				randomsSet.add(i);
			}
		} else {
			int numofrandstogenerate = 0;
			if (numberOfRandoms <= max) {
				numofrandstogenerate = numberOfRandoms;
			} else {
				numofrandstogenerate = max;
			}

			if (numofrandstogenerate == 0) {
				randomsSet.add(0);
			} else {
				for (int i = 0; i < numofrandstogenerate; i++) {

					int RNum = -1;
					RNum = (int) ((max-min) * Math.random()) + min;

					// generate random number
					while (randomsSet.contains(RNum)) {
						RNum = (int) ((max-min) * Math.random()) + min;
						// AnalysisLogger.getLogger().debug("generated " + RNum);
					}

					// AnalysisLogger.getLogger().debug("generated " + RNum);

					if (RNum > 0)
						randomsSet.add(RNum);
				}

			}
		}

		return randomsSet;
	}
	
}

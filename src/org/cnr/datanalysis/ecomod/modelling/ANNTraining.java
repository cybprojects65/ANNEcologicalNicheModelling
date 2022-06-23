package org.cnr.datanalysis.ecomod.modelling;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.gcube.ann.feedforwardann.DichotomicANN;

public class ANNTraining {

	public static List<String> buildTopologyString(String initialTopology, int min[], int max[], int step[],int idx){
		
		if (idx>=min.length)
			return new ArrayList<>();
		
		List<String> list = new ArrayList<>();
		//if (initialTopology.length()>0)
			//list.add(initialTopology);
		for (int i=min[idx];i<=max[idx];i=i+step[idx]) {
			String top = initialTopology+"|"+i;
			if (initialTopology.length()==0)
				top = ""+i;
			list.add(top);
			List<String> sublist = buildTopologyString(top, min, max, step, idx+1);
			list.addAll(sublist);
		}
		/*
		List<String> newlist = new ArrayList<>(list);
		
		for (String l : list) {
			List<String> sublist = buildTopologyString(l, min, max, step, idx+1);
			newlist.addAll(sublist);
		}
		*/
		return list;
	}
	
	//search for the best topology: growing strategy
	public static File findOptimalModel(int minNeurons[],int maxNeurons[], int neuronStep [], String species, File basePathOccurrences, File basePathEnvironmentalFeatures, int nfolds,
			double learningThreshold, int numberOfCycles, float learningRate, boolean balanceTrainingSet, boolean reduceDimensionality) throws Exception{
		
		List<File> allResults = new ArrayList<>();
		List<String> allTopologies = new ArrayList<>();;
		if (neuronStep[0]>0) {
			allTopologies = buildTopologyString("", minNeurons, maxNeurons, neuronStep, 0);
		}else {
			String topo = "";
			for (int i=0;i<minNeurons.length;i++) {
				if (i ==0 )
					topo = topo+minNeurons[i];
				else
					topo = topo+"|"+minNeurons[i];
			}
			allTopologies.add(topo);
		}
		
		System.out.println(allTopologies.toString().replace(",","\n"));
		
		//System.exit(0);
		int counter = 0;
		for (String topology:allTopologies) {
			
			
			System.out.println("TESTING TOPOLOGY : "+topology);
			ANNTraining trainer = new ANNTraining(species, basePathOccurrences, basePathEnvironmentalFeatures, nfolds, learningThreshold, numberOfCycles, learningRate, topology);
			trainer.skipFeature = -1;
			trainer.init(balanceTrainingSet,reduceDimensionality);
			trainer.evaluate();
			allResults.add(trainer.provenanceFile);
			System.out.println("TOPOLOGY : "+topology+ " SAVED IN "+trainer.provenanceFile +"\n");
			counter++;
			double status = Math.round(100d*(double)counter/(double)allTopologies.size());
			System.out.println("\n***STATUS : "+status+"***\n");
			
		}
		
		System.out.println("!TRAINED "+allResults.size()+ " MODELS!");
		
		double bestaccuracy = 0;
		File bestModel = null;
		String bestTopology = "";
		
		double bestquality = 0;
		File bestqualityModel = null;
		String bestqualityTopology = "";
		
		for (File result:allResults) {
			Properties p = new Properties();
			p.load(new FileInputStream(result));
			double accuracy = Double.parseDouble(p.getProperty("ACCURACY_NFOLD"));
			double tsaccuracy = Double.parseDouble(p.getProperty("ACCURACY_ON_TRAINING_SET"));
			double threshold = Double.parseDouble(p.getProperty("BEST_THRESHOLD"));
			String topology = p.getProperty("TOPOLOGY");
			System.out.println("->"+topology+"->nf_acc:"+accuracy+" ts_acc:"+tsaccuracy+" threshold:"+threshold);
			double accuracyStem = accuracy/threshold;
			if (accuracy>bestaccuracy) {
						bestaccuracy = accuracy;
						bestModel = result;
						bestTopology = p.getProperty("TOPOLOGY");
			}
			if (accuracyStem>bestquality) {
				bestquality = accuracyStem;
				bestqualityModel = result;
				bestqualityTopology = p.getProperty("TOPOLOGY");
	}
		}
		
		System.out.println("\nBEST ACCURACY MODEL:");
		System.out.println("ARCHITECTURE AM:"+bestTopology);
		Properties p = new Properties();
		p.load(new FileInputStream(bestModel));
		System.out.println(p.toString().replace(",", "\n"));
		
		System.out.println("\nBEST QUALITY MODEL:");
		System.out.println("ARCHITECTURE QM:"+bestqualityTopology);
		Properties p2 = new Properties();
		p2.load(new FileInputStream(bestqualityModel));
		System.out.println(p2.toString().replace(",", "\n"));
		
		
		return bestModel;
	}
	
	
	///////////////////
	
	public File basePathOccurrences;
	public File basePathEnvironmentalFeatures;
	public String species;
	public int folds;
	public double learningThreshold = 0.0001;
	public float learningRate = 0.5f;
	public int numberOfCycles = 1000;
	// String layerS = "0";
	// String layerS = "20|10|5";
	public String layerS = "";
	public DichotomicANN trainer;
	public File trainedANN;
	public File testedANN;
	public File trainedANNTrainingSetProjection;
	public String sessionID;
	public File basePathTS;
	public File provenanceFile;
	public File basePathTSFolds;
	public File mainTrainingSet;
	public File mainFeatureExtractor;
	
	public double averageAccuracyNFold;
	public int skipFeature = -1;
	
	public ANNTraining(String species, File basePathOccurrences, File basePathEnvironmentalFeatures, int folds,
			double learningThreshold, int numberOfCycles, float learningRate, String neuronsperlayer) {
		this.species = species;
		this.basePathEnvironmentalFeatures = basePathEnvironmentalFeatures;
		this.basePathOccurrences = basePathOccurrences;
		this.folds = folds;
		this.learningThreshold = learningThreshold;
		this.learningRate = learningRate;
		this.numberOfCycles = numberOfCycles;
		this.layerS = neuronsperlayer;
	}


	public void init(boolean balance, boolean reduceDimensionality) throws Exception {
		sessionID = "" + UUID.randomUUID();
		basePathTS = new File("./trainingsets/" + species + "_" + sessionID);
		if (!basePathTS.exists())
			basePathTS.mkdir();

		basePathTSFolds = new File(basePathTS, "nfold");
		if (!basePathTSFolds.exists())
			basePathTSFolds.mkdir();

		TrainingSetManager manager = new TrainingSetManager();
		mainTrainingSet = manager.generateTrainingSet(species, basePathTS, basePathOccurrences, basePathEnvironmentalFeatures,balance,reduceDimensionality);
		mainFeatureExtractor = manager.occurrenceEnrichmentFile;
		
		manager.sample8020ManyFold(basePathTSFolds, folds);

	}

	public void evaluate() throws Exception {
		
		train(mainTrainingSet);
		File trainingSetANN = new File(trainedANN.getAbsolutePath());
		File trainingSetProjection = new File(trainedANNTrainingSetProjection.getAbsolutePath());
		
		ANNEvaluation evaluator = new ANNEvaluation();
		double accuracy = evaluator.calcOptimalAccuracy(trainedANNTrainingSetProjection);
		
		if (accuracy>0) {
		File[] allFiles = basePathTSFolds.listFiles();
		
		HashSet<String> allSessions = new HashSet<>();
		if (folds>0) {
		for (File f: allFiles) {
			String session = f.getName().substring(f.getName().indexOf("_")+1);
			allSessions.add(session);
		}
		
		int countsessions = 0;
		double sumaccuracies = 0;
		averageAccuracyNFold = 0;
		for (String session:allSessions) {
			System.out.println("Evaluating session "+session+" #"+countsessions+"/"+allSessions.size());
			File training = new File(basePathTSFolds,"TRAINING_"+session);
			File test = new File(basePathTSFolds,"TEST_"+session);
			train(training);
			test(test);
			ANNEvaluation subevaluator = new ANNEvaluation();
			double subaccuracy = subevaluator .calcOptimalAccuracy(testedANN);
			sumaccuracies = sumaccuracies+subaccuracy;
			countsessions++;
		}
			averageAccuracyNFold = sumaccuracies/(double)allSessions.size();
		}else
			averageAccuracyNFold = accuracy;
		
		System.out.println("Total Average NFOLD accuracy: "+averageAccuracyNFold);
		}else {
			System.out.println("WARNING - UNPROMISING MODEL! - ACCURACY IS 0");
			
		}
		System.out.println("\nFinal accuracy on the training set: "+accuracy);
		System.out.println("Final best threshold: "+evaluator.bestThreshold);
		provenanceFile = new File(basePathTS,"Parameters.txt");
		FileWriter fw = new FileWriter(provenanceFile);
		fw.write("ACCURACY_ON_TRAINING_SET="+accuracy+"\n");
		fw.write("BEST_THRESHOLD="+evaluator.bestThreshold+"\n");
		fw.write("NFOLD="+folds+"\n");
		fw.write("ACCURACY_NFOLD="+averageAccuracyNFold+"\n");
		fw.write("SPECIES="+species+"\n");
		fw.write("BASEPATH_OCCURRENCES="+basePathOccurrences+"\n");
		fw.write("BASEPATH_ENVIRONMENTAL_FEATURES="+basePathEnvironmentalFeatures+"\n");
		fw.write("LEARNING_THRESHOLD="+learningThreshold+"\n");
		fw.write("LEARNING_RATE="+learningRate+"\n");
		fw.write("NCYCLES="+numberOfCycles+"\n");
		fw.write("TOPOLOGY="+layerS+"\n");
		fw.write("TRAINED_ANN="+trainingSetANN.getName()+"\n");
		fw.write("TRAINED_ANN_TRAINING_SET="+mainTrainingSet.getName()+"\n");
		fw.write("TRAINED_FEATURE_EXTRACTOR="+mainFeatureExtractor.getName()+"\n");
		fw.write("TRAINED_ANN_PROJECTION="+trainingSetProjection.getName()+"\n");
		fw.write("SESSION_ID="+sessionID+"\n");
		fw.write("FEATURE_SKIPPED="+skipFeature+"\n");
		fw.close();
		
	}
	
	public void train(File trainingFile) throws Exception {

		String[] layerSs = layerS.split("\\|");

		int[] layers = new int[layerSs.length];

		for (int j = 0; j < layers.length; j++)
			layers[j] = Integer.parseInt(layerSs[j]);

		trainer = new DichotomicANN();

		String targetColumn = "target";

		BufferedReader br = new BufferedReader(new FileReader(trainingFile));
		String header = br.readLine();
		br.close();
		String elements[] = header.split(",");
		
		List<String> inputColumnsL = new ArrayList<>();
		
		for (int i = 2; i < (elements.length - 1); i++) {// remove x,y,target columns
			if ((i-2) != skipFeature)
				inputColumnsL.add(elements[i]);
		}
		
		String inputColumns[] = new String[inputColumnsL.size()]; 
		inputColumns = inputColumnsL.toArray(inputColumns);
			
		/*
		String inputColumns[] = new String[1]; // remove x,y,target columns
		for (int i = 2; i < 3; i++) {
			inputColumns[i - 2] = elements[i];
		}
		*/
		
		trainer.train(trainingFile.getAbsolutePath(), learningThreshold, numberOfCycles, learningRate, layers, inputColumns, targetColumn);
		trainedANN = trainer.outputANN;
		trainedANNTrainingSetProjection = trainer.outputTrainingProjection;
	}
	
	public void test(File testFile) throws Exception {

		BufferedReader br = new BufferedReader(new FileReader(testFile));
		String header = br.readLine();
		br.close();
		String elements[] = header.split(",");
		String inputColumns[] = new String[elements.length - 3]; // remove x,y,target columns
		for (int i = 2; i < elements.length - 1; i++) {
			inputColumns[i - 2] = elements[i];
		}
		DichotomicANN tester = new DichotomicANN();
		tester.test(testFile.getAbsolutePath(), trainedANN.getAbsolutePath(), inputColumns);
		testedANN = tester.outputTestProjection;
		
	}
	
}

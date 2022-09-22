package org.cnr.datanalysis.ecomod.cloudcomputing;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.cnr.datanalysis.ecomod.modelling.ANNEcologicalNicheModel;
import org.cnr.datanalysis.ecomod.modelling.ANNTraining;

public class Main {

	
	public static void main(String[] args) throws Exception{

		
		
		File basePathOccurrences = new File("observationsDM");
		File basePathEnvironmentalFeatures = new File("environmentalfeaturesDM");
		
		if (basePathOccurrences.exists()) {
			//destroy directory
			FileUtils.deleteDirectory(basePathOccurrences);
		}
		if (!basePathEnvironmentalFeatures.exists()) {
			//destroy directory
			FileUtils.deleteDirectory(basePathEnvironmentalFeatures);
		}
		
		File trainingFolder = new File("trainingsets");
		if (!trainingFolder.exists())
			trainingFolder.mkdir();
		
		if (!basePathOccurrences.exists())
			basePathOccurrences.mkdir();
		if (!basePathEnvironmentalFeatures.exists())
			basePathEnvironmentalFeatures.mkdir();
		
		String species = "";
		double learningThreshold = 0.0001;
		int numberOfCycles = 5000;
		boolean balance = false;
		boolean reduceDimensionality = true;
		float learningRate = 0.5f;
		int nfolds = 0;
		int minNeurons []= {60};
		int maxNeurons []= {60};
		int neuronStep []= {0};
		
		if (args.length>0) {
			//summarize parameters
			System.out.println("V2.0");
			System.out.println("SUMMARY OF THE INPUT PARAMETERS:");

			species = args[0];
			//copy the input files to the directories
			File absencerecords = new File(args[1]);
			File absencerecordsDest = new File(basePathOccurrences,"Absence_"+species.replace(" ", "_")+".csv");
			FileUtils.copyFile(absencerecords, absencerecordsDest);
			System.out.println("Absence records file: "+absencerecordsDest.getAbsolutePath()+" bytes: "+absencerecordsDest.length());
			File presencerecords = new File(args[2]);
			File presencerecordsDest = new File(basePathOccurrences,"Presence_"+species.replace(" ", "_")+".csv");
			FileUtils.copyFile(presencerecords, presencerecordsDest);
			System.out.println("Presence records file: "+presencerecordsDest.getAbsolutePath()+" bytes: "+presencerecordsDest.length());
			File environmentalfeaturesZIP = new File(args[3]);
			File zipfile = new File(basePathEnvironmentalFeatures,environmentalfeaturesZIP.getName());
			FileUtils.copyFile(environmentalfeaturesZIP,zipfile);
			unzip(zipfile.toPath());
			File [] allAscFiles = basePathEnvironmentalFeatures.listFiles();
			for (File ascFile:allAscFiles) {
				
				if (!ascFile.getName().toLowerCase().endsWith(".asc")) {
					System.out.println("DELETING: "+ascFile.getName());
					if (ascFile.isDirectory()) {
						FileUtils.deleteDirectory(ascFile);
					}else
						FileUtils.forceDelete(ascFile);
				}else {
					System.out.println("Feature file: "+ascFile.getName());
				}
			}
			learningThreshold = Double.parseDouble(args[4]);
			learningRate = Float.parseFloat(args[5]);
			numberOfCycles = Integer.parseInt(args[6]);
			nfolds = Integer.parseInt(args[7]);
			//balance = Boolean.parseBoolean(args[6]);
			reduceDimensionality = Boolean.parseBoolean(args[8]);
			System.out.println("Learning thr: "+learningThreshold+"\n"+"Learning rate: "+learningRate+"\n"+"N of cycles: "+numberOfCycles+"\n"+"N folds for validation: "+nfolds+"\n"+"Use dimensionality reduction: "+reduceDimensionality);
			System.out.println("Min neurons per layer String: "+args[9]);
			String minNeuronS [] = args[9].split("#");
			String maxNeuronS [] = args[10].split("#");
			String neuronStepS [] = args[11].split("#");
			minNeurons = new int[minNeuronS.length];
			maxNeurons = new int[maxNeuronS.length];
			neuronStep = new int[neuronStepS.length];
			for (int i=0;i<minNeurons.length;i++) {
				minNeurons[i] = Integer.parseInt(minNeuronS[i]);
				maxNeurons[i] = Integer.parseInt(maxNeuronS[i]);
				neuronStep[i] = Integer.parseInt(neuronStepS[i]);
			}
	
			
			System.out.println("Min neurons per layer: "+Arrays.toString(minNeurons));
			System.out.println("Max neurons per layer: "+Arrays.toString(maxNeurons));
			System.out.println("Neuron steps per layer: "+Arrays.toString(neuronStep));
			
			System.out.println("********************************");
		}
		
			//System.exit(0);
			
			System.out.println("-----TRAINING-----");
			File bestModel = ANNTraining.findOptimalModel(minNeurons, maxNeurons, neuronStep, 
					species, basePathOccurrences, basePathEnvironmentalFeatures, nfolds, 
					learningThreshold, numberOfCycles, learningRate, balance,reduceDimensionality);
			
			System.out.println("-----PROJECTING-----");
			ANNEcologicalNicheModel annENM = new ANNEcologicalNicheModel(bestModel);
			File  projectedAnnENM = annENM.ENM(basePathEnvironmentalFeatures,reduceDimensionality);
			File projectedAnnENMAscFile = new File(projectedAnnENM.getAbsolutePath().replace(".csv", ".asc"));
			
			System.out.println("-----FINALISING-----");
			
			//release best model parameters and projected ann enm
			File output1 = new File("BestModelParameters.txt");
			FileUtils.copyFile(bestModel, output1);
			System.out.println("Parameters: "+output1.getName()+" size:"+output1.length());
			File output2 = new File("ANN_SDM.asc");
			FileUtils.copyFile(projectedAnnENMAscFile, output2);
			System.out.println("ASC: "+output2.getName()+" size:"+output2.length());
			File output3 = new File("ANN_SDM.ann");
			FileUtils.copyFile(annENM.preTrainedANN, output3);
			System.out.println("ANN: "+output3.getName()+" size:"+output3.length());
			File output4 = new File("ANN_SDM.fe");
			FileUtils.copyFile(annENM.preTrainedFE, output4);
			System.out.println("FE: "+output4.getName()+" size:"+output4.length());
			
			//destroy temp directories
			FileUtils.deleteDirectory(basePathOccurrences);
			FileUtils.deleteDirectory(basePathEnvironmentalFeatures);
			
			System.out.println("-----END-----");
	}
	
	public static void unzip(Path path) throws IOException{
	    //String fileBaseName = FilenameUtils.getBaseName(path.getFileName().toString());
	    Path destFolderPath = path.toFile().getParentFile().toPath();//Paths.get(path.getParent().toString(), fileBaseName);

	    try (ZipFile zipFile = new ZipFile(path.toFile(), ZipFile.OPEN_READ)){
	        Enumeration<? extends ZipEntry> entries = zipFile.entries();
	        while (entries.hasMoreElements()) {
	            ZipEntry entry = entries.nextElement();
	            Path entryPath = destFolderPath.resolve(entry.getName());
	            InputStream in = zipFile.getInputStream(entry);
                OutputStream out = new FileOutputStream(entryPath.toFile());
                IOUtils.copy(in, out);
                
                /*
	            if (entry.isDirectory()) {
	                Files.createDirectories(entryPath);
	            } else {
	                Files.createDirectories(entryPath.getParent());
	                try (InputStream in = zipFile.getInputStream(entry)){
	                    try (OutputStream out = new FileOutputStream(entryPath.toFile())){
	                        IOUtils.copy(in, out);                          
	                    }
	                }
	            }
	            */
	        }
	    }
	}
	
	
}

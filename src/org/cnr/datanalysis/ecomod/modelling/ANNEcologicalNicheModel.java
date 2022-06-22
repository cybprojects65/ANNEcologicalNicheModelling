package org.cnr.datanalysis.ecomod.modelling;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.List;
import java.util.Properties;

import org.cnr.datanalysis.ecomod.featureextraction.Observations;
import org.cnr.datanalysis.ecomod.featureextraction.OccurrenceEnrichment;
import org.gcube.ann.feedforwardann.DichotomicANN;

import it.cnr.raster.asc.processing.generalpurpose.CSVToASCConverter;

public class ANNEcologicalNicheModel {

	public static void main(String args[]) throws Exception{
		File provenanceFile = new File ("./trainingsets\\Carcharodon carcharias_008f10fc-9cc9-427a-bb8d-d56cdd68d024\\Parameters.txt");
		//File projectionFile = new File ("./trainingsets\\Carcharodon carcharias_90n1L\\carcharodon_carcharias.csv");
		File environmentalFiles = new File ("./environmentalfeatures/Global/");
		
		ANNEcologicalNicheModel annENM = new ANNEcologicalNicheModel(provenanceFile);
		//direct projection
		//File projectedAnnENM = annENM.project(projectionFile);
		File  projectedAnnENM = annENM.ENM(environmentalFiles);
		System.out.println("ANN projection is in "+projectedAnnENM.getAbsolutePath());
		System.out.println("ASC ANN projection is in "+projectedAnnENM.getAbsolutePath().replace(".csv", ".asc"));
	}
	
	public File preTrainedANN;
	public double spatialResolution;
	
	public ANNEcologicalNicheModel(File provenanceFile) throws Exception{
		this.preTrainedANN = getANNFile(provenanceFile);
	}
	
	public File ENM(File basePathEnvironmentalFeatures) throws Exception{
		File featureFile = generateFeatureFile(basePathEnvironmentalFeatures,preTrainedANN.getParentFile());
		File projection = project(featureFile);
		File newProjectionFolder = new File(projection.getParentFile(),"ENM");
		
		File newProjectionFile = new File(newProjectionFolder,"ANN_EMN.csv");
		if (!newProjectionFolder.exists())
			newProjectionFolder.mkdir();
		
		List<String> allLines = Files.readAllLines(projection.toPath());
		int linecounter = 0;
		BufferedWriter bw = new BufferedWriter(new FileWriter(newProjectionFile));
		bw.write("latitude,longitude,value\n");
		for (String l:allLines) {
			if (linecounter>0) {
				String elems [] =l.split(",");
				String x = elems[0];
				String y = elems[1];
				String v = elems[elems.length-1];
				String toWrite = y+","+x+","+v;
				bw.write(toWrite+"\n");
			}
			linecounter++;
		}
		bw.close();
		
		CSVToASCConverter.CSV2ASC(newProjectionFolder.getAbsolutePath(), "_", "_", "enm",
				0, 0, spatialResolution, false);
		
		return newProjectionFile;
	}
	
	public File generateFeatureFile(File basePathEnvironmentalFeatures, File destinationFolder) throws Exception{
		File environmentalFeatureFile = new File(destinationFolder,basePathEnvironmentalFeatures.getName()+".csv");
		Observations observations = new Observations();
		observations.buildObservations(basePathEnvironmentalFeatures);
		spatialResolution = observations.obsResolution;
		
		OccurrenceEnrichment enricher = new OccurrenceEnrichment(observations);
		enricher.enrichOccurrences(basePathEnvironmentalFeatures);
		enricher.save(environmentalFeatureFile);
		
		System.out.println("Feature set file saved to "+environmentalFeatureFile.getAbsolutePath());
		return environmentalFeatureFile;
		
	}
	
	public static File getANNFile(File provenanceFile) throws Exception{
		Properties p = new Properties();
		p.load(new FileInputStream(provenanceFile));
		
		String pathToANN = p.getProperty("TRAINED_ANN");
		File f = new File(provenanceFile.getParent(), pathToANN);
		return f;
	}

	
	public File project(File fileWithCoordinatesAndFeatureColumns) throws Exception {
		BufferedReader br = new BufferedReader(new FileReader(fileWithCoordinatesAndFeatureColumns));
		String header = br.readLine();
		br.close();
		String elements[] = header.split(",");
		String inputColumns[] = new String[elements.length - 3]; // remove x,y,target columns
		for (int i = 2; i < elements.length - 1; i++) {
			inputColumns[i - 2] = elements[i];
		}
		DichotomicANN tester = new DichotomicANN();
		tester.test(fileWithCoordinatesAndFeatureColumns.getAbsolutePath(), preTrainedANN.getAbsolutePath(), inputColumns);
		return tester.outputTestProjection;
		
	}
	
}

package org.cnr.datanalysis.ecomod.experiments;

import java.io.File;

import org.cnr.datanalysis.ecomod.modelling.ANNEcologicalNicheModel;

public class ProjectENM {

	public static void main(String args[]) throws Exception{
		File provenanceFile = new File ("./trainingsets\\Carcharodon carcharias_f9b2cf88-6ff7-4292-b6ff-b4b0275a71d9\\Parameters.txt");
		File environmentalFiles = new File ("./environmentalfeatures/Global/");
		boolean reduceDimensionality = true;
		ANNEcologicalNicheModel annENM = new ANNEcologicalNicheModel(provenanceFile);
		
		File  projectedAnnENM = annENM.ENM(environmentalFiles, reduceDimensionality);
		
		System.out.println("ANN projection is in "+projectedAnnENM.getAbsolutePath());
		System.out.println("ASC ANN projection is in "+projectedAnnENM.getAbsolutePath().replace(".csv", ".asc"));
	}
}

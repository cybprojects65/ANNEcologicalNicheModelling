package org.cnr.datanalysis.ecomod.featureextraction;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.cnr.datanalysis.ecomod.featureselection.FeatureSelector;

import it.cnr.raster.asc.filemanagement.AscRaster;
import it.cnr.raster.asc.filemanagement.AscRasterManager;
import it.cnr.raster.asc.filemanagement.AscRasterReader;
import it.cnr.raster.asc.filemanagement.utils.Triple;

public class OccurrenceEnrichment implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	Observations observations;
	FeatureVector[] features;
	FeatureSelector featureSelector;
	
	public OccurrenceEnrichment(Observations observations) {

		this.observations = observations;

	}

	public void setObservations(Observations observations) {
		this.observations = observations;
	}
	
	public void enrichOccurrences(File featureFilesBasepath, boolean doFeatureSelection) throws Exception {

		File[] fileList = featureFilesBasepath.listFiles();
		List<File> viableFeatureFiles = new ArrayList<>();

		for (File f : fileList) {
			if (f.getName().toLowerCase().endsWith(".asc")) {
				viableFeatureFiles.add(f);
			}
		}

		features = new FeatureVector[observations.getNumberOfObservations()];

		int nFeatures = viableFeatureFiles.size();
		int featureCounter = 0;
		for (File f : viableFeatureFiles) {
			System.out.println("F" + featureCounter + ": " + f.getName());
			AscRaster asc = new AscRasterReader().readRaster(f.getAbsolutePath());
			int observationcounter = 0;
			for (Triple t : observations.observationset) {

				FeatureVector fv = features[observationcounter];
				if (fv == null) {
					fv = new FeatureVector(t.x, t.y, t.v, nFeatures);
				}

				Double value = getValueFromRaster(asc, t.x, t.y, true);
				fv.addFeature(value, featureCounter);
				features[observationcounter] = fv;
				observationcounter++;
			}

			featureCounter++;
		}
		
		discardIncompleteVectors();
		
		if (doFeatureSelection) {
			if (featureSelector==null) {
				featureSelector = new FeatureSelector();
				features = featureSelector.selectFeaturesWithPCA(features);
			}else
				features = featureSelector.resizeFeaturesWithPCA(features);
		}

	}

	void discardIncompleteVectors() {
		List<FeatureVector> flist = new ArrayList<>();
		int discarded = 0;
		for (int i = 0; i < features.length; i++) {
			FeatureVector f = features[i];
			if (f.incomplete) {
				System.out
						.println("Discarding point " + f.x + "," + f.y + " for incompleteness on F" + f.incompleteIdx);
				discarded++;
			} else
				flist.add(f);
		}
		
		System.out.println("Discarded "+discarded+" points over "+features.length+" ("+Math.round(discarded*100/features.length)+"%)");
		features = new FeatureVector[flist.size()];
		features = flist.toArray(features);
	}

	Double getValueFromRaster(AscRaster asc, double x, double y, boolean applycorrection) {
		double resolution = asc.cellsize;

		double longitude = x;
		double latitude = y;
		if (applycorrection) {
			longitude = x - (resolution / 2);
			latitude = y - (resolution / 2);
		}

		Double value = AscRasterManager.getValue(longitude, latitude, asc);

		if (value == Double.parseDouble(asc.NDATA)) {
			System.out.println("No data for "+x+","+y);
			value = null;
		}
		return value;
	}
	
	public void save(File csvFile) throws Exception{
		
		BufferedWriter bw = new BufferedWriter(new FileWriter(csvFile));
		
		FeatureVector f0 = features[0];
		int nFeatures = f0.features.length;
		
		String header = "longitude,latitude,";
		for (int i=0;i<nFeatures;i++) {
			header = header+"F"+i+",";
		}	
		header = header +"target";
		bw.write(header+"\n");
		for (int i = 0; i < features.length; i++) {
			FeatureVector f = features[i];
			String line = f.x+","+f.y+",";
			for (int j=0;j<nFeatures;j++) {
				line = line+f.features[j]+",";
			}
			line = line + f.ANNTarget;
			bw.write(line+"\n");
		}
		bw.close();
	}

}

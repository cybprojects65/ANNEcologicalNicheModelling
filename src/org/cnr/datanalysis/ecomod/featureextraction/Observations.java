package org.cnr.datanalysis.ecomod.featureextraction;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import it.cnr.raster.asc.filemanagement.AscRaster;
import it.cnr.raster.asc.filemanagement.AscRasterReader;
import it.cnr.raster.asc.filemanagement.utils.Triple;

public class Observations implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public List<Triple> observationset;

	public int getNumberOfObservations() {
		return observationset.size();
	}
	
	public List<Triple> getObservations() {
		return observationset;
	}
	
	public void buildObservations(String species, File basepath, boolean balance) throws Exception {

		File absencerecordsfile = new File(basepath, "Absence_" + species.replace(" ", "_") + ".csv");
		File presencerecordsfile = new File(basepath, "Presence_" + species.replace(" ", "_") + ".csv");

		observationset = new ArrayList<Triple>();

		try {
			addTriples(absencerecordsfile, 0,false);
		} catch (Exception e) {
			System.out.println("Error: species " + species + " does not have viable absence record file associated.");
		}
		try {
			addTriples(presencerecordsfile, 1,false);
		} catch (Exception e) {
			System.out.println("Error: species " + species + " does not have viable presence record file associated.");
		}
		
		if (balance)
			balanceAbsencePresence();

	}
	
	public double obsResolution = 0;
	public void buildObservations(File basePathEnvironmentalFeatures) throws Exception {

		File [] allFiles = basePathEnvironmentalFeatures.listFiles();
		AscRaster asc = null;
		File selfile = null;
		for (File af:allFiles) {
			if (af.getName().toLowerCase().endsWith(".asc"))
			{
				asc = new AscRasterReader().readRaster(af.getAbsolutePath());
				selfile = af;
				break;
			}
		}
		System.out.println("Taking extension from "+selfile.getName());
		obsResolution = asc.cellsize;
		observationset = new ArrayList<Triple>();
		double minLong = asc.xll+((double)asc.cellsize/2d);
		double minLat = asc.yll+((double)asc.cellsize/2d);
		int nLat = asc.rows;
		int nLong = asc.cols;
		for (int i = 0; i<nLat;i++) {
			double lat = ((double)i * (double)asc.cellsize)+minLat;
			
			for (int j = 0; j<nLong;j++) {
				
				double lon = ((double)j * (double)asc.cellsize)+minLong;
				
				observationset.add(new Triple(lon,lat,-1));
				
			}
			
		}
				

	}

	public void balanceAbsencePresence() throws Exception {
		
		int nabsence = 0;
		int npresence = 0;
		
		for (Triple obs:observationset) {
			
			if (obs.v == 0)
				nabsence++;
			else 
				npresence++;
			
		}
		
		
		int nbalance = 0;
		double targettolimit = 0d;
		if (nabsence>npresence) {
			nbalance = npresence;
			targettolimit = 0d;
			System.out.println("Balancing absence ("+nabsence+") with respect to presence ("+npresence+")");
		}
		else {
			System.out.println("Balancing presence ("+npresence+") with respect to absence ("+nabsence+")");
			nbalance = nabsence;
			targettolimit = 1d;
		}
		
		int balancecounter = 0;
		List<Triple> newobservationset = new ArrayList<>();
		
		for (Triple obs:observationset) {
			
			if (obs.v == targettolimit)
			{
				if (balancecounter<nbalance) {
					balancecounter++;
					newobservationset.add(obs);
				}
				
			}else {
				newobservationset.add(obs);
			}
		}
		observationset = null;
		observationset = newobservationset;

		
	}
	
	
	public void addTriples(File observationFile, int ANNtarget, boolean invertlonlat) throws Exception {

		List<String> lines = Files.readAllLines(observationFile.toPath());
		int i = 0;
		for (String line : lines) {
			if (i > 0) {
				if (line != null && line.trim().length() > 0) {
					String elements[] = line.split(",");

					int longidx = 1;
					int latidx = 2;
					if (invertlonlat) {
						longidx = 2;
						latidx = 1;
					}
					/*
					try {
						Double.parseDouble(elements[0]);
						longidx = 0;
						latidx = 1;
					} catch (Exception ee) {
					}
					*/
					double longitude = Double.parseDouble(elements[longidx]);
					double latitude = Double.parseDouble(elements[latidx]);
					boolean alreadypresent = false;
					for (Triple t:observationset) {
						if (t.x == longitude && t.y == latitude) {
							alreadypresent = true;
							break;
						}
					}
					if (!alreadypresent)
						observationset.add(new Triple(longitude, latitude, ANNtarget));
				}
			}

			i++;
		}

	}

}

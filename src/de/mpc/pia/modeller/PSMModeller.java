package de.mpc.pia.modeller;

import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBException;

import org.apache.log4j.Logger;

import uk.ac.ebi.jmzidml.model.mzidml.Affiliation;
import uk.ac.ebi.jmzidml.model.mzidml.AnalysisCollection;
import uk.ac.ebi.jmzidml.model.mzidml.AnalysisProtocolCollection;
import uk.ac.ebi.jmzidml.model.mzidml.AnalysisSoftware;
import uk.ac.ebi.jmzidml.model.mzidml.AnalysisSoftwareList;
import uk.ac.ebi.jmzidml.model.mzidml.AuditCollection;
import uk.ac.ebi.jmzidml.model.mzidml.ContactRole;
import uk.ac.ebi.jmzidml.model.mzidml.Cv;
import uk.ac.ebi.jmzidml.model.mzidml.CvList;
import uk.ac.ebi.jmzidml.model.mzidml.CvParam;
import uk.ac.ebi.jmzidml.model.mzidml.DBSequence;
import uk.ac.ebi.jmzidml.model.mzidml.InputSpectra;
import uk.ac.ebi.jmzidml.model.mzidml.Inputs;
import uk.ac.ebi.jmzidml.model.mzidml.ModificationParams;
import uk.ac.ebi.jmzidml.model.mzidml.Organization;
import uk.ac.ebi.jmzidml.model.mzidml.Param;
import uk.ac.ebi.jmzidml.model.mzidml.ParamList;
import uk.ac.ebi.jmzidml.model.mzidml.PeptideEvidence;
import uk.ac.ebi.jmzidml.model.mzidml.PeptideEvidenceRef;
import uk.ac.ebi.jmzidml.model.mzidml.Person;
import uk.ac.ebi.jmzidml.model.mzidml.Provider;
import uk.ac.ebi.jmzidml.model.mzidml.Role;
import uk.ac.ebi.jmzidml.model.mzidml.SearchDatabase;
import uk.ac.ebi.jmzidml.model.mzidml.SearchDatabaseRef;
import uk.ac.ebi.jmzidml.model.mzidml.SearchModification;
import uk.ac.ebi.jmzidml.model.mzidml.SequenceCollection;
import uk.ac.ebi.jmzidml.model.mzidml.SourceFile;
import uk.ac.ebi.jmzidml.model.mzidml.SpectraData;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIdentification;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIdentificationItem;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIdentificationList;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIdentificationProtocol;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIdentificationResult;
import uk.ac.ebi.jmzidml.model.mzidml.UserParam;
import uk.ac.ebi.jmzidml.xml.io.MzIdentMLMarshaller;
import uk.ac.ebi.pride.jmztab.model.CVParam;
import uk.ac.ebi.pride.jmztab.model.FixedMod;
import uk.ac.ebi.pride.jmztab.model.MZBoolean;
import uk.ac.ebi.pride.jmztab.model.MZTabColumnFactory;
import uk.ac.ebi.pride.jmztab.model.MZTabConstants;
import uk.ac.ebi.pride.jmztab.model.MZTabDescription;
import uk.ac.ebi.pride.jmztab.model.Metadata;
import uk.ac.ebi.pride.jmztab.model.MsRun;
import uk.ac.ebi.pride.jmztab.model.PSM;
import uk.ac.ebi.pride.jmztab.model.Reliability;
import uk.ac.ebi.pride.jmztab.model.Section;
import uk.ac.ebi.pride.jmztab.model.SpectraRef;
import uk.ac.ebi.pride.jmztab.model.VariableMod;

import de.mpc.pia.intermediate.Accession;
import de.mpc.pia.intermediate.AccessionOccurrence;
import de.mpc.pia.intermediate.Group;
import de.mpc.pia.intermediate.Modification;
import de.mpc.pia.intermediate.PIAInputFile;
import de.mpc.pia.intermediate.Peptide;
import de.mpc.pia.intermediate.PeptideSpectrumMatch;
import de.mpc.pia.intermediate.xmlhandler.PIAIntermediateJAXBHandler;
import de.mpc.pia.modeller.psm.PSMExecuteCommands;
import de.mpc.pia.modeller.psm.PSMReportItem;
import de.mpc.pia.modeller.psm.PSMReportItemComparator;
import de.mpc.pia.modeller.psm.ReportPSM;
import de.mpc.pia.modeller.psm.ReportPSMSet;
import de.mpc.pia.modeller.report.SortOrder;
import de.mpc.pia.modeller.report.filter.AbstractFilter;
import de.mpc.pia.modeller.report.filter.FilterComparator;
import de.mpc.pia.modeller.report.filter.FilterFactory;
import de.mpc.pia.modeller.report.filter.psm.PSMScoreFilter;
import de.mpc.pia.modeller.report.filter.psm.PSMTopIdentificationFilter;
import de.mpc.pia.modeller.score.FDRData;
import de.mpc.pia.modeller.score.FDRScore;
import de.mpc.pia.modeller.score.ScoreModel;
import de.mpc.pia.modeller.score.ScoreModelEnum;
import de.mpc.pia.modeller.score.FDRData.DecoyStrategy;
import de.mpc.pia.modeller.score.comparator.RankCalculator;
import de.mpc.pia.modeller.score.comparator.ScoreComparator;
import de.mpc.pia.tools.PIAConstants;
import de.mpc.pia.tools.unimod.UnimodParser;
import de.mpc.pia.tools.unimod.jaxb.ModT;


/**
 * Modeller for the PSM related stuff.
 * 
 * @author julian
 *
 */
public class PSMModeller {
	
	/** logger for this class */
	private static final Logger logger = Logger.getLogger(PSMModeller.class);
	
	
	/** maps from the fileID to the {@link PIAInputFile}s, they are straight from the intermediateHandler */
	private Map<Long, PIAInputFile> inputFiles;
	
	/** maps from the string ID to the {@link SearchDatabase}s, they are straight from the intermediateHandler */
	private Map<String, SearchDatabase> searchDatabases;
	
	/** maps from the string ID to the {@link SpectraData} straight from the intermediateHandler */
	private Map<String, SpectraData> spectraData;
	
	/** maps from the string ID to the {@link AnalysisSoftware}s, they are straight from the intermediateHandler */
	private Map<String, AnalysisSoftware> analysisSoftware;
	
	/** the name of the PIA XML file */
	private String fileName;
	
	/** the number of PSMs in the intermediate file*/
	private Integer nrPSMs;
	
	/** maps from the spectrum ID in the PIA intermediate file to the report PSM */
	private Map<Long, ReportPSM> spectraPSMs;
	
	/** maps from the file ID to the List of {@link ReportPSM}s */
	private Map<Long, List<ReportPSM> > fileReportPSMs;
	
	/** List of the {@link ReportPSMSet}s for the whole file */
	private List<ReportPSMSet> reportPSMSets;
	
	/** map of the ReportPSMSets, for faster access in the other modellers, this is static per PIA XML file and global settings */
	private Map<String, ReportPSMSet> reportPSMSetMap;
	
	
	/** maps from the fileID to List of long names of the scores. Actually only a reference to the same field in the {@link PIAModeller}. */
	private Map<Long, List<String>> fileScoreNames;
	
	/** maps from the fileID to List of short names  of the scores. Actually only a reference to the same field in the {@link PIAModeller}. */
	private Map<Long, List<String>> fileScoreShortNames;
	
	/** mapping from fileId to the available sortings */
	private Map<Long, Set<String>> fileSortables;
	
	/** maps from the fileID to the corresponding FDR data */
	private Map<Long, FDRData> fileFDRData;
	
	/** maps from the fileID to whether an FDR is calculated or not */
	private Map<Long, Boolean> fileFDRCalculated;
	
	/** map from the file's id to the used top identifications for FDR calculation */
	private Map<Long, Integer> fileTopIdentifications;
	
	/** Maps, whether the file has PSMs with decoy information from the searchengine or otherwise in the PIA XML defined decoys or not */
	private Map<Long, Boolean> fileHasInternalDecoy;
	
	/** The warnings generated by the {@link PIAIntermediateJAXBHandler} for the PSMSetSettings */
	private Map<String, Set<Long>> psmSetSettingsWarnings;
	
	/** The represented {@link IdentificationKeySettings} in this Set are used for the calculation of PSM sets or not */
	private Map<String, Boolean> psmSetSettings;
	
	/** whether to create PSM sets at all or use every PSM independently (useful for data sets with several runs and one search engine) */
	private boolean createPSMSets;
	
	/** a list of score shortnames, representing the preferred score for the FDR calulation (if it is not manually set) */
	private List<String> preferredFDRScores;
	
	
	/** default decoy pattern */
	private String defaultDecoyPattern;
	
	/** default FDR threshold */
	private Double defaultFDRThreshold;
	
	/** default for the number of highest ranking PSMs (per spectrum) used for FDR calculation */
	private Integer defaultFDRTopIdentifications;
	
	
	/** the list of filters applied to the data, mapped by the file ID */
	private Map<Long, List<AbstractFilter>> fileFiltersMap;
	
	
	/** the PSMSetSettings for the SpectrumIdentificationResults in mzIdentML export */
	private Map<String, Boolean> resultPSMSetSettings;
	
	/** the PSMSetSettings for the SpectrumIdentificationItems in mzIdentML export */
	private Map<String, Boolean> itemPSMSetSettings;
	
	
	/**
	 * Basic constructor, creates the {@link ReportPSM}s and
	 * {@link ReportPSMSet}s from the given {@link Group}s. The {@link Group}s
	 * should derive from a {@link PIAInputFile}.
	 * 
	 * @param groups groups of the PIA intermediate file
	 * @param inputFiles the used {@link PIAInputFile}s
	 * @param fileScoreNames a map, which will be filled with the scoreNames for each file
	 * @param fileScoreShortNames a map, which will be filled with the scoreShortNames for each file
	 */
	public PSMModeller(Map<Long, Group> groups,
			Map<Long, PIAInputFile> inputFiles,
			Map<String, SearchDatabase> searchDatabases,
			Map<String, SpectraData> spectraData,
			Map<String, AnalysisSoftware> software,
			String fileName,
			Map<String, Set<Long>> psmSetSettingsWarnings,
			Integer nrPSMs) {
		
		// create the file mapping and also add the overview file with ID 0
		this.inputFiles = new HashMap<Long, PIAInputFile>(inputFiles.size()+1);
		this.inputFiles.put(0L, new PIAInputFile(0L, "All files",
				"Overview_of_all_files", "none"));
		this.inputFiles.putAll(inputFiles);
		
		fileFiltersMap = new HashMap<Long, List<AbstractFilter>>(inputFiles.size());
		
		this.searchDatabases = searchDatabases;
		this.spectraData = spectraData;
		this.analysisSoftware = software;
		this.fileName = fileName;
		
		// TODO: get defaults from ini-file (or something like that)
		defaultDecoyPattern = "s.*";
		defaultFDRThreshold = 0.01;
		defaultFDRTopIdentifications = 0;
		preferredFDRScores = new ArrayList<String>();
		
		// initialize the used PSM set settings
		this.psmSetSettingsWarnings = psmSetSettingsWarnings;
		this.psmSetSettings = new HashMap<String, Boolean>(IdentificationKeySettings.values().length);
		for (IdentificationKeySettings setting : IdentificationKeySettings.values()) {
			if ((psmSetSettingsWarnings.get(setting.toString()) != null) &&
					(psmSetSettingsWarnings.get(setting.toString()).size() > 0)) {
				this.psmSetSettings.put(setting.toString(), false);
			} else {
				this.psmSetSettings.put(setting.toString(), true);
			}
		}
		
		// remove redundant psmSetSettings (and use only the more failure tolerant ones)
		psmSetSettings =
				IdentificationKeySettings.noRedundantSettings(psmSetSettings);
		
		this.createPSMSets = true;
		if (this.createPSMSets) {
			this.psmSetSettings.remove(
					IdentificationKeySettings.FILE_ID.toString());
		}
		
		// no settings are needed for the calculation of the ReportPSMs, but the PSM Set settings are used
		this.nrPSMs = nrPSMs;
		createReportPSMsFromGroups(groups);
	}
	
	
	/**
	 * Applies the general settings and recalculates the PSMSets
	 */
	public void applyGeneralSettings(boolean createPSMSets,
			Map<String, Boolean> psmSetSettings) {
		// only do something, if it is needed
		if ((this.createPSMSets != createPSMSets) ||
				!this.psmSetSettings.equals(psmSetSettings)) {
			this.createPSMSets = createPSMSets;
			this.psmSetSettings = psmSetSettings;
			
			if (!this.createPSMSets) {
				// no sets across files needed -> put fileID into settings
				this.psmSetSettings.put(IdentificationKeySettings.FILE_ID.name(),
						true);
			}
			
			// rebuild the PSM sets
			List<AbstractFilter> filters = getFilters(0L);
			
			// map to create the PSMSets
			Map<String, List<ReportPSM>> psmSetsMap =
					new HashMap<String, List<ReportPSM>>();
			
			// sort the PSMs in sets with their identificationKeys
			for (ReportPSM psm : spectraPSMs.values()) {
				if (FilterFactory.satisfiesFilterList(psm, 0L, filters)) {
					String psmKey = psm.getIdentificationKey(this.psmSetSettings);
					
					// put the PSM in the psmKey -> ReportPSM map
					if (!psmSetsMap.containsKey(psmKey)) {
						psmSetsMap.put(psmKey, new ArrayList<ReportPSM>());
					}
					psmSetsMap.get(psmKey).add(psm);
				}
			}
			
			createReportPSMSets(psmSetsMap);
		}
	}
	
	
	/**
	 * Gets whether PSM sets should be used across files
	 * @return
	 */
	public Boolean getCreatePSMSets() {
		return createPSMSets;
	}
	
	
	/**
	 * Sets whether PSM sets should be used across files
	 * @param createPSMSets
	 */
	public void setCreatePSMSets(Boolean createPSMSets) {
		this.createPSMSets = createPSMSets;
	}
	
	
	/**
	 * Getter for the {@link IdentificationKeySettings}.
	 * @return
	 */
	public Map<String, Boolean> getPSMSetSettings() {
		return psmSetSettings;
	}
	
	
	/**
	 * Getter for the PSMSetSettingsWarnings
	 * @return
	 */
	public Map<String, Set<Long>> getPSMSetSettingsWarnings() {
		return psmSetSettingsWarnings;
	}
	
	
	/**
	 * This method creates for each {@link PeptideSpectrumMatch} in the
	 * given Map of {@link Group}s the corresponding {@link ReportPSM} and the
	 * List of {@link ReportPSMSet}s for the overview.
	 * 
	 * TODO: this could be run threaded (by PIA clusters)!
	 * 
	 * @return a mapping from the spectrum ID to the ReportPSM
	 */
	private void createReportPSMsFromGroups(Map<Long, Group> groups) {
		logger.info("createReportPSMsFromGroups started...");
		
		Integer psmsPerFile = nrPSMs / (inputFiles.size()-1);
		
		// reset the PSMs
		spectraPSMs = new HashMap<Long, ReportPSM>(nrPSMs);
		fileReportPSMs = new HashMap<Long, List<ReportPSM>>();
		
		// reset the scores
		fileScoreNames = new HashMap<Long, List<String>>();
		fileScoreShortNames = new HashMap<Long, List<String>>();
		
		// reset the available sortings
		fileSortables = new HashMap<Long, Set<String>>();
		fileSortables.put(0L, new HashSet<String>());
		
		// reset the FDR data
		fileFDRData = new HashMap<Long, FDRData>();
		fileFDRCalculated = new HashMap<Long, Boolean>();
		
		// reset the FDR data for the overview
		fileFDRData.put(0L, new FDRData(FDRData.DecoyStrategy.ACCESSIONPATTERN,
				defaultDecoyPattern, defaultFDRThreshold));
		fileFDRCalculated.put(0L, false);
		
		// reset the value for used top identifications in FDR calculation
		fileTopIdentifications = new HashMap<Long, Integer>();
		
		// reset internal decoy knowledge
		fileHasInternalDecoy = new HashMap<Long, Boolean>(inputFiles.size());
		for (Long fileID : inputFiles.keySet()) {
			if (fileID > 0) {
				fileHasInternalDecoy.put(fileID, false);
			}
		}
		
		
		// map to create the PSMSets
		Map<String, List<ReportPSM>> psmSetsMap =
				new HashMap<String, List<ReportPSM>>();
		
		// this map is used, to get the identification ranking for each score of a PSMs
		Map<Long, Map<String, Map<String, ArrayList<Double>>>> fileToRankings =
				new HashMap<Long, Map<String,Map<String, ArrayList<Double>>>>(); 
		
		
		// iterate through the groups
		long nrPSMs = 0;
		for (Map.Entry<Long, Group> groupIt : groups.entrySet()) {
			Map<String, Peptide> peptides = groupIt.getValue().getPeptides();
			
			// only groups with peptides can have PSMs
			if ((peptides != null) && (peptides.size() > 0)) {
				for (Map.Entry<String, Peptide> pepIt : peptides.entrySet()) {
					List<PeptideSpectrumMatch> spectra = pepIt.getValue().getSpectra();
					if (spectra != null) {
						for (PeptideSpectrumMatch spec : spectra) {
							ReportPSM psm = new ReportPSM(spec.getID(), spec);
							Long fileID = spec.getFile().getID();
							String psmKey = spec.getIdentificationKey(psmSetSettings);
							
							// add the accessions
							for (Accession acc
									: groupIt.getValue().getAllAccessions().values()) {
								// only add accession, if it was found in the spectrum's file
								if (acc.foundInFile(fileID)) {
									psm.addAccession(acc);
								}
							}
							
							if (spectraPSMs.put(spec.getID(), psm) != null) {
								// TODO: better warning
								logger.warn("psm with ID '"+spec.getID()+"' already in map");
							}
							
							
							// put the PSM in the fileID -> ReportPSMs mapping
							List<ReportPSM> filesPSMList = fileReportPSMs.get(fileID);
							if (filesPSMList == null) {
								filesPSMList = new ArrayList<ReportPSM>(psmsPerFile);
								fileReportPSMs.put(fileID, filesPSMList);
								
								// this file is new, so also add the scoreName-Maps and sorting maps
								fileScoreNames.put(fileID, new ArrayList<String>());
								fileScoreShortNames.put(fileID, new ArrayList<String>());
								
								fileSortables.put(fileID, new HashSet<String>());
								
								// also re-initialise the FDR data
								fileFDRData.put(fileID,
										new FDRData(FDRData.DecoyStrategy.ACCESSIONPATTERN,
												defaultDecoyPattern,
												defaultFDRThreshold));
								fileFDRCalculated.put(fileID, false);
								
								//and the topIdentifications
								fileTopIdentifications.put(fileID,
										defaultFDRTopIdentifications);
							}
							filesPSMList.add(psm);
							
							// also put the PSM in the psmKey -> ReportPSM map, which is needed for the creation of the ReportPSMSets
							List<ReportPSM> psmSets = psmSetsMap.get(psmKey);
							if (psmSets == null) {
								psmSets = new ArrayList<ReportPSM>();
								psmSetsMap.put(psmKey, psmSets);
							}
							psmSets.add(psm);
							
							
							// record everything needed for the identification ranking
							Map<String, Map<String, ArrayList<Double>>> keysToScores = fileToRankings.get(fileID);
							if (keysToScores == null) {
								keysToScores = new HashMap<String, Map<String, ArrayList<Double>>>(psmsPerFile);
								fileToRankings.put(fileID, keysToScores);
							}
							
							
							String scoreKey = createPSMScoreKey(psm);
							Map<String, ArrayList<Double>> scoreshortsToValues = keysToScores.get(scoreKey);
							if (scoreshortsToValues == null) {
								scoreshortsToValues = new HashMap<String, ArrayList<Double>>();
								keysToScores.put(scoreKey, scoreshortsToValues);
							}
							
							for (ScoreModel score : psm.getScores()) {
								// add the scorenames, if not yet done, and take the values for topIdentificationRanking
								List<String> scoreShortNames = fileScoreShortNames.get(fileID);
								if (!scoreShortNames.contains(score.getShortName())) {
									scoreShortNames.add(score.getShortName());
									fileScoreNames.get(fileID).add(score.getName());
									
									// add score to the available sortings
									String scoreSortName =
											PSMReportItemComparator.getScoreSortName(score.getShortName());
									if (scoreSortName != null) {
										fileSortables.get(fileID).add(scoreSortName);
									}
								}
								
								ArrayList<Double> scoreValues =  scoreshortsToValues.get(score.getShortName());
								if (scoreValues == null) {
									scoreValues = new ArrayList<Double>(3);
									scoreshortsToValues.put(score.getShortName(), scoreValues);
								}
								
								scoreValues.add(score.getValue());
							}
							
							
							if (!fileHasInternalDecoy.get(fileID) && 
									(spec.getIsDecoy() != null) &&
									spec.getIsDecoy()) {
								fileHasInternalDecoy.put(fileID, true);
							}
							
							nrPSMs++;
							if (nrPSMs % 100000 == 0) {
								logger.info(nrPSMs + " PSMs done");
							}
						}
					}
				}
			}
		}
		
		
		
		// create and fill the ReportPSMSets for the overview
		createReportPSMSets(psmSetsMap);
		
		// now sort all the PSMs' scores for ranking extraction
		for (Map<String, Map<String, ArrayList<Double>>> keysToScores
				: fileToRankings.values()) {
			for (Map<String, ArrayList<Double>> scoreshortsToValues
					: keysToScores.values()) {
				for (Map.Entry<String, ArrayList<Double>> score
						: scoreshortsToValues.entrySet()) {
					ScoreModelEnum model = ScoreModelEnum.getModelByDescription(score.getKey());
					
					// only sort, if we know how
					if (!model.equals(ScoreModelEnum.UNKNOWN_SCORE)) {
						Collections.sort(score.getValue());
						
						if (model.higherScoreBetter()) {
							Collections.reverse(score.getValue());
						}
					}
				}
			}
		}
		
		// now go through the PSMs again and get their score rankings
		for (ReportPSM psm : spectraPSMs.values()) {
			String scoreKey = createPSMScoreKey(psm);
			
			Map<String, ArrayList<Double>> scores =
					fileToRankings.get(psm.getFileID()).get(scoreKey);
			
			for (ScoreModel model : psm.getScores()) {
				if (!model.getType().equals(ScoreModelEnum.UNKNOWN_SCORE)) {
					psm.setIdentificationRank(model.getShortName(),
							scores.get(model.getShortName()).indexOf(model.getValue()) + 1);
				} else {
					// unsortable score gets -1 as rank
					psm.setIdentificationRank(model.getShortName(), -1);
				}
			}
		}
		
		logger.info("createReportPSMsFromGroups done.");
	}
	
	
	/**
	 * Creates the {@link ReportPSMSet}s, given the {@link ReportPSM}s in a
	 * mapping from the PSM-identificationKeys
	 * @param psmSetsMap
	 */
	private void createReportPSMSets(Map<String, List<ReportPSM>> psmSetsMap) {
		reportPSMSetMap = new HashMap<String, ReportPSMSet>(psmSetsMap.size());
		
		for (Map.Entry<String, List<ReportPSM>> psmSetsIt : psmSetsMap.entrySet()) {
			ReportPSMSet psmSet =
					new ReportPSMSet(psmSetsIt.getValue(), psmSetSettings);
			reportPSMSetMap.put(psmSetsIt.getKey(), psmSet);
		}
		
		reportPSMSets = new ArrayList<ReportPSMSet>(
				reportPSMSetMap.values());
		
		fileFDRData.put(0L,
				new FDRData(fileFDRData.get(0L).getDecoyStrategy(),
						fileFDRData.get(0L).getDecoyPattern(),
						fileFDRData.get(0L).getFDRThreshold()));
	}
	
	
	/**
	 * Returns a key by which the PSMs can be grouped for calculation of top
	 * rank identifications.
	 * 
	 * @param psm
	 * @return
	 */
	private String createPSMScoreKey(ReportPSM psm) {
		return psm.getSourceID() + ":" + psm.getSpectrum().getSpectrumTitle() +
				":" + psm.getMassToCharge() + ":" + psm.getSpectrum().getRetentionTime();
	}
	
	
	/**
	 * Getter for the files used in the PIA intermediate file, including the
	 * pseudo-overview-file.
	 * 
	 * @return
	 */
	public Map<Long, PIAInputFile> getFiles() {
		return inputFiles;
	}
	
	
	/**
	 * Getter for the filename of the PIA XML file.
	 * 
	 * @return
	 */
	public String getFileName() {
		return fileName;
	}
	
	
	/**
	 * Getter for the default decoy pattern.
	 * @return
	 */
	public String getDefaultDecoyPattern() {
		return defaultDecoyPattern;
	}
	
	
	/**
	 * Getter for the default FDR threshold for acceptance.
	 * @return
	 */
	public Double getDefaultFDRThreshold() {
		return defaultFDRThreshold;
	}
	
	
	/**
	 * Getter for the default number of highest ranking PSMs (per spectrum) used
	 * for FDR calculation.
	 * @return
	 */
	public Integer getDefaultFDRTopIdentifications() {
		return defaultFDRTopIdentifications;
	}
	
	
	/**
	 * Reports the mapping from the ReportPSMSet identificationKeys to the
	 * {@link ReportPSMSet}s. This map includes all possible, unfiltered PSM
	 * sets.
	 * 
	 * @return
	 */
	public Map<String, ReportPSMSet> getReportPSMSets() {
		return reportPSMSetMap;
	}
	
	
	/**
	 * Returns the number of PSMs or PSM sets for the given file ID. 
	 * @param fileID
	 * @return
	 */
	public int getNrReportPSMs(Long fileID) {
		if (fileID > 0) {
			if (fileReportPSMs.containsKey(fileID)) {
				return fileReportPSMs.get(fileID).size();
			} else {
				return -1;
			}
		} else {
			return reportPSMSets.size();
		}
	}
	
	
	/**
	 * Returns the filtered List of {@link ReportPSM}s for the given fileID.
	 * 
	 * @param fileID
	 * @param filters
	 * @return
	 */
	public List<ReportPSM> getFilteredReportPSMs(Long fileID,
			List<AbstractFilter> filters) {
		if (fileReportPSMs.containsKey(fileID)) {
			return FilterFactory.applyFilters(fileReportPSMs.get(fileID),
					filters, fileID);
		} else {
			logger.error("There are no ReportPSMs for the fileID " + fileID);
			return new ArrayList<ReportPSM>(1);
		}
	}
	
	
	/**
	 * Returns the filtered List of {@link ReportPSMSet}s for the PSM sets,
	 * applying the given filters.
	 * 
	 * @param filters
	 * @return
	 */
	public List<ReportPSMSet> getFilteredReportPSMSets(
			List<AbstractFilter> filters) {
		List<ReportPSMSet> filteredPSMSets = new ArrayList<ReportPSMSet>();
		
		// the PSM sets need a special filtering, some of the sets can become empty, due to filters on PSM level
		for (ReportPSMSet psmSet : reportPSMSets) {
			if (FilterFactory.satisfiesFilterList(psmSet, 0L, filters)) {
				List<ReportPSM> psms = FilterFactory.applyFilters(
						psmSet.getPSMs(), filters);
				
				if (psms.size() > 0) {
					ReportPSMSet set = new ReportPSMSet(psms, psmSetSettings);
					set.copyInfo(psmSet);
					filteredPSMSets.add(set);
				}
			}
		}
		
		return filteredPSMSets;
	}
	
	
	/**
	 * Returns the Score name, given the scoreShortName.
	 * @param fileID
	 * @param shortName
	 * @return
	 */
	public String getScoreName(Long fileID, String shortName) {
		
		// the indizes of the scoreShortName and scoreName should be equal
		int index = -1;
		if (fileScoreShortNames.containsKey(fileID)) {
			List<String> scoreShorts = fileScoreShortNames.get(fileID);
			for (int i = 0; i < scoreShorts.size(); i++) {
				if (shortName.equals(scoreShorts.get(i))) {
					index = i;
					break;
				}
			}
		}
		
		if ((index >= 0) && (fileScoreNames.containsKey(fileID))) {
			return fileScoreNames.get(fileID).get(index);
		}
		
		return null;
	}
	
	
	/**
	 * Getter for the shortNames of all scores of the given file
	 * 
	 * @param fileID
	 * @return
	 */
	public List<String> getScoreShortNames(Long fileID) {
		if (fileScoreShortNames.containsKey(fileID)) {
			return fileScoreShortNames.get(fileID);
		} else {
			return new ArrayList<String>(1);
		}
	}
	
	
	/**
	 * Returns the {@link FDRData} for the file with the given ID.
	 * 
	 * @param fileID
	 * @return
	 */
	public FDRData getFilesFDRData(Long fileID) {
		return fileFDRData.get(fileID);
	}
	
	
	/**
	 * Getter for the map of FDR data
	 * @return
	 */
	public Map<Long, FDRData> getFileFDRData() {
		return fileFDRData;
	}
	
	
	/**
	 * Returns the set number of top identifications used for the FDR
	 * calculation for the given file.
	 * 
	 * @param fileID
	 * @return
	 */
	public Integer getFilesTopIdentifications(Long fileID) {
		return fileTopIdentifications.get(fileID);
	}
	
	
	/**
	 * Sets the number of top identifications used for the FDR calculation for
	 * the given file.
	 * 
	 * @param fileID
	 * @return
	 */
	public void setFilesTopIdentifications(Long fileID, Integer topIDs) {
		fileTopIdentifications.put(fileID, topIDs);
	}
	
	
	/**
	 * Sets the number of top identifications used for the FDR calculation for
	 * all files.
	 * 
	 * @param fileID
	 * @return
	 */
	public void setAllTopIdentifications(Integer topIDs) {
		for (Long fileID : fileTopIdentifications.keySet()) {
			setFilesTopIdentifications(fileID, topIDs);
		}
	}
	
	
	/**
	 * Sets the given pattern as the decoy pattern for all files' FDR data and
	 * sets the FDR decoy strategy to
	 * {@link FDRData.DecoyStrategy}.ACCESSIONPATTERN, unless "searchengine" is
	 * given as pattern, which will set
	 * {@link FDRData.DecoyStrategy}.SEARCHENGINE as decoy strategy.
	 * 
	 * @param pattern
	 */
	public void setAllDecoyPattern(String pattern) {
		FDRData.DecoyStrategy decoyStrategy;
		boolean setPattern;
		
		if (FDRData.DecoyStrategy.SEARCHENGINE.toString().equals(pattern)) {
			decoyStrategy = FDRData.DecoyStrategy.SEARCHENGINE;
			setPattern = false;
		} else {
			decoyStrategy = FDRData.DecoyStrategy.ACCESSIONPATTERN;
			setPattern = true;
		}
		
		for (FDRData fdrData : fileFDRData.values()) {
			fdrData.setDecoyStrategy(decoyStrategy);
			if (setPattern) {
				fdrData.setDecoyPattern(pattern);
			}
		}
	}
	
	
	/**
	 * Updates the {@link FDRData} for the given file.
	 * 
	 * @param fileID
	 * @return
	 */
	public void updateFilesFDRData(Long fileID, DecoyStrategy decoyStrategy,
			String decoyPattern, Double fdrThreshold, String scoreModelShort,
			Integer topIdentifications) {
		FDRData fdrData = fileFDRData.get(fileID);
		
		if (fdrData != null) {
			fdrData.setDecoyStrategy(decoyStrategy);
			fdrData.setDecoyPattern(decoyPattern);
			fdrData.setFDRThreshold(fdrThreshold);
			fdrData.setScoreShortName(scoreModelShort);
			
			setFilesTopIdentifications(fileID, topIdentifications);
			
			logger.info(fileID + "'s FDRData set to: " +
					fdrData.getDecoyStrategy() + ", " +
					fdrData.getDecoyPattern() + ", " +
					fdrData.getFDRThreshold() + ", " +
					fdrData.getScoreShortName() + ", " +
					getFilesTopIdentifications(fileID));
		} else {
			logger.error("No FDRData for file with ID " + fileID);
		}
	}
	
	
	/**
	 * Returns a List of scoreShortNames of available Scores for FDR calculation
	 * for the given file.
	 * 
	 * @param fileID
	 * @return
	 */
	public List<String> getFilesAvailableScoreShortsForFDR(Long fileID) {
		List<String> fdrScoreNames = new ArrayList<String>();
		
		// the overview is treated separately (it has no available scores, but only the FDRScore
		if (fileID > 0) {
			if (fileScoreShortNames.containsKey(fileID)) {
				List<String> scoreShorts = fileScoreShortNames.get(fileID);
				for (int i=0; i < scoreShorts.size(); i++) {
					if (!ScoreModelEnum.PSM_LEVEL_FDR_SCORE.isValidDescriptor(scoreShorts.get(i))) {
						// FDR score is not available for FDR calculation
						fdrScoreNames.add(scoreShorts.get(i));
					}
				}
			} else {
				logger.error("No scores available for FDR calculation for the file with ID "+fileID);
			}
		}
		
		return fdrScoreNames;
	}
	
	
	/**
	 * Returns a mapping from the file IDs to the scoreNames used for FDR
	 * calculation for each file.
	 * 
	 * @return
	 */
	public Map<Long, String> getFileIDsToScoreOfFDRCalculation() {
		Map<Long, String> filenameToScoreOfFDRCalculation = new HashMap<Long, String>(inputFiles.size()-1);
		
		for (PIAInputFile file : inputFiles.values()) {
			if (file.getID() != 0L) {
				String scoreName;
				Boolean fdrCalculated = fileFDRCalculated.get(file.getID());
				if (fdrCalculated) {
					FDRData fdrData = fileFDRData.get(file.getID());
					scoreName = ScoreModelEnum.getName(fdrData.getScoreShortName());
				} else {
					scoreName = "no FDR calculated";
				}
				
				filenameToScoreOfFDRCalculation.put(file.getID(), scoreName);
			}
		}
		
		return filenameToScoreOfFDRCalculation;
	}
	
	
	/**
	 * Returns, whether for the given file an FDR is calculated.
	 * @param fileID
	 * @return
	 */
	public Boolean isFDRCalculated(Long fileID) {
		return fileFDRCalculated.get(fileID);
	}
	
	
	/**
	 * Returns true, if all files have a calculated FDR.
	 * @return
	 */
	public Boolean getAllFilesHaveFDRCalculated() {
		for (PIAInputFile file : inputFiles.values()) {
			if ((file.getID() > 0) &&
					((fileFDRCalculated.get(file.getID()) == null) ||
					!fileFDRCalculated.get(file.getID()))) {
				return false;
			}
			
		}
		
		return true;
	}
	
	
	/**
	 * Returns whether the combined FDR Score is calculated. 
	 * @return
	 */
	public boolean isCombinedFDRScoreCalculated() {
		return ((reportPSMSets.size() > 0) &&
				(fileFDRCalculated.get(0L) != null) &&
				fileFDRCalculated.get(0L));
	}
	
	
	/**
	 * Returns, whether the file with the given ID has internal decoys, i.e.
	 * PSMs which are set to be decoys in the PIA XML file.
	 * 
	 * @param fileID
	 * @return
	 */
	public Boolean getFileHasInternalDecoy(Long fileID) {
		return fileHasInternalDecoy.get(fileID);
	}
	
	
	/**
	 * Updates the decoy states of the PSMs with the current settings from the
	 * file's FDRData.
	 * 
	 */
	public void updateDecoyStates(Long fileID) {
		FDRData fdrData = fileFDRData.get(fileID);
		
		logger.info("updateDecoyStates " + fileID);
		
		// select either the PSMs from the given file or all and calculate the fdr
		if (fdrData == null) {
			logger.error("No FDR settings given for file with ID=" + fileID);
			// TODO: throw an exception or something
			return;
		} else {
			Pattern p = Pattern.compile(fdrData.getDecoyPattern());
			
			if (fileID > 0) {
				// get a List of the ReportPSMs for FDR calculation
				List<ReportPSM> listForFDR = fileReportPSMs.get(fileID);
				
				if (listForFDR == null) {
					logger.error("No PSMs found for the file with ID=" + fileID);
					// TODO: throw an exception
					return;
				}
				
				for (ReportPSM psm : listForFDR) {
					// dump all FDR data
					psm.dumpFDRCalculation();
					psm.updateDecoyStatus(fdrData.getDecoyStrategy(), p);
				}
			} else {
				// set decoy information for PSM sets
				for (ReportPSMSet psmSet : reportPSMSets) {
					psmSet.dumpFDRCalculation();
					psmSet.updateDecoyStatus(fdrData.getDecoyStrategy(), p);
				}
			}
		}
	}
	
	
	/**
	 * Calculate the FDR for all files. <br/>
	 * If no score for the FDR calculation is given, use a default.
	 */
	public void calculateAllFDR() {
		for (Long fileID : fileReportPSMs.keySet()) {
			calculateFDR(fileID);
		}
	}
	
	
	/**
	 * Calculate the FDR for the file given by fileID
	 * 
	 * @param fileID
	 */
	public void calculateFDR(Long fileID) {
		FDRData fdrData = fileFDRData.get(fileID);
		
		// select either the PSMs from the given file or all and calculate the fdr
		if (fdrData == null) {
			logger.error("No FDR settings given for file with ID=" + fileID);
			// TODO: throw an exception
			return;
		} else {
			// if no score for FDR score is given, set either a preferred or the
			// first available from the set
			if (fdrData.getScoreShortName() == null) {
				// first look in the preferred scores
				for (String scoreShort : preferredFDRScores) {
					if (fileScoreShortNames.get(fileID).contains(scoreShort)) {
						fdrData.setScoreShortName(scoreShort);
						break;
					}
				}
				
				// if no score is set yet, take the first best score
				if (fdrData.getScoreShortName() == null) {
					fdrData.setScoreShortName(
							fileScoreShortNames.get(fileID).get(0));
				}
				
				logger.info("set the score for FDR calculation for fileID=" +
						fileID + ": " + fdrData.getScoreShortName());
			}
			
			// recalculate the decoy status (especially important, if decoy pattern was changed)
			updateDecoyStates(fileID);
			
			// get a List of the ReportPSMs for FDR calculation
			List<ReportPSM> listForFDR = fileReportPSMs.get(fileID);
			
			if (listForFDR == null) {
				logger.error("No PSMs found for the file with ID=" + fileID);
				// TODO: throw an exception
				return;
			}
			
			if ((fileTopIdentifications.get(fileID) != null) &&
					(fileTopIdentifications.get(fileID) > 0)) {
				
				logger.info("applying topIdentification filter: top " +
						fileTopIdentifications.get(fileID) + " for " +
						fdrData.getScoreShortName());
				
				for (ReportPSM psm : listForFDR) {
					// as the used ReportPSMs may change with the filter, clear all prior FDR information
					psm.dumpFDRCalculation();
				}
				
				// only the topIdentifications should be used, so a filter is needed
				List<AbstractFilter> topRankFilter = new ArrayList<AbstractFilter>(1);
				
				topRankFilter.add(new PSMTopIdentificationFilter(
						FilterComparator.less_equal,
						fileTopIdentifications.get(fileID),
						false,
						fdrData.getScoreShortName()));
				
				List<ReportPSM> filteredList = FilterFactory.applyFilters(
						listForFDR,
						topRankFilter,
						fileID);
				
				listForFDR = filteredList;
			}
			
			// calculate the FDR values
			fdrData.calculateFDR(listForFDR);
			
			// and also calculate the FDR score
			FDRScore.calculateFDRScore(listForFDR, fdrData);
			
			
			List<String> scoreShorts = fileScoreShortNames.get(fileID);
			if (!scoreShorts.contains(ScoreModelEnum.PSM_LEVEL_FDR_SCORE.getShortName())) {
				// add the FDR score to scores of this file
				scoreShorts.add(ScoreModelEnum.PSM_LEVEL_FDR_SCORE.getShortName());
				fileScoreNames.get(fileID).add(ScoreModelEnum.PSM_LEVEL_FDR_SCORE.getName());
				
				// and also to the sortable fields
				fileSortables.get(fileID).add(
						PSMReportItemComparator.getScoreSortName(ScoreModelEnum.PSM_LEVEL_FDR_SCORE.getShortName()) );
			}
			
			// the FDR for this file is calculated now
			fileFDRCalculated.put(fileID, true);
		}
	}
	
	
	/**
	 * Clears all preferred FDR scores.
	 */
	public void resetPreferredFDRScores() {
		preferredFDRScores.clear();
		
		// set all FDR data scoreshorts for the regular files to null
		for (Map.Entry<Long, FDRData> fdrIt : fileFDRData.entrySet()) {
			if (!fdrIt.getKey().equals(0L)) {
				fdrIt.getValue().setScoreShortName(null);
			}
		}
	}
	
	
	/**
	 * Adds the given scoreShortNames to the preferred FDR scores.
	 */
	public void addPreferredFDRScores(List<String> scoreShortNames) {
		for (String scoreShortName : scoreShortNames) {
			addPreferredFDRScore(scoreShortName);
		}
	}
	
	
	/**
	 * Adds the score with the given short to the preferred FDR scores. If the
	 * score does not exist, do nothing.
	 * 
	 * @param scoreShortName
	 */
	public void addPreferredFDRScore(String scoreShortName) {
		if (scoreShortName == null) {
			return;
		}
		
		// get the unique score shortName, if the score is known
		ScoreModelEnum model =
				ScoreModelEnum.getModelByDescription(scoreShortName);
		String shortName = model.getShortName();
		if (shortName == null) {
			// for an unknown score, take the given shortName
			shortName = scoreShortName;
		}
		
		if (!preferredFDRScores.contains(shortName)) {
			preferredFDRScores.add(shortName);
		}
	}
	
	
	/**
	 * Calculates the Combined FDR Score for the PSM sets in the overview 
	 */
	public void calculateCombinedFDRScore() {
		Map<String, List<ReportPSMSet>> fileLists = new HashMap<String, List<ReportPSMSet>>();
		String key;
		
		updateDecoyStates(0L);
		
		// first we need the Average FDR Score for each PSM set
		for (ReportPSMSet set : reportPSMSets) {
			set.calculateAverageFDRScore();
			
			// put the PSM set into the List, which holds the sets identified in the same files
			if (set.getPSMs().size() > 1) {
				
				Set<Long> files = new TreeSet<Long>();
				for (ReportPSM psm : set.getPSMs()) {
					files.add(psm.getFileID());
				}
				
				StringBuffer sbKey = new StringBuffer("");
				
				for (Long file : files) {
					if (sbKey.length() > 0) {
						sbKey.append(":");
					}
					sbKey.append(file);
				}
				key = sbKey.toString();
			} else {
				key = set.getPSMs().get(0).getFileID().toString();
			}
			
			if (!fileLists.containsKey(key)) {
				fileLists.put(key, new ArrayList<ReportPSMSet>());
			}
			
			fileLists.get(key).add(set);
		}
		
		
		
		// go through the search-engine-sets, sort by AFS and calculate combined FDR Score
		for (Map.Entry<String, List<ReportPSMSet>> seSetIt : fileLists.entrySet()) {
			logger.info("Calculation of Average FDR Score for " + seSetIt.getKey());
			
			Collections.sort(seSetIt.getValue(),
					new ScoreComparator<ReportPSMSet>(ScoreModelEnum.AVERAGE_FDR_SCORE.getShortName()));
			
			FDRData fdrData = fileFDRData.get(0L);
			
			fdrData.setScoreShortName(ScoreModelEnum.AVERAGE_FDR_SCORE.getShortName());
			fdrData.calculateFDR(seSetIt.getValue());
			
			FDRScore.calculateFDRScore(seSetIt.getValue(), fdrData);
		}
		
		
		// add to the sortable fields
		fileSortables.get(0L).add(
				PSMReportItemComparator.getScoreSortName(ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE.getShortName()) );
		
		// and add to the score fields
		fileScoreShortNames.put(0L, new ArrayList<String>(1));
		fileScoreShortNames.get(0L).add(ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE.getShortName());
		fileScoreNames.put(0L, new ArrayList<String>(1));
		fileScoreNames.get(0L).add(ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE.getName());
		
		
		// correct the numbers of decoys etc.
		int nrDecoys = 0;
		int nrTargets = 0;
		int nrItems = 0;
		int nrFDRGoodDecoys = 0;
		int nrFDRGoodTargets = 0;
		double thr = fileFDRData.get(0L).getFDRThreshold();
		
		for (ReportPSMSet set : reportPSMSets) {
			if (!set.getFDRScore().getValue().equals(Double.NaN)) {
				nrItems++;
				if (set.getIsDecoy()) {
					nrDecoys++;
					if (set.getFDRScore().getValue() <= thr) {
						nrFDRGoodDecoys++;
					}
				} else {
					nrTargets++;
					if (set.getFDRScore().getValue() <= thr) {
						nrFDRGoodTargets++;
					}
				}
			}
		}
		
		fileFDRData.get(0L).correctNumbers(nrDecoys, nrFDRGoodDecoys,
				nrFDRGoodTargets, nrItems, nrTargets);
		
		// the combined FDR is calculated now
		fileFDRCalculated.put(0L, true);
	}
	
	
    /**
     * Sorts the List of {@link ReportPSM}s of the file given by fileID with the
     * prior specified sorting parameters.
     */
    public void sortReport(Long fileID, List<String> sortOrders,
    		Map<String, SortOrder> sortables) {
		List<Comparator<PSMReportItem>> compares =
				new ArrayList<Comparator<PSMReportItem>>();
		
		for (String sortKey : sortOrders) {
			Comparator<PSMReportItem> comp = PSMReportItemComparator.getComparatorByName(
					sortKey,
					sortables.get(sortKey));
			
			if (comp != null) {
				compares.add( comp);
			} else {
				logger.error("no comparator found for " + sortKey);
			}
		}
		
    	if (fileID > 0) {
    		Collections.sort(fileReportPSMs.get(fileID),
    				PSMReportItemComparator.getComparator(compares));
    	} else {
    		Collections.sort(reportPSMSets,
    				PSMReportItemComparator.getComparator(compares));
    	}
    }
    
    
	/**
	 * Returns a List of scoreShortNames of available Scores for ranking.
	 * 
	 * @param fileID
	 * @return
	 */
	public List<String> getFilesAvailableScoreShortsForRanking(Long fileID) {
		List<String> rankingScoreNames = new ArrayList<String>();
		
		if (fileScoreShortNames.containsKey(fileID)) {
			for (String scoreShort : fileScoreShortNames.get(fileID)) {
				rankingScoreNames.add(scoreShort);
			}
		}
		
		if ((rankingScoreNames.size() < 1) && (fileID > 0)) {
			logger.error("No scores available for ranking for the file with ID "+fileID);
		}
		
		return rankingScoreNames;
	}
    
	
	/**
	 * Calculates the ranking for the given file and scoreShortName. If the
	 * filter List is not null or empty, the Report is filtered before ranking.
	 */
	public void calculateRanking(Long fileID, String rankableShortName,
			List<AbstractFilter> filters) {
		if ((rankableShortName == null) || rankableShortName.trim().equals("")) {
			logger.error("No score shortName given for ranking calculation.");
			return;
		}
		
		// first, dump all prior ranking
		List<?> reports;
		if (fileID > 0) {
			reports = fileReportPSMs.get(fileID);
		} else {
			reports = reportPSMSets;
		}
		if (reports != null) {
			for (Object obj : reports) {
				if (obj instanceof PSMReportItem) {
					((PSMReportItem) obj).setRank(-1L);
				}
			}
		}
		
		if (fileID > 0) {
			RankCalculator.calculateRanking(rankableShortName,
					FilterFactory.applyFilters(fileReportPSMs.get(fileID),
							filters, fileID),
					new ScoreComparator<ReportPSM>(rankableShortName));
		} else {
			RankCalculator.calculateRanking(rankableShortName,
					FilterFactory.applyFilters(reportPSMSets,
							filters, fileID),
					new ScoreComparator<ReportPSMSet>(rankableShortName));
		}
	}
	
	
	/**
	 * Returns the filters set for the given file.
	 * @param fileID
	 * @return
	 */
	public List<AbstractFilter> getFilters(Long fileID) {
		List<AbstractFilter> filters = fileFiltersMap.get(fileID);
		if (filters == null) {
			filters = new ArrayList<AbstractFilter>();
			fileFiltersMap.put(fileID, filters);
		}
		
		return filters;
	}
	
	
	/**
	 * Add a new filter for the given file
	 */
	public boolean addFilter(Long fileID, AbstractFilter newFilter) {
		if (newFilter != null) {
			return getFilters(fileID).add(newFilter);
		} else {
			return false;
		}
	}
	
	
	/**
	 * Removes the filter from the given file at the given index.
	 * @param fileID
	 * @param removingIndex
	 * @return
	 */
	public AbstractFilter removeFilter(Long fileID, int removingIndex) {
		List<AbstractFilter> filters = getFilters(fileID);
		
		if ((removingIndex >= 0) &&
				(filters != null) &&
				(removingIndex < filters.size())) {
			return filters.remove(removingIndex);
		}
		
		return null;
	}
	
	
	/**
	 * Calculates the data for a histogram of the distribution of the PPM
	 * divergence. If fdrGood is true, only the FDR good target PSM (sets) are
	 * taken into account.
	 * 
	 * @param fileID
	 * @param fdrGood whether to use only the FDR good target PSM(set)s
	 * @return
	 */
	public List<List<Integer>> getPPMs(Long fileID, boolean fdrGood) {
		if (fdrGood && !isFDRCalculated(fileID)) {
			List<List<Integer>> labelled = new ArrayList<List<Integer>>();
			labelled.add(new ArrayList<Integer>());
			labelled.add(new ArrayList<Integer>());
			return labelled;
		}
		
		Map<Integer, Integer> ppmMap = new HashMap<Integer, Integer>();
		int counted = 0;
		int label_max = 0;
		int label_min = 0;
		
		// put the PPMs in 1-PPM bins in the map
		if (fileID > 0) {
			for (ReportPSM psm : fileReportPSMs.get(fileID)) {
				if (!fdrGood ||
						(!psm.getIsDecoy() && psm.getIsFDRGood())) {
					Integer label = (int)Math.floor(psm.getDeltaPPM() + 0.5d);
					
					if (!ppmMap.containsKey(label)) {
						ppmMap.put(label, 0);
					}
					ppmMap.put(label, ppmMap.get(label) + 1);
					counted++;
					
					if (label < label_min) {
						label_min = label;
					}
					if (label > label_max) {
						label_max = label;
					}
				}
			}
		} else {
			for (ReportPSMSet psm : reportPSMSets) {
				if (!fdrGood ||
						(!psm.getIsDecoy() && psm.getIsFDRGood())) {
					Integer label = (int)Math.floor(psm.getDeltaPPM() + 0.5d);
					
					if (!ppmMap.containsKey(label)) {
						ppmMap.put(label, 0);
					}
					ppmMap.put(label, ppmMap.get(label) + 1);
					counted++;
					
					if (label < label_min) {
						label_min = label;
					}
					if (label > label_max) {
						label_max = label;
					}
				}
			}
		}
		
		// create the PPM counts and labels list from the map
		List<Integer> ppms = new ArrayList<Integer>();
		List<Integer> labels = new ArrayList<Integer>();
		int drawn = 0;
		
		labels.add(0);
		if (ppmMap.containsKey(0)) {
			ppms.add(ppmMap.get(0));
			drawn += ppmMap.get(0);
		} else {
			ppms.add(0);
		}
		
		int i;
		for (i=1; (i < 10) && (drawn < 0.995 * counted); i++) {
			labels.add(i);
			if (ppmMap.containsKey(i)) {
				ppms.add(ppmMap.get(i));
				drawn += ppmMap.get(i);
			} else {
				ppms.add(0);
			}
			
			labels.add(0, -i);
			if (ppmMap.containsKey(-i)) {
				ppms.add(0, ppmMap.get(-i));
				drawn += ppmMap.get(-i);
			} else {
				ppms.add(0, 0);
			}
		}
		
		// all above
		for (drawn=0; i < label_max; i++) {
			if (ppmMap.containsKey(i)) {
				drawn += ppmMap.get(i);
			}
		}
		labels.add(null);
		ppms.add(drawn);
		
		// all below
		for (i=label_min, drawn=0; i < labels.get(0); i++) {
			if (ppmMap.containsKey(i)) {
				drawn += ppmMap.get(i);
			}
		}
		labels.add(0, null);
		ppms.add(0, drawn);
		
		List<List<Integer>> labelled = new ArrayList<List<Integer>>();
		labelled.add(ppms);
		labelled.add(labels);
		return labelled;
	}
	
	
	
	/**
	 * Returns, how many times a PSM set had how many identifications. The first
	 * entry in the list represents one identification, the second two...
	 * 
	 * @param fdrGood
	 * @return
	 */
	public List<Integer> getNrIdentifications(boolean fdrGood) {
		if (fdrGood && !isCombinedFDRScoreCalculated()) {
			List<Integer> IDs = new ArrayList<Integer>(1);
			IDs.add(0);
			return IDs;
		}
		
		Map<Integer, Integer> idMap = new HashMap<Integer, Integer>();
		int max_ids = 0;
		
		// count the number of identifications
		for (ReportPSMSet psm : reportPSMSets) {
			if (!fdrGood ||
					(!psm.getIsDecoy() && psm.getIsFDRGood())) {
				Integer ids = psm.getPSMs().size();
				
				if (!idMap.containsKey(ids)) {
					idMap.put(ids, 1);
				} else {
					idMap.put(ids, idMap.get(ids) + 1);
				}
				
				if (ids > max_ids) {
					max_ids = ids;
				}
			}
		}
		
		List<Integer> IDs = new ArrayList<Integer>(max_ids);
		for (int i=1; i <= max_ids; i++) {
			if (idMap.containsKey(i)) {
				IDs.add(idMap.get(i));
			} else {
				IDs.add(0);
			}
		}
		
		return IDs;
	}
	
	
    /**
     * Writes the PSM report for the file with the given ID and filtered with
     * the given filters in a loose CSV format.<br/>
     * If the export is for spectral counting, the filters are applied for every
     * file instead of the set overview (only important for the overview and).
     * 
     * @param writer
     * @param fileID
     * @throws IOException
     */
	public void exportCSV(Writer writer, Long fileID,
			boolean exportForSC, boolean filterExport) throws IOException {
		List<PSMReportItem> report;
		if (fileID > 0) {
			List<ReportPSM> rep = filterExport ? 
					getFilteredReportPSMs(fileID, getFilters(fileID)) :
					fileReportPSMs.get(fileID);
					
			report = new ArrayList<PSMReportItem>(rep.size());
			report.addAll(rep);
		} else {
			if (!exportForSC) {
				List<ReportPSMSet> rep = filterExport ? 
						getFilteredReportPSMSets(getFilters(fileID)) :
						reportPSMSets;
				report = new ArrayList<PSMReportItem>(rep.size());
				report.addAll(rep);
				
			} else {
				report = new ArrayList<PSMReportItem>();
				
				for (Long psmFileID: fileReportPSMs.keySet()) {
					if (getFilters(0L).size() > 0) {
						List<ReportPSM> part = filterExport ? 
								getFilteredReportPSMs(psmFileID, getFilters(0L)) :
								fileReportPSMs.get(psmFileID);
								
						report.addAll(part);
					} else {
						report.addAll(fileReportPSMs.get(psmFileID));
					}
				}
				
			}
		}
		
		if (!exportForSC) {
			if (fileID > 0) {
				writer.append(
						"\"sequence\";" +
						"\"modifications\";" +
						"\"charge\";" +
						"\"m/z\";" +
						"\"delta mass\";" +
						"\"delta ppm\";" +
						"\"retention time\";" +
						"\"missed\";" +
						"\"sourceID\";" +
						"\"accessions\";" +
						"\"scores\";" +
						"\"identification ranks\";" +
						"\"isDecoy\";" +
						"\"isUnique\";" +
						"\"isFDRGood\";" +
						"\n"
						);
			} else {
				writer.append(
						"\"sequence\";" +
						"\"modifications\";" +
						"\"charge\";" +
						"\"m/z\";" +
						"\"delta mass\";" +
						"\"delta ppm\";" +
						"\"retention time\";" +
						"\"missed\";" +
						"\"sourceID\";" +
						"\"accessions\";" +
						"\"scores\";" +
						"\"identification ranks\";" +
						"\"isDecoy\";" +
						"\"isFDRGood\";" +
						"\n"
						);
			}
		} else {
			// exportForSC is set
			writer.append(
					"\"accession\";" +
					"\"filename\";" +
					"\"sequence\";" +
					"\"modifications\";" +
					"\"charge\";" +
					"\"m/z\";" +
					"\"delta mass\";" +
					"\"delta ppm\";" +
					"\"retention time\";" +
					"\"missed\";" +
					"\"sourceID\";" +
					"\"spectrumTitle\";" +
					"\"scores\";" +
					"\"identification ranks\";" +
					"\"isDecoy\";" +
					"\"isUnique\";" +
					"\n"
					);
		}
		
		for (Object item : report) {
			if (!exportForSC) {
				if (item instanceof ReportPSM) {
					ReportPSM psm = (ReportPSM)item;
					
					StringBuffer accessionsSB = new StringBuffer();
					for (Accession acc : psm.getAccessions()) {
						if (accessionsSB.length() > 0) {
							accessionsSB.append(",");
						}
						accessionsSB.append(acc.getAccession());
					}
					
					writer.append(
							"\"" + psm.getSequence() + "\";" +
							"\"" + psm.getModificationsString() + "\";" +
							"\"" + psm.getCharge() + "\";" +
							"\"" + psm.getMassToCharge() + "\";" +
							"\"" + psm.getDeltaMass() + "\";" +
							"\"" + psm.getDeltaPPM() + "\";" +
							"\"" + psm.getRetentionTime() + "\";" +
							"\"" + psm.getMissedCleavages() + "\";" +
							"\"" + psm.getSourceID() + "\";" +
							"\"" + accessionsSB.toString() + "\";" +
							"\"" + psm.getScores() + "\";" +
							"\"" + psm.getIdentificationRanks() + "\";" +
							"\"" + psm.getIsDecoy() + "\";" +
							"\"" + ((psm.getSpectrum().getIsUnique() != null) ? psm.getSpectrum().getIsUnique() : false) + "\";" +
							"\"" + psm.getIsFDRGood() + "\";" +
							"\n"
							);
				} else if (item instanceof ReportPSMSet) {
					ReportPSMSet psm = (ReportPSMSet)item;
					
					StringBuffer accessionsSB = new StringBuffer();
					for (Accession acc : psm.getAccessions()) {
						if (accessionsSB.length() > 0) {
							accessionsSB.append(",");
						}
						accessionsSB.append(acc.getAccession());
					}
					
					StringBuffer scores = new StringBuffer();
					StringBuffer idRanks = new StringBuffer();
					
					for (ReportPSM rp : psm.getPSMs()) {
						scores.append("[");
						scores.append(rp.getScores());
						scores.append("]");
						
						idRanks.append("[");
						idRanks.append(rp.getIdentificationRanks());
						idRanks.append("]");
					}
					
					writer.append(
							"\"" + psm.getSequence() + "\";" +
							"\"" + psm.getModificationsString() + "\";" +
							"\"" + psm.getCharge() + "\";" +
							"\"" + psm.getMassToCharge() + "\";" +
							"\"" + psm.getDeltaMass() + "\";" +
							"\"" + psm.getDeltaPPM() + "\";" +
							"\"" + psm.getRetentionTime() + "\";" +
							"\"" + psm.getMissedCleavages() + "\";" +
							"\"" + psm.getSourceID() + "\";" +
							"\"" + accessionsSB.toString() + "\";" +
							"\"" + scores.toString() + "\";" +
							"\"" + idRanks.toString() + "\";" +
							"\"" + psm.getIsDecoy() + "\";" +
							"\"" + psm.getIsFDRGood() + "\";" +
							"\n"
							);
				}
			} else {
				// spectral counting export
				if (item instanceof ReportPSM) {
					ReportPSM psm = (ReportPSM)item;
					
					String exportLine = 
							"\"" + psm.getFileName() +  "\";" +
							"\"" + psm.getSequence() + "\";" +
							"\"" + psm.getModificationsString() + "\";" +
							"\"" + psm.getCharge() + "\";" +
							"\"" + psm.getMassToCharge() + "\";" +
							"\"" + psm.getDeltaMass() + "\";" +
							"\"" + psm.getDeltaPPM() + "\";" +
							"\"" + psm.getRetentionTime() + "\";" +
							"\"" + psm.getMissedCleavages() + "\";" +
							"\"" + psm.getSourceID() + "\";" +
							"\"" + psm.getSpectrumTitle() + "\";" +
							"\"" + psm.getScores() + "\";" +
							"\"" + psm.getIdentificationRanks() + "\";" +
							"\"" + psm.getIsDecoy() + "\";" +
							"\"" + ((psm.getSpectrum().getIsUnique() != null) ? psm.getSpectrum().getIsUnique() : false) + "\";";
					
					for (Accession accession : psm.getAccessions()) {
						writer.append("\"" + accession.getAccession() + "\";");
						writer.append(exportLine);
						writer.append("\n");
					}
				}
			}
		}
		
		writer.flush();
	}
	
	
	/**
     * Writes the PSM report into mzIdentML.
     * 
     * @throws IOException
     */
	public void exportMzIdentML(Writer writer, Long fileID,
			Boolean filterExport) throws IOException {
		logger.info("start writing mzIdentML file");
		
		UnimodParser unimodParser;
		try {
			unimodParser = new UnimodParser();
		} catch (JAXBException e) {
			logger.error("Could not initialize the UnimodParser.", e);
			writer.flush();
			return;
		}
		
		MzIdentMLMarshaller m = new MzIdentMLMarshaller();
		
		// XML header
		writer.write(m.createXmlHeader() + "\n");
		writer.write(m.createMzIdentMLStartTag("PIAExport for PSMs") + "\n");
		
		
		// there are some variables needed for additional tags later
		Cv psiCV = new Cv();
		Cv unimodCV = new Cv();
		Cv unitCV = new Cv();
		Map<String, DBSequence> sequenceMap = new HashMap<String, DBSequence>();
		Map<String, uk.ac.ebi.jmzidml.model.mzidml.Peptide> peptideMap =
				new HashMap<String, uk.ac.ebi.jmzidml.model.mzidml.Peptide>();
		Map<String, PeptideEvidence> pepEvidenceMap =
				new HashMap<String, PeptideEvidence>();
		Map<Long, SpectrumIdentificationList> silMap =
				new HashMap<Long, SpectrumIdentificationList>();
		AnalysisSoftware piaAnalysisSoftware = new AnalysisSoftware();
		Inputs inputs = new Inputs();
		AnalysisProtocolCollection analysisProtocolCollection =
				new AnalysisProtocolCollection();
		AnalysisCollection analysisCollection = new AnalysisCollection();
		
		
		writeCommonMzIdentMLTags(writer, m, unimodParser,
				psiCV, unimodCV, unitCV,
				sequenceMap, peptideMap, pepEvidenceMap, silMap,
				new HashMap<String, SpectrumIdentificationItem>() /* not further needed here */,
				piaAnalysisSoftware, inputs,
				analysisProtocolCollection, analysisCollection,
				fileID, filterExport);
		
		
		
		m.marshal(analysisCollection, writer);
		writer.write("\n");
		
		m.marshal(analysisProtocolCollection, writer);
		writer.write("\n");
		
		writer.write(m.createDataCollectionStartTag() + "\n");
		
		m.marshal(inputs, writer);
		writer.write("\n");
		
		
		writer.write(m.createAnalysisDataStartTag() + "\n");
		
		for (SpectrumIdentificationList siList : silMap.values()) {
			m.marshal(siList, writer);
			writer.write("\n");
		}
		
		writer.write(m.createAnalysisDataClosingTag() + "\n");
		
		writer.write(m.createDataCollectionClosingTag() + "\n");
		
		writer.write(m.createMzIdentMLClosingTag());
		
		writer.flush();
		logger.info("writing of mzIdentML done");
	}
	
	
	/**
	 * Writes (and creates) the MzIdentML tags which are common for an export
	 * with and without the ProteinDetectionList.
	 * <p>
	 * All given parameters need only to be created with "new", they are filled
	 * in this procedure.
	 * <p>
	 * The mzIdentML file will be written up to the {@link SequenceCollection}.
	 * 
	 * @param writer the output writer, i.e. where the file goes
	 * @param m the used marshaller
	 * @param unimodParser the prior initialised unimodParser
	 * @param psiCV the PSI CV
	 * @param unimodCV the UniMod CV
	 * @param unitCV the unit CV
	 * @param sequenceMap a map holding the {@link DBSequence}s, mapping from
	 * their IDs
	 * @param peptideMap a map holding the {@link uk.ac.ebi.jmzidml.model.mzidml.Peptide}s,
	 * mapping from their IDs
	 * @param pepEvidenceMap a map holding the {@link PeptideEvidence}s, mapping
	 * from their IDs
	 * @param silMap a map holding the {@link SpectrumIdentificationList}s for
	 * each file, mapping from the fileID (0 for the PSM sets)
	 * @param combinedSiiMap map containing the {@link SpectrumIdentificationItem}s
	 * of the PSM sets
	 * @param piaAnalysisSoftware PIA as an {@link AnalysisSoftware}
	 * @param inputs the {@link Inputs} data
	 * @param analysisProtocolCollection
	 * @param analysisCollection
	 * @param fileID the ID of the file for the report (0 for overview AND all
	 * other together)
	 * @param psmFilterMap a map from the file ID to the applied filters, may be
	 * null (no filters set or for protein export) 
	 * @throws IOException
	 */
	public void writeCommonMzIdentMLTags(
			Writer writer, MzIdentMLMarshaller m, UnimodParser unimodParser,
			Cv psiCV, Cv unimodCV, Cv unitCV,
			Map<String, DBSequence> sequenceMap,
			Map<String, uk.ac.ebi.jmzidml.model.mzidml.Peptide> peptideMap,
			Map<String, PeptideEvidence> pepEvidenceMap,
			Map<Long, SpectrumIdentificationList> silMap,
			Map<String, SpectrumIdentificationItem> combinedSiiMap,
			AnalysisSoftware piaAnalysisSoftware,
			Inputs inputs,
			AnalysisProtocolCollection analysisProtocolCollection,
			AnalysisCollection analysisCollection,
			Long fileID, Boolean filterPSMs)
			throws IOException {
		
		// the CV list
		CvList cvList = new CvList();
		
		psiCV.setUri("http://psidev.cvs.sourceforge.net/viewvc/*checkout*/psidev/psi/psi-ms/mzML/controlledVocabulary/psi-ms.obo");
		psiCV.setId("PSI-MS");
		// this is the version used for programming the exporter
		psiCV.setVersion("3.54.0");
		psiCV.setFullName("PSI-MS");
        cvList.getCv().add(psiCV);
        
		unimodCV.setUri("http://www.unimod.org/obo/unimod.obo");
		unimodCV.setId("UNIMOD");
		unimodCV.setFullName("UNIMOD");
		cvList.getCv().add(unimodCV);
		
		unitCV.setUri("http://obo.cvs.sourceforge.net/*checkout*/obo/obo/ontology/phenotype/unit.obo");
		unitCV.setId("UO");
		unitCV.setFullName("UNIT-ONTOLOGY");
		cvList.getCv().add(unitCV);
		
		m.marshal(cvList, writer);
		writer.write("\n");
		
		
		// AnalysisSoftware
		AnalysisSoftwareList analysisSoftwareList = new AnalysisSoftwareList();
		
		piaAnalysisSoftware.setName("PIA");
		piaAnalysisSoftware.setId("AS_PIA");
		piaAnalysisSoftware.setVersion(PIAConstants.version);
		
		Param tempParam = new Param();
		CvParam tempCvParam = new CvParam();
		tempCvParam.setAccession(PIAConstants.CV_PIA_ACCESSION);
		tempCvParam.setName(PIAConstants.CV_PIA_NAME);
		tempCvParam.setCv(psiCV);
		tempParam.setParam(tempCvParam);
		piaAnalysisSoftware.setSoftwareName(tempParam);
		
		analysisSoftwareList.getAnalysisSoftware().add(piaAnalysisSoftware);
		
		for (AnalysisSoftware software
				: analysisSoftware.values()) {
			analysisSoftwareList.getAnalysisSoftware().add(software);
		}
		
		m.marshal(analysisSoftwareList, writer);
		writer.write("\n");
		
		
		
		// TODO: why is this mandatory? the documentation does not state it so!
		Provider provider = new Provider();
		provider.setId("PROVIDER");
		ContactRole contactRole = new ContactRole();
		Person docOwner = new Person();
		docOwner.setId("PERSON_DOC_OWNER");
		//docOwner.setFirstName(firstName);
		//docOwner.setLastName(lastName);
        //docOwner.setEmail(email);
		
		contactRole.setContact(docOwner);
		Role role = new Role();
		tempCvParam = new CvParam();
		tempCvParam.setAccession("MS:1001271");
		tempCvParam.setCv(psiCV);
		tempCvParam.setName("researcher");
		role.setCvParam(tempCvParam);
		contactRole.setRole(role);
		provider.setContactRole(contactRole);
		
        Organization org = new Organization();
        org.setId("ORG_DOC_OWNER");
        //org.setName(affiliationName);
        //org.setAddress(address);

        List<Affiliation> affList = docOwner.getAffiliation();
        Affiliation aff = new Affiliation();
        aff.setOrganization(org);
        affList.add(aff);
        
		AuditCollection auditCollection = new AuditCollection();
        auditCollection.getPersonOrOrganization().add(docOwner);
        auditCollection.getPersonOrOrganization().add(org);
        
		m.marshal(provider, writer);
		writer.write("\n");
		
		m.marshal(auditCollection, writer);
		writer.write("\n");
		
		// get the information from PIA XML file
		if (fileID > 0) {
			inputs.getSearchDatabase().add(searchDatabases.get(fileID));
			inputs.getSpectraData().add(spectraData.get(fileID));
		} else {
			inputs.getSearchDatabase().addAll(searchDatabases.values());
			inputs.getSpectraData().addAll(spectraData.values());
		}
		
		SourceFile sourceFile = new SourceFile();
		sourceFile.setId("SF_pia_xml");
		sourceFile.setLocation(fileName);
		sourceFile.setName("PIA-XML-file");
		sourceFile.setExternalFormatDocumentation(
				"http://medizinisches-proteom-center.de/index.php/software-top/165-protein-inference-algorithms-pia");
		// TODO: create the file format (needs CV entry)
		//FileFormat fileFormat = new FileFormat();
		//piaFile.setFileFormat(fileFormat);
		inputs.getSourceFile().add(sourceFile);
		
		
		Map<String, SpectrumIdentification> spectrumIdentificationMap =
				new HashMap<String, SpectrumIdentification>();
		// maps from the searchDB to the files, which use it
		Map<String, Set<Long>> dbsInFiles = new HashMap<String, Set<Long>>();
		
		List<PIAInputFile> fileList;
		if (fileID > 0) {
			fileList = new ArrayList<PIAInputFile>(1);
			
			if (fileReportPSMs.containsKey(fileID)) {
				fileList.add(inputFiles.get(fileID));
			}
		} else {
			fileList = new ArrayList<PIAInputFile>();
			
			for (PIAInputFile file : inputFiles.values()) {
				if (fileReportPSMs.containsKey(file.getID())) {
					fileList.add(file);
				}
			}
		}
		for (PIAInputFile file : fileList) {
			if (file.getAnalysisCollection() != null) {
				for (SpectrumIdentification specID
						: file.getAnalysisCollection().getSpectrumIdentification()) {
					for (SearchDatabaseRef ref
							: specID.getSearchDatabaseRef()) {
						Set<Long> files =
								dbsInFiles.get(ref.getSearchDatabaseRef());
						if (files == null) {
							files = new HashSet<Long>();
							dbsInFiles.put(ref.getSearchDatabaseRef(), files);
						}
						files.add(file.getID());
					}
					
					analysisCollection.getSpectrumIdentification().add(specID);
					spectrumIdentificationMap.put(specID.getId(), specID);
					
					SpectrumIdentificationList sil =
							new SpectrumIdentificationList();
					sil.setId("SIL_" + specID.getId());
					specID.setSpectrumIdentificationList(sil);
					
					silMap.put(file.getID(), sil);
				}
			}
		}
		
		// build up the DBSequence, Peptide, PeptideEvidence and SpectrumIdentificationLists now
		for (PIAInputFile file : fileList) {
			if (file.getID() > 0) {
				// TODO: the ranking is problematic... what score should be used for ranking?
				// most probable would be the one used for protein scoring or
				// used for FDRScore calculation.
				// if this is not valid, use the search engine main score
				String rankScoreShort = null;
				
				SpectrumIdentificationList sil = 
						createSpectrumIdentificationListForFile(file.getID(),
								sequenceMap, peptideMap, pepEvidenceMap,
								dbsInFiles, unimodParser, psiCV, unitCV,
								rankScoreShort,
								(fileID > 0) ? filterPSMs : false);
				
				if ((sil != null) && (silMap.containsKey(file.getID()))) {
					// use the already set ID
					sil.setId(silMap.get(file.getID()).getId());
				}
				
				silMap.put(file.getID(), sil);
			}
		}
		
		SequenceCollection sequenceCollection = new SequenceCollection();
		
		// add the DBSequences into their list
		for (DBSequence dbSequence : sequenceMap.values()) {
			sequenceCollection.getDBSequence().add(dbSequence);
		}
		
		// add the peptides in their list
		for (uk.ac.ebi.jmzidml.model.mzidml.Peptide peptide
				: peptideMap.values()) {
			sequenceCollection.getPeptide().add(peptide);
		}
		
		// add the peptideEvidences into their list
		for (PeptideEvidence pe : pepEvidenceMap.values()) {
			sequenceCollection.getPeptideEvidence().add(pe);
		}
		
		m.marshal(sequenceCollection, writer);
		writer.write("\n");
		
		
		// update and write the analysisCollection and the analysisProtocolCollection
		for (PIAInputFile file : fileList) {
			if (file.getID() > 0) {
				// add all needed spectrum identification protocols
				for (SpectrumIdentificationProtocol specIdProt
						: file.getAnalysisProtocolCollection().getSpectrumIdentificationProtocol()) {
					if ((specIdProt.getEnzymes() != null) && 
							(specIdProt.getEnzymes().getEnzyme().size() < 1)) {
						// no enzymes given, sad, but possible
						specIdProt.setEnzymes(null);
					}
					
					for (SearchModification mod
							: specIdProt.getModificationParams().getSearchModification()) {
						if (mod.getCvParam().size() < 1) {
							// the cvParam of the modification is not set, try to do so
							ModT modification = unimodParser.
									getModificationByMass(
											Float.valueOf(mod.getMassDelta()).doubleValue(),
											mod.getResidues());
							
							if (modification != null) {
								tempCvParam = new CvParam();
								tempCvParam.setAccession(
										modification.getTitle());
								tempCvParam.setCv(unimodCV);
								tempCvParam.setName(modification.getTitle());
								mod.getCvParam().add(tempCvParam);
								mod.setMassDelta(
										modification.getDelta().getMonoMass().floatValue());
							}
						}
					}
					
					if (isFDRCalculated(file.getID())) {
						if (getFileFDRData().get(file.getID()).
								getDecoyStrategy().equals(FDRData.DecoyStrategy.ACCESSIONPATTERN)) {
							tempCvParam = new CvParam();
							tempCvParam.setAccession("MS:1001283");
							tempCvParam.setCv(psiCV);
							tempCvParam.setName("decoy DB accession regexp");
							tempCvParam.setValue(
									getFileFDRData().get(file.getID()).getDecoyPattern());
						} else {
							tempCvParam = new CvParam();
							tempCvParam.setAccession("MS:1001454");
							tempCvParam.setCv(psiCV);
							tempCvParam.setName("quality estimation with implicite decoy sequences");
						}
						
						specIdProt.getAdditionalSearchParams()
								.getCvParam().add(tempCvParam);
						
						// TODO: also use topidentification-info (needs CV values)
					}
					
					
					if (fileID.equals(file.getID()) && filterPSMs) {
						// add the filters
						for (AbstractFilter filter : getFilters(fileID)) {
							if (filter instanceof PSMScoreFilter) {
								// if score filters are set, they are the threshold
								
								ScoreModelEnum scoreModel =
										ScoreModelEnum.getModelByDescription(
												((PSMScoreFilter) filter).getScoreShortName());
								
								if (specIdProt.getThreshold() == null) {
									specIdProt.setThreshold(new ParamList());
								}
								
								if (!scoreModel.equals(ScoreModelEnum.UNKNOWN_SCORE)) {
									
									tempCvParam = new CvParam();
									tempCvParam.setAccession(scoreModel.getCvAccession());
									tempCvParam.setCv(psiCV);
									tempCvParam.setName(scoreModel.getCvName());
									tempCvParam.setValue(filter.getFilterValue().toString());
									
									specIdProt.getThreshold()
											.getCvParam().add(tempCvParam);
								} else {
									// TODO: also make scores from OBO available
									
									UserParam userParam = new UserParam();
									userParam.setName(((PSMScoreFilter) filter).getModelName());
									userParam.setValue(filter.getFilterValue().toString());
									
									specIdProt.getThreshold()
											.getUserParam().add(userParam);
								}
							} else {
								// all other report filters are AdditionalSearchParams
								UserParam userParam = new UserParam();
								userParam.setName("PIA:filter");
								userParam.setValue(filter.toString());
								specIdProt.getAdditionalSearchParams()
										.getUserParam().add(userParam);
							}
						}
					}
					
					// check, if the threshold is set by now
					if ((specIdProt.getThreshold() == null) ||
							(specIdProt.getThreshold().getParamGroup().size() < 1)) {
						if (specIdProt.getThreshold() == null) {
							specIdProt.setThreshold(new ParamList());
						}
						
						tempCvParam = new CvParam();
						tempCvParam.setAccession("MS:1001494");
						tempCvParam.setCv(psiCV);
						tempCvParam.setName("no threshold");
						specIdProt.getThreshold().getCvParam().add(tempCvParam);
					}
					
					analysisProtocolCollection
						.getSpectrumIdentificationProtocol().add(specIdProt);
				}
			}
		}
		
		
		if (fileID < 1) {
			// create the spectrumIdentificationList for PSM sets
			SpectrumIdentificationList combinedSil = 
					createSpectrumIdentificationListForOverview(
							sequenceMap, peptideMap, pepEvidenceMap, combinedSiiMap,
							psiCV, unitCV, filterPSMs);
			silMap.put(0L, combinedSil);
			
			
			// create the SpectrumIdentificationProtocol for the PSM sets
			SpectrumIdentificationProtocol combiningProtocol =
					new SpectrumIdentificationProtocol();
			
			combiningProtocol.setId("psm_combination_protocol");
			combiningProtocol.setAnalysisSoftware(piaAnalysisSoftware);
			
			tempParam = new Param();
			tempCvParam = new CvParam();
			tempCvParam.setAccession("MS:1001083");
			tempCvParam.setCv(psiCV);
			tempCvParam.setName("ms-ms search");
			// this may change in the future, but now we only use ms/ms search
			tempParam.setParam(tempCvParam);
			combiningProtocol.setSearchType(tempParam);
			
			// TODO: CV value for "create PSM sets"
			if (isCombinedFDRScoreCalculated()) {
				// TODO: CV value for "calculate FDR score"
			}
			
			if (filterPSMs) {
				// add the filters
				for (AbstractFilter filter : getFilters(0L)) {
					if (filter instanceof PSMScoreFilter) {
						// if score filters are set, they are the threshold
						
						ScoreModelEnum scoreModel =
								ScoreModelEnum.getModelByDescription(
										((PSMScoreFilter) filter).getScoreShortName());
						
						if (combiningProtocol.getThreshold() == null) {
							combiningProtocol.setThreshold(new ParamList());
						}
						
						if (!scoreModel.equals(ScoreModelEnum.UNKNOWN_SCORE)) {
							
							tempCvParam = new CvParam();
							tempCvParam.setAccession(scoreModel.getCvAccession());
							tempCvParam.setCv(psiCV);
							tempCvParam.setName(scoreModel.getCvName());
							tempCvParam.setValue(filter.getFilterValue().toString());
							
							combiningProtocol.getThreshold()
									.getCvParam().add(tempCvParam);
						} else {
							// TODO: also make scores from OBO available
							
							UserParam userParam = new UserParam();
							userParam.setName(((PSMScoreFilter) filter).getModelName());
							userParam.setValue(filter.getFilterValue().toString());
							
							combiningProtocol.getThreshold()
									.getUserParam().add(userParam);
						}
					} else {
						// all other report filters are AdditionalSearchParams
						UserParam userParam = new UserParam();
						userParam.setName("PIA:filter");
						userParam.setValue(filter.toString());
						combiningProtocol.getAdditionalSearchParams()
								.getUserParam().add(userParam);
					}
				}
			}
			
			// check, if the threshold is set by now
			if ((combiningProtocol.getThreshold() == null) ||
					(combiningProtocol.getThreshold().getParamGroup().size() < 1)) {
				if (combiningProtocol.getThreshold() == null) {
					combiningProtocol.setThreshold(new ParamList());
				}
				
				tempCvParam = new CvParam();
				tempCvParam.setAccession("MS:1001494");
				tempCvParam.setCv(psiCV);
				tempCvParam.setName("no threshold");
				combiningProtocol.getThreshold().getCvParam().add(tempCvParam);
			}
			
			analysisProtocolCollection.getSpectrumIdentificationProtocol()
					.add(combiningProtocol);
			
			SpectrumIdentification combiningId =
					new SpectrumIdentification();
			
			combiningId.setId("psm_combination");
			combiningId.setSpectrumIdentificationList(combinedSil);
			combiningId.setSpectrumIdentificationProtocol(combiningProtocol);
			
			for (SpectraData specData : spectraData.values()) {
				InputSpectra spectra = new InputSpectra();
				spectra.setSpectraData(specData);
				combiningId.getInputSpectra().add(spectra);
			}
			
			for (SearchDatabase searchDB : searchDatabases.values()) {
				SearchDatabaseRef ref = new SearchDatabaseRef();
				ref.setSearchDatabase(searchDB);
				combiningId.getSearchDatabaseRef().add(ref);
			}
			
			analysisCollection.getSpectrumIdentification().add(combiningId);
		}
	}
	
	
	/**
	 * Creates a {@link SpectrumIdentificationList} for the PSMs in the file
	 * given by its ID. This is only for real files, not for the overview  with
	 * ID=0.
	 * <p>
	 * As the sequenceMap, peptideMap and peptideEvidenceMap are filled during
	 * the creation of the SpectrumIdentificationList, their contents should be
	 * added to the respective lists after this call.
	 * 
	 * @param fileID the file id (>0)
	 * @param sequenceMap a map containing the {@link DBSequence} of this and
	 * other files, mapping from the accession
	 * @param peptideMap a map containing the {@link uk.ac.ebi.jmzidml.model.mzidml.Peptide}s
	 * of this and other files, mapping from the IDs
	 * @param peptideEvidenceMap a map containing the {@link PeptideEvidence}s
	 * of this and other files, mapping from the IDs
	 * @param spectraDataMap mapping from the {@link SpectraData} used for the
	 * {@link SpectrumIdentification}
	 * @param dbsInFiles maps from the ID of a {@link SearchDatabase} to a set
	 * of {@link PIAInputFile}s IDs, which use this database
	 * @param unimodParser the used unimodParser
	 * @param psiCV the PSI controlled vocabulary
	 * @param unitCV the used controlled vocabulary for the units
	 * @return a {@link SpectrumIdentificationList} for the file's PSMs or null,
	 * if the file does not exist or has no PSMs
	 */
	public SpectrumIdentificationList createSpectrumIdentificationListForFile(
			Long fileID,
			Map<String, DBSequence> sequenceMap,
			Map<String, uk.ac.ebi.jmzidml.model.mzidml.Peptide> peptideMap,
			Map<String, PeptideEvidence> peptideEvidenceMap,
			Map<String, Set<Long>> dbsInFiles,
			UnimodParser unimodParser,
			Cv psiCV,
			Cv unitCV,
			String rankScoreShort, Boolean filterPSMs) {
		if ((fileID == 0) || !fileReportPSMs.containsKey(fileID)) {
			logger.warn("invalid file ID " + fileID);
			return null;
		}
		
		SpectrumIdentificationList sil = new SpectrumIdentificationList();
		
		Map<String, SpectrumIdentificationResult> specIdResMap =
				new HashMap<String, SpectrumIdentificationResult>();
		
		// each PSM is one SpectrumIdentificationItem, iterate over the PSMs
		for (ReportPSM psm : fileReportPSMs.get(fileID)) {
			// first build or get the peptide of the PSM
			String pepId = psm.getPeptideStringID(true);
			uk.ac.ebi.jmzidml.model.mzidml.Peptide peptide =
					peptideMap.get(pepId);
			if (peptide == null) {
				peptide = new uk.ac.ebi.jmzidml.model.mzidml.Peptide();
				peptide.setId("PEP_" + pepId);
				
				peptide.setPeptideSequence(psm.getSequence());
				
				for (Map.Entry<Integer, Modification> modIt
						: psm.getModifications().entrySet()) {
					uk.ac.ebi.jmzidml.model.mzidml.Modification mod =
							new uk.ac.ebi.jmzidml.model.mzidml.Modification();
					
					ModT uniMod = unimodParser.getModification(
							modIt.getValue().getAccession(),
							modIt.getValue().getDescription(),
							modIt.getValue().getMass(),
							modIt.getValue().getResidue().toString());
					
					if (uniMod != null) {
						mod = unimodParser.createModification(uniMod,
								modIt.getKey(),
								modIt.getValue().getResidue().toString());
					} else {
						// build an "unknown modification"
						mod = new uk.ac.ebi.jmzidml.model.mzidml.Modification();
						mod.getResidues().add(
								modIt.getValue().getResidue().toString());
						mod.setLocation(modIt.getKey());
						
						CvParam tempCvParam = new CvParam();
						tempCvParam.setAccession("MS:1001460");
						tempCvParam.setCv(psiCV);
						tempCvParam.setName("unknown modification");
						mod.getCvParam().add(tempCvParam);
					}
					
					peptide.getModification().add(mod);
					
					// TODO: handle SubstitutionModifications
				}
				
				peptideMap.put(pepId, peptide);
			}
			
			
			// then build the peptide evidences
			for (Accession accession : psm.getAccessions()) {
				boolean foundOccurrence = false;
				
				for (AccessionOccurrence occurrence
						: psm.getPeptide().getAccessionOccurrences()) {
					// look if occurrences are given in the compilation
					if (accession.getAccession().equals(
							occurrence.getAccession().getAccession())) {
						String evidenceID = createPeptideEvidenceID(
								psm.getPeptideStringID(true),
								occurrence.getStart(), occurrence.getEnd(),
								accession);
						if (!peptideEvidenceMap.containsKey(evidenceID)) {
							PeptideEvidence pepEvi = createPeptideEvidence(evidenceID,
									occurrence.getStart(), occurrence.getEnd(),
									psm.getIsDecoy(), peptide,
									accession,
									sequenceMap, dbsInFiles, psiCV);
							
							peptideEvidenceMap.put(evidenceID, pepEvi);
						}
						foundOccurrence = true;
					}
				}
				
				if (!foundOccurrence) {
					// no occurrence given, so create peptideEvidence without position
					String evidenceID = createPeptideEvidenceID(
							psm.getPeptideStringID(true),
							null, null, accession);
					
					if (!peptideEvidenceMap.containsKey(evidenceID)) {
						PeptideEvidence pepEvi = createPeptideEvidence(evidenceID,
								null, null,
								psm.getIsDecoy(), peptide,
								accession,
								sequenceMap, dbsInFiles, psiCV);
						
						peptideEvidenceMap.put(evidenceID, pepEvi);
					}
				}
			}
			
			putPsmInSpectrumIdentificationResultMap(psm, specIdResMap,
					peptideMap, peptideEvidenceMap,
					psiCV, unitCV, rankScoreShort, filterPSMs);
		}
		
		sil.getSpectrumIdentificationResult().addAll(specIdResMap.values());
		
		return sil;
	}
	
	
	/**
	 * Creates a {@link SpectrumIdentificationList} for the PSM sets of the
	 * overview.
	 * <p>
	 * The sequenceMap, peptideMap and peptideEvidenceMap are no longer filled,
	 * but all entries should be available from prior calls of
	 * {@link #createSpectrumIdentificationListForFile(Long, Map, Map, Map, Map, UnimodParser, Cv, Cv, String)}
	 * for all available files.
	 * 
	 * @param sequenceMap
	 * @param peptideMap
	 * @param peptideEvidenceMap
	 * @param specIDMap
	 * @param psiCV
	 * @param unitCV
	 * @param filterPSMs
	 * @return
	 */
	public SpectrumIdentificationList createSpectrumIdentificationListForOverview(
			Map<String, DBSequence> sequenceMap,
			Map<String, uk.ac.ebi.jmzidml.model.mzidml.Peptide> peptideMap,
			Map<String, PeptideEvidence> peptideEvidenceMap,
			Map<String, SpectrumIdentificationItem> specIDMap,
			Cv psiCV,
			Cv unitCV,
			Boolean filterPSMs) {
		SpectrumIdentificationList sil = new SpectrumIdentificationList();
		sil.setId("combined_PSMs");
		
		Map<String, SpectrumIdentificationResult> specIdResMap =
				new HashMap<String, SpectrumIdentificationResult>();
		
		// each PSM is one SpectrumIdentificationItem, iterate over the PSMs
		for (ReportPSMSet psmSet : reportPSMSets) {
			SpectrumIdentificationItem sii =
					putPsmInSpectrumIdentificationResultMap(psmSet, specIdResMap,
							peptideMap, peptideEvidenceMap,
							psiCV, unitCV, null, filterPSMs);
			
			specIDMap.put(sii.getId(), sii);
		}
		
		sil.getSpectrumIdentificationResult().addAll(specIdResMap.values());
		
		return sil;
	}
	
	
	/**
	 * Creates a {@link PeptideEvidence} with the given parameters.
	 * 
	 * @param evidenceID
	 * @param start
	 * @param end
	 * @param isDecoy
	 * @param peptide
	 * @param accession
	 * @param sequenceMap
	 * @param dbsInFiles
	 * @param psiCV
	 * @return
	 */
	private PeptideEvidence createPeptideEvidence(String evidenceID,
			Integer start,
			Integer end,
			Boolean isDecoy,
			uk.ac.ebi.jmzidml.model.mzidml.Peptide peptide,
			Accession accession,
			Map<String, DBSequence> sequenceMap,
			Map<String, Set<Long>> dbsInFiles,
			Cv psiCV) {
		PeptideEvidence pepEvi = new PeptideEvidence();
		
		DBSequence dbSequence = sequenceMap.get(accession);
		if (dbSequence == null) {
			// create the dbSequence entry, if it is not yet created
			dbSequence = new DBSequence();
			
			dbSequence.setAccession(accession.getAccession());
			dbSequence.setId("DbSeq_" + accession.getAccession());
			
			if ((accession.getDbSequence() != null) &&
					(accession.getDbSequence().length() > 0)) {
				dbSequence.setLength(accession.getDbSequence().length());
				dbSequence.setSeq(accession.getDbSequence());
			}
			
			String dbRef = null;
			// look for a good description
			for (Map.Entry<Long, String> descIt
					: accession.getDescriptions().entrySet()) {
				if ((descIt.getValue() != null) &&
						(descIt.getValue().trim().length() > 0)) {
					// take this description and DBsequence_ref
					CvParam tempCvParam = new CvParam();
					tempCvParam.setAccession("MS:1001088");
					tempCvParam.setName(
							"protein description");
					tempCvParam.setCv(psiCV);
					tempCvParam.setValue(descIt.getValue());
					dbSequence.getCvParam().add(
							tempCvParam);
					
					for (String ref : accession.getSearchDatabaseRefs()) {
						if (dbsInFiles.get(ref).contains(descIt.getKey())) {
							dbRef = ref;
						}
					}
				}
			}
			
			if (dbRef == null) {
				// no description found -> use any sequenceRef
				dbRef = accession.getSearchDatabaseRefs().iterator().next();
			}
			
			dbSequence.setSearchDatabase(searchDatabases.get(dbRef));
			
			sequenceMap.put(accession.getAccession(), dbSequence);								
		}
		pepEvi.setDBSequence(dbSequence);
		
		pepEvi.setId(evidenceID);
		
		pepEvi.setIsDecoy(isDecoy);
		pepEvi.setPeptide(peptide);
		if (start != null) {
			pepEvi.setStart(start);
		}
		if (end != null) {
			pepEvi.setEnd(end);
		}
		
		return pepEvi;
	}
	
	
	/**
	 * Creates a String containing the ID of a {@link PeptideEvidence} with the
	 * given information. This string is used for the MzIdentML export.
	 * 
	 * @param peptideStringID the peptideStringID containing the seqeunce and
	 * modifications as in {@link PeptideSpectrumMatch#getPeptideStringID(boolean)}
	 * @param start the start in the dbSequence (if known)
	 * @param end the end in the dbSequence (if known)
	 * @param accession the accession of the protein
	 * @return
	 */
	public String createPeptideEvidenceID(String peptideStringID,
			Integer start, Integer end, Accession accession) {
		StringBuilder evidenceIDstr = new StringBuilder("PE_");
		evidenceIDstr.append(peptideStringID);
		if ((start != null) && (end != null)) {
			evidenceIDstr.append("-");
			evidenceIDstr.append(start);
			evidenceIDstr.append("-");
			evidenceIDstr.append(end);
		}
		evidenceIDstr.append("-");
		evidenceIDstr.append(accession.getAccession());
		
		return evidenceIDstr.toString();
	}
	
	
	/**
	 * Creates a {@link SpectrumIdentificationItem} for the given PSM and puts
	 * it into its {@link SpectrumIdentificationResult}, which will be created
	 * if necessary.
	 *  
	 * @param psm
	 * @param specIdResMap
	 * @param peptideMap
	 * @param peptideEvidenceMap
	 * @param psiCV
	 * @param unitCV
	 * @param rankScoreShort
	 * @return
	 */
	private SpectrumIdentificationItem putPsmInSpectrumIdentificationResultMap(
			PSMReportItem psm,
			Map<String, SpectrumIdentificationResult> specIdResMap,
			Map<String, uk.ac.ebi.jmzidml.model.mzidml.Peptide> peptideMap,
			Map<String, PeptideEvidence> peptideEvidenceMap,
			Cv psiCV,
			Cv unitCV,
			String rankScoreShort,
			Boolean filterPSM) {
		
		// build the SpectrumIdentificationItem into its result
		Long fileID = (psm instanceof ReportPSM) ? ((ReportPSM)psm).getFileID()
				: 0L;
		String psmIdentificationKey =
				getSpectrumIdentificationResultID(psm, fileID);
		
		SpectrumIdentificationResult specIdRes =
				specIdResMap.get(psmIdentificationKey);
		if (specIdRes == null) {
			// this spectrum has no identification yet
			specIdRes = new SpectrumIdentificationResult();
			
			specIdRes.setId(psmIdentificationKey);
			specIdRes.setSpectrumID(psm.getSourceID());
			
			List<ReportPSM> psmList = null;
			if (psm instanceof ReportPSM) {
				psmList = new ArrayList<ReportPSM>(1);
				psmList.add((ReportPSM)psm);
			} else if (psm instanceof ReportPSMSet) {
				psmList = ((ReportPSMSet) psm).getPSMs();
			}
			for (ReportPSM repPSM : psmList) {
				if ((repPSM.getSpectrum().getSpectrumIdentification().getInputSpectra() != null) &&
						(repPSM.getSpectrum().getSpectrumIdentification().getInputSpectra().size() > 0)) {
					specIdRes.setSpectraData(
							spectraData.get(
									repPSM.getSpectrum().getSpectrumIdentification().getInputSpectra().get(0).getSpectraDataRef()));
					// TODO: make the choice of spectrumID and spectraData more sophisticated 
					break;
				}
			}
			
			specIdResMap.put(psmIdentificationKey, specIdRes);
		} else {
			// enhance the spectrum with the spectrumID, if available
			if ((specIdRes.getSpectrumID() == null) &&
					(psm.getSourceID() != null)) {
				specIdRes.setSpectrumID(psm.getSourceID());
			}
			
			// enhance with spectraData, if available
			if  (specIdRes.getSpectraData() == null) {
				List<ReportPSM> psmList = null;
				if (psm instanceof ReportPSM) {
					psmList = new ArrayList<ReportPSM>(1);
					psmList.add((ReportPSM)psm);
				} else if (psm instanceof ReportPSMSet) {
					psmList = ((ReportPSMSet) psm).getPSMs();
				}
				for (ReportPSM repPSM : psmList) {
					if ((repPSM.getSpectrum().getSpectrumIdentification().getInputSpectra() != null) &&
							(repPSM.getSpectrum().getSpectrumIdentification().getInputSpectra().size() > 0)) {
						specIdRes.setSpectraData(
								spectraData.get(
										repPSM.getSpectrum().getSpectrumIdentification().getInputSpectra().get(0).getSpectraDataRef()));
						// TODO: make the choice of spectrumID and spectraData more sophisticated
						break;
					}
				}
			}
		}
		
		SpectrumIdentificationItem sii = new SpectrumIdentificationItem();
		specIdRes.getSpectrumIdentificationItem().add(sii);
		if (!createPSMSets && (psm instanceof ReportPSMSet)) {
			psmIdentificationKey =
					getSpectrumIdentificationItemID(psm, ((ReportPSMSet) psm).getPSMs().get(0).getFileID());
			psmIdentificationKey += ":set";
		} else {
			psmIdentificationKey = getSpectrumIdentificationItemID(psm, fileID);
		}
		sii.setId(psmIdentificationKey);
		
		sii.setChargeState(psm.getCharge());
		sii.setExperimentalMassToCharge(psm.getMassToCharge());
		
		if (filterPSM) {
			List<AbstractFilter> filters = null;
			filters = getFilters(fileID);
			
			sii.setPassThreshold(
					FilterFactory.satisfiesFilterList(psm, fileID, filters));
		} else {
			// without filters, always true
			sii.setPassThreshold(true);
		}
		
		sii.setPeptide(peptideMap.get(psm.getPeptideStringID(true)));
		if ((rankScoreShort == null) || (psm instanceof ReportPSMSet)) {
			sii.setRank(0);
		} else {
			Integer rank =
					((ReportPSM)psm).getIdentificationRank(rankScoreShort);
			if (rank != null) {
				sii.setRank(rank);
			} else {
				sii.setRank(0);
			}
		}
		
		
		// add the peptideEvidences 
		for (Accession accession : psm.getAccessions()) {
			boolean foundOccurrence = false;
			
			for (AccessionOccurrence occurrence
					: psm.getPeptide().getAccessionOccurrences()) {
				// look if occurrences are given in the compilation
				if (accession.getAccession().equals(
						occurrence.getAccession().getAccession())) {
					String evidenceID = createPeptideEvidenceID(
							psm.getPeptideStringID(true),
							occurrence.getStart(), occurrence.getEnd(),
							accession);
					
					PeptideEvidenceRef pepEvidenceRef =
							new PeptideEvidenceRef();
					pepEvidenceRef.setPeptideEvidence(
							peptideEvidenceMap.get(evidenceID));
					
					sii.getPeptideEvidenceRef().add(pepEvidenceRef);
					foundOccurrence = true;
				}
			}
			
			if (!foundOccurrence) {
				// no occurrence given, so use peptideEvidence without position
				String evidenceID = createPeptideEvidenceID(
						psm.getPeptideStringID(true),
						null, null, accession);
				
				PeptideEvidenceRef pepEvidenceRef =
						new PeptideEvidenceRef();
				pepEvidenceRef.setPeptideEvidence(
						peptideEvidenceMap.get(evidenceID));
				
				sii.getPeptideEvidenceRef().add(pepEvidenceRef);
			}
		}
		
		
		if (psm instanceof ReportPSM) {
			for (ScoreModel score : ((ReportPSM) psm).getScores()) {
				if (!score.getType().equals(ScoreModelEnum.UNKNOWN_SCORE)) {
					CvParam tempCvParam = new CvParam();
					tempCvParam.setAccession(score.getAccession());
					tempCvParam.setCv(psiCV);
					tempCvParam.setName(score.getName());
					tempCvParam.setValue(score.getValue().toString());
					
					sii.getCvParam().add(tempCvParam);
				} else {
					// TODO: add unknown scores...
				}
			}
		} else if (psm instanceof ReportPSMSet) {
			if (isCombinedFDRScoreCalculated()) {
				ScoreModel fdrScore = psm.getFDRScore();
				
				CvParam tempCvParam = new CvParam();
				tempCvParam.setAccession(
						ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE.getCvAccession());
				tempCvParam.setCv(psiCV);
				tempCvParam.setName(
						ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE.getCvName());
				if (fdrScore != null) {
					tempCvParam.setValue(fdrScore.getValue().toString());
				} else {
					tempCvParam.setValue(Double.toString(Double.NaN));
				}
				sii.getCvParam().add(tempCvParam);
			}
			
			// mark as consensus result
			CvParam tempCvParam = new CvParam();
			tempCvParam.setAccession("MS:1002315");
			tempCvParam.setCv(psiCV);
			tempCvParam.setName("consensus result");
			sii.getCvParam().add(tempCvParam);
		}
		
		if (psm.getRetentionTime() != null) {
			CvParam tempCvParam = new CvParam();
			tempCvParam.setAccession("MS:1000016");
			tempCvParam.setCv(psiCV);
			tempCvParam.setName("scan start time");
			tempCvParam.setValue(psm.getRetentionTime().toString());
			
			tempCvParam.setUnitCv(unitCV);
			tempCvParam.setUnitName("second");
			tempCvParam.setUnitAccession("UO:0000010");
			
			sii.getCvParam().add(tempCvParam);
		}
		
		if (psm.getSpectrumTitle() != null) {
			CvParam tempCvParam = new CvParam();
			tempCvParam.setAccession("MS:1000796");
			tempCvParam.setCv(psiCV);
			tempCvParam.setName("spectrum title");
			tempCvParam.setValue(psm.getSpectrumTitle());
			
			sii.getCvParam().add(tempCvParam);
		}
		
		if (psm.getDeltaMass() > -1) {
			CvParam tempCvParam = new CvParam();
			tempCvParam.setAccession("MS:1001975");
			tempCvParam.setCv(psiCV);
			tempCvParam.setName("delta m/z");
			tempCvParam.setValue(Double.toString(psm.getDeltaMass()));
			
			tempCvParam.setUnitCv(unitCV);
			tempCvParam.setUnitName("dalton");
			tempCvParam.setUnitAccession("UO:0000221");
			
			sii.getCvParam().add(tempCvParam);
		}
		
		return sii;
	}
	
	
	/**
	 * Returns the used ID for the {@link SpectrumIdentificationItem} in
	 * mzIdentML export.
	 * 
	 * @param psm
	 * @return
	 */
	public String getSpectrumIdentificationItemID(PSMReportItem psm,
			Long fileID) {
		if (itemPSMSetSettings == null) {
			// initialize the settings on first call
			itemPSMSetSettings = new HashMap<String,Boolean>();
			itemPSMSetSettings.put(
					IdentificationKeySettings.FILE_ID.toString(), true);
			itemPSMSetSettings.put(
					IdentificationKeySettings.CHARGE.toString(), true);
			itemPSMSetSettings.put(
					IdentificationKeySettings.RETENTION_TIME.toString(), true);
			itemPSMSetSettings.put(
					IdentificationKeySettings.MASSTOCHARGE.toString(), true);
			itemPSMSetSettings.put(
					IdentificationKeySettings.SOURCE_ID.toString(), true);
			itemPSMSetSettings.put(
					IdentificationKeySettings.SEQUENCE.toString(), true);
			itemPSMSetSettings.put(
					IdentificationKeySettings.MODIFICATIONS.toString(), true);
		}
		
		return PeptideSpectrumMatch.getIdentificationKey(itemPSMSetSettings,
				psm.getSequence(), psm.getModificationsString(),
				psm.getCharge(), psm.getMassToCharge(),
				psm.getRetentionTime(), psm.getSourceID(),
				psm.getSpectrumTitle(), fileID);
	}
	
	
	/**
	 * Returns the used ID for the {@link SpectrumIdentificationResult} in
	 * mzIdentML export.
	 * 
	 * @param psm
	 * @return
	 */
	public String getSpectrumIdentificationResultID(PSMReportItem psm,
			Long fileID) {
		if (resultPSMSetSettings == null) {
			// initialize the settings on first call
			resultPSMSetSettings = new HashMap<String,Boolean>();
			resultPSMSetSettings.put(
					IdentificationKeySettings.FILE_ID.toString(), true);
			resultPSMSetSettings.put(
					IdentificationKeySettings.CHARGE.toString(), true);
			resultPSMSetSettings.put(
					IdentificationKeySettings.RETENTION_TIME.toString(), true);
			resultPSMSetSettings.put(
					IdentificationKeySettings.MASSTOCHARGE.toString(), true);
			resultPSMSetSettings.put(
					IdentificationKeySettings.SOURCE_ID.toString(), true);
		}
		
		return PeptideSpectrumMatch.getIdentificationKey(resultPSMSetSettings,
				psm.getSequence(), psm.getModificationsString(),
				psm.getCharge(), psm.getMassToCharge(),
				psm.getRetentionTime(), psm.getSourceID(),
				psm.getSpectrumTitle(), fileID);
	}
	
	
	
	
	
	
	
	/**
     * Writes the PSM report into mzTab.
     * 
     * @throws IOException
     */
	public void exportMzTab(Writer writer, Long fileID,
			Boolean filterExport) throws IOException {
		logger.debug("Start exportMzTab");
		
		UnimodParser unimodParser; 
		try {
			unimodParser = new UnimodParser();
		} catch (JAXBException e) {
			logger.error("Could not initialize the UnimodParser.", e);
			writer.flush();
			return;
		}
		
		// Setting version, mode, and type in MZTabDescription 
		MZTabDescription tabDescription;
		tabDescription = new MZTabDescription(
				MZTabDescription.Mode.Complete,
				MZTabDescription.Type.Identification);
		
		Map<String, List<MsRun>> specIdRefToMsRuns =
				new HashMap<String, List<MsRun>>();
		
		Metadata mtd = createMetadataForMzTab(fileID, unimodParser,
				tabDescription, specIdRefToMsRuns);
		
		
		// finally add PIA to the list of used softwares
		int piaSoftwareNr = mtd.getSoftwareMap().size() + 1;
		mtd.addSoftwareParam(piaSoftwareNr,
				new CVParam("MS",
						PIAConstants.CV_PIA_ACCESSION,
						PIAConstants.CV_PIA_NAME,
						PIAConstants.version));
		if (fileID == 0) {
			mtd.addSoftwareSetting(piaSoftwareNr,
					"mzTab export of PSMs for overview");
			
			if (isCombinedFDRScoreCalculated()) {
				mtd.addSoftwareSetting(piaSoftwareNr,
						PIAConstants.CV_PSM_LEVEL_COMBINED_FDRSCORE_NAME + 
						" was calculated");
				
				for (Map.Entry<Long, FDRData> fdrIt : fileFDRData.entrySet()) {
					if (fdrIt.getKey() > 0) {
						mtd.addSoftwareSetting(piaSoftwareNr,
								"base score for FDR calculation for file " +
								fdrIt.getKey() + " = " +
								fdrIt.getValue().getScoreShortName());
					}
				}
			}
		} else {
			mtd.addSoftwareSetting(piaSoftwareNr,
					"mzTab export of PSMs for file " + fileID);
			
			if (isFDRCalculated(fileID)) {
				mtd.addSoftwareSetting(piaSoftwareNr,
						PIAConstants.CV_PSM_LEVEL_FDRSCORE_NAME + 
						" was calculated for file " + fileID);
				
				mtd.addSoftwareSetting(piaSoftwareNr,
						"base score for FDR calculation for file " +
						fileID + " = " +
						fileFDRData.get(fileID).getScoreShortName());
			}
		}
		
		if (filterExport) {
			for (AbstractFilter filter : getFilters(fileID)) {
				mtd.addSoftwareSetting(piaSoftwareNr, "applied filter " + 
						filter.toString());
			}
		}
		
		// write out the header
		writer.append(mtd.toString());
		
		// get the report PSMs
		List<PSMReportItem> report;
		boolean writeReliabilityCol = true;
		
		report = new ArrayList<PSMReportItem>();
		if (fileID > 0) {
			List<ReportPSM> rep = filterExport ? 
					getFilteredReportPSMs(fileID, getFilters(fileID)) :
					fileReportPSMs.get(fileID);
			
			report.addAll(rep);
			writeReliabilityCol = isFDRCalculated(fileID);
		} else {
			report.addAll(getFilteredReportPSMSets(getFilters(0L)));
			writeReliabilityCol = isCombinedFDRScoreCalculated();
		}
		
		// write out the PSMs
		writer.append(MZTabConstants.NEW_LINE);
		writePSMsForMzTab(mtd, report, specIdRefToMsRuns,
				writeReliabilityCol, writer, unimodParser);
		
		writer.flush();
		logger.debug("exportMzTab done");
	}
	
	
	/**
	 * This method creates the {@link Metadata} and fills it with the basic
	 * information for an export.
	 * 
	 * @param fileID the ID of the exported file (0 for overview)
	 * @param unimodParser a prior initialized instance on {@link UnimodParser}
	 * @param tabDescription the prior generated {@link MZTabDescription}
	 * @param spectrumIdentificationRefToMsRuns a mapping from the
	 * spectrumIdentificationRefs to the associated MsRuns, the map will be
	 * cleared and filled with the actual data in this method
	 * @return
	 * @throws IOException
	 */
	public Metadata createMetadataForMzTab(Long fileID,
			UnimodParser unimodParser, MZTabDescription tabDescription,
			Map<String, List<MsRun>> spectrumIdentificationRefToMsRuns)
					throws IOException {
		Metadata mtd = new Metadata(tabDescription);
		mtd.setDescription("PIA export of " + fileName);
		
		List<InputSpectra> inputSpectraList = new ArrayList<InputSpectra>();
		// all needed search modifications
		List<SearchModification> searchModifications =
				new ArrayList<SearchModification>();
		// all needed analysis protocol collections (for the software in MTD)
		List<AnalysisProtocolCollection> analysisProtocols =
				new ArrayList<AnalysisProtocolCollection>();
		// maps from the spectrumIdentification ID to the spectraData ID
		Map<String, List<String>> spectrumIdentificationToSpectraData = 
				new HashMap<String, List<String>>();
		if (fileID == 0) {
			for (PIAInputFile file : getFiles().values()) {
				if (file.getID() > 0) {
					SpectrumIdentification specID = file.getAnalysisCollection().
							getSpectrumIdentification().get(0);
					List<InputSpectra> inputSpectras = specID.getInputSpectra();
					if ((inputSpectras != null) && (inputSpectras.size() > 0)) {
						List<String> spectraDataRefs = new ArrayList<String>();
						for (InputSpectra inputSpectra : inputSpectras) {
							spectraDataRefs.add(
									inputSpectra.getSpectraDataRef());
						}
						spectrumIdentificationToSpectraData.put( specID.getId(),
								spectraDataRefs);
						
						inputSpectraList.addAll(inputSpectras);
					}
					
					ModificationParams modParams =
							file.getAnalysisProtocolCollection().
							getSpectrumIdentificationProtocol().get(0).
							getModificationParams();
					if ((modParams != null) &&
							(modParams.getSearchModification() != null)) {
						searchModifications.addAll(
								modParams.getSearchModification());
					}
					
					if (file.getAnalysisProtocolCollection() != null) {
						analysisProtocols.add(
								file.getAnalysisProtocolCollection());
					}
				}
			}
		} else {
			SpectrumIdentification specID = inputFiles.get(fileID).
					getAnalysisCollection().getSpectrumIdentification().get(0);
			List<InputSpectra> inputSpectras = specID.getInputSpectra();
			if ((inputSpectras != null) && (inputSpectras.size() > 0)) {
				List<String> spectraDataRefs = new ArrayList<String>();
				for (InputSpectra inputSpectra : inputSpectras) {
					spectraDataRefs.add(
							inputSpectra.getSpectraDataRef());
				}
				spectrumIdentificationToSpectraData.put( specID.getId(),
						spectraDataRefs);
				
				inputSpectraList.addAll(inputSpectras);
			}
			
			ModificationParams modParams =
					inputFiles.get(fileID).getAnalysisProtocolCollection().
					getSpectrumIdentificationProtocol().get(0).
					getModificationParams();
			if ((modParams != null) &&
					(modParams.getSearchModification() != null)) {
				searchModifications.addAll(
						modParams.getSearchModification());
			}
			
			if (inputFiles.get(fileID).getAnalysisProtocolCollection() != null) {
				analysisProtocols.add(
						inputFiles.get(fileID).getAnalysisProtocolCollection());
			}
		}
		
		// associate the spectraData (msRuns) to integer IDs
		Map<String, Integer> spectraDataID = new HashMap<String, Integer>();
		for (InputSpectra inputSpectra : inputSpectraList) {
			if (!spectraDataID.containsKey(
					inputSpectra.getSpectraDataRef())) {
				// this inputSpectra is not yet in the list
				Integer id = spectraDataID.size()+1;
				spectraDataID.put(
						inputSpectra.getSpectraDataRef(), id);
			}
		}
		inputSpectraList = null;
		
		// add msRuns
		for (Map.Entry<String, Integer> spectraIt : spectraDataID.entrySet()) {
			SpectraData sd = spectraData.get(spectraIt.getKey());
			
			MsRun msRun = new MsRun(spectraIt.getValue());
			
			msRun.setLocation(new URL("file://" + sd.getLocation()));
			
			if ((sd.getFileFormat() != null) &&
					(sd.getFileFormat().getCvParam() != null)) {
				msRun.setFormat(
						new CVParam(sd.getFileFormat().getCvParam().getCvRef(),
								sd.getFileFormat().getCvParam().getAccession(),
								sd.getFileFormat().getCvParam().getName(),
								sd.getFileFormat().getCvParam().getValue()));
			}
			
			if ((sd.getSpectrumIDFormat() != null) &&
					(sd.getSpectrumIDFormat().getCvParam() != null)) {
				msRun.setIdFormat(
						new CVParam(sd.getSpectrumIDFormat().getCvParam().getCvRef(),
								sd.getSpectrumIDFormat().getCvParam().getAccession(),
								sd.getSpectrumIDFormat().getCvParam().getName(),
								sd.getSpectrumIDFormat().getCvParam().getValue()));
			}
			
			mtd.addMsRun(msRun);
		}
		
		// this mapping is needed to reference reported PSMs to the MsRuns
		spectrumIdentificationRefToMsRuns.clear();
		for (Map.Entry<String, List<String>> iter
				: spectrumIdentificationToSpectraData.entrySet()) {
			Set<MsRun> runSet = new HashSet<MsRun>();
			
			for (String spectrumDataRef : iter.getValue()) {
				runSet.add(
						mtd.getMsRunMap().get(spectraDataID.get(spectrumDataRef)));
			}
			
			spectrumIdentificationRefToMsRuns.put(iter.getKey(),
					new ArrayList<MsRun>(runSet));
		}
		
		// add modifications
		int nrVariableMods = 0;
		int nrFixedMods = 0;
		Set<String> fixedMods = new HashSet<String>();
		Set<String> variableMods = new HashSet<String>();
		for (SearchModification searchMod : searchModifications) {
			
			String modAccession = null;
			String modName = null;
			
			if ((searchMod.getCvParam() != null) &&
					(searchMod.getCvParam().size() > 0)) {
				modAccession = searchMod.getCvParam().get(0).getAccession();
				modName = searchMod.getCvParam().get(0).getName();
			}
			
			ModT uniMod = unimodParser.getModification(
					modAccession,
					modName,
					(double)searchMod.getMassDelta(),
					searchMod.getResidues());
			
			for (String site : searchMod.getResidues()) {
				CVParam cvParam;
				
				if (uniMod != null) {
					cvParam = new CVParam(UnimodParser.getCv().getId(),
							UnimodParser.getCv().getId() + ":" + uniMod.getRecordId(),
							uniMod.getTitle(),
							null);
					
				} else {
					// build an "unknown modification"
					cvParam = new CVParam(PIAConstants.CV_PSI_MS_LABEL,
							"MS:1001460",
							"unknown modification",
							Float.toString(searchMod.getMassDelta()));
				}
				
				if (searchMod.isFixedMod()) {
					FixedMod fixedMod = new FixedMod(nrFixedMods+1);
					fixedMod.setParam(cvParam);
					fixedMod.setSite(site);
					
					if (!fixedMods.contains(site + cvParam.toString())) {
						nrFixedMods++;
						mtd.addFixedMod(fixedMod);
						fixedMods.add(site + cvParam.toString());
					}
				} else {
					VariableMod variableMod = new VariableMod(nrVariableMods+1);
					variableMod.setParam(cvParam);
					variableMod.setSite(site);
					
					if (!variableMods.contains(site + cvParam.toString())) {
						nrVariableMods++;
						mtd.addVariableMod(variableMod);
						variableMods.add(site + cvParam.toString());
					}
				}
			}
		}
		
		// adding the software
		int nrSoftwares = 0;
		for (AnalysisProtocolCollection protocol : analysisProtocols) {
			
			SpectrumIdentificationProtocol specIdProtocol =
					protocol.getSpectrumIdentificationProtocol().get(0);
			AnalysisSoftware software = 
					analysisSoftware.get(
							specIdProtocol.getAnalysisSoftwareRef());
			Param softwareName = software.getSoftwareName();
			if ((softwareName != null) && (softwareName.getCvParam() != null)) {
				mtd.addSoftwareParam(++nrSoftwares,
						new CVParam("MS",
								softwareName.getCvParam().getAccession(),
								softwareName.getCvParam().getName(),
								software.getVersion()));
				
			} else  {
				mtd.addSoftwareParam(++nrSoftwares,
						new CVParam(null,
								null,
								software.getName(),
								software.getVersion()));
			}
			
			
			// add tolerances
			if (specIdProtocol.getFragmentTolerance() != null) {
				for (CvParam param
						: specIdProtocol.getFragmentTolerance().getCvParam()) {
					StringBuilder sb = new StringBuilder();
					sb.append("fragment ");
					sb.append(param.getName());
					sb.append(" = ");
					sb.append(param.getValue());
					if (param.getUnitName() != null) {
						sb.append(param.getUnitName());
					}
					
					mtd.addSoftwareSetting(nrSoftwares, sb.toString());
				}
			}
			if (specIdProtocol.getParentTolerance() != null) {
				for (CvParam param
						: specIdProtocol.getParentTolerance().getCvParam()) {
					StringBuilder sb = new StringBuilder();
					sb.append("parent ");
					sb.append(param.getName());
					sb.append(" = ");
					sb.append(param.getValue());
					if (param.getUnitName() != null) {
						sb.append(param.getUnitName());
					}
					
					mtd.addSoftwareSetting(nrSoftwares, sb.toString());
				}
			}
			
			// add additional search params
			for (CvParam param
					: specIdProtocol.getAdditionalSearchParams().getCvParam()) {
				if (param.getValue() != null) {
					mtd.addSoftwareSetting(nrSoftwares,
							param.getName() + " = " + param.getValue());
				} else {
					mtd.addSoftwareSetting(nrSoftwares, param.getName());
				}
			}
			for (UserParam param
					: specIdProtocol.getAdditionalSearchParams().getUserParam()) {
				if (param.getValue() != null) {
					mtd.addSoftwareSetting(nrSoftwares,
							param.getName() + " = " + param.getValue());
				} else {
					mtd.addSoftwareSetting(nrSoftwares, param.getName());
				}
			}
		}
		
		return mtd;
	}
	
	
	/**
	 * Writes out a PSM section for the list of PSMs (which can be either sets
	 * or not).
	 * 
	 * @param metadata the metadata associated to teh columns (should be created
	 * by a call of {@link #createPSMsHeaderForMzTab(Long, Boolean, UnimodParser, MZTabDescription, Map)})
	 * @param report a List of {@link PSMReportItem}s containing the PSMs to be
	 * reported
	 * @param specIDRefToMsRuns a mapping from the spectrumIdentificationRefs to
	 * the associated MsRuns (should be filled by a call of 
	 * {@link #createPSMsHeaderForMzTab(Long, Boolean, UnimodParser, MZTabDescription, Map)})
	 * @param reliabilityCol whether the reliability column should be written
	 * @param writer the Writer for the export
	 * @param unimodParser instance of a prior initialized {@link UnimodParser}
	 * @throws IOException
	 */
	public void writePSMsForMzTab(Metadata metadata, List<PSMReportItem> report,
			Map<String, List<MsRun>> specIDRefToMsRuns, boolean reliabilityCol,
			Writer writer,
			UnimodParser unimodParser) throws IOException {
		// initialize the columns
		MZTabColumnFactory columnFactory =
				MZTabColumnFactory.getInstance(Section.PSM_Header);
		
		// add custom column for missed cleavages
		columnFactory.addOptionalColumn(
				PIAConstants.MZTAB_MISSED_CLEAVAGES_COLUMN_NAME, Integer.class);
		
		// add optional column for decoys
		CVParam decoyColumnParam =
				new CVParam("MS", "MS:1002217", "decoy peptide", null);
		columnFactory.addOptionalColumn(decoyColumnParam, String.class);
		
		// if it is set, write the reliability column
		if (reliabilityCol) {
			columnFactory.addReliabilityOptionalColumn();
		}
		
		
		writer.append(columnFactory.toString());
		writer.append(MZTabConstants.NEW_LINE);
		
		// cache the databaseRefs to an array with name and version
		Map<String, String[]> dbRefToDbNameAndVersion =
				new HashMap<String, String[]>();
		
		// cache the softwareRefs to the Params
		Map<String, uk.ac.ebi.pride.jmztab.model.Param> softwareParams = 
				new HashMap<String, uk.ac.ebi.pride.jmztab.model.Param>();
		
		
		// now write the PSMs
		int psmID = 0;
		for (PSMReportItem psmItem : report) {
			PSM mztabPsm = new PSM(columnFactory, metadata);
			
			mztabPsm.setSequence(psmItem.getSequence());
			String specIdRef = null;
			
			Set<String> softwareRefs = new HashSet<String>();
			
			
			List<ScoreModel> scoreModels = new ArrayList<ScoreModel>();
			
			if (psmItem instanceof ReportPSM) {
				psmID = ((ReportPSM) psmItem).getId().intValue();
				
				specIdRef = ((ReportPSM) psmItem).getSpectrum().
						getSpectrumIdentification().getId();
				
				softwareRefs.add(((ReportPSM) psmItem).getFile().
						getAnalysisProtocolCollection().
						getSpectrumIdentificationProtocol().get(0).
						getAnalysisSoftwareRef());
				
				scoreModels.addAll(((ReportPSM)psmItem).getScores());
			} else if (psmItem instanceof ReportPSMSet) {
				// in PSM sets, the ID does NOT represent the ID from the PIA
				// file but is an incremental value
				psmID++;
				
				Set<String> scoreShorts = new HashSet<String>();
				for (ReportPSM reportPSM : ((ReportPSMSet) psmItem).getPSMs()) {
					if (specIdRef != null) {
						specIdRef = reportPSM.getSpectrum().
								getSpectrumIdentification().getId();
					}
					
					softwareRefs.add(reportPSM.getFile().
							getAnalysisProtocolCollection().
							getSpectrumIdentificationProtocol().get(0).
							getAnalysisSoftwareRef());
					
					scoreShorts.addAll(fileScoreShortNames.get(
							reportPSM.getFile().getID()));
				}
				
				for (String scoreShort : scoreShorts) {
					ScoreModel scoreModel =
							((ReportPSMSet) psmItem).getBestScoreModel(scoreShort);
					if ((scoreModel != null) &&
							!scoreModel.getType().equals(ScoreModelEnum.PSM_LEVEL_FDR_SCORE)) {
						// the best PSM level FDRScore is not exported for PSM sets
						scoreModels.add(scoreModel);
					}
				}
				
				// add combined fdr score (if it is calculated)
				if (isCombinedFDRScoreCalculated() &&
						(psmItem.getFDRScore() != null)) {
					scoreModels.add(psmItem.getFDRScore());
				}
			}
			mztabPsm.setPSM_ID(psmID);
			
			if (psmItem.getAccessions().size() > 1) {
				mztabPsm.setUnique(MZBoolean.False);
			} else {
				mztabPsm.setUnique(MZBoolean.True);
			}
			
			for (Map.Entry<Integer, Modification> modIt
					: psmItem.getModifications().entrySet()) {
				uk.ac.ebi.pride.jmztab.model.Modification mod;
				ModT uniMod = unimodParser.getModification(
						modIt.getValue().getAccession(),
						modIt.getValue().getDescription(),
						modIt.getValue().getMass(),
						modIt.getValue().getResidue().toString());
				
				if (uniMod != null) {
					mod = new uk.ac.ebi.pride.jmztab.model.Modification(
							Section.PSM,
							uk.ac.ebi.pride.jmztab.model.Modification.Type.UNIMOD,
							uniMod.getRecordId().toString());
				} else {
					// not found in UNIMOD, create a CHEMMOD mass-shift
					mod = new uk.ac.ebi.pride.jmztab.model.Modification(
							Section.PSM,
							uk.ac.ebi.pride.jmztab.model.Modification.Type.CHEMMOD,
							modIt.getValue().getMass().toString());
				}
				
				mod.addPosition(modIt.getKey(), null);
				mztabPsm.addModification(mod);
			}
			
			if (psmItem.getRetentionTime() != null) {
				mztabPsm.setRetentionTime(
						psmItem.getRetentionTime().toString());
			}
			
			mztabPsm.setCharge(psmItem.getCharge());
			mztabPsm.setExpMassToCharge(psmItem.getMassToCharge());
			mztabPsm.setCalcMassToCharge(
					psmItem.getMassToCharge() - psmItem.getDeltaMass());
			
			// There is no URI for the PSM
			// mztabPsm.setURI("http://www.ebi.ac.uk/pride/link/to/peptide");
			
			if (psmItem.getSourceID() != null) {
				List<MsRun> runList = specIDRefToMsRuns.get(specIdRef);
				
				if ((runList != null) && (runList.size() == 1)) {
					SpectraRef specRef = new SpectraRef(runList.get(0),
							psmItem.getSourceID());
					mztabPsm.addSpectraRef(specRef);
				}
			}
			
			// add the search engines (i.e. analysisSoftwares)
			for (String softwareRef : softwareRefs) {
				uk.ac.ebi.pride.jmztab.model.Param softwareParam = 
						softwareParams.get(softwareRef);
				
				if (softwareParam == null) {
					AnalysisSoftware software =
							analysisSoftware.get(softwareRef);
					
					Param softwareName = software.getSoftwareName();
					if (softwareName.getCvParam() != null) {
						CvParam param = softwareName.getCvParam();
						
						softwareParam = new CVParam(param.getCvRef(),
								param.getAccession(), param.getName(),
								software.getVersion());
					} else if (softwareName.getUserParam() != null) {
						UserParam param = softwareName.getUserParam();
						
						softwareParam =
								new uk.ac.ebi.pride.jmztab.model.UserParam(
										param.getName(), software.getVersion());
					}
					
					softwareParams.put(softwareRef, softwareParam);
				}
				
				mztabPsm.addSearchEngineParam(softwareParam);
			}
			
			// add the scores
			Reliability reliability = null;
			for (ScoreModel scoreModel : scoreModels) {
				uk.ac.ebi.pride.jmztab.model.Param scoreParam;
				
				if (scoreModel.getType().equals(
						ScoreModelEnum.UNKNOWN_SCORE)) {
					scoreParam =
							new uk.ac.ebi.pride.jmztab.model.UserParam(
									scoreModel.getName(),
									scoreModel.getValue().toString());
				} else {
					scoreParam = new CVParam(
							PIAConstants.CV_PSI_MS_LABEL,
							scoreModel.getAccession(),
							scoreModel.getName(),
							scoreModel.getValue().toString());
					
					if (reliabilityCol &&
							(scoreModel.getType().equals(
									ScoreModelEnum.PSM_LEVEL_FDR_SCORE) ||
							scoreModel.getType().equals(
									ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE))) {
						if (scoreModel.getValue() <= 0.01) {
							reliability = Reliability.High;
						} else if (scoreModel.getValue() <= 0.05) {
							reliability = Reliability.Medium;
						} else {
							reliability = Reliability.Poor;
						}
					}
				}
				
				mztabPsm.addSearchEngineScoreParam(scoreParam);
			}
			
			// if the (combined) FDRScore is calculated, give the reliability
			// 1: high reliability     (combined) FDRScore <= 0.01
			// 2: medium reliability   (combined) FDRScore <= 0.05
			// 3: poor reliability     (combined) FDRScore >  0.05
			if (reliability != null) {
				mztabPsm.setReliability(reliability);
			}
			
			mztabPsm.setOptionColumn(
					PIAConstants.MZTAB_MISSED_CLEAVAGES_COLUMN_NAME,
					psmItem.getMissedCleavages());
			
			mztabPsm.setOptionColumn(decoyColumnParam,
					psmItem.getIsDecoy() ? "1" : "0");
			
			// one line and some special info per accession
			for (Accession accession : psmItem.getAccessions()) {
				mztabPsm.setAccession(accession.getAccession());
				
				// set the first available dbName and dbVersion
				for (String dbRef :	accession.getSearchDatabaseRefs()) {
					String[] nameAndVersion =
							dbRefToDbNameAndVersion.get(dbRef);
					// cache the name and version of databases
					if (nameAndVersion == null) {
						SearchDatabase sDB = searchDatabases.get(dbRef);
						
						if (sDB.getDatabaseName() != null) {
							nameAndVersion = new String[2];
							if (sDB.getDatabaseName().getCvParam() != null) {
								nameAndVersion[0] = 
										sDB.getDatabaseName().getCvParam().getName();
							} else if (sDB.getDatabaseName().getUserParam() != null) {
								nameAndVersion[0] = 
										sDB.getDatabaseName().getUserParam().getName();
							}
							nameAndVersion[1] = sDB.getVersion();
							
						} else if (sDB.getName() != null) {
							nameAndVersion = new String[2];
							nameAndVersion[0] = sDB.getName();
							nameAndVersion[1] = sDB.getVersion();
						} else {
							nameAndVersion = new String[1];
							nameAndVersion[0] = null;
						}
						
						dbRefToDbNameAndVersion.put(dbRef, nameAndVersion);
					}
					
					if (nameAndVersion[0] != null) {
						mztabPsm.setDatabase(nameAndVersion[0]);
						mztabPsm.setDatabaseVersion(nameAndVersion[1]);
					}
				}
				
				for (AccessionOccurrence occurrence :
					psmItem.getPeptide().getAccessionOccurrences()) {
					if (accession.equals(occurrence.getAccession())) {
						String dbSequence = accession.getDbSequence();
						if (dbSequence != null) {
							if (occurrence.getStart() > 1) {
								mztabPsm.setPre(
										dbSequence.substring(
												occurrence.getStart()-2,
												occurrence.getStart()-1));
							} else {
								mztabPsm.setPre("-");
							}
							
							
							if (occurrence.getEnd() < dbSequence.length()) {
								mztabPsm.setPost(
										dbSequence.substring(
												occurrence.getEnd(),
												occurrence.getEnd()+1));
							} else {
								mztabPsm.setPost("-");
							}
						}
						
						mztabPsm.setStart(occurrence.getStart().toString());
						mztabPsm.setEnd(occurrence.getEnd().toString());
					}
					// TODO: multiple occurrences in the the same protein
				}
				
				writer.append(mztabPsm.toString());
				writer.append(MZTabConstants.NEW_LINE);
			}
		}
	}
	
	
	
	
	/**
	 * Processes the command line on the PSM level
	 * @param model
	 * @param commands
	 * @return
	 */
	public static boolean processCLI(PSMModeller model, String[] commands) {
		if (model == null) {
			logger.error("No PSM modeller given while processing CLI commands");
			return false;
		}
		
		Pattern pattern = Pattern.compile("^([^=]+)=(.*)");
		Matcher commandParamMatcher;
		
		for (String command : commands) {
			String[] params = null;
			commandParamMatcher = pattern.matcher(command);
			
			if (commandParamMatcher.matches()) {
				command = commandParamMatcher.group(1);
				params = commandParamMatcher.group(2).split(",");
			}
			
			try {
				PSMExecuteCommands.valueOf(command).execute(model, params);
			} catch (IllegalArgumentException e) {
				logger.error("Could not process unknown call to " +
						command);
			}
		}
		
		return true;
	}
}
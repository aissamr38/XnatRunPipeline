
package org.nrg.xnat.icm.utils;

import java.util.Arrays;
import org.slf4j.LoggerFactory;
import org.nrg.xnat.icm.plugin.XnatIntRunPipelinePlugin;
import java.io.FileInputStream;
import java.util.zip.ZipEntry;
import java.io.OutputStream;
import java.util.zip.ZipOutputStream;
import java.io.FileOutputStream;
import org.dcm4che2.data.DicomObject;
import org.nrg.xdat.bean.CatCatalogBean;
import java.io.PrintWriter;
import java.io.FileReader;
import org.json.simple.parser.JSONParser;
import org.apache.commons.io.FilenameUtils;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.dcm4che2.io.DicomInputStream;
import org.nrg.xdat.model.CatEntryI;
import org.nrg.xdat.model.XnatResourcecatalogI;
import org.nrg.xnat.utils.CatalogUtils;
import org.nrg.xdat.om.XnatResourcecatalog;
import org.nrg.xdat.om.XnatAbstractresource;
import org.nrg.xdat.model.XnatAbstractresourceI;
import org.nrg.xdat.model.XnatImagescandataI;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xft.ItemI;
import java.util.Iterator;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.nrg.xdat.om.XnatSubjectdata;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVFormat;
import java.io.FileWriter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.nrg.xdat.XDAT;
import java.io.File;
import org.nrg.xft.security.UserI;
import java.util.List;
import org.slf4j.Logger;

public class XnatToBidsUtils
{
    private static final Logger _logger;
    private static List<String> _xnatSessionTypes;
    private UserI _xnatUser;
    private String _tempBuildPath;
    private String _tempBidsPath;
    private String _tempZipPath;
    private File _participantsTsvFile;
    private int _subjectIndex;
    private int _sessionIndex;
    private int _scanIndex;

    public XnatToBidsUtils(final String tempBuildPath) {
        this._tempBuildPath = tempBuildPath;
    }

    public String convertToBidsFunction(final String id_project, final List<String> id_subjects) {
        this.log("*****************************************************************************");
        this.log("***********************XNAT*****TO********BIDS*******************************");
        this.log("***********************VERSION**0.0.1**Beta**********************************");
        this.log("*****************************************************************************");
        this._xnatUser = XDAT.getUserDetails();
        XnatSubjectdata xnatSubject = null;
        this.log("XNAT to BIDS converting for project [" + id_project + "]...");
        try {
            new File(this._tempBuildPath).mkdirs();
            this._tempBidsPath = this._tempBuildPath + "/BIDS";
            new File(this._tempBidsPath).mkdirs();
            this._tempZipPath = this._tempBuildPath + "/ZIP";
            new File(this._tempZipPath).mkdirs();
            final File datasetDescriptionJsonFile = new File(this._tempBidsPath + "/dataset_description.json");
            final JSONObject datasetDescriptionObject = new JSONObject();
            datasetDescriptionObject.put((Object)"BIDSVersion", (Object)"1.0.0");
            datasetDescriptionObject.put((Object)"License", (Object)"This data is made available under the Creative Commons BY-SA 4.0 International License.");
            datasetDescriptionObject.put((Object)"Name", (Object)"XNAT dataset");
            final JSONArray referencesAndLinksList = new JSONArray();
            referencesAndLinksList.add((Object)"References and links for this dataset go here");
            referencesAndLinksList.add((Object)"Or here");
            datasetDescriptionObject.put((Object)"ReferencesAndLinks", (Object)referencesAndLinksList);
            this.writeJsonFile(datasetDescriptionJsonFile, datasetDescriptionObject);
            final File participantsJsonFile = new File(this._tempBidsPath + "/participants.json");
            final JSONObject participantsObject = new JSONObject();
            JSONObject participantstItem = new JSONObject();
            participantstItem.put((Object)"id", (Object)"1");
            participantstItem.put((Object)"label", (Object)"XNAT_ID");
            participantsObject.put((Object)"xnat_id", (Object)participantstItem);
            participantstItem = new JSONObject();
            participantstItem.put((Object)"id", (Object)"2");
            participantstItem.put((Object)"label", (Object)"Age");
            participantsObject.put((Object)"age", (Object)participantstItem);
            participantstItem = new JSONObject();
            participantstItem.put((Object)"id", (Object)"3");
            participantstItem.put((Object)"label", (Object)"Sex");
            participantsObject.put((Object)"sex", (Object)participantstItem);
            participantstItem = new JSONObject();
            participantstItem.put((Object)"id", (Object)"4");
            participantstItem.put((Object)"label", (Object)"Group");
            participantsObject.put((Object)"group", (Object)participantstItem);
            this.writeJsonFile(participantsJsonFile, participantsObject);
            this._participantsTsvFile = new File(this._tempBidsPath + "/participants.tsv");
            final FileWriter writer = new FileWriter(this._participantsTsvFile);
            final String[] headers = { "participant_id", "xnat_id", "age", "sex", "group" };
            final CSVPrinter csvPrinter = new CSVPrinter((Appendable)writer, CSVFormat.TDF.withHeader(headers));
            csvPrinter.flush();
            csvPrinter.close();
            final FileWriter bidsIgnoreFileWriter = new FileWriter(this._tempBidsPath + "/.bidsignore");
            bidsIgnoreFileWriter.write("unassigned/");
            bidsIgnoreFileWriter.flush();
            bidsIgnoreFileWriter.close();
            this._subjectIndex = 1;
            for (final String id_subject : id_subjects) {
                xnatSubject = XnatSubjectdata.getXnatSubjectdatasById((Object)id_subject, this._xnatUser, false);
                this.handleSubject(xnatSubject);
                ++this._subjectIndex;
            }
            this.createBidsZipFile();
        }
        catch (Exception e) {
            XnatToBidsUtils._logger.error(e.getMessage(), (Throwable)e);
            e.printStackTrace();
            try {
                FileUtils.deleteDirectory(new File(String.format("%s/nifti/", this._tempBuildPath)));
            }
            catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        finally {
            try {
                FileUtils.deleteDirectory(new File(String.format("%s/nifti/", this._tempBuildPath)));
            }
            catch (IOException ioe2) {
                ioe2.printStackTrace();
            }
        }
        this.log("XNAT to BIDS conversion ended for project [" + id_project + "].");
        this.log("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        this.log("+++++++++++++++++++++++XNAT+++++TO++++++++BIDS+++++++++++++++++++++++++++++");
        this.log("+++++++++++++++++++++++VERSION++0.0.1Beta++++++++++++++++++++++++++++++++++");
        this.log("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        return new File(this._tempZipPath, "BIDS.zip").getAbsolutePath();
    }

    private void handleSubject(final XnatSubjectdata xnatSubject) throws Exception {
        this.log("**************************************");
        this.log("Subject #" + this._subjectIndex + " [" + xnatSubject.getId() + "]");
        this.log("**************************************");
        final List<ItemI> assessors = (List<ItemI>)xnatSubject.getMinimalLoadAssessors();
        this._sessionIndex = 0;
        String sessionId = "";
        for (final ItemI ii : assessors) {
            sessionId = "";
            if (XnatToBidsUtils._xnatSessionTypes.contains(ii.getXSIType())) {
                sessionId = ii.getProperty("ID").toString();
                ++this._sessionIndex;
                if (this._sessionIndex == 1) {
                    final String subjectFolder = String.format("%s/%s", this._tempBidsPath, this.getSubjectBidsName(xnatSubject));
                    new File(subjectFolder).mkdirs();
                    final File scansJsonFile = new File(String.format("%s/%s_scans.json", subjectFolder, this.getSubjectBidsName(xnatSubject)));
                    final JSONObject scansObject = new JSONObject();
                    JSONObject scansItem = new JSONObject();
                    scansItem.put((Object)"id", (Object)"1");
                    scansItem.put((Object)"label", (Object)"SCAN ID");
                    scansObject.put((Object)"scan_id", (Object)scansItem);
                    scansItem = new JSONObject();
                    scansItem.put((Object)"id", (Object)"2");
                    scansItem.put((Object)"label", (Object)"Scan type");
                    scansObject.put((Object)"type", (Object)scansItem);
                    this.writeJsonFile(scansJsonFile, scansObject);
                }
                this._scanIndex = 0;
                final XnatImagesessiondata xnatImageSessionData = XnatImagesessiondata.getXnatImagesessiondatasById((Object)sessionId, this._xnatUser, false);
                this.handleSession(xnatSubject, xnatImageSessionData);
            }
        }
        if (this._sessionIndex > 0) {
            final Object[] recordValues = { xnatSubject.getId().replace('_', 'n'), xnatSubject.getId(), "" + xnatSubject.getAge(), xnatSubject.getGender(), "control" };
            this.writeTsvRecord(this._participantsTsvFile, recordValues);
        }
    }

    private void handleSession(final XnatSubjectdata xnatSubject, final XnatImagesessiondata xnatImageSessionData) throws Exception {
        this.log("++++++++++++++++++++++++++++++++++++++");
        this.log("Session #" + this._sessionIndex + " [" + xnatImageSessionData.getId() + "]");
        this.log("++++++++++++++++++++++++++++++++++++++");
        final String niftiDirPath = String.format("%s/nifti/", this._tempBuildPath);
        final File niftiDir = new File(niftiDirPath);
        if (!niftiDir.exists()) {
            niftiDir.mkdirs();
        }
        else {
            for (final String tempFile : niftiDir.list()) {
                new File(niftiDir, tempFile).delete();
            }
        }
        this._scanIndex = 0;
        for (final XnatImagescandataI xnatScan : xnatImageSessionData.getScans_scan()) {
            ++this._scanIndex;
            this.handleScan(xnatSubject, xnatImageSessionData, xnatScan);
        }
    }

    private void handleScan(final XnatSubjectdata xnatSubject, final XnatImagesessiondata xnatImageSessionData, final XnatImagescandataI xnatScan) throws Exception {
        final String sessionDir = String.format("%s/%s/%s", this._tempBidsPath, this.getSubjectBidsName(xnatSubject), this.getSessionBidsName(xnatImageSessionData));
        final String xnatProjectRootPath = xnatImageSessionData.getArchivePath();
        final File scansTsvFile = new File(String.format("%s/%s_%s_scans.tsv", sessionDir, this.getSubjectBidsName(xnatSubject), this.getSessionBidsName(xnatImageSessionData)));
        final String niftiDirPath = String.format("%s/nifti/", this._tempBuildPath);
        final File niftiDir = new File(niftiDirPath);
        if (!niftiDir.exists()) {
            niftiDir.mkdirs();
        }
        else {
            for (final String tempFile : niftiDir.list()) {
                new File(niftiDir, tempFile).delete();
            }
        }
        this.log("----------------------SCAN----------------------------");
        this.log("Scan# " + this._scanIndex + " : [" + xnatScan.getId() + "]..." + xnatScan.getType());
        final List<XnatAbstractresourceI> xnatScanFolders = (List<XnatAbstractresourceI>)xnatScan.getFile();
        String protocolName = "";
        for (final XnatAbstractresourceI xnatScanFolderResource : xnatScanFolders) {
            if ("DICOM".equals(((XnatAbstractresource)xnatScanFolderResource).getFormat())) {
                final XnatResourcecatalog xnatScanFolderResourceCatalog = (XnatResourcecatalog)xnatScanFolderResource;
                final CatCatalogBean xnatScanFolderResourceCatalogBean = xnatScanFolderResourceCatalog.getCatalog(xnatProjectRootPath);
                final File xnatScanFolderCatalogFile = CatalogUtils.getCatalogFile(xnatProjectRootPath, (XnatResourcecatalogI)xnatScanFolderResourceCatalog);
                final Iterator<CatEntryI> iterator2 = (Iterator<CatEntryI>)xnatScanFolderResourceCatalogBean.getEntries_entry().iterator();
                if (iterator2.hasNext()) {
                    final CatEntryI catEntry = iterator2.next();
                    final DicomInputStream din = new DicomInputStream(new File(xnatScanFolderCatalogFile.getParent() + "/" + catEntry.getUri()));
                    final DicomObject dicom = din.readDicomObject();
                    din.close();
                    protocolName = dicom.getString(1577008);
                    if (protocolName == null) {
                        protocolName = "";
                    }
                }
                break;
            }
        }
        this.log("Scan protocol name = " + protocolName);
        if ("".equals(protocolName)) {
            this.log("Protocol Name not found");
            return;
        }
        final String scanTypeLower = protocolName.toLowerCase();
        String acquisitionLabel = protocolName;
        acquisitionLabel = acquisitionLabel.replace("(", "");
        acquisitionLabel = acquisitionLabel.replace("_", "");
        acquisitionLabel = acquisitionLabel.replace(" ", "");
        acquisitionLabel = acquisitionLabel.replace(")", "");
        this.log("Scan acquisition label = " + acquisitionLabel);
        String destinationFolder = "unassigned";
        String fileScanType = "";
        if (scanTypeLower.indexOf("rest") != -1) {
            this.log("Found resting state fMRI");
            destinationFolder = "func";
            fileScanType = "task-rest";
            acquisitionLabel = "rest";
        }
        else if (scanTypeLower.indexOf("task") != -1) {
            this.log("Found task fMRI");
            destinationFolder = "func";
            fileScanType = "task-" + acquisitionLabel;
        }
        else if (scanTypeLower.indexOf("fmri") != -1) {
            destinationFolder = "func";
            if (scanTypeLower.indexOf("rs") != -1) {
                this.log("Found resting state fMRI");
                fileScanType = "task-rest";
            }
            else {
                this.log("Found fMRI");
                fileScanType = "bold";
            }
        }
        else if (scanTypeLower.indexOf("dti") != -1) {
            this.log("Found DTI(DWI)");
            destinationFolder = "dwi";
            fileScanType = "dwi";
        }
        else if (scanTypeLower.indexOf("dwi") != -1) {
            this.log("Found DWI");
            destinationFolder = "dwi";
            fileScanType = "dwi";
        }
        else if (scanTypeLower.indexOf("flair") != -1) {
            this.log("Found FLAIR");
            destinationFolder = "anat";
            fileScanType = "FLAIR";
        }
        else if (scanTypeLower.indexOf("flash") != -1) {
            this.log("Found FLAIR");
            destinationFolder = "anat";
            fileScanType = "FLAIR";
        }
        else if (scanTypeLower.indexOf("loca") != -1) {
            destinationFolder = "unassigned";
            this.log("Found Localizer");
        }
        else if (scanTypeLower.indexOf("t1") != -1) {
            destinationFolder = "anat";
            if (scanTypeLower.indexOf("rho") != -1) {
                this.log("Found T1 Rho");
                fileScanType = "T1map";
            }
            else if (scanTypeLower.indexOf("map") != -1) {
                this.log("Found T1 map");
                fileScanType = "T1map";
            }
            else {
                this.log("Found T1 weighted");
                fileScanType = "T1w";
            }
        }
        else if (scanTypeLower.indexOf("t2") != -1) {
            destinationFolder = "anat";
            if (scanTypeLower.indexOf("map") != -1) {
                this.log("Found T2 map");
                fileScanType = "T2map";
            }
            else if (scanTypeLower.indexOf("*") != -1) {
                this.log("Found T2 STAR weighted");
                fileScanType = "T2star";
            }
            else if (scanTypeLower.indexOf("star") != -1) {
                this.log("Found T2 STAR weighted");
                fileScanType = "T2star";
            }
            else {
                this.log("Found T2 weighted");
                fileScanType = "T2w";
            }
        }
        else if (scanTypeLower.indexOf("roton") != -1) {
            this.log("Found PD");
            destinationFolder = "anat";
            fileScanType = "PD";
        }
        else if (scanTypeLower.indexOf("ngiography") != -1) {
            this.log("Found Angiography");
            destinationFolder = "anat";
            fileScanType = "angio";
        }
        else if (scanTypeLower.indexOf("swi") != -1) {
            this.log("Found SWI");
            destinationFolder = "anat";
            fileScanType = "SWImagandphase";
        }
        else if (scanTypeLower.indexOf("fac") != -1) {
            this.log("Found Defacing mask");
            destinationFolder = "anat";
            fileScanType = "defacemask";
        }
        else if (scanTypeLower.indexOf("pd") != -1) {
            this.log("Found PD");
            destinationFolder = "anat";
            fileScanType = "PD";
        }
        else if (acquisitionLabel.indexOf("nback") != -1) {
            this.log("Found n-back");
            destinationFolder = "func";
            fileScanType = "task-nback";
        }
        else {
            this.log("Unknown type : " + xnatScan.getType());
            destinationFolder = "unassigned";
        }
        if (!new File(sessionDir).exists()) {
            new File(sessionDir).mkdirs();
            new File(sessionDir + "/anat").mkdirs();
            new File(sessionDir + "/dwi").mkdirs();
            new File(sessionDir + "/func").mkdirs();
            new File(sessionDir + "/unassigned").mkdirs();
            final FileWriter writer = new FileWriter(scansTsvFile);
            final String[] headers = { "scan_id", "filename", "type" };
            final CSVPrinter csvPrinter = new CSVPrinter((Appendable)writer, CSVFormat.TDF.withHeader(headers));
            csvPrinter.flush();
            csvPrinter.close();
        }
        String extension = "";
        String newFilename = "";
        for (final XnatAbstractresourceI xnatScanFolderResource2 : xnatScanFolders) {
            if ("DICOM".equals(((XnatAbstractresource)xnatScanFolderResource2).getFormat())) {
                for (final String tempFile2 : niftiDir.list()) {
                    new File(niftiDir, tempFile2).delete();
                }
                final XnatResourcecatalog xnatScanFolderResourceCatalog2 = (XnatResourcecatalog)xnatScanFolderResource2;
                final File xnatScanFolderCatalogFile2 = CatalogUtils.getCatalogFile(xnatProjectRootPath, (XnatResourcecatalogI)xnatScanFolderResourceCatalog2);
                String niftiNameFormat = this.getSubjectBidsName(xnatSubject);
                niftiNameFormat = niftiNameFormat + "_" + this.getSessionBidsName(xnatImageSessionData);
                niftiNameFormat = niftiNameFormat + "_run-" + String.format("%03d", this._scanIndex);
                niftiNameFormat += "_%d";
                final ProcessBuilder pb = new ProcessBuilder(new String[] { "dcm2niix", "-ba", "n", "-f", niftiNameFormat, "-o", niftiDir.getAbsolutePath(), xnatScanFolderCatalogFile2.getParent() });
                final Process p = pb.start();
                final InputStreamReader isr = new InputStreamReader(p.getInputStream());
                final BufferedReader br = new BufferedReader(isr);
                String ligne = "";
                while ((ligne = br.readLine()) != null) {}
                final String[] niftiDirList = niftiDir.list();
                String baseFileName = "";
                if (niftiDirList.length > 2) {
                    int jsonFiles = 0;
                    for (final String tempFile3 : niftiDirList) {
                        System.out.println("NIFTI file : " + tempFile3);
                        extension = FilenameUtils.getExtension(tempFile3);
                        if ("json".equals(extension)) {
                            if (++jsonFiles == 1) {
                                baseFileName = FilenameUtils.getBaseName(tempFile3);
                            }
                            else if (FilenameUtils.getBaseName(tempFile3).length() < baseFileName.length()) {
                                baseFileName = FilenameUtils.getBaseName(tempFile3);
                            }
                            else if (FilenameUtils.getBaseName(tempFile3).length() == baseFileName.length()) {
                                baseFileName = FilenameUtils.getBaseName(tempFile3);
                                baseFileName = baseFileName.substring(0, baseFileName.length() - 1);
                            }
                        }
                    }
                    if (jsonFiles == 1) {
                        baseFileName = "";
                    }
                }
                extension = "";
                newFilename = "";
                for (final String tempFile4 : niftiDirList) {
                    extension = FilenameUtils.getExtension(tempFile4);
                    if ("gz".equals(extension)) {
                        extension = "nii.gz";
                    }
                    newFilename = "";
                    if ("unassigned".equals(destinationFolder)) {
                        newFilename = String.format("%s_%s_acq-%s_run-%03d_unassigned", this.getSubjectBidsName(xnatSubject), this.getSessionBidsName(xnatImageSessionData), acquisitionLabel, this._scanIndex);
                    }
                    else if ("func".equals(destinationFolder) && fileScanType.indexOf("task-") == 0) {
                        newFilename = String.format("%s_%s_%s_acq-%s_run-%03d_bold", this.getSubjectBidsName(xnatSubject), this.getSessionBidsName(xnatImageSessionData), fileScanType, acquisitionLabel, this._scanIndex);
                    }
                    else {
                        newFilename = String.format("%s_%s_acq-%s_run-%03d_%s", this.getSubjectBidsName(xnatSubject), this.getSessionBidsName(xnatImageSessionData), acquisitionLabel, this._scanIndex, fileScanType);
                    }
                    if (!"".equals(baseFileName)) {
                        final String myBaseFileName = FilenameUtils.getBaseName(tempFile4);
                        if (myBaseFileName.indexOf(baseFileName) == 0 && myBaseFileName.length() > baseFileName.length()) {
                            newFilename += myBaseFileName.substring(baseFileName.length());
                            if ("nii.gz".equals(extension)) {
                                extension = "gz";
                            }
                        }
                    }
                    newFilename = newFilename + "." + extension;
                    System.out.println("Moving file " + new File(niftiDir, tempFile4).getAbsolutePath() + " to file " + sessionDir + "/" + destinationFolder + "/" + newFilename);
                    FileUtils.moveFile(new File(niftiDir, tempFile4), new File(sessionDir + "/" + destinationFolder, newFilename));
                    if ("func".equals(destinationFolder) && fileScanType.indexOf("task-") == 0 && "json".equals(extension)) {
                        this.log("JSON file " + sessionDir + "/" + destinationFolder + "/" + newFilename + " modified with TaskName = " + fileScanType.substring(fileScanType.indexOf("task-") + 5));
                        final Object jsonFileContents = new JSONParser().parse((Reader)new FileReader(new File(sessionDir + "/" + destinationFolder, newFilename)));
                        final JSONObject jsonObject = (JSONObject)jsonFileContents;
                        jsonObject.put((Object)"TaskName", (Object)fileScanType.substring(fileScanType.indexOf("task-") + 5));
                        final PrintWriter pw = new PrintWriter(new File(sessionDir + "/" + destinationFolder, newFilename));
                        pw.write(jsonObject.toJSONString());
                        pw.flush();
                        pw.close();
                    }
                }
                final Object[] recordValues = { xnatScan.getId(), xnatScanFolderCatalogFile2.getAbsolutePath(), xnatScan.getType() };
                this.writeTsvRecord(scansTsvFile, recordValues);
                break;
            }
        }
    }

    private String getSubjectBidsName(final XnatSubjectdata xnatSubject) {
        return "sub-" + xnatSubject.getId().replace('_', 'n');
    }

    private String getSessionBidsName(final XnatImagesessiondata xnatImageSessionData) {
        return "ses-" + xnatImageSessionData.getId().replace('_', 'n');
    }

    private void createBidsZipFile() {
        this.log("Creating BIDS zip file...");
        final File bidsFolder = new File(this._tempBidsPath);
        final File zipFile = new File(this._tempZipPath, "BIDS.zip");
        try {
            final FileOutputStream zipFileOutputStream = new FileOutputStream(zipFile);
            final ZipOutputStream zipOutputStream = new ZipOutputStream(zipFileOutputStream);
            this.addDirToZip(zipOutputStream, bidsFolder);
            zipOutputStream.finish();
            zipOutputStream.close();
            this.log("BIDS zip file created.");
        }
        catch (IOException ioe) {
            XnatToBidsUtils._logger.error(ioe.getMessage(), (Throwable)ioe);
            ioe.printStackTrace();
        }
    }

    private void addDirToZip(final ZipOutputStream zipOutputStream, final File folder) {
        try {
            for (final File folderFile : folder.listFiles()) {
                if (folderFile.isDirectory()) {
                    if (folderFile.list().length == 0) {
                        zipOutputStream.putNextEntry(new ZipEntry(folderFile.getPath().substring(this._tempBidsPath.length() + 1) + "/"));
                        zipOutputStream.closeEntry();
                    }
                    else {
                        this.addDirToZip(zipOutputStream, folderFile);
                    }
                }
                else {
                    final byte[] buffer = new byte[1024];
                    final FileInputStream fis = new FileInputStream(folderFile);
                    zipOutputStream.putNextEntry(new ZipEntry(folderFile.getPath().substring(this._tempBidsPath.length() + 1)));
                    int length;
                    while ((length = fis.read(buffer)) > 0) {
                        zipOutputStream.write(buffer, 0, length);
                    }
                    zipOutputStream.closeEntry();
                    fis.close();
                }
            }
        }
        catch (Exception e) {
            XnatToBidsUtils._logger.error(e.getMessage(), (Throwable)e);
            e.printStackTrace();
        }
    }

    private boolean writeJsonFile(final File jsonFile, final JSONObject contents) {
        try (final FileWriter file = new FileWriter(jsonFile)) {
            file.write(contents.toJSONString());
            file.flush();
            return true;
        }
        catch (IOException e) {
            XnatToBidsUtils._logger.error(e.getMessage(), (Throwable)e);
            e.printStackTrace();
            return false;
        }
    }

    private boolean writeTsvRecord(final File tsvFile, final Object[] recordValues) {
        try (final FileWriter writer = new FileWriter(tsvFile, true)) {
            final CSVPrinter csvPrinter = new CSVPrinter((Appendable)writer, CSVFormat.TDF);
            csvPrinter.printRecord(recordValues);
            csvPrinter.flush();
            csvPrinter.close();
            return true;
        }
        catch (IOException e) {
            XnatToBidsUtils._logger.error(e.getMessage(), (Throwable)e);
            e.printStackTrace();
            return false;
        }
    }

    private void log(final String logString) {
        System.out.println(logString);
        XnatToBidsUtils._logger.info(logString);
    }

    static {
        _logger = LoggerFactory.getLogger((Class)XnatIntRunPipelinePlugin.class);
        XnatToBidsUtils._xnatSessionTypes = Arrays.asList("xnat:mrSessionData", "xnat:crSessionData", "xnat:ctSessionData", "xnat:petSessionData", "xnat:petmrSessionData");
    }
}
package org.nrg.xnat.icm.rest;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.slf4j.LoggerFactory;
import org.nrg.xnat.icm.plugin.XnatIntRunPipelinePlugin;
import java.util.zip.ZipEntry;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestMethod;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.ApiOperation;

import org.nrg.xft.security.UserI;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.nrg.xdat.om.XnatSubjectdata;

import java.io.OutputStream;
import java.util.zip.ZipOutputStream;
import org.nrg.xdat.om.base.BaseXnatProjectdata;
import java.io.FileInputStream;
import java.io.File;
import java.io.ByteArrayOutputStream;
import org.nrg.xnat.icm.utils.XnatToBidsUtils;
import org.nrg.xnat.icm.rest.*;
import org.nrg.xft.XFTTable;
import org.nrg.xft.search.CriteriaCollection;
import org.nrg.xft.search.QueryOrganizer;
import org.nrg.xdat.XDAT;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.userdetails.cache.NullUserCache;
import org.springframework.web.bind.annotation.PathVariable;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.springframework.web.bind.annotation.RequestMapping;
import org.nrg.framework.annotations.XapiRestController;
import io.swagger.annotations.Api;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import java.util.*;
import java.io.*;
//import javax.ws.rs.WebApplicationException;
import java.lang.*;
import com.jcraft.jsch.*;
import java.io.InputStream;
import java.io.ObjectInputStream.GetField;
import java.io.PrintWriter;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import org.json.simple.*;
import org.json.simple.parser.JSONParser;


@Api(description = "run pipeline on Cluster")
@XapiRestController
@RequestMapping({ "/int-run-pipline" })
public class XnatRunPipelineApi
{
    private static final Logger _logger;
    private static List<String> _xnatSessionTypes;
    private static String _dcm2niixExecutablePath;
    private String _tempBuildPath;
    private boolean _imageFormatAddDicoms;
    private boolean _imageFormatAddNiftis;
    private boolean _imageFormatAddBothImageFormats;
    public static String listImages[] = null;
	private static String allOrListSubject;

    // Paramètres création session cluster à distance :
    private static int port = 22;
    private static ChannelExec channel = null;
    private static Session session = null;
    private static InputStream inputStream = null;
    private static OutputStream outputStream =null;
    private static ChannelShell channelShell = null;

    private static String fullPathScriptSlurm = "";
 	// Les paramètres du cluster à distance
    public  static String user = "";
    private  static String host = "";

    private static String ID_PROJECT = "";
    private static String datTimeNow = "";
    // Paramèttres par défaut de l'entête du script qui sont des constantes :
	private static String SCRIPT_SBATCH = "#!/bin/bash\n"
                                        + "#SBATCH -p batch\n";
    private static String NUMBER_OF_JOBS_PER_NODES = "#SBATCH --ntasks-per-node=1\n";
    private static String MAXIMAL_TIME_COMPUTATION = "#SBATCH -t 00:06:00\n";
    private static String NUMBER_OF_NODES = "#SBATCH -N 1\n";
    private static String STANDARD_OUTPUT_FILE = "#SBATCH -o ";   // définir le fichier des log.out !
    private static String STANDARD_ERROR_FILE = "#SBATCH -e ";    // définir le fichier des log.out !
    private static String NAME_OF_SLURM_JOB = "#SBATCH -J";       // attribuer un nom a  job lancé
    private static String LOAD_MODULES = "\n\nmodule purge\n"
                                       + "module load all\n"
                                       + "\nmodule load anaconda/3\n";
    private static String GET_USER_ENV = "\n#SBATCH --get-user-env=L\n";  // Pour sourcer l'env de l'user
    private static String LOAD_IMG_SINGULARITY = "";
    private static String ADDITIONAL_PARAMS = "\n";
    public static String ID_CLUSTER = "";
    public static String URI_HOST_XNAT = "";
    public static String CHANGE_WORKING_DIRECTORY = "#SBATCH --chdir=/tmp\n";

    // path vers le fichier de configuration /var/lib/tomcat8/xnat_config_run_pipeline_cluster.json dans xnat.
	private static String config_file_xnat = "xnat_config_run_pipeline_cluster.json";
    public static JSONObject jsonObject;
    public static ArrayList<String> listKeysJsonFile = null;



    @ApiOperation(value = "Get list of piplines in cluster on load page", notes = "Custom")
    @ApiResponses({ @ApiResponse(code = 200, message = "Connection success "), @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT Rest Api"), @ApiResponse(code = 500, message = "Unexpected internal serval error") })
    @RequestMapping(value = { "/check-user-id" }, produces = { "application/json" }, method = { RequestMethod.POST })
    @ResponseBody
    public void checkIdXnatIfIsIdCluster(final HttpServletResponse response)  throws IOException{
        final UserI xnatUser = XDAT.getUserDetails();
        final String idCluster = xnatUser.getUsername().replace("_", ".");
        String ligne = "";


        // Read config file and initialise  params

       try {

            readConfigFileJson();

       } catch (IOException ioe){

       } catch (Exception e){


       }

        JSONArray teamNames = (JSONArray) jsonObject.get("teamNames");

        response.setContentType("application/json");

        response.setCharacterEncoding("UTF-8");

        JSONObject obj = new JSONObject();


        try {

            final String program = "groups";
            //final String programName = userName ;
            final List<String> cmd = new ArrayList<String>();
            cmd.add(program);
            cmd.add(idCluster);
            log(cmd.toString());

            final ProcessBuilder pb = new ProcessBuilder(cmd);
            final Process p = pb.start();
            final InputStreamReader isr = new InputStreamReader(p.getInputStream());
            final BufferedReader br = new BufferedReader(isr);
            ligne = br.readLine();
        }
        catch (Exception e) {
            log(e.toString());
        }


        if (ligne!=null && !("admin-xnat-root".contains(idCluster))) {

            String []  tabTeamUser = ligne.split(" ");
            String [] array = new String[teamNames.size()] ;
            int i=0;
            for (Object ele : teamNames){
                array[i] = ele.toString();
                i++;
            }

            HashSet<String> set = new HashSet<>();

            set.addAll(Arrays.asList(tabTeamUser));

            set.retainAll(Arrays.asList(array));
            if(set.size()>=1){

                Map<String, String> _listPipelines  = new HashMap<>();

                String linkImgeSingularity = (String) jsonObject.get("linkAllImgSingularity");

                JSONArray jsonArray = (JSONArray) jsonObject.get("listPipelines");

                listImages = new String[jsonArray.size()];

                obj.put("pipelines", jsonArray);
                obj.put("id", idCluster);


             }else {
                obj.put("pipelines","null");
             }

        } else {

            obj.put("pipelines","null");

        }

        log("L'objet de la liste des pipelines a envoyer est "+ obj.toString());

        PrintWriter out = response.getWriter();

        out.print(obj);

        out.flush();

        log( "\n La date de création de ce fichier est : "+getDateTimeNow() + "    \n\n");
    }



    @ApiOperation(value = "Get list of piplines in cluster", notes = "Custom")
    @ApiResponses({ @ApiResponse(code = 200, message = "Connection success "), @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT Rest Api"), @ApiResponse(code = 500, message = "Unexpected internal serval error") })
    @RequestMapping(value = { "/get-pipelines/{id_project}" }, produces = { "application/json" }, method = { RequestMethod.POST })
    @ResponseBody
    public void getPipelinesOnBtnClick(final HttpServletResponse response, @PathVariable final String id_project, @RequestParam("idCluster") final String idCluster, @RequestParam("subject_ids") final String subject_ids)  throws IOException{
        final UserI xnatUser = XDAT.getUserDetails();
        ByteArrayOutputStream baos = null;
        final List<String> subjectsList = new LinkedList<String>();


        log("Runin pipeLine from [" + id_project + "]");


        log("id niolon = "+idCluster);

        user = idCluster;
        log("la liste séléctionné est   !! : "+subject_ids);
        response.setContentType("application/json");

        response.setCharacterEncoding("UTF-8");

        JSONObject obj = new JSONObject();



        if (checkIfIdUserExist(idCluster) && !("admin-xnat-root".contains(idCluster))) {

            Map<String, String> _listPipelines  = new HashMap<>();

            /*try {
            /*
            doConnectionCluster(passwordniolon);
            */

            String linkImgeSingularity = (String) jsonObject.get("linkAllImgSingularity");

            JSONArray jsonArray = (JSONArray) jsonObject.get("listPipelines");

            listImages = new String[jsonArray.size()];

            obj.put("pipelines", jsonArray);

        } else {

            obj.put("pipelines","null");

        }

        log(" l'objet de la liste des pipelines a envoyer est "+ obj.toString());

        PrintWriter out = response.getWriter();

        out.print(obj);

        out.flush();



        log( "\n La date de création de ce fichier est : "+getDateTimeNow() + "    \n\n");

    }



    /* Cette fonction permet de récuperer les les infos(lien de la doc, commande befor, after , participant, ...,  qui correspond à un pipeline */
    @ApiOperation(value = "Get link of pipeline", notes = "Custom")
    @ApiResponses({ @ApiResponse(code = 200, message = "Connection success "), @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT Rest Api"), @ApiResponse(code = 500, message = "Unexpected internal serval error") })
    @RequestMapping(value = { "/get-infos-pipeline" }, produces = { "application/json" }, method = { RequestMethod.POST })
    @ResponseBody
    public void getInfoAboutPipelineSelected(final HttpServletResponse response, @RequestParam("pipelineSelected") final String pipelineSelected)  throws IOException{


        if(jsonObject.equals(null)){

            /* Lire le fichier et initialisation des params */
            try {

                readConfigFileJson();

            } catch (IOException ioe){
                ioe.printStackTrace();

            } catch (Exception e){
                e.printStackTrace();

            }
        }

        String linkDoc = (String) ((JSONObject) getJsonObjectByKey(jsonObject,pipelineSelected)).get("linkDoc");
        String commande_befor = (String) ((JSONObject) getJsonObjectByKey(jsonObject,pipelineSelected)).get("commande_befor");
        String commande_after = (String) ((JSONObject) getJsonObjectByKey(jsonObject,pipelineSelected)).get("commande_after");
        String commande_participant = (String) ((JSONObject) getJsonObjectByKey(jsonObject,pipelineSelected)).get("commande_participant");
        // Préparer le listSessionOfSubject à envoyer en json

        response.setContentType("application/json");

        response.setCharacterEncoding("UTF-8");

        JSONObject obj = new JSONObject();

        obj.put("linkDoc",linkDoc);
        obj.put("commande_befor", commande_befor);
        obj.put("commande_after", commande_after);
        obj.put("commande_participant", commande_participant);


        log("lien a envoyer est  : " +obj.toString());

        PrintWriter out = response.getWriter();

        out.print(obj);

        out.flush();

    }



    /* Cette fonction permet de lancer le pipeline choisi dans le cluster de calcul */

    @ApiOperation(value = "Start piplines in cluster", notes = "Custom")
    @ApiResponses({ @ApiResponse(code = 200, message = "Connection success "), @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT Rest Api"), @ApiResponse(code = 500, message = "Unexpected internal serval error") })
    @RequestMapping(value = { "/start-pipeline/{id_project}" }, produces = { "application/json" }, method = { RequestMethod.POST })
    @ResponseBody
    public void startPipelineInCluster(final HttpServletResponse response, @PathVariable final String id_project, @RequestParam("selectPipeline") final String selectPipeline,  @RequestParam("idCluster") final String idCluster, @RequestParam("subject_ids") final String subject_ids, @RequestParam("nameExportDir") final String nameExportDir,
                 @RequestParam("additionalParams") final String additionalParams, @RequestParam("sessions_ids") final String sessions_ids,
                    @RequestParam("radioValue")  final String radioValue, @RequestParam("commande_befor")  final String commande_befor, @RequestParam("commande_after")  final String commande_after,
                    @RequestParam("commande_participant")  final String commande_participant, @RequestParam("command_extra_bids")  final String command_extra_bids)  throws IOException{

        final UserI xnatUser = XDAT.getUserDetails();
        final String userName  = xnatUser.getUsername().replace("_", ".");
        final List<String> subjectsList = new LinkedList<String>();
        String listOfSubjectWithSpaceSeparated = null;
        String listOfSubjectWithCamasSeparated = "";
        String SCRIPT_SBATCH_GLOBAL = "";
        String pipeLineSelected = "";
        String inputAndOutputDirectory = "";
        String ligneRetunrCommandeStartPipeline="";
        String pathErrorLogOut = "";
        String pathErrorLogErr = "";

        allOrListSubject = subject_ids ;
        ID_PROJECT =  id_project;

        ID_CLUSTER = idCluster ;





        log("** Start  pipeline **\n");

        log("Runin pipeLine from [" + id_project + "]");

        log(" vous avez selectionné le projet ou sujets :  : [" + subject_ids + "]");

        log("l Export path params   est : " + nameExportDir );


        /**
         *  On récupère la list des sujets ou des sessions choisies
         *
         */

        try {
            //System.out.println("Subjects = " + subject_ids);
            if ("all".equals(subject_ids)) {
                final QueryOrganizer qo = new QueryOrganizer("xnat:subjectData", xnatUser, "all");
                final CriteriaCollection cc = new CriteriaCollection("OR");
                cc.addClause("xnat:subjectData/project", (Object)id_project);
                cc.addClause("xnat:subjectData/sharing/share/project", (Object)id_project);
                qo.setWhere(cc);
                final String query = qo.buildQuery();
                final XFTTable table = XFTTable.Execute(query, xnatUser.getDBName(), xnatUser.getLogin());
                for (final Object[] array : table.rows()) {
                    final Object[] row = array;
                    for (final Object o : array) {
                        subjectsList.add(o.toString());
                    }
                }
            }
            else {
                final String[] split;
                final String[] tokens = split = subject_ids.split(",");
                for (final String token : split) {
                    subjectsList.add(token);
                }
            }


            final List<String> ssid = new ArrayList<String>();
                for (final String sid : subjectsList) {
                    log("subject   [" + sid + "]...");
                    final XnatSubjectdata xnatSubject = XnatSubjectdata.getXnatSubjectdatasById((Object)sid, xnatUser, false);
                    ssid.add(xnatSubject.getLabel());
                }
                final String listOfSubject = String.join(" ", ssid);
                listOfSubjectWithSpaceSeparated = listOfSubject;
                listOfSubjectWithCamasSeparated = String.join(",",ssid);
                log("list of subject " + listOfSubject);


        }catch ( Exception e) {
            e.printStackTrace();
        }


        log("Le pipeline Choisi est : " + selectPipeline + "");

        /*- Tester si le fichier de config n'est pas encore lu -*/
        if(jsonObject.equals(null)){

            /* Lire le fichier et initialisation des params */
            try {

                readConfigFileJson();

            } catch (IOException ioe){
                ioe.printStackTrace();

            } catch (Exception e){
                e.printStackTrace();

            }
        }

        pipeLineSelected = (String) ((JSONObject) getJsonObjectByKey(jsonObject,selectPipeline)).get("name");
        String xnat_batch_scripts = (String) jsonObject.get("xnat_batch_scripts");
        String data_xnat = (String) jsonObject.get("data_xnat");

        String exportDirectory = nameExportDir + "/" + data_xnat ;



        log("Vous avez choisi le pipeline : " +pipeLineSelected + "\n");
        log("Le script généré est  : \n ");

        datTimeNow = getDateTimeNow();

        sifOrSimg(selectPipeline);

        if (!additionalParams.equals(null)){
            ADDITIONAL_PARAMS = additionalParams;
        } else {
            ADDITIONAL_PARAMS = "\n";
        }



        inputAndOutputDirectory = nameExportDir + "/" + data_xnat + "/" + idCluster + "_" +  selectPipeline + "_" + id_project + "_" + datTimeNow;
        pathErrorLogOut = "/home/"+ idCluster  + "/" + xnat_batch_scripts + "/" + idCluster + "_" + selectPipeline + "_" + id_project + "_" + datTimeNow + ".out";
        pathErrorLogErr = "/home/"+ idCluster  + "/" + xnat_batch_scripts + "/" + idCluster + "_" + selectPipeline + "_" + id_project + "_" + datTimeNow + ".err";

        SCRIPT_SBATCH_GLOBAL = SCRIPT_SBATCH_GLOBAL
            + SCRIPT_SBATCH
            + NUMBER_OF_JOBS_PER_NODES
            + NUMBER_OF_NODES
            + CHANGE_WORKING_DIRECTORY
            + GET_USER_ENV
            + STANDARD_OUTPUT_FILE + pathErrorLogOut
            + "\n" + STANDARD_ERROR_FILE + pathErrorLogErr
            + "\n" + NAME_OF_SLURM_JOB + " " + idCluster + "_" + selectPipeline
            + "\n" + getOtherParamatersSbatch(selectPipeline)
            + "\n" + LOAD_MODULES
            + "\n" +  "if [ ! -d \"" + exportDirectory +"\"  ]; then"
            + "\n" +  "   mkdir -p " + exportDirectory
            + "\n" +  "   echo \"Creating of directory " + exportDirectory + " \""
            + "\n" +  "   chmod -R +777 " + exportDirectory
            + "\n" +  "fi"
            + "\n" +  "\n" + "\n" + "\n"
            + "\n" + "" + commandeDownloadData(listOfSubjectWithCamasSeparated, id_project, inputAndOutputDirectory, listOfSubjectWithSpaceSeparated, sessions_ids, command_extra_bids)
            + "\n" + LOAD_IMG_SINGULARITY
            /*+ "\n" + whichCommandSingularity(selectPipeline, inputAndOutputDirectory)*/
            + "\n" + prepareCommandSingularity(selectPipeline,inputAndOutputDirectory,id_project, commande_befor, commande_after, commande_participant)
            + "  " + additionalParams + "\n"
            + "\n" + deleteOrNotDataAfterConversionToBIDS(radioValue,inputAndOutputDirectory + "/" + id_project);

            log(SCRIPT_SBATCH_GLOBAL);

        final String namFileGenerated = generateFIleScripte(SCRIPT_SBATCH_GLOBAL, selectPipeline, idCluster, id_project);

        log( "Le fichier à envoyer est  " + namFileGenerated );


        log("\nLa commade générée est : ");
        log(prepareCommandSingularity(selectPipeline,inputAndOutputDirectory,id_project, commande_befor, commande_after, commande_participant));

        // To send file to the cluster

        try{

           ligneRetunrCommandeStartPipeline = sendFileToCluster(namFileGenerated, idCluster);

           log("\nLe fichier a été envoyer avec succé");

           System.out.println(" Voici le contenu de la ligne " + ligneRetunrCommandeStartPipeline);
        } catch (InterruptedException e) {

            e.printStackTrace();

        }catch (JSchException  jshe){

            jshe.printStackTrace();

        }catch (IOException ioe){

            ioe.printStackTrace();

        }catch (SftpException sftpe){
            sftpe.printStackTrace();
        }

        log("done !");




        /* Preparé les données à envoyer:
        * ce sont les informations qui  corréspond au lancement du calcul afin qu'un utilsateur puisse les sauvegarder
        * en .txt garder une trace
        */
        response.setContentType("application/json");

        response.setCharacterEncoding("UTF-8");

        JSONObject obj = new JSONObject();

        String pathWithnNameScriptSbatch = "/home/" + idCluster + "/" + xnat_batch_scripts + "/" + namFileGenerated;

        obj.put("idJob",ligneRetunrCommandeStartPipeline);
        obj.put("workindDirectory", inputAndOutputDirectory);
        obj.put("pathWithnNameScriptSbatch", pathWithnNameScriptSbatch);
        obj.put("pathErrorLogOut", pathErrorLogOut);
        obj.put("pathErrorLogErr", pathErrorLogErr);

        log("les sessions à envoyer sont : " +obj.toString());

        PrintWriter out = response.getWriter();

        out.print(obj);

        out.flush();



    }


    /* Return la commande singularity selon le pipeline choisi */
    public String prepareCommandSingularity(String selectPipeline, String inputAndOutputDirectory, String id_project, String commande_befor,
        String commande_after, String commande_participant){

        /* Le répértoire où sont les données en BIDS */
        String dirData = inputAndOutputDirectory + "/" + id_project + "BIDS";

        String singulartyRun = (String) jsonObject.get("singulartyRun");
        String linkImgeSingularity = (String) jsonObject.get("linkAllImgSingularity");
        String name =  (String) ((JSONObject) getJsonObjectByKey(jsonObject,selectPipeline)).get("name");
        String singularityCleanEnv = (String) ((JSONObject) getJsonObjectByKey(jsonObject,selectPipeline)).get("singularityCleanEnv");
        String inputDataBids = (String) ((JSONObject) getJsonObjectByKey(jsonObject,selectPipeline)).get("inputDataBids");
        String output = (String) ((JSONObject) getJsonObjectByKey(jsonObject,selectPipeline)).get("output");
        String path_licence =  (String) ((JSONObject) getJsonObjectByKey(jsonObject,selectPipeline)).get("path_licence");
        String licence_Params = (String) ((JSONObject) getJsonObjectByKey(jsonObject,selectPipeline)).get("licence_Params");
        String output_key = (String) ((JSONObject) getJsonObjectByKey(jsonObject,selectPipeline)).get("output_key");
        String data_key = (String) ((JSONObject) getJsonObjectByKey(jsonObject,selectPipeline)).get("data_key");
        //String commande_befor = (String) ((JSONObject) getJsonObjectByKey(jsonObject,selectPipeline)).get("commande_befor");
        //String commande_after = (String) ((JSONObject) getJsonObjectByKey(jsonObject,selectPipeline)).get("commande_after");
        //String commande_participant = (String) ((JSONObject) getJsonObjectByKey(jsonObject,selectPipeline)).get("commande_participant");
        String work_dir_params = (String) ((JSONObject) getJsonObjectByKey(jsonObject,selectPipeline)).get("work_dir_params");
        String basicParameters = (String) ((JSONObject) getJsonObjectByKey(jsonObject,selectPipeline)).get("basicParameters");

        String commande = "";


        /* Préparation de la commande */
        // On exclu bids_validator car il ne fait qu'un test de vérification si les données sont en BIDS, du coup
        // il a pas besoin de répertoire des outputs

        if(!selectPipeline.contains("bids_validator")){

        commande  = commande + singulartyRun
                    + singularityCleanEnv
                    + " " + "-B " + dirData + ":/" +  inputDataBids
                    + " " + "-B " + inputAndOutputDirectory + ":/" + output
                    + " " + path_licence
                    + " " + linkImgeSingularity + "/" + name
                    + " " + licence_Params
                    + " " + commande_befor
                    + " " + data_key + " " + "/" +  inputDataBids
                    + " " + output_key + " " + "/" + output
                    + " " + commande_participant
                    + " " + work_dir_params
                    + " " + commande_after
                    + " " + basicParameters;
        } else {

            commande  = commande + singulartyRun
                    + singularityCleanEnv
                    + " " + "-B " + dirData + ":/" +  inputDataBids
                    + " " + linkImgeSingularity + "/" + name
                    + " " + data_key + " " + "/" +  inputDataBids;
        }

                    /*if(!selectPipeline.contains("bids_validator")){

                        commande = commande + "";

                    }*/

        return commande;


    }


    /* Cette fonction permet de générer  la commande qui permet de supprimer (ou non) les donner après la conversion en bids */
    public String deleteOrNotDataAfterConversionToBIDS(String selectPipeline , String pathDataToDelet){

        if(selectPipeline.equals("yes")){
            return "rm -rf " + pathDataToDelet + "\n";
        }
        else {
            return "";
        }

    }


    /* Cette fonction permet de récuperer les la liste des sessions corréspondant à un sujet */
    @ApiOperation(value = "Get list of session in subject", notes = "Custom")
    @ApiResponses({ @ApiResponse(code = 200, message = "Connection success "), @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT Rest Api"), @ApiResponse(code = 500, message = "Unexpected internal serval error") })
    @RequestMapping(value = { "/get-sessions/{id_project}" }, produces = { "application/json" }, method = { RequestMethod.POST })
    @ResponseBody
    public void getSessionsOfSubjects(final HttpServletResponse response, @PathVariable final String id_project, @RequestParam("selectedSubject") final String selectedSubject)  throws IOException{
        final UserI xnatUser = XDAT.getUserDetails();
        ByteArrayOutputStream baos = null;
        final List<String> subjectsList = new LinkedList<String>();
        Map<String, String> listSessionOfSubject  = new HashMap<>();
        String [] listAllsession = null; // mettre toute les sessions d'un projet

        String[] arrayOfselectedSubject = selectedSubject.split(",");


        String dirName = "/data/xnat/archive/" + id_project + "/arc001";

        log("Vous avez choisi  [ " + id_project + " ] et sujet(s) : [ " + selectedSubject + " ]");




        File fileName = new File(dirName);

        File[] fileList = fileName.listFiles();

        listAllsession = new String[fileList.length];


        for (String subject : arrayOfselectedSubject) {

            for(int i= 0; i< listAllsession.length; i++){

                listAllsession[i] = fileList[i].getName();
                // tester si le nom commence par le préfix du sujet
                if (listAllsession[i].startsWith(subject + "_"))
                {
                    listSessionOfSubject.put(listAllsession[i],listAllsession[i]);

                    log(" " + listAllsession[i] + " : " + listAllsession[i]);
                }

            }
        }


        // Préparer le listSessionOfSubject à envoyer en json

        response.setContentType("application/json");

        response.setCharacterEncoding("UTF-8");

        JSONObject obj = new JSONObject();

        obj.putAll(listSessionOfSubject);

        log("les sessions à envoyer sont : " +obj.toString());

        PrintWriter out = response.getWriter();

        out.print(obj);

        out.flush();

    }


    /* Cette fonction permet de récuperer la liste des noms des groupes corréspondante à un utilisateur */
    @ApiOperation(value = "Get list of team names", notes = "Custom")
    @ApiResponses({ @ApiResponse(code = 200, message = "Connection success "), @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT Rest Api"), @ApiResponse(code = 500, message = "Unexpected internal serval error") })
    @RequestMapping(value = { "/get-team-names" }, produces = { "application/json" }, method = { RequestMethod.POST })
    @ResponseBody
    public void getTeamNames(final HttpServletResponse response, @RequestParam("idCluster") final String idCluster)  throws IOException{

        final UserI xnatUser = XDAT.getUserDetails();
        final String userName = xnatUser.getUsername().replace("_", ".");
        String ligne = "";
        System.out.println(" Votre id xnat est : " + userName);

       /* Read config file and initialize  params */
        try {

            readConfigFileJson();

        } catch (IOException ioe){

        } catch (Exception e){

        }

        JSONArray teamNames = (JSONArray) jsonObject.get("teamNames");
        JSONObject js = new JSONObject();


        try {

            final String program = "groups";
            //final String programName = userName ;
            final List<String> cmd = new ArrayList<String>();
            cmd.add(program);
            cmd.add(idCluster);
            log(cmd.toString());

            final ProcessBuilder pb = new ProcessBuilder(cmd);
            final Process p = pb.start();
            final InputStreamReader isr = new InputStreamReader(p.getInputStream());
            final BufferedReader br = new BufferedReader(isr);
            ligne = br.readLine();

            System.out.println("les groupes de " + userName + " sont  : " + ligne);
        }
        catch (Exception e) {
            log(e.toString());
        }


        if(ligne != null && !("admin-xnat-root".contains(idCluster)) ){


            String []  tabTeamUser = ligne.split(" ");
            String [] array = new String[teamNames.size()] ;
            int i=0;
            for (Object ele : teamNames){
                array[i] = ele.toString();
                i++;
            }

            HashSet<String> set = new HashSet<>();

            set.addAll(Arrays.asList(tabTeamUser));

            set.retainAll(Arrays.asList(array));


            if(set.size()>=1){


                System.out.println(set);

                //convert to array
                String[] intersection = {};
                intersection = set.toArray(intersection);

                System.out.println(Arrays.toString(intersection));

                final Map<String, String> listTeam  = new HashMap<>();
                // Stockage des teams dans une HashMap
                for (String el : intersection) {
                    listTeam.put(el, el);
                }

                js.putAll(listTeam);

             }else {
                js.put("data","null");
             }

        } else {
            js.put("data", "null");
        }

        response.setContentType("application/json");

        response.setCharacterEncoding("UTF-8");


        log("Les noms des équipes à envoyer sont : " +teamNames.toString());

        PrintWriter out = response.getWriter();

        out.print( js);

        out.flush();

    }

    /* Cette méthode permet de checker si l'id user fourni par l'utilisateur est bien correcte.  */
    public boolean checkIfIdUserExist(String idUser){

        String ligne = "";

        try {

            final String program = "groups";
            //final String programName = userName ;
            final List<String> cmd = new ArrayList<String>();
            cmd.add(program);
            cmd.add(idUser);
            log(cmd.toString());

            final ProcessBuilder pb = new ProcessBuilder(cmd);
            final Process p = pb.start();
            final InputStreamReader isr = new InputStreamReader(p.getInputStream());
            final BufferedReader br = new BufferedReader(isr);
            ligne = br.readLine();

            System.out.println("le res de groups " + idUser + " est : "+ ligne);
        }
        catch (Exception e) {
            log(e.toString());
        }

        if(ligne != null){
            return true;
        }
            //return !(ligne.contains("no such user"));
        else return false;

    }

    /* la méthode qui permet de  recupere le contenu la liste des images singularity sur niolon */
    public 	static  String[] recupererListImgCluster(String resultConsole){

    	listImages = resultConsole.split("\\r?\\n");
        System.out.println("\n la taille de tableau lines est  : "+listImages.length +"\n");

    	return listImages;
    }



    /* Cette a pour fonction d'établir une connection avec cluster de calcul */
    public void doConnectionCluster(String password) throws   InterruptedException, JSchException, IOException  {

        String remoteFile = "ls";

        //String  commande= "niolon_interactive; cd /hpc/crise/rahmani.a/script; ls";

        try {

            JSch.setLogger(new MyLogger());

            JSch jsch = new JSch();


        /*
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");

            config.put("StrictHostKeyChecking", "no");
            config.put("PreferredAuthentications", "publickey,keyboard-interactive,password");
        */


            Properties config = new Properties();

            config.put("kex", "diffie-hellman-group1-sha1,diffie-hellman-group14-sha1,diffie-hellman-group-exchange-sha1,diffie-hellman-group-exchange-sha256");

            //session.setConfig("cipher.s2c", "aes128-ctr");
            //session.setConfig("cipher.c2s", "aes128-ctr");

            config.put("StrictHostKeyChecking", "no");

            session = jsch.getSession(user, host, port);

            //jsch.addIdentity("/var/lib/tomcat08/.ssh");



            session.setPassword(password);



            session.setConfig(config);


            System.out.println("Establishing Connection...");

            session.connect();

            System.out.println("Connection established.");

            log("********************** \n ");

            log("Connection established");


            // Execution de la commande

            channel = (ChannelExec) session.openChannel("exec");

            channel.setCommand("ls /hpc/shared/apps/x86_64/softs/singularity_BIDSApps");
            //channel.setCommand("ls");

            while (channel.isConnected()) {
                Thread.sleep(1000);
            }

            //channel.setCommand("cd /hpc/crise/rahmani.a/script ");
           // channel.setCommand("ls /hpc/shared/apps/x86_64/softs/singularity_BIDSApps");

            ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
            channel.setOutputStream(responseStream);
            channel.connect();

            System.out.println(" ls success");

            log("ls success");

            while (channel.isConnected()) {
                Thread.sleep(100);
            }

            String responseString = new String(responseStream.toByteArray());
            System.out.println(responseString);

            /* On appel la fonction de recupération de l'enssemble des images*/
            log(recupererListImgCluster(responseString).toString());



			/*

            System.out.println("Crating SFTP Channel.");
            ChannelSftp sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();
            System.out.println("SFTP Channel created.");

            InputStream inputStream = sftpChannel.get(remoteFile);

            try (Scanner scanner = new Scanner(new InputStreamReader(inputStream))) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    System.out.println(line);
                }
            }*/

        } catch (JSchException e) {
            e.printStackTrace();

        }finally {
            if (session != null) {
                session.disconnect();
                //outputStream.close();
				//inputStream.close();

            }
            if (channel != null) {
                channel.disconnect();
                //channelShell.disconnect();
            }
        }



    }

    /* Cette fonction a pour but d'envoyer le fichier généré au cluster de calcul */
    public  String sendFileToCluster( String nameFileGenerated, String idCluster) throws InterruptedException, JSchException, IOException, SftpException {

        /* Chemin vers le local file */
        String localFile = fullPathScriptSlurm;

        log("Le fichier à envoyer est : " + fullPathScriptSlurm );
        /* Chemin où sera  transmit le fichier dans le cluster */
        String remoteDir = "/home/"+idCluster;

        String ligne = "";

        try {
                final String program = "sudo";
                final String programName = "/opt/bin/xnat-rpd.sh" ;
                final String option0 = "-u";
                final String option1 = idCluster;
                final String option2 = "-f";
                final String option3 = localFile;
                final List<String> cmd = new ArrayList<String>();
                cmd.add(program);
                cmd.add(programName);
                cmd.add(option0);
                cmd.add(option1);
                cmd.add(option2);
                cmd.add(option3);
                log(cmd.toString());

                final ProcessBuilder pb = new ProcessBuilder(cmd);
                final Process p = pb.start();
                final InputStreamReader isr = new InputStreamReader(p.getInputStream());
                final BufferedReader br = new BufferedReader(isr);

                ligne = br.readLine();

               /* while ((ligne = br.readLine()) != null) {
                    log(ligne);
                }*/


            }
            catch (Exception e) {
                log(e.toString());
            }




/*         try {

            JSch.setLogger(new MyLogger());

            JSch jsch = new JSch();

            //jsch.setKnownHosts("~/.ssh/known_hosts");
            Properties config = new Properties();

            //config.put("kex","curve25519-sha256, curve25519-sha256@libssh.org, diffie-hellman-group16-sha512, diffie-hellman-group18-sha512, diffie-hellman-group-exchange-sha256");

            config.put("kex", "diffie-hellman-group1-sha1,diffie-hellman-group14-sha1,diffie-hellman-group-exchange-sha1,diffie-hellman-group-exchange-sha256");

            //session.setConfig("cipher.s2c", "aes128-ctr");
            //session.setConfig("cipher.c2s", "aes128-ctr");

            config.put("StrictHostKeyChecking", "no");



            session = jsch.getSession(user, "niolon.int.univ-amu.fr", port);

            session.setPassword(password);


            session.setConfig(config);

            log("Establishing Connection...");

            session.connect();


            log("Connection established...");

            System.out.println("Crating SFTP Channel...");

            ChannelSftp sftpChannel = (ChannelSftp) session.openChannel("sftp");


            try {

                sftpChannel.connect();

            } catch (Exception e) {
                throw new JSchException("Unable to connect to SFTP server. " + e.toString());
            }


           // sftpChannel.connect();

            log("SFTP Channel created.");

            // la méthod  put pour envoyer le fichier
            sftpChannel.put(localFile, remoteDir);

            //sftpChannel.put("/var/lib/tomcat8/Bureau/xnat2bids_reconstruct.py", remoteDir);



            log(" submit  file with sucess ");

            sftpChannel.exit();


            channelShell = (ChannelShell) session.openChannel("shell");

            inputStream = channelShell.getInputStream();//The data arriving from the far end can be read from this stream.

            channelShell.setPty(true);

            channelShell.connect();

            log ("sell  channel is opned ! ");

            outputStream = channelShell.getOutputStream();

            PrintWriter printWriter = new PrintWriter(outputStream);

            printWriter.println("chmod +x " + remoteDir + "/" + nameFileGenerated);

            log("le scripte slum reçu est ====> "+remoteDir + "/" + nameFileGenerated);

            printWriter.println(" sbatch ./"+nameFileGenerated);


            printWriter.flush () ; // force the buffer data output


            byte[] tmp = new byte[1024];
            while(true){

                while(inputStream.available() > 0){
                    int i = inputStream.read(tmp, 0, 1024);
                    if(i < 0) break;
                    String s = new String(tmp, 0, i);
                    if(s.indexOf("--More--") >= 0){
                        outputStream.write((" ").getBytes());
                        outputStream.flush();
                    }
                    //System.out.println(s);
                }
                if(channelShell.isClosed()){
                    System.out.println("exit-status:"+channelShell.getExitStatus());
                    break;
                }
                try{
                    Thread.sleep(1000);
                }catch(Exception e){

                }

            }

        }catch (JSchException e) {

            e.printStackTrace();

        }finally {

            if (session != null) {
                session.disconnect();
                outputStream.close();
                inputStream.close();

                }
            if (channel != null) {
                channel.disconnect();
                channelShell.disconnect();
            } */


            return ligne;


        }



/*         final SSHClient sshClient = new SSHClient();
        sshClient.loadKnownHosts();
        sshClient.connect("pharo.int.univ-amu.fr");






        try {
            sshClient.authPassword(user, password);
            //final Session session1 = sshClient.startSession();
            try {

                sshClient.authPublickey(System.getProperty(user));
                //final String src = System.getProperty("user.home") + File.separator + "test_file";
                sshClient.newSCPFileTransfer().upload(new FileSystemFile(localFile),remoteDir);


            } finally {
                //session1.close();
            }
        } finally {
            sshClient.disconnect();


    } */



    public static String getDateTimeNow(){

        final Calendar now = Calendar.getInstance();
        String dateTime = "" + now.get(1);
        dateTime += "-";
        if (now.get(2) + 1 < 10) {
            dateTime += "0";
        }
        dateTime += now.get(2) + 1;
        dateTime += "-";
        if (now.get(5) < 10) {
            dateTime += "0";
        }
        dateTime += now.get(5);
        dateTime += "_";
        if (now.get(11) < 10) {
            dateTime += "0";
        }
        dateTime += now.get(11);
        dateTime += "h";
        if (now.get(12) < 10) {
            dateTime += "0";
        }
        final Date d = new Date();
        dateTime = dateTime + now.get(12) + "" + "_" + d.getSeconds();

        return  dateTime;

    }

    /* pour détérminer si l'img termine par .sif ou  */
    public void sifOrSimg(String imageSingularity){

        if (imageSingularity.contains(".sif")){

        LOAD_IMG_SINGULARITY = "\nmodule load singularity\n";

        }
        else {

            LOAD_IMG_SINGULARITY = "\n\n";

        }


    }



    // A partir de cette méthode on génère le fichier final à soumetre au cluster
    // Penser à passer le nom de projet en paramétre
    public static String generateFIleScripte(String codeSbatch, String pipeLineSelected, String userIdCluster , String idProject) {

      String    dirOutputpath = "/tmp/";
      String suiteName = userIdCluster + "_" + pipeLineSelected + "_" + idProject + "_" + datTimeNow + ".sh";

      /* On concerver le nom  pour récupérer le bon fichier */
      fullPathScriptSlurm = dirOutputpath+suiteName;


      File f = new File(dirOutputpath+suiteName);

      //f.setExecutable(true);

      System.out.println(f.exists());
      log("Le fichier a été créer ! ");

      if(!f.getParentFile().exists()) {
          f.getParentFile().mkdirs();

      }

      if(!f.exists()) {
         try {
            f.createNewFile();
         } catch (Exception e) {
            e.printStackTrace();
         }
      }

      try {
         File dir = new File(f.getParentFile(), f.getName());
         PrintWriter pWriter = new PrintWriter(dir);
         // Ajouter le contenu du script au fichier slurm
         pWriter.print(codeSbatch);
         //pWriter.print("writing anything..    . \n");
         //pWriter.print("writing anything..    .");

         pWriter.close();
      } catch (FileNotFoundException e) {
         e.printStackTrace();
      }
        try{

            Runtime rt = Runtime.getRuntime();
            Process proc = rt.exec("chmod +777 " + fullPathScriptSlurm );

        }catch (Exception e) {
                log(e.toString());
        }

        return suiteName;
    }

    /* Cette fonction permet de lire le contenu du fichier de conf   json sur xnat */
    public static void readConfigFileJson() throws Exception, IOException {

        listKeysJsonFile = new ArrayList<String>();
		// Parse json file
		JSONParser parser = new JSONParser();

		try {


		 jsonObject =  (JSONObject) parser.parse(new FileReader(config_file_xnat));

		 System.out.println("Le fichier de config a été bien lu et parsé ");


		} catch (IOException e) {

            e.printStackTrace();
            System.out.println(e.getMessage());

		}

		System.out.println("\n\n------------- Debut du programme ----------------\n");


		//System.out.println(jsonObject);

		System.out.println("This file contain " + jsonObject.size() + " objects");

		System.out.println("List of  objects ");

		System.out.println(jsonObject.keySet());

		JSONObject objectJsonOfPipeline = null;

		for (Object key : jsonObject.keySet()) {

			System.out.println(key);

			listKeysJsonFile.add((String) key);

			try {

					objectJsonOfPipeline = (JSONObject) jsonObject.get(key);

					getInfoJsonObject1(objectJsonOfPipeline);

					System.out.println("\n\n\n\n");


			} catch (Exception e) {

			}

		}


		System.out.println( listKeysJsonFile);

		System.out.println("\n---------------------------------\n");


    }


	 /* Cette méthode permet de récupérer un Object JSON  par sa clé */

    public static Object getJsonObjectByKey(JSONObject jsonObject, Object key) {

			return jsonObject.get(key);

    }

    /*
     * Pour chaque objet JSON dans le fichier de config
     * on récupére toutes les  (key, value)
     */

	public Map< String, String> getInfoJsonObject(JSONObject jsonObject){

		Map<String, String> jsonContent =  new HashMap<String, String>();

				for (Object key : jsonObject.keySet()) {

					jsonContent.put(key.toString(), jsonObject.get(key).toString());

				}

		return jsonContent;
	}


	public static void getInfoJsonObject1(JSONObject jsonObject){

		Map<String, String> jsonContent =  new HashMap<String, String>();

				for (Object key : jsonObject.keySet()) {

					jsonContent.put(key.toString(), jsonObject.get(key).toString());
					System.out.println("\"" + key.toString() + "\" : " + " \"" + jsonObject.get(key).toString() + "\"");

				}
	}


    public static String readFileAsString(String file)throws Exception {
        return new String(Files.readAllBytes(Paths.get(file)));

    }

    /*
     * Cette fonction s'occupe de générer la commande Xnatdownload pour télécharger les données selon les chox de l'usr
     */
    public static String  commandeDownloadData(String subjectSelected, String projectName, String dirIputdata, String lisSubjectWithSpaceSeparated, String sessions_ids, String command_extra_bids){

        /* L'uri de XNAT */
        URI_HOST_XNAT = (String) jsonObject.get("URI_HOST_XNAT");
        String xnat2bids = (String) jsonObject.get("xnat2bids");

        String filenameTxt = "\"" + dirIputdata + "/download_commandLine.txt" +"\"";

        String filenameCsv = "\"" + dirIputdata + "/download_report.csv" +"\"";

        String dirDataInBIDS =  dirIputdata + "/" + projectName + "BIDS";

        String commande = "source activate dax_env\n"
                + "\n" + "filenameTxt=" + filenameTxt
                + "\n" + "filenameCsv=" + filenameCsv
                + "\n \n" + "mkdir -p " + dirIputdata
                + "\n" + "Xnatdownload "+ URI_HOST_XNAT + " -p " + projectName + " -d " + dirIputdata;

          if(!allOrListSubject.equals("all") && (sessions_ids.equals(""))){

            commande +=   " --subj " + subjectSelected + "  -s all --rs NIFTI,BIDS \n";

          } else if(!allOrListSubject.equals("all") && !(sessions_ids.equals(""))){

            commande += " --sess " + sessions_ids + " -s all --rs NIFTI,BIDS";

          }else {

            commande += " all -s all --rs NIFTI,BIDS \n";

          }

          commande += "\n"
                    + "\n" +  "if [ -f $filenameTxt  ]; then"
                    + "\n" +  "   rm $filenameTxt"
                    + "\n" +  "   echo \"download_txt removed\""
                    + "\n" +  "fi"
                    + "\n" +  "if [ -f $filenameCsv  ]; then"
                    + "\n" +  "   rm $filenameCsv"
                    + "\n" +  "   echo \"download_csv removed\""
                    + "\n" +  "fi"
                    + "\n" +  "\n" + "\n" + "\n"
                    + "\n" +  "dirDataInBIDS=\"" + dirDataInBIDS + "\"\n"
                    + "\n" +  "python " + xnat2bids + " "
                    + dirIputdata + "/" + projectName + " " + "$dirDataInBIDS" + " " + command_extra_bids
                    + "\n";
                    //+  " " + lisSubjectWithSpaceSeparated +"\n";


          commande += "\nsource deactivate \n\n";

        // faut supprimer les fichier générer par Xnatdownload
        return commande;

    }

    /* Cette methode renvoi d'autre paramétre sbatch a rajouter au script en slurm */
    public String getOtherParamatersSbatch(String selectPipeline){

        String commande = "";

        switch(selectPipeline){

            case "fmriprep_20.2.3.simg" :
               commande += "\n" + "#SBATCH --mem=48gb"
                         + "\n" + "#SBATCH --cpus-per-task=64"
                         + "\n" + "#SBATCH --time=20:00:00"
                         + "\n";
                break;

            case "   " :

                break;

            default:
                commande = "";
        }

        return commande;
    }



    public String whichCommandSingularity(String whichPipeline, String inputDirBIDS){

        if (whichPipeline.contains("fmriprep")) {

            return getCommandeFmriPrepSimg(whichPipeline, inputDirBIDS);

        }

        if (whichPipeline.contains("macapype")) {

            return getCommandeMacapype(whichPipeline, inputDirBIDS);
        }

        if (whichPipeline.contains("mriqc")) {

            return getCommandeMriqc(whichPipeline, inputDirBIDS);
        }

        if(whichPipeline.contains("bids_validator")){

            return getCommandeBidsValidator(whichPipeline,inputDirBIDS);
        }

        else return "";

    }


    // version : sera la commande passée
    public String  getCommandeMacapype(String version, String inputDirBIDS){

        String dirData = inputDirBIDS + "/" + ID_PROJECT + "BIDS";


        String commande = "\n" + "singularity run -B " + dirData + ":/data/macapype "
                        + " " + "-B " + inputDirBIDS + ":/out ";
        commande += "/hpc/shared/apps/x86_64/softs/singularity_BIDSApps/" + version + " ";

        commande += "segment_pnh "
                  + "-data /data/macapype/ "
                  + "-out /out/" + "output" + ID_PROJECT + "_" + datTimeNow + " "
                  + "-soft ANTS -species macaque ";

    /*     commande += "python /opt/packages/macapype/workflows/segment_pnh.py "
                  + "-data /data/macapype/ "
                  + "-out /out/" + "output" + ID_PROJECT + "_" + datTimeNow + " "
                  + "-soft ANTS -params /opt/packages/macapype/workflows/params_segment_macaque_ants_based.json "; */

    return commande;
    }


    public String getCommandeFmriPrepSimg(String version , String inputDirBIDS){

        String dirdata = inputDirBIDS + "/" + ID_PROJECT + "BIDS";

        String commande = "\n" + "singularity run --cleanenv -B " + dirdata + ":/work_dir "
                        + "-B " + inputDirBIDS + ":/output "
                        + "-B /hpc/shared/apps/x86_64/softs/freesurfer/7.1.1:/license_path "
                        + "/hpc/shared/apps/x86_64/softs/singularity_BIDSApps/" + version + " "
                        + "--fs-license-file /license_path/.license "
                        + "/work_dir" + " /output "
                        + "participant -w /output/temp_data_test/ "
                        + ""
                        + "--cifti-output --low-mem --mem-mb 32000 --nthreads 64";

                        /*
                        + "--fs-license-file /license_path/.license "
                        + "/workdir /workdir/derivatives/fmriprep "
                        + "participant --participant-label 01 -w /work_dir/temp_data_test/ "
                        + "--cifti-output --low-mem --mem-mb 32000 --nthreads 64";
                        */

        return commande;
    }



    public String getCommandeMriqc(String version , String workDir){

        String dirData = workDir + "/" + ID_PROJECT + "BIDS";

        String commande = "\n" + "singularity run --cleanenv -B " + dirData + ":/data "
                        + "-B " + workDir + ":/out "
                        + "/hpc/shared/apps/x86_64/softs/singularity_BIDSApps/" + version + " "
                        + "--verbose-reports "
                        + "/data /out/" + "output" + ID_PROJECT + "_" + datTimeNow + " participant "
                        + " group";

        return commande;

    }


    /* BIDSApps bida_validator  */
    public String getCommandeBidsValidator(String version , String dataset){

        String dirData = dataset + "/" + ID_PROJECT + "BIDS";

        String commande = "\n" + "singularity run --cleanenv -B " + dirData + ":/data "
                        + "/hpc/shared/apps/x86_64/softs/singularity_BIDSApps/" + version + " "
                        + "/data"
                        + "\n";

        return commande;

    }



    public ZipOutputStream exportSubjectFiles(final ZipOutputStream zos, final String id_project, final String fileName, final List<String> subjectsList, final List<String> listOfSubject) {
        final UserI xnatUser = XDAT.getUserDetails();
        try {
            final File FolderFile = new File("/data/xnat/tmp/" + fileName);
            FolderFile.mkdirs();
            try {
                final String program = "python";
                final String programName = "xnat2bids_reconstruct.py";
                final String inputDir = XDAT.getSiteConfigPreferences().getArchivePath() + "/" + id_project;
                final String outputDir = FolderFile.toString();
                final String _tempProjectPath = XDAT.getSiteConfigPreferences().getArchivePath();
                final List<String> cmd = new ArrayList<String>();
                cmd.add(program);
                cmd.add(programName);
                cmd.add(inputDir);
                cmd.add(outputDir);
                cmd.addAll(listOfSubject);
                final ProcessBuilder pb = new ProcessBuilder(cmd);
                final Process p = pb.start();
                final InputStreamReader isr = new InputStreamReader(p.getInputStream());
                final BufferedReader br = new BufferedReader(isr);
                String ligne = "";
                while ((ligne = br.readLine()) != null) {
                    log(ligne);
                }
            }
            catch (Exception e) {
                log(e.toString());
            }
            return zos;
        }
        catch (Exception e2) {
            e2.printStackTrace();
            return null;
        }
    }


    public static void getAllFiles(final File dir, final List<File> fileList) {
        try {
            final File[] listFiles;
            final File[] files = listFiles = dir.listFiles();
            for (final File file : listFiles) {
                fileList.add(file);
                if (file.isDirectory()) {
                    System.out.println("directory:" + file.getCanonicalPath().toString());
                    getAllFiles(file, fileList);
                }
                else {
                    System.out.println("     file:" + file.getCanonicalPath().toString());
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ZipOutputStream writeZipFile(final File directoryToZip, final List<File> fileList) {
        try {
            final FileOutputStream fos = new FileOutputStream("/data/xnat/tmp/" + directoryToZip.getName() + ".tar.gz");
            log("Path file export : "+directoryToZip.getName());
            final ZipOutputStream zos = new ZipOutputStream(fos);
            for (final File file : fileList) {
                if (!file.isDirectory()) {
                    addToZip(directoryToZip, file, zos);
                }
            }
            zos.close();
            fos.close();
            return zos;
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        catch (IOException e2) {
            e2.printStackTrace();
            return null;
        }
    }

    public static void addToZip(final File directoryToZip, final File file, final ZipOutputStream zos) throws FileNotFoundException, IOException {
        final FileInputStream fis = new FileInputStream(file);
        final String zipFilePath = file.getCanonicalPath().substring(directoryToZip.getCanonicalPath().length() + 1, file.getCanonicalPath().length());
        System.out.println("Writing " + zipFilePath.toString() + " to zip file");
        final ZipEntry zipEntry = new ZipEntry(zipFilePath);
        zos.putNextEntry(zipEntry);
        final byte[] bytes = new byte[4096];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zos.write(bytes, 0, length);
        }
        zos.closeEntry();
        fis.close();
    }

    public static void clean(final String fileName) throws FileNotFoundException, IOException {
        FileUtils.deleteDirectory(new File("/data/xnat/tmp/" + fileName));
        FileUtils.forceDelete(new File("/data/xnat/tmp/" + fileName + ".tar.gz"));
    }

    public void zip2(final String fileName) {
        try {
            final ProcessBuilder pb = new ProcessBuilder(new String[] { "python", "zip.py", "" + fileName });
            final Process p = pb.start();
            final InputStreamReader isr = new InputStreamReader(p.getInputStream());
            final BufferedReader br = new BufferedReader(isr);
            String ligne = "";
            while ((ligne = br.readLine()) != null) {
                log(ligne);
            }
        }
        catch (Exception e) {
            log(e.toString());
        }
    }

    private static void log(final String logString) {
        System.out.println(logString);
        XnatRunPipelineApi._logger.info(logString);
    }

    static {
        _logger = LoggerFactory.getLogger((Class)XnatIntRunPipelinePlugin.class);
        XnatRunPipelineApi._xnatSessionTypes = Arrays.asList("xnat:mrSessionData", "xnat:crSessionData", "xnat:ctSessionData", "xnat:petSessionData", "xnat:petmrSessionData");
        XnatRunPipelineApi._dcm2niixExecutablePath = "dcm2niix";
    }


    // to print  logger in file.log
    public static class MyLogger implements com.jcraft.jsch.Logger {

    static java.util.Hashtable name=new java.util.Hashtable();
    static{
      name.put(new Integer(DEBUG), "DEBUG: ");
      name.put(new Integer(INFO), "INFO: ");
      name.put(new Integer(WARN), "WARN: ");
      name.put(new Integer(ERROR), "ERROR: ");
      name.put(new Integer(FATAL), "FATAL: ");
    }
    public boolean isEnabled(int level){
      return true;
    }
    public void log(int level, String message){
      System.err.print(name.get(new Integer(level)));
      System.err.println(message);
    }
  }

}








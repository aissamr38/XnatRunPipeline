## Partie Backend-JAVA

L'ensemble des fonctions ci-dessus sont des implémentations de contrôleur API REST. Ce sont des appelles à des méthodes selon la requête reçue en HTTP envoyée de l’extérieur (GUI).

Avant tout voici quelques éléments éléments importants ainsi que les fonctionnalités spécifiques à XNAT.

\+ L'annotation `@XapiRestController` remplace l'annotation standard `@RestController` de Spring. L'objectif principal est de permettre à ce contrôleur d'être localisé et inclus dans la documentation [Swagger XAPI](https://github.com/swagger-api/swagger-core/wiki/Swagger-2.X---Annotations).

\+ L'annotation `@RequestMapping` sur la déclaration de la classe `XnatRunPipelineApi` spécifie le chemin de premier niveau pour les URL lors de l'accès à cette commande. Tous les contrôleurs REST XAPI sont déjà situés sous le chemin **/xapi**, ce qui signifie que toutes les fonctions de ce contrôleur commenceront par **/xapi/int-run-pipline**.

\+ L'annotation `@ApiOperation` décrit une opération ou généralement une méthode HTTP par rapport à un chemin spécifique.

##### Ajouter une méthode :

Voici un exemple de méthode qui reçoit en paramètre `contentText` reçu par la requêt HTTP en passant par l'url suivant : **/xapi/int-run-pipline/testHello**.

La méthode vérifie si la chaîne de caractère reçu est identique à "hello world", elle renvoie le résultat sous un objet JSON au GUI.

```java
@ApiOperation(value = "get hello test ", notes = "Custom")

 @ApiResponses({ @ApiResponse(code = 200, message = "Connection success "), @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT Rest Api"), @ApiResponse(code = 500, message = "Unexpected internal serval error") })

 @RequestMapping(value = { "/testHello" }, produces = { "application/json" }, method = { RequestMethod.POST })

 @ResponseBody

public boolean testHello(final HttpServletResponse response, @RequestParam("contentText") final String contentText ) throws IOException{

return contentText.equals("hello world");

}
```

##### Explications sur les méthodes implémentées :

```java
@RequestMapping(value = { "/check-user-id" }, produces = { "application/json" }, method = { RequestMethod.POST })
public void checkIdXnatIfIsIdCluster(final HttpServletResponse response){}
```

* => Au moment de l'affichage de la page **XDATScreen_project_run_pipeline_data.vm** Cette fonction permet de vérifier Si l’identifiant de Xnat avec qui un utilisateur est connecté est un identifiant présent dans la base LDAP , elle renvoie la liste des BIDSApps présent sur le cluster. Dans le cas échéant elle renvoie null et demande à l'user de saisir son id figurant dans la base LDAP. (Pour éviter la confusion avec les id locales de XNAT).

```java
@RequestMapping(value = { "/get-pipelines/{id_project}" }, produces = { "application/json" }, method = { RequestMethod.POST })

public void getPipelinesOnBtnClick(final HttpServletResponse response, @PathVariable final String id_project, @RequestParam("idCluster") final String idCluster, @RequestParam("subject_ids") final String subject_ids) {}
```

* => Cette fonction permet de récupérer la liste des BIDSApps dans le cas où si l'utilisateur à saisie son id depuis l'interface GUI.

```java
@RequestMapping(value = { "/get-infos-pipeline" }, produces = { "application/json" }, method = { RequestMethod.POST })

public void getInfoAboutPipelineSelected(final HttpServletResponse response, @RequestParam("pipelineSelected") final String pipelineSelected){}
```

* => Afin qu’un utilisateur puisse avoir des informations détaillées sur la BIDSApps qu’il voulait lancer, cette fonction renvoie au bandeau en bleu (dans l'interface) toutes les informations y compris un lien vers la page de wiki officielle de la BIDSApps choisie.

```java
 @RequestMapping(value = { "/start-pipeline/{id_project}" }, produces = { "application/json" }, method = { RequestMethod.POST })

public void startPipelineInCluster(final HttpServletResponse response, @PathVariable final String id_project, ..., ){}
```

* => C'est la fonction qui permet de de générer le script final à envoyer au cluster, et le lance en mode sbatch :
  * Il permet de récupérer la liste des sujets choisis et les sessions sélectionnées.
  * Ajouter l'entêt du script bash.
  * Loader tous les modules qu'il faut.
  * créer le répertoire de travail s'il n'existe pas.
  * généré la commande de download data `commandeDownloadData(){}` pour télécharger les données selon les choix spécifiés par l'usr.
  * générer la commande singularity avec la fonction `prepareCommandSingularity(){}` selon la BIDSApps qui elle -même lit le fichier de configuration .json de xnat pour en extraire tous la params nécessaires pour générer la commande singularity.
  * générer le script .sh exécutable avec la fonction `generateFIleScripte(){}`
  * Envoyer le fichier au cluster grâce la fonction `sendFileToCluster(namFileGenerated, idCluster){}` , Cette fonction elle-même fait appel au fichier `/opt/bin/xnat-rpd.sh` (voir la doc avec Rémi pour comprendre le fonctionnement de ce fichier).
  * Une fois que le script est lancé sur niolon, on récupère toutes les informations liées au job lancé afin de garder une trace. Toutes ces infos sont envoyées au frontEnd sous une format JSON object:
    * **Path to export directory is :** Le chemin où seront stockées les données téléchargées et les fichiers de sortie de votre BIDSAPss.
    * **The full path and name of the generated file :** Le chemin où sera stocké le **ficher bash** (.sh) qui vous a été généré. **Notez bien** que tous les fichiers **.sh** générés sont stockés par défaut dans  le répertoire **\~/xnat-batch-scripts**.
    * **The full path of Output log :** Le chemin où se trouve le fichier des log des Outputs.
    * **The full path of Error log :** Le chemin où se trouve le fichier des log des Erreurs.

```java
@RequestMapping(value = { "/get-sessions/{id_project}" }, produces = { "application/json" }, method = { RequestMethod.POST })

public void getSessionsOfSubjects(final HttpServletResponse response, @PathVariable final String id_project, @RequestParam("selectedSubject") final String selectedSubject)
```

* => Cette fonction permet d'envoyer au frontEnd la/les session(s) qui correspond(ent) au(x) sujet(s) sélectionné(s).

```java
@RequestMapping(value = { "/get-team-names" }, produces = { "application/json" }, method = { RequestMethod.POST })

public void getTeamNames(final HttpServletResponse response, @RequestParam("idCluster") final String idCluster)  
```

* => Selon l'id passé en paramètre on récupère tous les groupes dont il fait partie un l'utilisateur sous forme d'une liste. Cette liste est envoyée au FrontEnd sous un objet JSON.

```java
public boolean checkIfIdUserExist(String idUser){}
```

* => Cette fonction Teste si un usr existe ou pas, elle envoie un boolean.

```java
public String deleteOrNotDataAfterConversionToBIDS(String selectPipeline , String pathDataToDelet){}
```

* => Selon le choix de l'utilisateur si on supprimer les data en bids après la fin du calcul ou non.

```java
public static void readConfigFileJson(){}
```

* => Cette fonction permet de lire le fichier de configuration **/var/lib/tomcat8/xnat_config_run_pipeline_cluster.json** sur présent sur **xnat**

```java
public static Object getJsonObjectByKey(JSONObject jsonObject, Object key){}
```

* => Cette méthode permet de récupérer un Object JSON à partir de sa clé passé en paramètre

```java
public Map< String, String> getInfoJsonObject(JSONObject jsonObject){}
```

* => Pour chaque objet JSON dans le fichier de config on récupère toutes les (key, value)

## Partie FrontEnd

Cette partie détaille le GUI de l'application.

Personnalisation des interfaces utilisateur dans les plugins XNAT [clic ici pour plus d'infos](https://wiki.xnat.org/documentation/xnat-developer-documentation/working-with-xnat-plugins/developing-xnat-plugins/customizing-user-interfaces-in-xnat-plugins).

Le fichier source se trouve dans le path : **src/main/resources/META-INF/resources/*templates*/screens/XDATScreen_project_run_pipeline_data.vm**

Pour ajouter une option au Menu, vous pouvez donc ajouter un **fichier.vm** dans le répertoire **src/main/resources/META-INF/resources/*templates*/screens/xnat_projectData/actionsBox** du projet, ensuite insérer quelque chose comme ceci :

```html
<!-- Sequence: 6 -->
<li class="yuimenuitem">
    <A href="$link.setPage('XDATScreen_edit_working_somthing.vm')">
        <div class="ic">&nbsp;</div><div class="ic_spacer">&nbsp;</div>Add something
    </A>
</li>
```

Rq: **XDATScreen_edit_working_somthing.vm** sera le nom de la page à ajouter.

* Vue que le plugin XNAT peut être constitué d'un certain nombre de composants différents, vous pouvez rajouter donc dans le corps du fichier **XDATScreen_project_run_pipeline_data.vm** du code en :
  * Velocity templates
  * JavaScript
  * CSS style sheets
  * HTML
  * JQuery
  * Ajax (Asynchronous *JavaScript* + XML)
* Pour toute partie ajouté à l’interface GUI de l'application, si elle devrait interagir avec le contrôleur API REST créé dans la partie Backend suivrez les instructions suivantes :
  * Écrivez une fonction en **javascript** et attribuez la un nom à vôtre choix (des fonctions anonymes sont aussi possibles).
  * Définir une url vers la méthode du contrôleur API REST **XnatRunPipelineApi**
  * Utiliser Ajax ou une autre méthode pour envoyer les données et recevoir aussi les réponses.

  ```javascript
  $.ajax({
  				url: "/xapi/int-run-pipline/yourlink",
  				data: {},
  				method:'POST', /* Aussi GET, PUT, DELETE,...*/
  				success: function(data) {
  
  					if(data != null){
  						// your code

  					}	
  					else {
           // your code
  
  					}
  
  				}
  
  ```
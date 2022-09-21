# **INT - XNAT RUN PIPELINE CLUSTER Plugin**

***Les instructions pour créer un projet plugin xnat sont dans la doc officielle de XNAT*** [via ce lien](https://wiki.xnat.org/documentation/xnat-developer-documentation/working-with-xnat-plugins/developing-xnat-plugins/creating-an-xnat-plugin-project)

#### **Prérequis**

* Installer openjdk-11-jdk

  ```
  ~$ : sudo apt update
  ~$ : sudo apt install openjdk-11-jdk
  ~$ :java -version # voir la version de java
  ```

Utilisation de java>=11 dans le terminal tapez la commande:

`~$: export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64`

Pour déployer le plugin il faut installer `gradle`

* installer gradle

  ```
  ~$ : VERSION=7.2
  ~$ : wget https://services.gradle.org/distributions/gradle-${VERSION}-bin.zip -P /tmp
  
  ~$ : sudo unzip -d /opt/gradle /tmp/gradle-${VERSION}-bin.zip
  ```
* Créé un lien symbolique

  ```
  ~$ : sudo ln -s /opt/gradle/gradle-${VERSION} /opt/gradle/latest
  ```
* Configuration de variable d'env

  ```
  ~$ : sudo nano /etc/profile.d/gradle.sh
  ```
* Collez la configuration suivante :

  ```
  export GRADLE_HOME=/opt/gradle/latest
  export PATH=${GRADLE_HOME}/bin:${PATH}
  ```

  **Rq :** Si `export GRADLE_HOME=/opt/gradle/latest` ne marche pas mettez

  `export GRADLE_HOME=/opt/gradle/gradle-7.2`
* Rendre le fichier exécutable :

  ```
  ~$ : sudo chmod +x /etc/profile.d/gradle.sh
  ```
* Loader l'environnement variable (sourcé l’environnement) :

  ```
  ~$ : source /etc/profile.d/gradle.sh
  ```
* Verification de l'installation :

  ```
  ~$ : gradle -v
  ```

#### **Déploiement et utilisation du plugin sur XNAT**

1. Allez sur le répertoire **XnatRunPipeline**

   ```
   ~$: cd XnatRunPipeline
   ```
2. Déployer avec gradle

   ```
   ~$: ./gradlew build
   ```
3. Allez sur le répertoire **XnatRunPipeline/build/libs**

   ```
   ~$: cd XnatRunPipeline/build/libs
   ```
4. Le plugin `xnat-plugin-run-pipeline-cluster-1.1.1.jar` sera généré
5. Ce plugin doit ensuite être copié dans le dossier des plugins de XNAT (voir la configuration de XNAT, généralement dans `/data/xnat/home/plugins`).

   ```
   ~$: scp xnat-plugin-run-pipeline-cluster-1.1.1.jar xnat@10.164.0.44:/data/xnat/home/plugins
   ```
6. Redémarrer **tomcat sur xnat**

   ```
   ~$: sudo systemctl restart tomcat8.service
   ```
7. XNAT prendra un moment pour décompresser et incorporer le nouveau plugin avant que l'application ne soit remise en ligne. Vous pouvez suivre la progression de l'application pendant sa construction en suivant le fichier de log **catalina.out**.

   ```
   ~$ : tail -f /var/lib/tomcat8/logs/catalina.out 
   ```

   Lorsque vous voyez un message dans le fichier de log du type **INFO : Server Startup in 105462 ms,** votre XNAT est à nouveau prêt à être utilisé.

#### **Iformation json file xnat**

Sur xnat allez dans le fichier **/var/lib/tomcat8/xnat_config_run_pipeline_cluster.json**

* **xnat_batch_scripts** : répertoire où seront stockés les scripts bash générés et leurs propres fichiers des logs (.out et .err)
* **data_xnat** : le nom du répertoire partagé par tous les membres d'une équipe et qui contient l'ensemble des données, les résultats des calculs lancés par chaque membre.
* **linkAllImgSingularity** : Chemin vers l'endroit où se trouvent toutes les **BIDSApps**
* **singulartyRun** : Début de la commande Singularity
* **URI_HOST_XNAT** : URI de XNAT
* **xnat2bids** : chemin vers le scripte **xnat2bids_reconstruct_afterDownload.py**
* **teamNames** : tableau des noms de toutes les équipes du labo
* **listPipelines** : tableau contient les noms des **BIDSApps.**

###### Chaque BIDSApps contient un ensemble de paires clé - valeur:

#### **Configuration json file xnat**

Pour rajouter une nouvelle image :

Sur xnat allez dans le fichier **/var/lib/tomcat8/xnat_config_run_pipeline_cluster.json**

* la clé : **listPipelines : ajouter le nom de l'image.**
* copier - coller une image déjà existante et modifiez la, avec les paramètres de la nouvelle image.
  * **name** : le nom de la BIDSApps
  * **linkDoc** : lien de la documentation officielle.
  * **singularityCleanEnv** : si la commande exige un **--cleanenv** mettez le ici, vous pouvez aussi rajouter tout les Binding nécessaire à votre commande.
  * **inputDataBids** : correspond à l'argument pour dire le chemin des données en BIDS (input data). Si la commande n'exige pas d'argument --> laissez vide.
  * **output** : correspond à l'argument pour dire le chemin où seront stockés les résultats (output dir). Si la commande n'exige pas d'argument --> laissez vide.
  * **path_licence** : chemin vers licence si la commande utilise des application qui nécessitent une licence.
  * **licence_Params** : la façon dont vous appelez cette licence dans la commande.
  * **output_key** :
  * **data_key** :
  * **commande_befor** : des arguments que peut prendre la commande singularity avant de faire appel à la commande de BIDSApps.
  * **commande_after** : tous les arguments qui peuvent être ajouter après l'appel de la BIDSApps.
  * **commande_participant** : participant, label, group, etc ... Laissez vide dans le cas contraire.
  * **work_dir_params** : le param workdir si la commande l'exige (e.g -w)
  * **basicParameters** : autres paramètres qui peuvent être utiles à la commande, pour pouvez mettre d'autres paramètres qui vont être ajouter à la fin de la commande par défaut.

# Documentation du code

  [documentation du code  plugin xnat-plugin-run-pipeline-cluster](./doc-fonction-plugin-xnat-run-pipeline.md)
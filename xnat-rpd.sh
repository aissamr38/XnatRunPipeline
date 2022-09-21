#!/bin/bash

bye (){
  case "$1" in
    1)
      msg="Wrong usage."
      ;;
    10)
      msg="File $bfile does not exist."
      ;;
    11)
      msg="User $user does not exist."
      ;;
    12)
      msg="$user home dir does not exist."
      ;;
    13)
      msg="User ($user) or file ($bfile) missing."
      ;;
  esac
  logger -t "$(basename $0)" -i "$msg"
  exit "$1"
}

usage (){
  echo "Usage: $(basename $0) -u USER -f BATCHFILE
    USER: domain user
    BATCHFILE: the batch file to move or start
    Move batch file to the USER home dir and start it on niolon."
  bye 1
}

# déplace le fichier $1 dans le home du user $2
  

main () {
  #test des paramètres
  test -f "$bfile" || bye 10
  id "$user" > /dev/null 2>&1 || bye 11
  test -d "/hpc_home/$user" || bye 12

  # le bfile au bon endroit
  destdir="/hpc_home/$user/xnat-batch-scripts/"
  mkdir -p "$destdir"
  mv "$bfile" "$destdir"
  chown -R "$user" "$destdir"
  
  # on lance le batch sur niolon
  # en retour, la sortie de la commande sbatch, exemple:
  # Submitted batch job 40091
  ssh niolon "$user /home/$user/xnat-batch-scripts/$(basename $bfile)"
}

while getopts "u:f:" opt; do
  case "$opt" in
    u)
      user="$OPTARG"
      ;;
    f)
      bfile="$OPTARG"
      ;;
    \?)
      usage
      ;;
    *)
      usage
      ;;
  esac
done

# user et bfile obligatoire
if [ -z "$user" ] || [ -z "$bfile" ]; then bye 13; fi

main

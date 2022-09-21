#!/bin/bash
set -e
set +x

echo "delete previous build"
rm -rf build

echo "Building plugin..."
./gradlew build
echo "Delete old jar..."
ssh xnat@xnatsand  "rm /data/xnat/home/plugins/xnat-plugin-run-pipeline-cluster-1.1.2.jar | exit   "

cd build/libs
echo "Copying new plugin..."
scp xnat-plugin-run-pipeline-cluster-1.1.2.jar xnat@xnatsand:/data/xnat/home/plugins

echo "Restarting tomcat8..."
ssh -t xnat@xnatsand "sudo systemctl restart tomcat8 | exit   "



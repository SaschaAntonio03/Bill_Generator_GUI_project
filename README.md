*** Run following commands in terminal from directory "billing-app" to build the click-on app: "comptability_program/project/Build/Java files main". *** 

cd "project/Build/billing-app"
mvn clean package

cd "target"
jpackage \
  --type app-image \
  --name BillingApp \
  --app-version 1.0 \
  --input . \
  --main-jar billing-app-1.0-SNAPSHOT.jar \
  --main-class BillingApp \
  --dest ../out \
  --java-options "-Xmx512m"


*** App will be outputed on the /out directory inside /target. ***

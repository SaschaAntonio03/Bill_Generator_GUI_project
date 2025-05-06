*** Run following commands in terminal from directory "billing-app" to build the click-on app: "comptability_program/project/Build/Java files main". *** 

cd "project/Build/billing-app"
mvn clean package

cd "target"

sudo jpackage \
  --type app-image \
  --name BillingApp \
  --app-version 1.0 \
  --input . \
  --main-jar billing-app-1.0-SNAPSHOT.jar \
  --main-class BillingApp \
  --resource-dir ../src/main/resources \
  --dest ../out \
  --java-options "-Xmx512m" \
  --mac-app-category "public.app-category.education" \
  --verbose


*** App will be outputed on the /out directory inside /target. ***

#!/bin/bash

sed -i 's/package="com.undatech.opaque"/package="com.undatech.opaquebeta"/' AndroidManifest.xml
sed -i 's/android:label="@string\/app_name"/android:label="@string\/app_name_beta"/' AndroidManifest.xml

find ./ -type f -name \*.java -exec sed -i 's/com.undatech.opaque.R;/com.undatech.opaquebeta.R;/' {} \;

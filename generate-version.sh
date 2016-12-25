#!/bin/bash

VERSION=$(git describe --tag --long)
echo $VERSION

echo "firecrest.version=$VERSION" > version.properties
cat firecrest/src/deb/control/control.src | sed "s/\[\[version\]\]/$VERSION/" > firecrest/src/deb/control/control
cat firecrest/src/deb/service/run.src | sed "s/\[\[version\]\]/$VERSION/" > firecrest/src/deb/service/run

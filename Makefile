VERSION := $(shell git describe --tag --long)

package: version
	mvn package

version:
	echo "firecrest.version=$(VERSION)" > version.properties
	cat firecrest/src/deb/control/control.src | sed "s/\[\[version\]\]/$(VERSION)/" > firecrest/src/deb/control/control
	cat firecrest/src/deb/service/run.src | sed "s/\[\[version\]\]/$(VERSION)/" > firecrest/src/deb/service/run

clean:
	mvn clean
	rm -f version.properties
	rm -f firecrest/src/deb/control/control
	rm -f firecrest/src/deb/service/run

server:
	java -jar firecrest/target/firecrest-$(VERSION).jar server firecrest.yml

.PHONY: clean package


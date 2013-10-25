Staging Extension 1.0
for
eXo Platform 3.5
===================

"Staging Extension" provides extensions based on "GateIN Management" (1.0.1-GA) to export/import eXo Platform data. This is helpful for System administrators to transfer data from a Platform Server to another.

This tool manage data of types:
* Pages and navigations (MOP extension provided by RedHat).
* IDM: organizational model elements.
* ECM Admin templates, configurations and metadata.
* Gadgets.
* Sites JCR Contents.
* Application registry.
* Another extension was added to provide a Platform extension package for sites configurations.

Getting Started
===============

Step 1 :  Build 
----------------

Prerequisite : install [Maven 3](http://maven.apache.org/download.html).

    git clone https://github.com/exo-addons/platform-staging-extension.git
    cd platform-staging-extension

then build the project with maven :

    mvn clean install

Step 2 : Deploy 
---------------

Prerequisite : install [eXo Platform 3.5 bundle] or [JBoss EPP]

	If the eXo Platform 3.5 is based on JBoss:
		cp ear/target/exo-platform-staging-extension.ear JBOSS_HONE/server/default/deploy
	If the bundle is based on JBoss EPP:
		cp ear-epp/target/exo-platform-staging-extension.ear JBOSS_EPP_HONE/server/default/deploy
	Else, if the eXo Platform 3.5 is based on Tomcat
		cp ear/target/exo-platform-staging-extension/lib/gatein-management-api-1.0.1-GA.jar CATALINA_HOME/lib
		cp ear/target/exo-platform-staging-extension/lib/gatein-management-core-1.0.1-GA.jar CATALINA_HOME/lib
		cp ear/target/exo-platform-staging-extension/lib/gatein-management-rest-1.0.1-GA.jar CATALINA_HOME/lib
		cp ear/target/exo-platform-staging-extension/lib/gatein-management-spi-1.0.1-GA.jar CATALINA_HOME/lib
		cp ear/target/exo-platform-staging-extension/lib/staging-extension-config-1.0.0-SNAPSHOT.jar CATALINA_HOME/lib
		cp ear/target/exo-platform-staging-extension/lib/staging-extension-content-1.0.0-SNAPSHOT.jar CATALINA_HOME/lib
		cp ear/target/exo-platform-staging-extension/lib/staging-extension-ecmadmin-1.0.0-SNAPSHOT.jar CATALINA_HOME/lib
		cp ear/target/exo-platform-staging-extension/lib/staging-extension-gadget-1.0.0-SNAPSHOT.jar CATALINA_HOME/lib
		cp ear/target/exo-platform-staging-extension/lib/staging-extension-idm-1.0.0-SNAPSHOT.jar CATALINA_HOME/lib
		cp ear/target/exo-platform-staging-extension/lib/staging-extension-mop-1.0.0-SNAPSHOT.jar CATALINA_HOME/lib
		cp ear/target/exo-platform-staging-extension/lib/staging-extension-registry-1.0.0-SNAPSHOT.jar CATALINA_HOME/lib
		cp ear/target/exo-platform-staging-extension/lib/staging-extension-mop-packaging-1.0.0-SNAPSHOT.jar CATALINA_HOME/lib
		cp -r ear/target/exo-platform-staging-extension/gatein-management-cli.war CATALINA_HOME/webapp

Step 3 : Configure Platform
----------------------------

If you are using JBoss, delete "PortalLoginModule" from the security domain "gatein-domain":

	If the bundle is based on JBoss, delete the entry from:
	JBOSS_HOME/server/default/deploy/gatein.ear/META-INF/gatein-jboss-beans.xml

Step 4 : Run
------------

Use eXo start script.

Now, use SSH client to connect to the console:
 ssh -p 2001 root@localhost
 *  mgmt connect -c portal
 *  y o u r  c o m m a n d s (Read "Admin guide" page for more details)

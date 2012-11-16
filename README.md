Platform Staging Extension
===================

"Platform Staging Extension" provides extensions based on "GateIN Management" (1.0.1-GA) to export/import Platform data. This is helpful for System administrators to transfer data from a Platform Server to another.
This tool manage data of types:

Pages and navigations (MOP extension provided by RedHat).
IDM: organizational model elements.
ECM Admin templates, configurations and metadata.
Gadgets
Sites JCR Contents
Another extension was added to provide a Platform extension package for sites configurations.

The MOP extension allows to export/import site related data (portal layout, pages and navigations).

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

Prerequisite : install [eXo Platform 3.5 bundle](http://www.exoplatform.com/company/en/download-exo-platform)

	If the bundle is based on JBoss:
		cp ear/target/exo-platform-staging-extension.ear JBOSS_HONE/server/default/deploy
	Else, if the bundle is based on Tomcat
		cp ear/target/exo-platform-staging-extension/lib/* CATALINA_HOME/lib
		cp -r ear/target/exo-platform-staging-extension/gatein-management-cli.war CATALINA_HOME/webapp

Step 3 : Run
------------

Use eXo start script.

Now, use SSH client to connect to the console:
 ssh -p 2000 root@localhost
 *  mgmt connect -c portal
 *  y o u r  c o m m a n d s
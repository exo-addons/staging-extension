Staging Extension for eXo Platform 4
====================================

"Staging Extension" provides extensions based on "GateIN Management" (1.1.0.Final) to export/import Platform data.
This is helpful for System administrators to transfer data from a Platform Server to another.
This tool manage data of types:

Sites : Pages and navigations (MOP extension provided by RedHat).
ECM Admin templates, configurations and metadata.
Gadgets
Sites : Contents, documents and categories
Packaging: generate a configuration extension with Sites configurations

The export/import operations can be done via REST, SSH or SCP.

Getting Started
===============

Step 1 :  Build 
----------------

Prerequisite : install [Maven 3](http://maven.apache.org/download.html).

    git clone https://github.com/exo-addons/staging-extension.git
    cd staging-extension

then build the project with maven :

    mvn clean install

Step 2 : Deploy 
---------------

Prerequisite : 

* Download and install [eXo Platform 4] if you use eXo Platform 4

	** If the bundle is based on JBoss:

    cp ear/target/staging-extension/lib/* JBOSS_HONE/standalone/deployments/platform.ear/lib

    cp ear/target/staging-extension/gatein-management-cli.war JBOSS_HONE/standalone/deployments/platform.ear

    cp ear/target/staging-extension/staging-portlet.war JBOSS_HONE/standalone/deployments/platform.ear

	** 	Else, if the bundle is based on Tomcat:

    cp ear/target/staging-extension/lib/* CATALINA_HOME/lib

    cp ear/target/staging-extension/gatein-management-cli.war CATALINA_HOME/webapps

    cp ear/target/staging-extension/staging-portlet.war CATALINA_HOME/webapps

* Download and install [JBoss JPP 6], if you use JPP 6 copy this file:

	** cp ear-epp/target/staging-extension.ear JBOSS_HONE/gatein/extensions

Step 3 : Run
------------

1/ Start eXo Platform 4 server.

2/ Use SSH client to connect to the console:

    ssh -p 2001 root@localhost
    mgmt connect

3/ Use "help" command to get the list of commands.

4/ Use "ls" command to get list of data to manage.

Step 4 : Use Portlet
------------

Staging extension features can be used via UI by adding "Staging Extension Portlet" into a page.
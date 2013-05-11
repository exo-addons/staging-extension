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

    git clone https://github.com/exo-addons/platform-staging-extension.git
    cd platform-staging-extension

then build the project with maven :

    mvn clean install

Step 2 : Deploy 
---------------

Prerequisite : install [eXo Platform 4]

	If the bundle is based on JBoss:
		cp ear/target/exo-platform-staging-extension.ear JBOSS_HONE/server/default/deploy
	Else, if the bundle is based on Tomcat
		cp ear/target/exo-platform-staging-extension/lib/* CATALINA_HOME/lib
		cp -r ear/target/exo-platform-staging-extension/gatein-management-cli.war CATALINA_HOME/webapps

Step 3 : Run
------------

1/ Start eXo Platform 4 server.
2/ Use SSH client to connect to the console:
 ssh -p 2001 root@localhost
 *  mgmt connect
3/ Use "help" command to get the list of commands.
4/ Use "ls" command to get list of data to manage.
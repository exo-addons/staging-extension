Platform Staging Extension
===================================

== Presentation ==

The [[Gatein Management||https://docs.jboss.org/author/display/GTNPORTAL34/GateIn+Management]] is a tool developed by RedHat. This tool provides an SPI that allows to define data management extensions. (The source code is available [[here>>https://github.com/gatein/gatein-management]] and it's currently merged with gatein codebase starting from 3.4 version)

"Platform Staging Extension" provides extensions based on "GateIN Management" (1.0.1-GA) to export/import Platform data. This is helpful for System administrators to transfer data from a Platform Server to another.
This tool manage data of types:
* Pages and navigations (MOP extension provided by RedHat).
* IDM: organizational model elements.
* ECM Admin templates, configurations and metadata.
* Gadgets
* Sites JCR Contents

Another extension was added to provide a Platform extension package for sites configurations.

The MOP extension allows to export/import site related data (portal layout, pages and navigations).

== How does it work ? ==

The GateIn Management Tool provides a foundation for managing components. This foundation defines common operations that can be applied to registered resources. Each extension will register new types of resources to manage and implement the operations. The operations are : read-resource, add-resource, remove-resource, update-resource, read-config-as-xml, export-resource, import-resource.

== How to install on Platform ? ==

1/ Modify the security domain "gatein-domain" to delete "PortalLoginModule": This login module is used when HTTP session replication is used for multiple Platform clustered servers. (EXOGTN-1275)
2/ Install exo-platform-staging-extension.ear.

== How to connect ? ==

*  ssh -p 2000 john@localhost
 *  mgmt connect -c portal
 *  ls 
 *  cd content 
 *  y o u r  c o m m a n d s

==== Sources ====

https://github.com/boubaker/platform-staging-extension
(Will be moved to exo-addons)

==== Mop ====

Sources : https://github.com/gatein/gatein-portal/tree/master/component/portal/src/main/java/org/exoplatform/portal/mop/management

===== Resources hierarchy =====

* mop
** portalsites
*** <site_name>
**** pages
***** <page_name>
**** navigation
***** <page_name>
**** portal
** groupsites
*** <group_name>
**** pages
***** <page_name>
**** navigation
***** <page_name>
**** group
** usersites
*** <user_name>
**** pages
***** <page_name>
**** navigation
***** <page_name>

===== Description =====

This extension allows export/import pages and navigations with a know XML format, the one used in Platform extension to define a portal. So, even a developper can use this tool to export the definition of a site that was done via UI.

===== Related links =====

https://docs.jboss.org/author/display/GTNPORTAL/GateIn+Management
http://vimeo.com/37750158

==== IDM ====

===== Resources hierarchy =====

* idm

===== Commands =====

{{success}}{{{
cd idm
ls
export -f /org-data.zip
import -f /org-data.zip
}}}{{/success}}

==== Contents ====

===== Resources hierarchy =====

* content
** sites
*** <site_name>
**** contents
**** seo

===== Commands =====

* list all sites
{{success}}{{{
cd content/sites
ls
}}}{{/success}}

* export all the sites (contents + SEO)
{{success}}{{{
cd content/sites
export -f /all-sites.zip
}}}{{/success}}

* export one site (contents + SEO)
{{success}}{{{
cd content/sites/acme
export -f /acme.zip
}}}{{/success}}

* export contents of a site
{{success}}{{{
cd content/sites/acme/contents
export -f /acme.zip
}}}{{/success}}

* export a site without the skeleton
{{success}}{{{
cd content/sites/acme/contents
export --filter no-skeleton:true -f /acme.zip
}}}{{/success}}

* export SEO metadata
{{success}}{{{
cd content/sites/acme/seo
export -f /acme.zip
}}}{{/success}}

* import multiple sites (all sites contained in the zip will be imported)
{{success}}{{{
cd content/sites
import -f /multiple-sites.zip
}}}{{/success}}

* import a site (it will import only the acme site, event if the zip contains data of others sites)
{{success}}{{{
cd content/sites/acme
import -f /acme.zip
}}}{{/success}}

==== ECM Admin resources ====

===== Resources hierarchy =====

** templates
*** applications
**** <application_name>
*** nodeypes
*** metadata
** taxonomy
*** <taxonomy_name>
** queries
** scripts
** drive
** action
** nodetype

===== Commands =====

* list all sun resources:
{{success}}{{{
cd ecmadmin
ls
}}}{{/success}}

* export all ECM Admin resources
{{success}}{{{
export -f /ecm-admin-data.zip
}}}{{/success}}

* import all ECM Admin resources
{{success}}{{{
import -f /ecm-admin-data.zip --filter replaceExisting:true
}}}{{/success}}

* list all taxonomies
{{success}}{{{
cd ecmadmin/taxonomy
ls
}}}{{/success}}

* export all taxonomies
{{success}}{{{
cd ecmadmin/taxonomy
export -f /taxonomies.zip
}}}{{/success}}

* export a taxonomy
{{success}}{{{
cd ecmadmin/taxonomy/acme
export -f /acme-taxonomy.zip
}}}{{/success}}

* import taxonomies
{{success}}{{{
cd ecmadmin/taxonomy
import -f /taxonomies.zip
}}}{{/success}}

* import a taxonomy
{{success}}{{{
cd ecmadmin/taxonomy
import -f /acme-taxonomy.zip
}}}{{/success}}

* list all drives
{{success}}{{{
cd ecmadmin/drive
ls
}}}{{/success}}

* export all drives: the exported file is a kernel configuration
{{success}}{{{
cd ecmadmin/drive
export -f /drives.zip
}}}{{/success}}

* export selected drives
{{success}}{{{
cd ecmadmin/drive
export -f /drives.zip --filter <drive_name1> --filter <drive_name2>
}}}{{/success}}

* import drives
{{success}}{{{
cd ecmadmin/drive
import -f /drives.zip
}}}{{/success}}

* list all shared queries
{{success}}{{{
cd ecmadmin/queries
ls
}}}{{/success}}

* export all queries (shared + users'): the exported file is a kernel configuration
{{success}}{{{
cd ecmadmin/queries
export -f /queries.zip
}}}{{/success}}

* import queries
{{success}}{{{
cd ecmadmin/queries
import -f /queries.zip
}}}{{/success}}

* list all groovy scripts
{{success}}{{{
cd ecmadmin/script
ls
}}}{{/success}}

* export all groovy scripts
{{success}}{{{
cd ecmadmin/script
export -f /scripts.zip
}}}{{/success}}

* export selected groovy scripts
{{success}}{{{
cd ecmadmin/script
export -f /scripts.zip --filter <script_name1> --filter <script_name2>
}}}{{/success}}

* import groovy scripts
{{success}}{{{
cd ecmadmin/script
import -f /scripts.zip
}}}{{/success}}

* list all action types
{{success}}{{{
cd ecmadmin/action
ls
}}}{{/success}}

* export all action types
{{success}}{{{
cd ecmadmin/action
export -f /actions.zip
}}}{{/success}}

* export selected action types
{{success}}{{{
cd ecmadmin/action
export -f /actions.zip --filter <action_name1> --filter <action_name2>
}}}{{/success}}

* import action types
{{success}}{{{
cd ecmadmin/action
import -f /actions.zip
}}}{{/success}}

* list all nodetypes
{{success}}{{{
cd ecmadmin/nodetype
ls
}}}{{/success}}

* export all nodetypes
{{success}}{{{
cd ecmadmin/nodetype
export -f /nodetypes.zip
}}}{{/success}}

* export selected nodetypes
{{success}}{{{
cd ecmadmin/nodetype
export -f /nodetypes.zip --filter <nodetype_name1> --filter <nodetype_name2>
}}}{{/success}}

* import nodetypes
{{success}}{{{
cd ecmadmin/nodetype
import -f /nodetypes.zip
}}}{{/success}}

* list all templates
{{success}}{{{
cd ecmadmin/templates
ls
}}}{{/success}}

* export all templates
{{success}}{{{
cd ecmadmin/templates
export -f /templates.zip
}}}{{/success}}

* export selected templates
{{success}}{{{
cd ecmadmin/templates/applications
export -f /applications.zip

cd ecmadmin/templates/nodetypes
export -f /nodetypes.zip

cd ecmadmin/templates/nodetypes
export -f /nodetypes.zip --filter <nodetype_name1> --filter <nodetype_name2>

cd ecmadmin/templates/metadata
export -f /metadata.zip

cd ecmadmin/templates/metadata
export -f /nodetypes.zip --filter <metadata_name1> --filter <metadata_name2>
}}}{{/success}}

==== Gadget ====

Sources : https://github.com/boubaker/platform-staging-extension
(have to be moved to exo-addons)

===== Resources hierarchy =====

* gadget
** <gadget_name>

===== Commands =====

{{success}}{{{

cd gadget
ls
export -f /org-data.zip
import -f /org-data.zip

cd gadget/
ls
export -f /gadget-data.zip
import -f /gadget-data.zip

}}}{{/success}}

==== Extension Packaging ====

Sources : https://github.com/gregorysebert/mop-packaging

* package exported data into a GateIn/eXo extension
{{success}}{{{
export --filter mop:/acme-mop.zip --filter content:/acme-content.zip -f /acme-extension.war
}}}{{/success}}
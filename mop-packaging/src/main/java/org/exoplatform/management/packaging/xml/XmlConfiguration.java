package org.exoplatform.management.packaging.xml;

import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IMarshallingContext;
import org.exoplatform.container.xml.Component;
import org.exoplatform.container.xml.Configuration;
import org.exoplatform.container.xml.ComponentPlugin;
import org.exoplatform.container.xml.ExternalComponentPlugins;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import  org.exoplatform.xml.object.XMLField;
import  org.exoplatform.xml.object.XMLObject;
import  org.exoplatform.xml.object.XMLCollection;
import java.util.Collection;

import org.exoplatform.container.xml.ValueParam;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: gregorysebert
 * Date: 12/07/12
 * Time: 19:09
 * To change this template use File | Settings | File Templates.
 */
public class XmlConfiguration {


    private static final String KERNEL_CONFIGURATION_1_1_URI = "<configuration xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.exoplaform.org/xml/ns/kernel_1_2.xsd http://www.exoplaform.org/xml/ns/kernel_1_2.xsd\" xmlns=\"http://www.exoplaform.org/xml/ns/kernel_1_2.xsd\">";

    private static final String EMPTY_FIELD_REGULAR_EXPRESSION = "<field name=\"([a-z|A-Z]*)\"/>";

    public void XmlConfiguration()
    {

    }

    public void addPortalConfiguration(FileOutputStream zos)
    {
        ExternalComponentPlugins externalComponentPlugins = new ExternalComponentPlugins();
        externalComponentPlugins.setTargetComponent("org.exoplatform.portal.config.UserPortalConfigService");

        ComponentPlugin componentPlugin = new ComponentPlugin();
        componentPlugin.setName("default.portal.config.user.listener");
        componentPlugin.setType("org.exoplatform.portal.config.NewPortalConfigListener");
        componentPlugin.setSetMethod("initListener");
        componentPlugin.setDescription("this listener init the portal configuration");

        InitParams initParams = new InitParams();

        ValueParam valueParam = new ValueParam();
        valueParam.setName("default.portal");
        valueParam.setValue("default");
        valueParam.setDescription("The default portal for checking db is empty or not");

        // Add portal configuration

        ObjectParameter  objectParamPortal = new ObjectParameter();
        objectParamPortal.setName("portal.configuration");
        objectParamPortal.setDescription("description");


        XMLObject xmlObject = new XMLObject();
        xmlObject.setType("org.exoplatform.portal.config.NewPortalConfig");

        XMLCollection xmlCollection = new XMLCollection();
        xmlCollection.setType("java.util.HashSet");

        Collection collection = new java.util.HashSet<String>();
        collection.add("default");

        XMLField xmlField = new XMLField();
        xmlField.setName("predefinedOwner");
        xmlField.setCollection(xmlCollection);
        try {
            xmlField.setCollectiontValue(collection);
        } catch (Exception e) {
            e.printStackTrace();
        }
        xmlObject.addField(xmlField);

        xmlField = new XMLField();
        xmlField.setName("ownerType");
        xmlField.setString("portal");
        xmlObject.addField(xmlField);

        xmlField = new XMLField();
        xmlField.setName("templateLocation");
        xmlField.setString("war:/conf/mop/default");
        xmlObject.addField(xmlField);

        try {
            objectParamPortal.setXMLObject(xmlObject);
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }


        // Add group configuration

        ObjectParameter  objectParamGroup = new ObjectParameter();
        objectParamGroup.setName("group.configuration");
        objectParamGroup.setDescription("description");

        xmlObject = new XMLObject();
        xmlObject.setType("org.exoplatform.portal.config.NewPortalConfig");

        xmlCollection = new XMLCollection();
        xmlCollection.setType("java.util.HashSet");

        collection = new java.util.HashSet<String>();
        collection.add("/platform/web-contributors");

        xmlField = new XMLField();
        xmlField.setName("predefinedOwner");
        xmlField.setCollection(xmlCollection);
        try {
            xmlField.setCollectiontValue(collection);
        } catch (Exception e) {
            e.printStackTrace();
        }
        xmlObject.addField(xmlField);

        xmlField = new XMLField();
        xmlField.setName("ownerType");
        xmlField.setString("group");
        xmlObject.addField(xmlField);

        xmlField = new XMLField();
        xmlField.setName("templateLocation");
        xmlField.setString("war:/conf/mop/default");
        xmlObject.addField(xmlField);

        try {
            objectParamGroup.setXMLObject(xmlObject);
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        initParams.addParam(valueParam);
        initParams.addParam(objectParamPortal);
        initParams.addParam(objectParamGroup);


        componentPlugin.setInitParams(initParams);

        ArrayList componentPluginsList = new ArrayList();
        componentPluginsList.add(componentPlugin);

        externalComponentPlugins.setComponentPlugins(componentPluginsList);

        Configuration configuration = new Configuration();
        configuration.addExternalComponentPlugins(externalComponentPlugins);

        try{
        zos.write(toXML(configuration));
        zos.close();
        //zos.closeEntry();
        }catch (Exception e) {}

    }

    protected byte[] toXML(Object obj) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            IBindingFactory bfact = BindingDirectory.getFactory(obj.getClass());
            IMarshallingContext mctx = bfact.createMarshallingContext();
            mctx.setIndent(2);
            mctx.marshalDocument(obj, "UTF-8", null, out);
            String outConf = new String(out.toByteArray());
            outConf = outConf.replace("<configuration>", KERNEL_CONFIGURATION_1_1_URI).replaceAll(EMPTY_FIELD_REGULAR_EXPRESSION, "");
            return outConf.getBytes();
        } catch (Exception ie) {
            throw ie;
        }
    }

}

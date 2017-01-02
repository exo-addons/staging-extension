package org.exoplatform.management.answer.operations;

import com.thoughtworks.xstream.XStream;
import org.exoplatform.faq.service.Category;
import org.gatein.management.api.operation.model.ExportTask;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;


public class CategoryMetaDataExportTask implements ExportTask{
    public static final String FILENAME = "/metadata.xml";

    protected final Category category;
    private final String type;

    public CategoryMetaDataExportTask(Category category, String type){
        this.category = category;
        this.type = type;
    }

    @Override
    public String getEntry() {
        return getEntryPath(type, category.getId());
    }

    public static String getEntryPath(String type, String id) {
        return new StringBuilder("answer/").append(type).append("/").append(id).append(FILENAME).toString();
    }

    @Override
    public void export(OutputStream outputStream) throws IOException {
        XStream xStream = new XStream();
        OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
        xStream.toXML(this.category, writer);
        writer.flush();

    }
}

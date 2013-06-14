package org.exoplatform.management.mop.binding;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import org.exoplatform.portal.config.model.ModelUnmarshaller;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PageNavigation;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.management.binding.xml.NavigationMarshaller;
import org.exoplatform.portal.mop.management.binding.xml.PageMarshaller;
import org.exoplatform.portal.mop.management.binding.xml.SiteLayoutMarshaller;
import org.gatein.management.api.ContentType;
import org.gatein.management.api.binding.BindingException;
import org.gatein.management.api.binding.BindingProvider;
import org.gatein.management.api.binding.Marshaller;

public class CustomMopBindingProvider implements BindingProvider {
  public static final CustomMopBindingProvider INSTANCE = new CustomMopBindingProvider();

  private CustomMopBindingProvider() {
  }

  public <T> Marshaller<T> getMarshaller(Class<T> type, ContentType contentType) throws BindingException {
    switch (contentType) {
    case XML:
      return getXmlMarshaller(type);
    case JSON:
    case ZIP:
    default:
      return null;
    }
  }

  @SuppressWarnings("unchecked")
  private <T> Marshaller<T> getXmlMarshaller(Class<T> type) {
    if (Page.class.isAssignableFrom(type)) {
      return (Marshaller<T>) XmlMarshallers.page_marshaller;
    } else if (Page.PageSet.class.isAssignableFrom(type)) {
      return (Marshaller<T>) XmlMarshallers.pages_marshaller;
    } else if (PageNavigation.class.isAssignableFrom(type)) {
      return (Marshaller<T>) XmlMarshallers.navigation_marshaller;
    } else if (PortalConfig.class.isAssignableFrom(type)) {
      return (Marshaller<T>) XmlMarshallers.site_marshaller;
    }

    return null;
  }

  private static class XmlMarshallers {

    // ------------------------------------ Page Marshallers
    // ------------------------------------//
    private static Marshaller<Page.PageSet> pages_marshaller = new PageMarshaller() {
      public Page.PageSet unmarshal(InputStream inputStream) throws BindingException {
        try {
          return ModelUnmarshaller.unmarshall(Page.PageSet.class, inputStream).getObject();
        } catch (Exception e) {
          throw new BindingException(e);
        }
      }
    };

    private static Marshaller<Page> page_marshaller = new Marshaller<Page>() {
      public void marshal(Page page, OutputStream outputStream) throws BindingException {
        Page.PageSet pages = new Page.PageSet();
        pages.setPages(new ArrayList<Page>(1));
        pages.getPages().add(page);

        XmlMarshallers.pages_marshaller.marshal(pages, outputStream);
      }

      public Page unmarshal(InputStream inputStream) throws BindingException {
        Page.PageSet pages = pages_marshaller.unmarshal(inputStream);

        if (pages.getPages().isEmpty())
          throw new BindingException("No page was unmarshalled.");

        if (pages.getPages().size() != 1)
          throw new BindingException("Multiple pages found.");

        return pages.getPages().get(0);
      }
    };

    private static Marshaller<PageNavigation> navigation_marshaller = new NavigationMarshaller() {
      public PageNavigation unmarshal(InputStream inputStream) throws BindingException {
        try {
          return ModelUnmarshaller.unmarshall(PageNavigation.class, inputStream).getObject();
        } catch (Exception e) {
          throw new BindingException(e);
        }
      }
    };

    private static Marshaller<PortalConfig> site_marshaller = new SiteLayoutMarshaller() {
      public PortalConfig unmarshal(InputStream inputStream) throws BindingException {
        try {
          return ModelUnmarshaller.unmarshall(PortalConfig.class, inputStream).getObject();
        } catch (Exception e) {
          throw new BindingException(e);
        }
      }
    };
  }
}

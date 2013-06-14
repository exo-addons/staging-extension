package org.exoplatform.portal.mop.management.exportimport;


import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.portal.config.DataStorage;
import org.exoplatform.portal.config.Query;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.importer.ImportMode;
import org.exoplatform.portal.mop.management.operations.page.PageUtils;

import java.util.ArrayList;
import java.util.List;

public class PageImportTask extends AbstractImportTask<Page.PageSet>
{
   private final DataStorage dataStorage;
   private Page.PageSet rollbackSaves;
   private Page.PageSet rollbackDeletes;

   public PageImportTask(Page.PageSet data, SiteKey siteKey, DataStorage dataStorage)
   {
      super(data, siteKey);
      this.dataStorage = dataStorage;
   }

   
   public void importData(ImportMode importMode) throws Exception
   {
      if (data == null || data.getPages() == null || data.getPages().isEmpty()) return;

      Query<Page> query = new Query<Page>(siteKey.getTypeName(), siteKey.getName(), Page.class);
      LazyPageList<Page> list = dataStorage.find(query);
      @SuppressWarnings("deprecation")
      int size = list.getAvailable();

      Page.PageSet dst = null;
      switch (importMode)
      {
         case CONSERVE:
            if (size == 0)
            {
               dst = data; // No pages exist yet.
               rollbackDeletes = data;
            }
            else
            {
               dst = null;
            }
            break;
         case INSERT:
            if (size == 0)
            {
               dst = data; // No pages exist yet.
               rollbackDeletes = data;
            }
            else
            {
               dst = new Page.PageSet();
               dst.setPages(new ArrayList<Page>());
               List<Page> existingPages = list.getAll();
               rollbackDeletes = new Page.PageSet();
               rollbackDeletes.setPages(new ArrayList<Page>());
               for (Page src : data.getPages())
               {
                  Page found = findPage(existingPages, src);
                  if (found == null)
                  {
                     dst.getPages().add(src);
                     rollbackDeletes.getPages().add(src);
                  }
               }
            }
            break;
         case MERGE:
            if (size == 0) // No pages exist yet.
            {
               dst = data;
               rollbackDeletes = data;
            }
            else
            {
               dst = new Page.PageSet();
               dst.setPages(new ArrayList<Page>(data.getPages().size()));
               List<Page> existingPages = list.getAll();
               rollbackSaves = new Page.PageSet();
               rollbackSaves.setPages(new ArrayList<Page>(size));
               rollbackDeletes = new Page.PageSet();
               rollbackDeletes.setPages(new ArrayList<Page>());
               for (Page src : data.getPages())
               {
                  dst.getPages().add(src);

                  Page found = findPage(existingPages, src);
                  if (found == null)
                  {
                     rollbackDeletes.getPages().add(src);
                  }
                  else
                  {
                     rollbackSaves.getPages().add(PageUtils.copy(found));
                  }
               }
            }
            break;
         case OVERWRITE:
            if (size == 0)
            {
               dst = data;
               rollbackDeletes = data;
            }
            else
            {
               List<Page> existingPages = new ArrayList<Page>(list.getAll());
               rollbackSaves = new Page.PageSet();
               rollbackSaves.setPages(new ArrayList<Page>(size));
               rollbackDeletes = new Page.PageSet();
               rollbackDeletes.setPages(new ArrayList<Page>());
               for (Page page : existingPages)
               {
                  Page copy = PageUtils.copy(page);
                  dataStorage.remove(page);
                  rollbackSaves.getPages().add(copy);
               }
               for (Page src : data.getPages())
               {
                  Page found = findPage(rollbackSaves.getPages(), src);
                  if (found == null)
                  {
                     rollbackDeletes.getPages().add(src);
                  }
               }

               dst = data;
            }
            break;
      }

      if (dst != null)
      {
         for (Page page : dst.getPages())
         {
            dataStorage.save(page);
         }
      }
   }

   
   public void rollback() throws Exception
   {
      if (rollbackDeletes != null && !rollbackDeletes.getPages().isEmpty())
      {
         for (Page page : rollbackDeletes.getPages())
         {
            dataStorage.remove(page);
         }
      }
      if (rollbackSaves != null && !rollbackSaves.getPages().isEmpty())
      {
         for (Page page : rollbackSaves.getPages())
         {
            dataStorage.save(page);
         }
      }
   }

   Page.PageSet getRollbackSaves()
   {
      return rollbackSaves;
   }

   Page.PageSet getRollbackDeletes()
   {
      return rollbackDeletes;
   }

   private Page findPage(List<Page> pages, Page src)
   {
      Page found = null;
      for (Page page : pages)
      {
         if (src.getName().equals(page.getName()))
         {
            found = page;
         }
      }
      return found;
   }
}

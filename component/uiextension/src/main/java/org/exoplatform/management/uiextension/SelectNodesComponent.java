package org.exoplatform.management.uiextension;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.management.uiextension.comparaison.NodeComparaison;
import org.exoplatform.management.uiextension.comparaison.NodeComparaisonState;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIGrid;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormDateTimeInput;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.input.UICheckBoxInput;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
@ComponentConfigs({ @ComponentConfig(
  type = UIGrid.class,
  id = "uiSelectedNodesGrid",
  template = "classpath:groovy/webui/component/explorer/popup/staging/UISelectedNodesGrid.gtmpl"), @ComponentConfig(
  type = UIGrid.class,
  id = "selectNodesGrid",
  template = "classpath:groovy/webui/component/explorer/popup/staging/UISelectNodesGrid.gtmpl"), @ComponentConfig(
  lifecycle = UIFormLifecycle.class,
  template = "classpath:groovy/webui/component/explorer/popup/staging/SelectContents.gtmpl",
  events = { @EventConfig(
    listeners = SelectNodesComponent.FilterActionListener.class), @EventConfig(
    listeners = SelectNodesComponent.SelectActionListener.class), @EventConfig(
    listeners = SelectNodesComponent.SelectAllActionListener.class), @EventConfig(
    listeners = SelectNodesComponent.DeleteAllActionListener.class), @EventConfig(
    listeners = SelectNodesComponent.DeleteActionListener.class) }) })
public class SelectNodesComponent extends UIForm implements UIPopupComponent {
  private static final Log LOG = ExoLogger.getLogger(SelectNodesComponent.class.getName());

  public static final String CONTENT_STATE_FIELD_NAME = "contentState";
  public static final String FILTER_CONTENT_FIELD_NAME = "filter";
  public static final String PUBLICATION_DATE_FIELD_NAME = "modifiedAfter";
  public static final String PUBLISHED_CONTENT_ONLY_FIELD_NAME = "publishedContentOnly";

  public static String[] COMPARAISON_BEAN_FIELD = { "title", "path", "publishedOnSource", "sourceModificationDate", "targetModificationDate", "stateLocalized" };

  public static String[] COMPARAISON_BEAN_ACTION = { "Select" };

  public static String[] SELECTED_COMPARAISON_BEAN_FIELD = { "title", "path", "stateLocalized" };

  public static String[] SELECTED_COMPARAISON_BEAN_ACTION = { "Delete" };

  private UIGrid nodesGrid;
  private UIGrid selectedNodesGrid;
  protected UIFormStringInput filterField;
  protected UIFormSelectBox stateSelectBoxInput;
  protected UICheckBoxInput publishedCheckBoxInput;
  UIFormDateTimeInput fromDateModified = null;
  private PushContentPopupComponent pushContentPopupComponent;

  String stateString = null;
  Calendar modifiedDateFilter = null;
  String filterString = null;
  boolean publishedContentOnly = false;

  private List<NodeComparaison> comparaisons;
  private List<NodeComparaison> filteredComparaison;

  public SelectNodesComponent() throws Exception {
    ResourceBundle resourceBundle = WebuiRequestContext.getCurrentInstance().getApplicationResourceBundle();

    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>();
    options.add(new SelectItemOption<String>(resourceBundle.getString("PushContent.state.all"), ""));
    options.add(new SelectItemOption<String>(NodeComparaisonState.MODIFIED_ON_SOURCE.getLabel(resourceBundle), NodeComparaisonState.MODIFIED_ON_SOURCE.getKey()));
    options.add(new SelectItemOption<String>(NodeComparaisonState.MODIFIED_ON_TARGET.getLabel(resourceBundle), NodeComparaisonState.MODIFIED_ON_TARGET.getKey()));
    options.add(new SelectItemOption<String>(NodeComparaisonState.NOT_FOUND_ON_SOURCE.getLabel(resourceBundle), NodeComparaisonState.NOT_FOUND_ON_SOURCE.getKey()));
    options.add(new SelectItemOption<String>(NodeComparaisonState.NOT_FOUND_ON_TARGET.getLabel(resourceBundle), NodeComparaisonState.NOT_FOUND_ON_TARGET.getKey()));
    options.add(new SelectItemOption<String>(NodeComparaisonState.SAME.getLabel(resourceBundle), NodeComparaisonState.SAME.getKey()));
    options.add(new SelectItemOption<String>(NodeComparaisonState.UNKNOWN.getLabel(resourceBundle), NodeComparaisonState.UNKNOWN.getKey()));

    stateSelectBoxInput = new UIFormSelectBox(CONTENT_STATE_FIELD_NAME, CONTENT_STATE_FIELD_NAME, options);
    addUIFormInput(stateSelectBoxInput);
    stateSelectBoxInput.setHTMLAttribute("style", "width:120px;");

    publishedCheckBoxInput = new UICheckBoxInput(PUBLISHED_CONTENT_ONLY_FIELD_NAME, PUBLISHED_CONTENT_ONLY_FIELD_NAME, false);
    publishedCheckBoxInput.setLabel("publishedContentOnly");
    publishedCheckBoxInput.setChecked(true);
    addUIFormInput(publishedCheckBoxInput);

    filterField = new UIFormStringInput(FILTER_CONTENT_FIELD_NAME, FILTER_CONTENT_FIELD_NAME, "");
    addUIFormInput(filterField);
    filterField.setHTMLAttribute("style", "width:90px;");

    fromDateModified = new UIFormDateTimeInput(PUBLICATION_DATE_FIELD_NAME, PUBLICATION_DATE_FIELD_NAME, null, false);
    addUIFormInput(fromDateModified);
    fromDateModified.setHTMLAttribute("style", "width:135px;");

    nodesGrid = addChild(UIGrid.class, "selectNodesGrid", "selectNodesGrid");
    nodesGrid.configure("path", COMPARAISON_BEAN_FIELD, COMPARAISON_BEAN_ACTION);

    selectedNodesGrid = addChild(UIGrid.class, "uiSelectedNodesGrid", "uiSelectedNodesGrid");
    selectedNodesGrid.configure("path", SELECTED_COMPARAISON_BEAN_FIELD, SELECTED_COMPARAISON_BEAN_ACTION);
  }

  public void init() throws Exception {
    String fieldJavascriptAction = event("Filter", null).replace("javascript:", "");
    filterField.setHTMLAttribute("onblur", fieldJavascriptAction);
    filterField.setHTMLAttribute("onkeypress", "if(event.keyCode == 13){" + fieldJavascriptAction + ";event.preventDefault();}");
    fromDateModified.setHTMLAttribute("onblur", "var textField = this; setTimeout(function(){ " + fieldJavascriptAction + "}, 200);");
    fromDateModified.setHTMLAttribute("oninput", "var textField = this; setTimeout(function(){ " + fieldJavascriptAction + "}, 200);");
    fromDateModified.setHTMLAttribute("onkeypress", "if(event.keyCode == 13)event.preventDefault()");
    publishedCheckBoxInput.setOnChange("Filter");
    stateSelectBoxInput.setOnChange("Filter");

    this.stateString = pushContentPopupComponent.stateString;
    stateSelectBoxInput.setValue(this.stateString);

    this.modifiedDateFilter = pushContentPopupComponent.modifiedDateFilter;
    fromDateModified.setCalendar(this.modifiedDateFilter);

    this.filterString = pushContentPopupComponent.filterString;
    filterField.setValue(this.filterString);

    this.publishedContentOnly = pushContentPopupComponent.publishedContentOnly;
    publishedCheckBoxInput.setChecked(this.publishedContentOnly);
  }

  public void setComparaisons(List<NodeComparaison> comparaisons) {
    this.comparaisons = comparaisons;
    filteredComparaison = new ArrayList<NodeComparaison>(comparaisons);
    computeComparaisons();
  }

  public void computeComparaisons() {
    List<NodeComparaison> alreadySelectedNodes = pushContentPopupComponent.getSelectedNodes();
    filteredComparaison.clear();

    pushContentPopupComponent.filterString = filterString = filterField.getValue();
    pushContentPopupComponent.modifiedDateFilter = modifiedDateFilter = fromDateModified.getCalendar();
    pushContentPopupComponent.stateString = stateString = stateSelectBoxInput.getValue();
    pushContentPopupComponent.publishedContentOnly = publishedContentOnly = publishedCheckBoxInput.getValue();

    for (NodeComparaison nodeComparaison : comparaisons) {
      if (!alreadySelectedNodes.contains(nodeComparaison) && checkFilter(nodeComparaison)) {
        filteredComparaison.add(nodeComparaison);
      }
    }
    nodesGrid.getUIPageIterator().setPageList(new LazyPageList<NodeComparaison>(new ListAccessImpl<NodeComparaison>(NodeComparaison.class, filteredComparaison), 5));

    if (pushContentPopupComponent.getSelectedNodes().isEmpty()) {
      selectedNodesGrid.getUIPageIterator().setPageList(
          new LazyPageList<NodeComparaison>(new ListAccessImpl<NodeComparaison>(NodeComparaison.class, pushContentPopupComponent.getDefaultSelection()), 5));
    } else {
      selectedNodesGrid.getUIPageIterator().setPageList(new LazyPageList<NodeComparaison>(new ListAccessImpl<NodeComparaison>(NodeComparaison.class, pushContentPopupComponent.getSelectedNodes()), 5));
    }
  }

  public static class FilterActionListener extends EventListener<SelectNodesComponent> {
    @Override
    public void execute(Event<SelectNodesComponent> event) throws Exception {
      SelectNodesComponent selectNodesComponent = event.getSource();

      selectNodesComponent.computeComparaisons();
      event.getRequestContext().addUIComponentToUpdateByAjax(selectNodesComponent.getNodesGrid());
    }
  }

  public static class SelectActionListener extends EventListener<SelectNodesComponent> {
    @Override
    public void execute(Event<SelectNodesComponent> event) throws Exception {
      SelectNodesComponent selectNodesComponent = event.getSource();
      PushContentPopupComponent pushContentPopupComponent = selectNodesComponent.getPushContentPopupComponent();

      String path = event.getRequestContext().getRequestParameter(OBJECTID);

      Iterator<NodeComparaison> comparaisons = selectNodesComponent.getComparaisons().iterator();
      NodeComparaison selectedNodeComparaison = null;
      while (selectedNodeComparaison == null && comparaisons.hasNext()) {
        NodeComparaison tmpNodeComparaison = comparaisons.next();
        if (path.equals(tmpNodeComparaison.getPath())) {
          selectedNodeComparaison = tmpNodeComparaison;
          break;
        }
      }
      pushContentPopupComponent.addSelection(selectedNodeComparaison);
      selectNodesComponent.computeComparaisons();

      event.getRequestContext().addUIComponentToUpdateByAjax(selectNodesComponent);
    }
  }

  public static class DeleteAllActionListener extends EventListener<SelectNodesComponent> {
    @Override
    public void execute(Event<SelectNodesComponent> event) throws Exception {
      SelectNodesComponent selectNodesComponent = event.getSource();
      selectNodesComponent.getSelectedNodesGrid().getUIPageIterator()
          .setPageList(new LazyPageList<NodeComparaison>(new ListAccessImpl<NodeComparaison>(NodeComparaison.class, selectNodesComponent.getPushContentPopupComponent().getDefaultSelection()), 5));

      selectNodesComponent.getPushContentPopupComponent().getSelectedNodes().clear();
      selectNodesComponent.computeComparaisons();
      event.getRequestContext().addUIComponentToUpdateByAjax(selectNodesComponent.getNodesGrid());
      event.getRequestContext().addUIComponentToUpdateByAjax(selectNodesComponent.getSelectedNodesGrid());
    }
  }

  static public class DeleteActionListener extends EventListener<UIForm> {
    public void execute(Event<UIForm> event) throws Exception {
      UIForm uiForm = event.getSource();
      PushContentPopupComponent pushContentPopupComponent = null;
      if (uiForm instanceof PushContentPopupComponent) {
        pushContentPopupComponent = (PushContentPopupComponent) uiForm;
      } else if (uiForm instanceof SelectNodesComponent) {
        pushContentPopupComponent = ((SelectNodesComponent) uiForm).getPushContentPopupComponent();
      }
      String path = event.getRequestContext().getRequestParameter(OBJECTID);

      try {
        Iterator<NodeComparaison> comparaisons = pushContentPopupComponent.getSelectedNodes().iterator();
        boolean removed = false;
        while (!removed && comparaisons.hasNext()) {
          NodeComparaison comparaison = comparaisons.next();
          if (path.equals(comparaison.getPath())) {
            comparaisons.remove();
            removed = true;
          }
        }
        if (removed) {
          if (pushContentPopupComponent.getSelectedNodes().isEmpty()) {
            pushContentPopupComponent.getSelectNodesComponent().getSelectedNodesGrid().getUIPageIterator()
                .setPageList(new LazyPageList<NodeComparaison>(new ListAccessImpl<NodeComparaison>(NodeComparaison.class, pushContentPopupComponent.getDefaultSelection()), 5));
          } else {
            pushContentPopupComponent.getSelectNodesComponent().getSelectedNodesGrid().getUIPageIterator()
                .setPageList(new LazyPageList<NodeComparaison>(new ListAccessImpl<NodeComparaison>(NodeComparaison.class, pushContentPopupComponent.getSelectedNodes()), 5));
          }
        }
        if (pushContentPopupComponent.getSelectNodesComponent().isRendered()) {
          pushContentPopupComponent.getSelectNodesComponent().computeComparaisons();
          event.getRequestContext().addUIComponentToUpdateByAjax(pushContentPopupComponent.getSelectNodesComponent());
        }
      } catch (Exception ex) {
        ApplicationMessage message = new ApplicationMessage("PushContent.msg.synchronizationError", null, ApplicationMessage.ERROR);
        message.setResourceBundle(pushContentPopupComponent.getResourceBundle());
        pushContentPopupComponent.getUIFormInputInfo(PushContentPopupComponent.INFO_FIELD_NAME).setValue(message.getMessage());
        LOG.error("Error while deleting '" + path + "' from selected contents:", ex);
      }
    }
  }

  public static class SelectAllActionListener extends EventListener<SelectNodesComponent> {
    @Override
    public void execute(Event<SelectNodesComponent> event) throws Exception {
      SelectNodesComponent selectNodesComponent = event.getSource();
      PushContentPopupComponent pushContentPopupComponent = selectNodesComponent.getPushContentPopupComponent();
      List<NodeComparaison> comparaisons = selectNodesComponent.getFilteredComparaison();
      for (NodeComparaison nodeComparaison : comparaisons) {
        pushContentPopupComponent.addSelection(nodeComparaison);
      }
      selectNodesComponent.computeComparaisons();
      event.getRequestContext().addUIComponentToUpdateByAjax(selectNodesComponent.getNodesGrid());
      event.getRequestContext().addUIComponentToUpdateByAjax(selectNodesComponent.getSelectedNodesGrid());
    }
  }

  @Override
  public void activate() {}

  @Override
  public void deActivate() {}

  public UIGrid getSelectedNodesGrid() {
    return selectedNodesGrid;
  }

  public UIGrid getNodesGrid() {
    return nodesGrid;
  }

  public List<NodeComparaison> getComparaisons() {
    return comparaisons;
  }

  public List<NodeComparaison> getFilteredComparaison() {
    return filteredComparaison;
  }

  public void setPushContentPopupComponent(PushContentPopupComponent pushContentPopupComponent) {
    this.pushContentPopupComponent = pushContentPopupComponent;
  }

  public PushContentPopupComponent getPushContentPopupComponent() {
    return pushContentPopupComponent;
  }

  private boolean checkFilter(NodeComparaison nodeComparaison) {
    boolean isMatch = filterString == null || filterString.isEmpty() || nodeComparaison.getTitle().toLowerCase().contains(filterString.toLowerCase());

    if (isMatch) {
      isMatch = modifiedDateFilter == null || nodeComparaison.getSourceModificationDateCalendar() == null || modifiedDateFilter.before(nodeComparaison.getSourceModificationDateCalendar());
    }

    if (isMatch) {
      isMatch = stateString == null || stateString.isEmpty() || nodeComparaison.getState().getKey().equals(stateString);
    }

    if (isMatch) {
      isMatch = !publishedContentOnly || nodeComparaison.isPublishedOnSource();
    }
    return isMatch;
  }

  public boolean isDefaultEntry(String path) {
    for (NodeComparaison comparaison : pushContentPopupComponent.getDefaultSelection()) {
      if (path.equals(comparaison.getPath())) {
        return true;
      }
    }
    return false;
  }

}
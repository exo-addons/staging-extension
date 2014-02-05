package org.exoplatform.management.uiextension;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.management.service.handler.content.NodeComparaison;
import org.exoplatform.management.service.handler.content.NodeComparaisonState;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIGrid;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.input.UICheckBoxInput;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
@ComponentConfigs(
  { @ComponentConfig(
    type = UIGrid.class,
    id = "selectNodesGrid",
    template = "classpath:groovy/webui/component/explorer/popup/staging/UISelectNodesGrid.gtmpl"), @ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "classpath:groovy/webui/component/explorer/popup/staging/SelectContents.gtmpl",
    events =
      { @EventConfig(
        listeners = SelectNodesPopupComponent.SelectActionListener.class), @EventConfig(
        listeners = SelectNodesPopupComponent.ChangeVisibilityActionListener.class) }) })
public class SelectNodesPopupComponent extends UIForm implements UIPopupComponent {
  private static final String DIFF_CONTENT_FIELD_NAME = "seeOnlyDifferent";

  public static String[] COMPARAISON_BEAN_FIELD =
    { "title", "path", "lastModifierUserName", "sourceModificationDate", "targetModificationDate", "stateLocalized" };

  public static String[] COMPARAISON_BEAN_ACTION =
    { "Select" };

  private UIGrid nodesGrid;
  protected UICheckBoxInput diffCheckBoxInput;
  protected boolean diffChecked = true;
  private List<NodeComparaison> comparaisons;
  private PushContentPopupComponent pushContentPopupComponent;

  public SelectNodesPopupComponent() throws Exception {
    diffCheckBoxInput = new UICheckBoxInput(DIFF_CONTENT_FIELD_NAME, DIFF_CONTENT_FIELD_NAME, true);
    addUIFormInput(diffCheckBoxInput);
    diffCheckBoxInput.setOnChange("ChangeVisibility", this.getId());
    diffCheckBoxInput.setLabel("seeOnlyDifferent");
    diffChecked = true;

    nodesGrid = addChild(UIGrid.class, "selectNodesGrid", "selectNodesGrid");
    nodesGrid.configure("path", COMPARAISON_BEAN_FIELD, COMPARAISON_BEAN_ACTION);
  }

  public void setComparaisons(List<NodeComparaison> comparaisons) {
    this.comparaisons = comparaisons;
    computeComparaisons();
  }

  public void computeComparaisons() {
    diffCheckBoxInput.setValue(diffChecked);

    List<NodeComparaison> alreadySelectedNodes = pushContentPopupComponent.getSelectedNodes();
    List<NodeComparaison> filteredComparaison = new ArrayList<NodeComparaison>();
    boolean diff = diffCheckBoxInput.getValue();
    for (NodeComparaison nodeComparaison : comparaisons) {
      if ((!diff || !NodeComparaisonState.SAME.equals(nodeComparaison.getState()))) {
        if (!alreadySelectedNodes.contains(nodeComparaison)) {
          filteredComparaison.add(nodeComparaison);
        }
      }
    }
    nodesGrid.getUIPageIterator().setPageList(new LazyPageList<NodeComparaison>(new ListAccessImpl<NodeComparaison>(NodeComparaison.class, filteredComparaison), 10));
  }

  public static class ChangeVisibilityActionListener extends EventListener<SelectNodesPopupComponent> {
    @Override
    public void execute(Event<SelectNodesPopupComponent> event) throws Exception {
      SelectNodesPopupComponent selectNodesPopupComponent = event.getSource();
      selectNodesPopupComponent.setDiffChecked(selectNodesPopupComponent.getDiffCheckBoxInput().getValue());

      selectNodesPopupComponent.computeComparaisons();
      event.getRequestContext().addUIComponentToUpdateByAjax(selectNodesPopupComponent.getNodesGrid());
    }
  }

  public static class SelectActionListener extends EventListener<SelectNodesPopupComponent> {
    @Override
    public void execute(Event<SelectNodesPopupComponent> event) throws Exception {
      SelectNodesPopupComponent selectNodesPopupComponent = event.getSource();
      PushContentPopupComponent pushContentPopupComponent = selectNodesPopupComponent.getPushContentPopupComponent();

      String path = event.getRequestContext().getRequestParameter(OBJECTID);

      Iterator<NodeComparaison> comparaisons = selectNodesPopupComponent.getComparaisons().iterator();
      NodeComparaison selectedNodeComparaison = null;
      while (selectedNodeComparaison == null && comparaisons.hasNext()) {
        NodeComparaison tmpNodeComparaison = comparaisons.next();
        if (path.equals(tmpNodeComparaison.getPath())) {
          selectedNodeComparaison = tmpNodeComparaison;
        }
      }
      pushContentPopupComponent.addSelection(selectedNodeComparaison);
      selectNodesPopupComponent.computeComparaisons();

      event.getRequestContext().addUIComponentToUpdateByAjax(selectNodesPopupComponent.getNodesGrid());
      event.getRequestContext().addUIComponentToUpdateByAjax(pushContentPopupComponent.getSelectedNodesGrid());
    }
  }

  @Override
  public void activate() {
  }

  @Override
  public void deActivate() {
  }

  public UIGrid getNodesGrid() {
    return nodesGrid;
  }

  public List<NodeComparaison> getComparaisons() {
    return comparaisons;
  }

  public void setPushContentPopupComponent(PushContentPopupComponent pushContentPopupComponent) {
    this.pushContentPopupComponent = pushContentPopupComponent;
  }

  public PushContentPopupComponent getPushContentPopupComponent() {
    return pushContentPopupComponent;
  }

  public UICheckBoxInput getDiffCheckBoxInput() {
    return diffCheckBoxInput;
  }

  public void setDiffChecked(boolean diffChecked) {
    this.diffChecked = diffChecked;
  }
}
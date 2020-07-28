package org.modelix.model.mpsplugin.plugin;



import jetbrains.mps.workbench.action.BaseAction;
import javax.swing.Icon;
import jetbrains.mps.workbench.action.ActionAccess;
import com.intellij.openapi.actionSystem.AnActionEvent;
import java.util.Map;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import javax.swing.tree.TreeNode;
import jetbrains.mps.ide.actions.MPSCommonDataKeys;
import org.jetbrains.annotations.NotNull;
import org.modelix.model.mpsplugin.history.CloudBranchTreeNode;

public class SwitchBranch_Action extends BaseAction {
  private static final Icon ICON = null;

  public SwitchBranch_Action() {
    super("Switch to This Branch", "", ICON);
    this.setIsAlwaysVisible(false);
    this.setActionAccess(ActionAccess.UNDO_PROJECT);
  }
  @Override
  public boolean isDumbAware() {
    return true;
  }
  @Override
  protected boolean collectActionData(AnActionEvent event, final Map<String, Object> _params) {
    if (!(super.collectActionData(event, _params))) {
      return false;
    }
    {
      Project p = event.getData(CommonDataKeys.PROJECT);
      if (p == null) {
        return false;
      }
    }
    {
      TreeNode p = event.getData(MPSCommonDataKeys.TREE_NODE);
      if (p == null) {
        return false;
      }
    }
    return true;
  }
  @Override
  public void doExecute(@NotNull final AnActionEvent event, final Map<String, Object> _params) {
    CloudBranchTreeNode branchTreeNode = ((CloudBranchTreeNode) event.getData(MPSCommonDataKeys.TREE_NODE));
    branchTreeNode.switchBranch();
  }
}

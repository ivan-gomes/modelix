package org.modelix.model.mpsplugin.plugin;



import jetbrains.mps.workbench.action.BaseAction;
import javax.swing.Icon;
import com.intellij.openapi.actionSystem.AnActionEvent;
import java.util.Map;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import javax.swing.tree.TreeNode;
import jetbrains.mps.ide.actions.MPSCommonDataKeys;
import org.jetbrains.annotations.NotNull;
import org.modelix.model.mpsplugin.history.CloudBranchTreeNode;
import org.modelix.model.mpsplugin.history.CloudTreeTreeNode;
import de.q60.mps.shadowmodels.runtime.model.persistent.IBranch;
import jetbrains.mps.baseLanguage.closures.runtime._FunctionTypes;
import jetbrains.mps.lang.smodel.generator.smodelAdapter.SPropertyOperations;
import org.modelix.model.lazy.TreeId;
import org.modelix.model.mpsplugin.CloudRepository;
import org.modelix.model.mpsplugin.IModelClient;
import org.modelix.model.mpsplugin.SharedExecutors;
import org.modelix.model.lazy.CLVersion;
import javax.swing.SwingUtilities;
import jetbrains.mps.plugins.projectplugins.ProjectPluginManager;
import org.jetbrains.mps.openapi.language.SProperty;
import jetbrains.mps.smodel.adapter.structure.MetaAdapterFactory;

public class LoadHistoryForBranch_Action extends BaseAction {
  private static final Icon ICON = null;

  public LoadHistoryForBranch_Action() {
    super("Show History", "", ICON);
    this.setIsAlwaysVisible(false);
    this.setExecuteOutsideCommand(true);
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
    final CloudBranchTreeNode branchTNode = (CloudBranchTreeNode) event.getData(MPSCommonDataKeys.TREE_NODE);
    CloudTreeTreeNode treeTNode = branchTNode.getAncestor(CloudTreeTreeNode.class);
    IBranch infoBranch = treeTNode.getCloudRepository().getInfoBranch();
    String branchName = infoBranch.computeRead(new _FunctionTypes._return_P0_E0<String>() {
      public String invoke() {
        return SPropertyOperations.getString(branchTNode.getBranchInfo(), PROPS.name$tAp1);
      }
    });
    final TreeId treeId = treeTNode.getTreeId();
    final String branchKey = treeId.getBranchKey(branchName);
    final CloudRepository cloudRepository = treeTNode.getCloudRepository();
    final IModelClient client = cloudRepository.getClient();
    SharedExecutors.FIXED.execute(new Runnable() {
      public void run() {
        String versionHash = client.get(branchKey);
        final CLVersion version = CLVersion.loadFromHash(versionHash, client.getStoreCache());
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            event.getData(CommonDataKeys.PROJECT).getComponent(ProjectPluginManager.class).getTool(CloudHistoryTool_Tool.class).load(cloudRepository, treeId, version);
          }
        });
      }
    });
  }

  private static final class PROPS {
    /*package*/ static final SProperty name$tAp1 = MetaAdapterFactory.getProperty(0xceab519525ea4f22L, 0x9b92103b95ca8c0cL, 0x110396eaaa4L, 0x110396ec041L, "name");
  }
}

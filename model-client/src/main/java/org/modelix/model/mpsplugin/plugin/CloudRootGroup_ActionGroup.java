package org.modelix.model.mpsplugin.plugin;



import jetbrains.mps.plugins.actions.GeneratedActionGroup;
import org.jetbrains.annotations.Nullable;
import jetbrains.mps.workbench.action.ApplicationPlugin;

public class CloudRootGroup_ActionGroup extends GeneratedActionGroup {
  public static final String ID = "org.modelix.model.mpsplugin.plugin.CloudRootGroup_ActionGroup";

  public CloudRootGroup_ActionGroup(@Nullable ApplicationPlugin plugin) {
    super("CloudRootGroup", ID, plugin);
    setIsInternal(false);
    setPopup(false);
    CloudRootGroup_ActionGroup.this.addAction("org.modelix.model.mpsplugin.plugin.AddCloudRepository_Action");
  }
}

package org.modelix.model.mpsplugin.plugin;



import jetbrains.mps.plugins.projectplugins.BaseProjectPlugin;
import java.util.List;
import jetbrains.mps.ide.tools.BaseTool;
import com.intellij.openapi.project.Project;
import jetbrains.mps.internal.collections.runtime.ListSequence;
import java.util.ArrayList;
import jetbrains.mps.plugins.part.ProjectPluginPart;

public class Mpsplugin_ProjectPlugin extends BaseProjectPlugin {
  public Mpsplugin_ProjectPlugin() {
  }
  public List<BaseTool> initAllTools1(Project project) {
    List<BaseTool> tools = ListSequence.fromList(new ArrayList<BaseTool>());
    ListSequence.fromList(tools).addElement(new CloudHistoryTool_Tool(project));
    ListSequence.fromList(tools).addElement(new CloudTool_Tool(project));
    return tools;
  }
  public void fillCustomParts(List<ProjectPluginPart> parts) {
    parts.add(new ProjectPlugin_ProjectPluginPart());
  }
}

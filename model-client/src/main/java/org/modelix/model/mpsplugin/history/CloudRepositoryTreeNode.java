package org.modelix.model.mpsplugin.history;



import jetbrains.mps.ide.ui.tree.TextTreeNode;
import de.q60.mps.shadowmodels.runtime.model.persistent.IBranchListener;
import de.q60.mps.shadowmodels.runtime.model.persistent.ITree;
import javax.swing.SwingUtilities;
import org.modelix.model.mpsplugin.CloudRepository;
import de.q60.mps.shadowmodels.runtime.model.persistent.IBranch;
import org.modelix.model.mpsplugin.CloudIcons;
import java.util.Map;
import org.jetbrains.mps.openapi.model.SNode;
import jetbrains.mps.internal.collections.runtime.MapSequence;
import java.util.LinkedHashMap;
import jetbrains.mps.ide.ThreadUtils;
import jetbrains.mps.internal.collections.runtime.Sequence;
import javax.swing.tree.TreeNode;
import org.modelix.model.mpsplugin.SharedExecutors;
import java.util.List;
import jetbrains.mps.baseLanguage.closures.runtime._FunctionTypes;
import jetbrains.mps.internal.collections.runtime.IListSequence;
import jetbrains.mps.internal.collections.runtime.ListSequence;
import jetbrains.mps.lang.smodel.generator.smodelAdapter.SLinkOperations;
import jetbrains.mps.internal.collections.runtime.ISelector;
import jetbrains.mps.internal.collections.runtime.IVisitor;
import org.jetbrains.mps.openapi.language.SContainmentLink;
import jetbrains.mps.smodel.adapter.structure.MetaAdapterFactory;

public class CloudRepositoryTreeNode extends TextTreeNode {
  private IBranchListener branchListener = new IBranchListener() {
    @Override
    public void treeChanged(ITree oldTree, ITree newTree) {
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          ((CloudView.CloudViewTree) getTree()).runRebuildAction(new Runnable() {
            public void run() {
              updateChildren();
            }
          }, true);
        }
      });
    }
  };
  private CloudRepository cloudRepository;
  private IBranch infoBranch;
  private CloudRepository.IListener repoListener = new CloudRepository.IListener() {
    @Override
    public void connectionStatusChanged(final boolean connected) {
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          if (connected) {
            infoBranch = cloudRepository.getInfoBranch();
            if (getTree() != null) {
              infoBranch.addListener(branchListener);
            }
          }
          updateText();
          updateChildren();
        }
      });
    }
  };

  public CloudRepositoryTreeNode(CloudRepository cloudRepository) {
    super(CloudIcons.REPOSITORY_ICON, cloudRepository.getBaseUrl());
    this.cloudRepository = cloudRepository;
    setAllowsChildren(true);
    setNodeIdentifier("" + System.identityHashCode(cloudRepository));
    cloudRepository.addListener(repoListener);
    updateText();
    updateChildren();
  }

  public CloudRepository getCloudRepository() {
    return this.cloudRepository;
  }

  public void updateText() {
    String text = cloudRepository.getBaseUrl();
    if (cloudRepository.isConnected()) {
      text += " (" + cloudRepository.getId() + ")";
    } else {
      text += " (not connected)";
    }
    String email = cloudRepository.getEmail();
    if ((email != null && email.length() > 0)) {
      text += " " + email;
    }
    setTextAndRepaint(text);
  }

  public void setTextAndRepaint(String text) {
    TreeModelUtil.setTextAndRepaint(this, text);
  }

  public void updateChildren() {
    if (cloudRepository.isConnected()) {
      final Map<SNode, CloudTreeTreeNode> existing = MapSequence.fromMap(new LinkedHashMap<SNode, CloudTreeTreeNode>(16, (float) 0.75, false));
      ThreadUtils.runInUIThreadAndWait(new Runnable() {
        public void run() {
          if (Sequence.fromIterable(TreeModelUtil.getChildren(CloudRepositoryTreeNode.this)).isEmpty()) {
            TreeModelUtil.setChildren(CloudRepositoryTreeNode.this, Sequence.<TreeNode>singleton(LoadingIcon.apply(new TextTreeNode("loading ..."))));
          }
          for (CloudTreeTreeNode node : Sequence.fromIterable(TreeModelUtil.getChildren(CloudRepositoryTreeNode.this)).ofType(CloudTreeTreeNode.class)) {
            MapSequence.fromMap(existing).put(node.getTreeInfo(), node);
          }
        }
      });

      SharedExecutors.FIXED.execute(new Runnable() {
        public void run() {
          final SNode info = cloudRepository.getInfo();
          final List<TreeNode> newChildren = cloudRepository.getInfoBranch().computeRead(new _FunctionTypes._return_P0_E0<IListSequence<TreeNode>>() {
            public IListSequence<TreeNode> invoke() {
              return ListSequence.fromList(SLinkOperations.getChildren(info, LINKS.trees$ECBY)).select(new ISelector<SNode, TreeNode>() {
                public TreeNode select(SNode it) {
                  TreeNode tn = (MapSequence.fromMap(existing).containsKey(it) ? MapSequence.fromMap(existing).get(it) : new CloudTreeTreeNode(cloudRepository, it));
                  return tn;
                }
              }).toListSequence();
            }
          });

          ThreadUtils.runInUIThreadNoWait(new Runnable() {
            public void run() {
              TreeModelUtil.setChildren(CloudRepositoryTreeNode.this, newChildren);
              Sequence.fromIterable(TreeModelUtil.getChildren(CloudRepositoryTreeNode.this)).ofType(CloudTreeTreeNode.class).visitAll(new IVisitor<CloudTreeTreeNode>() {
                public void visit(CloudTreeTreeNode it) {
                  it.updateChildren();
                }
              });
            }
          });
        }
      });
    } else {
      ThreadUtils.runInUIThreadNoWait(new Runnable() {
        public void run() {
          TreeModelUtil.clearChildren(CloudRepositoryTreeNode.this);
        }
      });
    }
  }

  @Override
  protected void onAdd() {
    super.onAdd();
    if (infoBranch != null) {
      infoBranch.addListener(branchListener);
    }
  }

  @Override
  protected void onRemove() {
    super.onRemove();
    if (infoBranch != null) {
      cloudRepository.getInfoBranch().removeListener(branchListener);
    }
  }

  private static final class LINKS {
    /*package*/ static final SContainmentLink trees$ECBY = MetaAdapterFactory.getContainmentLink(0xb6980ebdf01d459dL, 0xa95238740f6313b4L, 0x62b7d9b07cecbcbfL, 0x62b7d9b07cecbcc2L, "trees");
  }
}

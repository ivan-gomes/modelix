package org.modelix.model.mpsplugin.history;



import javax.swing.Icon;
import java.util.Set;
import jetbrains.mps.ide.ui.tree.MPSTreeNode;
import jetbrains.mps.internal.collections.runtime.SetSequence;
import java.util.HashSet;
import javax.swing.Timer;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import jetbrains.mps.ide.ui.tree.MPSTree;
import jetbrains.mps.internal.collections.runtime.ISelector;
import jetbrains.mps.internal.collections.runtime.NotNullWhereFilter;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.geom.Arc2D;

public class LoadingIcon implements Icon {
  private static LoadingIcon INSTANCE = new LoadingIcon();

  private Set<MPSTreeNode> activeNodes = SetSequence.fromSet(new HashSet<MPSTreeNode>());
  private double angle;
  private Timer timer;
  private int inactivity = 0;

  public static <T extends MPSTreeNode> T apply(final T treeNode) {
    treeNode.setIcon(INSTANCE);
    INSTANCE.register(treeNode);
    return treeNode;
  }

  public void register(MPSTreeNode treeNode) {
    SetSequence.fromSet(activeNodes).addElement(treeNode);
    ensureTimerRunning();
  }

  private void ensureTimerRunning() {
    if (timer == null || !(timer.isRunning())) {
      timer = new Timer(1000 / 60, new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          rotate();
          if (SetSequence.fromSet(activeNodes).isEmpty()) {
            if (inactivity > 5000 / 60) {
              SetSequence.fromSet(activeNodes).clear();
              timer.stop();
              timer = null;
            } else {
              inactivity++;
            }
            return;
          }

          for (MPSTree c : SetSequence.fromSet(activeNodes).select(new ISelector<MPSTreeNode, MPSTree>() {
            public MPSTree select(MPSTreeNode it) {
              return it.getTree();
            }
          }).where(new NotNullWhereFilter<MPSTree>()).distinct()) {
            c.repaint();
          }
        }
      });
      timer.start();
    }
  }

  private void rotate() {
    angle = (angle - 360.0 / 120.0) % 360.0;
  }

  @Override
  public void paintIcon(Component component, Graphics graphics, int x, int y) {
    inactivity = 0;
    ensureTimerRunning();

    double w = getIconWidth();
    double h = getIconHeight();
    Graphics2D g = (Graphics2D) graphics.create();
    try {
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

      g.setStroke(new BasicStroke(3.0f));
      g.setColor(new Color(80, 80, 80));

      g.draw(new Arc2D.Double(2.0 + x, 2.0 + y, w - 4.0, h - 4.0, angle, 250.0, Arc2D.OPEN));
    } finally {
      g.dispose();
    }
  }
  @Override
  public int getIconWidth() {
    return 16;
  }
  @Override
  public int getIconHeight() {
    return 16;
  }

}

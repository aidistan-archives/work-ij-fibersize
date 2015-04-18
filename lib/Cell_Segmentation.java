import ij.*;
import ij.gui.*;
import ij.plugin.frame.*;
import java.awt.*;
import java.awt.event.*;

public class Cell_Segmentation extends PlugInFrame {

  public final int WINDOW_WIDTH  = 140;
  public final int WINDOW_HEIGHT = 90;

  // External
  public RoiManager roiMgr;

  // Internal
  static PlugInFrame instance;

  public Cell_Segmentation() {
    super("Cell Seg");

    // Open ROI Manager
    roiMgr = RoiManager.getInstance();
    if (roiMgr == null)
      roiMgr = new RoiManager();

    // Open Cell Seg
    if (instance != null)
      instance.toFront();
    else {
      WindowManager.addWindow(this);
      setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
      GUI.center(this);
      setVisible(true);
      instance = this;
    }
  }

  // Overrides Component.addNotify(). getInsets() must be called after
  // addNotify() or it will not return the title bar height.
  public void addNotify() {
    super.addNotify();

    int xm = 20; // x margin
    int ym = getInsets().top + 25; // y margin
    int yw = 25;
    int yi = 10;

    setLayout(null);
    setForeground(java.awt.Color.darkGray);
    setResizable(false);

    Button segmentBtn = new Button("Segment");
    // segmentBtn.addActionListener(null);
    // segmentBtn.addKeyListener(null);
    segmentBtn.setName("segment");
    segmentBtn.setBounds(xm, ym, WINDOW_WIDTH - xm*2, yw);
    add(segmentBtn);

    // Button filterBtn = new Button("Filter");
    // filterBtn.addActionListener(null);
    // filterBtn.addKeyListener(null);
    // filterBtn.setName("filter");
    // filterBtn.setBounds(xm, ym + (yw + yi), WINDOW_WIDTH - xm*2, yw);
    // add(filterBtn);
  }

  // Overrides windowClosing in PluginFrame.
  public void windowClosing(WindowEvent e) {
    super.windowClosing(e);
    instance = null;
  }
}

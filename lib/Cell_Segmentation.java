import ij.*;
import ij.gui.*;
import ij.process.*;
import ij.plugin.filter.*;
import ij.plugin.frame.*;
import java.awt.*;
import java.awt.event.*;


/**
 * @version v2.0 (May 2, 2015) - implement a GUI
 *          v1.0 (May 1, 2015) - first implementation of this plugin
 * @author Aidi Stan (aidistan@live.cn)
 * @license the MIT license
 */

public class Cell_Segmentation extends PlugInFrame {

  // Constant
  public final int WINDOW_WIDTH = 300;
  public final int WINDOW_HEIGHT = 275;

  // External
  public ImagePlus _imp;

  // Internal
  public static PlugInFrame instance; // Singleton
  public ImagePlus imp;
  public ImageProcessor ip;

  // Controls
  public Scrollbar sclBar;
  public Checkbox chkBox;

  public Cell_Segmentation() {
    super("Cell Segmentation");
    if (instance != null) {
      instance.toFront();
      return;
    } else {
      WindowManager.addWindow(this);
      setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
      GUI.center(this);
      setVisible(true);

      // Initialize
      _imp = IJ.getImage();
      instance = this;
      imp = null;
      sclBar.setValue(get_current_ip().getAutoThreshold());
    }
  }

  /* Overrides Component.addNotify(). */
  // Init() must be called after addNotify() or getInsets() will not
  // return the title bar height.
  public void addNotify() {
    super.addNotify();
    int top = getInsets().top + 32;
    Label label;
    Button button;

    /*
     * Binaryzation
     */

    label = new Label("Step 1: Binaryzation", Label.LEFT);
    label.setFont(new Font("Helvetica", Font.BOLD, 12));
    label.setBounds(10, top, WINDOW_WIDTH - 20, 14);
    add(label);

    label = new Label("Threshold", Label.LEFT);
    label.setFont(new Font("Helvetica", Font.PLAIN, 12));
    label.setBounds(20, top + 30, 70, 14);
    add(label);

    sclBar = new Scrollbar(Scrollbar.HORIZONTAL, 0, 1, 0, 255);
    sclBar.setBounds(100, top + 28, WINDOW_WIDTH - 120, 16);
    add(sclBar);

    button = new Button("Binary");
    button.setFont(new Font("Helvetica", Font.PLAIN, 12));
    button.setBounds(20, top + 55, 73, 24);
    button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (imp != null) imp.close();
        binary();
      }
    });
    add(button);

    button = new Button("Erode");
    button.setFont(new Font("Helvetica", Font.PLAIN, 12));
    button.setBounds(113, top + 55, 73, 24);
    button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (imp == null) binary();
        ip.erode();
        imp.updateAndDraw();
      }
    });
    add(button);

    button = new Button("Dilate");
    button.setFont(new Font("Helvetica", Font.PLAIN, 12));
    button.setBounds(206, top + 55, 73, 24);
    button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (imp == null) binary();
        ip.dilate();
        imp.updateAndDraw();
      }
    });
    add(button);

    /*
     * Particle Analysis
     */

    label = new Label("Step 2: Particle Analysis", Label.LEFT);
    label.setFont(new Font("Helvetica", Font.BOLD, 12));
    label.setBounds(10, top + 100, WINDOW_WIDTH - 40, 14);
    add(label);

    label = new Label("Please select 'Add to Manager'", Label.LEFT);
    label.setFont(new Font("Helvetica", Font.PLAIN, 12));
    label.setBounds(20, top + 130, WINDOW_WIDTH - 40, 14);
    add(label);

    button = new Button("Analyze");
    button.setFont(new Font("Helvetica", Font.PLAIN, 12));
    button.setBounds(20, top + 155, 73, 24);
    button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        RoiManager roiMgr = RoiManager.getInstance();
        if (roiMgr != null) { roiMgr.reset(); roiMgr.close(); }
        if (imp == null) binary();
        analyze();
      }
    });
    add(button);

    chkBox = new Checkbox("Keep binary image", false);
    chkBox.setFont(new Font("Helvetica", Font.PLAIN, 12));
    chkBox.setBounds(120, top + 155, 160, 24);
    add(chkBox);

    label = new Label("Version 2.0 (May 2, 2015)", Label.RIGHT);
    label.setFont(new Font("Helvetica", Font.PLAIN, 11));
    label.setBounds(10, WINDOW_HEIGHT-24, WINDOW_WIDTH - 20, 14);
    add(label);

    setLayout(null);
    setResizable(false);
  }
  //
  // /* Overrides windowClosing in PluginFrame. */
  // public void windowClosing(WindowEvent e) {
  //     super.windowClosing(e);
  //     instance = null;
  // }

  // Get a copy of the current IP in Gray8 format
  public ImageProcessor get_current_ip() {
    int oriC = _imp.getC();
    _imp.setC(_imp.getNChannels());
    ImageProcessor ip = _imp.getChannelProcessor();
    ImageProcessor ip_ = ip.convertToByte(true);
    _imp.setC(oriC);
    return(ip_);
  }

  // Do the binaryzation
  public void binary() {
    ip = get_current_ip();
    ip.threshold(sclBar.getValue());
    imp = new ImagePlus("Cell Segmentation (Threshold : " + Integer.toString(sclBar.getValue()) + ")", ip);
    imp.show();
    imp.updateAndDraw();
    imp.addImageListener(new ImageListener() {
      public void imageClosed(ImagePlus imp) { ((Cell_Segmentation)instance).imp = null; }
      public void imageOpened(ImagePlus imp) {}
      public void imageUpdated(ImagePlus imp) {}
    });
  }

  // Do the analysis
  public void analyze() {
    ParticleAnalyzer pa = new ParticleAnalyzer();
    pa.showDialog();
    pa.analyze(imp, ip);

    // Rename
    RoiManager roiMgr = RoiManager.getInstance();
    Roi[] rois = roiMgr.getRoisAsArray();
    roiMgr.reset();
    for(int i = 0; i < rois.length; i++) {
      Roi roi = rois[i];
      roi = new PolygonRoi(roi.getInterpolatedPolygon(10, false), Roi.POLYGON);
      roi.setName(Integer.toString(i+1));
      roiMgr.addRoi(roi);
    }

    if (chkBox.getState()) {
      roiMgr.runCommand(imp, "Show All with labels");
    } else {
      imp.close();
      roiMgr.runCommand(_imp, "Show All with labels");
    }
  }

}

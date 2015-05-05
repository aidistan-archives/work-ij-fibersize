import ij.*;
import ij.gui.*;
import ij.process.*;
import ij.plugin.filter.*;
import ij.plugin.frame.*;
import java.awt.*;
import java.awt.event.*;


/**
 * @version v2.1 (May 5, 2015) - fix bugs and support fiber type selection
 *          v2.0 (May 2, 2015) - implement a GUI
 *          v1.0 (May 1, 2015) - first implementation of this plugin
 * @author Aidi Stan (aidistan@live.cn)
 * @license the MIT license
 */

public class Fiber_Size extends PlugInFrame {

  // Constant
  public final int WINDOW_WIDTH = 480;
  public final int WINDOW_HEIGHT = 250;

  // External
  public ImagePlus _imp;

  // Internal
  public static PlugInFrame instance; // Singleton
  public ImageListener impLst;
  public ImageProcessor ip;
  public ImagePlus imp;

  // Controls
  public TextField imgField;
  public Scrollbar sclBar;
  public TextField thdField;

  public Fiber_Size() {
    super("Fiber Size");
    if (instance != null) {
      instance.toFront();
      return;
    } else {
      // Initialize instance variables
      _imp = IJ.getImage();
      instance = this;
      imp = null;

      impLst = new ImageListener() {
        public void imageClosed(ImagePlus imp_) {
          if (imp_ == imp) imp = null;
        }
        public void imageOpened(ImagePlus imp_) {}
        public void imageUpdated(ImagePlus imp_) {}
      };
      ImagePlus.addImageListener(impLst);

      // Update the window
      WindowManager.addWindow(this);
      setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
      GUI.center(this);
      setVisible(true);
    }
  }

  /* Overrides Component.addNotify(). */
  // Init() must be called after addNotify() or getInsets() will not
  // return the title bar height.
  public void addNotify() {
    super.addNotify();
    int top = getInsets().top + 20;
    Label label;
    Button button;

    // For the whole frame
    Font normFont = new Font("Helvetica", Font.PLAIN, 12);
    Font boldFont = new Font("Helvetica", Font.BOLD, 12);
    setFont(normFont);
    setLayout(null);
    setResizable(false);

    /*
     * Step 1: Select Image
     */

    label = new Label("Step 1: Select Image", Label.LEFT);
    label.setFont(boldFont);
    label.setBounds(10, top, WINDOW_WIDTH - 20, 14);
    add(label);

    imgField = new TextField();
    imgField.setEditable(false);
    imgField.setText(_imp.getTitle());
    imgField.setBounds(20, top + 25, 260, 24);
    add(imgField);

    button = new Button("Change to current");
    button.setBounds(300, top + 25, 160, 24);
    button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (imp != null) imp.close();
        RoiManager roiMgr = RoiManager.getInstance();
        if (roiMgr != null) { roiMgr.reset(); roiMgr.close(); }

        _imp = IJ.getImage();
        imgField.setText(_imp.getTitle());
      }
    });
    add(button);

    /*
     * Step 2: Binary
     */

    label = new Label("Step 2: Binary Boundry", Label.LEFT);
    label.setFont(boldFont);
    label.setBounds(10, top + 65, WINDOW_WIDTH - 20, 14);
    add(label);

    label = new Label("Fiber Boundry", Label.LEFT);
    label.setBounds(20, top + 95, 90, 14);
    add(label);

    sclBar = new Scrollbar(Scrollbar.HORIZONTAL,
      get_current_ip().getAutoThreshold(), 1, 1, 255);
    sclBar.setBounds(120, top + 95, 100, 14);
    sclBar.addAdjustmentListener(new AdjustmentListener() {
      public void adjustmentValueChanged(AdjustmentEvent e) {
        thdField.setText(Integer.toString(sclBar.getValue()));
        if (imp != null) update_binaryzation();
      }
    });
    add(sclBar);

    thdField = new TextField();
    thdField.setEditable(false);
    thdField.setText(Integer.toString(sclBar.getValue()));
    thdField.setBounds(230, top + 90, 30, 24);
    add(thdField);

    button = new Button("Binary");
    button.setBounds(270, top + 90, 60, 24);
    button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (imp != null) imp.close();
        new_binaryzation();
      }
    });
    add(button);

    button = new Button("Erode");
    button.setBounds(335, top + 90, 60, 24);
    button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (imp == null) new_binaryzation();
        ip.erode();
        imp.updateAndDraw();
      }
    });
    add(button);

    button = new Button("Dilate");
    button.setBounds(400, top + 90, 60, 24);
    button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (imp == null) new_binaryzation();
        ip.dilate();
        imp.updateAndDraw();
      }
    });
    add(button);

    /*
     * Step 3: Analyze Particles
     */

    label = new Label("Step 3: Analyze Particles", Label.LEFT);
    label.setFont(boldFont);
    label.setBounds(10, top + 125, WINDOW_WIDTH - 20, 14);
    add(label);

    button = new Button("Analyze");
    button.setBounds(20, top + 150, 70, 24);
    button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (imp == null) new_binaryzation();
        RoiManager roiMgr = RoiManager.getInstance();
        if (roiMgr != null) { roiMgr.reset(); roiMgr.close(); }
        analyze();
      }
    });
    add(button);

    label = new Label("Please select 'Add to Manager' in the following prompted dialog.", Label.LEFT);
    label.setFont(new Font("Helvetica", Font.PLAIN, 11));
    label.setBounds(100, top + 155, WINDOW_WIDTH - 40, 14);
    add(label);

    label = new Label("Version 2.1.alpha (May 5, 2015)", Label.RIGHT);
    label.setFont(new Font("Helvetica", Font.PLAIN, 11));
    label.setBounds(10, WINDOW_HEIGHT-24, WINDOW_WIDTH - 20, 14);
    add(label);
  }

  /* Overrides windowClosing in PluginFrame. */
  public void windowClosing(WindowEvent e) {
    super.windowClosing(e);

    // Close the imp
    if (imp != null) imp.close();
    ImagePlus.removeImageListener(impLst);

    // For singleton
    instance = null;
  }

  // Get a copy of the current image in Gray8 format
  public ImageProcessor get_current_ip() {
    int oriC = _imp.getC();
    _imp.setC(_imp.getNChannels());
    ImageProcessor ip = _imp.getChannelProcessor().convertToByte(true);
    _imp.setC(oriC);
    return(ip);
  }

  // Create a binaryzation
  public void new_binaryzation() {
    ip = get_current_ip();
    ip.threshold(sclBar.getValue());
    imp = new ImagePlus("Threshold : " + Integer.toString(sclBar.getValue()), ip);
    imp.show();
    imp.updateAndDraw();
  }

  // Update the binaryzation
  public void update_binaryzation() {
    ip = get_current_ip();
    ip.threshold(sclBar.getValue());
    imp.setProcessor(ip);
    imp.setTitle("Threshold : " + Integer.toString(sclBar.getValue()));
  }

  // Analyze Particles
  public void analyze() {
    ParticleAnalyzer pa = new ParticleAnalyzer();
    if (!pa.showDialog()) return;
    pa.analyze(imp, ip);
    imp.close();

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

    roiMgr.runCommand(_imp, "Show All with labels");
  }

}

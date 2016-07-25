import ij.*;
import ij.gui.*;
import ij.process.*;
import ij.measure.*;
import ij.plugin.filter.*;
import ij.plugin.frame.*;
import java.awt.*;
import java.awt.event.*;

/**
 * @version v2.2 (Jul 26, 2016) - provide a utility macro
 *          v2.1 (May  5, 2015) - fix bugs and support fiber type selection
 *          v2.0 (May  2, 2015) - implement a GUI
 *          v1.0 (May  1, 2015) - first implementation of this plugin
 * @author Aidi Stan (aidistan@live.com)
 * @license the MIT license
 */

public class Fiber_Size extends PlugInFrame {

  // Constant
  public final int WINDOW_WIDTH = 480;
  public final int WINDOW_HEIGHT = 400;

  // External
  public ImagePlus orgImp;

  // Internal
  public static PlugInFrame instance; // Singleton
  public ImageListener imgLtn;
  public ImagePlus bndImp;
  public ImageProcessor bndIp;

  // Controls
  public int yPos;
  public TextField imgFld;
  public Scrollbar bndSclBar;
  public TextField bndThdFld;
  public Choice chnChoice;
  public Choice typChoice;
  public Scrollbar typSclBar;
  public TextField typThdFld;

  public Fiber_Size() {
    super("Fiber Size");
    if (instance != null) {
      instance.toFront();
      return;
    } else {
      // Initialize instance variables
      orgImp = IJ.getImage();
      bndImp = null;
      instance = this;

      imgLtn = new ImageListener() {
        public void imageClosed(ImagePlus imp) {
          if (imp == bndImp) bndImp = null;
        }
        public void imageOpened(ImagePlus imp_) {}
        public void imageUpdated(ImagePlus imp_) {}
      };
      ImagePlus.addImageListener(imgLtn);

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

    // Set defaults
    setFont(new Font("Helvetica", Font.PLAIN, 12));
    setLayout(null);
    setResizable(false);

    // Add controls
    yPos = getInsets().top + 15;
    this.add_step1_controls()
        .add_step2_controls()
        .add_step3_controls()
        .add_step4_controls()
        .add_version_info();
  }

  // Step 1: Select Image
  public Fiber_Size add_step1_controls() {
    Label label;
    Button button;

    label = new Label("Step 1: Select Image", Label.LEFT);
    label.setFont(new Font("Helvetica", Font.BOLD, 12));
    label.setBounds(10, yPos, WINDOW_WIDTH - 20, 14);
    add(label);

    imgFld = new TextField();
    imgFld.setEditable(false);
    imgFld.setText(orgImp.getTitle());
    imgFld.setBounds(20, yPos + 25, 270, 24);
    add(imgFld);

    button = new Button("Change to current");
    button.setBounds(300, yPos + 25, 160, 24);
    button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        closeAll();
        orgImp = IJ.getImage();
        imgFld.setText(orgImp.getTitle());
      }
    });
    add(button);

    yPos += 65;
    return(this);
  }

  // Step 2: Binary Fiber Boundary
  public Fiber_Size add_step2_controls() {
    Label label;
    Button button;

    label = new Label("Step 2: Binary Fiber Boundary", Label.LEFT);
    label.setFont(new Font("Helvetica", Font.BOLD, 12));
    label.setBounds(10, yPos, WINDOW_WIDTH - 20, 14);
    add(label);

    label = new Label("Threshold", Label.RIGHT);
    label.setBounds(20, yPos + 30, 70, 14);
    add(label);

    bndSclBar = new Scrollbar(Scrollbar.HORIZONTAL,
      get_ip().getAutoThreshold(), 1, 1, 255);
    bndSclBar.setBounds(100, yPos + 30, 120, 14);
    bndSclBar.addAdjustmentListener(new AdjustmentListener() {
      public void adjustmentValueChanged(AdjustmentEvent e) {
        bndThdFld.setText(Integer.toString(bndSclBar.getValue()));
        if (bndImp != null) update_binaryzation();
      }
    });
    add(bndSclBar);

    bndThdFld = new TextField();
    bndThdFld.setEditable(false);
    bndThdFld.setText(Integer.toString(bndSclBar.getValue()));
    bndThdFld.setBounds(230, yPos + 25, 30, 24);
    add(bndThdFld);

    button = new Button("Binary");
    button.setBounds(270, yPos + 25, 60, 24);
    button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (bndImp != null) bndImp.close();
        new_binaryzation();
      }
    });
    add(button);

    button = new Button("Erode");
    button.setBounds(335, yPos + 25, 60, 24);
    button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (bndImp == null) new_binaryzation();
        bndIp.erode();
        bndImp.updateAndDraw();
      }
    });
    add(button);

    button = new Button("Dilate");
    button.setBounds(400, yPos + 25, 60, 24);
    button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (bndImp == null) new_binaryzation();
        bndIp.dilate();
        bndImp.updateAndDraw();
      }
    });
    add(button);

    yPos += 65;
    return(this);
  }

  // Step 3: Analyze Particles
  public Fiber_Size add_step3_controls() {
    Label label;
    Button button;

    label = new Label("Step 3: Analyze Particles", Label.LEFT);
    label.setFont(new Font("Helvetica", Font.BOLD, 12));
    label.setBounds(10, yPos, WINDOW_WIDTH - 20, 14);
    add(label);

    button = new Button("Analyze");
    button.setBounds(20, yPos + 25, 70, 24);
    button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (bndImp == null) new_binaryzation();
        RoiManager roiMgr = RoiManager.getInstance();
        if (roiMgr != null) { roiMgr.reset(); roiMgr.close(); }
        analyze();
      }
    });
    add(button);

    label = new Label("Please select 'Add to Manager'.", Label.LEFT);
    label.setFont(new Font("Helvetica", Font.PLAIN, 11));
    label.setBounds(100, yPos + 30, WINDOW_WIDTH - 40, 14);
    add(label);

    yPos += 65;
    return(this);
  }

  // Step 4: Select Fiber Type
  public Fiber_Size add_step4_controls() {
    Label label;
    Button button;

    label = new Label("Step 4: Select Fiber Type", Label.LEFT);
    label.setFont(new Font("Helvetica", Font.BOLD, 12));
    label.setBounds(10, yPos, WINDOW_WIDTH - 20, 14);
    add(label);

    label = new Label("Select by", Label.RIGHT);
    label.setBounds(20, yPos + 30, 90, 14);
    add(label);

    chnChoice = new Choice();
    chnChoice.setBounds(115, yPos + 28, 100, 18);
    for (int i = 0; i <= orgImp.getNChannels(); i++) {
      chnChoice.add(i == 0 ? "None" : "Channel " + Integer.toString(i));
    }
    chnChoice.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
          // Check whether we can measure
          RoiManager roiMgr = RoiManager.getInstance();
          if (roiMgr == null) return;
          Roi[] rois = roiMgr.getRoisAsArray();
          if (rois.length == 0) return;

          // Close previous ones
          ResultsTable result = ResultsTable.getResultsTable();
          if (result != null) result.reset();
          ij.text.TextWindow window = ResultsTable.getResultsWindow();
          if (window != null) window.close();

          if (chnChoice.getSelectedIndex() > 0)
            measure();
        }
      }
    });
    add(chnChoice);

    label = new Label("Threshold by", Label.RIGHT);
    label.setBounds(20, yPos + 60, 90, 14);
    add(label);

    typChoice = new Choice();
    typChoice.add("Mode");
    typChoice.add("Mean");
    typChoice.setBounds(115, yPos + 58, 60, 18);
    typChoice.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) update_typSclBar();
      }
    });
    add(typChoice);

    label = new Label("at", Label.CENTER);
    label.setBounds(180, yPos + 60, 20, 14);
    add(label);

    typSclBar = new Scrollbar(Scrollbar.HORIZONTAL, 0, 1, 1, 255);
    typSclBar.setBounds(205, yPos + 60, 145, 14);
    typSclBar.addAdjustmentListener(new AdjustmentListener() {
      public void adjustmentValueChanged(AdjustmentEvent e) {
        typThdFld.setText(Integer.toString(typSclBar.getValue()));
        select_by_type("select");
      }
    });
    add(typSclBar);

    typThdFld = new TextField();
    typThdFld.setEditable(false);
    typThdFld.setText(Integer.toString(typSclBar.getValue()));
    typThdFld.setBounds(360, yPos + 55, 30, 24);
    add(typThdFld);

    button = new Button("Select");
    button.setBounds(20, yPos + 85, 70, 24);
    button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) { select_by_type("select"); }
    });
    add(button);

    button = new Button("Reset");
    button.setBounds(100, yPos + 85, 70, 24);
    button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) { select_by_type("reset"); }
    });
    add(button);

    label = new Label("Please reset before changing the channel.", Label.LEFT);
    label.setFont(new Font("Helvetica", Font.PLAIN, 11));
    label.setBounds(180, yPos + 90, WINDOW_WIDTH - 40, 14);
    add(label);

    yPos += 125;
    return(this);
  }

  // Version info
  public Fiber_Size add_version_info() {
    Label label;

    label = new Label("Version 2.2 (Jul 26, 2016)", Label.RIGHT);
    label.setFont(new Font("Helvetica", Font.PLAIN, 11));
    label.setBounds(10, WINDOW_HEIGHT-24, WINDOW_WIDTH - 20, 14);
    add(label);

    return(this);
  }

  // Overrides windowClosing in PluginFrame
  public void windowClosing(WindowEvent e) {
    super.windowClosing(e);
    closeAll();
    ImagePlus.removeImageListener(imgLtn);
    instance = null;
  }

  // Close all windows
  public void closeAll() {
    if (bndImp != null) bndImp.close();
    RoiManager roiMgr = RoiManager.getInstance();
    if (roiMgr != null) { roiMgr.reset(); roiMgr.close(); }
    ResultsTable result = ResultsTable.getResultsTable();
    if (result != null) result.reset();
    ij.text.TextWindow window = ResultsTable.getResultsWindow();
    if (window != null) window.close();
  }

  // Get a copy of the working ImageProcessor
  public ImageProcessor get_ip() {
    return(get_ip(orgImp.getNChannels()));
  }
  public ImageProcessor get_ip(int c) {
    int _c = orgImp.getC();
    orgImp.setC(c);
    ImageProcessor ip = orgImp.getChannelProcessor().convertToByte(true);
    orgImp.setC(_c);
    return(ip);
  }

  // Create a binaryzation
  public void new_binaryzation() {
    bndIp = get_ip();
    bndIp.threshold(bndSclBar.getValue());
    bndImp = new ImagePlus("Threshold : " + Integer.toString(bndSclBar.getValue()), bndIp);
    bndImp.show();
    bndImp.updateAndDraw();
  }

  // Update the binaryzation
  public void update_binaryzation() {
    bndIp = get_ip();
    bndIp.threshold(bndSclBar.getValue());
    bndImp.setProcessor(bndIp);
    bndImp.setTitle("Threshold : " + Integer.toString(bndSclBar.getValue()));
  }

  // Analyze Particles
  public void analyze() {
    ParticleAnalyzer pa = new ParticleAnalyzer();
    if (!pa.showDialog()) return;
    pa.analyze(bndImp, bndIp);
    bndImp.close();

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

    roiMgr.runCommand(orgImp, "Show All with labels");
  }

  // Measure Rois
  public void measure() {
    ImagePlus imp = new ImagePlus(null, get_ip(chnChoice.getSelectedIndex()));
    RoiManager roiMgr = RoiManager.getInstance();
    roiMgr.runCommand(imp, "Deselect");
    IJ.run("Set Measurements...", "area mean modal redirect=None decimal=1");
    roiMgr.runCommand(imp, "Measure");
    update_typSclBar();
  }

  // Update typSclBar
  public void update_typSclBar() {
    ResultsTable result = ResultsTable.getResultsTable();
    if (result == null) return;

    double[] vals;
    switch (typChoice.getSelectedIndex()) {
      case 0: vals = result.getColumnAsDoubles(ResultsTable.MODE); break;
      case 1: vals = result.getColumnAsDoubles(ResultsTable.MEAN); break;
      default: vals = null; break;
    }
    if (vals == null) return;

    typSclBar.setMinimum((int)Math.floor(vals[0]));
    typSclBar.setMaximum((int)Math.floor(vals[0]) + 1);
    for (int i = 1; i < vals.length; i++) {
      if(vals[i] < typSclBar.getMinimum())
        typSclBar.setMinimum((int)Math.floor(vals[i]));
      if(vals[i] > typSclBar.getMaximum())
        typSclBar.setMaximum((int)Math.floor(vals[i]) + 1);
    }
    typSclBar.setValue((int)(typSclBar.getMinimum() + typSclBar.getMaximum()) / 2);
    typThdFld.setText(Integer.toString(typSclBar.getValue()));
    select_by_type("select");
  }

  // Selct Rois by fiber type
  public Roi[] allRois;
  public void select_by_type(String param) {
    RoiManager roiMgr = RoiManager.getInstance();
    if (roiMgr == null) return;
    ResultsTable result = ResultsTable.getResultsTable();
    if (result == null) return;

    double[] vals;
    switch (typChoice.getSelectedIndex()) {
      case 0: vals = result.getColumnAsDoubles(ResultsTable.MODE); break;
      case 1: vals = result.getColumnAsDoubles(ResultsTable.MEAN); break;
      default: vals = null; break;
    }
    if (vals == null) return;
    int threshold = Integer.parseInt(typThdFld.getText());

    Roi[] rois = roiMgr.getRoisAsArray();
    if (rois.length == vals.length)
      allRois = rois;
    else
      rois = allRois;

    // It begins!
    roiMgr.reset();
    for(int i = 0; i < rois.length; i++) {
      Roi roi = rois[i];
      if (vals[i] > threshold)
        roiMgr.addRoi(roi);
      else if (param.equals("reset"))
        roiMgr.addRoi(roi);
    }

    if (param.equals("select"))
      roiMgr.runCommand(orgImp, "Show All without labels");
    else if (param.equals("reset"))
      roiMgr.runCommand(orgImp, "Show All with labels");
  }

}

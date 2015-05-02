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
  public final int WINDOW_HEIGHT = 280;

  // Internal
  public static PlugInFrame instance; // Singleton
  public ImagePlus inspectation;
  public ParticleAnalyzer pa;

  // Controls
  public Scrollbar thresholdBar;
  public TextField erodeField;
  public Checkbox inspectationChkbox;

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
      instance = this;
      thresholdBar.setValue(get_current_ip().getAutoThreshold());
      inspectation = null;
      pa = new ParticleAnalyzer();
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

    label = new Label("Binaryzation", Label.LEFT);
    label.setFont(new Font("Helvetica", Font.BOLD, 12));
    label.setBounds(10, top, WINDOW_WIDTH - 20, 14);
    add(label);

    label = new Label("Threshold", Label.LEFT);
    label.setFont(new Font("Helvetica", Font.PLAIN, 12));
    label.setBounds(20, top + 30, 70, 14);
    add(label);

    thresholdBar = new Scrollbar(Scrollbar.HORIZONTAL, 0, 1, 0, 255);
    thresholdBar.setBounds(100, top + 28, WINDOW_WIDTH - 120, 16);
    thresholdBar.addAdjustmentListener(new AdjustmentListener() {
      public void adjustmentValueChanged(AdjustmentEvent e) {
        if (inspectation == null) return;
        if (e.getValueIsAdjusting()) return;
        inspectation.close();
        inspectation = new ImagePlus(get_binary_title(), get_binary_ip());
        inspectation.show();
        inspectation.updateAndDraw();
      }
    });
    add(thresholdBar);

    label = new Label("Erode Iteration", Label.LEFT);
    label.setFont(new Font("Helvetica", Font.PLAIN, 12));
    label.setBounds(20, top + 60, 100, 14);
    add(label);

    erodeField = new TextField("0", 3);
    erodeField.setFont(new Font("Helvetica", Font.PLAIN, 12));
    erodeField.setBounds(130, top + 60, 30, 14);
    add(erodeField);

    inspectationChkbox = new Checkbox("Inspectation", false);
    inspectationChkbox.setFont(new Font("Helvetica", Font.PLAIN, 12));
    inspectationChkbox.setBounds(170, top + 60, WINDOW_WIDTH - 180, 14);
    inspectationChkbox.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED ) {
          inspectation = new ImagePlus(get_binary_title(), get_binary_ip());
          inspectation.show();
          inspectation.updateAndDraw();
          inspectation.addImageListener(new ImageListener() {
            public void imageClosed(ImagePlus imp) {
              inspectationChkbox.setState(false);
              inspectation = null;
            }
            public void imageOpened(ImagePlus imp) {}
            public void imageUpdated(ImagePlus imp) {}
          });
        } else {
          inspectation.close();
          inspectation = null;
        }
      }
    });
    add(inspectationChkbox);

    /*
     * Detection
     */

    label = new Label("Detection", Label.LEFT);
    label.setFont(new Font("Helvetica", Font.BOLD, 12));
    label.setBounds(10, top + 100, WINDOW_WIDTH - 20, 14);
    add(label);

    button = new Button("Particles Analysis Parameter");
    button.setFont(new Font("Helvetica", Font.PLAIN, 12));
    button.setBounds(30, top + 125, WINDOW_WIDTH - 60, 24);
    button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) { pa.showDialog(); }
    });
    add(button);

    label = new Label("(Please select 'Add to Manager')", Label.CENTER);
    label.setFont(new Font("Helvetica", Font.PLAIN, 12));
    label.setBounds(10, top + 155, WINDOW_WIDTH - 20, 14);
    add(label);

    button = new Button("START");
    button.setFont(new Font("Helvetica", Font.BOLD, 14));
    button.setBounds(20, WINDOW_HEIGHT-35, 100, 24);
    add(button);

    label = new Label("Version 2.0 (May 2, 2015)", Label.LEFT);
    label.setFont(new Font("Helvetica", Font.PLAIN, 11));
    label.setBounds(140, WINDOW_HEIGHT-30, WINDOW_WIDTH - 20, 14);
    add(label);

    setLayout(null);
    setResizable(false);
  }

  /* Overrides windowClosing in PluginFrame. */
  public void windowClosing(WindowEvent e) {
      super.windowClosing(e);
      instance = null;
  }

  // Get a copy of the current IP in Gray8 format
  public ImageProcessor get_current_ip() {
    ImagePlus imp = IJ.getImage();
    int oriC = imp.getC();
    imp.setC(imp.getNChannels());
    ImageProcessor ip = imp.getChannelProcessor();
    ImageProcessor ip_ = ip.convertToByte(true);
    imp.setC(oriC);
    return(ip_);
  }

  // Get the title of a binary IP
  public String get_binary_title() {
    return(
      "Inspectation (" +
      "threshold " + Integer.toString(thresholdBar.getValue()) +
      ", erode " + Integer.toString(Integer.parseInt(erodeField.getText())) +
      ")"
    );
  }

  // Get a binary IP (maybe for inspectation)
  public ImageProcessor get_binary_ip() {
    ImageProcessor ip = get_current_ip();
    ip.threshold(thresholdBar.getValue());
    return(ip);
  }
}

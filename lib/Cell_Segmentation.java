import ij.*;
import ij.gui.*;
import ij.process.*;
import ij.plugin.frame.*;
import ij.plugin.filter.*;
import java.awt.*;

public class Cell_Segmentation implements PlugInFilter {

  // Constant
  public static final Boolean showProcedure = false;

  // External
  public ImagePlus      _imp; // Original ImagePlus
  public ImageProcessor _ip;  // Original ImageProcessor

  // Internal
  public ImagePlus      imp;
  public ImageProcessor ip;
  public Roi[]          rois;

  public int setup(String arg, ImagePlus _imp) {
    this._imp = _imp;
    return DOES_ALL;
  }

  public void run(ImageProcessor _ip) {
    this._ip = _ip;

    // Create a copy in Gray8 format
    create_gray8_copy();
    if (showProcedure) get_snapshot("1. Gray8 Format");

    // Auto threshold
    ip.autoThreshold();
    if (showProcedure) get_snapshot("2. Auto Threshold");

    // Analyze particles
    analyze_particles();
  }

  public void create_gray8_copy() {
    // Copy the last channel of the original image,
    // since it's the skeleton in Liu's demo image
    int oriC = _imp.getC();
    _imp.setC(_imp.getNChannels());
    ip = _ip.convertToByte(true);
    _imp.setC(oriC);

    // Create a new ImagePlus to display
    imp = new ImagePlus("Cell Segmentation", ip);
    imp.show();
    imp.updateAndDraw();
  }

  public void analyze_particles() {
    // Get ParticleAnalyzer to work
    ParticleAnalyzer pa = new ParticleAnalyzer();
    pa.showDialog();
    pa.analyze(imp, ip);

    // Get rois out
    RoiManager roiMgr = RoiManager.getInstance();
    rois = roiMgr.getRoisAsArray();
    roiMgr.reset();
    roiMgr.close();
    imp.close();

    // Turn our attention back
    roiMgr = new RoiManager();
    for(int i = 0; i < rois.length; i++) {
      Roi roi = rois[i];
      roi = new PolygonRoi(roi.getInterpolatedPolygon(10, false), Roi.POLYGON);
      roi.setName(Integer.toString(i+1));
      roiMgr.addRoi(roi);
    }
  }

  public void get_snapshot(String title) {
    ImageProcessor ip = this.ip.duplicate();
    ImagePlus imp = new ImagePlus(title, ip);
    imp.show();
    imp.updateAndDraw();
  }

}

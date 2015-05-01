import ij.*;
import ij.gui.*;
import ij.process.*;
import ij.plugin.frame.*;
import ij.plugin.filter.*;
import java.awt.*;

public class Cell_Segmentation implements PlugInFilter {

  // External
  public ImagePlus  imp;
  public RoiManager roiMgr;

  public int setup(String arg, ImagePlus imp) {
    this.imp = imp;

    // Open ROI Manager
    roiMgr = RoiManager.getInstance();
    if (roiMgr == null)
      roiMgr = new RoiManager();

    return DOES_ALL;
  }

  public void run(ImageProcessor ip) {
    // Put this line to validate
    ip.invert();
  }

}

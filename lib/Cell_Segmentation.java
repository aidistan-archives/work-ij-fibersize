import ij.*;
import ij.gui.*;
import ij.process.*;
import ij.plugin.frame.*;
import ij.plugin.filter.*;
import java.awt.*;

public class Cell_Segmentation implements PlugInFilter {

  // External
  public RoiManager     roiMgr;
  public ImagePlus      imp;
  public ImageProcessor ip;

  public int setup(String arg, ImagePlus imp) {
    // Open ROI Manager
    roiMgr = RoiManager.getInstance();
    if (roiMgr == null)
    roiMgr = new RoiManager();

    return DOES_ALL;
  }

  public void run(ImageProcessor ip) {
    // Create a copy in Gray8 format
    create_gray8_copy(ip);
  }

  public void create_gray8_copy(ImageProcessor _ip) {
    ip = _ip.convertToByte(true);
    imp = new ImagePlus("Cell Segmentation", ip);
    imp.show();
    imp.updateAndDraw();
  }

}

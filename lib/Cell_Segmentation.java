import ij.*;
import ij.gui.*;
import ij.process.*;
import ij.plugin.frame.*;
import ij.plugin.filter.*;
import java.awt.*;

public class Cell_Segmentation implements PlugInFilter {

  // External
  public RoiManager     roiMgr;
  public ImagePlus      _imp;
  public ImageProcessor _ip;

  // Internal
  public ImagePlus      imp;
  public ImageProcessor ip;

  public int setup(String arg, ImagePlus _imp) {
    // Open ROI Manager
    roiMgr = RoiManager.getInstance();
    if (roiMgr == null)
    roiMgr = new RoiManager();

    // Save original ImagePlus
    this._imp = _imp;

    return DOES_ALL;
  }

  public void run(ImageProcessor _ip) {
    // Save original ImageProcessor
    this._ip = _ip;

    // Create a copy in Gray8 format
    create_gray8_copy();
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

}

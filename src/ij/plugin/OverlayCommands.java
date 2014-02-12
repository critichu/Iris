package ij.plugin;
import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.frame.RoiManager;
import ij.macro.Interpreter;
import ij.io.RoiDecoder;
import ij.plugin.filter.PlugInFilter;
import java.awt.*;

/** This plugin implements the commands in the Image/Overlay menu. */
public class OverlayCommands implements PlugIn {
	private static int opacity = 100;
	private static Roi defaultRoi;
	
	static {
		defaultRoi = new Roi(0, 0, 1, 1);
		defaultRoi.setStrokeColor(Roi.getColor());
	}

	public void run(String arg) {
		if (arg.equals("add"))
			addSelection();
		else if (arg.equals("image"))
			addImage(false);
		else if (arg.equals("image-roi"))
			addImage(true);
		else if (arg.equals("flatten"))
			flatten();
		else if (arg.equals("hide"))
			hide();
		else if (arg.equals("show"))
			show();
		else if (arg.equals("remove"))
			remove();
		else if (arg.equals("from"))
			fromRoiManager();
		else if (arg.equals("to"))
			toRoiManager();
		else if (arg.equals("options"))
			options();
	}
			
	void addSelection() {
		ImagePlus imp = IJ.getImage();
		String macroOptions = Macro.getOptions();
		if (macroOptions!=null && IJ.macroRunning() && macroOptions.indexOf("remove")!=-1) {
			imp.setOverlay(null);
			return;
		}
		Roi roi = imp.getRoi();
		if (roi==null && imp.getOverlay()!=null) {
			GenericDialog gd = new GenericDialog("No Selection");
			gd.addMessage("\"Overlay>Add\" requires a selection.");
			gd.setInsets(15, 40, 0);
			gd.addCheckbox("Remove existing overlay", false);
			gd.showDialog();
			if (gd.wasCanceled()) return;
			if (gd.getNextBoolean()) imp.setOverlay(null);
			return;
 		}
		if (roi==null) {
			IJ.error("This command requires a selection.");
			return;
		}
		roi = (Roi)roi.clone();
		Overlay overlay = imp.getOverlay();
		if (!roi.isDrawingTool()) {
			if (roi.getStroke()==null)
				roi.setStrokeWidth(defaultRoi.getStrokeWidth());
			if (roi.getStrokeColor()==null || Line.getWidth()>1&&defaultRoi.getStrokeColor()!=null)
				roi.setStrokeColor(defaultRoi.getStrokeColor());
			if (roi.getFillColor()==null)
				roi.setFillColor(defaultRoi.getFillColor());
		}
		boolean setPos = defaultRoi.getPosition()!=0;
		if (setPos && imp.getStackSize()>1) {
			if (imp.isHyperStack()||imp.isComposite()) {
				if (imp.getNSlices()>1)
					roi.setPosition(0, imp.getSlice(), 0);
				else if (imp.getNFrames()>1)
					roi.setPosition(0, 0, imp.getFrame());
			} else
				roi.setPosition(imp.getCurrentSlice());
		}
		//int width = Line.getWidth();
		//Rectangle bounds = roi.getBounds();
		//boolean tooWide = width>Math.max(bounds.width, bounds.height)/3.0;
		//if (roi.getStroke()==null && width>1 && !tooWide)
		//	roi.setStrokeWidth(Line.getWidth());
		//if (roi.getStrokeColor()==null)
		//	roi.setStrokeColor(Toolbar.getForegroundColor());
		boolean points = roi instanceof PointRoi && ((PolygonRoi)roi).getNCoordinates()>1;
		//if (points) roi.setStrokeColor(Color.red);
		if (IJ.altKeyDown() || (IJ.macroRunning() && Macro.getOptions()!=null)) {
			RoiProperties rp = new RoiProperties("Add to Overlay", roi);
			if (!rp.showDialog()) return;
		}
		String name = roi.getName();
		boolean newOverlay = name!=null && name.equals("new-overlay");
		if (overlay==null || newOverlay)
			overlay = OverlayLabels.createOverlay();
		overlay.add(roi);
		if (!roi.isDrawingTool()) {
			double dsw = defaultRoi.getStrokeWidth();
			defaultRoi = (Roi)roi.clone();
			if (roi.isLine())
				defaultRoi.setStrokeWidth(dsw);
		}
		defaultRoi.setPosition(setPos?1:0);
		imp.setOverlay(overlay);
		if (points || (roi instanceof ImageRoi) || (roi instanceof Arrow))
			imp.deleteRoi();
		Undo.setup(Undo.OVERLAY_ADDITION, imp);
	}
	
	void addImage(boolean createImageRoi) {
		ImagePlus imp = IJ.getImage();
		int[] wList = WindowManager.getIDList();
		if (wList==null || wList.length<2) {
			IJ.error("Add Image...", "The command requires at least two open images.");
			return;
		}
		String[] titles = new String[wList.length];
		for (int i=0; i<wList.length; i++) {
			ImagePlus imp2 = WindowManager.getImage(wList[i]);
			titles[i] = imp2!=null?imp2.getTitle():"";
		}
		int x=0, y=0;
		Roi roi = imp.getRoi();
		if (roi!=null && roi.isArea()) {
			Rectangle r = roi.getBounds();
			x = r.x; y = r.y;
		}
		int index = 0;
		if (wList.length==2) {
			ImagePlus i1 = WindowManager.getImage(wList[0]);
			ImagePlus i2 = WindowManager.getImage(wList[1]);
			if (i2.getWidth()<i1.getWidth() && i2.getHeight()<i1.getHeight())
				index = 1;
		} else if (imp.getID()==wList[0])
			index = 1;

		String title = createImageRoi?"Create Image ROI":"Add Image...";
		GenericDialog gd = new GenericDialog(title);
		if (createImageRoi)
			gd.addChoice("Image:", titles, titles[index]);
		else {
			gd.addChoice("Image to add:", titles, titles[index]);
			gd.addNumericField("X location:", x, 0);
			gd.addNumericField("Y location:", y, 0);
		}
		gd.addNumericField("Opacity (0-100%):", opacity, 0);
		//gd.addCheckbox("Create image selection", createImageRoi);
		gd.showDialog();
		if (gd.wasCanceled())
			return;
		index = gd.getNextChoiceIndex();
		if (!createImageRoi) {
			x = (int)gd.getNextNumber();
			y = (int)gd.getNextNumber();
		}
		opacity = (int)gd.getNextNumber();
		//createImageRoi = gd.getNextBoolean();
		ImagePlus overlay = WindowManager.getImage(wList[index]);
		if (wList.length==2) {
			ImagePlus i1 = WindowManager.getImage(wList[0]);
			ImagePlus i2 = WindowManager.getImage(wList[1]);
			if (i2.getWidth()<i1.getWidth() && i2.getHeight()<i1.getHeight()) {
				imp = i1;
				overlay = i2;
			}
		}
		if (overlay==imp) {
			IJ.error("Add Image...", "Image to be added cannot be the same as\n\""+imp.getTitle()+"\".");
			return;
		}
		if (overlay.getWidth()>imp.getWidth() && overlay.getHeight()>imp.getHeight()) {
			IJ.error("Add Image...", "Image to be added cannnot be larger than\n\""+imp.getTitle()+"\".");
			return;
		}
		if (createImageRoi && x==0 && y==0) {
			x = imp.getWidth()/2-overlay.getWidth()/2;
			y = imp.getHeight()/2-overlay.getHeight()/2;
		}	
		roi = new ImageRoi(x, y, overlay.getProcessor());
		roi.setName(overlay.getShortTitle());
		if (opacity!=100) ((ImageRoi)roi).setOpacity(opacity/100.0);
		if (createImageRoi)
			imp.setRoi(roi);
		else {
			Overlay overlayList = imp.getOverlay();
			if (overlayList==null) overlayList = new Overlay();
			overlayList.add(roi);
			imp.setOverlay(overlayList);
			Undo.setup(Undo.OVERLAY_ADDITION, imp);
		}
	}

	void hide() {
		ImagePlus imp = IJ.getImage();
		imp.setHideOverlay(true);
		RoiManager rm = RoiManager.getInstance();
		if (rm!=null) rm.runCommand("show none");
	}

	void show() {
		ImagePlus imp = IJ.getImage();
		imp.setHideOverlay(false);
		if (imp.getOverlay()==null) {
			RoiManager rm = RoiManager.getInstance();
			if (rm!=null && rm.getCount()>1) {
				if (!IJ.isMacro()) rm.toFront();
				rm.runCommand("show all with labels");
			}
		}
	}

	void remove() {
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp!=null) {
			ImageCanvas ic = imp.getCanvas();
			if (ic!=null)
				ic.setShowAllList(null);
			imp.setOverlay(null);
		}
	}

	void flatten() {
		ImagePlus imp = IJ.getImage();
		int flags = imp.isComposite()?0:IJ.setupDialog(imp, 0);
		if (flags==PlugInFilter.DONE)
			return;
		else if (flags==PlugInFilter.DOES_STACKS)
			flattenStack(imp);
		else {
			ImagePlus imp2 = imp.flatten();
			imp2.setTitle(WindowManager.getUniqueName(imp.getTitle()));
			imp2.show();
		}
	}
	
	void flattenStack(ImagePlus imp) {
		Overlay overlay = imp.getOverlay();
		boolean roiManagerOverlay = false;
		boolean roiManagerShowAllMode = false;
		if (overlay==null) {
			ImageCanvas ic = imp.getCanvas();
			if (ic!=null) {
				overlay = ic.getShowAllList();
				if (overlay!=null) {
					roiManagerOverlay = true;
					roiManagerShowAllMode = !Prefs.showAllSliceOnly;
				}
			}
		}
		if (overlay==null || !IJ.isJava16() || imp.getBitDepth()!=24) {
			IJ.error("Flatten Stack", "A stack in RGB format, an overlay and\nJava 1.6 are required to flatten a stack.");
			return;
		}
		ImageStack stack = imp.getStack();
		int stackSize = stack.getSize();
		for (int img=1; img<=stack.getSize(); img++) {
			ImageProcessor ip = stack.getProcessor(img);
			ImagePlus imp2 = new ImagePlus("temp", ip);
			Overlay overlay2 = overlay.duplicate();
			for (int i=overlay2.size()-1; i>=0; i--) {
				Roi roi = overlay2.get(i);
				int position = roi.getPosition();
				if (position==0 || position==img || roiManagerShowAllMode)
					roi.setPosition(0);
				else
					roi.setPosition(stackSize+1);
			}
			if (overlay2.size()>0) {
				imp2.setOverlay(overlay2);
				ImagePlus imp3 = imp2.flatten();
				stack.setPixels(imp3.getProcessor().getPixels(),img);
			}
		}
		imp.setStack(stack);
		imp.setOverlay(null);
		if (roiManagerOverlay) {
			RoiManager rm = RoiManager.getInstance();
			if (rm!=null)
				rm.runCommand("show none");
		}
	}

	void fromRoiManager() {
		ImagePlus imp = IJ.getImage();
		RoiManager rm = RoiManager.getInstance2();
		if (rm==null) {
			IJ.error("ROI Manager is not open");
			return;
		}
		Roi[] rois = rm.getRoisAsArray();
		if (rois.length==0) {
			IJ.error("ROI Manager is empty");
			return;
		}
		rm.moveRoisToOverlay(imp);
		imp.deleteRoi();
	}
	
	void toRoiManager() {
		ImagePlus imp = IJ.getImage();
		Overlay overlay = imp.getOverlay();
		if (overlay==null) {
			IJ.error("Overlay required");
			return;
		}
		RoiManager rm = RoiManager.getInstance();
		if (rm==null) {
			if (Macro.getOptions()!=null && Interpreter.isBatchMode())
				rm = Interpreter.getBatchModeRoiManager();
			if (rm==null) {
				Frame frame = WindowManager.getFrame("ROI Manager");
				if (frame==null)
					IJ.run("ROI Manager...");
				frame = WindowManager.getFrame("ROI Manager");
				if (frame==null || !(frame instanceof RoiManager))
					return;
				rm = (RoiManager)frame;
			}
		}
		if (overlay.size()>=4 && overlay.get(3).getPosition()!=0)
			Prefs.showAllSliceOnly = true;
		rm.runCommand("reset");
		rm.setEditMode(imp, false);
		for (int i=0; i<overlay.size(); i++)
			rm.add(imp, overlay.get(i), i);
		rm.setEditMode(imp, true);
		rm.runCommand("show all");
		imp.setOverlay(null);
	}
	
	void options() {
		ImagePlus imp = WindowManager.getCurrentImage();
		Overlay overlay = null;
		Roi roi = null;
		if (imp!=null) {
			overlay = imp.getOverlay();
			roi = imp.getRoi();
			if (roi!=null)
				roi = (Roi)roi.clone();
		}
		if (roi==null)
			roi = defaultRoi;
		if (roi==null) {
			int size = imp!=null?imp.getWidth():512;
			roi = new Roi(0, 0, size/4, size/4);
		}
		if (!roi.isDrawingTool()) {
			if (roi.getStroke()==null)
				roi.setStrokeWidth(defaultRoi.getStrokeWidth());
			if (roi.getStrokeColor()==null || Line.getWidth()>1&&defaultRoi.getStrokeColor()!=null)
				roi.setStrokeColor(defaultRoi.getStrokeColor());
			if (roi.getFillColor()==null)
				roi.setFillColor(defaultRoi.getFillColor());
		}
		//int width = Line.getWidth();
		//Rectangle bounds = roi.getBounds();
		//boolean tooWide = width>Math.max(bounds.width, bounds.height)/3.0;
		//if (roi.getStroke()==null && width>1 && !tooWide)
		//	roi.setStrokeWidth(Line.getWidth());
		if (roi.getStrokeColor()==null)
			roi.setStrokeColor(Roi.getColor());
		boolean points = roi instanceof PointRoi && ((PolygonRoi)roi).getNCoordinates()>1;
		if (points) roi.setStrokeColor(Color.red);
		roi.setPosition(defaultRoi.getPosition());
		RoiProperties rp = new RoiProperties("Overlay Options", roi);
		if (!rp.showDialog()) return;
		defaultRoi = roi;
	}
	
}

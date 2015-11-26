package fiji.threshold;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Undo;
import ij.gui.GenericDialog;
import ij.gui.NewImage;
import ij.gui.OvalRoi;
import ij.gui.Roi;
import ij.gui.YesNoCancelDialog;
import ij.plugin.CanvasResizer;
import ij.plugin.ContrastEnhancer;
import ij.plugin.MontageMaker;
import ij.plugin.PlugIn;
import ij.plugin.filter.RankFilters;
import ij.process.Blitter;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;

// AutoLocalThreshold segmentation 
// Following the guidelines at http://pacific.mpi-cbg.de/wiki/index.php/PlugIn_Design_Guidelines
// ImageJ plugin by G. Landini at bham. ac. uk
// 1.0  15/Apr/2009
// 1.1  01/Jun/2009
// 1.2  25/May/2010
// 1.3  1/Nov/2011 added constant offset to Niblack's method (request)
// 1.4  2/Nov/2011 Niblack's new constant should be subtracted to match mean,mode and midgrey methods. Midgrey method had the wrong constant sign.
// 1.5  18/Nov/2013 added 3 new local thresholding methdos: Constrast, Otsu and Phansalkar
                
public class Auto_Local_Threshold implements PlugIn {
        /** Ask for parameters and then execute.*/
        public void run(String arg) {
		// 1 - Obtain the currently active image:
		ImagePlus imp = IJ.getImage();

		if (null == imp){
			IJ.showMessage("There must be at least one image open");
			return;
		}

		if (imp.getBitDepth()!=8) {
			IJ.showMessage("Error", "Only 8-bit images are supported");
			return;
		}

		 // 2 - Ask for parameters:
		GenericDialog gd = new GenericDialog("Auto Local Threshold");
		String [] methods={"Try all", "Bernsen", "Contrast", "Mean", "Median", "MidGrey", "Niblack","Otsu", "Phansalkar", "Sauvola"};
		gd.addMessage("Auto Local Threshold v1.5");
		gd.addChoice("Method", methods, methods[0]);
		gd.addNumericField ("Radius",  15, 0);
		gd.addMessage ("Special paramters (if different from default)");
		gd.addNumericField ("Parameter_1",  0, 0);
		gd.addNumericField ("Parameter_2",  0, 0);
		gd.addCheckbox("White objects on black background",true);
		if (imp.getStackSize()>1) {
			gd.addCheckbox("Stack",false);
		}
		gd.addMessage("Thresholded result is always shown in white [255].");
		gd.showDialog();
		if (gd.wasCanceled()) return;
 
		// 3 - Retrieve parameters from the dialog
		String myMethod= gd.getNextChoice ();
		int radius = (int) gd.getNextNumber();
		double par1 = (double) gd.getNextNumber();
		double par2 = (double) gd.getNextNumber();
		boolean doIwhite = gd.getNextBoolean ();
		boolean doIstack=false; 

		int stackSize = imp.getStackSize();
		if (stackSize>1)
			doIstack = gd.getNextBoolean ();

		// 4 - Execute!
		//long start = System.currentTimeMillis();
		if(myMethod.equals("Try all")){
			ImageProcessor ip = imp.getProcessor();
			int xe = ip.getWidth();
			int ye = ip.getHeight();
			int ml = methods.length;
			ImagePlus imp2, imp3;
			ImageStack tstack=null, stackNew;
			if (stackSize>1 && doIstack){
				boolean doItAnyway = true;
				if (stackSize>25) {
					YesNoCancelDialog d = new YesNoCancelDialog(IJ.getInstance(),"Auto Local Threshold", "You might run out of memory.\n \nDisplay "+stackSize+" slices?\n \n \'No\' will process without display and\noutput results to the log window.");
					if (!d.yesPressed()){
//						doIlog=true; //will show in the log window
						doItAnyway=false;
					}
					if (d.cancelPressed())
						return;
				}

				for (int j=1; j<=stackSize; j++){
					imp.setSlice(j);
					ip = imp.getProcessor();
					tstack= new ImageStack(xe,ye);
					for (int k=1; k<ml;k++)
						tstack.addSlice(methods[k], ip.duplicate());
					imp2 = new ImagePlus("Auto Threshold", tstack);
					imp2.updateAndDraw();

					for (int k=1; k<ml;k++){
						imp2.setSlice(k);
						Object[] result = exec(imp2, methods[k], radius, par1, par2, doIwhite );
					 }
					//if (doItAnyway){
					CanvasResizer cr= new CanvasResizer();
					stackNew = cr.expandStack(tstack, (xe+2), (ye+18), 1, 1);
					imp3 = new ImagePlus("Auto Threshold", stackNew);
					imp3.updateAndDraw();
					MontageMaker mm= new MontageMaker();
					mm.makeMontage( imp3, 3, 3, 1.0, 1, (ml-1), 1, 0, true); // 3 columns and 3 rows
				}
				imp.setSlice(1);
				//if (doItAnyway)
				IJ.run("Images to Stack", "method=[Copy (center)] title=Montage");
				return;
			}
			else { //single image try all
				 tstack= new ImageStack(xe,ye);
				for (int k=1; k<ml;k++)
					tstack.addSlice(methods[k], ip.duplicate());
				imp2 = new ImagePlus("Auto Threshold", tstack);
				imp2.updateAndDraw();

				for (int k=1; k<ml;k++){
					imp2.setSlice(k);
					//IJ.log("analyzing slice with "+methods[k]);
					Object[] result = exec(imp2, methods[k], radius, par1, par2, doIwhite );
				}
				//imp2.setSlice(1);
				CanvasResizer cr= new CanvasResizer();
				stackNew = cr.expandStack(tstack, (xe+2), (ye+18), 1, 1);
				imp3 = new ImagePlus("Auto Threshold", stackNew);
				imp3.updateAndDraw();
				MontageMaker mm= new MontageMaker();
				mm.makeMontage( imp3, 3, 3, 1.0, 1, (ml-1), 1, 0, true);
				return;
			}
		}
		else { // selected a method
			if (stackSize>1 &&  doIstack ) { //whole stack
//				if (doIstackHistogram) {// one global histogram
//					Object[] result = exec(imp, myMethod, noWhite, noBlack, doIwhite, doIset, doIlog, doIstackHistogram );
//				}
//				else{ // slice by slice
					for (int k=1; k<=stackSize; k++){
						imp.setSlice(k);
						Object[] result = exec(imp, myMethod, radius, par1, par2, doIwhite );
					}
//				}
				imp.setSlice(1);
			}
			else { //just one slice
				Object[] result = exec(imp, myMethod, radius, par1, par2, doIwhite );
			}
			// 5 - If all went well, show the image:
			// not needed here as the source image is binarised 
		}
	}
	//IJ.showStatus(IJ.d2s((System.currentTimeMillis()-start)/1000.0, 2)+" seconds");


	/** Execute the plugin functionality: duplicate and scale the given image.
	* @return an Object[] array with the name and the scaled ImagePlus.
	* Does NOT show the new, image; just returns it. */
	 public Object[] exec(ImagePlus imp, String myMethod, int radius,  double par1, double par2, boolean doIwhite ) {

		// 0 - Check validity of parameters
		if (null == imp) return null;
		ImageProcessor ip = imp.getProcessor();
		int xe = ip.getWidth();
		int ye = ip.getHeight();

		//int [] data = (ip.getHistogram());

		IJ.showStatus("Thresholding...");
		long startTime = System.currentTimeMillis();
		//1 Do it
		if (imp.getStackSize()==1){
			    ip.snapshot();
			    Undo.setup(Undo.FILTER, imp);
		}
		// Apply the selected algorithm
		if(myMethod.equals("Bernsen")){
			Bernsen(imp,  radius, par1, par2, doIwhite);
		}
		else if(myMethod.equals("Contrast")){
			Contrast(imp, radius, par1, par2, doIwhite);
		}
		else if(myMethod.equals("Mean")){
			Mean(imp, radius, par1, par2, doIwhite);
		}
		else if(myMethod.equals("Median")){
			Median(imp, radius, par1, par2, doIwhite);
		}
		else if(myMethod.equals("MidGrey")){
			MidGrey(imp, radius, par1, par2, doIwhite);
		}
		else if(myMethod.equals("Niblack")){
			Niblack (imp, radius, par1, par2, doIwhite); 
		}
		else if(myMethod.equals("Otsu")){
			Otsu(imp, radius, par1, par2, doIwhite);
		}
		else if(myMethod.equals("Phansalkar")){
			Phansalkar(imp, radius, par1, par2, doIwhite);
		}
		else if(myMethod.equals("Sauvola")){
			Sauvola(imp, radius, par1, par2, doIwhite);
		}
		//IJ.showProgress((double)(255-i)/255);
		imp.updateAndDraw();
		imp.getProcessor().setThreshold(255, 255, ImageProcessor.NO_LUT_UPDATE);
		// 2 - Return the threshold and the image
		IJ.showStatus("\nDone " + (System.currentTimeMillis() - startTime) / 1000.0);
		return new Object[] {imp};
	}

	 public static void Bernsen(ImagePlus imp, int radius,  double par1, double par2, boolean doIwhite ) {
		// Bernsen recommends WIN_SIZE = 31 and CONTRAST_THRESHOLD = 15.
		//  1) Bernsen J. (1986) "Dynamic Thresholding of Grey-Level Images" 
		//    Proc. of the 8th Int. Conf. on Pattern Recognition, pp. 1251-1255
		//  2) Sezgin M. and Sankur B. (2004) "Survey over Image Thresholding 
		//   Techniques and Quantitative Performance Evaluation" Journal of 
		//   Electronic Imaging, 13(1): 146-165 
		//  http://citeseer.ist.psu.edu/sezgin04survey.html
		// Ported to ImageJ plugin from E Celebi's fourier_0.8 routines
		// This version uses a circular local window, instead of a rectagular one
		ImagePlus Maximp, Minimp;
		ImageProcessor ip=imp.getProcessor(), ipMax, ipMin;
		int contrast_threshold=15;
		int local_contrast;
		int mid_gray;
		byte object;
		byte backg;
		int temp;

		if (par1!=0) {
			IJ.log("Bernsen: changed contrast_threshold from :"+ contrast_threshold + "  to:" + par1);
			contrast_threshold= (int) par1;
		}

		if (doIwhite){
			object =  (byte) 0xff;
			backg =   (byte) 0;
		}
		else {
			object =  (byte) 0;
			backg =  (byte) 0xff;
		}

		Maximp=duplicateImage(ip);
		ipMax=Maximp.getProcessor();
		RankFilters rf=new RankFilters();
		rf.rank(ipMax, radius, rf.MAX);// Maximum
		//Maximp.show();
		Minimp=duplicateImage(ip);
		ipMin=Minimp.getProcessor();
		rf.rank(ipMin, radius, rf.MIN); //Minimum
		//Minimp.show();
		byte[] pixels = (byte [])ip.getPixels();
		byte[] max = (byte [])ipMax.getPixels();
		byte[] min = (byte [])ipMin.getPixels();

		for (int i=0; i<pixels.length; i++) {
			local_contrast = (int)((max[i]&0xff) -(min[i]&0xff));
			mid_gray =(int) ((min[i]&0xff) + (max[i]&0xff) )/ 2;
			temp=(int) (pixels[i] & 0x0000ff);
			if ( local_contrast < contrast_threshold )
				pixels[i] = ( mid_gray >= 128 ) ? object :  backg;  //Low contrast region
			else
				pixels[i] = (temp >= mid_gray ) ? object : backg;
		}    
		//imp.updateAndDraw();
		return;
	}

	 public static void Contrast(ImagePlus imp, int radius,  double par1, double par2, boolean doIwhite) {
		// G. Landini, 2013
		// Based on a simple contrast toggle. This procedure does not have user-provided paramters other than the kernel radius
		// Sets the pixel value to either white or black depending on whether its current value is closest to the local Max or Min respectively
		// The procedure is similar to Toggle Contrast Enhancement (see Soille, Morphological Image Analysis (2004), p. 259

		ImagePlus Maximp, Minimp;
		ImageProcessor ip=imp.getProcessor(), ipMax, ipMin;
		int c_value =0;
		int mid_gray;
		byte object;
		byte backg;


		if (doIwhite){
			object =  (byte) 0xff;
			backg =   (byte) 0;
		}
		else {
			object =  (byte) 0;
			backg =  (byte) 0xff;
		}

		Maximp=duplicateImage(ip);
		ipMax=Maximp.getProcessor();
		RankFilters rf=new RankFilters();
		rf.rank(ipMax, radius, rf.MAX);// Maximum
		//Maximp.show();
		Minimp=duplicateImage(ip);
		ipMin=Minimp.getProcessor();
		rf.rank(ipMin, radius, rf.MIN); //Minimum
		//Minimp.show();
		byte[] pixels = (byte [])ip.getPixels();
		byte[] max = (byte [])ipMax.getPixels();
		byte[] min = (byte [])ipMin.getPixels();
		for (int i=0; i<pixels.length; i++) {
			pixels[i] = ((Math.abs((int)(max[i]&0xff- pixels[i]&0xff)) <= Math.abs((int)(pixels[i]&0xff- min[i]&0xff)))) ? object : backg;
		}    
		//imp.updateAndDraw();
		return;
	}

	public static void Mean(ImagePlus imp, int radius,  double par1, double par2, boolean doIwhite ) {
		// See: Image Processing Learning Resourches HIPR2
		// http://homepages.inf.ed.ac.uk/rbf/HIPR2/adpthrsh.htm
		ImagePlus Meanimp;
		ImageProcessor ip=imp.getProcessor(), ipMean;
		int c_value = 0;
		byte object;
		byte backg;

		if (par1!=0) {
			IJ.log("Mean: changed c_value from :"+ c_value + "  to:" + par1);
			c_value= (int)par1;
		}

		if (doIwhite){
			object =  (byte) 0xff;
			backg =   (byte) 0;
		}
		else {
			object =  (byte) 0;
			backg =  (byte) 0xff;
		}

		Meanimp=duplicateImage(ip);
		ImageConverter ic = new ImageConverter(Meanimp);
		ic.convertToGray32();

		ipMean=Meanimp.getProcessor();
		RankFilters rf=new RankFilters();
		rf.rank(ipMean, radius, rf.MEAN);// Mean
		//Meanimp.show();
		byte[] pixels = (byte []) ip.getPixels();
		float[] mean = (float []) ipMean.getPixels();

		for (int i=0; i<pixels.length; i++) 
			pixels[i] = ( (int)(pixels[i] &0xff) > (int)( mean[i]  - c_value)) ? object : backg;
		//imp.updateAndDraw();
		return;
	}

	public static void Median(ImagePlus imp, int radius,  double par1, double par2, boolean doIwhite ) {
		// See: Image Processing Learning Resourches HIPR2
		// http://homepages.inf.ed.ac.uk/rbf/HIPR2/adpthrsh.htm
		ImagePlus Medianimp;
		ImageProcessor ip=imp.getProcessor(), ipMedian;
		int c_value = 0;
		byte object;
		byte backg;

		if (par1!=0) {
			IJ.log("Median: changed c_value from :"+ c_value + "  to:" + par1);
			c_value= (int) par1;
		}

		if (doIwhite){
			object =  (byte) 0xff;
			backg =   (byte) 0;
		}
		else {
			object =  (byte) 0;
			backg =  (byte) 0xff;
		}

		Medianimp=duplicateImage(ip);
		ipMedian=Medianimp.getProcessor();
		RankFilters rf=new RankFilters();
		rf.rank(ipMedian, radius, rf.MEDIAN);
		//Medianimp.show();
		byte[] pixels = (byte []) ip.getPixels();
		byte[] median = (byte []) ipMedian.getPixels();

		for (int i=0; i<pixels.length; i++) 
			pixels[i] = ( (int)(pixels[i] &0xff) > (int)( (median[i]  &0xff) - c_value)) ? object : backg;
		//imp.updateAndDraw();
		return;
	}

	public static void MidGrey(ImagePlus imp, int radius,  double par1, double par2, boolean doIwhite ) {
		// See: Image Processing Learning Resourches HIPR2
		// http://homepages.inf.ed.ac.uk/rbf/HIPR2/adpthrsh.htm
		ImagePlus Maximp, Minimp;
		ImageProcessor ip=imp.getProcessor(), ipMax, ipMin;
		int c_value =0;
		int mid_gray;
		byte object;
		byte backg;

		if (par1!=0) {
			IJ.log("MidGrey: changed c_value from :"+ c_value + "  to:" + par1);
			c_value= (int) par1;
		}

		if (doIwhite){
			object =  (byte) 0xff;
			backg =   (byte) 0;
		}
		else {
			object =  (byte) 0;
			backg =  (byte) 0xff;
		}

		Maximp=duplicateImage(ip);
		ipMax=Maximp.getProcessor();
		RankFilters rf=new RankFilters();
		rf.rank(ipMax, radius, rf.MAX);// Maximum
		//Maximp.show();
		Minimp=duplicateImage(ip);
		ipMin=Minimp.getProcessor();
		rf.rank(ipMin, radius, rf.MIN); //Minimum
		//Minimp.show();
		byte[] pixels = (byte [])ip.getPixels();
		byte[] max = (byte [])ipMax.getPixels();
		byte[] min = (byte [])ipMin.getPixels();

		for (int i=0; i<pixels.length; i++) {
				pixels[i] = ( (int)(pixels[i] &0xff) > (int)(((max[i]&0xff) +(min[i]&0xff))/2) - c_value ) ? object : backg;
		}    
		//imp.updateAndDraw();
		return;
	}

	public static void Niblack(ImagePlus imp, int radius,  double par1, double par2, boolean doIwhite  ) {
		// Niblack recommends K_VALUE = -0.2 for images with black foreground 
		// objects, and K_VALUE = +0.2 for images with white foreground objects.
		//  Niblack W. (1986) "An introduction to Digital Image Processing" Prentice-Hall.
		// Ported to ImageJ plugin from E Celebi's fourier_0.8 routines
		// This version uses a circular local window, instead of a rectagular one

		ImagePlus Meanimp, Varimp;
		ImageProcessor ip=imp.getProcessor(), ipMean, ipVar;
		double k_value;
		int c_value=0;

		byte object;
		byte backg ;

		if (doIwhite){
			k_value=0.2;
			object =  (byte) 0xff;
			backg =   (byte) 0;
		}
		else {
			k_value= -0.2;
			object =  (byte) 0;
			backg =  (byte) 0xff;
		}

		if (par1!=0) {
			IJ.log("Niblack: changed k_value from :"+ k_value + "  to:" + par1);
			k_value= par1;
		}

		if (par2!=0) {
			IJ.log("Niblack: changed c_value from :"+ c_value + "  to:" + par2);// requested feature, not in original
			c_value=(int)par2;
		}

		Meanimp=duplicateImage(ip);
		ImageConverter ic = new ImageConverter(Meanimp);
		ic.convertToGray32();

		ipMean=Meanimp.getProcessor();
		RankFilters rf=new RankFilters();
		rf.rank(ipMean, radius, rf.MEAN);// Mean
		//Meanimp.show();
		Varimp=duplicateImage(ip);
		ic = new ImageConverter(Varimp);
		ic.convertToGray32();
		ipVar=Varimp.getProcessor();
		rf.rank(ipVar, radius, rf.VARIANCE); //Variance
		//Varimp.show();
		byte[] pixels = (byte []) ip.getPixels();
		float[] mean = (float []) ipMean.getPixels();
		float[] var = (float []) ipVar.getPixels();

		for (int i=0; i<pixels.length; i++) 
			pixels[i] = ( (int)(pixels[i] &0xff) > (int)( mean[i] + k_value * Math.sqrt ( var[i] ) - c_value)) ? object : backg;
		//imp.updateAndDraw();
		return;
	}

	public static void Otsu(ImagePlus imp, int radius,  double par1, double par2, boolean doIwhite) {
		// Otsu's threshold algorithm
		// C++ code by Jordan Bevik <Jordan.Bevic@qtiworld.com>
		// ported to ImageJ plugin by G.Landini. Same algorithm as in Auto_Threshold, this time on local circular regions
		int[] data;
		int w=imp.getWidth();
		int h=imp.getHeight();
		int position;
		int radiusx2=radius * 2;
		ImageProcessor ip=imp.getProcessor();
		byte[] pixels = (byte []) ip.getPixels();
		byte[] pixelsOut = new byte[pixels.length]; // need this to avoid changing the image data (and further histograms)
		byte object;
		byte backg;

		if (doIwhite){
			object =  (byte) 0xff;
			backg =   (byte) 0;
		}
		else {
			object =  (byte) 0;
			backg =  (byte) 0xff;
		}

		int k,kStar;  // k = the current threshold; kStar = optimal threshold
		int N1, N;    // N1 = # points with intensity <=k; N = total number of points
		double BCV, BCVmax; // The current Between Class Variance and maximum BCV
		double num, denom;  // temporary bookeeping
		int Sk;  // The total intensity for all histogram points <=k
		int S, L=256; // The total intensity of the image. Need to hange here if modifying for >8 bits images
		int roiy;

		Roi roi = new OvalRoi(0, 0, radiusx2, radiusx2);
		//ip.setRoi(roi);
		for (int y =0; y<h; y++){
			IJ.showProgress((double)(y)/(h-1)); // this method is slow, so let's show the progress bar
			roiy = y-radius;
			for (int x = 0; x<w; x++){
				roi.setLocation(x-radius,roiy);
				ip.setRoi(roi);
				//ip.setRoi(new OvalRoi(x-radius, roiy, radiusx2, radiusx2));
				position=x+y*w;
				data = ip.getHistogram();

				// Initialize values:
				S = N = 0;
				for (k=0; k<L; k++){
					S += k * data[k];	// Total histogram intensity
					N += data[k];		// Total number of data points
				}

				Sk = 0;
				N1 = data[0]; // The entry for zero intensity
				BCV = 0;
				BCVmax=0;
				kStar = 0;

				// Look at each possible threshold value,
				// calculate the between-class variance, and decide if it's a max
				for (k=1; k<L-1; k++) { // No need to check endpoints k = 0 or k = L-1
					Sk += k * data[k];
					N1 += data[k];

					// The float casting here is to avoid compiler warning about loss of precision and
					// will prevent overflow in the case of large saturated images
					denom = (double)( N1) * (N - N1); // Maximum value of denom is (N^2)/4 =  approx. 3E10

					if (denom != 0 ){
						// Float here is to avoid loss of precision when dividing
						num = ( (double)N1 / N ) * S - Sk; 	// Maximum value of num =  255*N = approx 8E7
						BCV = (num * num) / denom;
					}
					else
						BCV = 0;

					if (BCV >= BCVmax){ // Assign the best threshold found so far
						BCVmax = BCV;
						kStar = k;
					}
				}
				// kStar += 1;	// Use QTI convention that intensity -> 1 if intensity >= k
				// (the algorithm was developed for I-> 1 if I <= k.)
				//return kStar;
				pixelsOut[position] = ((int) (pixels[position]&0xff)>kStar) ? object : backg;
			}
		}
		for (position=0; position<w*h; position++) pixels[position]=pixelsOut[position]; //update with thresholded pixels
	}

	public static void Phansalkar(ImagePlus imp, int radius,  double par1, double par2, boolean doIwhite) {
		// This is a modification of Sauvola's thresholding method to deal with low contrast images.
		// Phansalskar N. et al. Adaptive local thresholding for detection of nuclei in diversity stained
		// cytology images.International Conference on Communications and Signal Processing (ICCSP), 2011, 
		// 218 - 220.
		// In this method, the threshold t = mean*(1+p*exp(-q*mean)+k*((stdev/r)-1))
		// Phansalkar recommends k = 0.25, r = 0.5, p = 2 and q = 10. In this plugin, k and r are the 
                // parameters 1 and 2 respectively, but the values of p and q are fixed.
		//
		// Implemented from Phansalkar's paper description by G. Landini
		// This version uses a circular local window, instead of a rectagular one

		ImagePlus Meanimp, Varimp, Orimp;
		ImageProcessor ip=imp.getProcessor(), ipMean, ipVar, ipOri;
		double k_value = 0.25;
		double r_value = 0.5;
		double p_value = 2.0;
		double q_value = 10.0;
		byte object;
		byte backg;

		if (par1!=0) {
			IJ.log("Phansalkar: changed k_value from :"+ k_value + "  to:" + par1);
			k_value= par1;
		}

		if (par2!=0) {
			IJ.log("Phansalkar: changed r_value from :"+r_value + "  to:" + par2);
			r_value= par2;
		}

		if (doIwhite){
			object =  (byte) 0xff;
			backg =   (byte) 0;
		}
		else {
			object =  (byte) 0;
			backg =  (byte) 0xff;
		}

		Meanimp=duplicateImage(ip);
		ContrastEnhancer ce = new ContrastEnhancer();
		ce.stretchHistogram(Meanimp, 0.0);
		ImageConverter ic = new ImageConverter(Meanimp);
		ic.convertToGray32();
		ipMean=Meanimp.getProcessor();
		ipMean.multiply(1.0/255);

		Orimp=duplicateImage(ip);
		ce.stretchHistogram(Orimp, 0.0);
		ic = new ImageConverter(Orimp);
		ic.convertToGray32();
		ipOri=Orimp.getProcessor();
		ipOri.multiply(1.0/255); //original to compare
		//Orimp.show();

		RankFilters rf=new RankFilters();
		rf.rank(ipMean, radius, rf.MEAN);// Mean

		//Meanimp.show();
		Varimp=duplicateImage(ip);
		ce.stretchHistogram(Varimp, 0.0);
		ic = new ImageConverter(Varimp);
		ic.convertToGray32();
		ipVar=Varimp.getProcessor();
		ipVar.multiply(1.0/255);

		rf.rank(ipVar, radius, rf.VARIANCE); //Variance
		ipVar.sqr(); //SD

		//Varimp.show();
		byte[] pixels = (byte []) ip.getPixels();
		float[] ori = (float []) ipOri.getPixels();
		float[] mean = (float []) ipMean.getPixels();
		float[] sd = (float []) ipVar.getPixels();

		for (int i=0; i<pixels.length; i++) 
			pixels[i] = ( (ori[i]) > ( mean[i] * (1.0 + p_value * Math.exp(-q_value * mean[i]) + k_value * (( sd[i] / r_value)- 1.0)))) ? object : backg;
		//imp.updateAndDraw();
		return;
	}

	void Sauvola(ImagePlus imp, int radius,  double par1, double par2, boolean doIwhite) {
		// Sauvola recommends K_VALUE = 0.5 and R_VALUE = 128.
		// This is a modification of Niblack's thresholding method.
		// Sauvola J. and Pietaksinen M. (2000) "Adaptive Document Image Binarization"
		// Pattern Recognition, 33(2): 225-236
		// http://www.ee.oulu.fi/mvg/publications/show_pdf.php?ID=24
		// Ported to ImageJ plugin from E Celebi's fourier_0.8 routines
		// This version uses a circular local window, instead of a rectagular one

		ImagePlus Meanimp, Varimp;
		ImageProcessor ip=imp.getProcessor(), ipMean, ipVar;
		double k_value = 0.5;
		double r_value = 128;
		byte object;
		byte backg;

		if (par1!=0) {
			IJ.log("Sauvola: changed k_value from :"+ k_value + "  to:" + par1);
			k_value= par1;
		}

		if (par2!=0) {
			IJ.log("Sauvola: changed r_value from :"+r_value + "  to:" + par2);
			r_value= par2;
		}

		if (doIwhite){
			object =  (byte) 0xff;
			backg =   (byte) 0;
		}
		else {
			object =  (byte) 0;
			backg =  (byte) 0xff;
		}

		Meanimp=duplicateImage(ip);
		ImageConverter ic = new ImageConverter(Meanimp);
		ic.convertToGray32();

		ipMean=Meanimp.getProcessor();
		RankFilters rf=new RankFilters();
		rf.rank(ipMean, radius, rf.MEAN);// Mean
		//Meanimp.show();
		Varimp=duplicateImage(ip);
		ic = new ImageConverter(Varimp);
		ic.convertToGray32();
		ipVar=Varimp.getProcessor();
		rf.rank(ipVar, radius, rf.VARIANCE); //Variance
		//Varimp.show();
		byte[] pixels = (byte []) ip.getPixels();
		float[] mean = (float []) ipMean.getPixels();
		float[] var = (float []) ipVar.getPixels();

		for (int i=0; i<pixels.length; i++) 
			pixels[i] = ( (int)(pixels[i] &0xff) > (int)( mean[i] * (1.0+ k_value *(( Math.sqrt ( var[i] )/r_value)-1.0)))) ? object : backg;
		//imp.updateAndDraw();
		return;
	}


	private static ImagePlus duplicateImage(ImageProcessor iProcessor){
		int w=iProcessor.getWidth();
		int h=iProcessor.getHeight();
		ImagePlus iPlus=NewImage.createByteImage("Image", w, h, 1, NewImage.FILL_BLACK);
		ImageProcessor imageProcessor=iPlus.getProcessor();
		imageProcessor.copyBits(iProcessor, 0,0, Blitter.COPY);
		return iPlus;
	} 

}


/**
 * 
 */
package gui;

import java.io.File;

/**
 * @author George Kritikos
 *
 */
public class PicturesFileFilter extends javax.swing.filechooser.FileFilter {

	/* (non-Javadoc)
	 * @see java.io.FileFilter#accept(java.io.File)
	 */
	@Override
	public boolean accept(File pathname) {
		return(new PicturesFilenameFilter().accept(pathname.getParentFile(), pathname.getName()));
	}

	/* (non-Javadoc)
	 * @see javax.swing.filechooser.FileFilter#getDescription()
	 */
	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}
	
}

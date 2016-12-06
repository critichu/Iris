/**
 * 
 */
package iris.ui;

import iris.settings.UserSettings;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.security.CodeSource;
import java.util.HashSet;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

/**
 * @author George Kritikos
 *
 */
class IrisColonyPicker extends JFrame implements ActionListener, PropertyChangeListener {


	static { 
		System.setProperty("plugins.dir", "./plugins/");
		System.setProperty("sun.java2d.opengl", "true");
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 786629138708624640L;
	private JPanel contentPane;
	private JTextPane textPane;
	private JProgressBar progressBar;
	private static JButton btnOpenFolder;
	/**
	 * This is the combo box used to select the profile
	 */
	private static JComboBox comboBox = null;

	
	private static IrisColonyPicker irisColonyPickerInstance = null;
	
	
	
	
	/**
	 * This function returns the Iris frame instance
	 */
	public static IrisColonyPicker getInstance(){
		return(irisColonyPickerInstance);
	}

	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {


		//check if we have enough memory -- if so just run the IrisFrontend
		//memory threshold here is 1GB
		long maxHeapSize = Runtime.getRuntime().maxMemory();
		if(maxHeapSize>(long)1e9){
			
			

			IrisFrontend.userSettings = UserSettings.loadUserSettings();
			
			//apply user settings
			UserSettings.applyUserSettings(IrisFrontend.userSettings);
			
			
			System.setProperty("plugins.dir", "./plugins/");
			System.setProperty("sun.java2d.opengl", "true");

			EventQueue.invokeLater(new Runnable() {
				public void run() {
					try {
						irisColonyPickerInstance = new IrisColonyPicker();
						
						
						//try to set the icon
						try {
							irisColonyPickerInstance.setIconImage(ImageIO.read(new File("resources/icon_256x256.png")));
						}
						catch (Exception e) {
						    //no worries if it failed
						}
						

						//this call tells the system to redirect the System.out and System.err outputs
						//from the console to the textPane object
						irisColonyPickerInstance.redirectSystemStreams();

						System.setProperty("apple.laf.useScreenMenuBar", "false");
						irisColonyPickerInstance.setResizable(false);
						irisColonyPickerInstance.setVisible(true);
						
						
						


						if(IrisFrontend.userSettings!=null){
							System.out.println("Successfully loaded user settings for profiles:");
							HashSet<String> loadedUserSettings = IrisFrontend.userSettings.getLoadedUserSettings();
							for (String loadedProfileName : loadedUserSettings) {
								System.out.println("\t"+loadedProfileName);	
							}
							System.out.println();
							
						} else{
							System.out.println("Could not load user settings,\nusing default settings");
						}					
						
						
						
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});

		}
		else {

			try {
				//if there's not enough space, invoke another JVM, this time with enough heap space
				String pathToThisJar = getPathToJar();

				StringBuilder strBuilder = new StringBuilder();
				for (int i = 0; i < args.length; i++) {
					strBuilder.append(" "+args[i]);
				}
				String allArguments = strBuilder.toString();

				/*
				 * run on 1GB heap memory, unless it's a 64-bit system
				 */				
				String gigabytes = "1";
				String systemArchitecture = System.getProperty("sun.arch.data.model");
				if(systemArchitecture.equals("64")){
					gigabytes = "2";
				}
				
				ProcessBuilder pb = new ProcessBuilder(
						"java",
						"-jar",
						"-Xmx"+gigabytes+"G",
						pathToThisJar,
						allArguments
						);
				pb.directory(new File("."));
				Process process = pb.start();
				process.waitFor();
			} 
			catch (Exception e) {}

		}
	}


	private static String getPathToJar() throws URISyntaxException{
		CodeSource codeSource = IrisColonyPickerGUI.class.getProtectionDomain().getCodeSource();
		File jarFile = new File(codeSource.getLocation().toURI().getPath());
		String jarDir = jarFile.getParentFile().getPath();
		return(jarFile.getPath());
	}



	private static String getJarFolder() throws URISyntaxException{
		CodeSource codeSource = IrisColonyPickerGUI.class.getProtectionDomain().getCodeSource();
		File jarFile = new File(codeSource.getLocation().toURI().getPath());
		String jarDir = jarFile.getParentFile().getPath();
		return(jarDir);
	}


	public static void printUsage(){
		System.out.println("Usage: Iris ProfileName FolderLocation\n");
		System.out.println("Tip: call without any arguments to invoke GUI\n");
	}


	/**
	 * In case we're running in console mode, the combobox will be null
	 * @return
	 */
	public static String getCurrentlySelectedProfile(){
		if(comboBox!=null)
			return((String)comboBox.getSelectedItem());
		else
			return(IrisFrontend.selectedProfile);
	}

	/**
	 * Create the frame.
	 */
	public IrisColonyPicker() {
		this.setTitle("Iris ColonyPicker v"+IrisFrontend.IrisVersion);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 450, 300);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);

		progressBar = new JProgressBar(0, 100);
		progressBar.setBounds(43, 252, 363, 20);
		progressBar.setStringPainted(true);
		contentPane.add(progressBar);

		textPane = new JTextPane()
		{
			public boolean getScrollableTracksViewportWidth()
			{
				return getUI().getPreferredSize(this).width 
						<= getParent().getSize().width;
			}
		};
		textPane.setEditable(false);
		//textPane.setBounds(67, 36, 316, 208);

		JScrollPane scrollPane = new JScrollPane(textPane);
		scrollPane.setBounds(67, 36, 316, 208);


		contentPane.add(scrollPane);

		btnOpenFolder = new JButton("open file");
		btnOpenFolder.addActionListener(this);

		btnOpenFolder.setBounds(258, 6, 117, 29);
		contentPane.add(btnOpenFolder);

		comboBox = new JComboBox(IrisFrontend.profileCollection);
		comboBox.setSelectedIndex(0);
		comboBox.setBounds(77, 7, 166, 27);
		comboBox.addActionListener(this);
		contentPane.add(comboBox);

		//add a custom listener to comboBox clicks
		//this one will adjust the height of the comboBox
		BoundsPopupMenuListener listener = new BoundsPopupMenuListener(true, false);
		comboBox.addPopupMenuListener( listener );
		//comboBox.setPrototypeDisplayValue("ItemWWW");





		//make sure the log file is closed when the user closes the window
		this.addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowClosing(java.awt.event.WindowEvent windowEvent) {
				IrisFrontend.closeLog();
				System.exit(0);
			}
		});

	}





	//redirect the console output to the application's GUI
	private void updateTextPane(final String text) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				Document doc = textPane.getDocument();
				try {
					doc.insertString(doc.getLength(), text, null);
				} catch (BadLocationException e) {
					throw new RuntimeException(e);
				}
				textPane.setCaretPosition(doc.getLength() - 1);
			}
		});
	}

	private void updateTextPaneAndLog_err(final String text) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				//first, append this entry to the log file
				IrisFrontend.writeToLog(text);

				//then update the text pane
				Document doc = textPane.getDocument();

				//set the text color to be red
				SimpleAttributeSet set = new SimpleAttributeSet();
				StyleConstants.setForeground(set, Color.RED);

				try {
					doc.insertString(doc.getLength(), text, set);
				} catch (BadLocationException e) {
					throw new RuntimeException(e);
				}
				textPane.setCaretPosition(doc.getLength() - 1);
			}
		});
	}




	private void redirectSystemStreams() {

		OutputStream out = new OutputStream() {
			@Override
			public void write(final int b) throws IOException {
				updateTextPane(String.valueOf((char) b));
			}

			@Override
			public void write(byte[] b, int off, int len) throws IOException {
				updateTextPane(new String(b, off, len));
			}

			@Override
			public void write(byte[] b) throws IOException {
				write(b, 0, b.length);
			}
		};

		OutputStream err = new OutputStream() {
			@Override
			public void write(final int b) throws IOException {
				updateTextPaneAndLog_err(String.valueOf((char) b));
			}

			@Override
			public void write(byte[] b, int off, int len) throws IOException {
				updateTextPaneAndLog_err(new String(b, off, len));
			}

			@Override
			public void write(byte[] b) throws IOException {
				write(b, 0, b.length);
			}
		};

		System.setOut(new PrintStream(out, true));
		System.setErr(new PrintStream(err, true));
	}

	/* (non-Javadoc)
	 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
	 */
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if ("progress" == evt.getPropertyName()) {
			int progress = (Integer) evt.getNewValue();
			progressBar.setValue(progress);
		}

	}

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent e) {

		if(e.getSource()==btnOpenFolder){

			//make user select a file here
			File imageFile = selectFile();
			if(imageFile==null)
				return; // user didn't select a folder

			try{
				String args[] = new String[2];
				args[0] =  IrisFrontend.selectedProfile;
				args[1] =  imageFile.getPath();
				
							
				ColonyPickerFrontendBigMem.main(args);

			}
			catch(Exception e1){
				System.out.println("Error processing file!\n");
				e1.printStackTrace(System.err);
			}


			return;
		}
		if(e.getSource()==comboBox){

			IrisFrontend.selectedProfile = (String) comboBox.getSelectedItem();
		}

	}

	
	
	/**
	 * @return
	 */
	private File selectFile() {

		//create the filechooser object
		//System.setProperty("apple.awt.fileDialogForDirectories", "true");
	    FileDialog fc = new FileDialog(this);
	    fc.setTitle("please select a file");
	    fc.setFilenameFilter(new PicturesFilenameFilter());
	    fc.setVisible(true);
	   
	    String directoryPath = fc.getDirectory() + fc.getFile();
	    File directory = new File(directoryPath);
	    
	    if(directory.exists())
	    	return(directory);
	    else
	    	return(null);
	    	
	}


	/**
	 * @return
	 */
	/*private File selectFile() {


		boolean debugMode = false;

		if(debugMode)
			return(new File("/Users/george/Desktop/44hrs/cocort-44hr-lbnosalt-keio-1_D.JPG"));


		//create the filechooser object
		final JFileChooser fc = new JFileChooser();
		fc.setFileFilter(new FileNameExtensionFilter(
			    "Image files", ImageIO.getReaderFileSuffixes()));

		//make it show only folders
		//fc.setFileSelectionMode(JFileChooser.FILES_ONLY);

		int returnVal = fc.showOpenDialog(this);

		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File directory = fc.getSelectedFile();
			return(directory);
		}
		else{
			return null;
		}
	}*/






	public static String setdoubleQuote(String myText) {
		String quoteText = "";
		if (!myText.isEmpty()) {
			quoteText = "\"" + myText + "\"";
		}
		return quoteText;
	}


}

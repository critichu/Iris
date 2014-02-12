/**
 * 
 */
package gui;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
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
public class IrisGUI extends JFrame implements ActionListener, PropertyChangeListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 999066460213210379L;
	private JPanel contentPane;
	private JTextPane textPane;
	public JProgressBar progressBar;
	public static JButton btnOpenFolder;
	/**
	 * This is the combo box used to select the profile
	 */
	public static JComboBox comboBox = null;
	
	public static String selectedProfile;


	/**
	 * these are added specially for the multithreading case
	 */
	public static boolean multiThreaded = false;
	public static ExecutorService executorService;
	public static List<Callable<Object>> todoThread;
	public static int numberOfThreads = 4;


	/**
	 * This string array holds the names of all the profiles
	 */
	public static String[] profileCollection = {
		"Stm growth",
		"Ecoli growth",
		"Ecoli opacity",
		"Ecoli opacity 384",
		"Ecoli growth 384 - hazy colonies",
		"B.subtilis Opacity (HSB)",
		"B.subtilis Sporulation (HSB)",
		//"CPRG 384",
		"Biofilm formation",
		"Biofilm formation (HSB)",
		//"Biofilm formation - Simple Grid",
		//"Opacity",
		//"Opacity (fixed grid)"
	};


	/**
	 * This is the name of the log file to be written. 
	 * Iris opens it for appending on every invocation of the software, writing a header with the time.
	 * It closes it after a run is done.
	 */
	public static BufferedWriter logFile = null;

	/**
	 * This string holds the software version that is defined here once to be used whenever it needs to be displayed.
	 */
	public static String IrisVersion = "0.9.4";

	/**
	 * This string holds the commit id of Iris versioning in Git
	 */
	public static String IrisBuild = "11bfb32";


	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {

		if(args.length==0){//GUI version
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					try {
						IrisGUI frame = new IrisGUI();

						//this call tells the system to redirect the System.out and System.err outputs
						//from the console to the textPane object
						frame.redirectSystemStreams();

						System.setProperty("apple.laf.useScreenMenuBar", "false");
						frame.setResizable(false);
						frame.setVisible(true);				
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
		}
		else{ //command line version
			
			if(args.length<2){
				printUsage();
				return;
			}

			selectedProfile = args[0];
			String folderLocation = args[1];

			//IrisGUI this_ = new IrisGUI();
			//comboBox = new JComboBox(profileCollection);
			//comboBox.setSelectedIndex(0);
			//comboBox.setSelectedItem(profileName);

			ProcessFolderWorker processFolderWorker = new ProcessFolderWorker();
			processFolderWorker.directory = new File(folderLocation);

			try {
				processFolderWorker.doInBackground();
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
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
			return(selectedProfile);
	}

	/**
	 * Create the frame.
	 */
	public IrisGUI() {
		this.setTitle("Iris v"+IrisVersion);
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

		btnOpenFolder = new JButton("open folder");
		btnOpenFolder.addActionListener(this);

		btnOpenFolder.setBounds(258, 6, 117, 29);
		contentPane.add(btnOpenFolder);

		comboBox = new JComboBox(profileCollection);
		comboBox.setSelectedIndex(0);
		comboBox.setBounds(77, 7, 166, 27);
		contentPane.add(comboBox);






		//make sure the log file is closed when the user closes the window
		this.addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowClosing(java.awt.event.WindowEvent windowEvent) {
				closeLog();
				System.exit(0);
			}
		});

	}


	/**
	 * This function will create a unique log filename and open it for writing
	 */
	public static void openLog(String path){		
		String uniqueLogFilename = path + File.separator + getUniqueLogFilename();
		try {
			IrisGUI.logFile = new BufferedWriter(new FileWriter(uniqueLogFilename));
		} catch (IOException e) {
			System.err.println("Could not open log file");
			IrisGUI.logFile = null;
		}
	}

	/**
	 * Does what it says in the box
	 */
	public static void writeToLog(String text){
		try {
			if(logFile!=null) 
				IrisGUI.logFile.write(text);
		} catch (IOException e) {
			//System.err.println("Error writing log file");
			//fail silently, because the standard error is redirected to this function
		}
	}	

	/**
	 * Does what it says in the box
	 */
	public static void closeLog(){

		try {
			if(logFile!=null) 
				IrisGUI.logFile.close();
		} catch (IOException e) {
			System.err.println("Error writing log file");
		}
	}



	/**
	 * This function will create a unique filename, using the Iris version and the current time
	 * @return
	 */
	public static String getUniqueLogFilename() {
		return("iris_v"+IrisVersion+"_"+getDateTime()+".log");
	}

	/**
	 * This function will return the date and time in a format that can be used to create a unique filename.
	 * @return
	 */
	private final static String getDateTime(){
		DateFormat df = new SimpleDateFormat("yyyy.MM.dd_hh.mm.ss");
		return df.format(new Date());
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
				writeToLog(text);

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

			//make user select folder here
			File directory = selectFolder();

			if(directory==null) //user didn't select a folder
				return;

			//disable the open folder button, it will be enabled by ProcessFolderWorker, once it's done
			btnOpenFolder.setEnabled(false);
			progressBar.setValue(0);

			ProcessFolderWorker processFolderWorker = new ProcessFolderWorker();
			processFolderWorker.addPropertyChangeListener(IrisGUI.this);
			processFolderWorker.directory = directory;

			processFolderWorker.execute();

		}

	}

	/**
	 * @return
	 */
	private File selectFolder() {

		//create the filechooser object
		final JFileChooser fc = new JFileChooser();


		//make it show only folders
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		int returnVal = fc.showOpenDialog(this);

		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File directory = fc.getSelectedFile();
			return(directory);
		}
		else{
			return null;
		}
	}
}

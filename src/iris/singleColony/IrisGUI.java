/**
 * 
 */
package iris.singleColony;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

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


	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {


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
			return(IrisSingleColonyFrontend.selectedProfile);
	}

	/**
	 * Create the frame.
	 */
	public IrisGUI() {
		this.setTitle("Iris v"+IrisSingleColonyFrontend.IrisVersion);
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

		comboBox = new JComboBox(IrisSingleColonyFrontend.profileCollection);
		comboBox.setSelectedIndex(0);
		comboBox.setBounds(77, 7, 166, 27);
		comboBox.addActionListener(this);
		contentPane.add(comboBox);






		//make sure the log file is closed when the user closes the window
		this.addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowClosing(java.awt.event.WindowEvent windowEvent) {
				IrisSingleColonyFrontend.closeLog();
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
				IrisSingleColonyFrontend.writeToLog(text);

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

			return;
		}
		if(e.getSource()==comboBox){
			
	        IrisSingleColonyFrontend.selectedProfile = (String) comboBox.getSelectedItem();
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

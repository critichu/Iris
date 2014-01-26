/**
 * 
 */
package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JButton;
import javax.swing.JSplitPane;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;

/**
 * @author george
 *
 */
public class ColonySizer2GUI extends JFrame {

	private JPanel contentPane;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					ColonySizer2GUI frame = new ColonySizer2GUI();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public ColonySizer2GUI() {
		setTitle("Colony Sizer 2");
		setResizable(false);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 800, 500);
		
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		
		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);
		
		JMenuItem mntmOpenPicture = new JMenuItem("Open picture");
		mnFile.add(mntmOpenPicture);
		
		JMenuItem mntmOpenFolder = new JMenuItem("Open folder");
		mnFile.add(mntmOpenFolder);
		
		JMenu mnProfile = new JMenu("Profile");
		menuBar.add(mnProfile);
		
		JMenu mnSettings = new JMenu("Settings");
		menuBar.add(mnSettings);
		
		JMenu mnRun = new JMenu("Run");
		menuBar.add(mnRun);
		contentPane = new JPanel();
		setContentPane(contentPane);
		
	
		
		
		// create the status bar panel and shove it down the bottom of the frame
		JPanel statusPanel = new JPanel();
		statusPanel.setBorder(new BevelBorder(BevelBorder.LOWERED));
		this.add(statusPanel);
		statusPanel.setPreferredSize(new Dimension(this.getWidth(), 16));
		statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.X_AXIS));
		
		JLabel statusLabel = new JLabel("status");
		statusLabel.setHorizontalAlignment(SwingConstants.LEFT);
		statusPanel.add(statusLabel);
		
		
		
		
		//TODO: uncomment this once we have the name of the text pane object 
		//redirectSystemStreams();
	}
	
	
	
	/* TODO: change the name of the textPane to the name of the actual text pane that the GUI has
	 
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

		private void updateTextPane_err(final String text) {
		  SwingUtilities.invokeLater(new Runnable() {
		    public void run() {
		      Document doc = textPane.getDocument();

		      //set the text color to be red
		      SimpleAttributeSet set = new SimpleAttributeSet();
		      StyleConstants.setForeground(set, Color.Red);
		      
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
		      updateTextPane_err(String.valueOf((char) b));
		    }
		 
		    @Override
		    public void write(byte[] b, int off, int len) throws IOException {
		      updateTextPane_err(new String(b, off, len));
		    }
		 
		    @Override
		    public void write(byte[] b) throws IOException {
		      write(b, 0, b.length);
		    }
		  };
		 
		  System.setOut(new PrintStream(out, true));
		  System.setErr(new PrintStream(err, true));
		}
		
		*/

}

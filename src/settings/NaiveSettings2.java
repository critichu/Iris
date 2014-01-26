/**
 * 
 */
package settings;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.io.Serializable;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JEditorPane;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.ListSelectionModel;

/**
 * @author George Kritikos
 *
 */
public class NaiveSettings2 extends Settings implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 5923659408413964737L;
	private JPanel contentPane;

	/**
	 * Create the frame.
	 */
	public NaiveSettings2() {
		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		setBounds(100, 100, 450, 300);
		contentPane = new JPanel();
		contentPane.setLayout(null);
		setContentPane(contentPane);
		
		JCheckBox chckbxTestCheckBox = new JCheckBox("test check box 2");
		chckbxTestCheckBox.setBounds(60, 122, 208, 23);
		contentPane.add(chckbxTestCheckBox);
		
		
		
		
		
		
		chckbxTestCheckBox.setSelected(true);
		
		JList list = new JList();
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setBounds(267, 78, 66, -23);
		contentPane.add(list);
		
		
		///find out how to set lists
		
		
	}
}

package gui;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
public class TableTest extends JFrame
{
	public TableTest()
	{
		super();
		Container c = getContentPane();
		c.setLayout(new FlowLayout() );
		Object[][] data = {
				{"", ""},
				{"", ""},
				{"", ""},
				{"", ""},
				{"", ""},
				{"", ""},
				{"", ""},
				{"", ""},
				{"", ""},
				{"", ""},
				{"", ""},
				{"", ""},
				{"", ""},
				{"", ""},
				{"", ""}
		};
		String[] columnNames = {"Date", 
		"Title"};
		JTable searchTable = new JTable( data, columnNames );


		JScrollPane tableScrollPane = new JScrollPane();
		tableScrollPane.setPreferredSize(new Dimension(400,230));
		tableScrollPane.getViewport().add(searchTable);


		//searchTable.setPreferredScrollableViewportSize(new Dimension(400, 100));           
		//this also works

		c.add(tableScrollPane);        
	}
	public static void main(String args[])
	{
		TableTest window = new TableTest();
		window.addWindowListener(
				new WindowAdapter() 
				{
					public void windowClosing(java.awt.event.WindowEvent A)
					{
						System.exit(0);
					}
				}
				);
		window.setSize(500,200);
		
	}
}

package cz.vutbr.feec.imageprocessing.imagej.pluginsAndFilters;

import ij.ImagePlus;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import cz.vutbr.feec.imageprocessing.IOObject.ImagePlusSegmentedIOObject;
import cz.vutbr.feec.imageprocessing.IOObject.innerObject.SegmentMask;
import cz.vutbr.feec.imageprocessing.IOObject.render.ImagePlusSegmentsRenderer;

public class SegmentsMouseListener extends MouseAdapter {
	private ImagePlusSegmentedIOObject ipIO;
	private JLabel positionInfo;
	private JLabel label;
	private ImageIcon icon;

	public SegmentsMouseListener(ImagePlusSegmentedIOObject ipIO,
			ImageIcon icon, JLabel label, JLabel positionInfo) {
		this.ipIO = ipIO;
		this.positionInfo = positionInfo;
		this.label = label;
		this.icon = icon;
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		int x = e.getX() - label.getSize().width / 2
				+ ipIO.getImage().getWidth() / 2;
		int y = e.getY() - label.getSize().height / 2
				+ ipIO.getImage().getHeight() / 2;

		if (x < 0 || y < 0 || x > ipIO.getImage().getWidth()
				|| y > ipIO.getImage().getHeight()) {
			return;
		}

		String message = "";
		for (SegmentMask s : ipIO.getSegmentMasks()) {
			ImagePlus mask = s.getMask();
			/*
			 * System.out.println("ICON " + icon.getSize().width + " - " +
			 * icon.getSize().height); System.out.println("Y " + y);
			 * System.out.println("X " + x); System.out.println("W " +
			 * mask.getWidth()); System.out.println("H " + mask.getHeight());
			 * System.out.println("P " + mask.getProcessor().get(x, y));
			 */
			if (mask.getProcessor().get(x, y) > 127) {
				message += "ID:" + s.getId() + " ";
				s.setMarked(!s.isMarked());
			}
		}
		this.icon.setImage(ImagePlusSegmentsRenderer
				.getImage(ImagePlusSegmentsRenderer.drawSegmentBorders(
						ipIO.getImage(), ipIO.getSegmentMasks())));
		
		// System.out.println("MSG " + message);
		positionInfo.setText(message);
		positionInfo.repaint();
		label.repaint();
	}
}

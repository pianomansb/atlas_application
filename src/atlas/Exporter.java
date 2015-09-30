package atlas;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;

@SuppressWarnings("serial")
public class Exporter extends JComponent {
	private JList<String> labels;
	private JList<String> cards;
	private final DataManager dm;
	
	private Exporter(DataManager dm) {
		this.dm = dm;
		labels = new JList<String>(new DefaultListModel<String>());
		labels.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		cards = new JList<String>(new DefaultListModel<String>());
		
		populateLabelsList(labels);
		addLabelsListListener(labels);
		labels.setSelectedValue(Card.ALL, true);
		
		JScrollPane labelsPane = new JScrollPane(labels);
		JScrollPane cardsPane = new JScrollPane(cards);
		
		GroupLayout layout = new GroupLayout(this);
		setLayout(layout);
		layout.setHorizontalGroup(layout.createSequentialGroup()
			.addComponent(labelsPane)
			.addComponent(cardsPane)
		);
		layout.setVerticalGroup(layout.createParallelGroup()
			.addComponent(cardsPane)
			.addComponent(labelsPane)
		);
		
		
	}
	
	private void populateLabelsList(JList<String> labels) {
		DefaultListModel<String> model = (DefaultListModel<String>)labels.getModel();
		String[] labelsList = dm.getLabels();
		Arrays.sort(labelsList);
		for( String label : labelsList ){
			model.addElement(label);
		}
	}
	
	private void addLabelsListListener(JList<String> labels) {
		labels.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				populateCardsList(labels.getSelectedValue());
			}
		});
	}
	
	/** if null, removes all elements */
	private void populateCardsList(String selectedLabel) {
		DefaultListModel<String> model = (DefaultListModel<String>)cards.getModel();
		model.clear();
		if( selectedLabel == null ) return;
		for( String name : dm.getCardNamesForLabel(selectedLabel) ){
			model.addElement(name);
		}
	}
	
	public String getSelectedLabel() {
		return labels.getSelectedValue();
	}
	
	public int[] getSelectedCardIndices() {
		return cards.getSelectedIndices();
	}
	
	@Override
	public Dimension getPreferredSize() {
		return new Dimension(600,500);
	}
	
	
	public static void runExport(DataManager dm) {
		Exporter exporter = new Exporter(dm);
		while( exporter.getSelectedCardIndices().length == 0 ){
			int result = showCardSelectionDialog(exporter);
			if( result != JOptionPane.OK_OPTION ) return;
		}

		FileBrowserComponent fileBrowser = new FileBrowserComponent(false);
		while( fileBrowser.getChosenFile() == null ){
			int result = showDestinationSelectionDialog(fileBrowser);
			if( result != JOptionPane.OK_OPTION ) return;
		}
		
		FileValidator validator = new FileValidator(Paths.get(fileBrowser.getChosenFile().toURI()));
		Object choice = showFolderNamingDialog(validator);
		if( !choice.equals(JOptionPane.OK_OPTION) ) return;
		
		exportCards(
				dm.getCardsFromLabel(exporter.getSelectedLabel(), exporter.getSelectedCardIndices()), 
				fileBrowser.getChosenFile(),
				validator.getDestinationName() );
	}
	
	private static void exportCards(final Card[] cards, final File destination, final String destName) {
		final JDialog dialog = new JDialog((Frame)null, true);
		Object[] options = {};
		JOptionPane optionPane = new JOptionPane("Exporting...", 
				JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null, options);
		dialog.setContentPane(optionPane);
		dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		
		class RunExport extends SwingWorker<Void, Void> {
			@Override
			public Void doInBackground() {
				
				File dest = new File(destination, destName.concat(".tar"));
				try( TarArchiveOutputStream output = new TarArchiveOutputStream(new FileOutputStream(dest)) ) {
					for( Card card : cards ){
						addCardEntry(output, card);
						addCardImageEntry(output, card);
					}
					output.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				return null;
				
				/*File dest = new File(destination, destName);
				if( dest.mkdir() == false ){
					JOptionPane.showMessageDialog(null, 
							"Failed to create directory for export files - export failed");
					return null;
				}

				for( Card card : cards ){
					try {
						card.writeToFile(new File(dest, card.id + card.name + ".card"));
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					}
					try {
						Files.copy(
								Paths.get(DataManager.START_DIRECTORY.getPath(), card.imageFilename), 
								Paths.get(dest.getPath(), new File(card.imageFilename).getName()), 
								StandardCopyOption.COPY_ATTRIBUTES );
					} catch (IOException e) {
						e.printStackTrace();
					} 
				}
				return null;*/
			}
			
			@Override
			public void done() {
				dialog.setVisible(false);
			}
		};
		(new RunExport()).execute();
		dialog.pack();
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);
		
		JOptionPane.showMessageDialog(null, "Export complete!");
	}
	
	private static void addCardEntry(TarArchiveOutputStream output, Card card) throws IOException {
		File cardFile = card.getCurrentFile();
		ArchiveEntry entry = output.createArchiveEntry(cardFile, cardFile.getName());
		output.putArchiveEntry(entry);
		IOUtils.copy(new FileInputStream(cardFile), output);
		output.closeArchiveEntry();
	}
	
	private static void addCardImageEntry(TarArchiveOutputStream output, Card card) throws IOException {
		File imageFile = new File(DataManager.START_DIRECTORY, card.imageFilename);
		ArchiveEntry entry = output.createArchiveEntry(imageFile, card.imageFilename);
		output.putArchiveEntry(entry);
		IOUtils.copy(new FileInputStream(imageFile), output);
		output.closeArchiveEntry();
	}
	
	private static Object showFolderNamingDialog(FileValidator validator) {
		JButton cancelBut = new JButton("Cancel");
		JButton continueBut = new JButton("Continue");
		Object[] options = { continueBut, cancelBut };
		
		JDialog dialog = new JDialog((Frame)null, true);
		JOptionPane optionPane = new JOptionPane(
				validator, 
				JOptionPane.PLAIN_MESSAGE, 
				JOptionPane.OK_CANCEL_OPTION, 
				null, 
				options, 
				options[0]);
		optionPane.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent e) {
				if( dialog.isVisible() &&
						e.getSource() == optionPane &&
						JOptionPane.VALUE_PROPERTY.equals(e.getPropertyName()) )
					dialog.setVisible(false);
			}
		});
		
		cancelBut.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				optionPane.setValue(JOptionPane.CANCEL_OPTION);
			}
		});
		Action continueAction = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				optionPane.setValue(JOptionPane.OK_OPTION);
			}
		};
		continueAction.setEnabled(false);
		continueBut.setAction(continueAction);
		continueBut.setText("Continue");
		validator.setAction(continueAction);
		
		dialog.setContentPane(optionPane);
		dialog.pack();
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);
		
		return optionPane.getValue();
	}
	
	
	private static int showDestinationSelectionDialog(FileBrowserComponent fileBrowser) {
		Object[] options = {"Continue", "Cancel"};
		return JOptionPane.showOptionDialog(
				null,
				fileBrowser,
				"Export",
				JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE,
				null,
				options,
				options[0]);
	}
	
	
	private static int showCardSelectionDialog(Exporter exporter) {
		Object[] options = {"Continue", "Cancel"};
		return JOptionPane.showOptionDialog(
				null, 
				exporter, 
				"Export", 
				JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE,
				null,
				options,
				options[0]);
	}
}

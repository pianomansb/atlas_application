package atlas;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

@SuppressWarnings("serial")
public class FileBrowserComponent extends JComponent implements FileVisitor<Path> {
	private JList<String> list;
	private JLabel title;
	private ArrayList<File> loadedFiles;
	private Path curParent;
	private final File BACK = new File("<back>");
	private final boolean showPicturePreview;
	
	public FileBrowserComponent() {
		this(true);
	}
	
	public FileBrowserComponent(boolean showPicturePreview) {
		this.showPicturePreview = showPicturePreview;
		final PicturePreviewComponent preview = 
				showPicturePreview ? new PicturePreviewComponent() : null;
		list = new JList<String>(new DefaultListModel<String>());
		list.setVisibleRowCount(20);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if( e.getClickCount() == 2 ){
					open();
				}
			}
		});
		list.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if( showPicturePreview )
					preview.setFile(getFileFromSelection());
			}
		});
		list.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "enter");
		list.getActionMap().put("enter", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				open();
			}
		});
		JScrollPane scrollPane = new JScrollPane(list);
		title = new JLabel();
		title.setFont(new Font("SansSerif", Font.PLAIN, 16));
		
		GroupLayout layout = new GroupLayout(this);
		setLayout(layout);
		
		GroupLayout.SequentialGroup horizontalGroup = 
				layout.createSequentialGroup().addComponent(scrollPane);
		if( showPicturePreview ) horizontalGroup.addComponent(preview);
		
		GroupLayout.ParallelGroup verticalGroup = 
				layout.createParallelGroup().addComponent(scrollPane);
		if( showPicturePreview ) verticalGroup.addComponent(preview);
		
		layout.setHorizontalGroup(layout.createParallelGroup()
			.addComponent(title)
			.addGroup(horizontalGroup)
		);
		layout.setVerticalGroup(layout.createSequentialGroup()
			.addComponent(title)
			.addGroup(verticalGroup)
		);
		
		/*setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.LINE_START;
		add(title, c);
		
		c.gridx = 0;
		c.gridy = 1;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = .5;
		c.weighty = .5;
		add(scrollPane, c);
		
		if( showPicturePreview ){
			c.gridx = 1;
			c.gridy = 1;
			c.fill = GridBagConstraints.BOTH;
			c.weightx = .5;
			c.weighty = .5;
			add(preview, c);
		}*/
		
		curParent = Paths.get(System.getProperty("user.home"));
		refresh();
	}
	
	/**
	 * Tries to open the current selection. 
	 * If there is no selection, there is no effect. If the selection is a directory,
	 * the window is refreshed to show its contents. If the selection is a file, 
	 * the function returns false, indicating that the window should be hidden and 
	 * the result queried. 
	 * @return false if the selection is a file and browsing should be terminated, true otherwise
	 */
	public boolean open() {
		File selection = getFileFromSelection();
		if( selection == null ) return true;
		if( selection.equals(BACK) ){
			Path newPar = curParent.getParent();
			if( newPar == null ) return true;
			curParent = newPar;
			refresh();
			return true;
		}
		if( selection.isDirectory() ){
			curParent = selection.toPath();
			refresh();
			return true;
		}
		return false;
	}
	
	/** returns null if there is no selection */
	private File getFileFromSelection() {
		String selectedValue = list.getSelectedValue();
		if( selectedValue == null ) return null;
		if( selectedValue.equals("<back>") ) return BACK;
		return new File(curParent.toFile(), selectedValue);
	}
	
	private void refresh() {
		loadedFiles = new ArrayList<File>();
		loadedFiles.add(new File("<back>"));
		try {
			Files.walkFileTree(curParent, this);
		} catch (IOException e) {
			System.err.println("problem walking file tree");
			e.printStackTrace();
		}
		
		title.setText(curParent.toString());
		
		DefaultListModel<String> model = (DefaultListModel<String>)list.getModel();
		model.removeAllElements();
		for( File f : loadedFiles )
			model.addElement(f.getName());
	}
	
	public File getChosenFile() {
		return getFileFromSelection();
	}
	
	@Override
	public Dimension getPreferredSize() {
		if( !showPicturePreview ) return new Dimension(600, 500);
		else return super.getPreferredSize();
	} 

	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
			throws IOException {
		if( dir.equals(curParent) ) return FileVisitResult.CONTINUE;
		loadedFiles.add(dir.toFile());
		return FileVisitResult.SKIP_SUBTREE;
	}

	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
			throws IOException {
		if( !Files.isHidden(file) ) //TODO this isn't perfect
			loadedFiles.add(file.toFile());
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFileFailed(Path file, IOException exc)
			throws IOException {
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult postVisitDirectory(Path dir, IOException exc)
			throws IOException {
		return FileVisitResult.CONTINUE;
	}

	
	/**
	 * Holds information from the recently closed file browser.
	 */
	static class FileBrowserResult {
		/** the file selected by the user */
		public final File file;
		
		public FileBrowserResult(File file_) {
			file = file_;
		}
	}
}



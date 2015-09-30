package atlas;

import java.awt.Frame;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

public class Importer implements FileVisitor<Path> {
	private final DataManager dm;
	private final Path START_DIR;
	private ArrayList<String> failedImageCopies = new ArrayList<String>();
	
	private Importer(DataManager dm, Path start_dir) {
		this.dm = dm;
		START_DIR = start_dir;
	}
	
	/**
	 * Try to find the image with filename given, appended to START_DIR, and copy to 
	 * dm.START_DIRECTORY
	 * @return true if the copy succeeded, false if an exception was thrown
	 */
	private boolean copyImage(String imageFilename) {
		try {
			Files.copy(Paths.get(START_DIR.toString(), imageFilename), 
					Paths.get(DataManager.START_DIRECTORY.toString(), imageFilename), 
					StandardCopyOption.COPY_ATTRIBUTES);
		} catch (IOException e) {
			System.err.println("Copying image " + imageFilename + " failed.");
			return false;
		}
		return true;
	}
	
	public static void runImport(DataManager dm) {
		Path choice = Paths.get( showImportSelectionDialog().toURI() );
		
		importFiles(dm, choice);
	}

	private static File showImportSelectionDialog() {
		FileBrowserComponent fileBrowser = new FileBrowserComponent(false);
		File choice;
		do {
			Object[] options = {"Continue", "Cancel"};
			int result = JOptionPane.showOptionDialog(
					null, 
					fileBrowser, 
					"Import", 
					JOptionPane.OK_CANCEL_OPTION, 
					JOptionPane.PLAIN_MESSAGE,
					null, 
					options, 
					options[0] );
			if( result != JOptionPane.OK_OPTION ) return null;
			
			choice = fileBrowser.getChosenFile();
		} while( choice == null );
				
		return choice;
	}

	/** startdir is the absolute Path to the file the user chose, i.e. /foo/bar/file.tar 
	 * should be a properly exported .tar file */
	private static void importFiles(final DataManager dm, final Path startdir) {
		final JDialog dialog = createProgressDialog();
		
		class RunImport extends SwingWorker<Void, Void> {
			@Override
			public Void doInBackground() {
				//this variable will be used to denote a temporary holding site for every archived file,
				// not just .card files
				Path tempCardDest = Paths.get(DataManager.START_DIRECTORY.getAbsolutePath(), "SYSTEM_TEMP_CARD.card"); 
				try( TarArchiveInputStream input = 
						new TarArchiveInputStream(new FileInputStream(startdir.toFile())) ){
					TarArchiveEntry entry;
					while( (entry = input.getNextTarEntry()) != null ) {
						Importer.copyEntryToTempFile(input, entry, tempCardDest);
						if( entry.getName().endsWith(".card") )
							//load into memory and save to user's Atlas directory
							Card.parseFile(dm, tempCardDest.toFile(), true).writeToFile();
						else
							//just copy anything that's not a .card file
							Files.copy( tempCardDest, 
									Paths.get(DataManager.START_DIRECTORY.getPath(), entry.getName()) );
					}
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				try {
					Files.deleteIfExists(tempCardDest); //get rid of temp file
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				return null;
				
				/*Importer importer = new Importer(dm, startdir);
				try {
					Files.walkFileTree(startdir, importer);
				} catch (IOException e) {
					JOptionPane.showMessageDialog(null, "Problem encountered during import.");
					e.printStackTrace();
				}
				return null;*/
			}
			
			@Override
			public void done() {
				dialog.setVisible(false);
			}
		}
		
		(new RunImport()).execute();
		dialog.pack();
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);
		
		JOptionPane.showMessageDialog(null, "Import complete!");
		dm.refreshBrowser();
	}
	
	/**
	 * Extracts the bytes from the entry and with them creates or overwrites the file given by tempDest
	 * @param input
	 * @param entry
	 * @param tempDest
	 * @return the same path passed, which now points to the written bytes
	 * @throws IOException
	 */
	private static Path copyEntryToTempFile(TarArchiveInputStream input, TarArchiveEntry entry, Path tempDest) 
			throws IOException {
		byte[] entryBytes = new byte[(int)entry.getSize()];
		input.read(entryBytes, 0, entryBytes.length);
		return Files.write(tempDest, entryBytes);
	}
	
	private static JDialog createProgressDialog() {
		JDialog dialog = new JDialog((Frame)null, true);
		Object[] options = {};
		JOptionPane optionPane = new JOptionPane("Importing...", 
				JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null, options);
		dialog.setContentPane(optionPane);
		dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		return dialog;
	}

	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
			throws IOException {
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
			throws IOException {
		if( file.toString().endsWith(".card") ){
			Card card = Card.parseFile(dm, file.toFile(), true); //load external card into memory
			card.writeToFile(); //actually write new card to file at ~/Atlas/
			
			if( copyImage(card.imageFilename) == false )
				//log failure
				failedImageCopies.add(card.imageFilename);
		}
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
}

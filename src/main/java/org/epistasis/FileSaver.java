package org.epistasis;

import java.awt.Component;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import org.epistasis.combinatoric.mdr.gui.Frame;

public class FileSaver {
	private static File fSaveFolder = new File("");
	public static final ExtensionFilter epsFilter = new ExtensionFilter("eps",
			"Encapsulated Postscript Images (*.eps)");
	public static final ExtensionFilter txtFilter = new ExtensionFilter("txt",
			"Text Files (*.txt)");
	public static final ExtensionFilter jpgFilter = new ExtensionFilter("jpg",
			"JPEG Images (*.jpg)");
	public static final ExtensionFilter pngFilter = new ExtensionFilter("png",
			"PNG Images (*.png)");
	public static final List<? extends FileFilter> fltGraphics = Collections
			.unmodifiableList(Arrays.asList(FileSaver.jpgFilter, FileSaver.pngFilter,
					FileSaver.epsFilter));
	public static final List<? extends FileFilter> fltText = Collections
			.unmodifiableList(Arrays.asList(
					new JFileChooser().getAcceptAllFileFilter(), FileSaver.txtFilter));

	public static Pair<File, FileFilter> getSaveFile(final Component parent,
			final String title, final List<? extends FileFilter> filters) {
		File f = null;
		boolean overwrite = false;
		// configure the file chooser
		Frame.fileChooser.setDialogTitle(title);
		Frame.fileChooser.setMultiSelectionEnabled(false);
		Frame.fileChooser.setCurrentDirectory(FileSaver.fSaveFolder);
		if ((filters != null) && !filters.isEmpty()) {
			for (final FileFilter fileFilter : filters) {
				Frame.fileChooser.addChoosableFileFilter(fileFilter);
			}
			Frame.fileChooser.removeChoosableFileFilter(Frame.fileChooser
					.getAcceptAllFileFilter());
		}
		// keep asking until we get a satisfactory answer, which could be
		// one of: cancel, write a new file, or a confirmed overwrite
		while ((f == null) || (f.exists() && !overwrite)) {
			// ask for which file to write, and if the user cancels,
			// we're done
			if (Frame.fileChooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) {
				return null;
			}
			// get the user's selection
			f = Frame.fileChooser.getSelectedFile();
			// get the selected file filter
			final FileFilter filter = Frame.fileChooser.getFileFilter();
			// adjust file extension if necessary
			if ((filter != null) && (filter instanceof ExtensionFilter)) {
				final ExtensionFilter extFilter = (ExtensionFilter) filter;
				final String name = f.getAbsolutePath();
				final int pos = name.lastIndexOf('.');
				if (pos < 0) {
					f = new File(name + '.' + extFilter.getExtension());
				}
			}
			// if the file the user chose exists, ask if the file should
			// be overwritten
			if (f.exists()) {
				switch (JOptionPane.showConfirmDialog(parent, "File '" + f.toString()
						+ "' exists.  Overwrite?", title, JOptionPane.YES_NO_CANCEL_OPTION)) {
					// user chose yes, so write the file
					case JOptionPane.YES_OPTION:
						overwrite = true;
						break;
					// user chose no, so ask again what file to write
					case JOptionPane.NO_OPTION:
						overwrite = false;
						break;
					// user chose cancel, so we're done
					case JOptionPane.CANCEL_OPTION:
						return null;
				}
			}
		}
		// keep track of where to save these files
		FileSaver.fSaveFolder = f.getParentFile();
		return new Pair<File, FileFilter>(f, Frame.fileChooser.getFileFilter());
	}

	public static File getSaveFolder() {
		return FileSaver.fSaveFolder;
	}

	/**
	 * Open a PrintWriter to a file. This function opens a file chooser dialog to ask the user the filename to use.
	 * @param parent The parent window for the dialog
	 * @param title The title for the dialog
	 * @return PrintWriter to a file, or null if user cancelled
	 */
	public static PrintWriter openFileWriter(final Component parent,
			final String title) {
		return FileSaver.openFileWriter(parent, title, null);
	}

	/**
	 * Open a PrintWriter to a file. This function opens a file chooser dialog to ask the user the filename to use.
	 * @param parent The parent window for the dialog
	 * @param title The title for the dialog
	 * @param filters FileFilters to add to the dialog
	 * @return PrintWriter to a file, or null if user cancelled
	 */
	public static PrintWriter openFileWriter(final Component parent,
			final String title, final List<? extends FileFilter> filters) {
		final Pair<File, FileFilter> ff = FileSaver.getSaveFile(parent, title,
				filters);
		if (ff == null) {
			return null;
		}
		// open the file, and display any errors
		try {
			return new PrintWriter(new OutputStreamWriter(new FileOutputStream(
					ff.getFirst()), "UTF-8"));
		} catch (final IOException e) {
			JOptionPane.showMessageDialog(parent, e.getMessage(), "I/O Error",
					JOptionPane.ERROR_MESSAGE);
		}
		return null;
	}

	public static void saveText(final String text, final File file)
			throws IOException {
		final PrintWriter p = new PrintWriter(new OutputStreamWriter(
				new FileOutputStream(file), "UTF-8"));
		FileSaver.saveText(text, p);
		p.flush();
		p.close();
	}

	public static void saveText(final String text, final PrintWriter p)
			throws IOException {
		final BufferedReader r = new BufferedReader(new StringReader(text));
		String line;
		while ((line = r.readLine()) != null) {
			p.println(line);
		}
	}

	public static void saveText(final String text, final Writer w)
			throws IOException {
		final PrintWriter p = new PrintWriter(w);
		FileSaver.saveText(text, p);
		p.flush();
		p.close();
	}

	public static void setSaveFolder(final File fSaveFolder) {
		FileSaver.fSaveFolder = fSaveFolder;
	}

	public static class ExtensionFilter extends FileFilter {
		private final String extension;
		private final String description;

		public ExtensionFilter(final String extension, final String description) {
			this.extension = extension;
			this.description = description;
		}

		@Override
		public boolean accept(final File pathname) {
			return pathname.isDirectory()
					|| pathname.getName().endsWith('.' + extension);
		}

		@Override
		public String getDescription() {
			return description;
		}

		public String getExtension() {
			return extension;
		}
	}
}

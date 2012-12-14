package org.epistasis.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class SwingPropUtils {
	private static final File propsFile = new File(
			System.getProperty("java.home") + File.separator + "lib" + File.separator
					+ "swing.properties");
	private static Properties props = null;

	public static Properties getProperties() {
		SwingPropUtils.initProperties();
		return SwingPropUtils.props;
	}

	public static String getProperty(final String key) {
		String ret = null;
		if ((ret = System.getProperty(key)) != null) {
			return ret;
		}
		if ((ret = SwingPropUtils.getProperties().getProperty(key)) != null) {
			return ret;
		}
		return null;
	}

	private static void initProperties() {
		if (SwingPropUtils.props != null) {
			return;
		}
		SwingPropUtils.props = new Properties();
		if (!SwingPropUtils.propsFile.exists()
				|| !SwingPropUtils.propsFile.canRead()) {
			return;
		}
		try {
			SwingPropUtils.props.load(new FileInputStream(SwingPropUtils.propsFile));
		} catch (final IOException e) {
			// shouldn't happen, and if it does, no need to do anything
		}
	}

	public static String setProperty(final String key, final String value) {
		return SwingPropUtils.setProperty(key, value, true);
	}

	public static String setProperty(final String key, final String value,
			final boolean force) {
		final String old = SwingPropUtils.getProperty(key);
		if ((old != null) && !force) {
			return null;
		}
		System.setProperty(key, value);
		SwingPropUtils.initProperties();
		SwingPropUtils.props.remove(key);
		return old;
	}

	public static void useSystemLookAndFeel() {
		SwingPropUtils.useSystemLookAndFeel(true);
	}

	public static void useSystemLookAndFeel(final boolean force) {
		final String defaultlaf = SwingPropUtils.getProperty("swing.defaultlaf");
		if (force || (defaultlaf == null)) {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (final ClassNotFoundException ex) {
				// TODO Auto-generated catch block
				ex.printStackTrace();
			} catch (final InstantiationException ex) {
				// TODO Auto-generated catch block
				ex.printStackTrace();
			} catch (final IllegalAccessException ex) {
				// TODO Auto-generated catch block
				ex.printStackTrace();
			} catch (final UnsupportedLookAndFeelException ex) {
				// TODO Auto-generated catch block
				ex.printStackTrace();
			}
		}
	}
}

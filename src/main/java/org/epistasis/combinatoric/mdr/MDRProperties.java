/*
 * EvaProperties.java Created on February 17, 2006, 5:15 PM To change this template,
 * choose Tools | Template Manager and open the template
 * in the editor.
 */
package org.epistasis.combinatoric.mdr;

import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.Properties;

import org.epistasis.WrappedProperties;

/**
 * @author pandrews
 */
public abstract class MDRProperties
    {
    private static WrappedProperties props;
    private static String propertyFilePath = "mdr.properties";

    /*
      * First try to load EVA.properties from same folder as the executable or jar. Second,
      * get it as a resource file, presumably packed within
      * the EVA
      */
    static
        {
        try
            {
            MDRProperties.props = new WrappedProperties(
                    MDRProperties.propertyFilePath);
            }
        catch (final IOException ex)
            {
            System.err.println("MDRProperties static initializer caught exception: "
                               + ex);
            System.exit(1);
            }
        }

    public static void addListener(final PropertyChangeListener listener)
        {
        MDRProperties.props.addListener(listener);
        }

    public static void addListener(final String propertyName,
                                   final PropertyChangeListener listener)
        {
        MDRProperties.props.addListener(propertyName, listener);
        }

    public static boolean containsKey(final String propertyName)
        {
        return MDRProperties.props.containsKey(propertyName);
        }

    public static void fireInitialPropertyChangeValue(final String key)
        {
        MDRProperties.props.fireInitialPropertyChangeValue(key);
        }

    public static String get(final String propertyName)
        {
        return MDRProperties.props.get(propertyName);
        }

    public static boolean getBoolean(final String propertyName)
        {
        return MDRProperties.props.getBoolean(propertyName);
        }

    public static float getFloat(final String propertyName)
        {
        return MDRProperties.props.getFloat(propertyName);
        }

    public static int getInt(final String propertyName)
        {
        return MDRProperties.props.getInt(propertyName);
        }

    public static int[] getInts(final String propertyName)
        {
        return MDRProperties.props.getInts(propertyName);
        }

    public static Properties getProperties()
        {
        return MDRProperties.props.getProperties();
        }

    public static String[] getStrings(final String propertyName)
        {
        return MDRProperties.props.getStrings(propertyName);
        }

    public static void removeListener(final PropertyChangeListener listener)
        {
        MDRProperties.props.removeListener(listener);
        }

    public static void set(final String propertyName, final String newValue)
        {
        MDRProperties.props.set(propertyName, newValue);
        }

    public static void setBoolean(final String propertyName,
                                  final boolean newValue)
        {
        MDRProperties.props.setBoolean(propertyName, newValue);
        }

    public static void setFloat(final String propertyName, final float newValue)
        {
        MDRProperties.props.setFloat(propertyName, newValue);
        }

    public static void setInt(final String propertyName, final int newValue)
        {
        MDRProperties.props.setInt(propertyName, newValue);
        }

    public static void setInts(final String propertyName, final int[] integers)
        {
        MDRProperties.props.setInts(propertyName, integers);
        }

    public float[] getFloats(final String propertyName)
        {
        return MDRProperties.props.getFloats(propertyName);
        }
    }

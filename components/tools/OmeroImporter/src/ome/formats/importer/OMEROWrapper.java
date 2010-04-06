/*
 * ome.formats.importer.gui.GuiCommonElements
 *
 *------------------------------------------------------------------------------
 *  Copyright (C) 2006-2008 University of Dundee. All rights reserved.
 *
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *------------------------------------------------------------------------------
 */

package ome.formats.importer;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import loci.formats.ChannelFiller;
import loci.formats.ChannelSeparator;
import loci.formats.ClassList;
import loci.formats.FormatException;
import loci.formats.FormatTools;
import loci.formats.IFormatReader;
import loci.formats.ImageReader;
import loci.formats.MinMaxCalculator;
import loci.formats.in.LeicaReader;
import loci.formats.in.MetadataLevel;
import loci.formats.in.MetadataOptions;
import loci.formats.meta.MetadataStore;
import omero.model.Channel;
import omero.model.Pixels;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Brian Loranger brain at lifesci.dundee.ac.uk
 * @author Chris Allan callan at blackcat.ca
 *
 */
public class OMEROWrapper extends MinMaxCalculator {

    @SuppressWarnings("unused")
    private final static Log log = LogFactory.getLog(OMEROWrapper.class);

    private ChannelSeparator separator;
    private ChannelFiller filler;
    public Boolean minMaxSet = null;

    /**
     * Reference copy of <i>reader</i> so that we can be compatible with the
     * IFormatReader/ReaderWrapper interface but still maintain functionality
     * that we require.
     */
    private ImageReader iReader;

    /**
     * Wrapper for bio-formats
     * 
     * @param config - ImportConfigÂ
     */
    public OMEROWrapper(ImportConfig config)
    {
        if (config == null) {
            throw new IllegalArgumentException("An ImportConfig must be instantitated \n " +
            		"in order to properly configure all readers.");
        }
        try
        {
            String readers = config.readersPath.get();
            Class<?> k = getClass();
            if (new File(readers).exists()) {
                k = null;
            }
            iReader = new ImageReader(new ClassList(readers, IFormatReader.class, k));
            
//            // Now we apply the invocation handler
//            iReader =  (ImageReader) Proxy.newProxyInstance(
//                    getClass().getClassLoader(),
//                    new Class[]{IFormatReader.class},
//                    new ReaderInvocationHandler(iReader));
            
            filler = new ChannelFiller(iReader);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Unable to load readers.txt.");
        }
        reader = separator  = new ChannelSeparator(filler);
        //reader = separator = new ChannelSeparator(iReader);
        
        // Force unreadable characters to be removed from metadata key/value pairs 
        iReader.setMetadataFiltered(true);
        filler.setMetadataFiltered(true);
        separator.setMetadataFiltered(true);
    };

    /**
     * Obtains an object which represents a given sub-image of a plane within
     * the file.
     * @param id The path to the file.
     * @param planeNumber The plane or section within the file to obtain.
     * @param buf Pre-allocated buffer which has a <i>length</i> that can fit
     * the entire plane.
     * @return an object which represents the plane.
     * @throws FormatException If there is an error parsing the file.
     * @throws IOException If there is an error reading from the file or
     * acquiring permissions to read the file.
     */
    public Plane2D openPlane2D(String id, int planeNumber, byte[] buf)
            throws FormatException, IOException
    {
        return openPlane2D(id, planeNumber, buf, 0, 0, getSizeX(), getSizeY());
    }

    /**
     * Obtains an object which represents a given sub-image of a plane within
     * the file.
     * @param id The path to the file.
     * @param planeNumber The plane or section within the file to obtain.
     * @param buf Pre-allocated buffer which has a <i>length</i> that can fit
     * the byte count of the sub-image.
     * @param x X coordinate of the upper-left corner of the sub-image
     * @param y Y coordinate of the upper-left corner of the sub-image
     * @param w width of the sub-image
     * @param h height of the sub-image
     * @return an object which represents the sub-image of the plane.
     * @throws FormatException If there is an error parsing the file.
     * @throws IOException If there is an error reading from the file or
     * acquiring permissions to read the file.
     */
    public Plane2D openPlane2D(String id, int planeNumber, byte[] buf,
                               int x, int y, int w, int h)
            throws FormatException, IOException
    {
        // FIXME: HACK! The ChannelSeparator isn't exactly what one would call
        // "complete" so we have to work around the fact that it still copies
        // all of the plane data (all three channels) from the file if the file
        // is RGB.
        ByteBuffer plane;
        if (iReader.isRGB() || isLeicaReader()) {
            // System.err.println("RGB, not using cached buffer.");
            byte[] bytePlane = openBytes(planeNumber, x, y, w, h);
            plane = ByteBuffer.wrap(bytePlane);
        } else {
            // System.err.println("Not RGB, using cached buffer.");
            plane = ByteBuffer.wrap(openBytes(planeNumber, buf, x, y, w, h));
        }
        return new Plane2D(plane, getPixelType(), isLittleEndian(), getSizeX(),
                getSizeY());
    }

    /**
     * @return true if reader being used is LeicaReader
     */
    public boolean isLeicaReader() {
        if (iReader.getReader() instanceof LeicaReader) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * @return true if min-max is set
     * @throws FormatException
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    public boolean isMinMaxSet() throws FormatException, IOException {
        if (minMaxSet == null) {
            MetadataStore store = reader.getMetadataStore();
            int series = reader.getSeries();
            List<Pixels> pixels = (List<Pixels>) store.getRoot();
            Pixels p = pixels.get(series);
            Channel c = p.getChannel(p.getSizeC().getValue() - 1);
            if (c.getStatsInfo() == null) {
                minMaxSet = false;
            } else {
                minMaxSet = true;
            }
        }
        return minMaxSet;
    }

    /* (non-Javadoc)
     * @see loci.formats.MinMaxCalculator#updateMinMax(byte[], int)
     */
    @Override
    protected void updateMinMax(byte[] b, int ndx) throws FormatException,
            IOException {
        if (isMinMaxSet() == false)
            super.updateMinMax(b, ndx);
    }

    /* (non-Javadoc)
     * @see loci.formats.ReaderWrapper#close()
     */
    public void close() throws IOException {
        minMaxSet = null;
        super.close(false);
    }

    /**
     * Return the base image reader
     * 
     * @return See above.
     */
    public ImageReader getImageReader() {
        return iReader;
    }
    
    /**
     * @return true if using SPW reader
     */
    public boolean isSPWReader()
    {
        String[] domains = reader.getDomains();
        return Arrays.asList(domains).contains(FormatTools.HCS_DOMAIN);
    }

    /* (non-Javadoc)
     * @see loci.formats.ReaderWrapper#getMetadataOptions()
     */
    @Override
    public MetadataOptions getMetadataOptions()
    {
        return iReader.getMetadataOptions();
    }

    /* (non-Javadoc)
     * @see loci.formats.ReaderWrapper#setMetadataOptions(loci.formats.in.MetadataOptions)
     */
    @Override
    public void setMetadataOptions(MetadataOptions options)
    {
        iReader.setMetadataOptions(options);
    }

    /* (non-Javadoc)
     * @see loci.formats.ReaderWrapper#getSupportedMetadataLevels()
     */
    @Override
    public Set<MetadataLevel> getSupportedMetadataLevels()
    {
        return iReader.getSupportedMetadataLevels();
    }
}

/**
 * @author Chris Allan
 *
 */
class ReaderInvocationHandler implements InvocationHandler {

    private final IFormatReader reader;

    public ReaderInvocationHandler(IFormatReader reader) {
        reader.toString(); // NPE
        this.reader = reader;
    }

    public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {
        if ("equals".equals(method.getName())) {
            throw new UnsupportedOperationException();
        } else if ("hashCode".equals(method.getName())) {
            return new Integer(reader.hashCode());
        } else if ("toString".equals(method.getName())) {
            return "ReaderHandler [" + reader + "]";
        } else {
            try {
                return method.invoke(proxy, args);
            } catch (Exception e) {
                throw new FormatException(e);
            }
        }
    }
}

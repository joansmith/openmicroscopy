/*
 * org.openmicroscopy.shoola.env.data.PixelsService
 *
 *------------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 Open Microscopy Environment
 *      Massachusetts Institute of Technology,
 *      National Institutes of Health,
 *      University of Dundee
 *
 *
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation; either
 *    version 2.1 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public
 *    License along with this library; if not, write to the Free Software
 *    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *------------------------------------------------------------------------------
 */




/*------------------------------------------------------------------------------
 *
 * Written by:    Douglas Creager <dcreager@alum.mit.edu>
 *
 *------------------------------------------------------------------------------
 */




package org.openmicroscopy.shoola.env.data;

import java.io.File;
import java.io.FileNotFoundException;

import java.awt.Image;

import org.openmicroscopy.is.ImageServerException;
import org.openmicroscopy.is.PixelsFileFormat;
import org.openmicroscopy.is.PlaneStatistics;
import org.openmicroscopy.is.StackStatistics;
import org.openmicroscopy.is.CompositingSettings;
import org.openmicroscopy.ds.st.Pixels;
import org.openmicroscopy.ds.st.Repository;

/**
 * Heyo
 */

public interface PixelsService
{
    /**
     * <p>Creates a new pixels file on the image server.  The
     * dimensions of the pixels must be known beforehand, and must be
     * specified to this method.  All of the dimensions must be
     * positive integers.</p>
     *
     * <p>Each pixel in the new file must have the same storage type.
     * This type is specified by the <code>bytesPerPixel</code>,
     * <code>isSigned</code>, and <code>isFloat</code> parameters.</p>
     *
     * <p>This method returns the pixels ID generated by the image
     * server.  Note that this is not the same as the attribute ID of
     * a Pixels attribute.  The pixels ID can be used in the
     * <code>get*</code>, <code>set*</code>, and <code>convert*</code>
     * methods (among others) to perform pixel I/O with the image
     * server.</p>
     *
     * <p>The new pixels file will be created in write-only mode.  The
     * <code>set*</code> and <code>convert*</code> methods should be
     * used to populate the pixel array.  Once the array is fully
     * populated, the <code>finishPixels</code> method should be used
     * to place the file in read-only mode.  At this point, the
     * <code>get*</code> methods can be called to retrieve the
     * pixels.</p>
     *
     * @param sizeX the size (in pixels) of the image's X dimension
     * @param sizeY the size (in pixels) of the image's Y dimension
     * @param sizeZ the size (in pixels) of the image's Z dimension
     * @param sizeC the size (in pixels) of the image's C dimension
     * @param sizeT the size (in pixels) of the image's T dimension
     * @param bytesPerPixel the number of bytes used to store a single
     * pixel
     * @param isSigned whether the value of each pixel is signed or
     * not
     * @param isFloat whether the value of each pixel is a float or an
     * integer
     * @return the pixel ID of the new pixels file
     * @throws ImageServerException if there was an error connecting
     * to the image server or creating the pixels file
     */
    public Pixels newPixels(int sizeX,
                            int sizeY,
                            int sizeZ,
                            int sizeC,
                            int sizeT,
                            int bytesPerPixel,
                            boolean isSigned,
                            boolean isFloat)
        throws ImageServerException;

    /**
     * <p>Creates a new pixels file on the image server.  The
     * dimensions of the pixels must be known beforehand, and must be
     * specified to this method.  All of the dimensions must be
     * positive integers.  Each pixel in the new file must have the
     * same storage type.  This information is specified by the
     * <code>pixelsFileFormat</code> parameter.</p>
     *
     * <p>This method returns the pixels ID generated by the image
     * server.  Note that this is not the same as the attribute ID of
     * a Pixels attribute.  The pixels ID can be used in the
     * <code>get*</code>, <code>set*</code>, and <code>convert*</code>
     * methods (among others) to perform pixel I/O with the image
     * server.</p>
     *
     * <p>The new pixels file will be created in write-only mode.  The
     * <code>set*</code> and <code>convert*</code> methods should be
     * used to populate the pixel array.  Once the array is fully
     * populated, the <code>finishPixels</code> method should be used
     * to place the file in read-only mode.  At this point, the
     * <code>get*</code> methods can be called to retrieve the
     * pixels.</p>
     *
     * @param pixelsFileFormat the format of the new pixels file
     * @return the pixel ID of the new pixels file
     * @throws ImageServerException if there was an error connecting
     * to the image server or creating the pixels file
     */
    public Pixels newPixels(final PixelsFileFormat format)
        throws ImageServerException;

    /**
     * Returns the properties of a previously created pixels file.
     * @param pixelsID the pixels ID of a previously created pixels
     * file
     * @return a {@link PixelsFileFormat} object encoding the
     * properties of the pixels file
     * @throws ImageServerException if there was an error contacting
     * the image server or if the pixels ID does not exist
     */
    public PixelsFileFormat getPixelsInfo(Pixels pixels)
        throws ImageServerException;

    /**
     * Returns the SHA-1 digest of a previously created pixels file.
     * @param pixelsID the pixels ID of a previously created pixels
     * file
     * @return the SHA-1 digest of the pixels file
     * @throws ImageServerException if there was an error contacting
     * the image server or if the pixels ID does not exist
     */
    public String getPixelsSHA1(Pixels pixels)
        throws ImageServerException;

    /**
     * Returns the location of the specified pixels file in the image
     * server's filesystem.
     * @param pixelsID the pixels ID of a previously created pixels
     * file
     * @return the image-server-local path to the pixels file
     * @throws ImageServerException if there was an error contacting
     * the image server or if the pixels ID does not exist
     */
    public String getPixelsServerPath(Pixels pixels)
        throws ImageServerException;

    /**
     * Returns whether the <code>finishPixels</code> method has been
     * called on the specified pixels file.
     * @param pixelsID the pixels ID of a previously created pixels
     * file
     * @return whether the <code>finishPixels</code> method has been
     * called on the specified pixels file
     * @throws ImageServerException if there was an error contacting
     * the image server or if the pixels ID does not exist
     */
    public boolean isPixelsFinished(Pixels pixels)
        throws ImageServerException;

    /**
     * <p>This method returns the entire pixel file for the given
     * pixel ID.  Be very careful, these can easily be friggin-huge in
     * size.  You probably don't ever want to call this method, unless
     * you've made the appropriate checks on the dimensions of the
     * pixels to ensure that it won't be too large.</p>
     *
     * <p>You must also specify whether you want to receive a
     * big-endian or little-endian pixel array.  The image server will
     * take care of performing the appropriate conversion for you.</p>
     *
     * @param pixelsID the pixels ID of a previously created pixels
     * file
     * @param bigEndian whether the returns pixels should be in
     * big-endian order
     * @result an array of pixels for the specified file
     * @throws ImageServerException if there was an error contacting
     * the image server or if the pixels ID does not exist or is not
     * readable
     */
    public byte[] getPixels(Pixels pixels,
                            boolean bigEndian)
        throws ImageServerException;

    /**
     * <p>This method returns a pixel array of the specified stack.
     * The stack is specified by its C and T coordinates, which have
     * 0-based indices.  While this method is less likely to be a
     * memory hog as the {@link getPixels} method, it is still
     * possible for large images with few timepoints to cause
     * problems.  As usual when dealing with large images, care must
     * be taken to use your computational resources appropriately.</p>
     *
     * <p>You must also specify whether you want to receive a
     * big-endian or little-endian pixel array.  The image server will
     * take care of performing the appropriate conversion for you.</p>
     *
     * @param pixelsID the pixels ID of a previously created pixels
     * file
     * @param theC the C parameter of the desired stack
     * @param theT the T parameter of the desired stack
     * @param bigEndian whether the returns pixels should be in
     * big-endian order
     * @result an array of pixels for the specified stack
     * @throws ImageServerException if there was an error contacting
     * the image server or if the pixels ID does not exist or is not
     * readable
     */
    public byte[] getStack(Pixels pixels,
                           int theC, int theT,
                           boolean bigEndian)
        throws ImageServerException;

    /**
     * <p>This method returns a pixel array of the specified plane.
     * The plane is specified by its Z, C and T coordinates, which
     * have 0-based indices.  While this method is the least likely to
     * be a memory hog, care must still be taken to use your
     * computational resources appropriately.</p>
     *
     * <p>You must also specify whether you want to receive a
     * big-endian or little-endian pixel array.  The image server will
     * take care of performing the appropriate conversion for you.</p>
     *
     * @param pixelsID the pixels ID of a previously created pixels
     * file
     * @param theZ the C parameter of the desired plane
     * @param theC the C parameter of the desired plane
     * @param theT the T parameter of the desired plane
     * @param bigEndian whether the returns pixels should be in
     * big-endian order
     * @result an array of pixels for the specified plane
     * @throws ImageServerException if there was an error contacting
     * the image server or if the pixels ID does not exist or is not
     * readable
     */
    public byte[] getPlane(Pixels pixels,
                           int theZ, int theC, int theT,
                           boolean bigEndian)
        throws ImageServerException;

    /**
     * <p>This method returns a pixel array of an arbitrary
     * hyper-rectangular region of an image.  The region is specified
     * by two coordinate vectors, which have 0-based indices.  The
     * region boundaries must be well formed, and are inclusive.
     * (Each coordinate must be within the range of valid values for
     * that dimension, and each "0" coordinate must be less than or
     * equal to the respective "1" coordinate.)</p>
     *
     * <p>You must also specify whether you want to receive a
     * big-endian or little-endian pixel array.  The image server will
     * take care of performing the appropriate conversion for you.</p>
     *
     * @param pixelsID the pixels ID of a previously created pixels
     * file
     * @param bigEndian whether the returns pixels should be in
     * big-endian order
     * @result an array of pixels for the specified region
     * @throws ImageServerException if there was an error contacting
     * the image server or if the pixels ID does not exist or is not
     * readable
     */
    public byte[] getROI(Pixels pixels,
                         int x0,int y0,int z0,int c0,int t0,
                         int x1,int y1,int z1,int c1,int t1,
                         boolean bigEndian)
        throws ImageServerException;

    /**
     * <p>This method sends an entire array of pixels for the given
     * pixels ID.  The pixels are specified by a byte array, which
     * should be a raw pixel dump.  The endian-ness of the pixels
     * should be specified.</p>
     *
     * <p>It is up to the caller to ensure that the byte array is of
     * the correct size.</p>
     *
     * @param pixelsID the pixels ID of a previously created pixels
     * file
     * @param buf an array of pixels
     * @param bigEndian whether the returns pixels should be in
     * big-endian order
     * @throws ImageServerException if there was an error contacting
     * the image server or if the pixels ID does not exist or is not
     * writeable
     */
    public void setPixels(Pixels pixels,
                          byte[] buf,
                          boolean bigEndian)
        throws ImageServerException;

    /**
     * <p>This method sends an entire array of pixels for the given
     * pixels ID.  The pixels are specified by a local file, which
     * should be a raw pixel dump.  The endian-ness of the pixels
     * should be specified.</p>
     *
     * <p>It is up to the caller to ensure that the file is of the
     * correct size.</p>
     *
     * @param pixelsID the pixels ID of a previously created pixels
     * file
     * @param file a file containing a pixel dump
     * @param bigEndian whether the returns pixels should be in
     * big-endian order
     * @throws ImageServerException if there was an error contacting
     * the image server or if the pixels ID does not exist or is not
     * writeable
     * @throws FileNotFoundException if the specified file cannot be
     * read
     */
    public void setPixels(Pixels pixels,
                          File file,
                          boolean bigEndian)
        throws ImageServerException, FileNotFoundException;

    /**
     * <p>This method sends a stack of pixels for the given pixels ID.
     * The pixels are specified by a byte array, which should be a raw
     * pixel dump.  The endian-ness of the pixels should be
     * specified.</p>
     *
     * <p>It is up to the caller to ensure that the byte array is of
     * the correct size.</p>
     *
     * @param pixelsID the pixels ID of a previously created pixels
     * file
     * @param theC the C parameter of the desired stack
     * @param theT the T parameter of the desired stack
     * @param buf an array of pixels
     * @param bigEndian whether the returns pixels should be in
     * big-endian order
     * @throws ImageServerException if there was an error contacting
     * the image server or if the pixels ID does not exist or is not
     * writeable
     */
    public void setStack(Pixels pixels,
                         int theC, int theT,
                         byte[] buf,
                         boolean bigEndian)
        throws ImageServerException;

    /**
     * <p>This method sends a stack of pixels for the given pixels ID.
     * The pixels are specified by a local file, which should be a raw
     * pixel dump.  The endian-ness of the pixels should be
     * specified.</p>
     *
     * <p>It is up to the caller to ensure that the file is of the
     * correct size.</p>
     *
     * @param pixelsID the pixels ID of a previously created pixels
     * file
     * @param theC the C parameter of the desired stack
     * @param theT the T parameter of the desired stack
     * @param file a file containing a pixel dump
     * @param bigEndian whether the returns pixels should be in
     * big-endian order
     * @throws ImageServerException if there was an error contacting
     * the image server or if the pixels ID does not exist or is not
     * writeable
     * @throws FileNotFoundException if the specified file cannot be
     * read
     */
    public void setStack(Pixels pixels,
                         int theC, int theT,
                         File file,
                         boolean bigEndian)
        throws ImageServerException, FileNotFoundException;

    /**
     * <p>This method sends a plane of pixels for the given pixels ID.
     * The pixels are specified by a byte array, which should be a raw
     * pixel dump.  The endian-ness of the pixels should be
     * specified.</p>
     *
     * <p>It is up to the caller to ensure that the byte array is of
     * the correct size.</p>
     *
     * @param pixelsID the pixels ID of a previously created pixels
     * file
     * @param theZ the Z parameter of the desired plane
     * @param theC the C parameter of the desired plane
     * @param theT the T parameter of the desired plane
     * @param buf an array of pixels
     * @param bigEndian whether the returns pixels should be in
     * big-endian order
     * @throws ImageServerException if there was an error contacting
     * the image server or if the pixels ID does not exist or is not
     * writeable
     */
    public void setPlane(Pixels pixels,
                         int theZ, int theC, int theT,
                         byte[] buf,
                         boolean bigEndian)
        throws ImageServerException;

    /**
     * <p>This method sends a plane of pixels for the given pixels ID.
     * The pixels are specified by a local file, which should be a raw
     * pixel dump.  The endian-ness of the pixels should be
     * specified.</p>
     *
     * <p>It is up to the caller to ensure that the file is of the
     * correct size.</p>
     *
     * @param pixelsID the pixels ID of a previously created pixels
     * file
     * @param theZ the Z parameter of the desired plane
     * @param theC the C parameter of the desired plane
     * @param theT the T parameter of the desired plane
     * @param file a file containing a pixel dump
     * @param bigEndian whether the returns pixels should be in
     * big-endian order
     * @throws ImageServerException if there was an error contacting
     * the image server or if the pixels ID does not exist or is not
     * writeable
     * @throws FileNotFoundException if the specified file cannot be
     * read
     */
    public void setPlane(Pixels pixels,
                         int theZ, int theC, int theT,
                         File file,
                         boolean bigEndian)
        throws ImageServerException, FileNotFoundException;

    /**
     * <p>This method sends an arbitrary region of pixels for the
     * given pixels ID.  The pixels are specified by a byte array,
     * which should be a raw pixel dump.  The endian-ness of the
     * pixels should be specified.</p>
     *
     * <p>It is up to the caller to ensure that the byte array is of
     * the correct size.</p>
     *
     * @param pixelsID the pixels ID of a previously created pixels
     * file
     * @param buf an array of pixels
     * @param bigEndian whether the returns pixels should be in
     * big-endian order
     * @throws ImageServerException if there was an error contacting
     * the image server or if the pixels ID does not exist or is not
     * writeable
     */
    public void setROI(Pixels pixels,
                       int x0,int y0,int z0,int c0,int t0,
                       int x1,int y1,int z1,int c1,int t1,
                       byte[] buf,
                       boolean bigEndian)
        throws ImageServerException;

    /**
     * <p>This method sends an arbitrary region of pixels for the
     * given pixels ID.  The pixels are specified by a local file,
     * which should be a raw pixel dump.  The endian-ness of the
     * pixels should be specified.</p>
     *
     * <p>It is up to the caller to ensure that the file is of the
     * correct size.</p>
     *
     * @param pixelsID the pixels ID of a previously created pixels
     * file
     * @param file a file containing a pixel dump
     * @param bigEndian whether the returns pixels should be in
     * big-endian order
     * @throws ImageServerException if there was an error contacting
     * the image server or if the pixels ID does not exist or is not
     * writeable
     * @throws FileNotFoundException if the specified file cannot be
     * read
     */
    public void setROI(Pixels pixels,
                       int x0,int y0,int z0,int c0,int t0,
                       int x1,int y1,int z1,int c1,int t1,
                       File file,
                       boolean bigEndian)
        throws ImageServerException, FileNotFoundException;

    /**
     * <p>This method ends the writable phase of the life of a pixels
     * file in the image server.</p>
     *
     * <p>Pixels files in the image server can only be written to
     * immediately after they are created.  Once the pixels file is
     * fully populated, it is marked as being completed (via this
     * method).  As this point, all of the writing methods
     * (<code>set*</code>, <code>convert*</code>) become unavailable
     * for the pixels file, and the reading methods
     * (<code>get*</code>) become available.</p>
     *
     * @param pixelsID the pixels ID of a previously created pixels
     * file
     * @throws ImageServerException if there was an error contacting
     * the image server or if the pixels ID does not exist or is not
     * writeable
     */
    public void finishPixels(Pixels pixels)
        throws ImageServerException;

    /**
     * Returns a {@link PlaneStatistics} object containing basic pixel
     * statistics for each plane in the specified pixels file.
     *
     * @param pixelsID the pixels ID of a previously created pixels
     * file
     */
    public PlaneStatistics getPlaneStatistics(Pixels pixels)
        throws ImageServerException;

    /**
     * Returns a {@link StackStatistics} object containing basic pixel
     * statistics for each stack in the specified pixels file.
     *
     * @param pixelsID the pixels ID of a previously created pixels
     * file
     */
    public StackStatistics getStackStatistics(Pixels pixels)
        throws ImageServerException;

    /**
     * Composites a single plane of a multi-channel image into a
     * grayscale or RGB image, according to the state of the
     * <code>settings</code> parameter.
     *
     * @param pixelsID the pixels ID of a previously created pixels
     * file
     * @param settings a {@link CompositingSettings} object describing
     * the compositing which should be performed
     * @return an AWT {@link Image} suitable for display
     */
    public abstract Image getComposite(Pixels pixels,
                                       CompositingSettings settings)
        throws ImageServerException;

    /**
     * Saves the specified compositing settings as a default.  This
     * allows the {@link #getThumbnail} method to be used to quickly
     * retrieve a standard compositing for an image.
     *
     * @param pixelsID the pixels ID of a previously created pixels
     * file
     * @param settings a {@link CompositingSettings} object describing
     * the compositing which should be saved
     */
    public abstract void setThumbnail(Pixels pixels,
                                      CompositingSettings settings)
        throws ImageServerException;

    /**
     * Returns a thumbnail for the specified image.  This thumbnail
     * must have been previously set by the {@link #setThumbnail}
     * method.
     *
     * @param pixelsID the pixels ID of a previously created pixels
     * file
     */
    public abstract Image getThumbnail(Pixels pixels)
        throws ImageServerException;
}
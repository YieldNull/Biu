package com.bbbbiu.biu.lib.httpd.upload;

import com.bbbbiu.biu.lib.httpd.util.FileItemHeaders;
import com.bbbbiu.biu.lib.httpd.util.Streams;


import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.output.DeferredFileOutputStream;


/**
 * <p> The default implementation of the
 * //{@link FileItem FileItem} interface.
 * <p/>
 * <p> After retrieving an instance of this class from a {@link
 * FileItemFactory} instance (see
 * #parseRequest(javax.servlet.http.HttpServletRequest)}), you may
 * either request all contents of file at once using {@link #get()} or
 * request an {@link InputStream InputStream} with
 * {@link #getInputStream()} and process the file without attempting to load
 * it into memory, which may come handy with large files.
 * <p/>
 * <p>Temporary files, which are created for file items, should be
 * deleted later on. The best way to do this is using a
 * //{@link org.apache.commons.io.FileCleaningTracker}, which you can set on the
 * {@link FileItemFactory}. However, if you do use such a tracker,
 * then you must consider the following: Temporary files are automatically
 * deleted as soon as they are no longer needed. (More precisely, when the
 * corresponding instance of {@link File} is garbage collected.)
 * This is done by the so-called reaper thread, which is started and stopped
 * automatically by the {@link org.apache.commons.io.FileCleaningTracker} when
 * there are files to be tracked.
 * It might make sense to terminate that thread, for example, if
 * your web application ends. See the section on "Resource cleanup"
 * in the users guide of commons-fileupload.</p>
 *
 * @version $Id: FileItem.java 1565192 2014-02-06 12:14:16Z markt $
 * @since FileUpload 1.1
 */
public class FileItem {

    // ----------------------------------------------------- Manifest constants

    /**
     * The UID to use when serializing this instance.
     */
    private static final long serialVersionUID = 2237570099615271025L;

    /**
     * Default content charset to be used when no explicit charset
     * parameter is provided by the sender. Media subtypes of the
     * "text" type are defined to have a default charset value of
     * "ISO-8859-1" when received via HTTP.
     */
    public static final String DEFAULT_CHARSET = "ISO-8859-1";

    // ----------------------------------------------------------- Data members

    /**
     * UID used in unique file name generation.
     */
    private static final String UID =
            UUID.randomUUID().toString().replace('-', '_');

    /**
     * Counter used in unique identifier generation.
     */
    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    /**
     * The name of the form field as provided by the browser.
     */
    private String fieldName;

    /**
     * The content type passed by the browser, or <code>null</code> if
     * not defined.
     */
    private final String contentType;

    /**
     * Whether or not this item is a simple form field.
     */
    private boolean isFormField;

    /**
     * The original filename in the user's filesystem.
     */
    private final String fileName;

    /**
     * The size of the item, in bytes. This is used to cache the size when a
     * file item is moved from its original location.
     */
    private long size = -1;


    /**
     * The threshold above which uploads will be stored on disk.
     */
    private final int sizeThreshold;

    /**
     * The directory in which uploaded files will be stored, if stored on disk.
     */
    private final File repository;

    /**
     * Cached contents of the file.
     */
    private byte[] cachedContent;

    /**
     * Output stream for this item.
     */
    private transient DeferredFileOutputStream dfos;

    /**
     * The temporary file to use.
     */
    private transient File tempFile;

    /**
     * File to allow for serialization of the content of this item.
     */
    private File dfosFile;

    /**
     * The file items headers.
     */
    private FileItemHeaders headers;

    // ----------------------------------------------------------- Constructors

    /**
     * Constructs a new <code>FileItem</code> instance.
     *
     * @param fieldName     The name of the form field.
     * @param contentType   The content type passed by the browser or
     *                      <code>null</code> if not specified.
     * @param isFormField   Whether or not this item is a plain form field, as
     *                      opposed to a file upload.
     * @param fileName      The original filename in the user's filesystem, or
     *                      <code>null</code> if not specified.
     * @param sizeThreshold The threshold, in bytes, below which items will be
     *                      retained in memory and above which they will be
     *                      stored as a file.
     * @param repository    The data repository, which is the directory in
     *                      which files will be created, should the item size
     *                      exceed the threshold.
     */
    public FileItem(String fieldName,
                    String contentType, boolean isFormField, String fileName,
                    int sizeThreshold, File repository) {
        this.fieldName = fieldName;
        this.contentType = contentType;
        this.isFormField = isFormField;
        this.fileName = fileName;
        this.sizeThreshold = sizeThreshold;
        this.repository = repository;
    }

    // ------------------------------- Methods from javax.activation.DataSource

    /**
     * Returns an {@link InputStream InputStream} that can be
     * used to retrieve the contents of the file.
     *
     * @return An {@link InputStream InputStream} that can be
     * used to retrieve the contents of the file.
     * @throws IOException if an error occurs.
     */
    public InputStream getInputStream()
            throws IOException {
        if (!isInMemory()) {
            return new FileInputStream(dfos.getFile());
        }

        if (cachedContent == null) {
            cachedContent = dfos.getData();
        }
        return new ByteArrayInputStream(cachedContent);
    }

    public String getContentType() {
        return contentType;
    }

    public String getCharSet() {
        ParameterParser parser = new ParameterParser();
        parser.setLowerCaseNames(true);

        // Parameter parser can handleRequest null input
        Map<String, String> params = parser.parse(getContentType(), ';');
        return params.get("charset");
    }

    public String getName() {
        return Streams.checkFileName(fileName);
    }

    // ------------------------------------------------------- FileItem methods

    /**
     * Provides a hint as to whether or not the file contents will be read
     * from memory.
     *
     * @return <code>true</code> if the file contents will be read
     * from memory; <code>false</code> otherwise.
     */
    public boolean isInMemory() {
        if (cachedContent != null) {
            return true;
        }
        return dfos.isInMemory();
    }

    /**
     * Returns the size of the file.
     *
     * @return The size of the file, in bytes.
     */
    public long getSize() {
        if (size >= 0) {
            return size;
        } else if (cachedContent != null) {
            return cachedContent.length;
        } else if (dfos.isInMemory()) {
            return dfos.getData().length;
        } else {
            return dfos.getFile().length();
        }
    }

    /**
     * Returns the contents of the file as an array of bytes.  If the
     * contents of the file were not yet cached in memory, they will be
     * loaded from the disk storage and cached.
     *
     * @return The contents of the file as an array of bytes.
     */
    public byte[] get() {
        if (isInMemory()) {
            if (cachedContent == null) {
                cachedContent = dfos.getData();
            }
            return cachedContent;
        }

        byte[] fileData = new byte[(int) getSize()];
        InputStream fis = null;

        try {
            fis = new BufferedInputStream(new FileInputStream(dfos.getFile()));
            fis.read(fileData);
        } catch (IOException e) {
            fileData = null;
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }

        return fileData;
    }

    /**
     * Deletes the underlying storage for a file item, including deleting any
     * associated temporary disk file. Although this storage will be deleted
     * automatically when the <code>FileItem</code> instance is garbage
     * collected, this method can be used to ensure that this is done at an
     * earlier time, thus preserving system resources.
     */
    public void delete() {
        cachedContent = null;
        File outputFile = getStoreLocation();
        if (outputFile != null && outputFile.exists()) {
            outputFile.delete();
        }
    }

    /**
     * Returns the name of the field in the multipart form corresponding to
     * this file item.
     *
     * @return The name of the form field.
     * @see #setFieldName(String)
     */
    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public boolean isFormField() {
        return isFormField;
    }

    public void setFormField(boolean state) {
        isFormField = state;
    }

    /**
     * Returns an {@link OutputStream OutputStream} that can
     * be used for storing the contents of the file.
     *
     * @return An {@link OutputStream OutputStream} that can be used
     * for storing the contensts of the file.
     * @throws IOException if an error occurs.
     */
    public OutputStream getOutputStream()
            throws IOException {
        if (dfos == null) {
            File outputFile = getTempFile();
            dfos = new DeferredFileOutputStream(sizeThreshold, outputFile);
        }
        return dfos;
    }

    // --------------------------------------------------------- Public methods

    /**
     * Returns the {@link File} object for the <code>FileItem</code>'s
     * data's temporary location on the disk. Note that for
     * <code>FileItem</code>s that have their data stored in memory,
     * this method will return <code>null</code>. When handling large
     * files, you can use {@link File#renameTo(File)} to
     * move the file to new location without copying the data, if the
     * source and destination locations reside within the same logical
     * volume.
     *
     * @return The data file, or <code>null</code> if the data is stored in
     * memory.
     */
    public File getStoreLocation() {
        if (dfos == null) {
            return null;
        }
        return dfos.getFile();
    }

    // ------------------------------------------------------ Protected methods

    /**
     * Creates and returns a {@link File File} representing a uniquely
     * named temporary file in the configured repository path. The lifetime of
     * the file is tied to the lifetime of the <code>FileItem</code> instance;
     * the file will be deleted when the instance is garbage collected.
     *
     * @return The {@link File File} to be used for temporary storage.
     */
    protected File getTempFile() {
        if (tempFile == null) {
            File tempDir = repository;
            if (tempDir == null) {
                tempDir = new File(System.getProperty("java.io.tmpdir"));
            }

//            String tempFileName = format("upload_%s_%s.tmp", UID, getUniqueId());

            //更改临时文件名,
            //TODO 有相同文件则在后面加数字
            String tempFileName = fileName;
            tempFile = new File(tempDir, tempFileName);
        }
        return tempFile;
    }

    /**
     * Returns a string representation of this object.
     *
     * @return a string representation of this object.
     */
    @Override
    public String toString() {
        return String.format("name=%s, StoreLocation=%s, size=%s bytes, isFormField=%s, FieldName=%s",
                getName(), getStoreLocation(), Long.valueOf(getSize()),
                Boolean.valueOf(isFormField()), getFieldName());
    }

    /**
     * Returns the file item headers.
     *
     * @return The file items headers.
     */
    public FileItemHeaders getHeaders() {
        return headers;
    }

    /**
     * Sets the file item headers.
     *
     * @param pHeaders The file items headers.
     */
    public void setHeaders(FileItemHeaders pHeaders) {
        headers = pHeaders;
    }

}

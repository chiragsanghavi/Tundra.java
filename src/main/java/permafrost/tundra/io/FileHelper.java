/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Lachlan Dowding
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package permafrost.tundra.io;

import com.wm.app.b2b.server.MimeTypes;
import com.wm.app.b2b.server.ServerAPI;
import com.wm.app.b2b.server.ServiceException;
import com.wm.data.IData;
import com.wm.data.IDataCursor;
import com.wm.data.IDataFactory;
import com.wm.data.IDataUtil;
import permafrost.tundra.data.IDataHelper;
import permafrost.tundra.io.filter.FilenameFilterType;
import permafrost.tundra.io.filter.InclusionFilenameFilter;
import permafrost.tundra.io.filter.RegularExpressionFilenameFilter;
import permafrost.tundra.io.filter.WildcardFilenameFilter;
import permafrost.tundra.lang.BooleanHelper;
import permafrost.tundra.lang.CharsetHelper;
import permafrost.tundra.lang.ExceptionHelper;
import permafrost.tundra.lang.StringHelper;
import permafrost.tundra.math.LongHelper;
import permafrost.tundra.mime.MIMETypeHelper;
import permafrost.tundra.net.uri.URIHelper;
import permafrost.tundra.server.ServiceHelper;
import permafrost.tundra.time.DateTimeHelper;
import javax.xml.datatype.Duration;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.Date;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * A collection of convenience methods for working with files.
 */
public final class FileHelper {
    /**
     * Disallow instantiation of this class.
     */
    private FileHelper() {}

    /**
     * @return True if the file system is case insensitive.
     */
    public static boolean isCaseInsensitive() {
        return (new File("TUNDRA")).equals(new File("tundra"));
    }

    /**
     * @return True if the file system is case sensitive.
     */
    public static boolean isCaseSensitive() {
        return !isCaseInsensitive();
    }

    /**
     * Returns the MIME media type that describes the content of the given file.
     *
     * @param file  The file whose MIME type is to be returned.
     * @return      The MIME media type that describes the content of the given file.
     */
    public static String getMIMEType(File file) {
        String type = null;
        if (file != null) type = MimeTypes.getTypeFromName(file.getName());
        if (type == null) type = MIMETypeHelper.DEFAULT_MIME_TYPE_STRING;
        return type;
    }

    /**
     * Returns the MIME media type that describes the content of the given file.
     *
     * @param filename  The file whose MIME type is to be returned.
     * @return          The MIME media type that describes the content of the given file.
     */
    public static String getMIMEType(String filename) {
        return getMIMEType(construct(filename));
    }

    /**
     * Returns true if the given file exists and is a file.
     *
     * @param file  The file to check existence of.
     * @return      True if the given file exists and is a file.
     */
    public static boolean exists(File file) {
        return file != null && file.exists() && file.isFile();
    }

    /**
     * Returns true if the given file exists and is a file.
     *
     * @param filename  The file to check existence of.
     * @return          True if the given file exists and is a file.
     */
    public static boolean exists(String filename) {
        return exists(construct(filename));
    }

    /**
     * Creates a new, empty temporary file.
     *
     * @return              The file which was created.
     * @throws IOException  If the file already exists.
     */
    public static File create() throws IOException {
        return create((File)null);
    }

    /**
     * Creates a new, empty file; if file is null, a temporary file is created.
     *
     * @param file          The file to be created.
     * @return              The file which was created.
     * @throws IOException  If the file already exists.
     */
    public static File create(File file) throws IOException {
        if (file == null) {
            file = File.createTempFile("tundra", null);
        } else {
            File parent = file.getParentFile();
            if (parent != null) parent.mkdirs(); // automatically create directories if required
            if (!file.createNewFile()) {
                throw new IOException("Unable to create file because it already exists: " + normalize(file));
            }
        }
        return file;
    }

    /**
     * Creates a new, empty file; if filename is null, a temporary file is created.
     *
     * @param filename      The name of the file to be created.
     * @return              The name of the file which was created.
     * @throws IOException  If the filename is unparseable or the file already exists.
     */
    public static String create(String filename) throws IOException {
        return normalize(create(construct(filename)));
    }

    /**
     * Opens a file for reading, appending, or writing, and processes it by calling the given service with the resulting
     * stream.
     *
     * @param file              The file to be processed.
     * @param mode              The mode to use when opening the file.
     * @param service           The service to be invoked to process the opened stream
     * @param input             The input variable name to use for the opened stream.
     * @param pipeline          The input pipeline.
     * @param raise             Whether to rethrow any errors that occur.
     * @param logError          Whether to log any errors that occur when raise is false.
     * @return                  The output pipeline.
     * @throws ServiceException If any errors occur during processing.
     */
    public static IData process(File file, String mode, String service, String input, IData pipeline, boolean raise, boolean logError) throws ServiceException {
        if (file != null) {
            if (input == null) input = "$stream";
            Closeable stream = null;

            try {
                if (mode == null || mode.equalsIgnoreCase("read")) {
                    stream = InputStreamHelper.normalize(new FileInputStream(file));
                } else if (mode.equalsIgnoreCase("create") && FileHelper.exists(file)) {
                    throw new IOException("file already exists and will not be overwritten or appended to: " + file);
                } else {
                    if (!FileHelper.exists(file)) FileHelper.create(file);
                    stream = OutputStreamHelper.normalize(new FileOutputStream(file, mode.equalsIgnoreCase("append")));
                }

                IDataCursor cursor = pipeline.getCursor();
                IDataHelper.put(cursor, input, stream);
                cursor.destroy();

                pipeline = ServiceHelper.invoke(service, pipeline, true);
            } catch (Throwable exception) {
                if (raise) {
                    ExceptionHelper.raise(exception);
                } else {
                    pipeline = ServiceHelper.addExceptionToPipeline(pipeline, exception);
                    if (logError) ServerAPI.logError(exception);
                }
            } finally {
                CloseableHelper.close(stream);

                IDataCursor cursor = pipeline.getCursor();
                IDataHelper.remove(cursor, input);
                cursor.destroy();
            }
        }

        return pipeline;
    }

    /**
     * Deletes the given file.
     *
     * @param file          The file to be deleted.
     * @throws IOException  If the file cannot be deleted.
     */
    public static void remove(File file) throws IOException {
        if (file != null && exists(file) && !file.delete()) {
            throw new IOException("Unable to remove file: " + normalize(file));
        }
    }

    /**
     * Deletes the given file.
     *
     * @param filename      The name of the file to be deleted.
     * @throws IOException  If the filename is unparseable or the file cannot be deleted.
     */
    public static void remove(String filename) throws IOException {
        remove(construct(filename));
    }

    /**
     * Update the modified time of the given file to the current time, or create it if it does not exist.
     *
     * @param file          The file to be touched.
     * @throws IOException  If the file cannot be created.
     */
    public static void touch(File file) throws IOException {
        touch(file, true);
    }

    /**
     * Update the modified time of the given file to the current time, or create it if it does not exist.
     *
     * @param file          The file to be touched.
     * @param createMissing Whether to create the file if it does not already exist.
     * @throws IOException  If the file cannot be created.
     */
    public static void touch(File file, boolean createMissing) throws IOException {
        touch(file, createMissing, null);
    }

    /**
     * Update the modified time of the given file to the given time, or create it if it does not exist.
     *
     * @param file          The file to be touched.
     * @param createMissing Whether to create the file if it does not already exist.
     * @param lastModified  Optional time to set the file's last modified time to. If null, current time is used.
     * @throws IOException  If the file cannot be created.
     */
    public static void touch(File file, boolean createMissing, Calendar lastModified) throws IOException {
        touch(file, createMissing, lastModified == null ? System.currentTimeMillis() : lastModified.getTimeInMillis());
    }

    /**
     * Update the modified time of the given file to the given time, or create it if it does not exist.
     *
     * @param file          The file to be touched.
     * @param lastModified  Optional time to set the file's last modified time to.
     * @throws IOException  If the file cannot be created.
     */
    public static void touch(File file, boolean createMissing, long lastModified) throws IOException {
        if (file != null) {
            if (createMissing && !file.exists()) {
                create(file);
            }
            file.setLastModified(lastModified);
        }
    }

    /**
     * Renames the source file to the target name.
     *
     * @param source        The file to be renamed.
     * @param target        The new name of the file.
     * @throws IOException  If the file cannot be renamed.
     */
    public static void rename(File source, File target) throws IOException {
        if (source != null && target != null) {
            if (!exists(source) || exists(target) || !source.renameTo(target)) {
                throw new IOException("Unable to rename file " + normalize(source) + " to " + normalize(target));
            }
        }
    }

    /**
     * Renames the source file to the target name.
     *
     * @param source        The file to be renamed.
     * @param target        The new name of the file.
     * @throws IOException  If file cannot be renamed.
     */
    public static void rename(String source, String target) throws IOException {
        rename(construct(source), construct(target));
    }

    /**
     * Reads the given file completely, returning the file's content as a byte[].
     *
     * @param filename      The name of the file to be read.
     * @return              A byte[] containing the file's content.
     * @throws IOException  If there is a problem reading the file.
     */
    public static byte[] readToBytes(String filename) throws IOException {
        return readToBytes(construct(filename));
    }

    /**
     * Reads the given file completely, returning the file's content as a byte[].
     *
     * @param file          The file to be read.
     * @return              A byte[] containing the file's content.
     * @throws IOException  If there is a problem reading the file.
     */
    public static byte[] readToBytes(File file) throws IOException {
        byte[] content = null;

        if (file != null) {
            InputStream inputStream = null;
            ByteArrayOutputStream outputStream = null;
            try {
                inputStream = new FileInputStream(file);
                outputStream = new ByteArrayOutputStream(InputOutputHelper.DEFAULT_BUFFER_SIZE);

                InputOutputHelper.copy(inputStream, outputStream);

                content = outputStream.toByteArray();
            } finally {
                CloseableHelper.close(inputStream, outputStream);
            }
        }

        return content;
    }

    /**
     * Reads the given file completely, returning the file's content as a String.
     *
     * @param filename      The name of the file to be read.
     * @return              A String containing the file's content.
     * @throws IOException  If there is a problem reading the file.
     */
    public static String readToString(String filename) throws IOException {
        return readToString(filename, CharsetHelper.DEFAULT_CHARSET);
    }

    /**
     * Reads the given file completely, returning the file's content as a String.
     *
     * @param filename      The name of the file to be read.
     * @param charsetName   The character set the file's content is encoded with.
     * @return              A String containing the file's content.
     * @throws IOException  If there is a problem reading the file.
     */
    public static String readToString(String filename, String charsetName) throws IOException {
        return readToString(filename, CharsetHelper.normalize(charsetName));
    }

    /**
     * Reads the given file completely, returning the file's content as a String.
     *
     * @param filename      The name of the file to be read.
     * @param charset       The character set the file's content is encoded with.
     * @return              A String containing the file's content.
     * @throws IOException  If there is a problem reading the file.
     */
    public static String readToString(String filename, Charset charset) throws IOException {
        return readToString(construct(filename), CharsetHelper.normalize(charset));
    }

    /**
     * Reads the given file completely, returning the file's content as a String.
     *
     * @param file          The file to be read.
     * @return              A String containing the file's content.
     * @throws IOException  If there is a problem reading the file.
     */
    public static String readToString(File file) throws IOException {
        return readToString(file, CharsetHelper.DEFAULT_CHARSET);
    }

    /**
     * Reads the given file completely, returning the file's content as a String.
     *
     * @param file          The file to be read.
     * @param charsetName   The character set the file's content is encoded with.
     * @return              A String containing the file's content.
     * @throws IOException  If there is a problem reading the file.
     */
    public static String readToString(File file, String charsetName) throws IOException {
        return readToString(file, CharsetHelper.normalize(charsetName));
    }

    /**
     * Reads the given file completely, returning the file's content as a String.
     *
     * @param file          The file to be read.
     * @param charset       The character set the file's content is encoded with.
     * @return              A String containing the file's content.
     * @throws IOException  If there is a problem reading the file.
     */
    public static String readToString(File file, Charset charset) throws IOException {
        return StringHelper.normalize(readToBytes(file), CharsetHelper.normalize(charset));
    }

    /**
     * Reads the given file completely, returning the file's content as an InputStream.
     *
     * @param filename      The name of the file to be read.
     * @return              An InputStream containing the file's content.
     * @throws IOException  If there is a problem reading the file.
     */
    public static InputStream readToStream(String filename) throws IOException {
        return readToStream(construct(filename));
    }

    /**
     * Reads the given file completely, returning the file's content as an InputStream.
     *
     * @param file          The file to be read.
     * @return              An InputStream containing the file's content.
     * @throws IOException  If there is a problem reading the file.
     */
    public static InputStream readToStream(File file) throws IOException {
        return new ByteArrayInputStream(readToBytes(file));
    }

    /**
     * Writes content to a file; if the given file is null, a new temporary file is automatically created.
     *
     * @param file          The file to be written to; if null, a new temporary file is automatically created.
     * @param content       The content to be written.
     * @param append        If true, the content will be appended to the file, otherwise the content will overwrite any
     *                      previous content in the file.
     * @return              The file which the content was written to.
     * @throws IOException  If there is a problem writing to the file.
     */
    public static File writeFromStream(File file, InputStream content, boolean append) throws IOException {
        if (file == null || !exists(file)) file = create(file);
        if (content != null) InputOutputHelper.copy(content, new FileOutputStream(file, append));
        return file;
    }

    /**
     * Writes content to a file; if the given filename is null, a new temporary file is automatically created.
     *
     * @param filename      The name of the file to be written to; if null, a new temporary file is automatically
     *                      created.
     * @param content       The content to be written.
     * @param append        If true, the content will be appended to the file, otherwise the content will overwrite any
     *                      previous content in the file.
     * @return              The name of the file which the content was written to.
     * @throws IOException  If there is a problem writing to the file.
     */
    public static String writeFromStream(String filename, InputStream content, boolean append) throws IOException {
        return normalize(writeFromStream(construct(filename), content, append));
    }

    /**
     * Writes content to a file; if the given file is null, a new temporary file is automatically created.
     *
     * @param file          The file to be written to; if null, a new temporary file is automatically created.
     * @param content       The content to be written.
     * @param append        If true, the content will be appended to the file, otherwise the content will overwrite any
     *                      previous content in the file.
     * @return              The file which the content was written to.
     * @throws IOException  If there is a problem writing to the file.
     */
    public static File writeFromBytes(File file, byte[] content, boolean append) throws IOException {
        return writeFromStream(file, InputStreamHelper.normalize(content), append);
    }

    /**
     * Writes content to a file; if the given filename is null, a new temporary file is automatically created.
     *
     * @param filename      The name of the file to be written to; if null, a new temporary file is automatically
     *                      created.
     * @param content       The content to be written.
     * @param append        If true, the content will be appended to the file, otherwise the content will overwrite any
     *                      previous content in the file.
     * @return              The name of the file which the content was written to.
     * @throws IOException  If the filename is unparseable or there is a problem writing to the file.
     */
    public static String writeFromBytes(String filename, byte[] content, boolean append) throws IOException {
        return normalize(writeFromBytes(construct(filename), content, append));
    }

    /**
     * Writes content to a file; if the given file is null, a new temporary file is automatically created.
     *
     * @param file          The file to be written to; if null, a new temporary file is automatically created.
     * @param content       The content to be written.
     * @param charsetName   The character set to encode the content with.
     * @param append        If true, the content will be appended to the file, otherwise the content will overwrite any
     *                      previous content in the file.
     * @return              The file which the content was written to.
     * @throws IOException  If there is a problem writing to the file.
     */
    public static File writeFromString(File file, String content, String charsetName, boolean append) throws IOException {
        return writeFromString(file, content, CharsetHelper.normalize(charsetName), append);
    }

    /**
     * Writes content to a file; if the given file is null, a new temporary file is automatically created.
     *
     * @param file          The file to be written to; if null, a new temporary file is automatically created.
     * @param content       The content to be written.
     * @param charset       The character set to encode the content with.
     * @param append        If true, the content will be appended to the file, otherwise the content will overwrite any
     *                      previous content in the file.
     * @return              The file which the content was written to.
     * @throws IOException  If there is a problem writing to the file.
     */
    public static File writeFromString(File file, String content, Charset charset, boolean append) throws IOException {
        return writeFromStream(file, InputStreamHelper.normalize(content, charset), append);
    }

    /**
     * Writes content to a file; if the given filename is null, a new temporary file is automatically created.
     *
     * @param filename      The name of the file to be written to; if null, a new temporary file is automatically
     *                      created.
     * @param content       The content to be written.
     * @param charsetName   The character set to encode the content with.
     * @param append        If true, the content will be appended to the file, otherwise the content will overwrite any
     *                      previous content in the file.
     * @return              The name of the file which the content was written to.
     * @throws IOException  If the filename is unparseable or there is a problem writing to the file.
     */
    public static String writeFromString(String filename, String content, String charsetName, boolean append) throws IOException {
        return writeFromString(filename, content, CharsetHelper.normalize(charsetName), append);
    }

    /**
     * Writes content to a file; if the given filename is null, a new temporary file is automatically created.
     *
     * @param filename      The name of the file to be written to; if null, a new temporary file is automatically
     *                      created.
     * @param content       The content to be written.
     * @param charset       The character set to encode the content with.
     * @param append        If true, the content will be appended to the file, otherwise the content will overwrite any
     *                      previous content in the file.
     * @return              The name of the file which the content was written to.
     * @throws IOException  If the filename is unparseable or there is a problem writing to the file.
     */
    public static String writeFromString(String filename, String content, Charset charset, boolean append) throws IOException {
        return normalize(writeFromString(construct(filename), content, charset, append));
    }

    /**
     * Writes content to a file; if the given file is null, a new temporary file is automatically created.
     *
     * @param file          The file to be written to; if null, a new temporary file is automatically created.
     * @param content       The content to be written.
     * @param append        If true, the content will be appended to the file, otherwise the content will overwrite any
     *                      previous content in the file.
     * @return              The file which the content was written to.
     * @throws IOException  If there is a problem writing to the file.
     */
    public static File writeFromString(File file, String content, boolean append) throws IOException {
        return writeFromString(file, content, CharsetHelper.DEFAULT_CHARSET, append);
    }

    /**
     * Writes content to a file; if the given filename is null, a new temporary file is automatically created.
     *
     * @param filename      The name of the file to be written to; if null, a new temporary file is automatically
     *                      created.
     * @param content       The content to be written.
     * @param append        If true, the content will be appended to the file, otherwise the content will overwrite any
     *                      previous content in the file.
     * @return              The name of the file which the content was written to.
     * @throws IOException  If there is a problem writing to the file.
     */
    public static String writeFromString(String filename, String content, boolean append) throws IOException {
        return normalize(writeFromString(construct(filename), content, append));
    }

    /**
     * Copies the content in the source file to the target file.
     *
     * @param source        The file from which content will be copied.
     * @param target        The file to which content will be copied.
     * @param append        If true, the target content will be appended to, otherwise any previous content will be
     *                      overwritten.
     * @throws IOException  If the source file does not exist or there is a problem copying it.
     */
    public static void copy(File source, File target, boolean append) throws IOException {
        if (source != null && target != null) {
            if (!source.getCanonicalPath().equals(target.getCanonicalPath())) {
                // only needs to copy file when the paths are different
                InputOutputHelper.copy(new FileInputStream(source), new FileOutputStream(target, append));
            } else if (append) {
                // read fully into memory before writing to itself
                InputOutputHelper.copy(InputStreamHelper.memoize(new FileInputStream(source)), new FileOutputStream(target, append));
            } else {
                // otherwise update the last modified date
                touch(source);
            }
        }
    }

    /**
     * Copies the content in the source file to the target file.
     *
     * @param source        The file from which content will be copied.
     * @param target        The file to which content will be copied.
     * @param append        If true, the target content will be appended to, otherwise any previous content will be
     *                      overwritten.
     * @throws IOException  If the source file does not exist or there is a problem copying it.
     */
    public static void copy(String source, String target, boolean append) throws IOException {
        copy(construct(source), construct(target), append);
    }

    /**
     * GZips the given file as a new file in the same directory with the same name suffixed with ".gz".
     *
     * @param source        The file to be gzipped.
     * @param replace       Whether the source file should be deleted once compressed.
     * @return              The resulting gzipped file.
     * @throws IOException  If an IO error occurs.
     */
    public static String gzip(String source, boolean replace) throws IOException {
        return normalize(gzip(construct(source), replace));
    }

    /**
     * Gzips the given file as a new file in the same directory with the same name suffixed with ".gz".
     *
     * @param source        The file to be gzipped.
     * @param replace       Whether the source file should be deleted once compressed.
     * @return              The resulting gzipped file.
     * @throws IOException  If an IO error occurs.
     */
    public static File gzip(File source, boolean replace) throws IOException {
        return gzip(source, null, replace);
    }

    /**
     * Gzips the given file as a new file with the given target name.
     *
     * @param source        The file to be gzipped.
     * @param target        The file to write the gzipped content to.
     * @param replace       Whether the source file should be deleted once compressed.*
     * @return              The resulting gzipped file.
     * @throws IOException  If an IO error occurs.
     */
    public static String gzip(String source, String target, boolean replace) throws IOException {
        return normalize(gzip(construct(source), construct(target), replace));
    }

    /**
     * Gzips the given file as a new file with the given target name.
     *
     * @param source        The file to be gzipped.
     * @param target        The file to write the gzipped content to.
     * @param replace       Whether the source file should be deleted once compressed.
     * @return              The resulting gzipped file.
     * @throws IOException  If an IO error occurs.
     */
    public static File gzip(File source, File target, boolean replace) throws IOException {
        if (source == null) throw new NullPointerException("source must not be null");
        if (target == null) target = new File(source.getParentFile(), source.getName() + ".gz");

        if (source.exists()) {
            if (source.isFile()) {
                if (source.canRead() && (!replace || source.canWrite())) {
                    if (target.exists()) {
                        throw new IOException("Unable to create file because it already exists: " + normalize(target));
                    } else {
                        InputOutputHelper.copy(new FileInputStream(source), new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(target), InputOutputHelper.DEFAULT_BUFFER_SIZE)));
                        target.setLastModified(source.lastModified());
                        if (replace) source.delete();
                        return target;
                    }
                } else {
                    throw new IOException("Unable to gzip file because access is denied: " + normalize(source));
                }
            } else {
                throw new IOException("Unable to gzip file because it is a directory: " + normalize(source));
            }
        } else {
            throw new IOException("Unable to gzip file because it does not exist: " + normalize(source));
        }
    }

    /**
     * Deletes all files that match the given file pattern older than the given duration.
     *
     * @param  file                     The file pattern to be purged.
     * @param  duration                 The age files must be before they are deleted.
     * @return                          The number of files deleted.
     * @throws FileNotFoundException    If the parent directory does not exist.
     */
    public static long purge(String file, FilenameFilterType filterType, Duration duration) throws FileNotFoundException {
        return purge(FileHelper.construct(file), filterType, duration);
    }

    /**
     * Deletes all files that match the given file pattern older than the given duration.
     *
     * @param  file                     The file pattern to be purged.
     * @param  duration                 The age files must be before they are deleted.
     * @return                          The number of files deleted.
     * @throws FileNotFoundException    If the parent directory does not exist.
     */
    public static long purge(File file, FilenameFilterType filterType, Duration duration) throws FileNotFoundException {
        return purge(file, filterType, duration == null ? null : DateTimeHelper.earlier(duration));
    }

    /**
     * Deletes all files that match the given file pattern older than the given calendar.
     *
     * @param  file                     The file pattern to be purged.
     * @param  olderThan                Only files modified prior to this datetime will be deleted.
     * @return                          The number of files deleted.
     * @throws FileNotFoundException    If the parent directory does not exist.
     */
    public static long purge(File file, FilenameFilterType filterType, Calendar olderThan) throws FileNotFoundException {
        long count = 0;

        if (file != null) {
            FilenameFilter filter = new InclusionFilenameFilter(FilenameFilterType.normalize(filterType), file.getName());
            File parent = file.getParentFile();

            for (String item : DirectoryHelper.list(parent)) {
                File child = new File(parent, item);
                if (exists(child) && filter.accept(parent, item)) {
                    boolean shouldPurge = true;

                    if (olderThan != null) {
                        Calendar modified = Calendar.getInstance();
                        modified.setTime(new Date(child.lastModified()));
                        shouldPurge = modified.compareTo(olderThan) <= 0;
                    }

                    if (shouldPurge && child.delete()) count += 1;
                }
            }
        }

        return count;
    }

    /**
     * Zips the given file as a new file in the same directory with the same name suffixed with ".zip".
     *
     * @param source        The file to be zipped.
     * @param replace       Whether the source file should be deleted once compressed.
     * @return              The resulting zipped file.
     * @throws IOException  If an IO error occurs.
     */
    public static String zip(String source, boolean replace) throws IOException {
        return normalize(zip(construct(source), replace));
    }

    /**
     * Zips the given file as a new file in the same directory with the same name suffixed with ".zip".
     *
     * @param source        The file to be zipped.
     * @param replace       Whether the source file should be deleted once compressed.
     * @return              The resulting zipped file.
     * @throws IOException  If an IO error occurs.
     */
    public static File zip(File source, boolean replace) throws IOException {
        return zip(source, null, replace);
    }

    /**
     * Zips the given file as a new file with the given name.
     *
     * @param source        The file to be zipped.
     * @param target        The file to write the zipped content to.
     * @param replace       Whether the source file should be deleted once compressed.
     * @return              The resulting zipped file.
     * @throws IOException  If an IO error occurs.
     */
    public static String zip(String source, String target, boolean replace) throws IOException {
        return normalize(zip(construct(source), construct(target), replace));
    }

    /**
     * Zips the given file as a new file with the given target name.
     *
     * @param source        The file to be zipped.
     * @param target        The file to write the zipped content to. If not specified, defaults to a file with
     *                      the same name as source suffixed with ".zip".
     * @param replace       Whether the source file should be deleted once compressed.
     * @return              The resulting zipped file.
     * @throws IOException  If an IO error occurs.
     */
    public static File zip(File source, File target, boolean replace) throws IOException {
        if (source == null) throw new NullPointerException("source must not be null");
        if (target == null) target = new File(source.getParentFile(), source.getName() + ".zip");

        if (source.exists()) {
            if (source.isFile()) {
                if (source.canRead() && (!replace || source.canWrite())) {
                    if (target.exists()) {
                        throw new IOException("Unable to create file because it already exists: " + normalize(target));
                    } else {
                        InputStream inputStream = null;
                        ZipOutputStream outputStream = null;

                        try {
                            inputStream = new BufferedInputStream(new FileInputStream(source), InputOutputHelper.DEFAULT_BUFFER_SIZE);
                            outputStream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(target), InputOutputHelper.DEFAULT_BUFFER_SIZE));

                            outputStream.putNextEntry(new ZipEntry(source.getName()));

                            InputOutputHelper.copy(inputStream, outputStream, false);
                        } finally {
                            if (outputStream != null) outputStream.closeEntry();
                            CloseableHelper.close(inputStream, outputStream);
                            target.setLastModified(source.lastModified());
                            if (replace) source.delete();
                        }

                        return target;
                    }
                } else {
                    throw new IOException("Unable to zip file because access is denied: " + normalize(source));
                }
            } else {
                throw new IOException("Unable to zip file because it is a directory: " + normalize(source));
            }
        } else {
            throw new IOException("Unable to zip file because it does not exist: " + normalize(source));
        }
    }

    /**
     * Returns a File object given a file name.
     *
     * @param filename  The name of the file, specified as a path or file:// URI.
     * @return          The file representing the given name.
     */
    public static File construct(String filename) {
        File file = null;

        if (filename != null) {
            if (filename.toLowerCase().startsWith("file:")) {
                try {
                    file = new File(new URI(filename));
                } catch (IllegalArgumentException ex) {
                    // work around java's weird handling of file://server/path style URIs on Windows, by changing the URI
                    // to be file:////server/path
                    if (filename.toLowerCase().startsWith("file://") && !filename.toLowerCase().startsWith("file:///")) {
                        file = construct("file:////" + filename.substring(6, filename.length()));
                    } else {
                        throw ex;
                    }
                } catch (URISyntaxException ex) {
                    throw new IllegalArgumentException(ex);
                }
            } else {
                file = new File(filename);
            }
        }

        return file;
    }

    /**
     * Returns the canonical file:// URI representation of the given file.
     *
     * @param file  The file to be normalized.
     * @return      The canonical file:// URI representation of the given file.
     */
    public static String normalize(File file) {
        String filename = null;
        try {
            if (file != null) filename = URIHelper.normalize(file.getCanonicalFile().toURI().toString());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } catch (URISyntaxException ex) {
            throw new RuntimeException(ex);
        }
        return filename;
    }

    /**
     * Returns the canonical file:// URI representation of the given file.
     *
     * @param filename  The name of the file to be normalized.
     * @return          The canonical file:// URI representation of the given file.
     */
    public static String normalize(String filename) {
        return normalize(construct(filename));
    }

    /**
     * Returns true if the given file matches the given pattern.
     *
     * @param filename                      The name of the file to check against the pattern.
     * @param pattern                       Either a regular expression or wildcard pattern.
     * @param patternIsRegularExpression    Boolean indicating if the given pattern is a regular expression or wildcard
     *                                      pattern.
     * @return                              True if the given file matches the given pattern.
     */
    public static boolean match(String filename, String pattern, boolean patternIsRegularExpression) {
        return match(construct(filename), pattern, patternIsRegularExpression);
    }

    /**
     * Returns true if the given file matches the given pattern.
     *
     * @param file                          The file to check against the pattern.
     * @param pattern                       Either a regular expression or wildcard pattern.
     * @param patternIsRegularExpression    Boolean indicating if the given pattern is a regular expression or wildcard
     *                                      pattern.
     * @return                              True if the given file matches the given pattern.
     */
    public static boolean match(File file, String pattern, boolean patternIsRegularExpression) {
        boolean match = false;

        if (file != null && pattern != null) {
            java.io.FilenameFilter filter;
            if (patternIsRegularExpression) {
                filter = new RegularExpressionFilenameFilter(pattern);
            } else {
                filter = new WildcardFilenameFilter(pattern);
            }
            match = filter.accept(file.getParentFile(), file.getName());
        }

        return match;
    }

    /**
     * Returns whether the given file can be written to by this process.
     *
     * @param file  The file to check the permissions of.
     * @return      Whether the given file is writable by this process.
     */
    public static boolean isWritable(File file) {
        if (file == null) return false;
        return file.canWrite();
    }

    /**
     * Returns whether the given file can be written to by this process.
     *
     * @param filename  The file to check the permissions of.
     * @return          Whether the given file is writable by this process.
     */
    public static boolean isWritable(String filename) {
        return isWritable(construct(filename));
    }

    /**
     * Returns whether the given file can be read by this process.
     *
     * @param file  The file to check the permissions of.
     * @return      Whether the given file is readable by this process.
     */
    public static boolean isReadable(File file) {
        if (file == null) return false;
        return file.canRead();
    }

    /**
     * Returns whether the given file can be read by this process.
     *
     * @param filename  The file to check the permissions of.
     * @return          Whether the given file is readable by this process.
     */
    public static boolean isReadable(String filename) {
        return isReadable(construct(filename));
    }

    /**
     * Returns whether the given file can be executed by this process.
     *
     * @param file  The file to check the permissions of.
     * @return      Whether the given file is executable by this process.
     */
    public static boolean isExecutable(File file) {
        if (file == null) return false;
        return file.canExecute();
    }

    /**
     * Returns whether the given file can be executed by this process.
     *
     * @param filename  The file to check the permissions of.
     * @return          Whether the given file is executable by this process.
     */
    public static boolean isExecutable(String filename) {
        return isExecutable(construct(filename));
    }

    /**
     * Returns the length of the given file in bytes.
     *
     * @param file  The file to check the length of.
     * @return      The length in bytes of the given file.
     */
    public static long length(File file) {
        if (file == null) return 0;
        return file.length();
    }

    /**
     * Returns the length of the given file in bytes.
     *
     * @param filename  The file to check the length of.
     * @return          The length in bytes of the given file.
     */
    public static long length(String filename) {
        return length(construct(filename));
    }

    /**
     * Returns only the name component of the given file.
     *
     * @param file  The file to return the name of.
     * @return      The name component only of the given file.
     */
    public static String getName(File file) {
        if (file == null || file.equals("")) return null;
        return file.getName();
    }

    /**
     * Returns only the name component of the given file.
     *
     * @param filename  The file to return the name of.
     * @return          The name component only of the given file.
     */
    public static String getName(String filename) {
        return getName(construct(filename));
    }

    /**
     * Returns the base and extension parts of the given file's name.
     *
     * @param file  The file to get the name parts of.
     * @return      The parts of the given file's name.
     */
    public static String[] getNameParts(File file) {
        String[] parts = null;

        String name = getName(file);
        if (name != null) {
            parts = name.split("\\.(?=[^\\.]+$)");
        }

        return parts;
    }

    /**
     * Returns the filename extension for the given file.
     *
     * @param file The file whose extension is to be returned.
     * @return     The filename extension for the given file.
     */
    public static String getExtension(File file) {
        if (file == null) return null;

        String extension;

        String[] parts = getNameParts(file);
        if (parts.length > 1) {
            extension = parts[parts.length - 1];
        } else {
            extension = null;
        }

        return extension;
    }

    /**
     * Returns the base and extension parts of the given file's name.
     *
     * @param filename  The file to get the name parts of.
     * @return          The parts of the given file's name.
     */
    public static String[] getNameParts(String filename) {
        return getNameParts(construct(filename));
    }

    /**
     * Returns the parent directory containing the given file.
     *
     * @param file  The file to return the parent directory of.
     * @return      The parent directory of the given file.
     */
    public static File getParentDirectory(File file) {
        if (file == null) return null;
        return file.getParentFile();
    }

    /**
     * Returns the parent directory containing the given file.
     *
     * @param filename  The file to return the parent directory of.
     * @return          The parent directory of the given file.
     */
    public static File getParentDirectory(String filename) {
        return getParentDirectory(construct(filename));
    }

    /**
     * Returns the parent directory containing the given file as a string.
     *
     * @param file  The file to return the parent directory of.
     * @return      The parent directory of the given file.
     */
    public static String getParentDirectoryAsString(File file) {
        return normalize(getParentDirectory(file));
    }

    /**
     * Returns the parent directory containing the given file as a string.
     *
     * @param filename  The file to return the parent directory of.
     * @return          The parent directory of the given file.
     */
    public static String getParentDirectoryAsString(String filename) {
        return getParentDirectoryAsString(construct(filename));
    }

    /**
     * Returns the last modified datetime of the given file.
     *
     * @param file  The file to return the last modified datetime of.
     * @return      The last modified datetime of the given file.
     */
    public static Date getLastModifiedDate(File file) {
        if (file == null) return null;
        return new Date(file.lastModified());
    }

    /**
     * Returns the last modified datetime of the given file.
     *
     * @param filename  The file to return the last modified datetime of.
     * @return          The last modified datetime of the given file.
     */
    public static Date getLastModifiedDate(String filename) {
        return getLastModifiedDate(construct(filename));
    }

    /**
     * Returns the last modified datetime of the given file as an ISO8601 formatted datetime string.
     *
     * @param file  The file to return the last modified datetime of.
     * @return      The last modified datetime of the given file.
     */
    public static String getLastModifiedDateTimeString(File file) {
        return DateTimeHelper.emit(getLastModifiedDate(file));
    }

    /**
     * Returns the last modified datetime of the given file as an ISO8601 formatted datetime string.
     *
     * @param filename  The file to return the last modified datetime of.
     * @return          The last modified datetime of the given file.
     */
    public static String getLastModifiedDateTimeString(String filename) {
        return getLastModifiedDateTimeString(construct(filename));
    }

    /**
     * Returns an IData document containing the properties of the given file.
     *
     * @param filename  The file to return the properties for.
     * @return          The properties of the given file as an IData document.
     */
    public static IData getPropertiesAsIData(String filename) {
        return getPropertiesAsIData(construct(filename));
    }

    /**
     * Returns an IData document containing the properties of the given file.
     *
     * @param file  The file to return the properties for.
     * @return      The properties of the given file as an IData document.
     */
    public static IData getPropertiesAsIData(File file) {
        IData output = IDataFactory.create();
        IDataCursor cursor = output.getCursor();

        boolean isFile = file.isFile();
        boolean exists = exists(file);

        IDataUtil.put(cursor, "exists?", BooleanHelper.emit(exists));

        String parent = getParentDirectoryAsString(file);
        if (parent != null) IDataUtil.put(cursor, "parent", parent);

        String name = getName(file);
        if (name != null) IDataUtil.put(cursor, "name", name);

        String[] parts = getNameParts(file);
        if (parts != null) {
            if (parts.length > 0) IDataUtil.put(cursor, "base", parts[0]);
            if (parts.length > 1) IDataUtil.put(cursor, "extension", parts[1]);
        }

        if (isFile) IDataUtil.put(cursor, "type", getMIMEType(file));

        if (exists) {
            IDataUtil.put(cursor, "length", LongHelper.emit(length(file)));
            IDataUtil.put(cursor, "modified", getLastModifiedDateTimeString(file));
            IDataUtil.put(cursor, "executable?", BooleanHelper.emit(isExecutable(file)));
            IDataUtil.put(cursor, "readable?", BooleanHelper.emit(isReadable(file)));
            IDataUtil.put(cursor, "writable?", BooleanHelper.emit(isWritable(file)));
        }

        IDataUtil.put(cursor, "uri", normalize(file));

        cursor.destroy();

        return output;
    }
}

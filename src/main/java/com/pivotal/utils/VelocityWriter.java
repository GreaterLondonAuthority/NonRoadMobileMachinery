/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.utils;

import java.io.*;

/**
 * Implements a Writer clas that is most useful for collecting temporary output
 * from a velocity merge - it ignores whitespace for the most part and only allows
 * the output of singular empty lines
 */
public class VelocityWriter extends Writer {

    static final org.slf4j.Logger mobjLogger = org.slf4j.LoggerFactory.getLogger(VelocityWriter.class);

    private Writer writer;
    private String filename;
    private boolean isClosed;
    private long length;
    private boolean lastLineNotEmpty;
    private String lastLine;

    public VelocityWriter() throws IOException {
        filename=Common.getTemporaryFilename();
        writer=new OutputStreamWriter(new FileOutputStream(filename), "UTF-8");
    }

    /**
     * Closes the stream, flushing it first. Once the stream has been closed,
     * further write() or flush() invocations will cause an IOException to be
     * thrown. Closing a previously closed stream has no effect.
     *
     * @throws IOException If an I/O error occurs
     */
    public void close() throws IOException {
        writer.close();
        isClosed=true;
    }

    /**
     * Writes a portion of an array of characters
     *
     * @param cbuf Array of characters to output
     * @param off Offset from which to start writing characters
     * @param len Number of characters to writ
     * @throws IOException
     */
    public void write(char[] cbuf, int off, int len) throws IOException {

        // Check we haven't been sent a dud

        if (!Common.isBlank(cbuf)) {

            // Reduce the characters to a string to check if it isn't empty

            char[] subString=new char[len];
            System.arraycopy(cbuf,off,subString,0,len);
            String line=new String(subString).replaceAll("(^[ \r\n\t]+)|([ \r\t]+$)","");

            // We will only output the characters if the line is not empty
            // or the previous line had something in it and it is different from
            // the previous line

            if ((lastLineNotEmpty || !Common.isBlank(line)) && !Common.doStringsMatch(line,lastLine)) {
                writer.write(line);
                lastLineNotEmpty=!Common.isBlank(line);
                length+=line.length();
                lastLine=line;
            }
        }
    }

    /**
     * Flushes the stream. If the stream has saved any characters from the various
     * write() methods in a buffer, write them immediately to their intended
     * destination. Then, if that destination is another character or byte stream,
     * flush it. Thus one flush() invocation will flush all the buffers in a chain #
     * of Writers and OutputStreams.
     *
     * If the intended destination of this stream is an abstraction provided by the underlying
     * operating system, for example a file, then flushing the stream guarantees only
     * that bytes previously written to the stream are passed to the operating system for
     * writing; it does not guarantee that they are actually written to a physical
     * device such as a disk drive.
     *
     * @throws IOException If an I/O error occurs
     */
    public void flush() throws IOException {
        writer.flush();
    }

    /**
     * Returns the size of the output so far
     *
     * @return Number of characters in the output
     */
    public long getSize() {
        if (isClosed)
            return getFile().length();
        else
            return length;
    }

    /**
     * Returns the underlying file used to back this writer
     *
     * @return File object
     */
    public File getFile() {
        return new File(filename);
    }

    /**
     * Returns the content of the temporary file
     *
     * @return Writer content
     */
    public String toString() {
        return Common.readTextFile(getFile());
    }

    /**
     * Destroys the local file used to back the writer
     *
     * @throws Throwable Any errors
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void finalize() throws Throwable {
        super.finalize();
        if (getFile().exists()) getFile().delete();
    }
}

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

package permafrost.tundra.server.content;

import com.wm.app.b2b.server.ContentHandler;
import com.wm.app.b2b.server.HTTPState;
import com.wm.app.b2b.server.InvokeState;
import com.wm.app.b2b.server.ProtocolInfoIf;
import com.wm.app.b2b.server.ProtocolState;
import com.wm.net.HttpHeader;
import com.wm.util.Values;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.zip.GZIPInputStream;

public class HTTPCompressionContentHandler extends ProxyContentHandler {
    public HTTPCompressionContentHandler(ContentHandler handler) {
        super(handler);
    }

    /**
     * Reads input for service invocation. Turns properly formatted input into an instance of Values
     * suitable for service invocation. Called before service invocation to provide input. If the
     * transport is HTTP, and a Content-Encoding header was specified with the value "gzip", the
     * stream is first gzip decompressed before the proxy handler is invoked.
     *
     * @param inputStream  The input stream from which to read.
     * @param invokeState  The current invocation state (such as the current user).
     * @return             The prepared service invocation input pipeline.
     * @throws IOException If an error occurs reading from the input stream.
     */
    public Values getInputValues(InputStream inputStream, InvokeState invokeState) throws IOException {

        if (inputStream != null && invokeState != null) {
            ProtocolInfoIf protocolInfoIf = invokeState.getProtocolInfoIf();

            if (protocolInfoIf instanceof HTTPState) {
                HTTPState httpState = (HTTPState)protocolInfoIf;
                HttpHeader headers = httpState.getRequestHeader();
                if (headers != null) {
                    String contentEncoding = headers.getFieldValue(HttpHeader.CONTENT_ENCODING);
                    if (contentEncoding != null && contentEncoding.trim().equalsIgnoreCase("gzip")) {
                        inputStream = new GZIPInputStream(inputStream);
                        headers.clearField(HttpHeader.CONTENT_ENCODING);

                        // regenerate the transport info stored in the protocol state
                        try {
                            // use reflection to invoke protected method initProtocolProps
                            Method initProtocolProps = ProtocolState.class.getDeclaredMethod("initProtocolProps");
                            initProtocolProps.setAccessible(true);
                            initProtocolProps.invoke(httpState);
                        } catch (NoSuchMethodException ex) {
                            throw new RuntimeException(ex);
                        } catch (IllegalAccessException ex) {
                            throw new RuntimeException(ex);
                        } catch (InvocationTargetException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                }
            }
        }

        return super.getInputValues(inputStream, invokeState);
    }
}
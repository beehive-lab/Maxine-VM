/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

/**
 * Downloads content from a given URL to a given file.
 *
 * @param path where to write the content
 * @param urls the URLs to try, stopping after the first successful one
 */
public class URLConnectionDownload {

	/**
	 * Downloads content from a given URL to a given file.
	 * 
	 * @param args
	 *            arg[0] is the path where to write the content. The remainder
	 *            of args are the URLs to try, stopping after the first
	 *            successful one
	 */
    public static void main(String[] args) {
    	File path = new File(args[0]);
    	String[] urls = new String[args.length - 1];
    	System.arraycopy(args, 1, urls, 0, urls.length);

        File parent = path.getParentFile();
        makeDirectory(parent);
        
        // Enable use of system proxies
        System.setProperty("java.net.useSystemProxies", "true");

        String proxy = System.getenv("HTTP_PROXY");
        String proxyMsg = "";
        if (proxy != null) {
            Pattern p = Pattern.compile("(?:http://)?([^:]+)(:\\d+)?");
            Matcher m = p.matcher(proxy);
            if (m.matches()) {
                String host = m.group(1);
                String port = m.group(2);
                System.setProperty("http.proxyHost", host);
                if (port != null) {
                    port = port.substring(1); // strip ':'
                    System.setProperty("http.proxyPort", port);
                }
                proxyMsg = " via proxy  " + proxy;
            } else {
            	System.err.println("Value of HTTP_PROXY is not valid:  " + proxy);
            }
        } else {
        	System.err.println("** If behind a firewall without direct internet access, use the HTTP_PROXY environment variable (e.g. 'env HTTP_PROXY=proxy.company.com:80 max ...') or download manually with a web browser.");
        }

        for (String s : urls) {
            try {
                System.err.println("Downloading " + s + " to  " + path + proxyMsg);
                URL url = new URL(s);
                URLConnection conn = url.openConnection();
                // 10 second timeout to establish connection
                conn.setConnectTimeout(10000);
                InputStream in = conn.getInputStream();
                int size = conn.getContentLength();
                FileOutputStream out = new FileOutputStream(path);
                int read = 0;
                byte[] buf = new byte[8192];
                int n = 0;
                while ((read = in.read(buf)) != -1) {
                    n += read;
                    long percent = ((long) n * 100 / size);
                    System.err.print("\r " + n + " bytes " + (size == -1 ? "" : " (" + percent + "%)"));
                    out.write(buf, 0, read);
                }
                System.err.println();
                out.close();
                in.close();
                return;
            } catch (MalformedURLException e) {
                throw new Error("Error in URL " + s, e);
            } catch (IOException e) {
                System.err.println("Error reading from  " + s + ":  " + e);
                path.delete();
            }
        }
        throw new Error("Could not download content to  " + path + " from  " + Arrays.toString(urls));
    }

    private static void makeDirectory(File directory) {
        if (!directory.exists() && !directory.mkdirs()) {
            throw new Error("Could not make directory " + directory);
        }
    }
}


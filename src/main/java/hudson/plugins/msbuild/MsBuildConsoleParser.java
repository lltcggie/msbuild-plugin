/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014, Kyle Sweeney, Gregory Boissinot and other contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.msbuild;

import hudson.console.LineTransformationOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser to find the number of Warnings/Errors of MsBuild compilation
 *
 * @author Damien Finck
 */
public class MsBuildConsoleParser extends LineTransformationOutputStream {
    private final PrintStream out;
    private final Charset incharset;
    private final Charset outcharset;
    private final boolean itTrans;

    private int numberOfWarnings = -1;
    private int numberOfErrors = -1;

    public MsBuildConsoleParser(OutputStream out, Charset outcharset) {
        this.out = new PrintStream(out);
        this.incharset = Charset.forName("MS932");
        this.outcharset = outcharset;
        this.itTrans = this.incharset.name() == this.outcharset.name();
    }

    public int getNumberOfWarnings() {
        return numberOfWarnings;
    }

    public int getNumberOfErrors() {
        return numberOfErrors;
    }

    @Override
    protected void eol(byte[] b, int len) throws IOException {
        String line = this.incharset.decode(ByteBuffer.wrap(b, 0, len)).toString();

        // trim off CR/LF from the end
        line = trimEOL(line);

        Pattern patternWarnings = Pattern.compile(".*\\d+\\s個の警告.*");
        Pattern patternErrors = Pattern.compile(".*\\d+\\sエラー.*");

        Matcher mWarnings = patternWarnings.matcher(line);
        Matcher mErrors = patternErrors.matcher(line);

        if (mWarnings.matches()) { // Match the number of warnings
            String[] part = line.split(" ");
            try {
                numberOfWarnings = Integer.parseInt(part[4]);
            } catch (NumberFormatException e) {

            }
        } else if (mErrors.matches()) { // Match the number of errors
            String[] part = line.split(" ");
            try {
                numberOfErrors = Integer.parseInt(part[4]);
            } catch (NumberFormatException e) {

            }
        }

        String outline = line;
        if(this.itTrans) {
            outline = new String(line.getBytes(this.outcharset.name()), this.outcharset.name());
        }

        // Write to output
        out.println(outline);
    }

    @Override
    public void close() throws IOException {
        super.close();
        out.close();
    }
}

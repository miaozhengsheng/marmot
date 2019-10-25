package com.marmot.common.log;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

public class WrapperThrowableRenderer {


    private static final String LINE_SEP = System.getProperty("line.separator");
    private static final String[] INNER_PACKAGES = { "at com.liepin.swift.", "at com.liepin.cache.",
            "at com.liepin.dao.", "at com.liepin.router." };
    private static final String NORM_PACKAGE = "at com.liepin";

    public static String render(final Throwable throwable) {
        return render(throwable, false);
    }

    public static String renderSimplify(final Throwable throwable) {
        return render(throwable, true);
    }

    public static String renderDynamic(final Throwable throwable) {
            return render(throwable);
    }

    private static String render(final Throwable throwable, boolean simplify) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        try {
            throwable.printStackTrace(pw);
        } catch (RuntimeException ex) {
        }
        pw.flush();
        LineNumberReader reader = new LineNumberReader(new StringReader(sw.toString()));
        StringBuilder log = new StringBuilder();
        try {
            String line = reader.readLine();
            if (line != null) {
                log.append(line);
                while ((line = reader.readLine()) != null) {
                    if (simplify) {
                        if (!line.trim().startsWith(NORM_PACKAGE)) {
                            continue;
                        }
                        boolean flag = false;
                        for (String pg : INNER_PACKAGES) {
                            if (line.trim().startsWith(pg)) {
                                flag = true;
                                break;
                            }
                        }
                        if (flag) {
                            continue;
                        }
                    }
                    log.append(LINE_SEP);
                    log.append(line);
                }
            }
        } catch (IOException ex) {
            if (ex instanceof InterruptedIOException) {
                Thread.currentThread().interrupt();
            }
            if (log.length() != 0) {
                log.append(LINE_SEP);
            }
            log.append(ex.toString());
        }
        return log.toString();
    }


}

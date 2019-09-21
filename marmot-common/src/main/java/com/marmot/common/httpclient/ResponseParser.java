package com.marmot.common.httpclient;

import java.io.InputStream;

public abstract class ResponseParser {


    public abstract String getWriterType(); // for example: wt=XML, JSON, etc

    public abstract <T> T processResponse(InputStream body, String encoding);


}

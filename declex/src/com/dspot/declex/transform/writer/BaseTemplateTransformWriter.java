/**
 * Copyright (C) 2016 DSpot Sp. z o.o
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dspot.declex.transform.writer;

import java.io.IOException;
import java.io.Writer;

import org.androidannotations.holder.GeneratedClassHolder;

public class BaseTemplateTransformWriter<T extends GeneratedClassHolder> extends Writer {
	protected T holder;
	
	protected String strCode = "";
    private Writer out;
       
    public BaseTemplateTransformWriter(Writer out, T holder) {
    	this.holder = holder;
        this.out = out;
    }

    public void write(char[] cbuf, int off, int len)
            throws IOException {
    	
    	strCode = strCode + new String(cbuf, off, len);
    }

    public void flush() throws IOException {
        out.flush();
    }

    public void close() {
    }
}

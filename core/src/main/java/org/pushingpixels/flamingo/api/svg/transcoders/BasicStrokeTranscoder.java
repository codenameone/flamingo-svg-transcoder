/**
 * Copyright 2012 Emmanuel Bourg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.pushingpixels.flamingo.api.svg.transcoders;

import java.awt.BasicStroke;
import java.io.PrintWriter;

/**
 * Transcodes a stroke.
 * 
 * @author Emmanuel Bourg
 * @version $Revision$, $Date$
 */
public class BasicStrokeTranscoder extends Transcoder<BasicStroke> {

    public static BasicStrokeTranscoder INSTANCE = new BasicStrokeTranscoder();

    @Override
    public void transcode(BasicStroke stroke, PrintWriter output) {
        if (stroke.getDashArray() == null) {
            output.append("new Stroke(" + FloatTranscoder.INSTANCE.transcode(stroke.getLineWidth()) + ", " + stroke.getEndCap() + ", "
                    + stroke.getLineJoin() + ", " + FloatTranscoder.INSTANCE.transcode(stroke.getMiterLimit()) + ")");
        } else {
            output.append("new Stroke(" + FloatTranscoder.INSTANCE.transcode(stroke.getLineWidth()) + ", " + stroke.getEndCap() + ", "
                    + stroke.getLineJoin() + ", " + FloatTranscoder.INSTANCE.transcode(stroke.getMiterLimit()) + ", "
                    + FloatArrayTranscoder.INSTANCE.transcode(stroke.getDashArray()) + ", " 
                    + FloatTranscoder.INSTANCE.transcode(stroke.getDashPhase()) + ")");
        }
    }
}

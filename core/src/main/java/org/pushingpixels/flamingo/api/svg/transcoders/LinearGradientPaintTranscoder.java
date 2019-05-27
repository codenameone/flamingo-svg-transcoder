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

import java.io.PrintWriter;

import org.apache.batik.ext.awt.LinearGradientPaint;

/**
 * Transcodes a linear gradient.
 *
 * @author Emmanuel Bourg
 * @version $Revision$, $Date$
 */
public class LinearGradientPaintTranscoder extends MultipleGradientPaintTranscoder<LinearGradientPaint> {

    public static LinearGradientPaintTranscoder INSTANCE = new LinearGradientPaintTranscoder();

    @Override
    public void transcode(LinearGradientPaint paint, PrintWriter output) {
        StringBuilder colorsRep = new StringBuilder();
        if (paint.getFractions() == null) {
            colorsRep.append("null");
        } else {
            colorsRep.append(ColorArrayTranscoder.INSTANCE.transcode(paint.getColors()));
        }

        output.printf("new LinearGradientPaint(%s, %s, %s, %s, %s, %s, %s, %s, %s)",
                //
                DoubleTranscoder.INSTANCE.transcode(paint.getStartPoint().getX()),
                DoubleTranscoder.INSTANCE.transcode(paint.getStartPoint().getY()),
                DoubleTranscoder.INSTANCE.transcode(paint.getEndPoint().getX()),
                DoubleTranscoder.INSTANCE.transcode(paint.getEndPoint().getY()),
                FloatArrayTranscoder.INSTANCE.transcode(normalizeFractions(paint.getFractions())),
                colorsRep.toString(),
                transcode(paint.getCycleMethod()),
                transcode(paint.getColorSpace()),
                AffineTransformTranscoder.INSTANCE.transcode(paint.getTransform())).toString();
    }
}

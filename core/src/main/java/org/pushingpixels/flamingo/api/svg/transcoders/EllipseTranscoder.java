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

import java.awt.geom.Ellipse2D;
import java.awt.geom.PathIterator;
import java.io.PrintWriter;

/**
 * Transcodes an ellipse.
 * 
 * @author Emmanuel Bourg
 * @version $Revision$, $Date$
 */
public class EllipseTranscoder extends Transcoder<Ellipse2D> {

    public static EllipseTranscoder INSTANCE = new EllipseTranscoder();

    @Override
    public void transcode(Ellipse2D ellipse, PrintWriter output) {
        DoubleTranscoder transcoder = DoubleTranscoder.INSTANCE;
        PathIterator pathIterator = ellipse.getPathIterator(null);
        double[] coords = new double[6];
        output.println("shape = new GeneralPath();");
        for (; !pathIterator.isDone(); pathIterator.next()) {
            int type = pathIterator.currentSegment(coords);
            switch (type) {
                case PathIterator.SEG_CUBICTO:
                    output.println("((GeneralPath) shape).curveTo(" + coords[0] + ", " + coords[1] + ", " + coords[2] + ", " + coords[3] + ", " + coords[4] + ", " + coords[5] + ");");
                    break;
                case PathIterator.SEG_QUADTO:
                    output.println("((GeneralPath) shape).quadTo(" + coords[0] + ", " + coords[1] + ", " + coords[2] + ", " + coords[3] + ");");
                    break;
                case PathIterator.SEG_MOVETO:
                    output.println("((GeneralPath) shape).moveTo(" + coords[0] + ", " + coords[1] + ");");
                    break;
                case PathIterator.SEG_LINETO:
                    output.println("((GeneralPath) shape).lineTo(" + coords[0] + ", " + coords[1] + ");");
                    break;
                case PathIterator.SEG_CLOSE:
                    output.println("((GeneralPath) shape).closePath();");
                    break;
            }
        }
        output.println();
    }
}

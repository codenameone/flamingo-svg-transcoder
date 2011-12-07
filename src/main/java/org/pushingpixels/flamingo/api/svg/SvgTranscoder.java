/*
 * Copyright (c) 2005-2010 Flamingo Kirill Grouchnikov. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *  o Redistributions of source code must retain the above copyright notice, 
 *    this list of conditions and the following disclaimer. 
 *     
 *  o Redistributions in binary form must reproduce the above copyright notice, 
 *    this list of conditions and the following disclaimer in the documentation 
 *    and/or other materials provided with the distribution. 
 *     
 *  o Neither the name of Flamingo Kirill Grouchnikov nor the names of 
 *    its contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission. 
 *     
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE 
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 */

package org.pushingpixels.flamingo.api.svg;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.URL;
import java.util.Formatter;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.DocumentLoader;
import org.apache.batik.bridge.GVTBuilder;
import org.apache.batik.bridge.UserAgentAdapter;
import org.apache.batik.ext.awt.LinearGradientPaint;
import org.apache.batik.ext.awt.MultipleGradientPaint;
import org.apache.batik.ext.awt.MultipleGradientPaint.ColorSpaceEnum;
import org.apache.batik.ext.awt.MultipleGradientPaint.CycleMethodEnum;
import org.apache.batik.ext.awt.RadialGradientPaint;
import org.apache.batik.ext.awt.geom.ExtendedGeneralPath;
import org.apache.batik.gvt.CompositeGraphicsNode;
import org.apache.batik.gvt.CompositeShapePainter;
import org.apache.batik.gvt.FillShapePainter;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.gvt.ShapeNode;
import org.apache.batik.gvt.ShapePainter;
import org.apache.batik.gvt.StrokeShapePainter;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * SVG to Java2D transcoder.
 *
 * @author Kirill Grouchnikov.
 */
public class SvgTranscoder {
    /** Listener. */
    protected TranscoderListener listener;

    /** Print writer that wraps the {@link TranscoderListener#getWriter()} of the registered {@link #listener}. */
    protected PrintWriter externalPrintWriter;

    /** Print writer that wraps the {@link TranscoderListener#getWriter()} of the registered {@link #listener}. */
    protected PrintWriter printWriter;

    /** Class name for the generated Java2D code. */
    protected String javaClassName;

    /** Package name for the generated Java2D code. */
    protected String javaPackageName;

    protected boolean javaToImplementResizableIconInterface;

    protected final static String TOKEN_PACKAGE = "TOKEN_PACKAGE";
    protected final static String TOKEN_CLASSNAME = "TOKEN_CLASSNAME";
    protected final static String TOKEN_PAINTING_CODE = "TOKEN_PAINTING_CODE";
    protected final static String TOKEN_ORIG_X = "TOKEN_ORIG_X";
    protected final static String TOKEN_ORIG_Y = "TOKEN_ORIG_Y";
    protected final static String TOKEN_ORIG_WIDTH = "TOKEN_ORIG_WIDTH";
    protected final static String TOKEN_ORIG_HEIGHT = "TOKEN_ORIG_HEIGHT";

    /** URL of the SVG image. */
    protected URL url;

    /** The current composite. */
    private AlphaComposite currentComposite;

    /** The current paint, as a Java declaration. */
    private String currentPaint;
    
    /** The current stroke, as a Java declaration. */
    private String currentStroke;

    /** The current shape. */
    private Shape currentShape;
    
    /**
     * Creates a new transcoder.
     *
     * @param url           URL of the SVG image.
     * @param javaClassname Classname for the generated Java2D code.
     */
    public SvgTranscoder(URL url, String javaClassname) {
        this(javaClassname);
        this.url = url;
    }

    /**
     * Creates a new transcoder.
     *
     * @param javaClassname Classname for the generated Java2D code.
     */
    public SvgTranscoder(String javaClassname) {
        this.javaClassName = javaClassname;
        this.javaToImplementResizableIconInterface = false;
    }

    /**
     * Returns the filtered image content. The metadata are removed from
     * the document to prevent illegal elements from breaking the parsing.
     * (For example several KDE icons have unrecognized RDF elements)
     */
    private InputStream getInputStream() throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        SAXParser parser = factory.newSAXParser();
        XMLReader reader = parser.getXMLReader();
        reader.setEntityResolver(new EntityResolver() {
            public InputSource resolveEntity(String pid, String sid) throws SAXException {
                return new InputSource(new StringReader(""));
            }
        });
        
        SAXSource source = new SAXSource(reader, new InputSource(url.openStream()));
        
        Result result = new StreamResult(buffer);
        
        StreamSource stylesheet = new StreamSource(getClass().getResourceAsStream("/svg-cleanup.xsl"));
        
        Transformer transformer = TransformerFactory.newInstance().newTransformer(stylesheet);
        transformer.transform(source, result);
        
        stylesheet.getInputStream().close();
        
        return new ByteArrayInputStream(buffer.toByteArray());
    }

    /**
     * Transcodes the SVG image into Java2D code. Does nothing if the
     * {@link #listener} is <code>null</code>.
     */
    public void transcode() {
        if (listener == null) {
            return;
        }

        UserAgentAdapter ua = new UserAgentAdapter();
        DocumentLoader loader = new DocumentLoader(ua);
        BridgeContext context = new BridgeContext(ua, loader);
        context.setDynamicState(BridgeContext.DYNAMIC);
        ua.setBridgeContext(context);
        
        try {
            Document svgDoc = loader.loadDocument(url.toString(), getInputStream());
            new GVTBuilder().build(context, svgDoc);
            
            transcode(context);
        } catch (Exception e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Unable to transcode " + url, e);
        }
    }

    public void setJavaToImplementResizableIconInterface(boolean javaToImplementResizableIconInterface) {
        this.javaToImplementResizableIconInterface = javaToImplementResizableIconInterface;
    }

    public void setJavaPackageName(String javaPackageName) {
        this.javaPackageName = javaPackageName;
    }

    /**
     * Sets the listener.
     *
     * @param listener Listener.
     */
    public void setListener(TranscoderListener listener) {
        this.listener = listener;
        this.setPrintWriter(new PrintWriter(this.listener.getWriter()));
    }

    public void setPrintWriter(PrintWriter printWriter) {
        this.externalPrintWriter = printWriter;
    }

    /**
     * Transcodes the SVG image into Java2D code.
     */
    public void transcode(BridgeContext context) {
        GraphicsNode root = context.getGraphicsNode(context.getDocument());
        
        ByteArrayOutputStream paintingCodeStream = new ByteArrayOutputStream();
        this.printWriter = new IndentingPrintWriter(new PrintWriter(paintingCodeStream));
        transcodeGraphicsNode(root, "");
        this.printWriter.close();

        String templateString = readTemplate("SvgTranscoderTemplate" + (javaToImplementResizableIconInterface ? "Resizable" : "Plain") + ".templ");
        
        templateString = templateString.replaceAll(TOKEN_PACKAGE, javaPackageName != null ? "package " + javaPackageName + ";" : "");
        templateString = templateString.replaceAll(TOKEN_CLASSNAME, javaClassName);
        
        String separator = 
                  "        paint${count}(g, origAlpha, transformations);\n"
                + "    }\n\n"
                + "    private static void paint${count}(Graphics2D g, float origAlpha, java.util.LinkedList<AffineTransform> transformations) {\n"
                + "        Shape shape = null;\n";
        
        String paintingCode = new String(paintingCodeStream.toByteArray());
        templateString = templateString.replaceAll(TOKEN_PAINTING_CODE, TextSplitter.insert(paintingCode, separator, 3000));
        
        Rectangle2D bounds = root.getBounds();
        if (bounds == null) {
            bounds = new Rectangle2D.Double(0, 0, context.getDocumentSize().getWidth(), context.getDocumentSize().getHeight());
        }
        
        templateString = templateString.replaceAll(TOKEN_ORIG_X, "" + (int) Math.ceil(bounds.getX()));
        templateString = templateString.replaceAll(TOKEN_ORIG_Y, "" + (int) Math.ceil(bounds.getY()));
        templateString = templateString.replaceAll(TOKEN_ORIG_WIDTH, "" + (int) Math.ceil(bounds.getWidth()));
        templateString = templateString.replaceAll(TOKEN_ORIG_HEIGHT, "" + (int) Math.ceil(bounds.getHeight()));

        this.externalPrintWriter.println(templateString);
        this.externalPrintWriter.close();

        if (listener != null) {
            listener.finished();
        }
    }

    private String readTemplate(String name) {
        InputStream in = getClass().getResourceAsStream(name);
        StringBuilder buffer = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        try {
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                buffer.append(line + "\n");
            }
            reader.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        
        return buffer.toString();
    }

    /**
     * Transcodes the specified path iterator.
     *
     * @param pathIterator Path iterator.
     */
    private void transcodePathIterator(PathIterator pathIterator) {
        float[] coords = new float[6];
        printWriter.println("shape = new GeneralPath();");
        for (; !pathIterator.isDone(); pathIterator.next()) {
            int type = pathIterator.currentSegment(coords);
            switch (type) {
                case PathIterator.SEG_CUBICTO:
                    printWriter.println("((GeneralPath) shape).curveTo(" + coords[0] + ", " + coords[1] + ", " + coords[2] + ", " + coords[3] + ", " + coords[4] + ", " + coords[5] + ");");
                    break;
                case PathIterator.SEG_QUADTO:
                    printWriter.println("((GeneralPath) shape).quadTo(" + coords[0] + ", " + coords[1] + ", " + coords[2] + ", " + coords[3] + ");");
                    break;
                case PathIterator.SEG_MOVETO:
                    printWriter.println("((GeneralPath) shape).moveTo(" + coords[0] + ", " + coords[1] + ");");
                    break;
                case PathIterator.SEG_LINETO:
                    printWriter.println("((GeneralPath) shape).lineTo(" + coords[0] + ", " + coords[1] + ");");
                    break;
                // through
                case PathIterator.SEG_CLOSE:
                    printWriter.println("((GeneralPath) shape).closePath();");
                    break;
            }
        }
    }

    /**
     * Transcodes the specified shape.
     *
     * @param shape Shape.
     * @throws UnsupportedOperationException if the shape is unsupported.
     */
    private void transcodeShape(Shape shape) throws UnsupportedOperationException {
        if (shape == currentShape) {
            return;
        }
        
        if (shape instanceof ExtendedGeneralPath) {
            transcodePathIterator(((ExtendedGeneralPath) shape).getPathIterator(null));
            
        } else if (shape instanceof GeneralPath) {
            transcodePathIterator(((GeneralPath) shape).getPathIterator(null));
            
        } else if (shape instanceof Rectangle2D) {
            Rectangle2D rect = (Rectangle2D) shape;
            printWriter.println("shape = new Rectangle2D.Double("
                    + transcodeDouble(rect.getX()) + ", " + transcodeDouble(rect.getY())+ ", "
                    + transcodeDouble(rect.getWidth()) + ", " + transcodeDouble(rect.getHeight()) + ");");
            
        } else if (shape instanceof RoundRectangle2D) {
            RoundRectangle2D rRect = (RoundRectangle2D) shape;
            printWriter.println("shape = new RoundRectangle2D.Double("
                    + transcodeDouble(rRect.getX()) + ", " + transcodeDouble(rRect.getY()) + ", "
                    + transcodeDouble(rRect.getWidth()) + ", " + transcodeDouble(rRect.getHeight()) + ", "
                    + transcodeDouble(rRect.getArcWidth()) + ", " + transcodeDouble(rRect.getArcHeight()) + ");");
            
        } else if (shape instanceof Ellipse2D) {
            Ellipse2D ell = (Ellipse2D) shape;
            printWriter.println("shape = new Ellipse2D.Double(" + ell.getX() + ", " + ell.getY() + ", " + ell.getWidth() + ", " + ell.getHeight() + ");");
            
        } else if (shape instanceof Line2D.Float) {
            Line2D.Float l2df = (Line2D.Float) shape;
            printWriter.format(Locale.ENGLISH, "shape = new Line2D.Float(%ff, %ff, %ff, %ff);\n", l2df.x1, l2df.y1, l2df.x2, l2df.y2);
            
        } else {
            throw new UnsupportedOperationException(shape.getClass().getCanonicalName());
        }
        
        currentShape = shape;
    }

    /**
     * Transcodes the specified linear gradient paint.
     *
     * @param paint Linear gradient paint.
     * @throws IllegalArgumentException if the fractions are not strictly increasing.
     */
    private String transcodeLinearGradientPaint(LinearGradientPaint paint) throws IllegalArgumentException {
        CycleMethodEnum cycleMethod = paint.getCycleMethod();
        ColorSpaceEnum colorSpace = paint.getColorSpace();
        
        StringBuilder colorsRep = new StringBuilder();
        if (paint.getFractions() == null) {
            colorsRep.append("null");
        } else {
            colorsRep.append(transcodeArray(paint.getColors()));
        }

        String cycleMethodRep = null;
        if (cycleMethod == MultipleGradientPaint.NO_CYCLE) {
            cycleMethodRep = "NO_CYCLE";
        } else if (cycleMethod == MultipleGradientPaint.REFLECT) {
            cycleMethodRep = "REFLECT";
        } else if (cycleMethod == MultipleGradientPaint.REPEAT) {
            cycleMethodRep = "REPEAT";
        }

        String colorSpaceRep = null;
        if (colorSpace == MultipleGradientPaint.SRGB) {
            colorSpaceRep = "SRGB";
        } else if (colorSpace == MultipleGradientPaint.LINEAR_RGB) {
            colorSpaceRep = "LINEAR_RGB";
        }
        
        return new Formatter().format("new LinearGradientPaint(%s, %s, %s, %s, %s, %s, %s)", 
                transcodePoint(paint.getStartPoint()),
                transcodePoint(paint.getEndPoint()),
                transcodeGradientFractions(paint.getFractions()),
                colorsRep.toString(),
                cycleMethodRep, colorSpaceRep,
                transcodeTransform(paint.getTransform())).toString();
    }

    /**
     * Transcodes the specified radial gradient paint.
     *
     * @param paint Radial gradient paint.
     * @throws IllegalArgumentException if the fractions are not strictly increasing.
     */
    private String transcodeRadialGradientPaint(RadialGradientPaint paint) throws IllegalArgumentException {
        CycleMethodEnum cycleMethod = paint.getCycleMethod();
        ColorSpaceEnum colorSpace = paint.getColorSpace();
        
        StringBuilder colorsRep = new StringBuilder();
        if (paint.getFractions() == null) {
            colorsRep.append("null");
        } else {
            colorsRep.append(transcodeArray(paint.getColors()));
        }
        
        String cycleMethodRep = null;
        if (cycleMethod == MultipleGradientPaint.NO_CYCLE) {
            cycleMethodRep = "NO_CYCLE";
        } else if (cycleMethod == MultipleGradientPaint.REFLECT) {
            cycleMethodRep = "REFLECT";
        } else if (cycleMethod == MultipleGradientPaint.REPEAT) {
            cycleMethodRep = "REPEAT";
        }

        String colorSpaceRep = null;
        if (colorSpace == MultipleGradientPaint.SRGB) {
            colorSpaceRep = "SRGB";
        } else if (colorSpace == MultipleGradientPaint.LINEAR_RGB) {
            colorSpaceRep = "LINEAR_RGB";
        }
        
        return "new RadialGradientPaint("
                + transcodePoint(paint.getCenterPoint()) + ", " + transcodeFloat(paint.getRadius()) + ", " + transcodePoint(paint.getFocusPoint()) + ", "
                + transcodeGradientFractions(paint.getFractions()) + ", " + colorsRep.toString()
                + ", " + cycleMethodRep + ", " + colorSpaceRep
                + ", " + transcodeTransform(paint.getTransform()) + ")";
    }

    private String transcodeGradientFractions(float[] fractions) {
        float previousFraction = -1.0f;
        for (float currentFraction : fractions) {
            if (currentFraction < 0f || currentFraction > 1f) {
                throw new IllegalArgumentException("Fraction values must be in the range 0 to 1: " + currentFraction);
            }

            if (currentFraction < previousFraction) {
                throw new IllegalArgumentException("Keyframe fractions must be non-decreasing: " + currentFraction);
            }

            previousFraction = currentFraction;
        }
        
        StringBuilder builder = new StringBuilder();
        if (fractions == null) {
            builder.append("null");
        } else {
            String sep = "";
            builder.append("new float[]{");
            previousFraction = -1.0f;
            for (float fraction : fractions) {
                builder.append(sep);
                if (fraction == previousFraction) {
                    fraction += 0.000000001f;
                }
                if (fraction == 0 || fraction == 1) {
                    builder.append((int) fraction);
                } else {
                    builder.append(fraction).append("f");
                }
                sep = ", ";

                previousFraction = fraction;
            }
            builder.append("}");
        }
        
        return builder.toString();
    }

    /**
     * Transcodes the specified point.
     * 
     * @param point
     */
    private String transcodePoint(Point2D point) {
        return "new Point2D.Double(" + transcodeDouble(point.getX()) + ", " + transcodeDouble(point.getY()) + ")";
    }

    /**
     * Transcodes the specified transform.
     * 
     * @param transform
     */
    private String transcodeTransform(AffineTransform transform) {
        double[] matrix = new double[6];
        transform.getMatrix(matrix);
        
        return "new AffineTransform("
                + transcodeFloat((float) matrix[0]) + ", " + transcodeFloat((float) matrix[1]) + ", "
                + transcodeFloat((float) matrix[2]) + ", " + transcodeFloat((float) matrix[3]) + ", "
                + transcodeFloat((float) matrix[4]) + ", " + transcodeFloat((float) matrix[5]) + ")";
    }

    /**
     * Transcodes the specified color.
     * 
     * @param color
     */
    private String transcodeColor(Color color) {
        if (color.equals(Color.WHITE)) {
            return "WHITE";
        } else if (color.equals(Color.BLACK)) {
            return "BLACK";
        } else if (color.equals(Color.RED)) {
            return "RED";
        } else if (color.equals(Color.GREEN)) {
            return "GREEN";
        } else if (color.equals(Color.BLUE)) {
            return "BLUE";
        } else if (color.equals(Color.LIGHT_GRAY)) {
            return "LIGHT_GRAY";
        } else if (color.equals(Color.GRAY)) {
            return "GRAY";
        } else if (color.equals(Color.DARK_GRAY)) {
            return "DARK_GRAY";
        } else if (color.equals(Color.YELLOW)) {
            return "YELLOW";
        } else if (color.equals(Color.CYAN)) {
            return "CYAN";
        } else if (color.equals(Color.MAGENTA)) {
            return "MAGENTA";
        } else if (color.getTransparency() == Transparency.OPAQUE) {
            return "new Color(0x" + Integer.toHexString(color.getRGB()).toUpperCase().substring(2) + ")";
        } else {
            return "new Color(0x" + Integer.toHexString(color.getRGB()).toUpperCase() + ", true)";
        }
    }

    /**
     * Transcodes the specified array.
     * 
     * @param colors
     */
    private String transcodeArray(Color[] colors) {
        String sep = "";
        StringBuilder builder = new StringBuilder();
        builder.append("new Color[]{");
        for (Color color : colors) {
            builder.append(sep);
            builder.append(transcodeColor(color));
            sep = ", ";
        }
        builder.append("}");

        return builder.toString();
    }

    /**
     * Transcodes the specified paint.
     *
     * @param paint Paint.
     * @throws UnsupportedOperationException if the paint is unsupported.
     */
    private String transcodePaint(Paint paint) throws UnsupportedOperationException {
        if (paint instanceof RadialGradientPaint) {
            return transcodeRadialGradientPaint((RadialGradientPaint) paint);
        } else if (paint instanceof LinearGradientPaint) {
            return transcodeLinearGradientPaint((LinearGradientPaint) paint);
        } else if (paint instanceof Color) {
            return transcodeColor((Color) paint);
        } else {
            throw new UnsupportedOperationException(paint.getClass().getCanonicalName());
        }
    }

    /**
     * Transcodes the specified stroke.
     * 
     * @param stroke
     */
    private String transcodeStroke(BasicStroke stroke) {
        if (stroke.getDashArray() == null) {
            return "new BasicStroke(" + transcodeFloat(stroke.getLineWidth()) + ", " + stroke.getEndCap() + ", "
                    + stroke.getLineJoin() + ", " + transcodeFloat(stroke.getMiterLimit()) + ")";
        } else {
            StringBuilder dashRep = new StringBuilder();
            if (stroke.getDashArray() == null) {
                dashRep.append("null");
            } else {
                String sep = "";
                dashRep.append("new float[]{");
                for (float dash : stroke.getDashArray()) {
                    dashRep.append(sep);
                    dashRep.append(transcodeFloat(dash));
                    sep = ", ";
                }
                dashRep.append("}");
            }
            
            return "new BasicStroke(" + transcodeFloat(stroke.getLineWidth()) + ", " + stroke.getEndCap() + ", "
                    + stroke.getLineJoin() + ", " + transcodeFloat(stroke.getMiterLimit()) + ", "
                    + dashRep + ", " + transcodeFloat(stroke.getDashPhase()) + ")";
        }
        
    }

    /**
     * Transcode the specified float value.
     * 
     * @return
     */
    private String transcodeFloat(float f) {
        if (Math.abs(Math.round(f) - f) < 0.000001) {
            return String.valueOf(Math.round(f));
        } else {
            return String.valueOf(f) + "f";
        }
    }

    /**
     * Transcode the specified float value.
     * 
     * @return
     */
    private String transcodeDouble(double d) {
        if (Math.abs(Math.round(d) - d) < 0.000001) {
            return String.valueOf(Math.round(d));
        } else {
            return String.valueOf(d);
        }
    }

    /**
     * Transcodes the specified shape painter.
     *
     * @param painter Shape painter.
     * @throws UnsupportedOperationException if the shape painter is unsupported.
     */
    private void transcodeShapePainter(ShapePainter painter) throws UnsupportedOperationException {
        if (painter instanceof CompositeShapePainter) {
            transcodeCompositeShapePainter((CompositeShapePainter) painter);
        } else if (painter instanceof FillShapePainter) {
            transcodeFillShapePainter((FillShapePainter) painter);
        } else if (painter instanceof StrokeShapePainter) {
            transcodeStrokeShapePainter((StrokeShapePainter) painter);
        } else if (painter != null) {
            throw new UnsupportedOperationException(painter.getClass().getCanonicalName());
        }
    }

    /**
     * Transcodes the specified composite shape painter.
     *
     * @param painter Composite shape painter.
     */
    private void transcodeCompositeShapePainter(CompositeShapePainter painter) {
        for (int i = 0; i < painter.getShapePainterCount(); i++) {
            transcodeShapePainter(painter.getShapePainter(i));
        }
    }

    /**
     * Transcodes the specified fill shape painter.
     *
     * @param painter Fill shape painter.
     */
    private void transcodeFillShapePainter(FillShapePainter painter) {
        Paint paint = (Paint) painter.getPaint();
        if (paint == null) {
            return;
        }
        
        transcodeShape(painter.getShape());
        
        String p = transcodePaint(paint);
        if (!p.equals(currentPaint)) {
            currentPaint = p;
            printWriter.println("g.setPaint(" + currentPaint + ");");
        }
        printWriter.println("g.fill(shape);");
    }

    /**
     * Transcodes the specified stroke shape painter.
     *
     * @param painter Stroke shape painter.
     */
    private void transcodeStrokeShapePainter(StrokeShapePainter painter) {
        Shape shape = painter.getShape();
        Paint paint = (Paint) painter.getPaint();
        if (paint == null) {
            return;
        }
        
        transcodeShape(shape);

        String p = transcodePaint(paint);
        if (!p.equals(currentPaint)) {
            currentPaint = p;
            printWriter.println("g.setPaint(" + currentPaint + ");");
        }
        
        String stroke = transcodeStroke((BasicStroke) painter.getStroke());
        if (stroke == null && currentStroke != null || stroke != null && !stroke.equals(currentStroke)) {
            currentStroke = stroke;
            printWriter.println("g.setStroke(" + stroke + ");");
        }
        printWriter.println("g.draw(shape);");
    }

    /**
     * Transcodes the specified shape node.
     *
     * @param node    Shape node.
     * @param comment Comment (for associating the Java2D section with the corresponding SVG section).
     */
    private void transcodeShapeNode(ShapeNode node, String comment) {
        transcodeShapePainter(node.getShapePainter());
    }

    /**
     * Transcodes the specified composite graphics node.
     *
     * @param node    Composite graphics node.
     * @param comment Comment (for associating the Java2D section with the corresponding SVG section).
     */
    private void transcodeCompositeGraphicsNode(CompositeGraphicsNode node, String comment) {
        int count = 0;
        for (Object obj : node.getChildren()) {
            transcodeGraphicsNode((GraphicsNode) obj, comment + "_" + count);
            count++;
        }
    }

    /**
     * Transcodes the specified graphics node.
     *
     * @param node    Graphics node.
     * @param comment Comment (for associating the Java2D section with the corresponding SVG section).
     * @throws UnsupportedOperationException if the graphics node is unsupported.
     */
    private void transcodeGraphicsNode(GraphicsNode node, String comment) throws UnsupportedOperationException {
        AlphaComposite composite = (AlphaComposite) node.getComposite();
        if (composite != null && !composite.equals(currentComposite) && !(currentComposite == null && composite.getAlpha() == 1)) {
            currentComposite = composite;
            printWriter.println("g.setComposite(AlphaComposite.getInstance(" + composite.getRule() + ", " + transcodeFloat(composite.getAlpha()) + " * origAlpha));");
        }
        
        AffineTransform transform = node.getTransform();
        if (transform != null && !transform.isIdentity()) {
            printWriter.println("transformations.offer(g.getTransform());");
            printWriter.println("g.transform(" + transcodeTransform(transform) + ");");
        }
        
        try {
            printWriter.println("");
            printWriter.println("// " + comment);
            if (node instanceof ShapeNode) {
                transcodeShapeNode((ShapeNode) node, comment);
            } else if (node instanceof CompositeGraphicsNode) {
                transcodeCompositeGraphicsNode((CompositeGraphicsNode) node, comment);
            } else {
                throw new UnsupportedOperationException(node.getClass().getCanonicalName());
            }
        } finally {
            if (transform != null && !transform.isIdentity()) {
                printWriter.println("g.setTransform(transformations.poll());");
            }
        }
    }
}

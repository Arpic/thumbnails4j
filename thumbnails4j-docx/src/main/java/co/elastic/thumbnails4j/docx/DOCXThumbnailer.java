package co.elastic.thumbnails4j.docx;

import co.elastic.thumbnails4j.core.Dimensions;
import co.elastic.thumbnails4j.core.ThumbnailUtils;
import co.elastic.thumbnails4j.core.Thumbnailer;
import co.elastic.thumbnails4j.core.ThumbnailingException;
import fr.opensagres.poi.xwpf.converter.xhtml.XHTMLConverter;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class DOCXThumbnailer implements Thumbnailer {
    Logger logger = LoggerFactory.getLogger(DOCXThumbnailer.class);

    @Override
    public List<BufferedImage> getThumbnails(File input, List<Dimensions> dimensions) throws ThumbnailingException {
        FileInputStream fis;
        try {
            fis = new FileInputStream(input);
        } catch (FileNotFoundException e) {
            logger.error("Could not find file {}", input.getAbsolutePath());
            logger.error("With stack: ", e);
            throw new IllegalArgumentException(e);
        }
        return getThumbnails(fis, dimensions);
    }

    @Override
    public List<BufferedImage> getThumbnails(InputStream input, List<Dimensions> dimensions) throws ThumbnailingException {
        List<BufferedImage> results = new ArrayList<>();
        try {
            XWPFDocument docx = new XWPFDocument(input);
            InputStream imageStream = docx.getProperties().getThumbnailImage();
            if (imageStream==null) {
                byte[] htmlBytes = htmlBytesFromDocx(docx);
                for(Dimensions singleDimension: dimensions){
                    Dimensions expectedDimensions = docxPageDimensions(docx, singleDimension);
                    BufferedImage image = ThumbnailUtils.scaleHtmlToImage(htmlBytes, expectedDimensions);
                    results.add(ThumbnailUtils.scaleImage(image, singleDimension));
                }
            } else {
                BufferedImage image = ImageIO.read(imageStream);
                for(Dimensions singleDimension: dimensions) {
                    results.add(ThumbnailUtils.scaleImage(image, singleDimension));
                }

            }
        } catch (IOException e) {
            logger.error("Failed to read thumbnails from DOCX", e);
            throw new ThumbnailingException(e);
        }
        return results;
    }

    private Dimensions docxPageDimensions(XWPFDocument docx, Dimensions dimensions){
        CTPageSz pageSz = null;
        try {
            pageSz = docx.getDocument().getBody().getSectPr().getPgSz();
        } catch (NullPointerException e){
            logger.debug("No page size detected for DOCX document");
        }
        if (pageSz == null){
            return dimensions;
        } else {
            return new Dimensions(pageSz.getW().intValue(), pageSz.getH().intValue());
        }

    }

    private byte[] htmlBytesFromDocx(XWPFDocument docx) throws IOException {
        ByteArrayOutputStream htmlStream = new ByteArrayOutputStream();
        XHTMLConverter.getInstance().convert(docx, htmlStream, null);
        return htmlStream.toByteArray();
    }
}

package com.xmlcalabash.extensions.marklogic;

import com.xmlcalabash.core.XMLCalabash;
import com.xmlcalabash.util.Base64;
import com.xmlcalabash.util.S9apiUtils;
import com.xmlcalabash.util.XProcURIResolver;
import net.sf.saxon.s9api.*;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.Configuration;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.library.DefaultStep;
import com.xmlcalabash.util.TreeWriter;
import com.xmlcalabash.model.RuntimeValue;
import com.marklogic.xcc.*;
import com.marklogic.xcc.types.*;
import com.marklogic.xcc.types.XdmItem;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXSource;
import java.net.URI;
import java.net.URISyntaxException;
import java.io.*;
import java.net.URL;
import java.util.Hashtable;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;


/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Nov 4, 2008
 * Time: 11:24:59 AM
 * To change this template use File | Settings | File Templates.
 */

@XMLCalabash(
        name = "ml:insert-document",
        type = "{http://xmlcalabash.com/ns/extensions/marklogic}insert-document")

public class XCCInsertDocument extends XCCStep {
    private static final QName _encoding = new QName("encoding");
    private static final QName _bufferSize = new QName("","buffer-size");
    private static final QName _collections = new QName("","collections");
    private static final QName _format = new QName("","format");
    private static final QName _language = new QName("","language");
    private static final QName _locale = new QName("","locale");
    private static final QName _uri = new QName("","uri");

    public XCCInsertDocument(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);
    }

    public void run() throws SaxonApiException {
        super.run();

        String format = "xml";
        if (getOption(_format) != null) {
            format = getOption(_format).getString();
        }
        if (!"xml".equals(format) && !"text".equals(format) && !"binary".equals(format)) {
            throw new UnsupportedOperationException("Format must be 'xml', 'text', or 'binary'.");
        }

        XdmNode doc = source.read();
        XdmNode root = S9apiUtils.getDocumentElement(doc);

        String docstring = null;
        byte[] docbinary = null;

        if ("xml".equals(format)) {
            Serializer serializer = makeSerializer();
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            serializer.setOutputStream(stream);
            S9apiUtils.serialize(runtime, doc, serializer);

            try {
                docstring = stream.toString("UTF-8");
            } catch (UnsupportedEncodingException uee) {
                // This can't happen...
                throw new XProcException(uee);
            }
        } else if ("text".equals(format)) {
            docstring = doc.getStringValue();
        } else {
            if ("base64".equals(root.getAttributeValue(_encoding))) {
                docbinary = Base64.decode(doc.getStringValue());
            } else if (root.getAttributeValue(_encoding) == null) {
                docstring = root.getStringValue();
            } else {
                throw new UnsupportedOperationException("Binary content must be base64 encoded.");
            }
        }

        ContentCreateOptions options = ContentCreateOptions.newXmlInstance();

        if ("xml".equals(format)) {
            options.setFormatXml();
            options.setEncoding("UTF-8");
        }

        if ("text".equals(format)) {
            options.setFormatText();
            options.setEncoding("UTF-8");
        }

        if ("binary".equals(format)) {
            options.setFormatBinary();
        }

        if (getOption(_bufferSize) != null) {
            options.setBufferSize(getOption(_bufferSize).getInt());
        }
        if (getOption(_collections) != null) {
            String[] collections = getOption(_collections).getString().split("\\s+");
            options.setCollections(collections);
        }
        if (getOption(_language) != null) {
            options.setLanguage(getOption(_language).getString());
        }
        if (getOption(_locale) != null) {
            String value = getOption(_locale).getString();
            Locale locale = new Locale(value);
            options.setLocale(locale);
        }

        String dburi = getOption(_uri).getString();

        Content content = null;
        if (docbinary == null) {
            content = ContentFactory.newContent(dburi, docstring, options);
        } else {
            content = ContentFactory.newContent(dburi, docbinary, options);
        }

        ContentSource contentSource = constructContentSource();

        try {
            Session session = contentSource.newSession ();
            session.insertContent(content);
            session.close();
        } catch (Exception e) {
            throw new XProcException(e);
        }

        TreeWriter tree = new TreeWriter(runtime);
        tree.startDocument(step.getNode().getBaseURI());
        tree.addStartElement(XProcConstants.c_result);
        tree.startContent();
        tree.addText(dburi);
        tree.addEndElement();
        tree.endDocument();
        result.write(tree.getResult());
    }

    public static void configureStep(XProcRuntime runtime) {
        XProcURIResolver resolver = runtime.getResolver();
        URIResolver uriResolver = resolver.getUnderlyingURIResolver();
        URIResolver myResolver = new StepResolver(uriResolver);
        resolver.setUnderlyingURIResolver(myResolver);
    }

    private static class StepResolver implements URIResolver {
        Logger logger = LoggerFactory.getLogger(XCCInsertDocument.class);
        URIResolver nextResolver = null;

        public StepResolver(URIResolver next) {
            nextResolver = next;
        }

        @Override
        public Source resolve(String href, String base) throws TransformerException {
            try {
                URI baseURI = new URI(base);
                URI xpl = baseURI.resolve(href);
                if (library_xpl.equals(xpl.toASCIIString())) {
                    URL url = XCCInsertDocument.class.getResource(library_url);
                    logger.debug("Reading library.xpl for ml:insert-document from " + url);
                    InputStream s = XCCInsertDocument.class.getResourceAsStream(library_url);
                    if (s != null) {
                        SAXSource source = new SAXSource(new InputSource(s));
                        return source;
                    } else {
                        logger.info("Failed to read " + library_url + " for ml:insert-document");
                    }
                }
            } catch (URISyntaxException e) {
                // nevermind
            }

            if (nextResolver != null) {
                return nextResolver.resolve(href, base);
            } else {
                return null;
            }
        }
    }
}
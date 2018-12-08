package com.xmlcalabash.extensions.marklogic;

import com.marklogic.xcc.ContentSource;
import com.marklogic.xcc.ContentSourceFactory;
import com.marklogic.xcc.Request;
import com.marklogic.xcc.ResultItem;
import com.marklogic.xcc.ResultSequence;
import com.marklogic.xcc.Session;
import com.marklogic.xcc.ValueFactory;
import com.marklogic.xcc.types.XName;
import com.marklogic.xcc.types.XSString;
import com.marklogic.xcc.types.XdmBinary;
import com.marklogic.xcc.types.XdmDocument;
import com.marklogic.xcc.types.XdmElement;
import com.marklogic.xcc.types.XdmVariable;
import com.xmlcalabash.core.XMLCalabash;
import com.xmlcalabash.util.Base64;
import com.xmlcalabash.util.XProcURIResolver;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
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
import com.marklogic.xcc.types.XdmItem;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXSource;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Hashtable;

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
        name = "ml:adhoc-query",
        type = "{http://xmlcalabash.com/ns/extensions/marklogic}adhoc-query")

public class XCCAdhocQuery extends XCCStep {
    public XCCAdhocQuery(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);
    }

    public void run() throws SaxonApiException {
        super.run();

        XdmNode queryDocument = source.read();
        String queryString = queryDocument.getStringValue();

        ContentSource contentSource = constructContentSource();

        try {
            Session session = contentSource.newSession ();
            Request request = session.newAdhocQuery (queryString);
            processParams(request);

            ResultSequence rs = session.submitRequest (request);
            serializeResultSequence(rs);

            session.close();
        } catch (Exception e) {
            throw new XProcException(e);
        }
    }

    public static void configureStep(XProcRuntime runtime) {
        XProcURIResolver resolver = runtime.getResolver();
        URIResolver uriResolver = resolver.getUnderlyingURIResolver();
        URIResolver myResolver = new StepResolver(uriResolver);
        resolver.setUnderlyingURIResolver(myResolver);
    }

    private static class StepResolver implements URIResolver {
        Logger logger = LoggerFactory.getLogger(XCCAdhocQuery.class);
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
                    URL url = XCCAdhocQuery.class.getResource(library_url);
                    logger.debug("Reading library.xpl for ml:adhoc-query from " + url);
                    InputStream s = XCCAdhocQuery.class.getResourceAsStream(library_url);
                    if (s != null) {
                        SAXSource source = new SAXSource(new InputSource(s));
                        return source;
                    } else {
                        logger.info("Failed to read " + library_url + " for ml:adhoc-query");
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




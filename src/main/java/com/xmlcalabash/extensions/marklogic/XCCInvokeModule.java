package com.xmlcalabash.extensions.marklogic;

import com.xmlcalabash.core.XMLCalabash;
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
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.io.StringReader;
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
        name = "ml:invoke-module",
        type = "{http://xmlcalabash.com/ns/extensions/marklogic}invoke-module")

public class XCCInvokeModule extends XCCStep {
    private static final QName _module = new QName("", "module");

    public XCCInvokeModule(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);
    }

    public void run() throws SaxonApiException {
        super.run();

        String module = getOption(_module).getString();

        ContentSource contentSource = constructContentSource();

        try {
            Session session = contentSource.newSession ();
            Request request = session.newModuleInvoke(module);
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
        Logger logger = LoggerFactory.getLogger(XCCInvokeModule.class);
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
                    URL url = XCCInvokeModule.class.getResource(library_url);
                    logger.debug("Reading library.xpl for ml:invoke-module from " + url);
                    InputStream s = XCCInvokeModule.class.getResourceAsStream(library_url);
                    if (s != null) {
                        SAXSource source = new SAXSource(new InputSource(s));
                        return source;
                    } else {
                        logger.info("Failed to read " + library_url + " for ml:invoke-module");
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
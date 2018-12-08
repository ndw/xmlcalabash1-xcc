package com.xmlcalabash.extensions.marklogic;

import com.marklogic.xcc.ContentSource;
import com.marklogic.xcc.ContentSourceFactory;
import com.marklogic.xcc.Request;
import com.marklogic.xcc.ResultItem;
import com.marklogic.xcc.ResultSequence;
import com.marklogic.xcc.ValueFactory;
import com.marklogic.xcc.types.XName;
import com.marklogic.xcc.types.XSString;
import com.marklogic.xcc.types.XdmBinary;
import com.marklogic.xcc.types.XdmDocument;
import com.marklogic.xcc.types.XdmElement;
import com.marklogic.xcc.types.XdmItem;
import com.marklogic.xcc.types.XdmVariable;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.library.DefaultStep;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.Base64;
import com.xmlcalabash.util.TreeWriter;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.net.URI;
import java.util.Hashtable;

abstract public class XCCStep extends DefaultStep {
    private static final QName _user = new QName("", "user");
    private static final QName _password = new QName("", "password");
    private static final QName _host = new QName("", "host");
    private static final QName _port = new QName("", "port");
    private static final QName _contentBase = new QName("", "content-base");
    private static final QName _wrapper = new QName("", "wrapper");
    private static final QName _encoding = new QName("encoding");
    private static final QName _auth_method = new QName("auth-method");
    private static final QName c_encoding = new QName("c", XProcConstants.NS_XPROC_STEP, "encoding");

    protected static final String library_xpl = "http://xmlcalabash.com/extension/steps/marklogic-xcc.xpl";
    protected static final String library_url = "/com/xmlcalabash/extensions/xcc/library.xpl";

    protected ReadablePipe source = null;
    protected WritablePipe result = null;
    protected Hashtable<QName,String> params = new Hashtable<QName, String> ();

    protected String host = null;
    protected int port = 0;
    protected String user = null;
    protected String password = null;
    protected String contentBase = null;
    protected QName wrapper = XProcConstants.c_result;

    public XCCStep(XProcRuntime runtime, XAtomicStep step) {
        super(runtime, step);
    }

    public void setInput(String port, ReadablePipe pipe) {
        source = pipe;
    }

    public void setOutput(String port, WritablePipe pipe) {
        result = pipe;
    }

    public void setParameter(QName name, RuntimeValue value) {
        params.put(name, value.getString());
    }

    public void reset() {
        if (source != null) {
            source.resetReader();
        }
        if (result != null) {
            result.resetWriter();
        }
    }

    public void run() throws SaxonApiException {
        super.run();

        host = getOption(_host, "");
        port = getOption(_port, 0);
        user = getOption(_user, "");
        password = getOption(_password, "");
        contentBase = getOption(_contentBase, "");

        if (getOption(_wrapper) != null) {
            wrapper = getOption(_wrapper).getQName();
        }
    }

    protected ContentSource constructContentSource() {
        ContentSource contentSource = null;

        try {
            // FIXME: for secure connections, xccs:
            URI uri = URI.create("xcc://" + user + ":" + password + "@" + host + ":" + port + "/" + contentBase);
            contentSource = ContentSourceFactory.newContentSource(uri);
        } catch (Exception e) {
            throw new XProcException(e);
        }

        if ("basic".equals(getOption(_auth_method, ""))) {
            contentSource.setAuthenticationPreemptive(true);
        }

        return contentSource;
    }

    protected void processParams(Request request) {
        for (QName name : params.keySet()) {
            XSString value = ValueFactory.newXSString (params.get(name));
            XName xname = new XName(name.getNamespaceURI(), name.getLocalName());
            XdmVariable myVariable = ValueFactory.newVariable (xname, value);
            request.setVariable (myVariable);
        }
    }

    protected void serializeResultSequence(ResultSequence rs) {
        while (rs.hasNext()) {
            ResultItem rsItem = rs.next();
            XdmItem item = rsItem.getItem();

            // FIXME: This needs work...
            if (item instanceof XdmDocument || item instanceof XdmElement) {

                StringReader sr = new StringReader(item.asString());
                XdmNode xccXML = runtime.parse(new InputSource(sr));

                result.write(xccXML);
            } else if (item instanceof XdmBinary) {
                String base64 = Base64.encodeBytes(((XdmBinary) item).asBinaryData());
                TreeWriter treeWriter = new TreeWriter(runtime);
                treeWriter.startDocument(step.getNode().getBaseURI());
                treeWriter.addStartElement(wrapper);

                if (XProcConstants.NS_XPROC_STEP.equals(wrapper.getNamespaceURI())) {
                    treeWriter.addAttribute(_encoding, "base64");
                } else {
                    treeWriter.addAttribute(c_encoding, "base64");
                }

                treeWriter.startContent();
                treeWriter.addText(base64);
                treeWriter.addEndElement();
                treeWriter.endDocument();
                XdmNode node = treeWriter.getResult();
                result.write(node);
            } else {
                String text = item.asString();
                TreeWriter treeWriter = new TreeWriter(runtime);
                treeWriter.startDocument(step.getNode().getBaseURI());
                treeWriter.addStartElement(wrapper);
                treeWriter.startContent();
                treeWriter.addText(text);
                treeWriter.addEndElement();
                treeWriter.endDocument();
                XdmNode node = treeWriter.getResult();
                result.write(node);
            }
        }
    }


}

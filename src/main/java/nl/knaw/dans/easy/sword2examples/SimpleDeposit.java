package nl.knaw.dans.easy.sword2examples;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.net.URI;
import java.util.Iterator;

public class SimpleDeposit {
    private static final String BAGIT_URI = "http://purl.org/net/sword/package/BagIt";

    /**
     * Sends a bag to the easy-sword2 service and tracks its status until it is archived or failure is reported.
     *
     * @param args 0. bag to send, 1. collection URL (Col-IRI), 2. EASY user name, 3. EASY password
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            System.err.println("Usage: java nl.knaw.dans.easy.sword2examples.SimpleDeposit <bag filename> <Col-IRI> <EASY uid> <EASY passwd>");
            System.exit(1);
        }

        final String bagFileName = args[0];
        final URI colIri = new URI(args[1]);
        final String uid = args[2];
        final String pw = args[3];


        // 0. Calculated bag MD5.
        File bag = new File(bagFileName);
        FileInputStream fis = new FileInputStream(bag);
        String md5;
        try {
            System.out.println("Calculating MD5 of bag ...");
            md5 = DigestUtils.md5Hex(fis);
            System.out.println("Bag MD5 (hex) = " + md5);
        } finally {
            fis.close();
        }

        // 1. Send package to the collection URL
        System.out.println("Sending bag to Col-IRI: " + colIri + " ...");
        BasicCredentialsProvider credsProv = new BasicCredentialsProvider();
        credsProv.setCredentials(new AuthScope(colIri.getHost(), colIri.getPort()), new UsernamePasswordCredentials(uid, pw));
        CloseableHttpClient http = HttpClients.custom().setDefaultCredentialsProvider(credsProv).build();
        HttpPost post = new HttpPost(colIri);
        post.addHeader("Content-Disposition", "attachment; filename=bag.zip");
        post.addHeader("Content-MD5", md5);
        post.addHeader("Packaging", BAGIT_URI);
        post.setEntity(new FileEntity(bag, ContentType.create("application/zip")));
        CloseableHttpResponse response = http.execute(post);

        // 2. Check the response. If transfer corrupt (MD5 doesn't check out), report and end.
        String bodyText = readEntityAsString(response.getEntity());
        if (response.getStatusLine().getStatusCode() != 201) {
            System.err.println("Deposit FAILED. Status = " + response.getStatusLine());
            System.err.println("Response body follows:");
            System.err.println(bodyText);
            System.exit(2);
        }

        // 3. Get the statement URL. This is the URL from which to retrieve the current status of the deposit.
        System.out.println("Deposit receipt follows:");
        System.out.println(bodyText);
        URI statIri = new URI(getStringFromXml(bodyText, "//*[local-name() = 'link' and @rel = 'http://purl.org/net/sword/terms/statement']/@href"));
        System.out.println("Stat-IRI = " + statIri);

        // 4. Check statement every two seconds (a bit too frantic, but okay for this test). If status changes:
        // report new status. If status is an error (INVALID, REJECTED, FAILED) or ARCHIVED: end.
        String state = null;
        while (true) {
            Thread.sleep(2000);
            System.out.println("Retrieving current status from Stat-IRI: " + statIri + " ...");
            response = http.execute(new HttpGet(statIri));
            bodyText = readEntityAsString(response.getEntity());
            String newState = getStringFromXml(bodyText, "//*[local-name() = 'category' and @scheme = 'http://purl.org/net/sword/terms/state' and @label = 'State']/@term");
            if (!newState.equals(state)) {
                System.out.println("Current state = " + newState);
                state = newState;

                if (state.equals("INVALID") || state.equals("REJECTED") || state.equals("FAILED")) {
                    System.err.println("FAILURE. Complete statement follows:");
                    System.err.println(bodyText);
                    System.exit(3);
                } else if (state.equals("ARCHIVED")) {
                    System.out.println("SUCCESS. Deposit has been archived at: " + getStringFromXml(bodyText, "//*[local-name() = 'category' and @scheme = 'http://purl.org/net/sword/terms/state' and @label = 'State']"));
                    System.exit(0);
                }
            }
        }
    }

    public static String readEntityAsString(HttpEntity entity) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        IOUtils.copy(entity.getContent(), bos);
        return new String(bos.toByteArray(), "UTF-8");
    }

    public static String getStringFromXml(String xml, String expr) throws Exception {
        InputSource xmlSource = new InputSource(new StringReader(xml));
        XPathFactory xpathFactory = XPathFactory.newInstance();
        XPath xpath = xpathFactory.newXPath();
        return (String) xpath.evaluate(expr, xmlSource, XPathConstants.STRING);
    }
}

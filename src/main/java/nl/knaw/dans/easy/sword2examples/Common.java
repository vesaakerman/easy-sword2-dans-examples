/**
 * Copyright (C) 2016-17 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.sword2examples;

import gov.loc.repository.bagit.Bag;
import gov.loc.repository.bagit.BagFactory;
import gov.loc.repository.bagit.BagInfoTxt;
import gov.loc.repository.bagit.transformer.impl.TagManifestCompleter;
import gov.loc.repository.bagit.writer.impl.FileSystemWriter;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import org.apache.abdera.Abdera;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.Category;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Element;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.model.Link;
import org.apache.abdera.parser.Parser;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.security.DigestInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Common {
    static final String BAGIT_URI = "http://purl.org/net/sword/package/BagIt";
    static final BagFactory bagFactory = new BagFactory();

    /**
     * Assumes the entity is UTF-8 encoded text and reads it into a String.
     *
     * @param entity
     *        the http entity object
     * @return the entire http entity as a string
     * @throws IOException if an I/O error occurs
     */
    public static String readEntityAsString(HttpEntity entity) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        IOUtils.copy(entity.getContent(), bos);
        return new String(bos.toByteArray(), "UTF-8");
    }

    public static <T extends Element> T parse(String text) {
        Abdera abdera = Abdera.getInstance();
        Parser parser = abdera.getParser();
        Document<T> receipt = parser.parse(new StringReader(text));
        return receipt.getRoot();
    }

    static URI trackDeposit(CloseableHttpClient http, URI statUri) throws Exception {
        CloseableHttpResponse response;
        String bodyText;
        System.out.println("Start polling Stat-IRI for the current status of the deposit, waiting 10 seconds before every request ...");
        while (true) {
            Thread.sleep(10000);
            System.out.print("Checking deposit status ... ");
            response = http.execute(new HttpGet(statUri));
            if (response.getStatusLine().getStatusCode() != 200) {
                System.out.println("Stat-IRI returned " + response.getStatusLine().getStatusCode());
                System.exit(1);
            }
            bodyText = readEntityAsString(response.getEntity());
            Feed statement = parse(bodyText);
            List<Category> states = statement.getCategories("http://purl.org/net/sword/terms/state");
            if (states.isEmpty()) {
                System.err.println("ERROR: NO STATE FOUND");
                System.exit(1);
            }
            else if (states.size() > 1) {
                System.err.println("ERROR: FOUND TOO MANY STATES (" + states.size() + "). CAN ONLY HANDLE ONE");
                System.exit(1);
            }
            else {
                String state = states.get(0).getTerm();
                System.out.println(state);
                if (state.equals("INVALID") || state.equals("REJECTED") || state.equals("FAILED")) {
                    System.err.println("FAILURE. Complete statement follows:");
                    System.err.println(bodyText);
                    System.exit(3);
                }
                else if (state.equals("ARCHIVED")) {
                    List<Entry> entries = statement.getEntries();
                    System.out.println("SUCCESS. ");
                    if (entries.size() == 1) {
                        System.out.print("Deposit has been archived at: [" + entries.get(0).getId() + "]. ");

                        List<String> dois = getDois(entries.get(0));
                        int numDois = dois.size();
                        switch (numDois) {
                            case 1: System.out.print(" With DOI: [" + dois.get(0) + "]. ");
                                    break;
                            case 0: System.out.println("WARNING: No DOI found");
                                break;

                            default:
                                System.out.println("WARNING: More than one DOI found (" + numDois + "): ");
                                for (String doi: dois) {
                                    System.out.print(" [" + doi + "]");
                                }
                                System.out.println();
                                break;
                        }
                    } else {
                        System.out.println("WARNING: Found (" + entries.size() + ") entry's; should be ONE and only ONE");
                    }
                    String stateText = states.get(0).getText();
                    System.out.println("Dataset landing page will be located at: [" + stateText + "].");
                    System.out.println("Complete statement follows:");
                    System.out.println(bodyText);
                    return entries.get(0).getId().toURI();
                }
            }
        }
    }

    public static List<String> getDois(Entry entry) {
        List<String> dois = new ArrayList<String>();

        List<Link> links = entry.getLinks("self");
        for(Link link: links) {
            IRI href = link.getHref();
            if (href.getHost().equals("doi.org")) {
                String path = href.getPath();
                String doi = path.substring(1); // skip leading '/'
                dois.add(doi);
            }
        }
        return dois;
    }

    private static byte[] readChunk(InputStream is, int size) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] bytes = new byte[size];
        int c = is.read(bytes);
        bos.write(bytes, 0, c);
        return bos.toByteArray();
    }

    public static CloseableHttpClient createHttpClient(URI uri, String uid, String pw) {
        BasicCredentialsProvider credsProv = new BasicCredentialsProvider();
        credsProv.setCredentials(new AuthScope(uri.getHost(), uri.getPort()), new UsernamePasswordCredentials(uid, pw));
        return HttpClients.custom().setDefaultCredentialsProvider(credsProv).build();
    }

    public static CloseableHttpResponse sendChunk(DigestInputStream dis, int size, String method, URI uri, String filename, String mimeType, CloseableHttpClient http, boolean inProgress) throws Exception {
        // System.out.println(String.format("Sending chunk to %s, filename = %s, chunk size = %d, MIME-Type = %s, In-Progress = %s ... ", uri.toString(), filename, size, mimeType, Boolean.toString(inProgress)));
        byte[] chunk = readChunk(dis, size);
        String md5 = new String(Hex.encodeHex(dis.getMessageDigest().digest()));
        HttpUriRequest request = RequestBuilder.create(method).setUri(uri).setConfig(RequestConfig.custom()
        /*
         * When using an HTTPS-connection EXPECT-CONTINUE must be enabled, otherwise buffer overflow may follow
         */
                .setExpectContinueEnabled(true).build()) //
                    .addHeader("Content-Disposition", String.format("attachment; filename=%s", filename)) //
                    .addHeader("Content-MD5", md5) //
                    .addHeader("Packaging", BAGIT_URI) //
                    .addHeader("In-Progress", Boolean.toString(inProgress)) //
                    .setEntity(new ByteArrayEntity(chunk, ContentType.create(mimeType))) //
                .build();
        CloseableHttpResponse response = http.execute(request);
        // System.out.println("Response received.");
        return response;
    }

    public static void setBagIsVersionOf(File bagDir, URI versionOfUri) throws Exception {
        Bag bag = bagFactory.createBag(bagDir);
        BagInfoTxt info = bag.getBagInfoTxt();
        info.put("Is-Version-Of", versionOfUri.toASCIIString());

        // bag-info.txt's checksums have changed, so we need to update the tag manifests.
        TagManifestCompleter completer = new TagManifestCompleter(bagFactory);
        completer.complete(bag);
        FileSystemWriter writer = new FileSystemWriter(bagFactory);
        writer.setTagFilesOnly(true);
        bag.write(writer, bagDir);
    }

    public static void zipDirectory(File dir, File zipFile) throws Exception {
        if(zipFile.exists()) zipFile.delete();
        ZipFile zf = new ZipFile(zipFile);
        ZipParameters parameters = new ZipParameters();
        zf.addFolder(dir, parameters);
    }

    public static File copyToTarget(File dir) throws Exception {
        File dirInTarget = new File("target", dir.getName());
        FileUtils.deleteQuietly(dirInTarget);
        FileUtils.copyDirectory(dir, dirInTarget);
        return dirInTarget;
    }
}

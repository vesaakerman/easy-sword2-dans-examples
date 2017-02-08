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

import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Link;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.security.DigestInputStream;
import java.security.MessageDigest;

public class ContinuedDeposit {
    public static void main(String[] args) throws Exception {
        if (args.length != 5) {
            System.err.println("Usage: java nl.knaw.dans.easy.sword2examples.ContinuedDeposit <Col-IRI> <EASY uid> <EASY passwd> <chunk size> <bag dirname>");
            System.exit(1);
        }

        // 0. Read command line arguments
        final IRI colIri = new IRI(args[0]);
        final String uid = args[1];
        final String pw = args[2];
        final int chunkSize = Integer.parseInt(args[3]);
        final String bagFileName = args[4];

        depositPackage(new File(bagFileName), colIri, uid, pw, chunkSize);
    }

    public static URI depositPackage(File bagDir, IRI colIri, String uid, String pw, int chunkSize) throws Exception {
        // 0. Zip the bagDir
        File zipFile = new File(bagDir.getAbsolutePath() + ".zip");
        zipFile.delete();
        Common.zipDirectory(bagDir, zipFile);

        // 1. Set up stream for calculating MD5
        FileInputStream fis = new FileInputStream(zipFile);
        MessageDigest md = MessageDigest.getInstance("MD5");
        DigestInputStream dis = new DigestInputStream(fis, md);

        // 2. Post first chunk bag to Col-IRI
        CloseableHttpClient http = Common.createHttpClient(colIri.toURI(), uid, pw);
        CloseableHttpResponse response = Common.sendChunk(dis, chunkSize, "POST", colIri.toURI(),  "bag.zip.1", "application/octet-stream", http, chunkSize < zipFile.length());

        // 3. Check the response. If transfer corrupt (MD5 doesn't check out), report and exit.
        String bodyText = Common.readEntityAsString(response.getEntity());
        if (response.getStatusLine().getStatusCode() != 201) {
            System.err.println("FAILED. Status = " + response.getStatusLine());
            System.err.println("Response body follows:");
            System.err.println(bodyText);
            System.exit(2);
        }
        System.out.println("SUCCESS. Deposit receipt follows:");
        System.out.println(bodyText);

        Entry receipt = Common.parse(bodyText);
        Link seIriLink = receipt.getLink("edit");
        URI seIri = seIriLink.getHref().toURI();

        int remaining = (int) zipFile.length() - chunkSize;
        int count = 2;
        while(remaining > 0) {
            System.out.print(String.format("POST-ing chunk of %d bytes to SE-IRI (remaining: %d) ... ", chunkSize, remaining));
            response = Common.sendChunk(dis, chunkSize, "POST", seIri, "bag.zip." + count++, "application/octet-stream", http, remaining > chunkSize);
            remaining -= chunkSize;
            bodyText = Common.readEntityAsString(response.getEntity());
            if (response.getStatusLine().getStatusCode() != 200) {
                System.err.println("FAILED. Status = " + response.getStatusLine());
                System.err.println("Response body follows:");
                System.err.println(bodyText);
                System.exit(2);
            }
            System.out.println("SUCCESS.");
        }

        // 4. Get the statement URL. This is the URL from which to retrieve the current status of the deposit.
        System.out.println("Retrieving Statement IRI (Stat-IRI) from deposit receipt ...");
        receipt = Common.parse(bodyText);
        Link statIriLink = receipt.getLink("http://purl.org/net/sword/terms/statement");
        IRI statIri = statIriLink.getHref();
        System.out.println("Stat-IRI = " + statIri);

        // 5. Check statement every ten seconds (a bit too frantic, but okay for this test). If status changes:
        // report new status. If status is an error (INVALID, REJECTED, FAILED) or ARCHIVED: exit.
        return Common.trackDeposit(http, statIri.toURI());
    }
}

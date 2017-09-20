/**
 * Copyright (C) 2016-17 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.sword2examples;

import org.apache.abdera.i18n.iri.IRI;

import java.io.File;
import java.net.URI;

public class SequenceSimpleDeposit {

  public static void main(String[] args) throws Exception {
    if (args.length < 4) {
      System.err.println("Usage: java nl.knaw.dans.easy.sword2examples.SequenceSimpleDeposit <Col-IRI> <EASY uid> <EASY passwd> <bag dirname>...");
      System.exit(1);
    }

    // 0. Read command line arguments
    final IRI colIri = new IRI(args[0]);
    final String uid = args[1];
    final String pw = args[2];

    final String[] bagNames = new String[args.length - 3];
    System.arraycopy(args, 3, bagNames, 0, bagNames.length);

    System.out.println("Sending base revision of dataset ...");
    File baseBagDir = new File(bagNames[0]);
    File tempCopy = Common.copyToTarget(baseBagDir);
    URI baseUri = SimpleDeposit.depositPackage(tempCopy, colIri, uid, pw);

    for(int i = 1; i < bagNames.length; ++i) {
      File bagDir = new File(bagNames[i]);
      tempCopy = Common.copyToTarget(baseBagDir);
      Common.setBagIsVersionOf(tempCopy, baseUri);
      SimpleDeposit.depositPackage(tempCopy, colIri, uid, pw);
    }
  }
}

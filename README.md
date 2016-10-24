easy-sword2-dans-examples
=========================

Example client code for interacting with the EASY SWORD2 Deposit Service at DANS


About These Examples
--------------------

The examples in this project show how to implement a client in Java that interacts with the
EASY SWORD2 Desposit service at DANS. For a more detailed overview of the functionality of this
service see [easy-sword2]. This document provides details about the particular set-up at DANS
and how to use it.

[easy-sword2]: https://github.com/DANS-KNAW/easy-sword2

Using The DANS Acceptance Test Server
-------------------------------------

The following is a step-by-step instruction on how to run these examples using the DANS
acceptance test server at https://act.easy.dans.knaw.nl/.

1. From your account manager at DANS request access to the acceptance test server. In order to access the acceptance test server
   your IP address (range) needs to be whitelisted.
    
2. Create an EASY account via https://act.easy.dans.knaw.nl/ui/register.

3. From your account manager at DANS request the account to be enabled for SWORD deposits.

4. Run the examples using as Collection IRI (Col-IRI): https://act.easy.dans.knaw.nl/sword2/collection/1

After a successful run you should be able to see your deposit in your browser at the URL sent back to you, after
you log in via https://act.easy.dans.knaw.nl/ui/login.


Building
--------

To build the examples you need:

* Java 8 or higher
* Maven 3.3.3 or higher

It may also work with lower versions of Java and Maven, but this has not been tested.

Steps:

        git clone https://github.com/DANS-KNAW/easy-sword2-dans-examples.git
        cd easy-sword2-dans-examples
        mvn install


Deposit Packaging
-----------------

The [easy-sword2] service requires deposits to be sent as zipped bags (see [BagIt]). The EASY archive adds some
extra requirements:

* Apart from the `data` directory there must be a `metadata` directory
* `metadata` must contain two files:
    * `dataset.xml` &mdash; a valid [DDM] file describing the dataset as a whole
    * `files.xml` &mdash; an xml file describing the individual files (TODO: document exact format).
    
For an example, see `src/main/resources/examples/example-bag` in this project.
  
[BagIt]: https://datatracker.ietf.org/doc/draft-kunze-bagit
[DDM]: https://easy.dans.knaw.nl/schemas/md/2016/ddm.xsd


Running The Example
-------------------

You can run the example clients either from the command line or in your IDE. Each example contains a class with a `main` method
and expects certain arguments. To run from the command line, after building the project, unpack the resulting `tar.gz` file, for
example:

    tar -xzvf target/easy-sword2-dans-examples-1.x-SNAPSHOT.tar.gz -C ~/Downloads

This will create a new directory called `~/Downloads/easy-sword2-dans-examples-1.x-SNAPSHOT`. Change directory to this directory
before executing the commands below.

### Zipping the Example Bag

To zip the example bag, `cd` to the `examples` subdirectory and execute the following command:

    zip -r example.zip example-bag

Make sure that the result zip contains a directory called `example-bag` at the top of its hierarchy. The output of
 
    zipinfo examples/example.zip

should be similar to this: 

    Archive:  examples/example.zip   4293 bytes   13 files
    drwxr-xr-x  3.0 unx        0 bx stor  8-Jul-16 13:22 example-bag/
    -rw-r--r--  3.0 unx       63 tx defN  6-Jul-16 16:18 example-bag/bag-info.txt
    -rw-r--r--  3.0 unx       55 tx stor  6-Jul-16 16:18 example-bag/bagit.txt
    drwxr-xr-x  3.0 unx        0 bx stor  8-Jul-16 13:22 example-bag/data/
    drwxr-xr-x  3.0 unx        0 bx stor  8-Jul-16 13:22 example-bag/data/path/
    drwxr-xr-x  3.0 unx        0 bx stor  8-Jul-16 13:22 example-bag/data/path/to/
    -rw-r--r--  3.0 unx     1935 tx defN  6-Jul-16 16:18 example-bag/data/path/to/file.txt
    -rw-r--r--  3.0 unx      202 tx defN  6-Jul-16 16:18 example-bag/data/quicksort.hs
    -rw-r--r--  3.0 unx      105 tx defN  6-Jul-16 16:18 example-bag/manifest-md5.txt
    drwxr-xr-x  3.0 unx        0 bx stor  8-Jul-16 13:22 example-bag/metadata/
    -rw-r--r--  3.0 unx     1731 tx defN  6-Jul-16 16:18 example-bag/metadata/dataset.xml
    -rw-r--r--  3.0 unx      881 tx defN  6-Jul-16 16:18 example-bag/metadata/files.xml
    -rw-r--r--  3.0 unx      250 tx defN  6-Jul-16 16:18 example-bag/tagmanifest-md5.txt
    13 files, 5222 bytes uncompressed, 1957 bytes compressed:  62.5%

### Changing the Example Bag

To change the bag or make your own bags see [this wiki page].

[this wiki page]: https://github.com/DANS-KNAW/easy-sword2/wiki/HOWTO---Create-an-EASY-Deposit-Bag


### SimpleDeposit.java

To run the `SimpleDeposit` example, execute the following command, of course after filling in the username and password of your SWORD-enabled
EASY-account.

    java -cp bin/easy-sword2-dans-examples.jar nl.knaw.dans.easy.sword2examples.SimpleDeposit \
        examples/example.zip https://act.easy.dans.knaw.nl/sword2/collection/1 <username> <password>


### ContinuedDeposit.java

To run the `ContinuedDeposit` example, execute the following command, of course after filling in the username and password of your SWORD-enabled
EASY-account. This example takes one extra argument: the number of bytes in one chunk.

    java -cp bin/easy-sword2-dans-examples.jar nl.knaw.dans.easy.sword2examples.ContinuedDeposit \
        examples/example.zip https://act.easy.dans.knaw.nl/sword2/collection/1 <username> <password> <chunk-size>


### A bag with fetch.txt

A bag may contain a file called `fetch.txt`, which contains references to files that belong to this bag but are not present. To run an example
with this kind of bag, zip `example-bag-with-fetch` to `example.zip` and execute the following command (the same as for `SimpleDeposit`),
of course after filling in the username and password of your SWORD-enabled EASY-account.
                                                                                                      
    java -cp bin/easy-sword2-dans-examples.jar nl.knaw.dans.easy.sword2examples.SimpleDeposit \
        examples/example.zip https://act.easy.dans.knaw.nl/sword2/collection/1 <username> <password>


### Running from the Project Directory

Alternatively, you may run the example programs from the command line in the maven project. Open a command line in the root
of the maven project and then type the following (for `SimpleDeposit`): 

    mvn clean install
    mvn dependency:copy-dependencies
    java -cp "target/dependency/*:target/easy-sword2-dans-examples.jar" nl.knaw.dans.easy.sword2examples.SimpleDeposit \
       src/test/resources/examples/example.zip https://act.easy.dans.knaw.nl/sword2/collection/1 <username> <password>

This of course assumes that you have first zipped the example bag to the `src/test/resources/examples` sub-directory.



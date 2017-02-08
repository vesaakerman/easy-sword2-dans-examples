easy-sword2-dans-examples
=========================

Example client code for interacting with the EASY SWORD2 Deposit Service at DANS


About These Examples
--------------------

The examples in this project show how to implement a client in Java that interacts with the
EASY SWORD2 Deposit service at DANS. For a more detailed overview of the functionality of this
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
extra requirements. These are documented on the [EASY SWORDv2 deposit documentation page].
  
[EASY SWORDv2 deposit documentation page]: https://easy.dans.knaw.nl/doc/sword2.html
[BagIt]: https://datatracker.ietf.org/doc/draft-kunze-bagit


Running The Examples
--------------------

You can run the example clients either from the command line or in your IDE. Each example contains a class with a `main` method
and expects certain arguments. To run from the command line, after building the project, unpack the resulting `tar.gz` file, for
example:

    tar -xzvf target/easy-sword2-dans-examples-1.x-SNAPSHOT.tar.gz -C ~/Downloads

This will create a new directory called `~/Downloads/easy-sword2-dans-examples-1.x-SNAPSHOT`. Change directory to this directory
before executing the commands below.

The examples take one or more directories as input parameters. These directories should be in the format specified by BagIt. The code 
copies each directory to the `target`-folder of the project, zips it and sends it to the specified SWORDv2 service. The copying step 
has been built in because in some examples the bag must be modified before it is sent.

### Changing the Example Bags

To change the bag or make your own bags see [this wiki page].

[this wiki page]: https://github.com/DANS-KNAW/easy-sword2/wiki/HOWTO---Create-an-EASY-Deposit-Bag


### SimpleDeposit.java

To run the `SimpleDeposit` example, execute the following command, of course after filling in the username and password of your SWORD-enabled
EASY-account. For example:

    java -cp bin/easy-sword2-dans-examples.jar nl.knaw.dans.easy.sword2examples.SimpleDeposit \
        https://act.easy.dans.knaw.nl/sword2/collection/1 myuser mypassword src/main/resources/examples/medium


### ContinuedDeposit.java

To run the `ContinuedDeposit` example, execute the following command, of course after filling in the username and password of your SWORD-enabled
EASY-account. This example takes one extra argument: the number of bytes in one chunk.

    java -cp bin/easy-sword2-dans-examples.jar nl.knaw.dans.easy.sword2examples.ContinuedDeposit \
         https://act.easy.dans.knaw.nl/sword2/collection/1 myuser mypassword 50000 src/main/resources/examples/medium 

### SequenceXxx.java

It is possible to specify that a new bag is an update of an existing dataset. The SequenceXxx example demonstrate the sub-protocol used. The
command lines for the examples are almost exactly the same as for the ones concerned with a single deposit. The difference is that multiple
bag directories should be passed each representing a revision of the dataset, e.g.,

    java -cp bin/easy-sword2-dans-examples.jar nl.knaw.dans.easy.sword2examples.SequenceContinuedDeposit \
         https://act.easy.dans.knaw.nl/sword2/collection/1 myuser mypassword 50000 src/main/resources/examples/revision01 \
          src/main/resources/examples/revision02 src/main/resources/examples/revision03

### Running from the Project Directory

Instead of unarchiving the `tar.gz` file, you may choose to run the example programs from the command line in the maven project. Open a command line in the root
of the maven project and then type the following (for `SimpleDeposit`): 

    mvn clean install
    mvn dependency:copy-dependencies
    java -cp "target/dependency/*:target/easy-sword2-dans-examples.jar" nl.knaw.dans.easy.sword2examples.SimpleDeposit \
       https://act.easy.dans.knaw.nl/sword2/collection/1 myuser mypassword src/main/resources/examples/medium




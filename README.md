easy-sword2-dans-examples
=========================

Example client code for interacting with the EASY SWORD2 Deposit Service at DANS


About These Examples
--------------------

The examples in this project show how to implement a client in Java that interacts with the
EASY SWORD2 Desposit service at DANS. For a more detailed overview of the functionality of this
service see [easy-sword2]. This document provides details about the particular set-up at DANS
and how to use it.


How Run the Examples?
---------------------

The following is a step-by-step instruction on how to run these examples using the DANS
acceptance test server at https://act.easy.dans.knaw.nl/.

1. Request access to the acceptance test server. In order to access the acceptance test server
   your IP address (range) needs to be whitelisted.
    
2. Create an EASY account via https://act.easy.dans.knaw.nl/ui/register.

3. Request the account to be enabled for SWORD deposits.

4. Run the examples using as Collection IRI (Col-IRI): https://act.easy.dans.knaw.nl/sword2/collection/1

After a successful run you should be able to see your deposit in your browser at the URL sent back to you, after
you log in via https://act.easy.dans.knaw.nl/ui/login.



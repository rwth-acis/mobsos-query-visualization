LAS2peer-Query-Visualization-Service
====================================

This service can be used to visualize queries on RDB's.
The start_network.bat script uses the L2PNodeLauncher class to start the service. It does also register
the content of the "startup" directory and it starts the HTTP-connector at port 8080.

Steps to take before launching:

1. Add a default database by adding the service configuration file located in the "config" folder.

2. Check the LASHOST variable at the queryviz.js (should be ok if you use the default startup script)

3. Change the address of the "qv_code_template" script (located at the demo.html file) according to your
setup. This is needed for exported queries to work.


There also exists a help-document that explains the usage of the service itself.

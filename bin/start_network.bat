:: this script starts a las2peer node providing the example service of this project
:: pls execute it from the bin folder of your deployment by double-clicking on it

cd ..
set BASE=%CD%
set CLASSPATH="%BASE%/lib/*;"

java -cp %CLASSPATH% i5.las2peer.tools.L2pNodeLauncher -p 9011 "startService('i5.las2peer.services.mobsos.queryVisualization.QueryVisualizationService@0.6.7','someNewPass')" startWebConnector interactive
pause

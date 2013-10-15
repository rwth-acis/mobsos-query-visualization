set BASE=%CD%
set CLASSPATH="%BASE%/lib/*;"

java -cp %CLASSPATH% i5.las2peer.testing.L2pNodeLauncher -s 9011 - uploadStartupDirectory startService('i5.las2peer.services.queryVisualization.QueryVisualizationService','qvPass') startHttpConnector interactive
pause
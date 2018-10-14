java -XX:+UnlockCommercialFeatures -XX:+FlightRecorder -XX:StartFlightRecording=delay=60s,duration=60s,name=recording,filename=recording.jfr -Xms2G -Xmx2G -server -XX:+UseNUMA -XX:+UseParallelGC -XX:+AggressiveOpts -Dvertx.disableMetrics=true -Dvertx.disableH2c=true -Dvertx.disableWebsockets=true -Dvertx.flashPolicyHandler=false -Dvertx.threadChecks=false -Dvertx.disableContextTimings=true -Dvertx.disableTCCL=true  -Dvertx.host=0.0.0.0 -Dvertx.port=8080 -jar target/vertx-core-fat.jar -instances 32
if [ -z "VERTX_HOST" ]; then
  VERTX_HOST=172.16.0.2
fi
java -XX:+UnlockCommercialFeatures -XX:+FlightRecorder -XX:StartFlightRecording=delay=60s,duration=60s,name=recording,filename=recording.jfr -XX:+UseBiasedLocking -XX:BiasedLockingStartupDelay=0 -XX:+AggressiveOpts -Dio.netty.allocator.numDirectArenas=32 -Dio.netty.allocator.numHeapArenas=32 -Dvertx.disableWebsockets=true -Dvertx.disableH2c=true -Dvertx.disableContextTimings=true -Dvertx.disableTCCL=true -Dvertx.threadChecks=false -Dvertx.host=$VERTX_HOST -Dvertx.port=8080 -jar target/server-3.4.2-SNAPSHOT-fat.jar -instances 32
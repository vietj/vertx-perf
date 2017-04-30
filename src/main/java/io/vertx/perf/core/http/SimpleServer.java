package io.vertx.perf.core.http;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class SimpleServer extends AbstractVerticle {

  private final Buffer helloWorldBuffer = Buffer.buffer("Hello, world!");
  private final CharSequence helloWorldContentLength = HttpHeaders.createOptimized(String.valueOf(helloWorldBuffer.length()));
  private final DateFormat DATE_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyyy HH:mm:ss z");


  private static final CharSequence HEADER_SERVER_VERTX = HttpHeaders.createOptimized("vert.x");
  private static final CharSequence RESPONSE_TYPE_PLAIN = HttpHeaders.createOptimized("text/plain");

  private CharSequence dateString;


  private void setHeaders(HttpServerResponse resp) {
    resp.putHeader(HttpHeaders.CONTENT_TYPE, RESPONSE_TYPE_PLAIN);
    resp.putHeader(HttpHeaders.CONTENT_LENGTH, helloWorldContentLength);
    resp.putHeader(HttpHeaders.SERVER, HEADER_SERVER_VERTX );
    resp.putHeader(HttpHeaders.DATE, dateString);
  }

  private void formatDate() {
    dateString = HttpHeaders.createOptimized(DATE_FORMAT.format(new Date()));
  }

  public void start() throws Exception {

    InputStream in = Vertx.class.getClassLoader().getResourceAsStream("vertx-version.txt");
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] buffer = new byte[256];
    while (true) {
      int amount = in.read(buffer);
      if (amount == -1) {
        break;
      }
      out.write(buffer, 0, amount);
    }
    System.out.println("Vertx: " + out.toString());
    System.out.println("Default Event Loop Size: " + VertxOptions.DEFAULT_EVENT_LOOP_POOL_SIZE);

    vertx.setPeriodic(1000, tid -> formatDate());
    formatDate();
    HttpServer server = vertx.createHttpServer();
    String host = System.getProperty("vertx.host", "localhost");
    int port = Integer.getInteger("vertx.port", 8080);
    System.out.println("Host: " + host);
    System.out.println("Port: " + port);
    Handler<Throwable> errHandler = this::handleErr;
    server.requestHandler(req -> {
      req.exceptionHandler(errHandler);
      HttpServerResponse resp = req.response();
      setHeaders(resp);
      resp.exceptionHandler(errHandler);
      resp.end(helloWorldBuffer);
    }).listen(port, host);
  }

  private final Executor errs = Executors.newSingleThreadExecutor();

  private void handleErr(Throwable t) {
    errs.execute(() -> {
      t.printStackTrace();
    });
  }
}

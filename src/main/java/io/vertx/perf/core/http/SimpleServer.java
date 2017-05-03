package io.vertx.perf.core.http;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class SimpleServer extends AbstractVerticle implements Handler<HttpServerRequest> {

  private static final boolean DISABLE_OPT_HEADERS = Boolean.getBoolean("vertx.disableOptimizedHeaders");
  private static final String HOST = System.getProperty("vertx.host", "localhost");
  private static final int PORT = Integer.getInteger("vertx.port", 8080);

  static {
    try {
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
    } catch (IOException e) {
      e.printStackTrace();
    }
    System.out.println("Default Event Loop Size: " + VertxOptions.DEFAULT_EVENT_LOOP_POOL_SIZE);
    System.out.println("Host: " + HOST);
    System.out.println("Port: " + PORT);
    System.out.println("Optimized headers : " + !DISABLE_OPT_HEADERS);
  }

  private final DateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyyy HH:mm:ss z");
  private CharSequence dateString;
  private final Buffer buffer = Buffer.buffer("Hello, world!");
  private final CharSequence contentLength = DISABLE_OPT_HEADERS ? ("" + buffer.length()) : HttpHeaders.createOptimized(String.valueOf(buffer.length()));
  private final CharSequence headerServerVertx = DISABLE_OPT_HEADERS ? "vert.x" : HttpHeaders.createOptimized("vert.x");
  private final CharSequence responseTypePlain = DISABLE_OPT_HEADERS ? "text/plain" : HttpHeaders.createOptimized("text/plain");

  private void formatDate() {
    if (DISABLE_OPT_HEADERS) {
      dateString = dateFormat.format(new Date());
    } else {
      dateString = HttpHeaders.createOptimized(dateFormat.format(new Date()));
    }
  }

  public void start() throws Exception {
    vertx.setPeriodic(1000, tid -> formatDate());
    formatDate();
    vertx.createHttpServer()
        .requestHandler(this)
        .listen(PORT, HOST);
  }

  @Override
  public void handle(HttpServerRequest req) {
    req.exceptionHandler(errHandler);
    HttpServerResponse resp = req.response();
    MultiMap header = resp.headers();
    header.add(HttpHeaders.CONTENT_TYPE, responseTypePlain);
    header.add(HttpHeaders.CONTENT_LENGTH, contentLength);
    header.add(HttpHeaders.SERVER, headerServerVertx);
    header.add(HttpHeaders.DATE, dateString);
    resp.exceptionHandler(errHandler);
    resp.end(buffer);
  }

  private final Executor errs = Executors.newSingleThreadExecutor();
  private final Handler<Throwable> errHandler = this::handleErr;

  private void handleErr(Throwable t) {
    errs.execute(() -> {
      t.printStackTrace();
    });
  }
}

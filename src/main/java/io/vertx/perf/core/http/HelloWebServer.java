package io.vertx.perf.core.http;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.perf.http.HelloServerHandler;
import io.netty.perf.http.HelloServerInitializer;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.impl.HttpServerImpl;
import io.vertx.core.http.impl.ServerConnection;
import io.vertx.core.impl.ContextImpl;
import io.vertx.core.impl.EventLoopContext;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.impl.HandlerHolder;

import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HelloWebServer {

  static {
    ResourceLeakDetector.setLevel(Level.DISABLED);
  }

  private final int port;

  public HelloWebServer(int port) {
    this.port = port;
  }

  public void run() throws Exception {
    // Configure the server.

//    if (Epoll.isAvailable()) {
//      doRun(new EpollEventLoopGroup(), EpollServerSocketChannel.class, true);
//    } else {
    VertxInternal vertx = (VertxInternal) Vertx.vertx();
    NioEventLoopGroup eventLoopGroup = (NioEventLoopGroup) vertx.nettyEventLoopGroup();
    eventLoopGroup.setIoRatio(50); // Initial value
    doRun(vertx, eventLoopGroup, NioServerSocketChannel.class, false);
//    }
  }

  private static final Buffer buffer = Buffer.buffer("Hello, world!");
  private static final CharSequence contentLength = HttpHeaders.createOptimized(String.valueOf(buffer.length()));
  private static final CharSequence headerServerVertx = HttpHeaders.createOptimized("vert.x");
  private static final CharSequence responseTypePlain = HttpHeaders.createOptimized("text/plain");
  private static final String dateString = new SimpleDateFormat("EEE, dd MMM yyyyy HH:mm:ss z").format(new Date());

  private void doRun(VertxInternal vertx, EventLoopGroup loupGroup, Class<? extends ServerChannel> serverChannelClass, boolean isNative) throws InterruptedException {
    try {
      InetSocketAddress inet = new InetSocketAddress(port);

      ServerBootstrap b = new ServerBootstrap();

      if (isNative) {
        b.option(EpollChannelOption.SO_REUSEPORT, true);
      }

      Map<Channel, ServerConnection> connectionMap = new ConcurrentHashMap<>();

      b.option(ChannelOption.SO_BACKLOG, 1024);
      b.option(ChannelOption.SO_REUSEADDR, true);
      b.group(loupGroup).channel(serverChannelClass).childHandler(new ChannelInitializer<SocketChannel>() {
        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
          ChannelPipeline p = ch.pipeline();
          p.addLast("encoder", new HttpResponseEncoder());
          p.addLast("decoder", new HttpRequestDecoder(4096, 8192, 8192, false));
          ContextImpl context = new EventLoopContext(vertx, loupGroup.next(), null, null, null, new JsonObject(), Thread.currentThread().getContextClassLoader());
          ServerConnection connection = new ServerConnection(vertx, null, new HttpServerOptions(), ch, context, "localhost", null);
          Handler<HttpServerRequest> app = request -> {
            HttpServerResponse response = request.response();
            MultiMap headers = response.headers();
            headers
                .add(HttpHeaders.CONTENT_TYPE, responseTypePlain)
                .add(HttpHeaders.SERVER, headerServerVertx)
                .add(HttpHeaders.DATE, dateString)
                .add(HttpHeaders.CONTENT_LENGTH, contentLength);
            response.end(buffer);
          };
          HandlerHolder<HttpServerImpl.Handlers> holder = new HandlerHolder<>(context, new HttpServerImpl.Handlers(app, null, null));
          HttpServerImpl.ServerHandler handler = new HttpServerImpl.ServerHandler(connectionMap, ch, holder, connection, null);
          p.addLast("handler", handler);
        }
      });
      b.childOption(ChannelOption.ALLOCATOR, new PooledByteBufAllocator(true));
      b.childOption(ChannelOption.SO_REUSEADDR, true);

      Channel ch = b.bind(inet).sync().channel();

      System.out.printf("Httpd started. Listening on: %s%n", inet.toString());

      ch.closeFuture().sync();
    } finally {
      loupGroup.shutdownGracefully().sync();
    }
  }

  public static void main(String[] args) throws Exception {
    int port;
    if (args.length > 0) {
      port = Integer.parseInt(args[0]);
    } else {
      port = 8080;
    }
    new HelloWebServer(port).run();
  }
}
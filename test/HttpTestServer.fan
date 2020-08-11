


class HttpTestServer : HttpHandler {

  override async Void onHttpService(HttpReq req, HttpRes res) {
    echo("Server receive: "+req.headers)
    res.headers["Content-Type"] = "text/html; charset=utf-8"

    buf := NioBuf.makeMem(1024)
    buf.printLine("<html>
                        <body>Hello World</body>
                       </html>")
    buf.flip
    //await res.writeFixed(buf)
    await res.writeChunk(buf)
  }

  static Void main() {
    Server {
      port = 8080
      handler = HttpTestServer#
    }.start
  }
}
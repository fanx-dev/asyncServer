


class HttpTestServer : HttpHandler {

  override async Void onHttpService(HttpReq req, HttpRes res) {
    echo("Server receive: "+req.headers)
    res.headers["Content-Type"] = "text/html; charset=utf-8"
    res.buf.printLine("<html>
                        <body>Hello World</body>
                       </html>")

    await res.finish
    //return true
  }

  static Void main() {
    Server {
      port = 8080
      handler = HttpTestServer#
    }.start
  }
}
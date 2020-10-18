

const class TestPost : HttpHandler {

  override async Void onHttpService(HttpReq req, HttpRes res) {
    try {
      echo("Server receive: "+req.uri)
      res.headers["Content-Type"] = "text/html; charset=utf-8"

      client := HttpClient("localhost", 8080)
      Buf sbuf := NioBuf.makeMem(1024)
      sbuf.print("Hi")
      sbuf.flip
      await client.send("POST", `/`, sbuf)

      Str? str
      while (true) {
          buf := await client.read
          if (buf == null) break
          await res.writeChunk(buf)
      }
      client.close
    } catch (Err e) {
      e.trace
    }
  }

  static Void main() {
    Server {
      port = 8081
      handler = TestPost()
    }.start
  }
}
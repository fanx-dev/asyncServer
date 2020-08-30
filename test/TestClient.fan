


const class TestClient : HttpHandler {

  override async Void onHttpService(HttpReq req, HttpRes res) {
    echo("Server receive: "+req.uri)
    res.headers["Content-Type"] = "text/html; charset=utf-8"

    //client := HttpClient("localhost", 8080)
    client := HttpClient("www.baidu.com", 80)
    await client.get(`/`)

    while (true) {
        buf := await client.read
        if (buf == null) break
        await res.writeChunk(buf)
    }
  }

  static Void main() {
    Server {
      port = 8081
      handler = TestClient()
    }.start
  }
}
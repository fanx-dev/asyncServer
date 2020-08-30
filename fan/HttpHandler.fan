
const abstract class HttpHandler : Handler {

  override async Void onService(Socket socket) {
    //echo("onService: $socket")
    while (true) {
      close := await doHttp(socket)
      //echo("Connection close: $close, $socket")
      if (close) break
    }
  }

  protected virtual async Bool doHttp(Socket socket) {
    req := await HttpReq(socket).parse
    res := HttpRes(socket)
    await onHttpService(req, res)
    await res.close
    return (req.headers["Connection"] == "close")
  }

  abstract async Void onHttpService(HttpReq req, HttpRes res)
}


class HttpReq {
  private Buf buf
  private Socket socket

  Str? method
  Uri? uri
  Str? ver
  [Str:Str]? headers

  new make(Socket socket) {
    this.socket = socket
    this.buf = NioBuf.makeMem(4096)
    this.buf.size = 0
  }

  async HttpReq parse() {
    while (true) {
      line := await readLine
      if (line.isEmpty) continue
      toks := line.split
      method = toks[0]
      uri = Uri.fromStr(toks[1])
      ver = toks[2]
      break;
    }

    headers = CaseInsensitiveMap<Str,Str>()
    while (true) {
      line2 := await readLine
      //echo("readLine:$line2")
      if (line2.isEmpty) break
      fs := line2.splitBy(":", 2)
      key := fs[0].trim
      value := fs[1].trim
      headers[key] = value
    }

    //echo("end parse: $headers")
    return this
  }

  private async Str readLine() {
    //echo("readLine, buf:$buf")
    await HttpUtil.cur.readLine(socket, buf)
  }
}




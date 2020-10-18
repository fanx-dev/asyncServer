
class HttpReq {
  private Buf buf
  private Socket socket

  Str? method
  Uri? uri
  Str? ver
  [Str:Str]? headers
  private Bool moreBuf

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
    if (method == "POST") moreBuf = true
    return this
  }

  async Buf? read() {
    if (!moreBuf) return null
    //echo("readBuf")
    length := 0
    Buf? nbuf
    if (headers["Content-Length"] != null) {
      //echo("readContent-Length")
      length = headers["Content-Length"].toInt
      //echo("length:$length")
      nbuf = NioBuf.makeMem(length)
      nbuf.writeBuf(buf, length.min(buf.remaining))
      if (length-nbuf.pos > 0) {
        await socket.read(nbuf, length-nbuf.pos)
      }
      nbuf.flip
      //echo("nbuf:$nbuf")
      moreBuf = false
      return nbuf
    }
    else if (headers["Transfer-Encoding"] == "chunked") {
      //echo("readChunked")
      line := await readLine
      length = Int.fromStr(line, 16)
      if (length == 0) moreBuf = false
      //echo("length:$length")
      //read:\r\n
      length += 2
      nbuf = NioBuf.makeMem(length)
      //echo("$nbuf, $buf")
      nbuf.writeBuf(buf, length.min(buf.remaining))
      if (length-nbuf.pos > 0) {
        await socket.read(nbuf, length-nbuf.pos)
      }
      nbuf.flip
      return nbuf
    }
    throw Err("unsupport")
  }

  private async Str readLine() {
    //echo("readLine, buf:$buf")
    await HttpUtil.cur.readLine(socket, buf)
  }
}




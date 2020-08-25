
class HttpClient {
  Buf buf
  Socket? socket
  private Str host
  private Int port
  Str? ver
  private Bool moreBuf

  Int status
  Str? msg
  [Str:Str]? headers
  
  new make(Str host, Int port := 80) {
    this.host = host
    this.port = port
    this.buf = NioBuf.makeMem(1024)
    this.buf.size = 0
  }

  async Void get(Uri uri) {
    if (socket == null) {
      socket = await Socket.connect(host, port)
    }
    
    buf.print("GET $uri HTTP/1.1\r\n")
    buf.print("Host: $host\r\n")
    //buf.print("User-Agent: Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.7.6)\r\n")
    buf.print("\r\n")
    buf.flip
    //echo("$buf.readAllStr")
    await socket.write(buf)

    buf.clear
  
    while (true) {
      line := await readLine
      if (line.isEmpty) continue
      toks := line.split
      //echo("toks: $toks")
      ver = toks[0]
      status = (toks[1]).toInt
      msg = toks[2]
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
    moreBuf = true
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
      nbuf.writeBuf(buf)
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
      nbuf.writeBuf(buf)
      if (length-nbuf.pos > 0) {
        await socket.read(nbuf, length-nbuf.pos)
      }
      nbuf.flip
      return nbuf
    }
    throw Err("unsupport")
  }

  internal async Str readLine() {
    //echo("readLine, buf:$buf")
    start := buf.pos
    checkPos := start
    while (true) {
      for (; checkPos < buf.size; ++checkPos) {
        if (buf.get(checkPos) == '\r') {
          buf.seek(start)
          line := buf.readChars(checkPos-start)

          //skip \r\n
          buf.read
          buf.read

          //echo("readLine res:$line, buf:$buf")
          return line
        }
      }

      //echo("pos: $pos")
      buf.seek(buf.size)
      buf.size = buf.capacity
      n := await socket.read(buf, 1)
      //echo("n:$n, buf:$buf")

      buf.size = buf.pos
      buf.seek(checkPos)
      //echo("n:$n, buf:$buf")
      if (n < 0) {
        buf.seek(start)
        if (buf.remaining > 0) {
          return buf.readAllStr
        }
        throw IOErr("EOF")
      }
    }
    return ""
  }
}

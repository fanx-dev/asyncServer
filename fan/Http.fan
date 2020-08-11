
class HttpReq {
  private Buf buf
  private Socket socket

  Str? method
  Str? uri
  Str? ver
  [Str:Str]? headers

  new make(Socket socket) {
    this.socket = socket
    this.buf = NioBuf.makeMem(1024)
    this.buf.size = 0
  }

  async HttpReq parse() {
    while (true) {
      line := await readLine
      if (line.isEmpty) continue
      toks := line.split
      method = toks[0]
      uri = toks[1]
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


class HttpRes {
  Buf buf
  private Socket socket
  Str:Str headers
  Int statusCode := 200
  private Bool isCommitted := false
  private Bool isChunked := true

  new make(Socket socket) {
    this.socket = socket
    this.buf = NioBuf.makeMem(1024)
    headers = CaseInsensitiveMap<Str,Str>()
  }

  async Void writeFixed(Buf buffer) {
    await writeHeaders(buffer.size)
    await socket.write(buffer)
  }

  private async Void writeHeaders(Int contentLength) {
    if (isCommitted) {
      throw Err("Res already committed")
    }

    isCommitted = true
    //buf := NioBuf.makeMem(1024)
    buf.print("HTTP/1.1 ").print(statusCode).print(" ").print("Msg").print("\r\n")

    if (contentLength == -1) {
      isChunked = true
      headers["Transfer-Encoding"] = "chunked"
    }
    else {
      isChunked = false
      headers["Content-Length"] = contentLength.toStr
    }

    headers.each |v, k| {
      buf.print(k).print(": ").print(v).print("\r\n")
    }

    buf.print("\r\n")
    buf.flip
    await socket.write(buf)
    buf.clear
  }

  async Void writeChunk(Buf buffer) {
    if (!isCommitted) {
      await writeHeaders(-1)
    }

    buf.print(buffer.size.toHex).print("\r\n")
    buf.writeBuf(buffer, buffer.remaining)
    buf.print("\r\n")
    buf.flip
    await socket.write(buf)
    buf.clear
  }

  async Void close() {
    if (isChunked) {
      buf.print("0\r\n\r\n")
      buf.flip
      await socket.write(buf)
      buf.clear
    }
  }
}

abstract class HttpHandler : Handler {

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


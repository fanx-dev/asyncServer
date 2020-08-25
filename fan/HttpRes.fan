

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
    //buf.writeBuf(buffer, buffer.remaining)
    buf.flip
    await socket.write(buf)
    buf.clear

    await socket.write(buffer)
    
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



const class HttpUtil {
  static const HttpUtil cur := HttpUtil()
  
  async Str readLine(Socket socket, Buf buf) {
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
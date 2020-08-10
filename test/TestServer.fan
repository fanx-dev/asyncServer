

class TestServer : Handler {

  override async Void onService(Socket socket) {
    buf := NioBuf.makeMem(1024)
    n := await socket.read(buf, 1)
    buf.flip()
    echo("=====server receive: $buf: "+buf.readAllStr)

    
    buf.clear
    buf.printLine("HelloWorld")
    buf.flip
    echo("=====buf:$buf")
    n2 := await socket.write(buf)
    echo("=====send: "+n2)
    
  }

  static Void main() {
    Server {
      port = 8080
      handler = TestServer#
    }.start
  }
}




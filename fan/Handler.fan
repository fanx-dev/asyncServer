
using concurrent

native class Socket {
  Promise<Int> read(Buf buf, Int size)
  Promise<Int> write(Buf buf, Int size := buf.remaining)
  Bool close()

  static Promise<Socket> connect(Str host, Int port)
}

native abstract class Handler {
  abstract async Void onService(Socket socket)
}

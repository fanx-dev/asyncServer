
using concurrent

native class Socket {
  Promise<Int> read(Buf buf, Int size)
  Promise<Int> write(Buf buf, Int size := buf.remaining)
  Bool close()
}

native abstract class Handler {
  
  Promise<Socket> connect(Str host, Int port)

  abstract async Void onService(Socket socket)
}
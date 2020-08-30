
using concurrent

**
** TCP endpoint.
**
native class Socket {
  Promise<Int> read(Buf buf, Int size)
  Promise<Int> write(Buf buf, Int size := buf.remaining)
  Bool close()

  static Promise<Socket> connect(Str host, Int port)
}

**
** incoming callback in server socket
**
native const mixin Handler {
  abstract async Void onService(Socket socket)
}

**
** Context Adapter to support concurrent::Acotr
**
native const class ActorWorker {
  static ActorWorker fromActor(Actor actor)
  Bool onReceive(Obj? obj)
}

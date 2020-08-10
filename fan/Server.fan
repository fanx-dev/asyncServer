

class Server {
  Str host = "localhost"
  Int port = 12306
  Type handler
  Int threadSize = 10
  Int limit = 10240

  new make(|This| f) { f(this) }

  native Void start()

  internal Handler create() {
    handler.make()
  }
}

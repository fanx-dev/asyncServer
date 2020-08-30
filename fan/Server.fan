

class Server {
  Str host = "localhost"
  Int port = 12306
  Handler handler
  Int threadSize = 10
  Int limit = 10240


  new make(|This| f) { f(this) }

  native Void start(Bool join := true)
}


using concurrent

const class ActorWorkerTest : Actor {
  const ActorWorker worker := ActorWorker.fromActor(this)

  new make() : super.make() {}

  protected override Obj? receive(Obj? msg) {
    if (worker.onReceive(msg)) return null
    test()
    return null
  }


  async Void test() {
    client := HttpClient("www.baidu.com", 80)
    //echo("debug1")
    await client.get(`/`)
    //echo("debug2")

    while (true) {
        buf := await client.read
        if (buf == null) break
        echo(buf.readAllStr)
    }
    client.close
  }

  static Void main() {
    ActorWorkerTest().send(null)
    Actor.sleep(30sec)
  }

}
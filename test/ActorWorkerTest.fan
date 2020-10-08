
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
    client := HttpClient("localhost", 8080)

    try {
      await doReq(client)
    }
    catch (Err e){
      echo("ERROR1: $e")
    }

    echo("end doReq1")

    try {
      await doReq(client)
    }
    catch (Err e2) {
      echo("ERROR2: $e2")
    }

    echo("end doReq2")
    client.close
  }

  private async Void doReq(HttpClient client) {
    echo("debug1")
    await client.get(`/abc`)
    echo("debug2")

    while (true) {
        buf := await client.read
        if (buf == null) break
        echo(buf.readAllStr)
    }
  }

  static Void main() {
    ActorWorkerTest().send(null)
    Actor.sleep(30sec)
  }

}
//> using jsModuleKind es
//> using deps "dev.cheleb::zio-laminar-tapir:2.0.0-local"

import dev.cheleb.ziotapir.laminar.*

import zio.json.*
import sttp.tapir.*
import sttp.tapir.json.zio.*
import sttp.tapir.generic.auto.*
import demo.HttpError
import com.raquo.airstream.eventbus.EventBus

object `getting-started`:
  case class GetResponse(
      args: Map[String, String],
      headers: Map[String, String]
  ) derives JsonCodec

  val get = endpoint.get
    .in("get")
    .out(jsonBody[GetResponse])
    .errorOut(statusCode and plainBody[String])
    .mapErrorOut[Throwable](HttpError.decode)(HttpError.encode)

  val eventBus = EventBus[GetResponse]()

  get(()).emit(eventBus)

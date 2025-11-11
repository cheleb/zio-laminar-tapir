package demo

import dev.cheleb.ziotapir.*
import dev.cheleb.ziojwt.WithToken

//import zio.prelude.*
import zio.json.*
case class UserToken(token: String, expiration: Long, zozo: String)
    extends dev.cheleb.ziojwt.WithToken derives JsonDecoder

given JsonCodec[WithToken] = DeriveJsonCodec
  .gen[UserToken]
  .transform(
    identity,
    wt => UserToken(wt.token, wt.expiration, "default-zozo")
  )

given session: Session[UserToken] = SessionLive[UserToken]
